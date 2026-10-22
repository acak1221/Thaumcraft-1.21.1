package thaumcraft.api;

import java.util.ArrayList;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/**
 * The Architect focus capability of the original TC4 API.
 *
 * <p>Original {@code thaumcraft/api/IArchitect.java:17-28} published three members: the list of the
 * blocks the architect would touch ({@code ArrayList<BlockCoordinates> getArchitectBlocks(ItemStack,
 * World, int, int, int, int, EntityPlayer)}), the axis the client has to draw ({@code boolean
 * showAxis(ItemStack, World, EntityPlayer, int, EnumAxis)}) and its own {@code EnumAxis} of the
 * three axes. The native architect is a server-authoritative re-implementation: the server owns the
 * area of the focus ({@code WandArchitect.Area}), the client draws the area of the value the server
 * sent and no block list of the preview travels at all. The published members are therefore the
 * server side truth of the original: an add-on that asks a focus for the blocks of its area or for
 * the axes of its preview gets the original answer, and the published value type of the original,
 * {@link BlockCoordinates}, is the type of that list.</p>
 *
 * <p>Forced 1.21.1 change: {@code World} became {@link Level} and {@code EntityPlayer} became
 * {@link Player}; the two native architect foci publish the two members of the original with the
 * original rules. The native {@link #getMaxAreaSize(ItemStack)} and {@link #getAxisModes()} are
 * the additions of the port: the original had no member for the size of the area.</p>
 */
public interface IArchitect {
    /**
     * The three axes of the original.
     *
     * <p>Original {@code IArchitect:22-26}.</p>
     */
    enum EnumAxis {
        X,
        Y,
        Z
    }

    /**
     * Original {@code IArchitect:18} - the blocks the architect would touch.
     *
     * @param wand   the wand that holds the focus, as the original implementations read it
     * @param world  the world of the area
     * @param x      the X coordinate of the anchor
     * @param y      the Y coordinate of the anchor
     * @param z      the Z coordinate of the anchor
     * @param side   the face of the anchor, the ordinal of the original direction
     * @param player the player that owns the preview
     * @return the blocks of the area, in the order of the original walk
     */
    ArrayList<BlockCoordinates> getArchitectBlocks(ItemStack wand, Level world, int x, int y, int z, int side, Player player);

    /**
     * Original {@code IArchitect:20} - the axis the client has to draw.
     *
     * @param wand   the wand that holds the focus, as the original implementations read it
     * @param world  the world of the area
     * @param player the player that owns the preview
     * @param side   the face of the anchor, the ordinal of the original direction
     * @param axis   the axis of the preview
     * @return whether that axis is part of the preview
     */
    boolean showAxis(ItemStack wand, Level world, Player player, int side, EnumAxis axis);

    /**
     * The size of the area of the focus, the addition of the port.
     *
     * @param focus the focus
     * @return the maximum radius of the area
     */
    int getMaxAreaSize(ItemStack focus);

    /** Original Trade has two selectable in-plane axes; Warding has three. Axis zero always means all. */
    default int getAxisModes() { return 3; }
}
