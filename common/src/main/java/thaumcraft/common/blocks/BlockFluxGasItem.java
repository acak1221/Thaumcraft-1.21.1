package thaumcraft.common.blocks;

import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Original {@code BlockFluxGasItem}: the item of the flux gas.
 *
 * <p>The original item is a plain {@code ItemBlock} of the flux gas with a stack limit of
 * sixty four and no subtypes beyond the fluid level, and it always places the block with the
 * original metadata eight ({@code BlockFluxGasItem:9,17-19}), the exceptional nine unit birth
 * of a Forge {@code BlockFluidFinite}. 1.21.1 has no item metadata, so the level of the placed
 * cell is the state the item carries.</p>
 *
 * <p>Oracle: {@code thaumcraft/common/blocks/BlockFluxGasItem.java}, lines 6-19.</p>
 */
public class BlockFluxGasItem extends BlockItem {
    public BlockFluxGasItem(Block block, Properties properties) { super(block, properties); }

    /** The original metadata of the placed cell: the exceptional nine unit birth. */
    public static int placedMetadata() { return FluxFiniteBlock.PLACED_METADATA; }

    @Override protected BlockState getPlacementState(BlockPlaceContext context) {
        BlockState state = FluxFiniteBlock.placedState(getBlock(), context);
        return state != null && canPlace(context, state) ? state : null;
    }
}
