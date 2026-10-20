package thaumcraft.warp;

import java.util.ArrayDeque;
import java.util.List;
import java.util.Set;
import java.util.function.IntUnaryOperator;
import org.junit.jupiter.api.Test;
import thaumcraft.common.lib.WarpRules;
import static org.junit.jupiter.api.Assertions.*;

class WarpRulesTest {
    private static final class Draws implements IntUnaryOperator {
        private final ArrayDeque<Integer> values = new ArrayDeque<>(), bounds = new ArrayDeque<>();
        Draws(int[] bounds, int... values) { for (int n : bounds) this.bounds.add(n); for (int n : values) this.values.add(n); }
        @Override public int applyAsInt(int bound) {
            assertEquals(bounds.removeFirst().intValue(), bound, "the original requested RNG bound/order");
            int value = values.removeFirst(); assertTrue(value >= 0 && value < bound); return value;
        }
        void spent() { assertTrue(values.isEmpty()); assertTrue(bounds.isEmpty()); }
    }
    @Test void schedulerIncludesOnlyPositiveTwoThousandMultiplesAndWardSuspendsDecayToo() {
        for (int ticks : new int[]{0, 1, 1999, 2001, -2000}) assertFalse(WarpRules.scheduled(ticks, false, false));
        for (int ticks : new int[]{2000, 4000, 10000}) assertTrue(WarpRules.scheduled(ticks, false, false));
        assertFalse(WarpRules.scheduled(2000, true, false)); assertFalse(WarpRules.scheduled(2000, false, true));
    }
    @Test void firstRollIsConsumedEvenWhenCounterOrWarpRefuses() {
        for (int[] input : new int[][]{{0,100,0},{100,0,0},{100,-5,0},{0,100,-1}}) {
            var draws = new Draws(new int[]{100}, 0);
            var result = WarpRules.decide(input[0],input[1],input[2],true,draws);
            assertFalse(result.triggered()); assertEquals(input[1],result.remainingCounter()); draws.spent();
        }
    }
    @Test void squareRootGateIsInclusiveAndFailureDoesNotRollSeverityOrMask() {
        var accepted = new Draws(new int[]{100,12},4,1);
        assertTrue(WarpRules.decide(10,16,0,false,accepted).triggered()); accepted.spent();
        var refused = new Draws(new int[]{100},5);
        assertFalse(WarpRules.decide(10,16,0,true,refused).triggered()); refused.spent();
    }
    @Test void severityUsesWeightedWarpThenTheMaskAndTheCounterUsesTheOriginalCast() {
        var draws = new Draws(new int[]{100,93,4},10,92,3);
        var result = WarpRules.decide(90,100,0,true,draws);
        assertEquals(93,result.effectiveWarp()); assertEquals(80,result.remainingCounter());
        assertEquals(87,result.severity()); assertEquals(WarpRules.Event.NONE,result.event()); draws.spent();
        var small = new Draws(new int[]{100,1},1,0);
        assertEquals(-4,WarpRules.decide(1,1,0,false,small).remainingCounter()); small.spent();
    }
    @Test void gearParticipatesInSeverityButTheLimitStaysOneHundred() {
        var gear = new Draws(new int[]{100,4},0,3);
        assertEquals(4,WarpRules.decide(0,10,1,false,gear).effectiveWarp()); gear.spent();
        var high = new Draws(new int[]{100,100},0,99);
        assertEquals(100,WarpRules.decide(Integer.MAX_VALUE,Integer.MAX_VALUE,5,false,high).effectiveWarp()); high.spent();
    }
    @Test void everyOriginalFourValueBandAndTheOddSeventySixBranchIsPreserved() {
        for (int i : new int[]{-3,0,5,6,7,8,85,86,87,88}) assertEquals(WarpRules.Event.NONE,WarpRules.event(i));
        for (int i : new int[]{21,24,73,74,75,77,78,79,80}) assertEquals(WarpRules.Event.HUNGER,WarpRules.event(i));
        assertEquals(WarpRules.Event.CLARITY,WarpRules.event(76));
        assertEquals(WarpRules.Event.SPIDERS,WarpRules.event(57)); assertEquals(WarpRules.Event.REAL_SPIDERS,WarpRules.event(92));
        assertEquals(WarpRules.Event.MIST_MANY,WarpRules.event(65)); assertEquals(WarpRules.Event.MIST_MANY,WarpRules.event(99));
    }
    @Test void gatesUseStrictActualWarpAndAreIdempotent() {
        assertEquals(List.of(),WarpRules.unlocks(10,Set.of()));
        assertEquals(List.of("@BATHSALTS"),WarpRules.unlocks(11,Set.of()));
        assertEquals(List.of("@BATHSALTS"),WarpRules.unlocks(25,Set.of()));
        assertEquals(List.of("@BATHSALTS","ELDRITCHMINOR"),WarpRules.unlocks(26,Set.of()));
        assertEquals(List.of("@BATHSALTS","ELDRITCHMINOR"),WarpRules.unlocks(50,Set.of()));
        assertEquals(List.of("@BATHSALTS","ELDRITCHMINOR","ELDRITCHMAJOR"),WarpRules.unlocks(51,Set.of()));
        assertEquals(List.of(),WarpRules.unlocks(99,Set.of("BATHSALTS","ELDRITCHMINOR","ELDRITCHMAJOR")));
        assertEquals(List.of(),WarpRules.unlocks(99,Set.of("@BATHSALTS","ELDRITCHMINOR","ELDRITCHMAJOR")));
    }
    @Test void gazeUsesTheOriginalConeAndStrictRoundCap() {
        assertTrue(WarpRules.inGazeCone(7,7,8)); assertFalse(WarpRules.inGazeCone(8,8,8));
        assertFalse(WarpRules.inGazeCone(-3,3,8)); assertFalse(WarpRules.inGazeCone(0,0,8));
        double cone=Math.cos(.75F/2); assertFalse(WarpRules.inGazeCone(cone,1,8));
        assertTrue(WarpRules.inGazeCone(cone+.00001,1,8));
        assertEquals(8,WarpRules.gazeRange(0)); assertEquals(17,WarpRules.gazeRange(3)); assertEquals(24,WarpRules.gazeRange(100));
    }
}
