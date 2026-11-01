package thaumcraft.client.gui;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class ConfigTabLayoutTest {
    @Test void allSixCategoriesStayReachableAtTheMinimumVanillaGuiWidth() {
        int[] widths = {110, 110, 110, 110, 110, 110};
        int x = ThaumcraftConfigScreen.layoutTabs(320, widths);
        assertEquals(10, x);
        assertArrayEquals(new int[] {50, 50, 50, 50, 50, 50}, widths);
        for (int width : widths) {
            assertTrue(x >= 10 && x + width <= 310, "every tab stays inside the screen margins");
            x += width;
        }
        assertEquals(310, x);
    }

    @Test void aWideScreenCentersTheWholeRowWithoutChangingPreferredWidths() {
        int[] widths = {50, 60, 65, 80, 55, 50};
        assertEquals(247, ThaumcraftConfigScreen.layoutTabs(854, widths));
        assertArrayEquals(new int[] {50, 60, 65, 80, 55, 50}, widths);
    }
}
