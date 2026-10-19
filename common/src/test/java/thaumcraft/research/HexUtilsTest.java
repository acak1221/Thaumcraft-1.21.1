package thaumcraft.research;

import org.junit.jupiter.api.Test;
import thaumcraft.common.lib.utils.HexUtils;

import java.util.HashSet;
import java.util.List;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.*;

class HexUtilsTest {
    @Test
    void gridAndRingHaveHexagonalCardinalities() {
        for (int radius = 0; radius <= 5; radius++) {
            var grid = HexUtils.generateHexes(radius);
            assertEquals(1 + 3 * radius * (radius + 1), grid.size());
            for (var hex : grid.values()) assertTrue(HexUtils.getDistance(new HexUtils.Hex(0, 0), hex) <= radius);
            assertEquals(radius == 0 ? 1 : radius * 6, HexUtils.getRing(radius).size());
        }
    }

    @Test
    void pixelAndCubeConversionsRoundTripAcrossResearchGrid() {
        for (var hex : HexUtils.generateHexes(4).values()) {
            assertEquals(hex, hex.toPixel(12).toHex(12));
            assertEquals(hex, hex.toCubicHex().toHex());
            var cube = hex.toCubicHex();
            assertEquals(0, cube.x + cube.y + cube.z);
            for (int direction = 0; direction < 6; direction++) {
                assertEquals(1, HexUtils.getDistance(hex, hex.getNeighbour(direction)));
            }
        }
    }

    @Test
    void endpointDistributionKeepsOriginalFixedOrientationAndConsumesOneRandomDraw() {
        Random actual = new Random(91);
        Random expected = new Random(91);
        // The oracle discards this nonzero start, so using it would rotate the
        // endpoint list and fail these original radius=2, entries=5 positions.
        assertNotEquals(0, expected.nextInt(12));
        assertEquals(List.of(new HexUtils.Hex(-2, 2), new HexUtils.Hex(0, 2),
                        new HexUtils.Hex(2, -1), new HexUtils.Hex(1, -2), new HexUtils.Hex(-2, 0)),
                HexUtils.distributeRingRandomly(2, 5, actual));
        assertEquals(expected.nextLong(), actual.nextLong(), "Discard exactly one nextInt(12)");

        Random otherStart = new Random() {
            @Override public int nextInt(int bound) {
                assertEquals(12, bound);
                return 11;
            }
        };
        assertEquals(List.of(new HexUtils.Hex(-2, 2), new HexUtils.Hex(0, 2),
                        new HexUtils.Hex(2, -1), new HexUtils.Hex(1, -2), new HexUtils.Hex(-2, 0)),
                HexUtils.distributeRingRandomly(2, 5, otherStart));
    }

    @Test
    void emptyDistributionStillConsumesOriginalRandomDraw() {
        Random actual = new Random(91);
        Random expected = new Random(91);
        expected.nextInt(12);
        assertTrue(HexUtils.distributeRingRandomly(2, 0, actual).isEmpty());
        assertEquals(expected.nextLong(), actual.nextLong());
    }

    @Test
    void endpointDistributionIsDistinctAndRejectsImpossibleInput() {
        var first = HexUtils.distributeRingRandomly(4, 8, new Random(91));
        var second = HexUtils.distributeRingRandomly(4, 8, new Random(91));
        assertEquals(first, second);
        assertEquals(8, new HashSet<>(first).size());
        assertTrue(HexUtils.distributeRingRandomly(2, 0, new Random(1)).isEmpty());
        assertThrows(IllegalArgumentException.class, () -> HexUtils.distributeRingRandomly(1, 7, new Random(1)));
        assertThrows(IllegalArgumentException.class, () -> HexUtils.distributeRingRandomly(1, -1, new Random(1)));
        assertThrows(IllegalArgumentException.class, () -> HexUtils.distributeRingRandomly(-1, 1, new Random(1)));
        assertThrows(IllegalArgumentException.class, () -> HexUtils.generateHexes(-1));
    }
}
