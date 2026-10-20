package thaumcraft.client.renderers.entity;

import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;

/** Original RenderExplosiveOrb/RenderElectricOrb billboards and atlas cells.
 * All geometry is buffered; native renderer owns camera interpolation/culling. */
public final class RenderFocusOrb<T extends Entity> extends EntityRenderer<T> {
    private static final ResourceLocation FIRE = ResourceLocation.fromNamespaceAndPath("thaumcraft", "textures/misc/particles2.png");
    private static final ResourceLocation ELECTRIC = ResourceLocation.fromNamespaceAndPath("thaumcraft", "textures/misc/particles.png");
    private static final RenderType FIRE_TYPE = new OrbRenderType(true), ELECTRIC_TYPE = new OrbRenderType(false);
    private final boolean explosive;
    public RenderFocusOrb(EntityRendererProvider.Context context, boolean explosive) { super(context); this.explosive = explosive; shadowRadius = 0; }
    @Override public void render(T entity, float yaw, float partial, PoseStack pose, MultiBufferSource buffers, int light) {
        if (entity.isRemoved()) return;
        float u0 = explosive ? (entity.tickCount % 4) / 16F : (1 + entity.tickCount % 6) / 8F;
        float step = explosive ? 1 / 16F : 1 / 8F, v0 = explosive ? .8125F : .875F;
        float half = explosive ? .35F : (1.2F + (float)Math.sin(entity.tickCount / 5F) * .2F) / 2;
        pose.pushPose(); pose.mulPose(entityRenderDispatcher.cameraOrientation());
        var out = buffers.getBuffer(explosive ? FIRE_TYPE : ELECTRIC_TYPE);
        vertex(out, pose, -half, -half, u0, v0 + step);
        vertex(out, pose, half, -half, u0 + step, v0 + step);
        vertex(out, pose, half, half, u0 + step, v0);
        vertex(out, pose, -half, half, u0, v0);
        pose.popPose();
    }
    private static void vertex(VertexConsumer out, PoseStack pose, float x, float y, float u, float v) {
        out.addVertex(pose.last(), x, y, 0).setColor(1F, 1F, 1F, .8F).setUv(u,v)
                .setOverlay(OverlayTexture.NO_OVERLAY).setLight(220).setNormal(pose.last(), 0, 1, 0);
    }
    @Override public ResourceLocation getTextureLocation(T entity) { return explosive ? FIRE : ELECTRIC; }
    private static final class OrbRenderType extends RenderType {
        private OrbRenderType(boolean fire) {
            super(fire ? "thaumcraft_explosive_orb" : "thaumcraft_electric_orb", DefaultVertexFormat.NEW_ENTITY,
                    VertexFormat.Mode.QUADS, 1536, false, true, () -> begin(fire), () -> end(fire));
        }
        private static void begin(boolean fire) {
            RENDERTYPE_ENTITY_TRANSLUCENT_SHADER.setupRenderState();
            new TextureStateShard(fire ? FIRE : ELECTRIC, false, false).setupRenderState();
            (fire ? TRANSLUCENT_TRANSPARENCY : LIGHTNING_TRANSPARENCY).setupRenderState();
            LIGHTMAP.setupRenderState(); OVERLAY.setupRenderState(); NO_CULL.setupRenderState();
            LEQUAL_DEPTH_TEST.setupRenderState(); COLOR_DEPTH_WRITE.setupRenderState();
        }
        private static void end(boolean fire) {
            COLOR_DEPTH_WRITE.clearRenderState(); LEQUAL_DEPTH_TEST.clearRenderState(); NO_CULL.clearRenderState();
            OVERLAY.clearRenderState(); LIGHTMAP.clearRenderState();
            (fire ? TRANSLUCENT_TRANSPARENCY : LIGHTNING_TRANSPARENCY).clearRenderState();
            new TextureStateShard(fire ? FIRE : ELECTRIC, false, false).clearRenderState();
            RENDERTYPE_ENTITY_TRANSLUCENT_SHADER.clearRenderState();
        }
    }
}
