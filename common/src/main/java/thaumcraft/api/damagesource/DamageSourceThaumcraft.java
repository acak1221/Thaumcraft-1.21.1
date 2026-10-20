package thaumcraft.api.damagesource;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.entity.LivingEntity;

/** Native registry-backed counterparts of the original attributed attacks. */
public final class DamageSourceThaumcraft {
    public static final ResourceKey<DamageType> TENTACLE=key("tentacle"), SWARM=key("swarm");
    private DamageSourceThaumcraft() {}
    private static ResourceKey<DamageType> key(String id) { return ResourceKey.create(Registries.DAMAGE_TYPE,ResourceLocation.fromNamespaceAndPath("thaumcraft",id)); }
    public static DamageSource causeTentacleDamage(LivingEntity source) { return attributed(source,TENTACLE); }
    public static DamageSource causeSwarmDamage(LivingEntity source) { return attributed(source,SWARM); }
    private static DamageSource attributed(LivingEntity source,ResourceKey<DamageType> type) {
        return new DamageSource(source.registryAccess().registryOrThrow(Registries.DAMAGE_TYPE).getHolderOrThrow(type),source);
    }
}
