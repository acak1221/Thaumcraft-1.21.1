package thaumcraft.client.renderers.entity;

import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;

/**
 * Original TC4 RenderExplosiveOrb: one alpha blended billboard of the second particle
 * sheet with the original four frame strip, the original 0.7 scale and the projectile tilt.
 * The native RenderFocusOrb already draws the same billboard for the explosive orb.
 */
public final class RenderExplosiveOrb<T extends Entity> extends EntityRenderer<T> {
    private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath("thaumcraft", "textures/misc/particles2.png");
    private static final RenderType BLENDED = new OrbType();

    public RenderExplosiveOrb(EntityRendererProvider.Context context) {
        super(context);
        shadowRadius = 0;
    }

    @Override public ResourceLocation getTextureLocation(T entity) { return TEXTURE; }

    @Override public void render(T entity, float yaw, float partial, PoseStack pose, MultiBufferSource buffers, int light) {
        pose.pushPose();
        try {
            float u0 = (entity.tickCount % 4) / 16.0F;
            float u1 = u0 + .0625F;
            float v0 = .8125F;
            float v1 = v0 + .0625F;
            pose.mulPose(Axis.YP.rotationDegrees(180.0F - entity.getYRot()));
            pose.mulPose(Axis.XP.rotationDegrees(-entity.getXRot()));
            pose.scale(.7F, .7F, .7F);
            VertexConsumer out = buffers.getBuffer(BLENDED);
            vertex(out, pose, -.5F, -.5F, u0, v1);
            vertex(out, pose, .5F, -.5F, u1, v1);
            vertex(out, pose, .5F, .5F, u1, v0);
            vertex(out, pose, -.5F, .5F, u0, v0);
        } finally { pose.popPose(); }
    }

    private static void vertex(VertexConsumer out, PoseStack pose, float x, float y, float u, float v) {
        out.addVertex(pose.last(), x, y, 0).setColor(1.0F, 1.0F, 1.0F, .8F).setUv(u, v)
                .setOverlay(OverlayTexture.NO_OVERLAY).setLight(220).setNormal(pose.last(), 0, 1, 0);
    }

    private static final class OrbType extends RenderType {
        private OrbType() {
            super("thaumcraft_explosive_orb_legacy", DefaultVertexFormat.NEW_ENTITY, VertexFormat.Mode.QUADS, 1536, false, true,
                    OrbType::begin, OrbType::end);
        }

        private static void begin() {
            RENDERTYPE_ENTITY_TRANSLUCENT_SHADER.setupRenderState();
            new TextureStateShard(TEXTURE, false, false).setupRenderState();
            TRANSLUCENT_TRANSPARENCY.setupRenderState();
            LIGHTMAP.setupRenderState();
            OVERLAY.setupRenderState();
            NO_CULL.setupRenderState();
            LEQUAL_DEPTH_TEST.setupRenderState();
            COLOR_DEPTH_WRITE.setupRenderState();
        }

        private static void end() {
            COLOR_DEPTH_WRITE.clearRenderState();
            LEQUAL_DEPTH_TEST.clearRenderState();
            NO_CULL.clearRenderState();
            OVERLAY.clearRenderState();
            LIGHTMAP.clearRenderState();
            TRANSLUCENT_TRANSPARENCY.clearRenderState();
            new TextureStateShard(TEXTURE, false, false).clearRenderState();
            RENDERTYPE_ENTITY_TRANSLUCENT_SHADER.clearRenderState();
        }
    }
}
