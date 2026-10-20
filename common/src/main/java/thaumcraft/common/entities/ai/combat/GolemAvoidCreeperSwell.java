package thaumcraft.common.entities.ai.combat;

import java.util.EnumSet;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.util.DefaultRandomPos;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.level.pathfinder.Path;
import net.minecraft.world.phys.Vec3;
import thaumcraft.common.entities.golems.EntityGolemBase;
import thaumcraft.common.lib.golems.GolemAiRules;

/**
 * Original {@code AIAvoidCreeperSwell} (TC4 4.2.3.5, {@code thaumcraft/common/entities/ai/combat/AIAvoidCreeperSwell.java}).
 *
 * <p>A golem that sees a swelling creeper within five blocks flees: it looks for a spot sixteen blocks wide and seven high
 * away from the creeper, refuses a spot that is nearer to the creeper than the golem itself is, and walks there at 1.25 times
 * its own speed while the creeper is within seven blocks and at 1.125 times otherwise.</p>
 *
 * <p>The original registered this goal with priority 0, that is above every other golem goal.</p>
 */
public final class GolemAvoidCreeperSwell extends Goal {
    /** The original 1.7 goal priority of {@code AIAvoidCreeperSwell}. */
    public static final int PRIORITY = 0;

    private final EntityGolemBase golem;
    private Creeper threat;
    private Vec3 escape;
    private Path path;
    private float farSpeed;
    private float nearSpeed;

    public GolemAvoidCreeperSwell(EntityGolemBase golem) {
        this.golem = golem;
        setFlags(EnumSet.of(Flag.MOVE));
    }

    @Override public boolean requiresUpdateEveryTick() { return true; }

    @Override public boolean canUse() {
        if (farSpeed == 0.0F) {
            float speed = (float) golem.getAttributeValue(Attributes.MOVEMENT_SPEED);
            farSpeed = GolemAiRules.avoidCreeperFarSpeed(speed);
            nearSpeed = GolemAiRules.avoidCreeperNearSpeed(speed);
        }
        threat = null;
        for (Creeper creeper : golem.level().getEntitiesOfClass(Creeper.class,
                golem.getBoundingBox().inflate(GolemAiRules.CREEPER_AVOID_RANGE, GolemAiRules.CREEPER_AVOID_HEIGHT,
                        GolemAiRules.CREEPER_AVOID_RANGE))) {
            // Original: only a creeper that is actually swelling (state 1) and that the golem can see.
            if (creeper.getSwellDir() != 1 || !golem.getSensing().hasLineOfSight(creeper)) continue;
            threat = creeper;
            break;
        }
        if (threat == null) return false;
        escape = DefaultRandomPos.getPosAway(golem, 16, 7, threat.position());
        if (escape == null) return false;
        if (threat.distanceToSqr(escape) < threat.distanceToSqr(golem)) return false;
        path = golem.getNavigation().createPath(escape.x, escape.y, escape.z, 0);
        if (path == null) return false;
        return path.canReach();
    }

    @Override public boolean canContinueToUse() { return !golem.getNavigation().isDone(); }

    @Override public void start() {
        if (escape == null || path == null) return;
        double dx = escape.x + 0.5 - golem.getX();
        double dz = escape.z + 0.5 - golem.getZ();
        float length = (float) Math.sqrt(dx * dx + dz * dz);
        Vec3 motion = golem.getDeltaMovement();
        // The original divides by the length unguarded; a zero length would poison the motion with infinities.
        if (length > 0.0F) golem.setDeltaMovement(motion.x + dx / length * 0.8 + motion.x * 0.2, 0.3,
                motion.z + dz / length * 0.8 + motion.z * 0.2);
        golem.getNavigation().moveTo(path, nearSpeed);
    }

    @Override public void stop() {
        threat = null;
    }

    @Override public void tick() {
        if (threat == null) return;
        golem.getNavigation().setSpeedModifier(GolemAiRules.creeperIsNear(golem.distanceToSqr(threat)) ? nearSpeed : farSpeed);
    }
}
