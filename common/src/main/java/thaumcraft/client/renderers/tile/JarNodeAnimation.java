package thaumcraft.client.renderers.tile;

import java.util.function.LongSupplier;
import thaumcraft.common.tiles.TileJarNode;

/** The one second capture transition, sampled only while the jar is being rendered. */
public final class JarNodeAnimation {
    public static final float CAPTURE_SHELL_SCALE = 3F;
    public static final float CAPTURE_NODE_SCALE = 1F;
    public static final float STORED_NODE_SCALE = .7F;
    public static final Frame STORED = new Frame(1F, STORED_NODE_SCALE, false);

    private JarNodeAnimation() {}

    /** No clock is read for a settled jar; there is no tick task or retained animation state. */
    public static Frame sample(long deadlineMillis, LongSupplier clock) {
        return deadlineMillis <= 0L ? STORED : sample(deadlineMillis, clock.getAsLong());
    }

    public static Frame sample(long deadlineMillis, long nowMillis) {
        if (deadlineMillis <= 0L || nowMillis >= deadlineMillis) return STORED;
        // Clamp a clock moving backwards as well as the ordinary first frame.
        double remaining = Math.min(1D, ((double) deadlineMillis - nowMillis) / TileJarNode.ANIMATE_MILLIS);
        return new Frame((float) (1D + (CAPTURE_SHELL_SCALE - 1D) * remaining),
                (float) (STORED_NODE_SCALE + (CAPTURE_NODE_SCALE - STORED_NODE_SCALE) * remaining), true);
    }

    public record Frame(float shellScale, float nodeScale, boolean active) {}
}
