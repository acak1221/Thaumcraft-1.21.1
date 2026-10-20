package thaumcraft.api;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import net.minecraft.SharedConstants;
import net.minecraft.server.Bootstrap;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import thaumcraft.api.capabilities.IPlayerKnowledge;
import thaumcraft.api.capabilities.NativePlayerKnowledge;
import thaumcraft.api.research.ResearchCategory;
import thaumcraft.common.lib.research.ResearchCatalog;
import thaumcraft.common.lib.research.ResearchDefinition;
import thaumcraft.common.lib.research.PlayerKnowledge;

/** The player knowledge contract of the public API. */
class CapabilitiesApiTest {
    @BeforeAll static void bootstrap() {
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
        ResearchCatalog.loadBundled();
    }

    /** The first original research without prerequisites. */
    private static String rootResearch() {
        for (ResearchDefinition definition : ResearchCatalog.all()) {
            if (definition.parents().isEmpty() && definition.parentsHidden().isEmpty()) return definition.key();
        }
        throw new IllegalStateException("The corpus has no root research");
    }

    @Test void anUnknownResearchIsUnknown() {
        IPlayerKnowledge knowledge = new NativePlayerKnowledge(new PlayerKnowledge());
        assertFalse(knowledge.isResearchKnown("NOT_A_RESEARCH"));
        assertEquals(IPlayerKnowledge.EnumResearchStatus.UNKNOWN, knowledge.getResearchStatus("NOT_A_RESEARCH"));
        assertFalse(knowledge.addResearch("NOT_A_RESEARCH"));
        assertEquals(0, knowledge.getResearchStage("NOT_A_RESEARCH"));
    }

    @Test void aResearchIsCompletedAndListed() {
        IPlayerKnowledge knowledge = new NativePlayerKnowledge(new PlayerKnowledge());
        String root = rootResearch();
        assertTrue(knowledge.isResearchKnown(root));
        assertTrue(knowledge.addResearch(root));
        assertTrue(knowledge.isResearchComplete(root));
        assertEquals(IPlayerKnowledge.EnumResearchStatus.COMPLETE, knowledge.getResearchStatus(root));
        assertTrue(knowledge.getResearchList().contains(root));
        assertTrue(knowledge.getResearchStage(root) > 0);
        assertTrue(knowledge.removeResearch(root));
        assertFalse(knowledge.isResearchComplete(root));
    }

    @Test void aResearchWithMissingPrerequisitesIsRefused() {
        IPlayerKnowledge knowledge = new NativePlayerKnowledge(new PlayerKnowledge());
        ResearchDefinition blocked = null;
        for (ResearchDefinition definition : ResearchCatalog.all()) {
            if (!definition.parents().isEmpty()) { blocked = definition; break; }
        }
        assertNotNull(blocked);
        assertFalse(knowledge.addResearch(blocked.key()));
        assertEquals(IPlayerKnowledge.EnumResearchStatus.UNKNOWN, knowledge.getResearchStatus(blocked.key()));
    }

    @Test void theResearchFlagsAreKept() {
        IPlayerKnowledge knowledge = new NativePlayerKnowledge(new PlayerKnowledge());
        String root = rootResearch();
        assertFalse(knowledge.hasResearchFlag(root, IPlayerKnowledge.EnumResearchFlag.POPUP));
        assertTrue(knowledge.setResearchFlag(root, IPlayerKnowledge.EnumResearchFlag.POPUP));
        assertTrue(knowledge.hasResearchFlag(root, IPlayerKnowledge.EnumResearchFlag.POPUP));
        assertTrue(knowledge.clearResearchFlag(root, IPlayerKnowledge.EnumResearchFlag.POPUP));
        assertFalse(knowledge.hasResearchFlag(root, IPlayerKnowledge.EnumResearchFlag.POPUP));
    }

    @Test void theKnowledgeOfACategoryIsTheOriginalWeightedRoot() {
        IPlayerKnowledge knowledge = new NativePlayerKnowledge(new PlayerKnowledge());
        ResearchCategory category = new ResearchCategory("TEST", null,
                new thaumcraft.api.aspects.AspectList().add(thaumcraft.api.aspects.Aspect.EARTH, 10), null, null);
        assertEquals(0, knowledge.getKnowledgeRaw(IPlayerKnowledge.EnumKnowledgeType.OBSERVATION, category));
        assertTrue(knowledge.addKnowledge(IPlayerKnowledge.EnumKnowledgeType.OBSERVATION, category, 40));
        assertEquals(40, knowledge.getKnowledgeRaw(IPlayerKnowledge.EnumKnowledgeType.OBSERVATION, category));
        assertEquals(7, knowledge.getKnowledge(IPlayerKnowledge.EnumKnowledgeType.OBSERVATION, category));
    }

    @Test void clearingForgetsEverything() {
        IPlayerKnowledge knowledge = new NativePlayerKnowledge(new PlayerKnowledge());
        knowledge.addResearch(rootResearch());
        knowledge.clear();
        assertTrue(knowledge.getResearchList().isEmpty());
    }
}
