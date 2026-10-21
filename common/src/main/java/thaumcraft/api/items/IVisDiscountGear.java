package thaumcraft.api.items;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import thaumcraft.api.aspects.Aspect;

/**
 * Thaumcraft 6 compatibility interface for addons compiled against the 1.12 API.
 *
 * <p>Legacy {@code src/main/java/thaumcraft/api/items/IVisDiscountGear.java}: the alias keeps the
 * two argument overload of the 1.12 API and answers the canonical three argument contract from
 * it. The canonical contract itself is published at {@code thaumcraft/api/IVisDiscountGear.java}.</p>
 */
public interface IVisDiscountGear extends thaumcraft.api.IVisDiscountGear {
    int getVisDiscount(ItemStack stack, Player player);

    @Override
    default int getVisDiscount(ItemStack stack, Player player, Aspect aspect) {
        return getVisDiscount(stack, player);
    }
}
