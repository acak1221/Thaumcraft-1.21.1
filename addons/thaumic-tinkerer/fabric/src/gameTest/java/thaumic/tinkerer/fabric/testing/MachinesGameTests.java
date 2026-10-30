/**
 * New in the 1.21.1 port of Thaumic Tinkerer (no 1.7.10 counterpart, see addons/thaumic-tinkerer/NOTICE.md): test-only
 * code, distributed under the same licence as the add-on (CC BY-NC-SA 3.0).
 */
package thaumic.tinkerer.fabric.testing;

import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import thaumic.tinkerer.testing.MachinesRuntimeChecks;

/** Fabric wrappers of {@link MachinesRuntimeChecks}: one per check. Test-only, not distributed. */
public final class MachinesGameTests implements FabricGameTest {
    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE)
    public void registrationIsComplete(GameTestHelper test) {
        MachinesRuntimeChecks.registrationIsComplete(test);
    }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE)
    public void repairerTakesAndGivesBackADamagedTool(GameTestHelper test) {
        MachinesRuntimeChecks.repairerTakesAndGivesBackADamagedTool(test);
    }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE)
    public void repairerAutomationTakesDamagedToolsAndGivesMendedOnes(GameTestHelper test) {
        MachinesRuntimeChecks.repairerAutomationTakesDamagedToolsAndGivesMendedOnes(test);
    }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE)
    public void repairerFacesTheWayThePlayerLooked(GameTestHelper test) {
        MachinesRuntimeChecks.repairerFacesTheWayThePlayerLooked(test);
    }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE)
    public void repairerSavesItsToolAndItsState(GameTestHelper test) {
        MachinesRuntimeChecks.repairerSavesItsToolAndItsState(test);
    }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE)
    public void funnelNeedsAHopperUnderIt(GameTestHelper test) {
        MachinesRuntimeChecks.funnelNeedsAHopperUnderIt(test);
    }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE)
    public void funnelTakesFilledJarsOnly(GameTestHelper test) {
        MachinesRuntimeChecks.funnelTakesFilledJarsOnly(test);
    }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE)
    public void funnelMovesEssentiaToTheJarTheHopperPointsAt(GameTestHelper test) {
        MachinesRuntimeChecks.funnelMovesEssentiaToTheJarTheHopperPointsAt(test);
    }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE)
    public void funnelDoesNotCrashOnAJarLabelledWithAnotherAspect(GameTestHelper test) {
        MachinesRuntimeChecks.funnelDoesNotCrashOnAJarLabelledWithAnotherAspect(test);
    }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE)
    public void funnelDoesNotFillAJarOfAnotherAspect(GameTestHelper test) {
        MachinesRuntimeChecks.funnelDoesNotFillAJarOfAnotherAspect(test);
    }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE)
    public void funnelSavesItsJar(GameTestHelper test) {
        MachinesRuntimeChecks.funnelSavesItsJar(test);
    }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE)
    public void aKineticAttractorPullsItemsAndPushesThemAway(GameTestHelper test) {
        MachinesRuntimeChecks.aKineticAttractorPullsItemsAndPushesThemAway(test);
    }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE)
    public void aMagnetWithoutSignalDoesNothing(GameTestHelper test) {
        MachinesRuntimeChecks.aMagnetWithoutSignalDoesNothing(test);
    }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE)
    public void aMagnetOnlyMovesItems(GameTestHelper test) {
        MachinesRuntimeChecks.aMagnetOnlyMovesItems(test);
    }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE)
    public void aMagnetChangesPolarityWhenClicked(GameTestHelper test) {
        MachinesRuntimeChecks.aMagnetChangesPolarityWhenClicked(test);
    }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE)
    public void aCorporealAttractorChoosesByMouldAndAge(GameTestHelper test) {
        MachinesRuntimeChecks.aCorporealAttractorChoosesByMouldAndAge(test);
    }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE)
    public void aCorporealAttractorMovesTheMobsItSelects(GameTestHelper test) {
        MachinesRuntimeChecks.aCorporealAttractorMovesTheMobsItSelects(test);
    }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE)
    public void theMobMagnetMenuSetsTheAdultFlagWithItsButtons(GameTestHelper test) {
        MachinesRuntimeChecks.theMobMagnetMenuSetsTheAdultFlagWithItsButtons(test);
    }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE)
    public void theMobMagnetSavesItsMouldAndItsFlag(GameTestHelper test) {
        MachinesRuntimeChecks.theMobMagnetSavesItsMouldAndItsFlag(test);
    }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE)
    public void aSoulMouldLearnsAndForgets(GameTestHelper test) {
        MachinesRuntimeChecks.aSoulMouldLearnsAndForgets(test);
    }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE)
    public void theAspectalyzerHoldsAStackAndKnowsItsAspects(GameTestHelper test) {
        MachinesRuntimeChecks.theAspectalyzerHoldsAStackAndKnowsItsAspects(test);
    }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE)
    public void theAspectalyzerSlotTakesItemsWithAspectsOnly(GameTestHelper test) {
        MachinesRuntimeChecks.theAspectalyzerSlotTakesItemsWithAspectsOnly(test);
    }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE)
    public void theTabletSummonsAWolfFromThreeBestiaAndUsesThemUp(GameTestHelper test) {
        MachinesRuntimeChecks.theTabletSummonsAWolfFromThreeBestiaAndUsesThemUp(test);
    }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE)
    public void theTabletDoesNothingForAnUnknownSet(GameTestHelper test) {
        MachinesRuntimeChecks.theTabletDoesNothingForAnUnknownSet(test);
    }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE)
    public void infusedSoulAspectsSummonOnlyEveryTwentyMinutesAreKeptAndBindToTheLastTablet(GameTestHelper test) {
        MachinesRuntimeChecks.infusedSoulAspectsSummonOnlyEveryTwentyMinutesAreKeptAndBindToTheLastTablet(test);
    }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE)
    public void aMixedSetIsUsedUpEvenWhenOneIsInfused(GameTestHelper test) {
        MachinesRuntimeChecks.aMixedSetIsUsedUpEvenWhenOneIsInfused(test);
    }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE)
    public void aSummonedSlimeIsSizeOne(GameTestHelper test) {
        MachinesRuntimeChecks.aSummonedSlimeIsSizeOne(test);
    }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE)
    public void aWitherSkeletonComesWithAStoneSword(GameTestHelper test) {
        MachinesRuntimeChecks.aWitherSkeletonComesWithAStoneSword(test);
    }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE)
    public void theBladeSwitchesTheHarvestWithAShiftRightClick(GameTestHelper test) {
        MachinesRuntimeChecks.theBladeSwitchesTheHarvestWithAShiftRightClick(test);
    }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE)
    public void theBladeHasTheOriginalNumbers(GameTestHelper test) {
        MachinesRuntimeChecks.theBladeHasTheOriginalNumbers(test);
    }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE)
    public void aBladeHurtsWhoStrikesWithIt(GameTestHelper test) {
        MachinesRuntimeChecks.aBladeHurtsWhoStrikesWithIt(test);
    }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE, timeoutTicks = 200)
    public void aBlockingWielderTakesThreeMagicDamageInsteadOfTheBlow(GameTestHelper test) {
        MachinesRuntimeChecks.aBlockingWielderTakesThreeMagicDamageInsteadOfTheBlow(test);
    }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE)
    public void anActivatedBladeReapsTheSoulAspectsOfTheMobItKills(GameTestHelper test) {
        MachinesRuntimeChecks.anActivatedBladeReapsTheSoulAspectsOfTheMobItKills(test);
    }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE)
    public void aBladeThatIsNotActivatedLeavesTheLootAlone(GameTestHelper test) {
        MachinesRuntimeChecks.aBladeThatIsNotActivatedLeavesTheLootAlone(test);
    }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE)
    public void theBladeLeavesTheDeathsThatAreNotItsKillsAlone(GameTestHelper test) {
        MachinesRuntimeChecks.theBladeLeavesTheDeathsThatAreNotItsKillsAlone(test);
    }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE)
    public void anActivatedBladeClearsTheEquipmentToo(GameTestHelper test) {
        MachinesRuntimeChecks.anActivatedBladeClearsTheEquipmentToo(test);
    }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE, timeoutTicks = 200)
    public void aRealSwingOfTheActivatedBladeReapsAndHurtsItsWielder(GameTestHelper test) {
        MachinesRuntimeChecks.aRealSwingOfTheActivatedBladeReapsAndHurtsItsWielder(test);
    }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE)
    public void aReapedEndermanAndZombifiedPiglinStillRollTheirDimensionalShard(GameTestHelper test) {
        MachinesRuntimeChecks.aReapedEndermanAndZombifiedPiglinStillRollTheirDimensionalShard(test);
    }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE)
    public void everyMobOfTheTableHasAnEntityType(GameTestHelper test) {
        MachinesRuntimeChecks.everyMobOfTheTableHasAnEntityType(test);
    }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE)
    public void aSoulMouldLearnsThroughAPlayersInteractionAlsoInCreative(GameTestHelper test) {
        MachinesRuntimeChecks.aSoulMouldLearnsThroughAPlayersInteractionAlsoInCreative(test);
    }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE)
    public void breakingTheAspectalyzerScattersItsStackAndDropsTheBlock(GameTestHelper test) {
        MachinesRuntimeChecks.breakingTheAspectalyzerScattersItsStackAndDropsTheBlock(test);
    }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE)
    public void breakingTheRestorerScattersItsToolAndDropsTheBlock(GameTestHelper test) {
        MachinesRuntimeChecks.breakingTheRestorerScattersItsToolAndDropsTheBlock(test);
    }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE)
    public void breakingTheFunnelScattersItsJarAndDropsTheBlock(GameTestHelper test) {
        MachinesRuntimeChecks.breakingTheFunnelScattersItsJarAndDropsTheBlock(test);
    }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE)
    public void theFunnelPopsOffWithItsJarWhenTheHopperGoes(GameTestHelper test) {
        MachinesRuntimeChecks.theFunnelPopsOffWithItsJarWhenTheHopperGoes(test);
    }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE)
    public void breakingTheCorporealAttractorScattersItsMouldAndDropsTheBlock(GameTestHelper test) {
        MachinesRuntimeChecks.breakingTheCorporealAttractorScattersItsMouldAndDropsTheBlock(test);
    }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE)
    public void everyMachineBlockDropsItselfFromItsLootTable(GameTestHelper test) {
        MachinesRuntimeChecks.everyMachineBlockDropsItselfFromItsLootTable(test);
    }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE)
    public void onlyTheAspectalyzerAndTheFunnelConductRedstoneAsIn1710(GameTestHelper test) {
        MachinesRuntimeChecks.onlyTheAspectalyzerAndTheFunnelConductRedstoneAsIn1710(test);
    }
}
