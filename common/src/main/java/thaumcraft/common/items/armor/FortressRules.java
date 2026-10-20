package thaumcraft.common.items.armor;

/** The numbers of the Thaumium Fortress armor (original ItemFortressArmor, ConfigRecipes:360-370, EventHandlerRunic.entityHurt) without a world. */
public final class FortressRules {
    /** The original's set multiplier starts below 1 and each worn piece (legs, chest, helm; not boots) lifts it, a mask a little more. */
    public static final double SET_BASE = 0.875, SET_PER_PIECE = 0.125, SET_PER_MASK = 0.05;
    /** The original durability factor of armorMatThaumiumFortress. */
    public static final int DURABILITY_FACTOR = 40;
    /** Angry Ghost: a blow of d points withers its dealer with chance d / 10, for 80 ticks. Sipping Fiend: a blow of d heals one point with chance d / 12. */
    public static final float GHOST_DIVISOR = 10F, FIEND_DIVISOR = 12F;
    public static final int WITHER_TICKS = 80;
    public static final int MASK_DEVIL = 0, MASK_GHOST = 1, MASK_FIEND = 2;

    private FortressRules() {}

    /**
     * Original getProperties loop {@code for a = 1..3}: the slots in the order legs, chest, helm, each worn Fortress piece adds 0.125 and
     * one carrying a mask 0.05 more. The doubles are added in that order, as the original added them.
     */
    public static double set(boolean[] worn, boolean[] masked) {
        double set = SET_BASE;
        for (int slot = 0; slot < worn.length; slot++) {
            if (!worn[slot]) continue;
            set += SET_PER_PIECE;
            if (masked[slot]) set += SET_PER_MASK;
        }
        return set;
    }

    /** Whether a blow of that size triggers a mask whose chance is {@code damage / divisor} for a roll in [0, 1). */
    public static boolean triggers(float roll, float damage, float divisor) { return roll < damage / divisor; }
}
