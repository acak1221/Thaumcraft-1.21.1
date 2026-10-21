package thaumcraft.wands;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import thaumcraft.common.items.wands.WandManager;
import thaumcraft.common.items.wands.WandMaterials;
import thaumcraft.common.items.wands.WandVisDiscount;

/**
 * The wand click that makes a Thaumonomicon and the consumption numbers of the wand tooltip.
 *
 * <p>Original {@code thaumcraft/common/items/wands/WandManager.java:459-478}
 * ({@code createThaumonomicon}) refuses the bookshelf while a focus is installed in the wand:
 * {@code if (wand.getFocus(itemstack) != null) return false;}. Original
 * {@code ItemWandCasting:132-183} shows the consumption modifier of every stored aspect, which is
 * the ASPECT dependent cap modifier, the discount of the gear, the discount of the focus and the
 * discount of the sceptre ({@code ItemWandCasting:232-256}).</p>
 */
class WandThaumonomiconTest {
    @Test void aWandWithAFocusCannotMakeAThaumonomicon() {
        assertTrue(WandManager.mayCreateThaumonomicon(false), "a wand without a focus makes a Thaumonomicon");
        assertFalse(WandManager.mayCreateThaumonomicon(true), "a wand with a focus leaves the bookshelf alone");
    }

    @Test void theTooltipConsumptionIsTheAspectDependentCapModifier() {
        // Original ItemWandCasting:236-241 - the copper cap pays one for order and entropy and 1.1
        // for every other aspect (ConfigItems:331).
        assertEquals(1.0F, WandMaterials.COPPER.modifierFor("ordo"));
        assertEquals(1.0F, WandMaterials.COPPER.modifierFor("perditio"));
        assertEquals(1.1F, WandMaterials.COPPER.modifierFor("aer"));
        assertEquals(1.1F, WandMaterials.COPPER.modifierFor("ignis"));
        // Original ItemWandCasting:247-259 - the sceptre takes another tenth off.
        // Oracle ItemWandCasting:247-261: the frugal discount, then the sceptre discount of 0.1f,
        // never below a tenth.
        assertEquals(1.0F - 0 / 100F - 0 / 10F - 0.1F, WandVisDiscount.focusModifier(WandMaterials.COPPER.modifierFor("ordo"), 0, 0, true));
        assertEquals(1.1F - 0 / 100F - 0 / 10F - 0.1F, WandVisDiscount.focusModifier(WandMaterials.COPPER.modifierFor("aer"), 0, 0, true));
        // The frugal discount of the focus belongs to the tooltip as well (crafting is false there).
        assertEquals(1.1F - 0 / 100F - 1 / 10F - 0.1F, WandVisDiscount.focusModifier(WandMaterials.COPPER.modifierFor("aer"), 0, 1, true));
    }
}
