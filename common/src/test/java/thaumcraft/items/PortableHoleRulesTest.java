package thaumcraft.items;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashSet;
import java.util.Set;
import org.junit.jupiter.api.Test;
import thaumcraft.common.items.wands.foci.PortableHoleRules;

/** The numbers of the Portable Hole (original ItemFocusPortableHole and TileHole) without a world. */
class PortableHoleRulesTest {
    @Test void aDepthCostsTenCentivisABlockButNeverLessThanOneBlocksWorth() {
        assertEquals(10, PortableHoleRules.cost(0), "the original merges, keeping the larger: depth 0 still costs 10");
        assertEquals(10, PortableHoleRules.cost(1));
        assertEquals(20, PortableHoleRules.cost(2));
        assertEquals(330, PortableHoleRules.cost(33));
        assertEquals(410, PortableHoleRules.cost(41));
    }

    @Test void reachAndDurationGrowWithTheirUpgrades() {
        assertEquals(33, PortableHoleRules.maxDepth(0));
        assertEquals(41, PortableHoleRules.maxDepth(1));
        assertEquals(73, PortableHoleRules.maxDepth(5));
        assertEquals(120, PortableHoleRules.duration(0));
        assertEquals(180, PortableHoleRules.duration(1));
        assertEquals(420, PortableHoleRules.duration(5));
        assertEquals(1, PortableHoleRules.chainLength(0));
        assertEquals(34, PortableHoleRules.chainLength(33));
    }

    @Test void aHoleRunsIntoTheFaceThatWasClicked() {
        assertArrayEquals(new int[]{0, 1, 0}, PortableHoleRules.step(0), "the bottom face: up");
        assertArrayEquals(new int[]{0, -1, 0}, PortableHoleRules.step(1), "the top face: down");
        assertArrayEquals(new int[]{0, 0, 1}, PortableHoleRules.step(2), "the north face: south");
        assertArrayEquals(new int[]{0, 0, -1}, PortableHoleRules.step(3));
        assertArrayEquals(new int[]{1, 0, 0}, PortableHoleRules.step(4), "the west face: east");
        assertArrayEquals(new int[]{-1, 0, 0}, PortableHoleRules.step(5));
        assertThrows(IllegalArgumentException.class, () -> PortableHoleRules.step(6));
        assertThrows(IllegalArgumentException.class, () -> PortableHoleRules.step(-1));
    }

    @Test void theRingIsTheEightCellsAcrossTheDirection() {
        for (int side = 0; side < 6; side++) {
            int[][] ring = PortableHoleRules.ring(side);
            assertEquals(8, ring.length);
            Set<String> cells = new HashSet<>();
            int axis = side < 2 ? 1 : side < 4 ? 2 : 0;
            for (int[] cell : ring) {
                assertEquals(0, cell[axis], "nothing along the hole itself, side " + side);
                assertFalse(cell[0] == 0 && cell[1] == 0 && cell[2] == 0, "never the centre");
                for (int value : cell) assertTrue(value >= -1 && value <= 1);
                assertTrue(cells.add(cell[0] + "," + cell[1] + "," + cell[2]), "all different");
            }
        }
        assertArrayEquals(new int[]{-1, 0, -1}, PortableHoleRules.ring(0)[0], "the original loop starts at a = 0");
        assertArrayEquals(new int[]{1, 0, 1}, PortableHoleRules.ring(1)[7]);
        assertArrayEquals(new int[]{-1, -1, 0}, PortableHoleRules.ring(2)[0]);
        assertArrayEquals(new int[]{0, -1, -1}, PortableHoleRules.ring(4)[0]);
    }

    @Test void onlyTheFirstTickOfAChainBlockSpreadsAndOnlyWhileMoreIsLeft() {
        assertTrue(PortableHoleRules.spreads(0, 2, 4));
        assertFalse(PortableHoleRules.spreads(1, 13, 4), "a second tick does not");
        assertFalse(PortableHoleRules.spreads(0, 1, 4), "the last block of the chain does not");
        assertFalse(PortableHoleRules.spreads(0, 13, -1), "a ring block does not");
    }

    @Test void theOuterLandsRefuseIt() {
        assertTrue(PortableHoleRules.refusedIn("thaumcraft:outer_lands"));
        assertFalse(PortableHoleRules.refusedIn("minecraft:overworld"));
    }
}
