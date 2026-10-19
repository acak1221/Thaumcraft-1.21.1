package thaumcraft.items;

import static org.junit.jupiter.api.Assertions.*;

import java.util.HashSet;
import java.util.Set;
import org.junit.jupiter.api.Test;
import thaumcraft.common.lib.world.StreamAxeGeometry;
import thaumcraft.common.lib.world.StreamAxeGeometry.Cell;

/** Pure arithmetic of the Axe of the Stream; no world, entity or registry is exercised. */
class StreamAxeGeometryTest {
    private static StreamAxeGeometry.Probe cells(Set<Cell> logs) {
        return (x, y, z) -> logs.contains(new Cell(x, y, z));
    }

    @Test void singleLogIsItsOwnEndpoint() {
        assertEquals(new Cell(5, 64, 5), StreamAxeGeometry.walk(5, 64, 5, cells(Set.of(new Cell(5, 64, 5)))));
    }

    @Test void straightTrunkWalksToTheTopWhateverLogWasHit() {
        Set<Cell> trunk = new HashSet<>();
        for (int y = 64; y < 80; y++) trunk.add(new Cell(0, y, 0));
        assertEquals(new Cell(0, 79, 0), StreamAxeGeometry.walk(0, 64, 0, cells(trunk)));
        assertEquals(new Cell(0, 79, 0), StreamAxeGeometry.walk(0, 70, 0, cells(trunk)));
    }

    @Test void bottomLogHitFromTheTopStillGoesFurtherOnlyAwayFromTheAnchor() {
        // From the top anchor the farthest log is the bottom one: distance, not height, decides.
        Set<Cell> trunk = new HashSet<>();
        for (int y = 64; y < 72; y++) trunk.add(new Cell(0, y, 0));
        assertEquals(new Cell(0, 64, 0), StreamAxeGeometry.walk(0, 71, 0, cells(trunk)));
    }

    @Test void walkCrossesGapsAndDiagonalsOfTwoButNotThree() {
        Set<Cell> branch = Set.of(new Cell(0, 0, 0), new Cell(2, 2, 2), new Cell(4, 4, 4));
        assertEquals(new Cell(4, 4, 4), StreamAxeGeometry.walk(0, 0, 0, cells(branch)));
        Set<Cell> tooFar = Set.of(new Cell(0, 0, 0), new Cell(3, 0, 0));
        assertEquals(new Cell(0, 0, 0), StreamAxeGeometry.walk(0, 0, 0, cells(tooFar)));
    }

    @Test void scanOrderPrefersLowestXThenHighestYThenLowestZ() {
        // Both candidates are strictly farther than the anchor; the first scanned one wins
        // and the walk continues from it, so the later (+x) log is not taken first.
        Set<Cell> forked = Set.of(new Cell(0, 0, 0), new Cell(-1, 0, 0), new Cell(1, 0, 0));
        assertEquals(new Cell(-1, 0, 0), StreamAxeGeometry.walk(0, 0, 0, cells(forked)));
        Set<Cell> verticalFork = Set.of(new Cell(0, 0, 0), new Cell(0, 1, 0), new Cell(0, -1, 0));
        assertEquals(new Cell(0, 1, 0), StreamAxeGeometry.walk(0, 0, 0, cells(verticalFork)));
    }

    @Test void distanceMustStrictlyIncreaseSoEqualDistanceDoesNotBackTrack() {
        Set<Cell> ring = Set.of(new Cell(0, 0, 0), new Cell(1, 0, 0), new Cell(-1, 0, 0));
        Cell end = StreamAxeGeometry.walk(0, 0, 0, cells(ring));
        assertEquals(new Cell(-1, 0, 0), end);
    }

    @Test void firstOutOfBoundsCellEndsTheWholeWalk() {
        // A log 24 away is allowed, but scanning toward the 25th stops the walk even if
        // another in-bounds candidate would follow in scan order (original quirk).
        Set<Cell> line = new HashSet<>();
        for (int x = 0; x <= 30; x++) line.add(new Cell(x, 0, 0));
        Cell end = StreamAxeGeometry.walk(0, 0, 0, cells(line));
        assertTrue(end.x() <= StreamAxeGeometry.MAX_X, "walk must not leave the anchor-relative box: " + end);
        assertEquals(StreamAxeGeometry.MAX_X, end.x());
    }

    @Test void verticalBoundIsFortyEight() {
        Set<Cell> column = new HashSet<>();
        for (int y = 0; y <= 70; y++) column.add(new Cell(0, y, 0));
        assertEquals(new Cell(0, StreamAxeGeometry.MAX_Y, 0), StreamAxeGeometry.walk(0, 0, 0, cells(column)));
    }

    @Test void vacuumPullsTowardThePlayerBy0Point3PerTickAndClampsEachComponent() {
        // Item 3 blocks east of the attractor: velocity -= 0.3 * unit(delta).
        double[] v = StreamAxeGeometry.pull(0, 0, 0, 3, 0, 0);
        assertArrayEquals(new double[] {-0.3, 0, 0}, v, 1.0E-12);
        // Accumulation is clamped to +-0.35 per component.
        double[] second = StreamAxeGeometry.pull(v[0], v[1], v[2], 3, 0, 0);
        assertEquals(-0.35, second[0], 1.0E-12);
        double[] opposite = StreamAxeGeometry.pull(0.34, 0, 0, -3, 0, 0);
        assertEquals(0.35, opposite[0], 1.0E-12);
        // A diagonal splits the 0.3 impulse by direction, not per axis.
        double[] diagonal = StreamAxeGeometry.pull(0, 0, 0, 3, 4, 0);
        assertEquals(-0.18, diagonal[0], 1.0E-12);
        assertEquals(-0.24, diagonal[1], 1.0E-12);
    }

    @Test void vacuumNeverProducesNaNForCoincidentOrNonFinitePositions() {
        assertArrayEquals(new double[] {0.1, 0.2, 0.3}, StreamAxeGeometry.pull(0.1, 0.2, 0.3, 0, 0, 0));
        assertArrayEquals(new double[] {0.1, 0.2, 0.3}, StreamAxeGeometry.pull(0.1, 0.2, 0.3, Double.POSITIVE_INFINITY, 0, 0));
        assertArrayEquals(new double[] {0.1, 0.2, 0.3}, StreamAxeGeometry.pull(0.1, 0.2, 0.3, Double.NaN, 0, 0));
    }

    @Test void followAcceleratesFromTwentiethToFullBlockPerTickThenArrives() {
        int counter = StreamAxeGeometry.FOLLOW_START;
        // 10 blocks away: first step is delta / (distance * 19) => 1/19 block per tick on the axis.
        var first = StreamAxeGeometry.follow(0, 0, 0, 10, 0, 0, counter, 0, 0, 0);
        assertFalse(first.arrived());
        assertEquals(19, first.counter());
        assertEquals(1.0 / 19.0, first.vx(), 1.0E-12);
        assertEquals(0, first.vy(), 1.0E-12);
        // The counter bottoms out at one, where the speed is the full unit vector per tick.
        counter = 1;
        var last = StreamAxeGeometry.follow(0, 0, 0, 10, 0, 0, counter, 0, 0, 0);
        assertEquals(1, last.counter());
        assertEquals(1.0, last.vx(), 1.0E-12);
    }

    @Test void followArrivesInsideHalfABlockKeepingATenthOfItsMotion() {
        var arrived = StreamAxeGeometry.follow(0, 0, 0, 0.3, 0.3, 0, 5, 0.5, -0.2, 0.1);
        assertTrue(arrived.arrived());
        assertEquals(0.05, arrived.vx(), 1.0E-12);
        assertEquals(-0.02, arrived.vy(), 1.0E-12);
        assertEquals(0.01, arrived.vz(), 1.0E-12);
    }
}
