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
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import thaumcraft.common.Thaumcraft;
import thaumcraft.common.blocks.BlockArcaneBore;
import thaumcraft.common.blocks.BlockArcaneBoreBase;
import thaumcraft.common.container.ContainerArcaneBore;
import thaumcraft.common.items.ItemArcaneBore;
import thaumcraft.common.tiles.TileArcaneBore;
import thaumcraft.common.tiles.TileArcaneBoreBase;

/** Original wooden-device4/5. Registration is not a claim of verified gameplay. */
public final class ThaumcraftArcaneBore {
    private static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(Thaumcraft.MODID, Registries.BLOCK);
    private static final DeferredRegister<Item> ITEMS = DeferredRegister.create(Thaumcraft.MODID, Registries.ITEM);
    private static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES = DeferredRegister.create(Thaumcraft.MODID, Registries.BLOCK_ENTITY_TYPE);
    private static final DeferredRegister<MenuType<?>> MENUS = DeferredRegister.create(Thaumcraft.MODID, Registries.MENU);

    public static final RegistrySupplier<BlockArcaneBoreBase> ARCANE_BORE_BASE = BLOCKS.register("arcane_bore_base",
            () -> new BlockArcaneBoreBase(BlockBehaviour.Properties.of().mapColor(MapColor.WOOD)));
    public static final RegistrySupplier<BlockArcaneBore> ARCANE_BORE = BLOCKS.register("arcane_bore",
            () -> new BlockArcaneBore(BlockBehaviour.Properties.of().mapColor(MapColor.WOOD)));
    public static final RegistrySupplier<Item> ARCANE_BORE_BASE_ITEM = ITEMS.register("arcane_bore_base",
            () -> new BlockItem(ARCANE_BORE_BASE.get(), new Item.Properties()));
    public static final RegistrySupplier<Item> ARCANE_BORE_ITEM = ITEMS.register("arcane_bore",
            () -> new ItemArcaneBore(ARCANE_BORE.get(), new Item.Properties()));
    public static final RegistrySupplier<BlockEntityType<TileArcaneBoreBase>> ARCANE_BORE_BASE_BLOCK_ENTITY = BLOCK_ENTITIES.register("arcane_bore_base",
            () -> BlockEntityType.Builder.of(TileArcaneBoreBase::new, ARCANE_BORE_BASE.get()).build(null));
    public static final RegistrySupplier<BlockEntityType<TileArcaneBore>> ARCANE_BORE_BLOCK_ENTITY = BLOCK_ENTITIES.register("arcane_bore",
            () -> BlockEntityType.Builder.of(TileArcaneBore::new, ARCANE_BORE.get()).build(null));
    public static final RegistrySupplier<MenuType<ContainerArcaneBore>> ARCANE_BORE_MENU = MENUS.register("arcane_bore",
            () -> MenuRegistry.ofExtended(ContainerArcaneBore::new));

    private ThaumcraftArcaneBore() {}
    public static void register() {
        BLOCKS.register(); ITEMS.register(); BLOCK_ENTITIES.register(); MENUS.register();
        CreativeTabRegistry.append(ThaumcraftContent.TAB, ARCANE_BORE_BASE_ITEM, ARCANE_BORE_ITEM);
    }
}
