package thaumcraft.api.wands;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/**
 * The original TC4 wand contract.
 *
 * <p>Forced 1.21.1 changes: {@code EntityPlayer} became {@link Player} and {@code World} became {@link Level}.</p>
 */
public interface IWandable {
    /**
     * The original right click handler.
     *
     * @return the amount of vis the click costs
     */
    int onWandRightClick(Level level, ItemStack stack, Player player, int x, int y, int z, int side, int event);

    /** The original right click handler without a target. */
    ItemStack onWandRightClick(Level level, ItemStack stack, Player player);

    void onUsingWandTick(ItemStack stack, Player player, int ticks);

    void onWandStoppedUsing(ItemStack stack, Level level, Player player, int ticks);
}
