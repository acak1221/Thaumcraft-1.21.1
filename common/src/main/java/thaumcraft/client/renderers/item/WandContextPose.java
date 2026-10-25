package thaumcraft.client.renderers.item;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.world.item.ItemDisplayContext;
import org.joml.Vector3f;
import thaumcraft.client.renderers.models.gear.ModelWand;

/**
 * The context pose of the wand of the original.
 *
 * <p>Original {@code thaumcraft/client/renderers/item/ItemWandRenderer.java:59-86}: the renderer
 * places the wand differently for every one of the four 1.7.10 item render types. A staff is
 * lifted by half a block first ({@code :60-62}); a dropped item (the original {@code ENTITY}
 * type, which covers both the dropped item and the item frame of the port) is lifted by one
 * block, or by one and a half blocks and shrunk to nine tenths in the case of a staff
 * ({@code :64-70}); an item in a hand is moved to half a block to the side, one and a half
 * blocks up and half a block forward, and the first person view stretches it vertically by a
 * tenth ({@code :72-75}); an item in the inventory is shrunk to eight tenths for a staff, rolled
 * by sixty six degrees about Z, lifted by six tenths and, for a staff, shifted by minus seven
 * tenths in X and another six tenths in Y ({@code :77-85}).</p>
 *
 * <p>The modern builtin entry has already translated by (-.5,-.5,-.5), but its caller no longer
 * supplies the old first-person Y rotation/0.4 scale or the Forge inventory helper's 10-pixel
 * projection. {@link #apply} adapts those caller bases first; {@link #applyLegacy} retains the
 * original local pose. Confusing those two bases made the wand enormous in hand and clipped
 * its inventory icon on both loaders. Shape JSON display transforms remain identity.</p>
 *
 * <p>Original local constants and the caller adaptation can be checked without GL; final visual
 * acceptance still requires a real client. The one-pixel GUI margin is an explicit modern fit.</p>
 */
public final class WandContextPose {
    /** Original ItemWandRenderer:61: the staff lift that applies to every context. */
    public static final float STAFF_LIFT_Y = 0.5F;
    /** Original ItemWandRenderer:66: the lift of a dropped staff. */
    public static final float ENTITY_STAFF_LIFT_Y = 1.5F;
    /** Original ItemWandRenderer:67: the uniform scale of a dropped staff. */
    public static final float ENTITY_STAFF_SCALE = 0.9F;
    /** Original ItemWandRenderer:69: the lift of a dropped wand. */
    public static final float ENTITY_LIFT_Y = 1.0F;
    /** Original ItemWandRenderer:72: the offset of the wand in a hand. */
    public static final float HAND_X = 0.5F, HAND_Y = 1.5F, HAND_Z = 0.5F;
    /** Original ItemWandRenderer:74: the vertical stretch of the first person view. */
    public static final float FIRST_PERSON_SCALE_Y = 1.1F;
    /** Original ItemWandRenderer:79: the uniform scale of a staff in the inventory. */
    public static final float INVENTORY_STAFF_SCALE = 0.8F;
    /** Original ItemWandRenderer:81: the roll of the inventory view around Z. */
    public static final float INVENTORY_ROLL = 66.0F;
    /** Original ItemWandRenderer:82: the lift of the inventory view. */
    public static final float INVENTORY_LIFT_Y = 0.6F;
    /** Original ItemWandRenderer:84: the shift of a staff in the inventory. */
    public static final float INVENTORY_STAFF_X = -0.7F, INVENTORY_STAFF_Y = 0.6F;

    /** The four item render types of the original, plus the contexts it never saw. */
    public enum Kind { NONE, INVENTORY, ENTITY, EQUIPPED, EQUIPPED_FIRST_PERSON }

    private static final GuiFrame WAND_GUI = guiFrame(false);
    private static final GuiFrame STAFF_GUI = guiFrame(true);

    private WandContextPose() {}

    /**
     * The branch of the original that a 1.21.1 display context takes.
     *
     * <p>The 1.7.10 renderer knew four types. {@code INVENTORY} is the inventory of the port,
     * {@code ENTITY} is the dropped item and the item frame of the port, {@code EQUIPPED} is the
     * third person hand and {@code EQUIPPED_FIRST_PERSON} is the first person hand. The port
     * additionally has a head slot and a {@code NONE} context; the original never rendered the
     * wand there, so the head takes the in-world branch of the dropped item and {@code NONE}
     * takes no context pose at all.</p>
     */
    public static Kind kindOf(ItemDisplayContext context) {
        return switch (context) {
            case GUI -> Kind.INVENTORY;
            case GROUND, FIXED, HEAD -> Kind.ENTITY;
            case THIRD_PERSON_RIGHT_HAND, THIRD_PERSON_LEFT_HAND -> Kind.EQUIPPED;
            case FIRST_PERSON_RIGHT_HAND, FIRST_PERSON_LEFT_HAND -> Kind.EQUIPPED_FIRST_PERSON;
            case NONE -> Kind.NONE;
        };
    }

    /**
     * Adapt the actual modern caller, then retain ItemWandRenderer:59-86's local pose.
     *
     * @param pose   the pose of the builtin item renderer (the model display transform and the
     *               centring have been applied)
     * @param context the display context of the frame
     * @param staff  whether the wand is a staff
     */
    public static void apply(PoseStack pose, ItemDisplayContext context, boolean staff) {
        switch (kindOf(context)) {
            case INVENTORY -> {
                pose.translate(.5, .5, .5);
                var frame = staff ? STAFF_GUI : WAND_GUI;
                pose.translate(-frame.x * frame.scale, -frame.y * frame.scale, 0);
                pose.scale(frame.scale, frame.scale, frame.scale);
                guiOrientation(pose);
            }
            case EQUIPPED_FIRST_PERSON -> {
                // Old ItemRenderer (bly): the same hand translation, followed by Ry45/S.4.
                // Forge's EQUIPPED_BLOCK helper then supplied the old -.5 translation.
                pose.translate(.5, .5, .5);
                pose.mulPose(Axis.YP.rotationDegrees(leftHand(context) ? -45 : 45));
                pose.scale(.4F, .4F, .4F);
                pose.translate(-.5, -.5, -.5);
            }
            case EQUIPPED -> {
                // Undo ItemInHandLayer's post-arm basis; retain the shared model-arm pose.
                pose.translate(.5, .5, .5);
                pose.translate(leftHand(context) ? 1F / 16 : -1F / 16, -.125, .625);
                pose.mulPose(Axis.YP.rotationDegrees(-180));
                pose.mulPose(Axis.XP.rotationDegrees(90));
                // Original full-3D RenderPlayer path, mirrored for the modern left hand.
                pose.translate(leftHand(context) ? 1F / 16 : -1F / 16, .625, 1F / 16);
                pose.scale(.625F, -.625F, .625F);
                pose.mulPose(Axis.XP.rotationDegrees(-100));
                pose.mulPose(Axis.YP.rotationDegrees(leftHand(context) ? -45 : 45));
                pose.translate(-.5, -.5, -.5);
            }
            case ENTITY -> {
                // TC4 declines BLOCK_3D; Forge scaled this custom entity renderer by .5.
                pose.translate(.5, .5, .5);
                pose.scale(.5F, .5F, .5F);
            }
            case NONE -> { }
        }
        applyLegacy(pose, context, staff);
    }

    /** Original local rows, kept separate from the loader/caller adaptation. */
    static void applyLegacy(PoseStack pose, ItemDisplayContext context, boolean staff) {
        Kind kind = kindOf(context);
        if (kind == Kind.NONE) return;
        // Original ItemWandRenderer:59-61.
        if (staff) pose.translate(0, STAFF_LIFT_Y, 0);
        switch (kind) {
            case INVENTORY -> {
                // Original ItemWandRenderer:80-86.
                if (staff) pose.scale(INVENTORY_STAFF_SCALE, INVENTORY_STAFF_SCALE, INVENTORY_STAFF_SCALE);
                pose.mulPose(Axis.ZP.rotationDegrees(INVENTORY_ROLL));
                pose.translate(0, INVENTORY_LIFT_Y, 0);
                if (staff) pose.translate(INVENTORY_STAFF_X, INVENTORY_STAFF_Y, 0);
            }
            case ENTITY -> {
                // Original ItemWandRenderer:64-71.
                if (staff) {
                    pose.translate(0, ENTITY_STAFF_LIFT_Y, 0);
                    pose.scale(ENTITY_STAFF_SCALE, ENTITY_STAFF_SCALE, ENTITY_STAFF_SCALE);
                } else pose.translate(0, ENTITY_LIFT_Y, 0);
            }
            case EQUIPPED, EQUIPPED_FIRST_PERSON -> {
                // Original ItemWandRenderer:72-79.
                pose.translate(HAND_X, HAND_Y, HAND_Z);
                if (kind == Kind.EQUIPPED_FIRST_PERSON) pose.scale(1, FIRST_PERSON_SCALE_Y, 1);
            }
            case NONE -> { }
        }
    }

    /** The left hand mirrors yaw and use-roll; geometry, pitch and elapsed-use timing stay shared. */
    public static boolean leftHand(ItemDisplayContext context) {
        return context == ItemDisplayContext.FIRST_PERSON_LEFT_HAND || context == ItemDisplayContext.THIRD_PERSON_LEFT_HAND;
    }

    private static void guiOrientation(PoseStack pose) {
        // Forge INVENTORY_BLOCK: S(1,1,-1) Rx210 Ry45 Ry-90. Native GUI flips Y,
        // so its equivalent before that flip is Rx30 Ry-45; 10/16 is the old scale.
        pose.mulPose(Axis.XP.rotationDegrees(30));
        pose.mulPose(Axis.YP.rotationDegrees(-45));
    }

    private record GuiFrame(float x, float y, float scale) {}

    private static GuiFrame guiFrame(boolean staff) {
        // Conservative envelope of the retained ModelWand parts, including a focus and sceptre.
        // All variants share one frame, so fitting/removing a focus never moves the icon.
        float halfWidth = Math.max(3F / 16 * (staff ? ModelWand.Geometry.STAFF_FOCUS_XZ : ModelWand.Geometry.WAND_FOCUS),
                1F / 16 * (staff ? ModelWand.Geometry.STAFF_CAP_XZ : ModelWand.Geometry.WAND_CAP_XZ)
                        * ModelWand.Geometry.SCEPTRE_CAP_SCALE);
        float minY = staff ? .2F + ModelWand.Geometry.STAFF_FOCUS_SHIFT - 6F / 16 * ModelWand.Geometry.STAFF_FOCUS_Y : -3F / 16;
        float maxY = staff ? .2F + (ModelWand.Geometry.STAFF_RING_SHIFT + ModelWand.Geometry.STAFF_BOTTOM_SHIFT + 21F / 16)
                * ModelWand.Geometry.STAFF_CAP_Y : 21F / 16;
        var reference = new PoseStack();
        guiOrientation(reference);
        applyLegacy(reference, ItemDisplayContext.GUI, staff);
        reference.mulPose(Axis.XP.rotationDegrees(180));
        float x0 = Float.POSITIVE_INFINITY, y0 = Float.POSITIVE_INFINITY;
        float x1 = Float.NEGATIVE_INFINITY, y1 = Float.NEGATIVE_INFINITY;
        for (float x : new float[]{-halfWidth, halfWidth}) for (float y : new float[]{minY, maxY})
            for (float z : new float[]{-halfWidth, halfWidth}) {
                var point = reference.last().pose().transformPosition(new Vector3f(x, y, z));
                x0 = Math.min(x0, point.x); x1 = Math.max(x1, point.x);
                y0 = Math.min(y0, point.y); y1 = Math.max(y1, point.y);
            }
        // Keep a pixel around the complete original geometry in a native 16-pixel slot.
        float scale = Math.min(10F / 16, (14F / 16) / Math.max(x1 - x0, y1 - y0));
        return new GuiFrame((x0 + x1) / 2, (y0 + y1) / 2, scale);
    }
}
