package thaumic.tinkerer.fabric.testing;

import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import thaumic.tinkerer.testing.SimpleBlocksRuntimeChecks;

/**
 * Fabric wrappers of {@link SimpleBlocksRuntimeChecks}: one wrapper per check. Test-only, not distributed.
 *
 * <p>The two checks that count the victims of the potion handler (a table shared by the whole server) have a batch of
 * their own, so that no other check of the group hits somebody at the same time; batches run one after the other.</p>
 */
public final class SimpleBlocksGameTests implements FabricGameTest {
    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE)
    public void registrationIsComplete(GameTestHelper test) {
        SimpleBlocksRuntimeChecks.registrationIsComplete(test);
    }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE)
    public void tagsOfTheGroupAreBound(GameTestHelper test) {
        SimpleBlocksRuntimeChecks.tagsOfTheGroupAreBound(test);
    }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE)
    public void quartzRecipesCraftTheirResults(GameTestHelper test) {
        SimpleBlocksRuntimeChecks.quartzRecipesCraftTheirResults(test);
    }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE)
    public void pavingRecipesCraftTheirResults(GameTestHelper test) {
        SimpleBlocksRuntimeChecks.pavingRecipesCraftTheirResults(test);
    }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE)
    public void dropsAreTheOnesOfTheOriginal(GameTestHelper test) {
        SimpleBlocksRuntimeChecks.dropsAreTheOnesOfTheOriginal(test);
    }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE)
    public void aGasCloudSpreadsInDiamondsAndSettles(GameTestHelper test) {
        SimpleBlocksRuntimeChecks.aGasCloudSpreadsInDiamondsAndSettles(test);
    }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE)
    public void gasSpreadsIntoAirAndOtherGasButNotIntoSolidBlocksOrItself(GameTestHelper test) {
        SimpleBlocksRuntimeChecks.gasSpreadsIntoAirAndOtherGasButNotIntoSolidBlocksOrItself(test);
    }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE)
    public void aGasStateIsTheLightAndTheShadowOfTheOriginal(GameTestHelper test) {
        SimpleBlocksRuntimeChecks.aGasStateIsTheLightAndTheShadowOfTheOriginal(test);
    }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE, timeoutTicks = 200)
    public void aPhialReleasesGasAtTheHeadAndTheCloudSpreadsByItself(GameTestHelper test) {
        SimpleBlocksRuntimeChecks.aPhialReleasesGasAtTheHeadAndTheCloudSpreadsByItself(test);
    }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE)
    public void aPhialIsUsedUpEvenWithoutRoomButNotInCreativeMode(GameTestHelper test) {
        SimpleBlocksRuntimeChecks.aPhialIsUsedUpEvenWithoutRoomButNotInCreativeMode(test);
    }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE)
    public void theGasRemoverClearsACubeOnlyWhileSneaking(GameTestHelper test) {
        SimpleBlocksRuntimeChecks.theGasRemoverClearsACubeOnlyWhileSneaking(test);
    }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE)
    public void nitorGasStaysOnlyWhileACarrierIsNear(GameTestHelper test) {
        SimpleBlocksRuntimeChecks.nitorGasStaysOnlyWhileACarrierIsNear(test);
    }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE)
    public void theBrightNitorMakesGasAtTheHoldersHead(GameTestHelper test) {
        SimpleBlocksRuntimeChecks.theBrightNitorMakesGasAtTheHoldersHead(test);
    }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE)
    public void thePlatformIsSolidOnlyForWhoStandsAbove(GameTestHelper test) {
        SimpleBlocksRuntimeChecks.thePlatformIsSolidOnlyForWhoStandsAbove(test);
    }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE)
    public void aCamouflageIsSetAndClearedByTheHand(GameTestHelper test) {
        SimpleBlocksRuntimeChecks.aCamouflageIsSetAndClearedByTheHand(test);
    }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE)
    public void theTravelSlabAndStairsQuickenWhoWalkOnThem(GameTestHelper test) {
        SimpleBlocksRuntimeChecks.theTravelSlabAndStairsQuickenWhoWalkOnThem(test);
    }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE)
    public void aWardSlabSealsTheTwoCellsAbove(GameTestHelper test) {
        SimpleBlocksRuntimeChecks.aWardSlabSealsTheTwoCellsAbove(test);
    }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE)
    public void aWardSlabTurnsWithTheRedstoneSignal(GameTestHelper test) {
        SimpleBlocksRuntimeChecks.aWardSlabTurnsWithTheRedstoneSignal(test);
    }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE)
    public void aWardSlabPushesFloatingMobsAwayButNotPlayersOrPoweredOnes(GameTestHelper test) {
        SimpleBlocksRuntimeChecks.aWardSlabPushesFloatingMobsAwayButNotPlayersOrPoweredOnes(test);
    }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE)
    public void aForcefieldIsSolidForThreeSecondsAndThenGone(GameTestHelper test) {
        SimpleBlocksRuntimeChecks.aForcefieldIsSolidForThreeSecondsAndThenGone(test);
    }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE)
    public void seedsPlantOnFarmlandAndInfuseTheSoil(GameTestHelper test) {
        SimpleBlocksRuntimeChecks.seedsPlantOnFarmlandAndInfuseTheSoil(test);
    }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE)
    public void seedsRefuseWhatIsNotFarmlandOrFreeSpace(GameTestHelper test) {
        SimpleBlocksRuntimeChecks.seedsRefuseWhatIsNotFarmlandOrFreeSpace(test);
    }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE)
    public void aRipeCropYieldsSeedsLootAndLeavesResidue(GameTestHelper test) {
        SimpleBlocksRuntimeChecks.aRipeCropYieldsSeedsLootAndLeavesResidue(test);
    }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE)
    public void residueInTheSoilBreedsANewAspect(GameTestHelper test) {
        SimpleBlocksRuntimeChecks.residueInTheSoilBreedsANewAspect(test);
    }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE)
    public void aCropGrowsInItsTileAndNotByRandomTicks(GameTestHelper test) {
        SimpleBlocksRuntimeChecks.aCropGrowsInItsTileAndNotByRandomTicks(test);
    }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE)
    public void bonemealGrowsTheCropOnlyWhenTheConfigurationAllowsAndKeepsItsPicture(GameTestHelper test) {
        SimpleBlocksRuntimeChecks.bonemealGrowsTheCropOnlyWhenTheConfigurationAllowsAndKeepsItsPicture(test);
    }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE)
    public void aCropNeedsFarmland(GameTestHelper test) {
        SimpleBlocksRuntimeChecks.aCropNeedsFarmland(test);
    }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE)
    public void infusedSoilIsNeverTrampledOrDriedAndTurnsToDirtUnderStone(GameTestHelper test) {
        SimpleBlocksRuntimeChecks.infusedSoilIsNeverTrampledOrDriedAndTurnsToDirtUnderStone(test);
    }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE)
    public void theSoilAndTheCropRememberTheirAspects(GameTestHelper test) {
        SimpleBlocksRuntimeChecks.theSoilAndTheCropRememberTheirAspects(test);
    }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE)
    public void theSoilAndTheCropHoldAtMostTheirCaps(GameTestHelper test) {
        SimpleBlocksRuntimeChecks.theSoilAndTheCropHoldAtMostTheirCaps(test);
    }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE)
    public void theGrainAndPotionItemsCarryTheirAspects(GameTestHelper test) {
        SimpleBlocksRuntimeChecks.theGrainAndPotionItemsCarryTheirAspects(test);
    }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE)
    public void theFourEffectsAreBadEffectsWithoutAnyPowerOfTheirOwn(GameTestHelper test) {
        SimpleBlocksRuntimeChecks.theFourEffectsAreBadEffectsWithoutAnyPowerOfTheirOwn(test);
    }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE)
    public void drinkingAPotionGivesThreeMinutesOfItsEffect(GameTestHelper test) {
        SimpleBlocksRuntimeChecks.drinkingAPotionGivesThreeMinutesOfItsEffect(test);
    }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE, batch = "simple_blocks_air_hit", timeoutTicks = 200)
    public void anAirHitFlingsTheVictimForAWhile(GameTestHelper test) {
        SimpleBlocksRuntimeChecks.anAirHitFlingsTheVictimForAWhile(test);
    }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE, batch = "simple_blocks_fire_hit", timeoutTicks = 200)
    public void aFireHitSetsTheVictimAlight(GameTestHelper test) {
        SimpleBlocksRuntimeChecks.aFireHitSetsTheVictimAlight(test);
    }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE)
    public void anEarthHitRaisesAWallHalfWayBetween(GameTestHelper test) {
        SimpleBlocksRuntimeChecks.anEarthHitRaisesAWallHalfWayBetween(test);
    }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE, timeoutTicks = 200)
    public void aWaterImbuedPlayerTurnsNearbyLavaIntoObsidian(GameTestHelper test) {
        SimpleBlocksRuntimeChecks.aWaterImbuedPlayerTurnsNearbyLavaIntoObsidian(test);
    }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE)
    public void theFiresTransmuteTheBlocksOfTheirTables(GameTestHelper test) {
        SimpleBlocksRuntimeChecks.theFiresTransmuteTheBlocksOfTheirTables(test);
    }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE)
    public void theFiresKnowTheFlammabilityOfTheOriginal(GameTestHelper test) {
        SimpleBlocksRuntimeChecks.theFiresKnowTheFlammabilityOfTheOriginal(test);
    }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE)
    public void anAerFireTurnsTheDirtNextToItIntoSand(GameTestHelper test) {
        SimpleBlocksRuntimeChecks.anAerFireTurnsTheDirtNextToItIntoSand(test);
    }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE)
    public void anImbuedFireBurnsOutWithoutFuelUnlessItIsASource(GameTestHelper test) {
        SimpleBlocksRuntimeChecks.anImbuedFireBurnsOutWithoutFuelUnlessItIsASource(test);
    }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE)
    public void aChaosFireTurnsImbuedFireIntoPlainFire(GameTestHelper test) {
        SimpleBlocksRuntimeChecks.aChaosFireTurnsImbuedFireIntoPlainFire(test);
    }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE, timeoutTicks = 200)
    public void anImbuedFireIsAFireForTheGame(GameTestHelper test) {
        SimpleBlocksRuntimeChecks.anImbuedFireIsAFireForTheGame(test);
    }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE)
    public void nothingFailedWhileTheGroupWasRegistered(GameTestHelper test) {
        SimpleBlocksRuntimeChecks.nothingFailedWhileTheGroupWasRegistered(test);
    }
}
