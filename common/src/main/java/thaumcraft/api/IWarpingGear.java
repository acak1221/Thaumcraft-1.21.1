package thaumcraft.api;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

/**
 * Original thaumcraft.api.IWarpingGear: an item that adds permanent-looking warp to whoever holds or wears it. Void metal tools and
 * armor each add one. The sum is {@code WarpGear.fromGear}; what warp then does to a player (the warp events) is not ported yet.
 */
public interface IWarpingGear {
    int getWarp(ItemStack stack, Player player);
}
