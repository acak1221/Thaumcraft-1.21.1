package thaumcraft.eldritch;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import java.util.List;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.levelgen.LegacyRandomSource;
import net.minecraft.world.phys.Vec3;
import org.junit.jupiter.api.Test;
import thaumcraft.common.lib.utils.Utils;
import thaumcraft.common.lib.world.EldritchMobRules;

/**
 * The original TC4 eldritch golem lightning arc, checked against the numbers of
 * the oracle {@code thaumcraft/client/fx/beams/FXArc.java:37-73}.
 */
class EldritchArcTest {
    private static final double EPS = 1.0E-9;

    @Test void arcStartsAtTheOriginAndEndsAtTheTarget() {
        Vec3 target = new Vec3(3.5, -1.0, 2.0);
        List<Vec3> points = EldritchMobRules.golemArcPoints(target, 0.2, new LegacyRandomSource(7L));
        assertEquals(Vec3.ZERO, points.get(0), "the arc starts at its origin");
        assertEquals(target, points.get(points.size() - 1), "the arc ends at its target");
        assertTrue(points.size() >= 2, "the arc has at least the origin and the target");
    }

    @Test void arcWalksTheCleanParabolaAndOnlyTheEmittedPointIsNoisy() {
        Vec3 target = new Vec3(3.5, -1.0, 2.0);
        double heightGain = 0.2;
        List<Vec3> points = EldritchMobRules.golemArcPoints(target, heightGain, new LegacyRandomSource(11L));
        Vec3 velocity = Utils.calculateVelocity(Vec3.ZERO, target, heightGain, EldritchMobRules.GOLEM_ARC_GRAVITY);
        double limit = Utils.distanceSquared3d(Vec3.ZERO, velocity);
        Vec3 current = Vec3.ZERO;
        int walked = 0;
        for (int c = 1; c < points.size() - 1; ++c) {
            Vec3 clean = current.add(velocity);
            current = clean;
            double offset = points.get(c).distanceTo(clean);
            assertTrue(offset <= EldritchMobRules.GOLEM_ARC_NOISE * Math.sqrt(3.0) + EPS,
                    "the emitted point is displaced by the noise only, offset " + offset);
            ++walked;
            velocity = new Vec3(velocity.x, velocity.y - EldritchMobRules.GOLEM_ARC_GRAVITY / 1.9, velocity.z);
            if (Utils.distanceSquared3d(target, current) <= limit) break;
        }
        assertEquals(walked, points.size() - 2, "the walk stops when the target is within the launch velocity");
    }

    @Test void arcNeverWalksMoreThanFiftySteps() {
        List<Vec3> points = EldritchMobRules.golemArcPoints(new Vec3(200.0, 40.0, 0.0), 0.2, new LegacyRandomSource(3L));
        assertTrue(points.size() <= EldritchMobRules.GOLEM_ARC_STEPS + 2,
                "at most fifty walked points plus the origin and the target, got " + points.size());
    }

    @Test void arcIsDeterministicForTheSameNoise() {
        Vec3 target = new Vec3(2.5, 1.5, -1.0);
        assertEquals(EldritchMobRules.golemArcPoints(target, 0.1, new LegacyRandomSource(5L)),
                EldritchMobRules.golemArcPoints(target, 0.1, new LegacyRandomSource(5L)));
    }

    @Test void arcNumbersAreTheOriginalOnes() {
        assertEquals(0.115, EldritchMobRules.GOLEM_ARC_GRAVITY, EPS, "original FXArc:54");
        assertEquals(0.25, EldritchMobRules.GOLEM_ARC_NOISE, EPS, "original FXArc:55");
        assertEquals(50, EldritchMobRules.GOLEM_ARC_STEPS, "original FXArc:63");
    }
}
