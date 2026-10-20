package thaumcraft.common.entities;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.Container;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import thaumcraft.common.lib.entities.ProjectileRules;

/**
 * TC4 InventoryMob, the carried inventory of a mob.
 * <p>
 * The original is a plain {@code IInventory} with the original quirks: it
 * reports one slot more than it owns, it never merges into an empty slot when
 * it looks for a home (an empty slot is skipped), a damaged item is stored as
 * a whole stack in the first empty slot while an undamaged item is merged
 * partially, and the amount that fits is limited by the stack size of the item
 * and by the inventory limit of the mob.
 * <p>
 * Declared native adaptations: the metadata of the 1.7 stacks is the damage
 * value of the modern stacks, the "are the stacks equal" test is the native
 * component aware comparison, and the travelling trunk hook of the original
 * is a listener instead of a hard cast.
 */
public class InventoryMob implements Container {
    private final ItemStack[] inventory;
    private final Entity ent;
    private final int slotCount;
    private boolean inventoryChanged;
    private int stacklimit = ProjectileRules.INVENTORY_MOB_STACK_LIMIT;
    private Runnable openListener = () -> {};
    private Runnable closeListener = () -> {};

    public InventoryMob(Entity entity, int slots) {
        this(entity, slots, ProjectileRules.INVENTORY_MOB_STACK_LIMIT);
    }

    public InventoryMob(Entity entity, int slots, int limit) {
        this.slotCount = slots;
        this.inventory = new ItemStack[slots];
        this.stacklimit = limit;
        this.ent = entity;
    }

    public Entity getOwner() { return ent; }
    public int getSlotCount() { return slotCount; }
    public boolean isInventoryChanged() { return inventoryChanged; }
    public ItemStack[] getStacks() { return inventory; }

    /** Original {@code getInventorySlotContainItem}: the first slot with that item. */
    public int getInventorySlotContainItem(Item item) {
        for (int slot = 0; slot < inventory.length; slot++) {
            if (inventory[slot] == null) continue;
            if (!inventory[slot].is(item)) continue;
            return slot;
        }
        return -1;
    }

    /** Original {@code storeItemStack}: the slot that may receive the offered stack. */
    public int storeItemStack(ItemStack itemstack) {
        for (int slot = 0; slot < inventory.length; slot++) {
            ItemStack stored = inventory[slot];
            if (stored == null) continue;
            boolean sameItem = stored.is(itemstack.getItem());
            boolean stackable = stored.isStackable();
            boolean atStackLimit = stored.getCount() >= stored.getMaxStackSize();
            boolean atInventoryLimit = stored.getCount() >= getMaxStackSize();
            boolean differentMetadata = stored.isDamageableItem() && stored.getDamageValue() != itemstack.getDamageValue();
            if (!ProjectileRules.inventoryMobAccepts(sameItem, stackable, atStackLimit, atInventoryLimit, differentMetadata)) continue;
            return slot;
        }
        return -1;
    }

    /** Original {@code getFirstEmptyStack}. */
    public int getFirstEmptyStack() {
        boolean[] occupied = new boolean[inventory.length];
        for (int slot = 0; slot < inventory.length; slot++) occupied[slot] = inventory[slot] != null;
        return ProjectileRules.inventoryMobFirstEmpty(occupied);
    }

    /**
     * Original {@code storePartialItemStack}: stores as much as the slot and
     * the inventory limit allow and returns the count that is left over.
     */
    public int storePartialItemStack(ItemStack itemstack) {
        int offered = itemstack.getCount();
        int slot = storeItemStack(itemstack);
        if (slot < 0) slot = getFirstEmptyStack();
        if (slot < 0) return offered;
        if (inventory[slot] == null) inventory[slot] = new ItemStack(itemstack.getItem(), 0);
        int amount = ProjectileRules.inventoryMobStoreAmount(offered,
                inventory[slot].getMaxStackSize() - inventory[slot].getCount(),
                getMaxStackSize() - inventory[slot].getCount());
        if (amount == 0) return offered;
        inventory[slot].setCount(inventory[slot].getCount() + amount);
        setChanged();
        return offered - amount;
    }

    /**
     * Original {@code addItemStackToInventory}: a damaged item is stored as a
     * whole stack in the first empty slot, every other item is merged.
     */
    public boolean addItemStackToInventory(ItemStack itemstack) {
        if (!(itemstack.isDamageableItem() && itemstack.getDamageValue() > 0)) {
            int before;
            do {
                before = itemstack.getCount();
                itemstack.setCount(storePartialItemStack(itemstack));
            } while (itemstack.getCount() > 0 && itemstack.getCount() < before);
            return itemstack.getCount() < before;
        }
        int slot = getFirstEmptyStack();
        if (slot >= 0) {
            inventory[slot] = itemstack.copy();
            itemstack.setCount(0);
            setChanged();
            return true;
        }
        return false;
    }

    @Override public int getContainerSize() {
        // The original reports one slot more than it owns.
        return ProjectileRules.inventoryMobContainerSize(inventory.length);
    }

    @Override public boolean isEmpty() {
        for (ItemStack stack : inventory) if (stack != null && !stack.isEmpty()) return false;
        return true;
    }

    @Override public ItemStack getItem(int slot) {
        return slot >= 0 && slot < inventory.length && inventory[slot] != null ? inventory[slot] : ItemStack.EMPTY;
    }

    @Override public ItemStack removeItem(int slot, int amount) {
        if (slot < 0 || slot >= inventory.length || inventory[slot] == null) return ItemStack.EMPTY;
        ItemStack stored = inventory[slot];
        ItemStack taken = stored.split(amount);
        if (stored.isEmpty()) inventory[slot] = null;
        setChanged();
        return taken;
    }

    @Override public ItemStack removeItemNoUpdate(int slot) {
        if (slot < 0 || slot >= inventory.length || inventory[slot] == null) return ItemStack.EMPTY;
        ItemStack stored = inventory[slot];
        inventory[slot] = null;
        return stored;
    }

    @Override public void setItem(int slot, ItemStack stack) {
        if (slot < 0 || slot >= inventory.length) return;
        inventory[slot] = stack;
        setChanged();
    }

    @Override public int getMaxStackSize() { return stacklimit; }

    @Override public int getMaxStackSize(ItemStack stack) {
        return stack == null || stack.isEmpty() ? stacklimit : Math.min(stacklimit, stack.getMaxStackSize());
    }

    @Override public void setChanged() { inventoryChanged = true; }

    @Override public boolean stillValid(Player player) {
        // The original allows every player inside eight blocks of the mob.
        if (ent == null || ent.isRemoved()) return false;
        return player.distanceToSqr(ent) <= ProjectileRules.INVENTORY_MOB_REACH;
    }

    @Override public boolean canPlaceItem(int slot, ItemStack stack) { return true; }

    @Override public void clearContent() {
        for (int slot = 0; slot < inventory.length; slot++) inventory[slot] = null;
        setChanged();
    }

    /** Original {@code dropAllItems}: the whole inventory drops on death. */
    public void dropAllItems() {
        for (int slot = 0; slot < inventory.length; slot++) {
            if (inventory[slot] == null) continue;
            if (ent != null) ent.spawnAtLocation(inventory[slot], 0F);
            inventory[slot] = null;
        }
    }

    /** Original {@code hasSomething}. */
    public boolean hasSomething() {
        for (int slot = 0; slot < slotCount; slot++) if (inventory[slot] != null) return true;
        return false;
    }

    /** Original {@code allEmpty}. */
    public boolean allEmpty() {
        for (int slot = 0; slot < slotCount; slot++) if (inventory[slot] != null) return false;
        return true;
    }

    /** Original {@code getAmountNeeded}: the amount of the same item in the inventory. */
    public int getAmountNeeded(ItemStack stackInSlot) {
        int amount = 0;
        for (int slot = 0; slot < slotCount; slot++) {
            if (inventory[slot] == null) continue;
            if (!ItemStack.isSameItem(inventory[slot], stackInSlot)) continue;
            amount += inventory[slot].getCount();
        }
        return amount;
    }

    /** Original {@code getAmountNeededSmart}: the fuzzy variant counts the component equal stacks as well. */
    public int getAmountNeededSmart(ItemStack stackInSlot, boolean fuzzy) {
        int amount = 0;
        for (int slot = 0; slot < slotCount; slot++) {
            if (inventory[slot] == null) continue;
            if (fuzzy) {
                // The original fuzzy branch used the ore dictionary, which has no native counterpart.
                if (ItemStack.isSameItem(inventory[slot], stackInSlot)) amount += inventory[slot].getCount();
                continue;
            }
            if (!ItemStack.isSameItem(inventory[slot], stackInSlot)) continue;
            if (!ItemStack.isSameItemSameComponents(inventory[slot], stackInSlot)) continue;
            amount += inventory[slot].getCount();
        }
        return amount;
    }

    /** Original {@code getItemsNeeded}: a copy of every stored stack. */
    public ArrayList<ItemStack> getItemsNeeded(boolean fuzzy) {
        ArrayList<ItemStack> needed = new ArrayList<>();
        for (int slot = 0; slot < slotCount; slot++) {
            if (inventory[slot] == null) continue;
            needed.add(inventory[slot].copy());
        }
        return needed;
    }

    /** The original stores the stacks in a list of slot compounds. */
    public ListTag writeToNBT() {
        ListTag list = new ListTag();
        for (int slot = 0; slot < inventory.length; slot++) {
            if (inventory[slot] == null) continue;
            CompoundTag tag = new CompoundTag();
            tag.putByte("Slot", (byte) slot);
            tag.put("Item", inventory[slot].save(ent.registryAccess()));
            list.add(tag);
        }
        return list;
    }

    /** The original reads the stacks back from a list of slot compounds. */
    public void readFromNBT(ListTag list) {
        for (int slot = 0; slot < inventory.length; slot++) inventory[slot] = null;
        for (int index = 0; index < list.size(); index++) {
            CompoundTag tag = list.getCompound(index);
            int slot = tag.getByte("Slot") & 0xFF;
            if (slot < 0 || slot >= inventory.length) continue;
            ItemStack stack = ItemStack.parse(ent.registryAccess(), tag.getCompound("Item")).orElse(ItemStack.EMPTY);
            if (stack.isEmpty()) continue;
            inventory[slot] = stack;
        }
    }

    /** The original opened and closed the travelling trunk with the inventory. */
    public void setOpenListener(Runnable listener) { openListener = listener == null ? () -> {} : listener; }
    public void setCloseListener(Runnable listener) { closeListener = listener == null ? () -> {} : listener; }

    @Override public void startOpen(Player player) { openListener.run(); }

    @Override public void stopOpen(Player player) { closeListener.run(); }
}
