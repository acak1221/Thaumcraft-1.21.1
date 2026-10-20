package thaumcraft.common.config;

import dev.architectury.registry.CreativeTabRegistry;
import dev.architectury.registry.registries.DeferredRegister;
import dev.architectury.registry.registries.RegistrySupplier;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.MapColor;
import thaumcraft.common.Thaumcraft;
import thaumcraft.common.blocks.BlockTube;
import thaumcraft.common.blocks.BlockTubeBuffer;
import thaumcraft.common.blocks.BlockCentrifuge;
import thaumcraft.common.blocks.BlockEssentiaCrystalizer;
import thaumcraft.common.blocks.BlockEssentiaReservoir;
import thaumcraft.common.tiles.TileTube;
import thaumcraft.common.tiles.TileTubeBuffer;
import thaumcraft.common.tiles.TileCentrifuge;
import thaumcraft.common.tiles.TileEssentiaCrystalizer;
import thaumcraft.common.tiles.TileEssentiaReservoir;

/** Native blockTube:0..7 registrations; crystallizer VisNet acceleration is deferred. */
public final class ThaumcraftTubes {
    private static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(Thaumcraft.MODID, Registries.BLOCK);
    private static final DeferredRegister<Item> ITEMS = DeferredRegister.create(Thaumcraft.MODID, Registries.ITEM);
    private static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES = DeferredRegister.create(Thaumcraft.MODID, Registries.BLOCK_ENTITY_TYPE);

    public static final RegistrySupplier<BlockTube> ESSENTIA_TUBE = BLOCKS.register("essentia_tube",
            // Original setter resistance5 stores15 and exposes15/5: native3.
            () -> new BlockTube(BlockBehaviour.Properties.of().mapColor(MapColor.METAL)
                    .strength(.5F, 3).sound(SoundType.METAL).requiresCorrectToolForDrops().noOcclusion().dynamicShape()));
    public static final RegistrySupplier<BlockTube> ESSENTIA_VALVE = BLOCKS.register("essentia_valve",
            () -> new BlockTube(BlockBehaviour.Properties.of().mapColor(MapColor.METAL)
                    .strength(.5F, 3).sound(SoundType.METAL).requiresCorrectToolForDrops().noOcclusion().dynamicShape(), true));
    public static final RegistrySupplier<BlockTube> ESSENTIA_FILTER = variant("essentia_filter", BlockTube.Kind.FILTER);
    public static final RegistrySupplier<BlockTube> ESSENTIA_RESTRICT = variant("essentia_restrict", BlockTube.Kind.RESTRICT);
    public static final RegistrySupplier<BlockTube> ESSENTIA_ONEWAY = variant("essentia_oneway", BlockTube.Kind.ONEWAY);
    public static final RegistrySupplier<BlockTubeBuffer> ESSENTIA_BUFFER = BLOCKS.register("essentia_buffer",
            () -> new BlockTubeBuffer(BlockBehaviour.Properties.of().mapColor(MapColor.METAL)
                    .strength(.5F, 3).sound(SoundType.METAL).requiresCorrectToolForDrops().noOcclusion().dynamicShape()));
    public static final RegistrySupplier<BlockCentrifuge> CENTRIFUGE = BLOCKS.register("centrifuge",
            () -> new BlockCentrifuge(BlockBehaviour.Properties.of().mapColor(MapColor.METAL)
                    .strength(.5F, 3).sound(SoundType.METAL).requiresCorrectToolForDrops().noOcclusion()));
    public static final RegistrySupplier<BlockEssentiaCrystalizer> ESSENTIA_CRYSTALLIZER = BLOCKS.register("essentia_crystallizer",
            () -> new BlockEssentiaCrystalizer(BlockBehaviour.Properties.of().mapColor(MapColor.METAL)
                    .strength(.5F, 3).sound(SoundType.METAL).requiresCorrectToolForDrops().noOcclusion()));
    public static final RegistrySupplier<BlockEssentiaReservoir> ESSENTIA_RESERVOIR = BLOCKS.register("essentia_reservoir",
            // Original setHardness2 / setResistance17 (stored x3, exposed /5: 10.2); metal, not an opaque or normal cube.
            () -> new BlockEssentiaReservoir(BlockBehaviour.Properties.of().mapColor(MapColor.METAL)
                    .strength(2.0F, 10.2F).sound(SoundType.METAL).requiresCorrectToolForDrops().noOcclusion()
                    .isRedstoneConductor((state, level, pos) -> false).isSuffocating((state, level, pos) -> false).isViewBlocking((state, level, pos) -> false)));
    public static final RegistrySupplier<Item> ESSENTIA_TUBE_ITEM = ITEMS.register("essentia_tube",
            () -> tubeItem(ESSENTIA_TUBE.get()));
    public static final RegistrySupplier<Item> ESSENTIA_VALVE_ITEM = ITEMS.register("essentia_valve",
            () -> tubeItem(ESSENTIA_VALVE.get()));
    public static final RegistrySupplier<Item> ESSENTIA_FILTER_ITEM = ITEMS.register("essentia_filter",
            () -> tubeItem(ESSENTIA_FILTER.get()));
    public static final RegistrySupplier<Item> ESSENTIA_RESTRICT_ITEM = ITEMS.register("essentia_restrict",
            () -> tubeItem(ESSENTIA_RESTRICT.get()));
    public static final RegistrySupplier<Item> ESSENTIA_ONEWAY_ITEM = ITEMS.register("essentia_oneway",
            () -> tubeItem(ESSENTIA_ONEWAY.get()));
    public static final RegistrySupplier<Item> ESSENTIA_BUFFER_ITEM = ITEMS.register("essentia_buffer",
            () -> tubeItem(ESSENTIA_BUFFER.get()));
    public static final RegistrySupplier<Item> ESSENTIA_RESERVOIR_ITEM = ITEMS.register("essentia_reservoir",
            () -> new BlockItem(ESSENTIA_RESERVOIR.get(), new Item.Properties()) {
                @Override protected boolean placeBlock(BlockPlaceContext context, BlockState state) {
                    if (!super.placeBlock(context, state)) return false;
                    // BlockEssentiaReservoirItem.placeBlockAt: the opening faces the block that was clicked.
                    if (!context.getLevel().isClientSide && context.getLevel().getBlockEntity(context.getClickedPos()) instanceof TileEssentiaReservoir reservoir) {
                        reservoir.setFacing(thaumcraft.common.lib.essentia.ReservoirRules.placedFacing(context.getClickedFace()));
                    }
                    return true;
                }
            });
    public static final RegistrySupplier<Item> CENTRIFUGE_ITEM = ITEMS.register("centrifuge",
            // The original item assigns a clicked facing only to TileTube and
            // crystallizer, NOT centrifuge. Its persisted default stays NORTH.
            () -> new BlockItem(CENTRIFUGE.get(), new Item.Properties()));
    public static final RegistrySupplier<Item> ESSENTIA_CRYSTALLIZER_ITEM = ITEMS.register("essentia_crystallizer",
            () -> new BlockItem(ESSENTIA_CRYSTALLIZER.get(), new Item.Properties()) {
                @Override public void appendHoverText(net.minecraft.world.item.ItemStack stack, TooltipContext context,
                        java.util.List<net.minecraft.network.chat.Component> tooltip, net.minecraft.world.item.TooltipFlag flags) {
                    super.appendHoverText(stack, context, tooltip, flags);
                    // Actual Terra CV integration; no research-prose rewrite or
                    // claim that the other TC4 network consumers are implemented.
                    tooltip.add(net.minecraft.network.chat.Component.translatable("tooltip.thaumcraft.crystallizer.visnet")
                            .withStyle(net.minecraft.ChatFormatting.GRAY));
                }
            });
    public static final RegistrySupplier<BlockEntityType<TileTube>> TUBE_BLOCK_ENTITY = BLOCK_ENTITIES.register("essentia_tube",
            // The factory must select the filter-only marker API on DISK load,
            // not just when BlockTube.newBlockEntity handles fresh placement.
            () -> BlockEntityType.Builder.of(TileTube::create, ESSENTIA_TUBE.get(), ESSENTIA_VALVE.get(),
                    ESSENTIA_FILTER.get(), ESSENTIA_RESTRICT.get(), ESSENTIA_ONEWAY.get()).build(null));
    public static final RegistrySupplier<BlockEntityType<TileTubeBuffer>> BUFFER_BLOCK_ENTITY = BLOCK_ENTITIES.register("essentia_buffer",
            () -> BlockEntityType.Builder.of(TileTubeBuffer::new, ESSENTIA_BUFFER.get()).build(null));
    public static final RegistrySupplier<BlockEntityType<TileCentrifuge>> CENTRIFUGE_BLOCK_ENTITY = BLOCK_ENTITIES.register("centrifuge",
            () -> BlockEntityType.Builder.of(TileCentrifuge::new, CENTRIFUGE.get()).build(null));
    public static final RegistrySupplier<BlockEntityType<TileEssentiaCrystalizer>> CRYSTALLIZER_BLOCK_ENTITY = BLOCK_ENTITIES.register("essentia_crystallizer",
            () -> BlockEntityType.Builder.of(TileEssentiaCrystalizer::new, ESSENTIA_CRYSTALLIZER.get()).build(null));

    public static final RegistrySupplier<BlockEntityType<TileEssentiaReservoir>> RESERVOIR_BLOCK_ENTITY = BLOCK_ENTITIES.register("essentia_reservoir",
            () -> BlockEntityType.Builder.of(TileEssentiaReservoir::new, ESSENTIA_RESERVOIR.get()).build(null));

    private static RegistrySupplier<BlockTube> variant(String id, BlockTube.Kind kind) {
        return BLOCKS.register(id, () -> new BlockTube(BlockBehaviour.Properties.of().mapColor(MapColor.METAL)
                .strength(.5F, 3).sound(SoundType.METAL).requiresCorrectToolForDrops().noOcclusion().dynamicShape(), kind));
    }

    private static BlockItem tubeItem(Block block) {
        return new BlockItem(block, new Item.Properties()) {
            @Override protected boolean placeBlock(BlockPlaceContext context, BlockState state) {
                if (!super.placeBlock(context, state)) return false;
                // BlockTubeItem:41-46 uses the clicked face, NOT its opposite.
                if (!context.getLevel().isClientSide && context.getLevel().getBlockEntity(context.getClickedPos()) instanceof TileTube tube) {
                    tube.setFacing(context.getClickedFace());
                }
                return true;
            }
        };
    }

    private static boolean registered;
    private ThaumcraftTubes() {}

    public static void register() {
        if (registered) return;
        registered = true;
        BLOCKS.register();
        ITEMS.register();
        BLOCK_ENTITIES.register();
        CreativeTabRegistry.append(ThaumcraftContent.TAB, ESSENTIA_TUBE_ITEM, ESSENTIA_VALVE_ITEM,
                ESSENTIA_FILTER_ITEM, ESSENTIA_BUFFER_ITEM, ESSENTIA_RESTRICT_ITEM, ESSENTIA_ONEWAY_ITEM, CENTRIFUGE_ITEM,
                ESSENTIA_CRYSTALLIZER_ITEM, ESSENTIA_RESERVOIR_ITEM);
    }
}
