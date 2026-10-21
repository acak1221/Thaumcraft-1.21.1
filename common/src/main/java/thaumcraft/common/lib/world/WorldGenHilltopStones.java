package thaumcraft.common.lib.world;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.VineBlock;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.entity.SpawnerBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;
import net.minecraft.world.level.storage.loot.BuiltInLootTables;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.Vec3;
import thaumcraft.common.config.ThaumcraftMonsters;
import thaumcraft.common.config.ThaumcraftVillages;

/**
 * Oracle thaumcraft.common.lib.world.WorldGenHilltopStones, the TC4 hilltop
 * stone cluster.
 *
 * <p>The original is a 1.7.10 {@code WorldGenerator}: the TC4 world generator
 * draws a position inside the chunk, one chunk in forty grows a cluster of
 * stones on a hilltop, and the cluster carries a chest with the vanilla
 * dungeon loot and a wisp spawner in its middle. 1.21.1 has no
 * {@code WorldGenerator}, so the cluster became a deterministic per chunk
 * feature; the chance, the seven by seven footprint, the four layers, the
 * pillars, the vines and the centre are the original ones and live in
 * {@link VillageWorldGenRules}.
 *
 * <p>The "one chunk in forty" is the third draw of the exclusive chain of the
 * original (mound, eldritch ring, hilltop stones), so the cluster only grows in
 * a chunk the chain gave to it: {@link StructurePlacementRules#draw} rolls the
 * whole chain from the chunk random of the original.
 */
public final class WorldGenHilltopStones extends Feature<NoneFeatureConfiguration> {
    public WorldGenHilltopStones() {
        super(NoneFeatureConfiguration.CODEC);
    }

    @Override public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> context) {
        WorldGenLevel level = context.level();
        if (!ThaumcraftOreFeature.isSurfaceDimension(level)) return false;
        int chunkX = context.origin().getX() >> 4;
        int chunkZ = context.origin().getZ() >> 4;
        // Oracle ThaumcraftWorldGenerator:423-424 - the position inside the chunk and the
        // exclusive structure chain are drawn from the chunk random of the original, so the
        // cluster only grows in a chunk the chain gave to the hilltop stones.
        RandomSource random = RandomSource.create(StructurePlacementRules.chunkSeed(
                level.getLevel().getSeed(), chunkX, chunkZ));
        StructurePlacementRules.Draw draw = StructurePlacementRules.draw(random, chunkX, chunkZ);
        if (draw.kind() != StructurePlacementRules.Kind.HILLTOP_STONES) return false;
        // The 1.21.1 generation writes into the chunk that is generated, so the seven by seven
        // footprint of the cluster has to stay inside it.
        int x = inside(draw.x(), chunkX);
        int z = inside(draw.z(), chunkZ);
        // Oracle ThaumcraftWorldGenerator:424-425 - the cluster grows from nine below the
        // surface and never below the sea level. The original adds the nine back before it
        // builds, so the base of the cluster is the first air above the terrain, the
        // height of the original getHeightValuePos.
        int base = level.getHeight(Heightmap.Types.WORLD_SURFACE, x, z);
        if (base - 9 < level.getSeaLevel()) return false;
        // Oracle WorldGenHilltopStones:63-67 - the five ground samples.
        for (int[] offset : VillageWorldGenRules.HILLTOP_SAMPLE_OFFSETS) {
            if (!locationIsValidSpawn(level, x + offset[0], base, z + offset[1])) return false;
        }
        return build(level, random, x, base, z);
    }

    /**
     * The centre of the cluster inside its chunk.
     *
     * <p>The 1.21.1 generation writes into the chunk that is generated, so the seven by seven
     * footprint of the cluster has to stay inside the chunk of its centre.
     */
    private static int inside(int coordinate, int chunk) {
        int base = chunk << 4;
        int low = base + VillageWorldGenRules.HILLTOP_STONE_RADIUS;
        int high = base + 15 - VillageWorldGenRules.HILLTOP_STONE_RADIUS;
        return Math.min(Math.max(coordinate, low), high);
    }

    /**
     * Oracle WorldGenHilltopStones:35-61 - the ground has to carry one of the
     * valid blocks, with air above it.
     */
    static boolean locationIsValidSpawn(WorldGenLevel level, int x, int y, int z) {
        if (y < VillageWorldGenRules.HILLTOP_STONE_MIN_Y) return false;
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

    /** Oracle WorldGenHilltopStones:32 - stone, grass and dirt. */
    static boolean isValidGround(BlockState state) {
        return state.is(Blocks.STONE) || state.is(Blocks.GRASS_BLOCK) || state.is(Blocks.DIRT);
    }

    /**
     * Oracle WorldGenHilltopStones:63-119 - the cluster itself.
     */
    private boolean build(WorldGenLevel level, RandomSource random, int x, int y, int z) {
        BlockState ground = groundBlock(level, x, z);
        boolean vines = !snowy(level, x, z);
        boolean placed = false;
        for (int dx = -VillageWorldGenRules.HILLTOP_STONE_RADIUS; dx <= VillageWorldGenRules.HILLTOP_STONE_RADIUS; dx++) {
            for (int dz = -VillageWorldGenRules.HILLTOP_STONE_RADIUS; dz <= VillageWorldGenRules.HILLTOP_STONE_RADIUS; dz++) {
                if (VillageWorldGenRules.isHilltopCorner(dx, dz)) continue;
                int bx = x + dx;
                int bz = z + dz;
                // The cap of the cluster, half obsidian and half cosmetic solid.
                placed |= set(level, bx, y, bz, random.nextBoolean()
                        ? VillageWorldGenRules.Material.COSMETIC_SOLID_1 : VillageWorldGenRules.Material.OBSIDIAN);
                boolean stop = false;
                for (int layer = 1; layer <= VillageWorldGenRules.HILLTOP_STONE_LAYERS; layer++) {
                    int by = y - layer;
                    if (by < level.getMinBuildHeight()) continue;
                    BlockState state = level.getBlockState(new BlockPos(bx, by, bz));
                    if (replaceable(state)) placed |= setState(level, new BlockPos(bx, by, bz), ground);
                    if (bx == x && bz == z && layer == 1) {
                        // The middle of the cluster: a chest with the dungeon
                        // loot and a wisp spawner under it.
                        placed |= set(level, bx, y + 1, bz, VillageWorldGenRules.Material.COSMETIC_SOLID_1);
                        placed |= set(level, bx, y + 2, bz, VillageWorldGenRules.Material.CHEST);
                        fillDungeonChest(level, new BlockPos(bx, y + 2, bz), random);
                        placed |= set(level, bx, y, bz, VillageWorldGenRules.Material.MOB_SPAWNER);
                        setWispSpawner(level, new BlockPos(bx, y, bz), random);
                    }
                    if (stop || !VillageWorldGenRules.isHilltopPillar(dx, dz)) continue;
                    placed |= set(level, bx, y + layer, bz, VillageWorldGenRules.Material.COSMETIC_SOLID_0);
                    if (layer < 2 || !random.nextBoolean()) continue;
                    stop = true;
                    if (!vines) continue;
                    if (random.nextInt(VillageWorldGenRules.HILLTOP_STONE_VINE_CHANCE) == 0
                            && isAir(level, bx - 1, y + layer, bz)) {
                        growVines(level, bx - 1, y + layer, bz, Direction.EAST);
                    }
                    if (random.nextInt(VillageWorldGenRules.HILLTOP_STONE_VINE_CHANCE) == 0
                            && isAir(level, bx + 1, y + layer, bz)) {
                        growVines(level, bx + 1, y + layer, bz, Direction.WEST);
                    }
                    if (random.nextInt(VillageWorldGenRules.HILLTOP_STONE_VINE_CHANCE) == 0
                            && isAir(level, bx, y + layer, bz - 1)) {
                        growVines(level, bx, y + layer, bz - 1, Direction.SOUTH);
                    }
                    if (random.nextInt(VillageWorldGenRules.HILLTOP_STONE_VINE_CHANCE) != 0
                            || !isAir(level, bx, y + layer, bz + 1)) continue;
                    growVines(level, bx, y + layer, bz + 1, Direction.NORTH);
                }
            }
        }
        return placed;
    }

    /**
     * The block the original replaces the snow, the flowers and the grass
     * with: the top block of the biome. 1.21.1 has no biome top block, so
     * the surface block of the middle column is used.
     */
    private static BlockState groundBlock(WorldGenLevel level, int x, int z) {
        int surface = level.getHeight(Heightmap.Types.MOTION_BLOCKING, x, z);
        for (int y = surface - 1; y >= level.getMinBuildHeight(); y--) {
            BlockState state = level.getBlockState(new BlockPos(x, y, z));
            if (state.isAir() || state.is(Blocks.SNOW) || state.is(Blocks.SHORT_GRASS)) continue;
            return state;
        }
        return Blocks.GRASS_BLOCK.defaultBlockState();
    }

    /** Oracle WorldGenHilltopStones:68 - the vines only grow where it snows nowhere. */
    private static boolean snowy(WorldGenLevel level, int x, int z) {
        var biome = level.getBiome(new BlockPos(x, level.getMinBuildHeight() + 1, z)).value();
        return biome.hasPrecipitation() && biome.getBaseTemperature() < 0.15F;
    }

    /** Oracle WorldGenHilltopStones:81 - the blocks the ground block replaces. */
    private static boolean replaceable(BlockState state) {
        return state.isAir() || state.is(Blocks.SNOW) || state.is(Blocks.SHORT_GRASS) || state.is(Blocks.FERN)
                || state.is(Blocks.DEAD_BUSH) || state.is(BlockTags.FLOWERS);
    }

    private static boolean isAir(WorldGenLevel level, int x, int y, int z) {
        if (y < level.getMinBuildHeight() || y >= level.getMaxBuildHeight()) return false;
        return level.getBlockState(new BlockPos(x, y, z)).isAir();
    }

    /**
     * Oracle WorldGenHilltopStones:122-130 - a vine and up to four more below
     * it. The 1.7.10 metadata pointed at the side away from the stone, but
     * 1.21.1 removes a vine whose support is missing, so the vine is set on
     * the side of the stone it hangs on.
     */
    private static void growVines(WorldGenLevel level, int x, int y, int z, Direction support) {
        placeVine(level, x, y, z, support);
        int count = VillageWorldGenRules.HILLTOP_STONE_VINE_LENGTH;
        while (count > 0) {
            y--;
            if (!isAir(level, x, y, z)) break;
            placeVine(level, x, y, z, support);
            count--;
        }
    }

    private static void placeVine(WorldGenLevel level, int x, int y, int z, Direction support) {
        BlockPos pos = new BlockPos(x, y, z);
        if (level.isOutsideBuildHeight(pos) || !level.ensureCanWrite(pos)) return;
        BlockState state = Blocks.VINE.defaultBlockState();
        if (support == Direction.EAST) state = state.setValue(VineBlock.EAST, true);
        else if (support == Direction.WEST) state = state.setValue(VineBlock.WEST, true);
        else if (support == Direction.SOUTH) state = state.setValue(VineBlock.SOUTH, true);
        else state = state.setValue(VineBlock.NORTH, true);
        level.setBlock(pos, state, Block.UPDATE_CLIENTS);
    }

    /**
     * Oracle WorldGenHilltopStones:86-93 - the chest with the vanilla dungeon
     * loot, rolled twice.
     */
    private static void fillDungeonChest(WorldGenLevel level, BlockPos pos, RandomSource random) {
        if (!(level.getBlockEntity(pos) instanceof ChestBlockEntity chest)) return;
        ServerLevel server = level.getLevel();
        LootTable table = server.getServer().reloadableRegistries().getLootTable(BuiltInLootTables.SIMPLE_DUNGEON);
        if (table == LootTable.EMPTY) return;
        // The native chest rolls its loot with the chest parameter set, the one that
        // allows the origin the vanilla chest tables need.
        LootParams params = new LootParams.Builder(server)
                .withParameter(LootContextParams.ORIGIN, Vec3.atCenterOf(pos))
                .withLuck(0.0F)
                .create(LootContextParamSets.CHEST);
        int slot = 0;
        RandomSource source = RandomSource.create(random.nextLong());
        for (int pass = 0; pass < 2; pass++) {
            for (var stack : table.getRandomItems(params, source)) {
                if (slot >= chest.getContainerSize()) return;
                chest.setItem(slot++, stack);
            }
        }
    }

    /** Oracle WorldGenHilltopStones:98 - the spawner holds wisps. */
    private static void setWispSpawner(WorldGenLevel level, BlockPos pos, RandomSource random) {
        if (level.getBlockEntity(pos) instanceof SpawnerBlockEntity spawner) {
            spawner.setEntityId(ThaumcraftMonsters.WISP.get(), RandomSource.create(random.nextLong()));
        }
    }

    private static boolean set(WorldGenLevel level, int x, int y, int z, VillageWorldGenRules.Material material) {
        BlockPos pos = new BlockPos(x, y, z);
        if (level.isOutsideBuildHeight(pos) || !level.ensureCanWrite(pos)) return false;
        return level.setBlock(pos, ThaumcraftVillages.state(material, 0),
                Block.UPDATE_CLIENTS | Block.UPDATE_NEIGHBORS);
    }

    private static boolean setState(WorldGenLevel level, BlockPos pos, BlockState state) {
        if (level.isOutsideBuildHeight(pos) || !level.ensureCanWrite(pos)) return false;
        return level.setBlock(pos, state, Block.UPDATE_CLIENTS | Block.UPDATE_NEIGHBORS);
    }
}
