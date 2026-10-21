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
 * <p>The two village components are wired the same way and are checked by
 * {@link VillageComponentPlacementRuntimeChecks}.
 */
public final class SurfaceStructurePlacementRuntimeChecks {
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
                ThaumcraftVillages.BANKER_HOMES_KEY, ThaumcraftVillages.WIZARD_TOWERS_KEY, ThaumcraftNodes.NODE_KEY,
                ThaumcraftNodes.TOTEM_KEY)) {
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

    /**
     * The chunk the exclusive chain gives to the eldritch ring carries the ring.
     *
     * <p>Oracle {@code ThaumcraftWorldGenerator:446} also hangs a dark aura node
     * above the altar of the ring, two blocks above the base of the ring.
     */
    private static void eldritchRing(GameTestHelper test, ServerLevel level, long seed) {
        Block eldritch = ThaumcraftOuterLands.eldritch();
        test.assertTrue(eldritch != null, "the eldritch block of the ring is registered");
        List<int[]> candidates = chainChunks(seed, StructurePlacementRules.Kind.ELDRITCH_RING, 12);
        test.assertTrue(!candidates.isEmpty(), "the exclusive chain gives chunks to the eldritch ring");
        for (int[] candidate : candidates) {
            level.getChunk(candidate[0], candidate[1]);
            if (contains(level, candidate[0], candidate[1], eldritch)) {
                test.assertTrue(true, "the chunk the chain gave to the eldritch ring carries the ring");
                test.assertTrue(ringHasItsAuraNode(level, candidate[0], candidate[1]),
                        "the ring carries the dark aura node of the original above its altar");
                return;
            }
        }
        test.assertTrue(false, "no chunk the chain gave to the eldritch ring carries the ring");
    }

    /**
     * The dark aura node of the original above the altar of the ring, two blocks
     * above the base of the ring (oracle ThaumcraftWorldGenerator:446).
     *
     * <p>The column is scanned instead of a fixed height: the ring itself raises
     * the height map of its column, so the height after the generation is not the
     * height of the terrain the ring grew on.
     */
    private static boolean ringHasItsAuraNode(ServerLevel level, int chunkX, int chunkZ) {
        var random = RandomSource.create(StructurePlacementRules.chunkSeed(level.getSeed(), chunkX, chunkZ));
        var draw = StructurePlacementRules.draw(random, chunkX, chunkZ);
        int centreX = inside(draw.x(), chunkX);
        int centreZ = inside(draw.z(), chunkZ);
        return columnHasAuraNode(level, centreX, centreZ);
    }

    /** Whether the column of a surface structure carries the aura node of the original. */
    private static boolean columnHasAuraNode(ServerLevel level, int x, int z) {
        Block node = thaumcraft.common.config.ThaumcraftContent.AURA_NODE.get();
        for (int y = level.getMinBuildHeight(); y < level.getMaxBuildHeight(); ++y) {
            if (level.getBlockState(new BlockPos(x, y, z)).is(node)) return true;
        }
        return false;
    }

    /**
     * The hilltop stones of the original are dead code.
     *
     * <p>Oracle {@code ThaumcraftWorldGenerator:424-425} only draws a surface
     * structure when the surface of the chunk is below the sea level plus nine,
     * while {@code WorldGenHilltopStones:36} refuses a cluster whose base is below
     * eighty five. The two rules never meet, so the original never grows a cluster.
     * The port keeps both rules, so it does not grow one either; the check therefore
     * asserts the chain and the dead rule instead of a cluster.
     */
    private static void hilltopStones(GameTestHelper test, ServerLevel level, long seed) {
        List<int[]> candidates = chainChunks(seed, StructurePlacementRules.Kind.HILLTOP_STONES, 8);
        test.assertTrue(!candidates.isEmpty(), "the exclusive chain gives chunks to the hilltop stones");
        test.assertTrue(VillageWorldGenRules.HILLTOP_STONE_MIN_Y > level.getSeaLevel() + 9,
                "the original rules of the hilltop stones can never meet: the cluster needs a base of "
                        + VillageWorldGenRules.HILLTOP_STONE_MIN_Y + " and the world generator only draws a structure below "
                        + (level.getSeaLevel() + 9));
        boolean cluster = false;
        for (int[] candidate : candidates) {
            level.getChunk(candidate[0], candidate[1]);
            if (wispSpawner(level, candidate[0], candidate[1])) {
                cluster = true;
                break;
            }
        }
        test.assertTrue(!cluster, "no hilltop cluster grows, the rules of the original are dead code");
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
