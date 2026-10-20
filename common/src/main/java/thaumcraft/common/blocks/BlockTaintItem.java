package thaumcraft.common.blocks;

import net.minecraft.world.item.BlockItem;
import net.minecraft.world.level.block.Block;

/** Native crust/soil items; existing flesh_block retains original metadata2's canonical identity. */
public final class BlockTaintItem extends BlockItem {
    public BlockTaintItem(Block block,Properties properties) { super(block,properties); }
}
