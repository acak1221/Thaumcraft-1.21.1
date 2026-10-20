package thaumcraft.common.config;

import dev.architectury.event.events.common.LifecycleEvent;
import dev.architectury.registry.CreativeTabRegistry;
import dev.architectury.registry.registries.DeferredRegister;
import dev.architectury.registry.registries.RegistrySupplier;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import thaumcraft.common.Thaumcraft;
import thaumcraft.common.blocks.BlockInfusionMatrix;
import thaumcraft.common.blocks.BlockInfusionPillar;
import thaumcraft.common.blocks.BlockPedestal;
import thaumcraft.common.blocks.BlockTallowCandle;
import thaumcraft.common.crafting.InfusionStackRecipe;
import thaumcraft.common.crafting.InfusionEnchantmentRecipe;
import thaumcraft.common.lib.events.EssentiaHandler;
import thaumcraft.common.lib.potions.PotionFluxTaint;
import thaumcraft.common.tiles.TileInfusionMatrix;
import thaumcraft.common.tiles.TileInfusionPillar;
import thaumcraft.common.tiles.TilePedestal;

/** Original StoneDevice1..4 and white tallow candle; STACK recipes only at this stage. */
public final class ThaumcraftInfusion {
    private static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(Thaumcraft.MODID, Registries.BLOCK);
    private static final DeferredRegister<Item> ITEMS = DeferredRegister.create(Thaumcraft.MODID, Registries.ITEM);
    private static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES = DeferredRegister.create(Thaumcraft.MODID, Registries.BLOCK_ENTITY_TYPE);
    private static final DeferredRegister<RecipeType<?>> RECIPE_TYPES = DeferredRegister.create(Thaumcraft.MODID, Registries.RECIPE_TYPE);
    private static final DeferredRegister<RecipeSerializer<?>> SERIALIZERS = DeferredRegister.create(Thaumcraft.MODID, Registries.RECIPE_SERIALIZER);
    private static final DeferredRegister<MobEffect> EFFECTS = DeferredRegister.create(Thaumcraft.MODID, Registries.MOB_EFFECT);

    // Original setResistance(25) exposes native effective15, not25.
    private static BlockBehaviour.Properties stone() {
        return BlockBehaviour.Properties.of().mapColor(MapColor.STONE).strength(3, 15)
                .sound(SoundType.STONE).requiresCorrectToolForDrops().noOcclusion();
    }

    public static final RegistrySupplier<BlockInfusionMatrix> MATRIX = BLOCKS.register("infusion_matrix",
            () -> new BlockInfusionMatrix(stone().lightLevel(state -> 10)));
    public static final RegistrySupplier<BlockPedestal> PEDESTAL = BLOCKS.register("arcane_pedestal", () -> new BlockPedestal(stone()));
    public static final RegistrySupplier<BlockInfusionPillar> PILLAR = BLOCKS.register("infusion_pillar", () -> new BlockInfusionPillar(stone()));
    public static final RegistrySupplier<BlockTallowCandle> TALLOW_CANDLE = BLOCKS.register("tallow_candle",
            () -> new BlockTallowCandle(BlockBehaviour.Properties.of().mapColor(MapColor.SNOW)));
    public static final RegistrySupplier<Item> MATRIX_ITEM = ITEMS.register("infusion_matrix", () -> new BlockItem(MATRIX.get(), new Item.Properties()));
    public static final RegistrySupplier<Item> PEDESTAL_ITEM = ITEMS.register("arcane_pedestal", () -> new BlockItem(PEDESTAL.get(), new Item.Properties()));
    public static final RegistrySupplier<Item> TALLOW_CANDLE_ITEM = ITEMS.register("tallow_candle", () -> new BlockItem(TALLOW_CANDLE.get(), new Item.Properties()));
    // Pillars are formation states and drop their original stone/brick, not a new obtainable item.
    public static final RegistrySupplier<BlockEntityType<TileInfusionMatrix>> MATRIX_BLOCK_ENTITY = BLOCK_ENTITIES.register("infusion_matrix",
            () -> BlockEntityType.Builder.of(TileInfusionMatrix::new, MATRIX.get()).build(null));
    public static final RegistrySupplier<BlockEntityType<TilePedestal>> PEDESTAL_BLOCK_ENTITY = BLOCK_ENTITIES.register("arcane_pedestal",
            () -> BlockEntityType.Builder.of(TilePedestal::new, PEDESTAL.get()).build(null));
    public static final RegistrySupplier<BlockEntityType<TileInfusionPillar>> PILLAR_BLOCK_ENTITY = BLOCK_ENTITIES.register("infusion_pillar",
            () -> BlockEntityType.Builder.of(TileInfusionPillar::new, PILLAR.get()).build(null));
    public static final RegistrySupplier<RecipeType<InfusionStackRecipe>> INFUSION_RECIPE_TYPE = RECIPE_TYPES.register("infusion_stack",
            () -> new RecipeType<InfusionStackRecipe>() {
                @Override public String toString() { return "thaumcraft:infusion_stack"; }
            });
    public static final RegistrySupplier<RecipeSerializer<InfusionStackRecipe>> INFUSION_RECIPE_SERIALIZER = SERIALIZERS.register("infusion_stack",
            () -> InfusionStackRecipe.SERIALIZER);
    public static final RegistrySupplier<RecipeType<InfusionEnchantmentRecipe>> ENCHANTMENT_RECIPE_TYPE = RECIPE_TYPES.register("infusion_enchantment",
            () -> new RecipeType<InfusionEnchantmentRecipe>() {
                @Override public String toString() { return "thaumcraft:infusion_enchantment"; }
            });
    public static final RegistrySupplier<RecipeSerializer<InfusionEnchantmentRecipe>> ENCHANTMENT_RECIPE_SERIALIZER = SERIALIZERS.register("infusion_enchantment",
            () -> InfusionEnchantmentRecipe.SERIALIZER);
    public static final RegistrySupplier<MobEffect> FLUX_TAINT = EFFECTS.register("flux_taint", PotionFluxTaint::new);
    private static boolean registered;
    private ThaumcraftInfusion() {}

    public static void register() {
        if (registered) return;
        registered = true;
        BLOCKS.register();
        ITEMS.register();
        BLOCK_ENTITIES.register();
        RECIPE_TYPES.register();
        SERIALIZERS.register();
        EFFECTS.register();
        LifecycleEvent.SERVER_STOPPED.register(server -> EssentiaHandler.clear());
        CreativeTabRegistry.append(ThaumcraftContent.TAB, MATRIX_ITEM, PEDESTAL_ITEM, TALLOW_CANDLE_ITEM);
    }
}
