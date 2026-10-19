package thaumcraft.common.config;

import dev.architectury.registry.CreativeTabRegistry;
import dev.architectury.registry.registries.DeferredRegister;
import dev.architectury.registry.registries.RegistrySupplier;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import thaumcraft.common.Thaumcraft;
import thaumcraft.common.blocks.BlockNodeStabilizer;
import thaumcraft.common.blocks.BlockNodeConverter;
import thaumcraft.common.blocks.BlockNodeEnergized;
import thaumcraft.common.blocks.BlockVisRelay;
import thaumcraft.common.tiles.TileNodeStabilizer;
import thaumcraft.common.tiles.TileNodeConverter;
import thaumcraft.common.tiles.TileNodeEnergized;
import thaumcraft.common.tiles.TileVisRelay;

/** Real node devices. Worldgen stays separate; energized nodes have NO portable
 * item, and advanced stabilizers/special-node ecology remain a separate gate. */
public final class ThaumcraftNodeDevices {
    private static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(Thaumcraft.MODID, Registries.BLOCK);
    private static final DeferredRegister<Item> ITEMS = DeferredRegister.create(Thaumcraft.MODID, Registries.ITEM);
    private static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES = DeferredRegister.create(Thaumcraft.MODID, Registries.BLOCK_ENTITY_TYPE);

    public static final RegistrySupplier<BlockNodeStabilizer> NODE_STABILIZER = BLOCKS.register("node_stabilizer",
            () -> new BlockNodeStabilizer(BlockBehaviour.Properties.of().mapColor(MapColor.STONE)
                    .strength(3, 25).sound(SoundType.STONE).requiresCorrectToolForDrops().noOcclusion()));
    public static final RegistrySupplier<Item> NODE_STABILIZER_ITEM = ITEMS.register("node_stabilizer",
            () -> new BlockItem(NODE_STABILIZER.get(), new Item.Properties()) {
                @Override public void appendHoverText(ItemStack stack, TooltipContext context,
                        java.util.List<Component> tooltip, TooltipFlag flags) {
                    super.appendHoverText(stack, context, tooltip, flags);
                    // Honest bounded-port status; retain original book prose.
                    tooltip.add(Component.translatable("tooltip.thaumcraft.node_stabilizer.scope")
                            .withStyle(net.minecraft.ChatFormatting.GRAY));
                }
            });
    public static final RegistrySupplier<BlockEntityType<TileNodeStabilizer>> NODE_STABILIZER_BLOCK_ENTITY = BLOCK_ENTITIES.register("node_stabilizer",
            () -> BlockEntityType.Builder.of(TileNodeStabilizer::new, NODE_STABILIZER.get()).build(null));

    public static final RegistrySupplier<BlockNodeConverter> NODE_TRANSDUCER = BLOCKS.register("node_transducer",
            () -> new BlockNodeConverter(BlockBehaviour.Properties.of().mapColor(MapColor.STONE)
                    .strength(3, 25).sound(SoundType.STONE).requiresCorrectToolForDrops().noOcclusion()));
    public static final RegistrySupplier<Item> NODE_TRANSDUCER_ITEM = ITEMS.register("node_transducer",
            () -> new BlockItem(NODE_TRANSDUCER.get(), new Item.Properties()));
    public static final RegistrySupplier<BlockEntityType<TileNodeConverter>> NODE_TRANSDUCER_BLOCK_ENTITY = BLOCK_ENTITIES.register("node_transducer",
            () -> BlockEntityType.Builder.of(TileNodeConverter::new, NODE_TRANSDUCER.get()).build(null));

    public static final RegistrySupplier<BlockNodeEnergized> ENERGIZED_NODE = BLOCKS.register("energized_node",
            () -> new BlockNodeEnergized(BlockBehaviour.Properties.of().strength(2, 200)
                    .noOcclusion().noCollission().lightLevel(state -> 8)));
    public static final RegistrySupplier<BlockEntityType<TileNodeEnergized>> ENERGIZED_NODE_BLOCK_ENTITY = BLOCK_ENTITIES.register("energized_node",
            () -> BlockEntityType.Builder.of(TileNodeEnergized::new, ENERGIZED_NODE.get()).build(null));

    public static final RegistrySupplier<BlockVisRelay> VIS_RELAY = BLOCKS.register("vis_relay",
            () -> new BlockVisRelay(BlockBehaviour.Properties.of().mapColor(MapColor.METAL)
                    .strength(3, 17).sound(SoundType.METAL).requiresCorrectToolForDrops().noOcclusion()));
    public static final RegistrySupplier<Item> VIS_RELAY_ITEM = ITEMS.register("vis_relay",
            () -> new BlockItem(VIS_RELAY.get(), new Item.Properties()));
    public static final RegistrySupplier<BlockEntityType<TileVisRelay>> VIS_RELAY_BLOCK_ENTITY = BLOCK_ENTITIES.register("vis_relay",
            () -> BlockEntityType.Builder.of(TileVisRelay::new, VIS_RELAY.get()).build(null));

    private ThaumcraftNodeDevices() {}

    public static void register() {
        BLOCKS.register();
        ITEMS.register();
        BLOCK_ENTITIES.register();
        CreativeTabRegistry.append(ThaumcraftContent.TAB, NODE_STABILIZER_ITEM, NODE_TRANSDUCER_ITEM, VIS_RELAY_ITEM);
    }
}
