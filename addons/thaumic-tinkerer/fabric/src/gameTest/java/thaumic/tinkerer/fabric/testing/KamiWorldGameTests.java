/**
 * New in the 1.21.1 port of Thaumic Tinkerer (no 1.7.10 counterpart): the Fabric GameTest wrappers of the KamiWorld checks. Distributed under the same licence as the add-on
 * (CC BY-NC-SA 3.0, see addons/thaumic-tinkerer/NOTICE.md).
 */
package thaumic.tinkerer.fabric.testing;

import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import thaumic.tinkerer.testing.KamiWorldRuntimeChecks;

/** Fabric wrappers of {@link KamiWorldRuntimeChecks}: one per check. Test-only, not distributed. */
public final class KamiWorldGameTests implements FabricGameTest {
    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE)
    public void registrationIsComplete(GameTestHelper test) {
        KamiWorldRuntimeChecks.registrationIsComplete(test);
    }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE)
    public void bedrockDimensionFileDecodesAndIsTheLevelOfTheTestWorld(GameTestHelper test) {
        KamiWorldRuntimeChecks.bedrockDimensionFileDecodesAndIsTheLevelOfTheTestWorld(test);
    }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE, timeoutTicks = 600)
    public void bedrockDimensionIsSolidBedrockWithOreVeins(GameTestHelper test) {
        KamiWorldRuntimeChecks.bedrockDimensionIsSolidBedrockWithOreVeins(test);
    }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE, timeoutTicks = 1200)
    public void bedrockOreConfigDecidesDensityAndBlacklist(GameTestHelper test) {
        KamiWorldRuntimeChecks.bedrockOreConfigDecidesDensityAndBlacklist(test);
    }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE, timeoutTicks = 400)
    public void portalDestinationsFollowTheRules(GameTestHelper test) {
        KamiWorldRuntimeChecks.portalDestinationsFollowTheRules(test);
    }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE, timeoutTicks = 200)
    public void portalLeavesNonPlayersAlone(GameTestHelper test) {
        KamiWorldRuntimeChecks.portalLeavesNonPlayersAlone(test);
    }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE)
    public void warpGateKeepsItsFlagAndPearlsThroughSaving(GameTestHelper test) {
        KamiWorldRuntimeChecks.warpGateKeepsItsFlagAndPearlsThroughSaving(test);
    }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE)
    public void warpGateTeleportsToAnotherGateAndRefusesWhatItShould(GameTestHelper test) {
        KamiWorldRuntimeChecks.warpGateTeleportsToAnotherGateAndRefusesWhatItShould(test);
    }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE, timeoutTicks = 400)
    public void warpGateTakesAPearlOfAnotherDimension(GameTestHelper test) {
        KamiWorldRuntimeChecks.warpGateTakesAPearlOfAnotherDimension(test);
    }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE)
    public void warpGateMenuMovesOnlyPearls(GameTestHelper test) {
        KamiWorldRuntimeChecks.warpGateMenuMovesOnlyPearls(test);
    }

    /** Waits out the 60 ticks of spawn protection of the mock player before it is hurt. */
    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE, timeoutTicks = 200)
    public void soulHeartsAbsorbDamageBeforeHealth(GameTestHelper test) {
        KamiWorldRuntimeChecks.soulHeartsAbsorbDamageBeforeHealth(test);
    }

    /** Hits 11 ticks apart behind the 65 ticks of spawn protection, with the invulnerability frames of the original in between. */
    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE, timeoutTicks = 200)
    public void soulHeartsKeepTheFramesOfTheOriginal(GameTestHelper test) {
        KamiWorldRuntimeChecks.soulHeartsKeepTheFramesOfTheOriginal(test);
    }

    /** Hits at b, b+3, b+10, b+14 behind the 65 ticks of spawn protection: the game's frames and the pool's begin and end at different times. */
    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE, timeoutTicks = 200)
    public void soulHeartsAgreeWithTheOriginalWhenTheGamesFramesBeginLater(GameTestHelper test) {
        KamiWorldRuntimeChecks.soulHeartsAgreeWithTheOriginalWhenTheGamesFramesBeginLater(test);
    }

    /** Two mock players in pits of lava, ticked every tick, for about 100 ticks. */
    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE, timeoutTicks = 200)
    public void soulHeartsAreChargedOncePerTenTicksByLava(GameTestHelper test) {
        KamiWorldRuntimeChecks.soulHeartsAreChargedOncePerTenTicksByLava(test);
    }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE)
    public void soulHeartsExactRuleCutsTheFractionOfEveryHit(GameTestHelper test) {
        KamiWorldRuntimeChecks.soulHeartsExactRuleCutsTheFractionOfEveryHit(test);
    }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE)
    public void soulHeartsAreSavedPerPlayerAndNotShared(GameTestHelper test) {
        KamiWorldRuntimeChecks.soulHeartsAreSavedPerPlayerAndNotShared(test);
    }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE, timeoutTicks = 400)
    public void shardDropsFollowTheConditionsOfTheOriginal(GameTestHelper test) {
        KamiWorldRuntimeChecks.shardDropsFollowTheConditionsOfTheOriginal(test);
    }

    /** About 12,800 real deaths of Endermen and Piglins in one tick. */
    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE, timeoutTicks = 400)
    public void shardDropsComeFromRealDeathsWithTheChanceOfTheOriginal(GameTestHelper test) {
        KamiWorldRuntimeChecks.shardDropsComeFromRealDeathsWithTheChanceOfTheOriginal(test);
    }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE)
    public void kamiUnlockedCommandAnswersNoUntilEverythingIsResearched(GameTestHelper test) {
        KamiWorldRuntimeChecks.kamiUnlockedCommandAnswersNoUntilEverythingIsResearched(test);
    }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE)
    public void groupHadNoRegistrationFailures(GameTestHelper test) {
        KamiWorldRuntimeChecks.groupHadNoRegistrationFailures(test);
    }
}
