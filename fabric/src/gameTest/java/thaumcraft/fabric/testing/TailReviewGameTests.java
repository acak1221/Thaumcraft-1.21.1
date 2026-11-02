package thaumcraft.fabric.testing;

import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import thaumcraft.common.testing.*;

public final class TailReviewGameTests implements FabricGameTest {
    @GameTest(template = EMPTY_STRUCTURE, batch = "tail_village_profession_ai")
    public void tailVillageProfessionAi(GameTestHelper test) { TailVillageProfessionReviewRuntimeChecks.generatedBankerKeepsProfessionWithNormalBrain(test); }
    @GameTest(template = EMPTY_STRUCTURE, batch = "tail_village_legacy_restock")
    public void tailVillageLegacyRestock(GameTestHelper test) { TailVillageProfessionReviewRuntimeChecks.bankerLastOfferRestocksAfterFortyIdleTicks(test); }
    @GameTest(template = EMPTY_STRUCTURE, batch = "tail_arcane_craft_hook_errors")
    public void tailArcaneCraftHookErrors(GameTestHelper test) { TailArcaneCallbackReviewRuntimeChecks.paidCraftHookFailuresPreserveLiveOutputAndRethrow(test); }
    @GameTest(template = EMPTY_STRUCTURE, batch = "tail_crucible_live_craft_hooks")
    public void tailCrucibleLiveCraftHooks(GameTestHelper test) { TailCrucibleCraftHookReviewRuntimeChecks.craftHooksKeepLiveOutputUntilCallbacksFinish(test); }
    @GameTest(template = EMPTY_STRUCTURE, batch = "tail_arcane_door_planes")
    public void tailArcaneDoorPlanes(GameTestHelper test) { TailDoorShapeReviewRuntimeChecks.bothDoorHalvesKeepCanonicalOpenAndClosedPlanes(test); }
    @GameTest(template = EMPTY_STRUCTURE, batch = "tail_watcher_gaze_cadence")
    public void tailWatcherGazeCadence(GameTestHelper test) { TailMobCadenceReviewRuntimeChecks.watcherGazeUsesServerTicks(test); }
    @GameTest(template = EMPTY_STRUCTURE, batch = "taildecorationrandomorder")
    public void tailDecorationRandomOrder(GameTestHelper test) { TailWorldParityReviewRuntimeChecks.commonDecorationsKeepConditionalRandomDraws(test); }
    @GameTest(template = EMPTY_STRUCTURE, batch = "tailtraderandomorder")
    public void tailTradeRandomOrder(GameTestHelper test) { TailWorldParityReviewRuntimeChecks.wizardTradesRollAmountsBeforeRewardVariants(test); }
    @GameTest(template = EMPTY_STRUCTURE, batch = "tailtempletotemflag")
    public void tailTempleTotemFlag(GameTestHelper test) { TailWorldParityReviewRuntimeChecks.scatteredTempleDoesNotSuppressTotemAfterWildPass(test); }

    @GameTest(template = EMPTY_STRUCTURE, batch = "tail_boss_barrier_opening", timeoutTicks = 900)
    public void tailBossBarrierOpening(GameTestHelper test) { TailWorldgenReviewRuntimeChecks.bossOpeningRemovesCorrectNativeBarrier(test); }
    @GameTest(template = EMPTY_STRUCTURE, batch = "tailcultistmeleecadence")
    public void tailCultistMeleeCadence(GameTestHelper test) { TailMobCadenceReviewRuntimeChecks.cultistMeleeCooldownUsesServerTicks(test); }
    @GameTest(template = EMPTY_STRUCTURE, batch = "tailclericrangedcadence")
    public void tailClericRangedCadence(GameTestHelper test) { TailMobCadenceReviewRuntimeChecks.clericRangedCooldownUsesServerTicks(test); }
    @GameTest(template = EMPTY_STRUCTURE, batch = "tailgianteldritchbiome")
    public void tailGiantEldritchBiome(GameTestHelper test) { TailMobCadenceReviewRuntimeChecks.giantRemoteHitSummonsInRegisteredEldritchBiome(test); }

    @GameTest(template = EMPTY_STRUCTURE, batch = "tail_replant_custody", timeoutTicks = 900)
    public void tailReplantSeedCustody(GameTestHelper test) { TailReplantReviewRuntimeChecks.replantPaysSeedBeforeNativePlacementObserver(test); }
    @GameTest(template = EMPTY_STRUCTURE, batch = "tailvillagetradingmenu")
    public void tailVillageTradingMenu(GameTestHelper test) { TailGolemVillageReviewRuntimeChecks.freshVillageOffersCanActuallyTrade(test); }
    @GameTest(template = EMPTY_STRUCTURE, batch = "tailgatherentropynameditem")
    public void tailGatherEntropyNamedItem(GameTestHelper test) { TailGolemVillageReviewRuntimeChecks.gatherEntropyPicksNamedDiamond(test); }
    @GameTest(template = EMPTY_STRUCTURE, batch = "tailliquidremotechunk")
    public void tailLiquidRemoteChunk(GameTestHelper test) { TailGolemVillageReviewRuntimeChecks.liquidSearchDoesNotLoadRemoteMarker(test); }
    @GameTest(template = EMPTY_STRUCTURE, batch = "tailuseoppositechunk")
    public void tailUseOppositeChunk(GameTestHelper test) { TailGolemVillageReviewRuntimeChecks.useEmptySpaceDoesNotLoadOppositeChunk(test); }

    @GameTest(template = EMPTY_STRUCTURE, batch = "tailtaintpassageplannedwalls")
    public void tailTaintPassagePlannedWalls(GameTestHelper test) { TailWorldgenReviewRuntimeChecks.taintPassageSeesItsNewWalls(test); }
    @GameTest(template = EMPTY_STRUCTURE, batch = "tailtaintpassagebiome")
    public void tailTaintPassageBiome(GameTestHelper test) { TailWorldgenReviewRuntimeChecks.taintPassageConvertsBiomeWithoutFibres(test); }
    @GameTest(template = EMPTY_STRUCTURE, batch = "tailnestroominterior")
    public void tailNestRoomInterior(GameTestHelper test) { TailWorldgenReviewRuntimeChecks.nestRoomPreservesItsOpenInterior(test); }
    @GameTest(template = EMPTY_STRUCTURE, batch = "tailwizardtowerskullloot")
    public void tailWizardTowerSkullLoot(GameTestHelper test) { TailWorldgenReviewRuntimeChecks.wizardTowerWeightedSkullRollCreatesLoot(test); }
    @GameTest(template = EMPTY_STRUCTURE, batch = "tailroomfibretype")
    public void tailRoomFibreType(GameTestHelper test) { TailWorldgenReviewRuntimeChecks.roomFibreAdapterRetainsMushroomType(test); }
    @GameTest(template = EMPTY_STRUCTURE, batch = "tailbossdoorfamily")
    public void tailBossDoorFamily(GameTestHelper test) { TailWorldgenReviewRuntimeChecks.generatedBossDoorUsesUnbreakableFamily(test); }
    @GameTest(template = EMPTY_STRUCTURE, batch = "tailbossbarriercollision")
    public void tailBossBarrierCollision(GameTestHelper test) { TailWorldgenReviewRuntimeChecks.unbreakableAiryBarrierHasNativeCollision(test); }
    @GameTest(template = EMPTY_STRUCTURE, batch = "tailwardingfencecollision")
    public void tailWardingFenceCollision(GameTestHelper test) { TailWorldgenReviewRuntimeChecks.wardingFenceCollisionUsesEntityAndPower(test); }

    @GameTest(template = EMPTY_STRUCTURE, batch = "tail_watcher_controls")
    public void tailWatcherNativeControls(GameTestHelper test) { TailEntityWorldReviewRuntimeChecks.watcherNativeControlsProcessQueuedCommands(test); }
    @GameTest(template = EMPTY_STRUCTURE, batch = "tail_watcher_fire")
    public void tailWatcherFireImmunity(GameTestHelper test) { TailEntityWorldReviewRuntimeChecks.watcherRetainsOriginalFireImmunity(test); }
    @GameTest(template = EMPTY_STRUCTURE, batch = "tail_slime_spit")
    public void tailSlimeSpitRange(GameTestHelper test) { TailEntityWorldReviewRuntimeChecks.slimeSpitsAtPlayerEightBlocksAway(test); }
    @GameTest(template = EMPTY_STRUCTURE, batch = "tail_slime_loot")
    public void tailSmallSlimeDeathLoot(GameTestHelper test) { TailEntityWorldReviewRuntimeChecks.smallSlimeDeathDropsTaintedGoo(test); }
    @GameTest(template = EMPTY_STRUCTURE, batch = "tail_axe_persistence")
    public void tailStreamAxeGravityReload(GameTestHelper test) { TailEntityWorldReviewRuntimeChecks.axeChoppedDropReloadsWithOrdinaryGravity(test); }
    @GameTest(template = EMPTY_STRUCTURE, batch = "tail_utility_chunks")
    public void tailUtilityChunkCoordinates(GameTestHelper test) { TailUtilityReviewRuntimeChecks.chunkLookupUsesBlockCoordinatesWithoutLoading(test); }
    @GameTest(template = EMPTY_STRUCTURE, batch = "tail_utility_rays")
    public void tailUtilityNativeRayMiss(GameTestHelper test) { TailUtilityReviewRuntimeChecks.lineOfSightAndPointingDistinguishNativeMiss(test); }
    @GameTest(template = EMPTY_STRUCTURE, batch = "tail_utility_wear")
    public void tailUtilityUnbreakingWear(GameTestHelper test) { TailUtilityReviewRuntimeChecks.unbreakingOnlyDecidesWhetherOneWearPointIsAvoided(test); }
    @GameTest(template = EMPTY_STRUCTURE, batch = "tail_api_enchantment")
    public void tailEnchantmentApiEmptyTag(GameTestHelper test) { TailApiReviewRuntimeChecks.emptyEnchantmentTagKeepsPublicNativeView(test); }
    @GameTest(template = EMPTY_STRUCTURE, batch = "tail_api_damage")
    public void tailIndirectDamageAttribution(GameTestHelper test) { TailApiReviewRuntimeChecks.indirectDamageFactoriesPreserveDirectAndCausingEntities(test); }
    @GameTest(template = EMPTY_STRUCTURE, batch = "tail_stair_adapter")
    public void tailOuterLandsStairDirections(GameTestHelper test) { TailApiReviewRuntimeChecks.outerLandsStairAdapterRetainsAllMetadataDirections(test); }
    @GameTest(template = EMPTY_STRUCTURE, batch = "tail_block_shapes")
    public void tailRegisteredBlockShapeBounds(GameTestHelper test) { TailBlockShapeRuntimeChecks.furnaceCoreNozzlesAndPedestalKeepCanonicalBounds(test); }
    @GameTest(template = EMPTY_STRUCTURE, batch = "tail_eldritch_contact")
    public void tailEldritchNothingImmunity(GameTestHelper test) { TailEldritchContactRuntimeChecks.nothingDamagesSurvivalAndSparesCreative(test); }
    @GameTest(template = EMPTY_STRUCTURE, batch = "tail_crystal_persistence")
    public void tailEldritchCrystalOrientation(GameTestHelper test) { TailEldritchContactRuntimeChecks.crystalOrientationSurvivesSaveAndUpdate(test); }
    @GameTest(template = EMPTY_STRUCTURE, batch = "tail_warding_ground")
    public void tailWardingStoneGroundedMob(GameTestHelper test) { TailEldritchContactRuntimeChecks.wardingPushesOnlyAirborneMobs(test); }
    @GameTest(template = EMPTY_STRUCTURE, batch = "tail_pedestal_custody")
    public void tailPedestalRetrievalCustody(GameTestHelper test) { TailBlockOutputRuntimeChecks.pedestalRetainsCanceledRetrieval(test); }
    @GameTest(template = EMPTY_STRUCTURE, batch = "tail_mirror_link_cancel")
    public void tailMirrorBindingCancellation(GameTestHelper test) { TailBlockOutputRuntimeChecks.mirrorBindingRetainsCanceledOutput(test); }
    @GameTest(template = EMPTY_STRUCTURE, batch = "tail_mirror_link_reentry")
    public void tailMirrorBindingReentry(GameTestHelper test) { TailBlockOutputRuntimeChecks.mirrorBindingCannotReenterUnpaidSource(test); }
    @GameTest(template = EMPTY_STRUCTURE, batch = "tail_variant_collision")
    public void tailVariantPlacementCollision(GameTestHelper test) { TailVariantPlacementRuntimeChecks.variantAndFocusRespectNativeEntityCollision(test); }
    @GameTest(template = EMPTY_STRUCTURE, batch = "tail_crucible_callback")
    public void tailCrucibleCraftCallbackReplacement(GameTestHelper test) { TailCrucibleCraftRuntimeChecks.craftCallbackReplacementDoesNotLoseCatalystOutput(test); }
}
