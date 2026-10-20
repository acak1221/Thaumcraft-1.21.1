package thaumcraft.api.baubles;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;

/** Native type adaptation of Azanor's original four-slot Baubles item contract. */
public interface IBauble {
    BaubleType getBaubleType(ItemStack stack);
    default void onWornTick(ItemStack stack, LivingEntity wearer) {}
    default void onEquipped(ItemStack stack, LivingEntity wearer) {}
    default void onUnequipped(ItemStack stack, LivingEntity wearer) {}
    default boolean canEquip(ItemStack stack, LivingEntity wearer) { return true; }
    default boolean canUnequip(ItemStack stack, LivingEntity wearer) { return true; }
}
