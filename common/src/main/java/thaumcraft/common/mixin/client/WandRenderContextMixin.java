package thaumcraft.common.mixin.client;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import thaumcraft.client.renderers.item.WandRenderContext;

/** Oracle ItemWandRenderer:51-56 passes the real wielder, not Minecraft.player. */
@Mixin(ItemRenderer.class)
public abstract class WandRenderContextMixin {
    @Inject(method = "renderStatic(Lnet/minecraft/world/entity/LivingEntity;Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/item/ItemDisplayContext;ZLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;Lnet/minecraft/world/level/Level;III)V", at = @At("HEAD"))
    private void thaumcraft$pushWielder(LivingEntity entity, ItemStack stack, ItemDisplayContext context, boolean left,
            PoseStack pose, MultiBufferSource buffers, Level level, int light, int overlay, int seed, CallbackInfo ci) {
        WandRenderContext.push(entity);
    }
    @Inject(method = "renderStatic(Lnet/minecraft/world/entity/LivingEntity;Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/item/ItemDisplayContext;ZLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;Lnet/minecraft/world/level/Level;III)V", at = @At("RETURN"))
    private void thaumcraft$popWielder(LivingEntity entity, ItemStack stack, ItemDisplayContext context, boolean left,
            PoseStack pose, MultiBufferSource buffers, Level level, int light, int overlay, int seed, CallbackInfo ci) {
        WandRenderContext.pop();
    }
}
