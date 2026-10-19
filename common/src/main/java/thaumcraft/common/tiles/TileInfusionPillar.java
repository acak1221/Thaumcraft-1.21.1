package thaumcraft.common.tiles;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import thaumcraft.common.blocks.BlockInfusionPillar;
import thaumcraft.common.config.ThaumcraftInfusion;

/** Corner orientation now lives in the native, automatically persisted block state. */
public final class TileInfusionPillar extends BlockEntity {
    public TileInfusionPillar(BlockPos pos, BlockState state) {
        super(ThaumcraftInfusion.PILLAR_BLOCK_ENTITY.get(), pos, state);
    }

    public Direction getFacing() { return BlockInfusionPillar.getFacing(getBlockState()); }
    public DoubleBlockHalf getHalf() { return BlockInfusionPillar.getHalf(getBlockState()); }
    /** Original orientation2/3/4/5 values, NOT the modern horizontal data index. */
    public int getOrientation() { return getFacing().get3DDataValue(); }
}
