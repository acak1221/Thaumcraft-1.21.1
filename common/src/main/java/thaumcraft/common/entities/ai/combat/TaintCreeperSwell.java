package thaumcraft.common.entities.ai.combat;

import java.util.EnumSet;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal;
import thaumcraft.common.entities.monster.EntityTaintCreeper;

/**
 * Original {@code thaumcraft/common/entities/ai/combat/AICreeperSwell.java}.
 *
 * <p>The taint creeper starts swelling as soon as its target is nearer than three blocks, keeps the swell while the
 * target stays within seven blocks and in sight, and gives it up otherwise. The goal also stops the path of
 * the creeper, so the creeper stands still while it primes its charge.</p>
 *
 * <p>The original registered this goal with priority 1 and used the goal selector of 1.7.10, where the LOWEST
 * number had the highest precedence. 1.21.1 keeps the registration order of the goals, so the native registration
 * uses the original numbers and the original order.</p>
 */
public final class TaintCreeperSwell extends Goal {
    /** The original 1.7 goal priority of {@code AICreeperSwell}. */
    public static final int PRIORITY = 1;

    /** Original {@code func_75250_a}: squared distance below nine, or three blocks. */
    public static final double SWELL_RANGE = 9.0D;
    /** Original {@code func_75246_d}: squared distance beyond forty nine, or seven blocks. */
    public static final double SWELL_CANCEL_RANGE = 49.0D;

    private final EntityTaintCreeper creeper;
    private LivingEntity attackTarget;

    public TaintCreeperSwell(EntityTaintCreeper creeper) {
        this.creeper = creeper;
        setFlags(EnumSet.of(Flag.MOVE));
    }

    @Override public boolean requiresUpdateEveryTick() { return true; }

    @Override public boolean canUse() {
        LivingEntity target = creeper.getTarget();
        return creeper.getCreeperState() > 0 || target != null && creeper.distanceToSqr(target) < SWELL_RANGE;
    }

    @Override public boolean canContinueToUse() { return canUse(); }

    @Override public void start() {
        creeper.getNavigation().stop();
        attackTarget = creeper.getTarget();
    }

    @Override public void stop() {
        attackTarget = null;
    }

    /** Original {@code func_75246_d}: the swell state of the taint creeper. */
    @Override public void tick() {
        if (attackTarget == null) {
            creeper.setCreeperState(-1);
        } else if (creeper.distanceToSqr(attackTarget) > SWELL_CANCEL_RANGE) {
            creeper.setCreeperState(-1);
        } else if (!creeper.getSensing().hasLineOfSight(attackTarget)) {
            creeper.setCreeperState(-1);
        } else {
            creeper.setCreeperState(1);
        }
    }
}
