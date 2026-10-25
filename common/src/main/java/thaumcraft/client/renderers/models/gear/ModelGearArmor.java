package thaumcraft.client.renderers.models.gear;

import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;

/**
 * Shared scaffolding for the original 1.7.10 gear models
 * ({@code thaumcraft/client/renderers/models/gear/ModelRobe.java},
 * {@code ModelFortressArmor.java}, {@code ModelKnightArmor.java} and
 * {@code ModelLeaderArmor.java}).
 *
 * <p>All four extend 1.7.10's {@code ModelBiped} with a 128x64 sheet, EMPTY the
 * six biped cube lists and hang their own parts on the biped pivots. Native
 * 1.21.1 keeps the biped pivots as empty parts and hangs the gear parts on
 * them, so the pivots, the offsets and the UVs stay the original ones.
 *
 * <p>The original shows the head for the helm, the body for the chest and the
 * legs, the arms for the chest and the legs for the greaves; the same mapping
 * is reproduced by {@link #showSlot}.
 */
public abstract class ModelGearArmor extends HumanoidModel<LivingEntity> {
    protected ModelGearArmor(ModelPart root) {
        super(root);
    }

    /**
     * The empty biped pivots of the original ModelBiped: the gear replaces the
     * six cube lists, the pivots themselves stay the vanilla ones.
     */
    protected static PartDefinition biped(PartDefinition root) {
        root.addOrReplaceChild("head", CubeListBuilder.create(), PartPose.ZERO);
        root.addOrReplaceChild("hat", CubeListBuilder.create(), PartPose.ZERO);
        root.addOrReplaceChild("body", CubeListBuilder.create(), PartPose.ZERO);
        root.addOrReplaceChild("right_arm", CubeListBuilder.create(), PartPose.offset(-5, 2, 0));
        root.addOrReplaceChild("left_arm", CubeListBuilder.create(), PartPose.offset(5, 2, 0));
        root.addOrReplaceChild("right_leg", CubeListBuilder.create(), PartPose.offset(-1.9F, 12, 0));
        root.addOrReplaceChild("left_leg", CubeListBuilder.create(), PartPose.offset(1.9F, 12, 0));
        return root;
    }

    /** The original gear sheets are 128x64. */
    public static LayerDefinition layer(MeshDefinition mesh) {
        return LayerDefinition.create(mesh, 128, 64);
    }

    /**
     * A part that only one of the two original variants owns stays absent,
     * exactly as in the original constructor.
     */
    protected static ModelPart child(ModelPart root, String name) {
        return root.hasChild(name) ? root.getChild(name) : null;
    }

    /**
     * Takes the wearer's own humanoid pose, so the gear follows the entity
     * instead of guessing a second animation. The original copies the biped
     * rotations of the player model into the gear model.
     */
    public void copyPose(HumanoidModel<?> context) {
        head.copyFrom(context.head);
        hat.copyFrom(context.hat);
        body.copyFrom(context.body);
        rightArm.copyFrom(context.rightArm);
        leftArm.copyFrom(context.leftArm);
        rightLeg.copyFrom(context.rightLeg);
        leftLeg.copyFrom(context.leftLeg);
    }

    /**
     * Original gear render methods replace the copied arm rotations for zombie and skeleton
     * wearers. Modern vanilla zombie attack angles are different, so copying alone loses that pose.
     */
    public void copyPose(HumanoidModel<?> context, boolean zombieWearer, float ageInTicks) {
        copyPose(context);
        if (zombieWearer) zombieArms(attackTime, ageInTicks);
    }

    /**
     * Original ItemCultistRobeArmor:108-113 and the same rule in the fortress,
     * knight and leader armor: the head shows for the helm, the body for the
     * chest and the legs, the arms for the chest, the legs for the greaves.
     */
    public void showSlot(EquipmentSlot slot) {
        boolean helm = slot == EquipmentSlot.HEAD;
        boolean chest = slot == EquipmentSlot.CHEST;
        boolean legs = slot == EquipmentSlot.LEGS;
        head.visible = helm;
        hat.visible = helm;
        body.visible = chest || legs;
        rightArm.visible = chest;
        leftArm.visible = chest;
        rightLeg.visible = legs;
        leftLeg.visible = legs;
    }

    /**
     * Original setRotationAnglesZombie: the arms of a skeleton or a zombie are
     * raised and swing with the attack progress.
     */
    protected void zombieArms(float attackProgress, float ageInTicks) {
        float swing = (float) Math.sin(attackProgress * Math.PI);
        float release = (float) Math.sin((1 - (1 - attackProgress) * (1 - attackProgress)) * Math.PI);
        rightArm.zRot = 0;
        leftArm.zRot = 0;
        rightArm.yRot = -(0.1F - swing * 0.6F);
        leftArm.yRot = 0.1F - swing * 0.6F;
        rightArm.xRot = -1.5707964F;
        leftArm.xRot = -1.5707964F;
        rightArm.xRot -= swing * 1.2F - release * 0.4F;
        leftArm.xRot -= swing * 1.2F - release * 0.4F;
        rightArm.zRot += (float) Math.cos(ageInTicks * 0.09F) * 0.05F + 0.05F;
        leftArm.zRot -= (float) Math.cos(ageInTicks * 0.09F) * 0.05F + 0.05F;
        rightArm.xRot += (float) Math.sin(ageInTicks * 0.067F) * 0.05F;
        leftArm.xRot -= (float) Math.sin(ageInTicks * 0.067F) * 0.05F;
    }
}
