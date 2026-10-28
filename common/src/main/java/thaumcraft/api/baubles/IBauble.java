package thaumcraft.api.baubles;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
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
    /**
     * Whether a right click with this item in a hand puts it into the first free accessory slot of its kind. True in this
     * default, whatever the arguments, so an existing bauble is unchanged.
     *
     * <p>The hand equip is the port's own convenience: neither TC4 nor the Baubles mod had it (a bauble went into its slot by
     * the Baubles menu). It runs where the click would otherwise call {@code Item.use}, so a click that the targeted block or
     * {@code Item.useOn} has already consumed never reaches it. Return {@code false} for an item whose right click does something
     * of its own (a talisman that toggles, a belt that opens a screen): the click then goes on to {@code Item.use}, and the
     * player puts the item on in the Baubles menu. The click goes on to {@code Item.use} as well when no slot of the item's
     * kind is free, whatever this method answers.</p>
     *
     * <p>This method controls the hand equip and nothing else: the Baubles menu (key B, the button beside the player), a
     * shift-click into the menu, {@link #getBaubleType} and {@link #canEquip} are all unchanged. A
     * {@link thaumcraft.api.wands.IFocusPouch focus pouch} is never put on by a click, whatever this method answers.</p>
     *
     * <p>Called on the client and on the server for the same click, with the stack in the hand (it may hold more than one
     * item); the two answers must agree, so decide from the stack and from state the client also has (for example
     * {@code player.isShiftKeyDown()}), and change nothing.</p>
     *
     * @param stack  the stack in the hand, never empty
     * @param player the player using it; a spectator never equips
     * @return true to let the click equip the item, false to leave the click to {@code Item.use}
     */
    default boolean equipsOnUse(ItemStack stack, Player player) { return true; }
}
