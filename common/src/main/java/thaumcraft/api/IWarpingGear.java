package thaumcraft.api;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

/**
 * Original thaumcraft.api.IWarpingGear: an item that adds warp to whoever holds or wears it, for as long as it is held or worn.
 *
 * <p>Original WarpEvents.getWarpFromGear: the item in the main hand, the four armor pieces and the four accessory slots
 * (Baubles in 1.7.10) are counted; the off hand and the rest of the inventory are not. Void metal tools and armor each add
 * one. The sum takes part in the warp events like the permanent and sticky warp of the player's knowledge.</p>
 */
public interface IWarpingGear {
    int getWarp(ItemStack stack, Player player);
}
