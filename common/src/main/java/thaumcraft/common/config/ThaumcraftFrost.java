package thaumcraft.common.config;

import dev.architectury.registry.registries.DeferredRegister;
import dev.architectury.registry.registries.RegistrySupplier;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import thaumcraft.common.Thaumcraft;
import thaumcraft.common.entities.projectile.EntityFrostShard;

/** BASIC Frost's projectile only. The focus owns payment and its one spawn attempt. */
public final class ThaumcraftFrost {
    private static final DeferredRegister<EntityType<?>> ENTITIES = DeferredRegister.create(Thaumcraft.MODID, Registries.ENTITY_TYPE);
    // Original tracking range64/update20/velocity; damage3 gives a .6 x .6 body.
    // Native hurtMarked additionally sends velocity immediately after a bounce.
    public static final RegistrySupplier<EntityType<EntityFrostShard>> FROST_SHARD = ENTITIES.register("frost_shard",
            () -> EntityType.Builder.<EntityFrostShard>of(EntityFrostShard::new, MobCategory.MISC)
                    .sized(.6F, .6F).clientTrackingRange(4).updateInterval(20).build("thaumcraft:frost_shard"));
    private static boolean registered;

    private ThaumcraftFrost() {}

    public static void register() {
        if (registered) return;
        registered = true;
        ENTITIES.register();
    }
}
