package thaumcraft.client.renderers.entity;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import thaumcraft.common.entities.projectile.EntityDart;

/**
 * TC4 RenderDart: the vanilla arrow's geometry (two end caps and four crossed shaft quads on the arrow skin) drawn small
 * ({@code 0.025625} per pixel, {@code 0.75} along the shaft) and tinted blue-grey (0.5, 0.5, 0.6). The wobble after a hit is the
 * arrow's {@code shakeTime}. Compiled and registered only; nothing here has been seen in a running client.
 */
public final class RenderDart extends EntityRenderer<EntityDart> {
    private static final ResourceLocation ARROW = ResourceLocation.withDefaultNamespace("textures/entity/projectiles/arrow.png");
    private static final float SCALE = 0.025625F, LENGTH = 0.75F;
    /** Original glColor3f(0.5, 0.5, 0.6). */
    private static final int RED = 127, GREEN = 127, BLUE = 153;

    public RenderDart(EntityRendererProvider.Context context) { super(context); }

    @Override public void render(EntityDart dart, float yaw, float partialTick, PoseStack pose, MultiBufferSource buffers, int light) {
        pose.pushPose();
        try {
            pose.mulPose(Axis.YP.rotationDegrees(Mth.lerp(partialTick, dart.yRotO, dart.getYRot()) - 90.0F));
            pose.mulPose(Axis.ZP.rotationDegrees(Mth.lerp(partialTick, dart.xRotO, dart.getXRot())));
            float shake = dart.shakeTime - partialTick;
            if (shake > 0.0F) pose.mulPose(Axis.ZP.rotationDegrees(-Mth.sin(shake * 3.0F) * shake));
            pose.mulPose(Axis.XP.rotationDegrees(45.0F));
            pose.scale(SCALE * LENGTH, SCALE, SCALE);
            pose.translate(-4.0F, 0.0F, 0.0F);
            VertexConsumer buffer = buffers.getBuffer(RenderType.entityCutout(ARROW));
            PoseStack.Pose last = pose.last();
            // The end cap, seen from both sides.
            vertex(last, buffer, -7, -2, -2, 0.0F, 0.15625F, -1, 0, 0, light);
            vertex(last, buffer, -7, -2, 2, 0.15625F, 0.15625F, -1, 0, 0, light);
            vertex(last, buffer, -7, 2, 2, 0.15625F, 0.3125F, -1, 0, 0, light);
            vertex(last, buffer, -7, 2, -2, 0.0F, 0.3125F, -1, 0, 0, light);
            vertex(last, buffer, -7, 2, -2, 0.0F, 0.15625F, 1, 0, 0, light);
            vertex(last, buffer, -7, 2, 2, 0.15625F, 0.15625F, 1, 0, 0, light);
            vertex(last, buffer, -7, -2, 2, 0.15625F, 0.3125F, 1, 0, 0, light);
            vertex(last, buffer, -7, -2, -2, 0.0F, 0.3125F, 1, 0, 0, light);
            // The shaft: four quads a quarter turn apart.
            for (int side = 0; side < 4; side++) {
                pose.mulPose(Axis.XP.rotationDegrees(90.0F));
                last = pose.last();
                vertex(last, buffer, -8, -2, 0, 0.0F, 0.0F, 0, 1, 0, light);
                vertex(last, buffer, 8, -2, 0, 0.5F, 0.0F, 0, 1, 0, light);
                vertex(last, buffer, 8, 2, 0, 0.5F, 0.15625F, 0, 1, 0, light);
                vertex(last, buffer, -8, 2, 0, 0.0F, 0.15625F, 0, 1, 0, light);
            }
        } finally { pose.popPose(); }
        super.render(dart, yaw, partialTick, pose, buffers, light);
    }

    private static void vertex(PoseStack.Pose pose, VertexConsumer buffer, float x, float y, float z, float u, float v, int nx, int ny, int nz, int light) {
        buffer.addVertex(pose, x, y, z).setColor(RED, GREEN, BLUE, 255).setUv(u, v).setOverlay(OverlayTexture.NO_OVERLAY).setLight(light)
                .setNormal(pose, nx, ny, nz);
    }

    @Override public ResourceLocation getTextureLocation(EntityDart dart) { return ARROW; }
}
