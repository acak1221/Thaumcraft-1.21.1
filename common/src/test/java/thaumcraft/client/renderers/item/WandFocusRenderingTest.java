package thaumcraft.client.renderers.item;

import static org.junit.jupiter.api.Assertions.*;

import com.google.gson.JsonParser;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import javax.imageio.ImageIO;
import net.minecraft.SharedConstants;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.resources.metadata.animation.AnimationMetadataSection;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.item.ItemStack;
import org.joml.Matrix4f;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import thaumcraft.client.renderers.models.gear.ModelWand;

/** Records the vertices, shaders, colors, UVs and light of the production focus drawing path. */
class WandFocusRenderingTest {
    @BeforeAll static void bootstrap() {
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
        assertNotNull(FocusRenderingTestRegistry.depthFoci(), "focus fixtures register in the launcher session before native registry freeze");
    }

    @Test void allFourOriginalDepthFociDrawTheirOwnAnimatedCubeThenTranslucentPulsingShell() throws Exception {
        var foci = FocusRenderingTestRegistry.depthFoci();
        var names = List.of("primal", "pech", "warding", "portablehole");
        int[] frameCounts = {30, 9, 9, 16};
        int[] frameAtSeven = {6, 7, 7, 7};
        for (int index = 0; index < foci.size(); index++) {
            var focus = foci.get(index);
            var stack = new ItemStack(focus);
            var texture = focus.getFocusDepthLayerIcon(stack);
            assertEquals("thaumcraft:textures/items/focus_" + names.get(index) + "_depth.png", texture.toString());
            var animation = animation(texture);
            for (boolean staff : List.of(false, true)) {
                var pose = new PoseStack();
                pose.translate(2, 3, 4);
                var before = new Matrix4f(pose.last().pose());
                var recording = new Recording();
                PrimalWandFocusRenderer.renderFocus(new ModelWand(), focus, stack, pose, recording, 12345, 0, 7, staff,
                        requested -> { assertEquals(texture, requested); return animation; });
                assertEquals(before, pose.last().pose(), "both inner and outer poses restore the caller");
                assertEquals(2, recording.batches.size());
                var depth = recording.batches.get(0);
                assertSame(RenderType.entityCutoutNoCull(texture), depth.type);
                assertEquals(24, depth.vertices.size(), "all six depth cube faces are emitted");
                assertTrue(depth.vertices.stream().allMatch(v -> v.alpha == 255 && v.light == 12345));
                float minY = depth.vertices.stream().map(v -> v.y).min(Float::compare).orElseThrow();
                float maxY = depth.vertices.stream().map(v -> v.y).max(Float::compare).orElseThrow();
                assertEquals(staff ? 2.76175F : 2.83F, minY, .00001F);
                assertEquals(staff ? 2.93825F : 2.99F, maxY, .00001F);
                float v0 = depth.vertices.stream().map(v -> v.v).min(Float::compare).orElseThrow();
                float v1 = depth.vertices.stream().map(v -> v.v).max(Float::compare).orElseThrow();
                assertEquals((float) frameAtSeven[index] / frameCounts[index], v0, .00001F);
                assertEquals((float) (frameAtSeven[index] + 1) / frameCounts[index], v1, .00001F);
                var shell = recording.batches.get(1);
                assertSame(RenderType.entityTranslucent(ModelWand.FOCUS_TEXTURE), shell.type,
                        "an emissive shader would discard the original lightmap pulse");
                assertEquals(24, shell.vertices.size());
                assertTrue(shell.vertices.stream().allMatch(v -> v.alpha == 153 && v.light == 212));
                int rgb = focus.getFocusColor(stack);
                assertTrue(shell.vertices.stream().allMatch(v -> v.red == (rgb >> 16 & 255)
                        && v.green == (rgb >> 8 & 255) && v.blue == (rgb & 255)));
            }
        }
    }

    @Test void ordinaryAndAddonDepthFociFollowThePublicHookAndPulseAcrossTime() {
        for (boolean depth : List.of(false, true)) {
            var texture = ResourceLocation.fromNamespaceAndPath("addon", "textures/focus.png");
            var focus = depth ? FocusRenderingTestRegistry.addon() : FocusRenderingTestRegistry.ordinary();
            var lights = new ArrayList<Integer>();
            for (int tick : List.of(0, 5, 14)) {
                var recording = new Recording();
                PrimalWandFocusRenderer.renderFocus(new ModelWand(), focus, new ItemStack(focus), new PoseStack(),
                        recording, 100, 0, tick, false, requested -> {
                            assertEquals(texture, requested);
                            return PrimalWandFocusRenderer.Animation.STATIC;
                        });
                assertEquals(depth ? 2 : 1, recording.batches.size());
                var shell = recording.batches.getLast();
                assertTrue(shell.vertices.stream().allMatch(v -> v.alpha == (depth ? 153 : 242)));
                lights.add(shell.vertices.getFirst().light);
            }
            assertEquals(List.of(205, 214, 195), lights);
        }
    }

    @Test void nativeAnimationMetadataPreservesFrameDurationsAndSupportsTiledAddonSheets() {
        var metadata = AnimationMetadataSection.SERIALIZER.fromJson(JsonParser.parseString(
                "{\"width\":8,\"height\":8,\"frametime\":2,\"frames\":[3,{\"index\":0,\"time\":3},1]}").getAsJsonObject());
        var animation = PrimalWandFocusRenderer.Animation.from(16, 16, metadata);
        var topLeft = new PrimalWandFocusRenderer.UV(0, 0, .5F, .5F);
        var bottomRight = new PrimalWandFocusRenderer.UV(.5F, .5F, 1, 1);
        assertEquals(bottomRight, animation.at(0));
        assertEquals(bottomRight, animation.at(1));
        assertEquals(topLeft, animation.at(2));
        assertEquals(topLeft, animation.at(4));
        assertEquals(new PrimalWandFocusRenderer.UV(.5F, 0, 1, .5F), animation.at(5));
        assertEquals(bottomRight, animation.at(7));
        assertEquals(animation.at(6), animation.at(-1));
    }

    @Test void throwingVertexConsumerRestoresCallerPose() {
        var focus = FocusRenderingTestRegistry.depthFoci().getFirst();
        var pose = new PoseStack();
        pose.translate(1, 2, 3);
        var before = new Matrix4f(pose.last().pose());
        var failed = new VertexConsumer() {
            public VertexConsumer addVertex(float x, float y, float z) { throw new IllegalStateException("draw failed"); }
            public VertexConsumer setColor(int r, int g, int b, int a) { return this; }
            public VertexConsumer setUv(float u, float v) { return this; }
            public VertexConsumer setUv1(int u, int v) { return this; }
            public VertexConsumer setUv2(int u, int v) { return this; }
            public VertexConsumer setNormal(float x, float y, float z) { return this; }
        };
        assertThrows(IllegalStateException.class, () -> PrimalWandFocusRenderer.renderFocus(new ModelWand(), focus,
                new ItemStack(focus), pose, type -> failed, 0, 0, 0, false, texture -> PrimalWandFocusRenderer.Animation.STATIC));
        assertEquals(before, pose.last().pose());
    }

    private static PrimalWandFocusRenderer.Animation animation(ResourceLocation texture) throws Exception {
        String path = "assets/" + texture.getNamespace() + "/" + texture.getPath();
        try (var png = WandFocusRenderingTest.class.getClassLoader().getResourceAsStream(path);
             var json = WandFocusRenderingTest.class.getClassLoader().getResourceAsStream(path + ".mcmeta");
             var reader = new InputStreamReader(json, StandardCharsets.UTF_8)) {
            var image = ImageIO.read(png);
            var metadata = AnimationMetadataSection.SERIALIZER.fromJson(JsonParser.parseReader(reader)
                    .getAsJsonObject().getAsJsonObject("animation"));
            return PrimalWandFocusRenderer.Animation.from(image.getWidth(), image.getHeight(), metadata);
        }
    }

    private static final class Recording implements MultiBufferSource {
        final List<Batch> batches = new ArrayList<>();
        @Override public VertexConsumer getBuffer(RenderType type) {
            var batch = new Batch(type);
            batches.add(batch);
            return batch;
        }
    }
    private static final class Vertex {
        float x, y, z, u, v;
        int red, green, blue, alpha, light;
    }
    private static final class Batch implements VertexConsumer {
        final RenderType type;
        final List<Vertex> vertices = new ArrayList<>();
        private Vertex vertex;
        Batch(RenderType type) { this.type = type; }
        public VertexConsumer addVertex(float x, float y, float z) {
            vertex = new Vertex(); vertex.x = x; vertex.y = y; vertex.z = z; vertices.add(vertex); return this;
        }
        public VertexConsumer setColor(int r, int g, int b, int a) { vertex.red = r; vertex.green = g; vertex.blue = b; vertex.alpha = a; return this; }
        public VertexConsumer setUv(float u, float v) { vertex.u = u; vertex.v = v; return this; }
        public VertexConsumer setUv1(int u, int v) { return this; }
        public VertexConsumer setUv2(int u, int v) { vertex.light = u | v << 16; return this; }
        public VertexConsumer setNormal(float x, float y, float z) { return this; }
    }
}
