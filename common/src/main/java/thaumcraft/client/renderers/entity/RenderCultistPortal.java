package thaumcraft.client.renderers.entity;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import thaumcraft.common.entities.monster.EntityCultistPortal;

/**
 * Original TC4 RenderCultistPortal: one camera facing quad of the sixteen frame portal
 * sheet, scaled by the portal's age, wobbling on damage and fading with its health.
 * The original registered a boss bar through BossStatus; native 1.21.1 bosses publish
 * their bar through the entity's own BossEvent data.
 */
public final class RenderCultistPortal<T extends EntityCultistPortal> extends EntityRenderer<T> {
    public static final ResourceLocation PORTAL = ResourceLocation.fromNamespaceAndPath("thaumcraft", "textures/misc/cultist_portal.png");

    public RenderCultistPortal(EntityRendererProvider.Context context) {
        super(context);
        shadowRadius = .1F;
    }

    @Override public ResourceLocation getTextureLocation(T portal) { return PORTAL; }

    /** Original renderPortal:40-118. */
    @Override public void render(T portal, float yaw, float partial, PoseStack pose, MultiBufferSource buffers, int light) {
        long time = System.nanoTime() / 50000000L;
        float scaley = 1.5F;
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
        float scale = age / 50.0F * 1.3F;
        float m = (1.0F - portal.getHealth() / portal.getMaxHealth()) / 3.0F;
        float bob = Mth.sin(portal.tickCount / (5.0F - 12.0F * m)) * m + m;
        float bob2 = Mth.sin(portal.tickCount / (6.0F - 15.0F * m)) * m + m;
        float alpha = 1.0F - bob;
        scaley -= bob / 4.0F;
        scale -= bob2 / 3.0F;
        if (alpha <= 0.0F) return;
        pose.pushPose();
        try {
            // The original quad was built from the camera vectors, so it always faced the player.
            pose.translate(0.0D, portal.getBbHeight() / 2.0D, 0.0D);
            pose.mulPose(entityRenderDispatcher.cameraOrientation());
            pose.scale(scale, scaley, 1.0F);
            int frame = 15 - (int) (time % 16L);
            float u0 = frame / 16.0F;
            float u1 = u0 + .0625F;
            VertexConsumer out = buffers.getBuffer(RenderType.entityTranslucent(PORTAL));
            // Oracle RenderCultistPortal.java:93-108 - the four corners of the quad
            // are the bottom right, the top right, the top left and the bottom left
            // of the camera frame and they carry the texture coordinates
            // (uMax, 0), (uMax, 1), (u, 1) and (u, 0). The frame of the sheet is
            // therefore upright, its u runs from the left to the right and its v
            // from the bottom to the top.
            vertex(out, pose, -1, -1, u0, 0.0F, alpha);
            vertex(out, pose, 1, -1, u1, 0.0F, alpha);
            vertex(out, pose, 1, 1, u1, 1.0F, alpha);
            vertex(out, pose, -1, 1, u0, 1.0F, alpha);
        } finally { pose.popPose(); }
    }

    private static void vertex(VertexConsumer out, PoseStack pose, float x, float y, float u, float v, float alpha) {
        out.addVertex(pose.last(), x, y, 0).setColor(1.0F, 1.0F, 1.0F, alpha)
                .setUv(u, v).setOverlay(net.minecraft.client.renderer.texture.OverlayTexture.NO_OVERLAY)
                .setLight(220).setNormal(pose.last(), 0, 1, 0);
    }
}
