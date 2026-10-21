package thaumcraft.golems;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import net.minecraft.core.Direction;
import org.junit.jupiter.api.Test;
import thaumcraft.common.lib.golems.GolemDoorRules;

/**
 * The pure rules of the door goal of the original 4.2.3.5.
 *
 * <p>Oracle {@code thaumcraft/common/entities/ai/misc/AIDoorInteract.java:47} — the goal scans the
 * points of the current path from zero to {@code Math.min(currentPathIndex + 2, pathLength)}.</p>
 *
 * <p>Oracle {@code …/AIDoorInteract.java:52} — a path point counts as the door of the golem when its
 * squared distance is not greater than 2.25.</p>
 *
 * <p>Oracle {@code …/AIDoorInteract.java:55,63} and {@code …/AIOpenDoor.java:135} — the counter of
 * the interaction is two hundred and the door stays open for twenty ticks.</p>
 *
 * <p>Oracle {@code …/AIOpenDoor.java:141-146} — the direction of a fence gate is the 1.7.10
 * metadata order: south, west, north, east.</p>
 *
 * <p>These assertions are authored but NOT executed: the leader has to run {@code :common:test}.</p>
 */
class GolemDoorRulesTest {
    @Test void theScanOfThePathStopsAtTheCurrentIndexPlusTwo() {
        // Original AIDoorInteract.func_75250_a:47 —
        // for (int i = 0; i < Math.min(path.getCurrentPathIndex() + 2, path.getPathLength()); ++i)
        assertEquals(2, GolemDoorRules.scanLimit(0, 10), "the point the golem stands on and the next one");
        assertEquals(3, GolemDoorRules.scanLimit(1, 10), "the two points behind the current one are scanned as well");
        assertEquals(3, GolemDoorRules.scanLimit(1, 3), "the path is never longer than itself");
        assertEquals(10, GolemDoorRules.scanLimit(20, 10), "the whole path at most");
        assertEquals(0, GolemDoorRules.scanLimit(0, 0), "a golem that has no point of the path yet scans nothing");
    }

    @Test void theDoorOfTheGolemIsWithinTheSquaredDistanceOfTwoAndAQuarter() {
        // Original AIDoorInteract.func_75250_a:52.
        assertEquals(2.25, GolemDoorRules.reachSqr());
        assertTrue(GolemDoorRules.withinReach(0.0));
        assertTrue(GolemDoorRules.withinReach(2.25), "the original's test is 'not greater than 2.25'");
        assertFalse(GolemDoorRules.withinReach(2.26));
    }

    @Test void theDoorStaysOpenForTwentyTicksOfATwoHundredTickInteraction() {
        // Original AIDoorInteract.count and AIOpenDoor.field_75360_j.
        assertEquals(200, GolemDoorRules.counter());
        assertEquals(20, GolemDoorRules.openTicks());
    }

    @Test void theDirectionOfAFenceGateKeepsTheOriginalOrder() {
        // Original AIOpenDoor.func_75249_e:141 — the 1.7.10 metadata of a gate.
        assertEquals(0, GolemDoorRules.gateIndex(Direction.SOUTH));
        assertEquals(1, GolemDoorRules.gateIndex(Direction.WEST));
        assertEquals(2, GolemDoorRules.gateIndex(Direction.NORTH));
        assertEquals(3, GolemDoorRules.gateIndex(Direction.EAST));
        // A gate that faces the other way is turned before it is opened:
        // the original's test is 'the gate's direction is the opposite of the golem's'.
        assertEquals((GolemDoorRules.gateIndex(Direction.NORTH) + 2) % 4,
                GolemDoorRules.gateIndex(Direction.SOUTH));
    }
}
