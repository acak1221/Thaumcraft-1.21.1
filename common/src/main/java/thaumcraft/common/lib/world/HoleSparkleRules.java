package thaumcraft.common.lib.world;

import java.util.ArrayList;
import java.util.List;

/**
 * The sparkles around the portable hole of the original.
 *
 * <p>Original {@code thaumcraft/common/tiles/TileHole.java:127-211} ({@code surroundwithsparkles}):
 * the hole block is surrounded by sparkles on every edge where it touches a solid block, and the
 * original draws twenty four of them. The rule of the original:</p>
 *
 * <ul>
 * <li>{@code yp}, {@code xp}, {@code zp}, {@code yn}, {@code xn} and {@code zn} say whether the
 * block above, the block at +X, the block at +Z, the block below, the block at -X and the block
 * at -Z of the hole is an opaque cube ({@code Block.func_149662_c}, the MCP name
 * {@code isOpaqueCube});</li>
 * <li>{@code b1} to {@code b6} say whether the same six blocks are <b>not</b> the hole block
 * itself;</li>
 * <li>a sparkle is drawn for every combination of a horizontal neighbour that is not opaque and
 * a vertical neighbour that is opaque, when both of them are not the hole block.</li>
 * </ul>
 *
 * <p>The position of a sparkle is the edge of the hole block between the two neighbours, and one
 * of the three axes carries a random fraction (the {@code world.rand.nextFloat()} of the
 * original).</p>
 */
public final class HoleSparkleRules {

    /** The axis that carries the random fraction of a sparkle. */
    public enum Axis { X, Y, Z, NONE }

    /** One sparkle of the original: the offset of the edge and the axis of the random fraction. */
    public record Sparkle(int dx, int dy, int dz, Axis random) {}

    /**
     * The six neighbours of the hole block, as the original asks them.
     *
     * @param upOpaque    the block above is an opaque cube
     * @param eastOpaque  the block at +X is an opaque cube
     * @param southOpaque the block at +Z is an opaque cube
     * @param downOpaque  the block below is an opaque cube
     * @param westOpaque  the block at -X is an opaque cube
     * @param northOpaque the block at -Z is an opaque cube
     * @param upHole      the block above is the hole block
     * @param downHole    the block below is the hole block
     * @param southHole   the block at +Z is the hole block
     * @param northHole   the block at -Z is the hole block
     * @param westHole    the block at -X is the hole block
     * @param eastHole    the block at +X is the hole block
     */
    public record Neighbours(boolean upOpaque, boolean eastOpaque, boolean southOpaque,
                              boolean downOpaque, boolean westOpaque, boolean northOpaque,
                              boolean upHole, boolean downHole, boolean southHole,
                              boolean northHole, boolean westHole, boolean eastHole) {}

    private HoleSparkleRules() {}

    /**
     * The sparkles of the original, in the order of the original.
     *
     * <p>Original {@code TileHole.java:145-210}: the twenty four calls of
     * {@code Thaumcraft.proxy.sparkle}, each with the type two.</p>
     */
    public static List<Sparkle> sparkles(Neighbours n) {
        boolean yp = n.upOpaque(), xp = n.eastOpaque(), zp = n.southOpaque();
        boolean yn = n.downOpaque(), xn = n.westOpaque(), zn = n.northOpaque();
        boolean b1 = !n.upHole(), b2 = !n.downHole(), b3 = !n.northHole();
        boolean b4 = !n.southHole(), b5 = !n.westHole(), b6 = !n.eastHole();
        List<Sparkle> out = new ArrayList<>(24);
        if (!xp && yp && b6) out.add(new Sparkle(1, 1, 0, Axis.Z));
        if (!xn && yp && b5) out.add(new Sparkle(0, 1, 0, Axis.Z));
        if (!zp && yp && b4) out.add(new Sparkle(0, 1, 1, Axis.X));
        if (!zn && yp && b3) out.add(new Sparkle(0, 1, 0, Axis.X));
        if (!xp && yn && b6) out.add(new Sparkle(1, 0, 0, Axis.Z));
        if (!xn && yn && b5) out.add(new Sparkle(0, 0, 0, Axis.Z));
        if (!zp && yn && b4) out.add(new Sparkle(0, 0, 1, Axis.X));
        if (!zn && yn && b3) out.add(new Sparkle(0, 0, 0, Axis.X));
        if (!yp && xp && b1) out.add(new Sparkle(1, 1, 0, Axis.Z));
        if (!yn && xp && b2) out.add(new Sparkle(1, 0, 0, Axis.Z));
        if (!zp && xp && b4) out.add(new Sparkle(1, 0, 1, Axis.Y));
        if (!zn && xp && b3) out.add(new Sparkle(1, 0, 0, Axis.Y));
        if (!yp && xn && b1) out.add(new Sparkle(0, 1, 0, Axis.Z));
        if (!yn && xn && b2) out.add(new Sparkle(0, 0, 0, Axis.Z));
        if (!zp && xn && b4) out.add(new Sparkle(0, 0, 1, Axis.Y));
        if (!zn && xn && b3) out.add(new Sparkle(0, 0, 0, Axis.Y));
        if (!xp && zp && b6) out.add(new Sparkle(1, 0, 1, Axis.Y));
        if (!xn && zp && b5) out.add(new Sparkle(0, 0, 1, Axis.Y));
        if (!yp && zp && b1) out.add(new Sparkle(0, 1, 1, Axis.X));
        if (!yn && zp && b2) out.add(new Sparkle(0, 0, 1, Axis.X));
        if (!xp && zn && b6) out.add(new Sparkle(1, 0, 0, Axis.Y));
        if (!xn && zn && b5) out.add(new Sparkle(0, 0, 0, Axis.Y));
        if (!yp && zn && b1) out.add(new Sparkle(0, 1, 0, Axis.X));
        if (!yn && zn && b2) out.add(new Sparkle(0, 0, 0, Axis.X));
        return out;
    }
}
