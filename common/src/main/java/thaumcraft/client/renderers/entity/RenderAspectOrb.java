package thaumcraft.client.renderers.entity;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import thaumcraft.client.renderers.VisNetRender;
import thaumcraft.common.entities.EntityAspectOrb;

/** Original 16-frame/25ms atlas strip, asymmetric billboard, tint/blend and age shrink. */
public final class RenderAspectOrb extends EntityRenderer<EntityAspectOrb> {
    public RenderAspectOrb(EntityRendererProvider.Context context) { super(context); shadowRadius = .1F; shadowStrength = .5F; }
    @Override public ResourceLocation getTextureLocation(EntityAspectOrb orb) { return VisNetRender.PARTICLES; }
    @Override public void render(EntityAspectOrb orb, float yaw, float partial, PoseStack pose, MultiBufferSource buffers, int light) {
        var aspect = orb.getAspect();
        if (aspect == null || orb.isQuarantined() || orb.isConsumed() || !VisNetRender.flush(buffers)) return;
        int frame = (int)(System.nanoTime() / 25000000L % 16L);
        float size = .1F + .3F * ((EntityAspectOrb.MAX_AGE - orb.getOrbAge()) / (float)EntityAspectOrb.MAX_AGE);
        pose.pushPose();
        try {
            pose.mulPose(entityRenderDispatcher.cameraOrientation()); pose.scale(size, size, size);
            VisNetRender.draw(VisNetRender.PARTICLES, aspect.getBlend(), true, out -> {
                float[] xy = {-.5F,-.25F,.5F,-.25F,.5F,.75F,-.5F,.75F};
                for (int vertex = 0; vertex < 4; vertex++) out.addVertex(pose.last(), xy[vertex * 2], xy[vertex * 2 + 1], 0)
                        .setUv((frame + (vertex == 1 || vertex == 2 ? 1F : 0F)) / 16F, vertex < 2 ? .5625F : .5F)
                        .setColor((aspect.getColor() >> 16) & 255, (aspect.getColor() >> 8) & 255, aspect.getColor() & 255, 128);
            });
        } finally { pose.popPose(); }
    }
}
