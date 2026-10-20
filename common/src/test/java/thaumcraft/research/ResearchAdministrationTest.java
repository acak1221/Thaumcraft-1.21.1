package thaumcraft.research;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.common.lib.WarpEvents;
import thaumcraft.common.lib.research.PlayerKnowledge;
import thaumcraft.common.lib.research.ResearchAdministration;
import thaumcraft.common.lib.research.ResearchCatalog;
import static org.junit.jupiter.api.Assertions.*;

class ResearchAdministrationTest {
    @BeforeAll static void load() { ResearchCatalog.loadBundled(); }

    @Test void recursiveGrantIncludesHiddenParentsAndTheirSiblingsButKnownKeysStopTraversal() {
        var state = new PlayerKnowledge();
        assertTrue(ResearchAdministration.grant(state, "FLUXSCRUB"));
        for (String key : new String[]{"FLUXSCRUB", "VISPOWER", "BELLOWS", "TUBES", "INFUSION", "DISTILESSENTIA", "NITOR", "ALUMENTUM", "JARLABEL"})
            assertTrue(state.isResearchComplete(key), key);
        var known = new PlayerKnowledge();
        known.addResearch("FLUXSCRUB");
        assertTrue(ResearchAdministration.grant(known, "FLUXSCRUB"));
        assertEquals(java.util.Set.of("FLUXSCRUB"), known.getResearchComplete());
        assertFalse(ResearchAdministration.grant(known, "missing_admin_research"));
    }

    @Test void researchWarpIsAddedOnceAndResetPreservesKnowledgeScansAndWarp() {
        boolean before = WarpEvents.isWussMode();
        try {
            WarpEvents.setWussMode(false);
            var state = new PlayerKnowledge();
            assertTrue(ResearchAdministration.grant(state, "RESEARCHER2"));
            assertEquals(1, state.getWarpPerm()); assertEquals(0, state.getWarpSticky());
            assertEquals(1, state.getWarpCounter());
            ResearchAdministration.grant(state, "RESEARCHER2");
            assertEquals(1, state.getWarpPerm());
            state.addResearch("@BANNERS"); state.scanItem("admin_saved_scan"); state.setAspectPool("cognitio", 73);
            ResearchAdministration.resetResearch(state);
            assertFalse(state.isResearchComplete("RESEARCHER2")); assertFalse(state.isResearchComplete("@BANNERS"));
            assertTrue(state.isResearchComplete("RESEARCH"));
            assertTrue(state.hasScannedItem("admin_saved_scan")); assertEquals(73, state.getAspectPoolFor("cognitio"));
            assertEquals(1, state.getWarpPerm()); assertEquals(1, state.getWarpCounter());
            WarpEvents.setWussMode(true);
            ResearchAdministration.grant(state, "RESEARCHER2");
            assertTrue(state.isResearchComplete("RESEARCHER2")); assertEquals(1, state.getWarpPerm());
        } finally { WarpEvents.setWussMode(before); }
    }

    @Test void aspectAmountKeepsOriginalSignedShortNarrowingAndOverdrawQuirk() {
        var state = new PlayerKnowledge(); state.setAspectPool("aer", 33000);
        ResearchAdministration.giveAspect(state, Aspect.AIR, 32768);
        assertEquals(232, state.getAspectPoolFor("aer"));
        ResearchAdministration.giveAspect(state, Aspect.AIR, 32768);
        assertEquals(232, state.getAspectPoolFor("aer"));
        ResearchAdministration.giveAspect(state, Aspect.AIR, 65535);
        assertEquals(231, state.getAspectPoolFor("aer"));
        ResearchAdministration.giveAspect(state, Aspect.AIR, 65536);
        assertEquals(231, state.getAspectPoolFor("aer"));
        assertThrows(IllegalArgumentException.class, () -> ResearchAdministration.giveAspect(state, Aspect.AIR, 0));
    }

    @Test void directWarpCommandsKeepCounterAndClampWhileBypassingGameplayWuss() {
        var state = new PlayerKnowledge(); state.setWarpCounter(73);
        ResearchAdministration.warp(state, false, 50, "PERM");
        ResearchAdministration.warp(state, true, -100, "perm");
        assertEquals(0, state.getWarpPerm());
        ResearchAdministration.warp(state, false, 7, "TEMP");
        ResearchAdministration.warp(state, true, 5, "anything_else");
        assertEquals(7, state.getWarpTemp()); assertEquals(5, state.getWarpSticky());
        assertEquals(73, state.getWarpCounter());
        ResearchAdministration.warp(state, false, Integer.MAX_VALUE, "PERM");
        ResearchAdministration.warp(state, true, 1, "PERM");
        assertEquals(0, state.getWarpPerm(), "original int overflow is followed by the nonnegative setter");
        assertThrows(IllegalArgumentException.class, () -> ResearchAdministration.warp(state, true, 101, ""));
        assertThrows(IllegalArgumentException.class, () -> ResearchAdministration.warp(state, false, -1, ""));
    }
}
