package thaumcraft.api.client;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.Test;
import thaumcraft.api.aspects.Aspect;

/**
 * The aspect tag of {@code AspectRenderer}: the geometry the documentation promises, and what a call with nothing to draw does.
 * The pixels themselves need a running client; the layout the renderer reads is tested in {@code AspectTagLayoutTest}.
 */
class AspectRendererTest {
    @Test void theGeometryIsTheDocumentedOne() {
        assertEquals(16, AspectRenderer.TAG_SIZE, "the tag is a sixteen pixel square");
        assertEquals(16, AspectRenderer.AMOUNT_RIGHT, "the amount ends at the right edge of the tag");
        assertEquals(11, AspectRenderer.AMOUNT_TOP, "and starts eleven pixels down");
        assertEquals(-4, AspectRenderer.BONUS_OFFSET, "the bonus sparkle starts four pixels up and left of the tag");
        assertEquals(16, AspectRenderer.BONUS_SIZE);
        assertEquals(4, AspectRenderer.BONUS_NUMBER_CENTER_X);
        assertEquals(3, AspectRenderer.BONUS_NUMBER_TOP);
        assertEquals(16, AspectRenderer.BONUS_FRAMES);
        // The amount sits in the bottom right corner of the glyph: a half size line of text (9 * 0.5 = 4.5 pixels) from 11 ends
        // at 15.5, inside the 16 of the tag; a top of 12 or more would put the text below it.
        assertTrue(AspectRenderer.AMOUNT_TOP + 9 * 0.5 <= AspectRenderer.TAG_SIZE, "the amount stays inside the tag");
        assertTrue(AspectRenderer.AMOUNT_TOP + 9 * 0.5 > AspectRenderer.TAG_SIZE - 1, "and sits at its bottom edge");
    }

    @Test void drawHandsItsFourIntsToTheRendererInTheirOwnFields() {
        // draw() builds its layout through layout(): the swap of two adjacent ints of the public signature (x, y, amount, bonus)
        // would show here, not only in AspectTagLayoutTest, which calls AspectTagLayout.of directly.
        var tag = AspectRenderer.layout(4, 5, 6, 7);
        assertEquals(4, tag.x(), "x is the third parameter of draw, after graphics and aspect");
        assertEquals(5, tag.y());
        assertEquals(6, tag.amount());
        assertEquals(7, tag.bonus());
        assertEquals(1.0F, tag.scale(), 0.0F, "the public tag is drawn at its own size");
        var other = AspectRenderer.layout(-3, 120, 0, 2);
        assertEquals(-3, other.x());
        assertEquals(120, other.y());
        assertEquals(0, other.amount());
        assertEquals(2, other.bonus());
    }

    @Test void nothingToDrawIsNotAnError() {
        AspectRenderer.draw(null, null, 4, 5, 6, 0);
    }

    @Test void anAspectThatIsDrawnNeedsGraphicsAndSaysSoBeforeTouchingTheGame() {
        // Minecraft.getInstance() is null in a plain JVM, so any other path would fail with a different NullPointerException:
        // the message pins that the graphics were checked first.
        NullPointerException refused = assertThrows(NullPointerException.class, () -> AspectRenderer.draw(null, Aspect.AIR, 4, 5, 6, 0));
        assertEquals("graphics", refused.getMessage());
    }

    @Test void anAspectCannotBeMadeWithoutATextureSoThereIsAlwaysAGlyphToDraw() {
        // The documentation of draw() relies on it: an Aspect always has an image location.
        NullPointerException refused = assertThrows(NullPointerException.class,
                () -> new Aspect("aspectrenderertest", 0xFFFFFF, null, (ResourceLocation) null, 1));
        assertEquals("image", refused.getMessage());
        assertNull(Aspect.getAspect("aspectrenderertest"), "the refused aspect was not registered");
        assertTrue(Aspect.AIR.getImage() != null);
    }
}
