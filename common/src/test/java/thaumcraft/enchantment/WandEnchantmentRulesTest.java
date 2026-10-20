package thaumcraft.enchantment;

import org.junit.jupiter.api.Test;
import thaumcraft.common.lib.enchantment.EnchantmentFrugal;
import thaumcraft.common.lib.enchantment.EnchantmentPotency;
import thaumcraft.common.lib.enchantment.EnchantmentWandFortune;
import thaumcraft.common.lib.enchantment.WandEnchantmentRules;
import static org.junit.jupiter.api.Assertions.*;

class WandEnchantmentRulesTest {
    @Test void frugalMinimumCostKeepsTheOriginalFivePlusElevenPerLevel() {
        assertEquals(5, WandEnchantmentRules.frugalCost(1, 5));
        assertEquals(16, WandEnchantmentRules.frugalCost(2, 5));
        assertEquals(27, WandEnchantmentRules.frugalCost(3, 5));
        assertEquals(5, EnchantmentFrugal.MIN_COST_BASE);
    }
    @Test void potencyMinimumCostKeepsTheOriginalTenPlusElevenPerLevel() {
        assertEquals(10, WandEnchantmentRules.potencyCost(1, 10));
        assertEquals(21, WandEnchantmentRules.potencyCost(2, 10));
        assertEquals(32, WandEnchantmentRules.potencyCost(3, 10));
        assertEquals(10, EnchantmentPotency.MIN_COST_BASE);
    }
    @Test void wandFortuneMinimumCostKeepsTheOriginalFifteenPlusNinePerLevel() {
        assertEquals(15, WandEnchantmentRules.wandFortuneCost(1, 15));
        assertEquals(24, WandEnchantmentRules.wandFortuneCost(2, 15));
        assertEquals(33, WandEnchantmentRules.wandFortuneCost(3, 15));
        assertEquals(15, EnchantmentWandFortune.MIN_COST_BASE);
    }
    @Test void maximumCostIsTheVanillaMinimumPlusTheOriginalFiftyMargin() {
        assertEquals(61, WandEnchantmentRules.maxCost(1));
        assertEquals(71, WandEnchantmentRules.maxCost(2));
        assertEquals(81, WandEnchantmentRules.maxCost(3));
        assertEquals(61, EnchantmentFrugal.maxCost(1));
        assertEquals(61, EnchantmentPotency.maxCost(1));
        assertEquals(61, EnchantmentWandFortune.maxCost(1));
    }
    @Test void everyOriginalWandEnchantmentStopsAtLevelThree() {
        assertEquals(3, WandEnchantmentRules.FRUGAL_MAX_LEVEL);
        assertEquals(3, WandEnchantmentRules.POTENCY_MAX_LEVEL);
        assertEquals(3, WandEnchantmentRules.WAND_FORTUNE_MAX_LEVEL);
        assertEquals(3, EnchantmentFrugal.MAX_LEVEL);
        assertEquals(3, EnchantmentPotency.MAX_LEVEL);
        assertEquals(3, EnchantmentWandFortune.MAX_LEVEL);
    }
    @Test void frugalDamageChanceIsOneMinusTheLevelOverFive() {
        assertEquals(0x3f4ccccd, Float.floatToIntBits(WandEnchantmentRules.frugalDamageChance(1)));
        assertEquals(0x3f19999a, Float.floatToIntBits(WandEnchantmentRules.frugalDamageChance(2)));
        // The original computes 1 - 3/5 in single precision, which is 0.39999998 and not 0.4.
        assertEquals(0x3ecccccc, Float.floatToIntBits(WandEnchantmentRules.frugalDamageChance(3)));
    }
    @Test void theOriginalEnchantmentsThemselvesNeverChangeAFocusOrTheLoot() {
        // 4.2.3.5 never reads these enchantment levels: only the focus UPGRADES do.
        assertEquals(1F, WandEnchantmentRules.potencyMultiplier(3));
        assertEquals(0, WandEnchantmentRules.fortuneExtraDrops(3));
        assertEquals(1F, EnchantmentPotency.potencyMultiplier(1));
        assertEquals(0, EnchantmentWandFortune.fortuneExtraDrops(1));
    }
    @Test void sceptreCostIsFiftyPercentMoreThanThePlainWand() {
        assertEquals(1, WandEnchantmentRules.sceptreCost(1, 1));
        assertEquals(4, WandEnchantmentRules.sceptreCost(3, 1));
        assertEquals(4, WandEnchantmentRules.sceptreCost(1, 3));
        assertEquals(13, WandEnchantmentRules.sceptreCost(3, 3));
        assertEquals(81, WandEnchantmentRules.sceptreCost(6, 9));
        assertEquals(432, WandEnchantmentRules.sceptreCost(9, 32));
    }
    @Test void impossibleLevelsAreRejectedInsteadOfReturningANonsenseCost() {
        assertThrows(IllegalArgumentException.class, () -> WandEnchantmentRules.frugalCost(0, 5));
        assertThrows(IllegalArgumentException.class, () -> WandEnchantmentRules.potencyCost(-1, 10));
        assertThrows(IllegalArgumentException.class, () -> WandEnchantmentRules.wandFortuneCost(0, 15));
        assertThrows(IllegalArgumentException.class, () -> WandEnchantmentRules.maxCost(0));
        assertThrows(IllegalArgumentException.class, () -> WandEnchantmentRules.frugalDamageChance(0));
        assertThrows(IllegalArgumentException.class, () -> WandEnchantmentRules.potencyMultiplier(0));
        assertThrows(IllegalArgumentException.class, () -> WandEnchantmentRules.fortuneExtraDrops(0));
        assertThrows(IllegalArgumentException.class, () -> WandEnchantmentRules.sceptreCost(-1, 1));
    }
}
