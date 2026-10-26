package thaumcraft.common.lib;

import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import thaumcraft.api.IWarpingGear;
import thaumcraft.common.lib.baubles.PlayerAccessories;

/**
 * Original WarpEvents.getWarpFromGear (:306): the warp of the item in the player's hand, of the four armor pieces and of the four
 * Baubles slots (here the port's own four accessory slots, {@link PlayerAccessories}). Anything that is not an
 * {@link IWarpingGear} counts for nothing.
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
        var accessories = PlayerAccessories.get(player);
        for (int slot = 0; slot < 4; slot++) warp += of(accessories.getItem(slot), player);
        return warp;
    }
}
