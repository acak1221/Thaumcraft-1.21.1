package thaumcraft.api;

import net.minecraft.world.item.ItemStack;

/**
 * The Architect focus capability of the original TC4 API.
 *
 * <p>Original {@code thaumcraft/api/IArchitect.java} published three members: the maximum area of the
 * focus, the list of the blocks the architect would touch ({@code ArrayList<BlockCoordinates>
 * getArchitectBlocks(ItemStack, World, int, int, int, int, EntityPlayer)}) and the axis the client has to
 * draw ({@code boolean showAxis(ItemStack, World, EntityPlayer, int, EnumAxis)}), with its own
 * {@code EnumAxis} of the three axes.</p>
 *
 * <p>Forced 1.21.1 change (documented deviation): the native architect is a server-authoritative
 * re-implementation. The server owns the area of the focus ({@code WandArchitect.Area}), the client draws
 * the area of the value the server sent and no block list of the preview travels at all, so the native
 * interface publishes the size of the area and the number of the selectable axes instead of the list of
 * the highlighted blocks. The published value type of the original, {@link BlockCoordinates}, is available
 * again for an add-on that keeps its own preview.</p>
 */
public interface IArchitect {
    int getMaxAreaSize(ItemStack focus);
    /** Original Trade has two selectable in-plane axes; Warding has three. Axis zero always means all. */
    default int getAxisModes() { return 3; }
}
