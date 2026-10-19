package thaumcraft.common.blocks;

import com.mojang.serialization.MapCodec;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import thaumcraft.common.config.ThaumcraftMasonry;
import thaumcraft.common.tiles.TileInfusionPillar;

/** StoneDevice3/4 flattened into a paired block. Only the lower half has the original BE. */
public final class BlockInfusionPillar extends BaseEntityBlock {
    public static final MapCodec<BlockInfusionPillar> CODEC = simpleCodec(BlockInfusionPillar::new);
    public static final EnumProperty<DoubleBlockHalf> HALF = BlockStateProperties.DOUBLE_BLOCK_HALF;
    public static final DirectionProperty FACING = HorizontalDirectionalBlock.FACING;
    private static final VoxelShape LOWER_SELECTION = box(0, 0, 0, 16, 8, 16);
    // StoneDevice:472-473 really points the top's selection back into the base.
    private static final VoxelShape UPPER_SELECTION = box(0, -16, 0, 16, -8, 16);

    public BlockInfusionPillar(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(HALF, DoubleBlockHalf.LOWER).setValue(FACING, Direction.NORTH));
    }

    @Override protected MapCodec<? extends BaseEntityBlock> codec() { return CODEC; }
    @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) { builder.add(HALF, FACING); }
    public static Direction getFacing(BlockState state) { return state.getValue(FACING); }
    public static DoubleBlockHalf getHalf(BlockState state) { return state.getValue(HALF); }
    @Override public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return getHalf(state) == DoubleBlockHalf.LOWER ? new TileInfusionPillar(pos, state) : null;
    }
    @Override protected RenderShape getRenderShape(BlockState state) { return RenderShape.ENTITYBLOCK_ANIMATED; }
    @Override protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return getHalf(state) == DoubleBlockHalf.LOWER ? LOWER_SELECTION : UPPER_SELECTION;
    }
    @Override protected VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) { return Shapes.block(); }
    @Override protected VoxelShape getOcclusionShape(BlockState state, BlockGetter level, BlockPos pos) { return Shapes.empty(); }
    @Override protected BlockState rotate(BlockState state, Rotation rotation) { return state.setValue(FACING, rotation.rotate(getFacing(state))); }
    @Override protected BlockState mirror(BlockState state, Mirror mirror) { return state.rotate(mirror.getRotation(getFacing(state))); }

    @Override protected List<ItemStack> getDrops(BlockState state, LootParams.Builder params) {
        // StoneDevice:249-254: one cosmeticSolid7 (brick) OR cosmeticSolid6
        // (stone), independently for each half. Never seven arcane stones.
        return List.of(new ItemStack(getHalf(state) == DoubleBlockHalf.LOWER
                ? ThaumcraftMasonry.ARCANE_STONE_BRICKS.get() : ThaumcraftMasonry.ARCANE_STONE.get()));
    }

    @Override public ItemStack getCloneItemStack(LevelReader level, BlockPos pos, BlockState state) {
        return new ItemStack(getHalf(state) == DoubleBlockHalf.LOWER
                ? ThaumcraftMasonry.ARCANE_STONE_BRICKS.get() : ThaumcraftMasonry.ARCANE_STONE.get());
    }

    @Override protected boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
        BlockPos otherPos = getHalf(state) == DoubleBlockHalf.LOWER ? pos.above() : pos.below();
        if (level.isOutsideBuildHeight(otherPos)) return false;
        if (!level.hasChunkAt(otherPos)) return true; // An unknown partner is not air.
        BlockState other = level.getBlockState(otherPos);
        // Original neighbor logic checks 3/4, not the saved corner orientation.
        return other.is(this) && getHalf(other) != getHalf(state);
    }

    @Override protected BlockState updateShape(BlockState state, Direction direction, BlockState neighbor,
                                                LevelAccessor level, BlockPos pos, BlockPos neighborPos) {
        Direction partner = getHalf(state) == DoubleBlockHalf.LOWER ? Direction.UP : Direction.DOWN;
        return direction == partner && !state.canSurvive(level, pos) ? Blocks.AIR.defaultBlockState() : state;
    }

    @Override protected void neighborChanged(BlockState state, Level level, BlockPos pos, Block neighbor, BlockPos neighborPos, boolean moving) {
        if (!level.isClientSide && level.getBlockState(pos) == state && !state.canSurvive(level, pos)) level.destroyBlock(pos, true);
        super.neighborChanged(state, level, pos, neighbor, neighborPos, moving);
    }

    @Override protected void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean moving) {
        // Deferred, so the eight formation writes can be installed without a
        // half-pillar notifying/destructing its partner partway through commit.
        if (!level.isClientSide) level.scheduleTick(pos, this, 1);
    }

    @Override protected void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        if (level.getBlockState(pos) == state && !state.canSurvive(level, pos)) level.destroyBlock(pos, true);
    }
}
