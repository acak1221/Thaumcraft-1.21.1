package thaumcraft.common.config;

import dev.architectury.registry.CreativeTabRegistry;
import dev.architectury.registry.menu.MenuRegistry;
import dev.architectury.registry.registries.DeferredRegister;
import dev.architectury.registry.registries.RegistrySupplier;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.inventory.MenuType;
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
import thaumcraft.common.blocks.BlockArcaneWorkbench;
import thaumcraft.common.container.ContainerArcaneWorkbench;
import thaumcraft.common.crafting.ArcaneShapedRecipe;
import thaumcraft.common.crafting.ArcaneShapelessRecipe;
import thaumcraft.common.crafting.NativeArcaneRecipe;
import thaumcraft.common.tiles.TileArcaneWorkbench;

/** Independently registered native arcane-workbench slice. Call after ThaumcraftContent.register(). */
public final class ThaumcraftArcane {
    private static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(Thaumcraft.MODID, Registries.BLOCK);
    private static final DeferredRegister<Item> ITEMS = DeferredRegister.create(Thaumcraft.MODID, Registries.ITEM);
    private static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES = DeferredRegister.create(Thaumcraft.MODID, Registries.BLOCK_ENTITY_TYPE);
    private static final DeferredRegister<MenuType<?>> MENUS = DeferredRegister.create(Thaumcraft.MODID, Registries.MENU);
    private static final DeferredRegister<RecipeType<?>> RECIPE_TYPES = DeferredRegister.create(Thaumcraft.MODID, Registries.RECIPE_TYPE);
    private static final DeferredRegister<RecipeSerializer<?>> SERIALIZERS = DeferredRegister.create(Thaumcraft.MODID, Registries.RECIPE_SERIALIZER);

    public static final RegistrySupplier<BlockArcaneWorkbench> ARCANE_WORKBENCH = BLOCKS.register("arcane_workbench",
            () -> new BlockArcaneWorkbench(BlockBehaviour.Properties.of().mapColor(MapColor.WOOD)
                    .strength(2.5F).sound(SoundType.WOOD).noOcclusion()));
    public static final RegistrySupplier<Item> ARCANE_WORKBENCH_ITEM = ITEMS.register("arcane_workbench",
            () -> new BlockItem(ARCANE_WORKBENCH.get(), new Item.Properties()));
    public static final RegistrySupplier<BlockEntityType<TileArcaneWorkbench>> ARCANE_WORKBENCH_BLOCK_ENTITY = BLOCK_ENTITIES.register("arcane_workbench",
            () -> BlockEntityType.Builder.of(TileArcaneWorkbench::new, ARCANE_WORKBENCH.get()).build(null));
    public static final RegistrySupplier<MenuType<ContainerArcaneWorkbench>> ARCANE_WORKBENCH_MENU = MENUS.register("arcane_workbench",
            () -> MenuRegistry.ofExtended(ContainerArcaneWorkbench::new));
    public static final RegistrySupplier<RecipeType<NativeArcaneRecipe>> ARCANE_RECIPE_TYPE = RECIPE_TYPES.register("arcane",
            () -> new RecipeType<NativeArcaneRecipe>() {
                @Override public String toString() { return "thaumcraft:arcane"; }
            });
    public static final RegistrySupplier<RecipeSerializer<ArcaneShapedRecipe>> ARCANE_SHAPED_SERIALIZER = SERIALIZERS.register("arcane_shaped",
            ArcaneShapedRecipe.Serializer::new);
    public static final RegistrySupplier<RecipeSerializer<ArcaneShapelessRecipe>> ARCANE_SHAPELESS_SERIALIZER = SERIALIZERS.register("arcane_shapeless",
            ArcaneShapelessRecipe.Serializer::new);

    private ThaumcraftArcane() {}

    public static void register() {
        BLOCKS.register();
        ITEMS.register();
        BLOCK_ENTITIES.register();
        MENUS.register();
        RECIPE_TYPES.register();
        SERIALIZERS.register();
        CreativeTabRegistry.append(ThaumcraftContent.TAB, ARCANE_WORKBENCH_ITEM);
    }
}
