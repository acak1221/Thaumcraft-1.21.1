package thaumic.tinkerer.fabric.testing;

import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import thaumic.tinkerer.testing.HooksRuntimeChecks;

/** Fabric wrappers of {@link HooksRuntimeChecks}: one per check. Test-only, not distributed. */
public final class HooksGameTests implements FabricGameTest {
    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE)
    public void livingDamageIsPostedBeforeArmourAndChainsInOrder(GameTestHelper test) {
        HooksRuntimeChecks.livingDamageIsPostedBeforeArmourAndChainsInOrder(test);
    }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE)
    public void livingDamageCancelIsLikeInvulnerableAndFramesFollowTheListener(GameTestHelper test) {
        HooksRuntimeChecks.livingDamageCancelIsLikeInvulnerableAndFramesFollowTheListener(test);
    }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE)
    public void livingDamageIsNotPostedForHitsTheInvulnerabilityFramesRefuse(GameTestHelper test) {
        HooksRuntimeChecks.livingDamageIsNotPostedForHitsTheInvulnerabilityFramesRefuse(test);
    }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE)
    public void livingDamageSeesAFakePlayersBlowAndAZombiesBlowOnAPlayer(GameTestHelper test) {
        HooksRuntimeChecks.livingDamageSeesAFakePlayersBlowAndAZombiesBlowOnAPlayer(test);
    }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE)
    public void livingDamageSeesAnArrow(GameTestHelper test) {
        HooksRuntimeChecks.livingDamageSeesAnArrow(test);
    }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE)
    public void breakSpeedIsPostedForAServerPlayerAndAFakePlayer(GameTestHelper test) {
        HooksRuntimeChecks.breakSpeedIsPostedForAServerPlayerAndAFakePlayer(test);
    }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE)
    public void livingJumpIsPostedAfterTheJumpMotionIsApplied(GameTestHelper test) {
        HooksRuntimeChecks.livingJumpIsPostedAfterTheJumpMotionIsApplied(test);
    }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE, timeoutTicks = 200)
    public void livingFallIsPostedOnLandingAndCanHalveOrCancel(GameTestHelper test) {
        HooksRuntimeChecks.livingFallIsPostedOnLandingAndCanHalveOrCancel(test);
    }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE)
    public void useItemTickExtendsABowDraw(GameTestHelper test) {
        HooksRuntimeChecks.useItemTickExtendsABowDraw(test);
    }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE)
    public void livingDropsAreEditedBeforeTheyEnterTheWorld(GameTestHelper test) {
        HooksRuntimeChecks.livingDropsAreEditedBeforeTheyEnterTheWorld(test);
    }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE)
    public void livingDropsCoverAPlayersInventoryButNotADropByHand(GameTestHelper test) {
        HooksRuntimeChecks.livingDropsCoverAPlayersInventoryButNotADropByHand(test);
    }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE)
    public void floatingTicksReadAndResetTheRealCounter(GameTestHelper test) {
        HooksRuntimeChecks.floatingTicksReadAndResetTheRealCounter(test);
    }
}
