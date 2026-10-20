package thaumcraft.research;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.common.config.ResearchSettings;
import thaumcraft.common.lib.research.PlayerKnowledge;
import thaumcraft.common.lib.research.ResearchCatalog;
import thaumcraft.common.lib.research.ResearchCheatSheet;
import thaumcraft.common.lib.research.ResearchDefinition;
import thaumcraft.common.lib.research.ResearchRegistrationOrder;

/**
 * The creative-style cheat sheet: every enabled research is granted with no prerequisites, research warp is awarded exactly as a
 * normal completion awards it, and only aspects the player does not know yet are filled to 50.
 */
class ResearchCheatSheetTest {
    private static final ResearchSettings OFF = ResearchSettings.DEFAULTS;
    private static final ResearchSettings ON = new ResearchSettings(true, true, true, true, false, 0, 100);
    private static final ResearchSettings ON_WUSS = new ResearchSettings(true, true, true, true, true, 0, 100);

    @BeforeAll static void load() { ResearchCatalog.loadBundled(); }

    private static List<ResearchDefinition> enabled() { return ResearchRegistrationOrder.enabled(); }

    @Test void withTheSheetDisabledNothingChangesAndNothingIsReported() {
        PlayerKnowledge knowledge = new PlayerKnowledge();
        PlayerKnowledge untouched = knowledge.copy();
        assertFalse(ResearchCheatSheet.grant(knowledge, OFF));
        assertEquals(untouched.save(), knowledge.save());
    }

    @Test void everyEnabledResearchIsCompletedAndNothingElse() {
        PlayerKnowledge knowledge = new PlayerKnowledge();
        assertTrue(ResearchCheatSheet.grant(knowledge, ON));
        Set<String> expected = new HashSet<>();
        for (ResearchDefinition research : enabled()) expected.add(research.key());
        assertEquals(expected, knowledge.getResearchComplete());
        assertTrue(expected.size() > 150, "enabled research: " + expected.size());
    }

    @Test void researchWarpIsAwardedAsHalfPermanentRoundedUpAndHalfStickyRoundedDown() {
        PlayerKnowledge knowledge = new PlayerKnowledge();
        ResearchCheatSheet.grant(knowledge, ON);
        int perm = 0, sticky = 0, total = 0;
        for (ResearchDefinition research : enabled()) {
            perm += research.warp() - research.warp() / 2;
            sticky += research.warp() / 2;
            total += research.warp();
        }
        assertTrue(total > 0, "some research carries warp");
        assertEquals(perm, knowledge.getWarpPerm());
        assertEquals(sticky, knowledge.getWarpSticky());
        assertEquals(0, knowledge.getWarpTemp());
        assertEquals(total, knowledge.getTotalWarp());
        assertEquals(total, knowledge.getWarpCounter(), "the warp counter is set to the total");
    }

    @Test void wussModeAwardsNoWarpAtAll() {
        PlayerKnowledge knowledge = new PlayerKnowledge();
        assertTrue(ResearchCheatSheet.grant(knowledge, ON_WUSS));
        assertEquals(0, knowledge.getTotalWarp());
        assertEquals(0, knowledge.getWarpCounter());
        assertEquals(enabled().size(), knowledge.getResearchComplete().size(), "the research itself is still granted");
    }

    @Test void onlyAspectsTheMagicianDoesNotKnowAreFilledToFifty() {
        PlayerKnowledge knowledge = new PlayerKnowledge();
        knowledge.setAspectPool("aer", 17);
        assertTrue(knowledge.addDiscoveredAspect("lux"));
        knowledge.setAspectPool("lux", 3);
        ResearchCheatSheet.grant(knowledge, ON);
        assertEquals(17, knowledge.getAspectPoolFor("aer"), "a known primal aspect is never topped up");
        assertEquals(0, knowledge.getAspectPoolFor("ignis"), "primal aspects are known from the start with an empty pool");
        assertEquals(3, knowledge.getAspectPoolFor("lux"), "a known compound aspect keeps its pool");
        for (Aspect aspect : Aspect.aspects.values()) {
            assertTrue(knowledge.hasDiscoveredAspect(aspect.getTag()), aspect.getTag());
            if (!aspect.isPrimal() && !aspect.getTag().equals("lux")) assertEquals(50, knowledge.getAspectPoolFor(aspect.getTag()), aspect.getTag());
        }
    }

    @Test void aSecondGrantChangesNothingAndReportsNothing() {
        PlayerKnowledge knowledge = new PlayerKnowledge();
        assertTrue(ResearchCheatSheet.grant(knowledge, ON));
        var after = knowledge.save();
        assertFalse(ResearchCheatSheet.grant(knowledge, ON));
        assertEquals(after, knowledge.save());
    }

    @Test void researchAlreadyCompleteAwardsNoWarpAgain() {
        PlayerKnowledge knowledge = new PlayerKnowledge();
        ResearchCheatSheet.grant(knowledge, ON);
        int perm = knowledge.getWarpPerm();
        int sticky = knowledge.getWarpSticky();
        knowledge.removeResearch(enabled().get(0).key());
        assertTrue(ResearchCheatSheet.grant(knowledge, ON), "the one removed research is granted again");
        int warp = enabled().get(0).warp();
        assertEquals(perm + warp - warp / 2, knowledge.getWarpPerm());
        assertEquals(sticky + warp / 2, knowledge.getWarpSticky());
    }

    @Test void whenOnlyAspectsAreMissingTheGrantStillReportsAChange() {
        PlayerKnowledge knowledge = new PlayerKnowledge();
        for (ResearchDefinition research : enabled()) knowledge.addResearch(research.key());
        assertTrue(ResearchCheatSheet.grant(knowledge, ON));
        assertEquals(0, knowledge.getTotalWarp(), "completed research does not pay its warp a second time");
        assertEquals(50, knowledge.getAspectPoolFor("lux"));
        assertFalse(ResearchCheatSheet.grant(knowledge, ON));
    }

    @Test void theGrantNeverTouchesScansTemporaryWarpOrTheInitializedFlag() {
        PlayerKnowledge knowledge = new PlayerKnowledge();
        knowledge.scanItem("minecraft:stone");
        knowledge.setWarpTemp(4);
        knowledge.setInitializedAspects(true);
        ResearchCheatSheet.grant(knowledge, ON);
        assertTrue(knowledge.hasScannedItem("minecraft:stone"));
        assertEquals(4, knowledge.getWarpTemp());
        assertTrue(knowledge.hasInitializedAspects());
    }
}
