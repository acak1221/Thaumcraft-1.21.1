package thaumcraft.common.entities.ai.combat;

import java.util.EnumSet;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal;
import thaumcraft.common.entities.golems.EntityGolemBase;
import thaumcraft.common.lib.golems.GolemAiRules;

/**
 * Original {@code AINearestAttackableTarget} (TC4 4.2.3.5,
 * {@code thaumcraft/common/entities/ai/combat/AINearestAttackableTarget.java}) with its
 * {@code AINearestAttackableTargetSorter}.
 *
 * <p>The golem looks for the nearest living entity inside its own range that it is allowed to attack, and takes it as its
 * target. The search box is the golem's own bounding box widened by its range sideways and by four blocks upwards, and the
 * sorter is a plain distance comparison, so the nearest valid target wins and the first of two equidistant ones keeps the
 * order of the query.</p>
 *
 * <p>The original registered this goal in the target selector with priority 2.</p>
 */
public final class GolemNearestAttackableTarget extends Goal {
    /** The original 1.7 target-selector priority of {@code AINearestAttackableTarget}. */
    public static final int PRIORITY = 2;
    /** The original {@code AITarget}: a target that stays unseen for more than sixty ticks is given up. */
    public static final int UNSEEN_GRACE = 60;

    private final EntityGolemBase golem;
    private final int targetChance;
    private LivingEntity target;
    private int unseen;

    public GolemNearestAttackableTarget(EntityGolemBase golem, int targetChance) {
        this.golem = golem;
        this.targetChance = targetChance;
        setFlags(EnumSet.of(Flag.TARGET));
    }

    @Override public boolean requiresUpdateEveryTick() { return true; }

    @Override public boolean canUse() {
        float distance = golem.getRange();
        if (targetChance > 0 && !GolemAiRules.targetChancePassed(targetChance, golem.getRandom().nextInt(targetChance))) return false;
        double nearest = Double.MAX_VALUE;
        LivingEntity chosen = null;
        for (LivingEntity candidate : golem.level().getEntitiesOfClass(LivingEntity.class,
                golem.getBoundingBox().inflate(distance, 4.0F, distance))) {
            if (!GolemAiRules.isValidTarget(golem, candidate)) continue;
            double candidateDistance = golem.distanceToSqr(candidate);
            if (GolemAiRules.compareDistanceSq(candidateDistance, nearest) < 0) {
                nearest = candidateDistance;
                chosen = candidate;
            }
        }
        if (chosen == null) return false;
        target = chosen;
        return true;
    }

    @Override public boolean canContinueToUse() {
        LivingEntity current = target;
        if (current == null || !current.isAlive()) return false;
        if (golem.getTarget() != current) return false;
        if (golem.distanceToSqr(current) > golem.getRange() * golem.getRange()) return false;
        if (golem.getSensing().hasLineOfSight(current)) unseen = 0;
        else if (++unseen > UNSEEN_GRACE) return false;
        return true;
    }

    @Override public void start() {
        unseen = 0;
        LivingEntity current = target;
        if (current != null && GolemAiRules.isValidTarget(golem, current)) golem.setTarget(current);
    }

    @Override public void tick() {
        LivingEntity current = target;
        if (current == null) return;
        if (golem.getSensing().hasLineOfSight(current)) unseen = 0;
        else ++unseen;
    }

    @Override public void stop() {
        if (target != null && golem.getTarget() == target) golem.setTarget(null);
        target = null;
        unseen = 0;
    }
}
