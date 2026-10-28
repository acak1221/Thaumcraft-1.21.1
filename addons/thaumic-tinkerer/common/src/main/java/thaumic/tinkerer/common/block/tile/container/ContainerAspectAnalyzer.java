/**
 * This class was created by <Vazkii>. It's distributed as part of the ThaumicTinkerer Mod.
 *
 * ThaumicTinkerer is Open Source and distributed under a Creative Commons Attribution-NonCommercial-ShareAlike 3.0
 * License (http://creativecommons.org/licenses/by-nc-sa/3.0/deed.en_GB)
 *
 * ThaumicTinkerer is a Derivative Work on Thaumcraft 4. Thaumcraft 4 (c) Azanor 2012
 * (http://www.minecraftforum.net/topic/1585216-)
 *
 * Ported to Minecraft 1.21.1 for Thaumcraft 4 Reload (see addons/thaumic-tinkerer/NOTICE.md).
 */
package thaumic.tinkerer.common.block.tile.container;

import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import thaumcraft.api.ThaumcraftApiHelper;
import thaumic.tinkerer.common.content.Machines;

public class ContainerAspectAnalyzer extends AbstractContainerMenu {

    public static final int SLOT_X = 20;
    public static final int SLOT_Y = 30;

    private final Container analyzer;
    private final Slot slot;

    /** The client side: the slot contents arrive from the server. */
    public ContainerAspectAnalyzer(int id, Inventory playerInv) {
        this(id, playerInv, new SimpleContainer(1));
    }

    public ContainerAspectAnalyzer(int id, Inventory playerInv, Container analyzer) {
        super(Machines.MENU_ASPECT_ANALYZER.get(), id);
        this.analyzer = analyzer;
        checkContainerSize(analyzer, 1);
        analyzer.startOpen(playerInv.player);

        addSlot(slot = new SlotHasAspects(analyzer, 0, SLOT_X, SLOT_Y, playerInv.player.level()));
        PlayerInventoryLayout.addPlayerSlots(playerInv, this::addSlot);
    }

    public Container getAnalyzer() {
        return analyzer;
    }

    @Override
    public boolean stillValid(Player player) {
        return analyzer.stillValid(player);
    }

    @Override
    public void removed(Player player) {
        super.removed(player);
        analyzer.stopOpen(player);
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        ItemStack moved = ItemStack.EMPTY;
        Slot clicked = slots.get(index);

        if (clicked != null && clicked.hasItem()) {
            ItemStack stack = clicked.getItem();

            if (index == 0 || !stack.isEmpty() && slot.mayPlace(stack)) {
                moved = stack.copy();

                if (index < 1) {
                    if (!moveItemStackTo(stack, 1, 37, false)) return ItemStack.EMPTY;
                } else if (!moveItemStackTo(stack, 0, 1, false)) return ItemStack.EMPTY;

                if (stack.isEmpty()) clicked.setByPlayer(ItemStack.EMPTY);
                else clicked.setChanged();

                if (stack.getCount() == moved.getCount()) return ItemStack.EMPTY;

                clicked.onTake(player, stack);
            }
        }

        return moved;
    }

    /**
     * {@code SlotLimitedHasAspects} of Thaumcraft: the slot takes an item only if it has aspects (the object tags
     * plus the bonus tags, which is what a scan of the item gives).
     */
    public static class SlotHasAspects extends Slot {
        private final Level level;

        public SlotHasAspects(Container container, int index, int x, int y, Level level) {
            super(container, index, x, y);
            this.level = level;
        }

        @Override
        public boolean mayPlace(ItemStack stack) {
            if (stack.isEmpty() || level == null) return false;
            return ThaumcraftApiHelper.getObjectAspects(stack, level).size() > 0;
        }
    }
}
