package thaumcraft.api.damagesource;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;

/**
 * Native registry-backed counterparts of the original attributed attacks.
 *
 * <p>The original published FOUR ready-made sources (oracle
 * {@code thaumcraft/api/damagesource/DamageSourceThaumcraft.java:19-22}): {@code taint},
 * {@code tentacle}, {@code swarm} and {@code dissolve}.</p>
 *
 * <p>Forced 1.21.1 change: 1.7.10 built a {@code DamageSource} in the constructor of the class, so the
 * four sources were static fields. 1.21.1 has no such thing: every source is built from a
 * {@link net.minecraft.core.Holder} of a {@link DamageType} that lives in the datapack registry. The native
 * port therefore publishes the {@link ResourceKey} of every type and the factory that returns the ready
 * source of the original. The type ids keep the namespace of the mod, so they never collide with the
 * vanilla ones.</p>
 */
public final class DamageSourceThaumcraft {
    /** Oracle :19 - the taint poison. It bypasses the armour and it counts as magic damage. */
    public static final ResourceKey<DamageType> TAINT = key("taint");
    /** Oracle :20 - the bite of the taintacle. The taintacle is the attacker. */
    public static final ResourceKey<DamageType> TENTACLE = key("tentacle");
    /** Oracle :21 - the bite of the taint swarm. The swarm is the attacker. */
    public static final ResourceKey<DamageType> SWARM = key("swarm");
    /** Oracle :22 - the death fluid. It bypasses the armour. */
    public static final ResourceKey<DamageType> DISSOLVE = key("dissolve");

    private DamageSourceThaumcraft() {}

    /** The original {@code taint} source (oracle :19): the damage of the taint poison. */
    public static DamageSource taint(Level level) { return source(level, TAINT); }
    /** The original {@code dissolve} source (oracle :22): the damage of the death fluid. */
    public static DamageSource dissolve(Level level) { return source(level, DISSOLVE); }
    /** The original {@code causeTentacleDamage} (oracle :40): the taintacle is the attacker. */
    public static DamageSource causeTentacleDamage(LivingEntity source) { return attributed(source, TENTACLE); }
    /** The original {@code causeSwarmDamage} (oracle :36): the swarm is the attacker. */
    public static DamageSource causeSwarmDamage(LivingEntity source) { return attributed(source, SWARM); }

    private static ResourceKey<DamageType> key(String id) { return ResourceKey.create(Registries.DAMAGE_TYPE,ResourceLocation.fromNamespaceAndPath("thaumcraft",id)); }
    private static DamageSource source(Level level,ResourceKey<DamageType> type) {
        return new DamageSource(level.registryAccess().registryOrThrow(Registries.DAMAGE_TYPE).getHolderOrThrow(type));
    }
    private static DamageSource attributed(LivingEntity source,ResourceKey<DamageType> type) {
        return new DamageSource(source.registryAccess().registryOrThrow(Registries.DAMAGE_TYPE).getHolderOrThrow(type),source);
    }
}
