package thaumcraft.api.aspects;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import thaumcraft.common.lib.essentia.EssentiaSourceSearch;

/**
 * The original TC4 essentia source helper.
 *
 * <p>Oracle {@code thaumcraft/api/aspects/AspectSourceHelper.java:21-39} published two static helpers that
 * reached {@code thaumcraft.common.lib.events.EssentiaHandler} by reflection. The handler looked for ANY
 * {@link IAspectSource} around the tile and honoured both the direction and the range: the drain took one
 * unit of the aspect out of the first source that held it (oracle {@code EssentiaHandler:49-56}) and the
 * search asked every source whether it held the aspect (oracle {@code EssentiaHandler:71-78}).</p>
 *
 * <p>Forced 1.21.1 change: {@code ForgeDirection} became {@link Direction} and the reflective lookup became
 * a direct call, because the native handler owns the search itself. The unknown direction of 1.7.10 has no
 * native counterpart and is written as a {@code null} direction. The search is the original one:
 * {@link EssentiaSourceSearch} walks the same half box in the same order.</p>
 */
public class AspectSourceHelper {
    private AspectSourceHelper() {}

    /**
     * The original {@code drainEssentia}: one unit of the aspect is taken out of the first source of the
     * search that holds it.
     *
     * <p>The original sent its essence flow packet to the players around the tile
     * (oracle {@code EssentiaHandler:54}). The native port has no packet for an arbitrary consumer, the
     * infusion matrix keeps its own effect for its own drain.</p>
     */
    public static boolean drainEssentia(BlockEntity tile, Aspect aspect, Direction direction, int range) {
        if (tile == null || aspect == null) return false;
        Level level = tile.getLevel();
        if (level == null) return false;
        for (BlockPos pos : EssentiaSourceSearch.sources(level, tile.getBlockPos(), direction, range)) {
            BlockEntity source = level.getBlockEntity(pos);
            // Oracle EssentiaHandler:52 - a source that is gone stops the whole scan.
            if (!(source instanceof IAspectSource aspectSource)) break;
            // Oracle EssentiaHandler:53 - one unit out of the source, the next one is tried when it refuses.
            if (!aspectSource.takeFromContainer(aspect, 1)) continue;
            return true;
        }
        return false;
    }

    /**
     * The original {@code findEssentia}: true when any source of the search holds the aspect.
     *
     * <p>Oracle {@code EssentiaHandler:76} asks the source with {@code doesContainerContainAmount(aspect, 1)}.</p>
     */
    public static boolean findEssentia(BlockEntity tile, Aspect aspect, Direction direction, int range) {
        if (tile == null || aspect == null) return false;
        Level level = tile.getLevel();
        if (level == null) return false;
        for (BlockPos pos : EssentiaSourceSearch.sources(level, tile.getBlockPos(), direction, range)) {
            BlockEntity source = level.getBlockEntity(pos);
            if (!(source instanceof IAspectSource aspectSource)) break;
            if (aspectSource.doesContainerContainAmount(aspect, 1)) return true;
        }
        return false;
    }
}
