package thaumcraft.common.config;

import dev.architectury.registry.CreativeTabRegistry;
import dev.architectury.registry.registries.DeferredRegister;
import dev.architectury.registry.registries.RegistrySupplier;
import java.util.function.Supplier;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import thaumcraft.common.Thaumcraft;
import thaumcraft.common.blocks.BlockArcaneStoneSlab;
import thaumcraft.common.blocks.BlockCosmeticWoodStairs;

/**
 * The Greatwood and Silverwood stairs (ConfigBlocks:286-287, recipes ConfigRecipes:415-416) and the wood slab (blockSlabWood /
 * blockDoubleSlabWood, metadata 0 Greatwood and 1 Silverwood, ConfigRecipes:417-418). Stairs take the planks' strength (2.5 / 6); the
 * slab sets 2.0 hardness, resistance 5 stored (exposed 3.0) and wood sound. Neither registers fire spread in 1.7, so neither burns.
 * The slab reuses the arcane stone slab class: it has only the shared native half/double behaviour and the dry opacity-0 quirk.
 */
public final class ThaumcraftCosmeticWood {
    private static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(Thaumcraft.MODID, Registries.BLOCK);
    private static final DeferredRegister<Item> ITEMS = DeferredRegister.create(Thaumcraft.MODID, Registries.ITEM);

    public static final RegistrySupplier<BlockCosmeticWoodStairs> GREATWOOD_STAIRS = block("greatwood_stairs",
            () -> new BlockCosmeticWoodStairs(ThaumcraftNaturals.GREATWOOD_PLANKS.get().defaultBlockState(), stairs(MapColor.COLOR_BROWN)));
    public static final RegistrySupplier<BlockCosmeticWoodStairs> SILVERWOOD_STAIRS = block("silverwood_stairs",
            () -> new BlockCosmeticWoodStairs(ThaumcraftNaturals.SILVERWOOD_PLANKS.get().defaultBlockState(), stairs(MapColor.QUARTZ)));
    public static final RegistrySupplier<BlockArcaneStoneSlab> GREATWOOD_SLAB = block("greatwood_slab",
            () -> new BlockArcaneStoneSlab(slab(MapColor.COLOR_BROWN)));
    public static final RegistrySupplier<BlockArcaneStoneSlab> SILVERWOOD_SLAB = block("silverwood_slab",
            () -> new BlockArcaneStoneSlab(slab(MapColor.QUARTZ)));

    private static boolean registered;
    private ThaumcraftCosmeticWood() {}

    private static BlockBehaviour.Properties stairs(MapColor color) { return BlockBehaviour.Properties.of().mapColor(color).strength(2.5F, 6.0F).sound(SoundType.WOOD); }

    private static BlockBehaviour.Properties slab(MapColor color) { return BlockBehaviour.Properties.of().mapColor(color).strength(2.0F, 3.0F).sound(SoundType.WOOD); }

    private static <T extends Block> RegistrySupplier<T> block(String id, Supplier<T> factory) {
        RegistrySupplier<T> block = BLOCKS.register(id, factory);
        ITEMS.register(id, () -> new BlockItem(block.get(), new Item.Properties()));
        return block;
    }

    /** Call after ThaumcraftNaturals.register(): the stairs are built over its planks. */
    public static void register() {
        if (registered) return;
        registered = true;
        BLOCKS.register();
        ITEMS.register();
        CreativeTabRegistry.appendStack(ThaumcraftContent.TAB, () -> new ItemStack(GREATWOOD_STAIRS.get()));
        CreativeTabRegistry.appendStack(ThaumcraftContent.TAB, () -> new ItemStack(SILVERWOOD_STAIRS.get()));
        CreativeTabRegistry.appendStack(ThaumcraftContent.TAB, () -> new ItemStack(GREATWOOD_SLAB.get()));
        CreativeTabRegistry.appendStack(ThaumcraftContent.TAB, () -> new ItemStack(SILVERWOOD_SLAB.get()));
    }
}
