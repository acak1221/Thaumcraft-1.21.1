package thaumcraft.client.renderers.tile;

import org.joml.Matrix4f;

/** Algebraic replacement for oracle TileMirrorRenderer's object S/T/R and eye Q planes.
 * GL_OBJECT_LINEAR=9217, GL_EYE_LINEAR=9216; Q is NOT an explicit vertex UV. */
public final class MirrorProjection {
    private MirrorProjection() {}
    public static float distance(int layer) { return layer == 0 ? 65F : 16F - layer; }
    public static float scale(int layer) { return layer == 0 ? .125F : layer == 1 ? .5F : .0625F; }
    public static float q(float signedEyeDistance, int layer) {
        return -signedEyeDistance / (signedEyeDistance + distance(layer));
    }
    /** Oracle :101-107 (and axis permutations): multiplication order is OpenGL's. */
    public static Matrix4f texture(int layer, long millis, float cameraS, float cameraT,
                                   float nearS, float nearT, float signedEyeDistance) {
        float distance = distance(layer), scale = scale(layer);
        return new Matrix4f().translate(0, (millis % 700000L) / 250000F, 0)
                .scale(scale).translate(.5F, .5F, 0)
                .rotateZ((float) Math.toRadians((layer * layer * 4321 + layer * 9) * 2F))
                .translate(-.5F, -.5F, 0).translate(-cameraS, -cameraT, 0)
                .translate(nearS * distance / signedEyeDistance, nearT * distance / signedEyeDistance, 0);
    }
}
