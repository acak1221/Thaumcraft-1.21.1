package thaumic.tinkerer.fabric.testing;

import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import thaumic.tinkerer.testing.EnchantmentsRuntimeChecks;

/**
 * Fabric wrappers of {@link EnchantmentsRuntimeChecks}: one per check. Test-only, not distributed. The checks could not be run
 * when they were written (no Gradle in the porting session).
 */
public final class EnchantmentsGameTests implements FabricGameTest {
    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE)
    public void registrationIsComplete(GameTestHelper test) {
        EnchantmentsRuntimeChecks.registrationIsComplete(test);
    }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE)
    public void researchEntriesAreRegistered(GameTestHelper test) {
        EnchantmentsRuntimeChecks.researchEntriesAreRegistered(test);
    }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE)
    public void enchantmentsAreLoadedFromTheDataPack(GameTestHelper test) {
        EnchantmentsRuntimeChecks.enchantmentsAreLoadedFromTheDataPack(test);
    }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE)
    public void exclusiveSetsMatchTheRulesOfTheOriginal(GameTestHelper test) {
        EnchantmentsRuntimeChecks.exclusiveSetsMatchTheRulesOfTheOriginal(test);
    }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE)
    public void theTableOffersWhatTheOriginalOffered(GameTestHelper test) {
        EnchantmentsRuntimeChecks.theTableOffersWhatTheOriginalOffered(test);
    }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE)
    public void enchanterPersists(GameTestHelper test) {
        EnchantmentsRuntimeChecks.enchanterPersists(test);
    }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE)
    public void theMenuLocksTheToolWhileItWorks(GameTestHelper test) {
        EnchantmentsRuntimeChecks.theMenuLocksTheToolWhileItWorks(test);
    }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE)
    public void aMenuOfAnotherPlayerCannotChangeTheEnchanter(GameTestHelper test) {
        EnchantmentsRuntimeChecks.aMenuOfAnotherPlayerCannotChangeTheEnchanter(test);
    }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE)
    public void choosingAndChangingEnchantmentsFollowsTheRules(GameTestHelper test) {
        EnchantmentsRuntimeChecks.choosingAndChangingEnchantmentsFollowsTheRules(test);
    }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE)
    public void withoutSixPillarsTheEnchanterDoesNotStart(GameTestHelper test) {
        EnchantmentsRuntimeChecks.withoutSixPillarsTheEnchanterDoesNotStart(test);
    }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE, timeoutTicks = 300)
    public void theEnchanterEnchantsAToolFromAWandBetweenSixPillars(GameTestHelper test) {
        EnchantmentsRuntimeChecks.theEnchanterEnchantsAToolFromAWandBetweenSixPillars(test);
    }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE)
    public void vampirismHealsTheAttacker(GameTestHelper test) {
        EnchantmentsRuntimeChecks.vampirismHealsTheAttacker(test);
    }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE)
    public void valianceMultipliesTheWholeAttackAtLowHealth(GameTestHelper test) {
        EnchantmentsRuntimeChecks.valianceMultipliesTheWholeAttackAtLowHealth(test);
    }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE)
    public void theAttackEventArmsTheAttackOfAPlayer(GameTestHelper test) {
        EnchantmentsRuntimeChecks.theAttackEventArmsTheAttackOfAPlayer(test);
    }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE)
    public void theStrikeCounterIsKeptOnTheWeapon(GameTestHelper test) {
        EnchantmentsRuntimeChecks.theStrikeCounterIsKeptOnTheWeapon(test);
    }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE)
    public void pounceDependsOnTheBlockUnderTheFeet(GameTestHelper test) {
        EnchantmentsRuntimeChecks.pounceDependsOnTheBlockUnderTheFeet(test);
    }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE)
    public void desintegrateAndFlamingTouchChangeTheDigSpeedOfTheBlockUnderTheCrosshair(GameTestHelper test) {
        EnchantmentsRuntimeChecks.desintegrateAndFlamingTouchChangeTheDigSpeedOfTheBlockUnderTheCrosshair(test);
    }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE)
    public void shatterAndTunnelScaleTheDigSpeed(GameTestHelper test) {
        EnchantmentsRuntimeChecks.shatterAndTunnelScaleTheDigSpeed(test);
    }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE)
    public void aToolOfDisintegrateRefusesTheOtherBlocksAndWearsDoubleOnItsOwn(GameTestHelper test) {
        EnchantmentsRuntimeChecks.aToolOfDisintegrateRefusesTheOtherBlocksAndWearsDoubleOnItsOwn(test);
    }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE)
    public void slowFallShortensTheFallOnTheServer(GameTestHelper test) {
        EnchantmentsRuntimeChecks.slowFallShortensTheFallOnTheServer(test);
    }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE)
    public void shockwaveHurtsTheCreaturesAroundALanding(GameTestHelper test) {
        EnchantmentsRuntimeChecks.shockwaveHurtsTheCreaturesAroundALanding(test);
    }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE)
    public void quickDrawGivesTheArrowTheSpeedOfALongerDraw(GameTestHelper test) {
        EnchantmentsRuntimeChecks.quickDrawGivesTheArrowTheSpeedOfALongerDraw(test);
    }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE)
    public void everyHandlerIsRegisteredOnce(GameTestHelper test) {
        EnchantmentsRuntimeChecks.everyHandlerIsRegisteredOnce(test);
    }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE)
    public void theTableKnowsOnlyEnchantmentsOfTheWorld(GameTestHelper test) {
        EnchantmentsRuntimeChecks.theTableKnowsOnlyEnchantmentsOfTheWorld(test);
    }
}
