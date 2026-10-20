package thaumcraft.common.blocks;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import thaumcraft.common.config.ThaumcraftLamps;
import thaumcraft.common.tiles.TileArcaneLamp;

/** Original blockMetalDevice:7. The saved facing points toward the support, which need only be non-air. */
public final class BlockArcaneLamp extends BaseEntityBlock {
    public static final MapCodec<BlockArcaneLamp> CODEC = simpleCodec(BlockArcaneLamp::new);
    public static final DirectionProperty FACING = DirectionProperty.create("facing");
    private static final VoxelShape SHAPE = Block.box(4, 2, 4, 12, 14, 12);
    public BlockArcaneLamp(Properties properties) { super(properties); registerDefaultState(stateDefinition.any().setValue(FACING, Direction.DOWN)); }
    @Override protected MapCodec<? extends BaseEntityBlock> codec() { return CODEC; }
    @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) { builder.add(FACING); }
    @Override public BlockState getStateForPlacement(BlockPlaceContext context) { return defaultBlockState().setValue(FACING, context.getClickedFace().getOpposite()); }
    @Override protected RenderShape getRenderShape(BlockState state) { return RenderShape.MODEL; }
    @Override protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) { return SHAPE; }
    @Override public BlockEntity newBlockEntity(BlockPos pos, BlockState state) { return new TileArcaneLamp(pos, state); }
    @Override public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return level.isClientSide ? null : createTickerHelper(type, ThaumcraftLamps.ARCANE_LAMP_TILE.get(), TileArcaneLamp::tick);
    }
    @Override protected void neighborChanged(BlockState state, Level level, BlockPos pos, Block block, BlockPos from, boolean moved) {
        if (!level.isClientSide && level.getBlockState(pos.relative(state.getValue(FACING))).isAir()) level.destroyBlock(pos, true);
        super.neighborChanged(state, level, pos, block, from, moved);
    }
    @Override protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState replacement, boolean moved) {
        if (!state.is(replacement.getBlock()) && level.getBlockEntity(pos) instanceof TileArcaneLamp lamp) lamp.removeLights();
        super.onRemove(state, level, pos, replacement, moved);
    }
}
