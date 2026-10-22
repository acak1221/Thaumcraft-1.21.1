package thaumcraft.api.damagesource;

import net.minecraft.core.Holder;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.entity.Entity;

/**
 * The original TC4 indirect damage source.
 *
 * <p>Oracle {@code thaumcraft/api/damagesource/DamageSourceIndirectThaumcraftEntity.java:15-35}
 * (CFR 0.152 decompilation of {@code Thaumcraft-1.7.10-4.2.3.5.jar}): the class extended the
 * 1.7.10 {@code EntityDamageSourceIndirect}, so a damage source named the direct attacker and the
 * real cause, and it carried the two modifiers of the original: the fire damage and the
 * unblockable damage, the latter also zeroing the hunger damage.</p>
 *
 * <p>Forced 1.21.1 change: 1.21.1 has no {@code EntityDamageSourceIndirect} class and its
 * {@code DamageSource} is a record over a registered damage type and its two entities, so the
 * class became a factory. The two modifiers of the original are properties of the registered
 * damage type in 1.21.1 - the fire tag and the armour bypass with an exhaustion of zero - and the
 * two factories with the original names select a type that carries the property and refuse a type
 * that does not, so an add-on never loses a modifier silently.</p>
 */
public final class DamageSourceIndirectThaumcraftEntity {
    private DamageSourceIndirectThaumcraftEntity() {}

    /** A damage source that names the direct attacker and the real cause. */
    public static DamageSource create(Entity direct, Entity cause, Holder<DamageType> type) {
        return new DamageSource(type, cause, direct);
    }

    /** A damage source that names the direct attacker and the real cause. */
    public static DamageSource create(Entity direct, Entity cause, ResourceKey<DamageType> type, RegistryAccess registries) {
        return new DamageSource(registries.registryOrThrow(Registries.DAMAGE_TYPE).getHolderOrThrow(type), cause, direct);
    }

    /**
     * Original {@code func_76361_j} - the fire damage modifier.
     *
     * <p>The original set the flag of the damage source, so the victim burned. In 1.21.1 the fire
     * damage is the fire tag of the registered damage type, so the type has to carry it.</p>
     *
     * @param direct the direct attacker
     * @param cause  the real cause
     * @param type   the registered damage type, it has to carry the fire tag
     * @return the damage source with the fire damage
     */
    public static DamageSource setFireDamage(Entity direct, Entity cause, Holder<DamageType> type) {
        if (type == null || !type.is(DamageTypeTags.IS_FIRE)) {
            throw new IllegalArgumentException("The fire damage modifier needs a damage type of the fire tag");
        }
        return new DamageSource(type, cause, direct);
    }

    /**
     * Original {@code func_76348_h} - the unblockable modifier.
     *
     * <p>The original set the flag of the damage source and zeroed the hunger damage of it. In
     * 1.21.1 the armour bypass is the armour bypass tag of the registered damage type and the
     * hunger damage is its exhaustion, so the type has to carry both.</p>
     *
     * @param direct the direct attacker
     * @param cause  the real cause
     * @param type   the registered damage type, it has to bypass the armour and to exhaust nothing
     * @return the damage source with the unblockable damage
     */
    public static DamageSource setUnblockable(Entity direct, Entity cause, Holder<DamageType> type) {
        if (type == null || !type.is(DamageTypeTags.BYPASSES_ARMOR) || type.value().exhaustion() != 0.0F) {
            throw new IllegalArgumentException("The unblockable modifier needs a damage type that bypasses the armour and exhausts nothing");
        }
        return new DamageSource(type, cause, direct);
    }
}
