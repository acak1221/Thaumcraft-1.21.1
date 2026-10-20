package thaumcraft.golems;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import thaumcraft.common.items.golems.GolemDartRules;

/** The numbers of the golem dart launcher (original AIDartAttack and attackEntityWithRangedAttack). */
class GolemDartRulesTest {
    @Test void theIntervalIsThirtyTicksAndEightFewerForEveryAirUpgrade() {
        assertEquals(30, GolemDartRules.interval(0));
        assertEquals(22, GolemDartRules.interval(1));
        assertEquals(14, GolemDartRules.interval(2));
        assertEquals(6, GolemDartRules.interval(3));
    }

    @Test void fourAirUpgradesAreNotClampedAsInTheOriginal() {
        // An advanced golem could carry four; the original then waits zero or fewer ticks and fires on every tick.
        assertEquals(-2, GolemDartRules.interval(4));
        int timer = GolemDartRules.interval(4);
        for (int tick = 0; tick < 5; tick++) {
            timer = GolemDartRules.wait(timer);
            assertEquals(0, timer, "the wait never goes below zero, so the shot is due on every tick");
            timer = GolemDartRules.interval(4);
        }
    }

    @Test void theWaitStartsAtHalfAnIntervalAndCountsDownToZero() {
        assertEquals(15, GolemDartRules.firstDelay(30));
        assertEquals(7, GolemDartRules.firstDelay(14), "Java integer division");
        assertEquals(3, GolemDartRules.firstDelay(6));
        assertEquals(-1, GolemDartRules.firstDelay(-2));
        assertEquals(0, GolemDartRules.firstDelay(1));
        assertEquals(4, GolemDartRules.wait(5));
        assertEquals(0, GolemDartRules.wait(1));
        assertEquals(0, GolemDartRules.wait(0));
        assertEquals(0, GolemDartRules.wait(-3));
    }

    @Test void theSpreadNarrowsByOneAndThreeQuartersForEveryWaterUpgrade() {
        assertEquals(7.0F, GolemDartRules.inaccuracy(0), 0F);
        assertEquals(5.25F, GolemDartRules.inaccuracy(1), 0F);
        assertEquals(3.5F, GolemDartRules.inaccuracy(2), 0F);
        assertEquals(0.0F, GolemDartRules.inaccuracy(4), 0F);
        assertEquals(-1.75F, GolemDartRules.inaccuracy(5), 0F, "no clamping: the original spread simply changes sign");
    }

    @Test void theDartDoesFourTenthsOfTheAttackAsAFloatProduct() {
        assertEquals((double) (6F * 0.4F), GolemDartRules.damage(6F), 0D);
        assertEquals((double) (8F * 0.4F), GolemDartRules.damage(8F), 0D);
        assertEquals((double) (2.5F * 0.4F), GolemDartRules.damage(2.5F), 0D);
        assertTrue(GolemDartRules.damage(6F) != 2.4D, "the float product 2.4000000953... is what the arrow receives");
        assertEquals(1.6F, GolemDartRules.SPEED, 0F);
    }

    @Test void aTargetNearerThanThreeBlocksIsLeftToTheMeleeGoal() {
        assertTrue(GolemDartRules.tooClose(8.99));
        assertFalse(GolemDartRules.tooClose(9.0), "exactly three blocks is still shot at");
        assertFalse(GolemDartRules.tooClose(100.0));
        assertTrue(GolemDartRules.tooClose(0.0));
    }

    @Test void aShotNeedsTheTargetWithinEightyPercentOfTheRange() {
        float reach = 16F * 0.8F; // 12.8
        assertTrue(GolemDartRules.inReach((double) (reach * reach), 16F), "exactly at the reach");
        assertTrue(GolemDartRules.inReach(100.0, 16F));
        assertFalse(GolemDartRules.inReach((double) (reach * reach) + 0.01, 16F));
        assertFalse(GolemDartRules.inReach(169.0, 16F));
        // Water upgrades widen it with the range (16 + 4 per upgrade).
        assertTrue(GolemDartRules.inReach(256.0, 20F), "16 blocks away needs 80% of 20");
        assertFalse(GolemDartRules.inReach(256.0, 16F));
        // The square is taken in float, then widened: not the exact double product.
        float awkward = 17.6F * GolemDartRules.REACH_FACTOR;
        awkward *= awkward;
        assertTrue(GolemDartRules.inReach((double) awkward, 17.6F));
    }
}
