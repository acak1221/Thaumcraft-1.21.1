package thaumcraft.common.lib;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class SpaRulesTest {
    @Test void originalWardDurationUsesPermanentWarpSquareRootAndIntegerDivision(){
        assertEquals(32000,SpaRules.warpWardDuration(0));assertEquals(32000,SpaRules.warpWardDuration(1));
        assertEquals(32000,SpaRules.warpWardDuration(48));assertEquals(28571,SpaRules.warpWardDuration(49));
        assertEquals(20000,SpaRules.warpWardDuration(100));assertEquals(4,SpaRules.warpWardDuration(Integer.MAX_VALUE));
    }
    @Test void classicFluidSourceLightRetainsExtraMissingQuantum(){
        assertEquals(8,SpaRules.pureLight(0));assertEquals(7,SpaRules.pureLight(1));assertEquals(1,SpaRules.pureLight(6));
        assertEquals(0,SpaRules.pureLight(7));assertEquals(0,SpaRules.pureLight(8));
    }
    @Test void originalPostPlacementQuantaNormalizationIsNotLinearDecay(){
        int[] original={0,0,0,0,1,1,3,7};for(int meta=0;meta<original.length;meta++)assertEquals(original[meta],SpaRules.normalizedMetadata(meta));
        assertEquals(8,SpaRules.normalizedMetadata(8));assertEquals(15,SpaRules.normalizedMetadata(15));
    }
}
