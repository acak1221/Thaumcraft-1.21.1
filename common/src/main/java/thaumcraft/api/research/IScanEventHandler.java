package thaumcraft.api.research;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/** Called in registration order only after the thaumometer finds no physical entity or block target. */
@FunctionalInterface
public interface IScanEventHandler {
    ScanResult scanPhenomena(ItemStack stack, Level world, Player player);
}
