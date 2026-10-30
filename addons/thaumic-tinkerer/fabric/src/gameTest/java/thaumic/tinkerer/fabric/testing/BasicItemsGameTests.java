/**
 * New in the 1.21.1 port of Thaumic Tinkerer (no 1.7.10 counterpart: tests). Distributed under the same licence as the
 * add-on (CC BY-NC-SA 3.0, see addons/thaumic-tinkerer/NOTICE.md).
 */
package thaumic.tinkerer.fabric.testing;

import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import thaumic.tinkerer.testing.BasicItemsRuntimeChecks;

/** GameTests of the BasicItems group: one wrapper per check of thaumic.tinkerer.testing.BasicItemsRuntimeChecks. Test-only. */
public final class BasicItemsGameTests implements FabricGameTest {
    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE)
    public void registrationIsComplete(GameTestHelper test) {
        BasicItemsRuntimeChecks.registrationIsComplete(test);
    }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE)
    public void itemsCarryTheNumbersOfTheOriginal(GameTestHelper test) {
        BasicItemsRuntimeChecks.itemsCarryTheNumbersOfTheOriginal(test);
    }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE)
    public void dataPackCarriesTheRecipesAndTheTags(GameTestHelper test) {
        BasicItemsRuntimeChecks.dataPackCarriesTheRecipesAndTheTags(test);
    }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE)
    public void spellClothWipesTheEnchantmentsInTheCraftingMenu(GameTestHelper test) {
        BasicItemsRuntimeChecks.spellClothWipesTheEnchantmentsInTheCraftingMenu(test);
    }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE)
    public void spellClothLastsThirtySixCleaningsAndTheLastOneDestroysIt(GameTestHelper test) {
        BasicItemsRuntimeChecks.spellClothLastsThirtySixCleaningsAndTheLastOneDestroysIt(test);
    }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE)
    public void spellClothRefusesWhatIsNotACleaning(GameTestHelper test) {
        BasicItemsRuntimeChecks.spellClothRefusesWhatIsNotACleaning(test);
    }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE)
    public void cleansingTalismanSneakClickTogglesAndKeepsTheSwitch(GameTestHelper test) {
        BasicItemsRuntimeChecks.cleansingTalismanSneakClickTogglesAndKeepsTheSwitch(test);
    }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE)
    public void cleansingTalismanRemovesOneEffectPerSecondAndPaysDurability(GameTestHelper test) {
        BasicItemsRuntimeChecks.cleansingTalismanRemovesOneEffectPerSecondAndPaysDurability(test);
    }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE)
    public void cleansingTalismanWorksFromTheAmuletSlot(GameTestHelper test) {
        BasicItemsRuntimeChecks.cleansingTalismanWorksFromTheAmuletSlot(test);
    }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE)
    public void xpTalismanSwitchesOnSneakClickAndKeepsItsPoints(GameTestHelper test) {
        BasicItemsRuntimeChecks.xpTalismanSwitchesOnSneakClickAndKeepsItsPoints(test);
    }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE)
    public void xpTalismanTurnsTenPointsAndABottleIntoABottleOEnchanting(GameTestHelper test) {
        BasicItemsRuntimeChecks.xpTalismanTurnsTenPointsAndABottleIntoABottleOEnchanting(test);
    }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE)
    public void xpTalismanAbsorbsOrbsInRangeUpToItsLimit(GameTestHelper test) {
        BasicItemsRuntimeChecks.xpTalismanAbsorbsOrbsInRangeUpToItsLimit(test);
    }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE)
    public void xpTalismanCountsEveryOrbOfAMergedOrb(GameTestHelper test) {
        BasicItemsRuntimeChecks.xpTalismanCountsEveryOrbOfAMergedOrb(test);
    }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE)
    public void shareTomeBindsToItsPlayerAndHandsOverResearchAndAspects(GameTestHelper test) {
        BasicItemsRuntimeChecks.shareTomeBindsToItsPlayerAndHandsOverResearchAndAspects(test);
    }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE)
    public void shareTomeFallsBackToItsSnapshotWhenItsPlayerIsNotOnline(GameTestHelper test) {
        BasicItemsRuntimeChecks.shareTomeFallsBackToItsSnapshotWhenItsPlayerIsNotOnline(test);
    }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE)
    public void shareTomeRecipeFollowsTheSurvivalOption(GameTestHelper test) {
        BasicItemsRuntimeChecks.shareTomeRecipeFollowsTheSurvivalOption(test);
    }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE)
    public void infusedInkwellIsRefilledByEightInkSacs(GameTestHelper test) {
        BasicItemsRuntimeChecks.infusedInkwellIsRefilledByEightInkSacs(test);
    }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE)
    public void helmOfRevealingIsGogglesAndAThaumiumHelmetWithADiscount(GameTestHelper test) {
        BasicItemsRuntimeChecks.helmOfRevealingIsGogglesAndAThaumiumHelmetWithADiscount(test);
    }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE)
    public void everyGroupItemKeepsItsDataThroughSaving(GameTestHelper test) {
        BasicItemsRuntimeChecks.everyGroupItemKeepsItsDataThroughSaving(test);
    }
}
