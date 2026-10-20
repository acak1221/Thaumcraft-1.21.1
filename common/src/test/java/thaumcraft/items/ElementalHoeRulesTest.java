package thaumcraft.items;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import thaumcraft.common.items.equipment.ElementalHoeRules;

/** The numbers of the Hoe of Growth (original ItemElementalHoe) without a world. */
class ElementalHoeRulesTest {
    @Test void theMarginsAndWearAreTheOriginalOnes() {
        assertEquals(20, ElementalHoeRules.margin(false));
        assertEquals(150, ElementalHoeRules.margin(true));
        assertEquals(5, ElementalHoeRules.wear(false));
        assertEquals(25, ElementalHoeRules.wear(true));
        assertEquals(1, ElementalHoeRules.BONEMEAL_WEAR);
        assertEquals(1, ElementalHoeRules.TILL_RADIUS);
    }

    @Test void aSaplingGrowsWhileTheMarginIsLeftAndTheMarginMayBeUsedUpExactly() {
        assertTrue(ElementalHoeRules.mayGrow(0, 1500, false));
        assertTrue(ElementalHoeRules.mayGrow(1480, 1500, false), "exactly 20 left");
        assertFalse(ElementalHoeRules.mayGrow(1481, 1500, false), "19 left");
        assertTrue(ElementalHoeRules.mayGrow(1350, 1500, true), "exactly 150 left");
        assertFalse(ElementalHoeRules.mayGrow(1351, 1500, true), "149 left");
        assertTrue(ElementalHoeRules.mayGrow(1400, 1500, false), "a greatwood sapling needs only 20");
        assertFalse(ElementalHoeRules.mayGrow(1400, 1500, true), "a silverwood sapling needs 150");
    }
}
