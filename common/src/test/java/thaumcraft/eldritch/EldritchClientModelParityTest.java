package thaumcraft.eldritch;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
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
 * {@code ModelEldritchGolem.java} and {@code ModelEldritchGuardian.java}.</p>
 *
 * <p>The cosine of the original is the lookup table of the 1.7.10 {@code MathHelper}
 * (MCP {@code func_76134_b}), not {@link Math#cos(double)}: the original swung the eldritch
 * mobs with a sine table of 65536 entries, and the native 1.21.1 {@code Mth.cos} is that
 * same table. The expectations below therefore use {@link #originalCos(float)}.</p>
 */
class EldritchClientModelParityTest {
    private static final float EPS = 1.0E-5F;

    /**
     * The sine table of the original 1.7.10 {@code MathHelper} (the static block of the class
     * builds 65536 values of {@code (float) Math.sin(index * Math.PI * 2.0 / 65536.0)}).
     */
    private static final float[] ORIGINAL_SINE = new float[65536];

    static {
        for (int index = 0; index < 65536; index++) {
            ORIGINAL_SINE[index] = (float) Math.sin(index * Math.PI * 2.0D / 65536.0D);
        }
    }

    /**
     * Original 1.7.10 {@code MathHelper.func_76134_b} - the cosine of the original, a lookup
     * in the sine table above and not {@link Math#cos(double)}.
     */
    private static float originalCos(float value) {
        return ORIGINAL_SINE[(int) (value * 10430.378F + 16384.0F) & 0xFFFF];
    }

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
        // -(MathHelper.cos(limb * 0.6662f * 2.0f) * 0.4f) * amount and not a sine, and the
        // cosine of the original is the lookup table of the 1.7.10 MathHelper.
        for (float limb : new float[]{0.0F, 0.5F, 1.0F, 3.0F, 7.5F}) {
            float expected = -(originalCos(limb * .6662F * 2F) * .4F) * 1.0F;
            assertEquals(expected, ModelEldritchCrab.legSwing(limb, 1.0F), EPS, "the right legs at " + limb);
            float expectedHalf = -(originalCos(limb * .6662F * 2F + (float) Math.PI) * .4F) * 1.0F;
            assertEquals(expectedHalf, ModelEldritchCrab.legSwingHalfPhase(limb, 1.0F), EPS, "the left legs at " + limb);
            float expectedTail = originalCos(limb * .6662F) * 2F * 1.0F * .125F;
            assertEquals(expectedTail, ModelEldritchCrab.tailSway(limb, 1.0F), EPS, "the tail at " + limb);
            float expectedTilt = originalCos(limb * .6662F) * 1.0F * .125F;
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
        // swings with MathHelper.cos (MCP func_76134_b), the lookup table of the
        // 1.7.10 MathHelper, and not with a sine.
        for (float limb : new float[]{0.0F, 0.5F, 1.0F, 3.0F, 7.5F}) {
            assertEquals(originalCos(limb * .4F) * 2.0F * 1.0F * .5F,
                    ModelEldritchGolem.armSwing(limb, 1.0F), EPS, "the left arm at " + limb);
            assertEquals(originalCos(limb * .4F + (float) Math.PI) * 2.0F * 1.0F * .5F,
                    ModelEldritchGolem.armSwingHalfPhase(limb, 1.0F), EPS, "the right arm at " + limb);
            assertEquals(originalCos(limb * .4662F) * 1.4F * 1.0F,
                    ModelEldritchGolem.legSwing(limb, 1.0F), EPS, "the right leg at " + limb);
            assertEquals(originalCos(limb * .4662F + (float) Math.PI) * 1.4F * 1.0F,
                    ModelEldritchGolem.legSwingHalfPhase(limb, 1.0F), EPS, "the left leg at " + limb);
            assertEquals(originalCos(limb * .44F) * 1.4F * 1.0F,
                    ModelEldritchGolem.clothSwing(limb, 1.0F), EPS, "the cloth at " + limb);
            assertEquals(originalCos(limb * .44F + (float) Math.PI) * 1.4F * 1.0F,
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
        // The original built fifty-four ModelRenderers and hung thirteen of them under a
        // parent (oracle ModelEldritchGuardian:149-151, :202-203, :218-219 and :412-419):
        // the hood parts under the hood, the arm parts under the arms, the leg panels under
        // the centre panel, the side panels under the side panels and the cloak under the
        // cloak. The paths below are the original's hierarchy.
        for (String path : new String[]{"BeltR", "Mbelt", "MbeltL", "MbeltR", "BeltL", "Chestplate",
                "HoodEye", "Hood1", "Hood1/Hood2", "Hood1/Hood3", "Hood1/Hood4", "Backplate",
                "ShoulderplateTopR", "ShoulderplateR1", "ShoulderplateR2", "ShoulderplateR3", "ShoulderR",
                "ArmL1", "ArmL1/ArmL2", "ArmL1/ArmL3", "ArmR1", "ArmR1/ArmR2", "ArmR1/ArmR3", "ShoulderL",
                "ShoulderplateLtop", "ShoulderplateL1", "ShoulderplateL2", "ShoulderplateL3", "LegpanelR4",
                "LegpanelR5", "LegpanelR6", "BackpanelR1", "BackpanelR2", "BackpanelR3", "BackpanelL3",
                "LegpanelL4", "LegpanelL5", "LegpanelL6", "BackpanelL1", "BackpanelL2", "SidepanelL1",
                "SidepanelR1", "SidepanelR2", "SidepanelR2/SidepanelR3", "SidepanelR2/SidepanelR3/SidepanelR4",
                "SidepanelL2", "SidepanelL2/SidepanelL3", "SidepanelL2/SidepanelL3/SidepanelL4", "LegpanelC1",
                "LegpanelC1/LegpanelC2", "LegpanelC1/LegpanelC2/LegpanelC3", "Cloak1", "Cloak1/Cloak2",
                "Cloak1/Cloak2/Cloak3"}) {
            assertNotNull(part(root, path), "the original guardian has the part " + path);
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
     *
     * <p>The 1.7.10 {@code ModelBox} reads the flag of its {@code ModelRenderer} while it
     * is built, swaps the two x edges of the box and reverses the vertex order of every
     * face; the native 1.21.1 {@code ModelPart.Cube} does the same for a mirrored
     * {@code CubeListBuilder}. The test therefore compares the two plates of the model
     * with the same boxes built with and without the mirror flag.</p>
     */
    @Test void theGuardianMirrorsOnlyTheLeftShoulderPlate() {
        ModelPart root = ModelEldritchGuardian.createBodyLayer().bakeRoot();
        ModelPart left = root.getChild("ShoulderL");
        ModelPart right = root.getChild("ShoulderR");
        // The original left plate: the mirror flag before the box (oracle :221-222).
        assertArrayEquals(cornersOf(shoulderPlate(left, -1.5F, true)), cornersOf(left), 1.0E-6F,
                "the original mirrored the left shoulder plate");
        // The original right plate: the mirror flag after the box (oracle :182-186).
        assertArrayEquals(cornersOf(shoulderPlate(right, -3.5F, false)), cornersOf(right), 1.0E-6F,
                "the original did not mirror the right shoulder plate");
        // The mirror really changes the box, so the two expectations are not the same.
        assertFalse(java.util.Arrays.equals(cornersOf(shoulderPlate(left, -1.5F, true)),
                cornersOf(shoulderPlate(left, -1.5F, false))),
                "the mirror of the original changes the box of the left shoulder plate");
    }

    // ------------------------------------------------------------------ tools

    /** The part of the model at the given path, or null when the path does not exist. */
    private static ModelPart part(ModelPart root, String path) {
        ModelPart current = root;
        for (String name : path.split("/")) {
            if (!current.hasChild(name)) return null;
            current = current.getChild(name);
        }
        return current;
    }

    /** The number of the parts of a model without its own root. */
    private static long parts(ModelPart root) {
        return root.getAllParts().filter(part -> part != root).count();
    }

    /**
     * The original shoulder plate with the box of the oracle, built with or without the
     * mirror flag and with the pose of the part of the port.
     */
    private static ModelPart shoulderPlate(ModelPart part, float originX, boolean mirror) {
        var builder = CubeListBuilder.create().texOffs(56, 35);
        if (mirror) builder.mirror();
        var mesh = new MeshDefinition();
        mesh.getRoot().addOrReplaceChild("plate", builder.addBox(originX, -2.5F, -2.5F, 5, 5, 5),
                PartPose.offsetAndRotation(part.x, part.y, part.z, part.xRot, part.yRot, part.zRot));
        return LayerDefinition.create(mesh, 128, 64).bakeRoot().getChild("plate");
    }

    private static void assertPoint(ModelPart part, float x, float y, float z) {
        assertEquals(x, part.x, EPS, "the x of " + part);
        assertEquals(y, part.y, EPS, "the y of " + part);
        assertEquals(z, part.z, EPS, "the z of " + part);
    }

    /** The corners that a part would draw: the position and the texture coordinates. */
    private static float[] cornersOf(ModelPart part) {
        Recorder recorder = new Recorder();
        PoseStack pose = new PoseStack();
        part.render(pose, recorder, 15728640, 0, 0xffffffff);
        float[] corners = new float[recorder.corners.size() * 5];
        for (int index = 0; index < recorder.corners.size(); index++) {
            System.arraycopy(recorder.corners.get(index), 0, corners, index * 5, 5);
        }
        return corners;
    }

    /** A vertex consumer that only remembers the positions and the texture coordinates. */
    private static final class Recorder implements VertexConsumer {
        private final List<float[]> corners = new ArrayList<>();
        private float x, y, z;

        @Override public VertexConsumer addVertex(float x, float y, float z) { this.x = x; this.y = y; this.z = z; return this; }
        @Override public VertexConsumer setColor(int r, int g, int b, int a) { return this; }
        @Override public VertexConsumer setUv(float u, float v) { corners.add(new float[]{x, y, z, u, v}); return this; }
        @Override public VertexConsumer setUv1(int u, int v) { return this; }
        @Override public VertexConsumer setUv2(int u, int v) { return this; }
        @Override public VertexConsumer setNormal(float x, float y, float z) { return this; }
    }
}
