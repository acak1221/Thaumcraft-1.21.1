package thaumcraft.common.blocks;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;

/**
 * Golem Fetter and Active Golem Fetter (original BlockCosmeticSolid metadata 9 and 10), one block with an {@code active} state
 * here. A neighbour change toggles it with the redstone signal exactly as the original {@code onNeighborBlockChange} did
 * (BlockCosmeticSolid:337-346): inactive plus signal becomes active, active without signal becomes inactive. Like the original it
 * looks only when a neighbour changes, so a fetter placed beside an already powered block stays inactive until the next change.
 * A golem standing on an active fetter does nothing (original EntityGolemBase:382, 326, 362; see {@link #holds}).
 */
public class BlockGolemFetter extends Block {
    public static final BooleanProperty ACTIVE = BooleanProperty.create("active");

    public BlockGolemFetter(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(ACTIVE, false));
    }

    @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) { builder.add(ACTIVE); }

    @Override
    protected void neighborChanged(BlockState state, Level level, BlockPos pos, Block block, BlockPos fromPos, boolean movedByPiston) {
        if (!level.isClientSide) {
            boolean powered = level.hasNeighborSignal(pos); // original isBlockIndirectlyGettingPowered
            if (state.getValue(ACTIVE) != powered) level.setBlock(pos, state.setValue(ACTIVE, powered), Block.UPDATE_ALL);
        }
        super.neighborChanged(state, level, pos, block, fromPos, movedByPiston);
    }

    /** Whether a golem whose feet are in {@code feet} is held: the block directly under it is an active fetter (original {@code yy > 0 && ...}). */
    public static boolean holds(BlockGetter level, BlockPos feet) {
        BlockPos under = feet.below();
        if (level instanceof Level real && real.isOutsideBuildHeight(under)) return false;
        BlockState state = level.getBlockState(under);
        return state.getBlock() instanceof BlockGolemFetter && state.getValue(ACTIVE);
    }
}
