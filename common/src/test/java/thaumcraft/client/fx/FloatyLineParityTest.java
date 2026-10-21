package thaumcraft.client.fx;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

/**
 * The numbers of the original floaty line of the TC4 4.2.3.5 client.
 *
 * <p>Original (oracle {@code thaumcraft/client/lib/UtilsFX.java:733-817}, the same body in
 * {@code thaumcraft/client/renderers/entity/RenderCultist.java:83-176}): the line is a pair of crossed
 * textured ribbons. The length of the line is the number of blocks of the distance times the half of the
 * link quality, the quality is sixteen by default (oracle
 * {@code thaumcraft/common/config/Config.java:881}), the fade of a step is
 * {@code 1 - abs(i - length / 2) / (length / 2)} and the scrolling texture coordinate of a step is
 * {@code (1 - f2) * dist - time * speed}.</p>
 *
 * <p>The test asserts the pure numbers of the rule, without a level, without a block and without a
 * render context.</p>
 */
class FloatyLineParityTest {
    @Test void theOriginalLinkQualityIsSixteen() {
        assertEquals(16, FloatyLine.LINK_QUALITY, "the original Config.golemLinkQuality default");
    }

    @Test void theLengthIsEightStepsPerBlock() {
        // The original length is round(dist) * (quality / 2), the quality is sixteen.
        assertEquals(8.0F, FloatyLine.lineLength(1.0F), 1.0E-6, "one block is eight steps long");
        assertEquals(16.0F, FloatyLine.lineLength(2.0F), 1.0E-6, "two blocks are sixteen steps long");
        assertEquals(24.0F, FloatyLine.lineLength(3.4F), 1.0E-6, "the original rounds the number of blocks");
        assertEquals(0.0F, FloatyLine.lineLength(0.2F), 1.0E-6, "a line shorter than half a block has no length");
    }

    @Test void theFadeIsTheOriginalOne() {
        float length = 8.0F;
        assertEquals(0.0F, FloatyLine.fade(0, length), 1.0E-6, "the first step is fully transparent");
        assertEquals(1.0F, FloatyLine.fade(4, length), 1.0E-6, "the middle step is fully opaque");
        assertEquals(0.0F, FloatyLine.fade(8, length), 1.0E-6, "the last step is fully transparent");
        assertEquals(0.5F, FloatyLine.fade(2, length), 1.0E-6);
    }

    @Test void theScrollIsTheOriginalOne() {
        // The original coordinate is (1 - f2) * dist - time * speed with the speed of the cultist.
        assertEquals(4.0F, FloatyLine.scroll(0.5F, 8.0F, 0.0F, -0.03F), 1.0E-6, "the middle of the line");
        assertEquals(8.0F, FloatyLine.scroll(0.0F, 8.0F, 0.0F, -0.03F), 1.0E-6, "the start of the line");
        assertEquals(0.0F, FloatyLine.scroll(1.0F, 8.0F, 0.0F, -0.03F), 1.0E-6, "the end of the line");
        // The original wraps the time at 32767 and divides it by five.
        assertEquals(8.0F + 1.0F * 0.03F * 10.0F, FloatyLine.scroll(0.0F, 8.0F, 10.0F, -0.03F), 1.0E-4,
                "the negative speed of the original scrolls the texture forward");
    }

    @Test void thePhaseIsTheOriginalOne() {
        // The original phase is coordinate % 16 + dist * (1 - f2) * quality / 2 - (time % 32767 / 5).
        double phase = FloatyLine.phase(4.0D, 8.0F, 0.5F, 0.0F);
        assertEquals(4.0D + 8.0F * 0.5F * 8.0F, phase, 1.0E-6);
        assertEquals(4.0D, FloatyLine.phase(4.0D, 8.0F, 1.0F, 0.0F), 1.0E-6,
                "at the end of the line the distance term is gone");
        assertEquals(4.0D - 5.0D, FloatyLine.phase(4.0D, 8.0F, 1.0F, 25.0F), 1.0E-6,
                "the original time term is (time % 32767) / 5");
    }
}
