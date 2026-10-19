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
import thaumcraft.common.blocks.BlockFocalManipulator;
import thaumcraft.common.container.ContainerFocalManipulator;
import thaumcraft.common.tiles.TileFocalManipulator;

/** Original stone-device13, flattened without a second shadow focus registry. */
public final class ThaumcraftFocalManipulation {
    private static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(Thaumcraft.MODID, Registries.BLOCK);
    private static final DeferredRegister<Item> ITEMS = DeferredRegister.create(Thaumcraft.MODID, Registries.ITEM);
    private static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES = DeferredRegister.create(Thaumcraft.MODID, Registries.BLOCK_ENTITY_TYPE);
    private static final DeferredRegister<MenuType<?>> MENUS = DeferredRegister.create(Thaumcraft.MODID, Registries.MENU);

    public static final RegistrySupplier<BlockFocalManipulator> FOCAL_MANIPULATOR = BLOCKS.register("focal_manipulator",
            () -> new BlockFocalManipulator(BlockBehaviour.Properties.of().mapColor(MapColor.STONE)
                    .strength(3, 25).sound(SoundType.STONE).requiresCorrectToolForDrops().noOcclusion()));
    public static final RegistrySupplier<Item> FOCAL_MANIPULATOR_ITEM = ITEMS.register("focal_manipulator",
            () -> new BlockItem(FOCAL_MANIPULATOR.get(), new Item.Properties()));
    public static final RegistrySupplier<BlockEntityType<TileFocalManipulator>> FOCAL_MANIPULATOR_BLOCK_ENTITY = BLOCK_ENTITIES.register("focal_manipulator",
            () -> BlockEntityType.Builder.of(TileFocalManipulator::new, FOCAL_MANIPULATOR.get()).build(null));
    public static final RegistrySupplier<MenuType<ContainerFocalManipulator>> FOCAL_MANIPULATOR_MENU = MENUS.register("focal_manipulator",
            () -> MenuRegistry.ofExtended(ContainerFocalManipulator::new));

    private ThaumcraftFocalManipulation() {}

    public static void register() {
        BLOCKS.register();
        ITEMS.register();
        BLOCK_ENTITIES.register();
        MENUS.register();
        CreativeTabRegistry.append(ThaumcraftContent.TAB, FOCAL_MANIPULATOR_ITEM);
    }
}
