package thaumcraft.client.renderers.tile;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import thaumcraft.api.aspects.Aspect;

/**
 * The item form of the aura node.
 *
 * <p>Oracle {@code thaumcraft/client/renderers/tile/ItemNodeRenderer.java:37-170}: the item of the
 * invisible aura node is drawn as the node itself, with the fixture node of the four primal aspects
 * at forty each ({@code :39}), the alpha of one half ({@code :75}), the atlas of thirty two frames
 * ({@code :100}), the pulse of a strip ({@code :115}), the size of a strip ({@code :116}), the
 * alpha of a strip ({@code :117}) and the size of the core strip ({@code :127}).</p>
 *
 * <p>No test constructs a {@code Block}: every rule under test is a pure static function.</p>
 */
class ItemNodeRendererTest {
    @Test void theFixtureNodeOfTheOriginalIsAirFireEarthAndWaterAtForty() {
        assertEquals(4, ItemNodeRenderer.ITEM_ASPECTS.size(),
                "the fixture node of the original holds four aspects");
        assertEquals(40, ItemNodeRenderer.ITEM_ASPECTS.getAmount(Aspect.AIR), "forty of air");
        assertEquals(40, ItemNodeRenderer.ITEM_ASPECTS.getAmount(Aspect.FIRE), "forty of fire");
        assertEquals(40, ItemNodeRenderer.ITEM_ASPECTS.getAmount(Aspect.EARTH), "forty of earth");
        assertEquals(40, ItemNodeRenderer.ITEM_ASPECTS.getAmount(Aspect.WATER), "forty of water");
        assertEquals(0, ItemNodeRenderer.ITEM_ASPECTS.getAmount(Aspect.ORDER),
                "the fixture node of the original holds no order");
        assertEquals(0, ItemNodeRenderer.ITEM_ASPECTS.getAmount(Aspect.ENTROPY),
                "the fixture node of the original holds no entropy");
    }

    @Test void theAlphaTheScaleAndTheAtlasAreTheOriginalOnes() {
        assertEquals(0.5F, ItemNodeRenderer.ITEM_ALPHA, "the node of the item starts at one half");
        assertEquals(2.0F, ItemNodeRenderer.ITEM_SCALE, "the node of the item is scaled by two");
        assertEquals(32, ItemNodeRenderer.ATLAS_FRAMES, "the node atlas has thirty two frames");
        assertEquals(0.25F, ItemNodeRenderer.PULSE_BASE, "the base of the pulse is a quarter");
    }

    @Test void theFrameOfTheAtlasIsTheOriginalOne() {
        // Original ItemNodeRenderer:103 - (nanoseconds / 40000000 + 1) % 32.
        assertEquals(1, ItemNodeRenderer.atlasFrame(0L), "the first frame of the atlas is the one");
        assertEquals(2, ItemNodeRenderer.atlasFrame(40000000L), "the atlas advances every forty milliseconds");
        assertEquals(1, ItemNodeRenderer.atlasFrame(32L * 40000000L), "the atlas wraps after thirty two frames");
        for (long nanos = 0; nanos < 64L * 40000000L; nanos += 40000000L) {
            int frame = ItemNodeRenderer.atlasFrame(nanos);
            assertTrue(frame >= 0 && frame < 32, "the frame of the atlas is always one of the thirty two");
        }
    }

    @Test void thePulseOfAStripIsTheOriginalOne() {
        // Original ItemNodeRenderer:115 - sin(ticks / (14 - count)) * 0.25 + 0.5.
        assertEquals(0.5F, ItemNodeRenderer.stripPulse(0.0F, 0), 1.0E-6F,
                "the pulse of the first strip starts at one half");
        // The period of the pulse of the first strip is 2 * PI * 14 ticks, so a
        // quarter of a period is 14 * PI / 2 ticks: the sine is at its top there
        // and the pulse of the strip is at its top too.
        assertEquals(0.75F, ItemNodeRenderer.stripPulse((float) (14.0 * Math.PI / 2.0), 0), 1.0E-6F,
                "the pulse of the first strip is three quarters at a quarter of a period");
        // Three quarters of a period is 3 * 14 * PI / 2 ticks: the sine is at its
        // bottom and the pulse of the strip falls to its own quarter.
        assertEquals(0.25F, ItemNodeRenderer.stripPulse((float) (3.0 * 14.0 * Math.PI / 2.0), 0), 1.0E-6F,
                "the pulse of the first strip falls to a quarter at three quarters of a period");
        // The pulse repeats itself after a whole period of the original.
        assertEquals(ItemNodeRenderer.stripPulse(3.0F, 0),
                ItemNodeRenderer.stripPulse((float) (3.0 + 2.0 * Math.PI * 14.0), 0), 1.0E-6F,
                "the pulse of the first strip repeats itself after its period");
        // The divisor shrinks with the index of the aspect, so the later strips pulse faster.
        assertTrue(ItemNodeRenderer.stripPulse(1.0F, 3) != ItemNodeRenderer.stripPulse(1.0F, 0),
                "the pulse of the later strips uses the smaller divisor of the original");
    }

    @Test void theSizeOfAStripIsTheOriginalOne() {
        // Original ItemNodeRenderer:116 - 0.2 + pulse * amount / 50.
        assertEquals(0.6F, ItemNodeRenderer.stripSize(0.5F, 40), 1.0E-6F,
                "forty of an aspect with the pulse of one half is a size of six tenths");
        assertEquals(0.2F, ItemNodeRenderer.stripSize(0.5F, 0), 1.0E-6F,
                "an aspect without essence keeps the base of the size");
        assertEquals(1.2F, ItemNodeRenderer.stripSize(0.5F, 100), 1.0E-6F,
                "a full hundred of an aspect doubles the pulse and adds the base of the size");
    }

    @Test void theAlphaOfAStripIsTheOriginalOne() {
        // Original ItemNodeRenderer:117 - the alpha of the node divided by the number of the aspects.
        assertEquals(0.125F, ItemNodeRenderer.stripAlpha(0.5F, 4), 1.0E-6F,
                "the four aspect strips of the item share the alpha of the node");
        assertEquals(0.5F, ItemNodeRenderer.stripAlpha(0.5F, 1), 1.0E-6F,
                "a single aspect keeps the whole alpha of the node");
    }

    @Test void theSizeOfTheCoreStripIsTheOriginalOne() {
        // Original ItemNodeRenderer:127 - 0.1 + average / 150.
        assertEquals(0.1F + (160.0F / 4.0F) / 150.0F, ItemNodeRenderer.coreSize(160.0F, 4), 1.0E-6F,
                "the core strip of the item grows with the average of the aspects");
        assertEquals(0.1F, ItemNodeRenderer.coreSize(0.0F, 4), 1.0E-6F,
                "an empty node keeps the base of the core strip");
    }
}
