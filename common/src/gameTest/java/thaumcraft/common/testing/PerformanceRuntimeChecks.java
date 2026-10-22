package thaumcraft.common.testing;

import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.level.block.Blocks;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.common.Thaumcraft;
import thaumcraft.common.config.ThaumcraftEldritchBlocks;
import thaumcraft.common.config.ThaumcraftContent;
import thaumcraft.common.blocks.BlockMirror;
import thaumcraft.common.lib.events.EssentiaHandler;

/** Counts retained entries after real endpoint creation/search/removal, without a GC timing assumption. */
public final class PerformanceRuntimeChecks {
    private PerformanceRuntimeChecks() {}

    /** Bounded first-candidate subset of the suite's surface structure selector, not its full search. */
    public static void structureCandidateFootprint(GameTestHelper test) {
        var key = net.minecraft.resources.ResourceKey.create(net.minecraft.core.registries.Registries.DIMENSION,
                net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("thaumcraft_gametest", "natural"));
        var level = test.getLevel().getServer().getLevel(key);
        test.assertTrue(level != null, "real natural generation dimension exists");
        int initial = level.getChunkSource().getLoadedChunksCount();
        int forced = level.getForcedChunks().size();
        for (var kind : new thaumcraft.common.lib.world.StructurePlacementRules.Kind[] {
                thaumcraft.common.lib.world.StructurePlacementRules.Kind.ELDRITCH_RING,
                thaumcraft.common.lib.world.StructurePlacementRules.Kind.HILLTOP_STONES,
                thaumcraft.common.lib.world.StructurePlacementRules.Kind.MOUND}) {
            boolean found = false;
            search: for (int x = 0; x < 1024; x++) for (int z = 0; z < 128; z++) {
                var random = net.minecraft.util.RandomSource.create(thaumcraft.common.lib.world.StructurePlacementRules.chunkSeed(level.getSeed(), x, z));
                if (thaumcraft.common.lib.world.StructurePlacementRules.draw(random, x, z).kind() != kind) continue;
                int before = level.getChunkSource().getLoadedChunksCount();
                level.getChunk(x, z);
                int after = level.getChunkSource().getLoadedChunksCount();
                Thaumcraft.LOG.info("PERF structureCandidate kind={} x={} z={} holdersBefore={} holdersAfter={} delta={}", kind, x, z, before, after, after - before);
                found = true;
                break search;
            }
            test.assertTrue(found, "original selector has a candidate for " + kind);
        }
        Thaumcraft.LOG.info("PERF structureCandidates totalHolderDelta={} forcedBefore={} forcedAfter={}",
                level.getChunkSource().getLoadedChunksCount() - initial, forced, level.getForcedChunks().size());
        test.assertTrue(level.getForcedChunks().size() == forced, "candidate generation adds no forced-chunk entries");
        test.succeed();
    }

    public static void regenerationFootprint(GameTestHelper test) {
        var key = net.minecraft.resources.ResourceKey.create(net.minecraft.core.registries.Registries.DIMENSION,
                net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("thaumcraft_gametest", "natural"));
        var level = test.getLevel().getServer().getLevel(key);
        test.assertTrue(level != null, "real natural generation dimension exists");
        int before = level.getChunkSource().getLoadedChunksCount();
        int forced = level.getForcedChunks().size();
        try {
            var field = thaumcraft.common.config.ThaumcraftBiomeSettings.class.getDeclaredField("current");
            field.setAccessible(true);
            Object original = field.get(null);
            try {
                var settings = new java.util.Properties();
                settings.setProperty("cinnibar_ore", "true");
                thaumcraft.common.config.ThaumcraftBiomeSettings.configure(settings);
                long seed = thaumcraft.common.lib.events.ChunkRegeneration.chunkSeed(level.getSeed(), 12345, -12345);
                thaumcraft.common.lib.events.ChunkRegeneration.regenerate(level, 12345, -12345, seed);
                int first = level.getChunkSource().getLoadedChunksCount();
                thaumcraft.common.lib.events.ChunkRegeneration.regenerate(level, 12345, -12345, seed);
                int second = level.getChunkSource().getLoadedChunksCount();
                Thaumcraft.LOG.info("PERF regenerationFootprint holdersBefore={} first={} second={} firstDelta={} replayDelta={} forcedBefore={} forcedAfter={}",
                        before, first, second, first - before, second - first, forced, level.getForcedChunks().size());
                test.assertTrue(level.getForcedChunks().size() == forced, "regeneration does not retain forced chunks");
                test.assertTrue(second == first, "replaying the same ore regeneration does not expand chunk holders");
            } finally { field.set(null, original); }
        } catch (ReflectiveOperationException failure) { throw new AssertionError(failure); }
        test.succeed();
    }

    public static void mirrorCacheRetirement(GameTestHelper test) {
        var level = test.getLevel();
        BlockPos base = test.absolutePos(new BlockPos(1, 2, 1));
        EssentiaHandler.clear(level);
        int[] retained = new int[2];
        try {
            for (int mode = 0; mode < 2; mode++) {
                for (int i = 0; i < 64; i++) {
                    BlockPos pos = base.above(i * 2);
                    level.setBlock(pos, ThaumcraftEldritchBlocks.MIRROR.get().defaultBlockState().setValue(BlockMirror.ESSENTIA, true), 2);
                    if (mode == 1) level.setBlock(pos.above(), ThaumcraftContent.JAR.get().defaultBlockState(), 2);
                    var mirror = level.getBlockEntity(pos);
                    test.assertTrue(mirror != null, "real registered mirror endpoint exists");
                    EssentiaHandler.drainEssentiaForMirror(mirror, Aspect.AIR, Direction.NORTH, 1, true);
                    level.setBlock(pos, Blocks.AIR.defaultBlockState(), 2);
                    level.setBlock(pos.above(), Blocks.AIR.defaultBlockState(), 2);
                }
                retained[mode] = cacheSize(level, mode == 0 ? "mirrorRetryAfter" : "mirrorSources");
                EssentiaHandler.clear(level);
            }
            Thaumcraft.LOG.info("PERF mirrorCacheRetirement removalsPerMode=64 retryRetained={} sourcesRetained={}", retained[0], retained[1]);
            test.assertTrue(retained[0] == 0 && retained[1] == 0, "removed mirror coordinates must not remain cached: retry=" + retained[0] + " sources=" + retained[1]);
            test.succeed();
        } finally { EssentiaHandler.clear(level); }
    }

    private static int cacheSize(Object level, String name) {
        try {
            var levels = EssentiaHandler.class.getDeclaredField("LEVELS");
            levels.setAccessible(true);
            Object cache = ((Map<?, ?>) levels.get(null)).get(level);
            if (cache == null) return 0;
            var field = cache.getClass().getDeclaredField(name);
            field.setAccessible(true);
            return ((Map<?, ?>) field.get(cache)).size();
        } catch (ReflectiveOperationException failure) { throw new AssertionError(failure); }
    }
}
