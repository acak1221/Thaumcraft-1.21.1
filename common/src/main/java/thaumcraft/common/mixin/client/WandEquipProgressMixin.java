package thaumcraft.common.mixin.client;

import net.minecraft.client.renderer.ItemInHandRenderer;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
import thaumcraft.client.renderers.item.WandRenderIdentity;

/** Keep the current wand reference before vanilla computes either hand's equip-height target. */
@Mixin(ItemInHandRenderer.class)
public abstract class WandEquipProgressMixin {
    @Redirect(method = "tick", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/item/ItemStack;matches(Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/item/ItemStack;)Z"),
            require = 2, allow = 2)
    private boolean thaumcraft$chargeDoesNotEquipAnotherWand(ItemStack cached, ItemStack current) {
        return WandRenderIdentity.matches(cached, current);
    }
}
