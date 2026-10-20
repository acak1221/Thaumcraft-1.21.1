package thaumcraft.baubles;

import org.junit.jupiter.api.Test;
import thaumcraft.common.lib.runic.RunicRules;
import static org.junit.jupiter.api.Assertions.*;

class RunicAccessoryRulesTest {
    @Test void chargedRingsKeepOriginalSubtractionIncludingNegativeConfiguredBoundary() {
        assertEquals(2000,RunicRules.rechargeInterval(2000,0));assertEquals(1500,RunicRules.rechargeInterval(2000,1));
        assertEquals(1000,RunicRules.rechargeInterval(2000,2));assertEquals(0,RunicRules.rechargeInterval(500,1));
        assertEquals(-500,RunicRules.rechargeInterval(500,2));
    }
}
