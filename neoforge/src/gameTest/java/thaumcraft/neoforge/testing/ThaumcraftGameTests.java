package thaumcraft.neoforge.testing;

import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.ConnectionProtocol;
import net.minecraft.network.protocol.PacketFlow;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.neoforged.neoforge.network.registration.NetworkRegistry;
import thaumcraft.common.lib.network.ResearchPurchasePayload;
import thaumcraft.common.lib.network.ResearchNoteRequestPayload;
import thaumcraft.common.testing.RuntimeChecks;
import thaumcraft.common.testing.ResearchPurchaseRuntimeChecks;
import thaumcraft.common.testing.ResearchNoteRuntimeChecks;
import thaumcraft.common.testing.ResearchNoteIssuanceRuntimeChecks;
import thaumcraft.common.testing.ResearchTableRuntimeChecks;
import thaumcraft.common.testing.DeferredDropRuntimeChecks;
import thaumcraft.common.testing.TableDropCancellationRuntimeChecks;
import thaumcraft.common.testing.ResearchTableEditingRuntimeChecks;
import thaumcraft.common.testing.ResearchAspectCombinationRuntimeChecks;
import thaumcraft.common.testing.OreRuntimeChecks;
import thaumcraft.common.testing.NaturalOreGenerationRuntimeChecks;
import thaumcraft.common.testing.ArcaneWorkbenchRuntimeChecks;
import thaumcraft.common.testing.NaturalNodeRuntimeChecks;
import thaumcraft.common.testing.CrucibleRuntimeChecks;
import thaumcraft.common.testing.AlchemyResourceRuntimeChecks;
import thaumcraft.common.testing.FluxRuntimeChecks;
import thaumcraft.common.testing.ObjectAspectRuntimeChecks;
import thaumcraft.common.testing.MagicalTreeRuntimeChecks;
import thaumcraft.common.testing.ArcaneStoneRuntimeChecks;
import thaumcraft.common.testing.AlchemyFurnaceRuntimeChecks;
import thaumcraft.common.testing.AlembicRuntimeChecks;
import thaumcraft.common.testing.WandAssemblyRuntimeChecks;
import thaumcraft.common.testing.BellowsRuntimeChecks;
import thaumcraft.common.testing.JarLabelRuntimeChecks;
import thaumcraft.common.testing.PlantRuntimeChecks;
import thaumcraft.common.testing.FocusRuntimeChecks;
import thaumcraft.common.testing.EmberRuntimeChecks;
import thaumcraft.common.testing.TubeRuntimeChecks;
import thaumcraft.common.testing.JarTransportRuntimeChecks;

@PrefixGameTestTemplate(false)
public final class ThaumcraftGameTests {
    @GameTest(template = "empty", templateNamespace = "thaumcraft")
    public void essentiaTubesPullConserveAndToggleSides(GameTestHelper test) {
        TubeRuntimeChecks.run(test, NetworkedGameTestPlayer.create(test));
    }

    @GameTest(template = "empty", templateNamespace = "thaumcraft")
    public void jarsPullFromNativeTransportAndKeepManualActions(GameTestHelper test) {
        JarTransportRuntimeChecks.run(test, NetworkedGameTestPlayer.create(test));
    }

    @GameTest(template = "empty", templateNamespace = "thaumcraft")
    public void fireFocusSelectsPersistsAndPays(GameTestHelper test) {
        FocusRuntimeChecks.run(test, NetworkedGameTestPlayer.create(test));
    }

    @GameTest(template = "empty", templateNamespace = "thaumcraft")
    public void embersFlyDamageIgniteAndExpire(GameTestHelper test) {
        EmberRuntimeChecks.run(test, NetworkedGameTestPlayer.create(test));
    }

    @GameTest(template = "empty", templateNamespace = "thaumcraft")
    public void magicalFlowersGrowHarvestAndRefine(GameTestHelper test) {
        PlantRuntimeChecks.run(test);
    }

    @GameTest(template = "empty", templateNamespace = "thaumcraft")
    public void jarLabelsBindFilterAndPreserveContents(GameTestHelper test) {
        JarLabelRuntimeChecks.run(test, NetworkedGameTestPlayer.create(test));
    }

    @GameTest(template = "empty", templateNamespace = "thaumcraft")
    public void magicalTreesGrowDropAndCraft(GameTestHelper test) {
        MagicalTreeRuntimeChecks.run(test, NetworkedGameTestPlayer.create(test));
    }

    @GameTest(template = "empty", templateNamespace = "thaumcraft")
    public void arcaneStoneCraftsAndPreservesHalfBlocks(GameTestHelper test) {
        ArcaneStoneRuntimeChecks.run(test, NetworkedGameTestPlayer.create(test));
    }

    @GameTest(template = "empty", templateNamespace = "thaumcraft")
    public void alchemyFurnaceDistillsIntoRealAlembics(GameTestHelper test) {
        AlchemyFurnaceRuntimeChecks.run(test, NetworkedGameTestPlayer.create(test));
    }

    @GameTest(template = "empty", templateNamespace = "thaumcraft")
    public void alembicPreservesPhialsAndOrientation(GameTestHelper test) {
        AlembicRuntimeChecks.run(test, NetworkedGameTestPlayer.create(test));
    }

    @GameTest(template = "empty", templateNamespace = "thaumcraft")
    public void wandAssemblyUsesMaterialsAndResearch(GameTestHelper test) {
        WandAssemblyRuntimeChecks.run(test, NetworkedGameTestPlayer.create(test));
    }

    @GameTest(template = "empty", templateNamespace = "thaumcraft")
    public void bellowsHeatAndRespectNativeCompletion(GameTestHelper test) {
        BellowsRuntimeChecks.run(test, NetworkedGameTestPlayer.create(test));
    }

    @GameTest(template = "empty", templateNamespace = "thaumcraft")
    public void crucibleDissolvesAndCraftsWithOriginalCosts(GameTestHelper test) {
        CrucibleRuntimeChecks.run(test, NetworkedGameTestPlayer.create(test));
    }

    @GameTest(template = "empty", templateNamespace = "thaumcraft")
    public void alchemyResourcesPlaceBurnAndExplode(GameTestHelper test) {
        AlchemyResourceRuntimeChecks.run(test, NetworkedGameTestPlayer.create(test));
    }

    @GameTest(template = "empty", templateNamespace = "thaumcraft")
    public void fluxMovesAndExhaustsWithOriginalRules(GameTestHelper test) {
        FluxRuntimeChecks.run(test, NetworkedGameTestPlayer.create(test));
    }

    @GameTest(template = "empty", templateNamespace = "thaumcraft")
    public void objectAspectsUseRecipesAndRealComponents(GameTestHelper test) {
        ObjectAspectRuntimeChecks.run(test);
    }

    @GameTest(template = "empty", templateNamespace = "thaumcraft")
    public void infusedOresHarvestAndSmelt(GameTestHelper test) {
        OreRuntimeChecks.run(test, NetworkedGameTestPlayer.create(test));
    }

    @GameTest(template = "empty", templateNamespace = "thaumcraft", timeoutTicks = 600)
    public void naturalOresFeedThaumometerRecipe(GameTestHelper test) {
        NaturalOreGenerationRuntimeChecks.run(test);
    }

    @GameTest(template = "empty", templateNamespace = "thaumcraft")
    public void researchAspectsCombineAndBonusesRegenerate(GameTestHelper test) {
        ResearchAspectCombinationRuntimeChecks.run(test, NetworkedGameTestPlayer.create(test));
    }

    @GameTest(template = "empty", templateNamespace = "thaumcraft")
    public void arcaneWorkbenchCraftsWithVisAndPersists(GameTestHelper test) {
        ArcaneWorkbenchRuntimeChecks.run(test, NetworkedGameTestPlayer.create(test));
    }

    @GameTest(template = "empty", templateNamespace = "thaumcraft")
    public void naturalNodesDrainAndRegenerate(GameTestHelper test) {
        NaturalNodeRuntimeChecks.run(test, NetworkedGameTestPlayer.create(test));
    }

    @GameTest(template = "empty", templateNamespace = "thaumcraft")
    public void researchTableEditsSolveAndPreserveResources(GameTestHelper test) {
        ResearchTableEditingRuntimeChecks.run(test, NetworkedGameTestPlayer.create(test));
    }

    // Isolate retry timing from unrelated tests' initial inventory enqueues.
    @GameTest(template = "empty", templateNamespace = "thaumcraft", setupTicks = 80, timeoutTicks = 200)
    public void canceledTableOutputRetriesAfterCooldown(GameTestHelper test) {
        TableDropCancellationRuntimeChecks.canceledTableOutputRetriesAfterCooldown(test, NetworkedGameTestPlayer.create(test));
    }

    @GameTest(template = "empty", templateNamespace = "thaumcraft", setupTicks = 80)
    public void tableSpawnCallbackPreservesReplacementInventory(GameTestHelper test) {
        TableDropCancellationRuntimeChecks.tableSpawnCallbackPreservesReplacementInventory(test, NetworkedGameTestPlayer.create(test));
    }

    @GameTest(template = "empty", templateNamespace = "thaumcraft")
    public void rejectedSpawnPreservesExistingHiddenUuidOwner(GameTestHelper test) {
        TableDropCancellationRuntimeChecks.rejectedSpawnPreservesExistingHiddenUuidOwner(test);
    }

    // The cross-boundary NBT case and actual far-chunk unload/reload are separate checks.
    @GameTest(template = "empty", templateNamespace = "thaumcraft")
    public void originalTableCraftPlacementAndDrops(GameTestHelper test) {
        ResearchTableRuntimeChecks.originalTableCraftPlacementAndDrops(test, NetworkedGameTestPlayer.create(test));
    }

    @GameTest(template = "empty", templateNamespace = "thaumcraft")
    public void tableConversionDirectionsAndCosts(GameTestHelper test) {
        ResearchTableRuntimeChecks.tableConversionDirectionsAndCosts(test, NetworkedGameTestPlayer.create(test));
    }

    @GameTest(template = "empty", templateNamespace = "thaumcraft")
    public void tableSecondaryQuirksAndSearchOrder(GameTestHelper test) {
        ResearchTableRuntimeChecks.tableSecondaryQuirksAndSearchOrder(test, NetworkedGameTestPlayer.create(test));
    }

    @GameTest(template = "empty", templateNamespace = "thaumcraft")
    public void sharedTableMenusFiltersAndQuickMove(GameTestHelper test) {
        ResearchTableRuntimeChecks.sharedTableMenusFiltersAndQuickMove(test, NetworkedGameTestPlayer.create(test), NetworkedGameTestPlayer.create(test));
    }

    @GameTest(template = "empty", templateNamespace = "thaumcraft")
    public void tableMenuDistanceAndStaleIdentity(GameTestHelper test) {
        ResearchTableRuntimeChecks.tableMenuDistanceAndStaleIdentity(test, NetworkedGameTestPlayer.create(test));
    }

    @GameTest(template = "empty", templateNamespace = "thaumcraft")
    public void tableInventoryComponentsAndCountsPersist(GameTestHelper test) {
        ResearchTableRuntimeChecks.tableInventoryComponentsAndCountsPersist(test, NetworkedGameTestPlayer.create(test));
    }

    @GameTest(template = "empty", templateNamespace = "thaumcraft")
    public void tableBreakEvacuatesEachHalfOnce(GameTestHelper test) {
        ResearchTableRuntimeChecks.tableBreakEvacuatesEachHalfOnce(test, NetworkedGameTestPlayer.create(test));
    }

    @GameTest(template = "empty", templateNamespace = "thaumcraft")
    public void tablePairAcrossChunkBoundaryPersists(GameTestHelper test) {
        ResearchTableRuntimeChecks.tablePairAcrossChunkBoundaryPersists(test, NetworkedGameTestPlayer.create(test));
    }

    @GameTest(template = "empty", templateNamespace = "thaumcraft", timeoutTicks = 600)
    public void tablePairActuallyUnloadsAndReloads(GameTestHelper test) {
        ResearchTableRuntimeChecks.tablePairActuallyUnloadsAndReloads(test, NetworkedGameTestPlayer.create(test));
    }

    @GameTest(template = "empty", templateNamespace = "thaumcraft")
    public void tableFirstNativeTickRepairsReloadedOrphans(GameTestHelper test) {
        ResearchTableRuntimeChecks.tableFirstNativeTickRepairsReloadedOrphans(test, NetworkedGameTestPlayer.create(test));
    }

    @GameTest(template = "empty", templateNamespace = "thaumcraft")
    public void pendingDropSurvivesCollisionAndPersistence(GameTestHelper test) {
        DeferredDropRuntimeChecks.pendingDropSurvivesCollisionAndPersistence(test);
    }

    @GameTest(template = "empty", templateNamespace = "thaumcraft")
    public void quarantineRetainsUnknownData(GameTestHelper test) {
        DeferredDropRuntimeChecks.quarantineRetainsUnknownData(test);
    }

    // Negotiated native players exercise the shared handler/service. This does
    // not simulate C2S dispatch or claim ItemTossEvent cancellation coverage.
    @GameTest(template = "empty", templateNamespace = "thaumcraft")
    public void primaryRequestConsumesPaperAndLastInk(GameTestHelper test) {
        ResearchNoteIssuanceRuntimeChecks.primaryRequestConsumesPaperAndLastInk(test, notePlayer(test));
    }

    @GameTest(template = "empty", templateNamespace = "thaumcraft")
    public void existingNotesReuseWithoutResourcesOrRandomness(GameTestHelper test) {
        ResearchNoteIssuanceRuntimeChecks.existingNotesReuseWithoutResourcesOrRandomness(test, notePlayer(test));
    }

    @GameTest(template = "empty", templateNamespace = "thaumcraft")
    public void primaryRequestRefusalsAreAtomic(GameTestHelper test) {
        ResearchNoteIssuanceRuntimeChecks.primaryRequestRefusalsAreAtomic(test, notePlayer(test));
    }

    @GameTest(template = "empty", templateNamespace = "thaumcraft")
    public void primaryRequestUsesOnlyOrderedMainInventory(GameTestHelper test) {
        ResearchNoteIssuanceRuntimeChecks.primaryRequestUsesOnlyOrderedMainInventory(test, notePlayer(test));
    }

    @GameTest(template = "empty", templateNamespace = "thaumcraft")
    public void fullInventoryReusesFreedPaperSlotOrDrops(GameTestHelper test) {
        ResearchNoteIssuanceRuntimeChecks.fullInventoryReusesFreedPaperSlotOrDrops(test, notePlayer(test));
    }

    @GameTest(template = "empty", templateNamespace = "thaumcraft")
    public void nativeInkPreservesCreativeAndUnbreakable(GameTestHelper test) {
        ResearchNoteIssuanceRuntimeChecks.nativeInkPreservesCreativeAndUnbreakable(test, notePlayer(test));
    }

    @GameTest(template = "empty", templateNamespace = "thaumcraft")
    public void nativeUnbreakingUsesExactlyOnePlayerDraw(GameTestHelper test) {
        ResearchNoteIssuanceRuntimeChecks.nativeUnbreakingUsesExactlyOnePlayerDraw(test, notePlayer(test));
    }

    @GameTest(template = "empty", templateNamespace = "thaumcraft")
    public void invalidGenerationLeavesResourcesAndKnowledgeUnchanged(GameTestHelper test) {
        ResearchNoteIssuanceRuntimeChecks.invalidGenerationLeavesResourcesAndKnowledgeUnchanged(test, notePlayer(test));
    }

    @GameTest(template = "empty", templateNamespace = "thaumcraft")
    public void deliveryFailurePreservesPaperInkAndKnowledge(GameTestHelper test) {
        ResearchNoteIssuanceRuntimeChecks.deliveryFailurePreservesPaperInkAndKnowledge(test, notePlayer(test));
    }

    private static net.minecraft.server.level.ServerPlayer notePlayer(GameTestHelper test) {
        test.assertTrue(NetworkRegistry.getCodec(ResearchNoteRequestPayload.TYPE.id(),
                        ConnectionProtocol.PLAY, PacketFlow.SERVERBOUND) != null,
                "note-request key payload is registered for serverbound play");
        return NetworkedGameTestPlayer.create(test);
    }

    @GameTest(template = "empty", templateNamespace = "thaumcraft")
    public void noteComponentsPersistAndEncode(GameTestHelper test) { ResearchNoteRuntimeChecks.noteComponentsPersistAndEncode(test); }

    @GameTest(template = "empty", templateNamespace = "thaumcraft")
    public void solvedNoteLearnsOnceAndTransfers(GameTestHelper test) {
        var session = NetworkedGameTestPlayer.createSession(test);
        ResearchNoteRuntimeChecks.solvedNoteLearnsOnceAndTransfers(test, session.player(),
                (expected, required) -> session.captureKnowledge(test, expected, required));
    }

    @GameTest(template = "empty", templateNamespace = "thaumcraft")
    public void noteRefusalsAndWarpUseOriginalRules(GameTestHelper test) {
        var session = NetworkedGameTestPlayer.createSession(test);
        ResearchNoteRuntimeChecks.noteRefusalsAndWarpUseOriginalRules(test, session.player(),
                (expected, required) -> session.captureKnowledge(test, expected, required));
    }

    @GameTest(template = "empty", templateNamespace = "thaumcraft")
    public void originalScribingRecipesAndInkBounds(GameTestHelper test) { ResearchNoteRuntimeChecks.originalScribingRecipesAndInkBounds(test); }

    @GameTest(template = "empty", templateNamespace = "thaumcraft")
    public void secondaryPurchaseSpendsExactlyAndReplayIsIdempotent(GameTestHelper test) {
        var session = purchaseSession(test);
        ResearchPurchaseRuntimeChecks.secondaryPurchaseSpendsExactlyAndReplayIsIdempotent(test, session.player(),
                (expected, required) -> session.captureKnowledge(test, expected, required));
    }

    @GameTest(template = "empty", templateNamespace = "thaumcraft")
    public void secondaryPurchaseRejectsPartialFundsAtomically(GameTestHelper test) {
        var session = purchaseSession(test);
        ResearchPurchaseRuntimeChecks.secondaryPurchaseRejectsPartialFundsAtomically(test, session.player(),
                (expected, required) -> session.captureKnowledge(test, expected, required));
    }

    @GameTest(template = "empty", templateNamespace = "thaumcraft")
    public void secondaryPurchaseRequiresClueAndHiddenParents(GameTestHelper test) {
        var session = purchaseSession(test);
        ResearchPurchaseRuntimeChecks.secondaryPurchaseRequiresClueAndHiddenParents(test, session.player(),
                (expected, required) -> session.captureKnowledge(test, expected, required));
    }

    @GameTest(template = "empty", templateNamespace = "thaumcraft")
    public void secondaryPurchaseWarpAndPlayerPersistence(GameTestHelper test) {
        var session = purchaseSession(test);
        ResearchPurchaseRuntimeChecks.secondaryPurchaseWarpAndPlayerPersistence(test, session.player(),
                (expected, required) -> session.captureKnowledge(test, expected, required));
    }

    private static NetworkedGameTestPlayer.Session purchaseSession(GameTestHelper test) {
        test.assertTrue(NetworkRegistry.getCodec(ResearchPurchasePayload.TYPE.id(),
                        ConnectionProtocol.PLAY, PacketFlow.SERVERBOUND) != null,
                "purchase key payload is registered for serverbound play");
        return NetworkedGameTestPlayer.createSession(test);
    }

    @GameTest(template = "empty", templateNamespace = "thaumcraft", batch = "tc_isolated_bookshelf_wand")
    public void bookshelfWandCreatesOneFloatingBook(GameTestHelper test) { RuntimeChecks.bookshelfWandCreatesOneFloatingBook(test); }

    @GameTest(template = "empty", templateNamespace = "thaumcraft")
    public void normalBookOpensWithoutGrantingArbitraryResearch(GameTestHelper test) {
        RuntimeChecks.normalBookOpensWithoutGrantingArbitraryResearch(test, NetworkedGameTestPlayer.create(test));
    }

    @GameTest(template = "empty", templateNamespace = "thaumcraft")
    public void originalEntryRecipes(GameTestHelper test) { RuntimeChecks.originalEntryRecipes(test); }

    @GameTest(template = "empty", templateNamespace = "thaumcraft")
    public void bootstrapAndItemComponents(GameTestHelper test) { RuntimeChecks.bootstrapAndItemComponents(test); }

    @GameTest(template = "empty", templateNamespace = "thaumcraft")
    public void playerKnowledgeMixinPersists(GameTestHelper test) { RuntimeChecks.playerKnowledgeMixinPersists(test); }

    @GameTest(template = "empty", templateNamespace = "thaumcraft")
    public void jarDropRetainsEssentia(GameTestHelper test) { RuntimeChecks.jarDropRetainsEssentia(test); }

    @GameTest(template = "empty", templateNamespace = "thaumcraft")
    public void nodeDrainPreservesTc4Rounding(GameTestHelper test) {
        RuntimeChecks.nodeDrainPreservesTc4Rounding(test, NetworkedGameTestPlayer.create(test));
    }

    @GameTest(template = "empty", templateNamespace = "thaumcraft")
    public void thaumometerNodeScanUsesTc4Rewards(GameTestHelper test) {
        RuntimeChecks.thaumometerNodeScanUsesTc4Rewards(test, NetworkedGameTestPlayer.create(test));
    }

    @GameTest(template = "empty", templateNamespace = "thaumcraft")
    public void thaumometerBlockPickSharesDroppedItemIdentity(GameTestHelper test) {
        RuntimeChecks.thaumometerBlockPickSharesDroppedItemIdentity(test, NetworkedGameTestPlayer.create(test));
    }
}
