package thaumcraft.common.config;

import dev.architectury.registry.CreativeTabRegistry;
import dev.architectury.registry.menu.MenuRegistry;
import dev.architectury.registry.registries.DeferredRegister;
import dev.architectury.registry.registries.RegistrySupplier;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import thaumcraft.common.Thaumcraft;
import thaumcraft.common.blocks.BlockAlchemyFurnace;
import thaumcraft.common.container.ContainerAlchemyFurnace;
import thaumcraft.common.tiles.TileAlchemyFurnace;

/** Normal furnace and plain metalDevice:9 construct; no advanced machine placeholders. */
public final class ThaumcraftDistillation {
    private static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(Thaumcraft.MODID, Registries.BLOCK);
    private static final DeferredRegister<Item> ITEMS = DeferredRegister.create(Thaumcraft.MODID, Registries.ITEM);
    private static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES = DeferredRegister.create(Thaumcraft.MODID, Registries.BLOCK_ENTITY_TYPE);
    private static final DeferredRegister<MenuType<?>> MENUS = DeferredRegister.create(Thaumcraft.MODID, Registries.MENU);

    public static final RegistrySupplier<BlockAlchemyFurnace> ALCHEMY_FURNACE = BLOCKS.register("alchemy_furnace",
            () -> new BlockAlchemyFurnace(BlockBehaviour.Properties.of().mapColor(MapColor.STONE)
                    // Original BlockAlchemyFurnace:59 - setResistance(17) stores 51 and the getter
                    // divides it by five, so the effective blast resistance is 10.2.
                    .strength(3, 10.2F).sound(SoundType.STONE).noOcclusion()
                    .lightLevel(state -> state.getValue(BlockAlchemyFurnace.LIT) ? 12 : 0)));
    public static final RegistrySupplier<Item> ALCHEMY_FURNACE_ITEM = ITEMS.register("alchemy_furnace",
            () -> new BlockItem(ALCHEMY_FURNACE.get(), new Item.Properties()));
    public static final RegistrySupplier<BlockEntityType<TileAlchemyFurnace>> ALCHEMY_FURNACE_BLOCK_ENTITY = BLOCK_ENTITIES.register("alchemy_furnace",
            () -> BlockEntityType.Builder.of(TileAlchemyFurnace::new, ALCHEMY_FURNACE.get()).build(null));
    public static final RegistrySupplier<MenuType<ContainerAlchemyFurnace>> ALCHEMY_FURNACE_MENU = MENUS.register("alchemy_furnace",
            () -> MenuRegistry.ofExtended(ContainerAlchemyFurnace::new));
    public static final RegistrySupplier<Block> ALCHEMICAL_CONSTRUCT = BLOCKS.register("alchemical_construct",
            // Original metadata9 is an ordinary full-collision decorative
            // component with NO actual BE, menu, ticker or required tool tier.
            () -> new Block(BlockBehaviour.Properties.of().mapColor(MapColor.METAL)
                    .strength(3, 10.2F).sound(SoundType.METAL).noOcclusion()));
    public static final RegistrySupplier<Item> ALCHEMICAL_CONSTRUCT_ITEM = ITEMS.register("alchemical_construct",
            () -> new BlockItem(ALCHEMICAL_CONSTRUCT.get(), new Item.Properties()));

    private ThaumcraftDistillation() {}

    /** Called by the common bootstrap after ThaumcraftContent.register(). */
    public static void register() {
        BLOCKS.register();
        ITEMS.register();
        BLOCK_ENTITIES.register();
        MENUS.register();
        CreativeTabRegistry.append(ThaumcraftContent.TAB, ALCHEMY_FURNACE_ITEM, ALCHEMICAL_CONSTRUCT_ITEM);
    }
}
