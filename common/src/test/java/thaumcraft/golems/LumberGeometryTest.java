package thaumcraft.golems;

import static org.junit.jupiter.api.Assertions.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import org.junit.jupiter.api.Test;
import thaumcraft.common.entities.golems.LumberGeometry;

/** Pure source arithmetic of the Lumber core (original AIHarvestLogs); no golem, world or registry. */
class LumberGeometryTest {
    @Test void ordinaryRangeGivesRadiusSixAndTwentyFourAttempts() {
        assertEquals(6, LumberGeometry.radius(16.0F));
        assertEquals(24, LumberGeometry.attempts(6));
        assertEquals(7, LumberGeometry.radius(19.0F), "ceil, not floor, of range / 3");
    }

    @Test void workTimeUsesAnchorHardnessAndGolemStrength() {
        // Straw (strength 0) on a hardness-2 log: 20 * 2.
        assertEquals(40, LumberGeometry.delay(0, 2.0F));
        // Strength 4 (Iron / Thaumium): (20 - 12) * 2.
        assertEquals(16, LumberGeometry.delay(4, 2.0F));
        // Wood 1 / Tallow 2 / Clay 2 / Stone 3.
        assertEquals(34, LumberGeometry.delay(1, 2.0F));
        assertEquals(28, LumberGeometry.delay(2, 2.0F));
        assertEquals(22, LumberGeometry.delay(3, 2.0F));
    }

    @Test void workTimeNeverDropsBelowFiveTicksNorBecomesNegative() {
        assertEquals(5, LumberGeometry.delay(4, 0.5F));
        assertEquals(5, LumberGeometry.delay(0, 0.0F));
        assertEquals(5, LumberGeometry.delay(10, 2.0F), "(20 - 30) * 2 is negative, clamped to the five-tick floor");
    }

    @Test void pulseIntervalIsDelayOverRoundedSixthAndAlwaysPositive() {
        assertEquals(5, LumberGeometry.pulseMod(40)); // 40 / round(6.67)=7
        assertEquals(5, LumberGeometry.pulseMod(16)); // 16 / round(2.67)=3
        assertEquals(5, LumberGeometry.pulseMod(5));  // 5 / round(0.83)=1
        assertEquals(5, LumberGeometry.pulseMod(21)); // 21 / round(3.5)=4 (integer quotient)
        assertEquals(6, LumberGeometry.pulseMod(20)); // 20 / round(3.33)=3 (integer quotient)
        for (int delay = 5; delay < 400; delay++) assertTrue(LumberGeometry.pulseMod(delay) >= 1, "delay " + delay);
    }

    @Test void scanCellDrawsXThenYThenZWithTheSourceBounds() {
        List<Integer> bounds = new ArrayList<>();
        int[] low = LumberGeometry.candidate(100, 64, -50, 6, bound -> { bounds.add(bound); return 0; });
        assertEquals(List.of(13, 7, 13), bounds, "X and Z span 2r+1 cells, Y spans r+1, drawn in X, Y, Z order");
        assertArrayEquals(new int[] {94, 61, -56}, low, "lowest draw: home - r, home - r/2, home - r");
        int[] high = LumberGeometry.candidate(100, 64, -50, 6, bound -> bound - 1);
        assertArrayEquals(new int[] {106, 67, -44}, high, "highest draw: home + r, home + r/2, home + r");
    }

    @Test void everyRandomScanCellStaysInsideTheHomeBox() {
        Random random = new Random(12345);
        for (int index = 0; index < 5000; index++) {
            int[] cell = LumberGeometry.candidate(0, 0, 0, 6, random::nextInt);
            assertTrue(Math.abs(cell[0]) <= 6 && Math.abs(cell[2]) <= 6 && cell[1] >= -3 && cell[1] <= 3, java.util.Arrays.toString(cell));
        }
    }

    @Test void followUpAnchorsMustStayInsideTheHomeBoxOnEveryAxis() {
        assertTrue(LumberGeometry.withinHomeBox(0, 64, 0, 6, 6, 70, -6));
        assertFalse(LumberGeometry.withinHomeBox(0, 64, 0, 6, 7, 64, 0));
        assertFalse(LumberGeometry.withinHomeBox(0, 64, 0, 6, 0, 71, 0));
        assertFalse(LumberGeometry.withinHomeBox(0, 64, 0, 6, 0, 64, -7));
        assertTrue(LumberGeometry.withinHomeBox(Integer.MIN_VALUE, 0, 0, 6, Integer.MIN_VALUE + 6, 0, 0), "no int overflow near the world edge");
    }
}
