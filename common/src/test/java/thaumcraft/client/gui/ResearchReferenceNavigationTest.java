package thaumcraft.client.gui;

import static org.junit.jupiter.api.Assertions.*;

import java.lang.reflect.Field;
import java.util.*;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.junit.jupiter.api.Test;
import thaumcraft.common.lib.research.ResearchDefinition;

class ResearchReferenceNavigationTest {
    @Test void sourcePageOrdinalsMapPastWrappedTextAndConcealedPages() {
        // Descriptor 0 wraps over three pages; descriptor 1 is concealed; descriptor 2 is the recipe.
        var layout = List.of(0, 0, 0, 2, 3);
        assertEquals(0, GuiResearchRecipe.spreadForDescriptor(layout, 0));
        assertEquals(-1, GuiResearchRecipe.spreadForDescriptor(layout, 1));
        assertEquals(2, GuiResearchRecipe.spreadForDescriptor(layout, 2));
        assertEquals(4, GuiResearchRecipe.spreadForDescriptor(layout, 3));
    }

    @Test void referencesRequireCompletedResearchAndAVisibleExistingPage() {
        var current = new GuiResearchRecipe(null, research("CURRENT", null));
        var target = research("TARGET", "CLUE");
        assertNull(GuiResearchRecipe.followReference(current, target, 0, Set.of("CURRENT")));
        assertNull(GuiResearchRecipe.followReference(current, target, 0, Set.of("CURRENT", "TARGET")));
        assertNull(GuiResearchRecipe.followReference(current, target, 1, Set.of("CURRENT", "TARGET", "CLUE")));
        assertNotNull(GuiResearchRecipe.followReference(current, target, 0, Set.of("CURRENT", "TARGET", "CLUE")));
    }

    @Test void historyReturnsToThePreviousSpreadAndSkipsRevokedResearch() throws Exception {
        Screen map = new Screen(Component.literal("map")) {};
        var first = new GuiResearchRecipe(map, research("FIRST", null), 6);
        var second = GuiResearchRecipe.followReference(first, research("SECOND", null), 0, Set.of("FIRST", "SECOND"));
        assertNotNull(second);
        assertSame(first, second.backDestination(Set.of("FIRST", "SECOND")));
        Field page = GuiResearchRecipe.class.getDeclaredField("page"); page.setAccessible(true);
        assertEquals(6, page.getInt(first));
        assertSame(map, second.backDestination(Set.of("SECOND")));
    }

    private static ResearchDefinition research(String key, String required) {
        return new ResearchDefinition(key, "BASICS", 0, 0, 1, 0, Map.of(), Set.of(), List.of(), List.of(), List.of(),
                List.of(), List.of(), List.of(), null,
                List.of(new ResearchDefinition.Page("recipe_reference", null, "TestRecipe", required, null)), List.of());
    }
}
