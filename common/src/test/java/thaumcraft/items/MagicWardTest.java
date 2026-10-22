package thaumcraft.items;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;
import thaumcraft.common.lib.MagicWard;

/** The magic pass of the void thaumaturge robes (original ItemVoidRobeArmor.getProperties and Forge's ApplyArmor) without a world. */
class MagicWardTest {
    @Test void aPieceAbsorbsItsArmorOverThirtyFive() {
        // Oracle ItemVoidRobeArmor:229-240: double ratio = (double)this.field_77879_b / 35.0;
        assertEquals(35.0, MagicWard.DIVISOR);
        assertEquals(0.08571428571428572, MagicWard.ratio(3));
        assertEquals(0.2, MagicWard.ratio(7));
        assertEquals(0.17142857142857143, MagicWard.ratio(6));
    }

    @Test void theFullSetTakesSixteenThirtyFifthsOfTheDamageInOneStep() {
        // Forge's ApplyArmor scales the damage by twenty five, takes the shares off in one step and
        // divides by twenty five again; the shares are the armor over thirty five.
        assertEquals(10.857142F, MagicWard.reduce(20F, 3, 7, 6));
        assertEquals(16F, MagicWard.reduce(20F, 7), "the robe alone absorbs a fifth");
        assertEquals(20F, MagicWard.reduce(20F), "no ward, no change");
        assertEquals(0F, MagicWard.reduce(0F, 3, 7, 6));
    }

    @Test void aPieceIsWornByWhatItAbsorbedButAtLeastOnePoint() {
        assertEquals(4, MagicWard.wear(20F, 7), "20 * 0.2 = 4");
        assertEquals(3, MagicWard.wear(20F, 6), "20 * 6/35 = 3.43, truncated");
        assertEquals(1, MagicWard.wear(20F, 3), "20 * 3/35 = 1.7, truncated");
        assertEquals(1, MagicWard.wear(4F, 3), "a small blow still costs one point");
        assertEquals(1, MagicWard.wear(35F, 1), "exactly one point of absorption is one point");
        assertEquals(2, MagicWard.wear(70F, 1));
    }
}
