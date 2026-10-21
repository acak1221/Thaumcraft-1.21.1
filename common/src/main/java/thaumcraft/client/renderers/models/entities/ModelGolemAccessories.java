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
import thaumcraft.common.entities.golems.EntityGolemBase;

/**
 * Original TC4 models/entities/ModelGolemAccessories: the fourteen worn golem pieces
 * (fez, hat, hat rim, glasses, visor, bow tie, plate, dart gun, mace, head jar, head
 * brain and the evil head) on the golem's own 128x128 sheet.
 * The original renderer owns the 0.4 model scale, so this model keeps the original part
 * offsets and applies that scale itself.
 * The original RenderGolemBase picks the visible pieces from the golem decoration string;
 * the native selection is reproduced here through the part visibility flags.
 */
public final class ModelGolemAccessories extends EntityModel<EntityGolemBase> {
    private final ModelPart root;
    private final ModelPart headFez, plate, plateLeft, plateRight, headHat, headGlasses, visor;
    private final ModelPart headHatRim, dartgun, mace, bowtie, headJar, headBrain, evilHead;
    /** The original head offsets use the golem's head height, thirty units in the original renderer. */
    private final float headOffset;
    /** The decoration flags of the last {@link #setupAnim}, applied per render pass. */
    private boolean hasDartgun, hasFez, hasHat, hasBowtie, hasPlate, hasGlasses, hasVisor, hasMace, advanced, coreAtLeastZero;

    public ModelGolemAccessories(ModelPart root, float headOffset) {
        this.root = root;
        this.headOffset = headOffset;
        headFez = root.getChild("head_fez");
        plate = root.getChild("plate");
        plateLeft = root.getChild("plate_left");
        plateRight = root.getChild("plate_right");
        headHat = root.getChild("head_hat");
        headGlasses = root.getChild("head_glasses");
        visor = root.getChild("visor");
        headHatRim = root.getChild("head_hat_rim");
        dartgun = root.getChild("dartgun");
        mace = root.getChild("mace");
        bowtie = root.getChild("bowtie");
        headJar = root.getChild("head_jar");
        headBrain = root.getChild("head_brain");
        evilHead = root.getChild("evil_head");
    }

    /** Original ModelGolemAccessories:45-97. Texture 128x128. */
    public static LayerDefinition createBodyLayer(float headOffset) {
        var mesh = new MeshDefinition();
        var root = mesh.getRoot();
        root.addOrReplaceChild("head_fez", CubeListBuilder.create().texOffs(0, 94)
                .addBox(-4.5F, -15F, -6F, 9, 7, 9), PartPose.offset(0, headOffset, -2));
        root.addOrReplaceChild("plate", CubeListBuilder.create().texOffs(32, 40)
                .addBox(-6.5F, -1F, -7F, 13, 12, 13), PartPose.offset(0, headOffset, 0));
        root.addOrReplaceChild("plate_left", CubeListBuilder.create().texOffs(0, 44)
                .addBox(-8.5F, -4F, -6.5F, 3, 6, 12), PartPose.offset(0, headOffset, 0));
        root.addOrReplaceChild("plate_right", CubeListBuilder.create().texOffs(0, 44).mirror()
                .addBox(5.5F, -4F, -6.5F, 3, 6, 12), PartPose.offset(0, headOffset, 0));
        root.addOrReplaceChild("head_hat", CubeListBuilder.create().texOffs(0, 110)
                .addBox(-4.5F, -17F, -6F, 9, 9, 9), PartPose.offset(0, headOffset, -2));
        root.addOrReplaceChild("head_glasses", CubeListBuilder.create().texOffs(0, 80)
                .addBox(-4.5F, -8F, -6F, 9, 4, 9), PartPose.offset(0, headOffset, -2));
        root.addOrReplaceChild("visor", CubeListBuilder.create().texOffs(0, 70)
                .addBox(-5F, -8F, -6F, 10, 5, 5), PartPose.offset(0, headOffset, -2));
        root.addOrReplaceChild("head_hat_rim", CubeListBuilder.create().texOffs(36, 114)
                .addBox(-6.5F, -9F, -8F, 13, 1, 13), PartPose.offset(0, headOffset, -2));
        root.addOrReplaceChild("dartgun", CubeListBuilder.create().texOffs(80, 80)
                .addBox(7.9F, 7.5F, -3.5F, 6, 16, 7), PartPose.offset(0, headOffset, 0));
        root.addOrReplaceChild("mace", CubeListBuilder.create().texOffs(80, 26)
                .addBox(-13F, 15F, -5F, 6, 8, 10), PartPose.offset(0, headOffset, 0));
        root.addOrReplaceChild("bowtie", CubeListBuilder.create().texOffs(0, 0)
                .addBox(-8.5F, -2F, -6.5F, 17, 4, 12), PartPose.offset(0, headOffset, 0));
        root.addOrReplaceChild("head_jar", CubeListBuilder.create().texOffs(96, 56)
                .addBox(-4F, -15F, -5.5F, 8, 4, 8), PartPose.offset(0, headOffset, -2));
        root.addOrReplaceChild("head_brain", CubeListBuilder.create().texOffs(96, 70)
                .addBox(-3.5F, -14F, -5F, 7, 3, 7), PartPose.offset(0, headOffset, -2));
        root.addOrReplaceChild("evil_head", CubeListBuilder.create().texOffs(64, 65)
                .addBox(-4F, -9F, -5.5F, 8, 7, 8), PartPose.offset(0, headOffset, -2));
        return LayerDefinition.create(mesh, 128, 128);
    }

    /** Original func_78086_a:169-190: the dart gun and mace swing. */
    @Override public void prepareMobModel(EntityGolemBase golem, float limb, float amount, float partialTick) {
        int action = golem.getActionTimer();
        if (action > 0) {
            dartgun.xRot = -2.0F + 1.5F * triangleWave(action - partialTick, 10.0F);
            mace.xRot = -2.0F + 1.5F * triangleWave(action - partialTick, 10.0F);
        } else if (golem.getLeftArmTimer() > 0 || golem.getRightArmTimer() > 0) {
            if (golem.getLeftArmTimer() > 0) dartgun.xRot = -2.0F + 1.5F * triangleWave(golem.getLeftArmTimer() - partialTick, 10.0F);
            if (golem.getRightArmTimer() > 0) mace.xRot = -2.0F + 1.5F * triangleWave(golem.getRightArmTimer() - partialTick, 10.0F);
        } else if (!golem.getCarriedItem().isEmpty()) {
            dartgun.xRot = -1.0F;
            mace.xRot = -1.0F;
        } else {
            dartgun.xRot = (-.2F - 1.5F * triangleWave(limb, 13.0F)) * amount;
            mace.xRot = (-.2F + 1.5F * triangleWave(limb, 13.0F)) * amount;
        }
    }

    /** Original func_78087_a:142-167 plus the original decoration selection of func_78088_a. */
    @Override public void setupAnim(EntityGolemBase golem, float limb, float amount, float age, float yaw, float pitch) {
        float headYaw, headPitch;
        if (golem.getGolemCore() == -1 || golem.getBootupTimer() < 0) {
            headYaw = 0;
            headPitch = .57595867F;
        } else if (golem.isFettered()) {
            headYaw = 0;
            headPitch = .57595867F;
        } else if (golem.getBootupTimer() > 0) {
            headYaw = 0;
            headPitch = golem.getBootupTimer() / 57.295776F;
        } else {
            headYaw = yaw / 57.295776F;
            headPitch = pitch / 57.295776F;
        }
        headFez.yRot = headGlasses.yRot = headJar.yRot = headBrain.yRot = evilHead.yRot = visor.yRot
                = headHat.yRot = headHatRim.yRot = headYaw;
        headFez.xRot = headGlasses.xRot = headJar.xRot = headBrain.xRot = evilHead.xRot = visor.xRot
                = headHat.xRot = headHatRim.xRot = headPitch;
        String decoration = golem.getDecoration();
        hasDartgun = decoration != null && decoration.contains("R");
        hasFez = decoration != null && decoration.contains("F");
        hasHat = decoration != null && decoration.contains("H");
        hasBowtie = decoration != null && decoration.contains("B");
        hasPlate = decoration != null && decoration.contains("P");
        hasGlasses = decoration != null && decoration.contains("G");
        hasVisor = decoration != null && decoration.contains("V");
        hasMace = decoration != null && decoration.contains("M");
        advanced = golem.isAdvanced();
        coreAtLeastZero = golem.getGolemCore() >= 0;
        // The original advanced golem nudged the head wear down by one hundredth of a unit.
        float nudge = advanced ? -.01F : 0;
        headFez.setPos(headFez.x, headOffset + nudge, headFez.z);
        headHat.setPos(headHat.x, headOffset + nudge, headHat.z);
        headHatRim.setPos(headHatRim.x, headOffset + nudge, headHatRim.z);
        applyVisibility(false);
    }

    /**
     * The original draws the bow tie and the head jar with blending and every other piece cut out
     * (oracle {@code ModelGolemAccessories.func_78088_a}:98-152). The native renderer keeps both
     * passes, so the pieces of the other pass are hidden while a pass is drawn.
     */
    public void setBlendPass(boolean blended) { applyVisibility(blended); }

    private void applyVisibility(boolean blended) {
        headFez.visible = hasFez && !blended;
        headHat.visible = hasHat && !blended;
        headHatRim.visible = hasHat && !blended;
        bowtie.visible = hasBowtie && blended;
        plate.visible = hasPlate && !blended;
        plateLeft.visible = hasPlate && !blended;
        plateRight.visible = hasPlate && !blended;
        headGlasses.visible = hasGlasses && !blended;
        visor.visible = hasVisor && !blended;
        dartgun.visible = hasDartgun && !blended;
        mace.visible = hasMace && !blended;
        headBrain.visible = advanced && !blended;
        headJar.visible = advanced && blended;
        evilHead.visible = advanced && !blended && coreAtLeastZero;
        // The original evil head was scaled by 1.01 on X and Z to sit outside the head.
        evilHead.xScale = evilHead.zScale = evilHead.visible ? 1.01F : 1.0F;
    }

    /** Original doAbs:191-193; a triangle wave, not a cosine swing. */
    private static float triangleWave(float phase, float period) {
        return (Math.abs(phase % period - period * .5F) - period * .25F) / (period * .25F);
    }

    /** The original accessories share the golem model's 0.4 scale. */
    @Override public void renderToBuffer(PoseStack pose, VertexConsumer out, int light, int overlay, int color) {
        pose.pushPose();
        try {
            pose.scale(.4F, .4F, .4F);
            root.render(pose, out, light, overlay, color);
        } finally { pose.popPose(); }
    }
}
