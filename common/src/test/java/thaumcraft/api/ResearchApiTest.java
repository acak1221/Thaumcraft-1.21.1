package thaumcraft.api;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import net.minecraft.SharedConstants;
import net.minecraft.server.Bootstrap;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.api.aspects.AspectList;
import thaumcraft.api.research.ResearchCategories;
import thaumcraft.api.research.ResearchCategoriesCompat;
import thaumcraft.api.research.ResearchCategory;
import thaumcraft.api.research.ResearchEntry;
import thaumcraft.api.research.ResearchItem;
import thaumcraft.api.research.ResearchPage;

/** The research registry of the public API, read through the original shapes. */
class ResearchApiTest {
    @BeforeAll static void bootstrap() {
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
        ResearchCategories.loadNative();
    }

    @Test void theOriginalCorpusIsPublishedInThePublicRegistry() {
        var basics = ResearchCategories.getResearchList("BASICS");
        assertNotNull(basics);
        assertFalse(basics.research.isEmpty());
        assertEquals("THAUMATURGY", ResearchCategories.getResearch("FOCUSFIRE").category);
        assertTrue(ResearchCategories.getCategoryName("BASICS").length() > 0);
        assertEquals(6, ResearchCategories.researchCategories.size());
    }

    @Test void everyOriginalCategoryKeepsItsIconAndItsBackground() {
        // Original ConfigResearch.initCategories:67-73 registers the six categories
        // with the texture of their tab icon and the texture of their background.
        assertEquals("thaumcraft:textures/items/thaumonomiconcheat.png",
                ResearchCategories.getResearchList("BASICS").icon.toString());
        assertEquals("thaumcraft:textures/misc/r_thaumaturgy.png",
                ResearchCategories.getResearchList("THAUMATURGY").icon.toString());
        assertEquals("thaumcraft:textures/misc/r_crucible.png",
                ResearchCategories.getResearchList("ALCHEMY").icon.toString());
        assertEquals("thaumcraft:textures/misc/r_artifice.png",
                ResearchCategories.getResearchList("ARTIFICE").icon.toString());
        assertEquals("thaumcraft:textures/misc/r_golemancy.png",
                ResearchCategories.getResearchList("GOLEMANCY").icon.toString());
        assertEquals("thaumcraft:textures/misc/r_eldritch.png",
                ResearchCategories.getResearchList("ELDRITCH").icon.toString());
        assertEquals("thaumcraft:textures/gui/gui_researchback.png",
                ResearchCategories.getResearchList("THAUMATURGY").background.toString());
        assertEquals("thaumcraft:textures/gui/gui_researchbackeldritch.png",
                ResearchCategories.getResearchList("ELDRITCH").background.toString());
    }

    @Test void aResearchEntryCarriesTheOriginalData() {
        ResearchItem focus = ResearchCategories.getResearch("FOCUSFIRE");
        assertNotNull(focus);
        assertEquals("FOCUSFIRE", focus.key);
        assertNotNull(focus.definition(), "The original corpus entry is the source of the public view");
        assertTrue(focus.getPages() != null && focus.getPages().length > 0);
        assertEquals(3, focus.definition().aspectCost().get("ignis"));
        assertEquals(3, focus.definition().aspectCost().get("praecantatio"));
        assertEquals(2, focus.displayColumn);
        assertEquals(-2, focus.displayRow);
        assertTrue(focus.getName().length() > 0);
    }

    @Test void aResearchPageIsBuiltAndRead() {
        ResearchItem focus = ResearchCategories.getResearch("FOCUSFIRE");
        ResearchPage page = focus.getPages()[0];
        assertNotNull(page);
        assertEquals(ResearchPage.PageType.TEXT, page.type);
        assertNotNull(page.text);
        assertFalse(page.getTranslatedText().isEmpty());
    }

    @Test void theLegacyBridgeExposesTheSameEntry() {
        ResearchEntry entry = ResearchCategoriesCompat.getResearch("FOCUSFIRE");
        assertNotNull(entry);
        assertEquals("FOCUSFIRE", entry.getKey());
        assertEquals("THAUMATURGY", entry.getCategory());
        assertNotNull(entry.getIcons());
        assertEquals(1, entry.getParents().length);
    }

    @Test void anUnknownResearchKeyIsUnknown() {
        assertNull(ResearchCategories.getResearch("NOT_A_RESEARCH"));
        assertNull(ResearchCategoriesCompat.getResearch("NOT_A_RESEARCH"));
    }

    @Test void theCategoryFormulaReportsTheWeightedRoot() {
        AspectList formula = new AspectList().add(Aspect.EARTH, 10);
        ResearchCategory category = new ResearchCategory("TEST", null, formula, null, null);
        AspectList held = new AspectList().add(Aspect.EARTH, 40);
        // total = 1 * 1 * 40 * 10 / 10 = 40; the original reports the rounded square root.
        assertEquals(7, category.applyFormula(held));
        assertEquals(0, category.applyFormula(new AspectList()));
        assertEquals(0, category.applyFormula(held, 0.0D));
    }
}
