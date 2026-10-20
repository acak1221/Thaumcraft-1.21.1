package thaumcraft.common.tiles;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import thaumcraft.common.config.ThaumcraftEldritchBlocks;

/**
 * Original TileEldritchNothing: the hidden marker of an exposed eldritch
 * nothing block. It never ticks and never stores anything.
 */
public final class TileEldritchNothing extends BlockEntity {
    public TileEldritchNothing(BlockPos pos, BlockState state) {
        super(ThaumcraftEldritchBlocks.ELDRITCH_NOTHING_TILE.get(), pos, state);
    }
}
