package thaumcraft.client.renderers.models.gear;

import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.world.entity.LivingEntity;

/**
 * Original ModelRobe86-411 (oracle
 * {@code thaumcraft/client/renderers/models/gear/ModelRobe.java}): the taumaturge
 * robe of the cultists and of the void thaumaturge, on the 128x64 sheet.
 *
 * <p>The original empties the six biped cube lists and hangs 58 gear parts on
 * the biped pivots. Its {@code float} argument selects the variant: 1.0 is the
 * armoured model (chest plate, book, scroll and back plate) and 0.5 is the
 * cloth model (the swaying front and back cloths and the foci pouch). The
 * original ItemCultistRobeArmor:112-115 picks the armoured model for the helm
 * and the greaves and the cloth model for the chest and the boots.
 *
 * <p>The original assigns {@code mirror=true} to some parts BEFORE their
 * {@code addBox}; 1.7.10's ModelBox reads that flag while the box is built, so
 * only those parts are mirrored natively. Parts whose mirror is assigned AFTER
 * their {@code addBox} keep their plain UVs.
 */
public final class ModelRobe extends ModelGearArmor {
    private final ModelPart frontclothR1, frontclothR2, frontclothL1, frontclothL2;
    private final ModelPart clothbackR1, clothbackR2, clothbackR3, clothbackL1, clothbackL2, clothbackL3;
    private final boolean armoured;

    /** The original armoured model, {@code new ModelRobe(1.0f)}. */
    public ModelRobe(ModelPart root) {
        this(root, true);
    }

    /**
     * @param armoured {@code true} for the original {@code new ModelRobe(1.0f)},
     *                 {@code false} for the original {@code new ModelRobe(0.5f)}.
     */
    public ModelRobe(ModelPart root, boolean armoured) {
        super(root);
        this.armoured = armoured;
        var body = root.getChild("body");
        var leftLeg = root.getChild("left_leg");
        frontclothR1 = child(body, "frontcloth_r1");
        frontclothR2 = child(body, "frontcloth_r2");
        frontclothL1 = child(body, "frontcloth_l1");
        frontclothL2 = child(body, "frontcloth_l2");
        clothbackR1 = child(body, "clothback_r1");
        clothbackR2 = child(body, "clothback_r2");
        clothbackR3 = child(body, "clothback_r3");
        clothbackL1 = child(body, "clothback_l1");
        clothbackL2 = child(body, "clothback_l2");
        clothbackL3 = child(body, "clothback_l3");
    }

    /**
     * The original mesh. The armoured variant carries the chest plate, the
     * book, the scroll, the back plate and the two chest cloths; the cloth
     * variant carries the four swaying front cloths, the six back cloths and
     * the foci pouch instead.
     */
    public static LayerDefinition createBodyLayer(boolean armoured) {
        var mesh = new MeshDefinition();
        var root = biped(mesh.getRoot());
        var head = root.getChild("head");
        var body = root.getChild("body");
        var rightArm = root.getChild("right_arm");
        var leftArm = root.getChild("left_arm");
        var rightLeg = root.getChild("right_leg");
        var leftLeg = root.getChild("left_leg");
        head.addOrReplaceChild("hood1", CubeListBuilder.create().texOffs(16, 7).addBox(-4.5F, -9, -4.6F, 9, 9, 9), PartPose.ZERO);
        head.addOrReplaceChild("hood2", CubeListBuilder.create().texOffs(52, 13).addBox(-4, -9.7F, 2, 8, 9, 3), PartPose.offsetAndRotation(0, 0, 0, -.2268928F, 0, 0));
        head.addOrReplaceChild("hood3", CubeListBuilder.create().texOffs(52, 14).addBox(-3.5F, -10, 3.5F, 7, 8, 3), PartPose.offsetAndRotation(0, 0, 0, -.3490659F, 0, 0));
        head.addOrReplaceChild("hood4", CubeListBuilder.create().texOffs(53, 15).addBox(-3, -10.7F, 3.5F, 6, 7, 3), PartPose.offsetAndRotation(0, 0, 0, -.5759587F, 0, 0));

        body.addOrReplaceChild("mbelt", CubeListBuilder.create().texOffs(16, 55).addBox(-4, 7, -3, 8, 5, 1), PartPose.ZERO);
        body.addOrReplaceChild("mbelt_b", CubeListBuilder.create().texOffs(16, 55).addBox(-4, 7, -4, 8, 5, 1), PartPose.offsetAndRotation(0, 0, 0, 0, 3.141593F, 0));
        body.addOrReplaceChild("mbelt_l", CubeListBuilder.create().texOffs(16, 36).addBox(4, 8, -3, 1, 3, 6), PartPose.ZERO);
        body.addOrReplaceChild("mbelt_r", CubeListBuilder.create().texOffs(16, 36).addBox(-5, 8, -3, 1, 3, 6), PartPose.ZERO);

        if (armoured) {
            body.addOrReplaceChild("chestplate", CubeListBuilder.create().texOffs(16, 25).addBox(-4, 1, -3, 8, 6, 1), PartPose.ZERO);
            body.addOrReplaceChild("chestthing", CubeListBuilder.create().texOffs(56, 50).addBox(-2.5F, 1, -4, 5, 7, 1), PartPose.ZERO);
            body.addOrReplaceChild("scroll", CubeListBuilder.create().texOffs(78, 25).addBox(-2, 9.5F, 4, 8, 3, 3), PartPose.offsetAndRotation(0, 0, 0, 0, 0, .1919862F));
            body.addOrReplaceChild("backplate", CubeListBuilder.create().texOffs(36, 45).addBox(-4, 1, 1.9F, 8, 11, 2), PartPose.ZERO);
            body.addOrReplaceChild("book", CubeListBuilder.create().texOffs(81, 16).addBox(1, 0, 4, 5, 7, 2), PartPose.offsetAndRotation(0, 0, 0, 0, 0, .7679449F));
            body.addOrReplaceChild("clothchest_l", CubeListBuilder.create().texOffs(108, 38).mirror().addBox(2.1F, .5F, -3.5F, 2, 8, 1), PartPose.ZERO);
            body.addOrReplaceChild("clothchest_r", CubeListBuilder.create().texOffs(108, 38).addBox(-4.1F, .5F, -3.5F, 2, 8, 1), PartPose.ZERO);
        } else {
            body.addOrReplaceChild("frontcloth_r1", CubeListBuilder.create().texOffs(108, 38).addBox(0, 0, 0, 3, 8, 1), PartPose.offsetAndRotation(-3, 11, -2.9F, -.1047198F, 0, 0));
            body.addOrReplaceChild("frontcloth_r2", CubeListBuilder.create().texOffs(108, 47).addBox(0, 7.5F, 1.7F, 3, 3, 1), PartPose.offsetAndRotation(-3, 11, -2.9F, -.3316126F, 0, 0));
            body.addOrReplaceChild("frontcloth_l1", CubeListBuilder.create().texOffs(108, 38).mirror().addBox(0, 0, 0, 3, 8, 1), PartPose.offsetAndRotation(0, 11, -2.9F, -.1047198F, 0, 0));
            body.addOrReplaceChild("frontcloth_l2", CubeListBuilder.create().texOffs(108, 47).mirror().addBox(0, 7.5F, 1.7F, 3, 3, 1), PartPose.offsetAndRotation(0, 11, -2.9F, -.3316126F, 0, 0));
            body.addOrReplaceChild("clothback_r1", CubeListBuilder.create().texOffs(118, 16).mirror().addBox(0, 0, 0, 4, 8, 1), PartPose.offsetAndRotation(-4, 11.5F, 2.9F, .1047198F, 0, 0));
            body.addOrReplaceChild("clothback_r2", CubeListBuilder.create().texOffs(123, 9).addBox(0, 7.8F, -.9F, 1, 2, 1), PartPose.offsetAndRotation(-4, 11.5F, 2.9F, .2268928F, 0, 0));
            body.addOrReplaceChild("clothback_r3", CubeListBuilder.create().texOffs(120, 12).mirror().addBox(1, 7.8F, -.9F, 3, 3, 1), PartPose.offsetAndRotation(-4, 11.5F, 2.9F, .2268928F, 0, 0));
            body.addOrReplaceChild("clothback_l1", CubeListBuilder.create().texOffs(118, 16).addBox(0, 0, 0, 4, 8, 1), PartPose.offsetAndRotation(0, 11.5F, 2.9F, .1047198F, 0, 0));
            body.addOrReplaceChild("clothback_l2", CubeListBuilder.create().texOffs(123, 9).mirror().addBox(3, 7.8F, -.9F, 1, 2, 1), PartPose.offsetAndRotation(0, 11.5F, 2.9F, .2268928F, 0, 0));
            body.addOrReplaceChild("clothback_l3", CubeListBuilder.create().texOffs(120, 12).addBox(0, 7.8F, -.9F, 3, 3, 1), PartPose.offsetAndRotation(0, 11.5F, 2.9F, .2268928F, 0, 0));
        }

        rightArm.addOrReplaceChild("shoulder_r", CubeListBuilder.create().texOffs(16, 45).mirror().addBox(-3.5F, -2.5F, -2.5F, 5, 5, 5), PartPose.ZERO);
        rightArm.addOrReplaceChild("rarm1", CubeListBuilder.create().texOffs(88, 39).addBox(-3.5F, 2.5F, -2.5F, 5, 7, 5), PartPose.ZERO);
        rightArm.addOrReplaceChild("rarm2", CubeListBuilder.create().texOffs(76, 32).addBox(-3, 5.5F, 2.5F, 4, 4, 2), PartPose.ZERO);
        rightArm.addOrReplaceChild("rarm3", CubeListBuilder.create().texOffs(88, 32).addBox(-2.5F, 3.5F, 2.5F, 3, 2, 1), PartPose.ZERO);
        rightArm.addOrReplaceChild("shoulderplate_top_r", CubeListBuilder.create().texOffs(56, 25).addBox(-5.5F, -2.5F, -3.5F, 2, 1, 7), PartPose.offsetAndRotation(0, 0, 0, 0, 0, .4363323F));
        rightArm.addOrReplaceChild("shoulderplate_r1", CubeListBuilder.create().texOffs(56, 33).addBox(-4.5F, -1.5F, -3.5F, 1, 4, 7), PartPose.offsetAndRotation(0, 0, 0, 0, 0, .4363323F));
        rightArm.addOrReplaceChild("shoulderplate_r2", CubeListBuilder.create().texOffs(40, 33).addBox(-3.5F, 1.5F, -3.5F, 1, 3, 7), PartPose.offsetAndRotation(0, 0, 0, 0, 0, .4363323F));
        rightArm.addOrReplaceChild("shoulderplate_r3", CubeListBuilder.create().texOffs(40, 33).addBox(-2.5F, 3.5F, -3.5F, 1, 3, 7), PartPose.offsetAndRotation(0, 0, 0, 0, 0, .4363323F));

        leftArm.addOrReplaceChild("shoulder_l", CubeListBuilder.create().texOffs(16, 45).addBox(-1.5F, -2.5F, -2.5F, 5, 5, 5), PartPose.ZERO);
        leftArm.addOrReplaceChild("larm1", CubeListBuilder.create().texOffs(88, 39).mirror().addBox(-1.5F, 2.5F, -2.5F, 5, 7, 5), PartPose.ZERO);
        leftArm.addOrReplaceChild("larm2", CubeListBuilder.create().texOffs(76, 32).addBox(-1, 5.5F, 2.5F, 4, 4, 2), PartPose.ZERO);
        leftArm.addOrReplaceChild("larm3", CubeListBuilder.create().texOffs(88, 32).addBox(-.5F, 3.5F, 2.5F, 3, 2, 1), PartPose.ZERO);
        leftArm.addOrReplaceChild("shoulderplate_top_l", CubeListBuilder.create().texOffs(56, 25).addBox(3.5F, -2.5F, -3.5F, 2, 1, 7), PartPose.offsetAndRotation(0, 0, 0, 0, 0, -.4363323F));
        leftArm.addOrReplaceChild("shoulderplate_l1", CubeListBuilder.create().texOffs(56, 33).addBox(3.5F, -1.5F, -3.5F, 1, 4, 7), PartPose.offsetAndRotation(0, 0, 0, 0, 0, -.4363323F));
        leftArm.addOrReplaceChild("shoulderplate_l2", CubeListBuilder.create().texOffs(40, 33).addBox(2.5F, 1.5F, -3.5F, 1, 3, 7), PartPose.offsetAndRotation(0, 0, 0, 0, 0, -.4363323F));
        leftArm.addOrReplaceChild("shoulderplate_l3", CubeListBuilder.create().texOffs(40, 33).addBox(1.5F, 3.5F, -3.5F, 1, 3, 7), PartPose.offsetAndRotation(0, 0, 0, 0, 0, -.4363323F));

        rightLeg.addOrReplaceChild("legpanel_r4", CubeListBuilder.create().texOffs(76, 38).addBox(-3, .5F, -3.5F, 2, 3, 1), PartPose.offsetAndRotation(0, 0, 0, -.4363323F, 0, 0));
        rightLeg.addOrReplaceChild("legpanel_r5", CubeListBuilder.create().texOffs(76, 42).addBox(-3, 2.5F, -2.5F, 2, 3, 1), PartPose.offsetAndRotation(0, 0, 0, -.4363323F, 0, 0));
        rightLeg.addOrReplaceChild("legpanel_r6", CubeListBuilder.create().texOffs(82, 38).addBox(-3, 4.5F, -1.5F, 2, 3, 1), PartPose.offsetAndRotation(0, 0, 0, -.4363323F, 0, 0));
        rightLeg.addOrReplaceChild("sidepanel_r1", CubeListBuilder.create().texOffs(116, 25).addBox(-2.5F, .5F, -2.5F, 1, 4, 5), PartPose.offsetAndRotation(0, 0, 0, 0, 0, .4363323F));
        rightLeg.addOrReplaceChild("sidecloth_r1", CubeListBuilder.create().texOffs(116, 42).addBox(-2.5F, .5F, -2.5F, 1, 5, 5), PartPose.offsetAndRotation(0, 0, 0, 0, 0, .122173F));
        rightLeg.addOrReplaceChild("sidecloth_r2", CubeListBuilder.create().texOffs(116, 34).addBox(-1.5F, 5.5F, -2.5F, 1, 3, 5), PartPose.offsetAndRotation(0, 0, 0, 0, 0, .296706F));
        rightLeg.addOrReplaceChild("sidecloth_r3", CubeListBuilder.create().texOffs(116, 1).addBox(.4F, 8.4F, -2.5F, 1, 3, 5), PartPose.offsetAndRotation(0, 0, 0, 0, 0, .5235988F));

        leftLeg.addOrReplaceChild("legpanel_l4", CubeListBuilder.create().texOffs(76, 38).mirror().addBox(1, .5F, -3.5F, 2, 3, 1), PartPose.offsetAndRotation(0, 0, 0, -.4363323F, 0, 0));
        leftLeg.addOrReplaceChild("legpanel_l5", CubeListBuilder.create().texOffs(76, 42).mirror().addBox(1, 2.5F, -2.5F, 2, 3, 1), PartPose.offsetAndRotation(0, 0, 0, -.4363323F, 0, 0));
        leftLeg.addOrReplaceChild("legpanel_l6", CubeListBuilder.create().texOffs(82, 38).mirror().addBox(1, 4.5F, -1.5F, 2, 3, 1), PartPose.offsetAndRotation(0, 0, 0, -.4363323F, 0, 0));
        leftLeg.addOrReplaceChild("sidepanel_l1", CubeListBuilder.create().texOffs(116, 25).addBox(1.5F, .5F, -2.5F, 1, 4, 5), PartPose.offsetAndRotation(0, 0, 0, 0, 0, -.4363323F));
        leftLeg.addOrReplaceChild("sidecloth_l1", CubeListBuilder.create().texOffs(116, 42).addBox(1.5F, .5F, -2.5F, 1, 5, 5), PartPose.offsetAndRotation(0, 0, 0, 0, 0, -.122173F));
        leftLeg.addOrReplaceChild("sidecloth_l2", CubeListBuilder.create().texOffs(116, 34).addBox(.5F, 5.5F, -2.5F, 1, 3, 5), PartPose.offsetAndRotation(0, 0, 0, 0, 0, -.296706F));
        leftLeg.addOrReplaceChild("sidecloth_l3", CubeListBuilder.create().texOffs(116, 1).addBox(-1.4F, 8.4F, -2.5F, 1, 3, 5), PartPose.offsetAndRotation(0, 0, 0, 0, 0, -.5235988F));
        if (!armoured) {
            leftLeg.addOrReplaceChild("foci_pouch", CubeListBuilder.create().texOffs(100, 20).addBox(3.5F, .5F, -2.5F, 3, 6, 5), PartPose.offsetAndRotation(0, 0, 0, 0, 0, -.122173F));
        }
        return layer(mesh);
    }

    /** True for the original {@code new ModelRobe(1.0f)} variant. */
    public boolean armoured() {
        return armoured;
    }

    /**
     * Original ModelRobe419-427: the cloths swing with the walk cycle. The
     * armoured variant has no cloths, so the original animation has nothing to
     * move there.
     */
    @Override public void setupAnim(LivingEntity entity, float limbSwing, float limbSwingAmount, float ageInTicks, float headYaw, float headPitch) {
        super.setupAnim(entity, limbSwing, limbSwingAmount, ageInTicks, headYaw, headPitch);
        if (frontclothR1 == null) return;
        float a = (float) Math.cos(limbSwing * 0.6662F) * 1.4F * limbSwingAmount;
        float b = (float) Math.cos(limbSwing * 0.6662F + Math.PI) * 1.4F * limbSwingAmount;
        float c = Math.min(a, b);
        frontclothR1.xRot = c - 0.1047198F;
        frontclothL1.xRot = c - 0.1047198F;
        frontclothR2.xRot = c - 0.3316126F;
        frontclothL2.xRot = c - 0.3316126F;
        clothbackR1.xRot = -c + 0.1047198F;
        clothbackL1.xRot = -c + 0.1047198F;
        clothbackR3.xRot = -c + 0.2268928F;
        clothbackL3.xRot = -c + 0.2268928F;
        clothbackL2.xRot = clothbackL3.xRot;
        clothbackR2.xRot = clothbackL3.xRot;
    }

    /** The original ModelRobe465-481 for a skeleton or a zombie wearer. */
    public void setZombiePose(LivingEntity entity, float limbSwing, float limbSwingAmount, float ageInTicks) {
        zombieArms(attackTime, ageInTicks);
    }
}
