package thaumcraft.common.container;

import net.minecraft.world.Container;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import thaumcraft.common.lib.crafting.DeviceContainerRules;
import thaumcraft.common.lib.research.ObjectAspectResolver;

/**
 * Original SlotLimitedHasAspects: a slot that only accepts an item which
 * carries aspects, so the alchemy furnace and the deconstruction table can
 * never waste a plain item.
 */
public class SlotLimitedHasAspects extends Slot {
    private final Level level;

    public SlotLimitedHasAspects(Container container, int index, int x, int y) {
        this(container, index, x, y, levelOf(container));
    }

    public SlotLimitedHasAspects(Container container, int index, int x, int y, Level level) {
        super(container, index, x, y);
        this.level = level;
    }

    private static Level levelOf(Container container) { return container instanceof BlockEntity blockEntity ? blockEntity.getLevel() : null; }

    /** Original SlotLimitedHasAspects:22 - the item must carry aspects. */
    @Override public boolean mayPlace(ItemStack stack) {
        if (stack.isEmpty() || level == null) return false;
        return DeviceContainerRules.limitedHasAspects(ObjectAspectResolver.get(stack, level).size());
    }
}
