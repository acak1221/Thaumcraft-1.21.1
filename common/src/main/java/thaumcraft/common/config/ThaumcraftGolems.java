package thaumcraft.common.config;

import dev.architectury.registry.CreativeTabRegistry;
import dev.architectury.registry.menu.MenuRegistry;
import dev.architectury.registry.registries.DeferredRegister;
import dev.architectury.registry.registries.RegistrySupplier;
import java.util.List;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;
import thaumcraft.common.Thaumcraft;
import thaumcraft.common.container.ContainerGolem;
import thaumcraft.common.entities.golems.EntityGolemBase;
import thaumcraft.common.entities.golems.EntityGolemBobber;
import thaumcraft.common.items.golems.GolemData;
import thaumcraft.common.items.golems.GolemUpgrade;
import thaumcraft.common.items.golems.ItemGolemBell;
import thaumcraft.common.items.golems.ItemGolemCore;
import thaumcraft.common.items.golems.ItemGolemPlacer;
import thaumcraft.common.items.golems.ItemGolemUpgrade;

/** Ordinary living workers; materials and cores require their actual content prerequisites. */
public final class ThaumcraftGolems {
    private static final DeferredRegister<DataComponentType<?>> COMPONENTS = DeferredRegister.create(Thaumcraft.MODID, Registries.DATA_COMPONENT_TYPE);
    private static final DeferredRegister<EntityType<?>> ENTITIES = DeferredRegister.create(Thaumcraft.MODID, Registries.ENTITY_TYPE);
    private static final DeferredRegister<Item> ITEMS = DeferredRegister.create(Thaumcraft.MODID, Registries.ITEM);
    private static final DeferredRegister<MenuType<?>> MENUS = DeferredRegister.create(Thaumcraft.MODID, Registries.MENU);

    public static final RegistrySupplier<DataComponentType<GolemData>> GOLEM_DATA = COMPONENTS.register("golem_data",
            () -> DataComponentType.<GolemData>builder().persistent(GolemData.CODEC).networkSynchronized(GolemData.STREAM_CODEC).build());
    public static final RegistrySupplier<EntityType<EntityGolemBase>> GOLEM = ENTITIES.register("golem",
            () -> EntityType.Builder.<EntityGolemBase>of(EntityGolemBase::new, MobCategory.MISC)
                    .sized(.4F, .95F).clientTrackingRange(4).updateInterval(3).build("thaumcraft:golem"));
    public static final RegistrySupplier<EntityType<EntityGolemBobber>> GOLEM_BOBBER = ENTITIES.register("golem_bobber",
            () -> EntityType.Builder.<EntityGolemBobber>of(EntityGolemBobber::new, MobCategory.MISC)
                    .sized(.25F, .25F).noSave().clientTrackingRange(4).updateInterval(5).build("thaumcraft:golem_bobber"));
    public static final RegistrySupplier<Item> GOLEM_STRAW = placer("golem_straw", 0);
    public static final RegistrySupplier<Item> GOLEM_WOOD = placer("golem_wood", 1);
    public static final RegistrySupplier<Item> GOLEM_TALLOW = placer("golem_tallow", 2);
    public static final RegistrySupplier<Item> GOLEM_CLAY = placer("golem_clay", 3);
    public static final RegistrySupplier<Item> GOLEM_FLESH = placer("golem_flesh", 4);
    public static final RegistrySupplier<Item> GOLEM_STONE = placer("golem_stone", 5);
    public static final RegistrySupplier<Item> GOLEM_IRON = placer("golem_iron", 6);
    public static final RegistrySupplier<Item> GOLEM_THAUMIUM = placer("golem_thaumium", 7);
    public static final RegistrySupplier<Item> CORE_BLANK = ITEMS.register("golem_core_blank",
            () -> new ItemGolemCore(new Item.Properties(), 100));
    public static final RegistrySupplier<Item> CORE_FILL = ITEMS.register("golem_core_fill",
            () -> new ItemGolemCore(new Item.Properties().rarity(Rarity.UNCOMMON), 0));
    public static final RegistrySupplier<Item> CORE_EMPTY = ITEMS.register("golem_core_empty",
            () -> new ItemGolemCore(new Item.Properties().rarity(Rarity.UNCOMMON), 1));
    public static final RegistrySupplier<Item> CORE_GATHER = ITEMS.register("golem_core_gather",
            () -> new ItemGolemCore(new Item.Properties().rarity(Rarity.UNCOMMON), 2));
    public static final RegistrySupplier<Item> CORE_SORTING = ITEMS.register("golem_core_sorting",
            () -> new ItemGolemCore(new Item.Properties().rarity(Rarity.UNCOMMON), 10));
    public static final RegistrySupplier<Item> CORE_HARVEST = ITEMS.register("golem_core_harvest",
            () -> new ItemGolemCore(new Item.Properties().rarity(Rarity.UNCOMMON), 3));
    public static final RegistrySupplier<Item> CORE_LUMBER = ITEMS.register("golem_core_lumber",
            () -> new ItemGolemCore(new Item.Properties().rarity(Rarity.UNCOMMON), 7));
    public static final RegistrySupplier<Item> CORE_GUARD = ITEMS.register("golem_core_guard",
            () -> new ItemGolemCore(new Item.Properties().rarity(Rarity.UNCOMMON), 4));
    public static final RegistrySupplier<Item> CORE_FISHING = ITEMS.register("golem_core_fishing",
            () -> new ItemGolemCore(new Item.Properties().rarity(Rarity.UNCOMMON), 11));
    public static final RegistrySupplier<Item> CORE_LIQUID = ITEMS.register("golem_core_liquid",
            () -> new ItemGolemCore(new Item.Properties().rarity(Rarity.UNCOMMON), 5));
    public static final RegistrySupplier<Item> CORE_ALCHEMY = ITEMS.register("golem_core_essentia",
            () -> new ItemGolemCore(new Item.Properties().rarity(Rarity.UNCOMMON), 6));
    public static final RegistrySupplier<Item> CORE_BUTCHER = ITEMS.register("golem_core_butcher",
            () -> new ItemGolemCore(new Item.Properties().rarity(Rarity.UNCOMMON), 9));
    public static final RegistrySupplier<Item> CORE_USE = ITEMS.register("golem_core_use",
            () -> new ItemGolemCore(new Item.Properties().rarity(Rarity.UNCOMMON), 8));
    // Only upgrade types with an implemented, verified effect exist (see GolemUpgrade); all six now do.
    public static final RegistrySupplier<Item> UPGRADE_AIR = upgrade("golem_upgrade_air", GolemUpgrade.AIR);
    public static final RegistrySupplier<Item> UPGRADE_EARTH = upgrade("golem_upgrade_earth", GolemUpgrade.EARTH);
    public static final RegistrySupplier<Item> UPGRADE_FIRE = upgrade("golem_upgrade_fire", GolemUpgrade.FIRE);
    public static final RegistrySupplier<Item> UPGRADE_WATER = upgrade("golem_upgrade_water", GolemUpgrade.WATER);
    public static final RegistrySupplier<Item> UPGRADE_ORDER = upgrade("golem_upgrade_order", GolemUpgrade.ORDER);
    public static final RegistrySupplier<Item> UPGRADE_ENTROPY = upgrade("golem_upgrade_entropy", GolemUpgrade.ENTROPY);
    public static final RegistrySupplier<Item> GOLEM_BELL = ITEMS.register("golem_bell",
            () -> new ItemGolemBell(new Item.Properties().stacksTo(1)));
    public static final RegistrySupplier<MenuType<ContainerGolem>> GOLEM_MENU = MENUS.register("golem",
            () -> MenuRegistry.ofExtended(ContainerGolem::new));

    private ThaumcraftGolems() {}

    private static RegistrySupplier<Item> upgrade(String id, GolemUpgrade kind) {
        return ITEMS.register(id, () -> new ItemGolemUpgrade(new Item.Properties().rarity(Rarity.UNCOMMON), kind));
    }

    /** The item of an upgrade type. Every original type has one now. */
    public static Item upgradeItem(GolemUpgrade kind) {
        return switch (kind) {
            case AIR -> UPGRADE_AIR.get();
            case EARTH -> UPGRADE_EARTH.get();
            case FIRE -> UPGRADE_FIRE.get();
            case WATER -> UPGRADE_WATER.get();
            case ORDER -> UPGRADE_ORDER.get();
            case ENTROPY -> UPGRADE_ENTROPY.get();
        };
    }

    private static RegistrySupplier<Item> placer(String id, int material) {
        return ITEMS.register(id, () -> new ItemGolemPlacer(new Item.Properties().stacksTo(1)
                .component(GOLEM_DATA.get(), GolemData.EMPTY), material));
    }

    public static Item placerForMaterial(int material) {
        return switch (material) {
            case 0 -> GOLEM_STRAW.get();
            case 1 -> GOLEM_WOOD.get();
            case 2 -> GOLEM_TALLOW.get();
            case 3 -> GOLEM_CLAY.get();
            case 4 -> GOLEM_FLESH.get();
            case 5 -> GOLEM_STONE.get();
            case 6 -> GOLEM_IRON.get();
            case 7 -> GOLEM_THAUMIUM.get();
            default -> null;
        };
    }

    public static List<Item> placerItems() {
        return List.of(GOLEM_STRAW.get(), GOLEM_WOOD.get(), GOLEM_TALLOW.get(), GOLEM_CLAY.get(), GOLEM_FLESH.get(),
                GOLEM_STONE.get(), GOLEM_IRON.get(), GOLEM_THAUMIUM.get());
    }

    public static Item coreForType(int core) {
        return switch (core) {
            case 0 -> CORE_FILL.get();
            case 1 -> CORE_EMPTY.get();
            case 2 -> CORE_GATHER.get();
            case 3 -> CORE_HARVEST.get();
            case 4 -> CORE_GUARD.get();
            case 5 -> CORE_LIQUID.get();
            case 6 -> CORE_ALCHEMY.get();
            case 7 -> CORE_LUMBER.get();
            case 8 -> CORE_USE.get();
            case 9 -> CORE_BUTCHER.get();
            case 10 -> CORE_SORTING.get();
            case 11 -> CORE_FISHING.get();
            default -> null;
        };
    }

    public static void register() {
        COMPONENTS.register(); ENTITIES.register(); ITEMS.register(); MENUS.register();
        CreativeTabRegistry.append(ThaumcraftContent.TAB, GOLEM_STRAW, GOLEM_WOOD, GOLEM_TALLOW, GOLEM_CLAY, GOLEM_FLESH, GOLEM_STONE, GOLEM_IRON, GOLEM_THAUMIUM,
                CORE_BLANK, CORE_FILL, CORE_EMPTY, CORE_GATHER, CORE_HARVEST, CORE_LUMBER, CORE_GUARD, CORE_LIQUID, CORE_ALCHEMY, CORE_USE, CORE_BUTCHER, CORE_SORTING, CORE_FISHING, UPGRADE_AIR, UPGRADE_EARTH, UPGRADE_FIRE, UPGRADE_WATER, UPGRADE_ORDER, UPGRADE_ENTROPY, GOLEM_BELL);
    }
}
