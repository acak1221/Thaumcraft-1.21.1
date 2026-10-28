/**
 * This class was created by <Vazkii>. It's distributed as part of the ThaumicTinkerer Mod.
 *
 * ThaumicTinkerer is Open Source and distributed under a Creative Commons Attribution-NonCommercial-ShareAlike 3.0
 * License (http://creativecommons.org/licenses/by-nc-sa/3.0/deed.en_GB)
 *
 * ThaumicTinkerer is a Derivative Work on Thaumcraft 4. Thaumcraft 4 (c) Azanor 2012
 * (http://www.minecraftforum.net/topic/1585216-)
 *
 * File Created @ [14 Sep 2013, 18:35:37 (GMT)]
 * Ported to Minecraft 1.21.1 for Thaumcraft 4 Reload (see addons/thaumic-tinkerer/NOTICE.md).
 */
package thaumic.tinkerer.common.block.tile.container.slot;

import java.util.function.BooleanSupplier;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/**
 * The tool slot of the Enchanter's menu: holds one tool, accepts nothing and gives nothing back while the enchanter is
 * working (1.7.10 {@code SlotTool(TileEnchanter, ...)}).
 *
 * <p>The slot asks a {@link BooleanSupplier} whether the machine is working instead of holding the enchanter's block
 * entity, because the add-on's groups do not reference one another's classes; the Enchanter passes
 * {@code () -> enchanter.working}. A "tool" is what {@code Item.isItemTool} was for the original: an item that does not
 * stack and can be damaged (books are refused).</p>
 */
public class SlotTool extends Slot {

    private final BooleanSupplier working;

    public SlotTool(Container container, int slot, int x, int y, BooleanSupplier working) {
        super(container, slot, x, y);
        this.working = working;
    }

    @Override
    public boolean mayPlace(ItemStack stack) {
        return !working.getAsBoolean() && !stack.is(Items.BOOK) && stack.getMaxStackSize() == 1 && stack.isDamageableItem();
    }

    @Override
    public int getMaxStackSize() {
        return 1;
    }

    @Override
    public boolean mayPickup(Player player) {
        return !working.getAsBoolean();
    }
}
