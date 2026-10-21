package thaumcraft.taint;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;
import thaumcraft.common.lib.world.TaintBlockRules;

/**
 * The numbers of the original taint blocks.
 *
 * <p>Original {@code BlockTaint:248-259} overrides the HARDNESS per metadata, not the
 * light: 1.75 for the crust, 1.5 for the soil and 0.2 for the flesh. The original
 * taint block emits no light at all; the fibres carry the light 8 and 10
 * ({@code BlockTaintFibres:96-104}). The 1.7.10 blast resistance setter value 10 is
 * stored as 30 and the vanilla getter divides it by five, so the effective value
 * is 6.</p>
 */
class TaintBlockRulesTest {
    @Test void theCrustTheSoilAndTheFleshKeepTheirOriginalHardness() {
        assertEquals(1.75F, TaintBlockRules.taintHardness(0));
        assertEquals(1.5F, TaintBlockRules.taintHardness(1));
        assertEquals(0.2F, TaintBlockRules.taintHardness(2));
        assertEquals(2.0F, TaintBlockRules.taintHardness(3));
    }

    @Test void theTaintBlockKeepsTheEffectiveBlastResistanceOfTheOriginal() {
        assertEquals(6.0F, TaintBlockRules.TAINT_RESISTANCE);
        assertEquals(2.0F, TaintBlockRules.TAINT_FAMILY_HARDNESS);
    }

    @Test void theFibresKeepTheOriginalHardnessResistanceAndLight() {
        assertEquals(1.0F, TaintBlockRules.FIBRES_HARDNESS);
        assertEquals(3.0F, TaintBlockRules.FIBRES_RESISTANCE);
        assertEquals(8, TaintBlockRules.fibreLight(2));
        assertEquals(10, TaintBlockRules.fibreLight(4));
        assertEquals(0, TaintBlockRules.fibreLight(0));
        assertEquals(0, TaintBlockRules.fibreLight(1));
        assertEquals(0, TaintBlockRules.fibreLight(3));
    }
}
