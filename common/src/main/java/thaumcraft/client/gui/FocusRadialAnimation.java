package thaumcraft.client.gui;

/** Pure rules from oracle REHWandHandler:151-178,212,249-255. */
public final class FocusRadialAnimation {
    private float scale;
    private long lastTime;

    public float scale() { return scale; }

    /** One step per eligible rendered frame, never a time-difference catch-up loop. */
    public boolean advance(long time, boolean active) {
        if (time <= lastTime) return false;
        scale = Math.max(0F, Math.min(1F, scale + (active ? .05F : -.05F)));
        lastTime = time + 5L;
        return true;
    }

    /** Do not clamp the original float hover stepping (it can overshoot 1.3). */
    public static float hover(float scale, boolean hovered) {
        return hovered ? (scale < 1.3F ? scale + .025F : scale)
                : (scale > 1F ? scale - .025F : scale);
    }

    public static float radius(int count) { return 16F + count * 2.5F; }
    public static float angle(int index, int count, float scale) { return -90F * scale + index * (360F / count); }
}
