/**
 * This file is new in the Minecraft 1.21.1 port of Thaumic Tinkerer for Thaumcraft 4 Reload (no 1.7.10 counterpart, see
 * addons/thaumic-tinkerer/NOTICE.md). Thaumic Tinkerer was created by Vazkii; the port is distributed under a Creative
 * Commons Attribution-NonCommercial-ShareAlike 3.0 License (http://creativecommons.org/licenses/by-nc-sa/3.0/deed.en_GB)
 *
 * ThaumicTinkerer is a Derivative Work on Thaumcraft 4. Thaumcraft 4 (c) Azanor 2012
 * (http://www.minecraftforum.net/topic/1585216-)
 */
package thaumic.tinkerer.common.content;

import dev.architectury.registry.registries.RegistrySupplier;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import thaumcraft.api.aspects.Aspect;
import thaumic.tinkerer.common.block.BlockAspectAnalyzer;
import thaumic.tinkerer.common.block.BlockFunnel;
import thaumic.tinkerer.common.block.BlockMagnet;
import thaumic.tinkerer.common.block.BlockRepairer;
import thaumic.tinkerer.common.block.BlockSummon;
import thaumic.tinkerer.common.block.tile.TileAspectAnalyzer;
import thaumic.tinkerer.common.block.tile.TileFunnel;
import thaumic.tinkerer.common.block.tile.TileMagnet;
import thaumic.tinkerer.common.block.tile.TileMobMagnet;
import thaumic.tinkerer.common.block.tile.TileRepairer;
import thaumic.tinkerer.common.block.tile.TileSummon;
import thaumic.tinkerer.common.block.tile.container.ContainerAspectAnalyzer;
import thaumic.tinkerer.common.block.tile.container.ContainerMobMagnet;
import thaumic.tinkerer.common.item.ItemBloodSword;
import thaumic.tinkerer.common.item.ItemMobAspect;
import thaumic.tinkerer.common.item.ItemMobDisplay;
import thaumic.tinkerer.common.item.ItemSoulMould;
import thaumic.tinkerer.common.lib.LibBlockNames;
import thaumic.tinkerer.common.lib.LibItemNames;
import thaumic.tinkerer.common.registry.TTRegistry;

/**
 * Feature group: devices: aspect analyzer, repairer, funnel, magnets, necromancy tablet (summon) with its mob items
 * (the soul mould, the soul aspects, the mob display) and the cursed spirit's blade that reaps the soul aspects.
 */
public final class Machines implements ContentGroup {
    public static final Machines INSTANCE = new Machines();

    // The registered objects (set by register(), read afterwards).
    public static RegistrySupplier<BlockAspectAnalyzer> ASPECT_ANALYZER;
    public static RegistrySupplier<BlockRepairer> REPAIRER;
    public static RegistrySupplier<BlockFunnel> FUNNEL;
    public static RegistrySupplier<BlockMagnet> MAGNET;
    public static RegistrySupplier<BlockMagnet> MOB_MAGNET;
    public static RegistrySupplier<BlockSummon> SUMMON;

    public static RegistrySupplier<BlockEntityType<TileAspectAnalyzer>> TILE_ASPECT_ANALYZER;
    public static RegistrySupplier<BlockEntityType<TileRepairer>> TILE_REPAIRER;
    public static RegistrySupplier<BlockEntityType<TileFunnel>> TILE_FUNNEL;
    public static RegistrySupplier<BlockEntityType<TileMagnet>> TILE_MAGNET;
    public static RegistrySupplier<BlockEntityType<TileMobMagnet>> TILE_MOB_MAGNET;
    public static RegistrySupplier<BlockEntityType<TileSummon>> TILE_SUMMON;

    public static RegistrySupplier<MenuType<ContainerAspectAnalyzer>> MENU_ASPECT_ANALYZER;
    public static RegistrySupplier<MenuType<ContainerMobMagnet>> MENU_MOB_MAGNET;

    private Machines() {}

    @Override
    public void register() {
        // 1.7.10: Material.wood, hardness 1.7, resistance 1, wood step sound.
        ASPECT_ANALYZER = TTRegistry.block(LibBlockNames.ASPECT_ANALYZER, () -> new BlockAspectAnalyzer(
                BlockBehaviour.Properties.of().mapColor(MapColor.WOOD).strength(1.7F, 1.0F).sound(SoundType.WOOD)));
        TILE_ASPECT_ANALYZER = TTRegistry.BLOCK_ENTITIES.register(LibBlockNames.ASPECT_ANALYZER,
                () -> BlockEntityType.Builder.<TileAspectAnalyzer>of(TileAspectAnalyzer::new, ASPECT_ANALYZER.get()).build(null));
        MENU_ASPECT_ANALYZER = TTRegistry.MENUS.register(LibBlockNames.ASPECT_ANALYZER,
                () -> new MenuType<>(ContainerAspectAnalyzer::new, FeatureFlags.DEFAULT_FLAGS));

        // Material.iron (needs a pickaxe), hardness 5, resistance 10; the glass box lets light through. Not a redstone
        // conductor: 1.7.10 isNormalCube() was false (renderAsNormalBlock() false, canProvidePower() true), while the
        // 1.21.1 default asks for a full collision shape, which the repairer has.
        REPAIRER = TTRegistry.block(LibBlockNames.REPAIRER, () -> new BlockRepairer(BlockBehaviour.Properties.of()
                .mapColor(MapColor.METAL).strength(5.0F, 10.0F).requiresCorrectToolForDrops().noOcclusion()
                .isRedstoneConductor((state, level, pos) -> false)));
        TILE_REPAIRER = TTRegistry.BLOCK_ENTITIES.register(LibBlockNames.REPAIRER,
                () -> BlockEntityType.Builder.<TileRepairer>of(TileRepairer::new, REPAIRER.get()).build(null));

        // Material.rock, hardness 3, resistance 8, stone step sound.
        FUNNEL = TTRegistry.block(LibBlockNames.FUNNEL, () -> new BlockFunnel(BlockBehaviour.Properties.of()
                .mapColor(MapColor.STONE).strength(3.0F, 8.0F).sound(SoundType.STONE).requiresCorrectToolForDrops()
                .noOcclusion()));
        TILE_FUNNEL = TTRegistry.BLOCK_ENTITIES.register(LibBlockNames.FUNNEL,
                () -> BlockEntityType.Builder.<TileFunnel>of(TileFunnel::new, FUNNEL.get()).build(null));

        // Material.iron, hardness 1.7, resistance 1, wood step sound (as the original).
        MAGNET = TTRegistry.block(LibBlockNames.MAGNET, () -> new BlockMagnet(magnetProperties(), false));
        MOB_MAGNET = TTRegistry.block(LibBlockNames.MOB_MAGNET, () -> new BlockMagnet(magnetProperties(), true));
        TILE_MAGNET = TTRegistry.BLOCK_ENTITIES.register(LibBlockNames.MAGNET,
                () -> BlockEntityType.Builder.<TileMagnet>of(TileMagnet::new, MAGNET.get()).build(null));
        TILE_MOB_MAGNET = TTRegistry.BLOCK_ENTITIES.register(LibBlockNames.MOB_MAGNET,
                () -> BlockEntityType.Builder.<TileMobMagnet>of(TileMobMagnet::new, MOB_MAGNET.get()).build(null));
        MENU_MOB_MAGNET = TTRegistry.MENUS.register(LibBlockNames.MOB_MAGNET,
                () -> new MenuType<>(ContainerMobMagnet::new, FeatureFlags.DEFAULT_FLAGS));

        // Material.iron, hardness 3, resistance 50, metal step sound. 1.7.10 named the block "spawner".
        SUMMON = TTRegistry.block(LibBlockNames.SPAWNER, () -> new BlockSummon(BlockBehaviour.Properties.of()
                .mapColor(MapColor.METAL).strength(3.0F, 50.0F).sound(SoundType.METAL).requiresCorrectToolForDrops()
                .noOcclusion()));
        TILE_SUMMON = TTRegistry.BLOCK_ENTITIES.register(LibBlockNames.SPAWNER,
                () -> BlockEntityType.Builder.<TileSummon>of(TileSummon::new, SUMMON.get()).build(null));

        TTRegistry.item(LibItemNames.SOUL_MOULD, () -> new ItemSoulMould(new Item.Properties()));
        TTRegistry.item(LibItemNames.BLOOD_SWORD, () -> new ItemBloodSword(new Item.Properties()));
        // The creative tab lists the basic and the infused soul aspect of every aspect, in the order of 1.7.10.
        for (Aspect aspect : ItemMobAspect.ASPECTS) {
            for (ItemMobAspect.Form form : ItemMobAspect.Form.values()) {
                TTRegistry.item(ItemMobAspect.path(aspect, form), () -> new ItemMobAspect(aspect, form));
            }
        }
        TTRegistry.hiddenItem(LibItemNames.MOB_DISPLAY, () -> new ItemMobDisplay(new Item.Properties()));

        ItemBloodSword.registerEvents();
    }

    private static BlockBehaviour.Properties magnetProperties() {
        return BlockBehaviour.Properties.of().mapColor(MapColor.METAL).strength(1.7F, 1.0F).sound(SoundType.WOOD)
                .requiresCorrectToolForDrops().noOcclusion();
    }

    @Override
    public void ready() {}

    @Override
    public void registerClient() {
        thaumic.tinkerer.client.MachinesClient.register();
    }
}
