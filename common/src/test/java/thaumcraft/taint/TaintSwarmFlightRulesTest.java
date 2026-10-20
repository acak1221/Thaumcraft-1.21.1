package thaumcraft.taint;

import org.junit.jupiter.api.Test;
import thaumcraft.common.entities.monster.TaintSwarmFlightRules;
import static org.junit.jupiter.api.Assertions.*;

class TaintSwarmFlightRulesTest {
    @Test void originalZeroBottomPreservesFirstAllowedYOne() {
        assertFalse(TaintSwarmFlightRules.aboveWorldBottom(-1,0));
        assertFalse(TaintSwarmFlightRules.aboveWorldBottom(0,0));
        assertTrue(TaintSwarmFlightRules.aboveWorldBottom(1,0));
    }
    @Test void nativeNegativeBottomAllowsItsOwnFirstLayerWithoutDiscardingBelowzeroCaves() {
        assertFalse(TaintSwarmFlightRules.aboveWorldBottom(-65,-64));
        assertFalse(TaintSwarmFlightRules.aboveWorldBottom(-64,-64));
        assertTrue(TaintSwarmFlightRules.aboveWorldBottom(-63,-64));
        assertTrue(TaintSwarmFlightRules.aboveWorldBottom(-1,-64));
        assertTrue(TaintSwarmFlightRules.aboveWorldBottom(1,-64));
    }
}
