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
import net.minecraft.world.entity.projectile.AbstractArrow;

/**
 * Original TC4 RenderPrimalArrow: the vanilla arrow mesh tinted by the primal aspect and
 * the wisp trail behind it. The entity specific values are the original
 * {@code EntityPrimalArrow.type} and {@code EntityPrimalArrow.field_70252_j}; they are
 * exposed as overridable hooks so the arrow entity can supply them.
 */
public class RenderPrimalArrow<T extends AbstractArrow> extends EntityRenderer<T> {
    private static final ResourceLocation ARROW = ResourceLocation.withDefaultNamespace("textures/entity/projectiles/arrow.png");
    private static final ResourceLocation WISP = ResourceLocation.fromNamespaceAndPath("thaumcraft", "textures/misc/wisp.png");
    /** The original BlockCustomOreItem primal colours, in the original aspect order. */
    private static final int[] PRIMAL = {0xffff7e, 0xff3c01, 0x0090ff, 0x00a000, 0xeeccff, 0x555577};

    public RenderPrimalArrow(EntityRendererProvider.Context context) {
        super(context);
        shadowRadius = 0;
    }

    @Override public ResourceLocation getTextureLocation(T arrow) { return ARROW; }

    /** Original EntityPrimalArrow.type: the primal aspect index that tints the trail. */
    protected int primalType(T arrow) { return 0; }

    /** Original EntityPrimalArrow.field_70252_j: the spent life counter that fades the arrow. */
    protected int spentLife(T arrow) { return 0; }

    @Override public void render(T arrow, float yaw, float partial, PoseStack pose, MultiBufferSource buffers, int light) {
        float alpha = (100.0F - spentLife(arrow)) / 100.0F;
        pose.pushPose();
        try {
            float arrowYaw = Mth.lerp(partial, arrow.yRotO, arrow.getYRot());
            float arrowPitch = Mth.lerp(partial, arrow.xRotO, arrow.getXRot());
            pose.mulPose(Axis.YP.rotationDegrees(arrowYaw - 90.0F));
            pose.mulPose(Axis.ZP.rotationDegrees(arrowPitch));
            float shake = arrow.shakeTime - partial;
            if (shake > 0.0F) pose.mulPose(Axis.ZP.rotationDegrees(-Mth.sin(shake * 3.0F) * shake));
            pose.mulPose(Axis.XP.rotationDegrees(45.0F));
            pose.scale(.05625F, .05625F, .05625F);
            pose.translate(-4.0F, 0.0F, 0.0F);
            VertexConsumer out = buffers.getBuffer(RenderType.entityCutout(ARROW));
            PoseStack.Pose last = pose.last();
            // The original end cap, seen from both sides.
            vertex(last, out, -7, -2, -2, 0.0F, .15625F, -1, 0, 0, light, alpha);
            vertex(last, out, -7, -2, 2, .15625F, .15625F, -1, 0, 0, light, alpha);
            vertex(last, out, -7, 2, 2, .15625F, .3125F, -1, 0, 0, light, alpha);
            vertex(last, out, -7, 2, -2, 0.0F, .3125F, -1, 0, 0, light, alpha);
            vertex(last, out, -7, 2, -2, 0.0F, .15625F, 1, 0, 0, light, alpha);
            vertex(last, out, -7, 2, 2, .15625F, .15625F, 1, 0, 0, light, alpha);
            vertex(last, out, -7, -2, 2, .15625F, .3125F, 1, 0, 0, light, alpha);
            vertex(last, out, -7, -2, -2, 0.0F, .15625F, 1, 0, 0, light, alpha);
            // The original shaft: four quads a quarter turn apart.
            for (int side = 0; side < 4; side++) {
                pose.mulPose(Axis.XP.rotationDegrees(90.0F));
                last = pose.last();
                vertex(last, out, -8, -2, 0, 0.0F, 0.0F, 0, 1, 0, light, alpha);
                vertex(last, out, 8, -2, 0, .5F, 0.0F, 0, 1, 0, light, alpha);
                vertex(last, out, 8, 2, 0, .5F, .15625F, 0, 1, 0, light, alpha);
                vertex(last, out, -8, 2, 0, 0.0F, .15625F, 0, 1, 0, light, alpha);
            }
        } finally { pose.popPose(); }
        drawTrail(arrow, alpha, pose, buffers);
    }

    /** The original wisp trail quad behind the arrow. */
    private void drawTrail(T arrow, float alpha, PoseStack pose, MultiBufferSource buffers) {
        int type = Math.max(0, Math.min(PRIMAL.length - 1, primalType(arrow)));
        int color = PRIMAL[type];
        pose.pushPose();
        try {
            pose.mulPose(entityRenderDispatcher.cameraOrientation());
            float size = .5F;
            int frame = arrow.tickCount % 16;
            float u0 = (frame % 4) / 4F, u1 = (frame % 4 + 1) / 4F;
            float v0 = (frame / 4) / 4F, v1 = (frame / 4 + 1) / 4F;
            RenderType type2 = type < 5 ? RenderType.entityTranslucent(WISP) : RenderType.entityCutout(WISP);
            VertexConsumer out = buffers.getBuffer(type2);
            vertex(out, pose, -size, -size, 0, u1, v1, color >> 16 & 255, color >> 8 & 255, color & 255, alpha);
            vertex(out, pose, size, -size, 0, u0, v1, color >> 16 & 255, color >> 8 & 255, color & 255, alpha);
            vertex(out, pose, size, size, 0, u0, v0, color >> 16 & 255, color >> 8 & 255, color & 255, alpha);
            vertex(out, pose, -size, size, 0, u1, v0, color >> 16 & 255, color >> 8 & 255, color & 255, alpha);
        } finally { pose.popPose(); }
    }

    private static void vertex(PoseStack.Pose pose, VertexConsumer out, float x, float y, float z, float u, float v,
                               int nx, int ny, int nz, int light, float alpha) {
        out.addVertex(pose, x, y, z).setColor(1.0F, 1.0F, 1.0F, alpha).setUv(u, v)
                .setOverlay(OverlayTexture.NO_OVERLAY).setLight(light).setNormal(pose, nx, ny, nz);
    }

    private static void vertex(VertexConsumer out, PoseStack pose, float x, float y, float z, float u, float v,
                               int r, int g, int b, float alpha) {
        out.addVertex(pose.last(), x, y, z).setColor(r, g, b, alpha).setUv(u, v)
                .setOverlay(OverlayTexture.NO_OVERLAY).setLight(net.minecraft.client.renderer.LightTexture.FULL_BRIGHT)
                .setNormal(pose.last(), 0, 1, 0);
    }
}
