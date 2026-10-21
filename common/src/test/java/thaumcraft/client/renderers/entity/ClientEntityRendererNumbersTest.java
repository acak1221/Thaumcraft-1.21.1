package thaumcraft.client.renderers.entity;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

/**
 * The numbers of the original projectile and orb renderers, pinned in the port.
 *
 * <p>Every value below is the one of the 1.7.10 original:
 * <ul>
 *   <li>oracle {@code RenderDart.java:20-44}: the arrow is drawn with the pixel
 *       scale 0.025625, three quarters of that along the shaft, the colour
 *       0.5/0.5/0.6 of {@code glColor3f} and the forty five degree turn around
 *       the x axis with the shift of four pixels;</li>
 *   <li>oracle {@code RenderElectricOrb.java:29-44}: the strip of four frames
 *       starts at (1 + ticks % 6) / 8, the step of the strip is 0.125, the row
 *       is 0.875 (0.75 for the red golem orb), the bob is
 *       sin(ticks / 5) * 0.2 + 0.2 and the quad is scaled with 1 + bob;</li>
 *   <li>oracle {@code RenderEmber.java:28-39}: the frame is (7 + p) / 16 with
 *       p = 8 * (ticks / duration), the step of the frame is 0.0625, the row is
 *       0.5625, the size is 0.25 + ticks / duration and the alpha is 0.9;</li>
 *   <li>oracle {@code RenderExplosiveOrb.java:27-37}: the strip of four frames
 *       starts at (ticks % 4) / 16, the step of the strip is 0.0625, the row is
 *       0.8125, the quad is scaled with 0.7 and the alpha is 0.8;</li>
 *   <li>oracle {@code RenderAspectOrb.java:34-50}: the frame of the strip is
 *       the nanosecond clock divided by 25 milliseconds, the size is
 *       0.1 + 0.3 * (maxAge - age) / maxAge and the colour of the aspect is
 *       drawn with the alpha 128.</li>
 * </ul>
 */
class ClientEntityRendererNumbersTest {
    private static String source(String path) throws Exception {
        Path file = Path.of("src/main/java", path);
        assertTrue(Files.exists(file), "the source " + file + " is missing");
        return new String(Files.readAllBytes(file), StandardCharsets.UTF_8);
    }

    private static void contains(String text, String needle, String what) {
        assertTrue(text.contains(needle), what + " (expected " + needle + ")");
    }

    @Test void theDartKeepsTheOriginalGeometryAndColour() throws Exception {
        String text = source("thaumcraft/client/renderers/entity/RenderDart.java");
        contains(text, "0.025625F", "the dart keeps the original pixel scale");
        contains(text, "0.75F", "the dart keeps the original shaft length");
        contains(text, "127, GREEN = 127, BLUE = 153", "the dart keeps the original colour 0.5/0.5/0.6");
        contains(text, "rotationDegrees(45.0F)", "the dart keeps the original turn of forty five degrees");
        contains(text, "translate(-4.0F, 0.0F, 0.0F)", "the dart keeps the original shift of four pixels");
        contains(text, "0.15625F", "the dart keeps the original texture row of the cap");
    }

    @Test void theElectricOrbKeepsTheOriginalStripAndBob() throws Exception {
        String text = source("thaumcraft/client/renderers/entity/RenderElectricOrb.java");
        contains(text, "(1 + entity.tickCount % 6) / 8.0F", "the electric orb keeps the original strip start");
        contains(text, ".125F", "the electric orb keeps the original step of the strip");
        contains(text, "isRed(entity) ? .75F : .875F", "the electric orb keeps the original rows of the strip");
        contains(text, "Mth.sin(entity.tickCount / 5.0F) * .2F + .2F", "the electric orb keeps the original bob");
        contains(text, ".8F", "the electric orb keeps the original alpha of 0.8");
        contains(text, "setLight(220)", "the electric orb keeps the original brightness of 220");
    }

    @Test void theEmberKeepsTheOriginalFrameAndSize() throws Exception {
        String text = source("thaumcraft/client/renderers/entity/RenderEmber.java");
        contains(text, "(7 + frame) / 16F", "the ember keeps the original frame of the fire");
        contains(text, ".0625F", "the ember keeps the original step of the frame");
        contains(text, ".5625F", "the ember keeps the original row of the fire");
        contains(text, ".25F + fraction", "the ember keeps the original size of 0.25 + age / duration");
        contains(text, ".9F", "the ember keeps the original alpha of 0.9");
        contains(text, "setLight(220)", "the ember keeps the original brightness of 220");
    }

    @Test void theExplosiveOrbKeepsTheOriginalStripAndScale() throws Exception {
        String text = source("thaumcraft/client/renderers/entity/RenderExplosiveOrb.java");
        contains(text, "(entity.tickCount % 4) / 16.0F", "the explosive orb keeps the original strip start");
        contains(text, ".0625F", "the explosive orb keeps the original step of the strip");
        contains(text, ".8125F", "the explosive orb keeps the original row of the strip");
        contains(text, "pose.scale(.7F, .7F, .7F)", "the explosive orb keeps the original scale of 0.7");
        contains(text, ".8F", "the explosive orb keeps the original alpha of 0.8");
        contains(text, "TRANSLUCENT_TRANSPARENCY", "the explosive orb keeps the original standard alpha blend");
    }

    @Test void theAspectOrbKeepsTheOriginalStripAndSize() throws Exception {
        String text = source("thaumcraft/client/renderers/entity/RenderAspectOrb.java");
        contains(text, "System.nanoTime() / 25000000L % 16L", "the aspect orb keeps the original clock of the strip");
        contains(text, ".1F + .3F *", "the aspect orb keeps the original size of 0.1 + 0.3 * life");
        contains(text, "128", "the aspect orb keeps the original alpha of 128");
        contains(text, "aspect.getBlend()", "the aspect orb keeps the original blend of the aspect");
    }
}
