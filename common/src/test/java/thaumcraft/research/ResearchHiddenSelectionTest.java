package thaumcraft.common.lib.research;

import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class ResearchHiddenSelectionTest {
    @BeforeAll static void load() { ResearchCatalog.loadBundled(); }
    @AfterEach void restoreFeatures() { ResearchConditions.setFeatures(true, true); }

    @Test void noParentPoolKeepsOriginalFullCategoryHashMapOrderWithoutRequiringCluesOrPoints() {
        var actor = new PlayerKnowledge(); var before = actor.save();
        // Audited independently from ConfigResearch registration lines and default category HashMaps,
        // including nonhidden entries before filtering. Sorting or mapping only hidden entries differs.
        assertEquals(List.of("BANNERS", "BONEBOW", "TINYDART", "TINYHAMMER", "TINYBOWTIE", "TINYHAT",
                "TINYFEZ", "TINYGLASSES", "TINYVISOR", "TINYARMOR"), ResearchHiddenSelection.eligible(actor));
        assertEquals(before, actor.save());
        assertFalse(actor.isResearchComplete("@BANNERS")); assertEquals(0, actor.getAspectPoolFor("pannus"));
    }

    @Test void literalWorldTimeWindowsUsePrivateJavaRandomIncludingNegativeAndLongBoundaryVectors() {
        var actor = new PlayerKnowledge();
        assertEquals("BANNERS", ResearchHiddenSelection.choose(actor, 0));
        assertEquals("BANNERS", ResearchHiddenSelection.choose(actor, 49));
        assertEquals("TINYHAT", ResearchHiddenSelection.choose(actor, 50));
        assertEquals("TINYHAT", ResearchHiddenSelection.choose(actor, 99));
        assertEquals("TINYVISOR", ResearchHiddenSelection.choose(actor, 100));
        assertEquals("TINYHAMMER", ResearchHiddenSelection.choose(actor, 1000));
        assertEquals("BONEBOW", ResearchHiddenSelection.choose(actor, 24000));
        assertEquals("TINYDART", ResearchHiddenSelection.choose(actor, Long.MAX_VALUE));
        assertEquals("BANNERS", ResearchHiddenSelection.choose(actor, -49));
        assertEquals("TINYHAMMER", ResearchHiddenSelection.choose(actor, -50));
    }

    @Test void theSeedIsTheAbsoluteWorldTimeAndNotTheTimeOfDay() {
        var actor = new PlayerKnowledge();
        // Original ResearchManager.findHiddenResearch:154 seeds the private RNG with
        // new Random(player.worldObj.getTotalWorldTime()/10L/5L).
        assertEquals(0L, ResearchHiddenSelection.seed(0L));
        assertEquals(0L, ResearchHiddenSelection.seed(49L));
        assertEquals(1L, ResearchHiddenSelection.seed(50L));
        assertEquals(4800L, ResearchHiddenSelection.seed(240000L));
        assertEquals(-1L, ResearchHiddenSelection.seed(-50L));
        // The same world state always reveals the same hidden research, and the
        // choice is the original private Random draw of the world time.
        var keys = ResearchHiddenSelection.eligible(actor);
        assertEquals(keys.get(new java.util.Random(ResearchHiddenSelection.seed(1000L)).nextInt(keys.size())),
                ResearchHiddenSelection.choose(actor, 1000L));
        assertEquals(ResearchHiddenSelection.choose(actor, 123456L), ResearchHiddenSelection.choose(actor, 123456L));
        // A world time beyond a day is a different moment than the same time of day.
        assertNotEquals(ResearchHiddenSelection.choose(actor, 0L), ResearchHiddenSelection.choose(actor, 24000L));
    }

    @Test void normalAndHiddenParentsAreRequiredButDiscoveryTriggersNeedNotHaveBeenScanned() {
        var actor = new PlayerKnowledge();
        actor.addResearch("FOCUSFIRE");
        assertFalse(ResearchHiddenSelection.eligible(actor).contains("FOCUSHELLBAT"));
        actor.addResearch("INFUSION");
        assertTrue(ResearchHiddenSelection.eligible(actor).contains("FOCUSHELLBAT"));
        assertTrue(ResearchHiddenSelection.eligible(actor).contains("JARBRAIN"));
        assertFalse(actor.hasDiscoveredAspect("ignis") && actor.getAspectPoolFor("ignis") > 0);
        assertTrue(actor.getScannedEntities().isEmpty()); assertTrue(actor.getScannedItems().isEmpty());
        actor.addResearch("THAUMIUM");
        assertFalse(ResearchHiddenSelection.eligible(actor).contains("ARMORFORTRESS"));
        actor.addResearch("INFUSIONENCHANTMENT");
        assertTrue(ResearchHiddenSelection.eligible(actor).contains("ARMORFORTRESS"));
        actor.addResearch("JARBRAIN");
        assertFalse(ResearchHiddenSelection.eligible(actor).contains("JARBRAIN"));
    }

    @Test void triggerlessBathSaltsAndDisabledMirrorCannotAppearAndCompletingCandidatesExhaustsThePool() {
        var actor = new PlayerKnowledge(); actor.addResearch("INFUSION");
        assertTrue(ResearchHiddenSelection.eligible(actor).contains("MIRROR"));
        ResearchConditions.setFeatures(true, false);
        assertFalse(ResearchHiddenSelection.eligible(actor).contains("MIRROR"));
        ResearchConditions.setFeatures(true, true);
        for (var research : ResearchCatalog.all()) {
            for (String parent : research.parents()) actor.addResearch(parent);
            for (String parent : research.parentsHidden()) actor.addResearch(parent);
        }
        assertFalse(ResearchHiddenSelection.eligible(actor).contains("BATHSALTS"));
        for (var research : ResearchCatalog.all()) actor.addResearch(research.key());
        assertTrue(ResearchHiddenSelection.eligible(actor).isEmpty());
        assertNull(ResearchHiddenSelection.choose(actor, 0)); assertNull(ResearchHiddenSelection.choose(actor, 24000));
    }
}
