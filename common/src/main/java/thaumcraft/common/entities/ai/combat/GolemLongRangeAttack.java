package thaumcraft.common.entities.ai.combat;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.goal.RangedAttackGoal;
import net.minecraft.world.entity.monster.RangedAttackMob;
import thaumcraft.common.lib.golems.GolemAiRules;

/**
 * Original {@code AILongRangeAttack} (TC4 4.2.3.5, {@code thaumcraft/common/entities/ai/combat/AILongRangeAttack.java}).
 *
 * <p>The original extended the vanilla arrow attack with a minimum distance: a wielder that is closer to its target than
 * that distance stops shooting altogether, and a dead target is forgotten.</p>
 *
 * <p>The native 1.21.1 arrow attack is {@link RangedAttackGoal}; the port keeps that goal and adds the original rule on
 * top of it, so the sight, the cooldown and the shot itself stay the native ones.</p>
 */
public final class GolemLongRangeAttack extends RangedAttackGoal {
    /** The original 1.7 goal priority of {@code AILongRangeAttack}. */
    public static final int PRIORITY = 2;

    private final Mob wielder;
    private final double minDistance;

    public GolemLongRangeAttack(Mob mob, double minDistance, double distance, int min, int max, float fieldOfView) {
        super((RangedAttackMob) mob, distance, min, max, fieldOfView);
        this.wielder = mob;
        this.minDistance = minDistance;
    }

    @Override public boolean canUse() {
        boolean allowed = super.canUse();
        if (!allowed) return false;
        LivingEntity target = wielder.getTarget();
        if (target == null) return false;
        if (target.isDeadOrDying()) {
            wielder.setTarget(null); // Original: a dead target is forgotten.
            return false;
        }
        double distance = wielder.distanceToSqr(target.getX(), target.getBoundingBox().minY, target.getZ());
        return GolemAiRules.beyondLongRange(distance, minDistance);
    }
}
