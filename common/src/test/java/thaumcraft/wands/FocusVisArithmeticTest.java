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

    @Test void centivisTruncationZeroAspectsAndClampAreNotRoundedOrFreeRecipes() {
        float modifier = WandVisDiscount.focusModifier(.9F, 0, 5);
        assertEquals(1, (int)(5 * modifier)); assertEquals(0, (int)(2 * modifier));
        assertEquals(13, (int)(15 * WandVisDiscount.focusModifier(1F, 0, 1)));
        assertEquals(.1F, WandVisDiscount.focusModifier(.9F, 500, 5));
        assertEquals(1, (int)(15 * WandVisDiscount.focusModifier(.9F, 500, 5)));
        assertEquals(900, (int)(10 * 100 * WandVisDiscount.modifier(.9F, 0)));
        assertThrows(IllegalArgumentException.class, () -> WandVisDiscount.focusModifier(.9F, 0, -1));
        assertThrows(IllegalArgumentException.class, () -> WandVisDiscount.focusModifier(.9F, 0, 6));
        assertThrows(IllegalArgumentException.class, () -> WandVisDiscount.focusModifier(Float.NaN, 0, 0));
    }
}
