package thaumcraft.common.mixin.client;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.ItemInHandRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import thaumcraft.common.items.relics.ItemThaumometer;

/** The scanner supplies both arms; an empty main hand must not add a third arm. */
@Mixin(ItemInHandRenderer.class)
public abstract class ThaumometerEmptyHandArmMixin {
    @Shadow private ItemStack mainHandItem;
    @Shadow private ItemStack offHandItem;

    @Inject(method = "renderPlayerArm(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;IFFLnet/minecraft/world/entity/HumanoidArm;)V",
            at = @At("HEAD"), cancellable = true, require = 1, allow = 1)
    private void thaumcraft$scannerOwnsBothArms(PoseStack pose, MultiBufferSource buffers, int light,
                                               float equipped, float swing, HumanoidArm arm, CallbackInfo ci) {
        var player = Minecraft.getInstance().player;
        // The render cache owns this frame, including equip/fade transitions. A
        // cached main-hand map must keep its arm; a fading offhand scanner still
        // supplies both of its own arms after the inventory has already changed.
        if (player != null && arm == player.getMainArm() && mainHandItem.isEmpty()
                && offHandItem.getItem() instanceof ItemThaumometer) ci.cancel();
    }
}
