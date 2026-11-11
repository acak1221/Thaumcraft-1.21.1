package thaumcraft.client.lib;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import net.minecraft.network.chat.contents.TranslatableContents;
import org.junit.jupiter.api.Test;

/** The line a wand's tooltip gets for the vis discount of everything the player wears. */
class WandDiscountTooltipTest {
    private static final List<String> NAMES = List.of("Aer", "Terra", "Ignis", "Aqua", "Ordo", "Perditio");

    private static TranslatableContents line(Integer... points) {
        return (TranslatableContents) WandDiscountTooltip.line(NAMES, List.of(points)).orElseThrow().getContents();
    }

    @Test void gearThatChangesNothingAddsNoLine() {
        assertTrue(WandDiscountTooltip.line(NAMES, List.of(0, 0, 0, 0, 0, 0)).isEmpty());
    }

    @Test void oneNumberWhenEveryPrimalIsDiscountedAlike() {
        var line = line(7, 7, 7, 7, 7, 7);
        assertEquals("tc.port.wand.gear_discount", line.getKey());
        assertArrayEquals(new Object[] {"-7"}, line.getArgs());
    }

    @Test void visExhaustionReadsAsAHigherCost() {
        assertArrayEquals(new Object[] {"+10"}, line(-10, -10, -10, -10, -10, -10).getArgs());
    }

    /** The hover harness discounts air by five and every other primal by two. */
    @Test void primalsDiscountedDifferentlyAreListedEachWithItsNumber() {
        var line = line(5, 2, 2, 2, 2, 0);
        assertEquals("tc.port.wand.gear_discount_each", line.getKey());
        assertArrayEquals(new Object[] {"Aer -5%, Terra -2%, Ignis -2%, Aqua -2%, Ordo -2%"}, line.getArgs());
    }
}
