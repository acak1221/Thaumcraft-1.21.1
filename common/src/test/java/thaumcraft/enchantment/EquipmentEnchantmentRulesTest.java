package thaumcraft.enchantment;

import org.junit.jupiter.api.Test;
import thaumcraft.common.lib.enchantment.EquipmentEnchantmentRules;
import static org.junit.jupiter.api.Assertions.*;

class EquipmentEnchantmentRulesTest {
    @Test void repairTakesFloorSquareRootBeforeMultiplyingTheCappedLevel() {
        assertEquals(1, EquipmentEnchantmentRules.repairCentivis(1, 1));
        assertEquals(3, EquipmentEnchantmentRules.repairCentivis(7, 1));
        assertEquals(7, EquipmentEnchantmentRules.repairCentivis(31, 1));
        assertEquals(14, EquipmentEnchantmentRules.repairCentivis(31, 2));
        assertEquals(14, EquipmentEnchantmentRules.repairCentivis(31, 255));
    }
    @Test void rawCentivisHasCraftingDiscountAndMayTruncateToZero() {
        assertEquals(6, EquipmentEnchantmentRules.repairPrice(6, 1.1F));
        assertEquals(15, EquipmentEnchantmentRules.repairPrice(14, 1.1F));
        assertEquals(4, EquipmentEnchantmentRules.repairPrice(6, .8F));
        assertEquals(0, EquipmentEnchantmentRules.repairPrice(6, .1F));
    }
    @Test void hasteAddsForwardAccelerationWithIndependentImpulseAndWaterHalves() {
        assertEquals(0x3c75c28f, Float.floatToIntBits(EquipmentEnchantmentRules.hasteAcceleration(1, false, false)));
        assertEquals(0x3d3851eb, Float.floatToIntBits(EquipmentEnchantmentRules.hasteAcceleration(3, false, false)));
        assertEquals(0x3cb851eb, Float.floatToIntBits(EquipmentEnchantmentRules.hasteAcceleration(3, true, false)));
        assertEquals(0x3cb851eb, Float.floatToIntBits(EquipmentEnchantmentRules.hasteAcceleration(3, false, true)));
        assertEquals(0x3c3851eb, Float.floatToIntBits(EquipmentEnchantmentRules.hasteAcceleration(3, true, true)));
    }
}
