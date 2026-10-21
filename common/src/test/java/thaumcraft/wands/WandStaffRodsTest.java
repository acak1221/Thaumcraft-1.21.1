package thaumcraft.wands;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.IntUnaryOperator;
import org.junit.jupiter.api.Test;
import thaumcraft.common.items.wands.WandData;
import thaumcraft.common.items.wands.WandMaterials;
import thaumcraft.common.items.wands.WandRodPassive;

/** The nine staff rods (original StaffRod, ConfigItems:304-313) and the primal staff's random refill (WandRodPrimalOnUpdate()). */
class WandStaffRodsTest {
    private record Staff(String tag, int vis, int cost, String passive) {}

    private static final List<Staff> STAVES = List.of(
            new Staff("greatwood_staff", 125, 8, null), new Staff("obsidian_staff", 175, 14, "terra"), new Staff("blaze_staff", 175, 14, "ignis"),
            new Staff("ice_staff", 175, 14, "aqua"), new Staff("quartz_staff", 175, 14, "ordo"), new Staff("bone_staff", 175, 14, "perditio"),
            new Staff("reed_staff", 175, 14, "aer"), new Staff("silverwood_staff", 250, 24, null), new Staff("primal_staff", 250, 32, WandMaterials.ANY_PRIMAL));

    @Test void theNineStaffRodsHaveTheOriginalCapacityPriceAndAspect() {
        assertEquals(9, STAVES.size());
        for (Staff staff : STAVES) {
            var rod = WandMaterials.rod(staff.tag());
            assertEquals(staff.vis() * 100, rod.capacity(), staff.tag() + " capacity in centivis");
            assertEquals(staff.cost(), rod.craftCost(), staff.tag());
            assertEquals(staff.passive(), rod.passive(), staff.tag());
            assertEquals("ROD_" + staff.tag(), rod.research(), "the research key is ROD_<tag>");
            assertTrue(rod.staff(), staff.tag());
            assertEquals(staff.tag().substring(0, staff.tag().length() - "_staff".length()), rod.baseTag(), staff.tag());
        }
        for (var rod : WandMaterials.RODS.subList(0, 9)) {
            assertFalse(rod.staff(), rod.tag());
            assertEquals(rod.tag(), rod.baseTag());
        }
        assertEquals(List.of("greatwood_staff", "obsidian_staff", "blaze_staff", "ice_staff", "quartz_staff", "bone_staff", "reed_staff", "silverwood_staff", "primal_staff"),
                WandMaterials.RODS.subList(9, 18).stream().map(WandMaterials.Rod::tag).toList(), "the staves follow the wand rods, in this order");
        assertEquals(18, WandMaterials.RODS.size());
    }

    @Test void aStaffWandIsAssembledAtRodTimesCapPriceAndHoldsTheWholeStaffCapacity() {
        assertEquals(8 * 6, WandMaterials.assemblyCost("greatwood_staff", "thaumium"));
        assertEquals(32 * 1, WandMaterials.assemblyCost("primal_staff", "iron"));
        assertEquals(24 * 3, WandMaterials.assemblyCost("silverwood_staff", "gold"));
        // The largest per-aspect amount the component can store is a primal staff as a sceptre: 375 vis.
        WandData full = new WandData("primal_staff", "thaumium", Map.of("aer", 25000, "terra", 25000));
        assertEquals(25000, full.amount("aer"));
        assertEquals(37500, WandData.MAX_CENTIVIS, "a primal staff as a sceptre holds 375 vis");
        assertEquals(25000, WandMaterials.RODS.stream().mapToInt(WandMaterials.Rod::capacity).max().orElseThrow());
    }

    @Test void aFixedAspectStaffRefillsLikeAnElementalWandRodButOverAGreaterCapacity() {
        IntUnaryOperator none = bound -> { throw new AssertionError("a fixed-aspect rod must not draw a random number"); };
        WandData wand = new WandData("obsidian_staff", "iron", Map.of());
        assertEquals(1750, WandRodPassive.threshold(wand.capacity()), "a tenth of 175 vis");
        assertEquals(100, WandRodPassive.tick(wand, 17500, 200, none).amount("terra"));
        assertSame(wand, WandRodPassive.tick(wand, 17500, 50, none), "the fixed rod waits for the two hundredth tick, not the fiftieth");
        WandData enough = new WandData("obsidian_staff", "iron", Map.of("terra", 1750));
        assertSame(enough, WandRodPassive.tick(enough, 17500, 200, none));
        WandData greatwood = new WandData("greatwood_staff", "iron", Map.of());
        assertSame(greatwood, WandRodPassive.tick(greatwood, 12500, 200, none), "a plain staff core refills nothing");
        WandData plain = new WandData("silverwood_staff", "iron", Map.of());
        assertSame(plain, WandRodPassive.tick(plain, 25000, 200, none));
        assertNull(WandMaterials.rod("silverwood_staff").passive());
    }

    @Test void thePrimalStaffAddsOneVisEveryFiftiethTickToOneRandomLowPrimal() {
        WandData wand = new WandData("primal_staff", "thaumium", Map.of());
        List<Integer> bounds = new ArrayList<>();
        IntUnaryOperator first = bound -> { bounds.add(bound); return 0; };
        WandData next = WandRodPassive.tick(wand, 25000, 50, first);
        assertEquals(List.of(6), bounds, "all six primals are low, so the draw is among six");
        assertEquals(100, next.amount("aer"), "index 0 is aer (the original primal order)");
        for (String other : List.of("terra", "ignis", "aqua", "ordo", "perditio")) assertEquals(0, next.amount(other), other);
        // The index picks the aspect in the original primal order aer, terra, ignis, aqua, ordo, perditio.
        for (int index = 0; index < 6; index++) {
            int chosen = index;
            WandData picked = WandRodPassive.tick(wand, 25000, 100, bound -> chosen);
            assertEquals(100, picked.amount(WandData.PRIMALS.get(index)), "index " + index);
        }
        assertEquals(100, WandRodPassive.tick(wand, 25000, 0, bound -> 5).amount("perditio"), "tick zero is due as well");
    }

    @Test void thePrimalStaffOnlyDrawsWhenDueAndOnlyAmongThePrimalsBelowATenth() {
        IntUnaryOperator never = bound -> { throw new AssertionError("nothing is due"); };
        WandData wand = new WandData("primal_staff", "iron", Map.of());
        for (int tick : new int[] {1, 49, 51, 99, 101, 199}) assertSame(wand, WandRodPassive.tick(wand, 25000, tick, never), "tick " + tick);
        int threshold = WandRodPassive.threshold(wand.capacity());
        assertEquals(2500, threshold);
        // Four primals are at the tenth or above: only the remaining two can be chosen, and the draw is among two.
        WandData mixed = new WandData("primal_staff", "iron", Map.of("aer", threshold, "terra", threshold + 1, "ignis", 25000, "aqua", threshold));
        List<Integer> bounds = new ArrayList<>();
        WandData first = WandRodPassive.tick(mixed, 25000, 50, bound -> { bounds.add(bound); return 0; });
        assertEquals(List.of(2), bounds);
        assertEquals(100, first.amount("ordo"), "the first low primal in order");
        WandData second = WandRodPassive.tick(mixed, 25000, 50, bound -> 1);
        assertEquals(100, second.amount("perditio"));
        assertEquals(mixed.amount("aer"), second.amount("aer"));
        // Nothing low: the wand is left as it is and no number is drawn.
        WandData satisfied = new WandData("primal_staff", "iron", Map.of("aer", threshold, "terra", threshold, "ignis", threshold, "aqua", threshold,
                "ordo", threshold, "perditio", threshold));
        assertSame(satisfied, WandRodPassive.tick(satisfied, 25000, 50, never));
        // Just below the tenth still adds a whole vis, and the staff never passes its capacity.
        WandData nearly = new WandData("primal_staff", "iron", Map.of("aer", threshold - 1, "terra", threshold, "ignis", threshold, "aqua", threshold,
                "ordo", threshold, "perditio", threshold));
        assertEquals(threshold - 1 + 100, WandRodPassive.tick(nearly, 25000, 50, bound -> 0).amount("aer"));
    }
}
