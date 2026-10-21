package thaumcraft.common.lib.utils;

import java.util.ArrayList;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.NetherWartBlock;
import net.minecraft.world.level.block.StemBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;

/**
 * Original TC4 {@code thaumcraft.common.lib.utils.CropUtils}: the crop registry of the growth lamps, the
 * harvesting focus and the golem replanter.
 *
 * <p>The original kept four lists of {@code blockUnlocalizedName + metadata} strings. 1.21.1 has no block
 * metadata, so the growth stage of a crop is its {@code AGE} block state property and the lists keep the same
 * shape with the key {@code <registry name>#<stage>}. The stages of a crop are the same numbers the original
 * metadata carried, so a stage 7 wheat is still stage 7.
 *
 * <p>The predicates that decide whether a block is a crop, whether it may still grow and which stage it is, are
 * pure and are covered by {@code thaumcraft.utils.LegacyUtilsTest}.
 */
public final class CropUtils {
    /** Original {@code standardCrops}: the crops the harvesting focus and the golem treat as ordinary. */
    public static final ArrayList<String> standardCrops = new ArrayList<>();
    /** Original {@code clickableCrops}: the crops the player may click instead of harvest. */
    public static final ArrayList<String> clickableCrops = new ArrayList<>();
    /** Original {@code stackedCrops}: the crops that grow in a stack of two, one above the other. */
    public static final ArrayList<String> stackedCrops = new ArrayList<>();
    /** Original {@code lampBlacklist}: the crops the growth lamp must not grow. */
    public static final ArrayList<String> lampBlacklist = new ArrayList<>();

    private CropUtils() {}

    /** The list key of a block stage: {@code <registry name>#<stage>}. */
    public static String key(Block block, int stage) {
        if (block == null) return "#" + stage;
        var id = BuiltInRegistries.BLOCK.getKey(block);
        // A block that the registry does not know has no registry name; it also
        // never matches a registered crop, but it must not crash the crop rules.
        return (id == null ? "unregistered:" + block.getClass().getName() : id.toString()) + "#" + stage;
    }

    /** The list key of a block state: {@code <registry name>#<stage>} of the state's growth stage. */
    public static String key(BlockState state) {
        return key(state.getBlock(), age(state));
    }

    /**
     * The growth stage of a block: the original's crop metadata.
     *
     * <p>1.21.1 keeps the growth stage in the {@code AGE} property. A block without such a property has the
     * metadata 0, exactly as in the original.
     */
    public static int age(BlockState state) {
        if (state == null) return 0;
        if (state.hasProperty(BlockStateProperties.AGE_7)) return state.getValue(BlockStateProperties.AGE_7);
        if (state.hasProperty(BlockStateProperties.AGE_5)) return state.getValue(BlockStateProperties.AGE_5);
        if (state.hasProperty(BlockStateProperties.AGE_4)) return state.getValue(BlockStateProperties.AGE_4);
        if (state.hasProperty(BlockStateProperties.AGE_3)) return state.getValue(BlockStateProperties.AGE_3);
        if (state.hasProperty(BlockStateProperties.AGE_2)) return state.getValue(BlockStateProperties.AGE_2);
        if (state.hasProperty(BlockStateProperties.AGE_1)) return state.getValue(BlockStateProperties.AGE_1);
        return 0;
    }

    /** Original {@code IGrowable}: the block family that can be grown by bonemeal. */
    public static boolean isGrowable(BlockState state) {
        if (state == null) return false;
        return state.getBlock() instanceof CropBlock
                || state.getBlock() instanceof StemBlock
                || state.getBlock() instanceof NetherWartBlock;
    }

    /** Original {@code IGrowable.canGrow}: may the crop still grow one more stage? */
    public static boolean canGrow(BlockState state) {
        if (state == null) return false;
        if (state.getBlock() instanceof CropBlock crop) return state.getValue(CropBlock.AGE) < CropBlock.MAX_AGE;
        if (state.getBlock() instanceof StemBlock stem) return state.getValue(StemBlock.AGE) < StemBlock.MAX_AGE;
        if (state.getBlock() instanceof NetherWartBlock wart) return state.getValue(NetherWartBlock.AGE) < NetherWartBlock.MAX_AGE;
        return false;
    }

    /** The block of an item, or null when the item is not a block item. */
    public static Block blockOf(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return null;
        Block block = Block.byItem(stack.getItem());
        return block == null || block == Blocks.AIR ? null : block;
    }

    /** Original {@code addStandardCrop}. */
    public static void addStandardCrop(ItemStack stack, int grownMeta) {
        Block block = blockOf(stack);
        if (block == null) return;
        if (grownMeta == Short.MAX_VALUE) {
            for (int stage = 0; stage < 16; ++stage) standardCrops.add(key(block, stage));
        } else {
            standardCrops.add(key(block, grownMeta));
        }
        if (block instanceof CropBlock && grownMeta != 7) standardCrops.add(key(block, 7));
    }

    /** Original {@code addClickableCrop}. */
    public static void addClickableCrop(ItemStack stack, int grownMeta) {
        Block block = blockOf(stack);
        if (block == null) return;
        if (grownMeta == Short.MAX_VALUE) {
            for (int stage = 0; stage < 16; ++stage) clickableCrops.add(key(block, stage));
        } else {
            clickableCrops.add(key(block, grownMeta));
        }
        if (block instanceof CropBlock && grownMeta != 7) clickableCrops.add(key(block, 7));
    }

    /** Original {@code addStackedCrop(ItemStack, int)}. */
    public static void addStackedCrop(ItemStack stack, int grownMeta) {
        Block block = blockOf(stack);
        if (block == null) return;
        addStackedCrop(block, grownMeta);
    }

    /** Original {@code addStackedCrop(Block, int)}. */
    public static void addStackedCrop(Block block, int grownMeta) {
        if (block == null) return;
        if (grownMeta == Short.MAX_VALUE) {
            for (int stage = 0; stage < 16; ++stage) stackedCrops.add(key(block, stage));
        } else {
            stackedCrops.add(key(block, grownMeta));
        }
        if (block instanceof CropBlock && grownMeta != 7) stackedCrops.add(key(block, 7));
    }

    /** Original {@code blacklistLamp}. */
    public static void blacklistLamp(ItemStack stack, int meta) {
        Block block = blockOf(stack);
        if (block == null) return;
        if (meta == Short.MAX_VALUE) {
            for (int stage = 0; stage < 16; ++stage) lampBlacklist.add(key(block, stage));
        } else {
            lampBlacklist.add(key(block, meta));
        }
    }

    /**
     * Original {@code isGrownCrop}: is the crop at the given position ripe?
     *
     * <p>The original tested an {@code IGrowable} block that can no longer grow, a wheat block at stage 7 that is
     * not in the crop lists, a nether wart at stage 3 or more, a red mushroom of the second size, and finally the
     * three crop lists at the block's own stage, where a stacked crop also needs the block below to be the same
     * block. 1.21.1 has no mushroom size metadata, so the red mushroom size test no longer exists.
     */
    public static boolean isGrownCrop(Level level, int x, int y, int z) {
        if (level == null) return false;
        BlockPos pos = new BlockPos(x, y, z);
        BlockState state = level.getBlockState(pos);
        if (state.isAir()) return false;
        Block block = state.getBlock();
        boolean found = false;
        for (int stage = 0; stage < 16; ++stage) {
            if (!standardCrops.contains(key(block, stage)) && !clickableCrops.contains(key(block, stage))
                    && !stackedCrops.contains(key(block, stage))) continue;
            found = true;
            break;
        }
        BlockState below = level.getBlockState(pos.below());
        int stage = age(state);
        return isGrowable(state) && !canGrow(state)
                || state.getBlock() instanceof CropBlock && stage == 7 && !found
                || state.is(Blocks.NETHER_WART) && stage >= 3
                || standardCrops.contains(key(block, stage))
                || clickableCrops.contains(key(block, stage))
                || stackedCrops.contains(key(block, stage)) && below.getBlock() == block;
    }

    /** Original {@code doesLampGrow}: may the growth lamp grow this block? */
    public static boolean doesLampGrow(Level level, int x, int y, int z) {
        if (level == null) return false;
        BlockState state = level.getBlockState(new BlockPos(x, y, z));
        return !lampBlacklist.contains(key(state));
    }
}
