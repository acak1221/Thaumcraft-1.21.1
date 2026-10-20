package thaumcraft.common.entities.ai.combat;

import java.util.EnumSet;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal;
import thaumcraft.common.entities.golems.EntityGolemBase;
import thaumcraft.common.lib.golems.GolemAiRules;

/**
 * Original {@code AIHurtByTarget} (TC4 4.2.3.5, {@code thaumcraft/common/entities/ai/combat/AIHurtByTarget.java}).
 *
 * <p>The golem answers whoever last hurt it, as long as the attacker is a target the golem may attack at all. With the
 * original's "call for help" switch the golem also hands the very same attacker to every golem of its own kind within
 * sixteen blocks that has no target of its own.</p>
 *
 * <p>The original registered this goal in the target selector with priority 1.</p>
 */
public final class GolemHurtByTarget extends Goal {
    /** The original 1.7 target-selector priority of {@code AIHurtByTarget}. */
    public static final int PRIORITY = 1;
    /** The original target distance of {@code AIHurtByTarget}: sixteen blocks. */
    public static final double TARGET_DISTANCE = 16.0;

    private final EntityGolemBase golem;
    private final boolean callForHelp;
    private LivingEntity attacker;

    public GolemHurtByTarget(EntityGolemBase golem, boolean callForHelp) {
        this.golem = golem;
        this.callForHelp = callForHelp;
        setFlags(EnumSet.of(Flag.TARGET));
    }

    @Override public boolean canUse() {
        LivingEntity candidate = golem.getLastHurtByMob();
        if (candidate == null || !candidate.isAlive()) return false;
        if (!GolemAiRules.isValidTarget(golem, candidate)) return false;
        attacker = candidate;
        return true;
    }

    @Override public boolean canContinueToUse() {
        return attacker != null && golem.getTarget() == attacker && attacker.isAlive()
                && GolemAiRules.isValidTarget(golem, attacker);
    }

    @Override public void start() {
        LivingEntity candidate = attacker;
        if (candidate == null || !GolemAiRules.isValidTarget(golem, candidate)) return;
        golem.setTarget(candidate);
        if (!callForHelp) return;
        // Original: every golem of the same kind within sixteen blocks that has no target of its own joins the hunt.
        for (EntityGolemBase helper : golem.level().getEntitiesOfClass(EntityGolemBase.class,
                golem.getBoundingBox().inflate(TARGET_DISTANCE, TARGET_DISTANCE, TARGET_DISTANCE))) {
            if (helper == golem || helper.getTarget() != null || !helper.isAlive()) continue;
            if (GolemAiRules.isValidTarget(helper, candidate)) helper.setTarget(candidate);
        }
    }

    @Override public void stop() {
        if (attacker != null && golem.getTarget() == attacker) golem.setTarget(null);
        attacker = null;
    }
}
