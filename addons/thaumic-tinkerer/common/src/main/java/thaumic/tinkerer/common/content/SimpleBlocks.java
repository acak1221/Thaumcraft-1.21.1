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

import dev.architectury.registry.registries.RegistrySupplier;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.entity.BlockEntityType;
import thaumcraft.api.aspects.Aspect;
import thaumic.tinkerer.common.block.BlockForcefield;
import thaumic.tinkerer.common.block.BlockGaseousLight;
import thaumic.tinkerer.common.block.BlockGaseousShadow;
import thaumic.tinkerer.common.block.BlockInfusedFarmland;
import thaumic.tinkerer.common.block.BlockInfusedGrain;
import thaumic.tinkerer.common.block.BlockNitorGas;
import thaumic.tinkerer.common.block.BlockPlatform;
import thaumic.tinkerer.common.block.BlockTravelSlab;
import thaumic.tinkerer.common.block.BlockTravelStairs;
import thaumic.tinkerer.common.block.BlockWardSlab;
import thaumic.tinkerer.common.block.fire.BlockFireAir;
import thaumic.tinkerer.common.block.fire.BlockFireBase;
import thaumic.tinkerer.common.block.fire.BlockFireChaos;
import thaumic.tinkerer.common.block.fire.BlockFireEarth;
import thaumic.tinkerer.common.block.fire.BlockFireIgnis;
import thaumic.tinkerer.common.block.fire.BlockFireOrder;
import thaumic.tinkerer.common.block.fire.BlockFireWater;
import thaumic.tinkerer.common.block.quartz.BlockDarkQuartz;
import thaumic.tinkerer.common.block.quartz.BlockDarkQuartzSlab;
import thaumic.tinkerer.common.block.quartz.BlockDarkQuartzStairs;
import thaumic.tinkerer.common.block.tile.TileCamo;
import thaumic.tinkerer.common.block.tile.TileForcefield;
import thaumic.tinkerer.common.block.tile.TileInfusedFarmland;
import thaumic.tinkerer.common.block.tile.TileInfusedGrain;
import thaumic.tinkerer.common.block.tile.TileWardedSlab;
import thaumic.tinkerer.common.core.commands.SetTendencyCommand;
import thaumic.tinkerer.common.core.helper.AspectCropLootManager;
import thaumic.tinkerer.common.core.helper.BonemealEventHandler;
import thaumic.tinkerer.common.core.helper.NumericAspectHelper;
import thaumic.tinkerer.common.item.InfusedSeedComponents;
import thaumic.tinkerer.common.item.ItemBlockFire;
import thaumic.tinkerer.common.item.ItemBrightNitor;
import thaumic.tinkerer.common.item.ItemGas;
import thaumic.tinkerer.common.item.ItemGasRemover;
import thaumic.tinkerer.common.item.ItemInfusedGrain;
import thaumic.tinkerer.common.item.ItemInfusedPotion;
import thaumic.tinkerer.common.item.ItemInfusedSeeds;
import thaumic.tinkerer.common.item.quartz.ItemDarkQuartz;
import thaumic.tinkerer.common.lib.LibBlockNames;
import thaumic.tinkerer.common.lib.LibItemNames;
import thaumic.tinkerer.common.potion.ModPotions;
import thaumic.tinkerer.common.potion.PotionEffectHandler;
import thaumic.tinkerer.common.registry.TTRegistry;

/**
 * Feature group: environment blocks: dark quartz, slabs, stairs, gases, platform, imbued fire, infused crops,
 * forcefield, camouflage; potions.
 *
 * <p>Flattening of the 1.7.10 metadata, in one place (the full list is docs/deviations/SimpleBlocks.md): the dark
 * quartz block (meta 0 block, 1 chiseled, 2 pillar) is {@code dark_quartz}, {@code dark_quartz_chiseled},
 * {@code dark_quartz_pillar}; the half and full slab blocks are one slab with the vanilla double state; the infused
 * grain (meta air, fire, earth, water) and the infused potion are four items each ({@code infused_grain_aer},
 * {@code infused_potion_ignis}, ...); the infused seeds are one item with the aspect as a data component; the age of
 * the crop and of the fire is a block-state property.</p>
 */
public final class SimpleBlocks implements ContentGroup {
    public static final SimpleBlocks INSTANCE = new SimpleBlocks();

    /** The registry path of the infused crop (the constant of the lead has a capital letter in it, which is no valid path). */
    public static final String INFUSED_GRAIN_BLOCK = ItemInfusedSeeds.GRAIN_BLOCK_PATH;

    // ---- blocks -----------------------------------------------------------------------------------------------
    public static RegistrySupplier<Block> DARK_QUARTZ;
    public static RegistrySupplier<Block> DARK_QUARTZ_CHISELED;
    public static RegistrySupplier<Block> DARK_QUARTZ_PILLAR;
    public static RegistrySupplier<BlockDarkQuartzSlab> DARK_QUARTZ_SLAB;
    public static RegistrySupplier<BlockDarkQuartzStairs> DARK_QUARTZ_STAIRS;
    public static RegistrySupplier<BlockTravelSlab> TRAVEL_SLAB;
    public static RegistrySupplier<BlockTravelStairs> TRAVEL_STAIRS;
    public static RegistrySupplier<BlockWardSlab> WARD_SLAB;
    public static RegistrySupplier<BlockPlatform> PLATFORM;
    public static RegistrySupplier<BlockGaseousLight> GASEOUS_LIGHT;
    public static RegistrySupplier<BlockGaseousShadow> GASEOUS_SHADOW;
    public static RegistrySupplier<BlockNitorGas> NITOR_GAS;
    public static RegistrySupplier<BlockForcefield> FORCEFIELD;
    public static RegistrySupplier<BlockInfusedFarmland> INFUSED_FARMLAND;
    public static RegistrySupplier<BlockInfusedGrain> INFUSED_GRAIN;
    public static RegistrySupplier<BlockFireAir> FIRE_AIR;
    public static RegistrySupplier<BlockFireWater> FIRE_WATER;
    public static RegistrySupplier<BlockFireEarth> FIRE_EARTH;
    public static RegistrySupplier<BlockFireIgnis> FIRE_FIRE;
    public static RegistrySupplier<BlockFireOrder> FIRE_ORDER;
    public static RegistrySupplier<BlockFireChaos> FIRE_CHAOS;

    // ---- block entities (1.7.10 TileCamo, TileWardedSlab, TileForcefield, TileInfusedFarmland, TileInfusedGrain) -----
    public static RegistrySupplier<BlockEntityType<TileCamo>> TILE_CAMO;
    public static RegistrySupplier<BlockEntityType<TileWardedSlab>> TILE_WARDED_SLAB;
    public static RegistrySupplier<BlockEntityType<TileForcefield>> TILE_FORCEFIELD;
    public static RegistrySupplier<BlockEntityType<TileInfusedFarmland>> TILE_INFUSED_FARMLAND;
    public static RegistrySupplier<BlockEntityType<TileInfusedGrain>> TILE_INFUSED_GRAIN;

    // ---- items ------------------------------------------------------------------------------------------------
    public static RegistrySupplier<ItemDarkQuartz> DARK_QUARTZ_ITEM;
    public static RegistrySupplier<ItemGas> GASEOUS_SHADOW_ITEM;
    public static RegistrySupplier<ItemGas> GASEOUS_LIGHT_ITEM;
    public static RegistrySupplier<ItemGasRemover> GAS_REMOVER;
    public static RegistrySupplier<ItemBrightNitor> BRIGHT_NITOR;
    public static RegistrySupplier<ItemInfusedSeeds> INFUSED_SEEDS;
    public static final RegistrySupplier<?>[] INFUSED_GRAIN_ITEMS = new RegistrySupplier<?>[4];
    public static final RegistrySupplier<?>[] INFUSED_POTION_ITEMS = new RegistrySupplier<?>[4];

    private SimpleBlocks() {}

    @Override
    public void register() {
        InfusedSeedComponents.register();
        ModPotions.initPotions();
        registerBlocks();
        registerBlockEntities();
        registerItems();
        PotionEffectHandler.register();
        BonemealEventHandler.register();
        SetTendencyCommand.register();
        NumericAspectHelper.init();
    }

    private static void registerBlocks() {
        // dark quartz: the three metadata values are three blocks
        DARK_QUARTZ = TTRegistry.block(LibBlockNames.DARK_QUARTZ, () -> new BlockDarkQuartz(BlockDarkQuartz.defaultProperties()));
        DARK_QUARTZ_CHISELED = TTRegistry.block(LibBlockNames.DARK_QUARTZ_CHISELED,
                () -> new Block(BlockDarkQuartz.defaultProperties()));
        DARK_QUARTZ_PILLAR = TTRegistry.block(LibBlockNames.DARK_QUARTZ_PILLAR,
                () -> new RotatedPillarBlock(BlockDarkQuartz.defaultProperties()));
        DARK_QUARTZ_SLAB = TTRegistry.block(LibBlockNames.DARK_QUARTZ_SLAB,
                () -> new BlockDarkQuartzSlab(BlockDarkQuartz.defaultProperties()));
        DARK_QUARTZ_STAIRS = TTRegistry.block(LibBlockNames.DARK_QUARTZ_STAIRS,
                () -> new BlockDarkQuartzStairs(DARK_QUARTZ.get().defaultBlockState(), BlockDarkQuartz.defaultProperties()));

        TRAVEL_SLAB = TTRegistry.block(LibBlockNames.TRAVEL_SLAB, () -> new BlockTravelSlab(BlockTravelSlab.defaultProperties()));
        TRAVEL_STAIRS = TTRegistry.block(LibBlockNames.TRAVEL_STAIRS,
                () -> new BlockTravelStairs(Blocks.STONE.defaultBlockState(), BlockTravelStairs.defaultProperties()));
        WARD_SLAB = TTRegistry.block(LibBlockNames.WARD_SLAB, () -> new BlockWardSlab(BlockWardSlab.defaultProperties()));

        PLATFORM = TTRegistry.block(LibBlockNames.PLATFORM, () -> new BlockPlatform(BlockPlatform.defaultProperties()));

        // the gases and the forcefield have no item
        GASEOUS_LIGHT = TTRegistry.blockWithoutItem(LibBlockNames.GASEOUS_LIGHT,
                () -> new BlockGaseousLight(BlockGaseousLight.defaultProperties()));
        GASEOUS_SHADOW = TTRegistry.blockWithoutItem(LibBlockNames.GASEOUS_SHADOW,
                () -> new BlockGaseousShadow(BlockGaseousShadow.defaultProperties()));
        NITOR_GAS = TTRegistry.blockWithoutItem(LibBlockNames.NITOR_GAS, () -> new BlockNitorGas(BlockNitorGas.defaultProperties()));
        FORCEFIELD = TTRegistry.blockWithoutItem(LibBlockNames.FORCEFIELD,
                () -> new BlockForcefield(BlockForcefield.defaultProperties()));

        // the crops: farmland and grain have no item of their own (the seeds plant them)
        INFUSED_FARMLAND = TTRegistry.blockWithoutItem(LibBlockNames.BLOCK_INFUSED_FARMLAND,
                () -> new BlockInfusedFarmland(BlockInfusedFarmland.defaultProperties()));
        INFUSED_GRAIN = TTRegistry.blockWithoutItem(INFUSED_GRAIN_BLOCK, () -> new BlockInfusedGrain(BlockInfusedGrain.defaultProperties()));

        // the six fires have an item that places them anywhere
        FIRE_AIR = fire(LibBlockNames.BLOCK_FIRE_AIR, () -> new BlockFireAir(BlockFireBase.defaultProperties()));
        FIRE_WATER = fire(LibBlockNames.BLOCK_FIRE_WATER, () -> new BlockFireWater(BlockFireBase.defaultProperties()));
        FIRE_EARTH = fire(LibBlockNames.BLOCK_FIRE_EARTH, () -> new BlockFireEarth(BlockFireBase.defaultProperties()));
        FIRE_FIRE = fire(LibBlockNames.BLOCK_FIRE_FIRE, () -> new BlockFireIgnis(BlockFireBase.defaultProperties()));
        FIRE_ORDER = fire(LibBlockNames.BLOCK_FIRE_ORDER, () -> new BlockFireOrder(BlockFireBase.defaultProperties()));
        FIRE_CHAOS = fire(LibBlockNames.BLOCK_FIRE_CHAOS, () -> new BlockFireChaos(BlockFireBase.defaultProperties()));
    }

    private static <T extends BlockFireBase> RegistrySupplier<T> fire(String path, java.util.function.Supplier<T> factory) {
        return TTRegistry.block(path, factory, block -> new ItemBlockFire(block, new Item.Properties()));
    }

    private static void registerBlockEntities() {
        TILE_CAMO = TTRegistry.BLOCK_ENTITIES.register("camo",
                () -> BlockEntityType.Builder.of(TileCamo::new, PLATFORM.get()).build(null));
        TILE_WARDED_SLAB = TTRegistry.BLOCK_ENTITIES.register("warded_slab",
                () -> BlockEntityType.Builder.of(TileWardedSlab::new, WARD_SLAB.get()).build(null));
        TILE_FORCEFIELD = TTRegistry.BLOCK_ENTITIES.register("forcefield",
                () -> BlockEntityType.Builder.of(TileForcefield::new, FORCEFIELD.get()).build(null));
        TILE_INFUSED_FARMLAND = TTRegistry.BLOCK_ENTITIES.register("infused_farmland",
                () -> BlockEntityType.Builder.of(TileInfusedFarmland::new, INFUSED_FARMLAND.get()).build(null));
        TILE_INFUSED_GRAIN = TTRegistry.BLOCK_ENTITIES.register("infused_grain",
                () -> BlockEntityType.Builder.of(TileInfusedGrain::new, INFUSED_GRAIN.get()).build(null));
    }

    private static void registerItems() {
        DARK_QUARTZ_ITEM = TTRegistry.item(LibItemNames.DARK_QUARTZ, () -> new ItemDarkQuartz(new Item.Properties()));

        // the default item of 1.7.10 is the phial of shadow, the meta item the one of light
        GASEOUS_SHADOW_ITEM = TTRegistry.item(LibItemNames.GASEOUS_SHADOW, () -> new ItemGas(true, new Item.Properties()));
        GASEOUS_LIGHT_ITEM = TTRegistry.item(LibItemNames.GASEOUS_LIGHT, () -> new ItemGas(false, new Item.Properties()));
        GAS_REMOVER = TTRegistry.item(LibItemNames.GAS_REMOVER, () -> new ItemGasRemover(new Item.Properties().rarity(Rarity.UNCOMMON)));
        BRIGHT_NITOR = TTRegistry.item(LibItemNames.BRIGHT_NTIOR, () -> new ItemBrightNitor(new Item.Properties()));

        for (int i = 0; i < ItemInfusedGrain.ASPECTS.length; i++) {
            final int variant = i;
            INFUSED_GRAIN_ITEMS[i] = TTRegistry.item(ItemInfusedGrain.path(i), () -> new ItemInfusedGrain(variant, new Item.Properties()));
            INFUSED_POTION_ITEMS[i] = TTRegistry.item(ItemInfusedPotion.path(i),
                    () -> new ItemInfusedPotion(variant, new Item.Properties()));
        }

        INFUSED_SEEDS = TTRegistry.item(LibItemNames.INFUSED_SEEDS, () -> new ItemInfusedSeeds(new Item.Properties().rarity(Rarity.RARE)));
        // 1.7.10 getSubItems: the seeds of every primal aspect, in the order of Aspect.getPrimalAspects()
        // (aer, terra, ignis, aqua, ordo, perditio)
        for (Aspect primal : Aspect.getPrimalAspects()) {
            TTRegistry.addTabStack(() -> ItemInfusedSeeds.creativeStack(primal));
        }
    }

    @Override
    public void ready() {
        AspectCropLootManager.populateLootMap();
    }

    @Override
    public void registerClient() {
        thaumic.tinkerer.client.SimpleBlocksClient.register();
    }
}
