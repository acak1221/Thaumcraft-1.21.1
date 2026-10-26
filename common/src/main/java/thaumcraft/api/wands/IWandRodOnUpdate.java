package thaumcraft.api.wands;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

/**
 * The original TC4 wand rod update hook.
 *
 * <p>Add-on contract (original ItemWandCasting.onUpdate:350-355): the hook of an add-on {@link WandRod} is called on the
 * <b>server</b> once per tick for every wand, staff or sceptre with that rod in a player's inventory, with the live stack and
 * its carrier. It may change the stack's vis through {@code ItemWandCasting.addVis/addRealVis} (whole vis / centivis); the
 * stack is the inventory's own, so a change is saved and synchronised like any inventory change. Native rods refill through
 * the port's own passive and never call their views' hooks.</p>
 *
 * <p>Forced 1.21.1 change: {@code EntityPlayer} became {@link Player}.</p>
 */
public interface IWandRodOnUpdate {
    void onUpdate(ItemStack stack, Player player);
}
