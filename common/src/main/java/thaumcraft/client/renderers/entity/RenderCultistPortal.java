package thaumcraft.client.renderers.entity;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import org.joml.Vector3f;
import thaumcraft.client.renderers.CameraBasis;
import thaumcraft.common.entities.monster.EntityCultistPortal;

/**
 * Original TC4 RenderCultistPortal: one camera facing quad of the sixteen frame portal
 * sheet, scaled by the portal's age, wobbling on damage and fading with its health.
 * The original registered a boss bar through BossStatus; native 1.21.1 bosses publish
 * their bar through the entity's own BossEvent data.
 *
 * <p>Oracle {@code RenderCultistPortal.java:62-108}: the four corners of the quad
 * are the four corners of a unit square of the basis of the original
 * {@code ActiveRenderInfo} ({@code -left - up}, {@code -left + up},
 * {@code left + up} and {@code left - up}); the horizontal part of a corner is
 * multiplied by the scale and its vertical part by the scaley. The basis is built
 * from the look angles of the player and it is mirrored in the third person view,
 * so the quad of the original always faces the way the player looks and not the
 * way the camera looks.
 */
public final class RenderCultistPortal<T extends EntityCultistPortal> extends EntityRenderer<T> {
    public static final ResourceLocation PORTAL = ResourceLocation.fromNamespaceAndPath("thaumcraft", "textures/misc/cultist_portal.png");
    /** The original number of frames of the sheet, oracle :97. */
    public static final int FRAMES = 16;
    /** The original height of the quad, oracle :47. */
    public static final float SCALE_Y = 1.5F;
    /** The original width of the quad at the age of fifty, oracle :60. */
    public static final float SCALE_X = 1.3F;

    public RenderCultistPortal(EntityRendererProvider.Context context) {
        super(context);
        shadowRadius = .1F;
    }

    @Override public ResourceLocation getTextureLocation(T portal) { return PORTAL; }

    /** Original renderPortal:40-118. */
    @Override public void render(T portal, float yaw, float partial, PoseStack pose, MultiBufferSource buffers, int light) {
        long time = System.nanoTime() / 50000000L;
        float scaley = SCALE_Y;
        int age = (int) Math.min(50.0F, portal.tickCount + partial);
        if (portal.hurtTime > 0) {
            float d = (float) Math.sin(portal.hurtTime * 72.0F * Math.PI / 180.0F);
            scaley -= d / 4.0F;
            age = (int) (age + 6.0F * d);
        }
        if (portal.pulse > 0) {
            float d = (float) Math.sin(portal.pulse * 36.0F * Math.PI / 180.0F);
            scaley += d / 4.0F;
            age = (int) (age + 12.0F * d);
        }
        float scale = age / 50.0F * SCALE_X;
        float m = (1.0F - portal.getHealth() / portal.getMaxHealth()) / 3.0F;
        float bob = Mth.sin(portal.tickCount / (5.0F - 12.0F * m)) * m + m;
        float bob2 = Mth.sin(portal.tickCount / (6.0F - 15.0F * m)) * m + m;
        float alpha = 1.0F - bob;
        scaley -= bob / 4.0F;
        scale -= bob2 / 3.0F;
        if (alpha <= 0.0F) return;
        pose.pushPose();
        try {
            // Oracle RenderCultistPortal.java:64 - the quad is raised by the half
            // height of the portal.
            pose.translate(0.0D, portal.getBbHeight() / 2.0D, 0.0D);
            int frame = 15 - (int) (time % FRAMES);
            float[] uv = frameUv(frame);
            VertexConsumer out = buffers.getBuffer(RenderType.entityTranslucent(PORTAL));
            // Oracle RenderCultistPortal.java:93-108 - the four corners of the quad
            // are the bottom right, the top right, the top left and the bottom left
            // of the basis and they carry the texture coordinates (uMax, 0),
            // (uMax, 1), (u, 1) and (u, 0). The frame of the sheet is therefore
            // upright, its u runs from the left to the right and its v from the
            // bottom to the top.
            Vector3f left = basisLeft();
            Vector3f up = basisUp();
            Vector3f bottomRight = new Vector3f(left).negate().sub(up);
            Vector3f topRight = new Vector3f(left).negate().add(up);
            Vector3f topLeft = new Vector3f(left).add(up);
            Vector3f bottomLeft = new Vector3f(left).sub(up);
            vertex(out, pose, bottomRight, scale, scaley, uv[0], uv[1], alpha);
            vertex(out, pose, topRight, scale, scaley, uv[2], uv[3], alpha);
            vertex(out, pose, topLeft, scale, scaley, uv[4], uv[5], alpha);
            vertex(out, pose, bottomLeft, scale, scaley, uv[6], uv[7], alpha);
        } finally { pose.popPose(); }
    }

    /**
     * The four texture coordinates of the portal of the original.
     *
     * <p>Oracle {@code RenderCultistPortal.java:95-108}: the frame of the sheet is
     * counted backwards, {@code frame = 15 - time % 16}, and the four corners are
     * the bottom right, the top right, the top left and the bottom left of the
     * basis with the texture coordinates {@code (uMax, 0)}, {@code (uMax, 1)},
     * {@code (u, 1)} and {@code (u, 0)}.
     */
    public static float[] frameUv(int frame) {
        float u = frame / (float) FRAMES;
        float uMax = u + .0625F;
        return new float[]{uMax, 0.0F, uMax, 1.0F, u, 1.0F, u, 0.0F};
    }

    /**
     * The left vector of the basis of the original.
     *
     * <p>Oracle {@code ActiveRenderInfo.updateRenderInfo}: the vector is
     * {@code (cos(yaw), 0, sin(yaw))} and it is negated in the third person view.
     */
    public static Vector3f basisLeft() {
        return CameraBasis.left();
    }

    /**
     * The up vector of the basis of the original.
     *
     * <p>Oracle {@code ActiveRenderInfo.updateRenderInfo}: the vector is
     * {@code (-sin(yaw) sin(pitch), cos(pitch), cos(yaw) sin(pitch))} and its
     * horizontal part is negated in the third person view.
     */
    public static Vector3f basisUp() {
        return CameraBasis.up();
    }

    private static void vertex(VertexConsumer out, PoseStack pose, Vector3f corner, float scale, float scaley,
                               float u, float v, float alpha) {
        // The original multiplies the horizontal part of the corner with the scale
        // and its vertical part with the scaley.
        out.addVertex(pose.last(), corner.x * scale, corner.y * scaley, corner.z * scale)
                .setColor(1.0F, 1.0F, 1.0F, alpha)
                .setUv(u, v).setOverlay(net.minecraft.client.renderer.texture.OverlayTexture.NO_OVERLAY)
                .setLight(220).setNormal(pose.last(), 0, 1, 0);
    }
}
