package thaumcraft.common.lib.world;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;
import thaumcraft.common.config.ThaumcraftOuterLands;

/**
 * The eldritch ring of the overworld, the structure that seeds a labyrinth.
 *
 * <p>This is the single port of the original
 * {@code thaumcraft/common/lib/world/WorldGenEldritchRing.java:28-146}. The oracle
 * {@code ThaumcraftWorldGenerator.generateSurface:429-453} draws a position inside the chunk,
 * rolls the structure chain, and hands the ring the size of the labyrinth it may not grow into.
 * The ring then validates five ground samples, builds itself and seeds the labyrinth through
 * {@code MazeHandler}, exactly like the native {@link OuterLandsProvider.MazeSavedData} does.
 *
 * <p>The chain is drawn from the chunk random of the original
 * ({@link StructurePlacementRules#chunkSeed}), so the ring, the hilltop stones and the mound
 * of the chain always agree on which structure a chunk receives.
 *
 * <p>Oracle references:
 * <ul>
 *   <li>{@code WorldGenEldritchRing:35-62} - the valid ground blocks and the spawn samples.</li>
 *   <li>{@code WorldGenEldritchRing:64-83} - the seven by seven footprint and its nine layers.</li>
 *   <li>{@code WorldGenEldritchRing:84-142} - the altar, the banners, the obelisk and the ring stones.</li>
 *   <li>{@code ThaumcraftWorldGenerator:423-449} - the position, the sea level test and the labyrinth.</li>
 *   <li>{@code MazeThread:29-68} - the labyrinth seeding of the maze handler.</li>
 * </ul>
 */
public final class EldritchRingFeature extends Feature<NoneFeatureConfiguration> {
    public EldritchRingFeature() {
        super(NoneFeatureConfiguration.CODEC);
    }

    @Override public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> context) {
        WorldGenLevel level = context.level();
        if (!(level.getLevel() instanceof ServerLevel server)) return false;
        if (OuterLandsProvider.isOuterLands(level.getLevel())) return false;
        int chunkX = context.origin().getX() >> 4;
        int chunkZ = context.origin().getZ() >> 4;
        // Oracle ThaumcraftWorldGenerator:423-453 - the original draws the position inside
        // the chunk and then the exclusive structure chain from the chunk random of the
        // chunk. The ring only wins the chunk when the chain selects it, so the ring and
        // the hilltop stones can never share a chunk.
        RandomSource random = RandomSource.create(StructurePlacementRules.chunkSeed(
                level.getLevel().getSeed(), chunkX, chunkZ));
        StructurePlacementRules.Draw draw = StructurePlacementRules.draw(random, chunkX, chunkZ);
        return placeSelected(level, random, draw, chunkX, chunkZ);
    }

    /** Regeneration continues its caller's stream; fresh generation keeps its original stream above. */
    public boolean placeSelected(WorldGenLevel level, RandomSource random, StructurePlacementRules.Draw draw,
                                 int chunkX, int chunkZ) {
        if (!(level.getLevel() instanceof ServerLevel server)) return false;
        if (draw.kind() != StructurePlacementRules.Kind.ELDRITCH_RING) return false;
        int centreX = inside(draw.x(), chunkX);
        int centreZ = inside(draw.z(), chunkZ);
        // Oracle ThaumcraftWorldGenerator:424 - the ring grows nine blocks below the surface.
        int surface = level.getHeight(Heightmap.Types.WORLD_SURFACE, centreX, centreZ);
        int randPosY = surface - 9;
        // Oracle ThaumcraftWorldGenerator:425 - the surface structures only grow below the sea level.
        if (randPosY >= level.getSeaLevel()) return false;
        int[] size = StructurePlacementRules.ringSize(random);
        int width = size[0];
        int height = size[1];
        // Oracle ThaumcraftWorldGenerator:437 - the ring base is eight blocks above the roll.
        int j = randPosY + 8;
        for (int[] sample : StructurePlacementRules.RING_SAMPLES) {
            if (!validSpawn(level, centreX + sample[0], j, centreZ + sample[1])) return false;
        }
        var available = OuterLandsProvider.maze(server).forGeneration(new BlockPos(centreX, j, centreZ));
        if (available.isEmpty()) return false;
        OuterLandsProvider.MazeSavedData maze = available.get();
        if (maze.mazesInRange(chunkX, chunkZ, width, height)) return false;
        buildRing(level, random, centreX, j, centreZ);
        // Oracle ThaumcraftWorldGenerator:446 - the ring carries a dark aura node
        // above its altar, two blocks above the base of the ring.
        NodeGeneration.createRandomNodeAt(level, new BlockPos(centreX, j + 2, centreZ), random, false, true, false);
        // Oracle ThaumcraftWorldGenerator:447 - the ring seeds the labyrinth of the Outer Lands.
        long seed = random.nextLong();
        MazeGenerator generator = new MazeGenerator(width, height, seed);
        while (!generator.generate()) generator = new MazeGenerator(width, height, ++seed);
        maze.storeLabyrinth(generator, chunkX, chunkZ);
        return true;
    }

    /**
     * Whether {@link #place} of this chunk would build the ring, without building it.
     *
     * <p>The aura totem of the chunk asks this after it drew the same exclusive chain from the
     * chunk seed: the original suppresses the totem only in a chunk whose ring was actually
     * built (oracle ThaumcraftWorldGenerator:441).</p>
     */
    public static boolean preflight(WorldGenLevel level, int chunkX, int chunkZ) {
        if (!(level.getLevel() instanceof ServerLevel server)) return false;
        if (OuterLandsProvider.isOuterLands(level.getLevel())) return false;
        RandomSource random = RandomSource.create(StructurePlacementRules.chunkSeed(
                level.getLevel().getSeed(), chunkX, chunkZ));
        StructurePlacementRules.Draw draw = StructurePlacementRules.draw(random, chunkX, chunkZ);
        if (draw.kind() != StructurePlacementRules.Kind.ELDRITCH_RING) return false;
        int centreX = inside(draw.x(), chunkX);
        int centreZ = inside(draw.z(), chunkZ);
        int randPosY = level.getHeight(Heightmap.Types.WORLD_SURFACE, centreX, centreZ) - 9;
        if (randPosY >= level.getSeaLevel()) return false;
        int[] size = StructurePlacementRules.ringSize(random);
        int j = randPosY + 8;
        for (int[] sample : StructurePlacementRules.RING_SAMPLES) {
            if (!validSpawn(level, centreX + sample[0], j, centreZ + sample[1])) return false;
        }
        var maze = OuterLandsProvider.maze(server);
        return maze.availableForGeneration() && !maze.mazesInRange(chunkX, chunkZ, size[0], size[1]);
    }

    /**
     * The centre of the ring inside its chunk.
     *
     * <p>The 1.21.1 generation writes into the chunk that is generated, so the seven by seven
     * footprint of the ring has to stay inside the chunk of its centre.
     */
    private static int inside(int coordinate, int chunk) {
        int base = chunk << 4;
        int low = base + StructurePlacementRules.RING_RADIUS;
        int high = base + 15 - StructurePlacementRules.RING_RADIUS;
        return Math.min(Math.max(coordinate, low), high);
    }

    /** Oracle {@code WorldGenEldritchRing.LocationIsValidSpawn:39-62}. */
    private static boolean validSpawn(WorldGenLevel level, int x, int y, int z) {
        int distanceToAir = 0;
        int max = level.getMaxBuildHeight() - 1;
        BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
        while (y + distanceToAir <= max && !level.getBlockState(cursor.set(x, y + distanceToAir, z)).isAir()) {
            ++distanceToAir;
        }
        if (distanceToAir > StructurePlacementRules.MAX_DISTANCE_TO_AIR) return false;
        int surfaceY = y + distanceToAir - 1;
        if (surfaceY < level.getMinBuildHeight()) return false;
        if (!level.getBlockState(new BlockPos(x, surfaceY + 1, z)).isAir()) return false;
        BlockState surface = level.getBlockState(new BlockPos(x, surfaceY, z));
        if (isGroundBlock(surface)) return true;
        if (!surface.is(Blocks.SNOW) && !surface.is(Blocks.SNOW_BLOCK)) return false;
        return isGroundBlock(level.getBlockState(new BlockPos(x, surfaceY - 1, z)));
    }

    /** The 1.7 ground blocks of the ring (oracle WorldGenEldritchRing:35-37). */
    private static boolean isGroundBlock(BlockState state) {
        return state.is(Blocks.STONE) || state.is(Blocks.GRAVEL) || state.is(Blocks.PACKED_ICE)
                || state.is(Blocks.GRASS_BLOCK) || state.is(Blocks.SAND) || state.is(Blocks.DIRT);
    }

    /** Oracle {@code WorldGenEldritchRing.func_76484_a:64-145}. */
    private static void buildRing(WorldGenLevel level, RandomSource random, int i, int j, int k) {
        for (int x = i - StructurePlacementRules.RING_RADIUS; x <= i + StructurePlacementRules.RING_RADIUS; ++x) {
            for (int z = k - StructurePlacementRules.RING_RADIUS; z <= k + StructurePlacementRules.RING_RADIUS; ++z) {
                boolean corner = (x == i - StructurePlacementRules.RING_RADIUS || x == i + StructurePlacementRules.RING_RADIUS)
                        && (z == k - StructurePlacementRules.RING_RADIUS || z == k + StructurePlacementRules.RING_RADIUS);
                if (!corner) buildColumn(level, random, x, j, z);
                if (x == i && z == k) {
                    buildAltar(level, random, i, j, k);
                    continue;
                }
                if (!isCapPosition(x - i, z - k)) continue;
                set(level, x, j, z, DungeonRooms.Kind.COSMETIC_SOLID, 1);
                set(level, x, j + 1, z, DungeonRooms.Kind.ELDRITCH, 3);
            }
        }
    }

    /**
     * The floor and the cleared air above one ring column (oracle WorldGenEldritchRing:72-83).
     *
     * <p>The original fills the five layers at and below the ring base, one in four with
     * obsidian, and clears the four layers above it.
     */
    private static void buildColumn(WorldGenLevel level, RandomSource random, int x, int j, int z) {
        for (int q = 1 - StructurePlacementRules.RING_FLOOR_LAYERS; q <= StructurePlacementRules.RING_CLEARED_LAYERS; ++q) {
            BlockPos pos = new BlockPos(x, j + q, z);
            boolean writable = !level.isOutsideBuildHeight(pos) && WorldGenWriteArea.canWrite(level, pos);
            BlockState state = writable ? level.getBlockState(pos) : Blocks.AIR.defaultBlockState();
            boolean floor = q <= 0;
            // Oracle WorldGenEldritchRing:74 - the original fills the floor and everything that
            // may be replaced above it. The 1.7 "material is not solid" test is the native
            // "does not render as a full block" test.
            boolean clearable = state.canBeReplaced() || !state.isSolidRender(level, pos) || state.is(BlockTags.LEAVES);
            if (floor || clearable) {
                // The original places obsidian in the one in four roll and the
                // cosmetic solid in the other three (oracle WorldGenEldritchRing:75-79).
                if (StructurePlacementRules.ringFloor(random.nextInt(StructurePlacementRules.RING_OBSIDIAN_CHANCE))
                        == StructurePlacementRules.RingFloor.OBSIDIAN) {
                    if (writable) level.setBlock(pos, Blocks.OBSIDIAN.defaultBlockState(), Block.UPDATE_NONE);
                } else {
                    BlockState solid = ThaumcraftOuterLands.state(DungeonRooms.Kind.COSMETIC_SOLID, 1, -1);
                    if (solid != null && writable) level.setBlock(pos, solid, Block.UPDATE_NEIGHBORS | Block.UPDATE_CLIENTS);
                }
            }
            if (q <= 0) continue;
            // Oracle WorldGenEldritchRing:82 - the space above the ring is cleared.
            if (writable) level.setBlock(pos, Blocks.AIR.defaultBlockState(), Block.UPDATE_NONE);
        }
    }

    /** The cap stones of the ring (oracle WorldGenEldritchRing:139-142). */
    private static boolean isCapPosition(int dx, int dz) {
        if (Math.abs(dx) == Math.abs(dz)) return false;
        boolean ringEdgeX = Math.abs(dx) == StructurePlacementRules.RING_RADIUS && Math.abs(dz % 2) == 1;
        boolean ringEdgeZ = Math.abs(dz) == StructurePlacementRules.RING_RADIUS && Math.abs(dx % 2) == 1;
        return ringEdgeX || ringEdgeZ;
    }

    /** The altar and the obelisk of the ring centre (oracle WorldGenEldritchRing:84-137). */
    private static void buildAltar(WorldGenLevel level, RandomSource random, int x, int j, int z) {
        set(level, x, j + 1, z, DungeonRooms.Kind.ELDRITCH, 0);
        set(level, x, j, z, DungeonRooms.Kind.COSMETIC_SOLID, 1);
        int variant = random.nextInt(StructurePlacementRules.ALTAR_VARIANT_ROLLS);
        int altar = StructurePlacementRules.altarVariant(variant);
        if (altar != 0) {
            ThaumcraftOuterLands.configureAltar(level.getBlockEntity(new BlockPos(x, j + 1, z)), true, (byte) (altar - 1));
            if (altar != 1) return;
            for (int direction = 2; direction < 6; ++direction) {
                int[] offset = OuterLandsWiringRules.bannerOffset(direction);
                BlockPos bannerPos = new BlockPos(x + offset[0], j + 1, z + offset[1]);
                BlockState banner = ThaumcraftOuterLands.bannerState(8);
                if (banner == null) continue;
                if (level.isOutsideBuildHeight(bannerPos) || !WorldGenWriteArea.canWrite(level, bannerPos)) continue;
                level.setBlock(bannerPos, banner, Block.UPDATE_NEIGHBORS | Block.UPDATE_CLIENTS);
                ThaumcraftOuterLands.configureBanner(level.getBlockEntity(bannerPos),
                        (byte) StructurePlacementRules.bannerFacing(direction));
            }
        }
        set(level, x, j + 3, z, DungeonRooms.Kind.ELDRITCH, 1);
        for (int y = 4; y <= 7; ++y) set(level, x, j + y, z, DungeonRooms.Kind.ELDRITCH, 2);
    }

    private static void set(WorldGenLevel level, int x, int y, int z, DungeonRooms.Kind kind, int variant) {
        BlockState state = ThaumcraftOuterLands.state(kind, variant, -1);
        if (state == null) return;
        BlockPos pos = new BlockPos(x, y, z);
        if (level.isOutsideBuildHeight(pos) || !WorldGenWriteArea.canWrite(level, pos)) return;
        level.setBlock(pos, state, Block.UPDATE_NEIGHBORS | Block.UPDATE_CLIENTS);
    }
}
