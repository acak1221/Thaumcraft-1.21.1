package thaumcraft.common.items.wands.foci;

/**
 * The numbers of the Portable Hole (original ItemFocusPortableHole, TileHole), without a world. Side indices are the 1.7 and
 * {@code Direction.get3DDataValue} ones: 0 down, 1 up, 2 north, 3 south, 4 west, 5 east; a hole runs from the clicked face INTO the block.
 */
public final class PortableHoleRules {
    /** Centivis of each of the two aspects (entropy and air) for one block of depth. */
    public static final int BASE_COST = 10;
    /** Original: 33 blocks, 8 more for each Enlarge. */
    public static final int REACH = 33, REACH_PER_ENLARGE = 8;
    /** Original: 120 ticks, 60 more for each Extend. */
    public static final int DURATION = 120, DURATION_PER_EXTEND = 60;
    public static final String OUTER_LANDS = "thaumcraft:outer_lands";
    private static final int[][] STEP = {{0, 1, 0}, {0, -1, 0}, {0, 0, 1}, {0, 0, -1}, {1, 0, 0}, {-1, 0, 0}};

    private PortableHoleRules() {}

    public static int maxDepth(int enlarge) { return REACH + REACH_PER_ENLARGE * enlarge; }

    public static int duration(int extend) { return DURATION + DURATION_PER_EXTEND * extend; }

    /**
     * Original: the base cost is merged (the larger kept, not added) with base * depth, so a depth of 0 still costs one block's worth.
     * Returns centivis for EACH of the two aspects, before the wand's own modifiers.
     */
    public static int cost(int depth) { return Math.max(BASE_COST, BASE_COST * depth); }

    /** The original's byte counter: the depth the focus measured, plus the clicked block itself. */
    public static int chainLength(int depth) { return depth + 1; }

    /** One block along the hole, INTO the clicked face (the original's ++jj for side 0 and so on). */
    public static int[] step(int side) {
        if (side < 0 || side > 5) throw new IllegalArgumentException("side " + side);
        return STEP[side].clone();
    }

    /**
     * The eight cells round a chain block, in the original loop order {@code a = 0..8} (the centre skipped): the layer across the
     * direction of the hole. Offsets from the block.
     */
    public static int[][] ring(int side) {
        if (side < 0 || side > 5) throw new IllegalArgumentException("side " + side);
        int[][] cells = new int[8][];
        int index = 0;
        for (int a = 0; a < 9; ++a) {
            if (a / 3 == 1 && a % 3 == 1) continue;
            int u = -1 + a / 3, v = -1 + a % 3;
            cells[index++] = switch (side) {
                case 0, 1 -> new int[]{u, 0, v};
                case 2, 3 -> new int[]{u, v, 0};
                default -> new int[]{0, u, v};
            };
        }
        return cells;
    }

    /** Original: only the chain's first tick (countdown 0) spawns the ring and the next block, and only while more than one block is left. */
    public static boolean spreads(int countdown, int count, int side) { return countdown == 0 && count > 1 && side != -1; }

    /** Original refusal: the Outer Lands (the dimension of the eldritch) never take a hole. */
    public static boolean refusedIn(String dimension) { return OUTER_LANDS.equals(dimension); }
}
