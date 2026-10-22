package thaumcraft.common.lib.fx;

import net.minecraft.util.RandomSource;

/**
 * The two sparkle call sites of the original that belong to a block of the port.
 *
 * <p>The original draws the configurable sparkle through the proxy
 * ({@code thaumcraft/client/ClientProxy.java:648-655}):</p>
 *
 * <pre>{@code
 * public void sparkle(float x, float y, float z, float size, int color, float gravity) {
 *     if (this.getClientWorld() != null && this.getClientWorld().rand.nextInt(6) < this.particleCount(2)) {
 *         FXSparkle fx = new FXSparkle(this.getClientWorld(), x, y, z, size, color, 6);
 *         fx.noClip = true;
 *         fx.setGravity(gravity);
 *         ParticleEngine.instance.addEffect(this.getClientWorld(), fx);
 *     }
 * }
 * }</pre>
 *
 * <p>The rules of the two blocks are pure functions here, so that they can be asserted without a
 * block, a client or a particle.</p>
 */
public final class SparkleCallRules {
    @FunctionalInterface
    public interface ClientEffects {
        void sparkle(net.minecraft.world.level.Level level, double[] position, float size, int type, float gravity);
    }

    private static ClientEffects clientEffects = (level, position, size, type, gravity) -> {};

    public static void setClientEffects(ClientEffects effects) {
        clientEffects = java.util.Objects.requireNonNull(effects);
    }

    public static void sparkle(net.minecraft.world.level.Level level, double[] position, float size, int type, float gravity) {
        if (level.isClientSide) clientEffects.sparkle(level, position, size, type, gravity);
    }

    /** Oracle ClientProxy.java:649,899-903 - six outcomes, accepted below particleCount(2). */
    public static final int SPARKLE_ROLL = 6;
    /** Oracle ClientProxy.java:650 and FXSparkle.java:47 - multiplier six, life 3 * 6. */
    public static final int SPARKLE_MULTIPLIER = 6;

    /** Oracle BlockMagicalLeaves.java:214 - one of the leaves in five hundred sparkles. */
    public static final int LEAVES_ROLL = 500;
    /** Oracle BlockMagicalLeaves.java:215 - the type of the sparkle of the leaves. */
    public static final int LEAVES_TYPE = 7;
    /** Oracle BlockMagicalLeaves.java:215 - the size of the sparkle of the leaves. */
    public static final float LEAVES_SIZE = 2.0F;
    /** Oracle BlockMagicalLeaves.java:215 - the gravity of the sparkle of the leaves. */
    public static final float LEAVES_GRAVITY = 0.0F;

    /** Oracle BlockLifter.java:81 - the type of the sparkle of the lifter. */
    public static final int LIFTER_TYPE = 3;
    /** Oracle BlockLifter.java:81 - the size of the sparkle of the lifter. */
    public static final float LIFTER_SIZE = 1.0F;
    /** Oracle BlockLifter.java:81 - the gravity of the sparkle of the lifter. */
    public static final float LIFTER_GRAVITY = -0.3F;
    /** Oracle BlockLifter.java:81 - the base of the random offset of the sparkle of the lifter. */
    public static final float LIFTER_BASE = 0.2F;
    /** Oracle BlockLifter.java:81 - the spread of the random offset of the sparkle of the lifter. */
    public static final float LIFTER_SPREAD = 0.6F;

    private SparkleCallRules() {}

    /**
     * The condition of the sparkle of the magical leaves.
     *
     * <p>Oracle {@code BlockMagicalLeaves.java:214}: only the leaves whose metadata bit zero is
     * set (the silverwood leaves) sparkle, and only in one of five hundred random display ticks.
     * In 1.21.1 the two leaves are two blocks, so the metadata bit zero is the silverwood flag of
     * the block.</p>
     *
     * @param silverwood the silverwood leaves of the original
     * @param roll       the roll of the random display tick, {@code random.nextInt(500)}
     */
    public static boolean leavesSparkle(boolean silverwood, int roll) {
        return silverwood && roll == 0;
    }

    /**
     * The position of the sparkle of the magical leaves.
     *
     * <p>Oracle {@code BlockMagicalLeaves.java:215}: the block centre plus a random fraction of
     * the difference of two world-random fractions on each axis: offsets range from
     * just above -0.5 to just below 1.5, so the sparkle can lie outside the block.</p>
     *
     * @param x      the x of the block
     * @param y      the y of the block
     * @param z      the z of the block
     * @param random the world random of the original
     * @return the three coordinates of the sparkle
     */
    public static double[] leavesPosition(int x, int y, int z, RandomSource random) {
        return new double[]{
                x + 0.5F + random.nextFloat() - random.nextFloat(),
                y + 0.5F + random.nextFloat() - random.nextFloat(),
                z + 0.5F + random.nextFloat() - random.nextFloat()
        };
    }

    /**
     * The condition of the sparkle of the lifter.
     *
     * <p>Oracle {@code BlockLifter.java:80}: the levitator sparkles when it is not powered and
     * when it lifts something above itself.</p>
     *
     * @param gettingPower the power state of the tile of the original
     * @param rangeAbove   the number of blocks the lifter lifts above itself
     */
    public static boolean lifterSparkle(boolean gettingPower, int rangeAbove) {
        return !gettingPower && rangeAbove > 0;
    }

    /**
     * The position of the sparkle of the lifter.
     *
     * <p>Oracle {@code BlockLifter.java:81}: the sparkle is one block above the levitator and
     * wanders between twenty and eighty per cent of the block on each horizontal axis.</p>
     *
     * @param x      the x of the block
     * @param y      the y of the block
     * @param z      the z of the block
     * @param random the random display tick random of the original
     * @return the three coordinates of the sparkle
     */
    public static double[] lifterPosition(int x, int y, int z, RandomSource random) {
        return new double[]{
                x + LIFTER_BASE + random.nextFloat() * LIFTER_SPREAD,
                y + 1,
                z + LIFTER_BASE + random.nextFloat() * LIFTER_SPREAD
        };
    }
}
