package thaumcraft.api.aura;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.api.aspects.AspectList;
import thaumcraft.api.visnet.VisNetHandler;
import thaumcraft.common.tiles.TileNode;

/**
 * The aura helper of the add-on surface, projected onto the native TC4 aura node.
 *
 * <p>Forced 1.21.1 change: TC4 has no per-chunk aura and no flux; the native aura is the aura node's own
 * stored essence. The vis accessors therefore read and write the node at the given position, the drain
 * reaches the vis network at the given position, and the flux accessors of the TC6 shape do not exist in
 * this port.</p>
 */
public final class AuraHelper {
    private AuraHelper() {}

    /** The node at the position, or null when there is none. */
    public static TileNode node(Level level, BlockPos pos) {
        if (level == null || pos == null) return null;
        return level.getBlockEntity(pos) instanceof TileNode node ? node : null;
    }

    /** The total essence stored in the node at the position. */
    public static int getVis(Level level, BlockPos pos) {
        TileNode node = node(level, pos);
        if (node == null) return 0;
        int total = 0;
        for (Aspect aspect : node.getAspects().getAspects()) total += node.getAspects().getAmount(aspect);
        return total;
    }

    /** The essence of one aspect stored in the node at the position. */
    public static int getVis(Level level, BlockPos pos, Aspect aspect) {
        TileNode node = node(level, pos);
        if (node == null || aspect == null) return 0;
        return node.getAspects().getAmount(aspect);
    }

    /**
     * Removes essence from the vis network at the position.
     *
     * <p>The retained 1.12 reference drained the VIS NETWORK and not the essence of the node at the
     * position: {@code src/main/java/thaumcraft/common/world/aura/AuraHandler.java:27-33} delegates to
     * {@code VisNetHandler.drainVis(world, x, y, z, requested, simulate)}, and that method walks the
     * primal aspects of every root source that is in range of the position
     * ({@code src/main/java/thaumcraft/api/visnet/VisNetHandler.java:92-139}). The native projection
     * keeps that selection: the drain reaches the whole network at the position, not one node.</p>
     *
     * @param amount the amount to remove
     * @param simulate true to report what would be removed without changing the network
     * @return the amount that was (or would be) removed
     */
    public static int drainVis(Level level, BlockPos pos, int amount, boolean simulate) {
        if (level == null || pos == null || amount <= 0) return 0;
        return VisNetHandler.drainVis(level, pos, amount, simulate);
    }

    /** Adds essence to the node at the position; the node's own acceptance rules apply. */
    public static int addVis(Level level, BlockPos pos, AspectList aspects) {
        TileNode node = node(level, pos);
        if (node == null || aspects == null) return 0;
        int added = 0;
        for (Aspect aspect : aspects.getAspects()) {
            int amount = aspects.getAmount(aspect);
            if (amount <= 0) continue;
            int remainder = node.addToContainer(aspect, amount);
            added += amount - Math.max(0, remainder);
        }
        return added;
    }
}
