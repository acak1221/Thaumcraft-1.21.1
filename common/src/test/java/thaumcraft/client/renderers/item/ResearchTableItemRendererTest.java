package thaumcraft.client.renderers.item;

import static org.junit.jupiter.api.Assertions.*;

import com.google.gson.JsonParser;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;
import net.minecraft.client.renderer.MultiBufferSource;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class ResearchTableItemRendererTest {
    @BeforeAll static void bootstrap() {
        net.minecraft.SharedConstants.tryDetectVersion();
        net.minecraft.server.Bootstrap.bootStrap();
    }

    @Test void originalInventoryFixtureIncludesEveryToolsPaperAndScrollSurface() {
        var layers = render(true, 0x999999);
        assertEquals(5, layers.size());
        assertEquals(144, layer(layers, "restable.png").vertices, "original tabletop, four legs and crossbar");
        assertEquals(24, layer(layers, "entity_translucent", "restable.png").vertices, "translucent inkwell");
        assertEquals(264, layer(layers, "tablequill.png").vertices, "extruded quill with all pixel edges");
        assertEquals(24, layer(layers, "parchment.png").vertices, "six independent parchment sheets");
        var scroll = layer(layers, "restable2.png");
        assertEquals(48, scroll.vertices, "scroll tube and ribbon");
        assertEquals(24, scroll.grayVertices, "unassigned research-note ribbon uses original #999999");
        assertEquals(0, layer(layers, "restable.png").minX, 1E-6F);
        assertEquals(2, layer(layers, "restable.png").maxX, 1E-6F, "two-block research table stays full width");
    }

    @Test void sharedWorldPartsKeepEmptySlotsEmptyWhilePaperRemains() {
        var layers = render(false, null);
        assertEquals(2, layers.size());
        assertEquals(144, layer(layers, "restable.png").vertices);
        assertEquals(24, layer(layers, "parchment.png").vertices);
    }

    @Test void builtinDisplayRetainsTheTwoBlockFurnitureFraming() throws Exception {
        try (var stream = getClass().getClassLoader().getResourceAsStream("assets/thaumcraft/models/item/research_table.json")) {
            assertNotNull(stream);
            var model = JsonParser.parseReader(new InputStreamReader(stream, StandardCharsets.UTF_8)).getAsJsonObject();
            assertEquals("minecraft:builtin/entity", model.get("parent").getAsString());
            var display = model.getAsJsonObject("display");
            var gui = display.getAsJsonObject("gui");
            assertEquals(-3, gui.getAsJsonArray("translation").get(0).getAsInt());
            assertEquals(.43, gui.getAsJsonArray("scale").get(0).getAsDouble());
            assertEquals(5, display.size(), "GUI, ground, fixed and both right-hand contexts remain authored");
        }
    }

    private static Map<String, Mesh> render(boolean tools, Integer color) {
        var layers = new LinkedHashMap<String, Mesh>();
        MultiBufferSource buffers = type -> layers.computeIfAbsent(type.toString(), ignored -> new Mesh());
        ResearchTableItemRenderer.renderTableParts(0, tools, color, new PoseStack(), buffers, 0, 0);
        return layers;
    }

    private static Mesh layer(Map<String, Mesh> layers, String... parts) {
        return layers.entrySet().stream().filter(entry -> {
            for (String part : parts) if (!entry.getKey().contains(part)) return false;
            return true;
        }).findFirst().orElseThrow(() -> new AssertionError("Missing layer " + String.join(",", parts) + " in " + layers.keySet())).getValue();
    }

    private static final class Mesh implements VertexConsumer {
        int vertices, grayVertices;
        float minX = Float.POSITIVE_INFINITY, maxX = Float.NEGATIVE_INFINITY;
        @Override public VertexConsumer addVertex(float x, float y, float z) {
            vertices++; minX = Math.min(minX, x); maxX = Math.max(maxX, x); return this;
        }
        @Override public VertexConsumer setColor(int r, int g, int b, int a) {
            if (r == 0x99 && g == 0x99 && b == 0x99) grayVertices++;
            return this;
        }
        @Override public VertexConsumer setUv(float u, float v) { return this; }
        @Override public VertexConsumer setUv1(int u, int v) { return this; }
        @Override public VertexConsumer setUv2(int u, int v) { return this; }
        @Override public VertexConsumer setNormal(float x, float y, float z) { return this; }
    }
}
