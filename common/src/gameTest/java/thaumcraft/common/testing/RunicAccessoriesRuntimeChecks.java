package thaumcraft.common.testing;

import java.util.Map;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.gametest.framework.GameTestInfo;
import net.minecraft.gametest.framework.GameTestListener;
import net.minecraft.gametest.framework.GameTestRunner;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.animal.Cow;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import thaumcraft.common.config.ThaumcraftBaubles;
import thaumcraft.common.config.ThaumcraftContent;
import thaumcraft.common.items.wands.WandData;
import thaumcraft.common.lib.baubles.PlayerAccessories;
import thaumcraft.common.lib.runic.RunicShield;

/** Actual equipped categories, native connected player ticks/hurt, finite payment and all original depletion upgrades. */
public final class RunicAccessoriesRuntimeChecks {
    private RunicAccessoriesRuntimeChecks(){}
    public static void nativeEquipmentAndDepletionUpgrades(GameTestHelper test){
        var f=new TubeVariantsRuntimeChecks.Fixture(test);Cow[] victim={null};
        Runnable close=()->{if(f.closed)return;if(victim[0]!=null)victim[0].discard();f.drops();f.level.getServer().getConnection().getConnections().remove(f.connection);f.close();};
        var info=BoreWorkRuntimeChecks.testInfo(test);
        info.addListener(new GameTestListener(){
            @Override public void testStructureLoaded(GameTestInfo actual){}
            @Override public void testPassed(GameTestInfo actual,GameTestRunner runner){close.run();}
            @Override public void testFailed(GameTestInfo actual,GameTestRunner runner){close.run();}
            @Override public void testAddedForRerun(GameTestInfo previous,GameTestInfo next,GameTestRunner runner){close.run();}
        });
        try{
            f.open();var accessories=PlayerAccessories.get(f.player);
            accessories.setItem(0,new ItemStack(ThaumcraftBaubles.RUNIC_AMULET_EMERGENCY.get()));
            accessories.setItem(1,new ItemStack(ThaumcraftBaubles.RUNIC_RING_CHARGED.get()));
            accessories.setItem(2,new ItemStack(ThaumcraftBaubles.RUNIC_RING_HEALING.get()));
            accessories.setItem(3,new ItemStack(ThaumcraftBaubles.RUNIC_GIRDLE_KINETIC.get()));
            ItemStack payer=new ItemStack(ThaumcraftContent.WAND.get());payer.set(ThaumcraftContent.WAND_DATA.get(),new WandData("wood","gold",Map.of("aer",50,"terra",50)));f.player.getInventory().setItem(35,payer);
            f.player.setPos(f.base.getX()+.5,f.base.getY()-1,f.base.getZ()+.5);
            test.assertTrue(f.level.getServer().getConnection().getConnections().add(f.connection),"fixture joins the real native connection loop");
            boolean[] executed={false};
            test.onEachTick(()->{
                if(f.closed||executed[0])return;
                try{
                    var state=RunicShield.view(f.player);if(state.charge()!=1||f.player.tickCount<65)return;
                    executed[0]=true;
                    test.assertTrue(state.maximum()==24&&payer.get(ThaumcraftContent.WAND_DATA.get()).amount("aer")==0&&payer.get(ThaumcraftContent.WAND_DATA.get()).amount("terra")==0,
                            "native wearer discovers exact seven+four+four+nine maximum and pays only one50/50 raw recharge");
                    victim[0]=EntityType.COW.create(f.level);test.assertTrue(victim[0]!=null,"native cow factory");
                    victim[0].setPos(f.player.getX()+1.5,f.player.getY(),f.player.getZ());victim[0].setNoAi(true);victim[0].setNoGravity(true);f.level.addFreshEntity(victim[0]);
                    var floor=f.level.getBlockState(f.base.below(2));float health=f.player.getHealth();
                    f.player.hurt(f.level.damageSources().generic(),.5F);
                    var regeneration=f.player.getEffect(MobEffects.REGENERATION);
                    test.assertTrue(f.player.getHealth()==health&&RunicShield.view(f.player).charge()==8&&regeneration!=null&&regeneration.getDuration()==240&&regeneration.getAmplifier()==1,
                            "actual native fractional hit depletes one point, adds original regeneration240 amplifier1 and restores emergency eight without another vis payment");
                    test.assertTrue(victim[0].getHealth()<10&&f.level.getBlockState(f.base.below(2)).equals(floor)&&floor.is(Blocks.STONE),
                            "original kinetic blast damages a nearby real victim while preserving actual floor blocks");
                    // Native invulnerability subtracts the previous .5 hit, so9.5 supplies9 to the second armor/hurt stage.
                    f.player.hurt(f.level.damageSources().generic(),9.5F);
                    test.assertTrue(RunicShield.view(f.player).charge()==0&&f.player.getHealth()<health&&f.player.getEffect(MobEffects.REGENERATION).getDuration()==240,
                            "second real hit in the cooldown passes overflow health damage and cannot repeat emergency/healing upgrades");
                    close.run();test.succeed();
                }catch(RuntimeException|Error failure){close.run();throw failure;}
            });
            test.runAfterDelay(140,()->{if(!f.closed){close.run();test.fail("native accessory shield scenario did not receive its one finite recharge");}});
        }catch(RuntimeException|Error failure){close.run();throw failure;}
    }
}
