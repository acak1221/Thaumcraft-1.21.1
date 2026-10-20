package thaumcraft.common.entities.ai.combat;

import java.util.EnumSet;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.monster.Creeper;
import thaumcraft.common.lib.golems.GolemAiRules;

/**
 * Original {@code AICreeperSwell} (TC4 4.2.3.5, {@code thaumcraft/common/entities/ai/combat/AICreeperSwell.java}).
 *
 * <p>The taint creeper starts swelling as soon as a target is nearer than three blocks, keeps the swell while the target
 * stays within seven blocks and in sight, and gives it up otherwise. The goal also stops the creeper's path, so the
 * creeper stands still while it primes its charge.</p>
 *
 * <p>Documented deviation: the oracle's {@code shouldContinueExecuting} returns {@code false} (oracle line 52), which in
 * 1.7 would stop the goal before its own {@code updateTask} ever ran; the audited 1.12 reference of the same goal
 * ({@code src/main/java/thaumcraft/common/entities/ai/combat/AICreeperSwell.java}) does not override that method at all.
 * The port keeps the goal alive so that the swell state machine below, which is the behaviour the original clearly wanted,
 * actually runs.</p>
 *
 * <p>The original taint creeper ({@code EntityTaintCreeper}) has no native counterpart in this port, so the goal drives the
 * ordinary native creeper, whose swell state has the same three values the original used.</p>
 */
public final class GolemCreeperSwell extends Goal {
    /** The original 1.7 goal priority of {@code AICreeperSwell}. */
    public static final int PRIORITY = 2;

    private final Creeper creeper;
    private LivingEntity attackTarget;

    public GolemCreeperSwell(Creeper creeper) {
        this.creeper = creeper;
        setFlags(EnumSet.of(Flag.MOVE));
    }

    @Override public boolean requiresUpdateEveryTick() { return true; }

    @Override public boolean canUse() {
        LivingEntity target = creeper.getTarget();
        return creeper.getSwellDir() > 0 || target != null && creeper.distanceToSqr(target) < GolemAiRules.CREEPER_SWELL_RANGE;
    }

    @Override public boolean canContinueToUse() { return attackTarget != null || creeper.getSwellDir() > 0; }

    @Override public void start() {
        creeper.getNavigation().stop();
        attackTarget = creeper.getTarget();
    }

    @Override public void stop() {
        attackTarget = null;
    }

    @Override public void tick() {
        if (attackTarget == null) {
            creeper.setSwellDir(-1);
        } else if (creeper.distanceToSqr(attackTarget) > GolemAiRules.CREEPER_SWELL_CANCEL_RANGE_SQ) {
            creeper.setSwellDir(-1);
        } else if (!creeper.getSensing().hasLineOfSight(attackTarget)) {
            creeper.setSwellDir(-1);
        } else {
            creeper.setSwellDir(1);
        }
    }
}
