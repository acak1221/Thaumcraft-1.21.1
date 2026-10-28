package thaumcraft.foci;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashSet;
import java.util.Set;
import org.junit.jupiter.api.Test;
import thaumcraft.api.wands.IFocusPouch;
import thaumcraft.common.items.wands.FocusSelectionSources;

/**
 * The physical focus addresses of a pouch larger than the native eighteen cells (an {@link IFocusPouch} of an add-on, up to
 * {@link IFocusPouch#MAX_SELECTABLE_CELLS} cells): the extra cells have a band of their own after the native ones, so every
 * address the native pouch ever had is unchanged ({@link FocusSelectionSourcesTest} pins those).
 */
class FocusSelectionExtendedCellsTest {
    @Test void theCapIsTheApiConstantAndFitsTheBiggestKnownPouch() {
        assertEquals(IFocusPouch.MAX_SELECTABLE_CELLS, FocusSelectionSources.MAX_CELLS);
        assertEquals(128, FocusSelectionSources.MAX_CELLS);
        assertTrue(FocusSelectionSources.MAX_CELLS >= 13 * 9, "Thaumic Tinkerer's Bottomless Pouch has 13 x 9 = 117 cells");
        assertEquals(18, FocusSelectionSources.NATIVE_CELLS);
    }

    @Test void theExtendedCellsFollowTheNativeBandsAndKeepTheNativeAddressesAsTheyWere() {
        assertEquals(1017, FocusSelectionSources.inventoryPouch(0, 17), "the last native cell");
        assertEquals(3000, FocusSelectionSources.inventoryPouch(0, 18), "the first extra cell of the first main slot");
        assertEquals(3001, FocusSelectionSources.inventoryPouch(0, 19));
        assertEquals(3109, FocusSelectionSources.inventoryPouch(0, 127), "110 extra cells per slot");
        assertEquals(3110, FocusSelectionSources.inventoryPouch(1, 18));
        assertEquals(6959, FocusSelectionSources.inventoryPouch(35, 127), "the last extra cell of the main inventory");
        assertEquals(2071, FocusSelectionSources.wornPouch(3, 17));
        assertEquals(6960, FocusSelectionSources.wornPouch(0, 18), "the worn extra cells start where the main ones end");
        assertEquals(7399, FocusSelectionSources.wornPouch(3, 127));
    }

    @Test void everyCellOfEverySlotHasItsOwnAddressThatNamesItBack() {
        Set<Integer> seen = new HashSet<>();
        for (int slot = 0; slot < 36; slot++) {
            for (int cell = 0; cell < FocusSelectionSources.MAX_CELLS; cell++) {
                int address = FocusSelectionSources.inventoryPouch(slot, cell);
                assertTrue(seen.add(address), "inventory " + slot + "/" + cell);
                assertTrue(FocusSelectionSources.valid(address) && FocusSelectionSources.isInventoryPouch(address) && !FocusSelectionSources.isWornPouch(address));
                assertEquals(slot, FocusSelectionSources.outerSlot(address), "slot of " + address);
                assertEquals(cell, FocusSelectionSources.pouchSlot(address), "cell of " + address);
            }
        }
        for (int slot = 0; slot < 4; slot++) {
            for (int cell = 0; cell < FocusSelectionSources.MAX_CELLS; cell++) {
                int address = FocusSelectionSources.wornPouch(slot, cell);
                assertTrue(seen.add(address), "worn " + slot + "/" + cell);
                assertTrue(FocusSelectionSources.valid(address) && FocusSelectionSources.isWornPouch(address) && !FocusSelectionSources.isInventoryPouch(address));
                assertEquals(slot, FocusSelectionSources.outerSlot(address), "slot of " + address);
                assertEquals(cell, FocusSelectionSources.pouchSlot(address), "cell of " + address);
            }
        }
        assertEquals((36 + 4) * 128, seen.size());
    }

    @Test void theAddressesBetweenAndAfterTheBandsAreNotAddresses() {
        for (int address : new int[] {2072, 2500, 2999, 7400, 7401, 10000, 65535, 65536, Integer.MAX_VALUE, Integer.MIN_VALUE}) {
            assertFalse(FocusSelectionSources.valid(address), "address " + address);
            assertEquals(-1, FocusSelectionSources.outerSlot(address), "address " + address);
            assertEquals(-1, FocusSelectionSources.pouchSlot(address), "address " + address);
        }
    }

    @Test void aCellPastTheCapHasNoAddress() {
        assertThrows(IllegalArgumentException.class, () -> FocusSelectionSources.inventoryPouch(0, 128));
        assertThrows(IllegalArgumentException.class, () -> FocusSelectionSources.wornPouch(0, 128));
        assertThrows(IllegalArgumentException.class, () -> FocusSelectionSources.inventoryPouch(0, Integer.MAX_VALUE));
        assertThrows(IllegalArgumentException.class, () -> FocusSelectionSources.inventoryPouch(36, 18));
        assertThrows(IllegalArgumentException.class, () -> FocusSelectionSources.wornPouch(4, 18));
        assertThrows(IllegalArgumentException.class, () -> FocusSelectionSources.wornPouch(0, -1));
    }

    @Test void onlyTheFirstCapCellsOfALargerPouchAreAddressable() {
        assertEquals(18, FocusSelectionSources.addressable(new net.minecraft.world.item.ItemStack[18]));
        assertEquals(117, FocusSelectionSources.addressable(new net.minecraft.world.item.ItemStack[117]));
        assertEquals(128, FocusSelectionSources.addressable(new net.minecraft.world.item.ItemStack[128]));
        assertEquals(128, FocusSelectionSources.addressable(new net.minecraft.world.item.ItemStack[500]));
        assertEquals(0, FocusSelectionSources.addressable(new net.minecraft.world.item.ItemStack[0]));
    }
}
