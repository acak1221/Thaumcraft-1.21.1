package thaumcraft.client.renderers.entity;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import net.minecraft.client.model.geom.ModelPart;
import org.junit.jupiter.api.Test;
import thaumcraft.client.renderers.models.entities.ModelEldritchCrab;
import thaumcraft.client.renderers.models.entities.ModelEldritchGolem;
import thaumcraft.client.renderers.models.entities.ModelTaintSheep1;
import thaumcraft.client.renderers.models.entities.ModelTaintSheep2;
import thaumcraft.client.renderers.models.entities.ModelWatcher;

/**
 * The original TC4 client model geometry, checked part by part. These are the port's
 * own regression guards: every named part, offset and rest rotation below is the value
 * the 1.7.10 model used.
 */
class ClientModelGeometryTest {
    private static final float EPS = 1.0E-5F;

    private static void assertPart(ModelPart part, float x, float y, float z) {
        assertEquals(x, part.x, EPS, "x");
        assertEquals(y, part.y, EPS, "y");
        assertEquals(z, part.z, EPS, "z");
    }

    private static void assertRotation(ModelPart part, float x, float y, float z) {
        assertEquals(x, part.xRot, EPS, "xRot");
        assertEquals(y, part.yRot, EPS, "yRot");
        assertEquals(z, part.zRot, EPS, "zRot");
    }

    @Test void crabKeepsTheOriginalTwentyOneParts() {
        ModelPart root = ModelEldritchCrab.createBodyLayer().bakeRoot();
        assertEquals(21, root.getAllParts().count() - 1, "the original crab has twenty one parts");
        assertPart(root.getChild("torso"), 0, 18, 0);
        assertRotation(root.getChild("torso"), .0523599F, 0, 0);
        assertPart(root.getChild("tail_helm"), 0, 18, 0);
        assertRotation(root.getChild("tail_helm"), .1047198F, 0, 0);
        assertPart(root.getChild("tail_bare"), 0, 18, 0);
        assertPart(root.getChild("head0"), 0, 18, 0);
        assertPart(root.getChild("head1"), 0, 18, 0);
        assertPart(root.getChild("r_claw0"), -6, 17, -7);
        assertPart(root.getChild("r_claw1"), -6, 15.5F, -10);
        assertPart(root.getChild("r_claw2"), -6, 18.5F, -10);
        assertRotation(root.getChild("r_claw2"), .3141593F, 0, 0);
        assertPart(root.getChild("l_claw0"), 6, 17, -7);
        assertPart(root.getChild("l_claw1"), 6, 15.5F, -10);
        assertPart(root.getChild("l_claw2"), 6, 18.5F, -10);
        assertPart(root.getChild("r_arm"), -3, 17, -4);
        assertRotation(root.getChild("r_arm"), 0, .7504916F, 0);
        assertPart(root.getChild("l_arm"), 4, 17, -5);
        assertRotation(root.getChild("l_arm"), 0, -.7504916F, 0);
        assertPart(root.getChild("rr_leg0"), -4, 20, -1.5F);
        assertPart(root.getChild("rr_leg1"), -4, 20, -1.5F);
        assertPart(root.getChild("rf_leg0"), -4, 20, -3.5F);
        assertPart(root.getChild("rf_leg1"), -4, 20, -3.5F);
        assertPart(root.getChild("lr_leg0"), 4, 20, -1.5F);
        assertPart(root.getChild("lr_leg1"), 4, 20, -1.5F);
        assertPart(root.getChild("lf_leg0"), 4, 20, -3.5F);
        assertPart(root.getChild("lf_leg1"), 4, 20, -3.5F);
    }

    @Test void eldritchGolemKeepsTheOriginalHeadAndArmLinks() {
        ModelPart root = ModelEldritchGolem.createBodyLayer().bakeRoot();
        assertPart(root.getChild("head"), 0, 4.5F, -3.8F);
        assertRotation(root.getChild("head"), -.1047198F, 0, 0);
        assertPart(root.getChild("head2"), 0, 0, -5);
        assertPart(root.getChild("torso"), 0, 0, -2.5F);
        assertRotation(root.getChild("torso"), .1745329F, 0, 0);
        assertPart(root.getChild("arm_r"), -5, 3, -2);
        assertRotation(root.getChild("arm_r"), 0, 0, .1047198F);
        assertPart(root.getChild("arm_l"), 5, 3, -2);
        assertRotation(root.getChild("arm_l"), 0, 0, -.1047198F);
        // The original arms own their four shoulder pieces.
        ModelPart armR = root.getChild("arm_r");
        assertTrue(armR.hasChild("shoulder_r"));
        assertTrue(armR.hasChild("shoulder_r0"));
        assertTrue(armR.hasChild("shoulder_r1"));
        assertTrue(armR.hasChild("shoulder_r2"));
        ModelPart armL = root.getChild("arm_l");
        assertTrue(armL.hasChild("shoulder_l"));
        assertTrue(armL.hasChild("shoulder_l0"));
        assertTrue(armL.hasChild("shoulder_l1"));
        assertTrue(armL.hasChild("shoulder_l2"));
        assertRotation(armR.getChild("shoulder_r"), 0, 0, 1.186824F);
        assertRotation(armL.getChild("shoulder_l"), 0, 0, -1.186824F);
        assertPart(root.getChild("leg_r"), -2, 12.5F, 0);
        assertPart(root.getChild("leg_l"), 2, 12.5F, 0);
        assertPart(root.getChild("cloak1"), 0, 0, -2.5F);
        assertRotation(root.getChild("cloak1"), .1396263F, 0, 0);
        assertPart(root.getChild("front_cloth1"), -2, 12, 0);
        assertRotation(root.getChild("front_cloth1"), -.1047198F, 0, 0);
        assertPart(root.getChild("front_cloth2"), -2, 11, 0);
    }

    @Test void watcherKeepsTheOriginalSpinesAndTailChain() {
        ModelPart root = ModelWatcher.createBodyLayer().bakeRoot();
        ModelPart body = root.getChild("body");
        for (int i = 0; i < 12; i++) assertTrue(body.hasChild("spine" + i), "spine " + i);
        assertTrue(body.hasChild("eye"));
        assertTrue(body.hasChild("tail0"));
        ModelPart tail0 = body.getChild("tail0");
        assertTrue(tail0.hasChild("tail1"));
        assertTrue(tail0.getChild("tail1").hasChild("tail2"));
        assertPart(root.getChild("body"), 0, 0, 0);
    }

    @Test void taintSheepKeepsTheOriginalHeadAndLegOffsets() {
        ModelPart base = ModelTaintSheep2.createBodyLayer().bakeRoot();
        assertPart(base.getChild("head"), 0, 6, -8);
        assertPart(base.getChild("body"), 0, 5, 2);
        assertPart(base.getChild("right_front_leg"), -3, 12, 7);
        assertPart(base.getChild("left_front_leg"), 3, 12, 7);
        assertPart(base.getChild("right_hind_leg"), -3, 12, -5);
        assertPart(base.getChild("left_hind_leg"), 3, 12, -5);
        ModelPart wool = ModelTaintSheep1.createBodyLayer().bakeRoot();
        assertPart(wool.getChild("head"), 0, 6, -8);
        assertPart(wool.getChild("body"), 0, 5, 2);
        assertPart(wool.getChild("right_front_leg"), -3, 12, 7);
    }
}
