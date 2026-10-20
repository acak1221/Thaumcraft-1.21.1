package thaumcraft.common.config;

import dev.architectury.registry.registries.DeferredRegister;
import dev.architectury.registry.registries.RegistrySupplier;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import thaumcraft.common.Thaumcraft;
import thaumcraft.common.entities.projectile.EntityEmber;

/** The FIRE focus's real, short-lived projectile; no item or focus registration here. */
public final class ThaumcraftEmbers {
    private static final DeferredRegister<EntityType<?>> ENTITIES = DeferredRegister.create(Thaumcraft.MODID, Registries.ENTITY_TYPE);
    // TC4 ConfigEntities:104: range64, update20, velocity tracking. The inherited
    // EntityThrowable body is .25 x .25, independent of the growing visual quad.
    public static final RegistrySupplier<EntityType<EntityEmber>> EMBER = ENTITIES.register("ember",
            () -> EntityType.Builder.<EntityEmber>of(EntityEmber::new, MobCategory.MISC)
                    .sized(.25F, .25F).clientTrackingRange(4).updateInterval(20).build("thaumcraft:ember"));
    private static boolean registered;

    private ThaumcraftEmbers() {}

    public static void register() {
        if (registered) return;
        registered = true;
        ENTITIES.register();
    }

    /**
     * Exactly one attempted spawn. The focus owns payment, repetition and sound;
     * this method neither refunds vis nor treats a rejected spawn as accepted.
     * fireSeconds is TOTAL entity ignition, not the alchemist's-fire upgrade level.
     */
    public static boolean emit(ServerPlayer player, float damage, float scatter, int lifetime, int fireSeconds) {
        var ember = new EntityEmber(player.serverLevel(), player, scatter);
        ember.setDamage(damage);
        ember.setDuration(lifetime);
        ember.setFireSeconds(fireSeconds);
        // TC4 ItemFocusFire:133-136 advances by the FINAL scattered motion before
        // spawning. Do not substitute the player's eye position or a target ray.
        ember.setPos(ember.position().add(ember.getDeltaMovement()));
        return player.serverLevel().addFreshEntity(ember);
    }
}
