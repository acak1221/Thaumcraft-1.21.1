package thaumcraft.client.renderers.tile;

import static org.junit.jupiter.api.Assertions.*;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

class DeconstructionTableRewardTest {
    @Test void originalDrawTagKeepsItsTextureOrientationColourAlphaAndInheritedLight() {
        var vertices = new RecordingVertices();
        TileDeconstructionTableRenderer.rewardVertices(new PoseStack(), vertices, 0xA1B2C3, 0x00500020);
        float[][] expected = {{-8,8,0,0,1},{8,8,0,1,1},{8,-8,0,1,0},{-8,-8,0,0,0}};
        assertEquals(4, vertices.values.size());
        for (int i = 0; i < 4; i++) assertArrayEquals(expected[i], vertices.values.get(i));
        assertEquals(List.of(0xA1B2C3CC, 0xA1B2C3CC, 0xA1B2C3CC, 0xA1B2C3CC), vertices.colours);
        assertEquals(List.of(0x00500020, 0x00500020, 0x00500020, 0x00500020), vertices.lights);
    }

    private static final class RecordingVertices implements VertexConsumer {
        final List<float[]> values = new ArrayList<>();
        final List<Integer> colours = new ArrayList<>(), lights = new ArrayList<>();
        @Override public VertexConsumer addVertex(float x, float y, float z) { values.add(new float[]{x,y,z,0,0}); return this; }
        @Override public VertexConsumer setColor(int r, int g, int b, int a) { colours.add(r << 24 | g << 16 | b << 8 | a); return this; }
        @Override public VertexConsumer setUv(float u, float v) { var p = values.getLast(); p[3] = u; p[4] = v; return this; }
        @Override public VertexConsumer setUv1(int u, int v) { fail("drawTag has no entity overlay"); return this; }
        @Override public VertexConsumer setUv2(int u, int v) { lights.add(u | v << 16); return this; }
        @Override public VertexConsumer setNormal(float x, float y, float z) { fail("drawTag has no normal shading"); return this; }
    }
}
