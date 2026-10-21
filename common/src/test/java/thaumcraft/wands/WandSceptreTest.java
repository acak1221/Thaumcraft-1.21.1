package thaumcraft.wands;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Map;
import org.junit.jupiter.api.Test;
import thaumcraft.common.items.wands.WandData;
import thaumcraft.common.items.wands.WandMaterials;
import thaumcraft.common.items.wands.WandVisDiscount;

/**
 * The sceptre of the original wand.
 *
 * <p>Oracle ItemWandCasting:119-121 gives a sceptre one and a half times the capacity of its rod
 * ({@code getRod(stack).getCapacity() * (isSceptre(stack) ? 150 : 100)}) and oracle
 * ItemWandCasting:256-258 gives it ten percent less consumption
 * ({@code consumptionModifier -= 0.1f}).</p>
 */
class WandSceptreTest {
    private static final float DELTA = 1.0E-5F;

    @Test void aSceptreHoldsOneAndAHalfTimesTheCapacityOfItsRod() {
        assertEquals(2500, WandMaterials.capacity("wood", false));
        assertEquals(3750, WandMaterials.capacity("wood", true));
        assertEquals(7500, WandMaterials.capacity("greatwood", true));
        assertEquals(15000, WandMaterials.capacity("silverwood", true));
        assertEquals(11250, WandMaterials.capacity("obsidian", true));
        assertEquals(18750, WandMaterials.capacity("greatwood_staff", true));
        assertEquals(26250, WandMaterials.capacity("obsidian_staff", true));
        assertEquals(37500, WandMaterials.capacity("primal_staff", true));
        for (WandMaterials.Rod rod : WandMaterials.RODS) {
            assertEquals(rod.capacity(), WandMaterials.capacity(rod.tag(), false), rod.tag());
            assertEquals(rod.capacity() * 3 / 2, WandMaterials.capacity(rod.tag(), true), rod.tag());
        }
        assertEquals(37500, WandMaterials.MAX_CAPACITY, "the primal staff as a sceptre is the largest wand");
    }

    @Test void aSceptreConsumesTenPercentLessVis() {
        assertEquals(1.1F, WandVisDiscount.modifier(1.1F, 0), DELTA, "the iron cap of a plain wand");
        assertEquals(1.0F, WandVisDiscount.modifier(1.1F, 0, true), DELTA, "the iron cap of a sceptre");
        assertEquals(0.9F, WandVisDiscount.modifier(1.0F, 0, true), DELTA, "the gold cap of a sceptre");
        assertEquals(0.8F, WandVisDiscount.modifier(0.9F, 0, true), DELTA, "the thaumium cap of a sceptre");
        assertEquals(0.1F, WandVisDiscount.modifier(0.1F, 0, true), DELTA, "the floor of the original stays a tenth");
        assertEquals(0.1F, WandVisDiscount.modifier(0.2F, 0, true), DELTA, "a sceptre never drops below a tenth");
    }

    @Test void theSceptreDiscountIsTakenAfterTheGearDiscount() {
        // Original ItemWandCasting:247-259: cap modifier, then the gear discount, then the sceptre discount.
        assertEquals(0.85F, WandVisDiscount.modifier(1.0F, 5, true), DELTA);
        assertEquals(0.95F, WandVisDiscount.modifier(1.0F, 5, false), DELTA);
    }

    @Test void theFrugalFocusIsDiscountedBeforeTheSceptre() {
        assertEquals(0.6F, WandVisDiscount.focusModifier(0.9F, 0, 2, true), DELTA, "thaumium cap, frugal two, sceptre");
        assertEquals(0.7F, WandVisDiscount.focusModifier(0.9F, 0, 2, false), DELTA, "thaumium cap, frugal two");
        assertEquals(0.8F, WandVisDiscount.focusModifier(0.9F, 0, 0, true), DELTA, "thaumium cap, no frugal, sceptre");
        assertEquals(0.3F, WandVisDiscount.focusModifier(0.9F, 0, 5, true), DELTA, "thaumium cap, frugal five, sceptre");
        assertEquals(0.1F, WandVisDiscount.focusModifier(0.1F, 0, 0, true), DELTA, "the floor of the original stays a tenth");
    }

    @Test void theWandDataCarriesTheVisOfASceptre() {
        // The data component cannot see the sceptre marker of the stack, so it accepts the largest
        // capacity of its rod; the exact capacity of a stack is ItemWandCasting.getMaxVis.
        WandData sceptre = new WandData("wood", "iron", Map.of("aer", 3750));
        assertEquals(3750, sceptre.amount("aer"));
        assertEquals(2500, sceptre.capacity(), "the capacity of the rod alone");
        assertEquals(3750, sceptre.maxCapacity(), "the capacity of the same rod as a sceptre");
        assertEquals(3750, sceptre.withAmount("aer", 9999).amount("aer"), "a sceptre is never clipped to the rod capacity");
        assertEquals(0, sceptre.withAmount("aer", -1).amount("aer"));
        assertTrue(sceptre.amount("aer") > sceptre.capacity(), "the vis of a sceptre is above the capacity of its rod");
    }
}
