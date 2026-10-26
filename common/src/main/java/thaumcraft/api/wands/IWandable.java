package thaumcraft.api.wands;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/**
 * The original TC4 wand contract: a block, or a block entity, that answers a click with a wand, staff or sceptre.
 *
 * <p>Original ItemWandCasting: the block is asked first, then its block entity, before the registered wand triggers and
 * the native wand interactions.</p>
 *
 * <p>Forced 1.21.1 changes: {@code EntityPlayer} became {@link Player} and {@code World} became {@link Level}.</p>
 */
public interface IWandable {
    /**
     * The original click on the block (ItemWandCasting.onItemUseFirst), on both sides.
     *
     * @param side the clicked face, as {@code Direction.get3DDataValue()}
     * @param event the block metadata in 1.7.10; always 0 in 1.21.1
     * @return a negative value when the click was not handled (the wand goes on with its own behaviour); otherwise the
     *         click is handled and nothing else happens: 1 swings the arm (success), any other value is a quiet failure
     */
    int onWandRightClick(Level level, ItemStack stack, Player player, int x, int y, int z, int side, int event);

    /**
     * The original click without a target (ItemWandCasting.onItemRightClick), asked of the block the player looks at.
     *
     * @return the stack the hand holds afterwards when the click was handled, or {@code null} when it was not
     */
    ItemStack onWandRightClick(Level level, ItemStack stack, Player player);

    void onUsingWandTick(ItemStack stack, Player player, int ticks);

    void onWandStoppedUsing(ItemStack stack, Level level, Player player, int ticks);
}
