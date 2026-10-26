package thaumcraft.client.renderers.item;

import static org.junit.jupiter.api.Assertions.*;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.world.item.ItemDisplayContext;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.joml.Vector4f;
import org.junit.jupiter.api.Test;
import thaumcraft.client.renderers.models.gear.ModelWand;

/** Composes real caller bases with recorded ModelWand vertices, rather than testing local offsets alone. */
class WandViewportTest {
    @Test void firstPersonRestAndSwingRestoreTheOfficialOldCallerScaleAndYawForBothHands() {
        for (boolean staff : List.of(false, true)) for (boolean left : List.of(false, true))
            for (float swing : new float[]{0, .3F, .8F}) {
                var context = left ? ItemDisplayContext.FIRST_PERSON_LEFT_HAND : ItemDisplayContext.FIRST_PERSON_RIGHT_HAND;
                var nativeCaller = firstPersonCaller(left, swing);
                var actual = new PoseStack();
                actual.mulPose(nativeCaller);
                actual.translate(-.5, -.5, -.5);
                WandContextPose.apply(actual, context, staff);
                actual.mulPose(Axis.XP.rotationDegrees(180));
                // Official old ItemRenderer bly: RY45/S.4 preceded Forge's -.5 helper.
                var expected = new Matrix4f(nativeCaller).rotateY((float) Math.toRadians(left ? -45 : 45))
                        .scale(.4F).translate(-.5F, -.5F, -.5F).mul(legacyHand(staff, true))
                        .rotateX((float) Math.PI);
                assertMatrix(expected, actual.last().pose(), context + "/staff=" + staff + "/swing=" + swing);
            }
    }

    @Test void allRecordedWandStaffSceptreAndFocusPartsFitInsideTheNativeSlotWithAMargin() {
        for (boolean staff : List.of(false, true)) for (boolean sceptre : List.of(false, true))
            for (boolean focus : List.of(false, true)) {
                var pose = new PoseStack();
                // Actual GuiGraphics caller: center a 16-pixel slot, invert GUI Y, then ItemRenderer's -.5.
                pose.translate(8, 8, 0);
                pose.scale(16, -16, 16);
                pose.translate(-.5, -.5, -.5);
                WandContextPose.apply(pose, ItemDisplayContext.GUI, staff);
                pose.mulPose(Axis.XP.rotationDegrees(180));
                var points = geometry(staff, sceptre, focus).stream()
                        .map(point -> pose.last().pose().transformPosition(new Vector3f(point))).toList();
                float x0 = Float.POSITIVE_INFINITY, y0 = Float.POSITIVE_INFINITY;
                float x1 = Float.NEGATIVE_INFINITY, y1 = Float.NEGATIVE_INFINITY;
                for (var point : points) {
                    assertTrue(point.x >= 1 - .0001F && point.x <= 15 + .0001F, "slot X: " + point);
                    assertTrue(point.y >= 1 - .0001F && point.y <= 15 + .0001F, "slot Y: " + point);
                    x0 = Math.min(x0, point.x); x1 = Math.max(x1, point.x);
                    y0 = Math.min(y0, point.y); y1 = Math.max(y1, point.y);
                }
                assertTrue(Math.max(x1 - x0, y1 - y0) >= 9, "fitting must retain a readable icon, not shrink it to a dot");
            }
    }

    @Test void theFocusAndTopCapStayInTheRestingFirstPersonViewportInEitherHand() {
        for (boolean staff : List.of(false, true)) for (boolean sceptre : List.of(false, true))
            for (boolean left : List.of(false, true)) for (float aspect : new float[]{4F / 3, 16F / 9}) {
                var context = left ? ItemDisplayContext.FIRST_PERSON_LEFT_HAND : ItemDisplayContext.FIRST_PERSON_RIGHT_HAND;
                var pose = new PoseStack();
                pose.mulPose(firstPersonCaller(left, 0));
                pose.translate(-.5, -.5, -.5);
                WandContextPose.apply(pose, context, staff);
                pose.mulPose(Axis.XP.rotationDegrees(180));
                var projection = new Matrix4f().perspective((float) Math.toRadians(70), aspect, .05F, 100);
                var clip = projection.mul(pose.last().pose());
                // A staff's long tail legitimately extends below the hand viewport; its tip must remain visible.
                for (var point : geometry(staff, sceptre, true)) {
                    if (point.y > .55F) continue;
                    var projected = clip.transform(new Vector4f(point, 1));
                    assertTrue(projected.w > 0, "tip remains in front of the camera");
                    assertTrue(projected.z >= -projected.w && projected.z <= projected.w, "tip remains between near and far planes");
                    assertTrue(Math.abs(projected.x / projected.w) < 1, "tip horizontal clipping: " + projected);
                    assertTrue(Math.abs(projected.y / projected.w) < 1, "tip vertical clipping: " + projected);
                }
            }
    }

    @Test void activeUseKeepsItsOriginalPivotAndMirrorsTheActualProductionMatrixForTheLeftHand() {
        for (boolean first : List.of(false, true)) for (boolean staff : List.of(false, true))
            for (int ticks : new int[]{0, 1, 3, 25}) for (float wave : new float[]{-10, 0, 10}) {
                var rightContext = first ? ItemDisplayContext.FIRST_PERSON_RIGHT_HAND : ItemDisplayContext.THIRD_PERSON_RIGHT_HAND;
                var leftContext = first ? ItemDisplayContext.FIRST_PERSON_LEFT_HAND : ItemDisplayContext.THIRD_PERSON_LEFT_HAND;
                float elapsed = WandUsePose.useTicks(ticks, .25F);
                var local = new PoseStack();
                WandUsePose.apply(local, rightContext, elapsed, wave, -wave);
                var pivot = local.last().pose().transformPosition(new Vector3f(0, 1, 0));
                assertEquals(0, pivot.x, .00001F);
                assertEquals(1, pivot.y, .00001F);
                assertEquals(0, pivot.z, .00001F);
                // Independently compose the original right-hand order and angles around the pivot.
                var expected = new Matrix4f().translate(0, 1, 0);
                if (first) expected.rotateX((float) Math.toRadians(10));
                expected.rotateZ((float) Math.toRadians(first ? 10 : 33))
                        .rotateX((float) Math.toRadians(-60 * Math.min(ticks + .25F, 3) / 3))
                        .rotateZ((float) Math.toRadians(wave)).rotateX((float) Math.toRadians(-wave)).translate(0, -1, 0);
                assertMatrix(expected, local.last().pose(), "original right-hand use order");
                var right = heldPose(rightContext, staff);
                var left = heldPose(leftContext, staff);
                WandUsePose.apply(right, rightContext, elapsed, wave, -wave);
                WandUsePose.apply(left, leftContext, elapsed, wave, -wave);
                for (var point : geometry(staff, true, true)) {
                    var a = right.last().pose().transformPosition(new Vector3f(point));
                    var b = left.last().pose().transformPosition(new Vector3f(-point.x, point.y, point.z));
                    assertEquals(-a.x, b.x, .00001F, "left/right world X reflects around the shared camera/arm origin");
                    assertEquals(a.y, b.y, .00001F);
                    assertEquals(a.z, b.z, .00001F);
                }
            }
    }

    @Test void thirdPersonReplacesOnlyTheNativePostArmBasisWithTheOriginalFull3dBasis() {
        for (boolean staff : List.of(false, true)) for (boolean left : List.of(false, true)) {
            float sign = left ? -1 : 1;
            var context = left ? ItemDisplayContext.THIRD_PERSON_LEFT_HAND : ItemDisplayContext.THIRD_PERSON_RIGHT_HAND;
            var actual = new PoseStack();
            // Actual ItemInHandLayer after ArmedModel.translateToHand.
            actual.mulPose(Axis.XP.rotationDegrees(-90));
            actual.mulPose(Axis.YP.rotationDegrees(180));
            actual.translate(sign / 16, .125, -.625);
            actual.translate(-.5, -.5, -.5);
            WandContextPose.apply(actual, context, staff);
            actual.mulPose(Axis.XP.rotationDegrees(180));
            // Official old RenderPlayer bop, full-3D path; arm transform remains caller-owned.
            var expected = new Matrix4f().translate(-sign / 16, .625F, 1F / 16)
                    .scale(.625F, -.625F, .625F).rotateX((float) Math.toRadians(-100))
                    .rotateY((float) Math.toRadians(sign * 45)).translate(-.5F, -.5F, -.5F)
                    .mul(legacyHand(staff, false)).rotateX((float) Math.PI);
            assertMatrix(expected, actual.last().pose(), context + "/staff=" + staff);
        }
    }

    @Test void theGroundItemRestoresTheNonBlock3dForgeHelperScale() {
        for (boolean staff : List.of(false, true)) {
            var actual = new PoseStack();
            actual.translate(-.5, -.5, -.5);
            WandContextPose.apply(actual, ItemDisplayContext.GROUND, staff);
            var expected = new Matrix4f().scale(.5F).translate(0, staff ? 2 : 1, 0);
            if (staff) expected.scale(.9F);
            assertMatrix(expected, actual.last().pose(), "ground staff=" + staff);
        }
    }

    private static Matrix4f firstPersonCaller(boolean left, float swing) {
        float sign = left ? -1 : 1;
        float a = (float) Math.sin(swing * swing * Math.PI);
        float b = (float) Math.sin(Math.sqrt(swing) * Math.PI);
        // ItemInHandRenderer's swing translation, equipped hand base and attack rotations.
        float swingX = -.4F * b;
        float swingY = .2F * (float) Math.sin(Math.sqrt(swing) * 2 * Math.PI);
        float swingZ = -.2F * (float) Math.sin(swing * Math.PI);
        return new Matrix4f().translate(sign * (.56F + swingX), -.52F + swingY, -.72F + swingZ)
                .rotateY((float) Math.toRadians(sign * (45 - a * 20)))
                .rotateZ((float) Math.toRadians(sign * b * -20))
                .rotateX((float) Math.toRadians(b * -80))
                .rotateY((float) Math.toRadians(sign * -45));
    }

    private static PoseStack heldPose(ItemDisplayContext context, boolean staff) {
        var pose = new PoseStack();
        boolean left = WandContextPose.leftHand(context);
        if (context.firstPerson()) pose.mulPose(firstPersonCaller(left, 0));
        else {
            pose.mulPose(Axis.XP.rotationDegrees(-90));
            pose.mulPose(Axis.YP.rotationDegrees(180));
            pose.translate((left ? -1F : 1F) / 16, .125, -.625);
        }
        pose.translate(-.5, -.5, -.5);
        WandContextPose.apply(pose, context, staff);
        pose.mulPose(Axis.XP.rotationDegrees(180));
        return pose;
    }

    private static Matrix4f legacyHand(boolean staff, boolean firstPerson) {
        var result = new Matrix4f().translate(.5F, staff ? 2 : 1.5F, .5F);
        return firstPerson ? result.scale(1, 1.1F, 1) : result;
    }

    private static void assertMatrix(Matrix4f expected, Matrix4f actual, String message) {
        assertTrue(expected.equals(actual, .00001F), message + "\nexpected " + expected + "\nactual " + actual);
    }

    /** Record real ModelPart geometry with the literal per-piece transforms from oracle ModelWand. */
    private static List<Vector3f> geometry(boolean staff, boolean sceptre, boolean focus) {
        var model = new ModelWand();
        var vertices = new Recording();
        var pose = new PoseStack();
        if (staff) pose.translate(0, .2, 0);
        pose.pushPose();
        if (staff) { pose.translate(0, -.1, 0); pose.scale(1.2F, 2, 1.2F); }
        model.renderRod(pose, vertices, 0, 0);
        pose.popPose();
        pose.pushPose();
        pose.scale(staff ? 1.3F : 1.2F, staff ? 1.1F : 1, staff ? 1.3F : 1.2F);
        if (sceptre) {
            pose.pushPose(); pose.scale(1.3F, 1.3F, 1.3F); model.renderCap(pose, vertices, 0, 0); pose.popPose();
            pose.pushPose(); pose.translate(0, .3, 0); pose.scale(1, .66F, 1); model.renderCap(pose, vertices, 0, 0); pose.popPose();
        } else model.renderCap(pose, vertices, 0, 0);
        if (staff) {
            pose.translate(0, .225, 0);
            pose.pushPose(); pose.scale(1, .66F, 1); model.renderCap(pose, vertices, 0, 0); pose.popPose();
            pose.translate(0, .65, 0);
        }
        model.renderCapBottom(pose, vertices, 0, 0);
        pose.popPose();
        if (focus) {
            pose.pushPose();
            if (staff) { pose.translate(0, -.0475, 0); pose.scale(.525F, .5525F, .525F); }
            else pose.scale(.5F, .5F, .5F);
            model.renderFocus(pose, vertices, 0, 0, -1);
            pose.popPose();
        }
        return vertices.points;
    }

    private static final class Recording implements VertexConsumer {
        final List<Vector3f> points = new ArrayList<>();
        public VertexConsumer addVertex(float x, float y, float z) { points.add(new Vector3f(x, y, z)); return this; }
        public VertexConsumer setColor(int r, int g, int b, int a) { return this; }
        public VertexConsumer setUv(float u, float v) { return this; }
        public VertexConsumer setUv1(int u, int v) { return this; }
        public VertexConsumer setUv2(int u, int v) { return this; }
        public VertexConsumer setNormal(float x, float y, float z) { return this; }
    }
}
