package thaumcraft.common.entities.ai.combat;

import java.util.EnumSet;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.level.pathfinder.Path;
import thaumcraft.common.entities.golems.EntityGolemBase;
import thaumcraft.common.lib.golems.GolemAiRules;

/**
 * Original {@code AIGolemAttackOnCollide} (TC4 4.2.3.5, {@code thaumcraft/common/entities/ai/combat/AIGolemAttackOnCollide.java}).
 *
 * <p>The golem chases the target it was given, refreshes the path every four to ten ticks while it can see the target, and
 * swings every {@code getAttackSpeed()} ticks as soon as the target is inside the legacy reach, which is the target's own
 * width multiplied by four and squared, plus one. A target that is no longer valid is forgotten, exactly as the original did.</p>
 *
 * <p>The original registered this goal with priority 3. The 1.7 selector gave the <em>lowest</em> number the highest
 * precedence, the 1.21.1 selector gives the <em>highest</em> number the highest precedence, so a native registration has to
 * invert the number.</p>
 */
public final class GolemAttackOnCollide extends Goal {
    /** The original 1.7 goal priority of {@code AIGolemAttackOnCollide}. */
    public static final int PRIORITY = 3;

    private final EntityGolemBase golem;
    private LivingEntity target;
    private Path path;
    private int attackTick;
    private int counter;

    public GolemAttackOnCollide(EntityGolemBase golem) {
        this.golem = golem;
        setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
    }

    private double speed() { return golem.getAttributeValue(Attributes.MOVEMENT_SPEED); }

    @Override public boolean requiresUpdateEveryTick() { return true; }

    @Override public boolean canUse() {
        LivingEntity candidate = golem.getTarget();
        if (candidate == null) return false;
        if (!GolemAiRules.isValidTarget(golem, candidate)) {
            golem.setTarget(null); // Original: an invalid target is forgotten at once.
            return false;
        }
        Path created = golem.getNavigation().createPath(candidate, 0);
        if (created == null) return false; // Original: without a path the goal does not start.
        target = candidate;
        path = created;
        return true;
    }

    @Override public boolean canContinueToUse() {
        LivingEntity current = target;
        return current != null && golem.getTarget() == current && GolemAiRules.isValidTarget(golem, current)
                && !golem.getNavigation().isDone();
    }

    @Override public void start() {
        golem.getNavigation().moveTo(path, speed());
        counter = 0;
    }

    @Override public void stop() {
        target = null;
        path = null;
        golem.getNavigation().stop();
    }

    @Override public void tick() {
        LivingEntity current = target;
        if (current == null || golem.getTarget() != current) return;
        golem.getLookControl().setLookAt(current, 30, 30);
        if (golem.getSensing().hasLineOfSight(current) && --counter <= 0) {
            counter = GolemAiRules.REPATH_BASE + golem.getRandom().nextInt(GolemAiRules.REPATH_SPAN);
            golem.getNavigation().moveTo(current, speed());
        }
        if (attackTick > 0) --attackTick;
        double reach = GolemAiRules.meleeReach(current.getBbWidth(), 1.0F);
        double distance = golem.distanceToSqr(current.getX(), current.getBoundingBox().minY, current.getZ());
        if (distance <= reach && attackTick <= 0) {
            attackTick = golem.getAttackSpeed();
            golem.doHurtTarget(current); // The native golem owns the swing, the damage and the action timer.
        }
    }
}
