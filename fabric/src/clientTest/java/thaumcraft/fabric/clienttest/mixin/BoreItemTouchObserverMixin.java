package thaumcraft.fabric.clienttest.mixin;

import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import thaumcraft.fabric.clienttest.BoreClientSequence;

@Mixin(ItemEntity.class)
public abstract class BoreItemTouchObserverMixin {
    @Inject(method = "playerTouch(Lnet/minecraft/world/entity/player/Player;)V", at = @At("HEAD"), require = 1, allow = 1)
    private void thaumcraft$boreTouchHead(Player player, CallbackInfo ci) {
        BoreClientSequence.observeItemTouchHead((ItemEntity)(Object)this, player);
    }
    @Inject(method = "playerTouch(Lnet/minecraft/world/entity/player/Player;)V", at = @At("RETURN"), require = 1)
    private void thaumcraft$boreTouchReturn(Player player, CallbackInfo ci) {
        BoreClientSequence.observeItemTouchReturn((ItemEntity)(Object)this, player);
    }
}
