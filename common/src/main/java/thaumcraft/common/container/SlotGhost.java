package thaumcraft.common.container;

import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import thaumcraft.common.lib.crafting.DeviceContainerRules;

/**
 * Original SlotGhost: a recipe preview slot. It shows the wanted item and
 * never owns stock, so the player can never take it back.
 */
public class SlotGhost extends Slot {
    private final int limit;

    public SlotGhost(Container container, int index, int x, int y) {
        this(container, index, x, y, DeviceContainerRules.GHOST_SLOT_LIMIT);
    }

    public SlotGhost(Container container, int index, int x, int y, int limit) {
        super(container, index, x, y);
        this.limit = limit;
    }

    public int limit() { return limit; }

    @Override public int getMaxStackSize() { return limit; }
    @Override public int getMaxStackSize(ItemStack stack) { return limit; }

    /** Original SlotGhost:31 - a ghost is never taken out by the player. */
    @Override public boolean mayPickup(Player player) { return false; }
}
