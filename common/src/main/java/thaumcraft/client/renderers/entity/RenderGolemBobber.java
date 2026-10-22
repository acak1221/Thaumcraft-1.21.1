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
import net.minecraft.world.phys.Vec3;
import thaumcraft.common.entities.golems.EntityGolemBobber;

/** Original golem bobber quad and body-yaw/eye-height line, not a player hook. */
public final class RenderGolemBobber extends EntityRenderer<EntityGolemBobber> {
    /**
     * The original's own sprite.
     *
     * <p>TC4 4.2.3.5 draws the 8x8 cell <b>(1,2)</b> of the <b>vanilla 1.7.10</b>
     * {@code textures/particle/particles.png} (oracle
     * {@code thaumcraft/client/renderers/entity/RenderGolemBobber.java:31,40-45}:
     * {@code b0 = 1}, {@code b1 = 2}, {@code f2 = (b0*8+0)/128f = 0.0625},
     * {@code f3 = (b0*8+8)/128f = 0.125}, {@code f4 = (b1*8+0)/128f = 0.125},
     * {@code f5 = (b1*8+8)/128f = 0.1875}).</p>
     *
     * <p>1.21.1 has no such sheet — every particle owns a separate file — so the
     * exact cell was lifted out of the official Mojang 1.7.10 {@code client.jar}
     * ({@code assets/minecraft/textures/particle/particles.png}, the pixels of
     * the columns 8..15 and the rows 16..23) and shipped as this 8x8 asset.
     * The alpha of the cell is only 0 or 255, so the original's blending and
     * the native cutout draw the same pixels.</p>
     */
    private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath("thaumcraft", "textures/entity/golem_bobber.png");
    private static final RenderType BOBBER = RenderType.entityCutout(TEXTURE);

    public RenderGolemBobber(EntityRendererProvider.Context context) { super(context); }

    @Override public void render(EntityGolemBobber bobber, float yaw, float partialTick,
                                 PoseStack pose, MultiBufferSource buffers, int light) {
        if (bobber.isInvisible()) return;
        pose.pushPose();
        try {
            pose.scale(.5F, .5F, .5F);
            pose.mulPose(entityRenderDispatcher.cameraOrientation());
            pose.mulPose(Axis.YP.rotationDegrees(180));
            var vertices = buffers.getBuffer(BOBBER);
            // The original's four corners in the sheet: (f2,f5) (f3,f5) (f3,f4) (f2,f4) of the cell (1,2).
            float u0 = GolemBobberSprite.cellU(GolemBobberSprite.U0), u1 = GolemBobberSprite.cellU(GolemBobberSprite.U1);
            float v0 = GolemBobberSprite.cellV(GolemBobberSprite.V0), v1 = GolemBobberSprite.cellV(GolemBobberSprite.V1);
            quadVertex(vertices, pose.last(), -.5F, -.5F, u0, v1, light);
            quadVertex(vertices, pose.last(), .5F, -.5F, u1, v1, light);
            quadVertex(vertices, pose.last(), .5F, .5F, u1, v0, light);
            quadVertex(vertices, pose.last(), -.5F, .5F, u0, v0, light);
        } finally { pose.popPose(); }

        var fisher = bobber.getFisher();
        if (fisher != null) {
            // R:75–77 overwrites the earlier hand/swing vector. The effective
            // source anchor is body yaw + eye height, never the player's camera.
            float bodyYaw = Mth.lerp(partialTick, fisher.yBodyRotO, fisher.yBodyRot) * Mth.DEG_TO_RAD;
            double sin = Math.sin(bodyYaw), cos = Math.cos(bodyYaw);
            double bx = Mth.lerp(partialTick, bobber.xo, bobber.getX());
            double by = Mth.lerp(partialTick, bobber.yo, bobber.getY());
            double bz = Mth.lerp(partialTick, bobber.zo, bobber.getZ());
            double dx = Mth.lerp(partialTick, fisher.xo, fisher.getX()) - cos * .25 - sin * .7 - bx;
            double dy = Mth.lerp(partialTick, fisher.yo, fisher.getY()) + fisher.getEyeHeight() - .4 - (by + .25);
            double dz = Mth.lerp(partialTick, fisher.zo, fisher.getZ()) - sin * .25 + cos * .7 - bz;
            var lines = buffers.getBuffer(RenderType.lineStrip());
            for (int index = 0; index <= 16; ++index) {
                double t = index / 16.0;
                Vec3 tangent = new Vec3(dx, dy * (t + .5), dz);
                tangent = tangent.lengthSqr() < 1.0E-12 ? new Vec3(0, 1, 0) : tangent.normalize();
                // Incoming entity pose is already camera-relative. These are
                // bobber-local points: do not subtract/translate the camera again.
                lines.addVertex(pose.last(), (float) (dx * t), (float) (.25 + dy * (t * t + t) * .5), (float) (dz * t))
                        .setColor(0, 0, 0, 255)
                        .setNormal(pose.last(), (float) tangent.x, (float) tangent.y, (float) tangent.z);
            }
        } // Missing/out-of-order owner metadata hides only the rope, not the quad.
        super.render(bobber, yaw, partialTick, pose, buffers, light);
    }

    private static void quadVertex(VertexConsumer vertices, PoseStack.Pose pose, float x, float y, float u, float v, int light) {
        vertices.addVertex(pose, x, y, 0).setColor(255, 255, 255, 255).setUv(u, v)
                .setOverlay(OverlayTexture.NO_OVERLAY).setLight(light).setNormal(pose, 0, 1, 0);
    }

    @Override public ResourceLocation getTextureLocation(EntityGolemBobber bobber) { return TEXTURE; }
}
