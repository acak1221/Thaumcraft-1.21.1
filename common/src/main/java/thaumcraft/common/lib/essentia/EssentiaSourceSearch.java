package thaumcraft.common.lib.essentia;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import thaumcraft.api.aspects.IAspectSource;

/**
 * The original TC4 search for the essentia sources around a tile.
 *
 * <p>Oracle {@code thaumcraft/common/lib/events/EssentiaHandler.java:84-134}: the search walks a
 * rectangular HALF box around the tile. Two axes are full ({@code -range..range}), the third one is the
 * axis of the direction and it only holds the cells on that side ({@code 0..range-1}, multiplied by the
 * step of the direction). The unknown direction of 1.7.10 became UP and let the third axis run from
 * {@code -range} to {@code range-1}, so the whole column below the tile is part of the search.</p>
 *
 * <p>The order of the walk is the order of the original loops: the first axis outermost, the third axis
 * innermost. The first source of that order that holds the aspect is the source the original picks.</p>
 */
public final class EssentiaSourceSearch {
    private EssentiaSourceSearch() {}

    /** The world axis of one search loop. */
    public enum Axis { X, Y, Z }

    /**
     * The shape of the original search: the axis of every loop and the bounds of every loop.
     *
     * @param first the axis of the outer loop, it runs from {@code -span} to {@code span}
     * @param second the axis of the middle loop, it runs from {@code -span} to {@code span}
     * @param inner the axis of the direction, it runs from {@code innerFrom} to {@code innerTo}
     * @param span the range of the search
     * @param innerFrom the first cell of the inner loop
     * @param innerTo the last cell of the inner loop
     * @param innerSign the step of the direction on the inner axis
     */
    public record Shape(Axis first, Axis second, Axis inner, int span, int innerFrom, int innerTo, int innerSign) {
        /** The offset of one loop triple, in world coordinates. */
        public int[] offset(int a, int b, int c) {
            int[] offset = new int[3];
            offset[first.ordinal()] = a;
            offset[second.ordinal()] = b;
            offset[inner.ordinal()] = c * innerSign;
            return offset;
        }
        /** The number of cells the original walk visits, the tile itself excluded. */
        public int cells() { return Math.max(0, (2 * span + 1) * (2 * span + 1) * (innerTo - innerFrom + 1) - 1); }
    }

    /**
     * The shape of the search of one direction.
     *
     * <p>Oracle {@code EssentiaHandler:96-99} turns the unknown direction into UP with the inner loop
     * starting at {@code -range}; {@code :110-122} gives every other direction its axis and its step.</p>
     */
    public static Shape shape(Direction direction, int range) {
        int span = Math.max(0, range);
        if (direction == null) {
            // The unknown direction of 1.7.10: UP with the inner loop running from -range.
            return new Shape(Axis.X, Axis.Z, Axis.Y, span, -span, span - 1, 1);
        }
        return switch (direction) {
            case UP -> new Shape(Axis.X, Axis.Z, Axis.Y, span, 0, span - 1, 1);
            case DOWN -> new Shape(Axis.X, Axis.Z, Axis.Y, span, 0, span - 1, -1);
            case NORTH -> new Shape(Axis.X, Axis.Y, Axis.Z, span, 0, span - 1, -1);
            case SOUTH -> new Shape(Axis.X, Axis.Y, Axis.Z, span, 0, span - 1, 1);
            case WEST -> new Shape(Axis.Y, Axis.Z, Axis.X, span, 0, span - 1, -1);
            case EAST -> new Shape(Axis.Y, Axis.Z, Axis.X, span, 0, span - 1, 1);
        };
    }

    /**
     * Every source of the original search, in the order of the original walk.
     *
     * <p>Oracle {@code EssentiaHandler:123-125} only keeps the cells that hold an {@link IAspectSource};
     * the mutual link of two essentia mirrors of the original ( {@code :124} ) has no native counterpart,
     * because the native mirror is not an {@link IAspectSource}.</p>
     */
    public static List<BlockPos> sources(Level level, BlockPos origin, Direction direction, int range) {
        return sources(level, origin, direction, range, source -> true);
    }

    /** The sources of the original search that the filter accepts. */
    public static List<BlockPos> sources(Level level, BlockPos origin, Direction direction, int range, Predicate<BlockEntity> accept) {
        if (level == null || origin == null) return List.of();
        Shape shape = shape(direction, range);
        List<BlockPos> found = new ArrayList<>();
        int[] offset = new int[3];
        for (int a = -shape.span(); a <= shape.span(); a++) {
            for (int b = -shape.span(); b <= shape.span(); b++) {
                for (int c = shape.innerFrom(); c <= shape.innerTo(); c++) {
                    // Oracle EssentiaHandler:106 - the tile itself is never a source.
                    if (a == 0 && b == 0 && c == 0) continue;
                    offset[shape.first().ordinal()] = a;
                    offset[shape.second().ordinal()] = b;
                    offset[shape.inner().ordinal()] = c * shape.innerSign();
                    BlockPos pos = origin.offset(offset[0], offset[1], offset[2]);
                    if (!level.isInWorldBounds(pos) || !level.hasChunkAt(pos)) continue;
                    BlockEntity source = level.getBlockEntity(pos);
                    if (source instanceof IAspectSource && accept.test(source)) found.add(pos);
                }
            }
        }
        return found;
    }
}
