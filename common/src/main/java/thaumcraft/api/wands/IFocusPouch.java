package thaumcraft.api.wands;

import net.minecraft.world.item.ItemStack;

/**
 * An item that stores focuses for the wand's focus selector: the native focus pouch, and a pouch of an add-on.
 *
 * <p>TC4 did not have this interface. Its selector (the change focus key) found a pouch by the class {@code ItemFocusPouch}
 * and read the foci in it with {@code getInventory(ItemStack)}; Thaumic Tinkerer's Bottomless Pouch extended that class with
 * 117 cells. {@code getInventory} and {@code setInventory(ItemStack, ItemStack[])} keep the original names and become this
 * interface, so an add-on pouch is offered by the selector exactly like the native one without extending an internal class.</p>
 *
 * <p>What the selector does with a pouch (an item of the stack implements this interface):</p>
 * <ul>
 *   <li>It looks at the 36 main inventory slots (hotbar included) and at the four accessory slots, so a pouch that is also a
 *       belt {@link thaumcraft.api.baubles.IBauble} is listed while worn. The off-hand, the armour slots and the contents of
 *       other containers are not looked at. Only a stack of exactly one pouch counts.</li>
 *   <li>It lists every cell that holds a focus (an {@link ItemFocusBasic} that can be mounted). Other items in a cell are
 *       ignored and never touched. The selector UI is a ring with one button per focus, not a grid, so a pouch of any size
 *       is listed; only the first {@link #MAX_SELECTABLE_CELLS} cells are listed (the native pouch has 18).</li>
 *   <li>When the player picks a focus, it leaves its cell (the cell becomes {@link ItemStack#EMPTY}) and is mounted on the
 *       wand; the focus the wand had goes into the first empty cell among the first {@link #MAX_SELECTABLE_CELLS} cells of
 *       the first pouch that has one (worn pouches first, then the main inventory, each from its first cell), before any
 *       free inventory slot. The selection is all or nothing.</li>
 *   <li>A right click never puts a pouch into an accessory slot, whatever {@link thaumcraft.api.baubles.IBauble#equipsOnUse}
 *       answers: the click goes to the pouch's own {@code use}, which normally opens its screen. It is put on in the
 *       Baubles menu.</li>
 * </ul>
 *
 * <p>The pouch does its own storage, typically in a data component, and its own screen. Opening a screen, the rules for what
 * may be put in, the number of cells and their layout are all the add-on's.</p>
 */
public interface IFocusPouch {
    /**
     * The most cells of one pouch the focus selector reads and writes: the cells whose index is this number or more are
     * neither listed nor given a returned focus. Thaumic Tinkerer's pouch has 117 cells, which fit.
     */
    int MAX_SELECTABLE_CELLS = 128;

    /**
     * All cells of the pouch, in order, with {@link ItemStack#EMPTY} for an empty cell (never {@code null}).
     *
     * <p>The length is the pouch's number of cells and must be the same on every call for the same pouch stack. Every stack
     * of the result is an independent copy: the caller changes the array and its stacks freely, and a change reaches the pouch
     * only through {@link #setInventory}. Called on both logical sides: on the client to list the focuses, on the server to
     * select one.</p>
     *
     * <p>A stack that carries a payload this pouch cannot read (for example saved by another version) throws
     * {@link IllegalArgumentException}; the selector then ignores that pouch and leaves its payload untouched. Any other
     * {@link RuntimeException} is treated the same way (and written to the log once per pouch class), because the selector
     * also asks on the client's render thread, where an add-on's bug would otherwise close the game; do not rely on that, an
     * exception is the way to say "unreadable", not to report an error.</p>
     *
     * @param pouch one pouch of this item; its stack count is one
     * @return the cells, a new array
     * @throws IllegalArgumentException if the stack holds contents this pouch cannot read
     */
    ItemStack[] getInventory(ItemStack pouch);

    /**
     * Replaces all cells of the pouch.
     *
     * <p>{@code contents} has the length {@link #getInventory} returned for this stack; an entry is {@link ItemStack#EMPTY}
     * for an empty cell. Copy what is kept: the caller may still hold the array. Change nothing but the given stack. Called
     * on the server thread only; the selector calls it on the live stack of the player's inventory or accessory slot (the
     * same stack object stays in its slot) and also on a scratch copy of a stack while it plans a selection, so it must not
     * have any effect outside the stack it is given. It may not refuse an array {@link #getInventory} could have returned
     * with one cell changed.</p>
     *
     * <p>If it throws anyway (a {@link RuntimeException}, written to the log once per pouch class), the selection stays all or
     * nothing: while the selector plans on a scratch copy, the pouch gives no focus and takes none; if it throws on the live
     * stack, the selection is refused and every stack it had already written is put back as it was, so the wand keeps its focus
     * and no focus is lost or duplicated. A pouch that wrote part of its payload before throwing is therefore restored by the
     * selector, not by the pouch.</p>
     *
     * @param pouch    one pouch of this item; its stack count is one
     * @param contents the new cells, one entry per cell, none {@code null}
     * @throws IllegalArgumentException if {@code contents} has the wrong length
     */
    void setInventory(ItemStack pouch, ItemStack[] contents);
}
