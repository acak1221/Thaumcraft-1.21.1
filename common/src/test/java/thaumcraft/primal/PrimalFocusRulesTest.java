package thaumcraft.common.lib.primal;

import static org.junit.jupiter.api.Assertions.*;
import java.util.List;
import org.junit.jupiter.api.Test;

class PrimalFocusRulesTest {
    @Test void originalCostDrawOrderAndTwoHundredMillisecondClock(){
        var zero=new PrimalFocusRules.Costs(50,200,250,150,50,200);
        assertEquals(zero,PrimalFocusRules.costAtMillis(0));assertEquals(zero,PrimalFocusRules.costAtMillis(199));
        assertEquals(new PrimalFocusRules.Costs(50,200,150,200,250,250),PrimalFocusRules.costAtMillis(200));
        assertEquals(new PrimalFocusRules.Costs(200,50,50,200,150,200),PrimalFocusRules.costAtMillis(1700000000000L));
        assertEquals(zero,PrimalFocusRules.costAtMillis(-199)); // Java division toward zero, not floorDiv.
        assertEquals(new PrimalFocusRules.Costs(200,50,250,250,250,200),PrimalFocusRules.costAtMillis(-200));
    }
    @Test void allActualPrimalCostsRemainIndependentRawCentivis(){
        for(long bucket=0;bucket<2000;bucket++){
            var c=PrimalFocusRules.costAtMillis(bucket*200L);
            for(int amount:List.of(c.aqua(),c.aer(),c.terra(),c.ignis(),c.ordo(),c.perditio()))assertTrue(amount>=50&&amount<=250&&amount%50==0);
        }
    }
    @Test void deterministicMotionUsesOriginalIdPlusCounterIntOverflow(){
        var first=PrimalFocusRules.jitter(123,21);
        assertEquals(0x3bd5ff21,Float.floatToIntBits((float)first.x()));assertEquals(0x3bbd3552,Float.floatToIntBits((float)first.y()));assertEquals(0xba515166,Float.floatToIntBits((float)first.z()));
        var wrapped=PrimalFocusRules.jitter(Integer.MAX_VALUE,21);
        assertEquals(0x3b20dfb3,Float.floatToIntBits((float)wrapped.x()));assertEquals(0xbb676759,Float.floatToIntBits((float)wrapped.y()));assertEquals(0x3a9e8ad7,Float.floatToIntBits((float)wrapped.z()));
    }
    @Test void seekerKeepsSquaredDistanceAndFloatClamp(){
        assertEquals(0x3fb99999a0000000L,Double.doubleToLongBits(PrimalFocusRules.seekerAxis(.05,4,16)));
        assertEquals((double).2F,PrimalFocusRules.seekerAxis(.3,1,1));assertEquals((double)-.2F,PrimalFocusRules.seekerAxis(-.3,-1,1));
        assertTrue(Double.isNaN(PrimalFocusRules.seekerAxis(0,0,0)),"zero-distance original NaN remains visible for the entity's finite-motion terminal guard");
    }
    @Test void specialChanceIsInclusiveAndTaintOffsetsRemainAsymmetric(){
        int ordinary=0,portalBlock=0;
        for(int roll=0;roll<100;roll++){if(PrimalFocusRules.special(roll,false))ordinary++;if(PrimalFocusRules.special(roll,true))portalBlock++;}
        assertEquals(2,ordinary);assertEquals(11,portalBlock);
        assertEquals(-5,PrimalFocusRules.taintOffset(.2F,.9F));assertEquals(0,PrimalFocusRules.taintOffset(.9F,0));
    }
}
