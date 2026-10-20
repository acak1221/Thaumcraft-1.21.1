package thaumcraft.common.container;

import net.minecraft.world.Container;
import net.minecraft.world.item.BucketItem;
import net.minecraft.world.item.ItemStack;
import thaumcraft.common.lib.crafting.DeviceContainerRules;

/**
 * Original SlotGhostFluid: the liquid golem ghost preview. It only ever holds
 * a single fluid container and never gives it back.
 */
public class SlotGhostFluid extends SlotGhost {
    public SlotGhostFluid(Container container, int index, int x, int y) {
        super(container, index, x, y, DeviceContainerRules.ghostSlotLimit(true));
    }

    /** Original SlotGhostFluid:30 - only a fluid container fits here. */
    @Override public boolean mayPlace(ItemStack stack) {
        return DeviceContainerRules.ghostSlotAcceptsFluidContainer(!stack.isEmpty() && stack.getItem() instanceof BucketItem);
    }
}
