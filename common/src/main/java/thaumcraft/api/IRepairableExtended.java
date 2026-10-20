package thaumcraft.api;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

/** Original optional per-item repair hook, invoked before the inventory vis payment. */
public interface IRepairableExtended extends IRepairable {
    boolean doRepair(ItemStack stack, Player player, int level);
}
