package thaumcraft.client;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import thaumcraft.client.renderers.block.GasSurfaceRules;
import thaumcraft.client.renderers.block.OreRenderRules;
import thaumcraft.client.renderers.block.TaintOverlayRules;
import thaumcraft.common.blocks.BlockCosmeticStairs;
import thaumcraft.common.lib.world.TaintSpreadRules;

/** Oracle values only. No block instances or registry mutations. Authored, not executed. */
class BlockRendererFinalParityTest {
    @Test void oreUsesPackedIntegerMinimumNotFullbright() {
        assertEquals(160,OreRenderRules.overlayLight(0));
        assertEquals(160,OreRenderRules.overlayLight(159));
        assertEquals(176,OreRenderRules.overlayLight(176));
        // Do NOT clamp the low component independently: original uses Math.max(bb,160).
        assertEquals(0x00f00000,OreRenderRules.overlayLight(0x00f00000));
    }
    @Test void stoneInsetIsAnisotropicFilteringWorkaroundNotFastGraphics() {
        assertFalse(OreRenderRules.stonePass(1));
        assertTrue(OreRenderRules.stonePass(2));
        assertTrue(OreRenderRules.stonePass(16));
        assertEquals(.005F,OreRenderRules.STONE_MIN);
        assertEquals(.995F,OreRenderRules.STONE_MAX);
    }
    @Test void taintCubeOverlayUsesOnlyCrustAndSoilAndOppositeSide() {
        assertTrue(TaintOverlayRules.applies(0));assertTrue(TaintOverlayRules.applies(1));
        assertFalse(TaintOverlayRules.applies(2));assertFalse(TaintOverlayRules.applies(-1));
        assertEquals(200,TaintOverlayRules.BRIGHTNESS);
        assertArrayEquals(new int[]{1,0,3,2,5,4},java.util.stream.IntStream.range(0,6).map(TaintOverlayRules::textureSide).toArray());
        assertEquals(1,TaintSpreadRules.overlay(TaintOverlayRules.textureSide(1),0,53,0));
    }
    @Test void fluidKeepsNegativeForeignSampleAndUndefinedAllNegativeCorner() {
        assertEquals(-.109375F,GasSurfaceRules.heightForRender(false,false,0,true,false,-1));
        assertTrue(Float.isNaN(GasSurfaceRules.heightAverage(new float[]{-.109375F,-.109375F,-.109375F,-.109375F})));
        assertEquals(.875F,GasSurfaceRules.heightForRender(true,false,7,false,false,8));
        assertEquals(.5F,GasSurfaceRules.bottomLight(false));assertEquals(1F,GasSurfaceRules.bottomLight(true));
        assertEquals(.8F,GasSurfaceRules.sideLight(0));assertEquals(.8F,GasSurfaceRules.sideLight(1));
        assertEquals(.6F,GasSurfaceRules.sideLight(2));assertEquals(.6F,GasSurfaceRules.sideLight(3));
    }
    @Test void stairsChooseEveryFaceIndependentlyAtOrigin() {
        assertArrayEquals(new int[]{3,0,1,2,3,0},java.util.stream.IntStream.range(0,6)
                .map(side->BlockCosmeticStairs.textureIndex(0,0,0,side)).toArray());
    }
}
