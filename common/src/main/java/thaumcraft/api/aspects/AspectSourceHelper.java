package thaumcraft.api.aspects;

import net.minecraft.core.Direction;
import net.minecraft.world.level.block.entity.BlockEntity;
import thaumcraft.common.lib.events.EssentiaHandler;
import thaumcraft.common.tiles.TileInfusionMatrix;

/**
 * The original TC4 reflective essentia source helper.
 *
 * <p>Forced 1.21.1 change: {@code ForgeDirection} became {@link Direction} and the original reflective lookup
 * of {@code thaumcraft.common.lib.events.EssentiaHandler} became a direct call, because the native handler
 * owns the matrix, not an arbitrary tile. The native search is unsided and range 12, exactly as the original.</p>
 */
public class AspectSourceHelper {
    private AspectSourceHelper() {}

    /** True when the matrix actually took one unit of the aspect from a nearby source. */
    public static boolean drainEssentia(BlockEntity tile, Aspect aspect, Direction direction, int range) {
        if (!(tile instanceof TileInfusionMatrix matrix) || aspect == null) return false;
        return EssentiaHandler.drainEssentia(matrix, aspect);
    }

    /** True when the matrix currently still owes the aspect. */
    public static boolean findEssentia(BlockEntity tile, Aspect aspect, Direction direction, int range) {
        if (!(tile instanceof TileInfusionMatrix matrix) || aspect == null) return false;
        return matrix.getPendingEssentia(aspect) > 0;
    }
}
