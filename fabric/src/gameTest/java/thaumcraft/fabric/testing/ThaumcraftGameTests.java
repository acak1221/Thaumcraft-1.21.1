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
import thaumcraft.common.testing.FamilyBlockStatesRuntimeChecks;
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
import thaumcraft.common.testing.SurfaceStructurePlacementRuntimeChecks;
import thaumcraft.common.testing.VillageComponentPlacementRuntimeChecks;
import thaumcraft.common.testing.VillageTradeRuntimeChecks;
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
    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE, timeoutTicks = 100, batch = "review_followup_resources")
    public void followupDeathBucketConservesInputs(GameTestHelper test) {
        thaumcraft.common.testing.ResourceSafetyRuntimeChecks.deathBucketConservesInputs(test);
    }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE, timeoutTicks = 100, batch = "review_followup_resources")
    public void followupOculusPreservesNodeOnFailure(GameTestHelper test) {
        thaumcraft.common.testing.ResourceSafetyRuntimeChecks.oculusPreservesNodeOnFailure(test);
    }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE, batch = "review_followup_rarity")
    public void followupLootContainerRarity(GameTestHelper test) {
        thaumcraft.common.testing.LootContainerRarityRuntimeChecks.pickPlaceAndTooltip(test);
    }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE, batch = "review_followup_boss")
    public void followupPraetorHasOnePersistentChampion(GameTestHelper test) {
        thaumcraft.common.testing.WorldBossFollowupRuntimeChecks.praetorHasOnePersistentChampion(test);
    }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE, batch = "review_followup_boss")
    public void followupRestoredPraetorWarpHitsOnce(GameTestHelper test) {
        thaumcraft.common.testing.WorldBossFollowupRuntimeChecks.restoredPraetorWarpHitsOnce(test);
    }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE, batch = "review_followup_generation")
    public void followupGenerationRespectsDimensionPolicies(GameTestHelper test) {
        thaumcraft.common.testing.WorldBossFollowupRuntimeChecks.generationRespectsDimensionPolicies(test);
    }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE, batch = "review_followup_grindstone")
    public void followupSceptreGrindstonePreservesCapacity(GameTestHelper test) {
        thaumcraft.common.testing.WandSceptreGrindstoneRuntimeChecks.run(test);
    }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE, batch = "review_followup_loot")
    public void followupLootIncludesDatapackOverrides(GameTestHelper test) {
        LootSourceRegressionChecks.run(test);
    }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE, timeoutTicks = 300, batch = "review_api_thaumatorium")
    public void reviewThaumatoriumUsesServerRecipeSnapshot(GameTestHelper test) {
        thaumcraft.common.testing.ProgrammaticRecipeRuntimeChecks.thaumatoriumOpeningSnapshot(test);
    }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE, timeoutTicks = 300, batch = "review_api_recipes")
    public void reviewProgrammaticArcaneAndCrucibleRecipes(GameTestHelper test) {
        thaumcraft.common.testing.ProgrammaticRecipeRuntimeChecks.arcaneAndCrucible(test);
    }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE, timeoutTicks = 400, batch = "review_api_infusion")
    public void reviewProgrammaticInfusionsAndVirtualComparator(GameTestHelper test) {
        thaumcraft.common.testing.ProgrammaticRecipeRuntimeChecks.infusionsAndVirtualComparator(test);
    }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE, timeoutTicks = 100, batch = "review_death_fluid")
    public void reviewDeathFluidConservationAndDamage(GameTestHelper test) {
        thaumcraft.common.testing.DeathFluidRuntimeChecks.conservationAndDamage(test);
    }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE, batch = "review_pedestal")
    public void reviewPedestalConservesWholeNodeVis(GameTestHelper test) {
        thaumcraft.common.testing.WandPedestalRegressionChecks.conservesWholeNodeVis(test);
    }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE, batch = "review_pedestal")
    public void reviewPedestalPartialAndCompoundCharge(GameTestHelper test) {
        thaumcraft.common.testing.WandPedestalRegressionChecks.fillsPartialVisAndConvertsOnlyWithFocus(test);
    }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE, batch = "review_pedestal_sync")
    public void reviewPedestalInventoryUpdatesClients(GameTestHelper test) {
        thaumcraft.common.testing.WandPedestalRegressionChecks.inventoryChangesQueueNativeClientUpdates(test);
    }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE, batch = "review_regressions")
    public void reviewWandHandSelection(GameTestHelper test) {
        thaumcraft.common.testing.WandHandSelectionRuntimeChecks.run(test);
    }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE, batch = "review_mirrors")
    public void reviewSingletonMirrorPlacement(GameTestHelper test) {
        thaumcraft.common.testing.MirrorRuntimeChecks.singletonPlacementRetainsBothMirrorKinds(test);
    }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE, batch = "review_mirrors")
    public void reviewCrossDimensionMirrors(GameTestHelper test) {
        thaumcraft.common.testing.MirrorRuntimeChecks.crossDimensionPairsTransferWithoutLosingStock(test);
    }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE, timeoutTicks = 500000, batch = "review_mirror_cache")
    public void reviewEssentiaMirrorRefreshesSources(GameTestHelper test) {
        thaumcraft.common.testing.MirrorRuntimeChecks.essentiaMirrorDiscoversReplacementSourceAfterBackoff(test);
    }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE, batch = "review_regressions")
    public void reviewCraftingEventSeesUnpaidInputs(GameTestHelper test) {
        thaumcraft.common.testing.ReviewRegressionRuntimeChecks.craftingEventInputs(test);
    }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE, batch = "review_regressions")
    public void reviewAccessoryDimensionResendsUnchangedContents(GameTestHelper test) {
        thaumcraft.common.testing.ReviewRegressionRuntimeChecks.accessoryDimensionSnapshot(test);
    }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE, batch = "review_regressions")
    public void reviewTradeUsesNativeInteractionRange(GameTestHelper test) {
        thaumcraft.common.testing.ReviewRegressionRuntimeChecks.tradeNativeReach(test);
    }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE, batch = "api_cache_ownership")
    public void apiRecipeFirstAnswerCannotPoisonLaterAnswers(GameTestHelper test) {
        thaumcraft.common.testing.ApiCacheOwnershipRuntimeChecks.recipeAnswer(test, false);
    }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE, batch = "api_cache_ownership")
    public void apiRecipeFirstAnswerCannotPoisonElementTypes(GameTestHelper test) {
        thaumcraft.common.testing.ApiCacheOwnershipRuntimeChecks.recipeAnswer(test, true);
    }
    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE, batch = "nugget_ownership")
    public void nuggetOwnership(GameTestHelper test) { thaumcraft.common.testing.NuggetOwnershipRuntimeChecks.run(test); }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE, timeoutTicks = 200, batch = "g23_golem")
    public void g23BossGolemFight(GameTestHelper test) { thaumcraft.common.testing.BossFightRuntimeChecks.fight(test, "golem", false, false); }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE, batch = "g28_gear")
    public void g28GearDiscountPayments(GameTestHelper test) { thaumcraft.common.testing.GearHookAuditChecks.discountPayments(test); }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE, batch = "g28_gear")
    public void g28GearCrimsonAttack(GameTestHelper test) { thaumcraft.common.testing.GearHookAuditChecks.crimsonAttack(test); }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE, batch = "g28_gear")
    public void g28GearCrusherBreak(GameTestHelper test) { thaumcraft.common.testing.GearHookAuditChecks.crusherBreak(test); }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE, batch = "g28_gear")
    public void g28GearCreativeRepair(GameTestHelper test) { thaumcraft.common.testing.GearHookAuditChecks.creativeRepair(test); }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE, batch = "g28_gear")
    public void g28GearNativeUseAndAmulets(GameTestHelper test) { thaumcraft.common.testing.GearHookAuditChecks.nativeUseAndAmulets(test); }
    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE, timeoutTicks = 200, batch = "g23_warden")
    public void g23BossWardenFight(GameTestHelper test) { thaumcraft.common.testing.BossFightRuntimeChecks.fight(test, "warden", false, false); }
    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE, timeoutTicks = 200, batch = "g23_praetor")
    public void g23BossPraetorFight(GameTestHelper test) { thaumcraft.common.testing.BossFightRuntimeChecks.fight(test, "praetor", false, false); }
    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE, timeoutTicks = 200, batch = "g23_giant")
    public void g23BossGiantFight(GameTestHelper test) { thaumcraft.common.testing.BossFightRuntimeChecks.fight(test, "giant", false, false); }
    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE, timeoutTicks = 200, batch = "g23_taintacle")
    public void g23BossTaintacleFight(GameTestHelper test) { thaumcraft.common.testing.BossFightRuntimeChecks.fight(test, "taintacle", false, false); }
    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE, timeoutTicks = 200, batch = "g23_pech")
    public void g23BossPechFight(GameTestHelper test) { thaumcraft.common.testing.BossFightRuntimeChecks.fight(test, "pech", false, false); }
    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE, timeoutTicks = 200, batch = "g23_bar")
    public void g23BossPraetorBar(GameTestHelper test) { thaumcraft.common.testing.BossFightRuntimeChecks.fight(test, "praetor", true, false); }
    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE, timeoutTicks = 200, batch = "g23_lock")
    public void g23BossDeathPlacement(GameTestHelper test) { thaumcraft.common.testing.BossFightRuntimeChecks.fight(test, "praetor", false, true); }
    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE, timeoutTicks = 300, batch = "g26_world_powered_bore")
    public void deviceWorldPoweredBoreMinesAndReturnsItem(GameTestHelper test) {
        thaumcraft.common.testing.DeviceWorldRuntimeChecks.poweredBore(test);
    }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE, timeoutTicks = 1160, batch = "g26_world_tube_buffer")
    public void deviceWorldTubeAndBufferDeliverToConsumer(GameTestHelper test) {
        thaumcraft.common.testing.DeviceWorldRuntimeChecks.tubeAndBuffer(test);
    }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE, timeoutTicks = 80, batch = "g26_world_hungry_chest")
    public void deviceWorldHungryChestCollectsPhysicalInput(GameTestHelper test) {
        thaumcraft.common.testing.DeviceWorldRuntimeChecks.hungryChest(test);
    }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE, timeoutTicks = 180, batch = "g26_world_relay")
    public void deviceWorldRelayPaysActualConsumer(GameTestHelper test) {
        thaumcraft.common.testing.VisNetLifecycleChecks.worldScheduled(test);
    }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE, timeoutTicks = 360, batch = "g26_world_advanced_furnace")
    public void deviceWorldAdvancedFurnacePaysAndDelivers(GameTestHelper test) {
        thaumcraft.common.testing.AdvancedFurnaceRuntimeChecks.worldScheduled(test);
    }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE, timeoutTicks = 140, batch = "g26_world_thaumatorium")
    public void deviceWorldThaumatoriumPaysAndDelivers(GameTestHelper test) {
        thaumcraft.common.testing.ThaumatoriumRuntimeChecks.worldScheduled(test);
    }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE, timeoutTicks = 90, batch = "g26_world_deconstruction")
    public void deviceWorldDeconstructionPaysAndDelivers(GameTestHelper test) {
        thaumcraft.common.testing.DeviceWorldRuntimeChecks.deconstruction(test);
    }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE, timeoutTicks = 260, batch = "g26_world_furnace")
    public void deviceWorldFurnacePaysAndDelivers(GameTestHelper test) {
        thaumcraft.common.testing.DeviceWorldRuntimeChecks.furnace(test);
    }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE, timeoutTicks = 160, batch = "g26_world_centrifuge")
    public void deviceWorldCentrifugePaysAndDelivers(GameTestHelper test) {
        thaumcraft.common.testing.DeviceWorldRuntimeChecks.centrifuge(test);
    }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE, timeoutTicks = 1120, batch = "g26_world_crystallizer")
    public void deviceWorldCrystallizerPaysAndDelivers(GameTestHelper test) {
        thaumcraft.common.testing.DeviceWorldRuntimeChecks.crystallizer(test);
    }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE, timeoutTicks = 1550, batch = "g21_carry")
    public void g21CarryAndRefusals(GameTestHelper test) { thaumcraft.common.testing.GolemEndToEndRuntimeChecks.carryAndRefusals(test); }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE, timeoutTicks = 120, batch = "goal_lifecycle")
    public void goalLifecycleDecorationClosesGate(GameTestHelper test) { thaumcraft.common.testing.GolemEndToEndRuntimeChecks.decorationClosesRunningGate(test); }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE, timeoutTicks = 1050, batch = "g21_gather")
    public void g21GatherAndAir(GameTestHelper test) { thaumcraft.common.testing.GolemEndToEndRuntimeChecks.gatherAndAir(test); }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE, timeoutTicks = 1550, batch = "g21_harvest_lumber")
    public void g21HarvestAndLumberDelivery(GameTestHelper test) { thaumcraft.common.testing.GolemEndToEndRuntimeChecks.harvestAndLumberDelivery(test); }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE, timeoutTicks = 550, batch = "g21_fractional_drops")
    public void g21GatherFractionalDropPositions(GameTestHelper test) { thaumcraft.common.testing.GolemEndToEndRuntimeChecks.gatherFractionalDropPositions(test); }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE, timeoutTicks = 750, batch = "g21_use")
    public void g21UsePlantsExactlyOneSeed(GameTestHelper test) { thaumcraft.common.testing.GolemEndToEndRuntimeChecks.usePlantsExactlyOneSeed(test); }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE, timeoutTicks = 750, batch = "g21_use_empty_space")
    public void g21UseEmptySpacePlantsExactlyOneSeed(GameTestHelper test) { thaumcraft.common.testing.GolemEndToEndRuntimeChecks.useEmptySpacePlantsExactlyOneSeed(test); }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE, timeoutTicks = 1150, batch = "g21_water_measurement")
    public void g21MeasuredWaterPickupRadius(GameTestHelper test) { thaumcraft.common.testing.GolemEndToEndRuntimeChecks.measuredWaterPickupRadius(test); }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE, timeoutTicks = 1350, batch = "g21_butcher")
    public void g21ButcherLeavesBreedingPair(GameTestHelper test) { thaumcraft.common.testing.GolemEndToEndRuntimeChecks.butcherLeavesBreedingPair(test); }
    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE)
    public void sound_registry_covers_bundled_events(GameTestHelper helper) {
        // The mod's own class: on NeoForge this harness is a separate JPMS module.
        try (var input = thaumcraft.common.Thaumcraft.class.getResourceAsStream("/assets/thaumcraft/sounds.json")) {
            if (input == null) throw new IllegalStateException("Bundled sounds.json missing");
            var sounds = com.google.gson.JsonParser.parseReader(new java.io.InputStreamReader(input,
                    java.nio.charset.StandardCharsets.UTF_8)).getAsJsonObject();
            for (String id : sounds.keySet()) {
                var key = net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("thaumcraft", id);
                helper.assertTrue(net.minecraft.core.registries.BuiltInRegistries.SOUND_EVENT.containsKey(key),
                        "Unregistered bundled sound " + key);
                helper.assertTrue(net.minecraft.core.registries.BuiltInRegistries.SOUND_EVENT.get(key).getLocation().equals(key),
                        "Registered sound points at a different audio location: " + key);
            }
            helper.succeed();
        } catch (java.io.IOException failure) {
            throw new IllegalStateException(failure);
        }
    }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE, batch = "g12_gui")
    public void guiMagicBoxRejectedShiftConservesItems(GameTestHelper test) {
        thaumcraft.common.testing.GuiRejectionRuntimeChecks.magicBox(test);
    }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE, timeoutTicks = 1200, batch = "g7_save_chunks")
    public void saveCycleChunkDiskReload(GameTestHelper test) {
        thaumcraft.common.testing.SaveCycleRuntimeChecks.chunks(test);
    }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE, batch = "g7_save_player")
    public void saveCyclePlayerDiskRelogin(GameTestHelper test) {
        thaumcraft.common.testing.SaveCycleRuntimeChecks.player(test);
    }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE, batch = "g7_save_world")
    public void saveCycleWorldDiskReload(GameTestHelper test) {
        thaumcraft.common.testing.SaveCycleRuntimeChecks.savedData(test);
    }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE)
    public void interactionPickaxeConsumesPaidScan(GameTestHelper test) {
        thaumcraft.common.testing.InteractionResultRuntimeChecks.pickaxeConsumesPaidScan(test);
    }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE)
    public void interactionShovelConsumesOnlyPaidPlacement(GameTestHelper test) {
        thaumcraft.common.testing.InteractionResultRuntimeChecks.shovelConsumesOnlyPaidPlacement(test);
    }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE)
    public void blockDropAspectJar(GameTestHelper test) {
        thaumcraft.common.testing.BlockDropRuntimeChecks.jarRemoval(test, false);
    }
    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE)
    public void blockDropVoidJar(GameTestHelper test) {
        thaumcraft.common.testing.BlockDropRuntimeChecks.jarRemoval(test, true);
    }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE)
    public void blockDropHungryChest(GameTestHelper test) {
        thaumcraft.common.testing.BlockDropRuntimeChecks.inventoryRemoval(test, "hungry_chest");
    }
    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE)
    public void blockDropPedestal(GameTestHelper test) {
        thaumcraft.common.testing.BlockDropRuntimeChecks.inventoryRemoval(test, "arcane_pedestal");
    }
    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE)
    public void blockDropDeconstruction(GameTestHelper test) {
        thaumcraft.common.testing.BlockDropRuntimeChecks.inventoryRemoval(test, "deconstruction_table");
    }
    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE)
    public void blockDropWorkbench(GameTestHelper test) {
        thaumcraft.common.testing.BlockDropRuntimeChecks.inventoryRemoval(test, "arcane_workbench");
    }
    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE)
    public void blockDropAlchemyFurnace(GameTestHelper test) {
        thaumcraft.common.testing.BlockDropRuntimeChecks.inventoryRemoval(test, "alchemy_furnace");
    }
    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE)
    public void blockDropFocalManipulator(GameTestHelper test) {
        thaumcraft.common.testing.BlockDropRuntimeChecks.inventoryRemoval(test, "focal_manipulator");
    }
    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE)
    public void blockDropThaumatorium(GameTestHelper test) {
        thaumcraft.common.testing.BlockDropRuntimeChecks.inventoryRemoval(test, "thaumatorium");
    }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE)
    public void blockDropMagicBoxCreative(GameTestHelper test) {
        thaumcraft.common.testing.BlockDropRuntimeChecks.magicBoxRemoval(test, "creative");
    }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE)
    public void blockDropMagicBoxSilk(GameTestHelper test) {
        thaumcraft.common.testing.BlockDropRuntimeChecks.magicBoxRemoval(test, "silk");
    }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE)
    public void blockDropMagicBoxExplosion(GameTestHelper test) {
        thaumcraft.common.testing.BlockDropRuntimeChecks.magicBoxRemoval(test, "explosion");
    }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE)
    public void blockDropMagicBoxDestroy(GameTestHelper test) {
        thaumcraft.common.testing.BlockDropRuntimeChecks.magicBoxNonPlayerRemoval(test, false);
    }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE)
    public void blockDropMagicBoxSetBlock(GameTestHelper test) {
        thaumcraft.common.testing.BlockDropRuntimeChecks.magicBoxNonPlayerRemoval(test, true);
    }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE, batch = "permission_trunk_other_viewer")
    public void permissionTrunkOtherViewer(GameTestHelper test) {
        thaumcraft.common.testing.PermissionRuntimeChecks.trunkRejectsOtherViewer(test);
    }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE, batch = "permission_trunk_closed_menu")
    public void permissionTrunkClosedMenu(GameTestHelper test) {
        thaumcraft.common.testing.PermissionRuntimeChecks.trunkRejectsClosedMenu(test);
    }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE, batch = "permission_trunk_dead")
    public void permissionTrunkDeadViewer(GameTestHelper test) {
        thaumcraft.common.testing.PermissionRuntimeChecks.trunkRejectsDeadViewer(test);
    }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE, batch = "permission_denied_break")
    public void permissionSpectatorCannotBreakContainers(GameTestHelper test) {
        thaumcraft.common.testing.PermissionRuntimeChecks.spectatorCannotBreakContainers(test);
    }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE, batch = "permission_research_conversion")
    public void permissionResearchConversionSpectator(GameTestHelper test) {
        thaumcraft.common.testing.PermissionRuntimeChecks.researchConversionRejectsSpectator(test);
    }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE, batch = "permission_crucible_remote")
    public void permissionCrucibleRemoteWand(GameTestHelper test) {
        thaumcraft.common.testing.PermissionRuntimeChecks.crucibleRejectsRemoteWand(test);
    }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE, batch = "permission_trunk_spectator")
    public void permissionTrunkSpectator(GameTestHelper test) {
        thaumcraft.common.testing.PermissionRuntimeChecks.trunkRejectsSpectator(test);
    }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE, batch = "permission_trunk_remote")
    public void permissionTrunkRemote(GameTestHelper test) {
        thaumcraft.common.testing.PermissionRuntimeChecks.trunkRejectsRemote(test);
    }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE, batch = "rejected_session_custody")
    public void rejectedInfusionSurvivesPhysicalBlockRemoval(GameTestHelper test) {
        thaumcraft.common.testing.RejectedOwnerRuntimeChecks.infusion(test);
    }
    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE, batch = "rejected_owner_custody")
    public void rejectedPedestalSurvivesPhysicalBlockRemoval(GameTestHelper test) {
        thaumcraft.common.testing.RejectedOwnerRuntimeChecks.pedestal(test);
    }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE, timeoutTicks = 20, batch = "event_lifecycle")
    public void regenerationQueueLifecycle(GameTestHelper test) {
        thaumcraft.common.testing.EventLifecycleRuntimeChecks.regenerationQueueLifecycle(test);
    }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE, timeoutTicks = 100, batch = "perf_cache_retirement")
    public void mirrorCacheRetirement(GameTestHelper test) {
        thaumcraft.common.testing.PerformanceRuntimeChecks.mirrorCacheRetirement(test);
    }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE, timeoutTicks = 200, batch = "perf_chunk_footprint")
    public void regenerationFootprint(GameTestHelper test) {
        thaumcraft.common.testing.PerformanceRuntimeChecks.regenerationFootprint(test);
    }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE, timeoutTicks = 200, batch = "perf_structure_candidates")
    public void structureCandidateFootprint(GameTestHelper test) {
        thaumcraft.common.testing.PerformanceRuntimeChecks.structureCandidateFootprint(test);
    }
    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE, timeoutTicks = 200, batch = "mound_boundary")
    public void moundBoundaryIsCompleteOrRefused(GameTestHelper test) {
        thaumcraft.common.testing.MoundBoundaryRuntimeChecks.run(test);
    }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE, batch = "mound_rng_isolation")
    public void moundGenerationRandomIsIsolated(GameTestHelper test) {
        thaumcraft.common.testing.MoundRandomRuntimeChecks.generationRandomIsIsolated(test);
    }

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

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE, timeoutTicks = 200, batch = "g31_focus_launches")
    public void g31FocusCastingLaunchesAndPlayerCooldowns(GameTestHelper test) {
        thaumcraft.common.testing.FocusCastingAuditChecks.launches(test);
    }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE, timeoutTicks = 200, batch = "g31_focus_impact")
    public void g31PechCastImpactCannotReenter(GameTestHelper test) {
        thaumcraft.common.testing.FocusCastingAuditChecks.pechImpactReentry(test);
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

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE, timeoutTicks = 1200, batch = "tc_isolated_construct_centrifuge")
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

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE, timeoutTicks = 100)
    public void everyStateOfTheVariantBlocksHasAModelEntryAndTheGroupBlocksOwnOnlyTheirMetadata(GameTestHelper test) {
        FamilyBlockStatesRuntimeChecks.run(test);
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

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE, timeoutTicks = 600, batch = "g24_comparator")
    public void g24InfusionComparator(GameTestHelper test) {
        thaumcraft.common.testing.InfusionRitualAuditChecks.comparator(test);
    }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE, timeoutTicks = 600, batch = "g24_payment")
    public void g24InfusionProgressivePayment(GameTestHelper test) {
        thaumcraft.common.testing.InfusionRitualAuditChecks.progressivePaymentAndFinish(test);
    }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE, timeoutTicks = 600, batch = "g24_interrupt")
    public void g24InfusionInterruptions(GameTestHelper test) {
        thaumcraft.common.testing.InfusionRitualAuditChecks.interruptions(test);
    }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE, timeoutTicks = 600, batch = "g24_measure")
    public void g24InfusionMeasuredInstability(GameTestHelper test) {
        thaumcraft.common.testing.InfusionRitualAuditChecks.measuredInstability(test);
    }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE, timeoutTicks = 600, batch = "g24_missing")
    public void g24InfusionMissingIngredientRate(GameTestHelper test) {
        thaumcraft.common.testing.InfusionRitualAuditChecks.measuredMissingIngredients(test);
    }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE, timeoutTicks = 600, batch = "tc_isolated_infusion_altar")
    public void infusionAltarFormsAndExecutesAllInstabilityOutcomes(GameTestHelper test) {
        InfusionStructureRuntimeChecks.run(test, test.makeMockServerPlayerInLevel());
    }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE)
    public void essentiaTubesPullConserveAndToggleSides(GameTestHelper test) {
        TubeRuntimeChecks.run(test, test.makeMockServerPlayerInLevel());
    }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE, timeoutTicks = 240, batch = "g5_vis_essentia")
    public void furnaceAlembicCallbackConservesEssentia(GameTestHelper test) {
        thaumcraft.common.testing.VisEssentiaAccountingChecks.furnaceAlembicCallback(test);
    }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE, timeoutTicks = 240, batch = "g5_vis_network")
    public void g5PrimalNetworkAccounting(GameTestHelper test) {
        thaumcraft.common.testing.VisEssentiaAccountingChecks.primalNetwork(test);
    }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE, timeoutTicks = 240, batch = "g5_vis_caps")
    public void g5CapsAndSpendingAccounting(GameTestHelper test) {
        thaumcraft.common.testing.VisEssentiaAccountingChecks.capsAndSpending(test);
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
        PlantRuntimeChecks.run(test, test.makeMockServerPlayerInLevel());
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

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE, batch = "crafting_audit_grid")
    public void craftingAuditGridBoundaries(GameTestHelper test) {
        thaumcraft.common.testing.CraftingAuditRuntimeChecks.gridBoundaries(test);
    }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE, batch = "crafting_audit_shaped")
    public void craftingAuditShapedDamage(GameTestHelper test) {
        thaumcraft.common.testing.CraftingAuditRuntimeChecks.damagedIngredient(test, false);
    }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE, batch = "crafting_audit_shapeless")
    public void craftingAuditShapelessDamage(GameTestHelper test) {
        thaumcraft.common.testing.CraftingAuditRuntimeChecks.damagedIngredient(test, true);
    }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE, batch = "node_time_audit_curves")
    public void nodeTimeAuditCurves(GameTestHelper test) {
        thaumcraft.common.testing.NodeTimeAuditRuntimeChecks.curves(test);
    }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE, batch = "node_time_audit_zero")
    public void nodeTimeAuditZeroCapacityDecay(GameTestHelper test) {
        thaumcraft.common.testing.NodeTimeAuditRuntimeChecks.zeroCapacityDecay(test);
    }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE, batch = "node_time_audit_growth")
    public void nodeTimeAuditGrowth(GameTestHelper test) {
        thaumcraft.common.testing.NodeTimeAuditRuntimeChecks.growth(test);
    }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE)
    public void naturalNodesDrainAndRegenerate(GameTestHelper test) {
        NaturalNodeRuntimeChecks.run(test, test.makeMockServerPlayerInLevel());
    }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE, timeoutTicks = 2400)
    public void surfaceStructuresReachTheOverworld(GameTestHelper test) {
        SurfaceStructurePlacementRuntimeChecks.run(test);
    }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE, timeoutTicks = 2400)
    public void villageComponentsReachTheOverworld(GameTestHelper test) {
        VillageComponentPlacementRuntimeChecks.run(test);
    }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE, timeoutTicks = 2400)
    public void villageTradesReachTheVillagers(GameTestHelper test) {
        VillageTradeRuntimeChecks.run(test);
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

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE, batch = "tc_isolated_table_break")
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

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE, batch = "tc_isolated_table_orphan_repair")
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

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE, batch = "tc_isolated_bookshelf_wand")
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

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE, timeoutTicks = 1700, batch = "tc_isolated_golem_decorations")
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

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE, timeoutTicks = 1600, batch = "tc_isolated_golem_fetter")
    public void golemFetterHoldsGolemsWhileItsRedstoneSignalLasts(GameTestHelper test) {
        thaumcraft.common.testing.GolemFetterRuntimeChecks.run(test);
    }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE, timeoutTicks = 400, batch = "tc_isolated_traveling_trunk_item")
    public void travelingTrunkItemUpgradesMenuBellDamageAndSaving(GameTestHelper test) {
        thaumcraft.common.testing.TravelingTrunkRuntimeChecks.basics(test);
    }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE, timeoutTicks = 1600, batch = "tc_isolated_traveling_trunk_phases")
    public void travelingTrunkFollowsTeleportsDefendsAndPullsItems(GameTestHelper test) {
        thaumcraft.common.testing.TravelingTrunkRuntimeChecks.behavior(test);
    }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE, timeoutTicks = 1500, batch = "tc_isolated_golem_dart")
    public void golemDartLauncherShootsAtItsIntervalAndHurtsWhatItHits(GameTestHelper test) {
        thaumcraft.common.testing.GolemDartRuntimeChecks.run(test);
    }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE, timeoutTicks = 200)
    public void fireGolemCatchesAreDeliveredAlightForTwoSeconds(GameTestHelper test) {
        thaumcraft.common.testing.GolemFishingBurnRuntimeChecks.run(test);
    }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE, timeoutTicks = 200)
    public void elementalWandRodsBrewAssembleAndRefillTheirOwnPrimal(GameTestHelper test) {
        thaumcraft.common.testing.WandRodsRuntimeChecks.run(test);
    }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE, timeoutTicks = 200)
    public void staffRodsBrewAssembleStrikeAndRefill(GameTestHelper test) {
        thaumcraft.common.testing.StaffRodsRuntimeChecks.run(test);
    }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE)
    public void voidMetalAndTheVoidCapBrewCraftAndDiscount(GameTestHelper test) {
        thaumcraft.common.testing.VoidCapRuntimeChecks.run(test);
    }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE, timeoutTicks = 400)
    public void voidMetalGearMendsWithersAndWarps(GameTestHelper test) {
        thaumcraft.common.testing.VoidGearRuntimeChecks.run(test);
    }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE, timeoutTicks = 200)
    public void nativeClustersPurifyTripleAndMelt(GameTestHelper test) {
        thaumcraft.common.testing.ClusterRuntimeChecks.run(test);
    }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE, timeoutTicks = 200)
    public void original_object_aspects_and_aspect_inference(GameTestHelper test) {
        thaumcraft.common.testing.AspectCoverageRuntimeChecks.run(test);
    }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE, timeoutTicks = 200)
    public void arcane_furnace_nozzle_feeds_the_furnace_fire_essentia(GameTestHelper test) {
        thaumcraft.common.testing.ArcaneFurnaceNozzleRuntimeChecks.nozzleFeedsTheFurnaceFireEssentia(test);
    }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE, timeoutTicks = 200)
    public void theOriginalSmeltingBonusesAreRegistered(GameTestHelper test) {
        thaumcraft.common.testing.SmeltingBonusRuntimeChecks.run(test);
    }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE, timeoutTicks = 200)
    public void thePublicRecipeListHoldsEveryRecipeOfTheLevel(GameTestHelper test) {
        thaumcraft.common.testing.CraftingRecipeApiRuntimeChecks.run(test);
    }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE, timeoutTicks = 200)
    public void pickaxeOfTheCoreBurnsWearsAndFindsClusters(GameTestHelper test) {
        thaumcraft.common.testing.ElementalPickaxeRuntimeChecks.run(test);
    }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE, timeoutTicks = 200)
    public void hoeOfGrowthTillsFertilisesAndGrowsSaplings(GameTestHelper test) {
        thaumcraft.common.testing.ElementalHoeRuntimeChecks.run(test);
    }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE, timeoutTicks = 200)
    public void shovelOfTheEarthmoverMinesAndLaysSquares(GameTestHelper test) {
        thaumcraft.common.testing.ElementalShovelRuntimeChecks.run(test);
    }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE, timeoutTicks = 200, batch = "tc_isolated_zephyr_sword")
    public void swordOfTheZephyrLiftsBlowsAndSweeps(GameTestHelper test) {
        thaumcraft.common.testing.ElementalSwordRuntimeChecks.run(test);
    }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE, timeoutTicks = 200)
    public void voidThaumaturgeRobesMendWardAndDye(GameTestHelper test) {
        thaumcraft.common.testing.VoidRobeRuntimeChecks.run(test);
    }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE, timeoutTicks = 400)
    public void portableHoleCutsTunnelsAndGivesBlocksBack(GameTestHelper test) {
        thaumcraft.common.testing.PortableHoleRuntimeChecks.run(test);
    }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE, timeoutTicks = 200)
    public void thaumiumFortressWardsSetsAndMasks(GameTestHelper test) {
        thaumcraft.common.testing.FortressRuntimeChecks.run(test);
    }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE, timeoutTicks = 600, batch = "tc_isolated_essentia_reservoir")
    public void essentiaReservoirHoldsMixedEssentiaPullsAndBursts(GameTestHelper test) {
        thaumcraft.common.testing.ReservoirRuntimeChecks.run(test);
    }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE, timeoutTicks = 400)
    public void arcaneLevitatorLiftsItemsAndCreaturesAndStacks(GameTestHelper test) {
        thaumcraft.common.testing.LevitatorRuntimeChecks.run(test);
    }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE, timeoutTicks = 1000, batch = "m10_golem_reservoir")
    public void alchemyGolemsCollectAndDeliverMixedReservoirs(GameTestHelper test) {
        thaumcraft.common.testing.GolemReservoirRuntimeChecks.run(test);
    }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE, timeoutTicks = 500, batch = "m09_arcane_ear")
    public void arcaneEarListensToMatchingNotesAndPulsesRedstone(GameTestHelper test) {
        thaumcraft.common.testing.ArcaneEarRuntimeChecks.run(test);
    }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE, timeoutTicks = 400, batch = "r10_overstack")
    public void researchNoteWholeStacksSurviveNativeOwners(GameTestHelper test) {
        thaumcraft.common.testing.ResearchNoteOverstackRuntimeChecks.run(test, test.makeMockServerPlayerInLevel());
    }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE, timeoutTicks = 400, batch = "r10_copy")
    public void researchTableCopiesDiscoveriesWithExactCosts(GameTestHelper test) {
        thaumcraft.common.testing.ResearchNoteCopyRuntimeChecks.run(test, test.makeMockServerPlayerInLevel());
    }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE, timeoutTicks = 100, batch = "m08_warding")
    public void wardingPreservesOwnersStatesAndArchitectPayments(GameTestHelper test) {
        thaumcraft.common.testing.WardingRuntimeChecks.run(test);
    }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE, timeoutTicks = 200, batch = "m09_arcane_lamp")
    public void arcaneLampLightsDarkAirAndCleansItsOriginalRange(GameTestHelper test) {
        thaumcraft.common.testing.ArcaneLampRuntimeChecks.run(test);
    }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE, timeoutTicks = 200, batch = "m05_thaumatorium")
    public void thaumatoriumFormsPullsAndPaysNativeOutputs(GameTestHelper test) {
        thaumcraft.common.testing.ThaumatoriumRuntimeChecks.run(test);
    }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE, timeoutTicks = 1000, batch = "m07_enchantment_infusion")
    public void enchantmentInfusionPaysLevelsBeforeEssentiaAndRaisesActualCentre(GameTestHelper test) {
        thaumcraft.common.testing.EnchantmentInfusionRuntimeChecks.run(test);
    }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE, timeoutTicks = 400, batch = "m08_trade")
    public void tradeSwapsPayOnceAndRespectArchitectAndProtection(GameTestHelper test) {
        thaumcraft.common.testing.TradeRuntimeChecks.run(test);
    }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE, timeoutTicks = 200, batch = "m03_vis_charge")
    public void visChargeRelayCreditsTheActualWorkbenchWand(GameTestHelper test) {
        thaumcraft.common.testing.VisChargeRelayRuntimeChecks.run(test);
    }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE, timeoutTicks = 200, batch = "r12_warp")
    public void warpDispatchesOriginalBranchesWithNativeEffects(GameTestHelper test) {
        thaumcraft.common.testing.WarpRuntimeChecks.run(test);
    }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE, timeoutTicks = 200, batch = "g27_warp_sources")
    public void g27WarpOriginalSources(GameTestHelper test) { thaumcraft.common.testing.WarpRuntimeChecks.audit(test, "sources"); }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE, timeoutTicks = 200, batch = "g27_warp_craft")
    public void g27WarpRegisteredCraft(GameTestHelper test) { thaumcraft.common.testing.WarpRuntimeChecks.audit(test, "craft"); }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE, timeoutTicks = 200, batch = "g27_warp_research")
    public void g27WarpRegisteredResearch(GameTestHelper test) { thaumcraft.common.testing.WarpRuntimeChecks.audit(test, "research"); }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE, timeoutTicks = 200, batch = "g27_warp_rules")
    public void g27WarpDecayCapAndPersistence(GameTestHelper test) { thaumcraft.common.testing.WarpRuntimeChecks.audit(test, "rules"); }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE, timeoutTicks = 200, batch = "m11_brainy_zombies")
    public void brainyZombiesFightRageCureAndDropOriginalLoot(GameTestHelper test) {
        thaumcraft.common.testing.BrainyZombieRuntimeChecks.run(test);
    }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE, timeoutTicks = 400, batch = "r11_unknown_knowledge")
    public void unknownKnowledgeRevealsHiddenNotesAndRetriesFragments(GameTestHelper test) {
        thaumcraft.common.testing.UnknownKnowledgeRuntimeChecks.run(test, test.makeMockServerPlayerInLevel());
    }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE, timeoutTicks = 400, batch = "r12_item_clues")
    public void researchCluesRespectOriginalItemTriggersAndPersist(GameTestHelper test) {
        thaumcraft.common.testing.ResearchClueRuntimeChecks.itemCluesUseOriginalMetadataDamageAndNbt(test, test.makeMockServerPlayerInLevel());
    }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE, timeoutTicks = 400, batch = "r12_entity_scans")
    public void entityScansUseOriginalVariantsAndGrantNaturalClues(GameTestHelper test) {
        thaumcraft.common.testing.ResearchClueRuntimeChecks.entitiesUseNativeVariantsAndRealThaumometerTiming(test, test.makeMockServerPlayerInLevel());
    }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE, timeoutTicks = 200, batch = "m08_ward_behaviour")
    public void wardsPreserveSolidGroundAndFenceBehaviour(GameTestHelper test) {
        thaumcraft.common.testing.WardingBehaviourRuntimeChecks.run(test);
    }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE, timeoutTicks = 240, batch = "m08_excavation_special")
    public void excavationPreservesSpecialDropsAndDurableOutputs(GameTestHelper test) {
        thaumcraft.common.testing.ExcavationSpecialRuntimeChecks.run(test);
    }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE, timeoutTicks = 200, batch = "m07_equipment_enchantments")
    public void equipmentEnchantmentsAffectNativeGameplay(GameTestHelper test) {
        thaumcraft.common.testing.EquipmentEnchantmentRuntimeChecks.run(test);
    }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE, timeoutTicks = 2000, batch = "m07_custom_enchantments")
    public void customEnchantmentInfusionUsesNativeEquipmentAndPayments(GameTestHelper test) {
        thaumcraft.common.testing.CustomEnchantmentInfusionRuntimeChecks.run(test);
    }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE, timeoutTicks = 200, batch = "m09_bore_repair")
    public void boreRepairsItsActualToolWithOriginalCosts(GameTestHelper test) {
        thaumcraft.common.testing.BoreRepairRuntimeChecks.run(test);
    }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE, timeoutTicks = 200, batch = "m05_deconstruction_table")
    public void deconstructionConsumesItemsAndProducesOriginalResearchPoints(GameTestHelper test) {
        thaumcraft.common.testing.DeconstructionRuntimeChecks.run(test);
    }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE, timeoutTicks = 100, batch = "m09_growth_lamp")
    public void growthLampConsumesEssentiaAndFertilisesPlants(GameTestHelper test) {
        thaumcraft.common.testing.BiologicalLampRuntimeChecks.growth(test);
    }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE, timeoutTicks = 100, batch = "m09_fertility_lamp")
    public void fertilityLampConsumesEssentiaAndBreedsAnimals(GameTestHelper test) {
        thaumcraft.common.testing.BiologicalLampRuntimeChecks.fertility(test);
    }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE)
    public void crystalBlocksDropSixOwnShards(GameTestHelper test) {
        thaumcraft.common.testing.CrystalDropRuntimeChecks.run(test);
    }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE, timeoutTicks = 200, batch = "m00_admin_commands")
    public void adminCommandsRespectOriginalStateAndNativePermissions(GameTestHelper test) {
        thaumcraft.common.testing.AdministrationRuntimeChecks.run(test);
    }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE, timeoutTicks = 100, batch = "r13_cheat_sheet")
    public void cheatSheetUsesServerPermissionAndOriginalKnowledgeRules(GameTestHelper test) {
        thaumcraft.common.testing.CheatSheetRuntimeChecks.run(test);
    }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE, timeoutTicks = 200, batch = "m07_runic_shield")
    public void runicShieldAbsorbsNativeDamageAndPaysRegeneration(GameTestHelper test) {
        thaumcraft.common.testing.RunicShieldRuntimeChecks.run(test);
    }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE, timeoutTicks = 1800, batch = "m07_runic_augmentation")
    public void runicAugmentationUsesNativeInfusionAndOverflowSafeDemand(GameTestHelper test) {
        thaumcraft.common.testing.RunicAugmentationRuntimeChecks.run(test);
    }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE, timeoutTicks = 200, batch = "m11_firebat")
    public void fireBatPreservesNativeAttacksVariantsAndLoot(GameTestHelper test) {
        thaumcraft.common.testing.FireBatWispRuntimeChecks.fireBat(test);
    }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE, timeoutTicks = 200, batch = "m11_wisp")
    public void wispsKeepOriginalCombatMovementAndEssence(GameTestHelper test) {
        thaumcraft.common.testing.FireBatWispRuntimeChecks.wisp(test);
    }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE, timeoutTicks = 200, batch = "m11_monster_population")
    public void flyingMonstersUseOriginalNaturalPopulationRules(GameTestHelper test) {
        thaumcraft.common.testing.FireBatWispRuntimeChecks.population(test);
    }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE, timeoutTicks = 300, batch = "m02_unstable_nodes")
    public void unstableNodesEmitPaidAspectOrbsAndCanStabilise(GameTestHelper test) {
        thaumcraft.common.testing.UnstableNodeRuntimeChecks.run(test);
    }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE, timeoutTicks = 100, batch = "m02_primordial_pearl")
    public void primordialPearlPaysForOriginalNodeChangesAndExplosion(GameTestHelper test) {
        thaumcraft.common.testing.PrimordialPearlRuntimeChecks.run(test);
    }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE, timeoutTicks = 240, batch = "m08_combat_special")
    public void combatFocusSpecialisationsKeepOriginalCastingAndProjectiles(GameTestHelper test) {
        thaumcraft.common.testing.CombatFocusSpecialRuntimeChecks.run(test);
    }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE, timeoutTicks = 100, batch = "m08_shock_orb")
    public void shockOrbsDamageOriginalAreaAndPlaceTransientFields(GameTestHelper test) {
        thaumcraft.common.testing.ShockOrbRuntimeChecks.run(test);
    }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE, timeoutTicks = 120, batch = "g25_scan_container")
    public void scanKnowledgePreservesContainerBonusAboveBaseCap(GameTestHelper test) {
        thaumcraft.common.testing.ScanKnowledgeRuntimeChecks.containerBonusIsNotCappedAfterBase(test);
    }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE, timeoutTicks = 350, batch = "g25_scan_kinds")
    public void scanKnowledgePhysicalKindsAndRules(GameTestHelper test) {
        thaumcraft.common.testing.ScanKnowledgeRuntimeChecks.physicalKindsRecordExactKnowledge(test);
    }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE, timeoutTicks = 120, batch = "g25_scan_discovery")
    public void scanKnowledgeMissingParentsAndDiscovery(GameTestHelper test) {
        thaumcraft.common.testing.ScanKnowledgeRuntimeChecks.missingParentsBlockThenDiscoverOnce(test);
    }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE, timeoutTicks = 120, batch = "g25_scan_advanced")
    public void scanKnowledgeAdvancedMarkersSuppressThaumometer(GameTestHelper test) {
        thaumcraft.common.testing.ScanKnowledgeRuntimeChecks.advancedMarkersSuppressThaumometer(test);
    }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE, timeoutTicks = 100, batch = "m04_scan_api_locators")
    public void scanApiUsesNativeNbtAndNodeLocators(GameTestHelper test) {
        thaumcraft.common.testing.ScanApiRuntimeChecks.nativeApiUsesWorldNbtAndNodeLocators(test, test.makeMockServerPlayerInLevel());
    }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE, timeoutTicks = 100, batch = "m04_scan_api_identity")
    public void identicalPhysicalTargetsCancelOldScanSession(GameTestHelper test) {
        thaumcraft.common.testing.ScanApiRuntimeChecks.identicalPhysicalTargetsCancelOldSession(test, test.makeMockServerPlayerInLevel());
    }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE, timeoutTicks = 120, batch = "m04_scan_api_callbacks")
    public void scanCallbacksRespectOcclusionAndNativeTiming(GameTestHelper test) {
        thaumcraft.common.testing.ScanApiRuntimeChecks.callbacksRespectOcclusionAndTwentyTickSession(test, test.makeMockServerPlayerInLevel());
    }
    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE, timeoutTicks = 100, batch = "m04_object_aspect_direct")
    public void objectAspectApiDirectAndGroupedTagsReachProductionConsumers(GameTestHelper test) {
        thaumcraft.common.testing.ObjectAspectApiRuntimeChecks.nativeDirectAndGroupedTagsReachProductionConsumers(test);
    }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE, timeoutTicks = 100, batch = "m04_object_aspect_complex")
    public void objectAspectApiComplexRegistrationUsesOriginalServerPhase(GameTestHelper test) {
        thaumcraft.common.testing.ObjectAspectApiRuntimeChecks.complexRegistrationUsesOriginalBranchesAndServerPhase(test);
    }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE, timeoutTicks = 120, batch = "m04_object_aspect_callback")
    public void objectAspectApiCallbacksRetainOriginalExistingAmounts(GameTestHelper test) {
        thaumcraft.common.testing.ObjectAspectApiRuntimeChecks.callbackCanRegisterComplexTagsAndEarnOnlyOriginalExistingAmounts(test, test.makeMockServerPlayerInLevel());
    }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE, timeoutTicks = 300, batch = "m08_hellbat")
    public void hellbatFocusUsesOriginalTargetPaymentAndUpgradeGates(GameTestHelper test) {
        thaumcraft.common.testing.HellbatFocusRuntimeChecks.run(test);
    }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE, timeoutTicks = 200, batch = "m08_focus_pouch")
    public void focusPouchUsesNativeMenuAndAtomicWandSelection(GameTestHelper test) {
        thaumcraft.common.testing.FocusPouchRuntimeChecks.run(test);
    }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE, timeoutTicks = 200, batch = "m07_colored_candles")
    public void coloredCandlesRetainOriginalDyeCraftingAndSupportDrops(GameTestHelper test) {
        thaumcraft.common.testing.ColoredCandlesRuntimeChecks.run(test);
    }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE, timeoutTicks = 200, batch = "m02_hungry_nodes")
    public void hungryNodesRetainOriginalDamageConsumptionAndBlockDestruction(GameTestHelper test) {
        thaumcraft.common.testing.HungryNodeRuntimeChecks.run(test);
    }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE, timeoutTicks = 240, batch = "m09_accessories_menu")
    public void accessoriesUseNativeMenuPersistenceAndDeathCustody(GameTestHelper test) {
        thaumcraft.common.testing.AccessoriesRuntimeChecks.menuPersistenceAndDeath(test);
    }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE, timeoutTicks = 100)
    public void accessories_right_click_equip(GameTestHelper test) {
        thaumcraft.common.testing.AccessoriesRuntimeChecks.rightClickEquip(test);
    }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE, timeoutTicks = 200, batch = "m09_vis_amulet")
    public void visAmuletsTransferAndPayThroughActualNativeRelay(GameTestHelper test) {
        thaumcraft.common.testing.VisAmuletRuntimeChecks.nativeWornTransferAndRelayPayment(test);
    }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE, timeoutTicks = 180, batch = "m09_runic_accessories")
    public void runicAccessoriesUseOriginalEquipmentAndDepletionUpgrades(GameTestHelper test) {
        thaumcraft.common.testing.RunicAccessoriesRuntimeChecks.nativeEquipmentAndDepletionUpgrades(test);
    }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE, timeoutTicks = 100, batch = "m06_advanced_furnace")
    public void advancedFurnaceFormsAndProcessesOriginalNativeStructure(GameTestHelper test) {
        thaumcraft.common.testing.AdvancedFurnaceRuntimeChecks.run(test);
    }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE, timeoutTicks = 100, batch = "m06_advanced_callbacks")
    public void advancedFurnaceFencesPaymentAndNativeCallbacks(GameTestHelper test) {
        thaumcraft.common.testing.AdvancedFurnaceCallbackRuntimeChecks.run(test);
    }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE, timeoutTicks = 1200, batch = "m06_advanced_unload")
    public void advancedFurnacePreservesCustodyAcrossRealChunkUnload(GameTestHelper test) {
        thaumcraft.common.testing.AdvancedFurnaceUnloadRuntimeChecks.run(test);
    }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE, timeoutTicks = 200, batch = "r14_pech_entity")
    public void pechOriginalEntityUsesNativeGameplay(GameTestHelper test) {
        thaumcraft.common.testing.PechRuntimeChecks.entity(test);
    }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE, timeoutTicks = 200, batch = "r14_pech_trade")
    public void pechOriginalTradeUsesNativeGameplay(GameTestHelper test) {
        thaumcraft.common.testing.PechRuntimeChecks.trade(test);
    }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE, timeoutTicks = 200, batch = "r14_pech_curse")
    public void pechOriginalCurseUsesNativeGameplay(GameTestHelper test) {
        thaumcraft.common.testing.PechRuntimeChecks.curse(test);
    }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE, timeoutTicks = 200, batch = "r14_pech_beans")
    public void pechOriginalBeansUsesNativeGameplay(GameTestHelper test) {
        thaumcraft.common.testing.PechRuntimeChecks.beans(test);
    }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE, timeoutTicks = 200, batch = "m00_crystal_clusters")
    public void crystalClustersCraftPlaceOnAllOriginalFacesAndDropSixShards(GameTestHelper test) {
        thaumcraft.common.testing.CrystalClustersRuntimeChecks.sevenRecipesPlaceOnSixFacesAndRecoverTheirSixShards(test);
    }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE, timeoutTicks = 1400, batch = "m00_crystal_clusters_lifecycle")
    public void mixedCrystalOrientationSurvivesActualChunkUnload(GameTestHelper test) {
        thaumcraft.common.testing.CrystalClustersRuntimeChecks.mixedOrientationActuallyUnloadsAndReloadsFromChunkNbt(test);
    }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE, timeoutTicks = 100, batch = "m10_loot_bags")
    public void lootBagsPreserveOriginalPoolsAndPaidNativeDropCustody(GameTestHelper test) {
        thaumcraft.common.testing.LootBagRuntimeChecks.run(test);
    }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE, timeoutTicks = 100, batch = "m00_warp_notifications")
    public void warpNotificationsFollowOriginalCommittedDeltasAndNativePackets(GameTestHelper test) {
        thaumcraft.common.testing.WarpNotificationRuntimeChecks.run(test);
    }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE, timeoutTicks = 100, batch = "m09_runic_loot_potions")
    public void runicRecipesAcceptExactOriginalLootPotionIdentities(GameTestHelper test) {
        thaumcraft.common.testing.RunicPotionRuntimeChecks.actualLootPotionsPayRunicRecipeIdentities(test);
    }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE, timeoutTicks = 100, batch = "m04_pech_entity_scans")
    public void pechEntityScansReadActualThreeByteVariants(GameTestHelper test) {
        thaumcraft.common.testing.PechScanRuntimeChecks.run(test, test.makeMockServerPlayerInLevel());
    }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE, timeoutTicks = 140, batch = "m09_hover_girdle")
    public void hoverGirdleReducesOnlyPositiveFallDistanceThroughActualWornBelt(GameTestHelper test) {
        thaumcraft.common.testing.HoverGirdleRuntimeChecks.actualWornBeltReducesOnlyPositiveFallDistance(test);
    }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE, timeoutTicks = 200, batch = "r14_pech_goals")
    public void pechAvoidAndWatchGoalsRetainOriginalPlayerAndMutexRules(GameTestHelper test) {
        thaumcraft.common.testing.PechRuntimeChecks.goals(test);
    }


    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE, timeoutTicks = 200, batch = "m08_traveller_boots")
    public void travellerBootsPreserveOriginalNativeMovementAndJump(GameTestHelper test) {
        thaumcraft.common.testing.TravelGearRuntimeChecks.boots(test);
    }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE, timeoutTicks = 300, batch = "m08_hover_fuel")
    public void hoverHarnessConsumesNativeFuelAndRestoresFlightPermissions(GameTestHelper test) {
        thaumcraft.common.testing.TravelGearRuntimeChecks.harnessFuelAndPermissions(test);
    }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE, timeoutTicks = 200, batch = "m08_hover_mining")
    public void hoverHarnessMinesAtGroundSpeedWhileAirborne(GameTestHelper test) {
        thaumcraft.common.testing.TravelGearRuntimeChecks.harnessMining(test);
    }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE, timeoutTicks = 200, batch = "m08_hover_menu")
    public void hoverHarnessMenuPreservesActualJarCustody(GameTestHelper test) {
        thaumcraft.common.testing.TravelGearRuntimeChecks.harnessMenu(test);
    }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE, timeoutTicks = 200, batch = "m00_knowledge_notifications")
    public void knowledgeNotificationsUseOriginalCommittedFeedbackAndNativePackets(GameTestHelper test) {
        thaumcraft.common.testing.KnowledgeNotificationRuntimeChecks.run(test);
    }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE, timeoutTicks = 100, batch = "m04_knowledge_fragment")
    public void knowledgeFragmentsGrantOriginalPrimalResearchPoints(GameTestHelper test) {
        thaumcraft.common.testing.KnowledgeFragmentRuntimeChecks.run(test);
    }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE, timeoutTicks = 100, batch = "m10_primal_charm")
    public void primalCharmUsesActualInventoryTicksForFreeOrbsAndClues(GameTestHelper test) {
        thaumcraft.common.testing.PrimalCharmRuntimeChecks.run(test);
    }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE, timeoutTicks = 120, batch = "m02_taint_blocks")
    public void taintBlocksPreserveOriginalSpreadLootAndFallingCustody(GameTestHelper test) {
        thaumcraft.common.testing.TaintBlockRuntimeChecks.run(test);
    }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE, timeoutTicks = 120, batch = "g22_taint_distribution")
    public void taintSpreadMeasuresOracleDistributionAndTargets(GameTestHelper test) {
        thaumcraft.common.testing.TaintBlockRuntimeChecks.audit(test);
    }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE, timeoutTicks = 200, batch = "m08_taint_spore_growth")
    public void taintSporesGrowOnRealFibresAndBurstIntoActualSpiders(GameTestHelper test) {
        thaumcraft.common.testing.TaintSporeRuntimeChecks.growthAndBurst(test);
    }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE, timeoutTicks = 200, batch = "m08_taint_swarmer")
    public void taintSwarmersKeepOriginalBoundaryAndDeathLoot(GameTestHelper test) {
        thaumcraft.common.testing.TaintSporeRuntimeChecks.swarmerBoundaryAndLoot(test);
    }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE, timeoutTicks = 200, batch = "m08_taint_spider")
    public void taintSpidersKeepOriginalTargetsAndLoot(GameTestHelper test) {
        thaumcraft.common.testing.TaintSporeRuntimeChecks.spiderTargetsAndLoot(test);
    }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE, timeoutTicks = 180, batch = "m08_taint_swarm")
    public void taintSwarmsPreserveOriginalFlightCombatAndSummonedLifetime(GameTestHelper test) {
        thaumcraft.common.testing.TaintSwarmRuntimeChecks.run(test);
    }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE, timeoutTicks = 140, batch = "m08_taintacle_combat")
    public void taintaclesKeepOriginalCombatAndRootedAi(GameTestHelper test) {
        thaumcraft.common.testing.TaintacleRuntimeChecks.combatAndRootedAi(test);
    }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE, timeoutTicks = 140, batch = "m08_taintacle_lifetime")
    public void taintaclesKeepOriginalRetaliationLifetimeAndLoot(GameTestHelper test) {
        thaumcraft.common.testing.TaintacleRuntimeChecks.retaliationLifetimeAndLoot(test);
    }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE, timeoutTicks = 140, batch = "m08_taintacle_spawn")
    public void taintaclesRequireNoInventedFloorOrPotionMutation(GameTestHelper test) {
        thaumcraft.common.testing.TaintacleRuntimeChecks.noInventedFloorOrPotionMutation(test);
    }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE, timeoutTicks = 180, batch = "m08_primal_focus")
    public void primalFocusPaysActualVisAndUsesSeeker(GameTestHelper test) {
        thaumcraft.common.testing.PrimalFocusRuntimeChecks.actualFocusPaymentAndSeeker(test);
    }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE, timeoutTicks = 180, batch = "m08_primal_orb")
    public void primalOrbsCreateActualRareNodesAndTaint(GameTestHelper test) {
        thaumcraft.common.testing.PrimalFocusRuntimeChecks.actualRareImpactNodeAndTaint(test);
    }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE, timeoutTicks = 200, batch = "m02_pure_node_ecology")
    public void pureNodesCleanseOnlyTaintUnlessEmbeddedInSilverwood(GameTestHelper test) {
        thaumcraft.common.testing.NodeEcologyRuntimeChecks.pure(test);
    }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE, timeoutTicks = 240, batch = "m02_dark_node_ecology")
    public void darkNodesConvertBiomesAndRespectOriginalFourthGiantCap(GameTestHelper test) {
        thaumcraft.common.testing.NodeEcologyRuntimeChecks.dark(test);
    }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE, timeoutTicks = 200, batch = "m02_tainted_node_ecology")
    public void taintedNodesSpreadRealFibresAndInfectBeforeDarkEcology(GameTestHelper test) {
        thaumcraft.common.testing.NodeEcologyRuntimeChecks.tainted(test);
    }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE, timeoutTicks = 200, batch = "m02_silverwood_node_harvest")
    public void embeddedSilverwoodNodesHarvestOnlyThroughRealPlayerCustody(GameTestHelper test) {
        thaumcraft.common.testing.NodeEcologyRuntimeChecks.embeddedHarvest(test);
    }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE, timeoutTicks = 600, batch = "m02_node_biome_storage")
    public void embeddedSilverwoodBiomesAndAuraSurviveActualChunkStorage(GameTestHelper test) {
        thaumcraft.common.testing.NodeBiomeStorageRuntimeChecks.run(test);
    }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE, timeoutTicks = 100, batch = "g13_spawn_pools")
    public void spawnAuditNativeBiomePools(GameTestHelper test) {
        thaumcraft.common.testing.SpawnAuditRuntimeChecks.pools(test);
    }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE, timeoutTicks = 100, batch = "g13_spawn_guardian")
    public void spawnAuditGuardianIsolation(GameTestHelper test) {
        thaumcraft.common.testing.SpawnAuditRuntimeChecks.guardianIsolation(test);
    }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE, timeoutTicks = 100, batch = "g13_spawn_special")
    public void spawnAuditSpecialPaths(GameTestHelper test) {
        thaumcraft.common.testing.SpawnAuditRuntimeChecks.specialPaths(test);
    }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE, timeoutTicks = 100, batch = "g13_spawn_champion")
    public void spawnAuditChampionNaturalLifecycle(GameTestHelper test) {
        thaumcraft.common.testing.SpawnAuditRuntimeChecks.championNaturalLifecycle(test);
    }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE, timeoutTicks = 100, batch = "g13_spawn_light")
    public void spawnAuditOriginalCombinedDaylight(GameTestHelper test) {
        thaumcraft.common.testing.SpawnLightAuditRuntimeChecks.daylight(test);
    }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE, timeoutTicks = 100, batch = "g13_spawn_inhabited")
    public void spawnAuditInhabitedPlacement(GameTestHelper test) {
        thaumcraft.common.testing.SpawnLightAuditRuntimeChecks.inhabitedPlacement(test);
    }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE, timeoutTicks = 240, batch = "r14_biome_source")
    public void originalBiomesUseActualRegistrySourcesAndSpawnRows(GameTestHelper test) {
        thaumcraft.common.testing.BiomeRuntimeChecks.source(test);
    }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE, timeoutTicks = 600)
    public void generated_chunk_biomes_follow_the_world_seed(GameTestHelper test) {
        thaumcraft.common.testing.BiomeRuntimeChecks.generatedChunkBiomesFollowSeed(test);
    }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE, timeoutTicks = 400, batch = "r14_biome_trees")
    public void originalBiomeTreesAndDungeonsUseRealWorldgen(GameTestHelper test) {
        thaumcraft.common.testing.BiomeRuntimeChecks.treesAndDungeon(test);
    }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE, timeoutTicks = 1600, batch = "r14_biome_natural")
    public void magicalBiomesGenerateNaturallyAndSpawnActualPech(GameTestHelper test) {
        thaumcraft.common.testing.NaturalBiomeRuntimeChecks.run(test);
    }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE, timeoutTicks = 200, batch = "m08_taintacle_xp")
    public void taintacleExperienceFollowsOriginalDeathTimeAndPlayerCredit(GameTestHelper test) {
        thaumcraft.common.testing.TaintacleRuntimeChecks.originalExperienceWaitsForDeathTwenty(test);
    }


    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE, timeoutTicks = 200, batch = "m14_spa_backend_cycle")
    public void spaRetainsOriginalGridCycleRedstoneAndReload(GameTestHelper test) {
        thaumcraft.common.testing.SpaBackendRuntimeChecks.firstCycleGridOrderRedstoneAndReload(test);
    }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE, timeoutTicks = 200, batch = "m14_spa_backend_custody")
    public void spaPreservesOriginalContainersFluidPatchesAndInventory(GameTestHelper test) {
        thaumcraft.common.testing.SpaBackendRuntimeChecks.containerQuirksPatchIdentityAndNativePersistence(test);
    }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE, timeoutTicks = 200, batch = "m14_spa_storage")
    public void spaUsesNativeFabricTransactionsAndRetiredOwners(GameTestHelper test) {
        SpaFluidStorageRuntimeChecks.run(test);
    }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE, timeoutTicks = 200, batch = "g18_fluid_fill")
    public void fluidGolemFractionalFillConserves(GameTestHelper test) { FluidConservationRuntimeChecks.golemFractionalFill(test); }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE, timeoutTicks = 200, batch = "g18_fluid_drain")
    public void fluidGolemFractionalDrainConserves(GameTestHelper test) { FluidConservationRuntimeChecks.golemFractionalDrain(test); }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE, timeoutTicks = 200, batch = "g18_fluid_abort")
    public void fluidCrucibleAbortConserves(GameTestHelper test) { FluidConservationRuntimeChecks.crucibleAbort(test); }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE, timeoutTicks = 200, batch = "g18_fluid_drain_abort")
    public void fluidCrucibleDrainAbortConserves(GameTestHelper test) { FluidConservationRuntimeChecks.crucibleDrainAbort(test); }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE, timeoutTicks = 200, batch = "g18_fluid_bounds")
    public void fluidBoundaryAmountsConserve(GameTestHelper test) { FluidConservationRuntimeChecks.boundaryAmounts(test); }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE, timeoutTicks = 240, batch = "m14_spa_menu")
    public void spaMenuPreservesNativeOwnershipAndOriginalQuickMove(GameTestHelper test) {
        thaumcraft.common.testing.SpaMenuRuntimeChecks.run(test);
    }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE, timeoutTicks = 280, batch = "m14_spa_fluid")
    public void purifyingFluidPreservesOriginalClassicFlowWardAndSaltExpiry(GameTestHelper test) {
        thaumcraft.common.testing.PureFluidRuntimeChecks.run(test);
    }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE, timeoutTicks = 200, batch = "m14_crimson_rites")
    public void crimsonRitesCompleteResearchWithoutConsumingTheBook(GameTestHelper test) {
        thaumcraft.common.testing.EldritchItemsRuntimeChecks.reusableRitesCompleteCrimson(test);
    }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE, timeoutTicks = 200, batch = "m14_sanity_soap")
    public void sanitySoapUsesOriginalNativeReleaseAndWarpChances(GameTestHelper test) {
        thaumcraft.common.testing.EldritchItemsRuntimeChecks.soapUsesNativeReleaseAndOriginalChances(test);
    }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE, timeoutTicks = 200, batch = "m14_soap_recipe")
    public void sanitySoapCrucibleRecipePaysActualIngredients(GameTestHelper test) {
        thaumcraft.common.testing.EldritchItemsRuntimeChecks.soapCrucibleRecipePaysActualIngredients(test);
    }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE, timeoutTicks = 200, batch = "m11_pure_bucket")
    public void pureBucketPreservesOriginalSourcePickupPlacementAndCustody(GameTestHelper test) {
        thaumcraft.common.testing.PureBucketRuntimeChecks.run(test);
    }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE, timeoutTicks = 200, batch = "m11_pure_bucket_storage")
    public void pureBucketParticipatesInActualNativeFluidTransactions(GameTestHelper test) {
        PureBucketStorageRuntimeChecks.run(test);
    }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE, timeoutTicks = 200, batch = "m09_alchemy_leftovers")
    public void alchemyLeftoversBrewFromTheirCatalystsAtTheOriginalCost(GameTestHelper test) {
        thaumcraft.common.testing.AlchemyLeftoversRuntimeChecks.run(test, test.makeMockServerPlayerInLevel());
    }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE, timeoutTicks = 300, batch = "m09_item_grate")
    public void itemGrateFeedsDropsAndStopsLikeTheOriginalAndWoodStairsPlaceAndDrop(GameTestHelper test) {
        thaumcraft.common.testing.GrateRuntimeChecks.run(test, test.makeMockServerPlayerInLevel());
    }

    // ---- the new content wave: the eldritch bestiary, the cultists, the tainted animals,
    // ---- the champion system, the eldritch blocks, the Outer Lands, the projectiles,
    // ---- the device containers, the new items and the runic accessories.

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE, timeoutTicks = 300)
    public void eldritch_warden_caps_its_hits_and_keeps_its_ward(GameTestHelper test) {
        thaumcraft.common.testing.EldritchBestiaryRuntimeChecks.wardenDamageCapAndEnrage(test);
    }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE, timeoutTicks = 300)
    public void eldritch_golem_loses_its_head_and_hits_for_three_quarters(GameTestHelper test) {
        thaumcraft.common.testing.EldritchBestiaryRuntimeChecks.golemBeheadingAndMelee(test);
    }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE, timeoutTicks = 300)
    public void eldritch_crab_watcher_slime_and_zombie_keep_their_numbers(GameTestHelper test) {
        thaumcraft.common.testing.EldritchBestiaryRuntimeChecks.crabWatcherSlimeAndZombie(test);
    }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE, timeoutTicks = 300)
    public void cultist_faction_keeps_its_numbers_gear_and_loot(GameTestHelper test) {
        thaumcraft.common.testing.CultistFactionRuntimeChecks.factionNumbersGearAndLoot(test);
    }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE, timeoutTicks = 2400)
    public void cultist_praetor_keeps_its_boss_behaviour(GameTestHelper test) {
        thaumcraft.common.testing.CultistFactionRuntimeChecks.praetorBossBehaviour(test);
    }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE, timeoutTicks = 6000)
    public void cultist_portal_runs_its_stage_machine(GameTestHelper test) {
        thaumcraft.common.testing.CultistFactionRuntimeChecks.portalStageMachine(test);
    }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE, timeoutTicks = 300)
    public void tainted_animals_keep_their_numbers_and_drops(GameTestHelper test) {
        thaumcraft.common.testing.TaintedAnimalRuntimeChecks.animalNumbersAndDrops(test);
    }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE, timeoutTicks = 300)
    public void tainted_animals_convert_into_their_tainted_twin(GameTestHelper test) {
        thaumcraft.common.testing.TaintedAnimalRuntimeChecks.conversionIntoTheTaintedTwin(test);
    }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE, timeoutTicks = 2400)
    public void taint_crawler_and_seeds_keep_their_numbers(GameTestHelper test) {
        thaumcraft.common.testing.TaintedAnimalRuntimeChecks.crawlerAndSeedNumbers(test);
    }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE, timeoutTicks = 300)
    public void champion_promotion_uses_the_original_bonuses(GameTestHelper test) {
        thaumcraft.common.testing.ChampionRuntimeChecks.promotionUsesTheOriginalBonuses(test);
    }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE, timeoutTicks = 2400)
    public void champion_modifiers_answer_their_tick(GameTestHelper test) {
        thaumcraft.common.testing.ChampionRuntimeChecks.modifiersAnswerTickHitAndWound(test);
    }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE, timeoutTicks = 300)
    public void champion_modifiers_answer_their_hit_and_wound(GameTestHelper test) {
        thaumcraft.common.testing.ChampionRuntimeChecks.modifiersAnswerHitAndWound(test);
    }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE, timeoutTicks = 300)
    public void champion_effects_reach_player_victims_once(GameTestHelper test) {
        thaumcraft.common.testing.ChampionRuntimeChecks.effectsReachPlayerVictimsOnce(test);
    }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE, timeoutTicks = 300)
    public void champion_warp_reaches_player_victim_once(GameTestHelper test) {
        thaumcraft.common.testing.ChampionRuntimeChecks.warpedHitReachesPlayer(test);
    }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE, timeoutTicks = 300)
    public void infusion_jar_brain_awards_warp_once(GameTestHelper test) {
        thaumcraft.common.testing.InfusionWarpAuditChecks.jarBrainAwardsWarpOnce(test);
    }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE, timeoutTicks = 300)
    public void infusion_compass_stone_awards_warp_once(GameTestHelper test) {
        thaumcraft.common.testing.InfusionWarpAuditChecks.compassStoneAwardsWarpOnce(test);
    }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE, timeoutTicks = 300)
    public void champion_ward_uses_original_base_health(GameTestHelper test) {
        thaumcraft.common.testing.ChampionRuntimeChecks.wardUsesBaseHealth(test);
    }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE, timeoutTicks = 300)
    public void public_warp_api_preserves_original_guards(GameTestHelper test) {
        thaumcraft.common.testing.InfusionWarpAuditChecks.publicWarpApiPreservesOriginalGuards(test);
    }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE, timeoutTicks = 300)
    public void champion_whitelist_covers_the_cave_spider(GameTestHelper test) {
        thaumcraft.common.testing.ChampionRuntimeChecks.caveSpiderIsAChampionCandidate(test);
    }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE, timeoutTicks = 300)
    public void eldritch_altar_takes_eyes_and_switches_its_spawn_type(GameTestHelper test) {
        thaumcraft.common.testing.EldritchBlockRuntimeChecks.altarTakesEyesAndSwitchesItsSpawnType(test);
    }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE, timeoutTicks = 200)
    public void eldritch_glyphed_stone_gives_its_experience(GameTestHelper test) {
        thaumcraft.common.testing.EldritchBlockRuntimeChecks.glyphedStoneGivesItsExperience(test);
    }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE, timeoutTicks = 200)
    public void eldritch_ethereal_bloom_cleanses_back_to_the_generated_biome(GameTestHelper test) {
        thaumcraft.common.testing.EldritchBlockRuntimeChecks.etherealBloomCleansesBackToTheGeneratedBiome(test);
    }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE, timeoutTicks = 2400)
    public void eldritch_obelisk_slows_only_the_eldritch_mobs(GameTestHelper test) {
        thaumcraft.common.testing.EldritchBlockRuntimeChecks.obeliskSlowsOnlyTheEldritchMobs(test);
    }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE, timeoutTicks = 2400)
    public void magic_box_chain_owns_twenty_seven_slots_per_box(GameTestHelper test) {
        thaumcraft.common.testing.EldritchBlockRuntimeChecks.magicBoxChainOwnsTwentySevenSlotsPerBox(test);
    }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE, timeoutTicks = 2400)
    public void warding_stone_pushes_and_seals_its_ward(GameTestHelper test) {
        thaumcraft.common.testing.EldritchBlockRuntimeChecks.wardingStonePushesAndBannerKeepsItsAspect(test);
    }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE, timeoutTicks = 300)
    public void banner_keeps_its_colour_and_aspect(GameTestHelper test) {
        thaumcraft.common.testing.EldritchBlockRuntimeChecks.bannerKeepsItsColourAndAspect(test);
    }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE, timeoutTicks = 2400)
    public void flux_scrubber_spends_its_charges(GameTestHelper test) {
        thaumcraft.common.testing.EldritchBlockRuntimeChecks.fluxScrubberSpendsItsCharges(test);
    }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE, timeoutTicks = 300)
    public void outer_lands_dimension_and_biome_use_the_original_values(GameTestHelper test) {
        thaumcraft.common.testing.OuterLandsRuntimeChecks.dimensionAndBiomeUseTheOriginalValues(test);
    }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE, timeoutTicks = 300)
    public void outer_lands_maze_uses_the_original_cell_layout(GameTestHelper test) {
        thaumcraft.common.testing.OuterLandsRuntimeChecks.mazeUsesTheOriginalCellLayout(test);
    }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE, timeoutTicks = 300)
    public void village_components_use_the_original_layout(GameTestHelper test) {
        thaumcraft.common.testing.OuterLandsRuntimeChecks.villageComponentsUseTheOriginalLayout(test);
    }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE, timeoutTicks = 2400)
    public void bottle_of_taint_poisons_and_grows_fibres(GameTestHelper test) {
        thaumcraft.common.testing.ProjectileRuntimeChecks.bottleOfTaintPoisonsAndGrowsFibres(test);
    }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE, timeoutTicks = 2400)
    public void golem_orb_homes_and_deals_the_original_damage(GameTestHelper test) {
        thaumcraft.common.testing.ProjectileRuntimeChecks.golemOrbHomesAndDealsTheOriginalDamage(test);
    }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE, timeoutTicks = 2400)
    public void primal_arrow_keeps_its_damage_and_effects(GameTestHelper test) {
        thaumcraft.common.testing.ProjectileRuntimeChecks.primalArrowKeepsItsDamageAndEffects(test);
    }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE, timeoutTicks = 300)
    public void magic_box_and_hand_mirror_bind_the_original_slots(GameTestHelper test) {
        thaumcraft.common.testing.DeviceContainerRuntimeChecks.magicBoxAndHandMirrorBindTheOriginalSlots(test);
    }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE, timeoutTicks = 300)
    public void ghost_slots_keep_the_original_limits(GameTestHelper test) {
        thaumcraft.common.testing.DeviceContainerRuntimeChecks.ghostSlotsKeepTheOriginalLimits(test);
    }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE, timeoutTicks = 300)
    public void bone_bow_keeps_its_original_draw_and_damage(GameTestHelper test) {
        thaumcraft.common.testing.EquipmentItemRuntimeChecks.boneBowKeepsItsOriginalDrawAndDamage(test);
    }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE, timeoutTicks = 300)
    public void crimson_sword_and_primal_crusher_keep_their_numbers(GameTestHelper test) {
        thaumcraft.common.testing.EquipmentItemRuntimeChecks.crimsonSwordAndPrimalCrusherKeepTheirNumbers(test);
    }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE, timeoutTicks = 300)
    public void relics_and_reliquaries_keep_their_original_rules(GameTestHelper test) {
        thaumcraft.common.testing.EquipmentItemRuntimeChecks.relicsAndReliquariesKeepTheirOriginalRules(test);
    }

    // The settings adopted from TC4Tweaks, each with the tweak and with the original rule.
    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE, batch = "tc4tweaks_loot")
    public void tweakLootRerollsBooksAndAmulets(GameTestHelper test) {
        thaumcraft.common.testing.TweakRuntimeChecks.loot(test);
    }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE, batch = "tc4tweaks_shock")
    public void tweakEarthShockSparesItemsAndExperience(GameTestHelper test) {
        thaumcraft.common.testing.TweakRuntimeChecks.earthShock(test);
    }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE)
    public void tweakInfusionKeepsCentreItemData(GameTestHelper test) {
        thaumcraft.common.testing.TweakRuntimeChecks.infusionData(test);
    }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE, batch = "tc4tweaks_phial")
    public void tweakFilledPhialStaysInHand(GameTestHelper test) {
        thaumcraft.common.testing.TweakRuntimeChecks.filledToHand(test);
    }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE, timeoutTicks = 300)
    public void runic_accessories_keep_their_original_charge_and_slot(GameTestHelper test) {
        thaumcraft.common.testing.RunicBaubleRuntimeChecks.runicAccessoriesKeepTheirOriginalChargeAndSlot(test);
    }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE, timeoutTicks = 300)
    public void cultist_armour_keeps_its_original_numbers(GameTestHelper test) {
        thaumcraft.common.testing.RunicBaubleRuntimeChecks.cultistArmourKeepsItsOriginalNumbers(test);
    }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE, timeoutTicks = 400, batch = "m13_outer_lands")
    public void eldritchGateMovesThePlayerIntoTheOuterLands(GameTestHelper test) {
        thaumcraft.common.testing.OuterLandsRuntimeChecks.portal(test);
    }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE, timeoutTicks = 200, batch = "m13_outer_lands")
    public void outerLandsGeneratesTheLabyrinthOfTheEldritchRing(GameTestHelper test) {
        thaumcraft.common.testing.OuterLandsRuntimeChecks.labyrinth(test);
    }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE, timeoutTicks = 200, batch = "worldgen_crash_biome")
    public void worldgenCrashMissingBiomeRefusesFeature(GameTestHelper test) {
        thaumcraft.common.testing.WorldgenCrashRuntimeChecks.missingBiomeFeature(test);
    }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE, timeoutTicks = 200, batch = "worldgen_crash_column")
    public void worldgenCrashMissingBiomeRefusesColumn(GameTestHelper test) {
        thaumcraft.common.testing.WorldgenCrashRuntimeChecks.missingBiomeColumn(test);
    }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE, timeoutTicks = 200, batch = "worldgen_crash_rooms")
    public void worldgenCrashMalformedMazeSkipsRooms(GameTestHelper test) {
        thaumcraft.common.testing.WorldgenCrashRuntimeChecks.malformedMazeRooms(test);
    }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE, timeoutTicks = 200, batch = "worldgen_crash_ring")
    public void worldgenCrashMalformedMazeSkipsRing(GameTestHelper test) {
        thaumcraft.common.testing.WorldgenCrashRuntimeChecks.malformedMazeRing(test);
    }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE, timeoutTicks = 400, batch = "g30_maze")
    public void mazeAuditGeneratesRealRegionAndReloadsMap(GameTestHelper test) {
        thaumcraft.common.testing.MazeAuditRuntimeChecks.generation(test);
    }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE, timeoutTicks = 200, batch = "g30_return")
    public void mazeAuditReturnsThroughNearestHomeGate(GameTestHelper test) {
        thaumcraft.common.testing.MazeAuditRuntimeChecks.nearestReturnGate(test);
    }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE, timeoutTicks = 400, batch = "g30_unexplored")
    public void mazeAuditFindsUnexploredDestinationGate(GameTestHelper test) {
        thaumcraft.common.testing.MazeAuditRuntimeChecks.unexploredGate(test);
    }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE, timeoutTicks = 400, batch = "g30_bounds")
    public void mazeAuditReturnSearchUsesExactOracleBounds(GameTestHelper test) {
        thaumcraft.common.testing.MazeAuditRuntimeChecks.returnGateBounds(test);
    }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE, timeoutTicks = 200, batch = "m13_outer_lands")
    public void outerLandsIsARealRegisteredDimension(GameTestHelper test) {
        thaumcraft.common.testing.OuterLandsRuntimeChecks.levelStemKeepsTheOriginalValues(test);
    }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE, timeoutTicks = 200, batch = "m13_outer_lands")
    public void outerLandsGenerationWritesNoBlockEntityWarnings(GameTestHelper test) {
        thaumcraft.common.testing.OuterLandsRuntimeChecks.generationLogsNoBlockEntityWarnings(test);
    }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE, timeoutTicks = 100, batch = "m13_block_entity_markers")
    public void stateDependentBlockEntitiesKeepTheirEntityAndOnlyTheirOwnWarningIsDropped(GameTestHelper test) {
        thaumcraft.common.testing.BlockEntityMarkerRuntimeChecks.run(test);
    }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE, timeoutTicks = 300, batch = "r14_decoration_write_area")
    public void forestDecorationStaysInsideTheWriteAreaOfTheFeaturesStep(GameTestHelper test) {
        thaumcraft.common.testing.DecorationWriteAreaRuntimeChecks.run(test);
    }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE, timeoutTicks = 300, batch = "survival_progression")
    public void survival_chain_recipes_yield_their_items(GameTestHelper test) {
        thaumcraft.common.testing.SurvivalProgressionRuntimeChecks.chainRecipesYieldTheirItems(test);
    }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE, timeoutTicks = 300, batch = "survival_progression")
    public void survival_chain_ingredients_close_on_natural_acquisition(GameTestHelper test) {
        thaumcraft.common.testing.SurvivalProgressionRuntimeChecks.chainIngredientsCloseOnNaturalAcquisition(test);
    }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE, timeoutTicks = 300, batch = "survival_progression")
    public void survival_chain_research_is_purchasable_with_the_earned_pool(GameTestHelper test) {
        thaumcraft.common.testing.SurvivalProgressionRuntimeChecks.chainResearchIsPurchasableWithTheEarnedPool(test);
    }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE, timeoutTicks = 300, batch = "survival_progression")
    public void survival_chain_needs_no_creative_mode_or_fixture(GameTestHelper test) {
        thaumcraft.common.testing.SurvivalProgressionRuntimeChecks.chainNeedsNoCreativeModeOrFixture(test);
    }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE, timeoutTicks = 400, batch = "survival_progression")
    public void survival_outer_lands_is_reachable_from_the_overworld(GameTestHelper test) {
        thaumcraft.common.testing.SurvivalProgressionRuntimeChecks.outerLandsIsReachableFromTheOverworld(test);
    }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE, timeoutTicks = 300, batch = "survival_progression")
    public void survival_boss_loot_is_the_primordial_pearl(GameTestHelper test) {
        thaumcraft.common.testing.SurvivalProgressionRuntimeChecks.bossLootIsThePrimordialPearl(test);
    }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE, timeoutTicks = 300, batch = "survival_progression")
    public void survival_arcane_furnace_builds_and_pays_the_meat_nuggets(GameTestHelper test) {
        thaumcraft.common.testing.SurvivalProgressionRuntimeChecks.arcaneFurnaceIsBuiltByAWandAndPaysTheMeatNuggets(test);
    }

    // Research an add-on registers through the public API joins the book, the research table, purchase, clues,
    // completion and saves; a change to an original entry through its public view is laid over the corpus.
    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE, timeoutTicks = 100, batch = "api_addon_research")
    public void addonResearchJoinsGameplay(GameTestHelper test) {
        thaumcraft.common.testing.AddonResearchRuntimeChecks.addonResearchJoinsGameplay(test, test.makeMockServerPlayerInLevel());
    }

    // The IThaumcraftAddon hook ran before the world, and 1.7.10 ore dictionary names reach the tags of the world.
    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE, batch = "api_addon_lifecycle")
    public void addonHookRanBeforeTheWorld(GameTestHelper test) {
        thaumcraft.common.testing.AddonApiRuntimeChecks.addonHookRanBeforeTheWorld(test);
    }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE, batch = "api_ore_dictionary")
    public void oreDictionaryNamesReachTheTagsOfTheWorld(GameTestHelper test) {
        thaumcraft.common.testing.AddonApiRuntimeChecks.oreDictionaryNamesReachTheTagsOfTheWorld(test);
    }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE, batch = "api_effects")
    public void effectsOfTheOriginalPacketsAreSent(GameTestHelper test) {
        thaumcraft.common.testing.AddonApiRuntimeChecks.effectsOfTheOriginalPacketsAreSent(test);
    }

    // Wand parts and a focus registered by an add-on through the public API (test-only add-on of this harness).
    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE, timeoutTicks = 200, batch = "wand_api_assembly")
    public void wandApiAddonPartsAssembleInTheArcaneWorkbench(GameTestHelper test) {
        thaumcraft.common.testing.WandApiRuntimeChecks.addonPartsAssemble(test);
    }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE, timeoutTicks = 200, batch = "wand_api_on_update")
    public void wandApiAddonRodOnUpdateRefillsTheCarriedWand(GameTestHelper test) {
        thaumcraft.common.testing.WandApiRuntimeChecks.addonRodOnUpdateRuns(test);
    }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE, timeoutTicks = 200, batch = "wand_api_helper")
    public void wandApiHelperPaysChargesAndMountsAddonFoci(GameTestHelper test) {
        thaumcraft.common.testing.WandApiRuntimeChecks.wandHelperPaysAndMounts(test);
    }
}
