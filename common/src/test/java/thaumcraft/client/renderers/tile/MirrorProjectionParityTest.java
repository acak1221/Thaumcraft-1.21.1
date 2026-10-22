package thaumcraft.client.renderers.tile;

import static org.junit.jupiter.api.Assertions.*;
import org.joml.Vector4f;
import org.junit.jupiter.api.Test;

class MirrorProjectionParityTest {
    @Test void originalLayerDistancesAndScales() {
        assertEquals(65F, MirrorProjection.distance(0));
        assertEquals(15F, MirrorProjection.distance(1));
        assertEquals(1F, MirrorProjection.distance(15));
        assertEquals(.125F, MirrorProjection.scale(0));
        assertEquals(.5F, MirrorProjection.scale(1));
        assertEquals(.0625F, MirrorProjection.scale(15));
    }
    @Test void eyeLinearQIsTheInverseTranslatedPlaneNotAnExplicitUV() {
        assertEquals(-2F/67F, MirrorProjection.q(2, 0), 1e-6F);
        assertEquals(2F/13F, MirrorProjection.q(-2, 1), 1e-6F);
    }
    @Test void outerLayerPreservesOriginalTimeAndMatrixOrder() {
        // Oracle :101-107, layer zero: rotation=0, S=.125*(2-10*q+1*65/2*q).
        float q = MirrorProjection.q(2, 0);
        Vector4f uv = MirrorProjection.texture(0, 250000, 10, 20, 1, 3, 2)
                .transform(new Vector4f(2, 3, 1, q));
        assertEquals(.125F * (2 + 22.5F*q), uv.x, 1e-6F);
        assertEquals(.125F * (3 + 77.5F*q) + q, uv.y, 1e-6F);
        assertEquals(q, uv.w);
    }
    @Test void homogeneousRescalingKeepsTheSameProjectedCoordinates() {
        float d = -3, q = MirrorProjection.q(d, 3), multiplier = d + MirrorProjection.distance(3);
        var matrix = MirrorProjection.texture(3, 123456, 10, 20, .1F, .2F, d);
        var old = matrix.transform(new Vector4f(2, 3, 1, q));
        var stable = matrix.transform(new Vector4f(2*multiplier, 3*multiplier, multiplier, -d));
        assertEquals(old.x/old.w, stable.x/stable.w, 1e-5F);
        assertEquals(old.y/old.w, stable.y/stable.w, 1e-5F);
    }
}
