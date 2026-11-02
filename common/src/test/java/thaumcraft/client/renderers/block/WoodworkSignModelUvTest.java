package thaumcraft.client.renderers.block;

import static org.junit.jupiter.api.Assertions.*;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.model.geom.ModelPart;
import org.junit.jupiter.api.Test;

class WoodworkSignModelUvTest {
    @Test void nativeStandingGeometrySamplesOnlyItsBlockSpriteAtNaturalPixelScale() {
        ModelPart root = WoodworkSignModels.standingLayer().bakeRoot();
        var board = vertices(root.getChild("sign"));
        assertEquals(24, board.vertices);
        assertEquals(-.75F, board.minX);
        assertEquals(.75F, board.maxX);
        assertEquals(1, board.frontMaxU);
        assertEquals(.5F, board.frontMaxV, "24x12 native board covers 16x8 plank pixels after its native 2/3 scale");
        assertEquals(24, vertices(root.getChild("stick")).vertices);
    }

    @Test void nativeHangingBoardAndWallSupportNeverSampleOutsideTheSixteenPixelSprite() {
        ModelPart root = WoodworkSignModels.hangingLayer().bakeRoot();
        var board = vertices(root.getChild("board"));
        assertEquals(24, board.vertices);
        assertEquals(14F / 16, board.frontMaxU);
        assertEquals(10F / 16, board.frontMaxV);
        assertEquals(24, vertices(root.getChild("plank")).vertices);
        assertFalse(root.hasChild("normalChains"), "native alpha chains use their own material and unchanged geometry");
    }

    private static Uvs vertices(ModelPart part) {
        var collector = new Uvs();
        part.render(new PoseStack(), collector, 15728880, 0);
        assertTrue(collector.vertices > 0);
        return collector;
    }

    private static final class Uvs implements VertexConsumer {
        int vertices;
        float minX = Float.POSITIVE_INFINITY, maxX = Float.NEGATIVE_INFINITY;
        float u, v, frontMaxU, frontMaxV;
        @Override public VertexConsumer addVertex(float x, float y, float z) {
            vertices++; minX = Math.min(minX, x); maxX = Math.max(maxX, x); return this;
        }
        @Override public VertexConsumer setUv(float u, float v) {
            assertTrue(u >= 0 && u <= 1, "U must remain in the assigned block sprite: " + u);
            assertTrue(v >= 0 && v <= 1, "V must remain in the assigned block sprite: " + v);
            this.u = u; this.v = v; return this;
        }
        @Override public VertexConsumer setNormal(float x, float y, float z) {
            if (z == -1) { frontMaxU = Math.max(frontMaxU, u); frontMaxV = Math.max(frontMaxV, v); }
            return this;
        }
        @Override public VertexConsumer setColor(int r, int g, int b, int a) { return this; }
        @Override public VertexConsumer setUv1(int u, int v) { return this; }
        @Override public VertexConsumer setUv2(int u, int v) { return this; }
    }
}
