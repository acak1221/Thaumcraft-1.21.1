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
import thaumcraft.common.blocks.BlockFluidDeath;
import thaumcraft.common.blocks.BlockWandPedestal;
import thaumcraft.common.blocks.ItemWandPedestalFocus;
import thaumcraft.common.lib.tiles.LegacyTileRules;
import thaumcraft.common.tiles.TileArcaneFurnace;
import thaumcraft.common.tiles.TileArcaneFurnaceNozzle;
import thaumcraft.common.tiles.TileWandPedestal;

/**
 * The remaining TC4 blocks and tiles of the legacy port: the arcane furnace, the death fluid and the
 * wand pedestal.
 * <p>
 * The cosmetic block families of the original (the cosmetic solid, the cosmetic opaque, their slabs and
 * the eldritch stairs) are not registered here: they live in {@link ThaumcraftEldritchBlocks}, which keeps
 * the original 1.7.10 metadata as the {@code variant} block state of one block per hardness group.
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
            // Original BlockFluidFinite over BlockLiquid: setHardness100, so the hardness is one hundred
            // and the explosion resistance is one hundred as well (the 1.7 engine stored it as five
            // hundred and exposed a fifth of it). The light of the fluid is the light of the block.
            () -> new BlockFluidDeath(DEATH_FLUID_SOURCE.get(), fluid()));

    /** The arcane furnace: one block with the eleven original metadata values. */
    public static final RegistrySupplier<BlockArcaneFurnace> ARCANE_FURNACE = BLOCKS.register("arcane_furnace",
            // Original setHardness10 / setResistance500 (stored x3, exposed /5: 300), stone.
            () -> new BlockArcaneFurnace(arcaneFurnace()));
    /** The output nozzle of the arcane furnace. */
    public static final RegistrySupplier<BlockArcaneFurnaceNozzle> ARCANE_FURNACE_NOZZLE = BLOCKS.register("arcane_furnace_nozzle",
            () -> new BlockArcaneFurnaceNozzle(arcaneFurnace()));

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
    public static final RegistrySupplier<BlockEntityType<TileWandPedestal>> WAND_PEDESTAL_BLOCK_ENTITY =
            BLOCK_ENTITIES.register("wand_pedestal",
                    () -> BlockEntityType.Builder.of((pos, state) -> new TileWandPedestal(pos, state), WAND_PEDESTAL.get()).build(null));

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
        items.add(ITEMS.register("wand_pedestal", () -> new BlockItem(WAND_PEDESTAL.get(), new Item.Properties())));
        // The compound recharge focus, the original blockStoneDevice metadata eight: a 1.21.1 recipe result cannot carry the focus
        // block state, so the focus needs an item of its own (ConfigRecipes:342).
        items.add(ITEMS.register("wand_pedestal_focus", () -> new ItemWandPedestalFocus(WAND_PEDESTAL.get(), new Item.Properties())));
        CreativeTabRegistry.append(ThaumcraftContent.TAB, items.toArray(new RegistrySupplier[0]));
    }
}
