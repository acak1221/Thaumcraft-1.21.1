package thaumcraft.common.config;

import dev.architectury.registry.menu.MenuRegistry;
import dev.architectury.registry.registries.DeferredRegister;
import dev.architectury.registry.registries.RegistrySupplier;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import thaumcraft.common.Thaumcraft;
import thaumcraft.common.blocks.BlockThaumatorium;
import thaumcraft.common.blocks.BlockMnemonicMatrix;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.BlockItem;
import dev.architectury.registry.CreativeTabRegistry;
import thaumcraft.common.container.ContainerThaumatorium;
import thaumcraft.common.tiles.TileThaumatorium;
import thaumcraft.common.tiles.TileThaumatoriumTop;

/** Formed metalDevice 10/11; no item bypasses the paid two-construct structure. */
public final class ThaumcraftThaumatorium {
    private static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(Thaumcraft.MODID, Registries.BLOCK);
    private static final DeferredRegister<BlockEntityType<?>> TILES = DeferredRegister.create(Thaumcraft.MODID, Registries.BLOCK_ENTITY_TYPE);
    private static final DeferredRegister<MenuType<?>> MENUS = DeferredRegister.create(Thaumcraft.MODID, Registries.MENU);
    private static final DeferredRegister<Item> ITEMS = DeferredRegister.create(Thaumcraft.MODID, Registries.ITEM);
    private static BlockBehaviour.Properties properties() { return BlockBehaviour.Properties.of().mapColor(MapColor.METAL).strength(3, 10.2F).sound(SoundType.METAL).noOcclusion(); }
    public static final RegistrySupplier<BlockThaumatorium> THAUMATORIUM = BLOCKS.register("thaumatorium", () -> new BlockThaumatorium(properties(), false));
    public static final RegistrySupplier<BlockThaumatorium> THAUMATORIUM_TOP = BLOCKS.register("thaumatorium_top", () -> new BlockThaumatorium(properties(), true));
    public static final RegistrySupplier<BlockEntityType<TileThaumatorium>> TILE = TILES.register("thaumatorium", () -> BlockEntityType.Builder.of(TileThaumatorium::new, THAUMATORIUM.get()).build(null));
    public static final RegistrySupplier<BlockEntityType<TileThaumatoriumTop>> TOP_TILE = TILES.register("thaumatorium_top", () -> BlockEntityType.Builder.of(TileThaumatoriumTop::new, THAUMATORIUM_TOP.get()).build(null));
    public static final RegistrySupplier<MenuType<ContainerThaumatorium>> MENU = MENUS.register("thaumatorium", () -> MenuRegistry.ofExtended(ContainerThaumatorium::new));
    public static final RegistrySupplier<BlockMnemonicMatrix> MNEMONIC_MATRIX = BLOCKS.register("mnemonic_matrix", () -> new BlockMnemonicMatrix(properties()));
    public static final RegistrySupplier<Item> MNEMONIC_MATRIX_ITEM = ITEMS.register("mnemonic_matrix", () -> new BlockItem(MNEMONIC_MATRIX.get(), new Item.Properties()));
    private ThaumcraftThaumatorium() {}
    public static void register() { BLOCKS.register(); TILES.register(); MENUS.register(); ITEMS.register(); CreativeTabRegistry.append(ThaumcraftContent.TAB, MNEMONIC_MATRIX_ITEM); }
}
