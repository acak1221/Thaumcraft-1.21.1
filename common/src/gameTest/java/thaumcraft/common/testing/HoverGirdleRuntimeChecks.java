package thaumcraft.common.testing;

import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.gametest.framework.GameTestInfo;
import net.minecraft.gametest.framework.GameTestListener;
import net.minecraft.gametest.framework.GameTestRunner;
import net.minecraft.world.item.ItemStack;
import thaumcraft.common.config.ThaumcraftBaubles;
import thaumcraft.common.lib.baubles.PlayerAccessories;
import thaumcraft.common.lib.runic.RunicArmor;

/** Declared fall-distance boundary input; real worn slot and native player living ticks, no direct item tick invocation. */
public final class HoverGirdleRuntimeChecks {
    private HoverGirdleRuntimeChecks(){}
    public static void actualWornBeltReducesOnlyPositiveFallDistance(GameTestHelper test){
        var f=new TubeVariantsRuntimeChecks.Fixture(test);
        Runnable close=()->{f.level.getServer().getConnection().getConnections().remove(f.connection);f.close();};
        var info=BoreWorkRuntimeChecks.testInfo(test);
        info.addListener(new GameTestListener(){
            @Override public void testStructureLoaded(GameTestInfo actual){}
            @Override public void testPassed(GameTestInfo actual,GameTestRunner runner){close.run();}
            @Override public void testFailed(GameTestInfo actual,GameTestRunner runner){close.run();}
            @Override public void testAddedForRerun(GameTestInfo previous,GameTestInfo next,GameTestRunner runner){close.run();}
        });
        try{
            f.open();var inventory=PlayerAccessories.get(f.player);
            // Suspended native player with declared fall history: no gravity, floor intersection, or landing reset.
            f.player.setPos(f.base.getX()+.5,f.base.getY()+1,f.base.getZ()+.5);f.player.setDeltaMovement(net.minecraft.world.phys.Vec3.ZERO);
            inventory.setItem(3,new ItemStack(ThaumcraftBaubles.MUNDANE_BELT.get()));f.player.fallDistance=.2F;
            test.assertTrue(f.level.getServer().getConnection().getConnections().add(f.connection),"fixture connection participates in native Player.aiStep");
            int[] stage={0},since={f.player.tickCount};ItemStack[] girdle={null};
            test.onEachTick(()->{
                if(f.closed||f.player.tickCount-since[0]<3)return;
                try{
                    if(stage[0]==0){
                        test.assertTrue(Float.floatToIntBits(f.player.fallDistance)==Float.floatToIntBits(.2F),"ordinary worn belt preserves declared airborne fall distance in actual living ticks");
                        girdle[0]=new ItemStack(ThaumcraftBaubles.HOVER_GIRDLE.get());inventory.setItem(3,girdle[0]);stage[0]=1;
                    }else if(stage[0]==1){
                        test.assertTrue(Float.floatToIntBits(f.player.fallDistance)==Float.floatToIntBits(.2F-.33F)&&f.player.fallDistance<0,
                                "real worn Hover Girdle subtracts exact original FLOAT .33, crossing below zero without clamping");
                        test.assertTrue(inventory.getItem(3)==girdle[0]&&girdle[0].getCount()==1&&!girdle[0].isDamaged()&&!f.player.getAbilities().flying,
                                "actual belt survives with no invented durability/fuel/flight toggle");
                        f.player.fallDistance=0;stage[0]=2;
                    }else if(stage[0]==2){
                        test.assertTrue(f.player.fallDistance==0,"zero fall distance remains unchanged");f.player.fallDistance=-1;stage[0]=3;
                    }else{
                        test.assertTrue(f.player.fallDistance==-1&&RunicArmor.finalCharge(girdle[0])==0&&RunicArmor.finalCharge(RunicArmor.augment(girdle[0]))==1,
                                "negative fall distance remains unchanged and actual base-zero wearable remains a runic augmentation target");
                        close.run();test.succeed();
                    }
                    since[0]=f.player.tickCount;
                }catch(RuntimeException|Error failure){close.run();throw failure;}
            });
            test.runAfterDelay(100,()->{if(!f.closed){close.run();test.fail("native Hover Girdle living-tick boundary scenario did not finish; stage="+stage[0]);}});
        }catch(RuntimeException|Error failure){close.run();throw failure;}
    }
}
