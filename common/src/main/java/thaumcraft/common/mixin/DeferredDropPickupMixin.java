package thaumcraft.common.mixin;

import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import thaumcraft.common.lib.utils.DeferredItemDrops;

@Mixin(ItemEntity.class)
public abstract class DeferredDropPickupMixin {
    @Inject(method = "playerTouch", at = @At("HEAD"), cancellable = true)
    private void thaumcraft$guardUnadmittedOutputPickup(Player player, CallbackInfo ci) {
        if (!DeferredItemDrops.allowPlayerTouch((ItemEntity) (Object) this)) ci.cancel();
    }
}
