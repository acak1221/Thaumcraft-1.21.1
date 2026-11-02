package thaumcraft.common.config;

import dev.architectury.registry.CreativeTabRegistry;
import dev.architectury.registry.registries.DeferredRegister;
import dev.architectury.registry.registries.RegistrySupplier;
import java.util.LinkedHashMap;
import java.util.Map;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.storage.loot.functions.LootItemFunctionType;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import thaumcraft.common.Thaumcraft;
import thaumcraft.common.blocks.BlockCustomOre;
import thaumcraft.common.lib.loot.OreDropCount;

/** Ore block/items only: resource drops reuse ThaumcraftContent's existing items. */
public final class ThaumcraftOres {
    public static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(Thaumcraft.MODID, Registries.BLOCK);
    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(Thaumcraft.MODID, Registries.ITEM);
    private static final DeferredRegister<LootItemFunctionType<?>> LOOT_FUNCTIONS = DeferredRegister.create(
            Thaumcraft.MODID, Registries.LOOT_FUNCTION_TYPE);
    public static final RegistrySupplier<LootItemFunctionType<OreDropCount>> ORE_DROP_COUNT = LOOT_FUNCTIONS.register(
            "ore_drop_count", () -> new LootItemFunctionType<>(OreDropCount.CODEC));
    public static final RegistrySupplier<BlockCustomOre> CINNABAR = ore("cinnabar_ore", 0);
    public static final Map<String, RegistrySupplier<BlockCustomOre>> INFUSED = new LinkedHashMap<>();
    /** Modern deep-host counterparts; aspect metadata and shard identities stay the same. */
    public static final Map<String, RegistrySupplier<BlockCustomOre>> DEEPSLATE_INFUSED = new LinkedHashMap<>();
    public static final TagKey<Block> DEEPSLATE_INFUSED_ORES = TagKey.create(Registries.BLOCK,
            ResourceLocation.fromNamespaceAndPath(Thaumcraft.MODID, "deepslate_infused_ores"));

    static {
        String[] tags = {"aer", "ignis", "aqua", "terra", "ordo", "perditio"};
        String[] names = {"air", "fire", "water", "earth", "order", "entropy"};
        for (int i = 0; i < tags.length; i++) {
            INFUSED.put(tags[i], ore(names[i] + "_infused_stone", i + 1));
            DEEPSLATE_INFUSED.put(tags[i], ore("deepslate_" + names[i] + "_infused_stone", i + 1, true));
        }
    }

    public static final RegistrySupplier<BlockCustomOre> AMBER = ore("amber_ore", 7);

    private ThaumcraftOres() {}

    private static RegistrySupplier<BlockCustomOre> ore(String id, int originalVariant) {
        return ore(id, originalVariant, false);
    }

    private static RegistrySupplier<BlockCustomOre> ore(String id, int originalVariant, boolean deepslate) {
        RegistrySupplier<BlockCustomOre> block = BLOCKS.register(id, () -> new BlockCustomOre(originalVariant,
                // Original BlockCustomOre:60 - setResistance(5) stores 15 and the getter divides
                // it by five, so the effective blast resistance is 3.
                BlockBehaviour.Properties.of().mapColor(deepslate ? MapColor.DEEPSLATE : MapColor.STONE)
                        .strength(deepslate ? 3.0F : 1.5F, deepslate ? 6.0F : 3.0F)
                        .sound(deepslate ? SoundType.DEEPSLATE : SoundType.STONE).requiresCorrectToolForDrops()));
        ITEMS.register(id, () -> new BlockItem(block.get(), new Item.Properties()));
        return block;
    }

    public static void register() {
        BLOCKS.register();
        ITEMS.register();
        LOOT_FUNCTIONS.register();
        CreativeTabRegistry.appendStack(ThaumcraftContent.TAB, () -> new ItemStack(CINNABAR.get()));
        for (var ore : INFUSED.values()) CreativeTabRegistry.appendStack(ThaumcraftContent.TAB, () -> new ItemStack(ore.get()));
        for (var ore : DEEPSLATE_INFUSED.values()) CreativeTabRegistry.appendStack(ThaumcraftContent.TAB, () -> new ItemStack(ore.get()));
        CreativeTabRegistry.appendStack(ThaumcraftContent.TAB, () -> new ItemStack(AMBER.get()));
    }
}
