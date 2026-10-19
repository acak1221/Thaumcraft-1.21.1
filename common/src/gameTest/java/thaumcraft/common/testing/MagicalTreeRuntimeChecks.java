package thaumcraft.common.testing;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.BoneMealItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.levelgen.GenerationStep;
import net.minecraft.world.level.levelgen.placement.BiomeFilter;
import net.minecraft.world.phys.AABB;
import thaumcraft.common.blocks.BlockMagicalLeaves;
import thaumcraft.common.config.ThaumcraftNaturals;
import thaumcraft.common.lib.world.WorldGenSilverwoodTrees;

/** One native growth→survival harvest→real recipe/leaf loot check; loader wiring belongs to root. */
public final class MagicalTreeRuntimeChecks {
    private MagicalTreeRuntimeChecks() {}

    public static void run(GameTestHelper test, ServerPlayer player) {
        var level = test.getLevel();
        var placed = level.registryAccess().registryOrThrow(Registries.PLACED_FEATURE).getOrThrow(ThaumcraftNaturals.TREES_KEY);
        test.assertTrue(placed.feature().value().feature() == ThaumcraftNaturals.TREES.get()
                        && placed.placement().size() == 1 && placed.placement().getFirst() instanceof BiomeFilter,
                "one bundled tree pass per chunk, not separately multiplied silverwood/greatwood features");
        var forest = level.registryAccess().registryOrThrow(Registries.BIOME).getOrThrow(Biomes.FOREST).getGenerationSettings().features();
        int step = GenerationStep.Decoration.VEGETAL_DECORATION.ordinal();
        test.assertTrue(forest.size() > step && forest.get(step).stream().filter(holder -> holder.is(ThaumcraftNaturals.TREES_KEY)).count() == 1,
                "the actual loader biome hook includes the magical-tree pass exactly once at vegetal decoration");
        // High above the small GameTest structure. These fixture chunks may be
        // requested by this test; production tree code never requests chunks.
        BlockPos origin = test.absolutePos(new BlockPos(3, 64, 3));
        for (int x = (origin.getX() - 18) >> 4; x <= ((origin.getX() + 18) >> 4); x++) {
            for (int z = (origin.getZ() - 18) >> 4; z <= ((origin.getZ() + 18) >> 4); z++) level.getChunk(x, z);
        }
        Map<BlockPos, BlockState> saved = new LinkedHashMap<>();
        for (BlockPos pos : BlockPos.betweenClosed(origin.offset(-12, -1, -12), origin.offset(12, 24, 12))) {
            test.assertTrue(level.getBlockEntity(pos) == null, "tree fixture does not overwrite a block entity");
            saved.put(pos.immutable(), level.getBlockState(pos));
        }
        var oldMode = player.gameMode.getGameModeForPlayer();
        var oldPosition = player.position();
        var oldHand = player.getMainHandItem().copy();
        float exhaustion = player.getFoodData().getExhaustionLevel();
        AABB fixtureArea = new AABB(origin).inflate(14, 26, 14);
        Set<UUID> initialItems = level.getEntitiesOfClass(ItemEntity.class, fixtureArea).stream().map(ItemEntity::getUUID).collect(Collectors.toSet());
        long resumeSeed = level.random.nextLong();
        try {
            player.setGameMode(GameType.SURVIVAL);
            // Check the random growth gates BEFORE either full canopy exists.
            // Removing one tree and immediately ticking a sapling in the same
            // synchronous fixture sees stale threaded skylight, not a new RNG
            // outcome. Real random ticks occur after normal lighting updates.
            for (BlockPos pos : saved.keySet()) level.setBlock(pos, Blocks.AIR.defaultBlockState(), Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE);
            for (boolean silverwood : new boolean[] {false, true}) {
                var sapling = silverwood ? ThaumcraftNaturals.SILVERWOOD_SAPLING.get() : ThaumcraftNaturals.GREATWOOD_SAPLING.get();
                level.setBlock(origin.below(), Blocks.DIRT.defaultBlockState(), Block.UPDATE_CLIENTS);
                level.setBlock(origin, sapling.defaultBlockState(), Block.UPDATE_CLIENTS);
                var gate = RandomSource.create(0);
                test.assertTrue(gate.nextInt(silverwood ? 50 : 25) != 0, "fixed growth gate fixture is a source non-growth roll");
                level.getBlockState(origin).randomTick(level, origin, RandomSource.create(0));
                test.assertTrue(level.getBlockState(origin).is(sapling), "source 1/25 or 1/50 tick gate does not always grow; species="
                        + (silverwood ? "silverwood" : "greatwood") + "; state=" + level.getBlockState(origin));
            }
            for (boolean silverwood : new boolean[] {false, true}) {
                for (BlockPos pos : saved.keySet()) level.setBlock(pos, Blocks.AIR.defaultBlockState(), Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE);
                var sapling = silverwood ? ThaumcraftNaturals.SILVERWOOD_SAPLING.get() : ThaumcraftNaturals.GREATWOOD_SAPLING.get();
                var log = silverwood ? ThaumcraftNaturals.SILVERWOOD_LOG.get() : ThaumcraftNaturals.GREATWOOD_LOG.get();
                var leaves = silverwood ? ThaumcraftNaturals.SILVERWOOD_LEAVES.get() : ThaumcraftNaturals.GREATWOOD_LEAVES.get();
                var planks = silverwood ? ThaumcraftNaturals.SILVERWOOD_PLANKS.get() : ThaumcraftNaturals.GREATWOOD_PLANKS.get();
                level.setBlock(origin.below(), Blocks.DIRT.defaultBlockState(), Block.UPDATE_CLIENTS);
                level.setBlock(origin, sapling.defaultBlockState(), Block.UPDATE_CLIENTS);
                ItemStack meal = new ItemStack(Items.BONE_MEAL);
                test.assertTrue(!BoneMealItem.growCrop(meal, level, origin) && meal.getCount() == 1,
                        "TC4 magical saplings reject ordinary bone meal without consuming it");
                if (!silverwood) {
                    test.assertTrue(!sapling.grow(level, origin, RandomSource.create(0)) && level.getBlockState(origin).is(sapling),
                            "one soil block cannot sustain the original greatwood 2x2 trunk; failed growth restores sapling");
                }
                for (BlockPos soil : BlockPos.betweenClosed(origin.offset(-3, -1, -3), origin.offset(3, -1, 3))) {
                    level.setBlock(soil, Blocks.DIRT.defaultBlockState(), Block.UPDATE_CLIENTS);
                }
                if (silverwood) {
                    long before = WorldGenSilverwoodTrees.skippedKnotTrees();
                    test.assertTrue(!sapling.grow(level, origin, RandomSource.create(0)) && level.getBlockState(origin).is(sapling)
                                    && WorldGenSilverwoodTrees.skippedKnotTrees() == before + 1,
                            "fixed PURE-knot candidate is counted/skipped atomically, not rerolled into a NORMAL node or plain log");
                }
                // Fixed successful fixtures: no retry loop, no gameplay rarity
                // bypass inside the production generator. The public growth
                // entry is the same one called after a successful random tick.
                test.assertTrue(sapling.grow(level, origin, RandomSource.create(silverwood ? 7 : 0)), "actual magical sapling generates its original tree shape");
                BlockPos harvest = null;
                int logs = 0, canopy = 0, attached = 0, horizontal = 0;
                for (BlockPos pos : saved.keySet()) {
                    BlockState state = level.getBlockState(pos);
                    if (state.is(log)) {
                        ++logs;
                        if (harvest == null) harvest = pos;
                        if (state.getValue(RotatedPillarBlock.AXIS).isHorizontal()) ++horizontal;
                    } else if (state.is(leaves)) {
                        ++canopy;
                        test.assertTrue(!state.getValue(LeavesBlock.PERSISTENT) && !state.getValue(BlockMagicalLeaves.CHECK_DECAY),
                                "generated canopy is natural, not permanently player-placed or pre-decaying");
                        if (state.getValue(LeavesBlock.DISTANCE) == 1) ++attached;
                    }
                }
                test.assertTrue(logs > 16 && canopy > 32 && attached > 0 && horizontal > 0, "real branched/axis-aware trunk and supported canopy, not a reskinned oak");
                if (!silverwood) for (int x = -1; x <= 0; x++) for (int z = -1; z <= 0; z++) {
                    test.assertTrue(level.getBlockState(origin.offset(x, 0, z)).is(log), "greatwood chooses the first valid offset 2x2 soil root");
                }
                if (silverwood) test.assertTrue(level.getBlockState(origin.west(2)).is(log)
                        && level.getBlockState(origin.west(2)).getValue(RotatedPillarBlock.AXIS) == net.minecraft.core.Direction.Axis.X
                        && level.getBlockState(origin.north(2)).getValue(RotatedPillarBlock.AXIS) == net.minecraft.core.Direction.Axis.Z,
                        "silverwood keeps original cross trunk and horizontal root axes");
                AABB dropsArea = new AABB(harvest).inflate(2);
                Set<UUID> oldDrops = level.getEntitiesOfClass(ItemEntity.class, dropsArea).stream().map(ItemEntity::getUUID).collect(Collectors.toSet());
                try {
                    player.setPos(harvest.getX() + 1.5, harvest.getY(), harvest.getZ() + .5);
                    player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.IRON_AXE));
                    test.assertTrue(player.gameMode.destroyBlock(harvest), "survival axe can harvest the actually grown log");
                    var drops = level.getEntitiesOfClass(ItemEntity.class, dropsArea).stream().filter(drop -> !oldDrops.contains(drop.getUUID())).toList();
                    test.assertTrue(drops.size() == 1 && drops.getFirst().getItem().is(log.asItem()) && drops.getFirst().getItem().getCount() == 1,
                            "actual native log harvest drops its species once");
                    var input = CraftingInput.of(1, 1, List.of(drops.getFirst().getItem().copy()));
                    var recipe = level.getRecipeManager().getRecipeFor(RecipeType.CRAFTING, input, level).orElseThrow().value();
                    var result = recipe.assemble(input, level.registryAccess());
                    test.assertTrue(result.is(planks.asItem()) && result.getCount() == 4, "survival log drop feeds the loaded original four-plank recipe");
                } finally {
                    for (var drop : level.getEntitiesOfClass(ItemEntity.class, dropsArea)) if (!oldDrops.contains(drop.getUUID())) drop.discard();
                }
                var natural = leaves.defaultBlockState().setValue(BlockMagicalLeaves.CHECK_DECAY, true);
                ItemStack silk = new ItemStack(Items.IRON_HOE);
                ItemStack fortune = new ItemStack(Items.IRON_HOE);
                var enchantments = level.registryAccess().registryOrThrow(Registries.ENCHANTMENT);
                silk.enchant(enchantments.getHolderOrThrow(Enchantments.SILK_TOUCH), 1);
                fortune.enchant(enchantments.getHolderOrThrow(Enchantments.FORTUNE), 3);
                for (ItemStack tool : List.of(new ItemStack(Items.SHEARS), silk)) {
                    level.random.setSeed(707);
                    long expectedNext = level.random.nextLong();
                    level.random.setSeed(707);
                    long beforeDraws = BlockMagicalLeaves.SaplingChance.sourceRandomDraws();
                    var drops = Block.getDrops(natural, level, origin, null, player, tool);
                    long draws = BlockMagicalLeaves.SaplingChance.sourceRandomDraws() - beforeDraws;
                    long actualNext = level.random.nextLong();
                    test.assertTrue(drops.size() == 1 && drops.getFirst().is(leaves.asItem()) && drops.getFirst().getCount() == 1
                                    && draws == 0 && actualNext == expectedNext,
                            "native shears/silk leaf loot: species=" + leaves + ", seed=707, tool=" + tool + ", drops=" + drops
                                    + ", expected=one species leaf, sourceRngDraws=" + draws + "/expected0, nextLong=" + actualNext + "/expected" + expectedNext);
                }
                for (long seed : new long[] {0, 1, silverwood ? 61 : 180}) for (ItemStack tool : List.of(ItemStack.EMPTY, fortune)) {
                    level.random.setSeed(seed);
                    int bound = silverwood ? 250 : 200;
                    int expectedRoll = level.random.nextInt(bound);
                    boolean expected = expectedRoll == 0;
                    long expectedNext = level.random.nextLong();
                    level.random.setSeed(seed);
                    long beforeDraws = BlockMagicalLeaves.SaplingChance.sourceRandomDraws();
                    var drops = Block.getDrops(natural, level, origin, null, player, tool);
                    long draws = BlockMagicalLeaves.SaplingChance.sourceRandomDraws() - beforeDraws;
                    long actualNext = level.random.nextLong();
                    // Keep the exact first world nextInt(bound) expectation.
                    // Check both invocation count and RNG continuation, rather
                    // than accepting an empty stack or shifting to a later roll.
                    test.assertTrue(drops.size() == (expected ? 1 : 0) && drops.stream().allMatch(stack -> stack.is(sapling.asItem()) && stack.getCount() == 1)
                                    && draws == 1 && actualNext == expectedNext,
                            "exact source leaf loot: species=" + leaves + ", seed=" + seed + ", tool=" + tool + ", fortune=" + (tool.isEmpty() ? 0 : 3)
                                    + ", drops=" + drops + ", expectedSaplings=" + (expected ? 1 : 0) + ", expectedRoll=nextInt(" + bound + ")=" + expectedRoll
                                    + ", sourceRngDraws=" + draws + "/expected1, nextLong=" + actualNext + "/expected" + expectedNext);
                }
                for (BlockState suppressed : List.of(natural.setValue(LeavesBlock.PERSISTENT, true), natural.setValue(BlockMagicalLeaves.CHECK_DECAY, false))) {
                    long seed = silverwood ? 61 : 180; // Otherwise a winning sapling roll.
                    level.random.setSeed(seed);
                    long expectedNext = level.random.nextLong();
                    level.random.setSeed(seed);
                    long beforeDraws = BlockMagicalLeaves.SaplingChance.sourceRandomDraws();
                    var drops = Block.getDrops(suppressed, level, origin, null, player, ItemStack.EMPTY);
                    long draws = BlockMagicalLeaves.SaplingChance.sourceRandomDraws() - beforeDraws;
                    long actualNext = level.random.nextLong();
                    test.assertTrue(drops.isEmpty() && draws == 0 && actualNext == expectedNext,
                            "suppressed leaf loot: state=" + suppressed + ", seed=" + seed + ", tool=empty, drops=" + drops
                                    + ", expectedSaplings=0, sourceRngDraws=" + draws + "/expected0, nextLong=" + actualNext + "/expected" + expectedNext);
                }
                BlockPos isolated = origin.offset(12, 0, 12);
                level.setBlock(isolated, natural, Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE);
                level.getBlockState(isolated).randomTick(level, isolated, RandomSource.create(0));
                test.assertTrue(level.isEmptyBlock(isolated), "triggered unsupported natural leaf really decays through native randomTick");
            }
        } finally {
            for (var entry : saved.entrySet()) level.setBlock(entry.getKey(), entry.getValue(), Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE);
            for (var drop : level.getEntitiesOfClass(ItemEntity.class, fixtureArea)) if (!initialItems.contains(drop.getUUID())) drop.discard();
            player.setItemInHand(InteractionHand.MAIN_HAND, oldHand);
            player.setGameMode(oldMode);
            player.setPos(oldPosition);
            player.getFoodData().setExhaustion(exhaustion);
            level.random.setSeed(resumeSeed);
        }
        test.succeed();
    }
}
