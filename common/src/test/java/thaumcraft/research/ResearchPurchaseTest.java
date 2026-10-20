package thaumcraft.common.lib.research;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import net.minecraft.nbt.CompoundTag;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import static thaumcraft.common.lib.research.ResearchPurchase.Failure.*;

class ResearchPurchaseTest {
    @BeforeAll
    static void load() { ResearchCatalog.loadBundled(); }

    @Test
    void previewQuotesOriginalEnchantedFabricCostsWithoutMutatingAnyKnowledge() {
        PlayerKnowledge knowledge = funded("ENCHFABRIC");
        seedUnrelatedState(knowledge);
        CompoundTag before = knowledge.save();
        var quote = ResearchPurchase.preview(knowledge, "ENCHFABRIC");
        assertTrue(quote.success());
        assertEquals(Map.of("pannus", 3, "praecantatio", 3), quote.costs());
        assertTrue(quote.unlocked().isEmpty());
        assertEquals(before, knowledge.save());
        assertThrows(UnsupportedOperationException.class, () -> quote.costs().put("aer", 999));
        assertThrows(UnsupportedOperationException.class, () -> quote.unlocked().add("ENCHFABRIC"));
    }

    @Test
    void exactEnchantedFabricPurchaseDebitsBothPoolsButRetainsDiscoveryAndUnrelatedState() {
        PlayerKnowledge knowledge = funded("ENCHFABRIC");
        seedUnrelatedState(knowledge);
        var result = ResearchPurchase.purchase(knowledge, "ENCHFABRIC");
        assertTrue(result.success());
        assertEquals(Set.of("ENCHFABRIC"), result.unlocked());
        assertTrue(knowledge.isResearchComplete("ENCHFABRIC"));
        assertEquals(0, knowledge.getAspectPoolFor("pannus"));
        assertEquals(0, knowledge.getAspectPoolFor("praecantatio"));
        assertTrue(knowledge.hasDiscoveredAspect("pannus"));
        assertTrue(knowledge.hasDiscoveredAspect("praecantatio"));
        assertTrue(knowledge.hasInitializedAspects());
        assertEquals(19, knowledge.getAspectPoolFor("aer"));
        assertTrue(knowledge.isResearchComplete("@UNRELATED_CLUE"));
        assertTrue(knowledge.hasScannedItem("@minecraft:stone"));
        assertTrue(knowledge.hasScannedEntity("@minecraft:pig"));
        assertTrue(knowledge.hasScannedPhenomena("@NODE-preserved"));
        assertEquals(2, knowledge.getWarpPerm());
        assertEquals(4, knowledge.getWarpSticky());
        assertEquals(5, knowledge.getWarpTemp());
        assertEquals(77, knowledge.getWarpCounter());
        PlayerKnowledge restored = new PlayerKnowledge();
        restored.load(knowledge.save());
        assertEquals(knowledge.save(), restored.save());
    }

    @Test
    void lastInsufficientCostCannotDebitAnEarlierPoolOrCompleteResearch() {
        PlayerKnowledge knowledge = funded("ENCHFABRIC");
        seedUnrelatedState(knowledge);
        knowledge.setAspectPool("praecantatio", 2);
        CompoundTag before = knowledge.save();
        var preview = ResearchPurchase.preview(knowledge, "ENCHFABRIC");
        var result = ResearchPurchase.purchase(knowledge, "ENCHFABRIC");
        assertEquals(INSUFFICIENT_ASPECTS, preview.failure());
        assertEquals(INSUFFICIENT_ASPECTS, result.failure());
        assertEquals(Map.of("pannus", 3, "praecantatio", 3), result.costs());
        assertTrue(result.unlocked().isEmpty());
        assertEquals(before, knowledge.save());
    }

    @Test
    void missingAspectIsNotDiscoveredByFailedPurchaseAndOverdrawQuirkStillExists() {
        PlayerKnowledge knowledge = new PlayerKnowledge();
        knowledge.setAspectPool("pannus", 3);
        CompoundTag before = knowledge.save();
        assertEquals(INSUFFICIENT_ASPECTS, ResearchPurchase.purchase(knowledge, "ENCHFABRIC").failure());
        assertEquals(before, knowledge.save());
        assertFalse(knowledge.hasDiscoveredAspect("praecantatio"));
        // The action fixes preflight, not the original public pool API quirk.
        assertTrue(knowledge.addAspectPool("pannus", -4));
        assertEquals(3, knowledge.getAspectPoolFor("pannus"));
    }

    @Test
    void purchaseRevalidatesStateInsteadOfTrustingAnEarlierSuccessfulPreview() {
        PlayerKnowledge knowledge = funded("ENCHFABRIC");
        assertTrue(ResearchPurchase.preview(knowledge, "ENCHFABRIC").success());
        knowledge.setAspectPool("praecantatio", 2);
        CompoundTag before = knowledge.save();
        assertEquals(INSUFFICIENT_ASPECTS, ResearchPurchase.purchase(knowledge, "ENCHFABRIC").failure());
        assertEquals(before, knowledge.save());
    }

    @Test
    void replayCannotConsumeNewlyAddedPointsOrRepeatCompletion() {
        PlayerKnowledge knowledge = funded("ENCHFABRIC");
        assertTrue(ResearchPurchase.purchase(knowledge, "ENCHFABRIC").success());
        knowledge.setAspectPool("pannus", 20);
        knowledge.setAspectPool("praecantatio", 20);
        CompoundTag before = knowledge.save();
        assertEquals(ALREADY_COMPLETE, ResearchPurchase.preview(knowledge, "ENCHFABRIC").failure());
        assertEquals(ALREADY_COMPLETE, ResearchPurchase.purchase(knowledge, "ENCHFABRIC").failure());
        assertEquals(before, knowledge.save());
    }

    @Test
    void hiddenBannerRequiresClueAndKeepsThatClueAfterPurchase() {
        PlayerKnowledge knowledge = funded("BANNERS");
        CompoundTag before = knowledge.save();
        assertEquals(MISSING_CLUE, ResearchPurchase.purchase(knowledge, "BANNERS").failure());
        assertEquals(before, knowledge.save());
        knowledge.addResearch("@BANNERS");
        assertTrue(ResearchPurchase.purchase(knowledge, "BANNERS").success());
        assertTrue(knowledge.isResearchComplete("BANNERS"));
        assertTrue(knowledge.isResearchComplete("@BANNERS"));
        assertEquals(0, knowledge.getAspectPoolFor("sensus"));
        assertEquals(0, knowledge.getAspectPoolFor("pannus"));
        assertEquals(0, knowledge.getAspectPoolFor("praecantatio"));
    }

    @Test
    void fluxScrubberNeedsEveryVisibleAndHiddenParentEvenWhenClueExists() {
        PlayerKnowledge knowledge = funded("FLUXSCRUB");
        knowledge.addResearch("VISPOWER");
        knowledge.addResearch("BELLOWS");
        knowledge.addResearch("TUBES");
        knowledge.addResearch("@FLUXSCRUB");
        CompoundTag before = knowledge.save();
        assertEquals(MISSING_PREREQUISITES, ResearchPurchase.purchase(knowledge, "FLUXSCRUB").failure());
        assertEquals(before, knowledge.save());
        knowledge.addResearch("INFUSION");
        assertTrue(ResearchPurchase.purchase(knowledge, "FLUXSCRUB").success());
    }

    @Test
    void coreButcherAddsPermanentWarpOnceAndResetsCounterToFullCurrentTotal() {
        PlayerKnowledge knowledge = funded("COREBUTCHER");
        knowledge.addResearch("COREGUARD");
        knowledge.addResearch("COREHARVEST");
        knowledge.setWarpPerm(2);
        knowledge.setWarpSticky(4);
        knowledge.setWarpTemp(5);
        knowledge.setWarpCounter(77);
        assertTrue(ResearchPurchase.purchase(knowledge, "COREBUTCHER").success());
        assertEquals(3, knowledge.getWarpPerm());
        assertEquals(4, knowledge.getWarpSticky());
        assertEquals(5, knowledge.getWarpTemp());
        assertEquals(12, knowledge.getWarpCounter());
        CompoundTag after = knowledge.save();
        assertEquals(ALREADY_COMPLETE, ResearchPurchase.purchase(knowledge, "COREBUTCHER").failure());
        assertEquals(after, knowledge.save());
    }

    @Test
    void originalNormalDifficultyDoesNotTurnPrimaryNitorIntoAnAspectPurchase() {
        PlayerKnowledge knowledge = funded("NITOR");
        knowledge.addResearch("CRUCIBLE");
        CompoundTag before = knowledge.save();
        var result = ResearchPurchase.purchase(knowledge, "NITOR");
        assertEquals(PRIMARY_REQUIRES_NOTE, result.failure());
        assertEquals(Map.of("lux", 3, "ignis", 1), result.costs());
        assertEquals(before, knowledge.save());
    }

    @Test
    void conditionalVirtualAndAutoUnlockEntriesCannotBePurchased() {
        ResearchConditions.setFeatures(true, false);
        try { for (String key : List.of("MIRRORESSENTIA", "CAP_iron", "CRUCIBLE")) {
            PlayerKnowledge knowledge = funded(key);
            for (String parent : ResearchCatalog.get(key).parents()) knowledge.addResearch(parent);
            CompoundTag before = knowledge.save();
            var result = ResearchPurchase.purchase(knowledge, key);
            assertEquals(UNAVAILABLE, result.failure(), key);
            assertEquals(ResearchCatalog.get(key).aspectCost(), result.costs());
            assertEquals(before, knowledge.save(), key);
        } } finally { ResearchConditions.setFeatures(true, true); }
    }

    @Test
    void lostPrimalPearlNeedsItsClueAndEldritchCategoryGateSeparately() {
        PlayerKnowledge knowledge = funded("PRIMPEARL");
        knowledge.addResearch("@PRIMPEARL");
        CompoundTag before = knowledge.save();
        assertEquals(UNAVAILABLE, ResearchPurchase.purchase(knowledge, "PRIMPEARL").failure());
        assertEquals(before, knowledge.save());
        knowledge.addResearch("ELDRITCHMINOR");
        knowledge.removeResearch("@PRIMPEARL");
        assertEquals(MISSING_CLUE, ResearchPurchase.preview(knowledge, "PRIMPEARL").failure());
        knowledge.addResearch("@PRIMPEARL");
        assertTrue(ResearchPurchase.purchase(knowledge, "PRIMPEARL").success());
    }

    @Test
    void malformedAndUnknownKeysNeverMutateKnowledgeOrBecomeClueGrants() {
        PlayerKnowledge knowledge = funded("ENCHFABRIC");
        seedUnrelatedState(knowledge);
        CompoundTag before = knowledge.save();
        String[] keys = {null, "", " ", "@ENCHFABRIC", "enchfabric", " ENCHFABRIC", "ENCHFABRIC" + (char) 0, "NOT_A_RESEARCH", "x".repeat(257)};
        for (String key : keys) {
            var result = ResearchPurchase.purchase(knowledge, key);
            assertEquals(UNKNOWN_RESEARCH, result.failure(), String.valueOf(key));
            assertTrue(result.costs().isEmpty());
            assertTrue(result.unlocked().isEmpty());
            assertEquals(before, knowledge.save());
        }
    }

    @Test
    void siblingCompletionIsOneOrderedPassWithOwnPrerequisitesAndNoExtraCosts() {
        ResearchDefinition main = definition("MAIN", Set.of("Secondary"), Map.of("aer", 2), 3,
                List.of(), List.of("BLOCKED", "FIRST", "SECOND", "CONDITIONAL", "ABSENT"), List.of());
        ResearchDefinition blocked = definition("BLOCKED", Set.of(), Map.of("aer", 99), 9,
                List.of("SECOND"), List.of(), List.of());
        ResearchDefinition first = definition("FIRST", Set.of("Hidden"), Map.of("aer", 99), 2,
                List.of("MAIN"), List.of("GRANDCHILD"), List.of());
        ResearchDefinition second = definition("SECOND", Set.of("Virtual"), Map.of(), 1,
                List.of("FIRST"), List.of(), List.of());
        ResearchDefinition grandchild = definition("GRANDCHILD", Set.of(), Map.of(), 20,
                List.of("FIRST"), List.of(), List.of());
        ResearchDefinition conditional = definition("CONDITIONAL", Set.of(), Map.of(), 20,
                List.of("MAIN"), List.of(), List.of("if (unresolvedConfig)"));
        Map<String, ResearchDefinition> definitions = index(main, blocked, first, second, grandchild, conditional);
        PlayerKnowledge knowledge = new PlayerKnowledge();
        knowledge.setAspectPool("aer", 9);
        knowledge.setWarpPerm(2);
        knowledge.setWarpSticky(4);
        knowledge.setWarpTemp(5);
        var result = ResearchPurchase.purchase(knowledge, "MAIN", definitions::get);
        assertTrue(result.success());
        assertEquals(List.of("MAIN", "FIRST", "SECOND"), List.copyOf(result.unlocked()));
        assertEquals(7, knowledge.getAspectPoolFor("aer"));
        assertFalse(knowledge.isResearchComplete("BLOCKED"));
        assertFalse(knowledge.isResearchComplete("GRANDCHILD"));
        assertFalse(knowledge.isResearchComplete("CONDITIONAL"));
        assertEquals(6, knowledge.getWarpPerm());
        assertEquals(6, knowledge.getWarpSticky());
        assertEquals(5, knowledge.getWarpTemp());
        assertEquals(17, knowledge.getWarpCounter());
    }

    @Test
    void unexpectedResolverFailureBeforeCommitCannotLeavePartialDebitOrMainWarp() {
        ResearchDefinition main = definition("MAIN", Set.of("Secondary"), Map.of("aer", 2), 3,
                List.of(), List.of("BROKEN"), List.of());
        PlayerKnowledge knowledge = new PlayerKnowledge();
        knowledge.setAspectPool("aer", 9);
        CompoundTag before = knowledge.save();
        assertThrows(IllegalStateException.class, () -> ResearchPurchase.purchase(knowledge, "MAIN", key -> {
            if ("MAIN".equals(key)) return main;
            throw new IllegalStateException("broken test resolver");
        }));
        assertEquals(before, knowledge.save());
    }

    private static PlayerKnowledge funded(String key) {
        PlayerKnowledge knowledge = new PlayerKnowledge();
        ResearchCatalog.get(key).aspectCost().forEach(knowledge::setAspectPool);
        return knowledge;
    }

    private static void seedUnrelatedState(PlayerKnowledge knowledge) {
        knowledge.setInitializedAspects(true);
        knowledge.setAspectPool("aer", 19);
        knowledge.addResearch("@UNRELATED_CLUE");
        knowledge.scanItem("@minecraft:stone");
        knowledge.scanEntity("@minecraft:pig");
        knowledge.scanPhenomena("@NODE-preserved");
        knowledge.setWarpPerm(2);
        knowledge.setWarpSticky(4);
        knowledge.setWarpTemp(5);
        knowledge.setWarpCounter(77);
    }

    private static ResearchDefinition definition(String key, Set<String> flags, Map<String, Integer> costs, int warp,
                                                 List<String> parents, List<String> siblings, List<String> conditions) {
        return new ResearchDefinition(key, "ARTIFICE", 0, 0, 1, warp, costs, flags,
                parents, List.of(), siblings, List.of(), List.of(), List.of(), null, List.of(), conditions);
    }

    private static Map<String, ResearchDefinition> index(ResearchDefinition... definitions) {
        Map<String, ResearchDefinition> result = new LinkedHashMap<>();
        for (ResearchDefinition definition : definitions) result.put(definition.key(), definition);
        return result;
    }
}
