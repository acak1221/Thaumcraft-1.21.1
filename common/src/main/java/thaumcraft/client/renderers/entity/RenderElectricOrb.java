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
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;

/**
 * Original TC4 RenderElectricOrb: one additive billboard of the particle sheet with the
 * original four frame strip, the original bob and the original projectile tilt.
 * The native RenderFocusOrb already draws the same billboard for the shock orb; this
 * renderer keeps the original entity tilt that the native helper drops.
 */
public final class RenderElectricOrb<T extends Entity> extends EntityRenderer<T> {
    private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath("thaumcraft", "textures/misc/particles.png");
    private static final RenderType ADDITIVE = new OrbType();

    public RenderElectricOrb(EntityRendererProvider.Context context) {
        super(context);
        shadowRadius = 0;
    }

    @Override public ResourceLocation getTextureLocation(T entity) { return TEXTURE; }

    /** The original red golem orb used the second row of the strip. */
    protected boolean isRed(T entity) { return false; }

    @Override public void render(T entity, float yaw, float partial, PoseStack pose, MultiBufferSource buffers, int light) {
        pose.pushPose();
        try {
            float u0 = (1 + entity.tickCount % 6) / 8.0F;
            float u1 = u0 + .125F;
            float v0 = isRed(entity) ? .75F : .875F;
            float v1 = v0 + .125F;
            pose.mulPose(Axis.YP.rotationDegrees(180.0F - entity.getYRot()));
            pose.mulPose(Axis.XP.rotationDegrees(-entity.getXRot()));
            float bob = Mth.sin(entity.tickCount / 5.0F) * .2F + .2F;
            float scale = 1.0F + bob;
            pose.scale(scale, scale, scale);
            VertexConsumer out = buffers.getBuffer(ADDITIVE);
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
            super("thaumcraft_electric_orb", DefaultVertexFormat.NEW_ENTITY, VertexFormat.Mode.QUADS, 1536, false, true,
                    OrbType::begin, OrbType::end);
        }

        private static void begin() {
            RENDERTYPE_ENTITY_TRANSLUCENT_SHADER.setupRenderState();
            new TextureStateShard(TEXTURE, false, false).setupRenderState();
            LIGHTNING_TRANSPARENCY.setupRenderState();
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
            LIGHTNING_TRANSPARENCY.clearRenderState();
            new TextureStateShard(TEXTURE, false, false).clearRenderState();
            RENDERTYPE_ENTITY_TRANSLUCENT_SHADER.clearRenderState();
        }
    }
}
