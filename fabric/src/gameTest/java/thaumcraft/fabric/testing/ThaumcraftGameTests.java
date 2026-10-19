package thaumcraft.fabric.testing;

import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
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
import thaumcraft.common.testing.InfusionRuntimeChecks;
import thaumcraft.common.testing.InfusionStructureRuntimeChecks;
import thaumcraft.common.testing.GolemColorsRuntimeChecks;
import thaumcraft.common.testing.GolemFireOrderRuntimeChecks;
import thaumcraft.common.testing.GolemFishingLiveRuntimeChecks;
import thaumcraft.common.testing.GolemFishingSmeltingRuntimeChecks;
import thaumcraft.common.testing.GolemFishingUpgradesRuntimeChecks;
import thaumcraft.common.testing.GolemGuardTargetsRuntimeChecks;
import thaumcraft.common.testing.GolemUpgradeRuntimeChecks;
import thaumcraft.common.testing.GolemWaterRangeRuntimeChecks;
import thaumcraft.common.testing.GolemGhostSlotsRuntimeChecks;
import thaumcraft.common.testing.LumberGolemRuntimeChecks;
import thaumcraft.common.testing.StreamAxeRuntimeChecks;
import thaumcraft.common.testing.ThaumiumEquipmentRuntimeChecks;
import thaumcraft.common.testing.WispEssenceRuntimeChecks;
import thaumcraft.common.testing.GogglesRuntimeChecks;
import thaumcraft.common.testing.RobeRuntimeChecks;
import thaumcraft.common.testing.ShockFocusRuntimeChecks;
import thaumcraft.common.testing.ExcavationHarvestRuntimeChecks;
import thaumcraft.common.testing.ExcavationFocusRuntimeChecks;
import thaumcraft.common.testing.FrostFocusRuntimeChecks;
import thaumcraft.common.testing.FrostShardRuntimeChecks;
import thaumcraft.common.testing.VoidJarRuntimeChecks;
import thaumcraft.common.testing.JarItemRuntimeChecks;
import thaumcraft.common.testing.ValveRuntimeChecks;
import thaumcraft.common.testing.ShapelessResonatorRuntimeChecks;
import thaumcraft.common.testing.TubeVariantsRuntimeChecks;
import thaumcraft.common.testing.TubeFilterLabelRuntimeChecks;
import thaumcraft.common.testing.TubeBufferRuntimeChecks;
import thaumcraft.common.testing.BufferAtomicRuntimeChecks;
import thaumcraft.common.testing.CentrifugeRuntimeChecks;
import thaumcraft.common.testing.CentrifugeAtomicRuntimeChecks;
import thaumcraft.common.testing.CrystallizerRuntimeChecks;
import thaumcraft.common.testing.CrystallizerOutputRuntimeChecks;
import thaumcraft.common.testing.NodeStabilizerRuntimeChecks;
import thaumcraft.common.testing.NodeStabilizerLifecycleChecks;
import thaumcraft.common.testing.FluxOverfullRuntimeChecks;
import thaumcraft.common.testing.VisPowerRecipeRuntimeChecks;
import thaumcraft.common.testing.NodeTransductionRuntimeChecks;
import thaumcraft.common.testing.VisNetRuntimeChecks;
import thaumcraft.common.testing.VisNetLifecycleChecks;
import thaumcraft.common.testing.FocalRecipeRuntimeChecks;
import thaumcraft.common.testing.FocalManipulatorRuntimeChecks;
import thaumcraft.common.testing.FocusUpgradeRuntimeChecks;
import thaumcraft.common.testing.ExcavationUpgradeRuntimeChecks;
import thaumcraft.common.testing.ExcavationRemovalFailureRuntimeChecks;
import thaumcraft.common.testing.InfusionComponentPredicateGameTests;
import thaumcraft.common.testing.BorePistonAspectRuntimeChecks;

public final class ThaumcraftGameTests implements FabricGameTest {
    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE, timeoutTicks = 240, batch = "bore_chunk_envelope_native_prerequisite")
    public void boreNativeChunkEnvelopeIsNotLiveOwnerTag(GameTestHelper test) {
        thaumcraft.common.testing.BoreClientPrerequisiteRuntimeChecks.nativeChunkEnvelopeIsNotLiveOwnerTag(test);
    }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE, timeoutTicks = 240, batch = "bore_native_offhand_label_prerequisite")
    public void boreHeldJarRemovesLabelWithOccupiedOffhand(GameTestHelper test) {
        thaumcraft.common.testing.BoreClientPrerequisiteRuntimeChecks.heldJarRemovesLabelWithOccupiedOffhand(test);
    }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE, timeoutTicks = 240, batch = "bore_protection_architectury_deny")
    public void boreProtectionArchitecturyDeny(GameTestHelper test) {
        thaumcraft.common.testing.BoreProtectionRuntimeChecks.denialOrder(test, new BoreProtectionProbe(), false);
    }
    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE, timeoutTicks = 240, batch = "bore_protection_fabric_deny")
    public void boreProtectionFabricDeny(GameTestHelper test) {
        thaumcraft.common.testing.BoreProtectionRuntimeChecks.denialOrder(test, new BoreProtectionProbe(), true);
    }
    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE, timeoutTicks = 240, batch = "bore_protection_allowed")
    public void boreProtectionAllowed(GameTestHelper test) {
        thaumcraft.common.testing.BoreProtectionRuntimeChecks.allowedRemoval(test, new BoreProtectionProbe());
    }
    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE, timeoutTicks = 240, batch = "bore_protection_target_aba")
    public void boreProtectionTargetAba(GameTestHelper test) {
        thaumcraft.common.testing.BoreProtectionRuntimeChecks.targetAba(test, new BoreProtectionProbe());
    }
    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE, timeoutTicks = 240, batch = "bore_protection_before_top")
    public void boreProtectionBeforeTop(GameTestHelper test) {
        thaumcraft.common.testing.BoreProtectionRuntimeChecks.beforeRemovesTop(test, new BoreProtectionProbe());
    }
    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE, timeoutTicks = 240, batch = "bore_protection_after_top")
    public void boreProtectionAfterTop(GameTestHelper test) {
        thaumcraft.common.testing.BoreProtectionRuntimeChecks.afterOutcome(test, new BoreProtectionProbe(), false);
    }
    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE, timeoutTicks = 240, batch = "bore_protection_after_throw")
    public void boreProtectionAfterThrow(GameTestHelper test) {
        thaumcraft.common.testing.BoreProtectionRuntimeChecks.afterOutcome(test, new BoreProtectionProbe(), true);
    }
    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE, timeoutTicks = 240, batch = "bore_protection_unknown_false")
    public void boreProtectionUnknownFalse(GameTestHelper test) {
        thaumcraft.common.testing.BoreProtectionRuntimeChecks.unknownOutcome(test, new BoreProtectionProbe(), false);
    }
    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE, timeoutTicks = 240, batch = "bore_protection_unknown_throw")
    public void boreProtectionUnknownThrow(GameTestHelper test) {
        thaumcraft.common.testing.BoreProtectionRuntimeChecks.unknownOutcome(test, new BoreProtectionProbe(), true);
    }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE, timeoutTicks = 240, batch = "bore_native_retirement_revision_exhaustion")
    public void boreExhaustedRevisionRetainsOneOpaqueOwner(GameTestHelper test) {
        thaumcraft.common.testing.BoreWorkRuntimeChecks.exhaustedRevisionRetainsOneOpaqueOwner(test);
    }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE, timeoutTicks = 240, batch = "bore_native_retirement_stable")
    public void boreStableNativeRetirementQueuesToolsOnce(GameTestHelper test) {
        thaumcraft.common.testing.BoreRetirementCustodyRuntimeChecks.stableNativeRetirementQueuesToolsOnce(test);
    }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE, timeoutTicks = 240, batch = "bore_native_retirement_component_mismatch")
    public void boreRetirementComponentMismatchRetainsActualSlots(GameTestHelper test) {
        thaumcraft.common.testing.BoreRetirementCustodyRuntimeChecks.componentMismatchRetainsActualSlots(test);
    }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE, timeoutTicks = 240, batch = "bore_native_retirement_codec_failure")
    public void boreRetirementCodecFailureAfterRemovalRetainsAndSaves(GameTestHelper test) {
        thaumcraft.common.testing.BoreRetirementCustodyRuntimeChecks.codecFailureAfterOwnerRemovalRetainsAndSaves(test);
    }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE, timeoutTicks = 240, batch = "bore_native_journal_downgrade")
    public void boreReservedUuidCannotBecomeLegacyOutput(GameTestHelper test) {
        thaumcraft.common.testing.BoreJournalDowngradeRuntimeChecks.reservedUuidCannotBecomeLegacyOutput(test);
    }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE, timeoutTicks = 240, batch = "bore_native_external_authority")
    public void boreExternalRequestsCannotBorrowNativeOrigin(GameTestHelper test) {
        thaumcraft.common.testing.BoreWorkRuntimeChecks.externalRequestsCannotBorrowNativeOrigin(test);
    }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE, timeoutTicks = 240, batch = "bore_native_real_ore")
    public void boreOrdinaryOreUsesRealNativeTicksAndNoXp(GameTestHelper test) {
        thaumcraft.common.testing.BoreWorkRuntimeChecks.ordinaryOreUsesRealNativeTicksAndNoXp(test);
    }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE, timeoutTicks = 240, batch = "bore_native_lazy_essentia")
    public void boreBaseEssentiaBindsThenPrefillsWhileOff(GameTestHelper test) {
        thaumcraft.common.testing.BoreWorkRuntimeChecks.baseEssentiaNeedsLazyBindingAndThenPrefillsWhileOff(test);
    }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE, timeoutTicks = 240, batch = "bore_native_outer_callback_fence")
    public void boreOuterDestroyCallbackRemainsClosed(GameTestHelper test) {
        thaumcraft.common.testing.BoreNativeDispatchRuntimeChecks.outerDestroyCallbackRemainsClosed(test);
    }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE, timeoutTicks = 240, batch = "bore_native_finite_source_budget")
    public void boreCompleteFiniteEssentiaBudget(GameTestHelper test) {
        thaumcraft.common.testing.BoreSourceBudgetRuntimeChecks.completeFinite112EssentiaBudget(test);
    }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE, timeoutTicks = 240, batch = "bore_native_geometry")
    public void boreExtendedCollisionAndWandDirections(GameTestHelper test) {
        thaumcraft.common.testing.BoreBlockRuntimeChecks.extendedCollisionAndWandDirections(test);
    }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE, timeoutTicks = 240, batch = "bore_native_menu")
    public void boreNativeMenuAndReload(GameTestHelper test) {
        thaumcraft.common.testing.BoreBlockRuntimeChecks.nativeMenuAndReload(test);
    }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE, timeoutTicks = 240, batch = "bore_native_recipe_payment")
    public void boreRecipePaymentAndTopPredicates(GameTestHelper test) {
        thaumcraft.common.testing.BoreRecipeRuntimeChecks.basePaymentAndTopPredicates(test);
    }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE, timeoutTicks = 240, batch = "bore_prerequisite_piston_source")
    public void borePistonAspectsDistillation(GameTestHelper test) {
        BorePistonAspectRuntimeChecks.pistonRecipeTagsAndFiniteDistillation(test);
    }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE, timeoutTicks = 240, batch = "bore_prerequisite_recipe_predicate")
    public void infusionComponentPredicateCodec(GameTestHelper test) {
        InfusionComponentPredicateGameTests.recipeCodecAndDeclaredTools(test);
    }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE, timeoutTicks = 240, batch = "bore_prerequisite_session_predicate")
    public void infusionComponentPredicateSession(GameTestHelper test) {
        InfusionComponentPredicateGameTests.selectedSessionReloadAndReplacement(test);
    }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE, timeoutTicks = 240, batch = "m08_excavation_removal_failure")
    public void excavationExtraRemovalFailureAccounting(GameTestHelper test) {
        ExcavationRemovalFailureRuntimeChecks.run(test);
    }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE, timeoutTicks = 240, batch = "m08_focal_recipes")
    public void focalRealCharmAndManipulatorPayments(GameTestHelper test) {
        FocalRecipeRuntimeChecks.realCharmAndManipulatorPayments(test);
    }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE, timeoutTicks = 240, batch = "m08_focal_admission")
    public void focalAdmissionAndCancellation(GameTestHelper test) {
        FocalManipulatorRuntimeChecks.admissionAndCancellation(test);
    }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE, timeoutTicks = 240, batch = "m08_focal_source_reload")
    public void focalSourceDebitsAndReload(GameTestHelper test) {
        FocalManipulatorRuntimeChecks.sourceDebitsAndReload(test);
    }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE, timeoutTicks = 240, batch = "m08_focal_post_debit")
    public void focalPostDebitFences(GameTestHelper test) {
        FocalManipulatorRuntimeChecks.postDebitFences(test);
    }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE, timeoutTicks = 240, batch = "m08_focal_drop")
    public void focalDurableDropRefusal(GameTestHelper test) {
        FocalManipulatorRuntimeChecks.durableDropRefusal(test);
    }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE, timeoutTicks = 240, batch = "m08_focus_upgrade_casting")
    public void focusUpgradeDataAndCasting(GameTestHelper test) {
        FocusUpgradeRuntimeChecks.run(test);
    }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE, timeoutTicks = 240, batch = "m08_excavation_upgrades")
    public void excavationUpgradeTreasureAndEnlarge(GameTestHelper test) {
        ExcavationUpgradeRuntimeChecks.run(test);
    }

    // These native fixtures temporarily own level-wide callback/visibility state.
    // Distinct batches keep them separate from one another and every old case.
    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE, timeoutTicks = 240, batch = "m09b_vispower_recipes")
    public void visPowerRecipesPlacementAndAttunement(GameTestHelper test) {
        VisPowerRecipeRuntimeChecks.recipesPlacementAndAttunement(test);
    }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE, timeoutTicks = 240, batch = "m09b_transduction_lifecycle")
    public void nodeTransductionConversionPersistenceAndCooldown(GameTestHelper test) {
        NodeTransductionRuntimeChecks.conversionPersistenceAndCooldown(test);
    }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE, timeoutTicks = 240, batch = "m09b_transduction_failure")
    public void nodeTransductionIsolatedFailureLifecycle(GameTestHelper test) {
        NodeTransductionRuntimeChecks.isolatedFailureLifecycle(test);
    }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE, timeoutTicks = 240, batch = "m09b_visnet_source")
    public void visNetSourceBudgets(GameTestHelper test) {
        VisNetRuntimeChecks.sourceBudgets(test);
    }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE, timeoutTicks = 240, batch = "m09b_visnet_graph")
    public void visNetGraphSelection(GameTestHelper test) {
        VisNetRuntimeChecks.graphSelection(test);
    }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE, timeoutTicks = 240, batch = "m09b_visnet_callbacks")
    public void visNetCallbacksReloadAndIsolation(GameTestHelper test) {
        VisNetLifecycleChecks.callbacksReloadAndIsolation(test);
    }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE, timeoutTicks = 240, batch = "m09b_visnet_crystallizer")
    public void visNetCrystallizer(GameTestHelper test) {
        VisNetLifecycleChecks.crystallizer(test);
    }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE, timeoutTicks = 240, batch = "m09b_flux_overfull_state")
    public void fluxOverfullFlowStateAndPersistence(GameTestHelper test) {
        FluxOverfullRuntimeChecks.overfullFlowStateAndPersistence(test);
    }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE, timeoutTicks = 240, batch = "m09b_flux_original_exposure")
    public void fluxOriginalExposureAndGooTail(GameTestHelper test) {
        FluxOverfullRuntimeChecks.originalExposureAndGooTail(test);
    }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE, timeoutTicks = 240, batch = "m09b_flux_callback_wake")
    public void fluxCallbacksBoundariesAndNeighborWake(GameTestHelper test) {
        FluxOverfullRuntimeChecks.callbacksBoundariesAndNeighborWake(test);
    }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE, timeoutTicks = 240, batch = "node_stabilizer_recipe")
    public void nodeStabilizerRecipesPlacementAndTransientDevice(GameTestHelper test) {
        NodeStabilizerRuntimeChecks.recipesPlacementAndTransientDevice(test);
    }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE, timeoutTicks = 240, batch = "node_stabilizer_lock")
    public void nodeStabilizerLockPhaseDischargeAndWand(GameTestHelper test) {
        NodeStabilizerLifecycleChecks.lockPhaseDischargeAndWand(test);
    }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE, timeoutTicks = 240, batch = "node_stabilizer_regeneration")
    public void nodeStabilizerRegenerationRepairAndPersistence(GameTestHelper test) {
        NodeStabilizerLifecycleChecks.regenerationRepairAndPersistence(test);
    }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE, timeoutTicks = 200, batch = "crystallizer_recipes_item")
    public void crystallizerRecipesAndOrderedCrystalItem(GameTestHelper test) {
        CrystallizerRuntimeChecks.recipesAndOrderedCrystalItem(test);
    }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE, timeoutTicks = 200, batch = "crystallizer_timing_persistence")
    public void crystallizerSingleSlotTimingPowerAndPersistence(GameTestHelper test) {
        CrystallizerRuntimeChecks.singleSlotTimingPowerAndPersistence(test);
    }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE, timeoutTicks = 200, batch = "crystallizer_native_consumers")
    public void crystallizerNativeInventoryRoutingAndProducedConsumers(GameTestHelper test) {
        CrystallizerOutputRuntimeChecks.nativeInventoryRoutingAndProducedConsumers(test);
    }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE, timeoutTicks = 220, batch = "crystallizer_durable_ownership")
    public void crystallizerDurableEscrowCallbacksAndSpawnRetries(GameTestHelper test) {
        CrystallizerOutputRuntimeChecks.durableEscrowCallbacksAndSpawnRetries(test);
    }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE, timeoutTicks = 1200)
    public void constructAndCentrifugeRecipesAndPlainNativeBlocks(GameTestHelper test) {
        CentrifugeRuntimeChecks.recipesAndPlainNativeBlocks(test);
    }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE, timeoutTicks = 1200)
    public void centrifugeCompartmentsTimingPowerAndPersistence(GameTestHelper test) {
        CentrifugeRuntimeChecks.twoCompartmentsTimingPowerAndPersistence(test);
    }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE, timeoutTicks = 1200)
    public void centrifugeNativeDrawsConservationAndCallbackFences(GameTestHelper test) {
        CentrifugeAtomicRuntimeChecks.nativeDrawsConservationAndCallbackFences(test);
    }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE, timeoutTicks = 1200)
    public void essentiaBufferRecipesStockPersistenceAndTools(GameTestHelper test) {
        TubeBufferRuntimeChecks.recipesStockPersistenceAndTools(test);
    }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE, timeoutTicks = 1200)
    public void essentiaBufferBellowsChokesAndFillCadence(GameTestHelper test) {
        TubeBufferRuntimeChecks.bellowsChokesAndFillCadence(test);
    }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE, timeoutTicks = 1200)
    public void bufferAtomicCallersArbitrateAndFenceCallbacks(GameTestHelper test) {
        BufferAtomicRuntimeChecks.nativeCallersArbitrationAndFences(test);
    }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE, timeoutTicks = 1200)
    public void advancedTubeRecipesAndNativeTransport(GameTestHelper test) {
        TubeVariantsRuntimeChecks.recipesAndNativeTransport(test);
    }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE, timeoutTicks = 1200)
    public void tubeFilterLabelsPayPreserveMarkerAndFenceCallbacks(GameTestHelper test) {
        TubeFilterLabelRuntimeChecks.labelsPayPreserveMarkerAndFenceCallbacks(test);
    }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE, timeoutTicks = 1200)
    public void tubeFilterLabelReturnsSurviveNativeCancellationAndFailure(GameTestHelper test) {
        TubeFilterLabelRuntimeChecks.labelReturnsSurviveNativeCancellationAndFailure(test);
    }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE, timeoutTicks = 1200)
    public void essentiaValvesSampleEdgesAndPreserveBufferedEssentia(GameTestHelper test) {
        ValveRuntimeChecks.run(test, test.makeMockServerPlayerInLevel());
    }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE, timeoutTicks = 1200)
    public void shapelessArcaneUsesManagerMenuAndPayment(GameTestHelper test) {
        ShapelessResonatorRuntimeChecks.shapelessArcaneUsesManagerMenuAndPayment(test);
    }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE, timeoutTicks = 1200)
    public void resonatorInspectsTransportsWithoutMutation(GameTestHelper test) {
        ShapelessResonatorRuntimeChecks.resonatorInspectsTransportsWithoutMutation(test);
    }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE, timeoutTicks = 1200)
    public void voidJarsPullOverflowAndKeepManualActions(GameTestHelper test) {
        VoidJarRuntimeChecks.run(test, test.makeMockServerPlayerInLevel());
    }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE, timeoutTicks = 1200)
    public void jarItemsCraftPreserveComponentsAndExposeContents(GameTestHelper test) {
        JarItemRuntimeChecks.run(test, test.makeMockServerPlayerInLevel());
    }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE, timeoutTicks = 36000)
    public void frostFocusCraftsSelectsAndPays(GameTestHelper test) {
        FrostFocusRuntimeChecks.run(test, test.makeMockServerPlayerInLevel());
    }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE, timeoutTicks = 600)
    public void frostShardRicochetsDamagesAndPersists(GameTestHelper test) {
        FrostShardRuntimeChecks.run(test);
    }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE, timeoutTicks = 600)
    public void excavationHarvestPreservesProtectionLootAndContainers(GameTestHelper test) {
        ExcavationHarvestRuntimeChecks.run(test);
    }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE, timeoutTicks = 1600, batch = "m10_lumber_golem")
    public void lumberGolemFellsOneFarLogThroughTheGuardedPlatform(GameTestHelper test) {
        LumberGolemRuntimeChecks.run(test);
    }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE, timeoutTicks = 800, batch = "m10_golem_upgrades")
    public void golemUpgradesInstallPersistAndApplyEarthAirEntropy(GameTestHelper test) {
        GolemUpgradeRuntimeChecks.run(test);
    }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE, timeoutTicks = 2100, batch = "m10_golem_water_range")
    public void waterUpgradeWidensTheLiveWorkRadiusOfRunningLumberAndHarvestGolems(GameTestHelper test) {
        GolemWaterRangeRuntimeChecks.run(test);
    }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE, timeoutTicks = 2400, batch = "m10_golem_fire_order")
    public void fireIgnitesGuardTargetsAndOrderReplantsHarvestedCrops(GameTestHelper test) {
        GolemFireOrderRuntimeChecks.run(test, GolemPlacementProbe::open);
    }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE, timeoutTicks = 1000, batch = "m10_ghost_slots")
    public void fireUpgradesAddPagesOfGhostSlotsToItemFilterGolems(GameTestHelper test) {
        GolemGhostSlotsRuntimeChecks.run(test);
    }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE, timeoutTicks = 1500, batch = "m10_golem_colors")
    public void orderColoursRouteMarkersAndSlotsAndTheBellCyclesThem(GameTestHelper test) {
        GolemColorsRuntimeChecks.run(test);
    }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE, timeoutTicks = 200, batch = "m10_golem_fishing_fire")
    public void fireUpgradeCooksTheFishingGolemsCatch(GameTestHelper test) {
        GolemFishingSmeltingRuntimeChecks.run(test);
    }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE, timeoutTicks = 2400, batch = "m10_golem_guard_targets")
    public void orderGuardTargetSwitchesChooseWhomRealGuardsFight(GameTestHelper test) {
        GolemGuardTargetsRuntimeChecks.run(test);
    }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE, timeoutTicks = 600, batch = "m10_golem_fishing_upgrades")
    public void orderEntropyAndAirChangeWhatAFishingGolemCatches(GameTestHelper test) {
        GolemFishingUpgradesRuntimeChecks.run(test);
    }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE, timeoutTicks = 5600, batch = "m10_golem_fishing_live")
    public void realFishingGolemsCastIntoPondsAndDeliverCooksAndExtraCatches(GameTestHelper test) {
        GolemFishingLiveRuntimeChecks.run(test);
    }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE, timeoutTicks = 600)
    public void streamAxeChopsEndpointVacuumsAndFollowsDrops(GameTestHelper test) {
        StreamAxeRuntimeChecks.run(test);
    }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE, timeoutTicks = 600)
    public void excavationFocusChannelsAndPaysCompletedAttempts(GameTestHelper test) {
        ExcavationFocusRuntimeChecks.run(test, test.makeMockServerPlayerInLevel());
    }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE, timeoutTicks = 600)
    public void robesCraftDyeWashProtectAndRepair(GameTestHelper test) {
        RobeRuntimeChecks.run(test, test.makeMockServerPlayerInLevel());
    }

    // The dedicated GameTest clock runs far faster than20TPS; this case
    // deliberately waits on eleven real250ms cooldowns, not fake game time.
    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE, timeoutTicks = 36000)
    public void shockFocusPaysTargetsAndRespectsCooldown(GameTestHelper test) {
        ShockFocusRuntimeChecks.run(test, test.makeMockServerPlayerInLevel());
    }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE, timeoutTicks = 600)
    public void gogglesCraftProtectAndReduceVis(GameTestHelper test) {
        GogglesRuntimeChecks.run(test, test.makeMockServerPlayerInLevel());
    }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE, timeoutTicks = 600)
    public void harvestedNodeEssenceDistillsRealAura(GameTestHelper test) {
        WispEssenceRuntimeChecks.run(test, test.makeMockServerPlayerInLevel());
    }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE, timeoutTicks = 600)
    public void thaumiumEquipmentCraftsMinesProtectsAndRepairs(GameTestHelper test) {
        ThaumiumEquipmentRuntimeChecks.run(test, test.makeMockServerPlayerInLevel());
    }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE, timeoutTicks = 600, batch = "infusion_native_ownership")
    public void infusionConsumesDistilledEssentiaAndPersistsSession(GameTestHelper test) {
        InfusionRuntimeChecks.run(test, test.makeMockServerPlayerInLevel());
    }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE, timeoutTicks = 600)
    public void infusionAltarFormsAndExecutesAllInstabilityOutcomes(GameTestHelper test) {
        InfusionStructureRuntimeChecks.run(test, test.makeMockServerPlayerInLevel());
    }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE)
    public void essentiaTubesPullConserveAndToggleSides(GameTestHelper test) {
        TubeRuntimeChecks.run(test, test.makeMockServerPlayerInLevel());
    }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE)
    public void jarsPullFromNativeTransportAndKeepManualActions(GameTestHelper test) {
        JarTransportRuntimeChecks.run(test, test.makeMockServerPlayerInLevel());
    }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE)
    public void fireFocusSelectsPersistsAndPays(GameTestHelper test) {
        FocusRuntimeChecks.run(test, test.makeMockServerPlayerInLevel());
    }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE)
    public void embersFlyDamageIgniteAndExpire(GameTestHelper test) {
        EmberRuntimeChecks.run(test, test.makeMockServerPlayerInLevel());
    }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE)
    public void magicalFlowersGrowHarvestAndRefine(GameTestHelper test) {
        PlantRuntimeChecks.run(test);
    }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE)
    public void jarLabelsBindFilterAndPreserveContents(GameTestHelper test) {
        JarLabelRuntimeChecks.run(test, test.makeMockServerPlayerInLevel());
    }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE)
    public void magicalTreesGrowDropAndCraft(GameTestHelper test) {
        MagicalTreeRuntimeChecks.run(test, test.makeMockServerPlayerInLevel());
    }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE)
    public void arcaneStoneCraftsAndPreservesHalfBlocks(GameTestHelper test) {
        ArcaneStoneRuntimeChecks.run(test, test.makeMockServerPlayerInLevel());
    }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE)
    public void alchemyFurnaceDistillsIntoRealAlembics(GameTestHelper test) {
        AlchemyFurnaceRuntimeChecks.run(test, test.makeMockServerPlayerInLevel());
    }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE)
    public void alembicPreservesPhialsAndOrientation(GameTestHelper test) {
        AlembicRuntimeChecks.run(test, test.makeMockServerPlayerInLevel());
    }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE)
    public void wandAssemblyUsesMaterialsAndResearch(GameTestHelper test) {
        WandAssemblyRuntimeChecks.run(test, test.makeMockServerPlayerInLevel());
    }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE)
    public void bellowsHeatAndRespectNativeCompletion(GameTestHelper test) {
        BellowsRuntimeChecks.run(test, test.makeMockServerPlayerInLevel());
    }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE)
    public void crucibleDissolvesAndCraftsWithOriginalCosts(GameTestHelper test) {
        CrucibleRuntimeChecks.run(test, test.makeMockServerPlayerInLevel());
    }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE)
    public void alchemyResourcesPlaceBurnAndExplode(GameTestHelper test) {
        AlchemyResourceRuntimeChecks.run(test, test.makeMockServerPlayerInLevel());
    }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE)
    public void fluxMovesAndExhaustsWithOriginalRules(GameTestHelper test) {
        FluxRuntimeChecks.run(test, test.makeMockServerPlayerInLevel());
    }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE)
    public void objectAspectsUseRecipesAndRealComponents(GameTestHelper test) {
        ObjectAspectRuntimeChecks.run(test);
    }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE)
    public void infusedOresHarvestAndSmelt(GameTestHelper test) {
        OreRuntimeChecks.run(test, test.makeMockServerPlayerInLevel());
    }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE, timeoutTicks = 600)
    public void naturalOresFeedThaumometerRecipe(GameTestHelper test) {
        NaturalOreGenerationRuntimeChecks.run(test);
    }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE)
    public void researchAspectsCombineAndBonusesRegenerate(GameTestHelper test) {
        ResearchAspectCombinationRuntimeChecks.run(test, test.makeMockServerPlayerInLevel());
    }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE)
    public void arcaneWorkbenchCraftsWithVisAndPersists(GameTestHelper test) {
        ArcaneWorkbenchRuntimeChecks.run(test, test.makeMockServerPlayerInLevel());
    }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE)
    public void naturalNodesDrainAndRegenerate(GameTestHelper test) {
        NaturalNodeRuntimeChecks.run(test, test.makeMockServerPlayerInLevel());
    }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE)
    public void researchTableEditsSolveAndPreserveResources(GameTestHelper test) {
        ResearchTableEditingRuntimeChecks.run(test, test.makeMockServerPlayerInLevel());
    }

    // Isolate retry timing from unrelated tests' initial inventory enqueues.
    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE, setupTicks = 80, timeoutTicks = 200)
    public void canceledTableOutputRetriesAfterCooldown(GameTestHelper test) {
        TableDropCancellationRuntimeChecks.canceledTableOutputRetriesAfterCooldown(test, test.makeMockServerPlayerInLevel());
    }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE, setupTicks = 80)
    public void tableSpawnCallbackPreservesReplacementInventory(GameTestHelper test) {
        TableDropCancellationRuntimeChecks.tableSpawnCallbackPreservesReplacementInventory(test, test.makeMockServerPlayerInLevel());
    }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE)
    public void rejectedSpawnPreservesExistingHiddenUuidOwner(GameTestHelper test) {
        TableDropCancellationRuntimeChecks.rejectedSpawnPreservesExistingHiddenUuidOwner(test);
    }

    // The cross-boundary NBT case and actual far-chunk unload/reload are separate checks.
    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE)
    public void originalTableCraftPlacementAndDrops(GameTestHelper test) {
        ResearchTableRuntimeChecks.originalTableCraftPlacementAndDrops(test, test.makeMockServerPlayerInLevel());
    }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE)
    public void tableConversionDirectionsAndCosts(GameTestHelper test) {
        ResearchTableRuntimeChecks.tableConversionDirectionsAndCosts(test, test.makeMockServerPlayerInLevel());
    }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE)
    public void tableSecondaryQuirksAndSearchOrder(GameTestHelper test) {
        ResearchTableRuntimeChecks.tableSecondaryQuirksAndSearchOrder(test, test.makeMockServerPlayerInLevel());
    }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE)
    public void sharedTableMenusFiltersAndQuickMove(GameTestHelper test) {
        ResearchTableRuntimeChecks.sharedTableMenusFiltersAndQuickMove(test, test.makeMockServerPlayerInLevel(), test.makeMockServerPlayerInLevel());
    }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE)
    public void tableMenuDistanceAndStaleIdentity(GameTestHelper test) {
        ResearchTableRuntimeChecks.tableMenuDistanceAndStaleIdentity(test, test.makeMockServerPlayerInLevel());
    }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE)
    public void tableInventoryComponentsAndCountsPersist(GameTestHelper test) {
        ResearchTableRuntimeChecks.tableInventoryComponentsAndCountsPersist(test, test.makeMockServerPlayerInLevel());
    }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE)
    public void tableBreakEvacuatesEachHalfOnce(GameTestHelper test) {
        ResearchTableRuntimeChecks.tableBreakEvacuatesEachHalfOnce(test, test.makeMockServerPlayerInLevel());
    }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE)
    public void tablePairAcrossChunkBoundaryPersists(GameTestHelper test) {
        ResearchTableRuntimeChecks.tablePairAcrossChunkBoundaryPersists(test, test.makeMockServerPlayerInLevel());
    }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE, timeoutTicks = 600)
    public void tablePairActuallyUnloadsAndReloads(GameTestHelper test) {
        ResearchTableRuntimeChecks.tablePairActuallyUnloadsAndReloads(test, test.makeMockServerPlayerInLevel());
    }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE)
    public void tableFirstNativeTickRepairsReloadedOrphans(GameTestHelper test) {
        ResearchTableRuntimeChecks.tableFirstNativeTickRepairsReloadedOrphans(test, test.makeMockServerPlayerInLevel());
    }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE)
    public void pendingDropSurvivesCollisionAndPersistence(GameTestHelper test) {
        DeferredDropRuntimeChecks.pendingDropSurvivesCollisionAndPersistence(test);
    }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE)
    public void quarantineRetainsUnknownData(GameTestHelper test) {
        DeferredDropRuntimeChecks.quarantineRetainsUnknownData(test);
    }

    // R06 invokes the shared handler/native service; these calls are not C2S dispatch.
    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE)
    public void primaryRequestConsumesPaperAndLastInk(GameTestHelper test) {
        ResearchNoteIssuanceRuntimeChecks.primaryRequestConsumesPaperAndLastInk(test, test.makeMockServerPlayerInLevel());
    }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE)
    public void existingNotesReuseWithoutResourcesOrRandomness(GameTestHelper test) {
        ResearchNoteIssuanceRuntimeChecks.existingNotesReuseWithoutResourcesOrRandomness(test, test.makeMockServerPlayerInLevel());
    }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE)
    public void primaryRequestRefusalsAreAtomic(GameTestHelper test) {
        ResearchNoteIssuanceRuntimeChecks.primaryRequestRefusalsAreAtomic(test, test.makeMockServerPlayerInLevel());
    }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE)
    public void primaryRequestUsesOnlyOrderedMainInventory(GameTestHelper test) {
        ResearchNoteIssuanceRuntimeChecks.primaryRequestUsesOnlyOrderedMainInventory(test, test.makeMockServerPlayerInLevel());
    }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE)
    public void fullInventoryReusesFreedPaperSlotOrDrops(GameTestHelper test) {
        ResearchNoteIssuanceRuntimeChecks.fullInventoryReusesFreedPaperSlotOrDrops(test, test.makeMockServerPlayerInLevel());
    }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE)
    public void nativeInkPreservesCreativeAndUnbreakable(GameTestHelper test) {
        ResearchNoteIssuanceRuntimeChecks.nativeInkPreservesCreativeAndUnbreakable(test, test.makeMockServerPlayerInLevel());
    }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE)
    public void nativeUnbreakingUsesExactlyOnePlayerDraw(GameTestHelper test) {
        ResearchNoteIssuanceRuntimeChecks.nativeUnbreakingUsesExactlyOnePlayerDraw(test, test.makeMockServerPlayerInLevel());
    }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE)
    public void invalidGenerationLeavesResourcesAndKnowledgeUnchanged(GameTestHelper test) {
        ResearchNoteIssuanceRuntimeChecks.invalidGenerationLeavesResourcesAndKnowledgeUnchanged(test, test.makeMockServerPlayerInLevel());
    }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE)
    public void deliveryFailurePreservesPaperInkAndKnowledge(GameTestHelper test) {
        ResearchNoteIssuanceRuntimeChecks.deliveryFailurePreservesPaperInkAndKnowledge(test, test.makeMockServerPlayerInLevel());
    }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE)
    public void noteComponentsPersistAndEncode(GameTestHelper test) { ResearchNoteRuntimeChecks.noteComponentsPersistAndEncode(test); }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE)
    public void solvedNoteLearnsOnceAndTransfers(GameTestHelper test) {
        ResearchNoteRuntimeChecks.solvedNoteLearnsOnceAndTransfers(test, test.makeMockServerPlayerInLevel(), null);
    }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE)
    public void noteRefusalsAndWarpUseOriginalRules(GameTestHelper test) {
        ResearchNoteRuntimeChecks.noteRefusalsAndWarpUseOriginalRules(test, test.makeMockServerPlayerInLevel(), null);
    }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE)
    public void originalScribingRecipesAndInkBounds(GameTestHelper test) { ResearchNoteRuntimeChecks.originalScribingRecipesAndInkBounds(test); }

    // Vanilla Fabric mock proves the actual shared handler's state transitions;
    // negotiated S2C bytes are separately asserted by the NeoForge wrappers.
    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE)
    public void secondaryPurchaseSpendsExactlyAndReplayIsIdempotent(GameTestHelper test) {
        ResearchPurchaseRuntimeChecks.secondaryPurchaseSpendsExactlyAndReplayIsIdempotent(test, test.makeMockServerPlayerInLevel(), null);
    }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE)
    public void secondaryPurchaseRejectsPartialFundsAtomically(GameTestHelper test) {
        ResearchPurchaseRuntimeChecks.secondaryPurchaseRejectsPartialFundsAtomically(test, test.makeMockServerPlayerInLevel(), null);
    }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE)
    public void secondaryPurchaseRequiresClueAndHiddenParents(GameTestHelper test) {
        ResearchPurchaseRuntimeChecks.secondaryPurchaseRequiresClueAndHiddenParents(test, test.makeMockServerPlayerInLevel(), null);
    }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE)
    public void secondaryPurchaseWarpAndPlayerPersistence(GameTestHelper test) {
        ResearchPurchaseRuntimeChecks.secondaryPurchaseWarpAndPlayerPersistence(test, test.makeMockServerPlayerInLevel(), null);
    }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE)
    public void bookshelfWandCreatesOneFloatingBook(GameTestHelper test) { RuntimeChecks.bookshelfWandCreatesOneFloatingBook(test); }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE)
    public void normalBookOpensWithoutGrantingArbitraryResearch(GameTestHelper test) {
        RuntimeChecks.normalBookOpensWithoutGrantingArbitraryResearch(test, test.makeMockServerPlayerInLevel());
    }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE)
    public void originalEntryRecipes(GameTestHelper test) { RuntimeChecks.originalEntryRecipes(test); }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE)
    public void bootstrapAndItemComponents(GameTestHelper test) { RuntimeChecks.bootstrapAndItemComponents(test); }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE)
    public void playerKnowledgeMixinPersists(GameTestHelper test) { RuntimeChecks.playerKnowledgeMixinPersists(test); }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE)
    public void jarDropRetainsEssentia(GameTestHelper test) { RuntimeChecks.jarDropRetainsEssentia(test); }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE)
    public void nodeDrainPreservesTc4Rounding(GameTestHelper test) { RuntimeChecks.nodeDrainPreservesTc4Rounding(test); }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE)
    public void thaumometerNodeScanUsesTc4Rewards(GameTestHelper test) { RuntimeChecks.thaumometerNodeScanUsesTc4Rewards(test); }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE)
    public void thaumometerBlockPickSharesDroppedItemIdentity(GameTestHelper test) { RuntimeChecks.thaumometerBlockPickSharesDroppedItemIdentity(test); }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE, timeoutTicks = 400)
    public void advancedGolemsGainSlotSpeedRangeAndKeepTheirFlag(GameTestHelper test) {
        thaumcraft.common.testing.GolemAdvancedRuntimeChecks.run(test);
    }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE, timeoutTicks = 1700)
    public void golemAccessoriesInstallChangeStatsAndSurviveTheBell(GameTestHelper test) {
        thaumcraft.common.testing.GolemDecorationsRuntimeChecks.run(test);
    }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE, timeoutTicks = 1900)
    public void entropySwitchesChangeWhatRealGolemsFetchEmptyAndSort(GameTestHelper test) {
        thaumcraft.common.testing.GolemItemMatchRuntimeChecks.run(test);
    }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE, timeoutTicks = 1000)
    public void entropyQueuesTheFarthestConnectedWaterForLiquidGolems(GameTestHelper test) {
        thaumcraft.common.testing.GolemLiquidQueueRuntimeChecks.run(test);
    }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE)
    public void crystalBlocksDropSixOwnShards(GameTestHelper test) {
        thaumcraft.common.testing.CrystalDropRuntimeChecks.run(test);
    }
}
