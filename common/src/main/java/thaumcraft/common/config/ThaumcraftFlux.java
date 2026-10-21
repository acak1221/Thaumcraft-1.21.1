package thaumcraft.common.config;

import dev.architectury.registry.registries.CreativeTabRegistry;
import dev.architectury.registry.registries.DeferredRegister;
import dev.architectury.registry.registries.RegistrySupplier;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;
import thaumcraft.common.Thaumcraft;
import thaumcraft.common.blocks.BlockFluxGas;
import thaumcraft.common.blocks.BlockFluxGasItem;
import thaumcraft.common.blocks.BlockFluxGoo;
import thaumcraft.common.blocks.BlockFluxGooItem;
import thaumcraft.common.blocks.FluxFiniteBlock;
import thaumcraft.common.lib.potions.PotionInfectiousVisExhaust;
import thaumcraft.common.lib.potions.PotionVisExhaust;

/** TC4 ConfigBlocks:163-164; these are finite blocks, not vanilla source fluids. */
public final class ThaumcraftFlux {
    private static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(Thaumcraft.MODID, Registries.BLOCK);
    private static final DeferredRegister<Item> ITEMS = DeferredRegister.create(Thaumcraft.MODID, Registries.ITEMS);
    private static final DeferredRegister<MobEffect> EFFECTS = DeferredRegister.create(Thaumcraft.MODID, Registries.MOB_EFFECT);

    public static final RegistrySupplier<BlockFluxGas> FLUX_GAS = BLOCKS.register("flux_gas", () -> new BlockFluxGas(properties()));
    public static final RegistrySupplier<BlockFluxGoo> FLUX_GOO = BLOCKS.register("flux_goo", () -> new BlockFluxGoo(properties()));
    /**
     * The original items of the two finite fluids.
     *
     * <p>Original {@code ConfigBlocks:295-296} registers both blocks with their own item class
     * ({@code BlockFluxGooItem} and {@code BlockFluxGasItem}), and both items place the block
     * with the original metadata eight.</p>
     */
    public static final RegistrySupplier<Item> FLUX_GAS_ITEM = ITEMS.register("flux_gas",
            () -> new BlockFluxGasItem(FLUX_GAS.get(), new Item.Properties()));
    public static final RegistrySupplier<Item> FLUX_GOO_ITEM = ITEMS.register("flux_goo",
            () -> new BlockFluxGooItem(FLUX_GOO.get(), new Item.Properties()));
    private static final RegistrySupplier<MobEffect> VIS_EXHAUST = EFFECTS.register("vis_exhaust", PotionVisExhaust::new);
    private static final RegistrySupplier<MobEffect> INFECTIOUS_VIS_EXHAUST = EFFECTS.register("infectious_vis_exhaust", PotionInfectiousVisExhaust::new);

    private ThaumcraftFlux() {}

    private static BlockBehaviour.Properties properties() {
        return BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_PURPLE).noCollission().noOcclusion()
                .randomTicks().noLootTable().pushReaction(PushReaction.DESTROY)
                .isSuffocating((state, level, pos) -> false).isViewBlocking((state, level, pos) -> false)
                // BlockFluidBase scales luminosity by metadata, NOT metadata+1.
                .lightLevel(state -> state.getValue(FluxFiniteBlock.LEVEL) * 7 / 8);
    }

    public static void register() {
        BLOCKS.register();
        ITEMS.register();
        EFFECTS.register();
        // Original BlockFluxGas:29 and BlockFluxGoo:34 put both blocks in the tab of the mod.
        CreativeTabRegistry.append(ThaumcraftContent.TAB, FLUX_GAS_ITEM, FLUX_GOO_ITEM);
    }

    // Architectury's RegistrySupplier implements Holder, but is NOT the actual
    // native registry reference. Using it in effects fails NBT/network encoding
    // and misses reloaded effects because LivingEntity keys by holder identity.
    public static Holder<MobEffect> visExhaust() { return BuiltInRegistries.MOB_EFFECT.wrapAsHolder(VIS_EXHAUST.get()); }
    public static Holder<MobEffect> infectiousVisExhaust() { return BuiltInRegistries.MOB_EFFECT.wrapAsHolder(INFECTIOUS_VIS_EXHAUST.get()); }

    /**
     * Original TileCrucible.spill:186-215, including conditional RNG consumption:
     * chance, air-only boolean, goo-before-gas merge, then x/y/z and air-only boolean.
     * Loaded terrain is identical; an unavailable target is never force-loaded.
     */
    public static void spill(ServerLevel level, BlockPos crucible) {
        if (level.random.nextInt(4) != 0) return;
        BlockPos above = crucible.above();
        if (!FluxFiniteBlock.loaded(level, above)) return;
        BlockState state = level.getBlockState(above);
        if (state.isAir()) {
            placeSpill(level, above);
        } else if (state.is(FLUX_GOO.get()) && state.getValue(FluxFiniteBlock.LEVEL) < 7) {
            level.setBlock(above, state.setValue(FluxFiniteBlock.LEVEL, state.getValue(FluxFiniteBlock.LEVEL) + 1), Block.UPDATE_ALL);
        } else if (state.is(FLUX_GAS.get()) && state.getValue(FluxFiniteBlock.LEVEL) < 7) {
            level.setBlock(above, state.setValue(FluxFiniteBlock.LEVEL, state.getValue(FluxFiniteBlock.LEVEL) + 1), Block.UPDATE_ALL);
        } else {
            int x = -1 + level.random.nextInt(3);
            int y = -1 + level.random.nextInt(3);
            int z = -1 + level.random.nextInt(3);
            BlockPos target = crucible.offset(x, y, z);
            if (FluxFiniteBlock.loaded(level, target) && level.getBlockState(target).isAir()) placeSpill(level, target);
        }
    }

    private static void placeSpill(ServerLevel level, BlockPos pos) {
        FluxFiniteBlock block = level.random.nextBoolean() ? FLUX_GAS.get() : FLUX_GOO.get();
        level.setBlock(pos, block.withGeometry(block.defaultBlockState(), level, pos), Block.UPDATE_ALL);
    }

    /** Positive surcharge; subtract from gear discount before the original pricing calculation. */
    public static float visPenalty(Player player) {
        if (player == null) return 0;
        MobEffectInstance normal = player.getEffect(visExhaust());
        MobEffectInstance infectious = player.getEffect(infectiousVisExhaust());
        int amplifier = Math.max(normal == null ? -1 : normal.getAmplifier(), infectious == null ? -1 : infectious.getAmplifier());
        return amplifier < 0 ? 0 : (amplifier + 1) * .1F;
    }
}
