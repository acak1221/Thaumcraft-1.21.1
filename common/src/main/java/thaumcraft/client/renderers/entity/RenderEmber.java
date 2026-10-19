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
import thaumcraft.common.entities.projectile.EntityEmber;

/**
 * TC4 RenderEmber:33-65, using the retained ORIGINAL ParticleEngine atlas.
 * Row9, cells7..15 contain the orange/white fire animation: white tint, alpha.9,
 * size .25 + age/duration, camera-facing, SRC_ALPHA/ONE, no shadow. Basic FIRE
 * has no separate beam, flame trail or impact burst in this oracle. The two
 * per-tick projectile billboards themselves form its widening flame stream.
 * Native shaders replace fixed-function GL; their lighting/fog and simultaneous
 * overlapping depth-written quads still require inspected client evidence.
 */
public final class RenderEmber extends EntityRenderer<EntityEmber> {
    public static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath("thaumcraft", "textures/misc/particles.png");
    private static final RenderType ADDITIVE = new EmberRenderType();

    public RenderEmber(EntityRendererProvider.Context context) { super(context); shadowRadius = 0; }

    @Override public void render(EntityEmber entity, float yaw, float partialTick, PoseStack pose, MultiBufferSource buffers, int light) {
        if (entity.isRemoved() || entity.getDuration() <= 0) return;
        // The source deliberately uses whole entity ticks, NOT partialTick, for
        // both the frame and scale. Position interpolation remains native.
        float fraction = (float)entity.getAge() / entity.getDuration();
        int frame = (int)(8 * fraction);
        float u0 = (7 + frame) / 16F, u1 = u0 + .0625F;
        float v0 = .5625F, v1 = v0 + .0625F;
        float half = (.25F + fraction) * .5F;
        pose.pushPose();
        pose.mulPose(entityRenderDispatcher.cameraOrientation());
        VertexConsumer buffer = buffers.getBuffer(ADDITIVE);
        vertex(pose, buffer, -half, -half, u0, v1);
        vertex(pose, buffer, half, -half, u1, v1);
        vertex(pose, buffer, half, half, u1, v0);
        vertex(pose, buffer, -half, half, u0, v0);
        pose.popPose();
    }

    private static void vertex(PoseStack pose, VertexConsumer buffer, float x, float y, float u, float v) {
        buffer.addVertex(pose.last(), x, y, 0).setColor(1F, 1F, 1F, .9F)
                .setUv(u, v).setOverlay(OverlayTexture.NO_OVERLAY)
                // Preserve RenderEmber's literal packed brightness220 (block
                // light coordinate220, sky0), not environmental or full sky light.
                .setLight(220).setNormal(pose.last(), 0, 1, 0);
    }

    @Override public ResourceLocation getTextureLocation(EntityEmber entity) { return TEXTURE; }

    private static final class EmberRenderType extends RenderType {
        private static final TextureStateShard EMBER_TEXTURE = new TextureStateShard(TEXTURE, false, false);

        private EmberRenderType() {
            super("thaumcraft_ember_additive", DefaultVertexFormat.NEW_ENTITY, VertexFormat.Mode.QUADS,
                    1536, false, true, EmberRenderType::begin, EmberRenderType::end);
        }

        private static void begin() {
            RENDERTYPE_ENTITY_TRANSLUCENT_SHADER.setupRenderState();
            EMBER_TEXTURE.setupRenderState();
            LIGHTNING_TRANSPARENCY.setupRenderState(); // SRC_ALPHA / ONE, not eyes()'s ONE / ONE
            LIGHTMAP.setupRenderState();
            OVERLAY.setupRenderState();
            NO_CULL.setupRenderState();
            LEQUAL_DEPTH_TEST.setupRenderState();
            // RenderEmber never disables depth writes (unlike ParticleEngine).
            COLOR_DEPTH_WRITE.setupRenderState();
        }

        private static void end() {
            COLOR_DEPTH_WRITE.clearRenderState();
            LEQUAL_DEPTH_TEST.clearRenderState();
            NO_CULL.clearRenderState();
            OVERLAY.clearRenderState();
            LIGHTMAP.clearRenderState();
            LIGHTNING_TRANSPARENCY.clearRenderState();
            EMBER_TEXTURE.clearRenderState();
            RENDERTYPE_ENTITY_TRANSLUCENT_SHADER.clearRenderState();
        }
    }
}
