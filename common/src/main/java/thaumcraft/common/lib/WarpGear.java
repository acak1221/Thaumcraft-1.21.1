package thaumcraft.common.lib;

import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import thaumcraft.api.IWarpingGear;

/**
 * Original WarpEvents.getWarpFromGear (:306): the warp of the item in the player's hand plus that of the four armor pieces (the baubles
 * of the original have no equivalent here). Anything that is not an {@link IWarpingGear} counts for nothing.
 */
public final class WarpGear {
    private WarpGear() {}

    /** Original EventHandlerRunic.getFinalWarp. */
    public static int of(ItemStack stack, Player player) {
        if (stack.isEmpty() || !(stack.getItem() instanceof IWarpingGear gear)) return 0;
        return gear.getWarp(stack, player);
    }

    public static int fromGear(Player player) {
        int warp = of(player.getMainHandItem(), player);
        for (EquipmentSlot slot : new EquipmentSlot[] {EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET}) {
            warp += of(player.getItemBySlot(slot), player);
        }
        return warp;
    }
}
