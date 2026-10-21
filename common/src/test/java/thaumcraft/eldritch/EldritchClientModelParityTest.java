package thaumcraft.eldritch;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.model.geom.ModelPart;
import org.junit.jupiter.api.Test;
import thaumcraft.client.renderers.models.ModelEldritchGuardian;
import thaumcraft.client.renderers.models.entities.ModelEldritchCrab;
import thaumcraft.client.renderers.models.entities.ModelEldritchGolem;

/**
 * The geometry and the animation of the three eldritch entity models of the
 * original TC4 4.2.3.5, checked value by value against the oracle.
 *
 * <p>The oracle is the decompiled jar:
 * {@code thaumcraft/client/renderers/models/entities/ModelEldritchCrab.java},
 * {@code ModelEldritchGolem.java} and {@code ModelEldritchGuardian.java}.
 */
class EldritchClientModelParityTest {
    private static final float EPS = 1.0E-5F;

    // ------------------------------------------------------------- the crab

    @Test void theCrabKeepsTheTwentyOnePartsOfTheOriginal() {
        ModelPart root = ModelEldritchCrab.createBodyLayer().bakeRoot();
        for (String part : new String[]{"tail_helm", "tail_bare", "r_claw1", "head1", "r_claw0", "r_claw2",
                "l_claw2", "l_claw1", "r_arm", "torso", "head0", "rr_leg1", "rf_leg1", "lr_leg1", "lf_leg1",
                "rr_leg0", "rf_leg0", "lf_leg0", "lr_leg0", "l_claw0", "l_arm"}) {
            assertTrue(root.hasChild(part), "the original crab has the part " + part);
        }
        assertEquals(21, parts(root), "the original crab has twenty-one parts");
    }

    @Test void theCrabKeepsTheOriginalRotationPoints() {
        ModelPart root = ModelEldritchCrab.createBodyLayer().bakeRoot();
        // Original ModelEldritchCrab:45-116 - the rotation points of the parts.
        assertPoint(root.getChild("tail_helm"), 0.0F, 18.0F, 0.0F);
        assertPoint(root.getChild("r_claw1"), -6.0F, 15.5F, -10.0F);
        assertPoint(root.getChild("l_claw1"), 6.0F, 15.5F, -10.0F);
        assertPoint(root.getChild("r_claw0"), -6.0F, 17.0F, -7.0F);
        assertPoint(root.getChild("l_claw0"), 6.0F, 17.0F, -7.0F);
        assertPoint(root.getChild("r_claw2"), -6.0F, 18.5F, -10.0F);
        assertPoint(root.getChild("l_claw2"), 6.0F, 18.5F, -10.0F);
        assertPoint(root.getChild("r_arm"), -3.0F, 17.0F, -4.0F);
        assertPoint(root.getChild("l_arm"), 4.0F, 17.0F, -5.0F);
        assertPoint(root.getChild("torso"), 0.0F, 18.0F, 0.0F);
        assertPoint(root.getChild("rr_leg1"), -4.0F, 20.0F, -1.5F);
        assertPoint(root.getChild("rf_leg1"), -4.0F, 20.0F, -3.5F);
        assertPoint(root.getChild("lr_leg1"), 4.0F, 20.0F, -1.5F);
        assertPoint(root.getChild("lf_leg1"), 4.0F, 20.0F, -3.5F);
        // Original ModelEldritchCrab:48 and :52 and :65 and :89 - the rest rotations.
        assertEquals(.1047198F, root.getChild("tail_helm").xRot, EPS, "tail helm");
        assertEquals(.3141593F, root.getChild("r_claw2").xRot, EPS, "right claw two");
        assertEquals(.3141593F, root.getChild("l_claw2").xRot, EPS, "left claw two");
        assertEquals(.7504916F, root.getChild("r_arm").yRot, EPS, "right arm");
        assertEquals(-.7504916F, root.getChild("l_arm").yRot, EPS, "left arm");
        assertEquals(.0523599F, root.getChild("torso").xRot, EPS, "torso");
    }

    @Test void theCrabSwingsItsLegsWithTheOriginalCosine() {
        // Original ModelEldritchCrab:163-164 - the swing of the legs is
        // -(cos(limb * 0.6662 * 2) * 0.4) * amount and not a sine.
        for (float limb : new float[]{0.0F, 0.5F, 1.0F, 3.0F, 7.5F}) {
            float expected = -(float) (Math.cos(limb * .6662F * 2.0F) * .4F) * 1.0F;
            assertEquals(expected, ModelEldritchCrab.legSwing(limb, 1.0F), EPS, "the right legs at " + limb);
            float expectedHalf = -(float) (Math.cos(limb * .6662F * 2.0F + Math.PI) * .4F) * 1.0F;
            assertEquals(expectedHalf, ModelEldritchCrab.legSwingHalfPhase(limb, 1.0F), EPS, "the left legs at " + limb);
            float expectedTail = (float) (Math.cos(limb * .6662F) * 2.0F * .125F);
            assertEquals(expectedTail, ModelEldritchCrab.tailSway(limb, 1.0F), EPS, "the tail at " + limb);
            float expectedTilt = (float) (Math.cos(limb * .6662F) * .125F);
            assertEquals(expectedTilt, ModelEldritchCrab.tailSwayTilt(limb, 1.0F), EPS, "the tail tilt at " + limb);
        }
        // The sine and the cosine differ: the legs of the crab start at their rest
        // angle when the phase is zero.
        assertEquals(-.4F, ModelEldritchCrab.legSwing(0.0F, 1.0F), EPS, "the rest angle of the right legs");
        assertEquals(.4F, ModelEldritchCrab.legSwingHalfPhase(0.0F, 1.0F), EPS, "the rest angle of the left legs");
    }

    // ------------------------------------------------------------- the golem

    @Test void theGolemKeepsTheThirtySixPartsOfTheOriginal() {
        ModelPart root = ModelEldritchGolem.createBodyLayer().bakeRoot();
        for (String part : new String[]{"cloak1", "cloak2", "cloak3", "cloak_cl", "cloak_cr", "head", "head2",
                "collar_l", "collar_r", "collar_b", "collar_f", "collar_black", "front_cloth0", "front_cloth1",
                "front_cloth2", "torso", "arm_r", "arm_l", "back_panel_r1", "back_panel_l1", "waist_r1", "waist_r2",
                "waist_r3", "waist_l1", "waist_l2", "waist_l3", "leg_r", "leg_l"}) {
            assertTrue(root.hasChild(part), "the original golem has the part " + part);
        }
        assertEquals(36, parts(root), "the original golem has thirty-six parts");
    }

    @Test void theGolemKeepsTheOriginalRotationPoints() {
        ModelPart root = ModelEldritchGolem.createBodyLayer().bakeRoot();
        // Original ModelEldritchGolem:97-253 - the rotation points of the parts.
        assertPoint(root.getChild("head"), 0.0F, 4.5F, -3.8F);
        assertPoint(root.getChild("arm_r"), -5.0F, 3.0F, -2.0F);
        assertPoint(root.getChild("arm_l"), 5.0F, 3.0F, -2.0F);
        assertPoint(root.getChild("leg_r"), -2.0F, 12.5F, 0.0F);
        assertPoint(root.getChild("leg_l"), 2.0F, 12.5F, 0.0F);
        assertPoint(root.getChild("waist_r1"), -2.0F, 12.0F, 0.0F);
        assertPoint(root.getChild("waist_l3"), 2.0F, 12.0F, 0.0F);
        assertEquals(.1047198F, root.getChild("arm_r").zRot, EPS, "the right arm");
        assertEquals(-.1047198F, root.getChild("arm_l").zRot, EPS, "the left arm");
        assertEquals(.1396263F, root.getChild("waist_r1").zRot, EPS, "the right waist");
        assertEquals(-.1396263F, root.getChild("waist_l1").zRot, EPS, "the left waist");
    }

    @Test void theGolemSwingsWithTheOriginalCosine() {
        // Original ModelEldritchGolem:259-260, :312-313 and :323-324 - the golem
        // swings with MathHelper.cos (MCP func_76134_b) and not with a sine.
        for (float limb : new float[]{0.0F, 0.5F, 1.0F, 3.0F, 7.5F}) {
            assertEquals((float) (Math.cos(limb * .4F) * 2.0F * .5F),
                    ModelEldritchGolem.armSwing(limb, 1.0F), EPS, "the left arm at " + limb);
            assertEquals((float) (Math.cos(limb * .4F + Math.PI) * 2.0F * .5F),
                    ModelEldritchGolem.armSwingHalfPhase(limb, 1.0F), EPS, "the right arm at " + limb);
            assertEquals((float) (Math.cos(limb * .4662F) * 1.4F),
                    ModelEldritchGolem.legSwing(limb, 1.0F), EPS, "the right leg at " + limb);
            assertEquals((float) (Math.cos(limb * .4662F + Math.PI) * 1.4F),
                    ModelEldritchGolem.legSwingHalfPhase(limb, 1.0F), EPS, "the left leg at " + limb);
            assertEquals((float) (Math.cos(limb * .44F) * 1.4F),
                    ModelEldritchGolem.clothSwing(limb, 1.0F), EPS, "the cloth at " + limb);
            assertEquals((float) (Math.cos(limb * .44F + Math.PI) * 1.4F),
                    ModelEldritchGolem.clothSwingHalfPhase(limb, 1.0F), EPS, "the second cloth at " + limb);
        }
        // The sine and the cosine differ: the arms of the golem start at their rest
        // angle when the phase is zero.
        assertEquals(1.0F, ModelEldritchGolem.armSwing(0.0F, 1.0F), EPS, "the rest angle of the left arm");
        assertEquals(-1.0F, ModelEldritchGolem.armSwingHalfPhase(0.0F, 1.0F), EPS, "the rest angle of the right arm");
    }

    // ---------------------------------------------------------- the guardian

    @Test void theGuardianKeepsTheFiftyFourPartsOfTheOriginal() {
        ModelPart root = ModelEldritchGuardian.createBodyLayer().bakeRoot();
        for (String part : new String[]{"BeltR", "Mbelt", "MbeltL", "MbeltR", "BeltL", "HoodEye", "Hood1",
                "Hood2", "Hood3", "Hood4", "Backplate", "ShoulderplateTopR", "ShoulderplateR1", "ShoulderplateR2",
                "ShoulderplateR3", "ShoulderR", "ArmL1", "ArmL2", "ArmL3", "ArmR1", "ArmR2", "ArmR3",
                "ShoulderL", "ShoulderplateLtop", "ShoulderplateL1", "ShoulderplateL2", "ShoulderplateL3",
                "LegpanelR4", "LegpanelR5", "LegpanelR6", "BackpanelR1", "BackpanelR2", "BackpanelR3",
                "BackpanelL3", "LegpanelL4", "LegpanelL5", "LegpanelL6", "BackpanelL1", "BackpanelL2",
                "SidepanelL1", "SidepanelR1", "SidepanelR2", "SidepanelR3", "SidepanelR4", "SidepanelL2",
                "SidepanelL3", "SidepanelL4", "LegpanelC1", "LegpanelC2", "LegpanelC3", "Cloak1", "Cloak2",
                "Cloak3"}) {
            assertTrue(root.hasChild(part), "the original guardian has the part " + part);
        }
        assertEquals(54, parts(root), "the original guardian has fifty-four parts");
    }

    @Test void theGuardianKeepsTheOriginalRotationPoints() {
        ModelPart root = ModelEldritchGuardian.createBodyLayer().bakeRoot();
        // Original ModelEldritchGuardian:220-225 and :396-411 and :412-419.
        assertPoint(root.getChild("ShoulderL"), 5.0F, -4.0F, 0.0F);
        assertPoint(root.getChild("ShoulderR"), -5.0F, -4.0F, 0.0F);
        assertPoint(root.getChild("Cloak1"), -5.0F, -6.0F, 4.0F);
        assertPoint(root.getChild("Hood1"), 0.0F, -6.0F, 0.0F);
        assertPoint(root.getChild("LegpanelC1"), 0.0F, 5.5F, -3.0F);
    }

    /**
     * Original ModelEldritchGuardian:220-222 - the mirror flag of the left shoulder
     * plate is set before its box, so the original really mirrored that box. Every
     * other mirror flag of the original comes after its box and never had an effect.
     */
    @Test void theGuardianMirrorsOnlyTheLeftShoulderPlate() {
        ModelPart root = ModelEldritchGuardian.createBodyLayer().bakeRoot();
        List<float[]> left = uvsOf(root.getChild("ShoulderL"));
        List<float[]> right = uvsOf(root.getChild("ShoulderR"));
        assertEquals(24, right.size(), "a box of the original has twenty-four corners");
        assertEquals(24, left.size(), "a box of the original has twenty-four corners");
        // The two boxes share their size and their texture offset, so the mirror only
        // turns the u of every corner around inside the face.
        float[] leftU = new float[left.size()];
        float[] rightU = new float[right.size()];
        for (int index = 0; index < left.size(); index++) {
            leftU[index] = left.get(index)[0];
            rightU[index] = right.get(index)[0];
        }
        float[] sortedLeft = leftU.clone();
        float[] sortedRight = rightU.clone();
        java.util.Arrays.sort(sortedLeft);
        java.util.Arrays.sort(sortedRight);
        for (int index = 0; index < sortedLeft.length; index++) {
            assertEquals(sortedRight[index], sortedLeft[index], 1.0E-4F,
                    "the mirrored box keeps the same texture coordinates at the corner " + index);
        }
        boolean same = true;
        for (int index = 0; index < leftU.length; index++) {
            if (Math.abs(leftU[index] - rightU[index]) > 1.0E-4F) { same = false; break; }
        }
        assertTrue(!same, "the original mirrored the left shoulder plate");
    }

    // ------------------------------------------------------------------ tools

    /** The number of the parts of a model without its own root. */
    private static long parts(ModelPart root) {
        return root.getAllParts().filter(part -> part != root).count();
    }

    private static void assertPoint(ModelPart part, float x, float y, float z) {
        assertEquals(x, part.x, EPS, "the x of " + part);
        assertEquals(y, part.y, EPS, "the y of " + part);
        assertEquals(z, part.z, EPS, "the z of " + part);
    }

    /** The texture coordinates that a part would draw. */
    private static List<float[]> uvsOf(ModelPart part) {
        Recorder recorder = new Recorder();
        PoseStack pose = new PoseStack();
        part.render(pose, recorder, 15728640, 0, 0xffffffff);
        return recorder.uvs;
    }

    /** A vertex consumer that only remembers the texture coordinates. */
    private static final class Recorder implements VertexConsumer {
        private final List<float[]> uvs = new ArrayList<>();

        @Override public VertexConsumer addVertex(float x, float y, float z) { return this; }
        @Override public VertexConsumer setColor(int r, int g, int b, int a) { return this; }
        @Override public VertexConsumer setUv(float u, float v) { uvs.add(new float[]{u, v}); return this; }
        @Override public VertexConsumer setUv1(int u, int v) { return this; }
        @Override public VertexConsumer setUv2(int u, int v) { return this; }
        @Override public VertexConsumer setNormal(float x, float y, float z) { return this; }
    }
}
