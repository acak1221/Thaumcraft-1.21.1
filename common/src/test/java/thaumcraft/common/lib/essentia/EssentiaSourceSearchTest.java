package thaumcraft.common.lib.essentia;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import net.minecraft.core.Direction;
import org.junit.jupiter.api.Test;
import thaumcraft.common.lib.essentia.EssentiaSourceSearch.Axis;
import thaumcraft.common.lib.essentia.EssentiaSourceSearch.Shape;

/**
 * The geometry of the original TC4 source search.
 *
 * <p>Oracle {@code thaumcraft/common/lib/events/EssentiaHandler.java:84-134}: the search walks a
 * rectangular half box. Two axes are full, the axis of the direction holds only the cells on that side,
 * and the unknown direction of 1.7.10 became UP with the inner loop running from {@code -range}.</p>
 */
class EssentiaSourceSearchTest {
    /** The offsets of one shape in the order of the walk. */
    private static List<int[]> walk(Shape shape) {
        List<int[]> offsets = new ArrayList<>();
        for (int a = -shape.span(); a <= shape.span(); a++) {
            for (int b = -shape.span(); b <= shape.span(); b++) {
                for (int c = shape.innerFrom(); c <= shape.innerTo(); c++) {
                    if (a == 0 && b == 0 && c == 0) continue;
                    offsets.add(shape.offset(a, b, c));
                }
            }
        }
        return offsets;
    }

    /** The set of the cells of one axis of the walked shape. */
    private static Set<Integer> axis(List<int[]> offsets, int axis) {
        Set<Integer> values = new HashSet<>();
        for (int[] offset : offsets) values.add(offset[axis]);
        return values;
    }

    private static Set<Integer> range(int from, int to) {
        Set<Integer> values = new HashSet<>();
        for (int value = from; value <= to; value++) values.add(value);
        return values;
    }

    @Test void theUnknownDirectionIsTheOriginalUnsidedBox() {
        Shape shape = EssentiaSourceSearch.shape(null, 12);
        assertEquals(Axis.X, shape.first());
        assertEquals(Axis.Z, shape.second());
        assertEquals(Axis.Y, shape.inner());
        assertEquals(12, shape.span());
        assertEquals(-12, shape.innerFrom());
        assertEquals(11, shape.innerTo());
        assertEquals(1, shape.innerSign());
        List<int[]> offsets = walk(shape);
        // The original loop of oracle :103-127 visits 25 * 25 * 24 cells, the tile itself excluded.
        assertEquals(25 * 25 * 24 - 1, offsets.size());
        assertEquals(range(-12, 12), axis(offsets, 0));
        assertEquals(range(-12, 11), axis(offsets, 1));
        assertEquals(range(-12, 12), axis(offsets, 2));
    }

    @Test void theUpwardSearchOnlyLooksUp() {
        Shape shape = EssentiaSourceSearch.shape(Direction.UP, 4);
        assertEquals(Axis.X, shape.first());
        assertEquals(Axis.Z, shape.second());
        assertEquals(Axis.Y, shape.inner());
        assertEquals(0, shape.innerFrom());
        assertEquals(3, shape.innerTo());
        List<int[]> offsets = walk(shape);
        assertEquals(9 * 9 * 4 - 1, offsets.size());
        assertEquals(range(-4, 4), axis(offsets, 0));
        assertEquals(range(0, 3), axis(offsets, 1));
        assertEquals(range(-4, 4), axis(offsets, 2));
    }

    @Test void theDownwardSearchOnlyLooksDown() {
        List<int[]> offsets = walk(EssentiaSourceSearch.shape(Direction.DOWN, 4));
        assertEquals(range(-4, 4), axis(offsets, 0));
        assertEquals(range(-3, 0), axis(offsets, 1));
        assertEquals(range(-4, 4), axis(offsets, 2));
    }

    @Test void theNorthernSearchOnlyLooksNorth() {
        Shape shape = EssentiaSourceSearch.shape(Direction.NORTH, 4);
        assertEquals(Axis.X, shape.first());
        assertEquals(Axis.Y, shape.second());
        assertEquals(Axis.Z, shape.inner());
        List<int[]> offsets = walk(shape);
        assertEquals(range(-4, 4), axis(offsets, 0));
        assertEquals(range(-4, 4), axis(offsets, 1));
        assertEquals(range(-3, 0), axis(offsets, 2));
    }

    @Test void theSouthernSearchOnlyLooksSouth() {
        List<int[]> offsets = walk(EssentiaSourceSearch.shape(Direction.SOUTH, 4));
        assertEquals(range(-4, 4), axis(offsets, 0));
        assertEquals(range(-4, 4), axis(offsets, 1));
        assertEquals(range(0, 3), axis(offsets, 2));
    }

    @Test void theWesternSearchOnlyLooksWest() {
        Shape shape = EssentiaSourceSearch.shape(Direction.WEST, 4);
        assertEquals(Axis.Y, shape.first());
        assertEquals(Axis.Z, shape.second());
        assertEquals(Axis.X, shape.inner());
        List<int[]> offsets = walk(shape);
        assertEquals(range(-3, 0), axis(offsets, 0));
        assertEquals(range(-4, 4), axis(offsets, 1));
        assertEquals(range(-4, 4), axis(offsets, 2));
    }

    @Test void theEasternSearchOnlyLooksEast() {
        List<int[]> offsets = walk(EssentiaSourceSearch.shape(Direction.EAST, 4));
        assertEquals(range(0, 3), axis(offsets, 0));
        assertEquals(range(-4, 4), axis(offsets, 1));
        assertEquals(range(-4, 4), axis(offsets, 2));
    }

    @Test void theWalkOrderIsTheOrderOfTheOriginalLoops() {
        List<int[]> up = walk(EssentiaSourceSearch.shape(Direction.UP, 4));
        // The original loops run aa (x), then bb (z), then cc (y).
        assertArrayEquals(new int[] {-4, 0, -4}, up.getFirst());
        assertArrayEquals(new int[] {4, 3, 4}, up.getLast());
        List<int[]> unknown = walk(EssentiaSourceSearch.shape(null, 12));
        assertArrayEquals(new int[] {-12, -12, -12}, unknown.getFirst());
        assertArrayEquals(new int[] {12, 11, 12}, unknown.getLast());
    }

    @Test void theTileItselfIsNeverASource() {
        for (Direction direction : Direction.values()) {
            for (int[] offset : walk(EssentiaSourceSearch.shape(direction, 3))) {
                assertFalse(offset[0] == 0 && offset[1] == 0 && offset[2] == 0, direction + " must skip the tile");
            }
        }
    }

    @Test void aRangeBelowOneSearchesNothing() {
        assertEquals(0, walk(EssentiaSourceSearch.shape(Direction.UP, 0)).size());
        assertEquals(0, walk(EssentiaSourceSearch.shape(null, -3)).size());
    }

    @Test void theNumberOfCellsIsTheNumberOfTheOriginalLoop() {
        assertEquals(25 * 25 * 24 - 1, EssentiaSourceSearch.shape(null, 12).cells());
        assertEquals(9 * 9 * 4 - 1, EssentiaSourceSearch.shape(Direction.UP, 4).cells());
        assertTrue(EssentiaSourceSearch.shape(Direction.EAST, 1).cells() > 0);
    }
}
