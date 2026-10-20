package thaumcraft.common.lib.research;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class ResearchBrowserDataTest {
    @BeforeAll
    static void load() { ResearchCatalog.loadBundled(); }

    @Test
    void ordinaryEntriesRemainVisibleWhileConcealedEntriesNeedBothParentKinds() {
        ResearchDefinition ordinary = definition("ORDINARY", Set.of(), List.of(), 0, List.of());
        ResearchDefinition concealed = definition("CONCEALED", Set.of("Concealed"), List.of(), 0, List.of());
        assertTrue(ResearchBrowserData.isVisible(ordinary, Set.of()));
        assertFalse(ResearchBrowserData.isVisible(concealed, Set.of()));
        assertFalse(ResearchBrowserData.isVisible(concealed, Set.of("PARENT")));
        assertTrue(ResearchBrowserData.isVisible(concealed, Set.of("PARENT", "HIDDEN_PARENT")));
    }

    @Test
    void clueAndCompletionBypassHiddenLostAndConcealedButNeverVirtual() {
        for (Set<String> flags : List.of(Set.of("Hidden"), Set.of("Lost"), Set.of("Concealed"),
                Set.of("Hidden", "Lost", "Concealed"))) {
            ResearchDefinition research = definition("SECRET", flags, List.of(), 0, List.of());
            assertFalse(ResearchBrowserData.isVisible(research, Set.of()));
            assertTrue(ResearchBrowserData.isVisible(research, Set.of("@SECRET")));
            assertTrue(ResearchBrowserData.isVisible(research, Set.of("SECRET")));
        }
        assertFalse(ResearchBrowserData.isVisible(definition("VIRTUAL", Set.of("Virtual"), List.of(), 0, List.of()),
                Set.of("VIRTUAL", "@VIRTUAL")));
    }

    @Test
    void categoriesKeepOriginalOrderAndEldritchGate() {
        assertEquals(List.of("BASICS", "THAUMATURGY", "ALCHEMY", "ARTIFICE", "GOLEMANCY"),
                ResearchBrowserData.categories(Set.of()));
        assertEquals(List.of("BASICS", "THAUMATURGY", "ALCHEMY", "ARTIFICE", "GOLEMANCY", "ELDRITCH"),
                ResearchBrowserData.categories(Set.of("ELDRITCHMINOR")));
        assertTrue(ResearchBrowserData.entries("ELDRITCH", Set.of("VOIDMETAL")).isEmpty());
        assertTrue(ResearchBrowserData.entries("ELDRITCH", Set.of("ELDRITCHMINOR", "VOIDMETAL")).stream()
                .anyMatch(research -> research.key().equals("VOIDMETAL")));
    }

    @Test
    void closedConditionalFactsControlVisibilityAndReadablePages() {
        assertTrue(ResearchBrowserData.isVisible(ResearchCatalog.get("MIRROR"), Set.of("MIRROR", "@MIRROR")));
        ResearchConditions.setFeatures(true, false);
        try {
            assertFalse(ResearchBrowserData.isVisible(ResearchCatalog.get("MIRROR"), Set.of("MIRROR", "@MIRROR")));
            assertTrue(ResearchBrowserData.entries("ARTIFICE", Set.of()).stream()
                    .allMatch(research -> research.isEnabled() && !research.isVirtual()));
            assertTrue(ResearchBrowserData.readablePages(ResearchCatalog.get("MIRROR"), Set.of("MIRROR")).isEmpty());
        } finally { ResearchConditions.setFeatures(true, true); }
    }

    @Test
    void onlyCompletedResearchExposesTextPagesAndGatedTextNeedsItsResearch() {
        ResearchDefinition lamp = ResearchCatalog.get("ARCANELAMP");
        assertTrue(ResearchBrowserData.readablePages(lamp, Set.of()).isEmpty());
        assertTrue(ResearchBrowserData.readablePages(lamp, Set.of("@ARCANELAMP")).isEmpty());
        var basic = ResearchBrowserData.readablePages(lamp, Set.of("ARCANELAMP"));
        var gated = ResearchBrowserData.readablePages(lamp, Set.of("ARCANELAMP", "ARCANEBORE"));
        assertEquals(1, basic.size());
        assertEquals(2, gated.size());
        assertEquals("tc.research_page.ARCANELAMP.1", basic.getFirst().textKey());
        assertEquals("tc.research_page.ARCANELAMP.2", gated.getLast().textKey());
        assertTrue(gated.stream().allMatch(page -> "text".equals(page.kind())));
    }

    @Test
    void normalBookUnlocksOriginalSiblingsAndIsIdempotent() {
        PlayerKnowledge knowledge = new PlayerKnowledge();
        assertFalse(ResearchBrowserData.applyBookSiblingUnlocks(knowledge));
        knowledge.addResearch("DISTILESSENTIA");
        knowledge.addResearch("GOLEMSTRAW");
        assertTrue(ResearchBrowserData.applyBookSiblingUnlocks(knowledge));
        assertTrue(knowledge.getResearchComplete().containsAll(Set.of("JARLABEL", "COREGATHER", "GOLEMBELL")));
        assertFalse(ResearchBrowserData.applyBookSiblingUnlocks(knowledge));
        assertEquals(0, knowledge.getTotalWarp());
    }

    @Test
    void pinnedCorpusHasNoSiblingChainsSoTraversalOrderDoesNotChangeResult() {
        int links = 0;
        for (ResearchDefinition source : ResearchCatalog.all()) {
            for (String key : source.siblings()) {
                ResearchDefinition target = ResearchCatalog.get(key);
                assertNotNull(target);
                assertTrue(target.siblings().isEmpty(), source.key() + " -> " + key);
                assertTrue(target.conditions().isEmpty(), key);
                assertEquals(0, target.warp(), key);
                links++;
            }
        }
        assertEquals(3, links);
    }

    @Test
    void oneLivePassDoesNotRevisitEarlierSiblingOwnersAndAwardsWarpOnce() {
        ResearchDefinition earlier = definition("B", Set.of(), List.of("C"), 3, List.of());
        ResearchDefinition later = definition("A", Set.of(), List.of("B"), 0, List.of());
        ResearchDefinition last = definition("C", Set.of(), List.of(), 1, List.of());
        PlayerKnowledge knowledge = new PlayerKnowledge();
        knowledge.addResearch("A");
        List<ResearchDefinition> ordered = List.of(earlier, later, last);
        assertTrue(ResearchBrowserData.applyBookSiblingUnlocks(knowledge, ordered));
        assertTrue(knowledge.isResearchComplete("B"));
        assertFalse(knowledge.isResearchComplete("C"));
        // Sibling completion skips PARENT/HIDDEN_PARENT but keeps original warp split.
        assertEquals(2, knowledge.getWarpPerm());
        assertEquals(1, knowledge.getWarpSticky());
        assertEquals(3, knowledge.getWarpCounter());
        assertTrue(ResearchBrowserData.applyBookSiblingUnlocks(knowledge, ordered));
        assertTrue(knowledge.isResearchComplete("C"));
        assertEquals(3, knowledge.getWarpPerm());
        assertEquals(1, knowledge.getWarpSticky());
        assertEquals(4, knowledge.getWarpCounter());
        assertFalse(ResearchBrowserData.applyBookSiblingUnlocks(knowledge, ordered));
        assertEquals(4, knowledge.getTotalWarp());
    }

    private static ResearchDefinition definition(String key, Set<String> flags, List<String> siblings,
                                                 int warp, List<String> conditions) {
        return new ResearchDefinition(key, "BASICS", 0, 0, 1, warp, Map.of(), flags,
                List.of("PARENT"), List.of("HIDDEN_PARENT"), siblings,
                List.of(), List.of(), List.of(), null, List.of(), conditions);
    }
}
