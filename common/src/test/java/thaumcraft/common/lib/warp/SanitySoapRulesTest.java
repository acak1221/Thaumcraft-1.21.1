package thaumcraft.common.lib.warp;

import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;
import thaumcraft.common.lib.research.PlayerKnowledge;
import static org.junit.jupiter.api.Assertions.*;

class SanitySoapRulesTest {
    @Test void releaseBoundaryAndBonusesAreOriginalFloatValues(){
        assertFalse(SanitySoapRules.finished(5));assertTrue(SanitySoapRules.finished(4));assertTrue(SanitySoapRules.finished(0));
        assertEquals(.33F,SanitySoapRules.chance(false,false));assertEquals(0x3f147ae2,Float.floatToRawIntBits(SanitySoapRules.chance(true,false)));
        assertEquals(0x3f147ae2,Float.floatToRawIntBits(SanitySoapRules.chance(false,true)));assertEquals(0x3f547ae2,Float.floatToRawIntBits(SanitySoapRules.chance(true,true)));
        var boundary=new PlayerKnowledge();boundary.setWarpSticky(1);
        assertEquals(-1,SanitySoapRules.cleanse(boundary,true,true,()->.83F).stickyDelta());
    }
    @Test void removesOneStickyAndAllTemporaryWithoutTouchingPermanentOrWard(){
        var state=new PlayerKnowledge();state.setWarpPerm(9);state.setWarpSticky(3);state.setWarpTemp(40);state.setWarpCounter(100);
        var calls=new AtomicInteger();var result=SanitySoapRules.cleanse(state,false,false,()->{calls.incrementAndGet();return Math.nextDown(.33F);});
        assertEquals(new SanitySoapRules.Cleansing(-1,-40),result);assertTrue(result.changed());assertEquals(1,calls.get());
        assertEquals(9,state.getWarpPerm());assertEquals(2,state.getWarpSticky());assertEquals(0,state.getWarpTemp());assertEquals(11,state.getWarpCounter());
    }
    @Test void strictFailedRollStillClearsTemporaryAndRefreshesOnlyChangedCounter(){
        var state=new PlayerKnowledge();state.setWarpPerm(4);state.setWarpSticky(2);state.setWarpTemp(7);state.setWarpCounter(99);
        assertEquals(new SanitySoapRules.Cleansing(0,-7),SanitySoapRules.cleanse(state,true,true,()->.83000004F));
        assertEquals(2,state.getWarpSticky());assertEquals(6,state.getWarpCounter());
        state.setWarpCounter(99);assertFalse(SanitySoapRules.cleanse(state,true,true,()->.83000004F).changed());assertEquals(99,state.getWarpCounter());
    }
    @Test void unconditionalOneRollSurvivesZeroWarpAndSuccessfulStickyZero(){
        var state=new PlayerKnowledge();state.setWarpCounter(77);var count=new AtomicInteger();
        var result=SanitySoapRules.cleanse(state,false,false,()->{count.incrementAndGet();return 0;});
        assertFalse(result.changed());assertEquals(1,count.get());assertEquals(77,state.getWarpCounter());
    }
}
