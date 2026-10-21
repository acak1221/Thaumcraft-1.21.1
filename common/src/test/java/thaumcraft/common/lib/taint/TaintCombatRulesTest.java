package thaumcraft.common.lib.taint;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/** Official1.7 rv/rs strength/weakness values, independently distinguishable from modern potion attributes. */
class TaintCombatRulesTest {
    @Test void oldPotionOperationsAreNotTheModernAdditiveValues(){
        assertEquals(1.3D*1,TaintCombatRules.strengthModifier(0));assertEquals(1.3D*2,TaintCombatRules.strengthModifier(1));
        assertEquals((double)(-.5F*1F),TaintCombatRules.weaknessModifier(0));assertEquals((double)(-.5F*3F),TaintCombatRules.weaknessModifier(2));
        // Oracle EntityTaintacle:155-162: the tentacle adds three per strength level and takes two per weakness level.
        assertEquals((float)(7*2.3D)+(float)(3<<0),TaintCombatRules.tentacleExtras((float)(7*2.3D),0,-1));
        assertEquals(6.5F-(float)(2<<0),TaintCombatRules.tentacleExtras(6.5F,-1,0));
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
