package thaumcraft.common.lib.taint;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class TaintacleRulesTest {
    @Test void strictAgitationScalesWithActualHeight(){
        assertTrue(TaintacleRules.agitated(Math.nextDown(441D),3));assertFalse(TaintacleRules.agitated(441,3));
        assertTrue(TaintacleRules.agitated(Math.nextDown(49D),1));assertFalse(TaintacleRules.agitated(49,1));
    }
    @Test void floatFlailRetainsOriginalCrossingsRatherThanClamping(){
        float value=1;for(int i=0;i<10;i++)value=TaintacleRules.flail(value,true);
        assertTrue(value>3);assertEquals(value,TaintacleRules.flail(value,true));
        // Oracle EntityTaintacle:124-125: the flail intensity falls by 0.2f, in float.
        value=1.1F;assertEquals(1.1F-.2F,TaintacleRules.flail(value,false));assertEquals(1.1F-.2F,TaintacleRules.flail(1.1F-.2F,false));
    }
    @Test void RiseUsesIntegerEntityAgeRatherThanInterpolatedRenderTime(){
        // The rise is the remaining share of the height over the ten ticks of the rise, in float.
        assertEquals(3,TaintacleRules.ariseHeight(0,3));assertEquals((30F-29F)/30F*3F,TaintacleRules.ariseHeight(29,3));assertEquals(0,TaintacleRules.ariseHeight(30,3));
        assertEquals(1,TaintacleRules.ariseHeight(0,1));assertEquals((10F-9F)/10F*1F,TaintacleRules.ariseHeight(9,1));assertEquals(0,TaintacleRules.ariseHeight(10,1));
    }
    @Test void originalMeshHasNineOrFiveAnimatedStemJointsAndSeparateHead(){
        var main=TaintacleRules.segments(10,0,1,false,false);var small=TaintacleRules.segments(6,0,1,false,false);
        assertEquals(9,main.size());assertEquals(5,small.size());assertEquals(new TaintacleRules.Segment(0,0),main.getFirst());
        assertTrue(main.get(1).pitch()<0&&main.get(1).roll()<0);
        var agitated=TaintacleRules.segments(10,9,2,true,true);
        var calm=TaintacleRules.segments(10,9,2,false,true);
        assertTrue(agitated.getFirst().pitch()<calm.getFirst().pitch());
    }
}
