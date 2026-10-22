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
    /**
     * The projected texture coordinates of the outer layer, pinned as LITERAL values.
     *
     * <p>The original computes the coordinates from the eye-linear plane instead of an explicit
     * vertex UV, so the only honest check is the numbers themselves. The values below were computed
     * from the oracle's own chain ({@code TileMirrorRenderer:101-107}: translate, scale, the
     * per-layer rotation, the camera offset and the near-plane term) at a fixed time and camera,
     * and they are written out as literals here on purpose: an earlier version of this test only
     * asserted that a homogeneous rescale of the input leaves the ratio unchanged, which the
     * IDENTITY matrix satisfies as well - the test stayed green with the projection deleted.</p>
     */
    @Test void theProjectedCoordinatesAreTheOriginalOnes() {
        // Layer 0 at 250000 ms, camera (10, 20), near (1, 3), signed eye distance 2.
        float q0 = MirrorProjection.q(2, 0);
        Vector4f outer = MirrorProjection.texture(0, 250000, 10, 20, 1, 3, 2).transform(new Vector4f(2, 3, 1, q0));
        // The values below are measured from the production chain and cross-checked against a
        // standalone reimplementation of the oracle's rows, NOT read off the test run:
        // x = .16604477, y = .055970162, z = .125, w = -2/67 = -.029850746.
        assertEquals(0.16604477F, outer.x, 1e-6F);
        assertEquals(0.055970162F, outer.y, 1e-6F);
        assertEquals(0.125F, outer.z, 1e-6F);
        assertEquals(q0, outer.w, 1e-6F);
        // The projection is a real matrix, not the identity: the identity would leave (2,3) alone.
        assertNotEquals(2F, outer.x, "the identity matrix must not satisfy this");
        assertNotEquals(3F, outer.y, "the identity matrix must not satisfy this");
        // A second layer takes a different distance and scale, so the same input lands elsewhere.
        float q1 = MirrorProjection.q(2, 1);
        Vector4f inner = MirrorProjection.texture(1, 250000, 10, 20, 1, 3, 2).transform(new Vector4f(2, 3, 1, q1));
        assertEquals(0.6033256F, inner.x, 1e-6F, "layer 1 x");
        assertEquals(1.5543013F, inner.y, 1e-6F, "layer 1 y");
        assertNotEquals(outer.x, inner.x, "the layers must not collapse onto one another");
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
