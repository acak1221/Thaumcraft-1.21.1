package thaumcraft.common.config;

import dev.architectury.registry.registries.DeferredRegister;
import dev.architectury.registry.registries.RegistrySupplier;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import thaumcraft.common.Thaumcraft;
import thaumcraft.common.entities.EntityFollowingItem;
import thaumcraft.common.entities.EntityItemGrate;
import thaumcraft.common.entities.EntityPermanentItem;
import thaumcraft.common.entities.projectile.EntityBottleTaint;
import thaumcraft.common.entities.projectile.EntityGolemOrb;
import thaumcraft.common.entities.projectile.EntityPrimalArrow;

/**
 * The TC4 projectiles and their supporting entities.
 * <p>
 * The original registered every one of them with a tracking range of 64
 * blocks and an update interval of 20 ticks, which is the native
 * {@code clientTrackingRange(4)} and {@code updateInterval(20)} of a chunk
 * based range.
 */
public final class ThaumcraftProjectiles {
    private static final DeferredRegister<EntityType<?>> ENTITIES = DeferredRegister.create(Thaumcraft.MODID, Registries.ENTITY_TYPE);

    /** Original ConfigEntities "BottleTaint": the thrown bottle of taint. */
    public static final RegistrySupplier<EntityType<EntityBottleTaint>> BOTTLE_TAINT = ENTITIES.register("bottle_taint",
            () -> EntityType.Builder.<EntityBottleTaint>of(EntityBottleTaint::new, MobCategory.MISC)
                    .sized(.25F, .25F).clientTrackingRange(4).updateInterval(20).build("thaumcraft:bottle_taint"));
    /** Original ConfigEntities "GolemOrb": the homing orb of the cultists and the eldritch golem. */
    public static final RegistrySupplier<EntityType<EntityGolemOrb>> GOLEM_ORB = ENTITIES.register("golem_orb",
            () -> EntityType.Builder.<EntityGolemOrb>of(EntityGolemOrb::new, MobCategory.MISC)
                    .sized(.25F, .25F).clientTrackingRange(4).updateInterval(20).build("thaumcraft:golem_orb"));
    /** Original ConfigEntities "PrimalArrow": the arrow of the bone bow. */
    public static final RegistrySupplier<EntityType<EntityPrimalArrow>> PRIMAL_ARROW = ENTITIES.register("primal_arrow",
            () -> EntityType.Builder.<EntityPrimalArrow>of(EntityPrimalArrow::new, MobCategory.MISC)
                    .sized(.5F, .5F).clientTrackingRange(4).updateInterval(20).build("thaumcraft:primal_arrow"));
    /** Original ConfigEntities "FollowItem": the item that flies to its target. */
    public static final RegistrySupplier<EntityType<EntityFollowingItem>> FOLLOW_ITEM = ENTITIES.register("follow_item",
            () -> EntityType.Builder.<EntityFollowingItem>of(EntityFollowingItem::new, MobCategory.MISC)
                    .sized(.25F, .25F).clientTrackingRange(4).updateInterval(20).build("thaumcraft:follow_item"));
    /** Original ConfigEntities "PermanentItem": the endless item of the eldritch pedestal rooms. */
    public static final RegistrySupplier<EntityType<EntityPermanentItem>> PERMANENT_ITEM = ENTITIES.register("permanent_item",
            () -> EntityType.Builder.<EntityPermanentItem>of(EntityPermanentItem::new, MobCategory.MISC)
                    .sized(.25F, .25F).clientTrackingRange(4).updateInterval(20).build("thaumcraft:permanent_item"));
    /** Original ConfigEntities "SpecialItemGrate": the item of the item filter block. */
    public static final RegistrySupplier<EntityType<EntityItemGrate>> ITEM_GRATE = ENTITIES.register("item_grate",
            () -> EntityType.Builder.<EntityItemGrate>of(EntityItemGrate::new, MobCategory.MISC)
                    .sized(.25F, .25F).clientTrackingRange(4).updateInterval(20).build("thaumcraft:item_grate"));

    private static boolean registered;

    private ThaumcraftProjectiles() {}

    public static void register() {
        if (registered) return;
        registered = true;
        ENTITIES.register();
    }
}
