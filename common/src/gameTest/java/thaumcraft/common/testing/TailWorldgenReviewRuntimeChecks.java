package thaumcraft.common.testing;

import dev.architectury.event.EventResult;
import dev.architectury.event.events.common.EntityEvent;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.QuartPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.WorldGenRegion;
import net.minecraft.util.RandomSource;
import net.minecraft.util.datafix.DataFixTypes;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.status.ChunkPyramid;
import net.minecraft.world.level.chunk.status.ChunkStatus;
import net.minecraft.world.level.levelgen.LegacyRandomSource;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.AABB;
import thaumcraft.common.blocks.BlockAiry;
import thaumcraft.common.blocks.BlockTaintFibres;
import thaumcraft.common.blocks.ItemVariantBlock;
import thaumcraft.common.config.ThaumcraftBiomes;
import thaumcraft.common.config.ThaumcraftEldritchBlocks;
import thaumcraft.common.config.ThaumcraftOuterLands;
import thaumcraft.common.config.ThaumcraftTaint;
import thaumcraft.common.lib.world.BiomeColumns;
import thaumcraft.common.lib.world.DungeonRooms;
import thaumcraft.common.lib.world.FamilyVariant;
import thaumcraft.common.lib.world.MazeGenerator;
import thaumcraft.common.lib.world.OuterLandsDimension;
import thaumcraft.common.lib.world.OuterLandsBossRooms;
import thaumcraft.common.lib.world.OuterLandsProvider;
import thaumcraft.common.lib.world.OuterLandsRules;
import thaumcraft.common.lib.world.VillageWizardTower;

/** Native generation, registered adapters and entity collision. All lane input/RNG is explicit. */
public final class TailWorldgenReviewRuntimeChecks {
    private TailWorldgenReviewRuntimeChecks() {}

    /** Oracle GenPassage:231-241 queries the floor that the same generation just built. */
    public static void taintPassageSeesItsNewWalls(GameTestHelper test) {
        try (var lane = new RoomLane(test, 0)) {
            BlockPos target = lane.at(8, 3, 8);
            test.assertTrue(lane.level.getBlockState(target.below()).isAir(), "the real generation input has no old supporting wall");
            lane.generate(passage(OuterLandsRules.FEATURE_TAINT_PASSAGE), new RoomRandom(false, 1));
            test.assertTrue(!lane.level.getBlockState(target.below()).isAir(), "actual feature built the corridor floor");
            test.assertTrue(lane.level.getBlockState(target).is(ThaumcraftTaint.FIBRES.get()),
                    "actual taint passage grows fibres against its own new floor; target=" + lane.level.getBlockState(target));
        }
        test.succeed();
    }

    /** Oracle biome conversion happens even when the fibre roll is zero. */
    public static void taintPassageConvertsBiomeWithoutFibres(GameTestHelper test) {
        try (var lane = new RoomLane(test, 1)) {
            BlockPos target = lane.at(8, 3, 8);
            lane.level.setBlock(target.below(), Blocks.STONE.defaultBlockState(), TubeVariantsRuntimeChecks.QUIET);
            BiomeColumns.set(lane.level, target.getX(), target.getZ(), ThaumcraftBiomes.ELDRITCH_LANDS);
            test.assertTrue(!lane.biome(target).is(ThaumcraftBiomes.TAINT), "native input column begins outside the taint biome");
            int[] biomeLookups = {0};
            lane.generate(passage(OuterLandsRules.FEATURE_TAINT_PASSAGE), new RoomRandom(false, 0), lane.region(biomeLookups));
            test.assertTrue(!lane.level.getBlockState(target).is(ThaumcraftTaint.FIBRES.get()), "declared zero fibre roll leaves this cell empty");
            test.assertTrue(lane.biome(target).is(ThaumcraftBiomes.TAINT),
                    "actual taint passage converts the qualifying native column even without a fibre; biome=" + lane.biome(target));
            test.assertTrue(biomeLookups[0] > 0, "actual generation-region conversion uses its existing BIOMES chunk instead of unwrapping to the live server world");
        }
        test.succeed();
    }

    /** Oracle GenNestRoom:48 places random crust beside blocked exits, preserving the open interior. */
    public static void nestRoomPreservesItsOpenInterior(GameTestHelper test) {
        try (var lane = new RoomLane(test, 2)) {
            MazeGenerator.Cell cell = passage(OuterLandsRules.FEATURE_NEST_ROOM);
            lane.generate(cell, new RoomRandom(true, 1));
            BlockState interior = lane.level.getBlockState(lane.at(8, 2, 6));
            BlockState blockedWest = lane.level.getBlockState(lane.at(4, 2, 6));
            test.assertTrue(interior.isAir() && !blockedWest.isAir(),
                    "actual nest keeps ordinary interior open and crust beside blocked west exit; interior=" + interior + ", west=" + blockedWest);
        }
        test.succeed();
    }

    /** Exercises the actual chest-filling stage against a registered native chest and item registry. */
    public static void wizardTowerWeightedSkullRollCreatesLoot(GameTestHelper test) {
        try (var f = new TubeVariantsRuntimeChecks.Fixture(test)) {
            f.open();
            f.put(f.base, Blocks.CHEST);
            var chest = (ChestBlockEntity) f.level.getBlockEntity(f.base);
            test.assertTrue(chest != null, "native chest block entity exists");
            invokeFillChest(f.level, f.base, chestRandom(0));
            test.assertTrue(count(chest, Items.GLOWSTONE_DUST) == 4, "actual weighted fill positive control creates four first-entry rewards");
            chest.clearContent();
            invokeFillChest(f.level, f.base, chestRandom(23));
            test.assertTrue(count(chest, Items.SKELETON_SKULL) == 4,
                    "four original weight-three metadata-zero skull rewards resolve to native skeleton skulls; inventory=" + chest.saveWithFullMetadata(f.level.registryAccess()));
        }
        test.succeed();
    }

    /** The planned original metadata one must survive the registered native block adapter. */
    public static void roomFibreAdapterRetainsMushroomType(GameTestHelper test) {
        BlockState normal = ThaumcraftOuterLands.state(DungeonRooms.Kind.TAINT_FIBRES, 0, -1);
        BlockState mushroom = ThaumcraftOuterLands.state(DungeonRooms.Kind.TAINT_FIBRES, 1, -1);
        test.assertTrue(normal != null && mushroom != null && normal.is(ThaumcraftTaint.FIBRES.get())
                        && normal.getValue(BlockTaintFibres.TYPE) == 0 && mushroom.getValue(BlockTaintFibres.TYPE) == 1,
                "registered room adapter preserves both fibre types; normal=" + normal + ", mushroom=" + mushroom);
        test.succeed();
    }

    /** Goes through the real boss-room feature, not only the metadata mapping helper. */
    public static void generatedBossDoorUsesUnbreakableFamily(GameTestHelper test) {
        try (var lane = new RoomLane(test, 3)) {
            MazeGenerator.Cell cell = new MazeGenerator.Cell();
            cell.feature = (byte) OuterLandsRules.FEATURE_BOSS_UPPER_LEFT;
            cell.north = true;
            lane.generate(cell, new RoomRandom(false, 1));
            BlockPos target = lane.at(6, 4, 3);
            BlockState barrier = lane.level.getBlockState(target);
            test.assertTrue(barrier.is(ThaumcraftEldritchBlocks.AIRY_UNBREAKABLE.get()) && BlockAiry.variant(barrier) == 12
                            && barrier.getDestroySpeed(lane.level, target) < 0,
                    "actual boss doorway is original unbreakable metadata twelve; barrier=" + barrier);
        }
        test.succeed();
    }

    /** A real boss opening must retire the registered barrier family produced by the corrected adapter. */
    public static void bossOpeningRemovesCorrectNativeBarrier(GameTestHelper test) {
        try (var lane = new RoomLane(test, 4, 32)) {
            var cell = new MazeGenerator.Cell();
            cell.feature = (byte) OuterLandsRules.FEATURE_BOSS_UPPER_LEFT;
            cell.north = true;
            lane.generate(cell, new RoomRandom(false, 1));
            BlockPos lock = lane.at(8, 5, 3), barrier = lane.at(6, 4, 3), unrelated = lane.at(8, 5, 4);
            lane.level.setBlock(barrier, ThaumcraftEldritchBlocks.AIRY_UNBREAKABLE.get().defaultBlockState(), TubeVariantsRuntimeChecks.QUIET);
            lane.level.setBlock(unrelated, Blocks.GOLD_BLOCK.defaultBlockState(), TubeVariantsRuntimeChecks.QUIET);
            var factory = new net.minecraft.world.level.saveddata.SavedData.Factory<>(OuterLandsBossRooms.BossSavedData::new,
                    OuterLandsBossRooms.BossSavedData::load, DataFixTypes.LEVEL);
            var previous = lane.level.getDataStorage().computeIfAbsent(factory, OuterLandsBossRooms.BossSavedData.ID);
            lane.level.getDataStorage().set(OuterLandsBossRooms.BossSavedData.ID, new OuterLandsBossRooms.BossSavedData());
            List<Entity> spawned = new ArrayList<>();
            var area = new AABB(net.minecraft.world.phys.Vec3.atLowerCornerOf(lane.at(0, -1, 0)),
                    net.minecraft.world.phys.Vec3.atLowerCornerOf(lane.at(32, 15, 32)));
            EntityEvent.Add capture = (entity, world) -> {
                if (world == lane.level && area.contains(entity.position())) spawned.add(entity);
                return EventResult.pass();
            };
            EntityEvent.ADD.register(capture);
            try {
                OuterLandsBossRooms.openLock(lane.level, lock);
                test.assertTrue(spawned.size() == 1, "real native opening builds an arena and attempts one boss admission");
                test.assertTrue(lane.level.getBlockState(lock).isAir() && lane.level.getBlockState(barrier).isAir()
                                && lane.level.getBlockState(unrelated).is(Blocks.GOLD_BLOCK),
                        "real boss opening removes its correct barrier family and lock, preserving unrelated blocks; barrier=" + lane.level.getBlockState(barrier));
            } finally {
                EntityEvent.ADD.unregister(capture);
                spawned.forEach(Entity::discard);
                lane.level.getDataStorage().set(OuterLandsBossRooms.BossSavedData.ID, previous);
            }
        }
        test.succeed();
    }

    /** Selection remains invisible; native collision must still stop the actor. */
    public static void unbreakableAiryBarrierHasNativeCollision(GameTestHelper test) {
        try (var f = new TubeVariantsRuntimeChecks.Fixture(test)) {
            f.open();
            var pig = EntityType.PIG.create(f.level);
            test.assertTrue(pig != null, "native collision actor exists");
            try {
                pig.setPos(f.base.getX() + .5, f.base.getY(), f.base.getZ() + .5);
                test.assertTrue(f.level.addFreshEntity(pig), "native collision actor is admitted");
                f.put(f.base, ThaumcraftEldritchBlocks.AIRY_UNBREAKABLE.get());
                BlockState state = f.level.getBlockState(f.base);
                test.assertTrue(!state.getCollisionShape(f.level, f.base, CollisionContext.of(pig)).isEmpty(),
                        "original invisible unbreakable barrier has native full-block collision");
                pig.setPos(f.base.getX() - .5, f.base.getY(), f.base.getZ() + .5);
                pig.move(MoverType.SELF, new net.minecraft.world.phys.Vec3(2, 0, 0));
                test.assertTrue(pig.getX() < f.base.getX(), "actual native entity movement is stopped by the invisible barrier");
            } finally { pig.discard(); }
        }
        test.succeed();
    }

    /** Oracle BlockAiry:188-204 blocks living nonplayers only while the underlying ward is unpowered. */
    public static void wardingFenceCollisionUsesEntityAndPower(GameTestHelper test) {
        try (var f = new TubeVariantsRuntimeChecks.Fixture(test)) {
            f.open();
            BlockState stone = ((ItemVariantBlock) ThaumcraftEldritchBlocks.PAVING_STONE_WARDING_ITEM.get()).variantState();
            f.level.setBlock(f.base.below(), stone, TubeVariantsRuntimeChecks.QUIET);
            f.level.setBlock(f.base, FamilyVariant.with(ThaumcraftEldritchBlocks.AIRY.get().defaultBlockState(), 4), TubeVariantsRuntimeChecks.QUIET);
            var pig = EntityType.PIG.create(f.level);
            test.assertTrue(pig != null, "native ward-fence actor exists");
            try {
                pig.setPos(f.base.getX() + .5, f.base.getY(), f.base.getZ() + .5);
                test.assertTrue(f.level.addFreshEntity(pig), "native ward-fence actor is admitted");
                BlockState fence = f.level.getBlockState(f.base);
                boolean mobBlocked = !fence.getCollisionShape(f.level, f.base, CollisionContext.of(pig)).isEmpty();
                boolean playerPasses = fence.getCollisionShape(f.level, f.base, CollisionContext.of(f.player)).isEmpty();
                f.put(f.base.below().west(), Blocks.REDSTONE_BLOCK);
                test.assertTrue(f.level.hasNeighborSignal(f.base.below()), "actual warding stone receives redstone power");
                boolean poweredPasses = fence.getCollisionShape(f.level, f.base, CollisionContext.of(pig)).isEmpty();
                test.assertTrue(mobBlocked && playerPasses && poweredPasses,
                        "native ward fence blocks mobs, admits players and opens with power; mobBlocked=" + mobBlocked
                                + ", playerPasses=" + playerPasses + ", poweredPasses=" + poweredPasses);
            } finally { pig.discard(); }
        }
        test.succeed();
    }

    private static MazeGenerator.Cell passage(int feature) {
        var cell = new MazeGenerator.Cell();
        cell.feature = (byte) feature;
        cell.north = true;
        cell.south = true;
        return cell;
    }

    private static RandomSource chestRandom(int weightedRoll) {
        return new LegacyRandomSource(0) {
            @Override public int nextInt() { return 0; }
            @Override public int nextInt(int bound) { return bound == 81 ? weightedRoll : 0; }
        };
    }

    private static int count(ChestBlockEntity chest, net.minecraft.world.item.Item item) {
        int amount = 0;
        for (int slot = 0; slot < chest.getContainerSize(); slot++) if (chest.getItem(slot).is(item)) amount += chest.getItem(slot).getCount();
        return amount;
    }

    private static void invokeFillChest(ServerLevel level, BlockPos pos, RandomSource random) {
        try {
            var method = VillageWizardTower.class.getDeclaredMethod("fillChest", WorldGenLevel.class, BlockPos.class, RandomSource.class);
            method.setAccessible(true);
            method.invoke(null, level, pos, random);
        } catch (InvocationTargetException failure) {
            if (failure.getCause() instanceof RuntimeException runtime) throw runtime;
            if (failure.getCause() instanceof Error error) throw error;
            throw new AssertionError(failure.getCause());
        } catch (ReflectiveOperationException failure) { throw new AssertionError(failure); }
    }

    private static final class RoomRandom extends LegacyRandomSource {
        private final boolean booleanRoll;
        private final int taintRoll;
        RoomRandom(boolean booleanRoll, int taintRoll) { super(0); this.booleanRoll = booleanRoll; this.taintRoll = taintRoll; }
        @Override public boolean nextBoolean() { return booleanRoll; }
        @Override public int nextInt(int bound) { return bound == 3 ? taintRoll : bound == 4 ? 0 : Math.min(1, bound - 1); }
        @Override public float nextFloat() { return .99F; }
    }

    /** Owns one existing native chunk and restores its blocks, block entities, biomes and maze map. */
    private static final class RoomLane implements AutoCloseable {
        final GameTestHelper test;
        final ServerLevel level;
        final LevelChunk chunk;
        final int cx;
        final int cz;
        final int width;
        final List<LevelChunk> chunks = new ArrayList<>();
        final Map<BlockPos, BlockState> states = new LinkedHashMap<>();
        final Map<BlockPos, CompoundTag> tiles = new LinkedHashMap<>();
        final List<Holder<Biome>> biomes = new ArrayList<>();
        final OuterLandsProvider.MazeSavedData previousMaze;
        final int minQuartY;
        final int quartHeight;

        RoomLane(GameTestHelper test, int lane) { this(test, lane, 16); }

        RoomLane(GameTestHelper test, int lane, int width) {
            this.test = test;
            this.width = width;
            level = OuterLandsDimension.level(test.getLevel().getServer());
            test.assertTrue(level != null, "registered native Outer Lands dimension is loaded");
            cx = 1400 + lane * 4;
            cz = 1600;
            chunk = level.getChunk(cx, cz);
            for (int x = 0; x < width / 16; x++) for (int z = 0; z < width / 16; z++) chunks.add(level.getChunk(cx + x, cz + z));
            previousMaze = OuterLandsProvider.maze(level);
            minQuartY = QuartPos.fromBlock(level.getMinBuildHeight());
            quartHeight = QuartPos.fromBlock(level.getMaxBuildHeight() - 1) - minQuartY + 1;
            for (int x = 0; x < width / 4; x++) for (int y = 0; y < quartHeight; y++) for (int z = 0; z < width / 4; z++)
                biomes.add(level.getChunk(cx + x / 4, cz + z / 4).getNoiseBiome(cx * 4 + x, minQuartY + y, cz * 4 + z));
            for (BlockPos mutable : BlockPos.betweenClosed(at(0, -1, 0), at(width - 1, 14, width - 1))) {
                BlockPos pos = mutable.immutable();
                states.put(pos, level.getBlockState(pos));
                BlockEntity tile = level.getBlockEntity(pos);
                if (tile != null) tiles.put(pos, tile.saveWithFullMetadata(level.registryAccess()));
            }
            for (BlockPos pos : states.keySet()) {
                level.removeBlockEntity(pos);
                level.setBlock(pos, Blocks.AIR.defaultBlockState(), TubeVariantsRuntimeChecks.QUIET);
            }
        }

        BlockPos at(int x, int y, int z) { return new BlockPos(cx * 16 + x, OuterLandsRules.MAZE_FLOOR_Y + y, cz * 16 + z); }

        Holder<Biome> biome(BlockPos pos) { return chunk.getNoiseBiome(QuartPos.fromBlock(pos.getX()), QuartPos.fromBlock(pos.getY()), QuartPos.fromBlock(pos.getZ())); }

        void generate(MazeGenerator.Cell cell, RandomSource random) { generate(cell, random, level); }

        void generate(MazeGenerator.Cell cell, RandomSource random, WorldGenLevel generation) {
            var entry = new CompoundTag();
            entry.putInt("x", cx); entry.putInt("z", cz); entry.putShort("cell", cell.pack());
            var cells = new ListTag(); cells.add(entry);
            var tag = new CompoundTag(); tag.put("cells", cells);
            level.getServer().overworld().getDataStorage().set(OuterLandsProvider.MazeSavedData.ID,
                    OuterLandsProvider.MazeSavedData.load(tag, level.registryAccess()));
            test.assertTrue(ThaumcraftOuterLands.MAZE.get().place(new FeaturePlaceContext<>(Optional.empty(), generation,
                            level.getChunkSource().getGenerator(), random, at(0, 0, 0), NoneFeatureConfiguration.INSTANCE)),
                    "actual registered maze feature admits its declared native saved cell");
        }

        WorldGenRegion region(int[] biomeLookups) {
            return new WorldGenRegion(level, null, ChunkPyramid.GENERATION_PYRAMID.getStepTo(ChunkStatus.FEATURES), chunk) {
                @Override public boolean hasChunk(int x, int z) { return x == cx && z == cz; }
                @Override public ChunkAccess getChunk(int x, int z, ChunkStatus status, boolean load) {
                    if (!hasChunk(x, z)) throw new AssertionError("room generation requested an unowned chunk");
                    if (status == ChunkStatus.BIOMES) biomeLookups[0]++;
                    return chunk;
                }
                @Override public BlockState getBlockState(BlockPos pos) { return RoomLane.this.level.getBlockState(pos); }
                @Override public BlockEntity getBlockEntity(BlockPos pos) { return RoomLane.this.level.getBlockEntity(pos); }
                @Override public boolean setBlock(BlockPos pos, BlockState state, int flags, int depth) { return RoomLane.this.level.setBlock(pos, state, flags, depth); }
                @Override public int getHeight(net.minecraft.world.level.levelgen.Heightmap.Types type, int x, int z) { return RoomLane.this.level.getHeight(type, x, z); }
            };
        }

        @Override public void close() {
            level.getServer().overworld().getDataStorage().set(OuterLandsProvider.MazeSavedData.ID, previousMaze);
            for (BlockPos pos : states.keySet()) level.removeBlockEntity(pos);
            states.forEach((pos, state) -> level.setBlock(pos, state, TubeVariantsRuntimeChecks.QUIET));
            tiles.forEach((pos, tag) -> {
                BlockEntity tile = BlockEntity.loadStatic(pos, level.getBlockState(pos), tag, level.registryAccess());
                if (tile != null) level.setBlockEntity(tile);
            });
            for (LevelChunk owned : chunks) {
                owned.fillBiomesFromNoise((x, y, z, ignored) -> biomes.get(((x - cx * 4) * quartHeight + y - minQuartY) * (width / 4) + z - cz * 4),
                        level.getChunkSource().randomState().sampler());
                owned.setUnsaved(true);
            }
        }
    }
}
