package thaumcraft.common.entities.ai.combat;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.List;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal;
import thaumcraft.common.entities.golems.EntityGolemBase;
import thaumcraft.common.lib.golems.GolemAiRules;

/**
 * Original {@code AINearestButcherTarget} (TC4 4.2.3.5,
 * {@code thaumcraft/common/entities/ai/combat/AINearestButcherTarget.java}) with its
 * {@code AIOldestAttackableTargetSorter}.
 *
 * <p>Despite its name the butcher goal does not look for the nearest animal: it sorts the candidates by age, oldest first,
 * and then only accepts one whose herd holds more than two animals of exactly that kind. A young animal is therefore never
 * taken while an old one of the same kind survives, and a lone animal is left alone.</p>
 *
 * <p>The original registered this goal in the target selector with priority 1.</p>
 */
public final class GolemNearestButcherTarget extends Goal {
    /** The original 1.7 target-selector priority of {@code AINearestButcherTarget}. */
    public static final int PRIORITY = 1;

    private final EntityGolemBase golem;
    private final int targetChance;
    private LivingEntity target;

    public GolemNearestButcherTarget(EntityGolemBase golem, int targetChance) {
        this.golem = golem;
        this.targetChance = targetChance;
        setFlags(EnumSet.of(Flag.TARGET));
    }

    @Override public boolean requiresUpdateEveryTick() { return true; }

    @Override public boolean canUse() {
        float distance = golem.getRange();
        if (targetChance > 0 && !GolemAiRules.targetChancePassed(targetChance, golem.getRandom().nextInt(targetChance))) return false;
        List<LivingEntity> candidates = new ArrayList<>(golem.level().getEntitiesOfClass(LivingEntity.class,
                golem.getBoundingBox().inflate(distance, 4.0F, distance)));
        // Original AIOldestAttackableTargetSorter: the oldest animal first, the query order breaks a tie.
        candidates.sort(Comparator.comparingInt((LivingEntity animal) -> animal.tickCount).reversed());
        for (LivingEntity candidate : candidates) {
            if (!GolemAiRules.isValidTarget(golem, candidate)) continue;
            int herd = 0;
            for (LivingEntity member : golem.level().getEntitiesOfClass(LivingEntity.class,
                    golem.getBoundingBox().inflate(distance, 4.0F, distance),
                    member -> member.getClass() == candidate.getClass())) {
                if (GolemAiRules.isValidTarget(golem, member)) ++herd;
            }
            if (!GolemAiRules.butcherPopulationSufficient(herd)) continue;
            target = candidate;
            return true;
        }
        return false;
    }

    @Override public boolean canContinueToUse() {
        LivingEntity current = target;
        return current != null && current.isAlive() && golem.getTarget() == current
                && GolemAiRules.isValidTarget(golem, current);
    }

    @Override public void start() {
        LivingEntity current = target;
        if (current != null && GolemAiRules.isValidTarget(golem, current)) golem.setTarget(current);
    }

    @Override public void stop() {
        if (target != null && golem.getTarget() == target) golem.setTarget(null);
        target = null;
    }
}
