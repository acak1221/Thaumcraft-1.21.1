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
import thaumcraft.common.blocks.BlockDeconstructionTable;
import thaumcraft.common.container.ContainerDeconstructionTable;
import thaumcraft.common.tiles.TileDeconstructionTable;

/** Original blockTable:14, flattened into one native block and its real recipe. */
public final class ThaumcraftDeconstruction {
    private static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(Thaumcraft.MODID, Registries.BLOCK);
    private static final DeferredRegister<Item> ITEMS = DeferredRegister.create(Thaumcraft.MODID, Registries.ITEM);
    private static final DeferredRegister<BlockEntityType<?>> TILES = DeferredRegister.create(Thaumcraft.MODID, Registries.BLOCK_ENTITY_TYPE);
    private static final DeferredRegister<MenuType<?>> MENUS = DeferredRegister.create(Thaumcraft.MODID, Registries.MENU);
    public static final RegistrySupplier<BlockDeconstructionTable> TABLE = BLOCKS.register("deconstruction_table", () ->
            new BlockDeconstructionTable(BlockBehaviour.Properties.of().mapColor(MapColor.WOOD).strength(2.5F).sound(SoundType.WOOD).noOcclusion()));
    public static final RegistrySupplier<Item> ITEM = ITEMS.register("deconstruction_table", () -> new BlockItem(TABLE.get(), new Item.Properties()));
    public static final RegistrySupplier<BlockEntityType<TileDeconstructionTable>> TILE = TILES.register("deconstruction_table", () ->
            BlockEntityType.Builder.of(TileDeconstructionTable::new, TABLE.get()).build(null));
    public static final RegistrySupplier<MenuType<ContainerDeconstructionTable>> MENU = MENUS.register("deconstruction_table", () -> MenuRegistry.ofExtended(ContainerDeconstructionTable::new));
    private ThaumcraftDeconstruction() {}
    public static void register() { BLOCKS.register(); ITEMS.register(); TILES.register(); MENUS.register(); CreativeTabRegistry.append(ThaumcraftContent.TAB, ITEM); }
}
