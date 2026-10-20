package thaumcraft.common.tiles;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import thaumcraft.common.config.ThaumcraftEldritchBlocks;

/**
 * Original TileEldritchCap: the inert capstone of an eldritch structure. It
 * never ticks and never stores anything.
 */
public final class TileEldritchCap extends BlockEntity {
    public TileEldritchCap(BlockPos pos, BlockState state) {
        super(ThaumcraftEldritchBlocks.ELDRITCH_TILE.get(), pos, state);
    }
}
