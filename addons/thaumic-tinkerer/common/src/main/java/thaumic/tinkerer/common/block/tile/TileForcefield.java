/**
 * Created by pixlepix on 4/20/14.
 *
 * ThaumicTinkerer is Open Source and distributed under a Creative Commons Attribution-NonCommercial-ShareAlike 3.0
 * License (http://creativecommons.org/licenses/by-nc-sa/3.0/deed.en_GB)
 *
 * ThaumicTinkerer is a Derivative Work on Thaumcraft 4. Thaumcraft 4 (c) Azanor 2012
 * (http://www.minecraftforum.net/topic/1585216-)
 *
 * Ported to Minecraft 1.21.1 for Thaumcraft 4 Reload (see addons/thaumic-tinkerer/NOTICE.md).
 */
package thaumic.tinkerer.common.block.tile;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import thaumic.tinkerer.common.content.SimpleBlocks;
import thaumic.tinkerer.common.core.helper.SimpleBlockFx;

/** Counts sixty-one ticks, sparkling on the client, and then removes its block (the counter is not saved, as in 1.7.10). */
public class TileForcefield extends BlockEntity {
    public int ticks = 60;

    public TileForcefield(BlockPos pos, BlockState state) {
        super(SimpleBlocks.TILE_FORCEFIELD.get(), pos, state);
    }

    /** 1.7.10 {@code updateEntity}, on both sides. */
    public static void tick(Level level, BlockPos pos, BlockState state, TileForcefield tile) {
        if (!level.isClientSide && tile.ticks < 0) {
            level.setBlock(pos, Blocks.AIR.defaultBlockState(), 3);
        }
        tile.ticks--;
        if (level.isClientSide) {
            level.addParticle(SimpleBlockFx.SPARKLE, pos.getX() + level.random.nextDouble(), pos.getY() + level.random.nextDouble(),
                    pos.getZ() + level.random.nextDouble(), 0.0, 0.0, 0.0);
        }
    }
}
