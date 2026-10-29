/**
 * New in the 1.21.1 port of Thaumic Tinkerer (no 1.7.10 counterpart): the contract between the Focus of Dislocation (group
 * Foci) and the overlay that shows what it carries (group BasicItems, {@code HUDHandler}). Distributed under the same
 * licence as the add-on (CC BY-NC-SA 3.0, see addons/thaumic-tinkerer/NOTICE.md).
 */
package thaumic.tinkerer.common.item;

import net.minecraft.world.item.ItemStack;

/**
 * What the overlay of the Focus of Dislocation needs to know. In 1.7.10 {@code HUDHandler} cast the focus of the wand in
 * the player's hand to {@code ItemFocusDislocation} and called its {@code getPickedBlock} and {@code getStackTileEntity};
 * the groups of the add-on do not reference each other's classes, so the focus item implements this interface (the overlay
 * asks the focus item of the wand, {@code WandHelper.getFocusItem}) or a source is handed to
 * {@code HUDHandler.setDislocationSource}.
 *
 * <p>Both methods run on the client every frame: they must only read the stacks.</p>
 */
public interface IDislocationHudSource {
    /**
     * The stack to draw as the block the focus carries (1.7.10: the stack of the picked block, drawn at 16 x 16), or an
     * empty stack when the focus carries nothing (1.7.10: {@code getPickedBlock} answered null).
     *
     * @param wand  the wand in the main hand
     * @param focus the focus item of the wand (a copy: it carries the focus's own data components)
     */
    ItemStack pickedBlock(ItemStack wand, ItemStack focus);

    /**
     * Whether the carried block brings block-entity contents (1.7.10: {@code getStackTileEntity} was a non-empty compound),
     * which makes the overlay add the line "Extra Contents".
     */
    boolean hasExtraContents(ItemStack wand, ItemStack focus);
}
