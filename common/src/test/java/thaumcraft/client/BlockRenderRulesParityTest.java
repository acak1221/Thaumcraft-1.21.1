package thaumcraft.client;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.Test;
import thaumcraft.client.renderers.block.CandleDripRules;
import thaumcraft.client.renderers.block.GasSurfaceRules;
import thaumcraft.client.renderers.block.OreOverlayRules;

/**
 * The original numbers of the candle, the flux gas and the infused ore.
 *
 * <p>Every expectation is taken from the 4.2.3.5 oracle, not from the port:
 * {@code BlockCandleRenderer.java:33-71}, {@code BlockGasRenderer.java:11-66} and
 * {@code BlockCustomOreRenderer.java:40-63}. No {@code Block} is built here: the
 * rules are pure, so the suite stays a plain JUnit test.
 */
class BlockRenderRulesParityTest {
    private static final float EPS = 1.0E-6F;

    // ---------------------------------------------------------------- the candle

    @Test void theCandleBodyAndStubAreTheOriginalTwoBoxes() {
        // BlockCandleRenderer.java:44 and :68-69: W6..W10 x 0..W8, and 0.475..0.525 x W8..W10.
        assertEquals(0.375F, CandleDripRules.BODY.minX(), EPS);
        assertEquals(0.0F, CandleDripRules.BODY.minY(), EPS);
        assertEquals(0.375F, CandleDripRules.BODY.minZ(), EPS);
        assertEquals(0.625F, CandleDripRules.BODY.maxX(), EPS);
        assertEquals(0.5F, CandleDripRules.BODY.maxY(), EPS);
        assertEquals(0.625F, CandleDripRules.BODY.maxZ(), EPS);
        assertEquals(0.475F, CandleDripRules.STUB.minX(), EPS);
        assertEquals(0.5F, CandleDripRules.STUB.minY(), EPS);
        assertEquals(0.475F, CandleDripRules.STUB.minZ(), EPS);
        assertEquals(0.525F, CandleDripRules.STUB.maxX(), EPS);
        assertEquals(0.625F, CandleDripRules.STUB.maxY(), EPS);
        assertEquals(0.525F, CandleDripRules.STUB.maxZ(), EPS);
    }

    @Test void oneWaxDripOfTheOriginAtTheOrigin() {
        // BlockCandleRenderer.java:45-62 with seed x + y * z = 0: one drip, even index,
        // side = true, loc = 2, height = W1 * (1 + 0).
        CandleDripRules.Candle candle = CandleDripRules.candle(0, 0, 0);
        assertEquals(1, candle.drips().size());
        assertBox(candle.drips().get(0), 0.4375F, 0.0F, 0.3125F, 0.5F, 0.0625F, 0.375F);
    }

    @Test void theSeedIsTheOriginalSumOfThePosition() {
        // x + y * z = 100 + (-3) * 42 = -26: three drips. The second one is the odd
        // branch, where the length is the x extent and the width the z extent.
        CandleDripRules.Candle candle = CandleDripRules.candle(100, -3, 42);
        List<CandleDripRules.Box> drips = candle.drips();
        assertEquals(3, drips.size());
        assertBox(drips.get(0), 0.4375F, 0.0F, 0.625F, 0.5F, 0.125F, 0.6875F);
        assertBox(drips.get(1), 0.3125F, 0.0F, 0.5F, 0.375F, 0.0625F, 0.5625F);
        assertBox(drips.get(2), 0.4375F, 0.0F, 0.625F, 0.5F, 0.1875F, 0.6875F);
    }

    @Test void theLargestAndTheSmallestDripCountsAreOneAndFive() {
        assertEquals(CandleDripRules.MAX_DRIPS, CandleDripRules.candle(-94, 0, -100).drips().size());
        assertEquals(CandleDripRules.MIN_DRIPS, CandleDripRules.candle(1, 0, 0).drips().size());
        // BlockCandleRenderer.java:46-57: the height is W1 * (1 + nextInt(3)), so one of
        // the three sixteenths, and the drips stay inside the original block box.
        for (int x = -40; x <= 40; ++x) {
            for (CandleDripRules.Box drip : CandleDripRules.candle(x, 3, 7).drips()) {
                assertTrue(drip.minX() >= 0.0F && drip.maxX() <= 1.0F, "x of " + drip);
                assertTrue(drip.minZ() >= 0.0F && drip.maxZ() <= 1.0F, "z of " + drip);
                assertEquals(0.0F, drip.minY(), EPS);
                float height = drip.maxY();
                assertTrue(Math.abs(height - 0.0625F) < EPS || Math.abs(height - 0.125F) < EPS
                        || Math.abs(height - 0.1875F) < EPS, "height of " + drip);
            }
        }
    }

    @Test void everyDripIsSixteenthsOfTheOriginal() {
        // The original loc is 2 or 3, so the x (or z) offset starts at two sixteenths.
        for (int x = 0; x < 64; ++x) {
            for (CandleDripRules.Box drip : CandleDripRules.candle(x, 1, 1).drips()) {
                assertTrue(onSixteenth(drip.minX()) && onSixteenth(drip.maxX()), "x of " + drip);
                assertTrue(onSixteenth(drip.minZ()) && onSixteenth(drip.maxZ()), "z of " + drip);
            }
        }
    }

    // ------------------------------------------------------------- the flux gas

    @Test void theEightQuantaOfTheFiniteFluidAreTheOriginalHeights() {
        // getFluidHeightForRender: the same fluid with a liquid below is full, metadata
        // seven is 0.875, and the otherwise ordinary quanta are (meta + 1) / 8 * 0.875.
        assertEquals(1.0F, GasSurfaceRules.heightForRender(true, true, 0, false, false, 1), EPS);
        assertEquals(0.875F, GasSurfaceRules.heightForRender(true, false, 7, true, false, 8), EPS);
        assertEquals(0.4375F, GasSurfaceRules.heightForRender(true, false, 3, true, false, 4), EPS);
        assertEquals(0.109375F, GasSurfaceRules.heightForRender(true, false, 0, true, false, 1), EPS);
        assertEquals(1.0F, GasSurfaceRules.heightForRender(false, false, 0, false, true, 0), EPS);
        assertEquals(0.0F, GasSurfaceRules.heightForRender(false, false, 0, false, false, 0), EPS);
        // A foreign block samples the original minus one, the negative ignored by the average.
        assertEquals(-0.875F, GasSurfaceRules.heightForRender(false, false, 0, false, false, -1), EPS);
    }

    @Test void theAverageKeepsTheLastOverrideUnlessAFullOneWasSeen() {
        // getFluidHeightAverage: the last sample at or above 0.875 wins, a 1.0 is sticky.
        assertEquals(0.9F, GasSurfaceRules.heightAverage(new float[]{0.875F, 0.9F, 0.8F, 0.7F}), EPS);
        assertEquals(1.0F, GasSurfaceRules.heightAverage(new float[]{1.0F, 0.9F, 0.8F, 0.7F}), EPS);
        assertEquals(0.9F, GasSurfaceRules.heightAverage(new float[]{0.1F, 0.2F, 0.3F, 0.9F}), EPS);
        // Without an override the mean of the non negative samples is used.
        assertEquals(0.5F, GasSurfaceRules.heightAverage(new float[]{0.2F, 0.4F, 0.8F, 0.6F}), EPS);
        // Negative samples are ignored and an all negative corner is zero.
        assertEquals(0.3F, GasSurfaceRules.heightAverage(new float[]{-1.0F, -1.0F, 0.4F, 0.2F}), EPS);
        assertEquals(0.0F, GasSurfaceRules.heightAverage(new float[]{-1.0F, -1.0F, -1.0F, -1.0F}), EPS);
    }

    @Test void theFourCornersAreTheOriginalNeighbourhoodAverages() {
        // BlockGasRenderer.java:95-113, flow00 = flow[0] (x - 1, z - 1).
        float[] flow = {0.2F, 0.4F, 0.6F, 0.8F, 0.9F, 0.3F, 0.5F, 0.7F, 1.0F};
        GasSurfaceRules.Corners corners = GasSurfaceRules.corners(flow);
        assertEquals(0.9F, corners.northWest(), EPS);
        assertEquals(0.9F, corners.southWest(), EPS);
        assertEquals(1.0F, corners.southEast(), EPS);
        assertEquals(0.9F, corners.northEast(), EPS);
    }

    @Test void theGasRisesAndTheGooDoesNot() {
        assertTrue(GasSurfaceRules.rises(1));
        assertFalse(GasSurfaceRules.rises(-1));
    }

    @Test void theOriginalLightingFactorsAreTheOnesOfTheTessellator() {
        // BlockGasRenderer.java:11-15 and :139-153, :200-204.
        assertEquals(0.5F, GasSurfaceRules.LIGHT_Y_NEG, EPS);
        assertEquals(1.0F, GasSurfaceRules.LIGHT_Y_POS, EPS);
        assertEquals(0.8F, GasSurfaceRules.LIGHT_XZ_NEG, EPS);
        assertEquals(0.6F, GasSurfaceRules.LIGHT_XZ_POS, EPS);
        assertEquals(0.001F, GasSurfaceRules.RENDER_OFFSET, EPS);
        assertEquals(0.8F, GasSurfaceRules.sideLight(0), EPS);
        assertEquals(0.8F, GasSurfaceRules.sideLight(1), EPS);
        assertEquals(0.6F, GasSurfaceRules.sideLight(2), EPS);
        assertEquals(0.6F, GasSurfaceRules.sideLight(3), EPS);
        assertEquals(0.5F, GasSurfaceRules.bottomLight(false), EPS);
        assertEquals(1.0F, GasSurfaceRules.bottomLight(true), EPS);
        assertEquals(1.0F, GasSurfaceRules.topLight(), EPS);
    }

    // ------------------------------------------------------------- the ore

    @Test void theOverlayLightIsFlooredAtTheOriginalOneSixty() {
        assertEquals(160, OreOverlayRules.overlayLight(0));
        assertEquals(160, OreOverlayRules.overlayLight(159));
        assertEquals(160, OreOverlayRules.overlayLight(160));
        assertEquals(161, OreOverlayRules.overlayLight(161));
        assertEquals(240, OreOverlayRules.overlayLight(240));
    }

    @Test void theSecondStonePassBelongsToTheMinimalParticleSetting() {
        // BlockCustomOreRenderer.java:57 reads field_151443_J = particleSetting.
        assertFalse(OreOverlayRules.secondPass(0));
        assertFalse(OreOverlayRules.secondPass(1));
        assertTrue(OreOverlayRules.secondPass(2));
        assertTrue(OreOverlayRules.secondPass(3));
    }

    @Test void theSecondPassAndTheInventoryUseTheSameFiveThousandthsBox() {
        assertEquals(0.005F, OreOverlayRules.insetMin(), EPS);
        assertEquals(0.995F, OreOverlayRules.insetMax(), EPS);
    }

    // ------------------------------------------------------------- helpers

    private static void assertBox(CandleDripRules.Box box, float minX, float minY, float minZ,
                                  float maxX, float maxY, float maxZ) {
        assertEquals(minX, box.minX(), EPS, "minX");
        assertEquals(minY, box.minY(), EPS, "minY");
        assertEquals(minZ, box.minZ(), EPS, "minZ");
        assertEquals(maxX, box.maxX(), EPS, "maxX");
        assertEquals(maxY, box.maxY(), EPS, "maxY");
        assertEquals(maxZ, box.maxZ(), EPS, "maxZ");
    }

    private static boolean onSixteenth(float value) {
        float sixteenths = value * 16.0F;
        return Math.abs(sixteenths - Math.round(sixteenths)) < 1.0E-4F;
    }
}
