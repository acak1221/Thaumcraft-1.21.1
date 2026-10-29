/**
 * New in the 1.21.1 port of Thaumic Tinkerer (no 1.7.10 counterpart): the Fabric GameTest wrappers of the KamiGear checks. Distributed under the same licence as the add-on
 * (CC BY-NC-SA 3.0, see addons/thaumic-tinkerer/NOTICE.md).
 */
package thaumic.tinkerer.fabric.testing;

import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import thaumic.tinkerer.testing.KamiGearRuntimeChecks;

/** Fabric wrappers of {@link KamiGearRuntimeChecks}: one per check. Test-only, not distributed. */
public final class KamiGearGameTests implements FabricGameTest {
    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE)
    public void registrationIsComplete(GameTestHelper test) {
        KamiGearRuntimeChecks.registrationIsComplete(test);
    }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE)
    public void dataPackCarriesTheTagsAndTheNuggetRecipe(GameTestHelper test) {
        KamiGearRuntimeChecks.dataPackCarriesTheTagsAndTheNuggetRecipe(test);
    }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE)
    public void toolsNeverBreakAndTakeEnchantments(GameTestHelper test) {
        KamiGearRuntimeChecks.toolsNeverBreakAndTakeEnchantments(test);
    }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE)
    public void awakenedToolModesCycleAndSurviveSaving(GameTestHelper test) {
        KamiGearRuntimeChecks.awakenedToolModesCycleAndSurviveSaving(test);
    }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE)
    public void awakenedPickaxeBreaksOneBlockInModeZeroAndASquareInModeOne(GameTestHelper test) {
        KamiGearRuntimeChecks.awakenedPickaxeBreaksOneBlockInModeZeroAndASquareInModeOne(test);
    }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE)
    public void leftClickingBedrockWithTheAwakenedPickaxeBreaksTheSurroundings(GameTestHelper test) {
        KamiGearRuntimeChecks.leftClickingBedrockWithTheAwakenedPickaxeBreaksTheSurroundings(test);
    }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE)
    public void awakenedPickaxeLineModeDigsIntoTheWall(GameTestHelper test) {
        KamiGearRuntimeChecks.awakenedPickaxeLineModeDigsIntoTheWall(test);
    }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE)
    public void talismanBindsToAClickedBlockAndSurvivesSaving(GameTestHelper test) {
        KamiGearRuntimeChecks.talismanBindsToAClickedBlockAndSurvivesSaving(test);
    }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE)
    public void talismanFillsAChestAndLosesNothingWhenItIsFull(GameTestHelper test) {
        KamiGearRuntimeChecks.talismanFillsAChestAndLosesNothingWhenItIsFull(test);
    }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE)
    public void talismanPlacesBlocksOneByOneAndKeepsThemWhenBlocked(GameTestHelper test) {
        KamiGearRuntimeChecks.talismanPlacesBlocksOneByOneAndKeepsThemWhenBlocked(test);
    }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE)
    public void talismanSneakClickTogglesAndTheRingIsNotEquippedByClicking(GameTestHelper test) {
        KamiGearRuntimeChecks.talismanSneakClickTogglesAndTheRingIsNotEquippedByClicking(test);
    }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE)
    public void talismanWornMergesStacksWithoutLosingBlocks(GameTestHelper test) {
        KamiGearRuntimeChecks.talismanWornMergesStacksWithoutLosingBlocks(test);
    }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE)
    public void pouchKeepsItsSlotsAndRefusesItself(GameTestHelper test) {
        KamiGearRuntimeChecks.pouchKeepsItsSlotsAndRefusesItself(test);
    }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE)
    public void pouchOpensItsMenuOnUseInTheHandThatHoldsIt(GameTestHelper test) {
        KamiGearRuntimeChecks.pouchOpensItsMenuOnUseInTheHandThatHoldsIt(test);
    }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE)
    public void mirrorChoosesItsBlockAndCyclesItsSize(GameTestHelper test) {
        KamiGearRuntimeChecks.mirrorChoosesItsBlockAndCyclesItsSize(test);
    }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE)
    public void mirrorPlacesTheSquareOnlyWithEnoughBlocksAndTakesThemFromTheInventory(GameTestHelper test) {
        KamiGearRuntimeChecks.mirrorPlacesTheSquareOnlyWithEnoughBlocksAndTakesThemFromTheInventory(test);
    }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE)
    public void awakenedArmorPiecesSwitchOffOnSneakClickAndKeepTheSwitch(GameTestHelper test) {
        KamiGearRuntimeChecks.awakenedArmorPiecesSwitchOffOnSneakClickAndKeepTheSwitch(test);
    }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE)
    public void awakenedChestplateGrantsFlightOnlyWhileItIsOnAndSwitchedOn(GameTestHelper test) {
        KamiGearRuntimeChecks.awakenedChestplateGrantsFlightOnlyWhileItIsOnAndSwitchedOn(test);
    }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE)
    public void skyPearlKeepsItsDestinationUntilSneakClicked(GameTestHelper test) {
        KamiGearRuntimeChecks.skyPearlKeepsItsDestinationUntilSneakClicked(test);
    }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE)
    public void protoclayPutsTheRightAwakenedToolInTheHand(GameTestHelper test) {
        KamiGearRuntimeChecks.protoclayPutsTheRightAwakenedToolInTheHand(test);
    }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE)
    public void experienceDrainRemovesExperiencePointsNotLevels(GameTestHelper test) {
        KamiGearRuntimeChecks.experienceDrainRemovesExperiencePointsNotLevels(test);
    }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE)
    public void shadowbeamHurtsWhatItCrossesAndBouncesOffWalls(GameTestHelper test) {
        KamiGearRuntimeChecks.shadowbeamHurtsWhatItCrossesAndBouncesOffWalls(test);
    }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE)
    public void mirrorNeedsNoBlocksInCreativeMode(GameTestHelper test) {
        KamiGearRuntimeChecks.mirrorNeedsNoBlocksInCreativeMode(test);
    }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE)
    public void mirrorPaysFromTheFirstRingThatHasBlocks(GameTestHelper test) {
        KamiGearRuntimeChecks.mirrorPaysFromTheFirstRingThatHasBlocks(test);
    }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE)
    public void mirrorRefusesStairsAndTakesSlabs(GameTestHelper test) {
        KamiGearRuntimeChecks.mirrorRefusesStairsAndTakesSlabs(test);
    }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE)
    public void pouchMenuWritesOnlyToThePouchItWasOpenedFor(GameTestHelper test) {
        KamiGearRuntimeChecks.pouchMenuWritesOnlyToThePouchItWasOpenedFor(test);
    }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE)
    public void catAmuletScaresCreepersAndLeavesProvokedAndBrainMobsAlone(GameTestHelper test) {
        KamiGearRuntimeChecks.catAmuletScaresCreepersAndLeavesProvokedAndBrainMobsAlone(test);
    }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE)
    public void stuckProjectilesAreNotPushedByTheRobes(GameTestHelper test) {
        KamiGearRuntimeChecks.stuckProjectilesAreNotPushedByTheRobes(test);
    }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE)
    public void awakenedPickaxeGathersTheDropsOfTheClickedBlockToo(GameTestHelper test) {
        KamiGearRuntimeChecks.awakenedPickaxeGathersTheDropsOfTheClickedBlockToo(test);
    }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE)
    public void awakenedShovelColumnModeTakesOnlyTheSameBlockAlongTheColumn(GameTestHelper test) {
        KamiGearRuntimeChecks.awakenedShovelColumnModeTakesOnlyTheSameBlockAlongTheColumn(test);
    }
}
