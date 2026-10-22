package thaumcraft.client.renderers.item;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.world.item.ItemDisplayContext;
import org.joml.Matrix4f;
import org.junit.jupiter.api.Test;

/**
 * The context pose of the wand of the original.
 *
 * <p>Oracle {@code thaumcraft/client/renderers/item/ItemWandRenderer.java:59-86}: the renderer
 * places the wand differently for the four 1.7.10 item render types. A staff is lifted by half a
 * block, a dropped item by one block (one and a half and nine tenths for a staff), an item in a
 * hand by half a block to the side, one and a half up and half forward (the first person view
 * stretches it by a tenth in Y), and an item in the inventory is rolled by sixty six degrees
 * about Z, lifted by six tenths and shifted for a staff.</p>
 *
 * <p>The expected matrix of every case is composed again from the oracle rows - a translation,
 * the rotation and a scale, in the order of the original - and compared with the matrix that
 * {@link WandContextPose#apply} produces, so the order of the original is asserted and not only
 * its numbers.</p>
 *
 * <p>No test constructs a {@code Block}: every rule under test is a pure function of
 * {@link WandContextPose} and of a {@code PoseStack}, which is a plain matrix stack.</p>
 */
class WandContextPoseParityTest {
    private static Matrix4f applied(ItemDisplayContext context, boolean staff) {
        PoseStack pose = new PoseStack();
        WandContextPose.apply(pose, context, staff);
        return new Matrix4f(pose.last().pose());
    }

    private static final float DEG = (float) Math.PI / 180.0F;

    /** Original ItemWandRenderer:59-86, row by row, for a staff and for a wand. */
    private static Matrix4f expected(ItemDisplayContext context, boolean staff) {
        Matrix4f matrix = new Matrix4f();
        WandContextPose.Kind kind = WandContextPose.kindOf(context);
        if (kind == WandContextPose.Kind.NONE) return matrix;
        if (staff) matrix.translate(0.0F, 0.5F, 0.0F);                 // :59-61
        switch (kind) {
            case INVENTORY -> {                                          // :80-86
                if (staff) matrix.scale(0.8F, 0.8F, 0.8F);               // :82
                matrix.rotateZ(66.0F * DEG);                             // :84
                matrix.translate(0.0F, 0.6F, 0.0F);                      // :85
                if (staff) matrix.translate(-0.7F, 0.6F, 0.0F);          // :86
            }
            case ENTITY -> {                                             // :64-71
                if (staff) {
                    matrix.translate(0.0F, 1.5F, 0.0F);                  // :68
                    matrix.scale(0.9F, 0.9F, 0.9F);                      // :69
                } else matrix.translate(0.0F, 1.0F, 0.0F);               // :71
            }
            case EQUIPPED, EQUIPPED_FIRST_PERSON -> {                    // :72-79
                matrix.translate(0.5F, 1.5F, 0.5F);                      // :74-76
                if (kind == WandContextPose.Kind.EQUIPPED_FIRST_PERSON) matrix.scale(1.0F, 1.1F, 1.0F); // :77-78
            }
            case NONE -> { }
        }
        return matrix;
    }

    private static void assertMatrix(Matrix4f expected, Matrix4f actual, String message) {
        for (int column = 0; column < 4; column++) {
            for (int row = 0; row < 4; row++) {
                assertEquals(expected.get(column, row), actual.get(column, row), 1.0E-6F,
                        message + " at " + column + "," + row);
            }
        }
    }

    @Test void everyDisplayContextTakesTheBranchOfTheOriginal() {
        // Original ItemWandRenderer:64, :73 and :80.
        assertEquals(WandContextPose.Kind.INVENTORY, WandContextPose.kindOf(ItemDisplayContext.GUI));
        for (ItemDisplayContext context : new ItemDisplayContext[]{ItemDisplayContext.GROUND, ItemDisplayContext.FIXED, ItemDisplayContext.HEAD})
            assertEquals(WandContextPose.Kind.ENTITY, WandContextPose.kindOf(context), "the in-world branch of the original: " + context);
        for (ItemDisplayContext context : new ItemDisplayContext[]{ItemDisplayContext.THIRD_PERSON_RIGHT_HAND, ItemDisplayContext.THIRD_PERSON_LEFT_HAND})
            assertEquals(WandContextPose.Kind.EQUIPPED, WandContextPose.kindOf(context), "the third person branch of the original: " + context);
        for (ItemDisplayContext context : new ItemDisplayContext[]{ItemDisplayContext.FIRST_PERSON_RIGHT_HAND, ItemDisplayContext.FIRST_PERSON_LEFT_HAND})
            assertEquals(WandContextPose.Kind.EQUIPPED_FIRST_PERSON, WandContextPose.kindOf(context), "the first person branch of the original: " + context);
        assertEquals(WandContextPose.Kind.NONE, WandContextPose.kindOf(ItemDisplayContext.NONE));
    }

    @Test void theInventoryRollsTheWandBySixtySixDegreesAndLiftsIt() {
        // Original ItemWandRenderer:80-86. The staff scale precedes the roll, so the staff shift
        // is scaled with the wand; the lift is rotated by the roll, which is what the composed
        // matrix asserts.
        assertMatrix(expected(ItemDisplayContext.GUI, false), applied(ItemDisplayContext.GUI, false), "wand in the inventory");
        assertMatrix(expected(ItemDisplayContext.GUI, true), applied(ItemDisplayContext.GUI, true), "staff in the inventory");
        Matrix4f wand = applied(ItemDisplayContext.GUI, false);
        // JOML's rotateZ composes a right-handed rotation, so the m00/m10 pair of a +66 degree turn
        // reads back as -66 through atan2(m10, m00). The original's glRotatef(66, 0, 0, 1) and the
        // port's Axis.ZP.rotationDegrees(66) are the same matrix; only the sign convention of the
        // read-back differs. Assert the matrix pair, not a mis-signed angle.
        assertEquals(-66.0F * DEG, (float) Math.atan2(wand.m10(), wand.m00()), 1.0E-6F,
                "the inventory rolls the wand by sixty six degrees about Z (JOML right-handed read-back)");
        assertEquals(-0.6F * (float) Math.sin(66.0F * DEG), wand.m30(), 1.0E-6F,
                "the inventory lift is rotated by the roll, so it is applied after it (JOML right-handed X)");
        assertEquals(0.6F * (float) Math.cos(66.0F * DEG), wand.m31(), 1.0E-6F);
    }

    @Test void theDroppedItemIsLiftedByOneBlockAndTheStaffByOneAndAHalf() {
        // Original ItemWandRenderer:64-71.
        for (ItemDisplayContext context : new ItemDisplayContext[]{ItemDisplayContext.GROUND, ItemDisplayContext.FIXED}) {
            Matrix4f wand = applied(context, false);
            assertMatrix(expected(context, false), wand, "dropped wand " + context);
            assertEquals(0.0F, wand.m30(), 1.0E-6F);
            assertEquals(1.0F, wand.m31(), 1.0E-6F);
            assertEquals(1.0F, wand.m00(), 1.0E-6F);
            Matrix4f staff = applied(context, true);
            assertMatrix(expected(context, true), staff, "dropped staff " + context);
            assertEquals(0.5F + 1.5F, staff.m31(), 1.0E-6F, "the staff lift adds up to two blocks");
            assertEquals(0.9F, staff.m00(), 1.0E-6F, "a dropped staff is nine tenths of its size");
        }
    }

    @Test void theHandMovesTheWandAndTheFirstPersonStretchesIt() {
        // Original ItemWandRenderer:72-79.
        for (ItemDisplayContext context : new ItemDisplayContext[]{ItemDisplayContext.THIRD_PERSON_RIGHT_HAND, ItemDisplayContext.THIRD_PERSON_LEFT_HAND}) {
            assertMatrix(expected(context, false), applied(context, false), "third person wand " + context);
            assertMatrix(expected(context, true), applied(context, true), "third person staff " + context);
            Matrix4f wand = applied(context, false);
            assertEquals(0.5F, wand.m30(), 1.0E-6F);
            assertEquals(1.5F, wand.m31(), 1.0E-6F);
            assertEquals(0.5F, wand.m32(), 1.0E-6F);
            assertEquals(1.0F, wand.m11(), 1.0E-6F, "the third person view does not stretch the wand");
        }
        for (ItemDisplayContext context : new ItemDisplayContext[]{ItemDisplayContext.FIRST_PERSON_RIGHT_HAND, ItemDisplayContext.FIRST_PERSON_LEFT_HAND}) {
            assertMatrix(expected(context, false), applied(context, false), "first person wand " + context);
            assertMatrix(expected(context, true), applied(context, true), "first person staff " + context);
            Matrix4f wand = applied(context, false);
            assertEquals(1.1F, wand.m11(), 1.0E-6F, "the first person view stretches the wand by a tenth in Y only");
            assertEquals(1.0F, wand.m00(), 1.0E-6F);
            assertEquals(1.0F, wand.m22(), 1.0E-6F);
        }
    }

    @Test void theStaffLiftAppliesToEveryContextButTheOneTheOriginalNeverSaw() {
        // Original ItemWandRenderer:59-61 is the first row of the renderer, so the half block lift
        // of a staff is in every branch; NONE has no counterpart in the original at all.
        assertMatrix(new Matrix4f(), applied(ItemDisplayContext.NONE, true), "the original never rendered a contextless wand");
        assertMatrix(new Matrix4f(), applied(ItemDisplayContext.NONE, false), "the original never rendered a contextless wand");
        // The staff lift is the FIRST row, so its effect on the composed Y of the origin is not
        // simply +0.5 - the later rows scale and rotate it. What is invariant is that the staff is
        // lifted ABOVE the wand in every context the original rendered, and by the same matrix row.
        for (ItemDisplayContext context : ItemDisplayContext.values()) {
            if (context == ItemDisplayContext.NONE) continue;
            Matrix4f staff = applied(context, true);
            Matrix4f wand = applied(context, false);
            // The half block lift is applied before the branch, so it is scaled by whatever the
            // branch scales and rotated by whatever the branch rotates: comparing the origin's Y
            // alone would be wrong. The composed matrices differ, which is the real assertion.
            assertTrue(!staff.equals(wand), "a staff and a wand pose differently in " + context);
        }
        // The staff lift moves the builtin staff in the third person from one and a half to two,
        // because the third person branch has no scale and no rotation.
        assertEquals(2.0F, applied(ItemDisplayContext.THIRD_PERSON_RIGHT_HAND, true).m31(), 1.0E-6F);
        assertEquals(1.5F, applied(ItemDisplayContext.THIRD_PERSON_RIGHT_HAND, false).m31(), 1.0E-6F);
    }

    @Test void theConstantsAreTheOriginalOnes() {
        // Original ItemWandRenderer:60, :68, :69, :71, :74-76, :77-78, :82, :84, :85, :86.
        assertEquals(0.5F, WandContextPose.STAFF_LIFT_Y, 0.0F);
        assertEquals(1.5F, WandContextPose.ENTITY_STAFF_LIFT_Y, 0.0F);
        assertEquals(0.9F, WandContextPose.ENTITY_STAFF_SCALE, 0.0F);
        assertEquals(1.0F, WandContextPose.ENTITY_LIFT_Y, 0.0F);
        assertEquals(0.5F, WandContextPose.HAND_X, 0.0F);
        assertEquals(1.5F, WandContextPose.HAND_Y, 0.0F);
        assertEquals(0.5F, WandContextPose.HAND_Z, 0.0F);
        assertEquals(1.1F, WandContextPose.FIRST_PERSON_SCALE_Y, 0.0F);
        assertEquals(0.8F, WandContextPose.INVENTORY_STAFF_SCALE, 0.0F);
        assertEquals(66.0F, WandContextPose.INVENTORY_ROLL, 0.0F);
        assertEquals(0.6F, WandContextPose.INVENTORY_LIFT_Y, 0.0F);
        assertEquals(-0.7F, WandContextPose.INVENTORY_STAFF_X, 0.0F);
        assertEquals(0.6F, WandContextPose.INVENTORY_STAFF_Y, 0.0F);
        assertTrue(WandContextPose.INVENTORY_STAFF_X < 0.0F, "the staff shift of the original is negative in X");
    }
}
