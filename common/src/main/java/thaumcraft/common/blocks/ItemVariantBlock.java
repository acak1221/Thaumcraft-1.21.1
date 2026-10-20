package thaumcraft.common.blocks;

import java.util.Collection;
import java.util.Collections;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import thaumcraft.common.Thaumcraft;

/**
 * A block item that places ONE original metadata value of a family block.
 *
 * <p>The original TC4 recipes produce a block with a metadata: the paving stone of travel is
 * {@code blockCosmeticSolid} with the metadata two and the warded glass is
 * {@code blockCosmeticOpaque} with the metadata two. A 1.21.1 recipe result cannot carry a block
 * state, so every such metadata gets its own item that places the block with the variant set.
 */
public final class ItemVariantBlock extends BlockItem {
    private final int variant;
    private final String name;

    public ItemVariantBlock(Block block, int variant, String name, Properties properties) {
        super(block, properties);
        this.variant = variant;
        this.name = name;
    }

    /** The original metadata value that this item places. */
    public int variant() { return variant; }

    /** The item has its own name; the name of the block would name every metadata value. */
    @Override public String getDescriptionId() { return "item." + Thaumcraft.MODID + "." + name; }

    /** The original metadata value becomes the "variant" block state of the family block. */
    @Override protected BlockState getPlacementState(BlockPlaceContext context) { return variantState(); }

    /** The state of the family block with the original metadata value set. */
    public BlockState variantState() {
        BlockState state = this.getBlock().defaultBlockState();
        for (var property : state.getProperties()) {
            if (!(property instanceof IntegerProperty integer) || !"variant".equals(property.getName())) continue;
            Collection<Integer> values = integer.getPossibleValues();
            if (values.isEmpty()) continue;
            return state.setValue(integer, Math.clamp(variant, Collections.min(values), Collections.max(values)));
        }
        return state;
    }
}
