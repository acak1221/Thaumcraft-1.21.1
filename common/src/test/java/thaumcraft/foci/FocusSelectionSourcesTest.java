package thaumcraft.foci;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import thaumcraft.common.items.wands.FocusSelectionSources;

/** The physical focus addresses: held focus, the 36 inventory slots, then 18 cells in each inventory pouch and each of 4 worn pouches. */
class FocusSelectionSourcesTest {
    @Test void theThreeRangesStartAndEndWhereTheNativeSelectorExpectsThem() {
        assertEquals(1000, FocusSelectionSources.inventoryPouch(0, 0));
        assertEquals(1647, FocusSelectionSources.inventoryPouch(35, 17));
        assertEquals(2000, FocusSelectionSources.wornPouch(0, 0));
        assertEquals(2071, FocusSelectionSources.wornPouch(3, 17));
        assertEquals(1018, FocusSelectionSources.inventoryPouch(1, 0), "eighteen cells per pouch");
    }

    @Test void validIsTrueOnlyForTheHeldFocusTheInventorySlotsAndThePouchCells() {
        for (int address : new int[] {-1, 0, 35, 1000, 1647, 2000, 2071}) assertTrue(FocusSelectionSources.valid(address), "address " + address);
        for (int address : new int[] {-2, 36, 999, 1648, 1999, 2072, Integer.MIN_VALUE, Integer.MAX_VALUE}) assertFalse(FocusSelectionSources.valid(address), "address " + address);
    }

    @Test void theSlotAndCellComeBackOutOfEveryPouchAddress() {
        for (int slot = 0; slot < 36; slot++) {
            for (int cell = 0; cell < 18; cell++) {
                int address = FocusSelectionSources.inventoryPouch(slot, cell);
                assertTrue(FocusSelectionSources.isInventoryPouch(address));
                assertFalse(FocusSelectionSources.isWornPouch(address));
                assertEquals(slot, FocusSelectionSources.outerSlot(address));
                assertEquals(cell, FocusSelectionSources.pouchSlot(address));
            }
        }
        for (int slot = 0; slot < 4; slot++) {
            for (int cell = 0; cell < 18; cell++) {
                int address = FocusSelectionSources.wornPouch(slot, cell);
                assertTrue(FocusSelectionSources.isWornPouch(address));
                assertFalse(FocusSelectionSources.isInventoryPouch(address));
                assertEquals(slot, FocusSelectionSources.outerSlot(address));
                assertEquals(cell, FocusSelectionSources.pouchSlot(address));
            }
        }
    }

    @Test void addressesThatAreNotPouchCellsHaveNoOuterSlotOrCell() {
        for (int address : new int[] {-1, 0, 35, 999, 1648, 1999, 2072}) {
            assertEquals(-1, FocusSelectionSources.outerSlot(address), "address " + address);
            assertEquals(-1, FocusSelectionSources.pouchSlot(address), "address " + address);
        }
    }

    @Test void everyPouchCellHasItsOwnAddress() {
        java.util.Set<Integer> seen = new java.util.HashSet<>();
        for (int slot = 0; slot < 36; slot++) for (int cell = 0; cell < 18; cell++) assertTrue(seen.add(FocusSelectionSources.inventoryPouch(slot, cell)));
        for (int slot = 0; slot < 4; slot++) for (int cell = 0; cell < 18; cell++) assertTrue(seen.add(FocusSelectionSources.wornPouch(slot, cell)));
        assertEquals(36 * 18 + 4 * 18, seen.size());
        for (int address : seen) assertTrue(FocusSelectionSources.valid(address));
    }

    @Test void aPouchSlotOrCellOutsideTheirRangeIsRefused() {
        for (int[] bad : new int[][] {{-1, 0}, {36, 0}, {0, -1}, {0, 18}}) {
            assertThrows(IllegalArgumentException.class, () -> FocusSelectionSources.inventoryPouch(bad[0], bad[1]), bad[0] + "," + bad[1]);
        }
        for (int[] bad : new int[][] {{-1, 0}, {4, 0}, {0, -1}, {0, 18}}) {
            assertThrows(IllegalArgumentException.class, () -> FocusSelectionSources.wornPouch(bad[0], bad[1]), bad[0] + "," + bad[1]);
        }
    }
}
