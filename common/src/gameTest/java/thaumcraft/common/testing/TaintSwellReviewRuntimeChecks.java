package thaumcraft.common.testing;

import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.phys.Vec3;
import thaumcraft.common.entities.ai.combat.TaintCreeperSwell;
import thaumcraft.common.entities.monster.EntityTaintCreeper;

/** Actual native goal decisions at the original three/seven-block distance boundaries. */
public final class TaintSwellReviewRuntimeChecks {
    private TaintSwellReviewRuntimeChecks() {}

    public static void swellUsesSquaredDistanceAndReleasesItsMoveGoal(GameTestHelper test) {
        try (var f = new TubeVariantsRuntimeChecks.Fixture(test)) {
            f.open();
            var creeper = new EntityTaintCreeper(f.level);
            try {
                creeper.setPos(Vec3.atBottomCenterOf(f.base));
                creeper.setTarget(f.player);
                var goal = new TaintCreeperSwell(creeper);
                f.player.setPos(creeper.position().add(4, 0, 0));
                test.assertTrue(!goal.canUse(), "four-block target is outside the original three-block swell trigger");
                f.player.setPos(creeper.position().add(2, 0, 0));
                test.assertTrue(goal.canUse(), "two-block target still triggers the original swell");
                goal.start();
                creeper.setCreeperState(1);
                f.player.setPos(creeper.position().add(8, 0, 0));
                goal.tick();
                test.assertTrue(creeper.getCreeperState() == -1,
                        "eight-block target exceeds the original seven-block cancellation boundary");
                test.assertTrue(!goal.canContinueToUse(),
                        "a canceled swell releases MOVE instead of permanently keeping the fleeing target latched");
                goal.stop();
                f.player.setPos(creeper.position().add(2, 0, 0));
                test.assertTrue(goal.canUse(), "the stopped native goal may start again when the target returns");
            } finally { creeper.discard(); }
        }
        test.succeed();
    }
}
