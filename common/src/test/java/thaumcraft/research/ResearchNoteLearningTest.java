package thaumcraft.common.lib.research;

import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import static thaumcraft.common.lib.research.ResearchNoteLearning.Failure.*;

class ResearchNoteLearningTest {
    @BeforeAll static void load() { ResearchCatalog.loadBundled(); }

    @Test void solvingDoesNotLearnAndUnfinishedNoteCannotGrantResearch() {
        var knowledge = new PlayerKnowledge();
        knowledge.addResearch("CRUCIBLE");
        var before = knowledge.save();
        var result = ResearchNoteLearning.learn(knowledge, note("NITOR", false));
        assertEquals(UNFINISHED, result.failure());
        assertEquals(before, knowledge.save());
        assertTrue(result.unlocked().isEmpty());
    }

    @Test void transferredSolvedPrimaryNeedsNoReadersAspectDiscoveryOrPointPayment() {
        var knowledge = new PlayerKnowledge();
        knowledge.addResearch("CRUCIBLE");
        knowledge.setAspectPool("aer", 17);
        var aspects = knowledge.getAspectsDiscovered();
        assertFalse(knowledge.hasDiscoveredAspect("lux"));
        var result = ResearchNoteLearning.learn(knowledge, note("NITOR", true));
        assertTrue(result.success());
        assertEquals(Set.of("NITOR"), result.unlocked());
        assertEquals(aspects, knowledge.getAspectsDiscovered());
        assertThrows(UnsupportedOperationException.class, () -> result.unlocked().add("INFUSION"));
    }

    @Test void missingNormalParentRefusesWithoutChangingFullState() {
        var knowledge = new PlayerKnowledge();
        knowledge.setWarpCounter(91);
        var before = knowledge.save();
        assertEquals(MISSING_PREREQUISITES, ResearchNoteLearning.learn(knowledge, note("NITOR", true)).failure());
        assertEquals(before, knowledge.save());
    }

    @Test void hiddenParentsRemainMandatoryForSolvedNotes() {
        var knowledge = new PlayerKnowledge();
        knowledge.addResearch("FOCUSFIRE");
        knowledge.addResearch("@FOCUSHELLBAT");
        var before = knowledge.save();
        assertEquals(MISSING_PREREQUISITES, ResearchNoteLearning.learn(knowledge, note("FOCUSHELLBAT", true)).failure());
        assertEquals(before, knowledge.save());
        knowledge.addResearch("INFUSION");
        assertTrue(ResearchNoteLearning.learn(knowledge, note("FOCUSHELLBAT", true)).success());
    }

    @Test void hiddenSolvedNoteDoesNotDemandReadersOwnClue() {
        var knowledge = new PlayerKnowledge();
        knowledge.addResearch("INFUSION");
        assertFalse(knowledge.isResearchComplete("@JARBRAIN"));
        assertTrue(ResearchNoteLearning.learn(knowledge, note("JARBRAIN", true)).success());
        assertFalse(knowledge.isResearchComplete("@JARBRAIN"));
        assertTrue(knowledge.isResearchComplete("JARBRAIN"));
    }

    @Test void firstCompletionWarpUsesCeilingFloorAndFullCounterButReplayAddsNothing() {
        var knowledge = new PlayerKnowledge();
        knowledge.addResearch("INFUSION");
        knowledge.setWarpPerm(2);
        knowledge.setWarpSticky(4);
        knowledge.setWarpTemp(5);
        knowledge.setWarpCounter(99);
        assertTrue(ResearchNoteLearning.learn(knowledge, note("JARBRAIN", true)).success());
        assertEquals(4, knowledge.getWarpPerm());
        assertEquals(5, knowledge.getWarpSticky());
        assertEquals(5, knowledge.getWarpTemp());
        assertEquals(14, knowledge.getWarpCounter());
        var after = knowledge.save();
        assertEquals(ALREADY_COMPLETE, ResearchNoteLearning.learn(knowledge, note("JARBRAIN", true)).failure());
        assertEquals(after, knowledge.save());
    }

    @Test void originalDirectSiblingsCompleteForNormalPrimaryNote() {
        var knowledge = new PlayerKnowledge();
        knowledge.addResearch("NITOR");
        knowledge.addResearch("ALUMENTUM");
        var result = ResearchNoteLearning.learn(knowledge, note("DISTILESSENTIA", true));
        assertTrue(result.success());
        assertEquals(List.of("DISTILESSENTIA", "JARLABEL"), List.copyOf(result.unlocked()));
        assertFalse(knowledge.isResearchComplete("INFUSION"));
    }

    @Test void secondarySolvedNotesRemainUsableForOriginalHardModePath() {
        var knowledge = new PlayerKnowledge();
        var beforePools = knowledge.getAspectsDiscovered();
        assertTrue(ResearchNoteLearning.learn(knowledge, note("ENCHFABRIC", true)).success());
        assertEquals(beforePools, knowledge.getAspectsDiscovered());
    }

    @Test void unresolvedConditionalDataCannotGrantAndAspectlessVirtualHasNoValidNote() {
        for (String key : List.of("MIRROR", "MIRRORESSENTIA")) {
            var knowledge = new PlayerKnowledge();
            var before = knowledge.save();
            assertEquals(UNAVAILABLE, ResearchNoteLearning.learn(knowledge, note(key, true)).failure());
            assertEquals(before, knowledge.save());
        }
        assertThrows(IllegalArgumentException.class, () -> new ResearchNoteData("CAP_iron", 0, true, 0,
                List.of(new ResearchNoteData.Cell(0, 0, 1, "metallum"))));
    }

    private static ResearchNoteData note(String key, boolean complete) {
        // Trusted item-state fixture isolates learning from the separately tested
        // generator/solver. It does not assert this abbreviated grid was played.
        String tag = ResearchCatalog.get(key).aspectCost().keySet().iterator().next();
        return new ResearchNoteData(key, 0x123456, complete, 0,
                List.of(new ResearchNoteData.Cell(0, 0, 1, tag)));
    }
}
