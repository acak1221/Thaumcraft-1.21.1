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
import thaumcraft.common.blocks.BlockArcaneLamp;
import thaumcraft.common.blocks.BlockArcaneLight;
import thaumcraft.common.blocks.BlockAmber;
import thaumcraft.common.tiles.TileArcaneLamp;

/** Arcane Lamp and its real amber ingredient; invisible light deliberately has no inventory item. */
public final class ThaumcraftLamps {
    private static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(Thaumcraft.MODID, Registries.BLOCK);
    private static final DeferredRegister<Item> ITEMS = DeferredRegister.create(Thaumcraft.MODID, Registries.ITEM);
    private static final DeferredRegister<BlockEntityType<?>> TILES = DeferredRegister.create(Thaumcraft.MODID, Registries.BLOCK_ENTITY_TYPE);
    public static final RegistrySupplier<BlockArcaneLamp> ARCANE_LAMP = BLOCKS.register("arcane_lamp", () -> new BlockArcaneLamp(
            BlockBehaviour.Properties.of().mapColor(MapColor.METAL).strength(3, 10.2F).sound(SoundType.METAL).requiresCorrectToolForDrops().lightLevel(state -> 15).noOcclusion()));
    public static final RegistrySupplier<BlockArcaneLight> ARCANE_LIGHT = BLOCKS.register("arcane_light", () -> new BlockArcaneLight(
            BlockBehaviour.Properties.of().air().replaceable().noCollission().noOcclusion().noLootTable().lightLevel(state -> 15)));
    public static final RegistrySupplier<Block> AMBER_BLOCK = BLOCKS.register("amber_block", () -> new BlockAmber(
            BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_ORANGE).strength(1.5F, 3).sound(SoundType.STONE).requiresCorrectToolForDrops().noOcclusion()));
    public static final RegistrySupplier<Block> AMBER_BRICKS = BLOCKS.register("amber_bricks", () -> new BlockAmber(
            BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_ORANGE).strength(1.5F, 3).sound(SoundType.STONE).requiresCorrectToolForDrops().noOcclusion()));
    public static final RegistrySupplier<Item> ARCANE_LAMP_ITEM = ITEMS.register("arcane_lamp", () -> new BlockItem(ARCANE_LAMP.get(), new Item.Properties()));
    public static final RegistrySupplier<Item> AMBER_BLOCK_ITEM = ITEMS.register("amber_block", () -> new BlockItem(AMBER_BLOCK.get(), new Item.Properties()));
    public static final RegistrySupplier<Item> AMBER_BRICKS_ITEM = ITEMS.register("amber_bricks", () -> new BlockItem(AMBER_BRICKS.get(), new Item.Properties()));
    public static final RegistrySupplier<BlockEntityType<TileArcaneLamp>> ARCANE_LAMP_TILE = TILES.register("arcane_lamp", () ->
            BlockEntityType.Builder.of(TileArcaneLamp::new, ARCANE_LAMP.get()).build(null));
    private static boolean registered;
    private ThaumcraftLamps() {}
    public static void register() {
        if (registered) return;
        registered = true;
        BLOCKS.register(); ITEMS.register(); TILES.register();
        CreativeTabRegistry.append(ThaumcraftContent.TAB, ARCANE_LAMP_ITEM, AMBER_BLOCK_ITEM, AMBER_BRICKS_ITEM);
    }
}
