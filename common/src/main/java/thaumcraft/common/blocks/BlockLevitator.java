package thaumcraft.common.blocks;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import thaumcraft.common.config.ThaumcraftLevitator;
import thaumcraft.common.tiles.TileLevitator;
import net.minecraft.util.RandomSource;
import thaumcraft.common.lib.fx.SparkleCallRules;

/**
 * Original BlockLifter (the Arcane Levitator): wood, hardness 2.5, resistance 15 (stored x3, exposed /5: 9). Placing, removing or
 * changing the power of one tells the levitators directly UNDER it to count their stack again (the original's own direction; the ones
 * above learn at their next hundredth tick). The lift itself is {@link TileLevitator}.
 */
public final class BlockLevitator extends BaseEntityBlock {
    public static final MapCodec<BlockLevitator> CODEC = simpleCodec(BlockLevitator::new);

    public BlockLevitator(Properties properties) { super(properties); }
    @Override protected MapCodec<? extends BaseEntityBlock> codec() { return CODEC; }
    @Override public BlockEntity newBlockEntity(BlockPos pos, BlockState state) { return new TileLevitator(pos, state); }
    @Override protected RenderShape getRenderShape(BlockState state) { return RenderShape.MODEL; }

    @Override public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        // Oracle BlockLifter:78-82: only an unpowered, unobstructed lifter sparkles.
        if (level.getBlockEntity(pos) instanceof TileLevitator tile
                && SparkleCallRules.lifterSparkle(tile.gettingPower(), tile.getRange())) {
            SparkleCallRules.sparkle(level,
                    SparkleCallRules.lifterPosition(pos.getX(), pos.getY(), pos.getZ(), random),
                    SparkleCallRules.LIFTER_SIZE, SparkleCallRules.LIFTER_TYPE, SparkleCallRules.LIFTER_GRAVITY);
        }
    }
    @Override public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return createTickerHelper(type, ThaumcraftLevitator.LEVITATOR_BLOCK_ENTITY.get(), TileLevitator::tick);
    }

    @Override protected void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean movedByPiston) {
        updateStack(level, pos);
        super.onPlace(state, level, pos, oldState, movedByPiston);
    }

    @Override protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
        updateStack(level, pos);
        super.onRemove(state, level, pos, newState, movedByPiston);
    }

    @Override protected void neighborChanged(BlockState state, Level level, BlockPos pos, Block block, BlockPos fromPos, boolean movedByPiston) {
        if (level.getBlockEntity(pos) instanceof TileLevitator tile && tile.gettingPower() != tile.wasPowered()) updateStack(level, pos);
        super.neighborChanged(state, level, pos, block, fromPos, movedByPiston);
    }

    private void updateStack(Level level, BlockPos pos) {
        for (int count = 1; level.getBlockState(pos.below(count)).is(this); ++count) {
            if (level.getBlockEntity(pos.below(count)) instanceof TileLevitator tile) tile.requestUpdate();
        }
    }
}
