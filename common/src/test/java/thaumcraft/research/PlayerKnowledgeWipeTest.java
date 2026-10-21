package thaumcraft.research;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.common.lib.research.PlayerKnowledge;

/**
 * The wipe of a player against the 4.2.3.5 oracle.
 *
 * <p>Oracle {@code thaumcraft/common/lib/research/PlayerKnowledge.java}:24-34 - the wipe drops the
 * research, the discovered aspects, the scanned objects, entities and phenomena and the three warp
 * values, but it keeps the warp counter of the player.</p>
 */
class PlayerKnowledgeWipeTest {
    @Test void theWipeKeepsTheWarpCounterOfThePlayer() {
        PlayerKnowledge knowledge = new PlayerKnowledge();
        knowledge.addResearch("ORE");
        knowledge.addDiscoveredAspect("auram");
        knowledge.scanItem("thaumcraft:thaumium_ore");
        knowledge.setWarpPerm(5);
        knowledge.setWarpSticky(3);
        knowledge.setWarpTemp(2);
        knowledge.setWarpCounter(17);
        knowledge.reset();
        assertFalse(knowledge.isResearchComplete("ORE"));
        assertFalse(knowledge.hasDiscoveredAspect("auram"));
        assertFalse(knowledge.hasScannedItem("thaumcraft:thaumium_ore"));
        assertEquals(0, knowledge.getWarpPerm());
        assertEquals(0, knowledge.getWarpSticky());
        assertEquals(0, knowledge.getWarpTemp());
        // The original keeps the counter of the wiped player.
        assertEquals(17, knowledge.getWarpCounter());
    }

    @Test void theWipeKeepsTheDiscoveredPrimalAspects() {
        PlayerKnowledge knowledge = new PlayerKnowledge();
        knowledge.addDiscoveredAspect("ignis");
        knowledge.reset();
        assertTrue(knowledge.hasDiscoveredAspect("aer"));
        assertTrue(knowledge.hasDiscoveredAspect(Aspect.AIR.getTag()));
    }
}
