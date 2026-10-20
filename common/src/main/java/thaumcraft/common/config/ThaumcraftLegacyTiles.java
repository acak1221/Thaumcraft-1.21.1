package thaumcraft.common.config;

import dev.architectury.registry.CreativeTabRegistry;
import dev.architectury.registry.registries.DeferredRegister;
import dev.architectury.registry.registries.RegistrySupplier;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import thaumcraft.common.Thaumcraft;
import thaumcraft.common.blocks.BlockArcaneFurnace;
import thaumcraft.common.blocks.BlockArcaneFurnaceNozzle;
import thaumcraft.common.blocks.BlockCosmeticOpaque;
import thaumcraft.common.blocks.BlockCosmeticSolid;
import thaumcraft.common.blocks.BlockCosmeticStairs;
import thaumcraft.common.blocks.BlockCosmeticStoneSlab;
import thaumcraft.common.blocks.BlockCosmeticWoodSlab;
import thaumcraft.common.blocks.BlockFluidDeath;
import thaumcraft.common.blocks.BlockWandPedestal;
import thaumcraft.common.lib.tiles.LegacyTileRules;
import thaumcraft.common.tiles.TileArcaneFurnace;
import thaumcraft.common.tiles.TileArcaneFurnaceNozzle;
import thaumcraft.common.tiles.TileNode;
import thaumcraft.common.tiles.TileOwned;
import thaumcraft.common.tiles.TileWandPedestal;

/**
 * The remaining TC4 blocks and tiles of the legacy port: the arcane furnace, the death fluid, the cosmetic
 * blocks and the wand pedestal.
 * <p>
 * The registration lives in its own file so that the native content of the other subsystems stays untouched.
 * Call {@link #register()} after {@code ThaumcraftContent.register()}.
 */
public final class ThaumcraftLegacyTiles {
    private static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(Thaumcraft.MODID, Registries.BLOCK);
    private static final DeferredRegister<Item> ITEMS = DeferredRegister.create(Thaumcraft.MODID, Registries.ITEM);
    private static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES = DeferredRegister.create(Thaumcraft.MODID, Registries.BLOCK_ENTITY_TYPE);
    private static final DeferredRegister<net.minecraft.world.level.material.Fluid> FLUIDS = DeferredRegister.create(Thaumcraft.MODID, Registries.FLUID);

    /** The source of the death fluid. */
    public static final RegistrySupplier<BlockFluidDeath.Source> DEATH_FLUID_SOURCE =
            FLUIDS.register("death_fluid", () -> new BlockFluidDeath.Source());
    /** The flowing death fluid. */
    public static final RegistrySupplier<BlockFluidDeath.Flowing> DEATH_FLUID_FLOWING =
            FLUIDS.register("death_fluid_flowing", () -> new BlockFluidDeath.Flowing());
    /** The death fluid block. */
    public static final RegistrySupplier<BlockFluidDeath> DEATH_FLUID = BLOCKS.register("death_fluid",
            // Original BlockFluidFinite: the hardness of one hundred and the light of the fluid.
            () -> new BlockFluidDeath(DEATH_FLUID_SOURCE.get(), fluid()));

    /** The arcane furnace: one block with the eleven original metadata values. */
    public static final RegistrySupplier<BlockArcaneFurnace> ARCANE_FURNACE = BLOCKS.register("arcane_furnace",
            // Original setHardness10 / setResistance500 (stored x3, exposed /5: 300), stone.
            () -> new BlockArcaneFurnace(arcaneFurnace()));
    /** The output nozzle of the arcane furnace. */
    public static final RegistrySupplier<BlockArcaneFurnaceNozzle> ARCANE_FURNACE_NOZZLE = BLOCKS.register("arcane_furnace_nozzle",
            () -> new BlockArcaneFurnaceNozzle(arcaneFurnace()));

    /** The obsidian tile of the original cosmetic solid. */
    public static final RegistrySupplier<BlockCosmeticSolid> OBSIDIAN_TILE = BLOCKS.register("obsidian_tile",
            () -> new BlockCosmeticSolid(cosmetic(1), 1));
    /** The paving stone of the original cosmetic solid. */
    public static final RegistrySupplier<BlockCosmeticSolid> PAVING_STONE_TRAVEL = BLOCKS.register("paving_stone_travel",
            () -> new BlockCosmeticSolid(cosmetic(2), 2));
    /** The warding paving stone of the original cosmetic solid. */
    public static final RegistrySupplier<BlockCosmeticSolid> PAVING_STONE_WARDING = BLOCKS.register("paving_stone_warding",
            () -> new BlockCosmeticSolid(cosmetic(3), 3));
    /** The base of the obsidian totem; it holds a node. */
    public static final RegistrySupplier<BlockCosmeticSolid> OBSIDIAN_TOTEM_BASE = BLOCKS.register("obsidian_totem_base",
            () -> new BlockCosmeticSolid(cosmetic(8), 8));
    /** The eldritch stone of the original cosmetic solid. */
    public static final RegistrySupplier<BlockCosmeticSolid> ELDRITCH_STONE = BLOCKS.register("eldritch_stone",
            () -> new BlockCosmeticSolid(cosmetic(12), 12));
    /** The infused stone pillar of the original cosmetic solid. */
    public static final RegistrySupplier<BlockCosmeticSolid> INFUSED_STONE_PILLAR = BLOCKS.register("infused_stone_pillar",
            () -> new BlockCosmeticSolid(cosmetic(15), 15));
    /** The warded glass of the original cosmetic opaque; it remembers its owner. */
    public static final RegistrySupplier<BlockCosmeticOpaque> WARDED_GLASS = BLOCKS.register("warded_glass",
            () -> new BlockCosmeticOpaque(wardedGlass(), 2));
    /** The eldritch stairs of the original. */
    public static final RegistrySupplier<BlockCosmeticStairs> ELDRITCH_STAIRS = BLOCKS.register("eldritch_stairs",
            () -> new BlockCosmeticStairs(ELDRITCH_STONE.get().defaultBlockState(), stairs()));
    /** The arcane and eldritch half block of the original. */
    public static final RegistrySupplier<BlockCosmeticStoneSlab> COSMETIC_STONE_SLAB = BLOCKS.register("cosmetic_stone_slab",
            // Original setHardness2 / setResistance10 (stored x3, exposed /5: 6), stone.
            () -> new BlockCosmeticStoneSlab(stoneSlab()));
    /** The greatwood and silverwood half block of the original. */
    public static final RegistrySupplier<BlockCosmeticWoodSlab> COSMETIC_WOOD_SLAB = BLOCKS.register("cosmetic_wood_slab",
            // Original setHardness2 / setResistance5 (stored x3, exposed /5: 3), wood.
            () -> new BlockCosmeticWoodSlab(woodSlab()));
    /** The wand pedestal and its focus. */
    public static final RegistrySupplier<BlockWandPedestal> WAND_PEDESTAL = BLOCKS.register("wand_pedestal",
            // Original BlockStoneDevice: setHardness3 / setResistance25 (stored x3, exposed /5: 15), stone.
            () -> new BlockWandPedestal(stoneDevice()));

    public static final RegistrySupplier<BlockEntityType<TileArcaneFurnace>> ARCANE_FURNACE_BLOCK_ENTITY =
            BLOCK_ENTITIES.register("arcane_furnace",
                    () -> BlockEntityType.Builder.of(TileArcaneFurnace::new, ARCANE_FURNACE.get()).build(null));
    public static final RegistrySupplier<BlockEntityType<TileArcaneFurnaceNozzle>> ARCANE_FURNACE_NOZZLE_BLOCK_ENTITY =
            BLOCK_ENTITIES.register("arcane_furnace_nozzle",
                    () -> BlockEntityType.Builder.of(TileArcaneFurnaceNozzle::new, ARCANE_FURNACE.get()).build(null));
    public static final RegistrySupplier<BlockEntityType<TileOwned>> COSMETIC_OPAQUE_BLOCK_ENTITY =
            BLOCK_ENTITIES.register("warded_glass",
                    () -> BlockEntityType.Builder.of((pos, state) -> new TileOwned(ThaumcraftLegacyTiles.COSMETIC_OPAQUE_BLOCK_ENTITY.get(), pos, state),
                            WARDED_GLASS.get()).build(null));
    public static final RegistrySupplier<BlockEntityType<TileNode>> COSMETIC_SOLID_NODE_BLOCK_ENTITY =
            BLOCK_ENTITIES.register("obsidian_totem_base",
                    () -> BlockEntityType.Builder.of((pos, state) -> new TileNode(ThaumcraftLegacyTiles.COSMETIC_SOLID_NODE_BLOCK_ENTITY.get(), pos, state),
                            OBSIDIAN_TOTEM_BASE.get()).build(null));
    public static final RegistrySupplier<BlockEntityType<TileWandPedestal>> WAND_PEDESTAL_BLOCK_ENTITY =
            BLOCK_ENTITIES.register("wand_pedestal",
                    () -> BlockEntityType.Builder.of(TileWandPedestal::new, WAND_PEDESTAL.get()).build(null));

    private ThaumcraftLegacyTiles() {}

    /** The block properties of the legacy blocks, with the original hardness, resistance and sound. */
    private static BlockBehaviour.Properties fluid() {
        return BlockBehaviour.Properties.of().mapColor(MapColor.WATER)
                .strength(100.0F).noCollission().noOcclusion().dynamicShape().replaceable();
    }

    private static BlockBehaviour.Properties arcaneFurnace() {
        return BlockBehaviour.Properties.of().mapColor(MapColor.STONE)
                .strength(10.0F, 300.0F).sound(SoundType.STONE).noOcclusion()
                .lightLevel(state -> state.getBlock() instanceof BlockArcaneFurnace
                        && state.getValue(BlockArcaneFurnace.VARIANT) == 0 ? LegacyTileRules.FURNACE_LIGHT : 0);
    }

    /** The properties of a cosmetic solid variant, with its original hardness, resistance and light. */
    private static BlockBehaviour.Properties cosmetic(int variant) {
        return BlockBehaviour.Properties.of().mapColor(MapColor.STONE)
                .strength(LegacyTileRules.cosmeticSolidHardness(variant), LegacyTileRules.cosmeticSolidResistance(variant))
                .sound(SoundType.STONE)
                .lightLevel(state -> LegacyTileRules.cosmeticSolidLight(variant));
    }

    /** The warded glass is see through, unbreakable and immune to explosions. */
    private static BlockBehaviour.Properties wardedGlass() {
        return BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_LIGHT_GRAY)
                .strength(LegacyTileRules.cosmeticOpaqueHardness(2), LegacyTileRules.cosmeticOpaqueResistance(2))
                .sound(SoundType.STONE).noOcclusion();
    }

    private static BlockBehaviour.Properties stairs() {
        return BlockBehaviour.Properties.of().mapColor(MapColor.STONE)
                .strength(2.0F, 6.0F).sound(SoundType.STONE);
    }

    private static BlockBehaviour.Properties stoneSlab() {
        return BlockBehaviour.Properties.of().mapColor(MapColor.STONE)
                .strength(2.0F, 6.0F).sound(SoundType.STONE);
    }

    private static BlockBehaviour.Properties woodSlab() {
        return BlockBehaviour.Properties.of().mapColor(MapColor.WOOD)
                .strength(2.0F, 3.0F).sound(SoundType.WOOD);
    }

    private static BlockBehaviour.Properties stoneDevice() {
        return BlockBehaviour.Properties.of().mapColor(MapColor.STONE)
                .strength(3.0F, 15.0F).sound(SoundType.STONE).noOcclusion();
    }

    private static boolean registered;

    public static void register() {
        if (registered) return;
        registered = true;
        FLUIDS.register();
        BLOCKS.register();
        ITEMS.register();
        BLOCK_ENTITIES.register();
        List<RegistrySupplier<Item>> items = new ArrayList<>();
        // The arcane furnace and the death fluid have no item in the original either.
        items.add(ITEMS.register("obsidian_tile", () -> new BlockItem(OBSIDIAN_TILE.get(), new Item.Properties())));
        items.add(ITEMS.register("paving_stone_travel", () -> new BlockItem(PAVING_STONE_TRAVEL.get(), new Item.Properties())));
        items.add(ITEMS.register("paving_stone_warding", () -> new BlockItem(PAVING_STONE_WARDING.get(), new Item.Properties())));
        items.add(ITEMS.register("obsidian_totem_base", () -> new BlockItem(OBSIDIAN_TOTEM_BASE.get(), new Item.Properties())));
        items.add(ITEMS.register("eldritch_stone", () -> new BlockItem(ELDRITCH_STONE.get(), new Item.Properties())));
        items.add(ITEMS.register("infused_stone_pillar", () -> new BlockItem(INFUSED_STONE_PILLAR.get(), new Item.Properties())));
        items.add(ITEMS.register("warded_glass", () -> new BlockItem(WARDED_GLASS.get(), new Item.Properties())));
        items.add(ITEMS.register("eldritch_stairs", () -> new BlockItem(ELDRITCH_STAIRS.get(), new Item.Properties())));
        items.add(ITEMS.register("cosmetic_stone_slab", () -> new BlockItem(COSMETIC_STONE_SLAB.get(), new Item.Properties())));
        items.add(ITEMS.register("cosmetic_wood_slab", () -> new BlockItem(COSMETIC_WOOD_SLAB.get(), new Item.Properties())));
        items.add(ITEMS.register("wand_pedestal", () -> new BlockItem(WAND_PEDESTAL.get(), new Item.Properties())));
        CreativeTabRegistry.append(ThaumcraftContent.TAB, items.toArray(new RegistrySupplier[0]));
    }
}
