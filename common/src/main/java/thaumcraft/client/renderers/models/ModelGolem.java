package thaumcraft.client.renderers.models;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.world.entity.LivingEntity;

/** Active TC4 models/entities/ModelGolem(false), with renderer-fed pose state
 * instead of an EntityGolemBase dependency. No accessories, material or AI. */
public final class ModelGolem<T extends LivingEntity> extends EntityModel<T> {
    public final ModelPart golemHead, golemBody, golemRightArm, golemLeftArm, golemRightLeg, golemLeftLeg;
    private boolean active = true;
    private boolean inactive, carrying;
    private float partialTick, bootupTimer, jarCarryScale = 1;
    private int healingTimer, combatTimer, leftArmTimer, rightArmTimer;

    public ModelGolem(ModelPart root) {
        golemHead = root.getChild("head");
        golemBody = root.getChild("body");
        golemRightArm = root.getChild("right_arm");
        golemLeftArm = root.getChild("left_arm");
        golemRightLeg = root.getChild("right_leg");
        golemLeftLeg = root.getChild("left_leg");
    }

    /** Original constructor32–57: seven boxes, normal offset30, 128x128 UVs. */
    public static LayerDefinition createBodyLayer() {
        var mesh = new MeshDefinition();
        var root = mesh.getRoot();
        root.addOrReplaceChild("head", CubeListBuilder.create().texOffs(0, 0)
                .addBox(-4, -11, -5.5F, 8, 9, 8), PartPose.offset(0, 30, -2));
        root.addOrReplaceChild("body", CubeListBuilder.create().texOffs(0, 40)
                .addBox(-8, -2, -6, 16, 12, 11).texOffs(0, 70)
                .addBox(-4.5F, 10, -3, 9, 5, 6, new CubeDeformation(.5F)), PartPose.offset(0, 30, 0));
        root.addOrReplaceChild("right_arm", CubeListBuilder.create().texOffs(60, 21)
                .addBox(-12, -2.5F, -3, 4, 25, 6), PartPose.offset(0, 30, 0));
        root.addOrReplaceChild("left_arm", CubeListBuilder.create().texOffs(60, 21).mirror()
                .addBox(8, -2.5F, -3, 4, 25, 6), PartPose.offset(0, 30, 0));
        root.addOrReplaceChild("right_leg", CubeListBuilder.create().texOffs(37, 0)
                .addBox(-3.5F, -3, -3, 6, 16, 5), PartPose.offset(-4, 48, 0));
        root.addOrReplaceChild("left_leg", CubeListBuilder.create().texOffs(37, 0).mirror()
                .addBox(-3.5F, -3, -3, 6, 16, 5), PartPose.offset(5, 48, 0));
        return LayerDefinition.create(mesh, 128, 128);
    }

    /** Original core != -1; a coreless golem is a fully dormant statue. */
    public void setActive(boolean active) { this.active = active; }

    /** Original non-null carried-display item; Gather uses its carried snapshot. */
    public void setCarrying(boolean carrying) { this.carrying = carrying; }

    /** Original signed bootup value, in degrees when positive; no extra easing. */
    public void setBootupTimer(float bootupTimer) { this.bootupTimer = bootupTimer; }

    /** Original healing countdown; affects the primary skin's RGB, not pose. */
    public void setHealingTimer(int healingTimer) { this.healingTimer = healingTimer; }

    /** Original getActionTimer() countdown, not native normalized attackTime.
     * Gather has no combat goal and supplies zero unless a real action exists. */
    public void setCombatTimer(int combatTimer) { this.combatTimer = combatTimer; }

    /** Original independent raw five-tick arm countdowns, not folded ACT. */
    public void setArmTimers(int left, int right) {
        leftArmTimer = Math.max(0, left);
        rightArmTimer = Math.max(0, right);
    }

    /** Original core6 arm splay follows material-sized jar, not its fluid amount. */
    public void setJarCarryScale(float scale) { jarCarryScale = Math.max(.5F, Math.min(1, scale)); }

    /** Optional original menu-pause state: head down, limbs still animated.
     * This is distinct from lacking a core; ordinary active rendering uses false. */
    public void setInactive(boolean inactive) { this.inactive = inactive; }

    @Override public void prepareMobModel(T entity, float walkPhase, float walkSpeed, float partialTick) {
        super.prepareMobModel(entity, walkPhase, walkSpeed, partialTick);
        this.partialTick = partialTick;
    }

    @Override public void setupAnim(T entity, float walkPhase, float walkSpeed, float ageInTicks, float headYaw, float headPitch) {
        golemRightLeg.yRot = golemLeftLeg.yRot = 0;
        golemLeftArm.zRot = golemRightArm.zRot = 0;
        if (!active || bootupTimer < 0) {
            golemHead.yRot = 0;
            golemHead.xRot = .57595867F;
            golemRightLeg.xRot = golemLeftLeg.xRot = 0;
            golemRightArm.xRot = golemLeftArm.xRot = 0;
            return;
        }
        float splay = (1 - jarCarryScale) * 25 / 57.295776F;
        golemLeftArm.zRot = splay;
        golemRightArm.zRot = -splay;

        if (inactive) {
            golemHead.yRot = 0;
            golemHead.xRot = .57595867F;
        } else if (bootupTimer > 0) {
            golemHead.yRot = 0;
            golemHead.xRot = bootupTimer / 57.295776F;
        } else {
            golemHead.yRot = headYaw / 57.295776F;
            golemHead.xRot = headPitch / 57.295776F;
        }
        float walk = triangleWave(walkPhase, 13.0F);
        golemRightLeg.xRot = -1.5F * walk * walkSpeed;
        golemLeftLeg.xRot = 1.5F * walk * walkSpeed;

        // Reset BOTH arms for every rendered entity/frame before overlays.
        // Original M:151–157 left the untimed arm stale during one-arm actions.
        if (carrying) {
            golemRightArm.xRot = golemLeftArm.xRot = -1.0F;
        } else {
            golemRightArm.xRot = (-.2F + 1.5F * walk) * walkSpeed;
            golemLeftArm.xRot = (-.2F - 1.5F * walk) * walkSpeed;
        }
        if (leftArmTimer > 0) golemLeftArm.xRot = -2.0F + 1.5F * triangleWave(leftArmTimer - partialTick, 20.0F);
        if (rightArmTimer > 0) golemRightArm.xRot = -2.0F + 1.5F * triangleWave(rightArmTimer - partialTick, 20.0F);
        if (combatTimer > 0) {
            golemRightArm.xRot = -2.0F + 1.5F * triangleWave(combatTimer - partialTick, 5.0F);
            golemLeftArm.xRot = golemRightArm.xRot;
        }
    }

    /** Original helper167–168; this is a triangle wave, not a cosine walk. */
    private static float triangleWave(float phase, float period) {
        return (Math.abs(phase % period - period * .5F) - period * .25F) / (period * .25F);
    }

    @Override public void renderToBuffer(PoseStack pose, VertexConsumer vertices, int light, int overlay, int color) {
        if (healingTimer > 0) {
            // Original91–94, primary pass only. GL clamps these components to
            // [0,1]; encode the same tint locally without leaking global color.
            float redBlue = Math.min(1.0F, .5F + healingTimer / 10.0F);
            float green = Math.min(1.0F, .9F + healingTimer / 5.0F);
            color = (color & 0xff000000)
                    | ((int) ((color >>> 16 & 255) * redBlue) << 16)
                    | ((int) ((color >>> 8 & 255) * green) << 8)
                    | (int) ((color & 255) * redBlue);
        }
        pose.pushPose();
        try {
            // Original render60–80 owns this scale; the entity renderer must
            // not apply a second 0.4 scale to the normal model.
            pose.scale(.4F, .4F, .4F);
            golemHead.render(pose, vertices, light, overlay, color);
            golemBody.render(pose, vertices, light, overlay, color);
            golemRightLeg.render(pose, vertices, light, overlay, color);
            golemLeftLeg.render(pose, vertices, light, overlay, color);
            golemRightArm.render(pose, vertices, light, overlay, color);
            golemLeftArm.render(pose, vertices, light, overlay, color);
        } finally { pose.popPose(); }
    }
}
