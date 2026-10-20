package thaumcraft.common.testing;

import java.util.Map;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.gametest.framework.GameTestInfo;
import net.minecraft.gametest.framework.GameTestListener;
import net.minecraft.gametest.framework.GameTestRunner;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import thaumcraft.common.config.ThaumcraftContent;
import thaumcraft.common.config.ThaumcraftEquipment;
import thaumcraft.common.items.wands.WandData;
import thaumcraft.common.lib.runic.RunicArmor;
import thaumcraft.common.lib.runic.RunicShield;

/** Declared augmented armor + finite charged wand; real connected player, native living ticks and actual native hurt. */
public final class RunicShieldRuntimeChecks {
    private RunicShieldRuntimeChecks() {}
    public static void run(GameTestHelper test) {
        var f=new TubeVariantsRuntimeChecks.Fixture(test);
        Runnable close=()-> { test.getLevel().getServer().getConnection().getConnections().remove(f.connection); f.close(); };
        var info=BoreWorkRuntimeChecks.testInfo(test);
        info.addListener(new GameTestListener() {
            @Override public void testStructureLoaded(GameTestInfo actual) {}
            @Override public void testPassed(GameTestInfo actual,GameTestRunner runner) { close.run(); }
            @Override public void testFailed(GameTestInfo actual,GameTestRunner runner) { close.run(); }
            @Override public void testAddedForRerun(GameTestInfo previous,GameTestInfo next,GameTestRunner runner) { close.run(); }
        });
        try {
            f.open();
            f.player.setItemSlot(EquipmentSlot.FEET,RunicArmor.augment(new ItemStack(ThaumcraftEquipment.THAUMIUM_BOOTS.get())));
            ItemStack wand=new ItemStack(ThaumcraftContent.WAND.get());
            wand.set(ThaumcraftContent.WAND_DATA.get(),new WandData("greatwood","thaumium",Map.of("aer",45,"terra",45)));
            f.player.getInventory().setItem(35,wand);
            test.assertTrue(f.level.getServer().getConnection().getConnections().add(f.connection),
                    "fixture transport joins the real server connection loop for native Player.aiStep");
            // Frugal and inventory discount behavior are shared with the checked player Repair path.
            boolean[] paid={false}; long[] broken={-1};
            CompoundTag[] checkpoint={null};
            test.onEachTick(()-> {
                if(f.closed) return;
                try {
                    var shield=RunicShield.view(f.player);
                    if(!paid[0]) {
                        // Let ServerPlayer's native60-tick login protection expire.
                        // Neither its private counter nor the hurt path is bypassed.
                        if(shield.charge()!=1 || f.player.tickCount<65) return;
                        test.assertTrue(shield.maximum()==1 && f.player.getInventory().getItem(35).get(ThaumcraftContent.WAND_DATA.get()).amount("aer")==0
                                && f.player.getInventory().getItem(35).get(ThaumcraftContent.WAND_DATA.get()).amount("terra")==0,
                                "ordinary player tick recharges one real shield point with original50-centivis aer+terra discounted to45 by thaumium cap");
                        var saved=f.player.saveWithoutId(new CompoundTag());
                        test.assertTrue(saved.getInt("thaumcraft:runic_charge")==1,"actual native player save invokes runic persistence mixin");
                        checkpoint[0]=saved;
                        float health=f.player.getHealth();
                        test.assertTrue(RunicShield.onHurt(f.player,f.level.damageSources().drown(),.5F)==.5F
                                && RunicShield.onHurt(f.player,f.level.damageSources().wither(),.5F)==.5F
                                && RunicShield.onHurt(f.player,f.level.damageSources().fellOutOfWorld(),.5F)==.5F
                                && RunicShield.onHurt(f.player,f.level.damageSources().starve(),.5F)==.5F
                                && RunicShield.view(f.player).charge()==1,"exact original four excluded native damage types preserve charge");
                        f.player.hurt(f.level.damageSources().generic(),.5F);
                        test.assertTrue(f.player.getHealth()==health && RunicShield.view(f.player).charge()==0
                                        && f.player.getItemBySlot(EquipmentSlot.FEET).getDamageValue()==0,
                                "actual native hurt hook consumes the integer shield point through fractional truncation and prevents health/armor loss");
                        paid[0]=true; broken[0]=f.level.getGameTime();
                    } else {
                        test.assertTrue(shield.charge()==0,"empty finite payer cannot fabricate additional shield charge");
                        if(f.level.getGameTime()-broken[0]<5) return;
                        // Explicit replay of the declared saved checkpoint proves
                        // native decode; it is not another recharge/acquisition.
                        f.player.load(checkpoint[0]);
                        test.assertTrue(RunicShield.view(f.player).charge()==1
                                        && f.player.getInventory().getItem(35).get(ThaumcraftContent.WAND_DATA.get()).amount("aer")==0
                                        && f.player.getInventory().getItem(35).get(ThaumcraftContent.WAND_DATA.get()).amount("terra")==0,
                                "native player NBT decode restores saved charge without refunding the payer");
                        close.run(); test.succeed();
                    }
                } catch(RuntimeException|Error failure) { close.run(); throw failure; }
            });
            test.runAfterDelay(120,()->{if(!f.closed){close.run();test.fail("ordinary living tick did not recharge one shield point from the declared finite wand");}});
        } catch(RuntimeException|Error failure) { close.run(); throw failure; }
    }
}
