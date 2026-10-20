package thaumcraft.common.blocks;

import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.InteractionResult;
import thaumcraft.common.tiles.TileCrystal;

/** Original BlockCrystalItem.placeBlockAt: the clicked side is stored after the block and its tile exist. */
public final class BlockCrystalItem extends BlockItem {
    public BlockCrystalItem(Block block, Properties properties) { super(block, properties); }
    @Override public InteractionResult place(BlockPlaceContext context) {
        InteractionResult placed = super.place(context);
        // Native BLOCK_ENTITY_DATA is merged after placeBlock. Final clicked-side ownership must follow that native merge.
        if (placed.consumesAction() && context.getLevel().getBlockState(context.getClickedPos()).is(getBlock())
                && context.getLevel().getBlockEntity(context.getClickedPos()) instanceof TileCrystal tile)
            tile.setOrientation(context.getClickedFace());
        return placed;
    }
}
