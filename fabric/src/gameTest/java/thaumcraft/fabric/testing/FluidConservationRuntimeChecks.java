package thaumcraft.fabric.testing;

import dev.architectury.fluid.FluidStack;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidStorage;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariant;
import net.fabricmc.fabric.api.transfer.v1.transaction.Transaction;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.material.Fluids;
import thaumcraft.common.config.ThaumcraftAlchemy;
import thaumcraft.common.config.ThaumcraftSpa;
import thaumcraft.common.lib.fluid.GolemFluidAccess;
import thaumcraft.common.tiles.TileCrucible;
import thaumcraft.common.tiles.TileSpa;

/** Finite real registered tanks; all checks execute synchronously before producer ticks. */
public final class FluidConservationRuntimeChecks {
    private static final ResourceLocation WATER=ResourceLocation.withDefaultNamespace("water");
    private FluidConservationRuntimeChecks() {}

    public static void golemFractionalFill(GameTestHelper test) { fractional(test,true); }
    public static void golemFractionalDrain(GameTestHelper test) { fractional(test,false); }
    private static void fractional(GameTestHelper test,boolean fill) {
        var level=test.getLevel();var pos=test.absolutePos(new BlockPos(8,100,8));var before=level.getBlockState(pos);
        try {
            level.setBlock(pos,ThaumcraftSpa.ARCANE_SPA.get().defaultBlockState(),Block.UPDATE_CLIENTS);
            var spa=(TileSpa)level.getBlockEntity(pos);var handler=GolemFluidAccess.of(level,pos);
            long initial=fill?spa.getCapacityNative()-80:81000-1;
            test.assertTrue(spa.fill(null,FluidStack.create(Fluids.WATER,initial),true)==initial,"finite fractional native Spa fixture");
            int reported=fill?handler.fill(Direction.DOWN,WATER,1000,true):handler.drain(Direction.DOWN,WATER,1000,true);
            long delta=fill?spa.getFluidAmountNative()-initial:initial-spa.getFluidAmountNative();
            test.assertTrue(delta==reported*81L,"golem "+(fill?"fill":"drain")+" reported "+reported+"mB but changed real tank by "+delta+" droplets; must change exactly "+reported*81L);
            test.assertTrue(fill?reported==0&&spa.getFluidAmountNative()==initial:reported==999&&spa.getFluidAmountNative()==80,"unrepresentable fraction stays with its original owner");
        } finally {level.setBlock(pos,before,Block.UPDATE_CLIENTS);}
        test.succeed();
    }

    public static void crucibleAbort(GameTestHelper test) { crucibleAbort(test,true); }
    public static void crucibleDrainAbort(GameTestHelper test) { crucibleAbort(test,false); }
    private static void crucibleAbort(GameTestHelper test,boolean fill) {
        var level=test.getLevel();var pos=test.absolutePos(new BlockPos(8,100,8));var before=level.getBlockState(pos);
        try {
            level.setBlock(pos,ThaumcraftAlchemy.CRUCIBLE.get().defaultBlockState(),Block.UPDATE_CLIENTS);
            var tile=(TileCrucible)level.getBlockEntity(pos);var storage=FluidStorage.SIDED.find(level,pos,Direction.UP);var water=FluidVariant.of(Fluids.WATER);
            if(!fill)test.assertTrue(tile.fillWater(1000,true)==1000,"fund finite1000mB source");
            try(var tx=Transaction.openOuter()) {test.assertTrue((fill?storage.insert(water,81000,tx):storage.extract(water,81000,tx))==81000,"stage finite1000mB");}
            int expected=fill?0:1000;
            test.assertTrue(tile.getWaterAmount()==expected,"aborted Fabric "+(fill?"fill":"drain")+" must leave"+expected+"mB, actual="+tile.getWaterAmount());
        } finally {level.setBlock(pos,before,Block.UPDATE_CLIENTS);}
        test.succeed();
    }

    public static void boundaryAmounts(GameTestHelper test) {
        var level=test.getLevel();var pos=test.absolutePos(new BlockPos(8,100,8));var before=level.getBlockState(pos);
        try {
            for(boolean spaTank:new boolean[]{false,true}) {
                level.setBlock(pos,(spaTank?ThaumcraftSpa.ARCANE_SPA.get():ThaumcraftAlchemy.CRUCIBLE.get()).defaultBlockState(),Block.UPDATE_CLIENTS);
                var handler=GolemFluidAccess.of(level,pos);int capacity=spaTank?5000:1000;
                for(int amount=1;amount<=capacity;amount++) {
                    test.assertTrue(handler.fill(Direction.DOWN,WATER,amount,false)==amount&&handler.amount(Direction.DOWN)==0,"fill simulation unchanged at"+amount);
                    test.assertTrue(handler.fill(Direction.DOWN,WATER,amount,true)==amount&&handler.amount(Direction.DOWN)==amount,"exact accepted mB at"+amount);
                    test.assertTrue(handler.fill(Direction.DOWN,ResourceLocation.withDefaultNamespace("lava"),1,true)==0&&handler.amount(Direction.DOWN)==amount,"wrong-fluid rejection preserves contents");
                    test.assertTrue(handler.drain(Direction.DOWN,WATER,amount,false)==amount&&handler.amount(Direction.DOWN)==amount,"drain simulation unchanged");
                    test.assertTrue(handler.drain(Direction.DOWN,WATER,amount,true)==amount&&handler.amount(Direction.DOWN)==0,"exact fill/drain conservation at"+amount);
                }
                test.assertTrue(handler.fill(Direction.DOWN,WATER,capacity+1,true)==capacity,"overflow reports accepted, not requested");
                test.assertTrue(handler.fill(Direction.DOWN,WATER,1,true)==0&&handler.amount(Direction.DOWN)==capacity,"full-tank rejection retains source custody");
                handler.drain(Direction.DOWN,WATER,capacity,true);
                if(spaTank) {
                    var spa=(TileSpa)level.getBlockEntity(pos);
                    test.assertTrue(spa.fill(null,FluidStack.empty(),true)==0&&spa.drain(null,FluidStack.empty(),true).isEmpty(),"empty stacks mean nothing");
                    for(var face:Direction.values())test.assertTrue((FluidStorage.SIDED.find(level,pos,face)==null)==(face==Direction.UP),"Spa advertised top face absent only");
                    var source=FluidStack.create(Fluids.WATER,81);spa.fill(null,source,true);source.setAmount(0);
                    var copy=spa.fluid();copy.setAmount(0);test.assertTrue(spa.getFluidAmountNative()==81,"input and output stacks do not alias real tank");
                }
            }
        } finally {level.setBlock(pos,before,Block.UPDATE_CLIENTS);}
        test.succeed();
    }
}
