package thaumcraft.client.renderers.models.entities;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.util.Mth;
import thaumcraft.common.entities.monster.EntityEldritchCrab;

/**
 * Original TC4 models/entities/ModelEldritchCrab:22 parts on a 128x64 sheet, 22 boxes
 * and the helm/bare tail swap. All part offsets, box sizes and the rest rotations are
 * the original ones; only the ModelRenderer storage became native ModelPart.
 * The original {@code func_78787_b(128,64)} per-part texture size equals this model's
 * own texture size, so the native LayerDefinition carries it instead.
 */
public final class ModelEldritchCrab extends EntityModel<EntityEldritchCrab> {
    private final ModelPart root;
    private final ModelPart tailHelm, tailBare, rClaw1, head1, rClaw0, rClaw2, lClaw2, lClaw1;
    private final ModelPart rArm, torso, rRLeg1, rFLeg1, head0, lRLeg1, lFLeg1, rRLeg0, rFLeg0, lFLeg0, lRLeg0, lClaw0, lArm;

    public ModelEldritchCrab(ModelPart root) {
        this.root = root;
        tailHelm = root.getChild("tail_helm");
        tailBare = root.getChild("tail_bare");
        rClaw1 = root.getChild("r_claw1");
        head1 = root.getChild("head1");
        rClaw0 = root.getChild("r_claw0");
        rClaw2 = root.getChild("r_claw2");
        lClaw2 = root.getChild("l_claw2");
        lClaw1 = root.getChild("l_claw1");
        rArm = root.getChild("r_arm");
        torso = root.getChild("torso");
        rRLeg1 = root.getChild("rr_leg1");
        rFLeg1 = root.getChild("rf_leg1");
        head0 = root.getChild("head0");
        lRLeg1 = root.getChild("lr_leg1");
        lFLeg1 = root.getChild("lf_leg1");
        rRLeg0 = root.getChild("rr_leg0");
        rFLeg0 = root.getChild("rf_leg0");
        lFLeg0 = root.getChild("lf_leg0");
        lRLeg0 = root.getChild("lr_leg0");
        lClaw0 = root.getChild("l_claw0");
        lArm = root.getChild("l_arm");
    }

    /** Original ModelEldritchCrab:42-117. Texture 128x64, twenty-two boxes. */
    public static LayerDefinition createBodyLayer() {
        var mesh = new MeshDefinition();
        var root = mesh.getRoot();
        root.addOrReplaceChild("tail_helm", CubeListBuilder.create().texOffs(0, 0)
                .addBox(-4.5F, -4.5F, -0.4F, 9, 9, 9), PartPose.offsetAndRotation(0, 18, 0, .1047198F, 0, 0));
        root.addOrReplaceChild("tail_bare", CubeListBuilder.create().texOffs(64, 0)
                .addBox(-4F, -4F, -0.4F, 8, 8, 8), PartPose.offsetAndRotation(0, 18, 0, .1047198F, 0, 0));
        root.addOrReplaceChild("r_claw1", CubeListBuilder.create().texOffs(0, 47)
                .addBox(-2F, -1F, -5.066667F, 4, 3, 5), PartPose.offset(-6, 15.5F, -10));
        root.addOrReplaceChild("head1", CubeListBuilder.create().texOffs(0, 38)
                .addBox(-2F, -1.5F, -9.066667F, 4, 4, 1), PartPose.offset(0, 18, 0));
        root.addOrReplaceChild("r_claw0", CubeListBuilder.create().texOffs(0, 55)
                .addBox(-2F, -2.5F, -3.066667F, 4, 5, 3), PartPose.offset(-6, 17, -7));
        root.addOrReplaceChild("r_claw2", CubeListBuilder.create().texOffs(14, 54)
                .addBox(-1.5F, -1F, -4.066667F, 3, 2, 5), PartPose.offsetAndRotation(-6, 18.5F, -10, .3141593F, 0, 0));
        root.addOrReplaceChild("r_arm", CubeListBuilder.create().texOffs(44, 4)
                .addBox(-1F, -1F, -5.066667F, 2, 2, 6), PartPose.offsetAndRotation(-3, 17, -4, 0, .7504916F, 0));
        root.addOrReplaceChild("l_claw2", CubeListBuilder.create().texOffs(14, 54)
                .addBox(-1.5F, -1F, -4.066667F, 3, 2, 5), PartPose.offsetAndRotation(6, 18.5F, -10, .3141593F, 0, 0));
        // The original mirror flag was set before addBox and only mirrors that box's UVs.
        root.addOrReplaceChild("l_claw1", CubeListBuilder.create().texOffs(0, 47).mirror()
                .addBox(-2F, -1F, -5.066667F, 4, 3, 5), PartPose.offset(6, 15.5F, -10));
        root.addOrReplaceChild("l_claw0", CubeListBuilder.create().texOffs(0, 55).mirror()
                .addBox(-2F, -2.5F, -3.066667F, 4, 5, 3), PartPose.offset(6, 17, -7));
        root.addOrReplaceChild("l_arm", CubeListBuilder.create().texOffs(44, 4)
                .addBox(-1F, -1F, -4.066667F, 2, 2, 6), PartPose.offsetAndRotation(4, 17, -5, 0, -.7504916F, 0));
        root.addOrReplaceChild("torso", CubeListBuilder.create().texOffs(0, 18)
                .addBox(-3.5F, -3.5F, -6.066667F, 7, 7, 6), PartPose.offsetAndRotation(0, 18, 0, .0523599F, 0, 0));
        root.addOrReplaceChild("head0", CubeListBuilder.create().texOffs(0, 31)
                .addBox(-2.5F, -2F, -8.066667F, 5, 5, 2), PartPose.offset(0, 18, 0));
        root.addOrReplaceChild("rr_leg1", CubeListBuilder.create().texOffs(36, 4)
                .addBox(-4.5F, 1F, -0.9F, 2, 5, 2), PartPose.offset(-4, 20, -1.5F));
        root.addOrReplaceChild("rf_leg1", CubeListBuilder.create().texOffs(36, 4)
                .addBox(-5F, 1F, -1.066667F, 2, 5, 2), PartPose.offset(-4, 20, -3.5F));
        root.addOrReplaceChild("lr_leg1", CubeListBuilder.create().texOffs(36, 4)
                .addBox(2.5F, 1F, -0.9F, 2, 5, 2), PartPose.offset(4, 20, -1.5F));
        root.addOrReplaceChild("lf_leg1", CubeListBuilder.create().texOffs(36, 4)
                .addBox(3F, 1F, -1.066667F, 2, 5, 2), PartPose.offset(4, 20, -3.5F));
        root.addOrReplaceChild("rr_leg0", CubeListBuilder.create().texOffs(36, 0)
                .addBox(-4.5F, -1F, -0.9F, 6, 2, 2), PartPose.offset(-4, 20, -1.5F));
        root.addOrReplaceChild("rf_leg0", CubeListBuilder.create().texOffs(36, 0)
                .addBox(-5F, -1F, -1.066667F, 6, 2, 2), PartPose.offset(-4, 20, -3.5F));
        root.addOrReplaceChild("lf_leg0", CubeListBuilder.create().texOffs(36, 0)
                .addBox(-1F, -1F, -1.066667F, 6, 2, 2), PartPose.offset(4, 20, -3.5F));
        root.addOrReplaceChild("lr_leg0", CubeListBuilder.create().texOffs(36, 0)
                .addBox(-1.5F, -1F, -0.9F, 6, 2, 2), PartPose.offset(4, 20, -1.5F));
        return LayerDefinition.create(mesh, 128, 64);
    }

    /** Original func_78087_a:154-187. Limb phase, tail sway and the two claw idles. */
    @Override public void setupAnim(EntityEldritchCrab crab, float limb, float amount, float age, float yaw, float pitch) {
        rRLeg1.setRotation(0, .2094395F, .4363323F);
        rFLeg1.setRotation(0, -.2094395F, .4363323F);
        lRLeg1.setRotation(0, -.2094395F, -.4363323F);
        lFLeg1.setRotation(0, .2094395F, -.4363323F);
        rRLeg0.setRotation(0, .2094395F, .4363323F);
        rFLeg0.setRotation(0, -.2094395F, .4363323F);
        lFLeg0.setRotation(0, -.2094395F, -.4363323F);
        lRLeg0.setRotation(0, .2094395F, -.4363323F);
        float f9 = legSwing(limb, amount);
        float f10 = legSwingHalfPhase(limb, amount);
        rRLeg1.yRot += f9;
        rRLeg0.yRot += f9;
        lFLeg1.yRot += -f9;
        lFLeg0.yRot += -f9;
        rFLeg1.yRot += f10;
        rFLeg0.yRot += f10;
        lRLeg1.yRot += -f10;
        lRLeg0.yRot += -f10;
        rRLeg1.zRot += f9;
        rRLeg0.zRot += f9;
        lFLeg1.zRot += -f9;
        lFLeg0.zRot += -f9;
        rFLeg1.zRot += f10;
        rFLeg0.zRot += f10;
        lRLeg1.zRot += -f10;
        lRLeg0.zRot += -f10;
        // Original ModelEldritchCrab:181-182 - MathHelper.cos (MCP func_76134_b), not sin.
        tailBare.yRot = tailHelm.yRot = tailSway(limb, amount);
        tailBare.zRot = tailHelm.zRot = tailSwayTilt(limb, amount);
        float ticks = crab.tickCount;
        rClaw2.xRot = .3141593F - Mth.sin(ticks / 4F) * .25F;
        lClaw2.xRot = .3141593F + Mth.sin(ticks / 4.1F) * .25F;
        rClaw1.xRot = Mth.sin(ticks / 4F) * .125F;
        lClaw1.xRot = -Mth.sin(ticks / 4.1F) * .125F;
        // Original func_78088_a:122-126 renders exactly one of the two tails.
        tailHelm.visible = crab.hasHelm();
        tailBare.visible = !crab.hasHelm();
    }

    @Override public void renderToBuffer(PoseStack pose, VertexConsumer out, int light, int overlay, int color) {
        root.render(pose, out, light, overlay, color);
    }

    /**
     * Original ModelEldritchCrab:163 - the swing of the right legs of the crab.
     *
     * <p>The original used {@code MathHelper.cos} (MCP {@code func_76134_b}) and not the
     * sine: the legs of the crab start at their rest angle when the phase is zero.
     * {@code MathHelper.func_76134_b} is the lookup table of the 1.7.10 {@code MathHelper}
     * - 65536 sine values - and the native {@code Mth.cos} is that same table, so the swing
     * of the native crab is the exact swing of the original and not a {@code Math.cos}.</p>
     */
    public static float legSwing(float limb, float amount) {
        return -(Mth.cos(limb * .6662F * 2F) * .4F) * amount;
    }

    /** Original ModelEldritchCrab:164 - the swing of the left legs of the crab. */
    public static float legSwingHalfPhase(float limb, float amount) {
        return -(Mth.cos(limb * .6662F * 2F + (float) Math.PI) * .4F) * amount;
    }

    /** Original ModelEldritchCrab:181 - the sway of the tail of the crab around its axis. */
    public static float tailSway(float limb, float amount) {
        return Mth.cos(limb * .6662F) * 2F * amount * .125F;
    }

    /** Original ModelEldritchCrab:182 - the tilt of the tail of the crab. */
    public static float tailSwayTilt(float limb, float amount) {
        return Mth.cos(limb * .6662F) * amount * .125F;
    }
}
