package thaumcraft.common.lib.world;

/**
 * The stabilizer lock of an ordinary node and everything the lock changes.
 *
 * <p>Original {@code thaumcraft/common/tiles/TileNode.java}:
 * {@code checkLock():879-897} reads the block under the node; the node stabilizer (the original
 * {@code blockStoneDevice} metadata nine) gives the lock one and the advanced node stabilizer (the
 * original {@code blockStoneDevice} metadata ten) gives the lock two. {@code handleRecharge():644-650}
 * multiplies the regeneration interval by two for the lock one and by twenty for the lock two.
 * {@code handleNodeStability():775,781} divides the two repair rolls by the lock.</p>
 *
 * <p>The numbers are the ones of the original; the class holds no registry and no world state so
 * that the rules can be asserted without a bootstrap.</p>
 */
public final class NodeStabilizerRules {
    /** The lock of an ordinary node without a stabilizer. */
    public static final int NO_LOCK = 0;
    /** The lock of a node under the node stabilizer (the original {@code blockStoneDevice} metadata nine). */
    public static final int BASIC_LOCK = 1;
    /** The lock of a node under the advanced node stabilizer (the original {@code blockStoneDevice} metadata ten). */
    public static final int ADVANCED_LOCK = 2;
    /** Original {@code TileNode:278-290,628-642}: the regeneration interval of the node modifiers. */
    public static final int BASE_INTERVAL = 600, BRIGHT_INTERVAL = 400, PALE_INTERVAL = 900, FADING_INTERVAL = 0;
    /** Original {@code TileNode:775}: the UNSTABLE repair roll is {@code nextInt(10000 / lock)}. */
    public static final int STABILITY_BOUND = 10000, STABILITY_ROLL = 42;
    /** Original {@code TileNode:781}: the FADING repair roll is {@code nextInt(12500 / lock)}. */
    public static final int FADING_REPAIR_BOUND = 12500, FADING_REPAIR_ROLL = 69;
    /** Original {@code TileNode:648-650}: the lock two multiplies the regeneration interval by twenty. */
    public static final int ADVANCED_LOCK_INTERVAL_FACTOR = 20;
    /** Original {@code TileNode:644-646}: the lock one multiplies the regeneration interval by two. */
    public static final int BASIC_LOCK_INTERVAL_FACTOR = 2;

    private NodeStabilizerRules() {}

    /**
     * Original {@code TileNode:885-889}: the block under the node decides the lock.
     *
     * @param basic    the block under the node is the node stabilizer
     * @param advanced the block under the node is the advanced node stabilizer
     */
    public static int lock(boolean basic, boolean advanced) {
        if (advanced) return ADVANCED_LOCK;
        return basic ? BASIC_LOCK : NO_LOCK;
    }

    /**
     * Original {@code TileNode:628-651}: the regeneration interval of the node modifier, multiplied
     * by the lock. The advanced lock multiplies it by twenty, the ordinary lock by two.
     */
    public static int regenerationInterval(int modifierInterval, int lock) {
        if (lock == ADVANCED_LOCK) return modifierInterval * ADVANCED_LOCK_INTERVAL_FACTOR;
        if (lock == BASIC_LOCK) return modifierInterval * BASIC_LOCK_INTERVAL_FACTOR;
        return modifierInterval;
    }

    /**
     * Original {@code TileNode:775}: the bound of the UNSTABLE repair roll.
     *
     * <p>The original divides by the lock, so the advanced lock halves the bound (5000) and the
     * ordinary lock keeps it (10000).</p>
     */
    public static int stabilityBound(int lock) {
        if (lock <= NO_LOCK) return 0;
        return STABILITY_BOUND / lock;
    }

    /**
     * Original {@code TileNode:781}: the bound of the FADING repair roll.
     *
     * <p>The original divides by the lock, so the advanced lock halves the bound (6250) and the
     * ordinary lock keeps it (12500).</p>
     */
    public static int fadingRepairBound(int lock) {
        if (lock <= NO_LOCK) return 0;
        return FADING_REPAIR_BOUND / lock;
    }
}
