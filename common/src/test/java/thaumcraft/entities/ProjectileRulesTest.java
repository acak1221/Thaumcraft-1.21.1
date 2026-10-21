package thaumcraft.entities;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import thaumcraft.common.lib.entities.ProjectileRules;

/** The TC4 projectile and supporting entity rules of {@link ProjectileRules}. */
class ProjectileRulesTest {

    // ------------------------------------------------------------------
    // Bottle of taint
    // ------------------------------------------------------------------

    @Test void theBottleKeepsItsOriginalThrowConstants() {
        assertEquals(0.05F, ProjectileRules.BOTTLE_TAINT_GRAVITY);
        assertEquals(0.5F, ProjectileRules.BOTTLE_TAINT_LAUNCH_SPEED);
        assertEquals(-20F, ProjectileRules.BOTTLE_TAINT_PITCH_OFFSET);
    }

    @Test void theBottleSplashIsFiveBlocksAndLastsOneHundredTicks() {
        assertEquals(5D, ProjectileRules.BOTTLE_TAINT_SPLASH_RADIUS);
        assertEquals(100, ProjectileRules.BOTTLE_TAINT_POISON_TICKS);
        assertEquals(10, ProjectileRules.BOTTLE_TAINT_FIBRE_ATTEMPTS);
    }

    @Test void taintedMobsAndUndeadSurviveTheBottle() {
        assertTrue(ProjectileRules.bottleTaintAffects(false, false));
        assertFalse(ProjectileRules.bottleTaintAffects(true, false));
        assertFalse(ProjectileRules.bottleTaintAffects(false, true));
        assertFalse(ProjectileRules.bottleTaintAffects(true, true));
    }

    @Test void theBottleFibreOffsetIsTheOriginalIntCast() {
        assertEquals(0, ProjectileRules.bottleTaintFibreOffset(0.5F, 0.5F));
        assertEquals(5, ProjectileRules.bottleTaintFibreOffset(1F, 0F));
        assertEquals(-5, ProjectileRules.bottleTaintFibreOffset(0F, 1F));
        // The original multiplies and truncates in float, so 0.9 - 0.5 stays below two.
        assertEquals(1, ProjectileRules.bottleTaintFibreOffset(0.9F, 0.5F));
    }

    @Test void theBottleOnlyGrowsFibresOnAReplaceableBlockOverAFullCube() {
        assertTrue(ProjectileRules.bottleTaintFibreFits(true, true));
        assertFalse(ProjectileRules.bottleTaintFibreFits(false, true));
        assertFalse(ProjectileRules.bottleTaintFibreFits(true, false));
        assertFalse(ProjectileRules.bottleTaintFibreFits(false, false));
    }

    // ------------------------------------------------------------------
    // Golem orb
    // ------------------------------------------------------------------

    @Test void theGolemOrbDealsTheThrowerAttackDamageTimesItsFactor() {
        assertEquals(6F, ProjectileRules.golemOrbDamage(10F, false));
        assertEquals(10F, ProjectileRules.golemOrbDamage(10F, true));
        assertEquals(0F, ProjectileRules.golemOrbDamage(0F, true));
    }

    @Test void theGolemOrbLivesOneHundredSixtyTicksAndTheRedOneTwoHundredForty() {
        assertEquals(160, ProjectileRules.golemOrbLifetime(false));
        assertEquals(240, ProjectileRules.golemOrbLifetime(true));
    }

    @Test void theGolemOrbHomingDividesByTheSquaredDistanceAndTheOriginalAcceleration() {
        // Oracle EntityGolemOrb:95-102 - the original divides the delta by
        // getDistanceSqToEntity, the SQUARED distance to the target, and scales it by 0.2 in double.
        assertEquals(0D + (1D / 1D) * 0.2D, ProjectileRules.golemOrbHoming(0D, 1D, 1D));
        assertEquals(0.2D + (1D / 1D) * 0.2D, ProjectileRules.golemOrbHoming(0.2D, 1D, 1D));
        assertEquals(0D + (1D / 2D) * 0.2D, ProjectileRules.golemOrbHoming(0D, 1D, 2D));
        assertEquals(0D + (1D / 8D) * 0.2D, ProjectileRules.golemOrbHoming(0D, 1D, 8D));
    }

    @Test void theGolemOrbHomingStopsOnAZeroDistance() {
        assertEquals(0.3D, ProjectileRules.golemOrbHoming(0.3D, 1D, 0D));
    }

    @Test void theGolemOrbMotionIsClampedToAQuarter() {
        // Oracle EntityGolemOrb:103-105 clamps the float of the motion between -0.25f and 0.25f.
        assertEquals((double) 0.25F, ProjectileRules.clampGolemOrbSpeed(0.9D));
        assertEquals((double) -0.25F, ProjectileRules.clampGolemOrbSpeed(-0.9D));
        assertEquals((double) 0.2F, ProjectileRules.clampGolemOrbSpeed(0.2D));
    }

    // ------------------------------------------------------------------
    // Primal arrow
    // ------------------------------------------------------------------

    @Test void thePrimalArrowDealsItsOriginalBaseDamage() {
        assertEquals(2.1D, ProjectileRules.primalArrowDamage(ProjectileRules.PRIMAL_ARROW_TYPE_AIR));
        assertEquals(2.1D, ProjectileRules.primalArrowDamage(ProjectileRules.PRIMAL_ARROW_TYPE_FIRE));
        assertEquals(2.1D, ProjectileRules.primalArrowDamage(ProjectileRules.PRIMAL_ARROW_TYPE_SLOW));
        assertEquals(2.1D, ProjectileRules.primalArrowDamage(ProjectileRules.PRIMAL_ARROW_TYPE_HEAVY) / 1.5D);
    }

    @Test void theHeavyPrimalArrowDealsOneAndAHalfAndTheOrderAndWitherArrowsFourFifths() {
        // Oracle EntityPrimalArrow:355-368: the damage is the double 2.1 times 1.5 or times 0.8.
        assertEquals(2.1D * 1.5D, ProjectileRules.primalArrowDamage(ProjectileRules.PRIMAL_ARROW_TYPE_HEAVY));
        assertEquals(2.1D * 0.8D, ProjectileRules.primalArrowDamage(ProjectileRules.PRIMAL_ARROW_TYPE_ORDER));
        assertEquals(2.1D * 0.8D, ProjectileRules.primalArrowDamage(ProjectileRules.PRIMAL_ARROW_TYPE_WITHER));
    }

    @Test void thePrimalArrowHitDamageIsTheCeilingOfSpeedTimesDamage() {
        assertEquals(5, ProjectileRules.primalArrowHitDamage(2D, 2.1D));
        assertEquals(6, ProjectileRules.primalArrowHitDamage(2.4D, 2.1D));
        assertEquals(1, ProjectileRules.primalArrowHitDamage(0.1D, 2.1D));
    }

    @Test void theCriticalPrimalArrowAddsItsOriginalRoll() {
        assertEquals(4, ProjectileRules.primalArrowCritBound(4));
        assertEquals(5, ProjectileRules.primalArrowCritBound(6));
        assertEquals(2, ProjectileRules.primalArrowCritBound(0));
        assertEquals(9, ProjectileRules.primalArrowCritDamage(4, 5));
    }

    @Test void onlyTheBurningPrimalArrowBurnsAndTheFireArrowBurnsLonger() {
        assertEquals(5, ProjectileRules.primalArrowFireSeconds(true, ProjectileRules.PRIMAL_ARROW_TYPE_AIR));
        assertEquals(0, ProjectileRules.primalArrowFireSeconds(false, ProjectileRules.PRIMAL_ARROW_TYPE_AIR));
        assertEquals(0, ProjectileRules.primalArrowFireSeconds(true, ProjectileRules.PRIMAL_ARROW_TYPE_SLOW));
        assertEquals(5, ProjectileRules.primalArrowFireTypeSeconds(ProjectileRules.PRIMAL_ARROW_TYPE_FIRE));
        assertEquals(0, ProjectileRules.primalArrowFireTypeSeconds(ProjectileRules.PRIMAL_ARROW_TYPE_AIR));
    }

    @Test void thePrimalArrowLaunchSpeedIsTheChargeTimesTheDamageTimesOneAndAHalf() {
        // Oracle EntityPrimalArrow:135 creates the arrow with par3 * 1.5f, in float.
        assertEquals(2F * 1.5F, ProjectileRules.primalArrowLaunchSpeed(2F));
        assertEquals(2.5F * 1.5F, ProjectileRules.primalArrowLaunchSpeed(2.5F));
        assertEquals(1F * 2F, ProjectileRules.primalArrowChargeDamage(1F, 2F));
    }

    @Test void thePrimalArrowTypesKeepTheirOriginalEffects() {
        assertTrue(ProjectileRules.primalArrowAppliesSlowness(ProjectileRules.PRIMAL_ARROW_TYPE_SLOW));
        assertFalse(ProjectileRules.primalArrowAppliesSlowness(ProjectileRules.PRIMAL_ARROW_TYPE_AIR));
        assertTrue(ProjectileRules.primalArrowAppliesOrder(ProjectileRules.PRIMAL_ARROW_TYPE_ORDER));
        assertFalse(ProjectileRules.primalArrowAppliesOrder(ProjectileRules.PRIMAL_ARROW_TYPE_HEAVY));
        assertTrue(ProjectileRules.primalArrowAppliesWither(ProjectileRules.PRIMAL_ARROW_TYPE_WITHER));
        assertFalse(ProjectileRules.primalArrowAppliesWither(ProjectileRules.PRIMAL_ARROW_TYPE_SLOW));
    }

    @Test void theAirAndOrderPrimalArrowsIgnoreArmourAndTheFireArrowBurns() {
        assertTrue(ProjectileRules.primalArrowIgnoresArmour(ProjectileRules.PRIMAL_ARROW_TYPE_AIR));
        assertTrue(ProjectileRules.primalArrowIgnoresArmour(ProjectileRules.PRIMAL_ARROW_TYPE_ORDER));
        assertFalse(ProjectileRules.primalArrowIgnoresArmour(ProjectileRules.PRIMAL_ARROW_TYPE_HEAVY));
        assertTrue(ProjectileRules.primalArrowIsFireDamage(ProjectileRules.PRIMAL_ARROW_TYPE_FIRE));
        assertFalse(ProjectileRules.primalArrowIsFireDamage(ProjectileRules.PRIMAL_ARROW_TYPE_AIR));
    }

    @Test void thePrimalArrowDiesAfterAHundredTicksInTheGround() {
        assertEquals(100, ProjectileRules.PRIMAL_ARROW_IN_GROUND_LIMIT);
    }

    // ------------------------------------------------------------------
    // Following item
    // ------------------------------------------------------------------

    @Test void theFollowingItemAcceleratesWhileItsDivisorFalls() {
        assertEquals(20, ProjectileRules.FOLLOW_ITEM_SLOWDOWN);
        assertEquals(19, ProjectileRules.followItemSlowdown(20));
        assertEquals(1, ProjectileRules.followItemSlowdown(2));
        assertEquals(1, ProjectileRules.followItemSlowdown(1));
    }

    @Test void theFollowingItemMovesOneBlockPerDivisor() {
        assertEquals(1D / 20D, ProjectileRules.followItemSpeed(20));
        assertEquals(1D / 1D, ProjectileRules.followItemSpeed(1));
    }

    @Test void theFollowingItemStopsInsideHalfABlock() {
        assertTrue(ProjectileRules.followItemArrived(0.5D));
        assertTrue(ProjectileRules.followItemArrived(0.1D));
        assertFalse(ProjectileRules.followItemArrived(0.51D));
    }

    @Test void theFollowingItemAimsAtTheMiddleOfItsTarget() {
        assertEquals(10D + 2D * 0.5D, ProjectileRules.followItemTargetY(10D, 2D));
        assertEquals(0.04D, ProjectileRules.FOLLOW_ITEM_GRAVITY);
    }

    // ------------------------------------------------------------------
    // Permanent item
    // ------------------------------------------------------------------

    @Test void thePermanentItemRestartsItsAgeBeforeTheVanillaLifetime() {
        assertEquals(6000, ProjectileRules.ITEM_LIFETIME);
        assertFalse(ProjectileRules.itemNeverDespawns(5994));
        assertTrue(ProjectileRules.itemNeverDespawns(5995));
        assertTrue(ProjectileRules.itemNeverDespawns(6000));
    }

    // ------------------------------------------------------------------
    // Inventories
    // ------------------------------------------------------------------

    @Test void theMobInventoryReportsOneSlotMoreThanItOwns() {
        assertEquals(10, ProjectileRules.inventoryMobContainerSize(9));
        assertEquals(1, ProjectileRules.inventoryMobContainerSize(0));
    }

    @Test void theMobInventoryOnlyMergesIntoACompatibleSlot() {
        assertTrue(ProjectileRules.inventoryMobAccepts(true, true, false, false, false));
        assertFalse(ProjectileRules.inventoryMobAccepts(false, true, false, false, false));
        assertFalse(ProjectileRules.inventoryMobAccepts(true, false, false, false, false));
        assertFalse(ProjectileRules.inventoryMobAccepts(true, true, true, false, false));
        assertFalse(ProjectileRules.inventoryMobAccepts(true, true, false, true, false));
        assertFalse(ProjectileRules.inventoryMobAccepts(true, true, false, false, true));
    }

    @Test void theMobInventoryFindsTheFirstEmptySlot() {
        assertEquals(0, ProjectileRules.inventoryMobFirstEmpty(new boolean[]{false, false}));
        assertEquals(1, ProjectileRules.inventoryMobFirstEmpty(new boolean[]{true, false}));
        assertEquals(-1, ProjectileRules.inventoryMobFirstEmpty(new boolean[]{true, true}));
    }

    @Test void theMobInventoryStoresOnlyWhatFits() {
        assertEquals(10, ProjectileRules.inventoryMobStoreAmount(20, 10, 64));
        assertEquals(5, ProjectileRules.inventoryMobStoreAmount(20, 64, 5));
        assertEquals(0, ProjectileRules.inventoryMobStoreAmount(20, 0, 64));
        assertEquals(0, ProjectileRules.inventoryMobStoreAmount(20, 64, 0));
    }

    @Test void thePechTradeOnlyAcceptsTheOfferedItemInItsFirstSlot() {
        assertTrue(ProjectileRules.inventoryPechAccepts(0));
        assertFalse(ProjectileRules.inventoryPechAccepts(1));
        assertFalse(ProjectileRules.inventoryPechAccepts(4));
        assertEquals(64, ProjectileRules.inventoryPechStackLimit());
        assertEquals(5, ProjectileRules.INVENTORY_PECH_SLOTS);
    }
}
