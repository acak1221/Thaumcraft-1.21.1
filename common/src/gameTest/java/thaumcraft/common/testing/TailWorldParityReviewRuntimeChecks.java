package thaumcraft.common.testing;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.LegacyRandomSource;
import net.minecraft.world.level.levelgen.structure.BuiltinStructures;
import net.minecraft.world.level.levelgen.structure.StructureStart;
import net.minecraft.world.level.levelgen.structure.pieces.PiecesContainer;
import net.minecraft.world.level.levelgen.structure.structures.DesertPyramidPiece;
import thaumcraft.common.config.ThaumcraftBiomeSettings;
import thaumcraft.common.config.ThaumcraftOuterLands;
import thaumcraft.common.config.ThaumcraftVillages;
import thaumcraft.common.lib.world.AuraGenRules;
import thaumcraft.common.lib.world.DungeonRooms;
import thaumcraft.common.lib.world.OuterLandsProvider;
import thaumcraft.common.lib.world.ThaumcraftTotemFeature;
import thaumcraft.common.lib.world.VillageTradeRules;
import thaumcraft.common.lib.world.VillageWizardManager;
import thaumcraft.common.lib.world.VillageWorldGenRules;

/** Actual adapters and native state changes with pinned-oracle random-call witnesses. */
public final class TailWorldParityReviewRuntimeChecks {
    private static final int QUIET = Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE;

    private TailWorldParityReviewRuntimeChecks() {}

    /** GenCommon:291 draws nextInt(8) only when the first nextInt(3) is zero. */
    public static void commonDecorationsKeepConditionalRandomDraws(GameTestHelper test) {
        var level = test.getLevel();
        BlockPos target = test.absolutePos(new BlockPos(3, 3, 3));
        var positions = new ArrayList<BlockPos>();
        BlockPos.betweenClosed(target.offset(-1, -1, -1), target.offset(1, 1, 1))
                .forEach(pos -> positions.add(pos.immutable()));
        try (var lane = new BlockLane(level, positions)) {
            List<String> observations = new ArrayList<>();
            boolean ordinary = decorationCase(test, lane, target, new int[]{1, 1, 1},
                    List.of(3, 12), 4, observations);
            boolean glyphed = decorationCase(test, lane, target, new int[]{0, 1},
                    List.of(3, 8), 5, observations);
            boolean trap = decorationCase(test, lane, target, new int[]{0, 0},
                    List.of(3, 8), 10, observations);
            test.assertTrue(ordinary && glyphed && trap,
                    "actual common decorations consume the oracle's conditional draws and retain every outcome: " + observations);
        }
        test.succeed();
    }

    private static boolean decorationCase(GameTestHelper test, BlockLane lane, BlockPos target,
                                           int[] rolls, List<Integer> expectedBounds, int metadata,
                                           List<String> observations) {
        lane.clear();
        lane.level.setBlock(target, Blocks.STONE.defaultBlockState(), QUIET);
        var builder = new DungeonRooms.Builder();
        builder.commons.add(new int[]{target.getX(), target.getY(), target.getZ()});
        var random = new SequenceRandom(rolls);
        invokeDecorations(lane.level, builder, random);
        BlockState expected = ThaumcraftOuterLands.state(DungeonRooms.Kind.ELDRITCH, metadata, -1);
        test.assertTrue(expected != null, "the registered eldritch decoration exists for metadata " + metadata);
        boolean correct = random.bounds.equals(expectedBounds) && lane.level.getBlockState(target) == expected;
        observations.add("metadata=" + metadata + ", calls=" + random.bounds + ", state=" + lane.level.getBlockState(target));
        return correct;
    }

    private static void invokeDecorations(WorldGenLevel level, DungeonRooms.Builder builder, RandomSource random) {
        try {
            var method = OuterLandsProvider.OuterLandsMazeFeature.class.getDeclaredMethod("decorations",
                    WorldGenLevel.class, DungeonRooms.Builder.class, RandomSource.class);
            method.setAccessible(true);
            method.invoke(null, level, builder, random);
        } catch (InvocationTargetException failure) {
            if (failure.getCause() instanceof RuntimeException runtime) throw runtime;
            if (failure.getCause() instanceof Error error) throw error;
            throw new AssertionError(failure.getCause());
        } catch (ReflectiveOperationException failure) {
            throw new AssertionError(failure);
        }
    }

    /** VillageWizardManager:46/51 evaluates random stack amounts before reward metadata. */
    public static void wizardTradesRollAmountsBeforeRewardVariants(GameTestHelper test) {
        Villager villager = EntityType.VILLAGER.create(test.getLevel());
        test.assertTrue(villager != null, "an actual native villager receives the original trade adapters");
        try {
            List<String> observations = new ArrayList<>();
            var trades = VillageWizardManager.trades();
            boolean shards = tradeCase(villager, trades.get(9), new int[]{1, 4}, List.of(2, 6),
                    1, "thaumcraft:order_shard", 3, observations);
            boolean bauble = tradeCase(villager, trades.get(14), new int[]{2, 5}, List.of(3, 6),
                    7, "thaumcraft:discount_ring_perditio", 1, observations);
            villager.getOffers().clear();
            var fixedRandom = new SequenceRandom(new int[0]);
            VillageTradeRules.apply(villager, List.of(trades.get(1)), fixedRandom);
            var fixed = villager.getOffers().getFirst();
            boolean fixedControl = fixedRandom.bounds.isEmpty() && fixed.getCostA().is(Items.EMERALD)
                    && fixed.getCostA().getCount() == 1 && fixed.getResult().getCount() == 1;
            test.assertTrue(shards && bauble && fixedControl,
                    "actual wizard offers preserve amount-before-variant draw order and fixed trades consume no draw: " + observations);
        } finally {
            villager.discard();
        }
        test.succeed();
    }

    private static boolean tradeCase(Villager villager, VillageWorldGenRules.TradeSpec trade, int[] rolls,
                                       List<Integer> expectedBounds, int payment, String reward, int amount,
                                       List<String> observations) {
        villager.getOffers().clear();
        var random = new SequenceRandom(rolls);
        VillageTradeRules.apply(villager, List.of(trade), random);
        if (villager.getOffers().size() != 1) {
            observations.add("missing offer " + trade);
            return false;
        }
        var offer = villager.getOffers().getFirst();
        ItemStack result = offer.getResult();
        boolean correct = random.bounds.equals(expectedBounds) && offer.getCostA().is(Items.EMERALD)
                && offer.getCostA().getCount() == payment && result.getCount() == amount
                && result.is(BuiltInRegistries.ITEM.get(ResourceLocation.parse(reward)));
        observations.add("calls=" + random.bounds + ", pay=" + offer.getCostA() + ", reward=" + result);
        return correct;
    }

    /** Original generateSurface overwrites the scattered flag with the wild-pass return value. */
    public static void scatteredTempleDoesNotSuppressTotemAfterWildPass(GameTestHelper test) {
        var level = test.getLevel();
        test.assertTrue(level.dimension() == Level.OVERWORLD && ThaumcraftTotemFeature.permitsStructures(level)
                        && ThaumcraftBiomeSettings.worldgen().auraNodes(),
                "the actual overworld admits the shared totem stage and its aura prepass");
        var chunk = level.getChunkAt(test.absolutePos(new BlockPos(3, 3, 3)));
        var chunkPos = chunk.getPos();
        var starts = new HashMap<>(chunk.getAllStarts());
        var pyramid = level.registryAccess().registryOrThrow(Registries.STRUCTURE).get(BuiltinStructures.DESERT_PYRAMID);
        test.assertTrue(pyramid != null, "the actual vanilla desert pyramid structure is registered");
        var start = new StructureStart(pyramid, chunkPos, 0, new PiecesContainer(List.of(
                new DesertPyramidPiece(RandomSource.create(0), chunkPos.getMinBlockX(), chunkPos.getMinBlockZ()))));
        test.assertTrue(start.isValid(), "the fixture installs a native vanilla piece/start, not a mock world");
        int x = chunkPos.getMinBlockX() + 4, z = chunkPos.getMinBlockZ() + 4;
        int y = level.getHeight(Heightmap.Types.WORLD_SURFACE, x, z) + 2;
        test.assertTrue(y + 5 < level.getMaxBuildHeight(), "the native column fixture has room above the current surface");
        BlockPos ground = new BlockPos(x, y, z);
        var positions = new ArrayList<BlockPos>();
        for (int height = 0; height <= 5; height++) positions.add(ground.above(height));
        try (var lane = new BlockLane(level, positions)) {
            List<String> observations = new ArrayList<>();
            chunk.setAllStarts(Map.of());
            boolean ordinary = totemCase(test, lane, ground, chunkPos.x, chunkPos.z, 0, true, observations);
            chunk.setStartForStructure(pyramid, start);
            var temple = AuraGenRules.scatteredTemple(level, chunkPos.x, chunkPos.z);
            test.assertTrue(temple != null && temple.centre().equals(start.getBoundingBox().getCenter()),
                    "the actual chunk lookup recognizes the installed native scattered-temple start");
            boolean miss = totemCase(test, lane, ground, chunkPos.x, chunkPos.z, 1, false, observations);
            boolean hit = totemCase(test, lane, ground, chunkPos.x, chunkPos.z, 0, true, observations);
            test.assertTrue(ordinary && miss && hit,
                    "a scattered temple retains the original overwritten flag: reach rarity, build on hit, preserve ground on miss: " + observations);
        } finally {
            chunk.setAllStarts(starts);
        }
        test.succeed();
    }

    private static boolean totemCase(GameTestHelper test, BlockLane lane, BlockPos ground, int chunkX, int chunkZ,
                                       int rarityRoll, boolean shouldBuild, List<String> observations) {
        lane.clear();
        lane.level.setBlock(ground, Blocks.STONE.defaultBlockState(), QUIET);
        test.assertTrue(lane.level.getHeight(Heightmap.Types.WORLD_SURFACE, ground.getX(), ground.getZ()) == ground.getY() + 1,
                "the real native heightmap resolves the prepared ground");
        // Wild rarity misses; the exclusive structure chain chooses NONE. The same stream works with and without a temple.
        var random = new SequenceRandom(new int[]{1, 8, 8, 1, 1, 1, rarityRoll, 4, 4, 0});
        boolean built = ThaumcraftTotemFeature.placePass(lane.level, random, chunkX, chunkZ);
        var expectedBounds = shouldBuild ? List.of(36, 16, 16, 150, 66, 40, 360, 16, 16, 4)
                : List.of(36, 16, 16, 150, 66, 40, 360);
        boolean states = shouldBuild
                ? lane.level.getBlockState(ground) == ThaumcraftVillages.state(VillageWorldGenRules.Material.COSMETIC_SOLID_1, 0)
                    && lane.level.getBlockState(ground.above()) == ThaumcraftVillages.state(VillageWorldGenRules.Material.COSMETIC_SOLID_0, 0)
                    && lane.level.getBlockState(ground.above(2)) == ThaumcraftVillages.state(VillageWorldGenRules.Material.COSMETIC_SOLID_NODE, 0)
                : lane.level.getBlockState(ground).is(Blocks.STONE) && lane.level.getBlockState(ground.above()).isAir();
        observations.add("rarity=" + rarityRoll + ", built=" + built + ", calls=" + random.bounds
                + ", base=" + lane.level.getBlockState(ground) + ", second=" + lane.level.getBlockState(ground.above(2)));
        // The original node factory still consumes its complete generation rolls on the occupied cosmetic host.
        // Those later draws are delegated to native LegacyRandomSource, beyond this stage's exact prefix.
        boolean calls = shouldBuild ? random.bounds.size() >= expectedBounds.size()
                && random.bounds.subList(0, expectedBounds.size()).equals(expectedBounds)
                : random.bounds.equals(expectedBounds);
        return built == shouldBuild && states && calls;
    }

    /** Positional rolls make reversed evaluation order observable in both the calls and the resulting offer. */
    private static final class SequenceRandom extends LegacyRandomSource {
        final List<Integer> bounds = new ArrayList<>();
        final int[] rolls;
        SequenceRandom(int[] rolls) { super(0); this.rolls = rolls.clone(); }
        @Override public int nextInt(int bound) {
            int index = bounds.size();
            bounds.add(bound);
            return index < rolls.length ? Math.floorMod(rolls[index], bound) : super.nextInt(bound);
        }
    }

    /** Save and restore only this synchronous fixture's native blocks and original tile identities. */
    private static final class BlockLane implements AutoCloseable {
        final ServerLevel level;
        final Map<BlockPos, BlockState> states = new LinkedHashMap<>();
        final Map<BlockPos, BlockEntity> tiles = new LinkedHashMap<>();
        BlockLane(ServerLevel level, List<BlockPos> positions) {
            this.level = level;
            for (BlockPos pos : positions) {
                states.put(pos.immutable(), level.getBlockState(pos));
                if (level.getBlockEntity(pos) != null) tiles.put(pos.immutable(), level.getBlockEntity(pos));
            }
        }
        void clear() {
            for (BlockPos pos : states.keySet()) {
                level.removeBlockEntity(pos);
                level.setBlock(pos, Blocks.AIR.defaultBlockState(), QUIET);
            }
        }
        @Override public void close() {
            for (BlockPos pos : states.keySet()) level.removeBlockEntity(pos);
            states.forEach((pos, state) -> level.setBlock(pos, state, QUIET));
            tiles.values().forEach(tile -> { tile.clearRemoved(); level.setBlockEntity(tile); });
        }
    }
}
