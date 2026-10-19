package thaumcraft.client.renderers.entity;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import thaumcraft.client.renderers.models.ModelTrunk;
import thaumcraft.common.entities.golems.EntityTravelingTrunk;
import thaumcraft.common.items.golems.GolemUpgrade;

/**
 * TC4 RenderTravelingTrunk. The model is scaled and shifted by the original adjustTrunk numbers (its jump squash factors were set
 * only on the server and never changed on the client, so they are zero here: 1/1.4 on the sides, 2/1.5 overall, 2/1.33 with Earth),
 * the lid opens by {@code 1 - (1 - lidrot)^3} of a right angle, and a hit-angry trunk uses the angry texture. The upgrade icon on
 * the latch is a layer. Compiled and registered only; nothing here has been seen in a running client.
 */
public final class RenderTravelingTrunk extends MobRenderer<EntityTravelingTrunk, ModelTrunk<EntityTravelingTrunk>> {
    private static final ResourceLocation CALM = ResourceLocation.fromNamespaceAndPath("thaumcraft", "textures/models/trunk.png");
    private static final ResourceLocation ANGRY = ResourceLocation.fromNamespaceAndPath("thaumcraft", "textures/models/trunkangry.png");

    public RenderTravelingTrunk(EntityRendererProvider.Context context) {
        super(context, new ModelTrunk<>(ModelTrunk.createBodyLayer().bakeRoot()), 0.5F);
        addLayer(new UpgradeBadgeLayer(this));
    }

    /** Original adjustTrunk scale factors for a given upgrade: {sides, height}. */
    public static float[] trunkScale(int upgrade) {
        float squash = 1.0F / (0.0F + 1.0F);
        float sides = (float) (squash / 1.4);
        float overall = upgrade == GolemUpgrade.EARTH.id ? (float) (2.0 / 1.33) : (float) (2.0 / 1.5);
        return new float[] {sides * overall, 0.5F / sides * overall};
    }

    /** Original lid angle from the lid animation value: {@code -(1 - (1 - lidrot)^3) * pi / 2}. */
    public static float lidAngle(float lidrot) {
        float shut = 1.0F - lidrot;
        return -((1.0F - shut * shut * shut) * (float) Math.PI / 2.0F);
    }

    @Override protected void scale(EntityTravelingTrunk entity, PoseStack pose, float partialTick) {
        float[] scale = trunkScale(entity.getUpgrade());
        pose.scale(scale[0], scale[1], scale[0]);
        pose.translate(-0.5F, 0.5F, -0.5F);
        model.setLidAngle(lidAngle(entity.lidrot));
    }

    @Override public ResourceLocation getTextureLocation(EntityTravelingTrunk entity) {
        return entity.getAnger() > 0 ? ANGRY : CALM;
    }

    /**
     * The golem-upgrade icon on the latch (original ModelTrunk: a 0.15 block quad at (-0.075, -0.115) just in front of the latch,
     * turning with the lid). The original's quad order seems to draw it turned half a circle when seen from the front; this layer
     * draws it upright. Orientation is unverified.
     */
    private static final class UpgradeBadgeLayer extends RenderLayer<EntityTravelingTrunk, ModelTrunk<EntityTravelingTrunk>> {
        private UpgradeBadgeLayer(RenderTravelingTrunk renderer) { super(renderer); }

        @Override public void render(PoseStack pose, MultiBufferSource buffers, int light, EntityTravelingTrunk entity,
                                     float walkPhase, float walkSpeed, float partialTick, float age, float headYaw, float headPitch) {
            GolemUpgrade upgrade = GolemUpgrade.byId(entity.getUpgrade());
            if (upgrade == null || entity.isInvisible()) return;
            ResourceLocation icon = ResourceLocation.fromNamespaceAndPath("thaumcraft", "textures/items/golem_upgrade_" + upgrade.key + ".png");
            pose.pushPose();
            try {
                getParentModel().knob.translateAndRotate(pose);
                pose.translate(-0.075F, -0.115F, -0.94301F);
                pose.scale(0.15F, 0.15F, 0.15F);
                VertexConsumer buffer = buffers.getBuffer(RenderType.entityCutoutNoCull(icon));
                for (int corner = 0; corner < 4; corner++) {
                    float x = corner == 1 || corner == 2 ? 1 : 0, y = corner >= 2 ? 1 : 0;
                    buffer.addVertex(pose.last(), x, y, 0).setColor(255, 255, 255, 255).setUv(x, y)
                            .setOverlay(OverlayTexture.NO_OVERLAY).setLight(light).setNormal(pose.last(), 0, 0, -1);
                }
            } finally { pose.popPose(); }
        }
    }
}
