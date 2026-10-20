package thaumcraft.common.lib.research;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class ResearchCluesTest {
    @BeforeAll static void load() { ResearchCatalog.loadBundled(); }

    @Test void positiveActuallyAwardedAspectsUseOriginalPoolOrderWithoutParentsOrPriorDiscovery() {
        var actor = new PlayerKnowledge();
        assertFalse(actor.isResearchComplete("INFUSION")); assertFalse(actor.hasDiscoveredAspect("lux"));
        assertEquals(List.of("LAMPGROWTH", "LAMPFERTILITY"), ResearchClues.eligible(actor, key -> false, Map.of("lux", 1)));
        assertEquals(List.of("ARMORFORTRESS", "TINYVISOR", "TINYARMOR"), ResearchClues.eligible(actor, key -> false, Map.of("tutamen", 1)));
        assertTrue(ResearchClues.eligible(actor, key -> false, Map.of("lux", 0, "tutamen", -1)).isEmpty());
    }

    @Test void worldDrawAddsExactlyOneMarkerWithoutResearchCompletionPriceWarpOrExtraWeight() {
        var actor = new PlayerKnowledge(); actor.setWarpPerm(7); actor.scanItem("#keep");
        var expected = actor.copy(); expected.addResearch("@LAMPFERTILITY");
        AtomicInteger draws = new AtomicInteger();
        String selected = ResearchClues.reveal(actor, key -> key.equals("LAMPFERTILITY"), Map.of("lux", 1, "victus", 1), bound -> {
            draws.incrementAndGet(); assertEquals(2, bound); return 1;
        });
        assertEquals("LAMPFERTILITY", selected); assertEquals(1, draws.get());
        assertEquals(expected.save(), actor.save());
        assertFalse(actor.isResearchComplete("LAMPFERTILITY"));
        assertEquals(List.of("LAMPGROWTH"), ResearchClues.eligible(actor, key -> true, Map.of("lux", 1)).stream().filter(key -> key.startsWith("LAMP")).toList());
    }

    @Test void lostResearchAndHiddenEldritchMarkersCanPrecedeTheirBookAndParentGates() {
        var actor = new PlayerKnowledge();
        var lost = ResearchClues.eligible(actor, key -> Set.of("OUTERREV", "PRIMPEARL").contains(key), Map.of());
        assertEquals(Set.of("OUTERREV", "PRIMPEARL"), Set.copyOf(lost));
        assertEquals(List.of("ROD_primal_staff"), ResearchClues.eligible(actor, key -> key.equals("ROD_primal_staff"), Map.of()));
        assertEquals("ROD_primal_staff", ResearchClues.reveal(actor, key -> key.equals("ROD_primal_staff"), Map.of(), bound -> 0));
        assertFalse(actor.isResearchComplete("ELDRITCHMINOR")); assertTrue(actor.isResearchComplete("@ROD_primal_staff"));
    }

    @Test void duplicateCompletedUnknownTriggerAndPhenomenaDoNotConsumeChoiceRng() {
        var actor = new PlayerKnowledge(); actor.addResearch("JARBRAIN"); actor.addResearch("@BONEBOW");
        assertNull(ResearchClues.reveal(actor, key -> key.equals("JARBRAIN") || key.equals("BONEBOW"), Map.of(), bound -> {
            fail("duplicate and completed markers must be excluded before RNG"); return 0;
        }));
        assertNull(ResearchClues.reveal(actor, (ResearchClues.Context) null, Map.of("lux", 10), bound -> {
            fail("node phenomena have no clue witness and must not draw"); return 0;
        }));
        assertFalse(ResearchClues.eligible(actor, key -> key.equals("BATHSALTS"), Map.of()).contains("BATHSALTS"));
        // BATHSALTS has no declared direct or aspect trigger; a generic matcher cannot invent one.
        assertTrue(ResearchClues.eligible(actor, key -> false, Map.of("aer", 1)).isEmpty());
    }

    @Test void malformedRandomCannotCommitAnArbitraryClue() {
        var actor = new PlayerKnowledge(); var before = actor.save();
        assertThrows(IllegalArgumentException.class, () -> ResearchClues.reveal(actor, key -> false, Map.of("lux", 1), bound -> bound));
        assertEquals(before, actor.save());
    }
}
