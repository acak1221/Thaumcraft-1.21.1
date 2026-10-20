package thaumcraft.client.renderers.models.gear;

import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.world.entity.LivingEntity;

/**
 * Original ModelKnightArmor66-283 (oracle
 * {@code thaumcraft/client/renderers/models/gear/ModelKnightArmor.java}): the
 * cultists' plate armour on the 128x64 sheet.
 *
 * <p>The original empties the six biped cube lists and hangs the gear parts on
 * the biped pivots. Its {@code float} argument selects the variant: at or above
 * 1.0 the full model adds the chest plate, the tabbard, the cloak and the
 * front cloths.
 *
 * <p>The original declares {@code BeltR} and {@code BeltL} but never hangs them
 * on a biped part, so they are never drawn and are not reproduced here.
 */
public final class ModelKnightArmor extends ModelGearArmor {
    private final ModelPart helmet;
    private final ModelPart mbelt, mbeltL, mbeltR;
    private final ModelPart chestplate, frontcloth1, frontcloth2, tabbard, backplate;
    private final ModelPart cloak1, cloak2, cloak3, cloakAtL, cloakAtR;
    private final ModelPart shoulderR, shoulderR0, shoulderR1, shoulderR2, gauntletR, gauntletstrapR1, gauntletstrapR2;
    private final ModelPart shoulderL, shoulderL0, shoulderL1, shoulderL2, gauntletL, gauntletstrapL1, gauntletstrapL2;
    private final ModelPart sidepanelR0, sidepanelR1, sidepanelR2, sidepanelR3;
    private final ModelPart sidepanelL0, sidepanelL1, sidepanelL2, sidepanelL3;

    /** The original full model, {@code new ModelKnightArmor(1.0f)}. */
    public ModelKnightArmor(ModelPart root) {
        this(root, true);
    }

    /**
     * @param armoured {@code true} for the original {@code new ModelKnightArmor(1.0f)},
     *                 {@code false} for the original {@code new ModelKnightArmor(0.5f)}.
     */
    public ModelKnightArmor(ModelPart root, boolean armoured) {
        super(root);
        var head = root.getChild("head");
        var body = root.getChild("body");
        helmet = head.getChild("helmet");
        mbelt = body.getChild("mbelt");
        mbeltL = body.getChild("mbelt_l");
        mbeltR = body.getChild("mbelt_r");
        chestplate = child(body, "chestplate");
        frontcloth1 = child(body, "frontcloth1");
        frontcloth2 = child(body, "frontcloth2");
        tabbard = child(body, "tabbard");
        backplate = child(body, "backplate");
        cloak1 = child(body, "cloak1");
        cloak2 = child(body, "cloak2");
        cloak3 = child(body, "cloak3");
        cloakAtL = child(body, "cloak_at_l");
        cloakAtR = child(body, "cloak_at_r");
        shoulderR = root.getChild("right_arm").getChild("shoulder_r");
        shoulderR0 = root.getChild("right_arm").getChild("shoulder_r0");
        shoulderR1 = root.getChild("right_arm").getChild("shoulder_r1");
        shoulderR2 = root.getChild("right_arm").getChild("shoulder_r2");
        gauntletR = root.getChild("right_arm").getChild("gauntlet_r");
        gauntletstrapR1 = root.getChild("right_arm").getChild("gauntletstrap_r1");
        gauntletstrapR2 = root.getChild("right_arm").getChild("gauntletstrap_r2");
        shoulderL = root.getChild("left_arm").getChild("shoulder_l");
        shoulderL0 = root.getChild("left_arm").getChild("shoulder_l0");
        shoulderL1 = root.getChild("left_arm").getChild("shoulder_l1");
        shoulderL2 = root.getChild("left_arm").getChild("shoulder_l2");
        gauntletL = root.getChild("left_arm").getChild("gauntlet_l");
        gauntletstrapL1 = root.getChild("left_arm").getChild("gauntletstrap_l1");
        gauntletstrapL2 = root.getChild("left_arm").getChild("gauntletstrap_l2");
        sidepanelR0 = root.getChild("right_leg").getChild("sidepanel_r0");
        sidepanelR1 = root.getChild("right_leg").getChild("sidepanel_r1");
        sidepanelR2 = root.getChild("right_leg").getChild("sidepanel_r2");
        sidepanelR3 = root.getChild("right_leg").getChild("sidepanel_r3");
        sidepanelL0 = root.getChild("left_leg").getChild("sidepanel_l0");
        sidepanelL1 = root.getChild("left_leg").getChild("sidepanel_l1");
        sidepanelL2 = root.getChild("left_leg").getChild("sidepanel_l2");
        sidepanelL3 = root.getChild("left_leg").getChild("sidepanel_l3");
    }

    /** The original mesh; the light variant keeps only the metal belt. */
    public static LayerDefinition createBodyLayer(boolean armoured) {
        var mesh = new MeshDefinition();
        var root = biped(mesh.getRoot());
        var head = root.getChild("head");
        var body = root.getChild("body");
        var rightArm = root.getChild("right_arm");
        var leftArm = root.getChild("left_arm");
        var rightLeg = root.getChild("right_leg");
        var leftLeg = root.getChild("left_leg");
        head.addOrReplaceChild("helmet", CubeListBuilder.create().texOffs(41, 8).addBox(-4.5F, -9, -4.5F, 9, 9, 9), PartPose.ZERO);
        body.addOrReplaceChild("mbelt", CubeListBuilder.create().texOffs(56, 55).addBox(-4, 8, -3, 8, 4, 1), PartPose.ZERO);
        body.addOrReplaceChild("mbelt_l", CubeListBuilder.create().texOffs(76, 44).addBox(4, 8, -3, 1, 3, 6), PartPose.ZERO);
        body.addOrReplaceChild("mbelt_r", CubeListBuilder.create().texOffs(76, 44).addBox(-5, 8, -3, 1, 3, 6), PartPose.ZERO);
        if (armoured) {
            body.addOrReplaceChild("chestplate", CubeListBuilder.create().texOffs(56, 45).addBox(-4, 1, -3, 8, 7, 1), PartPose.ZERO);
            body.addOrReplaceChild("frontcloth1", CubeListBuilder.create().texOffs(120, 39).addBox(0, 0, 0, 6, 8, 1), PartPose.offsetAndRotation(-3, 11, -3.5F, -.1047198F, 0, 0));
            body.addOrReplaceChild("frontcloth2", CubeListBuilder.create().texOffs(100, 37).addBox(0, 7.5F, 1.8F, 6, 3, 1), PartPose.offsetAndRotation(-3, 11, -3.5F, -.3316126F, 0, 0));
            body.addOrReplaceChild("tabbard", CubeListBuilder.create().texOffs(114, 52).addBox(-3, 1.2F, -3.5F, 6, 10, 1), PartPose.ZERO);
            body.addOrReplaceChild("backplate", CubeListBuilder.create().texOffs(36, 45).addBox(-4, 1, 2, 8, 11, 2), PartPose.ZERO);
            body.addOrReplaceChild("cloak1", CubeListBuilder.create().texOffs(0, 47).addBox(0, 0, 0, 9, 12, 1), PartPose.offsetAndRotation(-4.5F, 1.3F, 4.2F, .1396263F, 0, 0));
            body.addOrReplaceChild("cloak2", CubeListBuilder.create().texOffs(0, 59).addBox(0, 11.7F, -2, 9, 4, 1), PartPose.offsetAndRotation(-4.5F, 1.3F, 4.2F, .3069452F, 0, 0));
            body.addOrReplaceChild("cloak3", CubeListBuilder.create().texOffs(0, 59).addBox(0, 15.2F, -4.2F, 9, 4, 1), PartPose.offsetAndRotation(-4.5F, 1.3F, 4.2F, .4465716F, 0, 0));
            body.addOrReplaceChild("cloak_at_l", CubeListBuilder.create().texOffs(0, 43).addBox(2.5F, 1, 2, 2, 1, 3), PartPose.offsetAndRotation(0, 0, 0, .1396263F, 0, 0));
            body.addOrReplaceChild("cloak_at_r", CubeListBuilder.create().texOffs(0, 43).addBox(-4.5F, 1, 2, 2, 1, 3), PartPose.offsetAndRotation(0, 0, 0, .1396263F, 0, 0));
        }
        rightArm.addOrReplaceChild("shoulder_r", CubeListBuilder.create().texOffs(56, 35).mirror().addBox(-3.5F, -2.5F, -2.5F, 5, 5, 5), PartPose.ZERO);
        rightArm.addOrReplaceChild("shoulder_r0", CubeListBuilder.create().texOffs(0, 0).mirror().addBox(-4.3F, -1.5F, -3, 3, 5, 6), PartPose.offsetAndRotation(0, 0, 0, 0, 0, .7853982F));
        rightArm.addOrReplaceChild("shoulder_r1", CubeListBuilder.create().texOffs(0, 19).mirror().addBox(-3.3F, 3.5F, -2.5F, 1, 1, 5), PartPose.offsetAndRotation(0, 0, 0, 0, 0, .7853982F));
        rightArm.addOrReplaceChild("shoulder_r2", CubeListBuilder.create().texOffs(0, 11).mirror().addBox(-2.3F, 3.5F, -3, 1, 2, 6), PartPose.offsetAndRotation(0, 0, 0, 0, 0, .7853982F));
        rightArm.addOrReplaceChild("gauntlet_r", CubeListBuilder.create().texOffs(100, 26).mirror().addBox(-3.5F, 3.5F, -2.5F, 2, 6, 5), PartPose.ZERO);
        rightArm.addOrReplaceChild("gauntletstrap_r1", CubeListBuilder.create().texOffs(84, 31).mirror().addBox(-1.5F, 3.5F, -2.5F, 3, 1, 5), PartPose.ZERO);
        rightArm.addOrReplaceChild("gauntletstrap_r2", CubeListBuilder.create().texOffs(84, 31).mirror().addBox(-1.5F, 6.5F, -2.5F, 3, 1, 5), PartPose.ZERO);
        leftArm.addOrReplaceChild("shoulder_l", CubeListBuilder.create().texOffs(56, 35).addBox(-1.5F, -2.5F, -2.5F, 5, 5, 5), PartPose.ZERO);
        leftArm.addOrReplaceChild("shoulder_l0", CubeListBuilder.create().texOffs(0, 0).mirror().addBox(1.3F, -1.5F, -3, 3, 5, 6), PartPose.offsetAndRotation(0, 0, 0, 0, 0, -.7853982F));
        leftArm.addOrReplaceChild("shoulder_l1", CubeListBuilder.create().texOffs(0, 19).mirror().addBox(2.3F, 3.5F, -2.5F, 1, 1, 5), PartPose.offsetAndRotation(0, 0, 0, 0, 0, -.7853982F));
        leftArm.addOrReplaceChild("shoulder_l2", CubeListBuilder.create().texOffs(0, 11).mirror().addBox(1.3F, 3.5F, -3, 1, 2, 6), PartPose.offsetAndRotation(0, 0, 0, 0, 0, -.7853982F));
        leftArm.addOrReplaceChild("gauntlet_l", CubeListBuilder.create().texOffs(114, 26).addBox(1.5F, 3.5F, -2.5F, 2, 6, 5), PartPose.ZERO);
        leftArm.addOrReplaceChild("gauntletstrap_l1", CubeListBuilder.create().texOffs(84, 31).mirror().addBox(-1.5F, 3.5F, -2.5F, 3, 1, 5), PartPose.ZERO);
        leftArm.addOrReplaceChild("gauntletstrap_l2", CubeListBuilder.create().texOffs(84, 31).mirror().addBox(-1.5F, 6.5F, -2.5F, 3, 1, 5), PartPose.ZERO);
        rightLeg.addOrReplaceChild("sidepanel_r0", CubeListBuilder.create().texOffs(96, 14).addBox(-3, -.5F, -2.5F, 5, 3, 5), PartPose.offsetAndRotation(0, 0, 0, 0, 0, .1396263F));
        rightLeg.addOrReplaceChild("sidepanel_r1", CubeListBuilder.create().texOffs(96, 7).mirror().addBox(0, 2.5F, -2.5F, 2, 2, 5), PartPose.offsetAndRotation(0, 0, 0, 0, 0, .1396263F));
        rightLeg.addOrReplaceChild("sidepanel_r2", CubeListBuilder.create().texOffs(114, 5).mirror().addBox(-2, 2.5F, -2.5F, 2, 3, 5), PartPose.offsetAndRotation(0, 0, 0, 0, 0, .1396263F));
        rightLeg.addOrReplaceChild("sidepanel_r3", CubeListBuilder.create().texOffs(116, 13).addBox(-3, 2.5F, -2.5F, 1, 4, 5), PartPose.offsetAndRotation(0, 0, 0, 0, 0, .1396263F));
        leftLeg.addOrReplaceChild("sidepanel_l0", CubeListBuilder.create().texOffs(96, 14).addBox(-2, -.5F, -2.5F, 5, 3, 5), PartPose.offsetAndRotation(0, 0, 0, 0, 0, -.1396263F));
        leftLeg.addOrReplaceChild("sidepanel_l1", CubeListBuilder.create().texOffs(96, 7).mirror().addBox(-2, 2.5F, -2.5F, 2, 2, 5), PartPose.offsetAndRotation(0, 0, 0, 0, 0, -.1396263F));
        leftLeg.addOrReplaceChild("sidepanel_l2", CubeListBuilder.create().texOffs(114, 5).mirror().addBox(0, 2.5F, -2.5F, 2, 3, 5), PartPose.offsetAndRotation(0, 0, 0, 0, 0, -.1396263F));
        leftLeg.addOrReplaceChild("sidepanel_l3", CubeListBuilder.create().texOffs(116, 13).addBox(2, 2.5F, -2.5F, 1, 4, 5), PartPose.offsetAndRotation(0, 0, 0, 0, 0, -.1396263F));
        return layer(mesh);
    }

    /**
     * Original ModelKnightArmor291-298: the front cloths and the three cloak
     * segments swing with the walk cycle.
     */
    @Override public void setupAnim(LivingEntity entity, float limbSwing, float limbSwingAmount, float ageInTicks, float headYaw, float headPitch) {
        super.setupAnim(entity, limbSwing, limbSwingAmount, ageInTicks, headYaw, headPitch);
        if (frontcloth1 == null) return;
        float a = (float) Math.cos(limbSwing * 0.6662F) * 1.4F * limbSwingAmount;
        float b = (float) Math.cos(limbSwing * 0.6662F + Math.PI) * 1.4F * limbSwingAmount;
        float c = Math.min(a, b);
        frontcloth1.xRot = c - 0.1047198F;
        frontcloth2.xRot = c - 0.3316126F;
        cloak1.xRot = -c / 2 + 0.1396263F;
        cloak2.xRot = -c / 2 + 0.3069452F;
        cloak3.xRot = -c / 2 + 0.4465716F;
    }

    /** The original ModelKnightArmor336-352 for a skeleton or a zombie wearer. */
    public void setZombiePose(LivingEntity entity, float limbSwing, float limbSwingAmount, float ageInTicks) {
        zombieArms(attackTime, ageInTicks);
    }
}
