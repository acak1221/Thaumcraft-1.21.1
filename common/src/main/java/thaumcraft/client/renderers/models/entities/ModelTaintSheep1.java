package thaumcraft.client.renderers.models.entities;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.util.Mth;
import thaumcraft.common.entities.monster.EntityTaintSheep;

/**
 * Original TC4 models/entities/ModelTaintSheep1: the wool overlay of the taint sheep,
 * identical to the vanilla 1.7 ModelSheep1 geometry. The original 1.7 inflation argument
 * of addBox is the native CubeDeformation, so the original 0.6/1.75/0.5 sizes are kept.
 */
public final class ModelTaintSheep1 extends EntityModel<EntityTaintSheep> {
    private final ModelPart root;
    private final ModelPart head, body, rightFrontLeg, leftFrontLeg, rightHindLeg, leftHindLeg;
    private float headRotationAngleX;

    public ModelTaintSheep1(ModelPart root) {
        this.root = root;
        head = root.getChild("head");
        body = root.getChild("body");
        rightFrontLeg = root.getChild("right_front_leg");
        leftFrontLeg = root.getChild("left_front_leg");
        rightHindLeg = root.getChild("right_hind_leg");
        leftHindLeg = root.getChild("left_hind_leg");
    }

    /** Original ModelTaintSheep1:8-27. Texture 64x64. */
    public static LayerDefinition createBodyLayer() {
        var mesh = new MeshDefinition();
        var root = mesh.getRoot();
        root.addOrReplaceChild("head", CubeListBuilder.create().texOffs(0, 0)
                .addBox(-3F, -4F, -4F, 6, 6, 6, new CubeDeformation(.6F)), PartPose.offset(0, 6, -8));
        root.addOrReplaceChild("body", CubeListBuilder.create().texOffs(28, 8)
                .addBox(-4F, -10F, -7F, 8, 16, 6, new CubeDeformation(1.75F)), PartPose.offset(0, 5, 2));
        root.addOrReplaceChild("right_front_leg", CubeListBuilder.create().texOffs(0, 16)
                .addBox(-2F, 0F, -2F, 4, 6, 4, new CubeDeformation(.5F)), PartPose.offset(-3, 12, 7));
        root.addOrReplaceChild("left_front_leg", CubeListBuilder.create().texOffs(0, 16)
                .addBox(-2F, 0F, -2F, 4, 6, 4, new CubeDeformation(.5F)), PartPose.offset(3, 12, 7));
        root.addOrReplaceChild("right_hind_leg", CubeListBuilder.create().texOffs(0, 16)
                .addBox(-2F, 0F, -2F, 4, 6, 4, new CubeDeformation(.5F)), PartPose.offset(-3, 12, -5));
        root.addOrReplaceChild("left_hind_leg", CubeListBuilder.create().texOffs(0, 16)
                .addBox(-2F, 0F, -2F, 4, 6, 4, new CubeDeformation(.5F)), PartPose.offset(3, 12, -5));
        return LayerDefinition.create(mesh, 64, 64);
    }

    /** Original func_78086_a:29-35: the TC4 head bob and head angle from the entity. */
    @Override public void prepareMobModel(EntityTaintSheep sheep, float limb, float amount, float partialTick) {
        head.setPos(head.x, 6.0F + sheep.getHeadRotationPointY(partialTick) * 9.0F, head.z);
        headRotationAngleX = sheep.getHeadRotationAngleX(partialTick);
    }

    /** Original func_78087_a:37-40 plus the 1.7 ModelQuadruped leg swing. */
    @Override public void setupAnim(EntityTaintSheep sheep, float limb, float amount, float age, float yaw, float pitch) {
        head.yRot = yaw / 57.295776F;
        head.xRot = headRotationAngleX;
        rightFrontLeg.xRot = Mth.cos(limb * .6662F) * 1.4F * amount;
        leftFrontLeg.xRot = Mth.cos(limb * .6662F + (float) Math.PI) * 1.4F * amount;
        rightHindLeg.xRot = Mth.cos(limb * .6662F + (float) Math.PI) * 1.4F * amount;
        leftHindLeg.xRot = Mth.cos(limb * .6662F) * 1.4F * amount;
    }

    @Override public void renderToBuffer(PoseStack pose, VertexConsumer out, int light, int overlay, int color) {
        root.render(pose, out, light, overlay, color);
    }
}
