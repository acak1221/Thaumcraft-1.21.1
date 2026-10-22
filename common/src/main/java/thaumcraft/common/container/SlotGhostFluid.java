package thaumcraft.common.container;

import net.minecraft.core.component.DataComponents;
import net.minecraft.world.Container;
import net.minecraft.world.item.BucketItem;
import net.minecraft.world.item.ItemStack;
import thaumcraft.common.lib.crafting.DeviceContainerRules;

/**
 * Original SlotGhostFluid: the liquid golem ghost preview. It only ever holds
 * a single fluid container and never gives it back.
 *
 * <p>Forced 1.21.1 change: 1.21.1 has no Forge fluid container registry, so the fluid container
 * of the original is the native one: a bucket of the vanilla or the item of an add-on that holds
 * a fluid in the bucket entity data component. The limit of one item and the rule that the slot never
 * gives the container back are the original ones.</p>
 */
public class SlotGhostFluid extends SlotGhost {
    public SlotGhostFluid(Container container, int index, int x, int y) {
        super(container, index, x, y, DeviceContainerRules.ghostSlotLimit(true));
    }

    /** Original SlotGhostFluid:30 - only a fluid container fits here. */
    @Override public boolean mayPlace(ItemStack stack) {
        return DeviceContainerRules.ghostSlotAcceptsFluidContainer(isFluidContainer(stack));
    }

    /**
     * The native fluid container of the original registry.
     *
     * @param stack the stack that is offered to the slot
     * @return whether the stack is a fluid container
     */
    public static boolean isFluidContainer(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return false;
        // The vanilla buckets of 1.21.1 and the bucket of an add-on that carries the bucket content.
        return stack.getItem() instanceof BucketItem || stack.has(DataComponents.BUCKET_ENTITY_DATA);
    }
}
