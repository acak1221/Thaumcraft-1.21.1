package thaumcraft.common.config;

import dev.architectury.registry.CreativeTabRegistry;
import dev.architectury.registry.registries.DeferredRegister;
import dev.architectury.registry.registries.RegistrySupplier;
import java.util.function.BiConsumer;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.SpawnEggItem;
import thaumcraft.common.Thaumcraft;
import thaumcraft.common.entities.monster.EntityTaintCreeper;

/**
 * The registration of the original taint creeper.
 *
 * <p>Original {@code thaumcraft/common/config/ConfigEntities.java:155}: the taint creeper is registered as
 * {@code TaintedCreeper}. The registration lives in its own class, because the taint creeper is a mob of the
 * monster layer and the loader owns the registry.</p>
 */
public final class ThaumcraftTaintCreeper {
    private static final DeferredRegister<EntityType<?>> ENTITIES = DeferredRegister.create(Thaumcraft.MODID, Registries.ENTITY_TYPE);
    private static final DeferredRegister<Item> ITEMS = DeferredRegister.create(Thaumcraft.MODID, Registries.ITEM);

    /** The original taint creeper: the size of the vanilla creeper. */
    public static final RegistrySupplier<EntityType<EntityTaintCreeper>> TAINT_CREEPER = ENTITIES.register("taint_creeper",
            () -> EntityType.Builder.<EntityTaintCreeper>of(EntityTaintCreeper::new, MobCategory.MONSTER)
                    .sized(.6F, 1.7F).clientTrackingRange(8).updateInterval(3).build("thaumcraft:taint_creeper"));

    private static final RegistrySupplier<SpawnEggItem> CREEPER_EGG = ITEMS.register("taint_creeper_spawn_egg",
            () -> new SpawnEggItem(TAINT_CREEPER.get(), 0x4C7A4C, 0x8B8B8B, new Item.Properties()));

    private static boolean registered;

    private ThaumcraftTaintCreeper() {}

    public static void register() {
        if (registered) return;
        registered = true;
        ENTITIES.register();
        ITEMS.register();
        CreativeTabRegistry.append(ThaumcraftContent.TAB, CREEPER_EGG);
    }

    /** Loader hook: the loader owns the registry, so the common module only publishes the supplier. */
    public static void registerAttributes(BiConsumer<EntityType<?>, AttributeSupplier.Builder> registrar) {
        registrar.accept(TAINT_CREEPER.get(), EntityTaintCreeper.createAttributes());
    }
}
