package thaumcraft.world;

import static org.junit.jupiter.api.Assertions.*;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.IntUnaryOperator;
import org.junit.jupiter.api.Test;
import thaumcraft.common.lib.world.TaintSpreadRules;

class TaintSpreadRulesTest {
    private static int draw(List<Integer> bounds,int... values){
        AtomicInteger index=new AtomicInteger();return TaintSpreadRules.fibreType(bound->{bounds.add(bound);return values[index.getAndIncrement()];},()->true,()->true);
    }
    @Test void branchThresholdsRetainBothOriginalTenGatesAndTwelveTail(){
        var bounds=new ArrayList<Integer>();assertEquals(0,draw(bounds,1));assertEquals(List.of(10),bounds);
        bounds.clear();assertEquals(1,draw(bounds,0,8));assertEquals(List.of(10,10),bounds);
        bounds.clear();assertEquals(2,draw(bounds,0,9,9));assertEquals(List.of(10,10,12),bounds);
        bounds.clear();assertEquals(3,draw(bounds,0,9,10));assertEquals(List.of(10,10,12),bounds);
        bounds.clear();assertEquals(3,draw(bounds,0,9,11));assertEquals(List.of(10,10,12),bounds);
    }
    @Test void eligibilityQueriesShortCircuitAfterFirstLottery(){
        var calls=new ArrayList<String>();IntUnaryOperator random=bound->{calls.add("rng"+bound);return 0;};
        assertEquals(0,TaintSpreadRules.fibreType(random,()->{calls.add("air");return false;},()->{fail("No floor query after blocked air");return true;}));assertEquals(List.of("rng10","air"),calls);
        calls.clear();assertEquals(0,TaintSpreadRules.fibreType(random,()->{calls.add("air");return true;},()->{calls.add("floor");return false;}));assertEquals(List.of("rng10","air","floor"),calls);
    }
    @Test void originalCollisionAndWalkingExposureRatesRemainDistinct(){
        assertEquals(1000,TaintSpreadRules.poisonBound(true,true));assertEquals(500,TaintSpreadRules.poisonBound(true,false));
        assertEquals(100,TaintSpreadRules.poisonBound(false,true));assertEquals(20,TaintSpreadRules.poisonBound(false,false));
        assertEquals(80,TaintSpreadRules.poisonDuration(true));assertEquals(160,TaintSpreadRules.poisonDuration(false));
    }
    @Test void representableBiomeBoundsKeepOriginalIntScheduleAndOverflowUsesUnbiasedLong(){
        var bounds=new ArrayList<Integer>();
        assertTrue(TaintSpreadRules.biomeChance(200,bound->{bounds.add(bound);return 0;},()->{fail("Default cannot consume long RNG");return 0;}));assertEquals(List.of(1000),bounds);
        bounds.clear();assertFalse(TaintSpreadRules.biomeChance(429496729,bound->{bounds.add(bound);return bound-1;},()->{fail("Largest valid original bound still uses int RNG");return 0;}));assertEquals(List.of(2147483645),bounds);
        assertTrue(TaintSpreadRules.biomeChance(429496730,bound->{fail("Overflowing original multiplication is never a native int RNG bound");return 1;},()->0L));
        assertFalse(TaintSpreadRules.biomeChance(Integer.MAX_VALUE,bound->{fail("No wrapped int bound");return 0;},()->2L));
        AtomicInteger calls=new AtomicInteger();assertTrue(TaintSpreadRules.biomeChance(Integer.MAX_VALUE,bound->{fail("No int draw");return 0;},()->calls.getAndIncrement()==0?-1L:0L));assertEquals(2,calls.get(),"Reject incomplete final range before returning a uniform zero");
        assertFalse(TaintSpreadRules.biomeChance(0,bound->{fail("Disabled biome spread consumes no RNG");return 0;},()->{fail("No long RNG");return 0;}));
    }
    @Test void renderHashUsesIntOverflowBeforeWideningAndNoWorldRandom(){
        assertArrayEquals(new double[]{-.25D,-.25D},TaintSpreadRules.grassOffset(0,0,0));
        assertArrayEquals(new double[]{.016666680574417114D,.050000011920928955D},TaintSpreadRules.grassOffset(1,2,3));
        assertArrayEquals(new double[]{-.11666665971279144D,-.01666666567325592D},TaintSpreadRules.grassOffset(123456789,70,-9123456));
        // x*z wraps in the original int expression before the java.util.Random seed widens.
        assertEquals(TaintSpreadRules.overlay(5,65536,71,65536),TaintSpreadRules.overlay(5,0,71,0));
        assertEquals(0,TaintSpreadRules.overlay(0,0,0,0));assertEquals(1,TaintSpreadRules.overlay(0,0,53,0));
        assertEquals(3,TaintSpreadRules.overlay(0,0,64,0));assertEquals(2,TaintSpreadRules.overlay(0,0,68,0));
    }
}
