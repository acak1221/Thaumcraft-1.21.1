package thaumcraft.research;

import org.junit.jupiter.api.Test;
import thaumcraft.common.lib.research.ScribingInk;
import thaumcraft.common.lib.research.ScribingInk.Plan;
import thaumcraft.common.lib.research.ScribingInk.State;

import java.util.ArrayList;
import java.util.List;
import java.util.function.IntUnaryOperator;

import static org.junit.jupiter.api.Assertions.*;

/** Pure plans; native inventory and Player behavior has dedicated GameTests. */
class ScribingInkTest {
    private static final IntUnaryOperator NO_RANDOM = bound -> {
        fail("This ink branch must not draw randomness (requested bound " + bound + ")");
        return 0;
    };

    @Test void markerAndStrictRemainingDurabilityAreThePreflight() {
        for (State state : List.of(new State(false, 0, 100, true),
                new State(true, 100, 100, true), new State(true, 101, 100, true),
                new State(true, 0, 0, false))) {
            assertFalse(state.usable());
            assertEquals(new Plan(false, state.damage(), state.damage()),
                    ScribingInk.planPlayer(state, false, 3, NO_RANDOM));
        }
    }

    @Test void creativeAcceptsUsableInkWithoutDamageOrRandomness() {
        State ink = new State(true, 99, 100, true);
        assertEquals(new Plan(true, 99, 99), ScribingInk.planPlayer(ink, true, 3, NO_RANDOM));
        assertEquals(99, ink.damage());
    }

    @Test void creativeStillCannotUseAnExhaustedOrUnmarkedStack() {
        assertEquals(new Plan(false, 100, 100),
                ScribingInk.planPlayer(new State(true, 100, 100, true), true, 3, NO_RANDOM));
        assertEquals(new Plan(false, 0, 0),
                ScribingInk.planPlayer(new State(false, 0, 100, true), true, 3, NO_RANDOM));
    }

    @Test void unbreakableInkIsAcceptedButCostsNoDamageOrRandomness() {
        State ink = new State(true, 37, 100, false);
        assertEquals(new Plan(true, 37, 37), ScribingInk.planPlayer(ink, false, 3, NO_RANDOM));
        assertTrue(ink.usable());
    }

    @Test void levelZeroSpendsOnePointWithoutAnyRandomDraw() {
        State ink = new State(true, 18, 100, true);
        Plan plan = ScribingInk.planPlayer(ink, false, 0, NO_RANDOM);
        assertEquals(new Plan(true, 18, 19), plan);
        assertTrue(plan.spendsPoint());
        assertEquals(18, ink.damage());
    }

    @Test void originalNegativeLevelConditionAlsoSpendsWithoutADraw() {
        assertEquals(new Plan(true, 18, 19),
                ScribingInk.planPlayer(new State(true, 18, 100, true), false, -1, NO_RANDOM));
    }

    @Test void lastPointReachesMaximumAndTheNextConsumptionIsRefusedWithoutADraw() {
        State ink = new State(true, 99, 100, true);
        Plan last = ScribingInk.planPlayer(ink, false, 0, NO_RANDOM);
        assertEquals(new Plan(true, 99, 100), last);
        // Plan changes only damage; the native adapter must retain stack count.
        State empty = new State(true, last.afterDamage(), 100, true);
        assertEquals(new Plan(false, 100, 100), ScribingInk.planPlayer(empty, false, 3, NO_RANDOM));
        assertEquals(99, ink.damage());
    }

    @Test void zeroRollSpendsExactlyOneWithOneLevelPlusOneBoundedDraw() {
        for (int level : List.of(1, 2, 3, 255)) {
            List<Integer> calls = new ArrayList<>();
            Plan plan = ScribingInk.planPlayer(new State(true, 44, 100, true), false, level, bound -> {
                calls.add(bound);
                return 0;
            });
            assertEquals(List.of(level + 1), calls);
            assertEquals(new Plan(true, 44, 45), plan);
        }
    }

    @Test void everyNonzeroRollWithinTheBoundPreventsDamageWithExactlyOneDraw() {
        for (int roll = 1; roll < 4; roll++) {
            List<Integer> calls = new ArrayList<>();
            int outcome = roll;
            Plan plan = ScribingInk.planPlayer(new State(true, 99, 100, true), false, 3, bound -> {
                calls.add(bound);
                return outcome;
            });
            assertEquals(List.of(4), calls);
            assertEquals(new Plan(true, 99, 99), plan);
            assertFalse(plan.spendsPoint());
        }
    }

    @Test void adversarialRandomCannotReturnNegativeOrTheExclusiveUpperBound() {
        State ink = new State(true, 12, 100, true);
        assertThrows(IllegalArgumentException.class, () -> ScribingInk.planPlayer(ink, false, 3, bound -> -1));
        assertThrows(IllegalArgumentException.class, () -> ScribingInk.planPlayer(ink, false, 3, bound -> bound));
        assertEquals(12, ink.damage());
    }

    @Test void boundOverflowIsRejectedBeforeCallingRandomButTheLargestValidBoundWorks() {
        State ink = new State(true, 12, 100, true);
        assertThrows(IllegalArgumentException.class,
                () -> ScribingInk.planPlayer(ink, false, Integer.MAX_VALUE, NO_RANDOM));
        List<Integer> calls = new ArrayList<>();
        assertEquals(new Plan(true, 12, 13),
                ScribingInk.planPlayer(ink, false, Integer.MAX_VALUE - 1, bound -> {
                    calls.add(bound);
                    return 0;
                }));
        assertEquals(List.of(Integer.MAX_VALUE), calls);
    }

    @Test void unusedRandomAndEnchantLevelAreNotValidatedAfterRefusalOrNoDamageBranches() {
        assertEquals(new Plan(false, 100, 100), ScribingInk.planPlayer(
                new State(true, 100, 100, true), false, Integer.MAX_VALUE, null));
        assertEquals(new Plan(true, 12, 12), ScribingInk.planPlayer(
                new State(true, 12, 100, true), true, Integer.MAX_VALUE, null));
        assertEquals(new Plan(true, 12, 12), ScribingInk.planPlayer(
                new State(true, 12, 100, false), false, Integer.MAX_VALUE, null));
        assertEquals(new Plan(true, 12, 13), ScribingInk.planPlayer(
                new State(true, 12, 100, true), false, 0, null));
    }

    @Test void failingRandomPropagatesWithoutChangingTheInputState() {
        State ink = new State(true, 12, 100, true);
        IllegalStateException failure = new IllegalStateException("RNG unavailable");
        assertSame(failure, assertThrows(IllegalStateException.class,
                () -> ScribingInk.planPlayer(ink, false, 3, bound -> { throw failure; })));
        assertEquals(new State(true, 12, 100, true), ink);
    }

    @Test void tableAlwaysIncrementsUsableInkEvenWhenPlayerDamageabilityWouldPreventIt() {
        State unbreakable = new State(true, 99, 100, false);
        assertEquals(new Plan(true, 99, 100), ScribingInk.planTable(unbreakable));
        assertEquals(new Plan(false, 100, 100), ScribingInk.planTable(new State(true, 100, 100, false)));
        assertEquals(new Plan(false, 0, 0), ScribingInk.planTable(new State(false, 0, 100, true)));
        assertEquals(99, unbreakable.damage());
    }

    @Test void maximumIntDurabilityDoesNotOverflowAndNegativeNativeStatesAreRejected() {
        assertEquals(new Plan(true, Integer.MAX_VALUE - 1, Integer.MAX_VALUE), ScribingInk.planPlayer(
                new State(true, Integer.MAX_VALUE - 1, Integer.MAX_VALUE, true), false, 0, NO_RANDOM));
        assertThrows(IllegalArgumentException.class, () -> new State(true, -1, 100, true));
        assertThrows(IllegalArgumentException.class, () -> new State(true, 0, -1, true));
    }
}
