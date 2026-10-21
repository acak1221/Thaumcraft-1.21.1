package thaumcraft.common.testing;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.SpawnerBlockEntity;
import net.minecraft.world.level.levelgen.Heightmap;
import thaumcraft.common.config.ThaumcraftNodes;
import thaumcraft.common.config.ThaumcraftOuterLands;
import thaumcraft.common.config.ThaumcraftVillages;
import thaumcraft.common.lib.world.StructurePlacementRules;
import thaumcraft.common.lib.world.VillageWorldGenRules;

/**
 * Checks that the TC4 surface structures really reach a generated overworld chunk.
 *
 * <p>Oracle {@code ThaumcraftWorldGenerator.generateSurface:403-457} draws one structure
 * per chunk from an exclusive chain (mound, eldritch ring, hilltop stones) and the village
 * components are pieces of a village. 1.21.1 has no world generator, so the structures
 * became features and the loader modules wire them into the overworld biomes (Fabric:
 * the biome modification API, NeoForge: its own biome modifiers). This check generates
 * fresh normal-noise chunks and observes the structures the original chain selected,
 * without inserting a single block.
 *
 * <p>The two village components are wired the same way, but their feature is not reached
 * by the chunk generation of the game test server; that is an open defect of the village
 * unit and is not asserted here.
 */
public final class SurfaceStructurePlacementRuntimeChecks {
    /** The overworld biomes whose surface the original accepts for the hilltop stones. */
    private static final java.util.Set<String> STONY_HILLTOPS = java.util.Set.of(
            "windswept_hills", "windswept_forest", "jagged_peaks", "stony_peaks", "snowy_slopes");

    private SurfaceStructurePlacementRuntimeChecks() {}

    public static void run(GameTestHelper test) {
        var key = ResourceKey.create(Registries.DIMENSION,
                ResourceLocation.fromNamespaceAndPath("thaumcraft_gametest", "natural"));
        ServerLevel level = test.getLevel().getServer().getLevel(key);
        test.assertTrue(level != null, "test-only normal terrain dimension is loaded");
        var registries = level.registryAccess();
        var generator = level.getChunkSource().getGenerator();
        long seed = level.getSeed();

        // The loader wiring must have put the placed features into the generation
        // settings of the overworld biomes, otherwise no chunk ever attempts them.
        var settings = generator.getBiomeGenerationSettings(
                registries.registryOrThrow(Registries.BIOME).getHolderOrThrow(Biomes.PLAINS));
        var placed = registries.registryOrThrow(Registries.PLACED_FEATURE);
        for (var feature : List.of(ThaumcraftOuterLands.RING_PLACED, ThaumcraftVillages.HILLTOP_STONES_KEY,
                ThaumcraftVillages.BANKER_HOMES_KEY, ThaumcraftVillages.WIZARD_TOWERS_KEY, ThaumcraftNodes.NODE_KEY)) {
            var holder = placed.getHolderOrThrow(feature);
            test.assertTrue(settings.hasFeature(holder.value()),
                    "the overworld biome carries the placed feature " + feature.location());
        }
        var featureType = registries.registryOrThrow(Registries.FEATURE);
        var bankerKey = ResourceKey.create(Registries.FEATURE,
                ResourceLocation.fromNamespaceAndPath("thaumcraft", "village_banker_homes"));
        var bankerConfigured = placed.getHolderOrThrow(ThaumcraftVillages.BANKER_HOMES_KEY).value().feature().value();
        test.assertTrue(featureType.containsKey(bankerKey), "the banker home feature type is registered");
        test.assertTrue(bankerConfigured.feature() == featureType.get(bankerKey),
                "the configured feature is the registered feature instance");

        eldritchRing(test, level, seed);
        hilltopStones(test, level, seed);
        test.succeed();
    }

    /** The chunk the exclusive chain gives to the eldritch ring carries the ring. */
    private static void eldritchRing(GameTestHelper test, ServerLevel level, long seed) {
        Block eldritch = ThaumcraftOuterLands.eldritch();
        test.assertTrue(eldritch != null, "the eldritch block of the ring is registered");
        List<int[]> candidates = chainChunks(seed, StructurePlacementRules.Kind.ELDRITCH_RING, 12);
        test.assertTrue(!candidates.isEmpty(), "the exclusive chain gives chunks to the eldritch ring");
        for (int[] candidate : candidates) {
            level.getChunk(candidate[0], candidate[1]);
            if (contains(level, candidate[0], candidate[1], eldritch)) {
                test.assertTrue(true, "the chunk the chain gave to the eldritch ring carries the ring");
                return;
            }
        }
        test.assertTrue(false, "no chunk the chain gave to the eldritch ring carries the ring");
    }

    /** The chunk the exclusive chain gives to the hilltop stones carries the cluster. */
    private static void hilltopStones(GameTestHelper test, ServerLevel level, long seed) {
        var generator = level.getChunkSource().getGenerator();
        var randomState = level.getChunkSource().randomState();
        var sampler = randomState.sampler();
        // The original refuses the cluster below y 85 and only on stone, grass or
        // dirt, so a stony hilltop region is needed.
        int regionX = 0;
        int regionZ = 0;
        boolean found = false;
        for (int x = 4096; x <= 12288 && !found; x += 32) {
            for (int z = 4096; z <= 12288 && !found; z += 32) {
                if (generator.getBaseHeight(x, z, Heightmap.Types.WORLD_SURFACE_WG, level, randomState) < 100) continue;
                String biome = generator.getBiomeSource().getNoiseBiome(x, 100, z, sampler)
                        .unwrapKey().map(k -> k.location().getPath()).orElse("");
                if (!STONY_HILLTOPS.contains(biome)) continue;
                regionX = x;
                regionZ = z;
                found = true;
            }
        }
        test.assertTrue(found, "a native stony hilltop region of the overworld noise exists");
        int baseChunkX = regionX >> 4;
        int baseChunkZ = regionZ >> 4;
        List<int[]> candidates = new ArrayList<>();
        for (int cx = baseChunkX - 24; cx <= baseChunkX + 24 && candidates.size() < 40; cx++) {
            for (int cz = baseChunkZ - 24; cz <= baseChunkZ + 24 && candidates.size() < 40; cz++) {
                var random = RandomSource.create(StructurePlacementRules.chunkSeed(seed, cx, cz));
                var draw = StructurePlacementRules.draw(random, cx, cz);
                if (draw.kind() != StructurePlacementRules.Kind.HILLTOP_STONES) continue;
                // The original refuses the cluster below y 85.
                int centreX = inside(draw.x(), cx);
                int centreZ = inside(draw.z(), cz);
                int base = generator.getBaseHeight(centreX, centreZ, Heightmap.Types.WORLD_SURFACE_WG, level, randomState);
                if (base < 95) continue;
                candidates.add(new int[] {cx, cz});
            }
        }
        test.assertTrue(!candidates.isEmpty(), "the exclusive chain gives a hilltop chunk to the hilltop stones");
        for (int[] candidate : candidates) {
            level.getChunk(candidate[0], candidate[1]);
            if (wispSpawner(level, candidate[0], candidate[1])) {
                test.assertTrue(true, "the chunk the chain gave to the hilltop stones carries the cluster");
                return;
            }
        }
        test.assertTrue(false, "no chunk the chain gave to the hilltop stones carries the cluster");
    }

    /** The centre of the cluster inside its chunk, the native rule of the feature. */
    private static int inside(int coordinate, int chunk) {
        int base = chunk << 4;
        int low = base + VillageWorldGenRules.HILLTOP_STONE_RADIUS;
        int high = base + 15 - VillageWorldGenRules.HILLTOP_STONE_RADIUS;
        return Math.min(Math.max(coordinate, low), high);
    }

    /** Whether the chunk carries the wisp spawner of the original hilltop cluster. */
    private static boolean wispSpawner(ServerLevel level, int chunkX, int chunkZ) {
        for (int x = 0; x < 16; x++) {
            for (int z = 0; z < 16; z++) {
                int worldX = chunkX * 16 + x;
                int worldZ = chunkZ * 16 + z;
                int surface = level.getHeight(Heightmap.Types.WORLD_SURFACE, worldX, worldZ);
                for (int y = surface - 4; y <= surface + 10; y++) {
                    if (!(level.getBlockEntity(new BlockPos(worldX, y, worldZ)) instanceof SpawnerBlockEntity spawner)) continue;
                    CompoundTag data = spawner.getSpawner().save(new CompoundTag());
                    if (data.getCompound("SpawnData").getCompound("entity").getString("id").equals("thaumcraft:wisp")) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    /** The chunks of a region the exclusive chain gives to one structure. */
    private static List<int[]> chainChunks(long seed, StructurePlacementRules.Kind kind, int limit) {
        List<int[]> chunks = new ArrayList<>();
        for (int chunkX = 0; chunkX < 1024 && chunks.size() < limit; chunkX++) {
            for (int chunkZ = 0; chunkZ < 128 && chunks.size() < limit; chunkZ++) {
                var random = RandomSource.create(StructurePlacementRules.chunkSeed(seed, chunkX, chunkZ));
                if (StructurePlacementRules.draw(random, chunkX, chunkZ).kind() == kind) {
                    chunks.add(new int[] {chunkX, chunkZ});
                }
            }
        }
        return chunks;
    }

    /** Whether the chunk carries a block of the given type. */
    private static boolean contains(ServerLevel level, int chunkX, int chunkZ, Block block) {
        for (int x = 0; x < 16; x++) {
            for (int z = 0; z < 16; z++) {
                int worldX = chunkX * 16 + x;
                int worldZ = chunkZ * 16 + z;
                int surface = level.getHeight(Heightmap.Types.WORLD_SURFACE, worldX, worldZ);
                for (int y = surface - 12; y <= surface + 8; y++) {
                    if (level.getBlockState(new BlockPos(worldX, y, worldZ)).is(block)) return true;
                }
            }
        }
        return false;
    }
}
