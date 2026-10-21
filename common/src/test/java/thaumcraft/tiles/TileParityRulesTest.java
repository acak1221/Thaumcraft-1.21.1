package thaumcraft.tiles;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import thaumcraft.common.lib.tiles.TileParityRules;

/**
 * The numbers of the TC4 4.2.3.5 block entities, checked against the decompiled 1.7.10
 * original. Every assertion names the original behaviour it protects.
 */
class TileParityRulesTest {
    // ----------------------------------------------------------------------------------------------
    // Crucible
    // ----------------------------------------------------------------------------------------------
    @Test void theCrucibleHoldsAThousandUnitsOfWaterAndAHundredAspects() {
        assertEquals(1000, TileParityRules.CRUCIBLE_CAPACITY);
        assertEquals(100, TileParityRules.CRUCIBLE_MAX_TAGS);
        assertTrue(TileParityRules.crucibleOverflows(101));
        assertFalse(TileParityRules.crucibleOverflows(100));
    }

    @Test void theCrucibleBoilsAtOneHundredAndFiftyOneAndStopsAtTwoHundred() {
        assertEquals(151, TileParityRules.CRUCIBLE_BOIL_HEAT);
        assertEquals(200, TileParityRules.CRUCIBLE_MAX_HEAT);
        assertEquals(1, TileParityRules.CRUCIBLE_COOL_STEP);
        assertEquals(3, TileParityRules.crucibleHeatStep(1), "a bellows adds two to the heat step");
        assertEquals(7, TileParityRules.crucibleHeatStep(3));
        assertEquals(1, TileParityRules.crucibleHeatStep(0));
    }

    @Test void theCrucibleCountsTheBellowsOnlyOnce() {
        // Oracle TileCrucible:69,98-100 - the count is found on the first tick
        // and then kept; the marker below one means "not counted yet".
        assertEquals(-1, TileParityRules.CRUCIBLE_BELLOWS_UNCOUNTED);
    }

    @Test void theCrucibleOverflowsEveryFifthTickAndDecaysAfterAHundredTicks() {
        assertEquals(5, TileParityRules.CRUCIBLE_OVERFLOW_INTERVAL);
        assertEquals(100L, TileParityRules.CRUCIBLE_DECAY_COUNTER);
        assertEquals(150, TileParityRules.CRUCIBLE_DECAY_HEAT);
        assertEquals(2, TileParityRules.CRUCIBLE_DECAY_WATER);
        assertFalse(TileParityRules.crucibleDecays(100L, 151), "the counter must be above a hundred");
        assertFalse(TileParityRules.crucibleDecays(101L, 150), "the heat must be above a hundred and fifty");
        assertTrue(TileParityRules.crucibleDecays(101L, 151));
    }

    @Test void aSmeltSpendsFiftyUnitsOfWater() {
        assertEquals(50, TileParityRules.CRUCIBLE_SMELT_WATER);
    }

    @Test void theFluidHeightOfTheCrucibleFollowsTheOriginalCurve() {
        // Oracle TileCrucible:315-325: the amount is divided by the capacity first, the base is
        // 0.3f + 0.5f of it and the aspects add their share of the rest.
        assertEquals(crucibleHeight(0, 0), TileParityRules.crucibleFluidHeight(0, 0));
        assertEquals(crucibleHeight(1000, 0), TileParityRules.crucibleFluidHeight(1000, 0));
        assertEquals(crucibleHeight(1000, 100), TileParityRules.crucibleFluidHeight(1000, 100),
                "a full crucible never reaches exactly one");
        assertEquals(crucibleHeight(1000, 200), TileParityRules.crucibleFluidHeight(1000, 200),
                "an overfull crucible is clamped just above one");
    }

    /** The original's own fluid height: {@code base = 0.3f + 0.5f * (amount / capacity)}. */
    private static float crucibleHeight(int water, int tags) {
        float base = 0.3F + 0.5F * (water / (float) 1000);
        float height = base + tags / 100F * (1 - base);
        if (height > 1.0F) height = 1.001F;
        if (height == 1.0F) height = 0.9999F;
        return height;
    }

    // ----------------------------------------------------------------------------------------------
    // Jars
    // ----------------------------------------------------------------------------------------------
    @Test void aJarHoldsSixtyFourUnitsAndDrawsOneEveryFifthTick() {
        assertEquals(64, TileParityRules.JAR_CAPACITY);
        assertEquals(5, TileParityRules.JAR_PULL_INTERVAL);
    }

    @Test void anUnlabelledJarOffersThirtyTwoAndALabelledOneSixtyFour() {
        assertEquals(32, TileParityRules.jarMinimumSuction(false, false));
        assertEquals(64, TileParityRules.jarMinimumSuction(true, false));
        assertEquals(32, TileParityRules.jarMinimumSuction(false, true));
        assertEquals(48, TileParityRules.jarMinimumSuction(true, true), "a labelled void jar offers forty eight");
    }

    @Test void aFullJarOffersNoSuctionAtAll() {
        assertEquals(32, TileParityRules.jarSuction(false, false, 0));
        assertEquals(64, TileParityRules.jarSuction(true, false, 63));
        assertEquals(0, TileParityRules.jarSuction(false, false, 64), "a full jar is closed");
        assertEquals(0, TileParityRules.jarSuction(true, false, 64));
        assertEquals(48, TileParityRules.jarSuction(true, true, 63));
        assertEquals(32, TileParityRules.jarSuction(true, true, 64), "a full void jar falls back to thirty two");
    }

    // ----------------------------------------------------------------------------------------------
    // Alembic
    // ----------------------------------------------------------------------------------------------
    @Test void theAlembicHoldsThirtyTwoUnits() {
        assertEquals(32, TileParityRules.ALEMBIC_CAPACITY);
    }

    @Test void theAlembicIsConnectedOnEverySideButDownAndItsFront() {
        // Oracle TileAlembic:194 - the faces are the ForgeDirection order.
        assertTrue(TileParityRules.alembicConnectable(2, 3), "north is connected to a south facing alembic");
        assertFalse(TileParityRules.alembicConnectable(3, 3), "the front is closed");
        assertFalse(TileParityRules.alembicConnectable(0, 3), "down is closed");
        assertTrue(TileParityRules.alembicConnectable(4, 3));
    }

    // ----------------------------------------------------------------------------------------------
    // Infusion matrix
    // ----------------------------------------------------------------------------------------------
    @Test void theMatrixRunsACycleEveryTenTicksAndTwentyWhileItPaysExperience() {
        assertEquals(10, TileParityRules.INFUSION_CYCLE_DELAY);
        assertEquals(20, TileParityRules.INFUSION_XP_CYCLE_DELAY);
        assertEquals(100, TileParityRules.infusionStructurePeriod(false));
        assertEquals(20, TileParityRules.infusionStructurePeriod(true));
    }

    @Test void theMatrixRollsTheHazardAgainstFiveHundredAndHasTwentyOneOutcomes() {
        assertEquals(500, TileParityRules.INFUSION_INSTABILITY_ROLL);
        assertEquals(21, TileParityRules.INFUSION_OUTCOMES);
        assertTrue(TileParityRules.infusionInstabilityTriggers(10, 10), "the roll is inclusive");
        assertFalse(TileParityRules.infusionInstabilityTriggers(10, 11));
        assertFalse(TileParityRules.infusionInstabilityTriggers(0, 0), "a stable matrix never fails");
    }

    @Test void theInstabilityOfTheMatrixNeverRisesAboveTwentyFive() {
        assertEquals(25, TileParityRules.INFUSION_INSTABILITY_CAP);
        assertEquals(25, TileParityRules.infusionInstabilityAfter(24, 1));
        assertEquals(25, TileParityRules.infusionInstabilityAfter(24, 2));
        assertEquals(3, TileParityRules.infusionInstabilityAfter(2, 1));
    }

    @Test void theMatrixCountsThePedestalsOfTheSquareOfSeventeen() {
        assertEquals(12, TileParityRules.INFUSION_SCAN_RADIUS);
        assertEquals(8, TileParityRules.INFUSION_PEDESTAL_RADIUS);
        assertTrue(TileParityRules.infusionPedestalCounts(8, 8));
        assertFalse(TileParityRules.infusionPedestalCounts(9, 0));
        assertFalse(TileParityRules.infusionPedestalCounts(0, 9));
    }

    @Test void theEssentiaSourceOfTheMatrixReachesTwelveBlocks() {
        assertEquals(12, TileParityRules.INFUSION_SOURCE_RANGE);
    }

    // ----------------------------------------------------------------------------------------------
    // Thaumatorium
    // ----------------------------------------------------------------------------------------------
    @Test void theThaumatoriumWorksEveryFifthTickAndReadsItsHeatEveryFortieth() {
        assertEquals(5, TileParityRules.THAUMATORIUM_WORK_INTERVAL);
        assertEquals(40, TileParityRules.THAUMATORIUM_HEAT_INTERVAL);
        assertEquals(128, TileParityRules.THAUMATORIUM_SUCTION);
        assertEquals(64, TileParityRules.THAUMATORIUM_STACK_LIMIT);
        assertEquals(64.0D, TileParityRules.THAUMATORIUM_USE_RANGE);
    }

    // ----------------------------------------------------------------------------------------------
    // Mirrors
    // ----------------------------------------------------------------------------------------------
    @Test void theMirrorRetriesItsLinkEveryFortyTicksAndWaitsLongerEveryTime() {
        assertEquals(40, TileParityRules.MIRROR_LINK_RETRY);
        assertEquals(20, TileParityRules.MIRROR_LINK_RETRY_STEP);
        assertEquals(600, TileParityRules.MIRROR_LINK_RETRY_MAX);
    }

    @Test void theInstabilityOfTheMirrorFallsByOneEveryTwentyTicks() {
        assertEquals(20, TileParityRules.MIRROR_INSTABILITY_DECAY);
    }

    @Test void theEjectionRateOfTheMirrorIsTheSquareOfTheInstabilityOverFifty() {
        assertEquals(50, TileParityRules.MIRROR_INSTABILITY_DIVISOR);
        assertEquals(0, TileParityRules.mirrorEjectRate(49), "below fifty the mirror ejects every tick");
        assertEquals(1, TileParityRules.mirrorEjectRate(50));
        assertEquals(4, TileParityRules.mirrorEjectRate(100));
    }

    @Test void theMirrorKeepsNothingInItsFirstTwentyTicks() {
        assertEquals(20, TileParityRules.MIRROR_EJECT_DELAY);
    }

    @Test void theInventoryOfTheMirrorIsOneSlotOfSixtyFour() {
        assertEquals(1, TileParityRules.MIRROR_INVENTORY_SLOTS);
        assertEquals(64, TileParityRules.MIRROR_STACK_LIMIT);
    }

    @Test void theEssentiaMirrorHandsOutOneUnitAndLooksEightBlocksDeep() {
        assertEquals(1, TileParityRules.MIRROR_ESSENTIA_MAX_TAKE);
        assertEquals(8, TileParityRules.MIRROR_ESSENTIA_RANGE);
    }
}
