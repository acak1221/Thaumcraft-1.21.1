package thaumcraft.golems;

import net.minecraft.SharedConstants;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.common.lib.golems.GolemAiRules;
import static org.junit.jupiter.api.Assertions.*;

/** The pure decision rules of TC4's golem goal set, ported to native 1.21.1. */
class GolemAiRulesTest {

    @BeforeAll static void bootstrap() {
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
    }

    /* ---------------------------------------------------------------- shared cadence */
    @Test void originalFiveTickSearchGate() {
        assertTrue(GolemAiRules.searchTick(0));
        assertFalse(GolemAiRules.searchTick(1));
        assertFalse(GolemAiRules.searchTick(4));
        assertTrue(GolemAiRules.searchTick(5));
        assertEquals(5, GolemAiRules.GOLEM_DELAY);
    }

    @Test void originalPathAndDetourBudgets() {
        assertEquals(200, GolemAiRules.pathBudget());
        assertEquals(20, GolemAiRules.detourBudget());
    }

    @Test void originalAdjacentAndHomeDistances() {
        assertTrue(GolemAiRules.adjacent(3.9));
        assertFalse(GolemAiRules.adjacent(4.0));
        assertTrue(GolemAiRules.atHome(5.0));
        assertFalse(GolemAiRules.atHome(5.1));
        assertTrue(GolemAiRules.homeReached(2.9));
        assertFalse(GolemAiRules.homeReached(3.0));
    }

    @Test void originalCarrySpace() {
        assertEquals(64, GolemAiRules.carrySpace(64, ItemStack.EMPTY));
        assertEquals(60, GolemAiRules.carrySpace(64, new ItemStack(Items.DIRT, 4)));
        assertEquals(0, GolemAiRules.carrySpace(64, new ItemStack(Items.DIRT, 64)));
    }

    /* ---------------------------------------------------------------- combat */
    @Test void originalMeleeReachUsesTheFourfoldWidth() {
        assertEquals(2.0, GolemAiRules.meleeReach(0.5F, 1.0F), 1.0E-9);
        assertEquals(5.0, GolemAiRules.meleeReach(1.0F, 1.0F), 1.0E-9);
        assertEquals(17.0, GolemAiRules.meleeReach(2.0F, 1.0F), 1.0E-9);
    }

    @Test void originalRepathDelayAndDistancePenalty() {
        assertEquals(4, GolemAiRules.repathDelay(0, 0));
        assertEquals(10, GolemAiRules.repathDelay(0, 6));
        assertEquals(20, GolemAiRules.repathDelay(10, 6));
        assertEquals(0, GolemAiRules.distancePenalty(256.0));
        assertEquals(5, GolemAiRules.distancePenalty(256.1));
        assertEquals(10, GolemAiRules.distancePenalty(1024.1));
    }

    @Test void originalNearestAndOldestSorters() {
        assertEquals(-1, GolemAiRules.compareDistanceSq(4.0, 9.0));
        assertEquals(1, GolemAiRules.compareDistanceSq(9.0, 4.0));
        assertEquals(0, GolemAiRules.compareDistanceSq(9.0, 9.0));
        assertEquals(-1, GolemAiRules.compareAge(100, 50));
        assertEquals(1, GolemAiRules.compareAge(50, 100));
        assertEquals(0, GolemAiRules.compareAge(50, 50));
    }

    @Test void originalButcherNeedsMoreThanTwoAnimals() {
        assertFalse(GolemAiRules.butcherPopulationSufficient(2));
        assertTrue(GolemAiRules.butcherPopulationSufficient(3));
    }

    @Test void originalTargetChanceGate() {
        assertTrue(GolemAiRules.targetChancePassed(0, 7));
        assertTrue(GolemAiRules.targetChancePassed(10, 0));
        assertFalse(GolemAiRules.targetChancePassed(10, 3));
    }

    @Test void originalCreeperSwellRules() {
        assertEquals(1.125F, GolemAiRules.avoidCreeperFarSpeed(1.0F), 1.0E-6);
        assertEquals(1.25F, GolemAiRules.avoidCreeperNearSpeed(1.0F), 1.0E-6);
        assertTrue(GolemAiRules.creeperIsNear(48.9));
        assertFalse(GolemAiRules.creeperIsNear(49.0));
        assertTrue(GolemAiRules.creeperSwells(8.9));
        assertFalse(GolemAiRules.creeperSwells(9.0));
        assertTrue(GolemAiRules.creeperSwellEnds(49.1));
        assertFalse(GolemAiRules.creeperSwellEnds(49.0));
    }

    @Test void originalLongRangeMinimumDistance() {
        assertTrue(GolemAiRules.beyondLongRange(9.0, 3.0));
        assertFalse(GolemAiRules.beyondLongRange(8.9, 3.0));
    }

    @Test void originalDartRules() {
        assertEquals(30, GolemAiRules.dartInterval(0));
        assertEquals(22, GolemAiRules.dartInterval(1));
        assertEquals(15, GolemAiRules.dartFirstDelay(30));
        assertTrue(GolemAiRules.dartTooClose(8.9));
        assertFalse(GolemAiRules.dartTooClose(9.0));
        assertTrue(GolemAiRules.dartInReach(64.0, 10.0F));
        assertFalse(GolemAiRules.dartInReach(64.1, 10.0F));
    }

    /* ---------------------------------------------------------------- interact */
    @Test void originalCropAndLogBreakDelays() {
        assertEquals(20, GolemAiRules.cropBreakDelay(0, 1.0F));
        assertEquals(16, GolemAiRules.cropBreakDelay(2, 1.0F));
        assertEquals(10, GolemAiRules.cropBreakDelay(20, 1.0F));
        assertEquals(20, GolemAiRules.logBreakDelay(0, 1.0F));
        assertEquals(14, GolemAiRules.logBreakDelay(2, 1.0F));
        assertEquals(5, GolemAiRules.logBreakDelay(20, 1.0F));
    }

    @Test void originalBreakPulseModulo() {
        assertEquals(6, GolemAiRules.pulseMod(36));
        assertEquals(6, GolemAiRules.pulseMod(6));
        assertEquals(1, GolemAiRules.pulseMod(0));
    }

    @Test void originalSearchRadii() {
        assertEquals(2, GolemAiRules.cropSearchRadius(16.0F));
        assertEquals(2, GolemAiRules.logSearchRadius(15.0F));
        assertEquals(2, GolemAiRules.fishSearchRadius(16.0F));
        assertEquals(8, GolemAiRules.logSearchAttempts(16.0F));
    }

    @Test void originalFishingQualityAndChances() {
        assertEquals(0.00012F, GolemAiRules.fishingQuality(2, 1, 2), 1.0E-9);
        assertEquals(0.00027F, GolemAiRules.fishingCatchChance(2, 1, 2, 1), 1.0E-9);
        assertEquals(0.085F, GolemAiRules.fishingJunkChance(2, 1, 0), 1.0E-6);
        assertEquals(0.10F, GolemAiRules.fishingJunkChance(0, 0, 0), 1.0E-6);
        assertEquals(0.025F, GolemAiRules.fishingJunkChance(0, 0, 3), 1.0E-6);
        assertEquals(0.05575F, GolemAiRules.fishingTreasureChance(2, 1, 2, 0), 1.0E-6);
        assertEquals(0.06825F, GolemAiRules.fishingTreasureChance(2, 1, 2, 1), 1.0E-6);
        assertEquals(300, GolemAiRules.fishingDelay(0));
        assertEquals(499, GolemAiRules.fishingDelay(199));
    }

    @Test void originalUseItemTickGate() {
        assertEquals(15, GolemAiRules.useItemNextTick(0, 0));
        assertEquals(12, GolemAiRules.useItemNextTick(0, 1));
    }

    /* ---------------------------------------------------------------- fluid */
    @Test void originalFluidTankRules() {
        assertTrue(GolemAiRules.fluidNearFull(1001, 2000));
        assertFalse(GolemAiRules.fluidNearFull(1000, 2000));
        assertEquals(1000, GolemAiRules.fluidRoom(1000, 2000));
        assertEquals(0, GolemAiRules.fluidRoom(2000, 2000));
        assertEquals(1000, GolemAiRules.BUCKET_MB);
    }

    @Test void originalEssentiaGatherRules() {
        assertEquals(32, GolemAiRules.essentiaGatherAmount(64, 32, 64));
        assertEquals(0, GolemAiRules.essentiaGatherAmount(64, 64, 64));
        assertTrue(GolemAiRules.essentiaAspectMatches(null, Aspect.METAL));
        assertTrue(GolemAiRules.essentiaAspectMatches(Aspect.METAL, Aspect.METAL));
        assertFalse(GolemAiRules.essentiaAspectMatches(Aspect.METAL, Aspect.EARTH));
        assertEquals(256.0F, GolemAiRules.pumpDistance(16.0F), 1.0E-6);
    }

    /* ---------------------------------------------------------------- inventory */
    @Test void originalItemPickupRules() {
        assertTrue(GolemAiRules.pickupAllowed(4));
        assertFalse(GolemAiRules.pickupAllowed(5));
        assertTrue(GolemAiRules.pickupInReach(2.0));
        assertFalse(GolemAiRules.pickupInReach(2.01));
    }

    @Test void originalPlacementChangeTest() {
        ItemStack four = new ItemStack(Items.DIRT, 4);
        assertFalse(GolemAiRules.placementChanged(four, four));
        assertTrue(GolemAiRules.placementChanged(new ItemStack(Items.DIRT, 3), four));
        assertTrue(GolemAiRules.placementChanged(ItemStack.EMPTY, four));
        assertTrue(GolemAiRules.placementChanged(new ItemStack(Items.SAND, 4), four));
    }

    @Test void originalNearestAndRangeRules() {
        assertTrue(GolemAiRules.nearerThan(4.0, 9.0));
        assertFalse(GolemAiRules.nearerThan(9.0, 4.0));
        assertTrue(GolemAiRules.withinRange(256.0, 16.0F));
        assertFalse(GolemAiRules.withinRange(256.1, 16.0F));
    }

    /* ---------------------------------------------------------------- misc */
    @Test void originalDoorRules() {
        assertTrue(GolemAiRules.doorInReach(2.25));
        assertFalse(GolemAiRules.doorInReach(2.26));
        assertEquals(200, GolemAiRules.DOOR_BUDGET);
        assertEquals(100, GolemAiRules.DOOR_START_BUDGET);
        assertEquals(20, GolemAiRules.OPEN_DOOR_TICKS);
    }

    @Test void originalWanderAndConversionRules() {
        assertTrue(GolemAiRules.mayWander(99));
        assertFalse(GolemAiRules.mayWander(100));
        assertTrue(GolemAiRules.wanderRoll(0));
        assertFalse(GolemAiRules.wanderRoll(1));
        assertTrue(GolemAiRules.convertGrassRoll(0));
        assertFalse(GolemAiRules.convertGrassRoll(1));
        assertEquals(40, GolemAiRules.CONVERT_GRASS_TICKS);
    }

    @Test void originalAltarFocusDistance() {
        assertTrue(GolemAiRules.altarInReach(16.0));
        assertFalse(GolemAiRules.altarInReach(16.1));
        assertEquals(40, GolemAiRules.ALTAR_FOCUS_INTERVAL);
    }

    @Test void originalReturnHomeDelay() {
        assertEquals(5, GolemAiRules.returnHomeDelay(5));
        assertEquals(50, GolemAiRules.returnHomeDelay(50));
        assertEquals(10, GolemAiRules.returnHomeDelayIncrement(5));
    }
}
