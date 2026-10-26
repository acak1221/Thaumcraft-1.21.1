package thaumcraft.common.testing;

import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.WorldGenRegion;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.entity.SpawnerBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.status.ChunkPyramid;
import net.minecraft.world.level.chunk.status.ChunkStatus;
import thaumcraft.common.lib.world.StructurePlacementRules;
import thaumcraft.common.lib.world.WorldGenMound;
import thaumcraft.common.lib.world.WorldGenMoundTemplate;
import thaumcraft.common.lib.world.WorldGenWriteArea;

/** Real generated chunks and blocks, with a deliberately restricted feature write envelope.
 * The terrain is a declared fixture; this is not a claim of natural mound discovery. */
public final class MoundBoundaryRuntimeChecks {
    private MoundBoundaryRuntimeChecks() {}

    public static void run(GameTestHelper test) {
        ServerLevel level = test.getLevel();
        // Two independent sites: four-chunk valid footprint, then a footprint reaching +2 chunks.
        check(test, level, 256, 256, 13, true);
        check(test, level, 320, 256, 15, false);
        test.succeed();
    }

    private static void check(GameTestHelper test, ServerLevel level, int bx, int bz, int local, boolean fits) {
        int x = bx + local, z = bz + local, base = 56;
        ChunkPos center = new ChunkPos(bx >> 4, bz >> 4);
        // Finish all chunks touched by the structure, including the forbidden +2 neighbour.
        for (int cx = center.x; cx <= (x + 18) >> 4; cx++)
            for (int cz = center.z; cz <= (z + 18) >> 4; cz++) level.getChunk(cx, cz);
        for (int dx = 0; dx < 19; dx++) for (int dz = 0; dz < 19; dz++) {
            for (int y = base - 1; y <= 64; y++) level.setBlock(new BlockPos(x + dx, y, z + dz), Blocks.STONE.defaultBlockState(), 2);
            for (int y = 65; y < level.getMaxBuildHeight(); y++) {
                BlockPos pos = new BlockPos(x + dx, y, z + dz);
                if (!level.isEmptyBlock(pos)) level.setBlock(pos, Blocks.AIR.defaultBlockState(), 2);
            }
        }
        int[] writes = {0};
        // Native region, native FEATURES step and native write guard. Only cache lookup is
        // adapted to already-generated chunks; no chunk can be loaded by the feature.
        WorldGenLevel bounded = new WorldGenRegion(level, null,
                ChunkPyramid.GENERATION_PYRAMID.getStepTo(ChunkStatus.FEATURES), level.getChunk(center.x, center.z)) {
            @Override public boolean hasChunk(int cx, int cz) {
                return level.getChunkSource().getChunkNow(cx, cz) != null;
            }
            @Override public ChunkAccess getChunk(int cx, int cz, ChunkStatus status, boolean load) {
                var chunk = level.getChunkSource().getChunkNow(cx, cz);
                if (chunk == null) throw new AssertionError("Feature attempted unloaded chunk " + cx + "," + cz);
                return chunk;
            }
            @Override public boolean setBlock(BlockPos pos, BlockState state, int flags, int depth) {
                test.assertTrue(WorldGenWriteArea.inside(center, 1, pos), "No write may escape feature radius");
                writes[0]++;
                return super.setBlock(pos, state, flags, depth);
            }
        };
        boolean built = new WorldGenMound().placeSelected(bounded, RandomSource.create(42),
                new StructurePlacementRules.Draw(StructurePlacementRules.Kind.MOUND, x, z));
        if (!fits) {
            test.assertTrue(!built, "Mound crossing +2 chunks must refuse atomically, not report a partial structure as success; writes=" + writes[0]);
            test.assertTrue(writes[0] == 0, "Rejected mound must not write terrain, loot, spawners or node");
            return;
        }
        test.assertTrue(built && writes[0] > 0, "Mound crossing four chunks must build");
        int inspected = 0;
        for (int layer = 0; layer < WorldGenMound.LAYERS; layer++)
            for (int dz = 0; dz < 19; dz++) for (int dx = 0; dx < 19; dx++) {
                var expected = WorldGenMoundTemplate.stateFor(WorldGenMoundTemplate.cell(layer, dz, dx));
                if (expected == null || special(dx, layer, dz)) continue;
                var pos = new BlockPos(x + dx, base + layer, z + dz);
                test.assertTrue(level.getBlockState(pos).is(expected.getBlock()), "Complete template at oracle surface-9: " + pos);
                inspected++;
            }
        BlockPos chestPos = new BlockPos(x + 10, base + 1, z + 9);
        test.assertTrue(level.getBlockEntity(chestPos) instanceof ChestBlockEntity, "Cross-boundary chest exists");
        ChestBlockEntity chest = (ChestBlockEntity) level.getBlockEntity(chestPos);
        test.assertTrue(!chest.isEmpty(), "Actual Forge dungeonChest descriptor produces items");
        test.assertTrue(level.getBlockEntity(new BlockPos(x + 9, base + 8, z + 9)) instanceof thaumcraft.common.tiles.TileNode,
                "Dark node exists at oracle surface-1 above mound centre");
        test.assertTrue(level.getBlockEntity(new BlockPos(x + 4, base + 5, z + 4)) instanceof SpawnerBlockEntity
                && level.getBlockEntity(new BlockPos(x + 4, base + 5, z + 14)) instanceof SpawnerBlockEntity, "Both spawners exist");
        test.assertTrue(level.getBlockState(new BlockPos(x + 9, base + 1, z + 7)).getBlock() instanceof thaumcraft.common.blocks.BlockLoot
                && level.getBlockState(new BlockPos(x + 9, base + 1, z + 11)).getBlock() instanceof thaumcraft.common.blocks.BlockLoot,
                "Both loot urn/crate blocks exist");
        var before = chest.saveWithFullMetadata(level.registryAccess());
        level.getChunk(chestPos);
        level.getChunk(chestPos);
        test.assertTrue(before.equals(chest.saveWithFullMetadata(level.registryAccess())), "Repeated native chunk requests do not refill chest");
        int priorWrites = writes[0];
        test.assertTrue(!new WorldGenMound().placeSelected(bounded, RandomSource.create(42),
                new StructurePlacementRules.Draw(StructurePlacementRules.Kind.MOUND, x, z)),
                "Immediate feature replay must not build another mound atop the first");
        test.assertTrue(writes[0] == priorWrites && before.equals(chest.saveWithFullMetadata(level.registryAccess())),
                "Immediate feature replay does not change blocks or refill existing chest");
        thaumcraft.common.Thaumcraft.LOG.info("G10 mound footprint local={} inspected {} template cells; chest/spawners verified", local, inspected);
    }

    private static boolean special(int x, int y, int z) {
        return (y == 1 && ((x == 9 && (z == 7 || z == 11)) || (x == 10 && z == 9)))
                || (y == 5 && x == 4 && (z == 4 || z == 14)) || (x == 9 && y == 8 && z == 9);
    }
}
