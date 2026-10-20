package thaumcraft.items;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import thaumcraft.common.lib.LevitatorRules;

/** The numbers of the Arcane Levitator (original TileLifter) without a world. */
class LevitatorRulesTest {
    @Test void theRangeIsTenAndTenMoreForEveryLevitatorUnderIt() {
        assertEquals(10, LevitatorRules.BASE_RANGE);
        assertEquals(10, LevitatorRules.RANGE_PER_LEVITATOR);
        assertEquals(100, LevitatorRules.RECHECK_TICKS);
        assertEquals(10, LevitatorRules.maxRange(0));
        assertEquals(20, LevitatorRules.maxRange(1));
        assertEquals(30, LevitatorRules.maxRange(2), "a stack of three lifts 30 blocks");
    }

    @Test void theFloatConstantsAreWidenedAsTheOriginalWidenedThem() {
        assertEquals((double) 0.35F, LevitatorRules.LIFT_CEILING);
        assertEquals((double) 0.1F, LevitatorRules.LIFT_STEP);
        assertEquals((double) 0.9F, LevitatorRules.SNEAK_DAMPING);
    }

    @Test void anEntityGainsTenthsUntilItRisesFasterThanTheCeiling() {
        assertEquals(0.1 * 1.0F, LevitatorRules.lifted(0.0, false), 1e-7, "from rest");
        assertEquals((double) 0.1F - 0.5, LevitatorRules.lifted(-0.5, false), 1e-12, "a fall is slowed by a tenth a tick");
        assertEquals(0.4F, LevitatorRules.lifted(0.3, false), 1e-6);
        assertEquals(0.35F + 0.1F, LevitatorRules.lifted(0.3499999, false), 1e-5, "just under the ceiling still gains");
        assertEquals((double) 0.35F, LevitatorRules.lifted((double) 0.35F, false), 0.0, "at the ceiling nothing is added");
        assertEquals(0.5, LevitatorRules.lifted(0.5, false), 0.0, "faster than the ceiling is left alone");
    }

    @Test void aSneakerKeepsNinetyPercentOfItsFallAndIsNotLifted() {
        assertEquals(-0.5 * (double) 0.9F, LevitatorRules.lifted(-0.5, true), 1e-12);
        assertEquals(0.0, LevitatorRules.lifted(0.0, true), 0.0, "at rest a sneaker is not lifted");
        assertEquals(0.2, LevitatorRules.lifted(0.2, true), 0.0, "rising is not slowed");
    }

    @Test void itemsPushableEntitiesAndHorsesAreLifted() {
        assertTrue(LevitatorRules.lifts(true, false, false));
        assertTrue(LevitatorRules.lifts(false, true, false));
        assertTrue(LevitatorRules.lifts(false, false, true));
        assertFalse(LevitatorRules.lifts(false, false, false), "a fixed thing such as an armor stand stays");
    }
}
