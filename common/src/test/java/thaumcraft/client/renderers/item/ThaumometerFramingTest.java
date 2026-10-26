package thaumcraft.client.renderers.item;

import static org.junit.jupiter.api.Assertions.*;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.renderer.block.model.ItemTransform;
import net.minecraft.world.item.ItemDisplayContext;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.junit.jupiter.api.Test;

/** Integrates the resource display, real native ItemTransform and original OBJ/screen geometry. */
class ThaumometerFramingTest {
    @Test void firstPersonRestoresTheOfficialOldCallerIncludingItsSwingOrder() throws Exception {
        for (float swing : new float[] {0, .25F, .75F}) {
            var actual = new PoseStack();
            actual.translate(.56F, -.52F, -.72F);
            swing(actual, swing, true);
            display(ItemDisplayContext.FIRST_PERSON_RIGHT_HAND).apply(false, actual);
            actual.translate(-.5F, -.5F, -.5F);
            ItemThaumometerRenderer.firstPersonBase(actual, false, 2, -3, .75F);
            ItemThaumometerRenderer.firstPersonBody(actual, false);

            // Official 1.7.10 client e80d9b3... bly.a(float): RY45, swing rotations,
            // S0.4, then pinned Forge EQUIPPED_BLOCK's T(-0.5), then TC4 :99-135.
            var legacy = new PoseStack();
            legacy.translate(.56F, -.52F, -.72F);
            swing(legacy, swing, false);
            legacy.scale(.4F, .4F, .4F);
            legacy.translate(-.5F, -.5F, -.5F);
            oracleFirstPerson(legacy, 2, -3, .75F);
            matrixEquals(legacy.last().pose(), actual.last().pose());
        }
    }

    @Test void eitherPhysicalHandKeepsTheFaceCentredAndTheReadoutRightWayRound() throws Exception {
        Matrix4f right = firstPerson(false);
        Matrix4f left = firstPerson(true);
        for (Matrix4f body : List.of(right, left)) {
            Vector3f centre = body.transformPosition(new Vector3f(0, .11F, 0));
            assertTrue(Math.abs(centre.x) < .02F && Math.abs(centre.y) < .06F,
                    "the instrument face remains at the optical axis: " + centre);
            assertEquals(-1.8696854F, centre.z, 1E-5F);
            assertTrue(body.determinant3x3() > 0, "a hand swap must retain a proper model basis");
            Matrix4f screen = new Matrix4f(body).translate(0, .11F, 0)
                    .rotateX((float) (Math.PI / 2)).rotateZ((float) (Math.PI / 2));
            Vector3f across = screen.transformDirection(new Vector3f(1, 0, 0));
            Vector3f down = screen.transformDirection(new Vector3f(0, 1, 0));
            assertTrue(across.x > .79F && Math.abs(across.y) < 1E-5F,
                    "text advances towards the right, including in the left hand: " + across);
            assertTrue(down.y < -.79F && Math.abs(down.x) < 1E-5F,
                    "the native font's positive Y advances down the face: " + down);
            for (Vector3f vertex : geometry()) {
                body.transformPosition(vertex);
                double halfHeight = -vertex.z * Math.tan(Math.toRadians(70 / 2.0));
                assertTrue(Math.abs(vertex.x) < halfHeight * 16 / 9 && Math.abs(vertex.y) < halfHeight,
                        "the full scanner fits the normal 16:9 first-person view: " + vertex);
            }
        }
        Vector3f rc = right.transformPosition(new Vector3f(0, .11F, 0));
        Vector3f lc = left.transformPosition(new Vector3f(0, .11F, 0));
        assertEquals(-rc.x, lc.x, 1E-5F);
        assertEquals(rc.y, lc.y, 1E-5F);
        assertEquals(rc.z, lc.z, 1E-5F);
    }

    @Test void guiMatchesTheLegacyHelperInPixelsAndFitsTheActualSlot() throws Exception {
        var nativePose = new PoseStack();
        nativePose.translate(8, 8, 0);
        nativePose.scale(16, -16, 16);
        display(ItemDisplayContext.GUI).apply(false, nativePose);
        nativePose.translate(-.5F, -.5F, -.5F);
        ItemThaumometerRenderer.staticPose(nativePose, ItemDisplayContext.GUI);

        // Pinned ForgeHooksClient.renderInventoryItem, INVENTORY_BLOCK true.
        var legacy = new PoseStack();
        legacy.translate(-2, 3, 0);
        legacy.scale(10, 10, 10);
        legacy.translate(1, .5F, 1);
        legacy.scale(1, 1, -1);
        legacy.mulPose(Axis.XP.rotationDegrees(210));
        legacy.mulPose(Axis.YP.rotationDegrees(45));
        legacy.mulPose(Axis.YP.rotationDegrees(-90));
        oracleInventory(legacy);

        float minX = Float.POSITIVE_INFINITY, minY = minX;
        float maxX = Float.NEGATIVE_INFINITY, maxY = maxX;
        for (Vector3f vertex : geometry()) {
            Vector3f actual = nativePose.last().pose().transformPosition(new Vector3f(vertex));
            Vector3f expected = legacy.last().pose().transformPosition(new Vector3f(vertex));
            assertEquals(expected.x, actual.x, 1E-4F, "original inventory helper X in pixels");
            assertEquals(expected.y, actual.y, 1E-4F, "original inventory helper Y in pixels");
            minX = Math.min(minX, actual.x); maxX = Math.max(maxX, actual.x);
            minY = Math.min(minY, actual.y); maxY = Math.max(maxY, actual.y);
        }
        assertTrue(minX >= 0 && minY >= 0 && maxX <= 16 && maxY <= 16,
                "body and screen fit inside one 16x16 slot: " + minX + "," + minY + ".." + maxX + "," + maxY);
        assertEquals(8, (minX + maxX) / 2, .2F);
        assertEquals(8, (minY + maxY) / 2, .2F);
    }

    @Test void thirdPersonMirrorsAroundTheNativeCentreWithoutInvertingTheBody() throws Exception {
        var right = new PoseStack();
        display(ItemDisplayContext.THIRD_PERSON_RIGHT_HAND).apply(false, right);
        right.translate(-.5F, -.5F, -.5F);
        ItemThaumometerRenderer.staticPose(right, ItemDisplayContext.THIRD_PERSON_RIGHT_HAND);
        var left = new PoseStack();
        display(ItemDisplayContext.THIRD_PERSON_LEFT_HAND).apply(true, left);
        left.translate(-.5F, -.5F, -.5F);
        ItemThaumometerRenderer.staticPose(left, ItemDisplayContext.THIRD_PERSON_LEFT_HAND);
        var expected = new Matrix4f().scaling(-1, 1, 1).mul(right.last().pose()).scale(1, 1, -1);
        matrixEquals(expected, left.last().pose());
        assertTrue(left.last().pose().determinant3x3() > 0);
    }

    private static Matrix4f firstPerson(boolean left) throws Exception {
        var context = left ? ItemDisplayContext.FIRST_PERSON_LEFT_HAND : ItemDisplayContext.FIRST_PERSON_RIGHT_HAND;
        var pose = new PoseStack();
        pose.translate(left ? -.56F : .56F, -.52F, -.72F);
        display(context).apply(left, pose);
        pose.translate(-.5F, -.5F, -.5F);
        ItemThaumometerRenderer.firstPersonBase(pose, left, 0, 0, 1);
        ItemThaumometerRenderer.firstPersonBody(pose, left);
        return new Matrix4f(pose.last().pose());
    }

    private static void swing(PoseStack pose, float time, boolean modern) {
        pose.mulPose(Axis.YP.rotationDegrees(45));
        pose.mulPose(Axis.YP.rotationDegrees(-(float) Math.sin(time * time * Math.PI) * 20));
        pose.mulPose(Axis.ZP.rotationDegrees(-(float) Math.sin(Math.sqrt(time) * Math.PI) * 20));
        pose.mulPose(Axis.XP.rotationDegrees(-(float) Math.sin(Math.sqrt(time) * Math.PI) * 80));
        if (modern) pose.mulPose(Axis.YP.rotationDegrees(-45));
    }

    private static void oracleFirstPerson(PoseStack pose, float pitchLag, float yawLag, float equipped) {
        pose.translate(1, .75F, -1);
        pose.mulPose(Axis.YP.rotationDegrees(-135));
        pose.mulPose(Axis.XP.rotationDegrees(pitchLag));
        pose.mulPose(Axis.YP.rotationDegrees(yawLag));
        pose.translate(-.56F, .52F + (1 - equipped) * 1.5F, .72F);
        pose.mulPose(Axis.YP.rotationDegrees(90));
        pose.translate(0, 0, -.72F);
        pose.mulPose(Axis.YP.rotationDegrees(90));
        pose.mulPose(Axis.ZP.rotationDegrees(90));
        pose.translate(.4F, -.4F, 0);
        pose.scale(2, 2, 2);
    }

    private static void oracleInventory(PoseStack pose) {
        pose.scale(.5F, .5F, .5F);
        pose.mulPose(Axis.XP.rotationDegrees(60));
        pose.mulPose(Axis.ZP.rotationDegrees(-30));
        pose.mulPose(Axis.YP.rotationDegrees(-248));
    }

    private static ItemTransform display(ItemDisplayContext context) throws Exception {
        try (var reader = reader("assets/thaumcraft/models/item/thaumometer.json")) {
            JsonObject display = JsonParser.parseReader(reader).getAsJsonObject().getAsJsonObject("display")
                    .getAsJsonObject(context.getSerializedName());
            return new ItemTransform(vector(display.getAsJsonArray("rotation")),
                    vector(display.getAsJsonArray("translation")).div(16), vector(display.getAsJsonArray("scale")));
        }
    }

    private static List<Vector3f> geometry() throws Exception {
        var vertices = new ArrayList<Vector3f>();
        try (var reader = reader("assets/thaumcraft/textures/models/scanner.obj")) {
            for (String line; (line = reader.readLine()) != null;) {
                if (!line.startsWith("v ")) continue;
                String[] parts = line.trim().split("\\s+");
                vertices.add(new Vector3f(Float.parseFloat(parts[1]), Float.parseFloat(parts[2]), Float.parseFloat(parts[3])));
            }
        }
        assertEquals(24, vertices.size(), "the oracle OBJ's actual position vertices");
        // Screen quad after T(0,.11,0)*RX90*RZ90*S2.5, as drawn by the renderer.
        for (float x : new float[] {-1.25F, 1.25F})
            for (float z : new float[] {-1.25F, 1.25F}) vertices.add(new Vector3f(x, .11F, z));
        return vertices;
    }

    private static BufferedReader reader(String resource) {
        var stream = ThaumometerFramingTest.class.getClassLoader().getResourceAsStream(resource);
        assertNotNull(stream, resource);
        return new BufferedReader(new InputStreamReader(stream, StandardCharsets.UTF_8));
    }

    private static Vector3f vector(JsonArray array) {
        return new Vector3f(array.get(0).getAsFloat(), array.get(1).getAsFloat(), array.get(2).getAsFloat());
    }

    private static void matrixEquals(Matrix4f expected, Matrix4f actual) {
        for (int column = 0; column < 4; column++)
            for (int row = 0; row < 4; row++)
                assertEquals(expected.get(column, row), actual.get(column, row), 1E-5F, column + "," + row);
    }
}
