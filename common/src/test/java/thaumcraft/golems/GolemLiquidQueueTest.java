package thaumcraft.golems;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;
import net.minecraft.core.BlockPos;
import org.junit.jupiter.api.Test;
import thaumcraft.common.items.golems.GolemLiquidQueue;

/** Pure rules of the Entropy queue of the Decanting golem (original AILiquidGather:168-183, 255-300). */
class GolemLiquidQueueTest {
    private static final BlockPos ORIGIN = new BlockPos(100, 64, 100);

    private static Predicate<BlockPos> cells(Set<BlockPos> set) { return set::contains; }

    private static Set<BlockPos> line(int length, int dx, int dy, int dz) {
        Set<BlockPos> cells = new HashSet<>();
        for (int step = 0; step < length; step++) cells.add(ORIGIN.offset(dx * step, dy * step, dz * step));
        return cells;
    }

    @Test void theFarthestSourceIsDrainedFirstAndTheMarkedBlockLast() {
        var water = line(8, 1, 0, 0);
        var queue = GolemLiquidQueue.build(ORIGIN, 16f, cells(water), cells(water));
        var expected = new ArrayList<BlockPos>();
        for (int step = 7; step >= 0; step--) expected.add(ORIGIN.offset(step, 0, 0));
        assertEquals(expected, queue, "7, 6, ... 1, then the marked block (distance 0)");
    }

    @Test void theMarkedBlockIsQueuedOnceDespiteBeingReachedFromEveryNeighbour() {
        var water = new HashSet<BlockPos>();
        for (int dx = -1; dx <= 1; dx++) for (int dz = -1; dz <= 1; dz++) water.add(ORIGIN.offset(dx, 0, dz));
        var queue = GolemLiquidQueue.build(ORIGIN, 16f, cells(water), cells(water));
        assertEquals(9, queue.size(), "nine distinct cells, no duplicates (the original queued one per visited neighbour)");
        assertEquals(9, new HashSet<>(queue).size());
        assertEquals(ORIGIN, queue.get(queue.size() - 1));
    }

    @Test void equalDistancesKeepTheOriginalDepthFirstOrder() {
        var water = new HashSet<BlockPos>();
        water.add(ORIGIN);
        BlockPos[] axes = {ORIGIN.offset(-1, 0, 0), ORIGIN.offset(0, -1, 0), ORIGIN.offset(0, 0, -1), ORIGIN.offset(0, 1, 0), ORIGIN.offset(0, 0, 1), ORIGIN.offset(1, 0, 0)};
        water.addAll(List.of(axes));
        var queue = GolemLiquidQueue.build(ORIGIN, 16f, cells(water), cells(water));
        var expected = new ArrayList<>(List.of(axes));
        expected.add(ORIGIN);
        // The expected order is that of the original recursion (checked against an independent recursive emulation of
        // AILiquidGather.getConnectedFluidBlocks): the walk descends into each cell at once, so (0,1,0) is found before (0,0,1).
        assertEquals(expected, queue, "ties keep the original depth-first discovery order");
    }

    @Test void diagonalNeighboursConnectButAGapOfTwoDoesNot() {
        var water = new HashSet<BlockPos>();
        water.add(ORIGIN);
        water.add(ORIGIN.offset(1, 1, 1));   // touches only by a corner
        water.add(ORIGIN.offset(2, 2, 2));   // chains on by another corner
        water.add(ORIGIN.offset(5, 0, 0));   // a gap of three empty cells
        water.add(ORIGIN.offset(0, 0, -3));  // a gap of two empty cells
        var queue = GolemLiquidQueue.build(ORIGIN, 16f, cells(water), cells(water));
        assertEquals(List.of(ORIGIN.offset(2, 2, 2), ORIGIN.offset(1, 1, 1), ORIGIN), queue);
    }

    @Test void flowingWaterConnectsButIsNeverQueued() {
        var connecting = line(6, 1, 0, 0);
        var sources = new HashSet<>(connecting);
        sources.remove(ORIGIN.offset(2, 0, 0)); // a flowing cell between two sources
        var queue = GolemLiquidQueue.build(ORIGIN, 16f, cells(connecting), cells(sources));
        assertFalse(queue.contains(ORIGIN.offset(2, 0, 0)));
        assertEquals(ORIGIN.offset(5, 0, 0), queue.get(0), "the walk passes through the flowing cell to the far end");
        assertEquals(5, queue.size());
    }

    @Test void onlyCellsWithinTheRangeAreVisitedAndTheWalkDoesNotLeaveItAndComeBack() {
        var water = line(30, 1, 0, 0);
        var queue = GolemLiquidQueue.build(ORIGIN, 16f, cells(water), cells(water));
        assertEquals(ORIGIN.offset(16, 0, 0), queue.get(0), "exactly range away is inside (squared distance 256 is not above 256)");
        assertEquals(17, queue.size());
        assertFalse(queue.contains(ORIGIN.offset(17, 0, 0)));
        // A ring of water that bends out of range and returns is cut where it leaves the range: no cell beyond is reached through it.
        var bend = new HashSet<BlockPos>();
        for (int x = 0; x <= 20; x++) bend.add(ORIGIN.offset(x, 0, 0));
        for (int z = 1; z <= 6; z++) bend.add(ORIGIN.offset(20, 0, z));
        for (int x = 19; x >= 14; x--) bend.add(ORIGIN.offset(x, 0, 6));
        var cut = GolemLiquidQueue.build(ORIGIN, 16f, cells(bend), cells(bend));
        assertFalse(cut.contains(ORIGIN.offset(14, 0, 6)), "(14,0,6) is in range but only reachable through cells out of range");
    }

    @Test void theRangeIsMeasuredFromTheMarkedBlockInThreeDimensions() {
        var column = line(12, 0, 1, 0);
        var queue = GolemLiquidQueue.build(ORIGIN, 8f, cells(column), cells(column));
        assertEquals(ORIGIN.offset(0, 8, 0), queue.get(0));
        assertEquals(9, queue.size());
        assertTrue(GolemLiquidQueue.inRange(ORIGIN, ORIGIN.offset(3, 4, 0), 5f), "3-4-5: the boundary is inclusive");
        assertFalse(GolemLiquidQueue.inRange(ORIGIN, ORIGIN.offset(3, 4, 1), 5f));
        assertTrue(GolemLiquidQueue.inRange(ORIGIN, ORIGIN, 0f));
    }

    @Test void aMarkedBlockThatIsNotASourceIsNotQueuedButStillAnchorsTheWalk() {
        var water = line(5, 1, 0, 0);
        var sources = new HashSet<>(water);
        sources.remove(ORIGIN);
        var queue = GolemLiquidQueue.build(ORIGIN, 16f, cells(water), cells(sources));
        assertEquals(List.of(ORIGIN.offset(4, 0, 0), ORIGIN.offset(3, 0, 0), ORIGIN.offset(2, 0, 0), ORIGIN.offset(1, 0, 0)), queue);
    }

    @Test void nothingConnectedMeansAnEmptyQueue() {
        assertTrue(GolemLiquidQueue.build(ORIGIN, 16f, pos -> false, pos -> true).isEmpty());
        var lone = Set.of(ORIGIN);
        assertTrue(GolemLiquidQueue.build(ORIGIN, 16f, cells(lone), cells(lone)).isEmpty(),
                "a lone marked block has no neighbour to reach it, so the golem falls back to draining it directly");
    }

    @Test void theWalkIsCappedSoAHugeLakeCannotStallTheServer() {
        var queue = GolemLiquidQueue.build(ORIGIN, 40f, pos -> true, pos -> true);
        assertTrue(queue.size() <= GolemLiquidQueue.MAX_VISITED, "queue " + queue.size());
        assertTrue(queue.size() > GolemLiquidQueue.MAX_VISITED / 2, "but a large part of it is still produced");
    }
}
