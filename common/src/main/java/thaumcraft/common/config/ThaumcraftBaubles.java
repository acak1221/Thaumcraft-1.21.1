package thaumcraft.common.config;

import dev.architectury.registry.CreativeTabRegistry;
import dev.architectury.registry.menu.MenuRegistry;
import dev.architectury.registry.registries.DeferredRegister;
import dev.architectury.registry.registries.RegistrySupplier;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;
import thaumcraft.api.baubles.BaubleType;
import thaumcraft.common.Thaumcraft;
import thaumcraft.common.container.ContainerAccessories;
import thaumcraft.common.items.baubles.ItemAmuletVis;
import thaumcraft.common.items.baubles.ItemBaubleBlank;
import thaumcraft.common.items.baubles.ItemRunicBauble;
import thaumcraft.common.items.baubles.VisAmuletData;

/** Native four-slot accessory inventory and original functional baubles, no external Baubles runtime. */
public final class ThaumcraftBaubles {
    private static final DeferredRegister<DataComponentType<?>> COMPONENTS = DeferredRegister.create(Thaumcraft.MODID, Registries.DATA_COMPONENT_TYPE);
    private static final DeferredRegister<Item> ITEMS = DeferredRegister.create(Thaumcraft.MODID, Registries.ITEM);
    private static final DeferredRegister<MenuType<?>> MENUS = DeferredRegister.create(Thaumcraft.MODID, Registries.MENU);
    public static final RegistrySupplier<DataComponentType<VisAmuletData>> LESSER_VIS = COMPONENTS.register("lesser_amulet_vis", () -> DataComponentType.<VisAmuletData>builder()
            .persistent(VisAmuletData.codec(2500)).networkSynchronized(VisAmuletData.streamCodec(2500)).build());
    public static final RegistrySupplier<DataComponentType<VisAmuletData>> VIS = COMPONENTS.register("amulet_vis", () -> DataComponentType.<VisAmuletData>builder()
            .persistent(VisAmuletData.codec(25000)).networkSynchronized(VisAmuletData.streamCodec(25000)).build());
    public static final RegistrySupplier<ItemAmuletVis> VIS_AMULET_LESSER = ITEMS.register("vis_amulet_lesser", () -> new ItemAmuletVis(props(Rarity.UNCOMMON), true));
    public static final RegistrySupplier<ItemAmuletVis> VIS_AMULET = ITEMS.register("vis_amulet", () -> new ItemAmuletVis(props(Rarity.RARE), false));
    public static final RegistrySupplier<ItemBaubleBlank> MUNDANE_AMULET = blank("mundane_amulet", BaubleType.AMULET);
    public static final RegistrySupplier<ItemBaubleBlank> MUNDANE_RING = blank("mundane_ring", BaubleType.RING);
    public static final RegistrySupplier<ItemBaubleBlank> MUNDANE_BELT = blank("mundane_belt", BaubleType.BELT);
    public static final RegistrySupplier<ItemRunicBauble> RUNIC_RING_LESSER = runic("runic_ring_lesser", BaubleType.RING, 1, ItemRunicBauble.Upgrade.NONE, Rarity.UNCOMMON);
    public static final RegistrySupplier<ItemRunicBauble> RUNIC_RING = runic("runic_ring", BaubleType.RING, 5, ItemRunicBauble.Upgrade.NONE, Rarity.RARE);
    public static final RegistrySupplier<ItemRunicBauble> RUNIC_RING_CHARGED = runic("runic_ring_charged", BaubleType.RING, 4, ItemRunicBauble.Upgrade.CHARGED, Rarity.RARE);
    public static final RegistrySupplier<ItemRunicBauble> RUNIC_RING_HEALING = runic("runic_ring_healing", BaubleType.RING, 4, ItemRunicBauble.Upgrade.HEALING, Rarity.RARE);
    public static final RegistrySupplier<ItemRunicBauble> RUNIC_AMULET = runic("runic_amulet", BaubleType.AMULET, 8, ItemRunicBauble.Upgrade.NONE, Rarity.RARE);
    public static final RegistrySupplier<ItemRunicBauble> RUNIC_AMULET_EMERGENCY = runic("runic_amulet_emergency", BaubleType.AMULET, 7, ItemRunicBauble.Upgrade.EMERGENCY, Rarity.RARE);
    public static final RegistrySupplier<ItemRunicBauble> RUNIC_GIRDLE = runic("runic_girdle", BaubleType.BELT, 10, ItemRunicBauble.Upgrade.NONE, Rarity.RARE);
    public static final RegistrySupplier<ItemRunicBauble> RUNIC_GIRDLE_KINETIC = runic("runic_girdle_kinetic", BaubleType.BELT, 9, ItemRunicBauble.Upgrade.KINETIC, Rarity.RARE);
    public static final java.util.Map<String,RegistrySupplier<ItemBaubleBlank>> DISCOUNT_RINGS=discountRings();
    private static java.util.Map<String,RegistrySupplier<ItemBaubleBlank>> discountRings() {
        var result=new java.util.LinkedHashMap<String,RegistrySupplier<ItemBaubleBlank>>();
        for(var aspect:thaumcraft.api.aspects.Aspect.getPrimalAspects())result.put(aspect.getTag(),ITEMS.register("discount_ring_"+aspect.getTag(),()->new ItemBaubleBlank(props(Rarity.COMMON),BaubleType.RING,aspect)));
        return java.util.Collections.unmodifiableMap(result);
    }
    public static final RegistrySupplier<MenuType<ContainerAccessories>> MENU = MENUS.register("accessories", () -> MenuRegistry.ofExtended(ContainerAccessories::new));
    private static boolean initialized;
    private ThaumcraftBaubles() {}
    private static Item.Properties props(Rarity rarity) { return new Item.Properties().stacksTo(1).rarity(rarity); }
    private static RegistrySupplier<ItemBaubleBlank> blank(String id, BaubleType type) { return ITEMS.register(id, () -> new ItemBaubleBlank(props(Rarity.COMMON), type)); }
    private static RegistrySupplier<ItemRunicBauble> runic(String id, BaubleType type, int charge, ItemRunicBauble.Upgrade upgrade, Rarity rarity) {
        return ITEMS.register(id, () -> new ItemRunicBauble(props(rarity), type, charge, upgrade));
    }
    public static void register() {
        if (initialized) return; initialized = true;
        COMPONENTS.register(); ITEMS.register(); MENUS.register();
        for (RegistrySupplier<? extends Item> item : java.util.List.of(VIS_AMULET_LESSER, VIS_AMULET, MUNDANE_AMULET, MUNDANE_RING, MUNDANE_BELT,
                RUNIC_RING_LESSER, RUNIC_RING, RUNIC_RING_CHARGED, RUNIC_RING_HEALING, RUNIC_AMULET, RUNIC_AMULET_EMERGENCY, RUNIC_GIRDLE, RUNIC_GIRDLE_KINETIC))
            CreativeTabRegistry.append(ThaumcraftContent.TAB, item);
        for(var item:DISCOUNT_RINGS.values())CreativeTabRegistry.append(ThaumcraftContent.TAB,item);
        thaumcraft.common.lib.network.AccessoryNetworking.init();
    }
}
