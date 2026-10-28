package thaumcraft.client.gui;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;
import thaumcraft.common.items.wands.FocusSelectionSources;

/**
 * The order in which the focus ring lists equal-looking foci: worn pouches first by slot and cell, then the 36 main slots,
 * each slot's own focus before the cells of the pouch in it. The ring now carries pouches of up to 128 cells; the order of the
 * eighteen native cells must be the one it always had.
 */
class FocusSelectOrderTest {
    /** The order before the pouch cells grew past eighteen. */
    private static int originalOrder(int address) {
        if (FocusSelectionSources.isWornPouch(address)) return FocusSelectionSources.outerSlot(address) * 18 + FocusSelectionSources.pouchSlot(address);
        if (FocusSelectionSources.isInventoryPouch(address)) return 72 + FocusSelectionSources.outerSlot(address) * 19 + 1 + FocusSelectionSources.pouchSlot(address);
        return 72 + address * 19;
    }

    private static List<Integer> nativeAddresses() {
        List<Integer> out = new ArrayList<>();
        for (int slot = 0; slot < 36; slot++) out.add(slot);
        for (int slot = 0; slot < 36; slot++) for (int cell = 0; cell < 18; cell++) out.add(FocusSelectionSources.inventoryPouch(slot, cell));
        for (int slot = 0; slot < 4; slot++) for (int cell = 0; cell < 18; cell++) out.add(FocusSelectionSources.wornPouch(slot, cell));
        return out;
    }

    @Test void theNativeAddressesAreListedInTheOrderTheyAlwaysWere() {
        List<Integer> before = new ArrayList<>(nativeAddresses());
        List<Integer> after = new ArrayList<>(before);
        before.sort(Comparator.comparingInt(FocusSelectOrderTest::originalOrder));
        after.sort(Comparator.comparingInt(GuiFocusSelect::sourceOrder));
        assertEquals(before, after);
    }

    @Test void theExtraCellsComeAfterTheNativeOnesOfTheSameSlotAndBeforeTheNextSlot() {
        int wornLast17 = GuiFocusSelect.sourceOrder(FocusSelectionSources.wornPouch(0, 17));
        int worn18 = GuiFocusSelect.sourceOrder(FocusSelectionSources.wornPouch(0, 18));
        int worn127 = GuiFocusSelect.sourceOrder(FocusSelectionSources.wornPouch(0, 127));
        int nextWorn = GuiFocusSelect.sourceOrder(FocusSelectionSources.wornPouch(1, 0));
        assertTrue(wornLast17 < worn18 && worn18 < worn127 && worn127 < nextWorn);

        int lastWorn = GuiFocusSelect.sourceOrder(FocusSelectionSources.wornPouch(3, 127));
        int loose0 = GuiFocusSelect.sourceOrder(0);
        int inv0cell0 = GuiFocusSelect.sourceOrder(FocusSelectionSources.inventoryPouch(0, 0));
        int inv0cell17 = GuiFocusSelect.sourceOrder(FocusSelectionSources.inventoryPouch(0, 17));
        int inv0cell18 = GuiFocusSelect.sourceOrder(FocusSelectionSources.inventoryPouch(0, 18));
        int inv0cell127 = GuiFocusSelect.sourceOrder(FocusSelectionSources.inventoryPouch(0, 127));
        int loose1 = GuiFocusSelect.sourceOrder(1);
        assertTrue(lastWorn < loose0 && loose0 < inv0cell0 && inv0cell0 < inv0cell17 && inv0cell17 < inv0cell18
                && inv0cell18 < inv0cell127 && inv0cell127 < loose1);
    }

    @Test void noTwoAddressesShareAPlaceInTheOrder() {
        Set<Integer> seen = new HashSet<>();
        for (int slot = 0; slot < 36; slot++) assertTrue(seen.add(GuiFocusSelect.sourceOrder(slot)));
        for (int slot = 0; slot < 36; slot++) for (int cell = 0; cell < 128; cell++) assertTrue(seen.add(GuiFocusSelect.sourceOrder(FocusSelectionSources.inventoryPouch(slot, cell))));
        for (int slot = 0; slot < 4; slot++) for (int cell = 0; cell < 128; cell++) assertTrue(seen.add(GuiFocusSelect.sourceOrder(FocusSelectionSources.wornPouch(slot, cell))));
        assertEquals(36 + 40 * 128, seen.size());
    }
}
