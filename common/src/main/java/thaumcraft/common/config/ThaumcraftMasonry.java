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
import thaumcraft.common.blocks.BlockArcaneStoneStairs;
import thaumcraft.common.blocks.BlockGolemFetter;

/** Original cosmetic-solid metadata 6/7 and their ordinary stone stairs/slab, and metadata 9/10, the Golem Fetter. */
public final class ThaumcraftMasonry {
    private static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(Thaumcraft.MODID, Registries.BLOCK);
    private static final DeferredRegister<Item> ITEMS = DeferredRegister.create(Thaumcraft.MODID, Registries.ITEM);

    // BlockCosmeticSolid:213-252 overrides the base hardness2/resistance10 for metadata6/7.
    public static final RegistrySupplier<Block> ARCANE_STONE = block("arcane_stone",
            () -> new Block(stone(4.0F, 20.0F).randomTicks()));
    public static final RegistrySupplier<Block> ARCANE_STONE_BRICKS = block("arcane_stone_bricks",
            () -> new Block(stone(4.0F, 20.0F).randomTicks()));
    // BlockStairs copies the reference's base hardness2, not its world/metadata override.
    public static final RegistrySupplier<BlockArcaneStoneStairs> STAIRS = block("arcane_stone_brick_stairs",
            () -> new BlockArcaneStoneStairs(stone(2.0F, 6.0F)));
    // ConfigBlocks:299/301 supplies these values for BOTH original slab halves/doubles.
    public static final RegistrySupplier<BlockArcaneStoneSlab> SLAB = block("arcane_stone_brick_slab",
            () -> new BlockArcaneStoneSlab(stone(2.0F, 6.0F)));

    // BlockCosmeticSolid base values (hardness 2, resistance 10 = explosion resistance 6 in today's units); metadata 9/10 override neither.
    public static final RegistrySupplier<BlockGolemFetter> GOLEM_FETTER = block("golem_fetter",
            () -> new BlockGolemFetter(stone(2.0F, 6.0F)));

    private ThaumcraftMasonry() {}

    private static BlockBehaviour.Properties stone(float hardness, float resistance) {
        // Original Material.rock: any pickaxe, no explicit higher harvest tier,
        // no flammability/fire-spread registration and no emitted light.
        return BlockBehaviour.Properties.of().mapColor(MapColor.STONE).strength(hardness, resistance)
                .sound(SoundType.STONE).requiresCorrectToolForDrops();
    }

    private static <T extends Block> RegistrySupplier<T> block(String id, Supplier<T> factory) {
        RegistrySupplier<T> block = BLOCKS.register(id, factory);
        ITEMS.register(id, () -> new BlockItem(block.get(), new Item.Properties()));
        return block;
    }

    /** Call after ThaumcraftContent.register(); JSON models need no client initializer. */
    public static void register() {
        BLOCKS.register();
        ITEMS.register();
        CreativeTabRegistry.appendStack(ThaumcraftContent.TAB, () -> new ItemStack(ARCANE_STONE.get()));
        CreativeTabRegistry.appendStack(ThaumcraftContent.TAB, () -> new ItemStack(ARCANE_STONE_BRICKS.get()));
        CreativeTabRegistry.appendStack(ThaumcraftContent.TAB, () -> new ItemStack(STAIRS.get()));
        CreativeTabRegistry.appendStack(ThaumcraftContent.TAB, () -> new ItemStack(SLAB.get()));
        CreativeTabRegistry.appendStack(ThaumcraftContent.TAB, () -> new ItemStack(GOLEM_FETTER.get()));
    }
}
