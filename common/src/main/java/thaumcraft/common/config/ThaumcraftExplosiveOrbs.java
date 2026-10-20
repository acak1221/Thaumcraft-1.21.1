package thaumcraft.common.config;

import dev.architectury.registry.registries.DeferredRegister;
import dev.architectury.registry.registries.RegistrySupplier;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import thaumcraft.common.Thaumcraft;
import thaumcraft.common.entities.projectile.EntityExplosiveOrb;

/** Original ConfigEntities explosive orb: range64, update20, inherited .25 body. */
public final class ThaumcraftExplosiveOrbs {
    private static final DeferredRegister<EntityType<?>> ENTITIES = DeferredRegister.create(Thaumcraft.MODID, Registries.ENTITY_TYPE);
    public static final RegistrySupplier<EntityType<EntityExplosiveOrb>> EXPLOSIVE_ORB = ENTITIES.register("explosive_orb",
            () -> EntityType.Builder.<EntityExplosiveOrb>of(EntityExplosiveOrb::new, MobCategory.MISC)
                    .sized(.25F, .25F).clientTrackingRange(4).updateInterval(20).build("thaumcraft:explosive_orb"));
    private static boolean registered;
    private ThaumcraftExplosiveOrbs() {}
    public static void register() { if (!registered) { registered = true; ENTITIES.register(); } }
}
