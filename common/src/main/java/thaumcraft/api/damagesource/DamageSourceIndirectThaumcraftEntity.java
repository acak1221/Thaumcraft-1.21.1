package thaumcraft.api.damagesource;

import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.entity.Entity;

/**
 * The original TC4 indirect damage source.
 *
 * <p>Forced 1.21.1 change: 1.21.1 has no {@code EntityDamageSourceIndirect} class; a damage source is a
 * record over a registered damage type plus its two entities, so this class builds one instead of extending
 * a class that no longer exists. The original "fire" and "unblockable" modifiers became damage type
 * properties, which the caller selects through the registered damage type.</p>
 */
public final class DamageSourceIndirectThaumcraftEntity {
    private DamageSourceIndirectThaumcraftEntity() {}

    /** A damage source that names the direct attacker and the real cause. */
    public static DamageSource create(Entity direct, Entity cause, Holder<DamageType> type) {
        return new DamageSource(type, cause, direct);
    }

    /** A damage source that names the direct attacker and the real cause. */
    public static DamageSource create(Entity direct, Entity cause, ResourceKey<DamageType> type, net.minecraft.core.RegistryAccess registries) {
        return new DamageSource(registries.registryOrThrow(net.minecraft.core.registries.Registries.DAMAGE_TYPE).getHolderOrThrow(type), cause, direct);
    }
}
