package thaumcraft.runic;

import java.util.Map;
import org.junit.jupiter.api.Test;
import thaumcraft.common.lib.runic.RunicRules;
import static org.junit.jupiter.api.Assertions.*;

class RunicRulesTest {
    @Test void augmentationPricesAndComponentsUseFinalChargeInsteadOfHardeningAlone() {
        assertEquals(Map.of("tutamen",16,"praecantatio",16,"potentia",32), RunicRules.aspects(0));
        assertEquals(Map.of("tutamen",32,"praecantatio",32,"potentia",64), RunicRules.aspects(1));
        assertEquals(7, RunicRules.instability(4)); assertEquals(6, RunicRules.componentCount(4));
        assertEquals(21, RunicRules.instability(33)); assertEquals(35, RunicRules.componentCount(33));
        assertEquals(1, RunicRules.vis(-5)); assertEquals(0, RunicRules.vis(-6));
    }
    @Test void originalSignedByteWrapIsPreservedAndNegativeChargeUsesJavaDivision() {
        assertEquals(-128, RunicRules.nextHardening(127)); assertEquals(0, RunicRules.nextHardening(-1));
        assertEquals(5, RunicRules.instability(-1)); assertEquals(4, RunicRules.instability(-2));
        assertEquals(2, RunicRules.componentCount(-7));
    }
    @Test void positiveDemandNeverDisappearsAtTheReproducedOriginalIntOverflowBoundary() {
        var boundary = RunicRules.aspects(25);
        assertEquals(2147483648L, boundary.values().stream().mapToLong(Integer::longValue).sum());
        assertEquals(Integer.MIN_VALUE, boundary.values().stream().reduce(0, Integer::sum));
        assertTrue(RunicRules.hasDemand(boundary));
        var saturated = RunicRules.aspects(26);
        assertEquals(Integer.MAX_VALUE, saturated.get("potentia"));
        assertEquals(-3, saturated.values().stream().reduce(0, Integer::sum));
        assertTrue(RunicRules.hasDemand(saturated));
        assertFalse(RunicRules.hasDemand(Map.of("tutamen",0,"praecantatio",0,"potentia",0)));
    }
    @Test void unsafeOriginalRandomBoundsAreRejectedAtTheRealInstabilityBoundary() {
        assertTrue(RunicRules.safeInstability(RunicRules.instability(39)));
        assertEquals(2,50-2*RunicRules.instability(39));
        assertFalse(RunicRules.safeInstability(RunicRules.instability(40)));
        assertEquals(0,50-2*RunicRules.instability(40));
        assertThrows(IllegalArgumentException.class,()->new java.util.Random(0).nextInt(50-2*RunicRules.instability(40)));
    }
    @Test void fractionalDamageTruncatesRemainingChargeAndOverflowDamagePassesThrough() {
        assertEquals(new RunicRules.Hit(0,0), RunicRules.absorb(1,.5F));
        assertEquals(new RunicRules.Hit(3,0), RunicRules.absorb(5,1.25F));
        assertEquals(new RunicRules.Hit(0,2.5F), RunicRules.absorb(5,7.5F));
        assertEquals(new RunicRules.Hit(0,0), RunicRules.absorb(5,5));
    }
}
