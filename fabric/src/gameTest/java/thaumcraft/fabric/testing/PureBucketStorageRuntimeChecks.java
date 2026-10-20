package thaumcraft.fabric.testing;

import net.fabricmc.fabric.api.transfer.v1.context.ContainerItemContext;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidConstants;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidStorage;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariant;
import net.fabricmc.fabric.api.transfer.v1.item.InventoryStorage;
import net.fabricmc.fabric.api.transfer.v1.transaction.Transaction;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import thaumcraft.common.config.ThaumcraftSpa;
import thaumcraft.common.tiles.TileSpa;

/** Real native bucket API and real Spa participate in the same abort/commit.
 * This new transfer API preserves native item components by Fabric convention;
 * original world/Spa whole-container actions separately keep plain templates. */
public final class PureBucketStorageRuntimeChecks {
    private PureBucketStorageRuntimeChecks(){}
    public static void run(GameTestHelper test){
        var level=test.getLevel();var pos=test.absolutePos(new BlockPos(8,100,8));var before=level.getBlockState(pos);
        test.assertTrue(level.getBlockEntity(pos)==null,"isolated native Pure container transfer site owns no previous block entity");
        try{
            level.setBlock(pos,ThaumcraftSpa.ARCANE_SPA.get().defaultBlockState(),Block.UPDATE_CLIENTS);
            var spa=(TileSpa)level.getBlockEntity(pos);var tank=FluidStorage.SIDED.find(level,pos,Direction.DOWN);
            var inventory=new SimpleContainer(1);inventory.setItem(0,new ItemStack(ThaumcraftSpa.BUCKET_PURE.get()));
            var slot=InventoryStorage.of(inventory,null).getSlot(0);var context=ContainerItemContext.ofSingleSlot(slot);
            var pure=FluidVariant.of(ThaumcraftSpa.PURE_FLUID_SOURCE.get());long bucket=FluidConstants.BUCKET;
            var filled=context.find(FluidStorage.ITEM);
            test.assertTrue(tank!=null&&filled!=null,"registered bidirectional Pure bucket supplies actual native item fluid storage alongside real Spa storage");
            try(var outer=Transaction.openOuter()){
                test.assertTrue(filled.extract(pure,bucket-1,outer)==0&&filled.extract(FluidVariant.of(net.minecraft.world.level.material.Fluids.WATER),bucket,outer)==0,
                        "native full container refuses partial or wrong-fluid extraction");
                test.assertTrue(filled.extract(pure,bucket,outer)==bucket&&tank.insert(pure,bucket,outer)==bucket
                        &&inventory.getItem(0).is(Items.BUCKET)&&spa.getFluidAmountNative()==bucket,
                        "actual native outer transaction stages one whole Pure bucket into real Spa and one empty container");
            }
            test.assertTrue(inventory.getItem(0).is(ThaumcraftSpa.BUCKET_PURE.get())&&spa.fluid().isEmpty(),"real native abort restores filled item and empty tank together");
            try(var outer=Transaction.openOuter()){
                filled=context.find(FluidStorage.ITEM);test.assertTrue(filled!=null&&filled.extract(pure,bucket,outer)==bucket&&tank.insert(pure,bucket,outer)==bucket,"real committed transfer owns one whole bucket on both participants");outer.commit();
            }
            test.assertTrue(inventory.getItem(0).is(Items.BUCKET)&&spa.getFluidAmountNative()==bucket,"native commit retains one actual empty and1000mB Pure in Spa");
            for(boolean commit:new boolean[]{false,true}){
                var empty=context.find(FluidStorage.ITEM);test.assertTrue(empty!=null,"native empty bucket API can resolve registered Pure source mapping");
                try(var outer=Transaction.openOuter()){
                    var normalized=FluidVariant.of(ThaumcraftSpa.PURE_FLUID_FLOWING.get());
                    test.assertTrue(normalized.equals(pure)&&normalized.getFluid()==ThaumcraftSpa.PURE_FLUID_SOURCE.get()
                                    &&empty.insert(normalized,bucket-1,outer)==0,
                            "actual native FluidVariant normalizes flowing to source and still refuses a partial physical container");
                    test.assertTrue(tank.extract(pure,bucket,outer)==bucket&&empty.insert(normalized,bucket,outer)==bucket,"native reverse transaction moves the same owned tank unit through its normalized Pure variant into one filled item");
                    if(commit)outer.commit();
                }
                test.assertTrue(inventory.getItem(0).is(commit?ThaumcraftSpa.BUCKET_PURE.get():Items.BUCKET)
                        &&spa.getFluidAmountNative()==(commit?0:bucket),"native reverse "+(commit?"commit":"abort")+" preserves one physical bucket across actual item/tank ownership");
            }
            test.succeed();
        }finally{level.setBlock(pos,before,Block.UPDATE_CLIENTS);}
    }
}
