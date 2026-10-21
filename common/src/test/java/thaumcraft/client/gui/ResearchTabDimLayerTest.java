package thaumcraft.client.gui;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.image.BufferedImage;
import java.io.InputStream;
import javax.imageio.ImageIO;
import org.junit.jupiter.api.Test;

/**
 * Why the tabs of the research browser need blending.
 *
 * <p>Original GuiResearchBrowser:505 draws the region 200,232 of gui_research.png over every tab but the selected
 * one. It is a shape of black with an alpha of at most 137 of 255, so it only dims the tab when the draw blends it.
 * {@code GuiGraphics#blit} does not enable blending (and a {@code fill} leaves it off), which turned the dim layer
 * into a solid black silhouette that hid the icon of every unselected tab.
 */
class ResearchTabDimLayerTest {
    private static BufferedImage frame() throws Exception {
        try (InputStream in = ResearchTabDimLayerTest.class.getResourceAsStream("/assets/thaumcraft/textures/gui/gui_research.png")) {
            return ImageIO.read(in);
        }
    }

    @Test void theDimLayerOfAnUnselectedTabIsTranslucentBlack() throws Exception {
        BufferedImage image = frame();
        int visible = 0, maxAlpha = 0;
        for (int x = 200; x < 224; x++) for (int y = 232; y < 256; y++) {
            int argb = image.getRGB(x, y);
            int alpha = argb >>> 24;
            if (alpha == 0) continue;
            visible++;
            maxAlpha = Math.max(maxAlpha, alpha);
            assertEquals(0, argb & 0xFFFFFF, "the dim layer is black at " + x + "," + y);
        }
        assertTrue(visible > 300, "the dim layer is a whole tab shape, " + visible + " visible pixels");
        assertEquals(137, maxAlpha, "no pixel of the dim layer is opaque: without blending every visible pixel is drawn solid");
    }

    @Test void theTabFramesThemselvesAreOpaqueWood() throws Exception {
        BufferedImage image = frame();
        for (int x : new int[] {152, 176}) {
            int opaque = 0;
            for (int dx = 0; dx < 24; dx++) for (int y = 232; y < 256; y++) if (image.getRGB(x + dx, y) >>> 24 == 255) opaque++;
            assertTrue(opaque > 200, "the tab frame at " + x + " is mostly opaque, " + opaque);
        }
    }
}
