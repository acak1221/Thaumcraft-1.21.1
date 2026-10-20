package thaumcraft.research;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import thaumcraft.common.lib.research.CrimsonRitesResearch;
import thaumcraft.common.lib.research.PlayerKnowledge;
import thaumcraft.common.lib.research.ResearchCatalog;
import static org.junit.jupiter.api.Assertions.*;

class CrimsonRitesResearchTest {
    @BeforeAll static void catalog(){ResearchCatalog.loadBundled();}
    @Test void directlyGrantsOnlyCrimsonAndOriginalThreeWarpOnce(){
        var state=new PlayerKnowledge();state.setWarpPerm(4);state.setWarpSticky(5);state.setWarpTemp(6);state.setWarpCounter(0);
        assertTrue(CrimsonRitesResearch.study(state,false));assertEquals(java.util.Set.of("CRIMSON"),state.getResearchComplete());
        assertEquals(6,state.getWarpPerm());assertEquals(6,state.getWarpSticky());assertEquals(6,state.getWarpTemp());assertEquals(18,state.getWarpCounter());
        assertFalse(CrimsonRitesResearch.study(state,false));assertEquals(18,state.getWarpCounter());assertEquals(6,state.getWarpPerm());
    }
    @Test void wussGrantsResearchWithoutWarpOrCounterReset(){
        var state=new PlayerKnowledge();state.setWarpPerm(4);state.setWarpCounter(77);
        assertTrue(CrimsonRitesResearch.study(state,true));assertTrue(state.isResearchComplete("CRIMSON"));assertEquals(4,state.getWarpPerm());assertEquals(0,state.getWarpSticky());assertEquals(77,state.getWarpCounter());
        assertFalse(CrimsonRitesResearch.study(state,false));assertEquals(4,state.getWarpPerm());
    }
}
