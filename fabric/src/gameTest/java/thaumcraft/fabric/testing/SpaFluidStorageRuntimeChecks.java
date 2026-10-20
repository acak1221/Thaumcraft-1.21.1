package thaumcraft.fabric.testing;

import dev.architectury.fluid.FluidStack;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidConstants;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidStorage;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariant;
import net.fabricmc.fabric.api.transfer.v1.transaction.Transaction;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponentPatch;
import net.minecraft.core.component.DataComponents;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.material.Fluids;
import thaumcraft.common.config.ThaumcraftSpa;
import thaumcraft.common.tiles.TileSpa;

/** Actual Fabric transactions, native save/load and a replaced block owner; no fluid-policy substitute. */
public final class SpaFluidStorageRuntimeChecks {
    private SpaFluidStorageRuntimeChecks() {}
    public static void run(GameTestHelper test) {
        var level=test.getLevel();var pos=test.absolutePos(new BlockPos(8,100,8));
        var before=level.getBlockState(pos);var above=level.getBlockState(pos.above());
        var destinationPos=pos.east(2);var destinationBefore=level.getBlockState(destinationPos);
        test.assertTrue(level.getBlockEntity(pos)==null&&level.getBlockEntity(destinationPos)==null,"declared isolated source and destination sites contain no preexisting block entity custody");
        try {
            level.setBlock(pos,ThaumcraftSpa.ARCANE_SPA.get().defaultBlockState(),Block.UPDATE_CLIENTS);
            level.setBlock(pos.above(),Blocks.AIR.defaultBlockState(),Block.UPDATE_CLIENTS);
            test.assertTrue(level.getBlockEntity(pos) instanceof TileSpa,"actual registered Spa block creates its native tank owner");
            var tile=(TileSpa)level.getBlockEntity(pos);var storage=FluidStorage.SIDED.find(level,pos,Direction.DOWN);
            long bucket=FluidConstants.BUCKET;
            test.assertTrue(storage!=null&&FluidStack.bucketAmount()==bucket&&tile.getCapacityNative()==5*bucket,
                    "actual Fabric API exposes five native buckets with no millibucket/droplet confusion");
            test.assertTrue(FluidStorage.SIDED.find(level,pos,Direction.UP)==null
                    &&FluidStorage.SIDED.find(level,pos,Direction.EAST)==storage,
                    "native top capability is absent and allowed sides share one transaction participant");
            var water=FluidVariant.of(Fluids.WATER);var lava=FluidVariant.of(Fluids.LAVA);
            level.setBlock(destinationPos,ThaumcraftSpa.ARCANE_SPA.get().defaultBlockState(),Block.UPDATE_CLIENTS);
            var destinationTile=(TileSpa)level.getBlockEntity(destinationPos);
            var destinationStorage=FluidStorage.SIDED.find(level,destinationPos,Direction.DOWN);
            test.assertTrue(destinationStorage!=null,"second real Spa exposes its independent native transfer participant");
            tile.setItem(0,new ItemStack(ThaumcraftSpa.BATH_SALTS.get()));
            long revision=tile.fluidRevision();
            try(var outer=Transaction.openOuter()) {
                test.assertTrue(storage.insert(water,4*bucket,outer)==4*bucket,"genuine outer transaction stages four buckets");
                try(var nested=outer.openNested()) {
                    test.assertTrue(storage.extract(water,bucket,nested)==bucket&&storage.insert(lava,bucket,nested)==0,
                            "nested actual extraction retains remaining fluid identity and refuses mixing");
                }
                test.assertTrue(tile.getFluidAmountNative()==4*bucket,"native nested abort restores its exact savepoint");
                try(var nested=outer.openNested()) {
                    test.assertTrue(storage.extract(water,2*bucket,nested)==2*bucket,"second nested extraction stages two buckets");
                    nested.commit();
                }
                test.assertTrue(tile.getFluidAmountNative()==2*bucket,"nested commit joins the actual outer transaction");
                var saved=tile.saveWithoutMetadata(level.registryAccess());
                var cold=new TileSpa(pos,level.getBlockState(pos));cold.loadWithComponents(saved,level.registryAccess());
                test.assertTrue(cold.fluid().isEmpty(),"native save during an open transaction contains only committed tank custody");
                TileSpa.tick(level,pos,level.getBlockState(pos),tile);
                test.assertTrue(level.isEmptyBlock(pos.above())&&tile.getItem(0).isEmpty()
                                &&tile.saveWithoutMetadata(level.registryAccess()).getInt("SaltCount")==1,
                        "otherwise-funded native mix producer cannot spend tentative fluid or its actual salt");
            }
            test.assertTrue(tile.fluid().isEmpty()&&tile.fluidRevision()==revision&&tile.getItem(0).getCount()==1,
                    "actual outer abort restores initial fluid and revision after nested commit");
            test.assertTrue(tile.removeItemNoUpdate(0).is(ThaumcraftSpa.BATH_SALTS.get())&&tile.getItem(0).isEmpty(),
                    "the declared salt input remains owned and is recovered before the independent tank cases");
            try(var funding=Transaction.openOuter()) {
                test.assertTrue(storage.insert(water,bucket,funding)==bucket,"declared transfer regression source owns one committed bucket");funding.commit();
            }
            for(boolean commit:new boolean[]{false,true}) {
                try(var outer=Transaction.openOuter()) {
                    test.assertTrue(storage.extract(water,bucket,outer)==bucket&&destinationStorage.insert(water,bucket,outer)==bucket,
                            "actual source and destination stage the same one-bucket transfer");
                    var committed=tile.saveWithoutMetadata(level.registryAccess());
                    boolean refused=false;
                    try{tile.loadWithComponents(committed,level.registryAccess());}
                    catch(IllegalStateException expected){refused="Cannot reload a live spa during a fluid transaction".equals(expected.getMessage());}
                    test.assertTrue(refused&&tile.getFluidAmountNative()==0&&destinationTile.getFluidAmountNative()==bucket,
                            "same-owner native reload explicitly refuses before restoring saved committed stock or invalidating its live lease");
                    if(commit)outer.commit();
                }
                test.assertTrue(tile.getFluidAmountNative()==(commit?0:bucket)&&destinationTile.getFluidAmountNative()==(commit?bucket:0),
                        "native "+(commit?"commit":"abort")+" after refused reload preserves exactly one bucket across both real tank owners");
            }
            try(var recovery=Transaction.openOuter()) {
                test.assertTrue(destinationStorage.extract(water,bucket,recovery)==bucket,"recover the sole actual destination bucket before independent capacity cases");recovery.commit();
            }
            try(var outer=Transaction.openOuter()) {
                test.assertTrue(storage.insert(water,6*bucket,outer)==5*bucket,"real capacity admits five of six buckets exactly");
                outer.commit();
            }
            test.assertTrue(tile.getFluidAmountNative()==5*bucket,"native final commit publishes five buckets");
            var tag=new CompoundTag();tag.putString("original_identity","different-fluid-tag");
            var patch=DataComponentPatch.builder().set(DataComponents.CUSTOM_DATA,CustomData.of(tag)).build();
            var tagged=FluidVariant.of(Fluids.WATER,patch);
            try(var outer=Transaction.openOuter()) {
                test.assertTrue(storage.extract(tagged,bucket,outer)==0&&storage.extract(water,5*bucket,outer)==5*bucket,
                        "actual extraction distinguishes full patches as original Forge fluid NBT did");
                test.assertTrue(storage.insert(tagged,bucket,outer)==bucket,"empty native tank can accept a tagged fluid variant");
                outer.commit();
            }
            test.assertTrue(tile.fluid().getPatch().equals(patch),"committed tank retains the exact native fluid component patch");
            try(var outer=Transaction.openOuter()) {
                test.assertTrue(storage.extract(tagged,bucket/2,outer)==bucket/2,"actual retired-owner fixture stages a half bucket");
                level.setBlock(pos,Blocks.AIR.defaultBlockState(),Block.UPDATE_ALL);
                level.setBlock(pos,ThaumcraftSpa.ARCANE_SPA.get().defaultBlockState(),Block.UPDATE_CLIENTS);
                var replacement=(TileSpa)level.getBlockEntity(pos);
                test.assertTrue(replacement!=tile&&storage.insert(water,bucket,outer)==0&&storage.extract(tagged,bucket,outer)==0,
                        "stale Fabric capability refuses both directions after a real block replacement");
            }
            test.assertTrue(((TileSpa)level.getBlockEntity(pos)).fluid().isEmpty()&&!tile.isLiveOwner(),
                    "aborted stale participant cannot restore old fluid into the replacement owner");
            test.succeed();
        } finally {
            level.setBlock(pos,before,Block.UPDATE_CLIENTS);level.setBlock(pos.above(),above,Block.UPDATE_CLIENTS);
            level.setBlock(destinationPos,destinationBefore,Block.UPDATE_CLIENTS);
        }
    }
}
