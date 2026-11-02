package thaumcraft.fabric.testing;

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
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.material.Fluids;
import thaumcraft.common.config.ThaumcraftAlchemy;
import thaumcraft.common.tiles.TileCrucible;

/** The registered Fabric endpoint must conserve the requested full fluid identity. */
public final class LoaderFluidReviewRuntimeChecks {
    private LoaderFluidReviewRuntimeChecks() {}

    public static void crucibleTaggedExtraction(GameTestHelper test) { taggedVariant(test, false); }
    public static void crucibleTaggedInsertion(GameTestHelper test) { taggedVariant(test, true); }

    private static void taggedVariant(GameTestHelper test, boolean insert) {
        var level = test.getLevel();
        var pos = test.absolutePos(new BlockPos(8, 100, 8));
        var before = level.getBlockState(pos);
        test.assertTrue(level.getBlockEntity(pos) == null, "the isolated fluid fixture has no foreign block entity");
        try {
            level.setBlock(pos, ThaumcraftAlchemy.CRUCIBLE.get().defaultBlockState(), Block.UPDATE_CLIENTS);
            var tile = (TileCrucible) level.getBlockEntity(pos);
            var storage = FluidStorage.SIDED.find(level, pos, Direction.UP);
            test.assertTrue(storage != null, "the actual registered crucible exposes its fluid endpoint");
            var water = FluidVariant.of(Fluids.WATER);
            var tag = new CompoundTag();
            tag.putString("fluid_identity", "tagged-water-review");
            var patch = DataComponentPatch.builder().set(DataComponents.CUSTOM_DATA, CustomData.of(tag)).build();
            var tagged = FluidVariant.of(Fluids.WATER, patch);
            long bucket = FluidConstants.BUCKET;
            if (!insert) test.assertTrue(tile.fillWater(1000, true) == 1000, "fund one finite plain-water bucket");
            try (var tx = Transaction.openOuter()) {
                long changed = insert ? storage.insert(tagged, bucket, tx) : storage.extract(tagged, bucket, tx);
                test.assertTrue(changed == 0, insert
                        ? "a plain-water-only tank cannot accept and discard a different fluid component patch"
                        : "a request for tagged water cannot extract stock advertised as plain water");
                test.assertTrue(tile.getWaterAmount() == (insert ? 0 : 1000), "refused variant leaves real stock unchanged");
                long ordinary = insert ? storage.insert(water, bucket, tx) : storage.extract(water, bucket, tx);
                test.assertTrue(ordinary == bucket, "the same native endpoint still transfers ordinary water");
                tx.commit();
            }
            test.assertTrue(tile.getWaterAmount() == (insert ? 1000 : 0), "native commit publishes exactly the plain bucket");
            try (var tx = Transaction.openOuter()) {
                long ordinary = insert ? storage.extract(water, bucket, tx) : storage.insert(water, bucket, tx);
                test.assertTrue(ordinary == bucket, "the inverse ordinary operation can be staged");
            }
            test.assertTrue(tile.getWaterAmount() == (insert ? 1000 : 0), "aborted inverse transfer retains committed stock");
        } finally {
            level.setBlock(pos, before, Block.UPDATE_CLIENTS);
        }
        test.succeed();
    }
}
