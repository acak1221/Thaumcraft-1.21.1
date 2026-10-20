package thaumcraft.common.lib;

/** The original lamp's triangular offsets, height cap and darkness threshold. */
public final class ArcaneLampRules {
    public static final int DRAW_BOUND = 16, REMOVE_RADIUS = 15, LIGHT = 15, DARKNESS_LIMIT = 9;
    private ArcaneLampRules() {}

    /** Old minY 0 meant five; the equivalent floor in a modern negative-height world is minY + five. */
    public static int candidateY(int sampled, int columnHeight, int minY) {
        return Math.max(minY + 5, Math.min(sampled, columnHeight + 4));
    }
    public static boolean darkEnough(int rawLight) { return rawLight < DARKNESS_LIMIT; }
}
