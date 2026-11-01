package thaumcraft.client.gui;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;

/** Oracle GuiResearchBrowser:287-291: normalized pan before integer division by two. */
class ResearchBrowserBackgroundTest {
    @Test void backgroundPanUsesCategoryBoundsNotRawPanPixels() {
        assertEquals(0, GuiResearchBrowser.backgroundOffset(-325, -325, 155, 288));
        assertEquals(144, GuiResearchBrowser.backgroundOffset(-85, -325, 155, 288));
        assertEquals(288, GuiResearchBrowser.backgroundOffset(155, -325, 155, 288));
        assertEquals(158, GuiResearchBrowser.backgroundOffset(-112, -352, 128, 316));
    }

    @Test void originalCastTruncatesBeforeTheBlitDividesByTwo() {
        assertEquals(142, GuiResearchBrowser.backgroundOffset(99, 0, 200, 288));
        assertEquals(156, GuiResearchBrowser.backgroundOffset(99, 0, 200, 316));
    }

    @Test void emptyAndOneColumnCategoriesKeepTheOriginalFixedCameraWithoutCrashing() {
        // The original extrema start at zero: an empty category and an entry at (0,0)
        // both produce -85..-112. The second ordered original check wins in that case.
        for (double pan : new double[] {-1000, -113, -85, 0, 1000}) {
            assertEquals(-113, GuiResearchBrowser.clampMap(pan, -85, -112));
            // Adding an entry in column 1 is still narrower than the pane's margins.
            assertEquals(-89, GuiResearchBrowser.clampMap(pan, -85, -88));
        }
    }

    @Test void wideCategoriesStillPanWithinTheirExistingBounds() {
        assertEquals(-325, GuiResearchBrowser.clampMap(-1000, -325, 155));
        assertEquals(12.5, GuiResearchBrowser.clampMap(12.5, -325, 155));
        assertEquals(154, GuiResearchBrowser.clampMap(1000, -325, 155));
    }
}
