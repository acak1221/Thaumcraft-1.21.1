package thaumcraft.api.wands;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/**
 * The original TC4 wand trigger contract.
 *
 * <p>Forced 1.21.1 changes: {@code EntityPlayer} became {@link Player} and {@code World} became {@link Level}.</p>
 */
public interface IWandTriggerManager {
    boolean performTrigger(Level level, ItemStack wand, Player player, int x, int y, int z, int side, int event);
}
