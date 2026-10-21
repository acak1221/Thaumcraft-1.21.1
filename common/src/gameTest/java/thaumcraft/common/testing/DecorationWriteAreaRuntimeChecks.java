package thaumcraft.common.testing;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.Heightmap;
import thaumcraft.common.Thaumcraft;
import thaumcraft.common.config.ThaumcraftBiomes;
import thaumcraft.common.config.ThaumcraftNaturals;
import thaumcraft.common.lib.world.WorldGenWriteArea;

/**
 * The decoration of a fresh Magical Forest stays inside the chunks that the features step may write to.
 *
 * <p>Minecraft 1.21.1 lets the decoration of a chunk write one chunk around it. The 1.7.10 decorator drew its
 * trees eight to twenty-three blocks into the chunk and a Greatwood canopy is up to sixteen blocks wide, so
 * a tree near the east or south edge reached into a chunk the step may not write to. The feature asked
 * {@code ensureCanWrite} about every block of the canopy, and vanilla logs an error each time that answer is
 * no, then the port dropped the whole tree. This generates real fresh forest chunks of the test-only natural
 * dimension, reads the log, and counts the trees that were refused.
 */
public final class DecorationWriteAreaRuntimeChecks {
    private static final String FAR_CHUNK = "Detected setBlock in a far chunk";
    /** Chunks on every side of a forest core: 7 x 7 = 49 chunks per core. */
    private static final int RADIUS = 3;
    private static final int CORES = 4;

    private DecorationWriteAreaRuntimeChecks() {}

    public static void run(GameTestHelper test) {
        var key = ResourceKey.create(Registries.DIMENSION, ResourceLocation.fromNamespaceAndPath("thaumcraft_gametest", "natural"));
        ServerLevel level = test.getLevel().getServer().getLevel(key);
        test.assertTrue(level != null, "existing test-only natural noise dimension is present");
        var generator = level.getChunkSource().getGenerator();
        var sampler = level.getChunkSource().randomState().sampler();
        List<BlockPos> cores = new ArrayList<>();
        // Pure native source/base-height queries only: no chunk generation until a whole fresh window is found.
        search:for (int x = 20480; x <= 28672 && cores.size() < CORES; x += 128) {
            candidate:for (int z = 20480; z <= 28672; z += 128) {
                for (BlockPos core : cores) if (Math.abs(core.getX() - x) < 16 * (2 * RADIUS + 2) && Math.abs(core.getZ() - z) < 16 * (2 * RADIUS + 2)) continue candidate;
                if (!generator.getBiomeSource().getNoiseBiome(x >> 2, 20, z >> 2, sampler).is(ThaumcraftBiomes.MAGICAL_FOREST)) continue;
                for (int dx : new int[] {-48, 0, 48}) for (int dz : new int[] {-48, 0, 48}) {
                    if (!generator.getBiomeSource().getNoiseBiome((x + dx) >> 2, 20, (z + dz) >> 2, sampler).is(ThaumcraftBiomes.MAGICAL_FOREST)) continue candidate;
                }
                for (int cx = (x >> 4) - RADIUS; cx <= (x >> 4) + RADIUS; cx++) for (int cz = (z >> 4) - RADIUS; cz <= (z >> 4) + RADIUS; cz++) {
                    if (level.getChunkSource().getChunkNow(cx, cz) != null) continue candidate;
                }
                int height = generator.getBaseHeight(x, z, Heightmap.Types.WORLD_SURFACE_WG, level, level.getChunkSource().randomState());
                if (height < 65 || height > 105) continue;
                cores.add(new BlockPos(x, height, z));
                if (cores.size() == CORES) break search;
            }
        }
        test.assertTrue(cores.size() == CORES, "the native biome lookup finds " + CORES + " fresh Magical Forest cores, found " + cores.size());
        // One forest window per tick: a whole window is a few seconds of generation and the server watchdog counts a tick.
        generate(test, level, cores, 0, new VanillaLogCapture(), WorldGenWriteArea.treesRefused());
    }

    private static void generate(GameTestHelper test, ServerLevel level, List<BlockPos> cores, int index, VanillaLogCapture log, long refusedBefore) {
        try {
            if (index < cores.size()) {
                BlockPos core = cores.get(index);
                int minX = (core.getX() >> 4) - RADIUS, minZ = (core.getZ() >> 4) - RADIUS;
                for (int cx = minX; cx <= minX + 2 * RADIUS; cx++) for (int cz = minZ; cz <= minZ + 2 * RADIUS; cz++) level.getChunk(cx, cz);
                test.runAfterDelay(1, () -> generate(test, level, cores, index + 1, log, refusedBefore));
                return;
            }
            int logs = 0;
            for (BlockPos core : cores) {
                int minX = (core.getX() >> 4) - RADIUS, minZ = (core.getZ() >> 4) - RADIUS;
                for (int cx = minX; cx <= minX + 2 * RADIUS; cx++) for (int cz = minZ; cz <= minZ + 2 * RADIUS; cz++) {
                    var chunk = level.getChunk(cx, cz);
                    for (int lx = 0; lx < 16; lx++) for (int lz = 0; lz < 16; lz++) for (int y = 50; y < 160; y++) {
                        var state = chunk.getBlockState(new BlockPos(cx * 16 + lx, y, cz * 16 + lz));
                        if (state.is(ThaumcraftNaturals.GREATWOOD_LOG.get()) || state.is(ThaumcraftNaturals.SILVERWOOD_LOG.get()) || state.is(Blocks.OAK_LOG)) logs++;
                    }
                }
            }
            long refused = WorldGenWriteArea.treesRefused() - refusedBefore;
            Thaumcraft.LOG.info("TC4 decoration write area: {} forest chunks, {} tree logs, {} trees refused, far-chunk log lines: {}",
                    cores.size() * (2 * RADIUS + 1) * (2 * RADIUS + 1), logs, refused, log.containing(FAR_CHUNK).size());
            test.assertTrue(logs > 0, "the generated forest windows contain real magical and oak trees");
            test.assertTrue(log.containing(FAR_CHUNK).isEmpty(),
                    "decorating the forest asks the write area silently and never triggers the vanilla far chunk error, " + log.sample(FAR_CHUNK));
            test.assertTrue(refused == 0, "no magical tree of the forest was refused for reaching outside the write area, refused: " + refused);
            log.close();
            test.succeed();
        } catch (RuntimeException failure) {
            log.close();
            throw failure;
        }
    }
}
