package thaumcraft.common.config;

import dev.architectury.registry.CreativeTabRegistry;
import dev.architectury.registry.menu.MenuRegistry;
import dev.architectury.registry.registries.DeferredRegister;
import dev.architectury.registry.registries.RegistrySupplier;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import thaumcraft.common.Thaumcraft;
import thaumcraft.common.blocks.BlockJar;
import thaumcraft.common.blocks.BlockAuraNode;
import thaumcraft.common.blocks.BlockTable;
import thaumcraft.common.container.ContainerResearchTable;
import thaumcraft.common.items.relics.ItemThaumometer;
import thaumcraft.common.items.relics.ItemThaumonomicon;
import thaumcraft.common.items.relics.ItemResonator;
import thaumcraft.common.entities.EntitySpecialItem;
import thaumcraft.common.entities.EntityInventoryItem;
import thaumcraft.common.items.relics.ScanSession;
import thaumcraft.common.items.ItemEssence;
import thaumcraft.common.items.ItemCrystalEssence;
import thaumcraft.common.items.CrystalEssenceData;
import thaumcraft.common.items.ItemInkwell;
import thaumcraft.common.items.ItemResearchNotes;
import thaumcraft.common.lib.research.ResearchNoteData;
import thaumcraft.common.items.PhialData;
import thaumcraft.common.items.ItemJar;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.common.items.wands.ItemWandCasting;
import thaumcraft.common.items.wands.WandData;
import thaumcraft.common.tiles.TileJar;
import thaumcraft.common.tiles.TileNode;
import thaumcraft.common.tiles.TileTable;
import thaumcraft.common.tiles.TileResearchTable;
import java.util.LinkedHashMap;
import java.util.Map;

public final class ThaumcraftContent {
    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(Thaumcraft.MODID, Registries.ITEM);
    public static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(Thaumcraft.MODID, Registries.BLOCK);
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES = DeferredRegister.create(Thaumcraft.MODID, Registries.BLOCK_ENTITY_TYPE);
    public static final DeferredRegister<EntityType<?>> ENTITIES = DeferredRegister.create(Thaumcraft.MODID, Registries.ENTITY_TYPE);
    public static final DeferredRegister<DataComponentType<?>> COMPONENTS = DeferredRegister.create(Thaumcraft.MODID, Registries.DATA_COMPONENT_TYPE);
    public static final DeferredRegister<CreativeModeTab> TABS = DeferredRegister.create(Thaumcraft.MODID, Registries.CREATIVE_MODE_TAB);
    public static final DeferredRegister<MenuType<?>> MENUS = DeferredRegister.create(Thaumcraft.MODID, Registries.MENU);
    public static final RegistrySupplier<MenuType<ContainerResearchTable>> RESEARCH_TABLE_MENU = MENUS.register("research_table",
            () -> MenuRegistry.ofExtended(ContainerResearchTable::new));

    public static final RegistrySupplier<DataComponentType<WandData>> WAND_DATA = COMPONENTS.register("wand_data",
            () -> DataComponentType.<WandData>builder().persistent(WandData.CODEC).networkSynchronized(WandData.STREAM_CODEC).build());
    public static final RegistrySupplier<DataComponentType<ScanSession>> SCAN_SESSION = COMPONENTS.register("scan_session",
            () -> DataComponentType.<ScanSession>builder().networkSynchronized(ScanSession.STREAM_CODEC).build());
    public static final RegistrySupplier<DataComponentType<PhialData>> PHIAL_DATA = COMPONENTS.register("phial_data",
            () -> DataComponentType.<PhialData>builder().persistent(PhialData.CODEC).networkSynchronized(PhialData.STREAM_CODEC).build());
    public static final RegistrySupplier<DataComponentType<CrystalEssenceData>> CRYSTAL_ESSENCE_DATA = COMPONENTS.register("crystal_essence_data",
            () -> DataComponentType.<CrystalEssenceData>builder().persistent(CrystalEssenceData.CODEC).networkSynchronized(CrystalEssenceData.STREAM_CODEC).build());
    public static final RegistrySupplier<DataComponentType<ResearchNoteData>> RESEARCH_NOTE_DATA = COMPONENTS.register("research_note_data",
            () -> DataComponentType.<ResearchNoteData>builder().persistent(ResearchNoteData.CODEC).networkSynchronized(ResearchNoteData.STREAM_CODEC).build());
    public static final RegistrySupplier<Item> INKWELL = ITEMS.register("inkwell", () -> new ItemInkwell(new Item.Properties()));
    public static final RegistrySupplier<Item> RESEARCH_NOTE = ITEMS.register("research_note", () -> new ItemResearchNotes(new Item.Properties().rarity(Rarity.RARE)));
    public static final RegistrySupplier<Item> DISCOVERY = ITEMS.register("discovery", () -> new ItemResearchNotes(new Item.Properties().rarity(Rarity.EPIC)));
    public static final RegistrySupplier<Item> EMPTY_PHIAL = ITEMS.register("phial_empty", () -> new ItemEssence(new Item.Properties()));
    public static final RegistrySupplier<Item> FILLED_PHIAL = ITEMS.register("phial_filled", () -> new ItemEssence(new Item.Properties().component(PHIAL_DATA.get(), new PhialData("aer"))));
    // Original creative entry is genuinely untyped. Do not install a default
    // component that suppresses native lazy typing or rerolls machine outputs.
    public static final RegistrySupplier<Item> CRYSTAL_ESSENCE = ITEMS.register("crystal_essence",
            () -> new ItemCrystalEssence(new Item.Properties().stacksTo(64)));
    public static final RegistrySupplier<Item> THAUMOMETER = ITEMS.register("thaumometer", () -> new ItemThaumometer(new Item.Properties().stacksTo(1)));
    public static final RegistrySupplier<Item> THAUMONOMICON = ITEMS.register("thaumonomicon", () -> new ItemThaumonomicon(new Item.Properties().stacksTo(1).rarity(Rarity.UNCOMMON)));
    public static final RegistrySupplier<Item> THAUMONOMICON_CHEAT = ITEMS.register("thaumonomicon_cheat", () -> new ItemThaumonomicon(new Item.Properties(), true));
    public static final RegistrySupplier<Item> PRIMORDIAL_PEARL = ITEMS.register("primordial_pearl", () -> new thaumcraft.common.items.ItemPrimordialPearl(new Item.Properties()));
    public static final RegistrySupplier<Item> RESONATOR = ITEMS.register("resonator", () -> new ItemResonator(new Item.Properties()));
    public static final RegistrySupplier<EntityType<EntitySpecialItem>> SPECIAL_ITEM = ENTITIES.register("special_item", () -> EntityType.Builder.<EntitySpecialItem>of(EntitySpecialItem::new, MobCategory.MISC).sized(0.25F, 0.25F).clientTrackingRange(6).updateInterval(20).build("thaumcraft:special_item"));
    public static final RegistrySupplier<EntityType<EntityInventoryItem>> INVENTORY_ITEM = ENTITIES.register("inventory_item", () -> EntityType.Builder.<EntityInventoryItem>of(EntityInventoryItem::new, MobCategory.MISC).sized(0.25F, 0.25F).clientTrackingRange(6).updateInterval(20).build("thaumcraft:inventory_item"));
    public static final RegistrySupplier<Item> IRON_WAND_CAP = ITEMS.register("wand_cap_iron", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> WAND = ITEMS.register("wand", () -> new ItemWandCasting(new Item.Properties().stacksTo(1).component(WAND_DATA.get(), WandData.EMPTY)));
    // Original BlockJar:71 and :299-305 - the ordinary and the void jar emit NO light
    // and the original jar sets no blast resistance at all, so the effective value is 0.
    // Oracle BlockJar:90 - the original calls setLightLevel(0.66f), and 1.7.10 stores
    // (int)(15.0f * 0.66f), which is the light nine of the native block.
    public static final RegistrySupplier<BlockJar> JAR = BLOCKS.register("jar", () -> new BlockJar(BlockBehaviour.Properties.of().mapColor(MapColor.NONE).strength(0.3F, 0.0F).lightLevel(state -> 9).sound(SoundType.GLASS).noOcclusion()));
    public static final RegistrySupplier<BlockJar> VOID_JAR = BLOCKS.register("jar_void", () -> new BlockJar(BlockBehaviour.Properties.of().mapColor(MapColor.NONE).strength(0.3F, 0.0F).lightLevel(state -> 9).sound(SoundType.GLASS).noOcclusion(), true));
    public static final RegistrySupplier<BlockAuraNode> AURA_NODE = BLOCKS.register("aura_node", () -> new BlockAuraNode(BlockBehaviour.Properties.of().strength(-1.0F, 3600000.0F).noOcclusion().noCollission().lightLevel(state -> 8)));
    public static final RegistrySupplier<thaumcraft.common.blocks.BlockSilverwoodNode> SILVERWOOD_KNOT = BLOCKS.register("silverwood_knot",
            () -> new thaumcraft.common.blocks.BlockSilverwoodNode(BlockBehaviour.Properties.of().mapColor(MapColor.QUARTZ).strength(2.5F).sound(SoundType.WOOD).lightLevel(state -> 7)));
    public static final RegistrySupplier<BlockTable> TABLE = BLOCKS.register("table", () -> new BlockTable(
            BlockBehaviour.Properties.of().mapColor(MapColor.WOOD).strength(2.5F).sound(SoundType.WOOD).noOcclusion()));
    public static final RegistrySupplier<Item> TABLE_ITEM = ITEMS.register("table", () -> new BlockItem(TABLE.get(), new Item.Properties()));
    public static final RegistrySupplier<Item> RESEARCH_TABLE_ITEM = ITEMS.register("research_table", () -> new BlockItem(TABLE.get(), new Item.Properties()) {
        @Override public String getDescriptionId() { return "item.thaumcraft.research_table"; }
        @Override public void registerBlocks(Map<Block, Item> blocks, Item item) {
            // A second flattened pick presentation must not replace the normal
            // table in Block.asItem(). Both items still place the base state.
        }
    });
    public static final RegistrySupplier<BlockEntityType<TileTable>> TABLE_BLOCK_ENTITY = BLOCK_ENTITIES.register("table",
            () -> BlockEntityType.Builder.of(TileTable::new, TABLE.get()).build(null));
    public static final RegistrySupplier<BlockEntityType<TileResearchTable>> RESEARCH_TABLE_BLOCK_ENTITY = BLOCK_ENTITIES.register("research_table",
            () -> BlockEntityType.Builder.of(TileResearchTable::new, TABLE.get()).build(null));
    public static final RegistrySupplier<BlockEntityType<TileJar>> JAR_BLOCK_ENTITY = BLOCK_ENTITIES.register("jar", () -> BlockEntityType.Builder.of((pos, state) -> new TileJar(ThaumcraftContent.JAR_BLOCK_ENTITY.get(), pos, state), JAR.get(), VOID_JAR.get()).build(null));
    public static final RegistrySupplier<BlockEntityType<TileNode>> NODE_BLOCK_ENTITY = BLOCK_ENTITIES.register("aura_node", () -> BlockEntityType.Builder.of((pos, state) -> new TileNode(ThaumcraftContent.NODE_BLOCK_ENTITY.get(), pos, state), AURA_NODE.get(), SILVERWOOD_KNOT.get()).build(null));
    public static final Map<String, RegistrySupplier<Item>> MATERIALS = new LinkedHashMap<>();
    public static final Map<String, RegistrySupplier<Item>> SHARDS = new LinkedHashMap<>();
    public static final Map<String, RegistrySupplier<Block>> CRYSTALS = new LinkedHashMap<>();
    public static final RegistrySupplier<CreativeModeTab> TAB = TABS.register("thaumcraft", () -> CreativeTabRegistry.create(Component.translatable("itemGroup.thaumcraft"), () -> new ItemStack(THAUMOMETER.get())));

    static {
        // Original ItemNugget metadata5 / ConfigRecipes:283,404-405. This is
        // one ninth of a resource quicksilver, not a whole-quicksilver alias.
        MATERIALS.put("quicksilver_drop", ITEMS.register("quicksilver_drop",
                () -> new thaumcraft.common.items.ItemNugget(new Item.Properties(), thaumcraft.common.lib.items.EquipmentRules.NUGGET_QUICKSILVER)));
        // Original ItemNugget metadata 1: the copper nugget. The original registers the item and its
        // icon unconditionally (ItemNugget:46-49) and only gates the creative listing and the
        // crucible recipes on Config.foundCopperIngot. Copper is vanilla in 1.21.1, so the item and
        // its ore dictionary name "nuggetCopper" (ConfigItems:277) are carried by this port.
        MATERIALS.put("copper_nugget", ITEMS.register("copper_nugget",
                () -> new thaumcraft.common.items.ItemNugget(new Item.Properties(), thaumcraft.common.lib.items.EquipmentRules.NUGGET_COPPER)));
        // Original ItemNugget metadata 16 (iron), 17 (copper), 31 (gold) and 21 (cinnabar): the "Native
        // Clusters" that purified ore becomes (ConfigRecipes:131-134) and that a furnace melts into two
        // ingots (ConfigRecipes:488-490). Copper is vanilla in 1.21.1, so its cluster is carried too.
        String[] clusterIds = {"cluster_iron", "cluster_gold", "cluster_cinnabar", "cluster_copper"};
        int[] clusterTypes = {thaumcraft.common.lib.items.EquipmentRules.NUGGET_CLUSTER_IRON,
                thaumcraft.common.lib.items.EquipmentRules.NUGGET_CLUSTER_GOLD,
                thaumcraft.common.lib.items.EquipmentRules.NUGGET_CLUSTER_CINNABAR,
                thaumcraft.common.lib.items.EquipmentRules.NUGGET_CLUSTER_COPPER};
        for (int cluster = 0; cluster < clusterIds.length; cluster++) {
            String id = clusterIds[cluster];
            int type = clusterTypes[cluster];
            MATERIALS.put(id, ITEMS.register(id, () -> new thaumcraft.common.items.ItemNugget(new Item.Properties(), type)));
        }
        ITEMS.register("jar", () -> new ItemJar(JAR.get(), new Item.Properties()));
        ITEMS.register("jar_void", () -> new ItemJar(VOID_JAR.get(), new Item.Properties()));
        ITEMS.register("aura_node", () -> new BlockItem(AURA_NODE.get(), new Item.Properties()));
        // This metadata flattening is explicit; behavioral resource items remain
        // pending in MIGRATION_STATUS rather than being counted as complete units.
        for (String id : new String[]{"alumentum", "nitor", "thaumium_ingot", "quick_silver", "magic_tallow", "zombie_brain", "amber_gem", "enchanted_fabric", "vis_filter", "knowledge_fragment", "mirrored_glass", "tainted_goo", "taint_tendril", "jar_label", "salis_mundus", "primal_charm", "void_ingot", "void_seed", "gold_coin"}) {
            MATERIALS.put(id, ITEMS.register(id, () -> switch (id) {
                case "nitor" -> new thaumcraft.common.items.ItemNitor(new Item.Properties());
                case "alumentum" -> new thaumcraft.common.items.ItemAlumentum(new Item.Properties());
                case "zombie_brain" -> new thaumcraft.common.items.ItemZombieBrain(new Item.Properties());
                case "jar_label" -> new thaumcraft.common.items.ItemJarLabel(new Item.Properties());
                case "primal_charm" -> new thaumcraft.common.items.ItemPrimalCharm(new Item.Properties());
                case "knowledge_fragment" -> new thaumcraft.common.items.ItemKnowledgeFragment(new Item.Properties());
                // Original ItemResource metadata 11 and 12: the tainted goo and the taint tendril poison their carrier.
                case "tainted_goo" -> new thaumcraft.common.items.ItemResource(new Item.Properties(), thaumcraft.common.lib.items.EquipmentRules.RESOURCE_TAINTED_GOO);
                case "taint_tendril" -> new thaumcraft.common.items.ItemResource(new Item.Properties(), thaumcraft.common.lib.items.EquipmentRules.RESOURCE_TAINT_TENDRIL);
                default -> new Item(new Item.Properties());
            }));
        }
        String[] tags = {"aer", "ignis", "aqua", "terra", "ordo", "perditio", "balanced"};
        String[] names = {"air", "fire", "water", "earth", "order", "entropy", "balance"};
        for (int i = 0; i < names.length; i++) {
            String id = names[i] + "_shard";
            // Original ItemShard metadata 0..6: the shard of the element.
            int shardType = i;
            SHARDS.put(tags[i], ITEMS.register(id, () -> new thaumcraft.common.items.ItemShard(new Item.Properties(), shardType)));
            if (i < 6) {
                String crystalId = "crystal_" + names[i];
                RegistrySupplier<Block> block = BLOCKS.register(crystalId, () -> new thaumcraft.common.blocks.BlockCrystal(ThaumcraftCrystalClusters.properties()));
                CRYSTALS.put(tags[i], block);
                ITEMS.register(crystalId, () -> new thaumcraft.common.blocks.BlockCrystalItem(block.get(), new Item.Properties()));
            }
        }
    }

    private ThaumcraftContent() {}
    public static Item crystalFor(String tag) {
        RegistrySupplier<Item> shard = SHARDS.get(tag);
        return shard == null ? null : shard.get();
    }
    public static String aspectForCrystal(Item item) {
        for (var entry : SHARDS.entrySet()) if (entry.getValue().get() == item) return entry.getKey();
        for (var entry : CRYSTALS.entrySet()) if (entry.getValue().get().asItem() == item) return entry.getKey();
        return null;
    }

    public static void register() {
        COMPONENTS.register(); BLOCKS.register(); ITEMS.register(); BLOCK_ENTITIES.register(); ENTITIES.register(); TABS.register(); MENUS.register();
        CreativeTabRegistry.append(TAB, THAUMOMETER, WAND, THAUMONOMICON, RESONATOR, PRIMORDIAL_PEARL);
        ThaumcraftCreativeTabs.publish(TAB, () -> ThaumcraftSettings.current().cheatSheet()
                ? java.util.List.of(new ItemStack(THAUMONOMICON_CHEAT.get())) : java.util.List.of());
        CreativeTabRegistry.append(TAB, INKWELL);
        CreativeTabRegistry.append(TAB, TABLE_ITEM);
        CreativeTabRegistry.append(TAB, EMPTY_PHIAL);
        CreativeTabRegistry.append(TAB, CRYSTAL_ESSENCE);
        CreativeTabRegistry.append(TAB, IRON_WAND_CAP);
        for (Aspect aspect : Aspect.aspects.values()) CreativeTabRegistry.appendStack(TAB, () -> ItemEssence.filled(aspect.getTag()));
        CreativeTabRegistry.appendStack(TAB, () -> new ItemStack(JAR.get()), () -> new ItemStack(VOID_JAR.get()), () -> new ItemStack(AURA_NODE.get()));
        for (RegistrySupplier<Item> item : MATERIALS.values()) CreativeTabRegistry.append(TAB, item);
        for (RegistrySupplier<Item> item : SHARDS.values()) CreativeTabRegistry.append(TAB, item);
        for (RegistrySupplier<Block> block : CRYSTALS.values()) CreativeTabRegistry.appendStack(TAB, () -> new ItemStack(block.get()));
    }
}
