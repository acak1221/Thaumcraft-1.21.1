package thaumcraft.client.gui;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

/** The matching rules of the Thaumonomicon search adopted from TC4Tweaks. */
class ResearchSearchTest {
    @Test void aQueryMatchesIgnoringCaseSpacesAndFormatting() {
        String query = ResearchSearch.normalize("  GoGGles ");
        assertEquals("goggles", query);
        assertTrue(ResearchSearch.matches("Goggles of Revealing", query));
        assertTrue(ResearchSearch.matches("§6Goggles§r of Revealing", query));
        assertFalse(ResearchSearch.matches("Thaumometer", query));
    }

    @Test void russianNamesMatchWithAndWithoutTheDiaeresis() {
        assertTrue(ResearchSearch.matches("Жёлудь мироздания", ResearchSearch.normalize("желудь")));
        assertTrue(ResearchSearch.matches("Очки откровения", ResearchSearch.normalize("ОЧКИ")));
        assertTrue(ResearchSearch.matches("Café", ResearchSearch.normalize("cafe")));
    }

    @Test void anEmptyQueryMatchesNothing() {
        assertFalse(ResearchSearch.matches("anything", ResearchSearch.normalize("   ")));
        assertEquals("", ResearchSearch.normalize(null));
    }
}
