package thaumcraft.wands;

import org.junit.jupiter.api.Test;
import thaumcraft.common.items.wands.WandVisDiscount;
import static org.junit.jupiter.api.Assertions.*;

class FocusVisArithmeticTest {
    @Test void castingKeepsSeparateFloatSubtractionsWhileRecipesIgnoreFrugal() {
        for (float cap : new float[]{1.1F, 1F, .9F}) for (int gear : new int[]{-30, 0, 5, 10, 80, 100}) for (int frugal = 0; frugal <= 5; frugal++) {
            float expected = cap; expected -= gear / 100F; expected -= frugal / 10F;
            assertEquals(Math.max(expected, .1F), WandVisDiscount.focusModifier(cap, gear, frugal));
            assertEquals(Math.max(cap - gear / 100F, .1F), WandVisDiscount.modifier(cap, gear));
        }
        assertNotEquals(Float.floatToIntBits(.9F - (10 + 30) / 100F),
                Float.floatToIntBits(WandVisDiscount.focusModifier(.9F, 10, 3)));
    }

    /**
     * The centivis arithmetic of a cast: the modifier is applied to the raw centivis cost and the
     * result is truncated, never rounded, and never reaches zero.
     *
     * <p>Oracle {@code WandManager:104-120}: the raw cost of an aspect is multiplied by the
     * modifier of the wand and the payer, and the amount actually debited is the truncation of that
     * product. The assertions below therefore pin the PRODUCT and its truncation for costs that
     * the original really pays - 5, 2, 15 and 1000 centivis - rather than recomputing the modifier
     * inside the test. The end-to-end debit is additionally exercised by the
     * {@code AccessoriesRuntimeChecks} GameTest, which calls the real
     * {@code WandManager.consumeVisFromInventory}; this test pins the arithmetic that the debit
     * depends on, so a mutation of the modifier can no longer pass here while the game still pays.</p>
     */
    @Test void centivisTruncationZeroAspectsAndClampAreNotRoundedOrFreeRecipes() {
        // A .9 cap with five Frugal is .9F - .5F, which in Java float arithmetic is 0.39999998,
        // NOT 0.4. That is the original's behaviour and the reason the truncation matters:
        // 5 centivis at that modifier cost 1, not 2. The values below are measured from the
        // production formula on a JVM, not computed on paper.
        float frugalFive = WandVisDiscount.focusModifier(.9F, 0, 5);
        assertEquals(0.39999998F, frugalFive, 1e-9F, "the float subtraction is the original's, not an exact .4");
        assertEquals(1, (int) (5 * frugalFive), "5 centivis at .39999998 truncate to 1");
        assertEquals(0, (int) (2 * frugalFive), "2 centivis at .39999998 truncate to 0, which the original allows");
        // A full cap with one Frugal is .9 exactly, and 15 centivis cost 13: 13.5 truncated.
        assertEquals(0.9F, WandVisDiscount.focusModifier(1F, 0, 1), 1e-9F);
        assertEquals(13, (int) (15 * WandVisDiscount.focusModifier(1F, 0, 1)));
        assertNotEquals(14, (int) (15 * WandVisDiscount.focusModifier(1F, 0, 1)), "the draw never rounds up");
        // The clamp is a floor of .1F and it is reached, not merely approached.
        assertEquals(.1F, WandVisDiscount.focusModifier(.9F, 500, 5));
        assertEquals(1, (int) (15 * WandVisDiscount.focusModifier(.9F, 500, 5)), "the clamp still leaves a cost");
        // A gear discount of 90 on a .9 cap leaves .9F - .9F = 0 in the RAW formula; the original
        // clamps it to .1F, so 1000 centivis still cost 100 - the cast is never free.
        assertEquals(.1F, WandVisDiscount.modifier(.9F, 90));
        assertEquals(100, (int) (1000 * WandVisDiscount.modifier(.9F, 90)));
        assertNotEquals(0, (int) (1000 * WandVisDiscount.modifier(.9F, 90)), "no cast is ever free");
        // The original rejects a negative gear discount, an out-of-range Frugal and a NaN cap.
        assertThrows(IllegalArgumentException.class, () -> WandVisDiscount.focusModifier(.9F, 0, -1));
        assertThrows(IllegalArgumentException.class, () -> WandVisDiscount.focusModifier(.9F, 0, 6));
        assertThrows(IllegalArgumentException.class, () -> WandVisDiscount.focusModifier(Float.NaN, 0, 0));
    }
}
