package thaumcraft.api.wands;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

/**
 * The original TC4 wand rod update hook.
 *
 * <p>Forced 1.21.1 change: {@code EntityPlayer} became {@link Player}.</p>
 */
public interface IWandRodOnUpdate {
    void onUpdate(ItemStack stack, Player player);
}
