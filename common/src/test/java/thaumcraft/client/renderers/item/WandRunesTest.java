package thaumcraft.client.renderers.item;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

/** The original ModelWand rune numbers (JAR bytecode, render offsets 938-1199 and drawRune). */
class WandRunesTest {
    @Test void theRuneCountsAndLightAreTheOriginalOnes() {
        assertEquals(10, WandRunes.SCEPTRE_RUNES);
        assertEquals(4, WandRunes.STAFF_BANDS);
        assertEquals(14, WandRunes.STAFF_BAND_RUNES);
        assertEquals(200, WandRunes.LIGHT);
    }

    @Test void aStaffBandStartsThreeRunesFurtherAndWraps() {
        assertEquals(0, WandRunes.staffRune(0, 0));
        assertEquals(13, WandRunes.staffRune(0, 13));
        assertEquals(3, WandRunes.staffRune(1, 0));
        assertEquals(15, WandRunes.staffRune(3, 6));
        assertEquals(0, WandRunes.staffRune(3, 7), "(7 + 9) % 16");
    }

    @Test void thePulseIsTheOriginalSine() {
        // drawRune: red sin(t/5)*0.1+0.88, green sin(t/7)*0.1+0.63, alpha sin(t/10)*0.2+0.6, t = age + rune * 5.
        float[] start = WandRunes.pulse(0, 0);
        assertEquals(.88F, start[0], 1e-6F);
        assertEquals(.63F, start[1], 1e-6F);
        assertEquals(.6F, start[3], 1e-6F);
        float[] later = WandRunes.pulse(2, 5);
        float t = 15;
        assertEquals((float) Math.sin(t / 5F) * .1F + .88F, later[0], 1e-5F);
        assertEquals((float) Math.sin(t / 10F) * .2F + .6F, later[3], 1e-5F);
    }
}
