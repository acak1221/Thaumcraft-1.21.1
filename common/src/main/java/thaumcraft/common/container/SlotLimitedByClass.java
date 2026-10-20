package thaumcraft.common.container;

import net.minecraft.world.Container;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import thaumcraft.common.lib.crafting.DeviceContainerRules;

/**
 * Original SlotLimitedByClass: a slot that only accepts an item of one
 * assignable class, for example a basic focus or an excavation focus.
 */
public class SlotLimitedByClass extends Slot {
    private final Class<?> accepted;
    private final int limit;

    public SlotLimitedByClass(Class<?> accepted, Container container, int index, int x, int y) {
        this(accepted, 64, container, index, x, y);
    }

    public SlotLimitedByClass(Class<?> accepted, int limit, Container container, int index, int x, int y) {
        super(container, index, x, y);
        this.accepted = accepted;
        this.limit = limit;
    }

    public Class<?> accepted() { return accepted; }

    /** Original SlotLimitedByClass:29 - the item class must be assignable. */
    @Override public boolean mayPlace(ItemStack stack) {
        return !stack.isEmpty() && DeviceContainerRules.limitedByClass(accepted, stack.getItem().getClass());
    }

    @Override public int getMaxStackSize() { return limit; }
    @Override public int getMaxStackSize(ItemStack stack) { return limit; }
}
