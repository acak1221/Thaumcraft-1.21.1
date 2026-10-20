package thaumcraft.common.blocks;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import thaumcraft.common.tiles.TileThaumatorium;
import thaumcraft.common.tiles.TileThaumatoriumTop;

/** Original TileBrainbox has no ticker; its face points at the selected machine half and contributes two formula slots. */
public final class BlockMnemonicMatrix extends Block {
    public static final DirectionProperty FACING = BlockStateProperties.FACING;
    public static final MapCodec<BlockMnemonicMatrix> CODEC = simpleCodec(BlockMnemonicMatrix::new);
    public BlockMnemonicMatrix(Properties properties) { super(properties); registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH)); }
    @Override protected MapCodec<? extends Block> codec() { return CODEC; }
    @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) { builder.add(FACING); }
    @Override public BlockState getStateForPlacement(BlockPlaceContext context) { return defaultBlockState().setValue(FACING, context.getClickedFace().getOpposite()); }
    private void refresh(Level level, BlockPos pos) {
        if (level.isClientSide) return;
        for (Direction side : Direction.values()) {
            BlockPos neighbor = pos.relative(side); if (!level.hasChunkAt(neighbor)) continue;
            if (level.getBlockEntity(neighbor) instanceof TileThaumatorium tile) tile.refreshUpgrades();
            else if (level.getBlockEntity(neighbor) instanceof TileThaumatoriumTop top && top.main() != null) top.main().refreshUpgrades();
        }
    }
    @Override protected void onPlace(BlockState state, Level level, BlockPos pos, BlockState old, boolean moving) { super.onPlace(state, level, pos, old, moving); refresh(level, pos); }
    @Override protected void neighborChanged(BlockState state, Level level, BlockPos pos, Block block, BlockPos from, boolean moving) { refresh(level, pos); }
    @Override protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState after, boolean moving) { super.onRemove(state, level, pos, after, moving); if (!after.is(this)) refresh(level, pos); }
}
