package thaumcraft.client.renderers.models.gear;

import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.world.entity.LivingEntity;

/**
 * Original ModelFortressArmor101-527 (oracle
 * {@code thaumcraft/client/renderers/models/gear/ModelFortressArmor.java}): the
 * Thaumium Fortress helm, cuirass and thigh guards on the 128x64 sheet.
 *
 * <p>The original empties the six biped cube lists and hangs the gear parts on
 * the biped pivots. Its {@code float} argument selects the variant: below 1.0
 * it is the light model (only the metal belt) and at or above 1.0 the full
 * model with the chest plate, the scroll, the back plate and the book.
 *
 * <p>The original ModelFortressArmor:560-585 hides the ornaments, the extra
 * shoulder plates and the scroll or the book according to how many fortress
 * pieces the wearer has; that rule is reproduced by
 * {@link #setPieceVisibility(int, int, boolean)}.
 */
public final class ModelFortressArmor extends ModelGearArmor {
    private final ModelPart mask0, mask1, mask2, goggles;
    private final ModelPart ornamentL, ornamentL2, ornamentR, ornamentR2;
    private final ModelPart helmet, helmetR, helmetL, helmetB, capsthingy, flapR, flapL, gemOrnament, gem;
    private final ModelPart mbelt, mbeltL, mbeltR, beltR, beltL, chestplate, scroll, backplate, book;
    private final ModelPart shoulderR, gauntletR, gauntletstrapR1, gauntletstrapR2, shoulderplateRtop, shoulderplateR1, shoulderplateR2, shoulderplateR3;
    private final ModelPart shoulderL, gauntletL, gauntletstrapL1, gauntletstrapL2, shoulderplateLtop, shoulderplateL1, shoulderplateL2, shoulderplateL3;
    private final ModelPart legpanelR1, legpanelR2, legpanelR3, legpanelR4, legpanelR5, legpanelR6;
    private final ModelPart sidepanelR1, sidepanelR2, sidepanelR3, backpanelR1, backpanelR2, backpanelR3;
    private final ModelPart legpanelL1, legpanelL2, legpanelL3, legpanelL4, legpanelL5, legpanelL6;
    private final ModelPart sidepanelL1, sidepanelL2, sidepanelL3, backpanelL1, backpanelL2, backpanelL3;

    /** The original full model, {@code new ModelFortressArmor(1.0f)}. */
    public ModelFortressArmor(ModelPart root) {
        this(root, true);
    }

    /**
     * @param armoured {@code true} for the original {@code new ModelFortressArmor(1.0f)},
     *                 {@code false} for the original {@code new ModelFortressArmor(0.5f)}.
     */
    public ModelFortressArmor(ModelPart root, boolean armoured) {
        super(root);
        var head = root.getChild("head");
        var body = root.getChild("body");
        mask0 = head.getChild("mask0");
        mask1 = head.getChild("mask1");
        mask2 = head.getChild("mask2");
        goggles = head.getChild("goggles");
        ornamentL = head.getChild("ornament_l");
        ornamentL2 = head.getChild("ornament_l2");
        ornamentR = head.getChild("ornament_r");
        ornamentR2 = head.getChild("ornament_r2");
        helmet = head.getChild("helmet");
        helmetR = head.getChild("helmet_r");
        helmetL = head.getChild("helmet_l");
        helmetB = head.getChild("helmet_b");
        capsthingy = head.getChild("capsthingy");
        flapR = head.getChild("flap_r");
        flapL = head.getChild("flap_l");
        gemOrnament = head.getChild("gem_ornament");
        gem = head.getChild("gem");
        mbelt = child(body, "mbelt");
        mbeltL = child(body, "mbelt_l");
        mbeltR = child(body, "mbelt_r");
        beltR = child(body, "belt_r");
        beltL = child(body, "belt_l");
        chestplate = child(body, "chestplate");
        scroll = child(body, "scroll");
        backplate = child(body, "backplate");
        book = child(body, "book");
        shoulderR = root.getChild("right_arm").getChild("shoulder_r");
        gauntletR = root.getChild("right_arm").getChild("gauntlet_r");
        gauntletstrapR1 = root.getChild("right_arm").getChild("gauntletstrap_r1");
        gauntletstrapR2 = root.getChild("right_arm").getChild("gauntletstrap_r2");
        shoulderplateRtop = root.getChild("right_arm").getChild("shoulderplate_rtop");
        shoulderplateR1 = root.getChild("right_arm").getChild("shoulderplate_r1");
        shoulderplateR2 = root.getChild("right_arm").getChild("shoulderplate_r2");
        shoulderplateR3 = root.getChild("right_arm").getChild("shoulderplate_r3");
        shoulderL = root.getChild("left_arm").getChild("shoulder_l");
        gauntletL = root.getChild("left_arm").getChild("gauntlet_l");
        gauntletstrapL1 = root.getChild("left_arm").getChild("gauntletstrap_l1");
        gauntletstrapL2 = root.getChild("left_arm").getChild("gauntletstrap_l2");
        shoulderplateLtop = root.getChild("left_arm").getChild("shoulderplate_ltop");
        shoulderplateL1 = root.getChild("left_arm").getChild("shoulderplate_l1");
        shoulderplateL2 = root.getChild("left_arm").getChild("shoulderplate_l2");
        shoulderplateL3 = root.getChild("left_arm").getChild("shoulderplate_l3");
        legpanelR1 = root.getChild("right_leg").getChild("legpanel_r1");
        legpanelR2 = root.getChild("right_leg").getChild("legpanel_r2");
        legpanelR3 = root.getChild("right_leg").getChild("legpanel_r3");
        legpanelR4 = root.getChild("right_leg").getChild("legpanel_r4");
        legpanelR5 = root.getChild("right_leg").getChild("legpanel_r5");
        legpanelR6 = root.getChild("right_leg").getChild("legpanel_r6");
        sidepanelR1 = root.getChild("right_leg").getChild("sidepanel_r1");
        sidepanelR2 = root.getChild("right_leg").getChild("sidepanel_r2");
        sidepanelR3 = root.getChild("right_leg").getChild("sidepanel_r3");
        backpanelR1 = root.getChild("right_leg").getChild("backpanel_r1");
        backpanelR2 = root.getChild("right_leg").getChild("backpanel_r2");
        backpanelR3 = root.getChild("right_leg").getChild("backpanel_r3");
        legpanelL1 = root.getChild("left_leg").getChild("legpanel_l1");
        legpanelL2 = root.getChild("left_leg").getChild("legpanel_l2");
        legpanelL3 = root.getChild("left_leg").getChild("legpanel_l3");
        legpanelL4 = root.getChild("left_leg").getChild("legpanel_l4");
        legpanelL5 = root.getChild("left_leg").getChild("legpanel_l5");
        legpanelL6 = root.getChild("left_leg").getChild("legpanel_l6");
        sidepanelL1 = root.getChild("left_leg").getChild("sidepanel_l1");
        sidepanelL2 = root.getChild("left_leg").getChild("sidepanel_l2");
        sidepanelL3 = root.getChild("left_leg").getChild("sidepanel_l3");
        backpanelL1 = root.getChild("left_leg").getChild("backpanel_l1");
        backpanelL2 = root.getChild("left_leg").getChild("backpanel_l2");
        backpanelL3 = root.getChild("left_leg").getChild("backpanel_l3");
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
        for (int index = 0; index < 3; index++) {
            head.addOrReplaceChild("mask" + index, CubeListBuilder.create().texOffs(52 + index * 24, 2).addBox(-4.5F, -5, -4.6F, 9, 5, 1), PartPose.ZERO);
        }
        head.addOrReplaceChild("goggles", CubeListBuilder.create().texOffs(100, 18).addBox(-4.5F, -5, -4.25F, 9, 5, 1), PartPose.ZERO);
        head.addOrReplaceChild("ornament_l", CubeListBuilder.create().texOffs(78, 8).mirror().addBox(1.5F, -9, -6.5F, 2, 2, 1), PartPose.offsetAndRotation(0, 0, 0, -.1396263F, 0, 0));
        head.addOrReplaceChild("ornament_l2", CubeListBuilder.create().texOffs(78, 8).mirror().addBox(3.5F, -10, -6.5F, 1, 2, 1), PartPose.offsetAndRotation(0, 0, 0, -.1396263F, 0, 0));
        head.addOrReplaceChild("ornament_r", CubeListBuilder.create().texOffs(78, 8).addBox(-3.5F, -9, -6.5F, 2, 2, 1), PartPose.offsetAndRotation(0, 0, 0, -.1396263F, 0, 0));
        head.addOrReplaceChild("ornament_r2", CubeListBuilder.create().texOffs(78, 8).addBox(-4.5F, -10, -6.5F, 1, 2, 1), PartPose.offsetAndRotation(0, 0, 0, -.1396263F, 0, 0));
        head.addOrReplaceChild("helmet", CubeListBuilder.create().texOffs(41, 8).addBox(-4.5F, -9, -4.5F, 9, 4, 9), PartPose.ZERO);
        head.addOrReplaceChild("helmet_r", CubeListBuilder.create().texOffs(21, 13).addBox(-6.5F, -3, -4.5F, 1, 5, 9), PartPose.offsetAndRotation(0, 0, 0, 0, 0, .5235988F));
        head.addOrReplaceChild("helmet_l", CubeListBuilder.create().texOffs(21, 13).mirror().addBox(5.5F, -3, -4.5F, 1, 5, 9), PartPose.offsetAndRotation(0, 0, 0, 0, 0, -.5235988F));
        head.addOrReplaceChild("helmet_b", CubeListBuilder.create().texOffs(41, 21).addBox(-4.5F, -3, 5.5F, 9, 5, 1), PartPose.offsetAndRotation(0, 0, 0, .5235988F, 0, 0));
        head.addOrReplaceChild("capsthingy", CubeListBuilder.create().texOffs(21, 0).addBox(-4.5F, -6, -6.5F, 9, 1, 2), PartPose.ZERO);
        head.addOrReplaceChild("flap_r", CubeListBuilder.create().texOffs(59, 10).addBox(-10, -2, -1, 3, 3, 1), PartPose.offsetAndRotation(0, 0, 0, 0, -.5235988F, .5235988F));
        head.addOrReplaceChild("flap_l", CubeListBuilder.create().texOffs(59, 10).mirror().addBox(7, -2, -1, 3, 3, 1), PartPose.offsetAndRotation(0, 0, 0, 0, .5235988F, -.5235988F));
        head.addOrReplaceChild("gem_ornament", CubeListBuilder.create().texOffs(68, 11).addBox(-1.5F, -9, -7, 3, 3, 2), PartPose.offsetAndRotation(0, 0, 0, -.1396263F, 0, 0));
        head.addOrReplaceChild("gem", CubeListBuilder.create().texOffs(72, 8).addBox(-1, -8.5F, -7.5F, 2, 2, 1), PartPose.offsetAndRotation(0, 0, 0, -.1396263F, 0, 0));

        if (armoured) {
            body.addOrReplaceChild("belt_r", CubeListBuilder.create().texOffs(76, 44).addBox(-5, 4, -3, 1, 3, 6), PartPose.ZERO);
            body.addOrReplaceChild("belt_l", CubeListBuilder.create().texOffs(76, 44).addBox(4, 4, -3, 1, 3, 6), PartPose.ZERO);
            body.addOrReplaceChild("chestplate", CubeListBuilder.create().texOffs(56, 45).addBox(-4, 1, -4, 8, 7, 2), PartPose.ZERO);
            body.addOrReplaceChild("scroll", CubeListBuilder.create().texOffs(34, 27).addBox(-2, 9.5F, 4, 8, 3, 3), PartPose.offsetAndRotation(0, 0, 0, 0, 0, .1919862F));
            body.addOrReplaceChild("backplate", CubeListBuilder.create().texOffs(36, 45).addBox(-4, 1, 2, 8, 11, 2), PartPose.ZERO);
            body.addOrReplaceChild("book", CubeListBuilder.create().texOffs(100, 8).addBox(1, -.3F, 4, 5, 7, 2), PartPose.offsetAndRotation(0, 0, 0, 0, 0, .7679449F));
        } else {
            body.addOrReplaceChild("mbelt", CubeListBuilder.create().texOffs(56, 55).addBox(-4, 8, -3, 8, 4, 1), PartPose.ZERO);
            body.addOrReplaceChild("mbelt_l", CubeListBuilder.create().texOffs(76, 44).addBox(4, 8, -3, 1, 3, 6), PartPose.ZERO);
            body.addOrReplaceChild("mbelt_r", CubeListBuilder.create().texOffs(76, 44).addBox(-5, 8, -3, 1, 3, 6), PartPose.ZERO);
        }

        rightArm.addOrReplaceChild("shoulder_r", CubeListBuilder.create().texOffs(56, 35).addBox(-3.5F, -2.5F, -2.5F, 5, 5, 5), PartPose.ZERO);
        rightArm.addOrReplaceChild("gauntlet_r", CubeListBuilder.create().texOffs(100, 26).addBox(-3.5F, 3.5F, -2.5F, 2, 6, 5), PartPose.ZERO);
        rightArm.addOrReplaceChild("gauntletstrap_r1", CubeListBuilder.create().texOffs(84, 31).addBox(-1.5F, 3.5F, -2.5F, 3, 1, 5), PartPose.ZERO);
        rightArm.addOrReplaceChild("gauntletstrap_r2", CubeListBuilder.create().texOffs(84, 31).addBox(-1.5F, 6.5F, -2.5F, 3, 1, 5), PartPose.ZERO);
        rightArm.addOrReplaceChild("shoulderplate_rtop", CubeListBuilder.create().texOffs(110, 37).addBox(-5.5F, -2.5F, -3.5F, 2, 1, 7), PartPose.offsetAndRotation(0, 0, 0, 0, 0, .4363323F));
        rightArm.addOrReplaceChild("shoulderplate_r1", CubeListBuilder.create().texOffs(110, 45).addBox(-4.5F, -1.5F, -3.5F, 1, 4, 7), PartPose.offsetAndRotation(0, 0, 0, 0, 0, .4363323F));
        rightArm.addOrReplaceChild("shoulderplate_r2", CubeListBuilder.create().texOffs(94, 45).addBox(-3.5F, 1.5F, -3.5F, 1, 3, 7), PartPose.offsetAndRotation(0, 0, 0, 0, 0, .4363323F));
        rightArm.addOrReplaceChild("shoulderplate_r3", CubeListBuilder.create().texOffs(94, 45).addBox(-2.5F, 3.5F, -3.5F, 1, 3, 7), PartPose.offsetAndRotation(0, 0, 0, 0, 0, .4363323F));

        leftArm.addOrReplaceChild("shoulder_l", CubeListBuilder.create().texOffs(56, 35).mirror().addBox(-1.5F, -2.5F, -2.5F, 5, 5, 5), PartPose.ZERO);
        leftArm.addOrReplaceChild("gauntlet_l", CubeListBuilder.create().texOffs(114, 26).addBox(1.5F, 3.5F, -2.5F, 2, 6, 5), PartPose.ZERO);
        leftArm.addOrReplaceChild("gauntletstrap_l1", CubeListBuilder.create().texOffs(84, 31).mirror().addBox(-1.5F, 3.5F, -2.5F, 3, 1, 5), PartPose.ZERO);
        leftArm.addOrReplaceChild("gauntletstrap_l2", CubeListBuilder.create().texOffs(84, 31).mirror().addBox(-1.5F, 6.5F, -2.5F, 3, 1, 5), PartPose.ZERO);
        leftArm.addOrReplaceChild("shoulderplate_ltop", CubeListBuilder.create().texOffs(110, 37).mirror().addBox(3.5F, -2.5F, -3.5F, 2, 1, 7), PartPose.offsetAndRotation(0, 0, 0, 0, 0, -.4363323F));
        leftArm.addOrReplaceChild("shoulderplate_l1", CubeListBuilder.create().texOffs(110, 45).mirror().addBox(3.5F, -1.5F, -3.5F, 1, 4, 7), PartPose.offsetAndRotation(0, 0, 0, 0, 0, -.4363323F));
        leftArm.addOrReplaceChild("shoulderplate_l2", CubeListBuilder.create().texOffs(94, 45).mirror().addBox(2.5F, 1.5F, -3.5F, 1, 3, 7), PartPose.offsetAndRotation(0, 0, 0, 0, 0, -.4363323F));
        leftArm.addOrReplaceChild("shoulderplate_l3", CubeListBuilder.create().texOffs(94, 45).mirror().addBox(1.5F, 3.5F, -3.5F, 1, 3, 7), PartPose.offsetAndRotation(0, 0, 0, 0, 0, -.4363323F));

        rightLeg.addOrReplaceChild("legpanel_r1", CubeListBuilder.create().texOffs(0, 51).addBox(-1, .5F, -3.5F, 3, 4, 1), PartPose.offsetAndRotation(0, 0, 0, -.4363323F, 0, 0));
        rightLeg.addOrReplaceChild("legpanel_r2", CubeListBuilder.create().texOffs(8, 51).addBox(-1, 3.5F, -2.5F, 3, 4, 1), PartPose.offsetAndRotation(0, 0, 0, -.4363323F, 0, 0));
        rightLeg.addOrReplaceChild("legpanel_r3", CubeListBuilder.create().texOffs(0, 56).addBox(-1, 6.5F, -1.5F, 3, 3, 1), PartPose.offsetAndRotation(0, 0, 0, -.4363323F, 0, 0));
        rightLeg.addOrReplaceChild("legpanel_r4", CubeListBuilder.create().texOffs(0, 43).addBox(-3, .5F, -3.5F, 2, 3, 1), PartPose.offsetAndRotation(0, 0, 0, -.4363323F, 0, 0));
        rightLeg.addOrReplaceChild("legpanel_r5", CubeListBuilder.create().texOffs(0, 47).addBox(-3, 2.5F, -2.5F, 2, 3, 1), PartPose.offsetAndRotation(0, 0, 0, -.4363323F, 0, 0));
        rightLeg.addOrReplaceChild("legpanel_r6", CubeListBuilder.create().texOffs(6, 43).addBox(-3, 4.5F, -1.5F, 2, 3, 1), PartPose.offsetAndRotation(0, 0, 0, -.4363323F, 0, 0));
        rightLeg.addOrReplaceChild("sidepanel_r1", CubeListBuilder.create().texOffs(0, 22).addBox(-2.5F, .5F, -2.5F, 1, 4, 5), PartPose.offsetAndRotation(0, 0, 0, 0, 0, .4363323F));
        rightLeg.addOrReplaceChild("sidepanel_r2", CubeListBuilder.create().texOffs(0, 31).addBox(-1.5F, 3.5F, -2.5F, 1, 3, 5), PartPose.offsetAndRotation(0, 0, 0, 0, 0, .4363323F));
        rightLeg.addOrReplaceChild("sidepanel_r3", CubeListBuilder.create().texOffs(12, 31).addBox(-.5F, 5.5F, -2.5F, 1, 3, 5), PartPose.offsetAndRotation(0, 0, 0, 0, 0, .4363323F));
        rightLeg.addOrReplaceChild("backpanel_r1", CubeListBuilder.create().texOffs(0, 18).addBox(-3, .5F, 2.5F, 5, 3, 1), PartPose.offsetAndRotation(0, 0, 0, .4363323F, 0, 0));
        rightLeg.addOrReplaceChild("backpanel_r2", CubeListBuilder.create().texOffs(0, 18).addBox(-3, 2.5F, 1.5F, 5, 3, 1), PartPose.offsetAndRotation(0, 0, 0, .4363323F, 0, 0));
        rightLeg.addOrReplaceChild("backpanel_r3", CubeListBuilder.create().texOffs(0, 18).addBox(-3, 4.5F, .5F, 5, 3, 1), PartPose.offsetAndRotation(0, 0, 0, .4363323F, 0, 0));

        leftLeg.addOrReplaceChild("legpanel_l1", CubeListBuilder.create().texOffs(0, 51).mirror().addBox(-2, .5F, -3.5F, 3, 4, 1), PartPose.offsetAndRotation(0, 0, 0, -.4363323F, 0, 0));
        leftLeg.addOrReplaceChild("legpanel_l2", CubeListBuilder.create().texOffs(8, 51).mirror().addBox(-2, 3.5F, -2.5F, 3, 4, 1), PartPose.offsetAndRotation(0, 0, 0, -.4363323F, 0, 0));
        leftLeg.addOrReplaceChild("legpanel_l3", CubeListBuilder.create().texOffs(0, 56).mirror().addBox(-2, 6.5F, -1.5F, 3, 3, 1), PartPose.offsetAndRotation(0, 0, 0, -.4363323F, 0, 0));
        leftLeg.addOrReplaceChild("legpanel_l4", CubeListBuilder.create().texOffs(0, 43).mirror().addBox(1, .5F, -3.5F, 2, 3, 1), PartPose.offsetAndRotation(0, 0, 0, -.4363323F, 0, 0));
        leftLeg.addOrReplaceChild("legpanel_l5", CubeListBuilder.create().texOffs(0, 47).mirror().addBox(1, 2.5F, -2.5F, 2, 3, 1), PartPose.offsetAndRotation(0, 0, 0, -.4363323F, 0, 0));
        leftLeg.addOrReplaceChild("legpanel_l6", CubeListBuilder.create().texOffs(6, 43).mirror().addBox(1, 4.5F, -1.5F, 2, 3, 1), PartPose.offsetAndRotation(0, 0, 0, -.4363323F, 0, 0));
        leftLeg.addOrReplaceChild("sidepanel_l1", CubeListBuilder.create().texOffs(0, 22).mirror().addBox(1.5F, .5F, -2.5F, 1, 4, 5), PartPose.offsetAndRotation(0, 0, 0, 0, 0, -.4363323F));
        leftLeg.addOrReplaceChild("sidepanel_l2", CubeListBuilder.create().texOffs(0, 31).mirror().addBox(.5F, 3.5F, -2.5F, 1, 3, 5), PartPose.offsetAndRotation(0, 0, 0, 0, 0, -.4363323F));
        leftLeg.addOrReplaceChild("sidepanel_l3", CubeListBuilder.create().texOffs(12, 31).mirror().addBox(-.5F, 5.5F, -2.5F, 1, 3, 5), PartPose.offsetAndRotation(0, 0, 0, 0, 0, -.4363323F));
        leftLeg.addOrReplaceChild("backpanel_l1", CubeListBuilder.create().texOffs(0, 18).mirror().addBox(-2, .5F, 2.5F, 5, 3, 1), PartPose.offsetAndRotation(0, 0, 0, .4363323F, 0, 0));
        leftLeg.addOrReplaceChild("backpanel_l2", CubeListBuilder.create().texOffs(0, 18).mirror().addBox(-2, 2.5F, 1.5F, 5, 3, 1), PartPose.offsetAndRotation(0, 0, 0, .4363323F, 0, 0));
        leftLeg.addOrReplaceChild("backpanel_l3", CubeListBuilder.create().texOffs(0, 18).mirror().addBox(-2, 4.5F, .5F, 5, 3, 1), PartPose.offsetAndRotation(0, 0, 0, .4363323F, 0, 0));
        return layer(mesh);
    }

    /**
     * Original ModelFortressArmor558-585: the ornaments, the extra shoulder
     * plates, the scroll and the book appear only with a full fortress set,
     * the mask only with the infused mask and the goggles only without them.
     *
     * @param set    the number of worn fortress pieces, -1 when none is worn.
     * @param mask   the infused mask, -1 when there is none.
     * @param goggles whether the helm carries the infused goggles.
     */
    public void setPieceVisibility(int set, int mask, boolean goggles) {
        this.goggles.visible = !goggles;
        mask0.visible = mask != 0;
        mask1.visible = mask != 1;
        mask2.visible = mask != 2;
        if (scroll != null) scroll.visible = set < 3;
        if (book != null) book.visible = set < 2;
        ornamentL.visible = set < 3;
        ornamentL2.visible = set < 3;
        ornamentR.visible = set < 3;
        ornamentR2.visible = set < 3;
        gemOrnament.visible = set < 3;
        gem.visible = set < 3;
        flapL.visible = set < 2;
        flapR.visible = set < 2;
        shoulderplateLtop.visible = set < 2;
        shoulderplateL1.visible = set < 2;
        shoulderplateL2.visible = set < 3;
        shoulderplateL3.visible = set < 3;
        shoulderplateRtop.visible = set < 2;
        shoulderplateR1.visible = set < 2;
        shoulderplateR2.visible = set < 3;
        shoulderplateR3.visible = set < 3;
        sidepanelR2.visible = set < 2;
        sidepanelL2.visible = set < 2;
        sidepanelR3.visible = set < 3;
        sidepanelL3.visible = set < 3;
    }

    /** The original ModelFortressArmor628-643 for a skeleton or a zombie wearer. */
    public void setZombiePose(LivingEntity entity, float limbSwing, float limbSwingAmount, float ageInTicks) {
        zombieArms(attackTime, ageInTicks);
    }
}
