package thaumcraft.common.lib.utils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

class ExperiencePointsTest {
    @Test void cumulativePointsFollowTheNativeCurveAcrossBothBoundaries() {
        assertEquals(0L, ExperiencePoints.atLevel(0));
        assertEquals(7L, ExperiencePoints.atLevel(1));
        assertEquals(315L, ExperiencePoints.atLevel(15));
        assertEquals(352L, ExperiencePoints.atLevel(16));
        assertEquals(394L, ExperiencePoints.atLevel(17));
        assertEquals(1_395L, ExperiencePoints.atLevel(30));
        assertEquals(1_507L, ExperiencePoints.atLevel(31));
        assertEquals(1_628L, ExperiencePoints.atLevel(32));
    }

    @Test void nominalFocalRanksConvertEightLevelStepsToFixedPointPrices() {
        assertEquals(112L, ExperiencePoints.atLevel(8));
        assertEquals(352L, ExperiencePoints.atLevel(16));
        assertEquals(828L, ExperiencePoints.atLevel(24));
        assertEquals(1_628L, ExperiencePoints.atLevel(32));
        assertEquals(2_920L, ExperiencePoints.atLevel(40));
    }

    @Test void partialBarsRoundToTheNearestWholePoint() {
        assertEquals(0L, ExperiencePoints.available(0, 0.0F));
        assertEquals(1L, ExperiencePoints.available(0, 0.1F));
        assertEquals(4L, ExperiencePoints.available(0, 0.5F));
        assertEquals(315L + 19L, ExperiencePoints.available(15, 0.5F));
        assertEquals(352L + 21L, ExperiencePoints.available(16, 0.5F));
        assertEquals(1_395L + 56L, ExperiencePoints.available(30, 0.5F));
        assertEquals(1_507L + 61L, ExperiencePoints.available(31, 0.5F));
        assertEquals(ExperiencePoints.atLevel(32), ExperiencePoints.available(31, 1.0F));
    }

    @Test void floatAccumulationKeepsTheLastPointAndAllowsRoundedFullBars() {
        // Native point grants can leave the float just below its exact fraction.
        assertEquals(1L, ExperiencePoints.available(0, Math.nextDown(1.0F / 7.0F)));
        assertEquals(ExperiencePoints.atLevel(40) + 1L,
                ExperiencePoints.available(40, Math.nextDown(1.0F / 202.0F)));
        // Native giveExperiencePoints(10252) from zero produces this near-full bar.
        assertEquals(10_252L, ExperiencePoints.available(63, 0.99999845F));
        // Native exact 112-point grant/debit can leave a tiny positive progress.
        assertEquals(112L, ExperiencePoints.available(8, 2.18E-7F));
        assertEquals(0L, ExperiencePoints.available(0, 1.85E-6F));
    }

    @Test void highLevelsAndPartialBarsSaturateWithoutIntegerOrLongOverflow() {
        assertEquals(5_444_999_821_250_002_220L, ExperiencePoints.atLevel(1_100_000_000));
        assertEquals(9_223_372_031_843_981_383L, ExperiencePoints.atLevel(1_431_655_783));
        assertEquals(9_223_372_031_843_981_383L + 1L,
                ExperiencePoints.available(1_431_655_783, 1.0F / 12_884_901_889L));
        assertEquals(Long.MAX_VALUE, ExperiencePoints.available(1_431_655_783, 0.5F));
        assertEquals(Long.MAX_VALUE, ExperiencePoints.atLevel(1_431_655_784));
        assertEquals(Long.MAX_VALUE, ExperiencePoints.atLevel(Integer.MAX_VALUE));
        assertEquals(Long.MAX_VALUE, ExperiencePoints.available(Integer.MAX_VALUE, 1.0F));
    }

    @Test void invalidInputsCannotCreateNegativePricesOrUnboundedBars() {
        assertThrows(IllegalArgumentException.class, () -> ExperiencePoints.atLevel(-1));
        assertThrows(IllegalArgumentException.class, () -> ExperiencePoints.available(-1, 0.5F));
        assertThrows(IllegalArgumentException.class, () -> ExperiencePoints.canAfford(null, -1));
        assertThrows(IllegalArgumentException.class, () -> ExperiencePoints.spend(null, -1));
        assertEquals(112L, ExperiencePoints.available(8, -1.0F));
        assertEquals(112L, ExperiencePoints.available(8, Float.NaN));
        assertEquals(112L, ExperiencePoints.available(8, Float.NEGATIVE_INFINITY));
        assertEquals(135L, ExperiencePoints.available(8, 2.0F));
        assertEquals(135L, ExperiencePoints.available(8, Float.POSITIVE_INFINITY));
    }
}
