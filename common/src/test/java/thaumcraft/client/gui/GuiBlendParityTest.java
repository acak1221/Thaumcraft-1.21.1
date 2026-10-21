package thaumcraft.client.gui;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.image.BufferedImage;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import javax.imageio.ImageIO;
import org.junit.jupiter.api.Test;

/**
 * Why the screens of the port have to blend their own sheets.
 *
 * <p>The original draws every GUI with the ordinary alpha blend: the 1.7.10
 * {@code GuiScreen} enables {@code GL_BLEND} for the whole screen. The 1.21.1
 * {@code GuiGraphics#blit} does not touch the blend state at all - the plain
 * overload ends in {@code innerBlit}, which neither enables nor disables
 * blending - and a {@code fill} leaves the blend off, because the GUI render
 * type of the vanilla game is a translucent one whose teardown disables it.
 * A sheet with translucent pixels is therefore drawn as solid black when the
 * screen does not enable the blend itself.
 *
 * <p>The screens below draw TC4 sheets that really have translucent pixels, so
 * the test pins both halves of the fix: the sheet is translucent and the port
 * enables the blend before it draws it.
 */
class GuiBlendParityTest {
    private static BufferedImage sheet(String name) throws Exception {
        try (InputStream in = GuiBlendParityTest.class.getResourceAsStream("/assets/thaumcraft/textures/gui/" + name)) {
            assertTrue(in != null, "the sheet " + name + " is missing");
            return ImageIO.read(in);
        }
    }

    /** The number of pixels of the region that are not fully opaque. */
    private static int translucent(BufferedImage image, int u, int v, int width, int height) {
        int count = 0;
        for (int x = u; x < u + width; x++)
            for (int y = v; y < v + height; y++)
                if ((image.getRGB(x, y) >>> 24) != 255) count++;
        return count;
    }

    private static String source(String path) throws Exception {
        Path file = Path.of("src/main/java", path);
        assertTrue(Files.exists(file), "the source " + file + " is missing");
        return new String(Files.readAllBytes(file), StandardCharsets.UTF_8);
    }

    @Test void theSheetsOfThePortedScreensReallyAreTranslucent() throws Exception {
        // The focus pouch, oracle GuiFocusPouch, 175x232 art.
        assertTrue(translucent(sheet("gui_focuspouch.png"), 0, 0, 175, 232) > 5000,
                "the frame of the focus pouch is translucent");
        // The locked marker of the focus pouch, the cell 240,0 of the same sheet.
        assertTrue(translucent(sheet("gui_focuspouch.png"), 240, 0, 16, 16) > 200,
                "the locked marker of the focus pouch is translucent");
        // The hover harness, oracle GuiHoverHarness, 176x166 art.
        assertTrue(translucent(sheet("guihoverharness.png"), 0, 0, 176, 166) > 5000,
                "the frame of the hover harness is translucent");
        assertTrue(translucent(sheet("guihoverharness.png"), 240, 0, 16, 16) > 200,
                "the locked marker of the hover harness is translucent");
        // The hand mirror, oracle GuiHandMirror, 176x166 art.
        assertTrue(translucent(sheet("guihandmirror.png"), 0, 0, 176, 166) > 5000,
                "the frame of the hand mirror is translucent");
        // The research book, oracle GuiResearchRecipe, the 512x360 page.
        assertTrue(translucent(sheet("gui_researchbook.png"), 0, 0, 512, 360) > 5000,
                "the page of the research book is translucent");
    }

    @Test void thePortedScreensEnableTheBlendBeforeTheyDrawTheirSheet() throws Exception {
        for (String screen : new String[] {
                "thaumcraft/client/gui/GuiFocusPouch.java",
                "thaumcraft/client/gui/GuiHoverHarness.java",
                "thaumcraft/client/gui/GuiHandMirror.java",
                "thaumcraft/client/gui/GuiResearchRecipe.java",
                "thaumcraft/client/gui/GuiResearchPopup.java"}) {
            String text = source(screen);
            assertTrue(text.contains("RenderSystem.enableBlend()"), screen + " does not enable the blend");
            assertTrue(text.contains("RenderSystem.defaultBlendFunc()"), screen + " does not set the blend function");
        }
    }

    @Test void theSheetsThatThePortDrawsOpaqueNeedNoBlend() throws Exception {
        // The ghost frame of the golem configuration, the cell 184,0 of the sheet
        // of the golem, is the one region of that sheet that the port draws
        // without the blend.
        assertEquals(0, translucent(sheet("guigolem.png"), 184, 0, 24, 24),
                "the ghost frame of the golem configuration is opaque");
    }
}
