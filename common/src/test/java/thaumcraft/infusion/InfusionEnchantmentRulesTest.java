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
    @Test void pointPricesConvertTheNominalCostRatherThanThePayersCurrentLevel() {
        assertEquals(7, InfusionEnchantmentRules.xpPoints(1, 0));
        assertEquals(16, InfusionEnchantmentRules.xpPoints(1, 1));
        assertEquals(216, InfusionEnchantmentRules.xpPoints(20, 1));
        assertEquals(441, InfusionEnchantmentRules.xpPoints(20, 2));
        int[] levels = {15, 16, 17, 30, 31, 32};
        int[] points = {315, 352, 394, 1395, 1507, 1628};
        for (int i = 0; i < levels.length; i++) {
            assertEquals(levels[i], InfusionEnchantmentRules.xpLevels(levels[i] * 3, 0));
            assertEquals(points[i], InfusionEnchantmentRules.xpPoints(levels[i] * 3, 0));
        }
    }
    @Test void pricesRejectTheFirstUnrepresentablePointCostAndNominalOverflow() {
        assertEquals(2_147_407_943, InfusionEnchantmentRules.xpPoints(21_863 * 3, 0));
        assertEquals(21_864, InfusionEnchantmentRules.xpLevels(21_864 * 3, 0));
        assertThrows(ArithmeticException.class, () -> InfusionEnchantmentRules.xpPoints(21_864 * 3, 0));
        assertThrows(ArithmeticException.class, () -> InfusionEnchantmentRules.xpPoints(Integer.MAX_VALUE, 0));
        assertThrows(ArithmeticException.class, () -> InfusionEnchantmentRules.xpPoints(Integer.MAX_VALUE, 3));
        assertThrows(IllegalArgumentException.class, () -> InfusionEnchantmentRules.xpPoints(-1, 0));
        assertThrows(IllegalArgumentException.class, () -> InfusionEnchantmentRules.xpPoints(1, -1));
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
