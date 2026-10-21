package thaumcraft.common.lib.golems;

/**
 * The pure rules of the Fill, Empty and Sorting routes of the golem.
 *
 * <p>TC4 4.2.3.5 oracle: {@code thaumcraft/common/entities/ai/inventory/AIHomePlace.java:35-39},
 * {@code …/AIHomeDrop.java:37-47}, {@code …/AIFillTake.java:38-41}, {@code …/AIFillGoto.java:52-145},
 * {@code …/AIEmptyPlace.java:31-33}, {@code …/AIEmptyDrop.java:20-34}, {@code …/AIEmptyGoto.java:39-110},
 * {@code …/AISortingPlace.java:31-33}, {@code …/AISortingGoto.java:39-104},
 * {@code …/AIHomeTake.java:27-31}, {@code …/AIHomeTakeSorting.java},
 * {@code thaumcraft/common/entities/golems/GolemHelper.java:49,66-76} and
 * {@code thaumcraft/common/config/Config.java:879-880}. The class is deliberately world-free, so the
 * goals and the tests only have to call it.</p>
 */
public final class GolemRouteRules {
    private GolemRouteRules() {}

    /**
     * Original {@code Config.golemDelay} — the cadence of the decisions of the routes.
     *
     * <p>The original's default is five and its minimum is one
     * ({@code oracle thaumcraft/common/config/Config.java:425-430,879}); the goals that use it are
     * the replace, the place, the take and the goto goals — <b>not</b> the drop goal
     * ({@code AIHomeDrop.func_75250_a}:37-39 has no delay gate).</p>
     */
    public static final int DECISION_DELAY = 5;

    /** Whether this tick is a decision tick of the original's routes. */
    public static boolean decisionTick(int tickCount) { return tickCount % DECISION_DELAY == 0; }

    /**
     * Original {@code AIHomePlace.func_75250_a}:37 and the other home goals — the <b>squared</b>
     * distance of the golem from its home that still counts as "at the home".
     */
    public static final double HOME_REACH_SQR = 5.0;

    /** Whether the squared distance of the golem from its home is inside the original's reach. */
    public static boolean atHome(double homeDistanceSqr) { return homeDistanceSqr <= HOME_REACH_SQR; }

    /**
     * Original {@code GolemHelper.ADJACENT_RANGE} — the <b>squared</b> distance of a marked block
     * that still counts as "next to the golem".
     */
    public static final double ADJACENT_SQR = 4.0;

    /** Whether the squared distance of a marked block is inside the original's adjacency. */
    public static boolean adjacent(double markerDistanceSqr) { return markerDistanceSqr < ADJACENT_SQR; }

    /**
     * Original {@code AIFillGoto.count} and the count of the other route goals — the budget of a
     * route in ticks.
     */
    public static final int ROUTE_BUDGET = 200;

    /**
     * Original {@code AIFillGoto.func_75246_d}:128 — the budget of the random detour that follows
     * an exhausted route.
     */
    public static final int DETOUR_BUDGET = 20;

    /** Original {@code AIFillGoto.func_75246_d}:127 — the horizontal reach of the random detour. */
    public static final int DETOUR_XZ = 2;

    /** Original {@code AIFillGoto.func_75246_d}:127 — the vertical reach of the random detour. */
    public static final int DETOUR_Y = 1;

    /**
     * Original {@code countChest} of the place and the take goals — the ticks the chest of the
     * golem stays open after a transfer.
     */
    public static final int CHEST_WINDOW = 5;

    /**
     * Original {@code Config.golemIgnoreDelay} — the wall clock time in milliseconds an item that
     * has no destination is ignored.
     */
    public static final long IGNORE_DELAY_MS = 10_000L;
}
