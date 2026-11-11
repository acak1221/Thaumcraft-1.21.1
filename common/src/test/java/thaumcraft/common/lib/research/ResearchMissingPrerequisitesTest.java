package thaumcraft.common.lib.research;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/**
 * The prerequisites the tooltip of a locked research cell lists (the port's own addition): a lacking
 * research the book already shows is named, one it does not show yet is only counted.
 */
class ResearchMissingPrerequisitesTest {
    @BeforeAll static void load() { ResearchCatalog.loadBundled(); }

    /** FOCUSHELLBAT needs FOCUSFIRE (an ordinary cell) and INFUSION (concealed until DISTILESSENTIA). */
    @Test void aShownPrerequisiteIsNamedAndAConcealedOneIsOnlyCounted() {
        var missing = ResearchBrowserData.missingPrerequisites(ResearchCatalog.get("FOCUSHELLBAT"), Set.of("BASICTHAUMATURGY"));
        assertEquals(List.of("FOCUSFIRE"), missing.shown().stream().map(ResearchDefinition::key).toList());
        assertEquals(1, missing.hidden());
    }

    @Test void aConcealedPrerequisiteIsNamedOnceItsOwnPrerequisitesAreComplete() {
        var missing = ResearchBrowserData.missingPrerequisites(ResearchCatalog.get("FOCUSHELLBAT"),
                Set.of("BASICTHAUMATURGY", "FOCUSFIRE", "DISTILESSENTIA"));
        assertEquals(List.of("INFUSION"), missing.shown().stream().map(ResearchDefinition::key).toList());
        assertEquals(0, missing.hidden());
    }

    @Test void nothingIsListedOnceEveryPrerequisiteIsComplete() {
        assertTrue(ResearchBrowserData.missingPrerequisites(ResearchCatalog.get("FOCUSHELLBAT"), Set.of("FOCUSFIRE", "INFUSION")).isEmpty());
    }
}
