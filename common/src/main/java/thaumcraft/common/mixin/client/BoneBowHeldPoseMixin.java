package thaumcraft.common.mixin.client;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.ArmedModel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.layers.ItemInHandLayer;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import thaumcraft.client.renderers.item.BoneBowPose;
import thaumcraft.common.items.equipment.ItemBowBone;

/**
 * Oracle ItemBowBoneRenderer: the held bone bow replaces the native equip pose. It starts at the
 * hand of the arm like the original; see {@link BoneBowPose}.
 */
@Mixin(ItemInHandLayer.class)
public abstract class BoneBowHeldPoseMixin {
    @Inject(method = "renderArmWithItem", at = @At("HEAD"), cancellable = true)
    private void thaumcraft$boneBow(LivingEntity entity, ItemStack stack, ItemDisplayContext context,
            HumanoidArm arm, PoseStack pose, MultiBufferSource buffers, int light, CallbackInfo ci) {
        if (!(stack.getItem() instanceof ItemBowBone)) return;
        if (!(((ItemInHandLayer<?, ?>) (Object) this).getParentModel() instanceof ArmedModel model)) return;
        pose.pushPose();
        try {
            model.translateToHand(arm, pose);
            BoneBowPose.apply(pose, arm == HumanoidArm.LEFT);
            // Old renderItemIn2D occupies x/y 0..1 and z -1/16..0. Native generated
            // quads are centred by ItemRenderer, so restore the old icon origin.
            pose.translate(.5F, .5F, -.03125F);
            try (var glint = thaumcraft.client.renderers.item.BoneBowGlint.buffers(pose)) {
                Minecraft.getInstance().getItemRenderer().renderStatic(entity, stack, ItemDisplayContext.NONE,
                        false, pose, glint.source(), entity.level(), light, net.minecraft.client.renderer.texture.OverlayTexture.NO_OVERLAY, entity.getId());
            }
        } finally { pose.popPose(); }
        ci.cancel();
    }
}
