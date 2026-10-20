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
import thaumcraft.common.blocks.BlockEssentiaLamp;
import thaumcraft.common.tiles.TileArcaneLampFertility;
import thaumcraft.common.tiles.TileArcaneLampGrowth;

public final class ThaumcraftBiologicalLamps {
    private static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(Thaumcraft.MODID, Registries.BLOCK);
    private static final DeferredRegister<Item> ITEMS = DeferredRegister.create(Thaumcraft.MODID, Registries.ITEM);
    private static final DeferredRegister<BlockEntityType<?>> TILES = DeferredRegister.create(Thaumcraft.MODID, Registries.BLOCK_ENTITY_TYPE);
    private static BlockBehaviour.Properties properties() { return BlockBehaviour.Properties.of().mapColor(MapColor.METAL).strength(3, 10.2F).sound(SoundType.METAL).requiresCorrectToolForDrops().noOcclusion(); }
    public static final RegistrySupplier<BlockEssentiaLamp> GROWTH_LAMP = BLOCKS.register("growth_lamp", () -> new BlockEssentiaLamp(properties(), true));
    public static final RegistrySupplier<BlockEssentiaLamp> FERTILITY_LAMP = BLOCKS.register("fertility_lamp", () -> new BlockEssentiaLamp(properties(), false));
    public static final RegistrySupplier<Item> GROWTH_LAMP_ITEM = ITEMS.register("growth_lamp", () -> new BlockItem(GROWTH_LAMP.get(), new Item.Properties()));
    public static final RegistrySupplier<Item> FERTILITY_LAMP_ITEM = ITEMS.register("fertility_lamp", () -> new BlockItem(FERTILITY_LAMP.get(), new Item.Properties()));
    public static final RegistrySupplier<BlockEntityType<TileArcaneLampGrowth>> GROWTH_TILE = TILES.register("growth_lamp", () -> BlockEntityType.Builder.of(TileArcaneLampGrowth::new, GROWTH_LAMP.get()).build(null));
    public static final RegistrySupplier<BlockEntityType<TileArcaneLampFertility>> FERTILITY_TILE = TILES.register("fertility_lamp", () -> BlockEntityType.Builder.of(TileArcaneLampFertility::new, FERTILITY_LAMP.get()).build(null));
    private static boolean registered;
    private ThaumcraftBiologicalLamps() {}
    public static void register() {
        if (registered) return;
        registered = true; BLOCKS.register(); ITEMS.register(); TILES.register();
        CreativeTabRegistry.append(ThaumcraftContent.TAB, GROWTH_LAMP_ITEM, FERTILITY_LAMP_ITEM);
    }
}
