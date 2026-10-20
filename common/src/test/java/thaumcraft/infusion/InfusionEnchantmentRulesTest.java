package thaumcraft.infusion;

import java.util.List;
import org.junit.jupiter.api.Test;
import thaumcraft.common.lib.infusion.InfusionEnchantmentRules;
import static org.junit.jupiter.api.Assertions.*;

class InfusionEnchantmentRulesTest {
    @Test void xpIsTheOriginalFirstCostThirdTimesTheNextLevel() {
        assertEquals(1, InfusionEnchantmentRules.xpLevels(1, 0));
        assertEquals(2, InfusionEnchantmentRules.xpLevels(1, 1));
        assertEquals(12, InfusionEnchantmentRules.xpLevels(20, 1));
        assertEquals(18, InfusionEnchantmentRules.xpLevels(20, 2));
        assertEquals(1, InfusionEnchantmentRules.xpLevels(0, 0));
    }
    @Test void everyEnchantmentLevelContributesToInstabilityBeforeIntegerDivision() {
        assertEquals(2, InfusionEnchantmentRules.instability(2, 0));
        assertEquals(2, InfusionEnchantmentRules.instability(2, 1));
        assertEquals(3, InfusionEnchantmentRules.instability(2, 2));
        assertEquals(5, InfusionEnchantmentRules.instability(2, 7));
    }
    @Test void essentiaAddsTheTruncatedSurchargeToTheWholeBaseCost() {
        assertEquals(4, InfusionEnchantmentRules.aspectCost(4, 0));
        assertEquals(8, InfusionEnchantmentRules.aspectCost(4, 1));
        assertEquals(12, InfusionEnchantmentRules.aspectCost(4, 2));
        assertEquals(6, InfusionEnchantmentRules.aspectCost(4, .7F));
        assertEquals(17, InfusionEnchantmentRules.aspectCost(16, .1F));
    }
    @Test void originalFloatEntryAdditionsAreNotReplacedByAnIntegerPercentage() {
        float modifier = InfusionEnchantmentRules.essentiaModifier(0, List.of(1, 3, 3));
        assertEquals(0x3f333334, Float.floatToIntBits(modifier));
        assertEquals(13, InfusionEnchantmentRules.aspectCost(8, modifier));
        assertEquals(29, InfusionEnchantmentRules.aspectCost(8, InfusionEnchantmentRules.essentiaModifier(2, List.of(1, 3, 3))));
    }
    @Test void malformedInputsCannotCreateNegativeCostsOrWraparound() {
        assertThrows(IllegalArgumentException.class, () -> InfusionEnchantmentRules.xpLevels(-1, 0));
        assertThrows(IllegalArgumentException.class, () -> InfusionEnchantmentRules.essentiaModifier(0, List.of(-1)));
        assertThrows(IllegalArgumentException.class, () -> InfusionEnchantmentRules.aspectCost(0, 1));
        assertThrows(IllegalArgumentException.class, () -> InfusionEnchantmentRules.aspectCost(1, Float.NaN));
        assertThrows(ArithmeticException.class, () -> InfusionEnchantmentRules.aspectCost(Integer.MAX_VALUE, 1));
    }
}
