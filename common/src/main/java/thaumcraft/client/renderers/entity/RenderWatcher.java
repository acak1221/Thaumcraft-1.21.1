package thaumcraft.client.renderers.entity;

import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import thaumcraft.client.renderers.models.entities.ModelWatcher;
import thaumcraft.common.entities.monster.EntityWatcher;

/**
 * Original TC4 RenderWatcher: the guardian-like watcher mesh plus its target beam.
 * The original beam was a 1.7 FX tube with two side quads and a scrolling end cap; the
 * native port rebuilds the same geometry with native vertex buffers instead of the
 * removed 1.7 FX engine.
 */
public final class RenderWatcher extends MobRenderer<EntityWatcher, ModelWatcher> {
    private static final ResourceLocation SKIN = ResourceLocation.fromNamespaceAndPath("thaumcraft", "textures/models/watcher.png");
    private static final ResourceLocation BEAM = ResourceLocation.fromNamespaceAndPath("thaumcraft", "textures/models/watcher_beam.png");
    private static final RenderType BEAM_TYPE = new BeamType();

    public RenderWatcher(EntityRendererProvider.Context context) {
        super(context, new ModelWatcher(ModelWatcher.createBodyLayer().bakeRoot()), .5F);
    }

    @Override public ResourceLocation getTextureLocation(EntityWatcher watcher) { return SKIN; }

    @Override public void render(EntityWatcher watcher, float yaw, float partial, PoseStack pose, MultiBufferSource buffers, int light) {
        super.render(watcher, yaw, partial, pose, buffers, light);
        LivingEntity target = watcher.getTargetedEntity();
        if (target == null) return;
        drawBeam(watcher, target, partial, pose, buffers);
    }

    /** Original func_177109_a:33-121, the watcher beam. */
    private static void drawBeam(EntityWatcher watcher, LivingEntity target, float partial, PoseStack pose, MultiBufferSource buffers) {
        float charge = watcher.getBeamCharge(partial);
        Vec3 selfPos = watcher.getPosition(partial);
        Vec3 start = new Vec3(0.0D, watcher.getBbHeight(), 0.0D);
        Vec3 targetPos = target.getPosition(partial);
        Vec3 end = new Vec3(targetPos.x - selfPos.x, targetPos.y - selfPos.y + target.getBbHeight() * .5D, targetPos.z - selfPos.z);
        Vec3 delta = end.subtract(start);
        double length = delta.length() + 1.0D;
        if (delta.lengthSqr() < 1.0E-7) return;
        Vec3 direction = delta.normalize();
        float beamLength = (float) length;
        float pitch = (float) Math.acos(direction.y);
        float yawAngle = (float) Math.atan2(direction.z, direction.x);
        pose.pushPose();
        try {
            pose.translate((float) start.x, (float) start.y, (float) start.z);
            pose.mulPose(Axis.YP.rotationDegrees((1.5707964F + -yawAngle) * 57.295776F));
            pose.mulPose(Axis.XP.rotationDegrees(pitch * 57.295776F));
            float radius = .2F;
            float outer = radius * 1.41F;
            float scroll = watcher.tickCount / 20.0F + partial / 20.0F;
            float v = -1.0F + (scroll % 1.0F);
            float vEnd = beamLength * (.5F / radius) + v;
            float chargeSquared = charge * charge;
            int red = (int) (64.0F + chargeSquared * 240.0F);
            int green = (int) (32.0F + chargeSquared * 192.0F);
            int blue = (int) (128.0F - chargeSquared * 64.0F);
            VertexConsumer out = buffers.getBuffer(BEAM_TYPE);
            // The two side quads of the original square tube.
            quad(out, pose, -radius, 0, beamLength, .4999F, v, vEnd, red, green, blue);
            quad(out, pose, 0, radius, beamLength, .4999F, v, vEnd, red, green, blue);
            // The scrolling end cap of the original beam.
            float cap = watcher.tickCount % 2 == 0 ? .5F : 0.0F;
            triangle(out, pose, -outer, outer, beamLength, .5F, cap + .5F, red, green, blue);
            triangle(out, pose, outer, outer, beamLength, 1.0F, cap + .5F, red, green, blue);
            triangle(out, pose, outer, -outer, beamLength, 1.0F, cap, red, green, blue);
            triangle(out, pose, -outer, -outer, beamLength, .5F, cap, red, green, blue);
        } finally { pose.popPose(); }
    }

    private static void quad(VertexConsumer out, PoseStack pose, float x, float y, float z, float u, float v0, float v1,
                             int red, int green, int blue) {
        vertex(out, pose, x, y, 0, u, v1, red, green, blue);
        vertex(out, pose, x, y, z, u, v0, red, green, blue);
        vertex(out, pose, x + .2F, y + .2F, 0, 0.0F, v0, red, green, blue);
        vertex(out, pose, x + .2F, y + .2F, z, 0.0F, v1, red, green, blue);
    }

    private static void triangle(VertexConsumer out, PoseStack pose, float x, float y, float z, float u, float v,
                                  int red, int green, int blue) {
        vertex(out, pose, x, y, z, u, v, red, green, blue);
        vertex(out, pose, x + .2F, y, z, u + .5F, v, red, green, blue);
        vertex(out, pose, x, y + .2F, z, u, v + .5F, red, green, blue);
    }

    private static void vertex(VertexConsumer out, PoseStack pose, float x, float y, float z, float u, float v,
                               int red, int green, int blue) {
        out.addVertex(pose.last(), x, y, z).setColor(red, green, blue, 255).setUv(u, v)
                .setOverlay(net.minecraft.client.renderer.texture.OverlayTexture.NO_OVERLAY)
                .setLight(net.minecraft.client.renderer.LightTexture.FULL_BRIGHT)
                .setNormal(pose.last(), 0, 1, 0);
    }

    /** The original beam used additive blending over the watcher beam sheet. */
    private static final class BeamType extends RenderType {
        private BeamType() {
            super("thaumcraft_watcher_beam", DefaultVertexFormat.NEW_ENTITY, VertexFormat.Mode.QUADS, 1536, false, true,
                    BeamType::begin, BeamType::end);
        }

        private static void begin() {
            RENDERTYPE_ENTITY_TRANSLUCENT_SHADER.setupRenderState();
            new TextureStateShard(BEAM, false, false).setupRenderState();
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
            new TextureStateShard(BEAM, false, false).clearRenderState();
            RENDERTYPE_ENTITY_TRANSLUCENT_SHADER.clearRenderState();
        }
    }
}
