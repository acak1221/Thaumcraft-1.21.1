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

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE)
    public void soulHeartsAbsorbDamageBeforeHealth(GameTestHelper test) {
        KamiWorldRuntimeChecks.soulHeartsAbsorbDamageBeforeHealth(test);
    }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE)
    public void soulHeartsAreSavedPerPlayerAndNotShared(GameTestHelper test) {
        KamiWorldRuntimeChecks.soulHeartsAreSavedPerPlayerAndNotShared(test);
    }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE)
    public void shardDropPoolHasItsThreeConditions(GameTestHelper test) {
        KamiWorldRuntimeChecks.shardDropPoolHasItsThreeConditions(test);
    }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE, timeoutTicks = 400)
    public void shardDropsAreAddedToTheEntityLootTables(GameTestHelper test) {
        KamiWorldRuntimeChecks.shardDropsAreAddedToTheEntityLootTables(test);
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
