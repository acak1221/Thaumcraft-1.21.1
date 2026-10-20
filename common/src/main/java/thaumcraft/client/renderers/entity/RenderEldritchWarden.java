package thaumcraft.client.renderers.entity;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import thaumcraft.client.renderers.models.ModelEldritchGuardian;
import thaumcraft.common.entities.monster.EntityEldritchWarden;

/**
 * Original TC4 RenderEldritchGuardian: the warden is the same 54-part guardian mesh with
 * the warden sheet, a 1.5 scale and a spawn-timer lift out of the floor.
 * The native ModelEldritchGuardian is bound to the guardian entity, so this renderer
 * bakes the same layer and repeats the original animation for the warden's own arm lift.
 */
public final class RenderEldritchWarden extends EntityRenderer<EntityEldritchWarden> {
    private static final ResourceLocation SKIN = ResourceLocation.fromNamespaceAndPath("thaumcraft", "textures/models/eldritch_warden.png");
    private final ModelPart root;
    private final ModelPart hood1, armL1, armR1, legpanelC1, legpanelC2, legpanelC3;
    private final ModelPart cloak1, cloak2, cloak3;
    private final ModelPart sidepanelL2, sidepanelL3, sidepanelL4, sidepanelR2, sidepanelR3, sidepanelR4;

    public RenderEldritchWarden(EntityRendererProvider.Context context) {
        super(context);
        shadowRadius = .5F;
        root = ModelEldritchGuardian.createBodyLayer().bakeRoot();
        root.getChild("HoodEye").visible = false;
        hood1 = root.getChild("Hood1");
        armL1 = root.getChild("ArmL1");
        armR1 = root.getChild("ArmR1");
        legpanelC1 = root.getChild("LegpanelC1");
        legpanelC2 = legpanelC1.getChild("LegpanelC2");
        legpanelC3 = legpanelC2.getChild("LegpanelC3");
        cloak1 = root.getChild("Cloak1");
        cloak2 = cloak1.getChild("Cloak2");
        cloak3 = cloak2.getChild("Cloak3");
        sidepanelL2 = root.getChild("SidepanelL2");
        sidepanelL3 = sidepanelL2.getChild("SidepanelL3");
        sidepanelL4 = sidepanelL3.getChild("SidepanelL4");
        sidepanelR2 = root.getChild("SidepanelR2");
        sidepanelR3 = sidepanelR2.getChild("SidepanelR3");
        sidepanelR4 = sidepanelR3.getChild("SidepanelR4");
    }

    @Override public ResourceLocation getTextureLocation(EntityEldritchWarden warden) { return SKIN; }

    /** Original doRenderLiving:60-79 sank the warden while it was still spawning. */
    @Override public Vec3 getRenderOffset(EntityEldritchWarden warden, float partial) {
        int timer = warden.getSpawnTimer();
        if (timer <= 0) return Vec3.ZERO;
        return new Vec3(0, -warden.getBbHeight() * (timer / 150F), 0);
    }

    @Override public void render(EntityEldritchWarden warden, float yaw, float partial, PoseStack pose, MultiBufferSource buffers, int light) {
        pose.pushPose();
        try {
            // The original RenderLiving transform: the model is drawn flipped and
            // shifted down by 1.501 units, then the warden's own 1.5 scale.
            pose.scale(-1.0F, -1.0F, 1.0F);
            pose.scale(1.5F, 1.5F, 1.5F);
            pose.translate(0.0F, -1.501F, 0.0F);
            animate(warden, partial);
            VertexConsumer out = buffers.getBuffer(RenderType.entityTranslucent(SKIN));
            root.render(pose, out, light, OverlayTexture.NO_OVERLAY, 0xffffffff);
        } finally { pose.popPose(); }
    }

    /** The original ModelEldritchGuardian animation, with the warden's own arm lift. */
    private void animate(EntityEldritchWarden warden, float age) {
        float headYaw = Mth.rotLerp(age, warden.yHeadRotO, warden.getYHeadRot());
        float headPitch = Mth.lerp(age, warden.xRotO, warden.getXRot());
        hood1.yRot = headYaw / 57.295776F;
        hood1.xRot = headPitch / 57.295776F;
        armL1.xRot = -1 - warden.armLiftL + Mth.sin((age + 20) / 10) * .08F;
        armR1.xRot = -1 - warden.armLiftR + Mth.sin(age / 10) * .08F;
        legpanelC1.xRot = -.15F + Mth.sin(age / 8) * .12F;
        legpanelC2.xRot = Mth.sin((age - 5) / 8) * .13F;
        legpanelC3.xRot = Mth.sin((age - 10) / 8) * .14F;
        cloak1.xRot = .2F + Mth.sin(age / 7) * .08F;
        cloak2.xRot = Mth.sin((age - 5) / 7) * .1F;
        cloak3.xRot = Mth.sin((age - 10) / 7) * .12F;
        sidepanelL2.zRot = -.2F + Mth.sin((age + 10) / 8) * .12F;
        sidepanelL3.zRot = Mth.sin((age + 5) / 8) * .13F;
        sidepanelL4.zRot = Mth.sin(age / 8) * .14F;
        sidepanelR2.zRot = .2F + Mth.sin((age - 5) / 8) * .12F;
        sidepanelR3.zRot = Mth.sin((age - 10) / 8) * .13F;
        sidepanelR4.zRot = Mth.sin((age - 15) / 8) * .14F;
    }
}
