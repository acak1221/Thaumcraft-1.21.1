package thaumcraft.client.renderers.tile;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import java.util.Objects;
import java.util.Random;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.lwjgl.opengl.GL11;
import thaumcraft.client.renderers.VisNetRender;
import thaumcraft.client.renderers.models.ModelTunnelField;

/** The six oracle mirror planes, with projective texture sampling instead of glTexGen. */
public final class LinkedMirrorField {
    private static ShaderInstance shader;
    private LinkedMirrorField() {}
    public static void setShader(ShaderInstance value) { shader = Objects.requireNonNull(value); }

    public static void render(PoseStack pose, MultiBufferSource buffers, BlockPos block, Direction face) {
        if (!VisNetRender.flush(buffers)) return;
        ShaderInstance selected = Objects.requireNonNull(shader, "Linked mirror shader not registered");
        var camera = Minecraft.getInstance().gameRenderer.getMainCamera();
        var eye = camera.getPosition();
        // ActiveRenderInfo was the unprojected centre of the near clipping plane.
        // Invert the ACTUAL matrices, including third-person/bob/hurt transforms,
        // rather than estimating that point from the camera quaternion alone.
        Vector3f near = new Matrix4f(RenderSystem.getProjectionMatrix()).invert()
                .transformProject(new Vector3f(0, 0, -1));
        new Matrix4f(RenderSystem.getModelViewMatrix()).mul(pose.last().pose()).invert().transformPosition(near);
        near.add((float) (block.getX() - eye.x), (float) (block.getY() - eye.y), (float) (block.getZ() - eye.z));
        int axis = face.getAxis() == Direction.Axis.X ? 0 : face.getAxis() == Direction.Axis.Y ? 1 : 2;
        int s = axis == 0 ? 2 : 0, t = axis == 1 ? 2 : 1;
        float[] cameraPosition = {(float) eye.x, (float) eye.y, (float) eye.z};
        float[] relative = {(float) (block.getX() - eye.x), (float) (block.getY() - eye.y), (float) (block.getZ() - eye.z)};
        float[] active = {near.x, near.y, near.z};
        boolean positive = face.getAxisDirection() == Direction.AxisDirection.POSITIVE;
        float offset = positive ? .99F : .01F;
        float signedDistance = (positive ? 1F : -1F) * (relative[axis] + offset - active[axis]);
        if (Math.abs(signedDistance) < 1.0e-6F) return; // edge-on: no finite projected area
        Random random = new Random(31100L); // oracle resets for EVERY face/render
        for (int layer = 0; layer < 16; ++layer) {
            float fade = layer == 0 ? .1F : 1F / (17F - layer);
            float red = random.nextFloat() * .5F + .1F;
            float green = random.nextFloat() * .5F + .4F;
            float blue = random.nextFloat() * .5F + .5F;
            if (layer == 0) red = green = blue = 1F;
            float r = red * fade, g = green * fade, b = blue * fade;
            // Multiply homogeneous S/T/R/Q by (f9+f5). Same projected UV, but
            // no infinity when the viewer is exactly f5 blocks from the plane.
            float homogeneousScale = signedDistance + MirrorProjection.distance(layer);
            Objects.requireNonNull(selected.getUniform("PortalQ")).set(-signedDistance);
            Objects.requireNonNull(selected.getUniform("PortalTextureMat")).set(MirrorProjection.texture(layer,
                    System.currentTimeMillis(), cameraPosition[s], cameraPosition[t], active[s], active[t], signedDistance));
            // Alpha is ONE in the original; the fade multiplies RGB, not alpha.
            VisNetRender.drawWithBlend(layer == 0 ? ModelTunnelField.TUNNEL : ModelTunnelField.FIELD,
                    layer == 0 ? GL11.GL_SRC_ALPHA : GL11.GL_ONE,
                    layer == 0 ? GL11.GL_ONE_MINUS_SRC_ALPHA : GL11.GL_ONE, true, selected, out -> {
                        float[][] corners = {{.1875F,.1875F},{.1875F,.8125F},{.8125F,.8125F},{.8125F,.1875F}};
                        for (float[] corner : corners) {
                            float[] v = new float[3]; v[axis] = offset; v[s] = corner[0]; v[t] = corner[1];
                            out.addVertex(pose.last(), v[0], v[1], v[2])
                                    .setUv((relative[s] + v[s]) * homogeneousScale, (relative[t] + v[t]) * homogeneousScale)
                                    .setColor(r, g, b, 1F);
                        }
                    });
        }
    }
}
