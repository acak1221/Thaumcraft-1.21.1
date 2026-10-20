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
import thaumcraft.common.entities.monster.EntityEldritchGolem;

/**
 * Original TC4 models/entities/ModelEldritchGolem: thirty-five parts on a 128x64 sheet.
 * Every box, offset, rest rotation, mirror flag and the Arm/Shoulder parent links are the
 * original ones; the original {@code func_78787_b(128,64)} per-part texture size equals this
 * model's own texture size and is carried by the native LayerDefinition instead.
 * The head is the live one, Head2 the headless stump; the original renders exactly one.
 */
public final class ModelEldritchGolem extends EntityModel<EntityEldritchGolem> {
    private final ModelPart root;
    private final ModelPart frontCloth0, frontCloth1, frontCloth2;
    private final ModelPart collarL, collarR, collarB, collarF, collarBlack;
    private final ModelPart cloak1, cloak2, cloak3, cloakCL, cloakCR;
    private final ModelPart head, head2, torso;
    private final ModelPart armR, armL;
    private final ModelPart backPanelR1, backPanelL1;
    private final ModelPart waistR1, waistR2, waistR3, waistL1, waistL2, waistL3;
    private final ModelPart legR, legL;

    public ModelEldritchGolem(ModelPart root) {
        this.root = root;
        frontCloth0 = root.getChild("front_cloth0");
        frontCloth1 = root.getChild("front_cloth1");
        frontCloth2 = root.getChild("front_cloth2");
        collarL = root.getChild("collar_l");
        collarR = root.getChild("collar_r");
        collarB = root.getChild("collar_b");
        collarF = root.getChild("collar_f");
        collarBlack = root.getChild("collar_black");
        cloak1 = root.getChild("cloak1");
        cloak2 = root.getChild("cloak2");
        cloak3 = root.getChild("cloak3");
        cloakCL = root.getChild("cloak_cl");
        cloakCR = root.getChild("cloak_cr");
        head = root.getChild("head");
        head2 = root.getChild("head2");
        torso = root.getChild("torso");
        armR = root.getChild("arm_r");
        armL = root.getChild("arm_l");
        backPanelR1 = root.getChild("back_panel_r1");
        backPanelL1 = root.getChild("back_panel_l1");
        waistR1 = root.getChild("waist_r1");
        waistR2 = root.getChild("waist_r2");
        waistR3 = root.getChild("waist_r3");
        waistL1 = root.getChild("waist_l1");
        waistL2 = root.getChild("waist_l2");
        waistL3 = root.getChild("waist_l3");
        legR = root.getChild("leg_r");
        legL = root.getChild("leg_l");
    }

    /** Original ModelEldritchGolem:47-153. Texture 128x64. */
    public static LayerDefinition createBodyLayer() {
        var mesh = new MeshDefinition();
        var root = mesh.getRoot();
        root.addOrReplaceChild("cloak1", CubeListBuilder.create().texOffs(0, 47)
                .addBox(-5F, 1.5F, 4F, 10, 12, 1), PartPose.offsetAndRotation(0, 0, -2.5F, .1396263F, 0, 0));
        root.addOrReplaceChild("cloak3", CubeListBuilder.create().texOffs(0, 37)
                .addBox(-5F, 17.5F, -.8F, 10, 4, 1), PartPose.offsetAndRotation(0, 0, -2.5F, .4465716F, 0, 0));
        root.addOrReplaceChild("cloak2", CubeListBuilder.create().texOffs(0, 59)
                .addBox(-5F, 13.5F, 1.7F, 10, 4, 1), PartPose.offsetAndRotation(0, 0, -2.5F, .3069452F, 0, 0));
        root.addOrReplaceChild("cloak_cl", CubeListBuilder.create().texOffs(0, 43)
                .addBox(3F, .5F, 2F, 2, 1, 3), PartPose.offsetAndRotation(0, 0, -2.5F, .1396263F, 0, 0));
        root.addOrReplaceChild("cloak_cr", CubeListBuilder.create().texOffs(0, 43)
                .addBox(-5F, .5F, 2F, 2, 1, 3), PartPose.offsetAndRotation(0, 0, -2.5F, .1396263F, 0, 0));
        root.addOrReplaceChild("head", CubeListBuilder.create().texOffs(47, 12)
                .addBox(-3.5F, -6F, -2.5F, 7, 7, 5), PartPose.offsetAndRotation(0, 4.5F, -3.8F, -.1047198F, 0, 0));
        root.addOrReplaceChild("head2", CubeListBuilder.create().texOffs(26, 16)
                .addBox(-2F, -2F, -2F, 4, 4, 4), PartPose.offsetAndRotation(0, 0, -5F, -.1047198F, 0, 0));
        root.addOrReplaceChild("collar_l", CubeListBuilder.create().texOffs(75, 50)
                .addBox(3.5F, -.5F, -7F, 1, 4, 10), PartPose.offsetAndRotation(0, 0, -2.5F, .837758F, 0, 0));
        root.addOrReplaceChild("collar_r", CubeListBuilder.create().texOffs(67, 50)
                .addBox(-4.5F, -.5F, -7F, 1, 4, 10), PartPose.offsetAndRotation(0, 0, -2.5F, .837758F, 0, 0));
        root.addOrReplaceChild("collar_b", CubeListBuilder.create().texOffs(77, 59)
                .addBox(-3.5F, -.5F, 2F, 7, 4, 1), PartPose.offsetAndRotation(0, 0, -2.5F, .837758F, 0, 0));
        root.addOrReplaceChild("collar_f", CubeListBuilder.create().texOffs(77, 59)
                .addBox(-3.5F, -.5F, -7F, 7, 4, 1), PartPose.offsetAndRotation(0, 0, -2.5F, .837758F, 0, 0));
        root.addOrReplaceChild("collar_black", CubeListBuilder.create().texOffs(22, 0)
                .addBox(-3.5F, 0F, -6F, 7, 1, 8), PartPose.offsetAndRotation(0, 0, -2.5F, .837758F, 0, 0));
        root.addOrReplaceChild("front_cloth0", CubeListBuilder.create().texOffs(114, 52)
                .addBox(-3F, 3.2F, -3.5F, 6, 10, 1), PartPose.offsetAndRotation(0, 0, -2.5F, .1745329F, 0, 0));
        root.addOrReplaceChild("front_cloth1", CubeListBuilder.create().texOffs(114, 39)
                .addBox(-1F, 1.5F, -3.5F, 6, 6, 1), PartPose.offsetAndRotation(-2F, 12F, 0, -.1047198F, 0, 0));
        root.addOrReplaceChild("front_cloth2", CubeListBuilder.create().texOffs(114, 47)
                .addBox(-1F, 8.5F, -1.5F, 6, 3, 1), PartPose.offsetAndRotation(-2F, 11F, 0, -.3316126F, 0, 0));
        root.addOrReplaceChild("torso", CubeListBuilder.create().texOffs(34, 45).mirror()
                .addBox(-5F, 2.5F, -3F, 10, 10, 6), PartPose.offsetAndRotation(0, 0, -2.5F, .1745329F, 0, 0));
        var armR = root.addOrReplaceChild("arm_r", CubeListBuilder.create().texOffs(78, 32)
                .addBox(-3.5F, 1.5F, -2F, 4, 13, 5), PartPose.offsetAndRotation(-5F, 3F, -2F, 0, 0, .1047198F));
        armR.addOrReplaceChild("shoulder_r1", CubeListBuilder.create().texOffs(0, 23)
                .addBox(-3.3F, 4F, -2.5F, 1, 2, 6), PartPose.offsetAndRotation(0, 0, 0, 0, 0, 1.186824F));
        armR.addOrReplaceChild("shoulder_r", CubeListBuilder.create().texOffs(0, 0)
                .addBox(-4.3F, -1F, -3F, 4, 5, 7), PartPose.offsetAndRotation(0, 0, 0, 0, 0, 1.186824F));
        armR.addOrReplaceChild("shoulder_r2", CubeListBuilder.create().texOffs(0, 12)
                .addBox(-2.3F, 4F, -3F, 2, 3, 7), PartPose.offsetAndRotation(0, 0, 0, 0, 0, 1.186824F));
        armR.addOrReplaceChild("shoulder_r0", CubeListBuilder.create().texOffs(56, 31)
                .addBox(-4.5F, -1.5F, -2.5F, 5, 6, 6), PartPose.offsetAndRotation(0, 0, 0, 0, 0, 0));
        var armL = root.addOrReplaceChild("arm_l", CubeListBuilder.create().texOffs(78, 32).mirror()
                .addBox(-.5F, 1.5F, -2F, 4, 13, 5), PartPose.offsetAndRotation(5F, 3F, -2F, 0, 0, -.1047198F));
        armL.addOrReplaceChild("shoulder_l1", CubeListBuilder.create().texOffs(0, 23).mirror()
                .addBox(2.3F, 4F, -2.5F, 1, 2, 6), PartPose.offsetAndRotation(0, 0, 0, 0, 0, -1.186824F));
        armL.addOrReplaceChild("shoulder_l0", CubeListBuilder.create().texOffs(56, 31).mirror()
                .addBox(-.5F, -1.5F, -2.5F, 5, 6, 6), PartPose.offsetAndRotation(0, 0, 0, 0, 0, 0));
        armL.addOrReplaceChild("shoulder_l", CubeListBuilder.create().texOffs(0, 0).mirror()
                .addBox(.3F, -1F, -3F, 4, 5, 7), PartPose.offsetAndRotation(0, 0, 0, 0, 0, -1.186824F));
        armL.addOrReplaceChild("shoulder_l2", CubeListBuilder.create().texOffs(0, 12).mirror()
                .addBox(.3F, 4F, -3F, 2, 3, 7), PartPose.offsetAndRotation(0, 0, 0, 0, 0, -1.186824F));
        root.addOrReplaceChild("back_panel_r1", CubeListBuilder.create().texOffs(96, 7)
                .addBox(0F, 2.5F, -2.5F, 2, 2, 5), PartPose.offsetAndRotation(-2F, 12F, 0, 0, 0, .1396263F));
        root.addOrReplaceChild("waist_r1", CubeListBuilder.create().texOffs(96, 14)
                .addBox(-3F, -.5F, -2.5F, 5, 3, 5), PartPose.offsetAndRotation(-2F, 12F, 0, 0, 0, .1396263F));
        root.addOrReplaceChild("waist_r2", CubeListBuilder.create().texOffs(116, 13)
                .addBox(-3F, 2.5F, -2.5F, 1, 4, 5), PartPose.offsetAndRotation(-2F, 12F, 0, 0, 0, .1396263F));
        root.addOrReplaceChild("waist_r3", CubeListBuilder.create().texOffs(114, 5).mirror()
                .addBox(-2F, 2.5F, -2.5F, 2, 3, 5), PartPose.offsetAndRotation(-2F, 12F, 0, 0, 0, .1396263F));
        root.addOrReplaceChild("leg_r", CubeListBuilder.create().texOffs(79, 19)
                .addBox(-2.5F, 2.5F, -2F, 4, 9, 4), PartPose.offsetAndRotation(-2F, 12.5F, 0, 0, 0, 0));
        root.addOrReplaceChild("waist_l1", CubeListBuilder.create().texOffs(96, 14).mirror()
                .addBox(-2F, -.5F, -2.5F, 5, 3, 5), PartPose.offsetAndRotation(2F, 12F, 0, 0, 0, -.1396263F));
        root.addOrReplaceChild("waist_l2", CubeListBuilder.create().texOffs(116, 13).mirror()
                .addBox(2F, 2.5F, -2.5F, 1, 4, 5), PartPose.offsetAndRotation(2F, 12F, 0, 0, 0, -.1396263F));
        root.addOrReplaceChild("waist_l3", CubeListBuilder.create().texOffs(114, 5).mirror()
                .addBox(0F, 2.5F, -2.5F, 2, 3, 5), PartPose.offsetAndRotation(2F, 12F, 0, 0, 0, -.1396263F));
        root.addOrReplaceChild("back_panel_l1", CubeListBuilder.create().texOffs(96, 7).mirror()
                .addBox(-2F, 2.5F, -2.5F, 2, 2, 5), PartPose.offsetAndRotation(2F, 12F, 0, 0, 0, -.1396263F));
        root.addOrReplaceChild("leg_l", CubeListBuilder.create().texOffs(79, 19).mirror()
                .addBox(-1.5F, 2.5F, -2F, 4, 9, 4), PartPose.offsetAndRotation(2F, 12.5F, 0, 0, 0, 0));
        return LayerDefinition.create(mesh, 128, 64);
    }

    /** Original func_78086_a:206-217: the shared two-arm attack swing, else the idle swing. */
    @Override public void prepareMobModel(EntityEldritchGolem golem, float limb, float amount, float partialTick) {
        int attack = golem.getAttackTimer();
        if (attack > 0) {
            armR.xRot = armL.xRot = -2.0F + 1.5F * triangleWave(attack - partialTick, 10.0F);
        } else {
            armR.xRot = Mth.sin(limb * .4F + (float) Math.PI) * 2.0F * amount * .5F;
            armL.xRot = Mth.sin(limb * .4F) * 2.0F * amount * .5F;
        }
    }

    /** Original func_78087_a:178-198 plus the cloth half of func_78088_a:171-179. */
    @Override public void setupAnim(EntityEldritchGolem golem, float limb, float amount, float age, float yaw, float pitch) {
        if (golem.getSpawnTimer() > 0) {
            head.yRot = 0;
            head.xRot = (golem.getSpawnTimer() / 2) / 57.295776F;
        } else {
            head.yRot = yaw / 4.0F / 57.295776F;
            head.xRot = pitch / 2.0F / 57.295776F;
            head2.yRot = yaw / 57.295776F;
            head2.xRot = pitch / 57.295776F;
        }
        legR.xRot = Mth.sin(limb * .4662F) * 1.4F * amount;
        legL.xRot = Mth.sin(limb * .4662F + (float) Math.PI) * 1.4F * amount;
        float a = Mth.sin(limb * .44F) * 1.4F * amount;
        float b = Mth.sin(limb * .44F + (float) Math.PI) * 1.4F * amount;
        float c = Math.min(a, b);
        frontCloth1.xRot = c - .1047198F;
        frontCloth2.xRot = c - .3316126F;
        cloak1.xRot = -c / 3.0F + .1396263F;
        cloak2.xRot = -c / 3.0F + .3069452F;
        cloak3.xRot = -c / 3.0F + .4465716F;
        // Original func_78088_a:184-188 renders the live head or the headless stump.
        head.visible = !golem.isHeadless();
        head2.visible = golem.isHeadless();
    }

    /** Original doAbs:219-221; a triangle wave, not a cosine swing. */
    private static float triangleWave(float phase, float period) {
        return (Math.abs(phase % period - period * .5F) - period * .25F) / (period * .25F);
    }

    @Override public void renderToBuffer(PoseStack pose, VertexConsumer out, int light, int overlay, int color) {
        root.render(pose, out, light, overlay, color);
    }
}
