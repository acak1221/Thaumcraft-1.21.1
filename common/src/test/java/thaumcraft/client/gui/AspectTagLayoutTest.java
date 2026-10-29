package thaumcraft.client.gui;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/**
 * The pure half of the aspect tag: what {@code AspectRenderer.draw} hands to the renderer, and the thresholds and texts the
 * renderer reads from it. The four adjacent ints of {@code draw} (x, y, amount, bonus) are the thing a swap would corrupt.
 */
class AspectTagLayoutTest {
    @Test void theArgumentsOfDrawLandInTheirOwnFields() {
        AspectTagLayout tag = AspectTagLayout.of(4, 5, 6, 7, 1.0F);
        assertEquals(4, tag.x(), "x is the first position argument");
        assertEquals(5, tag.y(), "y is the second");
        assertEquals(6, tag.amount(), "amount follows the position");
        assertEquals(7, tag.bonus(), "bonus is last");
        assertEquals(1.0F, tag.scale(), 0.0F);
        AspectTagLayout swapped = AspectTagLayout.of(5, 4, 7, 6, 2.0F);
        assertEquals(5, swapped.x());
        assertEquals(4, swapped.y());
        assertEquals(7, swapped.amount());
        assertEquals(6, swapped.bonus());
        assertEquals(2.0F, swapped.scale(), 0.0F);
    }

    @Test void theAmountIsShownAboveZeroOnly() {
        assertFalse(AspectTagLayout.of(0, 0, 0, 0, 1.0F).showsAmount());
        assertFalse(AspectTagLayout.of(0, 0, -3, 0, 1.0F).showsAmount());
        assertTrue(AspectTagLayout.of(0, 0, 1, 0, 1.0F).showsAmount());
    }

    @Test void theBonusIsASparkleAboveZeroAndAlsoANumberAboveOne() {
        AspectTagLayout none = AspectTagLayout.of(0, 0, 5, 0, 1.0F), negative = AspectTagLayout.of(0, 0, 5, -2, 1.0F);
        AspectTagLayout one = AspectTagLayout.of(0, 0, 5, 1, 1.0F), two = AspectTagLayout.of(0, 0, 5, 2, 1.0F);
        assertFalse(none.showsBonus() || none.showsBonusNumber());
        assertFalse(negative.showsBonus() || negative.showsBonusNumber());
        assertTrue(one.showsBonus());
        assertFalse(one.showsBonusNumber(), "a bonus of one is the sparkle alone");
        assertTrue(two.showsBonus() && two.showsBonusNumber(), "a bonus above one adds the number");
    }

    @Test void theNumbersAreWrittenAsPlainDigits() {
        assertEquals("12345", AspectTagLayout.of(0, 0, 12345, 0, 1.0F).amountText(), "no thousands separator");
        assertEquals("7", AspectTagLayout.of(0, 0, 7, 0, 1.0F).amountText(), "no plus sign");
        assertEquals("9", AspectTagLayout.of(0, 0, 0, 9, 1.0F).bonusText());
    }
}
