package thaumcraft.common.entities.ai.combat;

import java.util.EnumSet;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.Goal;
import thaumcraft.common.entities.golems.EntityGolemBase;
import thaumcraft.common.entities.projectile.EntityDart;
import thaumcraft.common.items.golems.GolemDartRules;
import thaumcraft.common.lib.golems.GolemAiRules;

/**
 * Original {@code AIDartAttack} (TC4 4.2.3.5, {@code thaumcraft/common/entities/ai/combat/AIDartAttack.java}).
 *
 * <p>The golem's dart launcher shoots at a target that is at least three blocks away: one dart every thirty ticks, eight
 * ticks less for every Air upgrade, and only while the target is in sight and within eighty percent of the golem's range.
 * Inside three blocks the goal gives up, so the melee goal takes over.</p>
 *
 * <p>The original registered this goal with priority 2.</p>
 */
public final class GolemDartAttack extends Goal {
    /** The original 1.7 goal priority of {@code AIDartAttack}. */
    public static final int PRIORITY = 2;
    private static final SoundEvent DART_SHOT = SoundEvent.createVariableRangeEvent(
            ResourceLocation.fromNamespaceAndPath("thaumcraft", "golemironshoot"));

    private final EntityGolemBase golem;
    private LivingEntity attackTarget;
    private final int maxRangedAttackTime;
    private int rangedAttackTime;

    public GolemDartAttack(EntityGolemBase golem) {
        this.golem = golem;
        this.maxRangedAttackTime = GolemAiRules.dartInterval(GolemAiRules.airUpgrades(golem));
        this.rangedAttackTime = GolemAiRules.dartFirstDelay(maxRangedAttackTime);
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
        double distance = golem.distanceToSqr(candidate.getX(), candidate.getBoundingBox().minY, candidate.getZ());
        if (GolemAiRules.dartTooClose(distance)) return false;
        attackTarget = candidate;
        return true;
    }

    @Override public boolean canContinueToUse() { return canUse() && !golem.getNavigation().isDone(); }

    @Override public void stop() {
        attackTarget = null;
        rangedAttackTime = GolemAiRules.dartFirstDelay(maxRangedAttackTime);
    }

    @Override public void tick() {
        LivingEntity current = attackTarget;
        if (current == null || golem.getTarget() != current) return;
        double distance = golem.distanceToSqr(current.getX(), current.getBoundingBox().minY, current.getZ());
        boolean sees = golem.getSensing().hasLineOfSight(current);
        golem.getNavigation().moveTo(current, speed());
        if (!sees) return;
        golem.getLookControl().setLookAt(current, 30, 30);
        rangedAttackTime = Math.max(rangedAttackTime - 1, 0);
        if (rangedAttackTime > 0) return;
        if (!GolemAiRules.dartInReach(distance, golem.getRange())) return;
        shootDart(current);
        rangedAttackTime = maxRangedAttackTime;
    }

    /** Original {@code attackEntityWithRangedAttack}: one dart, the golem's own attack attribute times 0.4. */
    private void shootDart(LivingEntity target) {
        if (!(golem.level() instanceof ServerLevel server) || !golem.isAlive()) return;
        EntityDart dart = new EntityDart(server, golem, target, GolemDartRules.SPEED,
                GolemDartRules.inaccuracy(GolemAiRules.waterUpgrades(golem)));
        dart.setBaseDamage(GolemDartRules.damage((float) golem.getAttributeValue(Attributes.ATTACK_DAMAGE)));
        server.playSound(null, golem.getX(), golem.getY(), golem.getZ(), DART_SHOT, SoundSource.NEUTRAL, 0.5F,
                1.0F / (golem.getRandom().nextFloat() * 0.4F + 0.6F));
        server.addFreshEntity(dart);
    }
}
