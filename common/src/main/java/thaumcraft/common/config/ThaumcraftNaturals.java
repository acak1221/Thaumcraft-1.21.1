package thaumcraft.common.config;

import dev.architectury.event.events.common.LifecycleEvent;
import dev.architectury.registry.CreativeTabRegistry;
import dev.architectury.registry.registries.DeferredRegister;
import dev.architectury.registry.registries.RegistrySupplier;
import java.util.List;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.FireBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.placement.PlacedFeature;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.storage.loot.functions.LootItemFunctionType;
import thaumcraft.common.Thaumcraft;
import thaumcraft.common.blocks.BlockMagicalLeaves;
import thaumcraft.common.blocks.BlockMagicalLog;
import thaumcraft.common.blocks.BlockMagicalSapling;
import thaumcraft.common.lib.world.ThaumcraftTreeFeature;

/** TC4 magical wood, leaves and saplings; no node registration or retrogen. */
public final class ThaumcraftNaturals {
    private static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(Thaumcraft.MODID, Registries.BLOCK);
    private static final DeferredRegister<Item> ITEMS = DeferredRegister.create(Thaumcraft.MODID, Registries.ITEM);
    private static final DeferredRegister<Feature<?>> FEATURES = DeferredRegister.create(Thaumcraft.MODID, Registries.FEATURE);
    private static final DeferredRegister<LootItemFunctionType<?>> LOOT_FUNCTIONS = DeferredRegister.create(
            Thaumcraft.MODID, Registries.LOOT_FUNCTION_TYPE);

    public static final RegistrySupplier<BlockMagicalLog> GREATWOOD_LOG = log("greatwood_log", MapColor.COLOR_BROWN);
    public static final RegistrySupplier<BlockMagicalLog> SILVERWOOD_LOG = log("silverwood_log", MapColor.QUARTZ);
    public static final RegistrySupplier<Block> GREATWOOD_PLANKS = planks("greatwood_planks", MapColor.COLOR_BROWN);
    public static final RegistrySupplier<Block> SILVERWOOD_PLANKS = planks("silverwood_planks", MapColor.QUARTZ);
    public static final RegistrySupplier<BlockMagicalLeaves> GREATWOOD_LEAVES = leaves("greatwood_leaves", false);
    public static final RegistrySupplier<BlockMagicalLeaves> SILVERWOOD_LEAVES = leaves("silverwood_leaves", true);
    public static final RegistrySupplier<BlockMagicalSapling> GREATWOOD_SAPLING = sapling("greatwood_sapling", false);
    public static final RegistrySupplier<BlockMagicalSapling> SILVERWOOD_SAPLING = sapling("silverwood_sapling", true);

    public static final RegistrySupplier<LootItemFunctionType<BlockMagicalLeaves.SaplingChance>> LEAF_SAPLING_CHANCE =
            LOOT_FUNCTIONS.register("magical_leaf_sapling_chance", () -> new LootItemFunctionType<>(BlockMagicalLeaves.SaplingChance.CODEC));
    public static final RegistrySupplier<ThaumcraftTreeFeature> TREES = FEATURES.register("tc4_magical_trees", ThaumcraftTreeFeature::new);
    public static final ResourceKey<PlacedFeature> TREES_KEY = ResourceKey.create(Registries.PLACED_FEATURE, id("tc4_magical_trees"));
    public static final TagKey<Biome> TREE_BIOMES = TagKey.create(Registries.BIOME, id("magical_tree_biomes"));

    private ThaumcraftNaturals() {}

    private static RegistrySupplier<BlockMagicalLog> log(String id, MapColor color) {
        var block = BLOCKS.register(id, () -> new BlockMagicalLog(BlockBehaviour.Properties.of()
                .mapColor(color).strength(2.5F).sound(SoundType.WOOD)));
        ITEMS.register(id, () -> new BlockItem(block.get(), new Item.Properties()));
        return block;
    }

    private static RegistrySupplier<Block> planks(String id, MapColor color) {
        var block = BLOCKS.register(id, () -> new Block(BlockBehaviour.Properties.of()
                .mapColor(color).strength(2.5F, 6.0F).sound(SoundType.WOOD)));
        ITEMS.register(id, () -> new BlockItem(block.get(), new Item.Properties()));
        return block;
    }

    private static RegistrySupplier<BlockMagicalLeaves> leaves(String id, boolean silverwood) {
        var block = BLOCKS.register(id, () -> new BlockMagicalLeaves(silverwood, BlockBehaviour.Properties.ofFullCopy(Blocks.OAK_LEAVES)
                .lightLevel(state -> silverwood ? 7 : 0)));
        ITEMS.register(id, () -> new BlockItem(block.get(), new Item.Properties()));
        return block;
    }

    private static RegistrySupplier<BlockMagicalSapling> sapling(String id, boolean silverwood) {
        var block = BLOCKS.register(id, () -> new BlockMagicalSapling(silverwood, BlockBehaviour.Properties.of()
                .mapColor(MapColor.PLANT).noCollission().instabreak().sound(SoundType.GRASS).randomTicks()
                .lightLevel(state -> silverwood ? 8 : 0)));
        ITEMS.register(id, () -> new BlockMagicalSapling.SaplingItem(block.get(), new Item.Properties()));
        return block;
    }

    public static void register() {
        BLOCKS.register();
        ITEMS.register();
        FEATURES.register();
        LOOT_FUNCTIONS.register();
        List<RegistrySupplier<? extends Block>> all = List.of(GREATWOOD_LOG, SILVERWOOD_LOG, GREATWOOD_PLANKS,
                SILVERWOOD_PLANKS, GREATWOOD_LEAVES, SILVERWOOD_LEAVES, GREATWOOD_SAPLING, SILVERWOOD_SAPLING);
        for (var block : all) CreativeTabRegistry.appendStack(ThaumcraftContent.TAB, () -> new ItemStack(block.get()));
        LifecycleEvent.SETUP.register(() -> {
            FireBlock fire = (FireBlock) Blocks.FIRE;
            // FireBlock takes spread/encouragement first, flammability second.
            fire.setFlammable(GREATWOOD_LOG.get(), 5, 5);
            fire.setFlammable(SILVERWOOD_LOG.get(), 5, 5);
            fire.setFlammable(ThaumcraftContent.SILVERWOOD_KNOT.get(), 5, 5);
            fire.setFlammable(GREATWOOD_PLANKS.get(), 5, 20);
            fire.setFlammable(SILVERWOOD_PLANKS.get(), 5, 20);
            fire.setFlammable(GREATWOOD_LEAVES.get(), 30, 60);
            fire.setFlammable(SILVERWOOD_LEAVES.get(), 30, 60);
            fire.setFlammable(GREATWOOD_SAPLING.get(), 60, 100);
            fire.setFlammable(SILVERWOOD_SAPLING.get(), 60, 100);
        });
    }

    private static ResourceLocation id(String path) { return ResourceLocation.fromNamespaceAndPath(Thaumcraft.MODID, path); }
}
