package thaumcraft.client.renderers.item;

import static org.junit.jupiter.api.Assertions.*;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import net.minecraft.client.renderer.block.model.ItemTransform;
import net.minecraft.world.item.ItemDisplayContext;
import org.joml.Vector3f;
import org.junit.jupiter.api.Test;
import thaumcraft.client.renderers.models.ModelArcaneWorkbench;

class DeconstructionTableItemRendererTest {
    @Test void nativeOriginalTableMeshStillOccupiesTheSameBlock() {
        var pose = new PoseStack();
        DeconstructionTableItemRenderer.tablePose(pose);
        var mesh = new Bounds();
        new ModelArcaneWorkbench().root.render(pose, mesh, 0, 0);
        assertEquals(6 * 6 * 4, mesh.count, "the original tabletop, base and four legs");
        for (int axis = 0; axis < 3; axis++) {
            assertEquals(0, mesh.minimum[axis], 1E-6F);
            assertEquals(1, mesh.maximum[axis], 1E-6F);
        }
    }

    @Test void stationaryScannerIsCentredAboveTheSameTabletop() throws Exception {
        var pose = new PoseStack();
        DeconstructionTableItemRenderer.meterPose(pose);
        JsonObject scanner = resource("thaumometer");
        var display = scanner.getAsJsonObject("display").getAsJsonObject("ground");
        ItemTransform ground = new ItemTransform(new Vector3f(),
                vector(display.getAsJsonArray("translation")).div(16), vector(display.getAsJsonArray("scale")));
        ground.apply(false, pose);
        pose.translate(-.5F, -.5F, -.5F);
        ItemThaumometerRenderer.staticPose(pose, ItemDisplayContext.GROUND);
        Vector3f face = pose.last().pose().transformPosition(new Vector3f(0, .11F, 0));
        assertEquals(.5F, face.x, 1E-6F);
        assertEquals(.5F, face.z, 1E-6F);
        // Original stationary RenderItem bob .1, Forge frame S1.25/Ty.05/Ry-90/S.5,
        // then ItemThaumometerRenderer ENTITY S.5. The modern display lift is cancelled.
        assertEquals(.92F + .8F * (.1F + 1.25F * .05F) + .8F * 1.25F * .5F * .5F * .11F, face.y, 1E-6F);
        assertTrue(face.y > 1, "the native entity-item display lifts the scanner face above the tabletop");
        assertEquals(.0035F, 1.081F - face.y, 1E-6F, "the reward glyph sits above the scanner face, rather than being occluded by it");
        Vector3f across = pose.last().pose().transformDirection(new Vector3f(1, 0, 0));
        assertEquals(0, across.x, 1E-6F);
        assertEquals(0, across.y, 1E-6F);
        assertEquals(.25F, across.z, 1E-6F, "original Forge frame rotation and complete scale");
    }

    @Test void builtinItemRetainsEveryPreviouslyAuthoredDisplayTransform() throws Exception {
        JsonObject item = resource("deconstruction_table");
        assertEquals("minecraft:builtin/entity", item.get("parent").getAsString());
        assertFalse(item.has("elements"), "the dedicated renderer supplies the scanner and original mesh");
        assertEquals(resource("arcane_workbench").get("display"), item.get("display"));
    }

    private static JsonObject resource(String item) throws Exception {
        try (var stream = DeconstructionTableItemRendererTest.class.getClassLoader()
                .getResourceAsStream("assets/thaumcraft/models/item/" + item + ".json")) {
            assertNotNull(stream, item);
            return JsonParser.parseReader(new InputStreamReader(stream, StandardCharsets.UTF_8)).getAsJsonObject();
        }
    }

    private static Vector3f vector(com.google.gson.JsonArray values) {
        return new Vector3f(values.get(0).getAsFloat(), values.get(1).getAsFloat(), values.get(2).getAsFloat());
    }

    private static final class Bounds implements VertexConsumer {
        final float[] minimum = {Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY};
        final float[] maximum = {Float.NEGATIVE_INFINITY, Float.NEGATIVE_INFINITY, Float.NEGATIVE_INFINITY};
        int count;
        @Override public VertexConsumer addVertex(float x, float y, float z) {
            float[] position = {x, y, z};
            for (int axis = 0; axis < 3; axis++) {
                minimum[axis] = Math.min(minimum[axis], position[axis]);
                maximum[axis] = Math.max(maximum[axis], position[axis]);
            }
            count++;
            return this;
        }
        @Override public VertexConsumer setColor(int red, int green, int blue, int alpha) { return this; }
        @Override public VertexConsumer setUv(float u, float v) { return this; }
        @Override public VertexConsumer setUv1(int u, int v) { return this; }
        @Override public VertexConsumer setUv2(int u, int v) { return this; }
        @Override public VertexConsumer setNormal(float x, float y, float z) { return this; }
    }
}
