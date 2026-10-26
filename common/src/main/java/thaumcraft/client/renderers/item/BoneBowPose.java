package thaumcraft.client.renderers.item;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;

/**
 * Oracle ItemBowBoneRenderer:48-64,82-88: the held bone bow, from the hand.
 *
 * <p>The original renderer is called inside {@code RenderBiped/RenderPlayer.renderEquippedItems}:
 * the arm ({@code postRender}, the native {@code translateToHand}), the hand offset
 * {@code (-1/16, 7/16, 1/16)} and the branch of a flat item. Its {@code glPopMatrix/glPushMatrix}
 * only drops the push of {@code ForgeHooksClient}; it then undoes that flat item branch exactly
 * ({@link #undoFlatItemBranch}) and applies the vanilla bow branch and the 2D item transform of
 * {@code ItemRenderer}. The native layer never applies the flat item branch, so neither it nor its
 * inverse is applied here: the pose starts at the hand.</p>
 */
public final class BoneBowPose {
    public static final float OUTER_SCALE = 2.6666667F, INNER_SCALE = .625F, ICON_SCALE = 1.5F;
    public static final float OUTER_ROLL = -20, OUTER_PITCH = 90, SECOND_ROLL = -60;
    private BoneBowPose() {}

    /** The pose after the right arm: the original hand offset, the bow branch and the 2D item transform. */
    public static void apply(PoseStack pose) { apply(pose, false); }

    /**
     * The pose after the arm. The original drew the right hand only; for the left hand the
     * sideways hand offset is mirrored, as the native layer mirrors its own.
     */
    public static void apply(PoseStack pose, boolean leftArm) {
        // RenderBiped/RenderPlayer.renderEquippedItems: the hand of the arm.
        pose.translate(leftArm ? .0625F : -.0625F, .4375F, .0625F);
        // Original :57-63, the vanilla bow branch.
        pose.translate(0, .125F, .3125F);
        pose.mulPose(Axis.YP.rotationDegrees(-20));
        pose.scale(INNER_SCALE, -INNER_SCALE, INNER_SCALE);
        pose.mulPose(Axis.XP.rotationDegrees(-100));
        pose.mulPose(Axis.YP.rotationDegrees(45));
        // Original :82-88, the 2D item transform of ItemRenderer.
        pose.translate(0, -.3F, 0);
        pose.scale(ICON_SCALE, ICON_SCALE, ICON_SCALE);
        pose.mulPose(Axis.YP.rotationDegrees(50));
        pose.mulPose(Axis.ZP.rotationDegrees(335));
        pose.translate(-.9375F, -.0625F, 0);
    }

    /** The flat item branch of 1.7.10 renderEquippedItems the original renderer received. */
    public static void flatItemBranch(PoseStack pose) {
        pose.translate(.25F, .1875F, -.1875F);
        pose.scale(.375F, .375F, .375F);
        pose.mulPose(Axis.ZP.rotationDegrees(60));
        pose.mulPose(Axis.XP.rotationDegrees(-90));
        pose.mulPose(Axis.ZP.rotationDegrees(20));
    }

    /** Original :50-56, which cancels {@link #flatItemBranch} exactly. */
    public static void undoFlatItemBranch(PoseStack pose) {
        pose.mulPose(Axis.ZP.rotationDegrees(OUTER_ROLL));
        pose.mulPose(Axis.XP.rotationDegrees(OUTER_PITCH));
        pose.mulPose(Axis.ZP.rotationDegrees(SECOND_ROLL));
        pose.scale(OUTER_SCALE, OUTER_SCALE, OUTER_SCALE);
        pose.translate(-.25F, -.1875F, .1875F);
    }
}
