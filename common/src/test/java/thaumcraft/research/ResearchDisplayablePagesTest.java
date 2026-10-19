package thaumcraft.common.lib.research;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/** Pure completion/page-gate coverage; native RecipeManager rendering needs client evidence separately. */
class ResearchDisplayablePagesTest {
    @Test
    void completedVisiblePagesKeepOrderAndOnlyResolvedRecipesReachTheBook() {
        var text = new ResearchDefinition.Page("text", "text.first", null, null, "original text");
        var table = new ResearchDefinition.Page("recipe_reference", null, "Table", null, "original recipe");
        var gated = new ResearchDefinition.Page("recipe_reference", null, "WardedJar", "EXTRA", "gated recipe");
        var unavailable = new ResearchDefinition.Page("recipe_reference", null, "RobeChest", null, "unported recipe");
        var concealed = new ResearchDefinition.Page("text", "text.concealed", null, "EXTRA", "concealed text");
        var expression = new ResearchDefinition.Page("expression", null, null, null, "rc.toArray(new IRecipe[0])");
        var research = new ResearchDefinition("ENTRY", "BASICS", 0, 0, 0, 0, Map.of(), Set.of(),
                List.of(), List.of(), List.of(), List.of(), List.of(), List.of(), null,
                List.of(text, table, gated, unavailable, concealed, expression), List.of());
        var attempted = new ArrayList<String>();
        java.util.function.Predicate<ResearchDefinition.Page> available = page -> {
            attempted.add(page.recipeKey());
            return Set.of("Table", "WardedJar").contains(page.recipeKey());
        };
        assertTrue(ResearchBrowserData.displayablePages(research, Set.of(), available).isEmpty());
        assertTrue(ResearchBrowserData.displayablePages(research, Set.of("@ENTRY"), available).isEmpty());
        assertTrue(attempted.isEmpty(), "Clues must not resolve recipe pages");
        assertEquals(List.of(text, table), ResearchBrowserData.displayablePages(research, Set.of("ENTRY"), available));
        assertEquals(List.of("Table", "RobeChest"), attempted, "Concealed pages must not reach the resolver");
        assertEquals(List.of(text, table, gated, concealed),
                ResearchBrowserData.displayablePages(research, Set.of("ENTRY", "EXTRA"), available));
        assertEquals(List.of(text), ResearchBrowserData.readablePages(research, Set.of("ENTRY")));
        assertEquals(List.of(text, concealed), ResearchBrowserData.readablePages(research, Set.of("ENTRY", "EXTRA")));
    }
}
