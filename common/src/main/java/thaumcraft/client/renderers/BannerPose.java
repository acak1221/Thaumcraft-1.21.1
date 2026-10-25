package thaumcraft.client.renderers;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.world.item.ItemDisplayContext;

/** Matrix operations from original ItemBannerRenderer and TileBannerRenderer. */
public final class BannerPose {
    private BannerPose() {}

    public static void item(PoseStack pose, ItemDisplayContext context) {
        // Native slot framing, using the resting model's actual bounds after the original chain.
        // It must be independent of context: compensating the equipped translation cancels it.
        pose.translate(.5F, .5F, .5F);
        pose.scale(.5F, .5F, .5F);
        pose.translate(.5F, .515625F, .546875F);
        // Original ItemBannerRenderer bytecode32-73: the turn is about Y, not Z.
        if (context == ItemDisplayContext.FIRST_PERSON_LEFT_HAND
                || context == ItemDisplayContext.FIRST_PERSON_RIGHT_HAND
                || context == ItemDisplayContext.THIRD_PERSON_LEFT_HAND
                || context == ItemDisplayContext.THIRD_PERSON_RIGHT_HAND) {
            pose.translate(1, 1, 1);
        }
        pose.translate(-.5F, -.5F, -.5F);
        pose.mulPose(Axis.YP.rotationDegrees(180));
        pose.translate(-.5F, -1, -.5F);
    }

    public static void model(PoseStack pose, int originalFacing, boolean hasWorld) {
        // Original TileBannerRenderer bytecode57-132.
        pose.translate(.5F, 1.5F, .5F);
        pose.mulPose(Axis.XP.rotationDegrees(180));
        if (hasWorld) {
            pose.mulPose(Axis.YP.rotationDegrees(180));
            pose.mulPose(Axis.YP.rotationDegrees(originalFacing * 360F / 16F));
        }
    }

    public static void aspect(PoseStack pose, float wave) {
        // Original TileBannerRenderer bytecode321-359: turn over about Y before following the cloth.
        pose.translate(0, 0, .05001F);
        pose.scale(.0375F, .0375F, .0375F);
        pose.mulPose(Axis.YP.rotationDegrees(180));
        pose.mulPose(Axis.XP.rotationDegrees(-wave * 57.295776F * 2F));
    }
}
