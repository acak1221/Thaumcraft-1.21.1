package thaumcraft.client.renderers.models;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import java.util.Random;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;

/**
 * The native replacement of the 1.7.10 fixed function "tunnel" effect.
 *
 * <p>The original TC4 draws the eldritch void, the eldritch obelisk, the portable
 * hole and the linked mirror with {@code GL11.glTexGen} texture coordinate
 * generation and a hand written projection matrix: sixteen nested quads are
 * pushed away from the camera, their texture is generated from the eye position
 * and the two inner layers are drawn with the additive blend. Minecraft 1.21.1
 * has no fixed function pipeline any more, so the effect is reproduced with the
 * modern shader pipeline:
 *
 * <ul>
 *   <li>the same sixteen layers, the same rotation {@code (i * i * 4321 + i * 9) * 2}
 *       degrees around the normal of the face and the same per layer fade
 *       {@code 1 / (16 - i + 1)};</li>
 *   <li>the layer zero keeps the original {@code textures/misc/tunnel.png} with the
 *       original {@code 0.1} alpha, the layer one and every deeper layer use
 *       {@code textures/misc/particlefield.png};</li>
 *   <li>instead of the removed projection matrix every layer is drawn smaller and
 *       pushed into the block, which produces the same receding tunnel;</li>
 *   <li>the original colour of a layer is reproduced with the original
 *       {@code new Random(31100L)} sequence.</li>
 * </ul>
 *
 * <p>This is an approximation of the original effect, not a pixel exact copy:
 * the view dependent texture generation of the original cannot be reproduced
 * without the fixed function pipeline. The additive blend of the original inner
 * layers is the vanilla {@code RenderType.eyes}, whose transparency is
 * {@code RenderStateShard.ADDITIVE_TRANSPARENCY}, i.e. the very
 * {@code glBlendFunc(GL_ONE, GL_ONE)} of the original.</p>
 */
public final class ModelTunnelField {
    /** The original outer layer texture. */
    public static final ResourceLocation TUNNEL = ResourceLocation.fromNamespaceAndPath("thaumcraft", "textures/misc/tunnel.png");
    /** The original inner layer texture. */
    public static final ResourceLocation FIELD = ResourceLocation.fromNamespaceAndPath("thaumcraft", "textures/misc/particlefield.png");
    /** The original far distance fallback texture. */
    public static final ResourceLocation FIELD_FAR = ResourceLocation.fromNamespaceAndPath("thaumcraft", "textures/misc/particlefield32.png");
    /** The original random seed of every plane. */
    public static final long SEED = 31100L;
    /** The original number of layers. */
    public static final int LAYERS = 16;
    /** The original texture scale of the outer layer. */
    public static final float OUTER_TEXTURE_SCALE = .125F;
    /** The original texture scale of the second layer. */
    public static final float INNER_TEXTURE_SCALE = .5F;

    private final Random random = new Random(SEED);

    /**
     * Draws the tunnel on the given face of the unit cube the pose is placed in.
     *
     * @param face  the face of the block the tunnel leaves through
     * @param fade  an extra multiplier for the alpha of every layer
     * @param light the packed light of the block
     * @param overlay the packed overlay of the block
     */
    public void renderFace(PoseStack pose, MultiBufferSource buffers, Direction face, float fade, int light, int overlay) {
        // Original TileMirrorRenderer:70-71 draws the outer layer with the ordinary
        // alpha blend (glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA)) and
        // TileMirrorRenderer:74-77 draws the second layer with the additive blend
        // glBlendFunc(GL_ONE, GL_ONE); every deeper layer keeps that blend.
        // The 1.21.1 RenderType.eyes uses the vanilla
        // RenderStateShard.ADDITIVE_TRANSPARENCY, whose blend is exactly
        // RenderSystem.blendFunc(ONE, ONE), and its shader ignores the lightmap
        // like the original, which draws the tunnel with GL_TEXTURE_2D disabled.
        VertexConsumer tunnel = buffers.getBuffer(RenderType.entityTranslucent(TUNNEL));
        VertexConsumer field = buffers.getBuffer(RenderType.eyes(FIELD));
        for (int layer = 0; layer < LAYERS; layer++) {
            float distance = LAYERS - layer;
            float scale = 1.0F / (distance + 1.0F);
            float alpha = layer == 0 ? .1F : scale;
            float textureScale = layer == 0 ? OUTER_TEXTURE_SCALE : (layer == 1 ? INNER_TEXTURE_SCALE : .0625F);
            VertexConsumer out = layer == 0 ? tunnel : field;
            float red = random.nextFloat() * .5F + .1F;
            float green = random.nextFloat() * .5F + .4F;
            float blue = random.nextFloat() * .5F + .5F;
            if (layer == 0) {
                red = 1.0F;
                green = 1.0F;
                blue = 1.0F;
            }
            pose.pushPose();
            try {
                face(pose, face, distance / (float) LAYERS);
                // The original turns every layer around the normal of the face.
                pose.mulPose(Axis.ZP.rotationDegrees((layer * layer * 4321 + layer * 9) * 2.0F));
                quad(pose, out, scale, red, green, blue, alpha * fade, textureScale, light, overlay);
            } finally {
                pose.popPose();
            }
        }
    }

    /**
     * The original far distance fallback: a single flat quad of the coarse
     * particle field, drawn when the viewer is more than 512 blocks away.
     */
    public void renderFarFace(PoseStack pose, MultiBufferSource buffers, Direction face, int light, int overlay) {
        VertexConsumer out = buffers.getBuffer(RenderType.entityTranslucent(FIELD_FAR));
        pose.pushPose();
        try {
            face(pose, face, 0.0F);
            quad(pose, out, 1.0F, .5F, .5F, .5F, 1.0F, 1.0F, light, overlay);
        } finally {
            pose.popPose();
        }
    }

    /** Moves the pose onto the given face of the unit cube and pushes it into the block. */
    private static void face(PoseStack pose, Direction face, float depth) {
        float offset = depth * .5F;
        switch (face) {
            case DOWN -> {
                pose.translate(.5F, offset, .5F);
                pose.mulPose(Axis.XP.rotationDegrees(90.0F));
            }
            case UP -> {
                pose.translate(.5F, 1.0F - offset, .5F);
                pose.mulPose(Axis.XP.rotationDegrees(-90.0F));
            }
            case NORTH -> {
                pose.translate(.5F, .5F, offset);
                pose.mulPose(Axis.YP.rotationDegrees(180.0F));
            }
            case SOUTH -> {
                pose.translate(.5F, .5F, 1.0F - offset);
            }
            case WEST -> {
                pose.translate(offset, .5F, .5F);
                pose.mulPose(Axis.YP.rotationDegrees(-90.0F));
            }
            default -> {
                pose.translate(1.0F - offset, .5F, .5F);
                pose.mulPose(Axis.YP.rotationDegrees(90.0F));
            }
        }
    }

    /**
     * One layer: a square of the half size {@code half} that is centred on the
     * face and repeats its texture {@code textureScale} times.
     */
    private static void quad(PoseStack pose, VertexConsumer out, float scale, float red, float green, float blue,
                             float alpha, float textureScale, int light, int overlay) {
        float half = scale * .5F;
        float max = textureScale;
        // The order of the original quad: top left, top right, bottom right, bottom left.
        float[][] corners = {{-half, half}, {half, half}, {half, -half}, {-half, -half}};
        float[][] uv = {{0, 0}, {max, 0}, {max, max}, {0, max}};
        for (int index = 0; index < 4; index++) {
            out.addVertex(pose.last(), corners[index][0], corners[index][1], 0.0F)
                    .setColor(red, green, blue, alpha)
                    .setUv(uv[index][0], uv[index][1])
                    .setOverlay(overlay).setLight(light)
                    .setNormal(pose.last(), 0.0F, 0.0F, 1.0F);
        }
    }
}
