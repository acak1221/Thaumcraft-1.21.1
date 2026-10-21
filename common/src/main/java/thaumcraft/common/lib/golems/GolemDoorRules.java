package thaumcraft.common.lib.golems;

import net.minecraft.core.Direction;

/**
 * The pure rules of the door goal of the golem.
 *
 * <p>TC4 4.2.3.5 oracle, {@code thaumcraft/common/entities/ai/misc/AIDoorInteract.java:24-95} and
 * {@code …/AIOpenDoor.java:117-171}. The class is deliberately world-free, so the goal and the tests
 * only have to call it.</p>
 */
public final class GolemDoorRules {
    private GolemDoorRules() {}

    /**
     * Original {@code AIDoorInteract.func_75250_a}:47 — the scan of the points of the current path.
     *
     * <p>The loop of the original runs from zero to {@code Math.min(currentPathIndex + 2, pathLength)},
     * so the golem looks at the points it is about to reach and at the two points behind them.</p>
     *
     * @param currentPathIndex the index of the point the golem is standing on
     * @param pathLength       the number of points of the path
     */
    public static int scanLimit(int currentPathIndex, int pathLength) {
        return Math.min(currentPathIndex + 2, pathLength);
    }

    /**
     * Original {@code AIDoorInteract.func_75250_a}:52 — the squared distance of a path point from
     * the golem that still counts as "the door the golem is blocked by".
     */
    public static double reachSqr() { return 2.25; }

    /** Whether the squared distance of a path point is inside the original's reach. */
    public static boolean withinReach(double distanceSqr) { return distanceSqr <= reachSqr(); }

    /**
     * Original {@code AIDoorInteract.count} — the counter of the interaction.
     *
     * <p>{@code AIDoorInteract.func_75250_a}:55 and :63 set it to 200 when the door was found, and
     * {@code func_75246_d}:82 lowers it by one every tick.</p>
     */
    public static int counter() { return 200; }

    /**
     * Original {@code AIOpenDoor.field_75360_j} — the ticks the door stays open.
     *
     * <p>{@code AIOpenDoor.func_75249_e}:135 sets it to twenty, {@code func_75246_d}:168 lowers it
     * and {@code func_75253_b}:130 ends the interaction when it is spent.</p>
     */
    public static int openTicks() { return 20; }

    /**
     * The direction order of the 1.7.10 fence gate metadata.
     *
     * <p>Original {@code AIOpenDoor.func_75249_e}:141-146 — the metadata of a gate holds its
     * direction in the two low bits (zero is south, one west, two north, three east) and the open
     * bit above them; the golem turns a gate that faces the other way before it opens it. The
     * 1.21.1 gate has a direction property instead of the metadata, so the same order is used to
     * compare the two directions.</p>
     */
    public static int gateIndex(Direction direction) {
        return switch (direction) {
            case SOUTH -> 0;
            case WEST -> 1;
            case NORTH -> 2;
            case EAST -> 3;
            default -> 0;
        };
    }
}
