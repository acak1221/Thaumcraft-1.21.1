package thaumcraft.common.lib.world;

/**
 * Registry-free arithmetic of the Axe of the Stream, kept apart so the original
 * numbers are testable without a world. Sources: TC4 4.2.3.5
 * BlockUtils.findBlocks (greedy outward walk), ItemElementalAxe.onUsingTick
 * (held vacuum) and EntityFollowingItem.onUpdate (drop follow).
 */
public final class StreamAxeGeometry {
    /** Original anchor-relative bounds: |dx|<=24, |dy|<=48, |dz|<=24. */
    public static final int MAX_X = 24, MAX_Y = 48, MAX_Z = 24;
    public static final double PULL = 0.3, CLAMP = 0.35;
    /** EntityFollowingItem starts at 20 and counts down to 1, speeding the approach. */
    public static final int FOLLOW_START = 20;
    public static final double ARRIVE = 0.5;

    private StreamAxeGeometry() {}

    public record Cell(int x, int y, int z) {}

    public interface Probe {
        /** True when this cell is a legitimate next link: same Block, recognised log, hardness >= 0, loaded. */
        boolean candidate(int x, int y, int z);
    }

    /**
     * Original BlockUtils.findBlocks, iterated instead of recursed. Scan order is X -2..2,
     * Y +2..-2, Z -2..2. The first candidate strictly farther from the ANCHOR than the
     * current one wins and the walk restarts from it. The first scanned cell outside the
     * anchor-relative bounds ends the whole walk (original quirk), even if a later cell in
     * the same scan would have qualified. Distance strictly increases, so it terminates.
     */
    public static Cell walk(int ax, int ay, int az, Probe probe) {
        int lx = ax, ly = ay, lz = az;
        double lastDistance = 0.0;
        while (true) {
            boolean moved = false;
            scan:
            for (int xx = -2; xx <= 2; xx++) {
                for (int yy = 2; yy >= -2; yy--) {
                    for (int zz = -2; zz <= 2; zz++) {
                        int cx = lx + xx, cy = ly + yy, cz = lz + zz;
                        if (Math.abs(cx - ax) > MAX_X || Math.abs(cy - ay) > MAX_Y || Math.abs(cz - az) > MAX_Z) break scan;
                        if (!probe.candidate(cx, cy, cz)) continue;
                        double dx = cx - ax, dy = cy - ay, dz = cz - az;
                        double distance = dx * dx + dy * dy + dz * dz;
                        if (!(distance > lastDistance)) continue;
                        lastDistance = distance;
                        lx = cx; ly = cy; lz = cz;
                        moved = true;
                        break scan;
                    }
                }
            }
            if (!moved) return new Cell(lx, ly, lz);
        }
    }

    /**
     * One held-use tick for one item: velocity -= unit(delta) * 0.3, each component clamped
     * to +-0.35. delta = (itemX-playerX, itemY-playerY+playerHeight/2, itemZ-playerZ); the
     * caller supplies it already built, so the original Y expression stays explicit. A
     * zero-length or non-finite delta leaves the velocity unchanged (the original divided
     * by zero and produced NaN motion).
     */
    public static double[] pull(double vx, double vy, double vz, double dx, double dy, double dz) {
        double length = Math.sqrt(dx * dx + dy * dy + dz * dz);
        if (!(length > 1.0E-9) || Double.isInfinite(length)) return new double[] {vx, vy, vz};
        vx -= dx / length * PULL;
        vy -= dy / length * PULL;
        vz -= dz / length * PULL;
        return new double[] {clamp(vx), clamp(vy), clamp(vz)};
    }

    private static double clamp(double value) {
        return value > CLAMP ? CLAMP : value < -CLAMP ? -CLAMP : value;
    }

    /** Result of one EntityFollowingItem tick: the velocity to apply and whether the item arrived. */
    public record Follow(double vx, double vy, double vz, int counter, boolean arrived) {}

    /**
     * EntityFollowingItem.onUpdate with a live target. The counter drops by one while above 1;
     * farther than 0.5 the velocity is delta / (distance * counter) (so it accelerates from
     * 0.05 to 1 block per tick), otherwise the item arrives, keeping a tenth of its motion.
     */
    public static Follow follow(double ix, double iy, double iz, double tx, double ty, double tz,
                                int counter, double mx, double my, double mz) {
        double xd = tx - ix, yd = ty - iy, zd = tz - iz;
        if (counter > 1) counter--;
        double distance = Math.sqrt(xd * xd + yd * yd + zd * zd);
        if (distance > ARRIVE && !Double.isInfinite(distance)) {
            double scaled = distance * counter;
            return new Follow(xd / scaled, yd / scaled, zd / scaled, counter, false);
        }
        return new Follow(mx * 0.1, my * 0.1, mz * 0.1, counter, true);
    }
}
