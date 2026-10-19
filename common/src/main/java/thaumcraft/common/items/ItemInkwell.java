package thaumcraft.common.items;

import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import thaumcraft.api.IScribeTools;
import thaumcraft.common.blocks.BlockTable;

/** Original ItemInkwell: one stack, 100 ink, and the concrete table-conversion hook. */
public final class ItemInkwell extends Item implements IScribeTools {
    public static final int CAPACITY = 100;

    public ItemInkwell(Properties properties) { super(properties.durability(CAPACITY)); }

    @Override public InteractionResult useOn(UseOnContext context) {
        return context.getPlayer() == null ? InteractionResult.PASS
                : tryCreateResearchTable(context.getLevel(), context.getClickedPos(), context.getPlayer(), context.getHand());
    }

    /** BlockTable invokes this before ordinary GUI activation, as old use-first did. */
    public InteractionResult tryCreateResearchTable(Level level, BlockPos pos, Player player, InteractionHand hand) {
        return BlockTable.tryCreateResearchTable(level, pos, player, hand);
    }

    public static boolean isUsable(ItemStack stack) {
        return !stack.isEmpty() && stack.getItem() instanceof IScribeTools
                && stack.getDamageValue() < stack.getMaxDamage();
    }
}
