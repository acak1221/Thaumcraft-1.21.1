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
import thaumcraft.common.blocks.BlockAdvancedAlchemyFurnace;
import thaumcraft.common.tiles.TileAlchemyFurnaceAdvanced;
import thaumcraft.common.tiles.TileAlchemyFurnaceAdvancedNozzle;
import thaumcraft.common.tiles.TileAlchemyFurnaceAdvancedPart;

/** Original metalDevice3 ingredient and formed alchemy-furnace metadata0..4. */
public final class ThaumcraftAdvancedFurnace {
    private static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(Thaumcraft.MODID,Registries.BLOCK);
    private static final DeferredRegister<Item> ITEMS = DeferredRegister.create(Thaumcraft.MODID,Registries.ITEM);
    private static final DeferredRegister<BlockEntityType<?>> TILES = DeferredRegister.create(Thaumcraft.MODID,Registries.BLOCK_ENTITY_TYPE);
    private static BlockBehaviour.Properties properties() { return BlockBehaviour.Properties.of().mapColor(MapColor.METAL).strength(3,10.2F).sound(SoundType.METAL); }
    public static final RegistrySupplier<Block> ADVANCED_CONSTRUCT = BLOCKS.register("advanced_alchemical_construct", () -> new Block(properties()));
    public static final RegistrySupplier<Item> ADVANCED_CONSTRUCT_ITEM = ITEMS.register("advanced_alchemical_construct", () -> new BlockItem(ADVANCED_CONSTRUCT.get(),new Item.Properties()));
    public static final RegistrySupplier<BlockAdvancedAlchemyFurnace> FURNACE = BLOCKS.register("advanced_alchemical_furnace", () -> new BlockAdvancedAlchemyFurnace(properties().noOcclusion().lightLevel(state -> state.getValue(BlockAdvancedAlchemyFurnace.LIGHT))));
    public static final RegistrySupplier<Item> FURNACE_ITEM = ITEMS.register("advanced_alchemical_furnace", () -> new thaumcraft.common.items.ItemAdvancedAlchemyFurnace(FURNACE.get(),new Item.Properties()));
    public static final RegistrySupplier<BlockEntityType<TileAlchemyFurnaceAdvanced>> TILE = TILES.register("advanced_alchemical_furnace", () -> BlockEntityType.Builder.of(TileAlchemyFurnaceAdvanced::new,FURNACE.get()).build(null));
    public static final RegistrySupplier<BlockEntityType<TileAlchemyFurnaceAdvancedNozzle>> NOZZLE = TILES.register("advanced_alchemical_furnace_nozzle", () -> BlockEntityType.Builder.of(TileAlchemyFurnaceAdvancedNozzle::new,FURNACE.get()).build(null));
    public static final RegistrySupplier<BlockEntityType<TileAlchemyFurnaceAdvancedPart>> PART_TILE = TILES.register("advanced_alchemical_furnace_part", () -> BlockEntityType.Builder.of(TileAlchemyFurnaceAdvancedPart::new,FURNACE.get()).build(null));
    private ThaumcraftAdvancedFurnace() {}
    public static void register() { BLOCKS.register(); ITEMS.register(); TILES.register(); CreativeTabRegistry.append(ThaumcraftContent.TAB,ADVANCED_CONSTRUCT_ITEM,FURNACE_ITEM); }
}
