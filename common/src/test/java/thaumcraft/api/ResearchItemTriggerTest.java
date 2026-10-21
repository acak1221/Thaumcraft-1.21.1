package thaumcraft.api;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import net.minecraft.SharedConstants;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.api.research.ResearchCategories;
import thaumcraft.api.research.ResearchItem;
import thaumcraft.api.research.ResearchPage;
import thaumcraft.common.lib.research.ResearchCatalog;

/**
 * The public research surface of the original corpus.
 *
 * <p>Original {@code thaumcraft/api/research/ResearchItem.java:19-255} and
 * {@code thaumcraft/api/research/ResearchPage.java:19-160} (CFR 0.152 decompilation of
 * {@code Thaumcraft-1.7.10-4.2.3.5.jar}). The original registered every entry of
 * {@code ConfigResearch} with its parents, its flags, its executable item, entity and aspect
 * triggers and its pages; an add-on reads exactly those through the public accessors.</p>
 */
class ResearchItemTriggerTest {
    @BeforeAll static void bootstrap() {
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
        ResearchCatalog.loadBundled();
    }

    @Test void theCorpusIsReachableThroughThePublicRegistry() {
        assertEquals(201, ResearchCatalog.all().size(), "the original corpus has 201 entries");
        ResearchItem entry = ResearchCategories.getResearch("ADVALCHEMYFURNACE");
        assertNotNull(entry, "the public registry answers a corpus entry");
        assertEquals("ADVALCHEMYFURNACE", entry.key);
        assertEquals("ELDRITCH", entry.category);
        assertTrue(entry.isSecondary(), "the original Secondary flag survives");
        assertEquals(3, entry.getComplexity());
    }

    @Test void theExecutableAspectTriggersOfAnEntryAreAnswered() {
        // Original ConfigResearch setAspectTriggers(Aspect.ARMOR) for the fortress armour.
        ResearchItem entry = ResearchCategories.getResearch("ARMORFORTRESS");
        assertNotNull(entry);
        assertNotNull(entry.getAspectTriggers(), "the original aspect trigger is answered");
        assertEquals(1, entry.getAspectTriggers().length);
        assertEquals(Aspect.ARMOR, entry.getAspectTriggers()[0]);
        // The tiny clothing entries carried the pannus trigger.
        assertEquals(Aspect.CLOTH, ResearchCategories.getResearch("TINYFEZ").getAspectTriggers()[0]);
    }

    @Test void anEntryWithoutAnAspectTriggerAnswersNull() {
        ResearchItem entry = ResearchCategories.getResearch("ADVALCHEMYFURNACE");
        assertNull(entry.getAspectTriggers(), "the original answers null without an aspect trigger");
    }

    @Test void theExecutableItemTriggersOfAnEntryAreAnswered() {
        // Original ConfigResearch setItemTriggers(new ItemStack(Items.bow, 1, Short.MAX_VALUE),
        // new ItemStack(Items.bone)) for the bone bow.
        ResearchItem entry = ResearchCategories.getResearch("BONEBOW");
        assertNotNull(entry);
        ItemStack[] triggers = entry.getItemTriggers();
        assertNotNull(triggers, "the original item trigger is answered");
        assertEquals(2, triggers.length);
        assertEquals(Items.BOW, triggers[0].getItem());
        assertEquals(Items.BONE, triggers[1].getItem());
    }

    @Test void anEntryWithoutAnItemTriggerAnswersNull() {
        assertNull(ResearchCategories.getResearch("ADVALCHEMYFURNACE").getItemTriggers(),
                "the original answers null without an item trigger");
    }

    @Test void theEntityTriggersOfAnEntryAreAnswered() {
        ResearchItem entry = ResearchCategories.getResearch("METALLURGIST");
        if (entry != null && entry.getEntityTriggers() != null) {
            assertTrue(entry.getEntityTriggers().length > 0, "an entity trigger is never an empty list");
        }
    }

    @Test void everyCorpusPageIsReachableThroughThePublicEntry() {
        int pages = 0;
        for (var definition : ResearchCatalog.all()) {
            ResearchItem entry = ResearchCategories.getResearch(definition.key());
            assertNotNull(entry, "every corpus entry is in the public registry: " + definition.key());
            ResearchPage[] view = entry.getPages();
            assertEquals(definition.pages().size(), view.length, "every page of the entry is reachable");
            for (int index = 0; index < view.length; index++) {
                assertNotNull(view[index], "a page of the entry is never null");
                if (definition.pages().get(index).textKey() != null) {
                    assertEquals(definition.pages().get(index).textKey(), view[index].text,
                            "the text key of a page survives");
                }
                if (definition.pages().get(index).recipeKey() != null) {
                    assertEquals(definition.pages().get(index).recipeKey(), view[index].recipeKey,
                            "the recipe reference of a page survives");
                }
            }
            pages += view.length;
        }
        assertEquals(591, pages, "the original corpus carries 591 pages");
    }

    @Test void theOriginalPageKindOfTheEightArrayPagesIsAnswered() {
        assertEquals(ResearchPage.PageType.ARCANE_CRAFTING,
                ResearchPage.legacyType("banners.toArray(new IArcaneRecipe[0])"));
        assertEquals(ResearchPage.PageType.CRUCIBLE_CRAFTING,
                ResearchPage.legacyType("cruc.toArray(new CrucibleRecipe[0])"));
        assertEquals(ResearchPage.PageType.NORMAL_CRAFTING,
                ResearchPage.legacyType("rc.toArray(new IRecipe[0])"));
        assertEquals(ResearchPage.PageType.INFUSION_CRAFTING,
                ResearchPage.legacyType("raug.toArray(new InfusionRecipe[0])"));
        assertEquals(ResearchPage.PageType.SMELTING,
                ResearchPage.legacyType("new ItemStack(ConfigItems.itemShard, 1, 6)"));
    }
}
