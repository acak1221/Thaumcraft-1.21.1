package thaumcraft.common.lib.world;

import java.util.Random;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;
import thaumcraft.common.config.ThaumcraftVillages;

/**
 * Oracle thaumcraft.common.lib.world.WorldGenEldritchRing, the TC4 eldritch
 * ring.
 *
 * <p>The original is a 1.7.10 {@code WorldGenerator}: the TC4 world generator
 * draws a position inside the chunk, one chunk in sixty six grows an eldritch
 * ring, and the ring carries an eldritch altar with its obelisk in the middle
 * and eldritch stones around it. 1.21.1 has no {@code WorldGenerator}, so the
 * ring became a deterministic per chunk feature; the chance, the seven by
 * seven footprint, the nine layers, the ring stones and the altar are the
 * original ones and live in {@link VillageWorldGenRules}.
 */
public final class WorldGenEldritchRing extends Feature<NoneFeatureConfiguration> {
    /** The device of the Outer Lands unit that carries the eldritch banners. */
    private static final ResourceLocation WOODEN_DEVICE =
            ResourceLocation.fromNamespaceAndPath("thaumcraft", "wooden_device");

    public WorldGenEldritchRing() {
        super(NoneFeatureConfiguration.CODEC);
    }

    @Override public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> context) {
        WorldGenLevel level = context.level();
        if (!ThaumcraftOreFeature.isSurfaceDimension(level)) return false;
        int chunkX = context.origin().getX() >> 4;
        int chunkZ = context.origin().getZ() >> 4;
        Random random = new Random(VillageWorldGenRules.chunkSeed(level.getLevel().getSeed(), chunkX, chunkZ));
        // Oracle ThaumcraftWorldGenerator:423-424 - the position inside the chunk.
        int x = chunkX * 16 + random.nextInt(16);
        int z = chunkZ * 16 + random.nextInt(16);
        if (VillageWorldGenRules.surfaceStructure(random) != VillageWorldGenRules.SurfaceStructure.ELDRITCH_RING) {
            return false;
        }
        // Oracle ThaumcraftWorldGenerator:438-443 - the size of the maze the
        // ring may not grow into.
        int width = VillageWorldGenRules.eldritchRingSize(random.nextInt());
        int height = VillageWorldGenRules.eldritchRingSize(random.nextInt());
        if (VillageWorldGenRules.mazesInRange(chunkX, chunkZ, width, height)) return false;
        int surface = level.getHeight(Heightmap.Types.MOTION_BLOCKING, x, z) - 1;
        if (surface - 9 < level.getSeaLevel()) return false;
        // Oracle ThaumcraftWorldGenerator:437 - the ring grows one below the surface.
        int y = surface - 1;
        // Oracle WorldGenEldritchRing:65 - the five ground samples.
        for (int[] offset : VillageWorldGenRules.ELDRITCH_SAMPLE_OFFSETS) {
            if (!locationIsValidSpawn(level, x + offset[0], y, z + offset[1])) return false;
        }
        return build(level, random, x, y, z);
    }

    /**
     * Oracle WorldGenEldritchRing:39-62 - the ground has to carry one of the
     * valid blocks, with air above it.
     */
    static boolean locationIsValidSpawn(WorldGenLevel level, int x, int y, int z) {
        int distanceToAir = 0;
        BlockPos pos = new BlockPos(x, y, z);
        while (!level.getBlockState(pos).isAir()) {
            distanceToAir++;
            if (distanceToAir > 2 || y + distanceToAir >= level.getMaxBuildHeight()) return false;
            pos = new BlockPos(x, y + distanceToAir, z);
        }
        int groundY = y + distanceToAir - 1;
        BlockState ground = level.getBlockState(new BlockPos(x, groundY, z));
        BlockState above = level.getBlockState(new BlockPos(x, groundY + 1, z));
        BlockState below = level.getBlockState(new BlockPos(x, groundY - 1, z));
        if (!above.isAir()) return false;
        if (isValidGround(ground)) return true;
        return (ground.is(Blocks.SNOW) || ground.is(Blocks.SHORT_GRASS)) && isValidGround(below);
    }

    /** Oracle WorldGenEldritchRing:36 - stone, sand, packed ice, grass, gravel and dirt. */
    static boolean isValidGround(BlockState state) {
        return state.is(Blocks.STONE) || state.is(Blocks.SAND) || state.is(Blocks.PACKED_ICE)
                || state.is(Blocks.GRASS_BLOCK) || state.is(Blocks.GRAVEL) || state.is(Blocks.DIRT);
    }

    /**
     * Oracle WorldGenEldritchRing:64-145 - the ring itself.
     */
    private boolean build(WorldGenLevel level, Random random, int x, int y, int z) {
        boolean placed = false;
        for (int dx = -VillageWorldGenRules.ELDRITCH_RING_RADIUS; dx <= VillageWorldGenRules.ELDRITCH_RING_RADIUS; dx++) {
            for (int dz = -VillageWorldGenRules.ELDRITCH_RING_RADIUS; dz <= VillageWorldGenRules.ELDRITCH_RING_RADIUS; dz++) {
                if (VillageWorldGenRules.isEldritchCorner(dx, dz)) continue;
                int bx = x + dx;
                int bz = z + dz;
                for (int q = -4; q <= 4; q++) {
                    int by = y + q;
                    BlockState state = level.getBlockState(new BlockPos(bx, by, bz));
                    // Oracle WorldGenEldritchRing:74-79 - everything below the
                    // surface is filled, above it only what is replaceable.
                    if (q <= 0 || state.canBeReplaced() || state.getFluidState().isEmpty() || state.is(BlockTags.LEAVES)) {
                        if (random.nextInt(4) == 0) {
                            placed |= set(level, bx, by, bz, VillageWorldGenRules.Material.OBSIDIAN, 0);
                        } else {
                            placed |= set(level, bx, by, bz, VillageWorldGenRules.Material.COSMETIC_SOLID_1, 0);
                        }
                    }
                    if (q <= 0) continue;
                    // Oracle WorldGenEldritchRing:82 - the space above the ring is cleared.
                    setAir(level, bx, by, bz);
                }
                if (bx == x && bz == z) {
                    placed |= buildAltar(level, random, bx, y, bz);
                    continue;
                }
                if (!VillageWorldGenRules.isEldritchRingPosition(dx, dz)) continue;
                // Oracle WorldGenEldritchRing:140-141 - the ring stones.
                placed |= set(level, bx, y, bz, VillageWorldGenRules.Material.COSMETIC_SOLID_1, 0);
                placed |= set(level, bx, y + 1, bz, VillageWorldGenRules.Material.ELDRITCH_CAPSTONE, 0);
            }
        }
        return placed;
    }

    /**
     * Oracle WorldGenEldritchRing:84-137 - the eldritch altar in the middle of
     * the ring and its obelisk.
     */
    private boolean buildAltar(WorldGenLevel level, Random random, int x, int y, int z) {
        boolean placed = false;
        placed |= set(level, x, y + 1, z, VillageWorldGenRules.Material.ELDRITCH_ALTAR, 0);
        placed |= set(level, x, y, z, VillageWorldGenRules.Material.COSMETIC_SOLID_1, 0);
        int roll = random.nextInt(10);
        // Oracle WorldGenEldritchRing:88-89 - the altar only arms itself when
        // it is the real eldritch altar block.
        if (level.getBlockEntity(new BlockPos(x, y + 1, z)) != null) {
            if (VillageWorldGenRules.eldritchArmsAltar(roll)) {
                placed |= buildBanners(level, x, y, z);
            }
        }
        placed |= set(level, x, y + 3, z, VillageWorldGenRules.Material.ELDRITCH_OBELISK, 0);
        placed |= set(level, x, y + 4, z, VillageWorldGenRules.Material.ELDRITCH_OBELISK_MIDDLE, 0);
        placed |= set(level, x, y + 5, z, VillageWorldGenRules.Material.ELDRITCH_OBELISK_MIDDLE, 0);
        placed |= set(level, x, y + 6, z, VillageWorldGenRules.Material.ELDRITCH_OBELISK_MIDDLE, 0);
        placed |= set(level, x, y + 7, z, VillageWorldGenRules.Material.ELDRITCH_OBELISK_MIDDLE, 0);
        return placed;
    }

    /**
     * Oracle WorldGenEldritchRing:97-122 - the four banner devices around the
     * altar.
     */
    private boolean buildBanners(WorldGenLevel level, int x, int y, int z) {
        Block device = BuiltInRegistries.BLOCK.getOptional(WOODEN_DEVICE).orElse(null);
        if (device == null) return false;
        boolean placed = false;
        for (int i = 0; i < VillageWorldGenRules.ELDRITCH_BANNER_OFFSETS.length; i++) {
            int[] offset = VillageWorldGenRules.ELDRITCH_BANNER_OFFSETS[i];
            BlockPos pos = new BlockPos(x + offset[0], y + 1, z + offset[1]);
            if (level.isOutsideBuildHeight(pos) || !level.ensureCanWrite(pos)) continue;
            placed |= level.setBlock(pos, device.defaultBlockState(), Block.UPDATE_CLIENTS | Block.UPDATE_NEIGHBORS);
        }
        return placed;
    }

    private static boolean set(WorldGenLevel level, int x, int y, int z, VillageWorldGenRules.Material material, int rotation) {
        BlockPos pos = new BlockPos(x, y, z);
        if (level.isOutsideBuildHeight(pos) || !level.ensureCanWrite(pos)) return false;
        return level.setBlock(pos, ThaumcraftVillages.state(material, rotation), Block.UPDATE_CLIENTS | Block.UPDATE_NEIGHBORS);
    }

    private static void setAir(WorldGenLevel level, int x, int y, int z) {
        BlockPos pos = new BlockPos(x, y, z);
        if (level.isOutsideBuildHeight(pos) || !level.ensureCanWrite(pos)) return;
        level.setBlock(pos, Blocks.AIR.defaultBlockState(), Block.UPDATE_CLIENTS | Block.UPDATE_NEIGHBORS);
    }
}
