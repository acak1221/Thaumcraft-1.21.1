package thaumcraft.common.blocks;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.functions.LootItemConditionalFunction;
import net.minecraft.world.level.storage.loot.functions.LootItemFunctionType;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;
import thaumcraft.common.config.ThaumcraftNaturals;

/** TC4 BlockMagicalLeaves:144-200,224-230,258-265, with native distance/waterlogging. */
public final class BlockMagicalLeaves extends LeavesBlock {
    public static final MapCodec<BlockMagicalLeaves> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            Codec.BOOL.fieldOf("silverwood").forGetter(BlockMagicalLeaves::isSilverwood), propertiesCodec())
            .apply(instance, BlockMagicalLeaves::new));
    // Old bit8 is NOT the same as native distance7. Undamaged generated leaves
    // do not decay spontaneously; placed leaves carry native persistent=true.
    public static final BooleanProperty CHECK_DECAY = BooleanProperty.create("check_decay");
    /** Original BlockMagicalLeaves:248 - the one in two hundred roll of the greatwood sapling. */
    public static final int SAPLING_ROLL_GREATWOOD = 200;
    /** Original BlockMagicalLeaves:250 - the one in two hundred and fifty roll of the silverwood sapling. */
    public static final int SAPLING_ROLL_SILVERWOOD = 250;
    private final boolean silverwood;

    public BlockMagicalLeaves(boolean silverwood, Properties properties) {
        super(properties);
        this.silverwood = silverwood;
        registerDefaultState(defaultBlockState().setValue(CHECK_DECAY, false));
    }

    @Override public MapCodec<BlockMagicalLeaves> codec() { return CODEC; }
    public boolean isSilverwood() { return silverwood; }

    @Override protected List<ItemStack> getDrops(BlockState state, LootParams.Builder params) {
        // SaplingChance deliberately sets count=0 on a failed original roll.
        // Native 1.21.1 LootTable.createStackSplitter forwards that empty stack
        // into getRandomItems' result list (0 < maxStackSize); it does NOT omit
        // it. Preserve the exact roll, but expose no phantom AIR/zero-count drop.
        List<ItemStack> drops = new ArrayList<>(super.getDrops(state, params));
        drops.removeIf(ItemStack::isEmpty);
        return drops;
    }

    @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(CHECK_DECAY);
    }

    @Override protected boolean isRandomlyTicking(BlockState state) {
        return state.getValue(CHECK_DECAY) && !state.getValue(PERSISTENT);
    }

    @Override protected boolean decaying(BlockState state) {
        return state.getValue(CHECK_DECAY) && !state.getValue(PERSISTENT) && state.getValue(DISTANCE) > 4;
    }

    @Override protected void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        // TC4 refuses the check if the surrounding five-block cube is unloaded.
        if (!level.hasChunksAt(pos.offset(-5, 0, -5), pos.offset(5, 0, 5))) return;
        if (!state.is(this) || !state.getValue(CHECK_DECAY) || state.getValue(PERSISTENT)) return;
        // Do not clear bit8 using a stale native distance while a newly broken
        // trunk's distance ticks are still propagating. The original support
        // test walks four face-adjacent leaf steps, not a Euclidean radius.
        if (!supported(level, pos)) {
            dropResources(state, level, pos);
            level.removeBlock(pos, false);
        }
        // Original BlockMagicalLeaves:213-219: a leaf that still has its support keeps
        // the original metadata eight. The original writes the same metadata back
        // (the line that would clear the bit is commented out at :216), so the leaf
        // is checked again on every later random tick and the sapling roll of the
        // original loot table keeps its condition.
    }

    @Override protected void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        super.tick(state, level, pos, random);
        BlockState updated = level.getBlockState(pos);
        // Also receive support loss from vanilla/tagged logs, whose native
        // block class cannot invoke TC4's old beginLeavesDecay extension hook.
        if (updated.is(this) && updated.getValue(DISTANCE) > state.getValue(DISTANCE) && !updated.getValue(PERSISTENT)) {
            level.setBlock(pos, updated.setValue(CHECK_DECAY, true), Block.UPDATE_CLIENTS);
        }
    }

    private static boolean supported(ServerLevel level, BlockPos origin) {
        Set<BlockPos> visited = new HashSet<>();
        Set<BlockPos> frontier = Set.of(origin);
        visited.add(origin);
        for (int distance = 1; distance <= 4; distance++) {
            Set<BlockPos> next = new HashSet<>();
            for (BlockPos pos : frontier) for (Direction direction : Direction.values()) {
                BlockPos neighbor = pos.relative(direction);
                if (!visited.add(neighbor) || level.isOutsideBuildHeight(neighbor)) continue;
                BlockState state = level.getBlockState(neighbor);
                if (state.is(BlockTags.LOGS) || state.getBlock() instanceof BlockMagicalLog) return true;
                if (state.is(BlockTags.LEAVES) || state.getBlock() instanceof LeavesBlock) next.add(neighbor);
            }
            frontier = next;
        }
        return false;
    }

    @Override protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState replacement, boolean moving) {
        if (!replacement.is(this) && !level.isClientSide) beginDecay(level, pos, 1);
        super.onRemove(state, level, pos, replacement, moving);
    }

    public static void beginDecay(Level level, BlockPos origin, int radius) {
        if (!level.hasChunksAt(origin.offset(-radius - 1, 0, -radius - 1), origin.offset(radius + 1, 0, radius + 1))) return;
        for (BlockPos pos : BlockPos.betweenClosed(origin.offset(-radius, -radius, -radius), origin.offset(radius, radius, radius))) {
            if (level.isOutsideBuildHeight(pos)) continue;
            BlockState state = level.getBlockState(pos);
            if (state.getBlock() instanceof BlockMagicalLeaves && !state.getValue(CHECK_DECAY)) {
                level.setBlock(pos, state.setValue(CHECK_DECAY, true), Block.UPDATE_CLIENTS);
            }
        }
    }

    /** Loot tables handle shears/silk and old bit8/bit4; Fortune is deliberately ignored. */
    public static final class SaplingChance extends LootItemConditionalFunction {
        private static final AtomicLong SOURCE_RANDOM_DRAWS = new AtomicLong();
        public static final MapCodec<SaplingChance> CODEC = RecordCodecBuilder.mapCodec(instance ->
                commonFields(instance).and(Codec.intRange(SAPLING_ROLL_GREATWOOD, SAPLING_ROLL_SILVERWOOD)
                        .fieldOf("bound").forGetter(function -> function.bound))
                        .apply(instance, SaplingChance::new));
        private final int bound;

        private SaplingChance(List<LootItemCondition> conditions, int bound) {
            super(conditions);
            this.bound = bound;
        }

        @Override public LootItemFunctionType<SaplingChance> getType() { return ThaumcraftNaturals.LEAF_SAPLING_CHANCE.get(); }
        /** Diagnostic count only; never saved or used to choose a gameplay result. */
        public static long sourceRandomDraws() { return SOURCE_RANDOM_DRAWS.get(); }

        @Override protected ItemStack run(ItemStack stack, LootContext context) {
            // Original world.rand.nextInt(200/250), not loot-context float RNG.
            int roll = context.getLevel().random.nextInt(bound);
            SOURCE_RANDOM_DRAWS.incrementAndGet();
            stack.setCount(roll == 0 ? 1 : 0);
            return stack;
        }
    }
}
