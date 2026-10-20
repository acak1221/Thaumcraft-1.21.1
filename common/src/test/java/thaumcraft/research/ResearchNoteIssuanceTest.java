package thaumcraft.common.lib.research;

import java.util.List;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import thaumcraft.common.lib.research.ResearchNoteData.Cell;
import static org.junit.jupiter.api.Assertions.*;
import static thaumcraft.common.lib.research.ResearchNoteIssuance.Failure.*;

/** Pure admission checks; native inventory/RNG/delivery are tested in real loaders. */
class ResearchNoteIssuanceTest {
    @BeforeAll static void load() { ResearchCatalog.loadBundled(); }

    @Test void primaryIssuanceNeedsParentsButNoReadersAspectDiscoveryOrResearchPointPrice() {
        var knowledge = new PlayerKnowledge();
        assertEquals(MISSING_PREREQUISITES, ResearchNoteIssuance.validate(knowledge, "NITOR"));
        knowledge.addResearch("CRUCIBLE");
        var before = knowledge.save();
        assertFalse(knowledge.hasDiscoveredAspect("lux"));
        assertEquals(NONE, ResearchNoteIssuance.validate(knowledge, "NITOR"));
        assertEquals(before, knowledge.save());
    }

    @Test void hiddenParentsAndOwnClueAreSeparateIssuanceRequirements() {
        var knowledge = new PlayerKnowledge();
        knowledge.addResearch("FOCUSFIRE");
        assertEquals(MISSING_PREREQUISITES, ResearchNoteIssuance.validate(knowledge, "FOCUSHELLBAT"));
        knowledge.addResearch("INFUSION");
        assertEquals(MISSING_CLUE, ResearchNoteIssuance.validate(knowledge, "FOCUSHELLBAT"));
        knowledge.addResearch("@FOCUSHELLBAT");
        assertEquals(NONE, ResearchNoteIssuance.validate(knowledge, "FOCUSHELLBAT"));
        assertFalse(knowledge.isResearchComplete("FOCUSHELLBAT"));
    }

    @Test void categoryGateAppliesBeyondDirectParentsAndDefaultSecondaryCannotObtainFreePrimaryNotes() {
        var knowledge = new PlayerKnowledge();
        knowledge.addResearch("CAP_thaumium");
        knowledge.addResearch("VOIDMETAL");
        assertEquals(UNAVAILABLE, ResearchNoteIssuance.validate(knowledge, "CAP_void"));
        knowledge.addResearch("ELDRITCHMINOR");
        assertEquals(NONE, ResearchNoteIssuance.validate(knowledge, "CAP_void"));
        assertEquals(SECONDARY_REQUIRES_PURCHASE, ResearchNoteIssuance.validate(knowledge, "ENCHFABRIC"));
    }

    @Test void virtualAutoAspectlessConditionalAndCompletedResearchAreRefused() {
        var knowledge = new PlayerKnowledge();
        for (String key : List.of("CAP_iron", "ELDRITCHMINOR")) {
            assertEquals(UNAVAILABLE, ResearchNoteIssuance.validate(knowledge, key), key);
        }
        assertEquals(SECONDARY_REQUIRES_PURCHASE, ResearchNoteIssuance.validate(knowledge, "MIRRORESSENTIA"));
        ResearchConditions.setFeatures(true, false);
        try { assertEquals(UNAVAILABLE, ResearchNoteIssuance.validate(knowledge, "MIRRORESSENTIA")); }
        finally { ResearchConditions.setFeatures(true, true); }
        knowledge.addResearch("CRUCIBLE");
        knowledge.addResearch("NITOR");
        assertEquals(ALREADY_COMPLETE, ResearchNoteIssuance.validate(knowledge, "NITOR"));
    }

    @Test void malformedUnknownInputsDoNotChangeAnyKnowledgeState() {
        var knowledge = new PlayerKnowledge();
        knowledge.addResearch("CRUCIBLE");
        knowledge.scanItem("@minecraft:stone");
        knowledge.setAspectPool("aer", 19);
        knowledge.setWarpPerm(7);
        knowledge.setWarpSticky(3);
        knowledge.setWarpTemp(2);
        knowledge.setWarpCounter(81);
        var before = knowledge.save();
        for (String key : new String[]{null, "", " ", "@NITOR", "nitor", " NITOR", "NITOR\0", "UNKNOWN", "x".repeat(129)}) {
            assertEquals(UNKNOWN_RESEARCH, ResearchNoteIssuance.validate(knowledge, key));
            assertEquals(before, knowledge.save());
        }
    }

    @Test void generatedStateGuardRejectsWrongKeySolvedCopiedAndPlacedData() {
        var note = new ResearchNoteData("NITOR", 0x123456, false, 0, List.of(new Cell(0, 0, 1, "lux")));
        assertTrue(ResearchNoteIssuance.validGeneration("NITOR", note));
        assertFalse(ResearchNoteIssuance.validGeneration("FOCUSFIRE", note));
        assertFalse(ResearchNoteIssuance.validGeneration("NITOR", null));
        assertFalse(ResearchNoteIssuance.validGeneration("NITOR", note.withCells(note.cells(), true)));
        assertFalse(ResearchNoteIssuance.validGeneration("NITOR", new ResearchNoteData("NITOR", 0, false, 1, note.cells())));
        assertFalse(ResearchNoteIssuance.validGeneration("NITOR", note.withCells(
                List.of(note.cellAt(0, 0), new Cell(1, 0, 2, "aer")), false)));
    }
}
