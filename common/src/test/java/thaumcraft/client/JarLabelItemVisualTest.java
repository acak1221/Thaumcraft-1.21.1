package thaumcraft.client;

import static org.junit.jupiter.api.Assertions.*;

import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import javax.imageio.ImageIO;
import net.minecraft.client.renderer.block.model.BlockModel;
import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.Test;
import thaumcraft.api.aspects.Aspect;

/** Native model predicates and original ItemResource:108-126 pass/color contract. */
class JarLabelItemVisualTest {
    @Test void blankAndBoundLabelsChooseDifferentNativeModels() throws Exception {
        BlockModel blank = model("jar_label"), bound = model("jar_label_bound");
        assertTrue(blank.hasTexture("layer0"));
        assertFalse(blank.hasTexture("layer1"), "an unbound label has no colored mark");
        assertTrue(bound.hasTexture("layer0"));
        assertTrue(bound.hasTexture("layer1"));
        assertTrue(bound.getOverrides().isEmpty(), "the selected model cannot recurse back through its parent override");
        var override = assertSingle(blank.getOverrides());
        assertEquals(ResourceLocation.fromNamespaceAndPath("thaumcraft", "item/jar_label_bound"), override.getModel());
        var predicate = assertSingle(override.getPredicates().toList());
        assertEquals(ThaumcraftJarLabelClient.BOUND_PROPERTY, predicate.getProperty());
        assertTrue(ThaumcraftJarLabelClient.bound(null) < predicate.getValue(), "blank remains on its base model");
        for (Aspect aspect : Aspect.aspects.values()) {
            assertEquals(predicate.getValue(), ThaumcraftJarLabelClient.bound(aspect), aspect.getTag());
        }
    }

    @Test void onlyTheBoundOverlayUsesTheAspectColorWithNativeOpaqueAlpha() {
        assertEquals(-1, ThaumcraftJarLabelClient.color(null, 1));
        for (Aspect aspect : Aspect.aspects.values()) {
            assertEquals(-1, ThaumcraftJarLabelClient.color(aspect, 0), "the label frame stays untinted");
            assertEquals(-1, ThaumcraftJarLabelClient.color(aspect, 2));
            assertEquals(255, ThaumcraftJarLabelClient.color(aspect, 1) >>> 24, aspect.getTag());
        }
        assertEquals(0xFFFFFF7E, ThaumcraftJarLabelClient.color(Aspect.AIR, 1));
        assertEquals(0xFFFF5A01, ThaumcraftJarLabelClient.color(Aspect.FIRE, 1));
        assertEquals(0xFF3CD4FC, ThaumcraftJarLabelClient.color(Aspect.WATER, 1));
        assertEquals(0xFF404040, ThaumcraftJarLabelClient.color(Aspect.ENTROPY, 1));
    }

    @Test void theOriginalOverlayContainsVisibleGrayscaleInkThatActuallyNeedsTheTint() throws Exception {
        try (var stream = getClass().getResourceAsStream("/assets/thaumcraft/textures/items/label_over.png")) {
            assertNotNull(stream);
            var image = ImageIO.read(stream);
            assertEquals(0xFF595959, image.getRGB(7, 6), "the original opaque grayscale ink sample");
            int intensity = image.getRGB(7, 6) & 255;
            int fire = ThaumcraftJarLabelClient.color(Aspect.FIRE, 1);
            int water = ThaumcraftJarLabelClient.color(Aspect.WATER, 1);
            assertEquals(89, intensity * ((fire >>> 16) & 255) / 255);
            assertEquals(20, intensity * ((water >>> 16) & 255) / 255);
            assertEquals(0, intensity * (fire & 255) / 255);
            assertEquals(87, intensity * (water & 255) / 255);
        }
    }

    private BlockModel model(String name) throws Exception {
        try (var stream = getClass().getResourceAsStream("/assets/thaumcraft/models/item/" + name + ".json")) {
            assertNotNull(stream, name);
            return BlockModel.fromStream(new InputStreamReader(stream, StandardCharsets.UTF_8));
        }
    }

    private static <T> T assertSingle(java.util.List<T> values) {
        assertEquals(1, values.size());
        return values.getFirst();
    }
}
