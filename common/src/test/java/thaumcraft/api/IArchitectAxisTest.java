package thaumcraft.api;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import thaumcraft.api.IArchitect.EnumAxis;
import thaumcraft.common.items.wands.WandArchitect;

/**
 * The axes of the architect preview of the original.
 *
 * <p>Oracle {@code thaumcraft/common/items/wands/foci/ItemFocusTrade.java:301-319} and
 * {@code thaumcraft/common/items/wands/foci/ItemFocusWarding.java:288-309} (CFR 0.152
 * decompilation of {@code Thaumcraft-1.7.10-4.2.3.5.jar}). The trade focus draws two axes per
 * face and the selected dimension of the area picks the first or the second of them; the warding
 * focus draws all three axes when the dimension is zero and otherwise exactly the axis of the
 * selected dimension for the face of the anchor.</p>
 */
class IArchitectAxisTest {
    private static WandArchitect.Area area(int dimension) {
        return new WandArchitect.Area(3, 3, 3, dimension);
    }

    @Test void theTradeFocusDrawsTwoAxesPerFace() {
        // Oracle ItemFocusTrade:301-319 - the Y face draws X and Z.
        assertTrue(WandArchitect.showAxis(area(0), 0, EnumAxis.X));
        assertTrue(WandArchitect.showAxis(area(0), 1, EnumAxis.Z));
        assertFalse(WandArchitect.showAxis(area(0), 0, EnumAxis.Y));
        // The Z face draws Y and X.
        assertTrue(WandArchitect.showAxis(area(0), 2, EnumAxis.Y));
        assertTrue(WandArchitect.showAxis(area(0), 3, EnumAxis.X));
        assertFalse(WandArchitect.showAxis(area(0), 2, EnumAxis.Z));
        // The X face draws Y and Z.
        assertTrue(WandArchitect.showAxis(area(0), 4, EnumAxis.Y));
        assertTrue(WandArchitect.showAxis(area(0), 5, EnumAxis.Z));
        assertFalse(WandArchitect.showAxis(area(0), 4, EnumAxis.X));
    }

    @Test void theSelectedDimensionPicksOneAxisOfTheTradePreview() {
        assertTrue(WandArchitect.showAxis(area(1), 0, EnumAxis.X), "the dimension one picks the X axis");
        assertFalse(WandArchitect.showAxis(area(1), 0, EnumAxis.Z));
        assertTrue(WandArchitect.showAxis(area(2), 0, EnumAxis.Z), "the dimension two picks the Z axis");
        assertFalse(WandArchitect.showAxis(area(2), 0, EnumAxis.X));
        assertTrue(WandArchitect.showAxis(area(1), 2, EnumAxis.Y));
        assertTrue(WandArchitect.showAxis(area(2), 2, EnumAxis.X));
        assertTrue(WandArchitect.showAxis(area(1), 4, EnumAxis.Y));
        assertTrue(WandArchitect.showAxis(area(2), 4, EnumAxis.Z));
    }

    @Test void theWardingFocusDrawsEveryAxisWithoutASelectedDimension() {
        for (int side = 0; side <= 5; side++) {
            assertTrue(WandArchitect.showAxisWarding(area(0), side, EnumAxis.X));
            assertTrue(WandArchitect.showAxisWarding(area(0), side, EnumAxis.Y));
            assertTrue(WandArchitect.showAxisWarding(area(0), side, EnumAxis.Z));
        }
    }

    @Test void theWardingFocusDrawsTheAxisOfTheSelectedDimension() {
        assertTrue(WandArchitect.showAxisWarding(area(1), 0, EnumAxis.X), "the dimension one is the X axis on a Y face");
        assertFalse(WandArchitect.showAxisWarding(area(1), 0, EnumAxis.Z));
        assertTrue(WandArchitect.showAxisWarding(area(2), 0, EnumAxis.Z), "the dimension two is the Z axis on a Y face");
        assertTrue(WandArchitect.showAxisWarding(area(3), 0, EnumAxis.Y), "the dimension three is the Y axis on a Y face");
        assertTrue(WandArchitect.showAxisWarding(area(1), 2, EnumAxis.Y), "the dimension one is the Y axis on a Z face");
        assertTrue(WandArchitect.showAxisWarding(area(2), 2, EnumAxis.X));
        assertTrue(WandArchitect.showAxisWarding(area(3), 2, EnumAxis.Z));
        assertTrue(WandArchitect.showAxisWarding(area(1), 4, EnumAxis.Y), "the dimension one is the Y axis on an X face");
        assertTrue(WandArchitect.showAxisWarding(area(2), 4, EnumAxis.Z));
        assertTrue(WandArchitect.showAxisWarding(area(3), 4, EnumAxis.X));
    }

    @Test void theFaceOfTheOriginalDirectionOrdinalIsTheNativeFace() {
        assertEquals(net.minecraft.core.Direction.DOWN, WandArchitect.face(0));
        assertEquals(net.minecraft.core.Direction.UP, WandArchitect.face(1));
        assertEquals(net.minecraft.core.Direction.NORTH, WandArchitect.face(2));
        assertEquals(net.minecraft.core.Direction.SOUTH, WandArchitect.face(3));
        assertEquals(net.minecraft.core.Direction.WEST, WandArchitect.face(4));
        assertEquals(net.minecraft.core.Direction.EAST, WandArchitect.face(5));
    }
}
