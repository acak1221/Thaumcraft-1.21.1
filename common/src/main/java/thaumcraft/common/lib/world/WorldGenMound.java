package thaumcraft.common.lib.world;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
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
import thaumcraft.common.config.ThaumcraftEldritchBlocks;

/**
 * The TC4 mound, the first structure of the exclusive chain of the overworld.
 *
 * <p>Oracle {@code thaumcraft/common/lib/world/WorldGenMound.java:29-2550}: the original is a
 * 1.7.10 {@code WorldGenerator} that writes a nineteen by nineteen by sixteen block structure
 * with 2441 explicit block calls, a loot room with two containers and a chest and two mob
 * spawners. {@code ThaumcraftWorldGenerator.generateSurface:428-434} draws it in one chunk of
 * 150, below the sea level, and hangs a dark aura node over its centre.
 *
 * <p>1.21.1 has no world generator, so the mound became a deterministic per chunk feature. The
 * chain is drawn from the chunk random of the original
 * ({@link StructurePlacementRules#chunkSeed}), so the mound, the eldritch ring and the hilltop
 * stones always agree on which structure a chunk receives.
 *
 * <p>The shape of the structure lives in {@link WorldGenMoundTemplate}; the loot room and the
 * spawners are written here, in the order of the original.
 *
 * <p>Oracle references:
 * <ul>
 *   <li>{@code WorldGenMound:35-58} - the ground samples of the mound.</li>
 *   <li>{@code WorldGenMound:63-1562} - the block table of the structure.</li>
 *   <li>{@code WorldGenMound:1569-2519} - the second half of the block table.</li>
 *   <li>{@code WorldGenMound:2520-2547} - the loot containers, the chest and the spawners.</li>
 *   <li>{@code ThaumcraftWorldGenerator:423-434} - the position, the sea level test and the
 *       aura node of the mound.</li>
 * </ul>
 */
public final class WorldGenMound extends Feature<NoneFeatureConfiguration> {
    /** The width and the depth of the mound in blocks, oracle WorldGenMound:63-64. */
    public static final int SIZE = WorldGenMoundTemplate.SIZE;
    /** The height of the mound in blocks, oracle WorldGenMound:63-64. */
    public static final int LAYERS = WorldGenMoundTemplate.LAYERS;
    /** The mound grows nine blocks below the surface, oracle ThaumcraftWorldGenerator:424. */
    public static final int DEPTH = 9;
    /** The first container of the loot room, oracle WorldGenMound:2521. */
    public static final int LOOT_X = 9;
    /** The first container of the loot room, oracle WorldGenMound:2521. */
    public static final int LOOT_Y = 1;
    /** The first container of the loot room, oracle WorldGenMound:2521. */
    public static final int LOOT_Z = 7;
    /** The second container of the loot room, oracle WorldGenMound:2524. */
    public static final int LOOT_Z_SECOND = 11;
    /** The chest of the loot room, oracle WorldGenMound:2526-2530. */
    public static final int CHEST_X = 10;
    /** The chest of the loot room, oracle WorldGenMound:2526-2530. */
    public static final int CHEST_Y = 1;
    /** The chest of the loot room, oracle WorldGenMound:2526-2530. */
    public static final int CHEST_Z = 9;
    /** The one in three roll of the trapped chest, oracle WorldGenMound:2525. */
    public static final int TRAPPED_CHEST_CHANCE = 3;
    /** The one in five roll of the second chest filling, oracle WorldGenMound:2533. */
    public static final int SECOND_FILLING_CHANCE = 5;

    public WorldGenMound() {
        super(NoneFeatureConfiguration.CODEC);
    }

    @Override public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> context) {
        WorldGenLevel level = context.level();
        if (!ThaumcraftOreFeature.isSurfaceDimension(level)) return false;
        int chunkX = context.origin().getX() >> 4;
        int chunkZ = context.origin().getZ() >> 4;
        // Oracle ThaumcraftWorldGenerator:423-428 - the position inside the chunk and the
        // exclusive structure chain are drawn from the chunk random of the original, so the
        // mound only grows in a chunk the chain gave to it.
        RandomSource random = RandomSource.create(StructurePlacementRules.chunkSeed(
                level.getLevel().getSeed(), chunkX, chunkZ));
        StructurePlacementRules.Draw draw = StructurePlacementRules.draw(random, chunkX, chunkZ);
        if (draw.kind() != StructurePlacementRules.Kind.MOUND) return false;
        int i = draw.x();
        int k = draw.z();
        // Oracle ThaumcraftWorldGenerator:424 - the mound grows nine blocks below the surface.
        int j = level.getHeight(Heightmap.Types.WORLD_SURFACE, i, k) - DEPTH;
        // Oracle ThaumcraftWorldGenerator:425 - the surface structures only grow below the sea level.
        if (j >= level.getSeaLevel()) return false;
        // Oracle WorldGenMound:64 - the five ground samples of the mound.
        if (!locationIsValidSpawn(level, i + 9, j + 9, k + 9)) return false;
        if (!locationIsValidSpawn(level, i, j + 9, k)) return false;
        if (!locationIsValidSpawn(level, i + 18, j + 9, k)) return false;
        if (!locationIsValidSpawn(level, i + 18, j + 9, k + 18)) return false;
        if (!locationIsValidSpawn(level, i, j + 9, k + 18)) return false;
        build(level, random, i, j, k);
        // Oracle ThaumcraftWorldGenerator:432 - the original draws a value it drops and then
        // hangs a dark aura node over the centre of the mound.
        random.nextInt(200);
        NodeGeneration.createRandomNodeAt(level, new BlockPos(i + 9, j + 8, k + 9), random, false, true, false);
        return true;
    }

    /**
     * Oracle {@code WorldGenMound.LocationIsValidSpawn:35-58}.
     *
     * <p>The ground of the sample has to be stone, grass or dirt; a snow layer or a tall
     * grass on top of one of them is accepted as well.
     */
    static boolean locationIsValidSpawn(WorldGenLevel level, int x, int y, int z) {
        int distanceToAir = 0;
        int max = level.getMaxBuildHeight() - 1;
        BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
        while (y + distanceToAir <= max && !level.getBlockState(cursor.set(x, y + distanceToAir, z)).isAir()) {
            ++distanceToAir;
        }
        if (distanceToAir > StructurePlacementRules.MAX_DISTANCE_TO_AIR) return false;
        int groundY = y + distanceToAir - 1;
        if (groundY < level.getMinBuildHeight()) return false;
        if (!level.getBlockState(new BlockPos(x, groundY + 1, z)).isAir()) return false;
        BlockState ground = level.getBlockState(new BlockPos(x, groundY, z));
        if (isGround(ground)) return true;
        if (!ground.is(Blocks.SNOW) && !ground.is(Blocks.SHORT_GRASS)) return false;
        return isGround(level.getBlockState(new BlockPos(x, groundY - 1, z)));
    }

    /** Oracle {@code WorldGenMound.GetValidSpawnBlocks:31-33} - stone, grass and dirt. */
    static boolean isGround(BlockState state) {
        return state.is(Blocks.STONE) || state.is(Blocks.GRASS_BLOCK) || state.is(Blocks.DIRT);
    }

    /**
     * Oracle {@code WorldGenMound:63-2547} - the table, the loot room and the spawners.
     */
    private void build(WorldGenLevel level, RandomSource random, int i, int j, int k) {
        for (int layer = 0; layer < LAYERS; layer++) {
            for (int z = 0; z < SIZE; z++) {
                for (int x = 0; x < SIZE; x++) {
                    BlockState state = WorldGenMoundTemplate.stateFor(WorldGenMoundTemplate.cell(layer, z, x));
                    if (state == null) continue;
                    set(level, i + x, j + layer, k + z, state);
                }
            }
        }
        // Oracle WorldGenMound:2520-2524 - the two containers of the loot room. The rarity of
        // a container comes from the random of the generator, the kind of it from the random
        // of the world.
        set(level, i + LOOT_X, j + LOOT_Y, k + LOOT_Z, lootState(level, random, lootRarity(random)));
        set(level, i + LOOT_X, j + LOOT_Y, k + LOOT_Z_SECOND, lootState(level, random, lootRarity(random)));
        // Oracle WorldGenMound:2525-2530 - the chest of the loot room. One roll in three
        // gives a trapped chest and puts the tunder of it below the base of the mound.
        boolean trapped = random.nextInt(TRAPPED_CHEST_CHANCE) == 0;
        set(level, i + CHEST_X, j + CHEST_Y, k + CHEST_Z, trapped ? Blocks.TRAPPED_CHEST.defaultBlockState()
                : Blocks.CHEST.defaultBlockState());
        if (trapped) set(level, i + CHEST_X, j - 1, k + CHEST_Z, Blocks.TNT.defaultBlockState());
        // Oracle WorldGenMound:2531-2536 - the loot of the chest, the dungeon loot of the
        // original, rolled a second time in one roll out of five.
        BlockPos chestPos = new BlockPos(i + CHEST_X, j + CHEST_Y, k + CHEST_Z);
        if (level.getBlockEntity(chestPos) instanceof ChestBlockEntity chest) {
            fillDungeonChest(level, chest, random);
            if (random.nextInt(SECOND_FILLING_CHANCE) == 0) fillDungeonChest(level, chest, random);
        }
        // Oracle WorldGenMound:2538-2544 - the two spawners of the rooms of the mound.
        set(level, i + 4, j + 5, k + 4, Blocks.SPAWNER.defaultBlockState());
        set(level, i + 4, j + 5, k + 14, Blocks.SPAWNER.defaultBlockState());
        setSpawner(level, new BlockPos(i + 4, j + 5, k + 4), EntityType.SKELETON, random);
        setSpawner(level, new BlockPos(i + 4, j + 5, k + 14), EntityType.ZOMBIE, random);
    }

    /**
     * Oracle {@code WorldGenMound:2520-2521} - the rarity of a loot container.
     *
     * <p>The original rolls one float and reads the rarity from it: below a tenth the
     * container is rare, below a third it is ordinary and above that it is common.
     */
    public static int lootRarity(RandomSource random) {
        float roll = random.nextFloat();
        return roll < 0.1F ? 2 : (roll < 0.33F ? 1 : 0);
    }

    /**
     * Oracle {@code WorldGenMound:2521} - the kind of a loot container.
     *
     * <p>The original rolls the random of the world for it: three rolls in ten give the
     * crate, the others the urn.
     */
    static BlockState lootState(WorldGenLevel level, RandomSource random, int rarity) {
        boolean crate = level.getLevel().getRandom().nextFloat() < 0.3F;
        Block block = crate ? ThaumcraftEldritchBlocks.LOOT_CRATE.get() : ThaumcraftEldritchBlocks.LOOT_URN.get();
        return block.defaultBlockState().setValue(
                thaumcraft.common.blocks.BlockLoot.RARITY, Math.min(rarity, 2));
    }

    /**
     * Oracle {@code WorldGenMound:2531-2536} - the loot of the chest of the loot room.
     *
     * <p>The original fills the chest with the dungeon loot of the vanilla chest hooks. 1.21.1
     * has no chest hook, so the native dungeon table is used with the chest parameter set,
     * the same way the hilltop stones of the original do it.
     */
    private static void fillDungeonChest(WorldGenLevel level, ChestBlockEntity chest, RandomSource random) {
        ServerLevel server = level.getLevel();
        LootTable table = server.getServer().reloadableRegistries().getLootTable(BuiltInLootTables.SIMPLE_DUNGEON);
        if (table == LootTable.EMPTY) return;
        LootParams params = new LootParams.Builder(server)
                .withParameter(LootContextParams.ORIGIN, Vec3.atCenterOf(chest.getBlockPos()))
                .withLuck(0.0F)
                .create(LootContextParamSets.CHEST);
        int slot = 0;
        for (var stack : table.getRandomItems(params, random)) {
            if (slot >= chest.getContainerSize()) return;
            chest.setItem(slot++, stack);
        }
    }

    /** Oracle {@code WorldGenMound:2539-2544} - the mob of a spawner of the mound. */
    private static void setSpawner(WorldGenLevel level, BlockPos pos, EntityType<?> mob, RandomSource random) {
        if (level.getBlockEntity(pos) instanceof SpawnerBlockEntity spawner) {
            spawner.setEntityId(mob, random);
        }
    }

    /**
     * The write of one block of the mound.
     *
     * <p>The structure is nineteen blocks wide and the original places it in the chunk of its
     * position, so it reaches into the neighbouring chunk. The 1.21.1 generation may write
     * into the eight neighbours of the chunk it generates, and the neighbour has to be there.
     */
    private static boolean set(WorldGenLevel level, int x, int y, int z, BlockState state) {
        BlockPos pos = new BlockPos(x, y, z);
        if (level.isOutsideBuildHeight(pos)) return false;
        if (!level.hasChunk(x >> 4, z >> 4)) return false;
        if (!level.ensureCanWrite(pos)) return false;
        return level.setBlock(pos, state, Block.UPDATE_CLIENTS | Block.UPDATE_NEIGHBORS);
    }
}
