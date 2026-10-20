package thaumcraft.common.entities.golems;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.world.Container;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import thaumcraft.common.items.golems.TrunkRules;

/**
 * Original InventoryTrunk: an array of 36 stacks of which the first {@code rows * 9} are the trunk's slots. Saved as a list of
 * {@code {Slot: byte, ...stack}} compounds; a slot outside the array or an unreadable stack is dropped on load as the original did.
 */
public final class InventoryTrunk implements Container {
    private final NonNullList<ItemStack> stacks = NonNullList.withSize(TrunkRules.MAX_SLOTS, ItemStack.EMPTY);
    private final EntityTravelingTrunk trunk;

    InventoryTrunk(EntityTravelingTrunk trunk) { this.trunk = trunk; }

    /** The slots in use (original {@code slotCount}); the array behind them is always 36 long. */
    @Override public int getContainerSize() { return trunk.slotCount(); }
    @Override public boolean isEmpty() { return stacks.stream().allMatch(ItemStack::isEmpty); }
    @Override public ItemStack getItem(int slot) { return slot >= 0 && slot < stacks.size() ? stacks.get(slot) : ItemStack.EMPTY; }
    @Override public ItemStack removeItem(int slot, int amount) {
        if (slot < 0 || slot >= stacks.size()) return ItemStack.EMPTY;
        ItemStack removed = ContainerHelper.removeItem(stacks, slot, amount);
        if (!removed.isEmpty()) setChanged();
        return removed;
    }
    @Override public ItemStack removeItemNoUpdate(int slot) { return slot >= 0 && slot < stacks.size() ? ContainerHelper.takeItem(stacks, slot) : ItemStack.EMPTY; }
    @Override public void setItem(int slot, ItemStack stack) {
        if (slot < 0 || slot >= stacks.size()) return;
        stacks.set(slot, stack);
        setChanged();
    }
    @Override public void setChanged() {}
    @Override public boolean stillValid(Player player) { return true; } // Distance and liveness belong to the menu.
    @Override public void clearContent() { for (int slot = 0; slot < stacks.size(); slot++) stacks.set(slot, ItemStack.EMPTY); }

    /** Merges as much of {@code stack} as fits into the trunk's slots, then fills empty ones; returns what is left (a new stack, or empty). */
    public ItemStack insert(ItemStack stack) {
        ItemStack rest = stack.copy();
        int slots = getContainerSize();
        int limit = Math.min(rest.getMaxStackSize(), 64); // original stacklimit 64
        for (int slot = 0; slot < slots && !rest.isEmpty(); slot++) {
            ItemStack present = stacks.get(slot);
            if (present.isEmpty() || !ItemStack.isSameItemSameComponents(present, rest)) continue;
            int moved = Math.min(limit - present.getCount(), rest.getCount());
            if (moved > 0) { present.grow(moved); rest.shrink(moved); }
        }
        for (int slot = 0; slot < slots && !rest.isEmpty(); slot++) {
            if (!stacks.get(slot).isEmpty()) continue;
            int moved = Math.min(limit, rest.getCount());
            stacks.set(slot, rest.copyWithCount(moved));
            rest.shrink(moved);
        }
        return rest;
    }

    /** Everything stored, in slot order, without the empty slots. */
    public List<ItemStack> contents() {
        List<ItemStack> all = new ArrayList<>();
        for (ItemStack stack : stacks) if (!stack.isEmpty()) all.add(stack.copy());
        return all;
    }

    public ListTag save(HolderLookup.Provider provider) {
        ListTag list = new ListTag();
        for (int slot = 0; slot < stacks.size(); slot++) {
            ItemStack stack = stacks.get(slot);
            if (stack.isEmpty()) continue;
            CompoundTag entry = new CompoundTag();
            entry.putByte("Slot", (byte) slot);
            list.add(stack.save(provider, entry));
        }
        return list;
    }

    public void load(ListTag list, HolderLookup.Provider provider) {
        clearContent();
        for (int index = 0; index < list.size(); index++) {
            CompoundTag entry = list.getCompound(index);
            int slot = entry.getByte("Slot") & 0xFF;
            if (slot >= stacks.size()) continue;
            ItemStack.parse(provider, entry).ifPresent(stack -> stacks.set(slot, stack));
        }
    }

    /** Original dropAllItems: every stack is dropped at the trunk and the inventory emptied. */
    public void dropAll() {
        for (int slot = 0; slot < stacks.size(); slot++) {
            ItemStack stack = stacks.get(slot);
            if (stack.isEmpty()) continue;
            trunk.spawnAtLocation(stack, 0.0F);
            stacks.set(slot, ItemStack.EMPTY);
        }
    }
}
