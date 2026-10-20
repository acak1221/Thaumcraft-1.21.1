package thaumcraft.common.mixin.client;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import thaumcraft.client.renderers.item.PrimalWandFocusRenderer;

/** Mapped1.21.1 ItemRenderer.render has one final popPose after the selected body model; keep its actual display transform. */
@Mixin(ItemRenderer.class)
public abstract class PrimalWandItemRendererMixin {
    @Inject(method = "render(Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/item/ItemDisplayContext;ZLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;IILnet/minecraft/client/resources/model/BakedModel;)V",
            at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/vertex/PoseStack;popPose()V", shift = At.Shift.BEFORE))
    private void thaumcraft$primalFocus(ItemStack stack, ItemDisplayContext context, boolean leftHand, PoseStack pose,
                                      MultiBufferSource buffers, int light, int overlay, BakedModel model, CallbackInfo callback) {
        PrimalWandFocusRenderer.render(stack, pose, buffers, light, overlay);
    }
}
