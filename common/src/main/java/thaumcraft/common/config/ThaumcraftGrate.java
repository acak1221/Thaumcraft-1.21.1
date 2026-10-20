package thaumcraft.common.config;

import dev.architectury.registry.CreativeTabRegistry;
import dev.architectury.registry.registries.DeferredRegister;
import dev.architectury.registry.registries.RegistrySupplier;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import thaumcraft.common.Thaumcraft;
import thaumcraft.common.blocks.BlockItemGrate;
import thaumcraft.common.tiles.TileGrate;

/**
 * The original Item Grate (BlockMetalDevice metadata 5/6, TileGrate); call after
 * ThaumcraftContent.register() and after the eldritch block family, whose metal
 * device holds the same grate tile.
 */
public final class ThaumcraftGrate {
    private static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(Thaumcraft.MODID, Registries.BLOCK);
    private static final DeferredRegister<Item> ITEMS = DeferredRegister.create(Thaumcraft.MODID, Registries.ITEM);
    private static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES = DeferredRegister.create(Thaumcraft.MODID, Registries.BLOCK_ENTITY_TYPE);

    public static final RegistrySupplier<BlockItemGrate> ITEM_GRATE = BLOCKS.register("item_grate",
            // BlockMetalDevice: hardness 3, resistance 17 stored (x3 in 1.7, exposed /5: 10.2), iron material (needs a pickaxe to drop); the plate is not a full cube.
            () -> new BlockItemGrate(BlockBehaviour.Properties.of().mapColor(MapColor.METAL).strength(3.0F, 10.2F).sound(SoundType.METAL).requiresCorrectToolForDrops().noOcclusion()));
    public static final RegistrySupplier<Item> ITEM_GRATE_ITEM = ITEMS.register("item_grate",
            () -> new BlockItem(ITEM_GRATE.get(), new Item.Properties()));
    /**
     * The grate tile is the tile of the item grate block and of the grate metadata
     * of the metal device, so the type claims both blocks.
     */
    public static final RegistrySupplier<BlockEntityType<TileGrate>> GRATE_BLOCK_ENTITY = BLOCK_ENTITIES.register("item_grate",
            () -> BlockEntityType.Builder.of(TileGrate::new, ITEM_GRATE.get(),
                    ThaumcraftEldritchBlocks.METAL_DEVICE.get()).build(null));

    private static boolean registered;
    private ThaumcraftGrate() {}

    public static void register() {
        if (registered) return;
        registered = true;
        BLOCKS.register();
        ITEMS.register();
        BLOCK_ENTITIES.register();
        CreativeTabRegistry.append(ThaumcraftContent.TAB, ITEM_GRATE_ITEM);
    }
}
