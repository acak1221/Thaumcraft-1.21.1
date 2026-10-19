package thaumcraft.common.config;

import dev.architectury.event.events.common.TickEvent;
import dev.architectury.registry.CreativeTabRegistry;
import dev.architectury.registry.registries.DeferredRegister;
import dev.architectury.registry.registries.RegistrySupplier;
import net.minecraft.core.registries.Registries;
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
import thaumcraft.common.blocks.BlockCrucible;
import thaumcraft.common.crafting.CrucibleRecipe;
import thaumcraft.common.tiles.TileCrucible;

/** Native crucible slice. Resource outputs reuse ThaumcraftContent.MATERIALS. */
public final class ThaumcraftAlchemy {
    private static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(Thaumcraft.MODID, Registries.BLOCK);
    private static final DeferredRegister<Item> ITEMS = DeferredRegister.create(Thaumcraft.MODID, Registries.ITEM);
    private static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES = DeferredRegister.create(Thaumcraft.MODID, Registries.BLOCK_ENTITY_TYPE);
    private static final DeferredRegister<RecipeType<?>> RECIPE_TYPES = DeferredRegister.create(Thaumcraft.MODID, Registries.RECIPE_TYPE);
    private static final DeferredRegister<RecipeSerializer<?>> SERIALIZERS = DeferredRegister.create(Thaumcraft.MODID, Registries.RECIPE_SERIALIZER);

    public static final RegistrySupplier<BlockCrucible> CRUCIBLE = BLOCKS.register("crucible",
            () -> new BlockCrucible(BlockBehaviour.Properties.of().mapColor(MapColor.METAL)
                    .strength(3, 17).sound(SoundType.METAL).noOcclusion()));
    public static final RegistrySupplier<Item> CRUCIBLE_ITEM = ITEMS.register("crucible",
            () -> new BlockItem(CRUCIBLE.get(), new Item.Properties()));
    public static final RegistrySupplier<BlockEntityType<TileCrucible>> CRUCIBLE_BLOCK_ENTITY = BLOCK_ENTITIES.register("crucible",
            () -> BlockEntityType.Builder.of(TileCrucible::new, CRUCIBLE.get()).build(null));
    public static final RegistrySupplier<RecipeType<CrucibleRecipe>> CRUCIBLE_RECIPE_TYPE = RECIPE_TYPES.register("crucible",
            () -> new RecipeType<CrucibleRecipe>() {
                @Override public String toString() { return "thaumcraft:crucible"; }
            });
    public static final RegistrySupplier<RecipeSerializer<CrucibleRecipe>> CRUCIBLE_SERIALIZER = SERIALIZERS.register("crucible",
            CrucibleRecipe.Serializer::new);

    private ThaumcraftAlchemy() {}

    public static void register() {
        BLOCKS.register();
        ITEMS.register();
        BLOCK_ENTITIES.register();
        RECIPE_TYPES.register();
        SERIALIZERS.register();
        CreativeTabRegistry.append(ThaumcraftContent.TAB, CRUCIBLE_ITEM);
        TickEvent.SERVER_LEVEL_POST.register(TileCrucible::tickOutputs);
    }
}
