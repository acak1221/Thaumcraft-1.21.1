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
}
