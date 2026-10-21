package thaumcraft.wands;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Map;
import java.util.function.IntUnaryOperator;
import org.junit.jupiter.api.Test;
import thaumcraft.common.items.wands.WandData;
import thaumcraft.common.items.wands.WandMaterials;
import thaumcraft.common.items.wands.WandRodPassive;

/** The six elemental rods (original ConfigItems:298-303) and the slow refill of their own primal aspect (WandRodPrimalOnUpdate). */
class WandRodPassiveTest {
    /** A fixed-aspect rod never asks for a random number: asking fails the test. */
    private static final IntUnaryOperator NO_PICK = bound -> { throw new AssertionError("a fixed-aspect rod must not draw a random number"); };
    private static final Map<String, String> ASPECT_OF = Map.of("obsidian", "terra", "blaze", "ignis", "ice", "aqua", "quartz", "ordo",
            "bone", "perditio", "reed", "aer");

    @Test void theElementalRodsHoldSeventyFiveVisAndCostSixEach() {
        for (var entry : ASPECT_OF.entrySet()) {
            var rod = WandMaterials.rod(entry.getKey());
            assertEquals(7500, rod.capacity(), entry.getKey());
            assertEquals(6, rod.craftCost(), entry.getKey());
            assertEquals(entry.getValue(), rod.passive(), entry.getKey() + " refills its own primal");
            assertEquals("ROD_" + entry.getKey(), rod.research());
        }
        for (String plain : List.of("wood", "greatwood", "silverwood")) assertNull(WandMaterials.rod(plain).passive(), plain);
        assertEquals(List.of("wood", "greatwood", "silverwood", "obsidian", "blaze", "ice", "quartz", "bone", "reed"),
                WandMaterials.RODS.subList(0, 9).stream().map(WandMaterials.Rod::tag).toList(), "the earlier rods keep their places");
        assertEquals(36, WandMaterials.assemblyCost("obsidian", "thaumium"));
        assertEquals(6, WandMaterials.assemblyCost("reed", "iron"));
        assertThrows(IllegalArgumentException.class, () -> WandMaterials.rod("copper_staff"));
    }

    @Test void aRodRefillsItsAspectOnEveryTwoHundredthTickOfTheCarrier() {
        WandData wand = new WandData("obsidian", "iron", Map.of());
        assertEquals(100, WandRodPassive.tick(wand, 7500, 200, NO_PICK).amount("terra"), "one whole vis");
        assertEquals(100, WandRodPassive.tick(wand, 7500, 0, NO_PICK).amount("terra"), "tick zero counts too, like the original modulo");
        assertEquals(100, WandRodPassive.tick(wand, 7500, 400, NO_PICK).amount("terra"));
        for (int tick : new int[] {1, 50, 199, 201, 399}) assertSame(wand, WandRodPassive.tick(wand, 7500, tick, NO_PICK), "tick " + tick);
        for (String other : List.of("aer", "ignis", "aqua", "ordo", "perditio")) assertEquals(0, WandRodPassive.tick(wand, 7500, 200, NO_PICK).amount(other), other);
    }

    @Test void theRefillStopsAtATenthOfTheCapacity() {
        assertEquals(750, WandRodPassive.threshold(7500));
        WandData wand = new WandData("blaze", "gold", Map.of("ignis", 749));
        assertEquals(849, WandRodPassive.tick(wand, 7500, 200, NO_PICK).amount("ignis"), "just below the tenth it still adds a whole vis");
        WandData full = new WandData("blaze", "gold", Map.of("ignis", 750));
        assertSame(full, WandRodPassive.tick(full, 7500, 200, NO_PICK), "at a tenth it leaves the wand alone");
        WandData rich = new WandData("blaze", "gold", Map.of("ignis", 7000));
        assertSame(rich, WandRodPassive.tick(rich, 7500, 200, NO_PICK));
    }

    @Test void thePlainRodsNeverRefillAndTheCapacityIsKept() {
        for (String plain : List.of("wood", "greatwood", "silverwood")) {
            WandData wand = new WandData(plain, "iron", Map.of());
            assertSame(wand, WandRodPassive.tick(wand, 7500, 200, NO_PICK), plain);
        }
        // A wand with the refill aspect already near its capacity cap can never be pushed over it.
        WandData small = new WandData("reed", "iron", Map.of("aer", 740));
        WandData next = WandRodPassive.tick(small, 7500, 200, NO_PICK);
        assertEquals(840, next.amount("aer"));
        assertTrue(next.amount("aer") <= next.capacity());
        assertEquals(WandRodPassive.PERIOD, 200);
        assertEquals(WandRodPassive.STEP, 100);
    }

    @Test void theOtherAspectsOfTheWandAreKept() {
        WandData wand = new WandData("ice", "thaumium", Map.of("aer", 5000, "ordo", 1234));
        WandData next = WandRodPassive.tick(wand, 7500, 200, NO_PICK);
        assertEquals(100, next.amount("aqua"));
        assertEquals(5000, next.amount("aer"));
        assertEquals(1234, next.amount("ordo"));
        assertEquals("ice", next.rod());
        assertEquals("thaumium", next.cap());
    }
}
