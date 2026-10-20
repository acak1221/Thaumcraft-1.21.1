package thaumcraft.client.renderers.models.gear;

import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.world.entity.LivingEntity;

/**
 * Original ModelLeaderArmor78-294 (oracle
 * {@code thaumcraft/client/renderers/models/gear/ModelLeaderArmor.java}): the
 * cultist leaders' armour on the 128x64 sheet.
 *
 * <p>The original empties the six biped cube lists and hangs the gear parts on
 * the biped pivots. Its {@code float} argument selects the variant: at or above
 * 1.0 the full model adds the chest plate, the collar, the cloak, the chest
 * cloths and the leg cloths.
 *
 * <p>The original declares {@code BeltR} and {@code BeltL} but never hangs them
 * on a biped part outside the full variant; both are reproduced there.
 */
public final class ModelLeaderArmor extends ModelGearArmor {
    private final ModelPart helmet;
    private final ModelPart mbelt, mbeltL, mbeltR;
    private final ModelPart beltL, beltR, chestplate, chestOrnament, chestClothL, chestClothR, backplate;
    private final ModelPart legClothR, legClothL;
    private final ModelPart collarF, collarB, collarR, collarL;
    private final ModelPart cloak1, cloak2, cloak3, cloakTL, cloakTR;
    private final ModelPart shoulderR, shoulderR1, shoulderR2, shoulderR5, shoulderR3, shoulderR4, gauntletR, gauntletR2, gauntletstrapR1, gauntletstrapR2;
    private final ModelPart shoulderL, shoulderL1, shoulderL2, shoulderL3, shoulderL5, shoulderL4, gauntletL, gauntletL2, gauntletstrapL1, gauntletstrapL2;
    private final ModelPart backpanelR1, backpanelR2, backpanelR3, backpanelR4;
    private final ModelPart backpanelL1, backpanelL2, backpanelL3, backpanelL4;

    /** The original full model, {@code new ModelLeaderArmor(1.0f)}. */
    public ModelLeaderArmor(ModelPart root) {
        this(root, true);
    }

    /**
     * @param armoured {@code true} for the original {@code new ModelLeaderArmor(1.0f)},
     *                 {@code false} for the original {@code new ModelLeaderArmor(0.5f)}.
     */
    public ModelLeaderArmor(ModelPart root, boolean armoured) {
        super(root);
        var head = root.getChild("head");
        var body = root.getChild("body");
        var rightArm = root.getChild("right_arm");
        var leftArm = root.getChild("left_arm");
        var rightLeg = root.getChild("right_leg");
        var leftLeg = root.getChild("left_leg");
        helmet = head.getChild("helmet");
        mbelt = body.getChild("mbelt");
        mbeltL = body.getChild("mbelt_l");
        mbeltR = body.getChild("mbelt_r");
        beltL = child(body, "belt_l");
        beltR = child(body, "belt_r");
        chestplate = child(body, "chestplate");
        chestOrnament = child(body, "chest_ornament");
        chestClothL = child(body, "chest_cloth_l");
        chestClothR = child(body, "chest_cloth_r");
        backplate = child(body, "backplate");
        legClothR = child(body, "leg_cloth_r");
        legClothL = child(body, "leg_cloth_l");
        collarF = child(body, "collar_f");
        collarB = child(body, "collar_b");
        collarR = child(body, "collar_r");
        collarL = child(body, "collar_l");
        cloak1 = child(body, "cloak1");
        cloak2 = child(body, "cloak2");
        cloak3 = child(body, "cloak3");
        cloakTL = child(body, "cloak_tl");
        cloakTR = child(body, "cloak_tr");
        shoulderR = rightArm.getChild("shoulder_r");
        shoulderR1 = rightArm.getChild("shoulder_r1");
        shoulderR2 = rightArm.getChild("shoulder_r2");
        shoulderR5 = rightArm.getChild("shoulder_r5");
        shoulderR3 = rightArm.getChild("shoulder_r3");
        shoulderR4 = rightArm.getChild("shoulder_r4");
        gauntletR = rightArm.getChild("gauntlet_r");
        gauntletR2 = rightArm.getChild("gauntlet_r2");
        gauntletstrapR1 = rightArm.getChild("gauntletstrap_r1");
        gauntletstrapR2 = rightArm.getChild("gauntletstrap_r2");
        shoulderL = leftArm.getChild("shoulder_l");
        shoulderL1 = leftArm.getChild("shoulder_l1");
        shoulderL2 = leftArm.getChild("shoulder_l2");
        shoulderL3 = leftArm.getChild("shoulder_l3");
        shoulderL5 = leftArm.getChild("shoulder_l5");
        shoulderL4 = leftArm.getChild("shoulder_l4");
        gauntletL = leftArm.getChild("gauntlet_l");
        gauntletL2 = leftArm.getChild("gauntlet_l2");
        gauntletstrapL1 = leftArm.getChild("gauntletstrap_l1");
        gauntletstrapL2 = leftArm.getChild("gauntletstrap_l2");
        backpanelR1 = rightLeg.getChild("backpanel_r1");
        backpanelR2 = rightLeg.getChild("backpanel_r2");
        backpanelR3 = rightLeg.getChild("backpanel_r3");
        backpanelR4 = rightLeg.getChild("backpanel_r4");
        backpanelL1 = leftLeg.getChild("backpanel_l1");
        backpanelL2 = leftLeg.getChild("backpanel_l2");
        backpanelL3 = leftLeg.getChild("backpanel_l3");
        backpanelL4 = leftLeg.getChild("backpanel_l4");
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
            body.addOrReplaceChild("belt_l", CubeListBuilder.create().texOffs(76, 44).addBox(4, 4, -3, 1, 3, 6), PartPose.ZERO);
            body.addOrReplaceChild("belt_r", CubeListBuilder.create().texOffs(76, 44).addBox(-5, 4, -3, 1, 3, 6), PartPose.ZERO);
            body.addOrReplaceChild("chestplate", CubeListBuilder.create().texOffs(56, 45).addBox(-4, 1, -3.8F, 8, 7, 2), PartPose.ZERO);
            body.addOrReplaceChild("chest_ornament", CubeListBuilder.create().texOffs(76, 53).addBox(-2.5F, 3, -4.8F, 5, 5, 1), PartPose.ZERO);
            body.addOrReplaceChild("chest_cloth_l", CubeListBuilder.create().texOffs(20, 47).mirror().addBox(1.5F, 1.2F, -4.5F, 3, 9, 1), PartPose.offsetAndRotation(0, 0, 0, .0663225F, 0, 0));
            body.addOrReplaceChild("chest_cloth_r", CubeListBuilder.create().texOffs(20, 47).addBox(-4.5F, 1.2F, -4.5F, 3, 9, 1), PartPose.offsetAndRotation(0, 0, 0, .0663225F, 0, 0));
            body.addOrReplaceChild("leg_cloth_r", CubeListBuilder.create().texOffs(20, 55).addBox(0, 0, 0, 3, 8, 1), PartPose.offsetAndRotation(-4.5F, 10.4F, -3.9F, -.0349066F, 0, 0));
            body.addOrReplaceChild("leg_cloth_l", CubeListBuilder.create().texOffs(20, 55).mirror().addBox(0, 0, 0, 3, 8, 1), PartPose.offsetAndRotation(1.5F, 10.4F, -3.9F, -.0349066F, 0, 0));
            body.addOrReplaceChild("backplate", CubeListBuilder.create().texOffs(36, 45).addBox(-4, 1, 2, 8, 11, 2), PartPose.ZERO);
            body.addOrReplaceChild("collar_b", CubeListBuilder.create().texOffs(17, 26).addBox(-4.5F, -1.5F, 7, 9, 4, 1), PartPose.offsetAndRotation(0, 0, -2.5F, .2268928F, 0, 0));
            body.addOrReplaceChild("collar_r", CubeListBuilder.create().texOffs(17, 11).addBox(-5.5F, -1.5F, -3, 1, 4, 11), PartPose.offsetAndRotation(0, 0, -2.5F, .2268928F, 0, 0));
            body.addOrReplaceChild("collar_l", CubeListBuilder.create().texOffs(17, 11).addBox(4.5F, -1.5F, -3, 1, 4, 11), PartPose.offsetAndRotation(0, 0, -2.5F, .2268928F, 0, 0));
            body.addOrReplaceChild("collar_f", CubeListBuilder.create().texOffs(17, 31).addBox(-4.5F, -1.5F, -3, 9, 4, 1), PartPose.offsetAndRotation(0, 0, -2.5F, .2268928F, 0, 0));
            body.addOrReplaceChild("cloak1", CubeListBuilder.create().texOffs(0, 47).addBox(-4.5F, 2, 1, 9, 12, 1), PartPose.offsetAndRotation(0, 0, 3, .1396263F, 0, 0));
            body.addOrReplaceChild("cloak2", CubeListBuilder.create().texOffs(0, 59).addBox(-4.5F, 14, -1.3F, 9, 4, 1), PartPose.offsetAndRotation(0, 0, 3, .3069452F, 0, 0));
            body.addOrReplaceChild("cloak3", CubeListBuilder.create().texOffs(0, 59).addBox(-4.5F, 17, -3.7F, 9, 4, 1), PartPose.offsetAndRotation(0, 0, 3, .4465716F, 0, 0));
            body.addOrReplaceChild("cloak_tl", CubeListBuilder.create().texOffs(0, 43).addBox(2.5F, 1, -1, 2, 1, 3), PartPose.offsetAndRotation(0, 0, 3, .1396263F, 0, 0));
            body.addOrReplaceChild("cloak_tr", CubeListBuilder.create().texOffs(0, 43).addBox(-4.5F, 1, -1, 2, 1, 3), PartPose.offsetAndRotation(0, 0, 3, .1396263F, 0, 0));
        }
        rightArm.addOrReplaceChild("shoulder_r", CubeListBuilder.create().texOffs(56, 35).addBox(-3.5F, -2.5F, -2.5F, 5, 5, 5), PartPose.ZERO);
        rightArm.addOrReplaceChild("shoulder_r1", CubeListBuilder.create().texOffs(0, 0).addBox(-4.3F, -1.5F, -3, 3, 5, 6), PartPose.offsetAndRotation(0, 0, 0, 0, 0, .7853982F));
        rightArm.addOrReplaceChild("shoulder_r2", CubeListBuilder.create().texOffs(0, 19).addBox(-3.3F, 3.5F, -2.5F, 1, 1, 5), PartPose.offsetAndRotation(0, 0, 0, 0, 0, .7853982F));
        rightArm.addOrReplaceChild("shoulder_r5", CubeListBuilder.create().texOffs(18, 4).addBox(-2.3F, -1.5F, 3, 1, 6, 1), PartPose.offsetAndRotation(0, 0, 0, 0, 0, .7853982F));
        rightArm.addOrReplaceChild("shoulder_r3", CubeListBuilder.create().texOffs(0, 11).addBox(-2.3F, 3.5F, -3, 1, 2, 6), PartPose.offsetAndRotation(0, 0, 0, 0, 0, .7853982F));
        rightArm.addOrReplaceChild("shoulder_r4", CubeListBuilder.create().texOffs(18, 4).addBox(-2.3F, -1.5F, -4, 1, 6, 1), PartPose.offsetAndRotation(0, 0, 0, 0, 0, .7853982F));
        rightArm.addOrReplaceChild("gauntlet_r", CubeListBuilder.create().texOffs(100, 26).addBox(-3.5F, 3.5F, -2.5F, 2, 6, 5), PartPose.ZERO);
        rightArm.addOrReplaceChild("gauntlet_r2", CubeListBuilder.create().texOffs(102, 37).addBox(-5, 3.5F, -2, 1, 5, 4), PartPose.offsetAndRotation(0, 0, 0, 0, 0, -.1675516F));
        rightArm.addOrReplaceChild("gauntletstrap_r1", CubeListBuilder.create().texOffs(84, 31).mirror().addBox(-1.5F, 3.5F, -2.5F, 3, 1, 5), PartPose.ZERO);
        rightArm.addOrReplaceChild("gauntletstrap_r2", CubeListBuilder.create().texOffs(84, 31).mirror().addBox(-1.5F, 6.5F, -2.5F, 3, 1, 5), PartPose.ZERO);
        leftArm.addOrReplaceChild("shoulder_l", CubeListBuilder.create().texOffs(56, 35).addBox(-1.5F, -2.5F, -2.5F, 5, 5, 5), PartPose.ZERO);
        leftArm.addOrReplaceChild("shoulder_l1", CubeListBuilder.create().texOffs(0, 0).addBox(1.3F, -1.5F, -3, 3, 5, 6), PartPose.offsetAndRotation(0, 0, 0, 0, 0, -.7853982F));
        leftArm.addOrReplaceChild("shoulder_l2", CubeListBuilder.create().texOffs(0, 19).mirror().addBox(2.3F, 3.5F, -2.5F, 1, 1, 5), PartPose.offsetAndRotation(0, 0, 0, 0, 0, -.7853982F));
        leftArm.addOrReplaceChild("shoulder_l3", CubeListBuilder.create().texOffs(0, 11).addBox(1.3F, 3.5F, -3, 1, 2, 6), PartPose.offsetAndRotation(0, 0, 0, 0, 0, -.7853982F));
        leftArm.addOrReplaceChild("shoulder_l5", CubeListBuilder.create().texOffs(18, 4).addBox(1.3F, -1.5F, 3, 1, 6, 1), PartPose.offsetAndRotation(0, 0, 0, 0, 0, -.7853982F));
        leftArm.addOrReplaceChild("shoulder_l4", CubeListBuilder.create().texOffs(18, 4).addBox(1.3F, -1.5F, -4, 1, 6, 1), PartPose.offsetAndRotation(0, 0, 0, 0, 0, -.7853982F));
        leftArm.addOrReplaceChild("gauntlet_l", CubeListBuilder.create().texOffs(114, 26).addBox(1.5F, 3.5F, -2.5F, 2, 6, 5), PartPose.ZERO);
        leftArm.addOrReplaceChild("gauntlet_l2", CubeListBuilder.create().texOffs(102, 37).addBox(4, 3.5F, -2, 1, 5, 4), PartPose.offsetAndRotation(0, 0, 0, 0, 0, .1675516F));
        leftArm.addOrReplaceChild("gauntletstrap_l1", CubeListBuilder.create().texOffs(84, 31).mirror().addBox(-1.5F, 3.5F, -2.5F, 3, 1, 5), PartPose.ZERO);
        leftArm.addOrReplaceChild("gauntletstrap_l2", CubeListBuilder.create().texOffs(84, 31).mirror().addBox(-1.5F, 6.5F, -2.5F, 3, 1, 5), PartPose.ZERO);
        rightLeg.addOrReplaceChild("backpanel_r1", CubeListBuilder.create().texOffs(0, 25).addBox(-3, -.5F, 2.5F, 5, 7, 1), PartPose.offsetAndRotation(0, 0, 0, .0698132F, 0, 0));
        rightLeg.addOrReplaceChild("backpanel_r2", CubeListBuilder.create().texOffs(96, 14).addBox(-3, -.5F, -2.5F, 5, 3, 5), PartPose.offsetAndRotation(0, 0, 0, 0, 0, .1396263F));
        rightLeg.addOrReplaceChild("backpanel_r3", CubeListBuilder.create().texOffs(116, 13).addBox(-3, 2.5F, -2.5F, 1, 4, 5), PartPose.offsetAndRotation(0, 0, 0, 0, 0, .1396263F));
        rightLeg.addOrReplaceChild("backpanel_r4", CubeListBuilder.create().texOffs(0, 25).mirror().addBox(-3, -.5F, -3.5F, 5, 7, 1), PartPose.offsetAndRotation(0, 0, 0, -.0349066F, 0, 0));
        leftLeg.addOrReplaceChild("backpanel_l1", CubeListBuilder.create().texOffs(0, 25).addBox(-2, -.5F, 2.5F, 5, 7, 1), PartPose.offsetAndRotation(0, 0, 0, .0698132F, 0, 0));
        leftLeg.addOrReplaceChild("backpanel_l2", CubeListBuilder.create().texOffs(96, 14).addBox(-2, -.5F, -2.5F, 5, 3, 5), PartPose.offsetAndRotation(0, 0, 0, 0, 0, -.1396263F));
        leftLeg.addOrReplaceChild("backpanel_l3", CubeListBuilder.create().texOffs(116, 13).addBox(2, 2.5F, -2.5F, 1, 4, 5), PartPose.offsetAndRotation(0, 0, 0, 0, 0, -.1396263F));
        leftLeg.addOrReplaceChild("backpanel_l4", CubeListBuilder.create().texOffs(0, 25).mirror().addBox(-2, -.5F, -3.5F, 5, 7, 1), PartPose.offsetAndRotation(0, 0, 0, -.0349066F, 0, 0));
        return layer(mesh);
    }

    /**
     * Original ModelLeaderArmor301-309: the leg cloths and the three cloak
     * segments swing with the walk cycle.
     */
    @Override public void setupAnim(LivingEntity entity, float limbSwing, float limbSwingAmount, float ageInTicks, float headYaw, float headPitch) {
        super.setupAnim(entity, limbSwing, limbSwingAmount, ageInTicks, headYaw, headPitch);
        if (legClothR == null) return;
        float a = (float) Math.cos(limbSwing * 0.6662F) * 1.4F * limbSwingAmount;
        float b = (float) Math.cos(limbSwing * 0.6662F + Math.PI) * 1.4F * limbSwingAmount;
        float c = Math.min(a, b);
        legClothR.xRot = a - 0.1047198F;
        legClothL.xRot = b - 0.1047198F;
        cloak1.xRot = -c / 2 + 0.1396263F;
        cloak2.xRot = -c / 2 + 0.3069452F;
        cloak3.xRot = -c / 2 + 0.4465716F;
    }

    /** The original ModelLeaderArmor347-363 for a skeleton or a zombie wearer. */
    public void setZombiePose(LivingEntity entity, float limbSwing, float limbSwingAmount, float ageInTicks) {
        zombieArms(attackTime, ageInTicks);
    }
}
