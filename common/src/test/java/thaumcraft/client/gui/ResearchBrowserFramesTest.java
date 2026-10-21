package thaumcraft.client.gui;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import thaumcraft.common.lib.research.ResearchCatalog;

/**
 * The frames of the research cells.
 *
 * <p>Original GuiResearchBrowser:387-399 draws four frames of gui_research.png: the round cell 54, the
 * hidden cell 86 or 230 and the ordinary cell 0 or 110, depending on whether the research is bought
 * instead of written on a note.</p>
 */
class ResearchBrowserFramesTest {
    @BeforeAll static void load() { ResearchCatalog.loadBundled(); }

    @Test void aRoundCellAlwaysUsesItsOwnFrame() {
        var round = ResearchCatalog.get("BASICTHAUMATURGY");
        assertEquals(54, GuiResearchBrowser.badgeFrame(round, false));
        assertEquals(54, GuiResearchBrowser.badgeFrame(round, true));
    }

    @Test void aHiddenCellUsesTheBoughtFrameOnlyWhenItIsBought() {
        var hidden = ResearchCatalog.get("FOCUSHELLBAT");
        assertEquals(86, GuiResearchBrowser.badgeFrame(hidden, false));
        assertEquals(230, GuiResearchBrowser.badgeFrame(hidden, true));
    }

    @Test void anOrdinaryCellUsesTheBoughtFrameOnlyWhenItIsBought() {
        var ordinary = ResearchCatalog.get("FOCUSFIRE");
        assertEquals(0, GuiResearchBrowser.badgeFrame(ordinary, false));
        assertEquals(110, GuiResearchBrowser.badgeFrame(ordinary, true));
    }
}
