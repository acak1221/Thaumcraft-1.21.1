package thaumcraft.common.container;

import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import thaumcraft.common.lib.crafting.DeviceContainerRules;

/**
 * Original ContainerGhostSlots: the shared ghost slot mechanism. A ghost slot
 * is a recipe preview - it shows the item a device wants, the player may
 * change that preview, and the preview never becomes real stock: the cursor
 * and the player inventory are never touched by a ghost click.
 */
public class ContainerGhostSlots extends AbstractContainerMenu {
    /** The original ghost positions, taken from the golem configuration GUI. */
    public static final int GHOST_X = 100, GHOST_Y = 16, GHOST_COLUMN_WIDTH = 28, GHOST_ROW_HEIGHT = 31;

    public ContainerGhostSlots(MenuType<?> type, int id, Inventory inventory, int ghostSlots, Container ghosts) {
        this(type, id, inventory, ghostSlots, ghosts, false);
    }

    /**
     * A fluid preview takes the original SlotGhostFluid, which only ever holds
     * one fluid container, exactly like the liquid golem configuration.
     */
    public ContainerGhostSlots(MenuType<?> type, int id, Inventory inventory, int ghostSlots, Container ghosts, boolean fluid) {
        super(type, id);
        checkContainerSize(ghosts, ghostSlots);
        for (int slot = 0; slot < ghostSlots; slot++) {
            int x = GHOST_X + slot / 2 * GHOST_COLUMN_WIDTH, y = GHOST_Y + slot % 2 * GHOST_ROW_HEIGHT;
            addSlot(fluid ? new SlotGhostFluid(ghosts, slot, x, y) : new SlotGhost(ghosts, slot, x, y));
        }
        bindPlayerInventory(inventory);
    }

    protected void bindPlayerInventory(Inventory inventory) {
        for (int row = 0; row < 3; row++) {
            for (int column = 0; column < 9; column++) {
                addSlot(new Slot(inventory, column + row * 9 + 9, 8 + column * 18, 84 + row * 18));
            }
        }
        for (int column = 0; column < 9; column++) addSlot(new Slot(inventory, column, 8 + column * 18, 142));
    }

    /** Original ContainerGhostSlots - a ghost slot is a SlotGhost. */
    protected boolean isGhostSlot(Slot slot) { return slot instanceof SlotGhost; }

    /**
     * Original ContainerGhostSlots:32-140. A ghost click only changes the
     * preview; the carried stack and the player inventory stay untouched.
     */
    @Override public void clicked(int index, int button, ClickType type, Player player) {
        if (index >= 0 && index < slots.size()) {
            Slot slot = slots.get(index);
            if (isGhostSlot(slot)) {
                ItemStack current = slot.getItem();
                int limit = slot.getMaxStackSize();
                if (type == ClickType.QUICK_MOVE) {
                    // Oracle ContainerGhostSlots:47-56 - a quick move clears the
                    // ghost or adds sixteen items, never more than the limit.
                    if (current.isEmpty()) return;
                    int count = DeviceContainerRules.ghostQuickMoveCount(current.getCount(), limit, button == 1);
                    slot.set(count <= 0 ? ItemStack.EMPTY : current.copyWithCount(count));
                } else if (type == ClickType.PICKUP) {
                    // Oracle ContainerGhostSlots:59-95.
                    ItemStack carried = getCarried();
                    boolean same = !current.isEmpty() && !carried.isEmpty() && ItemStack.isSameItemSameComponents(current, carried);
                    boolean accepts = carried.isEmpty() || slot.mayPlace(carried);
                    if (current.isEmpty()) {
                        if (accepts) {
                            int count = DeviceContainerRules.ghostClickCount(0, carried.getCount(), carried.getMaxStackSize(), limit, false, button == 1);
                            if (count > 0) slot.set(carried.copyWithCount(count));
                        }
                    } else if (accepts) {
                        int count = DeviceContainerRules.ghostClickCount(current.getCount(), carried.getCount(), carried.getMaxStackSize(), limit, same, button == 1);
                        if (count != current.getCount()) slot.set(count <= 0 ? ItemStack.EMPTY : current.copyWithCount(count));
                    } else if (same && carried.getMaxStackSize() > 1
                            && current.getCount() + carried.getCount() <= carried.getMaxStackSize()) {
                        // Oracle ContainerGhostSlots:96-110 - a ghost whose slot
                        // refuses the carried item empties instead.
                        slot.set(ItemStack.EMPTY);
                    }
                } else if (type == ClickType.CLONE) {
                    // Oracle ContainerGhostSlots:110-117 - the creative clone
                    // fills the ghost with a full stack.
                    if (!current.isEmpty()) slot.set(current.copyWithCount(current.getMaxStackSize()));
                } else if (type == ClickType.THROW) {
                    // Oracle ContainerGhostSlots:122-130 - a throw removes one
                    // ghost item or the whole preview, and never drops it.
                    int count = DeviceContainerRules.ghostThrowCount(current.getCount(), button == 1);
                    slot.set(count <= 0 ? ItemStack.EMPTY : current.copyWithCount(count));
                } else {
                    // Every other click type is refused for a ghost slot, exactly
                    // like the original, which never called the vanilla click.
                    return;
                }
                broadcastChanges();
                return;
            }
        }
        super.clicked(index, button, type, player);
    }

    /**
     * A ghost slot never owns stock, so a quick move must never turn a preview
     * into a real item.
     */
    @Override public ItemStack quickMoveStack(Player player, int index) {
        if (index < 0 || index >= slots.size()) return ItemStack.EMPTY;
        Slot slot = slots.get(index);
        if (isGhostSlot(slot) || !slot.hasItem() || !slot.mayPickup(player)) return ItemStack.EMPTY;
        ItemStack stack = slot.getItem();
        ItemStack original = stack.copy();
        int playerStart = slots.size() - 36;
        boolean moved = index < playerStart ? moveItemStackTo(stack, playerStart, slots.size(), true) : moveItemStackTo(stack, 0, playerStart, false);
        if (!moved || stack.getCount() == original.getCount()) return ItemStack.EMPTY;
        if (stack.isEmpty()) slot.set(ItemStack.EMPTY); else slot.setChanged();
        slot.onTake(player, stack);
        return original;
    }

    /** Original ContainerGhostSlots:19 - the base container is never usable alone. */
    @Override public boolean stillValid(Player player) { return false; }
}
