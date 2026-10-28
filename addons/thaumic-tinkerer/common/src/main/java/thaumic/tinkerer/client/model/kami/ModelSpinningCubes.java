/**
 * This class was created by <Vazkii>. It's distributed as part of the ThaumicTinkerer Mod.
 *
 * ThaumicTinkerer is Open Source and distributed under a Creative Commons Attribution-NonCommercial-ShareAlike 3.0
 * License (http://creativecommons.org/licenses/by-nc-sa/3.0/deed.en_GB)
 *
 * ThaumicTinkerer is a Derivative Work on Thaumcraft 4. Thaumcraft 4 (c) Azanor 2012
 * (http://www.minecraftforum.net/topic/1585216-)
 *
 * Ported to Minecraft 1.21.1 for Thaumcraft 4 Reload (see addons/thaumic-tinkerer/NOTICE.md).
 */
package thaumic.tinkerer.client.model.kami;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import org.joml.Matrix4f;

/**
 * The little cubes that circle the Celestial Gateway (used by the KamiWorld group's block entity renderer). 1.7.10 drew
 * {@code cubes} small white cubes on a wobbling circle, turning, and repeated the ring {@code repeat} times, the older
 * repeats translucent (alpha 0.2), with the texture switched off.
 */
public final class ModelSpinningCubes {

    private ModelSpinningCubes() {}

    /**
     * @param ticks      the client player's tick count, which drives the motion (1.7.10 {@code clientPlayer().ticksExisted})
     * @param cubes      cubes per ring
     * @param repeat     how many older rings are drawn behind the current one
     * @param origRepeat the number of rings the call started with (an older ring is translucent)
     */
    public static void renderSpinningCubes(PoseStack poseStack, MultiBufferSource buffers, double ticks, int cubes,
            int repeat, int origRepeat) {
        final float modifier = 6F;
        final float rotationModifier = 0.25F;
        final float radiusBase = 0.7F;
        final float radiusMod = 0.1F;

        for (int ring = repeat; ring >= 0; ring--) {
            double ringTicks = ticks - 0.75 * (origRepeat - ring);
            float offsetPerCube = 360 / cubes;
            VertexConsumer consumer = buffers.getBuffer(RenderType.debugQuads());

            poseStack.pushPose();
            poseStack.translate(-0.025F, 0.85F, -0.025F);
            for (int i = 0; i < cubes; i++) {
                float offset = offsetPerCube * i;
                float deg = (int) (ringTicks / rotationModifier % 360F + offset);
                float rad = deg * (float) Math.PI / 180F;
                float radiusX = (float) (radiusBase + radiusMod * Math.sin(ringTicks / modifier));
                float radiusZ = (float) (radiusBase + radiusMod * Math.cos(ringTicks / modifier));
                float x = (float) (radiusX * Math.cos(rad));
                float z = (float) (radiusZ * Math.sin(rad));
                float y = (float) Math.cos((ringTicks + 50 * i) / 5F) / 10F;

                poseStack.pushPose();
                poseStack.translate(x, y, z);
                float xRotate = (float) Math.sin(ringTicks * rotationModifier) / 2F;
                float yRotate = (float) Math.max(0.6F, Math.sin(ringTicks * 0.1F) / 2F + 0.5F);
                float zRotate = (float) Math.cos(ringTicks * rotationModifier) / 2F;
                poseStack.mulPose(new org.joml.Quaternionf().rotateAxis((float) Math.toRadians(deg),
                        new org.joml.Vector3f(xRotate, yRotate, zRotate).normalize()));
                int alpha = ring < origRepeat ? 51 : 255;
                cube(poseStack.last().pose(), consumer, 1F / 16F, alpha);
                poseStack.popPose();
            }
            poseStack.popPose();
        }
    }

    /** One cube of the given edge length, white, with the given alpha (0 to 255). */
    private static void cube(Matrix4f pose, VertexConsumer consumer, float size, int alpha) {
        float[][] faces = {
                { 0, 0, 0, 1, 0, 0, 1, 1, 0, 0, 1, 0 }, // north
                { 0, 0, 1, 0, 1, 1, 1, 1, 1, 1, 0, 1 }, // south
                { 0, 0, 0, 0, 1, 0, 0, 1, 1, 0, 0, 1 }, // west
                { 1, 0, 0, 1, 0, 1, 1, 1, 1, 1, 1, 0 }, // east
                { 0, 0, 0, 0, 0, 1, 1, 0, 1, 1, 0, 0 }, // down
                { 0, 1, 0, 1, 1, 0, 1, 1, 1, 0, 1, 1 } // up
        };
        for (float[] face : faces) {
            for (int v = 0; v < 4; v++) {
                consumer.addVertex(pose, face[v * 3] * size, face[v * 3 + 1] * size, face[v * 3 + 2] * size)
                        .setColor(255, 255, 255, alpha);
            }
        }
    }

    /** The light the cubes glow with (1.7.10: full brightness). */
    public static int light() {
        return LightTexture.FULL_BRIGHT;
    }
}
