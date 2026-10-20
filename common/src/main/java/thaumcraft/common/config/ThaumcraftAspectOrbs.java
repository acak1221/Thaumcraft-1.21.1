package thaumcraft.common.config;

import dev.architectury.registry.registries.DeferredRegister;
import dev.architectury.registry.registries.RegistrySupplier;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import thaumcraft.common.Thaumcraft;
import thaumcraft.common.entities.EntityAspectOrb;

public final class ThaumcraftAspectOrbs {
    private static final DeferredRegister<EntityType<?>> ENTITIES = DeferredRegister.create(Thaumcraft.MODID, Registries.ENTITY_TYPE);
    public static final RegistrySupplier<EntityType<EntityAspectOrb>> ORB = ENTITIES.register("aspect_orb", () -> EntityType.Builder
            .<EntityAspectOrb>of(EntityAspectOrb::new, MobCategory.MISC).sized(.125F, .125F).clientTrackingRange(8).updateInterval(20).build("thaumcraft:aspect_orb"));
    private ThaumcraftAspectOrbs() {}
    public static void register() { ENTITIES.register(); }
}
