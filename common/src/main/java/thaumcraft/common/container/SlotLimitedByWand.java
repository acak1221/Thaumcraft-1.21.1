package thaumcraft.common.container;

import net.minecraft.world.Container;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import thaumcraft.common.items.wands.ItemWandCasting;
import thaumcraft.common.lib.crafting.DeviceContainerRules;

/**
 * Original SlotLimitedByWand: the arcane workbench wand slot. A casting wand
 * fits, a staff never does.
 */
public class SlotLimitedByWand extends Slot {
    private final int limit;

    public SlotLimitedByWand(Container container, int index, int x, int y) {
        this(container, index, x, y, 64);
    }

    public SlotLimitedByWand(Container container, int index, int x, int y, int limit) {
        super(container, index, x, y);
        this.limit = limit;
    }

    /** Original SlotLimitedByWand:29 - a casting wand that is not a staff. */
    @Override public boolean mayPlace(ItemStack stack) {
        if (stack.isEmpty() || !(stack.getItem() instanceof ItemWandCasting)) return false;
        return DeviceContainerRules.limitedByWand(true, ItemWandCasting.isStaff(stack));
    }

    @Override public int getMaxStackSize() { return limit; }
    @Override public int getMaxStackSize(ItemStack stack) { return limit; }
}
