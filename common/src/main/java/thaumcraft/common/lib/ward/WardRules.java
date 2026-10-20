package thaumcraft.common.lib.ward;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.Attributes;

/**
 * The original TC4 absorption wards and the capacity of 1.21.1 that carries them.
 *
 * <p>The 1.7 original had no clamp on the absorption at all: the eldritch warden, the eldritch
 * guardian, the warded champion and the warded praetor simply raised their absorption to the size of
 * their ward. 1.21.1 clamps {@code LivingEntity#setAbsorptionAmount} by the
 * {@code generic.max_absorption} attribute, whose default is zero, so a ward of the original size was
 * always clamped away again and every ward of the port stayed at zero.
 *
 * <p>The ward is therefore written through the attribute: the max absorption of the mob is raised to
 * the size of the ward first, and the absorption itself follows.
 *
 * <p>Oracle: {@code thaumcraft/common/entities/monster/boss/EntityEldritchWarden.java}:318 (the
 * warden), {@code thaumcraft/common/entities/monster/EntityEldritchGuardian.java}:246-259 (the
 * guardian), {@code thaumcraft/common/lib/utils/EntityUtils.java}:271-274 (the warded champion) and
 * {@code thaumcraft/common/entities/monster/mods/ChampionModWarded.java}:25-30 (its regeneration).
 */
public final class WardRules {
    private WardRules() {}

    /**
     * The absorption capacity a ward of the given size needs. The capacity of an entity never drops
     * below the ward it already carries.
     */
    public static double capacity(float ward) { return Math.max(0.0D, ward); }

    /**
     * Raises the max absorption of a mob so that it can carry the given amount. The attribute of the
     * 1.21.1 mob has a range of zero to two thousand and forty eight, so a ward always fits.
     */
    public static void ensureCapacity(LivingEntity mob, float amount) {
        AttributeInstance absorption = mob.getAttribute(Attributes.MAX_ABSORPTION);
        if (absorption == null) return;
        if (absorption.getBaseValue() < amount) absorption.setBaseValue(amount);
    }

    /**
     * The original warded praetor: the ward becomes the whole absorption of the mob.
     */
    public static void setWard(LivingEntity mob, float ward) {
        ensureCapacity(mob, ward);
        mob.setAbsorptionAmount(ward);
    }

    /**
     * The original warden, the original guardian and the original warded champion: the ward
     * joins the absorption the mob already carries.
     */
    public static void addWard(LivingEntity mob, float ward) {
        float total = mob.getAbsorptionAmount() + ward;
        ensureCapacity(mob, total);
        mob.setAbsorptionAmount(total);
    }
}
