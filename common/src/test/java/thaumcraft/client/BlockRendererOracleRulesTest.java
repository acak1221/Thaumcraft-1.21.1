package thaumcraft.client;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import thaumcraft.client.renderers.block.CandleDripRules;
import thaumcraft.client.renderers.block.GasSurfaceRules;
import thaumcraft.client.renderers.block.OreOverlayRules;
import thaumcraft.client.renderers.block.EldritchCrustBakedModel;

/** Pure original-value guards: no registry bootstrap or Block construction. */
class BlockRendererOracleRulesTest {
    @Test void originDripIsThreeQuantaNotOne() {
        // Oracle BlockCandleRenderer:48-54, JDK Random(0): 0,true,0,2.
        var candle = CandleDripRules.candle(0,0,0);
        assertEquals(1,candle.drips().size());
        assertEquals(new CandleDripRules.Box(.4375F,0,.3125F,.5F,.1875F,.375F),candle.drips().getFirst());
        assertEquals(new CandleDripRules.Box(.375F,0,.375F,.625F,.5F,.625F),candle.body());
        assertEquals(new CandleDripRules.Box(.475F,.5F,.475F,.525F,.625F,.525F),candle.stub());
    }
    @Test void positionSeedUsesIntOverflowAndXPlusYTimesZ() {
        assertEquals(CandleDripRules.candle(7,2,3),CandleDripRules.candle(13,0,0));
        assertEquals(CandleDripRules.candle(0,65536,65536),CandleDripRules.candle(0,0,0));
        for(int x=-20;x<20;x++) {
            int count = CandleDripRules.candle(x,64,3).drips().size();
            assertTrue(count>=1&&count<=5);
        }
    }
    @Test void foreignGasSampleAndEmptyAveragePreserveOriginalQuirks() {
        // Oracle BlockGasRenderer:38-65, Forge finite quanta -1 / 8 * .875.
        assertEquals(-.109375F,GasSurfaceRules.heightForRender(false,false,0,true,false,-1));
        assertTrue(Float.isNaN(GasSurfaceRules.heightAverage(new float[]{-1,-1,-1,-1})));
        assertEquals(1F,GasSurfaceRules.heightAverage(new float[]{1,.875F,0,0}));
        assertEquals(.875F,GasSurfaceRules.heightAverage(new float[]{.875F,0,0,0}));
    }
    @Test void gasCornersFollowTheOriginalNineSampleOrder() {
        var corners = GasSurfaceRules.corners(new float[]{0,.1F,.2F,.3F,.4F,.5F,.6F,.7F,.8F});
        assertEquals(.2F,corners.northWest(),.000001F);
        assertEquals(.3F,corners.southWest(),.000001F);
        assertEquals(.6F,corners.southEast(),.000001F);
        assertEquals(.5F,corners.northEast(),.000001F);
        assertEquals(.5F,GasSurfaceRules.bottomLight(false));
        assertEquals(1F,GasSurfaceRules.bottomLight(true));
        assertEquals(.8F,GasSurfaceRules.sideLight(0));
        assertEquals(.6F,GasSurfaceRules.sideLight(2));
    }
    @Test void oreUsesPackedLightMaxAndMinimalParticlesNotFastGraphics() {
        // Oracle BlockCustomOreRenderer:63-69.
        assertEquals(160,OreOverlayRules.overlayLight(0));
        assertEquals(0xF00000,OreOverlayRules.overlayLight(0xF00000));
        assertFalse(OreOverlayRules.secondPass(0));
        assertFalse(OreOverlayRules.secondPass(1));
        assertTrue(OreOverlayRules.secondPass(2));
        assertEquals(.005F,OreOverlayRules.insetMin());
        assertEquals(.995F,OreOverlayRules.insetMax());
    }
    @Test void everyCrustMaskUsesOriginalTwoSixteenthsOnlyOnExposedSides() {
        // Oracle BlockEldritchRenderer:40-64; Direction bits DOWN,UP,NORTH,SOUTH,WEST,EAST.
        for(int mask=0;mask<64;mask++) {
            assertArrayEquals(new float[]{(mask&16)!=0?0:2,(mask&1)!=0?0:2,(mask&4)!=0?0:2,
                    (mask&32)!=0?16:14,(mask&2)!=0?16:14,(mask&8)!=0?16:14},
                    EldritchCrustBakedModel.bounds(mask));
        }
    }
}
