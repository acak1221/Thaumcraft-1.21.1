package thaumcraft.common.blocks;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.material.FlowingFluid;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import thaumcraft.common.config.ThaumcraftLegacyTiles;
import thaumcraft.common.lib.tiles.LegacyTileRules;

/**
 * Original BlockFluidDeath: the death fluid.
 * <p>
 * The fluid is a rare, slow and glowing liquid. It holds four quanta in a source block, so it spreads three
 * blocks and dries up again, and it dissolves every living thing that touches it: the damage is the quanta of
 * the cell plus one.
 * <p>
 * 1.21.1 has no finite fluids, so the death fluid follows the native pure fluid structure: the block owns the
 * classic quanta flow and the fluid only carries its identity.
 * <p>
 * Oracle: {@code thaumcraft/common/blocks/BlockFluidDeath.java}, lines 33-81 and
 * {@code thaumcraft/common/config/ConfigBlocks.java}:166.
 */
public final class BlockFluidDeath extends LiquidBlock {
    private static final Codec<FlowingFluid> DEATH_CODEC = BuiltInRegistries.FLUID.byNameCodec().comapFlatMap(
            fluid -> fluid instanceof DeathFluid death ? DataResult.success(death) : DataResult.error(() -> "Death fluid required"),
            fluid -> fluid);
    public static final MapCodec<LiquidBlock> CODEC = RecordCodecBuilder.<LiquidBlock>mapCodec(instance -> instance.group(
            DEATH_CODEC.fieldOf("fluid").forGetter(block -> ((BlockFluidDeath) block).fluid),
            propertiesCodec()).apply(instance, BlockFluidDeath::new));
    /** The four quanta of a death fluid source block. */
    public static final int QUANTA_PER_BLOCK = LegacyTileRules.DEATH_FLUID_QUANTA;
    /** The light the death fluid gives off. */
    public static final int LIGHT = 8;
    /** The sides the death fluid spreads along. */
    private static final Direction[] SIDES = {Direction.WEST, Direction.NORTH, Direction.EAST, Direction.SOUTH};

    public BlockFluidDeath(FlowingFluid fluid, Properties properties) {
        super(fluid, properties.lightLevel(state -> LIGHT));
    }

    @Override public MapCodec<LiquidBlock> codec() { return CODEC; }

    /** The original quants of a cell: four in the source and one less in every step away from it. */
    public static int quanta(int level) {
        if (level <= 0) return QUANTA_PER_BLOCK;
        return Math.max(0, QUANTA_PER_BLOCK - level);
    }

    /** The damage the death fluid does in a cell: the quanta of the cell plus one. */
    public static int damage(int level) { return LegacyTileRules.deathFluidDamage(level); }

    @Override protected FluidState getFluidState(BlockState state) {
        int level = state.getValue(LEVEL);
        if (level <= 0) return fluid.getSource(false);
        if (level < QUANTA_PER_BLOCK) return fluid.getFlowing(QUANTA_PER_BLOCK - level, false);
        return Fluids.EMPTY.defaultFluidState();
    }

    /**
     * BlockFluidDeath:55-59 - the death fluid dissolves every living thing in it.
     */
    @Override protected void entityInside(BlockState state, Level level, BlockPos pos, Entity entity) {
        if (level.isClientSide || !(entity instanceof LivingEntity living)) return;
        living.hurt(level.damageSources().magic(), damage(state.getValue(LEVEL)));
    }

    /** The original death fluid does not push entities and does not drown them. */
    @Override protected VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return Shapes.empty();
    }

    /**
     * The classic quanta flow of the death fluid: the source spreads downwards first and then sideways, and
     * every flowing cell without a stronger neighbour dries up again.
     */
    public void flowTick(Level level, BlockPos pos, BlockState state) {
        int current = state.getValue(LEVEL);
        if (current >= QUANTA_PER_BLOCK) {
            level.setBlock(pos, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
            return;
        }
        if (current == 0) {
            if (canFlowInto(level, pos.below())) {
                spread(level, pos.below(), 1);
                return;
            }
            for (Direction side : SIDES) {
                BlockPos target = pos.relative(side);
                if (canFlowInto(level, target)) spread(level, target, 1);
            }
            return;
        }
        int best = QUANTA_PER_BLOCK;
        for (Direction side : new Direction[]{Direction.UP, Direction.WEST, Direction.NORTH, Direction.EAST, Direction.SOUTH}) {
            BlockPos target = pos.relative(side);
            if (!level.hasChunkAt(target)) continue;
            BlockState neighbour = level.getBlockState(target);
            if (!(neighbour.getBlock() instanceof BlockFluidDeath)) continue;
            int neighbourLevel = neighbour.getValue(LEVEL);
            if (neighbourLevel < best) best = neighbourLevel;
        }
        int next = best >= QUANTA_PER_BLOCK ? current + 1 : best + 1;
        if (next == current) return;
        if (next >= QUANTA_PER_BLOCK) level.setBlock(pos, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
        else level.setBlock(pos, state.setValue(LEVEL, next), Block.UPDATE_ALL);
    }

    private void spread(Level level, BlockPos pos, int level0) {
        BlockState state = level.getBlockState(pos);
        level.setBlock(pos, state.setValue(LEVEL, level0), Block.UPDATE_ALL);
    }

    private static boolean canFlowInto(Level level, BlockPos pos) {
        if (!level.hasChunkAt(pos)) return false;
        BlockState state = level.getBlockState(pos);
        if (state.getBlock() instanceof BlockFluidDeath) return state.getValue(LEVEL) > 1;
        return state.isAir();
    }

    @Override protected void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean moved) {
        if (!oldState.is(this)) level.scheduleTick(pos, this, 5);
        super.onPlace(state, level, pos, oldState, moved);
    }

    @Override protected void neighborChanged(BlockState state, Level level, BlockPos pos, Block neighbor, BlockPos neighborPos, boolean moving) {
        level.scheduleTick(pos, this, 5);
        super.neighborChanged(state, level, pos, neighbor, neighborPos, moving);
    }

    @Override protected void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        flowTick(level, pos, state);
    }

    /**
     * The death fluid itself: a rare liquid that glows, is very slow and never becomes a source again.
     */
    public abstract static class DeathFluid extends FlowingFluid {
        @Override public Fluid getSource() { return ThaumcraftLegacyTiles.DEATH_FLUID_SOURCE.get(); }
        @Override public Fluid getFlowing() { return ThaumcraftLegacyTiles.DEATH_FLUID_FLOWING.get(); }
        @Override public Item getBucket() { return Items.AIR; }
        @Override public boolean isSame(Fluid other) { return other instanceof DeathFluid; }
        @Override protected boolean canConvertToSource(Level level) { return false; }
        @Override protected float getExplosionResistance() { return 100F; }
        @Override public int getTickDelay(LevelReader level) { return 5; }
        @Override public int getSlopeFindDistance(LevelReader level) { return 4; }
        @Override public int getDropOff(LevelReader level) { return 1; }
        @Override public boolean canBeReplacedWith(FluidState state, BlockGetter level, BlockPos pos, Fluid other, Direction direction) {
            return direction == Direction.DOWN && !other.is(FluidTags.WATER);
        }
        @Override protected void beforeDestroyingBlock(LevelAccessor level, BlockPos pos, BlockState state) {
            Block.dropResources(state, level, pos, level.getBlockEntity(pos));
        }
        @Override public BlockState createLegacyBlock(FluidState state) {
            return ThaumcraftLegacyTiles.DEATH_FLUID.get().defaultBlockState()
                    .setValue(LiquidBlock.LEVEL, state.isSource() ? 0 : QUANTA_PER_BLOCK - state.getAmount());
        }
        @Override public float getOwnHeight(FluidState state) {
            return state.isSource() ? 1F : (QUANTA_PER_BLOCK - state.getAmount()) / (float) QUANTA_PER_BLOCK;
        }
        @Override public VoxelShape getShape(FluidState state, BlockGetter level, BlockPos pos) {
            return state.isSource() ? Shapes.block() : Shapes.box(0, 0, 0, 1, getHeight(state, level, pos), 1);
        }
        @Override public void tick(Level level, BlockPos pos, FluidState state) {
            if (level.getBlockState(pos).getBlock() instanceof BlockFluidDeath death) death.flowTick(level, pos, level.getBlockState(pos));
        }
    }

    /** The source of the death fluid. */
    public static final class Source extends DeathFluid {
        @Override public boolean isSource(FluidState state) { return true; }
        @Override public int getAmount(FluidState state) { return QUANTA_PER_BLOCK; }
    }

    /** The flowing death fluid. */
    public static final class Flowing extends DeathFluid {
        @Override protected void createFluidStateDefinition(StateDefinition.Builder<Fluid, FluidState> builder) {
            super.createFluidStateDefinition(builder);
            builder.add(LEVEL);
        }
        @Override public boolean isSource(FluidState state) { return false; }
        @Override public int getAmount(FluidState state) { return state.getValue(LEVEL); }
    }
}
