package thaumcraft.api;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import thaumcraft.api.aspects.Aspect;

public interface IVisDiscountGear {
    /** Integer percentage points, not a multiplier. Aspect may be null for a general tooltip query. */
    int getVisDiscount(ItemStack stack, Player player, Aspect aspect);
}
