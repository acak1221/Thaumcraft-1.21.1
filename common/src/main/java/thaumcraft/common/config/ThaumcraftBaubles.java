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
    public static final RegistrySupplier<thaumcraft.common.items.baubles.ItemGirdleHover> HOVER_GIRDLE = ITEMS.register("hover_girdle", () -> new thaumcraft.common.items.baubles.ItemGirdleHover(props(Rarity.RARE)));
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
    /** The three blank baubles the original creative tab showed, by their original flattened id. */
    public static final java.util.Map<String,RegistrySupplier<ItemBaubleBlank>> MUNDANE_BLANKS=java.util.Map.of(
            "mundane_amulet",MUNDANE_AMULET,"mundane_ring",MUNDANE_RING,"mundane_belt",MUNDANE_BELT);
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
        for (RegistrySupplier<? extends Item> item : java.util.List.of(VIS_AMULET_LESSER, VIS_AMULET, HOVER_GIRDLE,
                RUNIC_RING_LESSER, RUNIC_RING, RUNIC_RING_CHARGED, RUNIC_RING_HEALING, RUNIC_AMULET, RUNIC_AMULET_EMERGENCY, RUNIC_GIRDLE, RUNIC_GIRDLE_KINETIC))
            CreativeTabRegistry.append(ThaumcraftContent.TAB, item);
        // The discount rings are NOT in the creative tab. The original ItemBaubleBlanks is one item with the
        // metadata 0 the amulet, 1 the ring, 2 the belt and 3..8 the six discount rings, and its
        // func_150895_a (getSubItems) lists ONLY the metadata 0, 1 and 2 (oracle
        // thaumcraft/common/items/baubles/ItemBaubleBlanks.java:73-77). The discount rings reach the player
        // through the loot bags (oracle Config.java:536-538 and 572) and through the wizard trade (oracle
        // VillageWizardManager.java:51), never through the tab.
        for (var blank : thaumcraft.common.lib.baubles.RunicBaubleRules.creativeTabBlanks()) {
            var supplier = MUNDANE_BLANKS.get(blank.id());
            if (supplier == null) throw new IllegalStateException("The original creative tab blank is not registered: " + blank.id());
            CreativeTabRegistry.append(ThaumcraftContent.TAB, supplier);
        }
        thaumcraft.common.lib.network.AccessoryNetworking.init();
    }
}
