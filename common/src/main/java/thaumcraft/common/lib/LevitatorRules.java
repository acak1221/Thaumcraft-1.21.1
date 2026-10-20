package thaumcraft.common.lib;

/** The numbers of the Arcane Levitator (original TileLifter) without a world; the float constants are widened as the original widened them. */
public final class LevitatorRules {
    /** A levitator lifts up to 10 blocks, and 10 more for every unpowered levitator stacked directly under it. */
    public static final int BASE_RANGE = 10, RANGE_PER_LEVITATOR = 10;
    /** The range is worked out again when the block asks for it and on every hundredth tick. */
    public static final int RECHECK_TICKS = 100;
    /** An entity rises by 0.1 a tick while it is slower than 0.35 upward; one that sneaks keeps 90 % of its fall instead. */
    public static final double LIFT_CEILING = 0.35F, LIFT_STEP = 0.1F, SNEAK_DAMPING = 0.9F;

    private LevitatorRules() {}

    public static int maxRange(int unpoweredBelow) { return BASE_RANGE + RANGE_PER_LEVITATOR * unpoweredBelow; }

    /** The vertical speed after one tick of lift (or, for a sneaker, of slowing a fall). */
    public static double lifted(double vertical, boolean sneaking) {
        if (sneaking) return vertical < 0.0 ? vertical * SNEAK_DAMPING : vertical;
        return vertical < LIFT_CEILING ? vertical + LIFT_STEP : vertical;
    }

    /** Dropped items, anything that can be pushed (creatures, players, boats) and horses are lifted. */
    public static boolean lifts(boolean item, boolean pushable, boolean horse) { return item || pushable || horse; }
}
