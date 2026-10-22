package thaumcraft.client.renderers.item;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;

/** Oracle ItemBowBoneRenderer:48-64,82-88; replaces, rather than appends to, the equip matrix. */
public final class BoneBowPose {
    public static final float OUTER_SCALE = 2.6666667F, INNER_SCALE = .625F, ICON_SCALE = 1.5F;
    public static final float OUTER_ROLL = -20, OUTER_PITCH = 90, SECOND_ROLL = -60;
    private BoneBowPose() {}
    public static void apply(PoseStack pose) {
        pose.mulPose(Axis.ZP.rotationDegrees(OUTER_ROLL));
        pose.mulPose(Axis.XP.rotationDegrees(OUTER_PITCH));
        pose.mulPose(Axis.ZP.rotationDegrees(SECOND_ROLL));
        pose.scale(OUTER_SCALE, OUTER_SCALE, OUTER_SCALE);
        pose.translate(-.25F, -.1875F, .1875F);
        pose.translate(0, .125F, .3125F);
        pose.mulPose(Axis.YP.rotationDegrees(-20));
        pose.scale(INNER_SCALE, -INNER_SCALE, INNER_SCALE);
        pose.mulPose(Axis.XP.rotationDegrees(-100));
        pose.mulPose(Axis.YP.rotationDegrees(45));
        pose.translate(0, -.3F, 0);
        pose.scale(ICON_SCALE, ICON_SCALE, ICON_SCALE);
        pose.mulPose(Axis.YP.rotationDegrees(50));
        pose.mulPose(Axis.ZP.rotationDegrees(335));
        pose.translate(-.9375F, -.0625F, 0);
    }
}
