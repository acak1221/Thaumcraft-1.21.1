package thaumcraft.common.entities.golems;

import java.util.function.IntUnaryOperator;

/**
 * Registry-free numbers of the Lumber core (original AIHarvestLogs). Kept apart so the
 * source arithmetic is testable without a world, entity or random source.
 */
public final class LumberGeometry {
    /** Golem work radius: the scan reaches {@code ceil(range / 3)}. Ordinary range 16 gives 6. */
    public static int radius(float range) { return (int) Math.ceil(range / 3.0F); }

    /** Number of random candidate cells tried per scan: {@code radius * 4} (24 at the baseline). */
    public static int attempts(int radius) { return (int) Math.ceil(radius * 4.0F); }

    /**
     * Work time for one cut, from the ANCHOR's hardness rather than any axe:
     * {@code (int) max(5, (20 - strength * 3) * hardness)}.
     */
    public static int delay(int strength, float hardness) {
        return (int) Math.max(5.0F, (20.0F - strength * 3.0F) * hardness);
    }

    /** Progress-pulse interval: {@code delay / round(delay / 6)}. The 5-tick floor keeps the divisor at least 1. */
    public static int pulseMod(int delay) {
        return delay / Math.max(1, Math.round(delay / 6.0F));
    }

    /**
     * One random scan cell: X and Z uniformly in home +-radius, Y in
     * {@code home + nextInt(1 + radius) - radius / 2} truncated toward zero. Draw order is
     * X, Y, Z, as in the original expression. {@code nextInt} receives the exclusive bound.
     */
    public static int[] candidate(int homeX, int homeY, int homeZ, int radius, IntUnaryOperator nextInt) {
        float r = radius;
        int x = (int) ((float) (homeX + nextInt.applyAsInt((int) (1.0F + r * 2.0F))) - r);
        int y = (int) ((float) (homeY + nextInt.applyAsInt((int) (1.0F + r))) - r / 2.0F);
        int z = (int) ((float) (homeZ + nextInt.applyAsInt((int) (1.0F + r * 2.0F))) - r);
        return new int[] {x, y, z};
    }

    /**
     * Whether a follow-up anchor stays inside the home box on every axis (original
     * checkAdjacent): {@code |home - cell| <= radius} for X, Y and Z.
     */
    public static boolean withinHomeBox(int homeX, int homeY, int homeZ, int radius, int x, int y, int z) {
        return Math.abs((long) homeX - x) <= radius && Math.abs((long) homeY - y) <= radius && Math.abs((long) homeZ - z) <= radius;
    }

    private LumberGeometry() {}
}
