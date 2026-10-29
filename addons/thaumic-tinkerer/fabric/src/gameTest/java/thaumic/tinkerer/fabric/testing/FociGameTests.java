package thaumic.tinkerer.fabric.testing;

import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import thaumic.tinkerer.testing.FociRuntimeChecks;

/** Fabric wrappers of {@link FociRuntimeChecks}: one per check. Test-only, not distributed. */
public final class FociGameTests implements FabricGameTest {
    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE)
    public void registrationIsComplete(GameTestHelper test) {
        FociRuntimeChecks.registrationIsComplete(test);
    }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE)
    public void researchEntriesHaveTheOriginalPlaces(GameTestHelper test) {
        FociRuntimeChecks.researchEntriesHaveTheOriginalPlaces(test);
    }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE)
    public void recipesHaveTheOriginalNumbers(GameTestHelper test) {
        FociRuntimeChecks.recipesHaveTheOriginalNumbers(test);
    }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE)
    public void focusesCarryTheOriginalNumbers(GameTestHelper test) {
        FociRuntimeChecks.focusesCarryTheOriginalNumbers(test);
    }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE)
    public void everyFocusMountsOnAWandAndSurvivesSaving(GameTestHelper test) {
        FociRuntimeChecks.everyFocusMountsOnAWandAndSurvivesSaving(test);
    }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE)
    public void heldFociStartTheUseAndTheOthersDoNot(GameTestHelper test) {
        FociRuntimeChecks.heldFociStartTheUseAndTheOthersDoNot(test);
    }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE)
    public void flightLaunchesThePlayerAndPays(GameTestHelper test) {
        FociRuntimeChecks.flightLaunchesThePlayerAndPays(test);
    }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE)
    public void flightPutsTheKickCounterOfTheConnectionBackToZero(GameTestHelper test) {
        FociRuntimeChecks.flightPutsTheKickCounterOfTheConnectionBackToZero(test);
    }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE)
    public void healMendsOnePointEveryThirtyTicksAndPaysPerHeal(GameTestHelper test) {
        FociRuntimeChecks.healMendsOnePointEveryThirtyTicksAndPaysPerHeal(test);
    }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE)
    public void smeltReadsTheFurnaceRecipesOfTheLevel(GameTestHelper test) {
        FociRuntimeChecks.smeltReadsTheFurnaceRecipesOfTheLevel(test);
    }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE)
    public void smeltTurnsCobblestoneIntoStoneAfterTwentyOneTicks(GameTestHelper test) {
        FociRuntimeChecks.smeltTurnsCobblestoneIntoStoneAfterTwentyOneTicks(test);
    }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE)
    public void smeltLeavesWhatTheFurnaceDoesNotMakeIntoABlock(GameTestHelper test) {
        FociRuntimeChecks.smeltLeavesWhatTheFurnaceDoesNotMakeIntoABlock(test);
    }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE)
    public void telekinesisPullsItemsAndPaysOnlyWhenThereAreSome(GameTestHelper test) {
        FociRuntimeChecks.telekinesisPullsItemsAndPaysOnlyWhenThereAreSome(test);
    }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE)
    public void deflectShiftsForeignProjectilesAndSparesTheOwnAndTheBlacklisted(GameTestHelper test) {
        FociRuntimeChecks.deflectShiftsForeignProjectilesAndSparesTheOwnAndTheBlacklisted(test);
    }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE)
    public void deflectEnlargeWidensTheBox(GameTestHelper test) {
        FociRuntimeChecks.deflectEnlargeWidensTheBox(test);
    }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE)
    public void distortionLetsArrowsFlyPastTheHolderWhileTheOtherPlayerIsHit(GameTestHelper test) {
        FociRuntimeChecks.distortionLetsArrowsFlyPastTheHolderWhileTheOtherPlayerIsHit(test);
    }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE)
    public void enderChestOpensTheChestOfThePlayerAndPays(GameTestHelper test) {
        FociRuntimeChecks.enderChestOpensTheChestOfThePlayerAndPays(test);
    }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE)
    public void dislocationCarriesAChestWithItsContentsAndPutsItDown(GameTestHelper test) {
        FociRuntimeChecks.dislocationCarriesAChestWithItsContentsAndPutsItDown(test);
    }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE)
    public void dislocationCastTakesTheBlockTheRayHitsAndPlacesAgainstTheNextOne(GameTestHelper test) {
        FociRuntimeChecks.dislocationCastTakesTheBlockTheRayHitsAndPlacesAgainstTheNextOne(test);
    }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE)
    public void dislocationRefusesWhatItMust(GameTestHelper test) {
        FociRuntimeChecks.dislocationRefusesWhatItMust(test);
    }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE)
    public void dislocationRefusesABlockEntityThatIsTooLargeToCarry(GameTestHelper test) {
        FociRuntimeChecks.dislocationRefusesABlockEntityThatIsTooLargeToCarry(test);
    }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE)
    public void dislocationCarriesALargeButAllowedChestAcrossTheNetwork(GameTestHelper test) {
        FociRuntimeChecks.dislocationCarriesALargeButAllowedChestAcrossTheNetwork(test);
    }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE)
    public void dislocationTakesASpawnerAtTheSpawnerPrice(GameTestHelper test) {
        FociRuntimeChecks.dislocationTakesASpawnerAtTheSpawnerPrice(test);
    }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE)
    public void frugalLowersThePriceAndPotencyQuickensMending(GameTestHelper test) {
        FociRuntimeChecks.frugalLowersThePriceAndPotencyQuickensMending(test);
    }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE)
    public void aRunicStaffAddsAPotencyLevelToEfreetsFlame(GameTestHelper test) {
        FociRuntimeChecks.aRunicStaffAddsAPotencyLevelToEfreetsFlame(test);
    }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE)
    public void mendingWorksWhileTheWandIsHeldAcrossRealTicks(GameTestHelper test) {
        FociRuntimeChecks.mendingWorksWhileTheWandIsHeldAcrossRealTicks(test);
    }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE)
    public void telekinesisWorksWhileTheWandIsHeldAcrossRealTicks(GameTestHelper test) {
        FociRuntimeChecks.telekinesisWorksWhileTheWandIsHeldAcrossRealTicks(test);
    }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE)
    public void dislocationRefusesThePortableHoleBlacklist(GameTestHelper test) {
        FociRuntimeChecks.dislocationRefusesThePortableHoleBlacklist(test);
    }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE)
    public void dislocationCarriesWaterAsASource(GameTestHelper test) {
        FociRuntimeChecks.dislocationCarriesWaterAsASource(test);
    }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE)
    public void dislocationDoesNotTakeFlowingWater(GameTestHelper test) {
        FociRuntimeChecks.dislocationDoesNotTakeFlowingWater(test);
    }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE)
    public void dislocationRefusesTheHalvesOfTallPlantsAndNeverOverwritesTheBlockAbove(GameTestHelper test) {
        FociRuntimeChecks.dislocationRefusesTheHalvesOfTallPlantsAndNeverOverwritesTheBlockAbove(test);
    }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE)
    public void dislocationKeepsTheStateAndTheItemsOfAFurnace(GameTestHelper test) {
        FociRuntimeChecks.dislocationKeepsTheStateAndTheItemsOfAFurnace(test);
    }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE)
    public void aHalfOfADoubleChestIsASingleChestInTheFocus(GameTestHelper test) {
        FociRuntimeChecks.aHalfOfADoubleChestIsASingleChestInTheFocus(test);
    }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE)
    public void twoLoadsHaveTwoKeysInTheFocusSelector(GameTestHelper test) {
        FociRuntimeChecks.twoLoadsHaveTwoKeysInTheFocusSelector(test);
    }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE)
    public void registrationHadNoFailures(GameTestHelper test) {
        FociRuntimeChecks.registrationHadNoFailures(test);
    }
}
