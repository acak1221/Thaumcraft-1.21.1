package thaumcraft.research;

import static org.junit.jupiter.api.Assertions.*;
import java.util.List;
import org.junit.jupiter.api.Test;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.common.lib.research.PrimalCharmRules;

class PrimalCharmRulesTest {
    @Test void literalTwentyOfTwentyThousandGateHasNoBoundaryOverlapWithClueFortyTwo() {
        assertEquals(19,PrimalCharmRules.worldRoll(bound->{assertEquals(20000,bound);return 19;}));
        assertTrue(PrimalCharmRules.emits(0));assertTrue(PrimalCharmRules.emits(19));assertFalse(PrimalCharmRules.emits(20));assertFalse(PrimalCharmRules.emits(42));assertFalse(PrimalCharmRules.emits(19999));
    }
    @Test void originalPrimalDrawIsUniformAirEarthFireWaterOrderEntropyNotRegistryOrder() {
        var expected=List.of(Aspect.AIR,Aspect.EARTH,Aspect.FIRE,Aspect.WATER,Aspect.ORDER,Aspect.ENTROPY);
        for(int i=0;i<6;i++) {int index=i;assertSame(expected.get(i),PrimalCharmRules.aspect(bound->{assertEquals(6,bound);return index;}));}
    }
    @Test void clueIsOnlyFortyTwoAndRequiresBothMainAndMarkerToBeAbsent() {
        assertTrue(PrimalCharmRules.clues(42,false,false));assertFalse(PrimalCharmRules.clues(41,false,false));assertFalse(PrimalCharmRules.clues(43,false,false));
        assertFalse(PrimalCharmRules.clues(42,true,false));assertFalse(PrimalCharmRules.clues(42,false,true));assertFalse(PrimalCharmRules.clues(42,true,true));
    }
    @Test void tooltipUsesIdentityHashAndWholeSixSecondBucketAndPreservesIntOverflow() {
        assertEquals(-1,PrimalCharmRules.tooltip(0,0));assertEquals(-1,PrimalCharmRules.tooltip(42,0));assertEquals(-1,PrimalCharmRules.tooltip(42,119));
        assertEquals(-1,PrimalCharmRules.tooltip(42,120));
        assertEquals(1,PrimalCharmRules.tooltip(6,0));assertEquals(1,PrimalCharmRules.tooltip(6,119));assertEquals(-1,PrimalCharmRules.tooltip(6,120));
        assertEquals(0,PrimalCharmRules.tooltip(22,0));assertEquals(2,PrimalCharmRules.tooltip(41,0));assertEquals(3,PrimalCharmRules.tooltip(30,0));assertEquals(4,PrimalCharmRules.tooltip(14,0));
        assertEquals(1,PrimalCharmRules.tooltip(Integer.MAX_VALUE,240)); // Widening before addition incorrectly returns3.
        assertEquals(PrimalCharmRules.tooltip(Integer.MIN_VALUE,0),PrimalCharmRules.tooltip(Integer.MAX_VALUE,120));
    }
}
