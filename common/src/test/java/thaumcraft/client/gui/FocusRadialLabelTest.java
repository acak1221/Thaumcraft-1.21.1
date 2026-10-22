package thaumcraft.client.gui;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;
import org.junit.jupiter.api.Test;

/**
 * The centre line of the held-F focus radial.
 *
 * <p>The original radial (oracle {@code thaumcraft/client/lib/REHWandHandler.java:185-295})
 * draws no centre text at all: it names the hovered focus in the tooltip that it draws
 * at the mouse (oracle {@code REHWandHandler.java:287-289}). The port adds one line that
 * names the focused focus, and that line has to stay ONE line.</p>
 *
 * <p>The client smoke frame of the radial showed two of them on the same row: the name of
 * the focused focus ("Wand Focus: Fire &mdash; slot 9") at x=255 and the placeholder
 * ("Point at a focus") at x=280, so the two runs lay on top of each other from the fifth
 * character on. The font is not at fault: the client smoke renders the radial a second
 * time with a scripted hover point
 * ({@code fabric/src/clientTest/java/thaumcraft/fabric/clienttest/BookClientSmoke.java:272-276}),
 * so the first pass (the real mouse is not over a focus) drew the placeholder and the
 * second pass (the scripted hover) drew the name of the focused focus. Both passes end up
 * in the same frame.</p>
 *
 * <p>Two rules keep that frame readable, and they are what this test pins:</p>
 * <ul>
 *   <li>the centre line carries the focused focus and nothing else, so no render pass can
 *       leave a second, different line behind;</li>
 *   <li>the line is centred with {@code centre - font.width(text) / 2}, the expression the
 *       original uses for its centred text, and it is drawn once.</li>
 * </ul>
 */
class FocusRadialLabelTest {
    /** The name line of the focused focus, as the client smoke frame shows it. */
    private static final String NAME_LINE = "Wand Focus: Fire — slot 9";
    /** The name line is 130 pixels wide in the 1.21.1 default font (measured on the frame). */
    private static final int NAME_LINE_WIDTH = 130;
    /** The removed placeholder is 81 pixels wide in the same font (measured on the frame). */
    private static final int PLACEHOLDER_WIDTH = 81;

    @Test void theCentreLineNamesTheFocusedFocus() {
        Component focused = Component.literal(NAME_LINE);
        assertSame(focused, GuiFocusSelect.centreLine(focused, 1),
                "the centre line is the name of the focused focus");
        assertSame(focused, GuiFocusSelect.centreLine(focused, 4),
                "the focused focus may be any of the eligible foci");
    }

    @Test void theCentreLineHasNoHoverDependentPlaceholder() {
        // Nothing focused but eligible foci present: the line is empty. A placeholder here
        // would be drawn by the render pass without a hover and would stay on screen when
        // the second pass (the scripted hover of the client smoke) draws the name.
        assertNull(GuiFocusSelect.centreLine(null, 1),
                "no hover may not leave a placeholder behind, it would overlap the name line");
        assertNull(GuiFocusSelect.centreLine(null, 12),
                "the same holds for a full page of eligible foci");
    }

    @Test void theEmptyInventoryStillSaysSo() {
        Component empty = GuiFocusSelect.centreLine(null, 0);
        assertNotNull(empty, "an empty inventory still has to say that it has no foci");
        assertTrue(empty.getContents() instanceof TranslatableContents,
                "the empty message is a translated message");
        assertEquals("tc.port.focus.empty", ((TranslatableContents) empty.getContents()).getKey(),
                "the empty message is the translated one of the port");
    }

    @Test void theCentreLineIsCentredWithTheOriginalExpression() {
        for (int textWidth = 0; textWidth < 400; textWidth++) {
            assertEquals(320 - textWidth / 2, GuiFocusSelect.centredOffset(320, textWidth),
                    "a centred line sits at (centre - font.width(text) / 2)");
        }
        // The two lines of the broken frame: the name of the focused focus at 255 and the
        // placeholder at 280. The name line keeps its place after the fix.
        assertEquals(255, GuiFocusSelect.centredOffset(320, NAME_LINE_WIDTH),
                "the name line of the focused focus stays at x=255 on a 640 wide screen");
        assertEquals(280, GuiFocusSelect.centredOffset(320, PLACEHOLDER_WIDTH),
                "the placeholder of the broken frame was drawn at x=280, 25 pixels further right");
    }

    @Test void theCentreLineStaysBelowTheRingAndInsideTheScreen() {
        // The client smoke frame drew the centre line on row 222 of a 360 high screen.
        assertEquals(222, GuiFocusSelect.centreLineY(360, 180, 18.5F),
                "the centre line sits below the ring of the client smoke frame");
        assertEquals(252, GuiFocusSelect.centreLineY(360, 240, 18.5F),
                "the centre line never falls out of the screen");
    }

    @Test void theCentreLineIsDrawnOnceAndHasNoPlaceholder() throws Exception {
        String source = source("thaumcraft/client/gui/GuiFocusSelect.java");
        int draws = source.split("centered\\(graphics, selected", -1).length - 1;
        assertEquals(1, draws, "the centre line is drawn once per render pass");
        assertFalse(source.contains("tc.port.focus.choose"),
                "the hover-dependent placeholder is gone: it would survive a render pass without a hover");
        assertTrue(source.contains("centredOffset(centerX, font.width(label))"),
                "the line is centred with the measured width of its own text");
    }

    private static String source(String path) throws Exception {
        Path file = Path.of("src/main/java", path);
        assertTrue(Files.exists(file), "the source " + file + " is missing");
        return new String(Files.readAllBytes(file), StandardCharsets.UTF_8);
    }
}
