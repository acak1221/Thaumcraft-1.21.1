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
        // Oracle ItemBootsTraveller:78-89: the ground bonus is 0.055f, divided by 4.0f in the water,
        // the air speed is 0.03f or 0.05f and the fall distance loses 0.25f.
        assertEquals(.055F,TravelGearRules.groundAcceleration(false));assertEquals(.055F/4F,TravelGearRules.groundAcceleration(true));
        assertEquals(.05F,TravelGearRules.airAcceleration(false));assertEquals(.03F,TravelGearRules.airAcceleration(true));assertEquals((double).275F,TravelGearRules.jumpBonus());
        assertEquals(.125F-.25F,TravelGearRules.afterBootFall(.125F));assertEquals(-.5F,TravelGearRules.afterBootFall(-.5F));
        // Oracle Hover:97-103: mod = 0.7f + 0.075f * haste, plus 0.21f for the girdle, capped at one.
        assertEquals(.7F+.075F*0,TravelGearRules.damping(0,false));assertEquals(Float.intBitsToFloat(0x3f68f5c2),TravelGearRules.damping(0,true));assertEquals(1F,TravelGearRules.damping(3,true));
    }
}
