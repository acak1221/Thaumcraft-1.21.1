package thaumcraft.common.items;

import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import thaumcraft.common.blocks.BlockArcaneBore;

/** A plain newly placed head, not a carrier for installed tools/credit/journals.
 * The root registers this item only for arcane_bore, not the stockless base. */
public final class ItemArcaneBore extends BlockItem {
    public ItemArcaneBore(Block block, Item.Properties properties) { super(block, properties); }
    @Override public void appendHoverText(ItemStack stack, TooltipContext context, java.util.List<Component> tooltip, TooltipFlag flags) {
        super.appendHoverText(stack, context, tooltip, flags);
        tooltip.add(Component.translatable("tooltip.thaumcraft.arcane_bore.scope"));
    }
    @Override protected BlockState getPlacementState(BlockPlaceContext context) {
        if (context.getItemInHand().has(DataComponents.BLOCK_ENTITY_DATA)
                || context.getItemInHand().has(DataComponents.BLOCK_STATE)
                || !context.getLevel().hasChunkAt(context.getClickedPos())
                || !BlockArcaneBore.knownBase(context.getLevel(), context.getClickedPos(), context.getClickedFace())) return null;
        return super.getPlacementState(context);
    }
    @Override protected boolean placeBlock(BlockPlaceContext context, BlockState state) {
        // Recheck after native placement/collision callbacks; never accept the
        // original bad plank support or a stale cast merely because a state was prepared.
        if (context.getItemInHand().has(DataComponents.BLOCK_ENTITY_DATA)
                || context.getItemInHand().has(DataComponents.BLOCK_STATE)
                || !context.getLevel().hasChunkAt(context.getClickedPos())
                || !BlockArcaneBore.knownBase(context.getLevel(), context.getClickedPos(), state.getValue(BlockArcaneBore.ATTACHMENT))) return false;
        return super.placeBlock(context, state);
    }
}
