package thaumcraft.api.baubles;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;

/** Native type adaptation of Azanor's original four-slot Baubles item contract. */
public interface IBauble {
    BaubleType getBaubleType(ItemStack stack);
    /** Base no-op worn callback, called once per game tick by native accessory ticking; nullable arguments are ignored by this default. */
    default void onWornTick(ItemStack stack, LivingEntity wearer) {}
    /** Base no-op equip notification; nullable arguments are ignored and no packet/state change occurs in this default. */
    default void onEquipped(ItemStack stack, LivingEntity wearer) {}
    /** Base no-op unequip notification; nullable arguments are ignored and no packet/state change occurs in this default. */
    default void onUnequipped(ItemStack stack, LivingEntity wearer) {}
    /** Returns true in this default, including null arguments, without changing equipment; implementations may impose restrictions. */
    default boolean canEquip(ItemStack stack, LivingEntity wearer) { return true; }
    /** Returns true in this default, including null arguments, without changing equipment; implementations may impose restrictions. */
    default boolean canUnequip(ItemStack stack, LivingEntity wearer) { return true; }
}
