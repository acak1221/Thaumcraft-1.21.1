package thaumcraft.client.renderers.item;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.world.item.ItemDisplayContext;

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
 * <p>The basis of the chain is the basis that the 1.21.1 {@code ItemRenderer} hands to a builtin
 * item renderer: the display transform of the model has been applied and the model has been
 * centred with {@code translate(-0.5, -0.5, -0.5)} ({@code net/minecraft/client/renderer/entity/ItemRenderer.java},
 * the {@code ItemTransform.apply} / {@code translate(-0.5f)} / {@code renderByItem} sequence of
 * {@code render}). That is the very basis of the 1.7.10 custom item renderer: one unit is one
 * block and the origin is the centre of the item, with Y upwards, in the hand, in the inventory
 * and in the world. The model is drawn in this basis with the model basis correction of
 * {@link ThaumcraftGearModelsClient#renderWand} applied afterwards, exactly like the original.</p>
 *
 * <p>Every number of this class is a constant of the oracle, so the chain can be asserted without
 * a running client.</p>
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
     * Original ItemWandRenderer:59-86: the context pose, without the model basis correction.
     *
     * @param pose   the pose of the builtin item renderer (the model display transform and the
     *               centring have been applied)
     * @param context the display context of the frame
     * @param staff  whether the wand is a staff
     */
    public static void apply(PoseStack pose, ItemDisplayContext context, boolean staff) {
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
}
