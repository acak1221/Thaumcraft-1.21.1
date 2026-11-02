package thaumcraft.fabric.testing;

import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import thaumcraft.common.testing.*;

public final class ComprehensiveReviewGameTests implements FabricGameTest {
    @GameTest(template = EMPTY_STRUCTURE, batch = "review_trunk_reentry")
    public void reviewTrunkReentry(GameTestHelper test) { TrunkSpawnerReviewRuntimeChecks.reentrantPlacementKeepsOnePayloadOwner(test); }
    @GameTest(template = EMPTY_STRUCTURE, batch = "review_trunk_controls")
    public void reviewTrunkCancellationAndCreative(GameTestHelper test) { TrunkSpawnerReviewRuntimeChecks.cancellationAndCreativePreserveOriginalPlacementRules(test); }
    @GameTest(template = EMPTY_STRUCTURE, batch = "review_follow_arrival")
    public void reviewFollowingItemArrival(GameTestHelper test) { FollowingItemReviewRuntimeChecks.arrivalReleasesSyncedTarget(test); }
    @GameTest(template = EMPTY_STRUCTURE, batch = "review_follow_save")
    public void reviewFollowingItemSave(GameTestHelper test) { FollowingItemReviewRuntimeChecks.followingSparkleTypeSurvivesNativeSave(test); }
    @GameTest(template = EMPTY_STRUCTURE, batch = "review_arcane_close")
    public void reviewArcaneCraftCallbackClose(GameTestHelper test) { ComprehensiveArcaneReviewRuntimeChecks.craftCallbackCloseRetainsPaidOutput(test); }
    @GameTest(template = EMPTY_STRUCTURE, batch = "review_arcane_price")
    public void reviewArcaneOverflowRecipe(GameTestHelper test) { ComprehensiveArcaneReviewRuntimeChecks.unrepresentableApiRecipeIsRefusedWithoutBlockingValidRecipe(test); }
    @GameTest(template = EMPTY_STRUCTURE, batch = "review_mirror_reentry")
    public void reviewMirrorEjectionReentry(GameTestHelper test) { ComprehensiveMirrorReviewRuntimeChecks.ejectionOwnsOnlyOneItemAcrossRecursiveAdmission(test); }
    @GameTest(template = EMPTY_STRUCTURE, batch = "review_mirror_cancel")
    public void reviewMirrorEjectionCancellation(GameTestHelper test) { ComprehensiveMirrorReviewRuntimeChecks.canceledEjectionRetainsPaidCustodyAndOriginalPhysics(test); }
    @GameTest(template = EMPTY_STRUCTURE, batch = "review_special_immunity")
    public void reviewSpecialItemImmunity(GameTestHelper test) { FollowingItemReviewRuntimeChecks.specialSubtypesKeepOriginalExplosionImmunity(test); }
    @GameTest(template = EMPTY_STRUCTURE, batch = "review_crucible_extract")
    public void reviewCrucibleTaggedExtraction(GameTestHelper test) { LoaderFluidReviewRuntimeChecks.crucibleTaggedExtraction(test); }
    @GameTest(template = EMPTY_STRUCTURE, batch = "review_crucible_insert")
    public void reviewCrucibleTaggedInsertion(GameTestHelper test) { LoaderFluidReviewRuntimeChecks.crucibleTaggedInsertion(test); }
    @GameTest(template = EMPTY_STRUCTURE, batch = "review_infusion_empty_tag")
    public void reviewNativeInfusionEmptyTag(GameTestHelper test) { NativeInfusionViewReviewRuntimeChecks.emptyNativeTagDoesNotAbortPublicRecipeEnumeration(test); }
    @GameTest(template = EMPTY_STRUCTURE, batch = "review_taint_swell")
    public void reviewTaintCreeperSwell(GameTestHelper test) { TaintSwellReviewRuntimeChecks.swellUsesSquaredDistanceAndReleasesItsMoveGoal(test); }
    @GameTest(template = EMPTY_STRUCTURE, batch = "review_hoe_border")
    public void reviewElementalHoeBoundary(GameTestHelper test) { ElementalHoeBoundaryReviewRuntimeChecks.nativeWorldBorderStopsNeighbourTilling(test); }
    @GameTest(template = EMPTY_STRUCTURE, batch = "review_crab_cooldown")
    public void reviewEldritchCrabCooldown(GameTestHelper test) { EldritchCrabReviewRuntimeChecks.mountedCrabAttacksAgainAfterItsCooldown(test); }
    @GameTest(template = EMPTY_STRUCTURE, batch = "review_handmirror_dimension")
    public void reviewMalformedHandMirrorDimension(GameTestHelper test) { HandMirrorLinkReviewRuntimeChecks.malformedStoredDimensionDoesNotCrashMirrorLookup(test); }
    @GameTest(template = EMPTY_STRUCTURE, batch = "review_grate_hopper")
    public void reviewGrateHopperCustody(GameTestHelper test) { ComprehensiveGrateReviewRuntimeChecks.nativeHopperRetainsCanceledGrateOutput(test); }
    @GameTest(template = EMPTY_STRUCTURE, batch = "review_arcane_cursor")
    public void reviewArcaneCallbackCursorAndOverflow(GameTestHelper test) { ComprehensiveArcaneReviewRuntimeChecks.craftCallbacksRetainReplacementCursorAndOverflow(test); }
    @GameTest(template = EMPTY_STRUCTURE, batch = "review_key_scope")
    public void reviewArcaneKeyScope(GameTestHelper test) { ArcaneKeyScopeReviewRuntimeChecks.keysCannotGrantAcrossDimensionsOrBlockKinds(test); }
    @GameTest(template = EMPTY_STRUCTURE, batch = "review_legacy_key_scope")
    public void reviewLegacyKeyScope(GameTestHelper test) { ArcaneKeyScopeReviewRuntimeChecks.legacyUnscopedKeysDoNotGrantAccess(test); }
    @GameTest(template = EMPTY_STRUCTURE, batch = "review_hole_create_border")
    public void reviewPortableHoleCreationBoundary(GameTestHelper test) { ComprehensivePortableHoleReviewRuntimeChecks.creationRespectsWorldBorder(test); }
    @GameTest(template = EMPTY_STRUCTURE, batch = "review_hole_focus_border")
    public void reviewPortableHoleFocusBoundary(GameTestHelper test) { ComprehensivePortableHoleReviewRuntimeChecks.focusRefusesBorderCrossingBeforePayment(test); }
    @GameTest(template = EMPTY_STRUCTURE, batch = "review_hole_tick_border")
    public void reviewPortableHoleDelayedBoundary(GameTestHelper test) { ComprehensivePortableHoleReviewRuntimeChecks.delayedSpreadRespectsChangedBorderAndRestores(test); }
    @GameTest(template = EMPTY_STRUCTURE, batch = "review_example_spark")
    public void reviewExampleSparkPermissions(GameTestHelper test) { ExampleSparkReviewRuntimeChecks.sparkRespectsWorldBorderAndAdventure(test); }
    @GameTest(template = EMPTY_STRUCTURE, batch = "review_key_output_cancel")
    public void reviewArcaneKeyOutputCancellation(GameTestHelper test) { ArcaneKeyOutputReviewRuntimeChecks.canceledLinkedOutputRetainsOnePaidKey(test); }
    @GameTest(template = EMPTY_STRUCTURE, batch = "review_key_output_reentry")
    public void reviewArcaneKeyOutputReentry(GameTestHelper test) { ArcaneKeyOutputReviewRuntimeChecks.recursiveLinkedOutputDoesNotDuplicateOrDoubleCharge(test); }
    @GameTest(template = EMPTY_STRUCTURE, batch = "review_tag_sync_controls")
    public void reviewArcaneAndCrucibleEmptyTagSync(GameTestHelper test) { ComprehensiveRecipeSyncReviewRuntimeChecks.arcaneAndCrucibleKeepEmptyTagsDisabledAcrossPackets(test); }
    @GameTest(template = EMPTY_STRUCTURE, batch = "review_tag_sync_infusion")
    public void reviewStackInfusionEmptyTagSync(GameTestHelper test) { ComprehensiveRecipeSyncReviewRuntimeChecks.stackInfusionEmptyTagsSurvivePacketsWithoutBecomingCraftable(test); }
    @GameTest(template = EMPTY_STRUCTURE, batch = "review_tag_sync_enchantment")
    public void reviewEnchantmentInfusionEmptyTagSync(GameTestHelper test) { ComprehensiveRecipeSyncReviewRuntimeChecks.enchantmentInfusionEmptyTagSurvivesPacketsWithoutBecomingCraftable(test); }
    @GameTest(template = EMPTY_STRUCTURE, batch = "review_warded_output_cancel")
    public void reviewWardedOutputCancellation(GameTestHelper test) { WardedStoneOutputReviewRuntimeChecks.canceledReclaimRetainsOneRemovedBlock(test); }
    @GameTest(template = EMPTY_STRUCTURE, batch = "review_warded_output_reentry")
    public void reviewWardedOutputReentry(GameTestHelper test) { WardedStoneOutputReviewRuntimeChecks.recursiveReclaimCannotDuplicateTheSameBlock(test); }
}
