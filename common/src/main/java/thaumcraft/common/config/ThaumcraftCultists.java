package thaumcraft.common.config;

import dev.architectury.registry.CreativeTabRegistry;
import dev.architectury.registry.registries.DeferredRegister;
import dev.architectury.registry.registries.RegistrySupplier;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.SpawnEggItem;
import thaumcraft.common.Thaumcraft;
import thaumcraft.common.entities.monster.EntityCultist;
import thaumcraft.common.entities.monster.EntityCultistCleric;
import thaumcraft.common.entities.monster.EntityCultistKnight;
import thaumcraft.common.entities.monster.EntityCultistLeader;
import thaumcraft.common.entities.monster.EntityCultistPortal;
import thaumcraft.common.entities.monster.EntityCultistPortalGreater;

/**
 * The TC4 cultist faction: the crimson knight, the crimson cleric, the praetor, the
 * crimson portal and the greater crimson portal, plus the homing energy orb they throw.
 * The original ConfigEntities registers "CultistKnight", "CultistCleric", "CultistLeader"
 * and "CultistPortal"; the greater portal is the later alias of the same entity.
 */
public final class ThaumcraftCultists {
    private static final DeferredRegister<EntityType<?>> ENTITIES = DeferredRegister.create(Thaumcraft.MODID, Registries.ENTITY_TYPE);
    private static final DeferredRegister<Item> ITEMS = DeferredRegister.create(Thaumcraft.MODID, Registries.ITEM);

    public static final RegistrySupplier<EntityType<EntityCultistKnight>> CULTIST_KNIGHT = ENTITIES.register("cultist_knight",
            () -> EntityType.Builder.<EntityCultistKnight>of(EntityCultistKnight::new, MobCategory.MONSTER)
                    .sized(0.6F, 1.8F).eyeHeight(1.53F).clientTrackingRange(4).updateInterval(3)
                    .build("thaumcraft:cultist_knight"));
    public static final RegistrySupplier<EntityType<EntityCultistCleric>> CULTIST_CLERIC = ENTITIES.register("cultist_cleric",
            () -> EntityType.Builder.<EntityCultistCleric>of(EntityCultistCleric::new, MobCategory.MONSTER)
                    .sized(0.6F, 1.8F).eyeHeight(1.53F).clientTrackingRange(4).updateInterval(3)
                    .build("thaumcraft:cultist_cleric"));
    public static final RegistrySupplier<EntityType<EntityCultistLeader>> CULTIST_LEADER = ENTITIES.register("cultist_leader",
            () -> EntityType.Builder.<EntityCultistLeader>of(EntityCultistLeader::new, MobCategory.MONSTER)
                    .sized(0.75F, 2.25F).eyeHeight(1.91F).clientTrackingRange(4).updateInterval(3)
                    .build("thaumcraft:cultist_leader"));
    public static final RegistrySupplier<EntityType<EntityCultistPortal>> CULTIST_PORTAL = ENTITIES.register("cultist_portal",
            () -> EntityType.Builder.<EntityCultistPortal>of(EntityCultistPortal::new, MobCategory.MONSTER)
                    .sized(1.5F, 3.0F).eyeHeight(0.0F).clientTrackingRange(4).updateInterval(20).fireImmune()
                    .build("thaumcraft:cultist_portal"));
    public static final RegistrySupplier<EntityType<EntityCultistPortalGreater>> CULTIST_PORTAL_GREATER = ENTITIES.register("cultist_portal_greater",
            () -> EntityType.Builder.<EntityCultistPortalGreater>of(EntityCultistPortalGreater::new, MobCategory.MONSTER)
                    .sized(1.5F, 3.0F).eyeHeight(0.0F).clientTrackingRange(4).updateInterval(20).fireImmune()
                    .build("thaumcraft:cultist_portal_greater"));

    public static final RegistrySupplier<SpawnEggItem> KNIGHT_EGG = ITEMS.register("cultist_knight_spawn_egg",
            () -> new SpawnEggItem(CULTIST_KNIGHT.get(), 0xFF5055, 0x000080, new Item.Properties()));
    public static final RegistrySupplier<SpawnEggItem> CLERIC_EGG = ITEMS.register("cultist_cleric_spawn_egg",
            () -> new SpawnEggItem(CULTIST_CLERIC.get(), 0xFF5055, 0x800000, new Item.Properties()));
    public static final RegistrySupplier<SpawnEggItem> LEADER_EGG = ITEMS.register("cultist_leader_spawn_egg",
            () -> new SpawnEggItem(CULTIST_LEADER.get(), 0xFF5055, 0x505050, new Item.Properties()));

    private static boolean registered;

    private ThaumcraftCultists() {}

    public static void register() {
        if (registered) return;
        registered = true;
        ENTITIES.register();
        ITEMS.register();
        CreativeTabRegistry.append(ThaumcraftContent.TAB, KNIGHT_EGG, CLERIC_EGG, LEADER_EGG);
    }

    /**
     * Loader hook: the cultist attribute suppliers. The loader owns the registry,
     * so the common module only publishes the suppliers.
     */
    public static void registerAttributes(java.util.function.BiConsumer<EntityType<?>,
            net.minecraft.world.entity.ai.attributes.AttributeSupplier.Builder> registrar) {
        registrar.accept(CULTIST_KNIGHT.get(), thaumcraft.common.entities.monster.EntityCultistKnight.createAttributes());
        registrar.accept(CULTIST_CLERIC.get(), thaumcraft.common.entities.monster.EntityCultistCleric.createAttributes());
        registrar.accept(CULTIST_LEADER.get(), thaumcraft.common.entities.monster.EntityCultistLeader.createAttributes());
        registrar.accept(CULTIST_PORTAL.get(), thaumcraft.common.entities.monster.EntityCultistPortal.createAttributes());
        registrar.accept(CULTIST_PORTAL_GREATER.get(), thaumcraft.common.entities.monster.EntityCultistPortal.createAttributes());
    }
}
