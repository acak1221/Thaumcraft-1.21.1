package thaumic.tinkerer.enchantments;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.UUID;
import org.junit.jupiter.api.Test;
import thaumic.tinkerer.common.enchantment.AttackBoost;
import thaumic.tinkerer.common.enchantment.EnchantmentFormulas;
import thaumic.tinkerer.common.enchantment.EnchantmentFormulas.Attack;

/**
 * Test-only code of the port (no 1.7.10 counterpart), CC BY-NC-SA 3.0 like the add-on.
 *
 * <p>The numbers of the fourteen enchantments against hand-worked results of the 1.7.10 handler ({@code
 * ModEnchantmentHandler}): the expected values below were computed from the original statements, not from the port.</p>
 */
class EnchantmentFormulasTest {
    private static final float EPSILON = 1.0E-4F;

    private static Attack attack(int pounce, boolean inAir, int finalStrike, int roll, int valiance, float health, int focused,
            int dispersed, int successive) {
        return new Attack(pounce, inAir, finalStrike, roll, valiance, health, 20F, focused, dispersed, successive);
    }

    private static Attack plain() {
        return attack(0, false, 0, 0, 0, 20F, 0, 0, 0);
    }

    // ---- damage ------------------------------------------------------------------------------------------------

    @Test
    void anAttackWithoutEnchantmentsIsNotChanged() {
        assertEquals(10F, EnchantmentFormulas.modifyDamage(10F, plain()), EPSILON);
        assertEquals(1F, EnchantmentFormulas.damageFactor(plain()), EPSILON);
    }

    @Test
    void pounceAddsAQuarterPerLevelWhileTheBlockBelowIsAir() {
        assertEquals(15F, EnchantmentFormulas.modifyDamage(10F, attack(2, true, 0, 0, 0, 20F, 0, 0, 0)), EPSILON);
        assertEquals(12.5F, EnchantmentFormulas.modifyDamage(10F, attack(1, true, 0, 0, 0, 20F, 0, 0, 0)), EPSILON);
        assertEquals(10F, EnchantmentFormulas.modifyDamage(10F, attack(5, false, 0, 0, 0, 20F, 0, 0, 0)), EPSILON);
    }

    @Test
    void finalStrikeTriplesTheDamageWhenTheDieIsBelowTheLevel() {
        // Random.nextInt(19) gives 0..18; "finalStrike > roll": level 3 triples on 0, 1 and 2.
        for (int roll = 0; roll < 19; roll++) {
            float expected = roll < 3 ? 30F : 10F;
            assertEquals(expected, EnchantmentFormulas.modifyDamage(10F, attack(0, false, 3, roll, 0, 20F, 0, 0, 0)), EPSILON,
                    "roll " + roll);
        }
    }

    @Test
    void valianceAddsATenthPerLevelBelowHalfHealth() {
        assertEquals(12F, EnchantmentFormulas.modifyDamage(10F, attack(0, false, 0, 0, 2, 9F, 0, 0, 0)), EPSILON);
        // exactly half of 20 is not "below": 10 / 20 < .5F is false
        assertEquals(10F, EnchantmentFormulas.modifyDamage(10F, attack(0, false, 0, 0, 2, 10F, 0, 0, 0)), EPSILON);
        assertEquals(10F, EnchantmentFormulas.modifyDamage(10F, attack(0, false, 0, 0, 2, 20F, 0, 0, 0)), EPSILON);
    }

    @Test
    void focusedStrikesHalveTheDamageAndGrowItWithTheCounterWhichStartsAtFive() {
        // 10 / 2 = 5; 5 + .5 * max(s, 5) * 5 * level
        assertEquals(17.5F, EnchantmentFormulas.modifyDamage(10F, attack(0, false, 0, 0, 0, 20F, 1, 0, 0)), EPSILON);
        assertEquals(30F, EnchantmentFormulas.modifyDamage(10F, attack(0, false, 0, 0, 0, 20F, 2, 0, 0)), EPSILON);
        assertEquals(30F, EnchantmentFormulas.modifyDamage(10F, attack(0, false, 0, 0, 0, 20F, 2, 0, 5)), EPSILON);
        // the counter is not capped (Math.max where the author meant Math.min): 7 > 5
        assertEquals(22.5F, EnchantmentFormulas.modifyDamage(10F, attack(0, false, 0, 0, 0, 20F, 1, 0, 7)), EPSILON);
    }

    @Test
    void dispersedStrikesBoostTheFirstHitAndCutTheNextOnes() {
        // 10 * (1 + .6 * 1 + 0) / (1 + .25 * 1 + 0)
        assertEquals(12.8F, EnchantmentFormulas.modifyDamage(10F, attack(0, false, 0, 0, 0, 20F, 0, 1, 0)), EPSILON);
        // 10 * (1 + 1.2 + 5 / 5) / (1 + .5 + 5 * 2.5)
        assertEquals(32F / 14F, EnchantmentFormulas.modifyDamage(10F, attack(0, false, 0, 0, 0, 20F, 0, 2, 5)), EPSILON);
        // the integer division: 12 / 5 = 2
        assertEquals(10F * (1F + .6F + 2F) / (1F + .25F + 30F),
                EnchantmentFormulas.modifyDamage(10F, attack(0, false, 0, 0, 0, 20F, 0, 1, 12)), EPSILON);
    }

    @Test
    void theEnchantmentsMultiplyInTheOrderOfTheOriginalAndTheFactorIsTheSameForEveryDamage() {
        Attack all = attack(1, true, 2, 0, 1, 5F, 1, 0, 0);
        // 10 * 1.25 * 3 * 1.1 = 41.25, then focused: 20.625 + .5 * 5 * 20.625
        float expected = 10F * 1.25F * 3F * 1.1F;
        expected = expected / 2;
        expected += .5F * 5 * expected;
        assertEquals(expected, EnchantmentFormulas.modifyDamage(10F, all), 0.001F);
        assertEquals(EnchantmentFormulas.damageFactor(all) * 4F, EnchantmentFormulas.modifyDamage(4F, all), 0.001F);
    }

    @Test
    void theStrikeCounterResetsOnANewTargetAndJumpsToFiveOnTheSecondHit() {
        UUID none = new UUID(0, 0);
        UUID pig = UUID.randomUUID();
        UUID cow = UUID.randomUUID();
        assertEquals(0, EnchantmentFormulas.nextSuccessiveStrikes(none, 0, pig));
        assertEquals(5, EnchantmentFormulas.nextSuccessiveStrikes(pig, 0, pig));
        assertEquals(6, EnchantmentFormulas.nextSuccessiveStrikes(pig, 5, pig));
        assertEquals(7, EnchantmentFormulas.nextSuccessiveStrikes(pig, 6, pig));
        assertEquals(0, EnchantmentFormulas.nextSuccessiveStrikes(pig, 9, cow));
    }

    @Test
    void vampirismHealsByItsLevel() {
        assertEquals(1F, EnchantmentFormulas.vampirismHeal(1), EPSILON);
        assertEquals(2F, EnchantmentFormulas.vampirismHeal(2), EPSILON);
    }

    // ---- movement -------------------------------------------------------------------------------------------------

    @Test
    void ascentBoostMultipliesTheJumpByLevelPlusTwoOverTwo() {
        assertEquals(1.5D, EnchantmentFormulas.ascentBoostFactor(1), 1e-9);
        assertEquals(2.0D, EnchantmentFormulas.ascentBoostFactor(2), 1e-9);
        assertEquals(2.5D, EnchantmentFormulas.ascentBoostFactor(3), 1e-9);
        assertEquals(3.0D, EnchantmentFormulas.ascentBoostFactor(4), 1e-9);
    }

    @Test
    void slowFallActsOnAFallPastTheFirstTickAndTwoPointNineBlocks() {
        double falling = -0.5;
        assertTrue(EnchantmentFormulas.slowFallActs(1, false, falling, 3.5F));
        assertFalse(EnchantmentFormulas.slowFallActs(0, false, falling, 3.5F), "no enchantment");
        assertFalse(EnchantmentFormulas.slowFallActs(1, true, falling, 3.5F), "sneaking turns it off");
        assertFalse(EnchantmentFormulas.slowFallActs(1, false, -0.0784000015258789, 3.5F), "not faster than the first tick");
        assertFalse(EnchantmentFormulas.slowFallActs(1, false, falling, 2.5F), "fallen too little");
        assertTrue(EnchantmentFormulas.slowFallActs(1, false, falling, 2.9F), "the clamp value of the distance keeps it on");

        assertTrue(EnchantmentFormulas.slowFallActsOnServer(2, false, false, 3.5F));
        assertFalse(EnchantmentFormulas.slowFallActsOnServer(2, false, true, 3.5F), "on the ground");
        assertFalse(EnchantmentFormulas.slowFallActsOnServer(2, true, false, 3.5F));
    }

    @Test
    void slowFallDividesTheMotionAndShortensTheFall() {
        assertEquals(-0.5D / 1.33D, EnchantmentFormulas.slowFallMotion(-0.5, 1), 1e-6);
        assertEquals(-0.5D / 1.99D, EnchantmentFormulas.slowFallMotion(-0.5, 3), 1e-6);
        assertEquals(9F, EnchantmentFormulas.slowFallDistance(10F, 3), EPSILON);
        assertEquals(10F - 1F / 3F, EnchantmentFormulas.slowFallDistance(10F, 1), EPSILON);
        assertEquals(2.9F, EnchantmentFormulas.slowFallDistance(3F, 3), EPSILON, "never below 2.9");
    }

    @Test
    void shockwaveHurtsATenthPerLevelAndBlockOfTheFallBeyondThree() {
        assertEquals(2F, EnchantmentFormulas.shockwaveDamage(2, 10F), EPSILON);
        assertEquals(.5F, EnchantmentFormulas.shockwaveDamage(1, 5F), EPSILON);
        assertTrue(EnchantmentFormulas.shockwaveActs(1, 3.5F));
        assertFalse(EnchantmentFormulas.shockwaveActs(1, 3F), "a fall of exactly three blocks is not enough");
        assertFalse(EnchantmentFormulas.shockwaveActs(0, 30F));
    }

    // ---- mining ----------------------------------------------------------------------------------------------------

    @Test
    void shatterSpeedsUpOnlyBlocksHarderThanTwenty() {
        assertEquals(6D, EnchantmentFormulas.shatterFactor(2, 50F), 1e-9, "obsidian");
        assertEquals(.8D, EnchantmentFormulas.shatterFactor(2, 1.5F), 1e-9, "stone");
        assertEquals(.8D, EnchantmentFormulas.shatterFactor(5, 20F), 1e-9, "twenty is not harder than twenty");
        assertEquals(.8D, EnchantmentFormulas.shatterFactor(5, -1F), 1e-9, "bedrock has a hardness of -1");
    }

    @Test
    void tunnelRewardsTheLineAndPunishesTheTurn() {
        assertEquals(1D, EnchantmentFormulas.tunnelFactor(3, false, 170F), 1e-9, "nothing remembered");
        assertEquals(1.6D, EnchantmentFormulas.tunnelFactor(3, true, 49.9F), 1e-9);
        assertEquals(.3D, EnchantmentFormulas.tunnelFactor(3, true, 50F), 1e-9);
        assertEquals(1.2D, EnchantmentFormulas.tunnelFactor(1, true, 0F), 1e-9);
    }

    @Test
    void disintegrateAndFlamingTouchBreakTheirBlocksAtOnceAndNoOther() {
        double instant = EnchantmentFormulas.INSTANT_BREAK_FACTOR;
        double none = EnchantmentFormulas.NO_BREAK_FACTOR;
        // Disintegrate: hardness up to 1.5 (dirt .5, stone 1.5), not cobblestone (2.0)
        assertEquals(instant, EnchantmentFormulas.breakSpeedFactor(0, 0, 1, 0, 0.5F, false, false, 0F));
        assertEquals(instant, EnchantmentFormulas.breakSpeedFactor(0, 0, 1, 0, 1.5F, false, false, 0F));
        assertEquals(none, EnchantmentFormulas.breakSpeedFactor(0, 0, 1, 0, 2.0F, false, false, 0F));
        assertEquals(none, EnchantmentFormulas.breakSpeedFactor(0, 0, 1, 0, 2.0F, true, false, 0F), "wood is not its block");
        // Flaming Touch: wood only
        assertEquals(instant, EnchantmentFormulas.breakSpeedFactor(0, 0, 0, 1, 2.0F, true, false, 0F));
        assertEquals(none, EnchantmentFormulas.breakSpeedFactor(0, 0, 0, 1, 1.5F, false, false, 0F));
        // no such enchantment: the product of Shatter and Tunnel, or nothing
        assertEquals(1D, EnchantmentFormulas.breakSpeedFactor(0, 0, 0, 0, 50F, false, false, 0F), 1e-9);
        assertEquals(6D * 1.6D, EnchantmentFormulas.breakSpeedFactor(2, 3, 0, 0, 50F, false, true, 10F), 1e-9);
    }

    // ---- Quick Draw --------------------------------------------------------------------------------------------------

    @Test
    void quickDrawTakesOneExtraTickEveryFourthTickAtLevelOneAndEveryThirdAtLevelTwo() {
        // The handler adds one when the clock is a multiple of 6 - level; the first multiple of five below 72000 is reached on
        // the 5th tick, and every 4 ticks after that (the extra tick moves the clock to the next multiple at once).
        assertEquals(0, EnchantmentFormulas.quickDrawBonusTicks(1, 4));
        assertEquals(1, EnchantmentFormulas.quickDrawBonusTicks(1, 5));
        assertEquals(1, EnchantmentFormulas.quickDrawBonusTicks(1, 8));
        assertEquals(2, EnchantmentFormulas.quickDrawBonusTicks(1, 9));
        assertEquals(4, EnchantmentFormulas.quickDrawBonusTicks(1, 20));
        // level two: multiples of four: the 4th tick, then every 3 ticks
        assertEquals(0, EnchantmentFormulas.quickDrawBonusTicks(2, 3));
        assertEquals(1, EnchantmentFormulas.quickDrawBonusTicks(2, 4));
        assertEquals(2, EnchantmentFormulas.quickDrawBonusTicks(2, 7));
        assertEquals(6, EnchantmentFormulas.quickDrawBonusTicks(2, 20));
        assertEquals(0, EnchantmentFormulas.quickDrawBonusTicks(0, 20));
        assertEquals(0, EnchantmentFormulas.quickDrawBonusTicks(2, 0));
    }

    @Test
    void aLongerDrawNeverGivesLessThanAShorterOne() {
        for (int level = 1; level <= 2; level++) {
            int previous = 0;
            for (int ticks = 0; ticks <= 100; ticks++) {
                int bonus = EnchantmentFormulas.quickDrawBonusTicks(level, ticks);
                assertTrue(bonus >= previous, "level " + level + " ticks " + ticks);
                previous = bonus;
            }
        }
    }

    // ---- the damage solver of the attack ---------------------------------------------------------------------------

    @Test
    void theAttackBaseIsSolvedSoThatTheWholeDamageIsMultiplied() {
        // damage(a) = .8 a + 2 (the part of the enchantment that does not grow with the base): base 7 gives 7.6
        double solved = AttackBoost.solveBase(7D, 3D, a -> .8D * a + 2D);
        assertEquals(26D, solved, 1e-9);
        assertEquals(3D * (.8D * 7D + 2D), .8D * solved + 2D, 1e-9);
        // a factor below one lowers the base; a factor that would need a negative base stops at zero
        assertEquals(2.25D, AttackBoost.solveBase(7D, .5D, a -> .8D * a + 2D), 1e-9);
        assertEquals(0D, AttackBoost.solveBase(7D, .1D, a -> .2D * a + 5D), 1e-9);
    }

    @Test
    void aFlatDamageLineFallsBackToScalingTheBase() {
        assertEquals(21D, AttackBoost.solveBase(7D, 3D, a -> 5D), 1e-9);
    }
}
