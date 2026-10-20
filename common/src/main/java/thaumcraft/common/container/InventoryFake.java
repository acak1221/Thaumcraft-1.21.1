package thaumcraft.common.container;

import java.util.List;
import net.minecraft.core.NonNullList;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

/**
 * Original InventoryFake: a throwaway inventory used to report a single
 * crafted item to the crafting event without owning any real stock. It never
 * marks itself dirty, never opens for a player and accepts everything.
 *
 * <p>1.21.1 adaptation: {@code getItem} returns {@link ItemStack#EMPTY}
 * instead of the original null for an index outside the list, because a modern
 * container may never hand out a null stack.</p>
 */
public final class InventoryFake implements Container {
    public static final String NAME = "container.fake";
    public static final int MAX_STACK_SIZE = 64;
    private final NonNullList<ItemStack> stacks;
    private final String name;

    public InventoryFake(int size) { this(NAME, size); }

    public InventoryFake(String name, int size) {
        this.name = name;
        this.stacks = NonNullList.withSize(Math.max(0, size), ItemStack.EMPTY);
    }

    public InventoryFake(ItemStack[] contents) {
        this(NAME, contents.length);
        for (int slot = 0; slot < contents.length; slot++) {
            ItemStack stack = contents[slot];
            if (stack != null && !stack.isEmpty()) stacks.set(slot, stack.copy());
        }
    }

    public InventoryFake(List<ItemStack> contents) { this(contents.toArray(new ItemStack[0])); }

    public String getName() { return name; }

    @Override public int getContainerSize() { return stacks.size(); }

    @Override public boolean isEmpty() {
        for (ItemStack stack : stacks) if (!stack.isEmpty()) return false;
        return true;
    }

    @Override public ItemStack getItem(int slot) { return slot >= 0 && slot < stacks.size() ? stacks.get(slot) : ItemStack.EMPTY; }

    @Override public ItemStack removeItem(int slot, int amount) {
        if (slot < 0 || slot >= stacks.size()) return ItemStack.EMPTY;
        ItemStack stack = stacks.get(slot);
        if (stack.isEmpty()) return ItemStack.EMPTY;
        if (stack.getCount() <= amount) {
            stacks.set(slot, ItemStack.EMPTY);
            return stack;
        }
        ItemStack taken = stack.split(amount);
        if (stack.isEmpty()) stacks.set(slot, ItemStack.EMPTY);
        return taken;
    }

    @Override public ItemStack removeItemNoUpdate(int slot) {
        if (slot < 0 || slot >= stacks.size()) return ItemStack.EMPTY;
        ItemStack stack = stacks.get(slot);
        stacks.set(slot, ItemStack.EMPTY);
        return stack;
    }

    @Override public void setItem(int slot, ItemStack stack) {
        if (slot < 0 || slot >= stacks.size()) return;
        stacks.set(slot, stack == null ? ItemStack.EMPTY : stack);
    }

    @Override public int getMaxStackSize() { return MAX_STACK_SIZE; }

    /** Original InventoryFake:75 - a fake inventory accepts everything. */
    @Override public boolean canPlaceItem(int slot, ItemStack stack) { return true; }

    /** Original InventoryFake:80 - a fake inventory is never dirty. */
    @Override public void setChanged() {}

    /** Original InventoryFake:71 - a fake inventory is always usable. */
    @Override public boolean stillValid(Player player) { return true; }

    @Override public void startOpen(Player player) {}

    @Override public void stopOpen(Player player) {}

    @Override public void clearContent() { stacks.clear(); }

    /** The live contents, for a caller that has to report them. */
    public NonNullList<ItemStack> getItems() { return stacks; }
}
