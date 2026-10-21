package thaumcraft.client.fx;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import thaumcraft.client.fx.particles.FXVent;
import thaumcraft.common.tiles.TileThaumatorium;

/**
 * Original TileThaumatorium:221-230 - the vent of the machine.
 *
 * <p>The original draws one white vent particle in front of the machine for every tick of the
 * vent counter. The six random offsets and their order are the ones of the original call.
 */
public final class ThaumatoriumClientEffects implements TileThaumatorium.ClientEffects {
    @Override public void vent(Level level, BlockPos pos, Direction facing) {
        var random = level.getRandom();
        float fx = 0.1F - random.nextFloat() * 0.2F;
        float fz = 0.1F - random.nextFloat() * 0.2F;
        float fy = 0.1F - random.nextFloat() * 0.2F;
        float fx2 = 0.1F - random.nextFloat() * 0.2F;
        float fz2 = 0.1F - random.nextFloat() * 0.2F;
        float fy2 = 0.1F - random.nextFloat() * 0.2F;
        int color = 0xFFFFFF;
        FXVent.emit(level,
                pos.getX() + 0.5F + fx + facing.getStepX() / 2.0F,
                pos.getY() + 0.5F + fy,
                pos.getZ() + 0.5F + fz + facing.getStepZ() / 2.0F,
                facing.getStepX() / 4.0F + fx2, fy2, facing.getStepZ() / 4.0F + fz2, color);
    }
}
