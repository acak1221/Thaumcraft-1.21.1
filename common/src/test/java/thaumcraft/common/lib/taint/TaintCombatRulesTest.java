package thaumcraft.common.lib.taint;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/** Official1.7 rv/rs strength/weakness values, independently distinguishable from modern potion attributes. */
class TaintCombatRulesTest {
    @Test void oldPotionOperationsAreNotTheModernAdditiveValues(){
        assertEquals(1.3D,TaintCombatRules.strengthModifier(0));assertEquals(2.6D,TaintCombatRules.strengthModifier(1));
        assertEquals(-.5D,TaintCombatRules.weaknessModifier(0));assertEquals(-1.5D,TaintCombatRules.weaknessModifier(2));
        assertEquals(19.1F,TaintCombatRules.tentacleExtras((float)(7*2.3D),0,-1));
        assertEquals(4.5F,TaintCombatRules.tentacleExtras(6.5F,-1,0));
        assertEquals(0x417f3334,Float.floatToRawIntBits(TaintCombatRules.tentacleExtras((float)(6.5D*2.3D),0,0)));
    }
    @Test void nativeAttributeClampPrecedesSeparateOriginalJavaShiftBonuses(){
        assertEquals(7,TaintCombatRules.tentacleExtras(7,-1,-1));
        assertEquals(13,TaintCombatRules.tentacleExtras(7,1,-1));assertEquals(-1,TaintCombatRules.tentacleExtras(7,-1,2));
        assertEquals(10,TaintCombatRules.tentacleExtras(7,32,-1));
        assertEquals(-2147483648F,TaintCombatRules.tentacleExtras(0,31,-1));
        assertEquals(7,TaintCombatRules.tentacleExtras(7,-1,31));
    }
}
