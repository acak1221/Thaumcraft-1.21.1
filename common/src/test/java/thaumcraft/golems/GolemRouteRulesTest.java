package thaumcraft.golems;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import thaumcraft.common.lib.golems.GolemRouteRules;

/**
 * The pure rules of the Fill, Empty and Sorting routes of the original 4.2.3.5.
 *
 * <p>Oracle {@code thaumcraft/common/config/Config.java:425-430,879-880} — the decision cadence
 * of the routes is five ticks ({@code golemDelay}).</p>
 *
 * <p>Oracle {@code thaumcraft/common/entities/ai/inventory/AIHomePlace.java:35-39},
 * {@code …/AIHomeTake.java:27-31} — the home of the golem is reached within a <b>squared</b>
 * distance of five.</p>
 *
 * <p>Oracle {@code thaumcraft/common/entities/golems/GolemHelper.java:49,66-76} — a marked block is
 * adjacent to the golem within a <b>squared</b> distance of four.</p>
 *
 * <p>Oracle {@code thaumcraft/common/entities/ai/inventory/AIFillGoto.java:128,140} — the route has a
 * budget of two hundred ticks and the random detour that follows it a budget of twenty, inside a
 * reach of two and one.</p>
 *
 * <p>Oracle {@code thaumcraft/common/entities/ai/inventory/AIHomePlace.java:109} — the chest of the
 * golem stays open for five ticks after a transfer.</p>
 *
 * <p>These assertions are authored but NOT executed: the leader has to run {@code :common:test}.</p>
 */
class GolemRouteRulesTest {
    @Test void theDecisionCadenceOfTheRoutesIsFiveTicks() {
        // Original Config.golemDelay:879 — the default of the original is five.
        assertEquals(5, GolemRouteRules.DECISION_DELAY);
        assertTrue(GolemRouteRules.decisionTick(0));
        assertTrue(GolemRouteRules.decisionTick(5));
        assertTrue(GolemRouteRules.decisionTick(200));
        assertFalse(GolemRouteRules.decisionTick(1));
        assertFalse(GolemRouteRules.decisionTick(4));
        assertFalse(GolemRouteRules.decisionTick(201));
    }

    @Test void theHomeOfTheGolemIsReachedWithinTheSquaredDistanceOfFive() {
        // Original AIHomePlace.func_75250_a:37 — the squared distance of the home.
        assertEquals(5.0, GolemRouteRules.HOME_REACH_SQR);
        assertTrue(GolemRouteRules.atHome(0.0));
        assertTrue(GolemRouteRules.atHome(4.9));
        assertTrue(GolemRouteRules.atHome(5.0), "the original's test is 'not greater than five'");
        assertFalse(GolemRouteRules.atHome(5.1));
    }

    @Test void aMarkedBlockIsAdjacentWithinTheSquaredDistanceOfFour() {
        // Original GolemHelper.ADJACENT_RANGE and getMarkedContainersAdjacentToGolem:70.
        assertEquals(4.0, GolemRouteRules.ADJACENT_SQR);
        assertTrue(GolemRouteRules.adjacent(0.0));
        assertTrue(GolemRouteRules.adjacent(3.99));
        assertFalse(GolemRouteRules.adjacent(4.0), "the original's test is 'less than four'");
        assertFalse(GolemRouteRules.adjacent(4.01));
    }

    @Test void theRouteAndItsDetourHaveTheOriginalBudgets() {
        // Original AIFillGoto.func_75249_e:140 and func_75246_d:128.
        assertEquals(200, GolemRouteRules.ROUTE_BUDGET);
        assertEquals(20, GolemRouteRules.DETOUR_BUDGET);
        // Original AIFillGoto.func_75246_d:127 — RandomPositionGenerator with (2, 1).
        assertEquals(2, GolemRouteRules.DETOUR_XZ);
        assertEquals(1, GolemRouteRules.DETOUR_Y);
    }

    @Test void theChestOfTheGolemStaysOpenForFiveTicks() {
        // Original countChest of the place and the take goals.
        assertEquals(5, GolemRouteRules.CHEST_WINDOW);
    }

    @Test void anItemWithoutADestinationIsIgnoredForTenSeconds() {
        // Original Config.golemIgnoreDelay:880 — the wall clock time in milliseconds.
        assertEquals(10_000L, GolemRouteRules.IGNORE_DELAY_MS);
    }
}
