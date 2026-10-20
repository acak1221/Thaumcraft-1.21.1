package thaumcraft.client.renderers.models.entities;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import thaumcraft.common.entities.monster.EntityWatcher;

/**
 * Original TC4 models/entities/ModelWatcher: one five-box body, twelve spines, one eye
 * and a three-part tail on a 64x64 sheet. The original {@code func_178706_a()} always
 * returned 54, so the original renderer's "rebuild the model" branch never fired and is
 * not reproduced here.
 */
public final class ModelWatcher extends EntityModel<EntityWatcher> {
    private static final int SPINES = 12;
    private final ModelPart root;
    private final ModelPart body;
    private final ModelPart[] spines = new ModelPart[SPINES];
    private final ModelPart eye;
    private final ModelPart tail0, tail1, tail2;

    public ModelWatcher(ModelPart root) {
        this.root = root;
        body = root.getChild("body");
        for (int i = 0; i < SPINES; i++) spines[i] = body.getChild("spine" + i);
        eye = body.getChild("eye");
        tail0 = body.getChild("tail0");
        tail1 = tail0.getChild("tail1");
        tail2 = tail1.getChild("tail2");
    }

    /** Original ModelWatcher:33-79. Texture 64x64. */
    public static LayerDefinition createBodyLayer() {
        var mesh = new MeshDefinition();
        var root = mesh.getRoot();
        var body = root.addOrReplaceChild("body", CubeListBuilder.create().texOffs(0, 0)
                .addBox(-6F, 10F, -8F, 12, 12, 16)
                .texOffs(0, 28).mirror().addBox(-8F, 10F, -6F, 2, 12, 12)
                .texOffs(0, 28).addBox(6F, 10F, -6F, 2, 12, 12)
                .texOffs(16, 40).addBox(-6F, 8F, -6F, 12, 2, 12)
                .texOffs(16, 40).addBox(-6F, 22F, -6F, 12, 2, 12), PartPose.ZERO);
        for (int i = 0; i < SPINES; i++) {
            body.addOrReplaceChild("spine" + i, CubeListBuilder.create().texOffs(0, 0)
                    .addBox(-1F, -4.5F, -1F, 2, 9, 2), PartPose.ZERO);
        }
        body.addOrReplaceChild("eye", CubeListBuilder.create().texOffs(8, 0)
                .addBox(-1F, 15F, 0F, 2, 2, 1), PartPose.ZERO);
        var tail0 = body.addOrReplaceChild("tail0", CubeListBuilder.create().texOffs(40, 0)
                .addBox(-2F, 14F, 7F, 4, 4, 8), PartPose.ZERO);
        var tail1 = tail0.addOrReplaceChild("tail1", CubeListBuilder.create().texOffs(0, 54)
                .addBox(0F, 14F, 0F, 3, 3, 7), PartPose.ZERO);
        tail1.addOrReplaceChild("tail2", CubeListBuilder.create().texOffs(41, 32)
                .addBox(0F, 14F, 0F, 2, 2, 6)
                .texOffs(25, 19).addBox(1F, 10.5F, 3F, 1, 9, 9), PartPose.ZERO);
        return LayerDefinition.create(mesh, 64, 64);
    }

    /** Original func_78087_a:90-140 plus the original getPositionEyes:143-151. */
    @Override public void setupAnim(EntityWatcher watcher, float limb, float amount, float age, float yaw, float pitch) {
        float partial = age - watcher.tickCount;
        body.yRot = yaw / 57.295776F;
        body.xRot = pitch / 57.295776F;
        float[] rotX = {1.75F, .25F, 0, 0, .5F, .5F, .5F, .5F, 1.25F, .75F, 0, 0};
        float[] rotY = {0, 0, 0, 0, .25F, 1.75F, 1.25F, .75F, 0, 0, 0, 0};
        float[] rotZ = {0, 0, .25F, 1.75F, 0, 0, 0, 0, 0, 0, .75F, 1.25F};
        float[] offX = {0, 0, 8, -8, -8, 8, 8, -8, 0, 0, 8, -8};
        float[] offY = {-8, -8, -8, -8, 0, 0, 0, 0, 8, 8, 8, 8};
        float[] offZ = {8, -8, 0, 0, -8, -8, 8, 8, 8, -8, 0, 0};
        float extension = (1.0F - watcher.getSpikeExtension(partial)) * .55F;
        for (int i = 0; i < SPINES; i++) {
            ModelPart spine = spines[i];
            spine.xRot = (float) Math.PI * rotX[i];
            spine.yRot = (float) Math.PI * rotY[i];
            spine.zRot = (float) Math.PI * rotZ[i];
            float wobble = 1.0F + Mth.sin(age * 1.5F + i) * .01F - extension;
            spine.setPos(offX[i] * wobble, 16.0F + offY[i] * wobble, offZ[i] * wobble);
        }
        // Original field_78800_c/field_78797_d/field_78898_e are the eye's X/Y/Z offsets.
        eye.setPos(eye.x, eye.y, -8.25F);
        LivingEntity look = Minecraft.getInstance().player;
        if (watcher.isTargeting()) look = watcher.getTargetedEntity();
        if (look != null) {
            Vec3 target = look.getEyePosition(0F);
            Vec3 self = watcher.getEyePosition(0F);
            eye.setPos(eye.x, target.y - self.y > 0 ? 0 : 1, eye.z);
            Vec3 lookVec = watcher.getLookAngle();
            double lx = lookVec.x, lz = lookVec.z;
            double dx = self.x - target.x, dz = self.z - target.z;
            double length = Math.sqrt(dx * dx + dz * dz);
            if (length > 1.0E-7) {
                // Original vec33.rotateAroundX(PI/2) on the flattened direction.
                double nx = dx / length, nz = -dz / length;
                double dot = lx * nx + lz * nz;
                eye.x = (float) (Math.acos(Math.abs(dot)) * 2.0 * Math.signum(dot));
            }
        }
        eye.visible = true;
        float swing = watcher.getTailSwing(partial);
        tail0.yRot = Mth.sin(swing) * (float) Math.PI * .05F;
        tail1.yRot = Mth.sin(swing) * (float) Math.PI * .1F;
        tail1.setPos(-1.5F, .5F, 14.0F);
        tail2.yRot = Mth.sin(swing) * (float) Math.PI * .15F;
        tail2.setPos(.5F, .5F, 6.0F);
    }

    /** The original eye position helper; native Entity.getEyePosition(float) is the same value. */
    public static Vec3 eyePosition(Entity entity, float partial) {
        return entity.getEyePosition(partial);
    }

    @Override public void renderToBuffer(PoseStack pose, VertexConsumer out, int light, int overlay, int color) {
        root.render(pose, out, light, overlay, color);
    }
}
