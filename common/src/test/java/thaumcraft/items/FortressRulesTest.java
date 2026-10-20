package thaumcraft.items;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import thaumcraft.common.items.armor.FortressRules;
import thaumcraft.common.lib.MagicWard;

/** The numbers of the Thaumium Fortress armor (original ItemFortressArmor.getProperties and EventHandlerRunic.entityHurt) without a world. */
class FortressRulesTest {
    @Test void theSetMultiplierStartsBelowOneAndEachPieceLiftsIt() {
        boolean[] none = {false, false, false}, all = {true, true, true};
        assertEquals(0.875, FortressRules.set(none, none), 1e-12, "nothing worn");
        assertEquals(1.0, FortressRules.set(new boolean[]{false, false, true}, none), 1e-12, "one piece is exactly 1");
        assertEquals(1.125, FortressRules.set(new boolean[]{true, true, false}, none), 1e-12);
        assertEquals(1.25, FortressRules.set(all, none), 1e-12, "the whole set is a quarter more");
        assertEquals(1.3, FortressRules.set(all, new boolean[]{false, false, true}), 1e-12, "a mask on the helm adds 0.05");
        assertEquals(1.4, FortressRules.set(all, all), 1e-12, "a mask on each piece (not obtainable) 0.15");
    }

    @Test void theMasksTriggerByTheSizeOfTheBlow() {
        assertEquals(12F, FortressRules.FIEND_DIVISOR);
        assertEquals(10F, FortressRules.GHOST_DIVISOR);
        assertEquals(80, FortressRules.WITHER_TICKS);
        assertTrue(FortressRules.triggers(0.49F, 6F, 12F), "6 of 12 is half");
        assertFalse(FortressRules.triggers(0.5F, 6F, 12F), "a roll equal to the chance does not trigger");
        assertTrue(FortressRules.triggers(0.999F, 10F, 10F), "a blow of 10 always wakes the ghost");
        assertFalse(FortressRules.triggers(0F, 0F, 12F), "no damage, no chance");
    }

    @Test void fireIsWardedByAFifthOfTheArmorOverTwentyAndTheShareCapsAtOne() {
        assertEquals(20.0, MagicWard.FIRE_DIVISOR);
        assertEquals(0.15, MagicWard.fireRatio(3), 1e-12);
        assertEquals(0.35, MagicWard.fireRatio(7), 1e-12);
        assertEquals(0.3, MagicWard.fireRatio(6), 1e-12);
        // The whole set with its bonus: 16 / 20 * 1.25 = exactly everything.
        assertEquals(0F, MagicWard.reduceByRatios(20F, MagicWard.fireRatio(3) * 1.25, MagicWard.fireRatio(7) * 1.25, MagicWard.fireRatio(6) * 1.25), 1e-5F);
        assertEquals(0F, MagicWard.reduceByRatios(20F, 0.7, 0.7), 0F, "more than everything is still everything, not less than nothing");
        assertEquals(10F, MagicWard.reduceByRatios(20F, 0.25, 0.25), 1e-5F);
        assertEquals(20F, MagicWard.reduceByRatios(20F), 0F);
        assertEquals(3, MagicWard.wearByRatio(20F, 0.15), "20 * 0.15 = 3");
        assertEquals(1, MagicWard.wearByRatio(1F, 0.15), "a small blow still wears one point");
    }
}
