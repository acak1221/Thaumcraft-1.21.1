package thaumcraft.golems;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import net.minecraft.core.Direction;
import org.junit.jupiter.api.Test;
import thaumcraft.common.entities.golems.GolemHelper;
import thaumcraft.common.items.golems.GolemMarker;

/**
 * The single entry point of the searches of the golem, pinned against the original.
 *
 * <p>The original had <b>one</b> helper class with the searches of every core
 * (TC4 4.2.3.5 oracle, {@code thaumcraft/common/entities/golems/GolemHelper.java:48-721}). The
 * native tree had split them into pure rule classes; the native now has the one entry point again
 * ({@code thaumcraft/common/entities/golems/GolemHelper.java}).</p>
 *
 * <p>No test of this class constructs a {@code Block}: the only constant it reads is
 * {@code ADJACENT_RANGE}, which the compiler inlines, and the other assertions are about the
 * direction order and the colour rule, which are pure functions.</p>
 */
class GolemHelperStructureTest {

    @Test void theAdjacentRangeIsTheOriginal() {
        // Original GolemHelper.ADJACENT_RANGE:49 and getMarkedContainersAdjacentToGolem:70.
        assertEquals(4.0, GolemHelper.ADJACENT_RANGE);
    }

    @Test void theDirectionOrderIsTheOrderOfTheOriginal() {
        // Original getMarkedSides:126-138 numbers the sides with the ordinal of the
        // 1.7.10 ForgeDirection; the 1.21.1 Direction has the same order.
        assertEquals(0, Direction.DOWN.ordinal());
        assertEquals(1, Direction.UP.ordinal());
        assertEquals(2, Direction.NORTH.ordinal());
        assertEquals(3, Direction.SOUTH.ordinal());
        assertEquals(4, Direction.WEST.ordinal());
        assertEquals(5, Direction.EAST.ordinal());
    }

    @Test void theColourRuleIsTheOriginal() {
        // Original Marker.equalsFuzzy:31-37 — the colour of the probe accepts a marker of the
        // same colour, and the colour -1 accepts a marker of any colour.
        assertTrue(GolemMarker.accepts(GolemMarker.ANY, 0));
        assertTrue(GolemMarker.accepts(GolemMarker.ANY, 15));
        assertTrue(GolemMarker.accepts(7, 7));
        assertFalse(GolemMarker.accepts(7, 8));
    }
}
