package thaumcraft.common.mixin.client;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.layers.VillagerProfessionLayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.npc.Villager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import thaumcraft.client.renderers.entity.LegacyVillagerSkins;

@Mixin(VillagerProfessionLayer.class)
public abstract class VillagerProfessionLayerMixin {
    @Inject(method = "render(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;ILnet/minecraft/world/entity/LivingEntity;FFFFFF)V",
            at = @At("HEAD"), cancellable = true)
    private void thaumcraft$preserveOriginalFullSkin(PoseStack poses, MultiBufferSource buffers, int light,
            LivingEntity entity, float limbSwing, float limbSwingAmount, float partialTick, float age,
            float headYaw, float headPitch, CallbackInfo callback) {
        if (entity instanceof Villager villager
                && LegacyVillagerSkins.texture(villager.getVillagerData().getProfession()) != null) callback.cancel();
    }
}
