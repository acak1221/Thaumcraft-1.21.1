/**
 * This class is new in the port of Thaumic Tinkerer to Minecraft 1.21.1 for Thaumcraft 4 Reload (no 1.7.10
 * counterpart); it is distributed under the same licence as the add-on (Creative Commons
 * Attribution-NonCommercial-ShareAlike 3.0, see addons/thaumic-tinkerer/NOTICE.md).
 */
package thaumic.tinkerer.common.block.fire;

import java.util.function.Predicate;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Property;

/**
 * The transmutation rules and the flammability of the imbued fires.
 *
 * <p>1.7.10 kept the rules in a {@code HashMap<Block, Block>} keyed by block id, so a rule covered every metadata
 * of its block ("log" is every kind of log). 1.21.1 splits such a block in a family of blocks, so a rule is a
 * predicate over the block state, and a family is a block tag (see docs/deviations/SimpleBlocks.md for the families).
 * The result is the default state of the target block, as the 1.7.10 result was its metadata 0; the properties the
 * old and the new block share (the facing of stairs, the shape of a fence) are carried over.</p>
 */
public final class FireRules {
    private FireRules() {}

    /** A transmutation: the blocks that burn into the result. */
    public record Rule(Predicate<BlockState> from, Block to) {
        public boolean matches(BlockState state) {
            return from.test(state);
        }

        /** The state the matching block turns into. */
        public BlockState result(BlockState old) {
            return carryOver(old, to.defaultBlockState());
        }

        /** Whether the block is a result of this rule ({@code HashMap.containsValue}). */
        public boolean producing(BlockState state) {
            return state.getBlock() == to;
        }
    }

    public static Rule rule(Block from, Block to) {
        return new Rule(state -> state.is(from), to);
    }

    public static Rule rule(TagKey<Block> from, Block to) {
        return new Rule(state -> state.is(from), to);
    }

    public static Rule rule(Predicate<BlockState> from, Block to) {
        return new Rule(from, to);
    }

    /** The rule for blocks of the given list. */
    public static Rule rule(Block[] from, Block to) {
        return new Rule(state -> {
            for (Block block : from) {
                if (state.is(block)) return true;
            }
            return false;
        }, to);
    }

    /** Still water: 1.7.10 {@code Blocks.water} was the source block, {@code flowing_water} the flowing one. */
    public static boolean isWaterSource(BlockState state) {
        return state.is(Blocks.WATER) && state.getValue(LiquidBlock.LEVEL) == 0;
    }

    /** Copies the properties of {@code old} that {@code fresh} has too (same name and value type). */
    public static BlockState carryOver(BlockState old, BlockState fresh) {
        BlockState result = fresh;
        for (Property<?> property : old.getProperties()) {
            if (fresh.hasProperty(property) && !property.getName().equals("level") && !property.getName().equals("age")) {
                result = copy(result, old, property);
            }
        }
        return result;
    }

    private static <T extends Comparable<T>> BlockState copy(BlockState to, BlockState from, Property<T> property) {
        return to.setValue(property, from.getValue(property));
    }

    // ---- flammability: what Blocks.fire.getFlammability answered in 1.7.10 ------------------------------------

    /**
     * The chance for a neighbouring block to burn, the second number of the {@code setFireInfo} calls of 1.7.10 (planks 20,
     * logs 5, leaves 60, wool 60, plants 100, TNT 100...). The vanilla fire of 1.7.10 consulted that table for the blocks
     * of every mod, so the fires of the add-on do too; the blocks of 1.21.1 that belong to the same families are included.
     */
    public static int vanillaFlammability(BlockState state) {
        if (state.is(BlockTags.PLANKS) || state.is(BlockTags.WOODEN_SLABS) || state.is(BlockTags.WOODEN_FENCES)
                || state.is(BlockTags.FENCE_GATES) || state.is(BlockTags.WOODEN_STAIRS)) return 20;
        if (state.is(BlockTags.LOGS_THAT_BURN)) return 5;
        if (state.is(BlockTags.LEAVES)) return 60;
        if (state.is(Blocks.BOOKSHELF)) return 20;
        if (state.is(Blocks.TNT)) return 100;
        if (state.is(Blocks.SHORT_GRASS) || state.is(Blocks.TALL_GRASS) || state.is(Blocks.FERN) || state.is(Blocks.LARGE_FERN)
                || state.is(BlockTags.SMALL_FLOWERS) || state.is(BlockTags.TALL_FLOWERS) || state.is(Blocks.DEAD_BUSH)
                || state.is(Blocks.VINE)) return 100;
        if (state.is(BlockTags.WOOL)) return 60;
        if (state.is(Blocks.COAL_BLOCK)) return 5;
        if (state.is(Blocks.HAY_BLOCK)) return 20;
        if (state.is(BlockTags.WOOL_CARPETS)) return 20;
        return 0;
    }
}
