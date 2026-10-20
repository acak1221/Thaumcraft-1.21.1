package thaumcraft.common.lib.research;

import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class ResearchNoteCopyingTest {
    @BeforeAll static void load() { ResearchCatalog.loadBundled(); }
    private static ResearchNoteData note(String key, boolean complete, int copies) {
        return new ResearchNoteData(key, 0x123456, complete, copies, List.of(new ResearchNoteData.Cell(0, 0, 1, "aer")));
    }
    private static PlayerKnowledge funded() {
        var actor = new PlayerKnowledge(); actor.addResearch("RESEARCHDUPE");
        actor.setAspectPool("lux", 10); actor.setAspectPool("ignis", 10); actor.setWarpPerm(7);
        actor.scanItem("#r10-preserved"); return actor;
    }
    @Test void consecutiveCopiesUseOldCounterAndOnlyTheirResearchAspectPrices() {
        var actor = funded(); var before = actor.save(); var input = note("NITOR", true, 0);
        var first = ResearchNoteCopying.plan(actor, input, 1);
        assertNotNull(first); assertEquals(Map.of("lux", 3, "ignis", 1), first.costs());
        assertEquals(2, first.count()); assertEquals(1, first.note().copies());
        assertEquals(7, first.knowledge().getAspectPoolFor("lux")); assertEquals(9, first.knowledge().getAspectPoolFor("ignis"));
        var second = ResearchNoteCopying.plan(first.knowledge(), first.note(), first.count());
        assertNotNull(second); assertEquals(Map.of("lux", 4, "ignis", 2), second.costs());
        assertEquals(3, second.count()); assertEquals(2, second.note().copies());
        assertEquals(3, second.knowledge().getAspectPoolFor("lux")); assertEquals(7, second.knowledge().getAspectPoolFor("ignis"));
        assertEquals(input.cells(), second.note().cells()); assertEquals(input.key(), second.note().key()); assertEquals(input.color(), second.note().color());
        assertEquals(before, actor.save()); assertEquals(0, input.copies());
        assertEquals(7, second.knowledge().getWarpPerm()); assertTrue(second.knowledge().hasScannedItem("#r10-preserved"));
        assertFalse(second.knowledge().isResearchComplete("NITOR"));
    }
    @Test void oneMissingAspectCannotPartiallyPayTheOther() {
        var actor = funded(); actor.setAspectPool("ignis", 0); var before = actor.save();
        assertNull(ResearchNoteCopying.plan(actor, note("NITOR", true, 0), 1)); assertEquals(before, actor.save());
    }
    @Test void researchGateSolvedFlagKnownDefinitionAndPositiveCountAreAuthoritative() {
        var actor = funded(); actor.removeResearch("RESEARCHDUPE");
        assertNull(ResearchNoteCopying.plan(actor, note("NITOR", true, 0), 1));
        actor.addResearch("RESEARCHDUPE");
        assertNull(ResearchNoteCopying.plan(actor, note("NITOR", false, 0), 1));
        assertThrows(IllegalArgumentException.class, () -> note("NO_SUCH_RESEARCH", true, 0));
        assertNull(ResearchNoteCopying.plan(actor, null, 1));
        assertNull(ResearchNoteCopying.plan(actor, note("NITOR", true, 0), 0));
    }
    @Test void integerOverflowAndSignedShortPoolCannotTurnAnUnpayableCopyIntoACredit() {
        var actor = funded(); actor.setAspectPool("lux", 32767); actor.setAspectPool("ignis", 32767);
        assertNull(ResearchNoteCopying.plan(actor, note("NITOR", true, Integer.MAX_VALUE), 1));
        assertNull(ResearchNoteCopying.plan(actor, note("NITOR", true, 32766), 1));
        assertNull(ResearchNoteCopying.plan(actor, note("NITOR", true, 0), Integer.MAX_VALUE));
        actor.setAspectPool("lux", 32768);
        assertNull(ResearchNoteCopying.plan(actor, note("NITOR", true, 0), 1));
    }
    @Test void countsCross99And127WithoutClampingSplittingOrResettingCopies() {
        var actor = funded(); actor.setAspectPool("lux", 1000); actor.setAspectPool("ignis", 1000);
        var hundred = ResearchNoteCopying.plan(actor, note("NITOR", true, 98), 99);
        assertNotNull(hundred); assertEquals(100, hundred.count()); assertEquals(99, hundred.note().copies());
        assertEquals(Map.of("lux", 101, "ignis", 99), hundred.costs());
        var beyond = ResearchNoteCopying.plan(actor, note("NITOR", true, 126), 127);
        assertNotNull(beyond); assertEquals(128, beyond.count()); assertEquals(127, beyond.note().copies());
        assertEquals(Map.of("lux", 129, "ignis", 127), beyond.costs());
    }
    @Test void alreadyLearnedResearchMayStillBeCopiedForAnotherReader() {
        var actor = funded(); actor.addResearch("NITOR");
        assertNotNull(ResearchNoteCopying.plan(actor, note("NITOR", true, 0), 1));
    }
}
