package thaumcraft.api;

import net.minecraft.world.item.ItemStack;

/** Native Architect focus capability; wand dimensions are owned by the server, never by a client-selected block list. */
public interface IArchitect {
    int getMaxAreaSize(ItemStack focus);
    /** Original Trade has two selectable in-plane axes; Warding has three. Axis zero always means all. */
    default int getAxisModes() { return 3; }
}
