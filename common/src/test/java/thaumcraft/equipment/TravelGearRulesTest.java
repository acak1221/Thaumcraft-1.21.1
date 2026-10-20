package thaumcraft.equipment;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import thaumcraft.common.items.armor.TravelGearRules;

class TravelGearRulesTest {
    @Test void lastFuelPaysAClosingFrameAtExactOriginalBoundary() {
        for(boolean girdle:new boolean[]{false,true}) {
            int steps=girdle?288:360,fuel=1,charge=0;
            for(int tick=1;tick<=steps;tick++) {var paid=TravelGearRules.expend(fuel,charge,girdle);fuel=paid.fuel();charge=paid.charge();assertTrue(paid.sustained());assertFalse(paid.consumed());assertEquals(1,fuel);assertEquals(tick,charge);}
            var finalFrame=TravelGearRules.expend(fuel,charge,girdle);assertEquals(0,finalFrame.fuel());assertEquals(0,finalFrame.charge());assertTrue(finalFrame.consumed());assertFalse(finalFrame.sustained());
            assertEquals(new TravelGearRules.Expenditure(0,0,false,false),TravelGearRules.expend(0,0,girdle));
        }
    }
    @Test void chargeRemainsAnOuterHarnessCounterAcrossJarOrGirdleChanges() {
        assertEquals(new TravelGearRules.Expenditure(1,0,true,true),TravelGearRules.expend(2,360,false));
        assertEquals(new TravelGearRules.Expenditure(1,0,true,true),TravelGearRules.expend(2,300,true));
        assertEquals(new TravelGearRules.Expenditure(0,173,false,false),TravelGearRules.expend(0,173,false));
        for(int invalid:new int[]{-1,361,Integer.MAX_VALUE})assertThrows(IllegalArgumentException.class,()->TravelGearRules.expend(1,invalid,false));
        for(int invalid:new int[]{-1,65,Integer.MAX_VALUE})assertThrows(IllegalArgumentException.class,()->TravelGearRules.expend(invalid,0,false));
    }
    @Test void originalFloatMovementAndUnclampedFallArePreserved() {
        assertEquals(.055F,TravelGearRules.groundAcceleration(false));assertEquals(.055F/4F,TravelGearRules.groundAcceleration(true));
        assertEquals(.05F,TravelGearRules.airAcceleration(false));assertEquals(.03F,TravelGearRules.airAcceleration(true));assertEquals((double).275F,TravelGearRules.jumpBonus());
        assertEquals(-.125F,TravelGearRules.afterBootFall(.125F));assertEquals(-.5F,TravelGearRules.afterBootFall(-.5F));
        assertEquals(.7F,TravelGearRules.damping(0,false));assertEquals(Float.intBitsToFloat(0x3f68f5c2),TravelGearRules.damping(0,true));assertEquals(1F,TravelGearRules.damping(3,true));
    }
}
