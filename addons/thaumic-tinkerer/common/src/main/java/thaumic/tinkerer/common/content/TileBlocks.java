/**
 * This class was created by <Vazkii>. It's distributed as part of the ThaumicTinkerer Mod.
 *
 * ThaumicTinkerer is Open Source and distributed under a Creative Commons Attribution-NonCommercial-ShareAlike 3.0
 * License (http://creativecommons.org/licenses/by-nc-sa/3.0/deed.en_GB)
 *
 * ThaumicTinkerer is a Derivative Work on Thaumcraft 4. Thaumcraft 4 (c) Azanor 2012
 * (http://www.minecraftforum.net/topic/1585216-)
 *
 * Ported to Minecraft 1.21.1 for Thaumcraft 4 Reload (see addons/thaumic-tinkerer/NOTICE.md).
 */
package thaumic.tinkerer.common.content;

import dev.architectury.registry.menu.MenuRegistry;
import dev.architectury.registry.registries.RegistrySupplier;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import thaumcraft.api.ItemApi;
import thaumic.tinkerer.client.TileBlocksClient;
import thaumic.tinkerer.common.block.BlockAnimationTablet;
import thaumic.tinkerer.common.block.BlockGolemConnector;
import thaumic.tinkerer.common.block.BlockRPlacer;
import thaumic.tinkerer.common.block.mobilizer.BlockMobilizer;
import thaumic.tinkerer.common.block.mobilizer.BlockMobilizerRelay;
import thaumic.tinkerer.common.block.tile.TileEntityMobilizer;
import thaumic.tinkerer.common.block.tile.TileEntityRelay;
import thaumic.tinkerer.common.block.tile.TileGolemConnector;
import thaumic.tinkerer.common.block.tile.TileRPlacer;
import thaumic.tinkerer.common.block.tile.container.ContainerAnimationTablet;
import thaumic.tinkerer.common.block.tile.container.ContainerRemotePlacer;
import thaumic.tinkerer.common.block.tile.tablet.TileAnimationTablet;
import thaumic.tinkerer.common.block.tile.transvector.TileTransvectorDislocator;
import thaumic.tinkerer.common.block.tile.transvector.TileTransvectorInterface;
import thaumic.tinkerer.common.block.transvector.BlockTransvectorDislocator;
import thaumic.tinkerer.common.block.transvector.BlockTransvectorInterface;
import thaumic.tinkerer.common.item.ItemConnector;
import thaumic.tinkerer.common.lib.LibBlockNames;
import thaumic.tinkerer.common.lib.LibItemNames;
import thaumic.tinkerer.common.network.packet.PacketPlacerButton;
import thaumic.tinkerer.common.network.packet.PacketTabletButton;
import thaumic.tinkerer.common.registry.TTRegistry;

/**
 * Feature group: devices: animation tablet, remote placer, levitation locomotive and relay, golem connector, transvector
 * interface and dislocator (and the Transvector Binder that links them).
 *
 * <p>Registers the seven blocks with their block entity types, the two menus, the binder and the two client-to-server
 * packets of the menus. Block entity types and menus are reached through the accessors below; the block entities, menus
 * and blocks use them, not the other way round, so the registrations are the only place that names the pairs.</p>
 */
public final class TileBlocks implements ContentGroup {
    public static final TileBlocks INSTANCE = new TileBlocks();

    /** {@code LibFeatures.INTERFACE_DISTANCE}: how far from a Transvector Interface its target may be. */
    public static final int INTERFACE_DISTANCE = 4;
    /** {@code LibFeatures.DISLOCATOR_DISTANCE}: how far from a Transvector Dislocator its target may be. */
    public static final int DISLOCATOR_DISTANCE = 16;

    private static RegistrySupplier<BlockAnimationTablet> animationTablet;
    private static RegistrySupplier<BlockEntityType<TileAnimationTablet>> animationTabletType;
    private static RegistrySupplier<MenuType<ContainerAnimationTablet>> animationTabletMenu;

    private static RegistrySupplier<BlockRPlacer> remotePlacer;
    private static RegistrySupplier<BlockEntityType<TileRPlacer>> placerType;
    private static RegistrySupplier<MenuType<ContainerRemotePlacer>> remotePlacerMenu;

    private static RegistrySupplier<BlockMobilizer> mobilizer;
    private static RegistrySupplier<BlockEntityType<TileEntityMobilizer>> mobilizerType;
    private static RegistrySupplier<BlockMobilizerRelay> relay;
    private static RegistrySupplier<BlockEntityType<TileEntityRelay>> relayType;

    private static RegistrySupplier<BlockGolemConnector> golemConnector;
    private static RegistrySupplier<BlockEntityType<TileGolemConnector>> golemConnectorType;

    private static RegistrySupplier<BlockTransvectorInterface> transvectorInterface;
    private static RegistrySupplier<BlockEntityType<TileTransvectorInterface>> interfaceType;
    private static RegistrySupplier<BlockTransvectorDislocator> dislocator;
    private static RegistrySupplier<BlockEntityType<TileTransvectorDislocator>> dislocatorType;

    private static RegistrySupplier<ItemConnector> connector;

    private TileBlocks() {}

    @Override
    public void register() {
        animationTablet = TTRegistry.block(LibBlockNames.ANIMATION_TABLET, BlockAnimationTablet::new);
        animationTabletType = TTRegistry.BLOCK_ENTITIES.register(LibBlockNames.ANIMATION_TABLET,
                () -> BlockEntityType.Builder.of(TileAnimationTablet::new, animationTablet.get()).build(null));
        animationTabletMenu = TTRegistry.MENUS.register(LibBlockNames.ANIMATION_TABLET,
                () -> MenuRegistry.ofExtended(ContainerAnimationTablet::fromNetwork));

        remotePlacer = TTRegistry.block(LibBlockNames.REMOTE_PLACER, BlockRPlacer::new);
        placerType = TTRegistry.BLOCK_ENTITIES.register(LibBlockNames.REMOTE_PLACER,
                () -> BlockEntityType.Builder.of(TileRPlacer::new, remotePlacer.get()).build(null));
        remotePlacerMenu = TTRegistry.MENUS.register(LibBlockNames.REMOTE_PLACER,
                () -> MenuRegistry.ofExtended(ContainerRemotePlacer::fromNetwork));

        mobilizer = TTRegistry.block(LibBlockNames.MOBILIZER, BlockMobilizer::new);
        mobilizerType = TTRegistry.BLOCK_ENTITIES.register(LibBlockNames.MOBILIZER,
                () -> BlockEntityType.Builder.of(TileEntityMobilizer::new, mobilizer.get()).build(null));
        relay = TTRegistry.block(LibBlockNames.MOBILIZER_RELAY, BlockMobilizerRelay::new);
        relayType = TTRegistry.BLOCK_ENTITIES.register(LibBlockNames.MOBILIZER_RELAY,
                () -> BlockEntityType.Builder.of(TileEntityRelay::new, relay.get()).build(null));

        golemConnector = TTRegistry.block(LibBlockNames.GOLEMCONNECTOR, BlockGolemConnector::new);
        golemConnectorType = TTRegistry.BLOCK_ENTITIES.register(LibBlockNames.GOLEMCONNECTOR,
                () -> BlockEntityType.Builder.of(TileGolemConnector::new, golemConnector.get()).build(null));

        transvectorInterface = TTRegistry.block(LibBlockNames.INTERFACE, BlockTransvectorInterface::new);
        interfaceType = TTRegistry.BLOCK_ENTITIES.register(LibBlockNames.INTERFACE,
                () -> BlockEntityType.Builder.of(TileTransvectorInterface::new, transvectorInterface.get()).build(null));
        dislocator = TTRegistry.block(LibBlockNames.DISLOCATOR, BlockTransvectorDislocator::new);
        dislocatorType = TTRegistry.BLOCK_ENTITIES.register(LibBlockNames.DISLOCATOR,
                () -> BlockEntityType.Builder.of(TileTransvectorDislocator::new, dislocator.get()).build(null));

        connector = TTRegistry.item(LibItemNames.CONNECTOR, ItemConnector::new);

        PacketTabletButton.register();
        PacketPlacerButton.register();
    }

    @Override
    public void registerClient() {
        TileBlocksClient.register();
    }

    // ---- registered objects ---------------------------------------------------------------------------------------

    public static BlockEntityType<TileAnimationTablet> animationTabletType() {
        return animationTabletType.get();
    }

    public static MenuType<ContainerAnimationTablet> animationTabletMenu() {
        return animationTabletMenu.get();
    }

    public static BlockAnimationTablet animationTabletBlock() {
        return animationTablet.get();
    }

    public static BlockEntityType<TileRPlacer> placerType() {
        return placerType.get();
    }

    public static MenuType<ContainerRemotePlacer> remotePlacerMenu() {
        return remotePlacerMenu.get();
    }

    public static BlockRPlacer remotePlacerBlock() {
        return remotePlacer.get();
    }

    public static BlockEntityType<TileEntityMobilizer> mobilizerType() {
        return mobilizerType.get();
    }

    public static BlockEntityType<TileEntityRelay> relayType() {
        return relayType.get();
    }

    public static BlockMobilizer mobilizerBlock() {
        return mobilizer.get();
    }

    public static BlockMobilizerRelay relayBlock() {
        return relay.get();
    }

    public static BlockEntityType<TileGolemConnector> golemConnectorType() {
        return golemConnectorType.get();
    }

    public static BlockGolemConnector golemConnectorBlock() {
        return golemConnector.get();
    }

    public static BlockEntityType<TileTransvectorInterface> interfaceType() {
        return interfaceType.get();
    }

    public static BlockTransvectorInterface interfaceBlock() {
        return transvectorInterface.get();
    }

    public static BlockEntityType<TileTransvectorDislocator> dislocatorType() {
        return dislocatorType.get();
    }

    public static BlockTransvectorDislocator dislocatorBlock() {
        return dislocator.get();
    }

    public static ItemConnector connectorItem() {
        return connector.get();
    }

    // ---- helpers of the recipes ---------------------------------------------------------------------------------

    /** The stack of a 1.7.10 {@code ConfigItems} field and metadata; fails loudly (the unit is recorded as a failure). */
    public static ItemStack legacyItem(String field, int meta) {
        ItemStack stack = ItemApi.getItem(field, meta);
        if (stack == null) throw new IllegalStateException("No Thaumcraft item for " + field + ":" + meta);
        return stack;
    }

    /** The stack of a 1.7.10 {@code ConfigBlocks} field and metadata. */
    public static ItemStack legacyBlock(String field, int meta) {
        ItemStack stack = ItemApi.getBlock(field, meta);
        if (stack == null) throw new IllegalStateException("No Thaumcraft block for " + field + ":" + meta);
        return stack;
    }

    /** 1.7.10 used a block field without a metadata for "any metadata": the ingredient of every item of the family. */
    public static Ingredient anyOfLegacyBlocks(String field) {
        List<Item> items = new ArrayList<>();
        for (ResourceLocation id : ItemApi.legacyItemIds(field, ItemApi.WILDCARD)) {
            if (BuiltInRegistries.ITEM.containsKey(id)) items.add(BuiltInRegistries.ITEM.get(id));
        }
        if (items.isEmpty()) throw new IllegalStateException("No Thaumcraft items for " + field + ":*");
        return Ingredient.of(items.toArray(new Item[0]));
    }

    /**
     * Thaumcraft's {@code Config.allowMirrors}: without mirrors the Transvector Dislocator has neither research nor recipe.
     * The API does not expose the setting (API request M1 in docs/api-requests/TileBlocks.md), so mirrors are allowed,
     * which is the setting's default.
     */
    public static boolean mirrorsAllowed() {
        return true;
    }

    /** The block the registry path names, for the models' sake: kept here so tests can list the group's blocks. */
    public static List<Block> blocks() {
        return List.of(animationTabletBlock(), remotePlacerBlock(), mobilizerBlock(), relayBlock(), golemConnectorBlock(),
                interfaceBlock(), dislocatorBlock());
    }
}
