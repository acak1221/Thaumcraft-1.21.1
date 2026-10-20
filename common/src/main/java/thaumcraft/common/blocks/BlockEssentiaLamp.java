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
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import thaumcraft.common.config.ThaumcraftBiologicalLamps;
import thaumcraft.common.tiles.TileArcaneLampFertility;
import thaumcraft.common.tiles.TileArcaneLampGrowth;
import thaumcraft.common.tiles.TileEssentiaLamp;

/** Original metalDevice8/13. Support-facing pipe; original Fertility has no support-break rule. */
public final class BlockEssentiaLamp extends BaseEntityBlock {
    public static final DirectionProperty FACING = DirectionProperty.create("facing");
    public static final BooleanProperty ACTIVE = BooleanProperty.create("active");
    private static final VoxelShape SHAPE = box(4, 2, 4, 12, 14, 12);
    private final boolean growth;
    public BlockEssentiaLamp(Properties properties, boolean growth) {
        super(properties.lightLevel(state -> state.getValue(ACTIVE) ? 15 : 8)); this.growth = growth;
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.DOWN).setValue(ACTIVE, false));
    }
    @Override protected MapCodec<? extends BaseEntityBlock> codec() { return simpleCodec(properties -> new BlockEssentiaLamp(properties, growth)); }
    @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) { builder.add(FACING, ACTIVE); }
    @Override public BlockState getStateForPlacement(BlockPlaceContext context) { return defaultBlockState().setValue(FACING, context.getClickedFace().getOpposite()); }
    @Override protected RenderShape getRenderShape(BlockState state) { return RenderShape.MODEL; }
    @Override protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) { return SHAPE; }
    @Override public BlockEntity newBlockEntity(BlockPos pos, BlockState state) { return growth ? new TileArcaneLampGrowth(pos, state) : new TileArcaneLampFertility(pos, state); }
    @Override public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        if (level.isClientSide) return null;
        return growth ? createTickerHelper(type, ThaumcraftBiologicalLamps.GROWTH_TILE.get(), TileEssentiaLamp::tick)
                : createTickerHelper(type, ThaumcraftBiologicalLamps.FERTILITY_TILE.get(), TileEssentiaLamp::tick);
    }
    @Override protected void neighborChanged(BlockState state, Level level, BlockPos pos, Block block, BlockPos from, boolean moved) {
        if (growth && !level.isClientSide && level.getBlockState(pos) == state
                && level.getBlockState(pos.relative(state.getValue(FACING))).isAir()) level.destroyBlock(pos, true);
        super.neighborChanged(state, level, pos, block, from, moved);
    }
}
