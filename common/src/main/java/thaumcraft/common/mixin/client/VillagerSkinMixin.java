package thaumcraft.common.mixin.client;

import net.minecraft.client.renderer.entity.VillagerRenderer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.npc.Villager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import thaumcraft.client.renderers.entity.LegacyVillagerSkins;

@Mixin(VillagerRenderer.class)
public abstract class VillagerSkinMixin {
    @Inject(method = "getTextureLocation(Lnet/minecraft/world/entity/npc/Villager;)Lnet/minecraft/resources/ResourceLocation;",
            at = @At("HEAD"), cancellable = true)
    private void thaumcraft$originalVillagerSkin(Villager villager, CallbackInfoReturnable<ResourceLocation> callback) {
        var texture = LegacyVillagerSkins.texture(villager.getVillagerData().getProfession());
        if (texture != null) callback.setReturnValue(texture);
    }
}
