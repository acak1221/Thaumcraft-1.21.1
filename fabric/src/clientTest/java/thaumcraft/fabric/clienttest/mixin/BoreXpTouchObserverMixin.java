package thaumcraft.fabric.clienttest.mixin;

import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import thaumcraft.fabric.clienttest.BoreClientSequence;

@Mixin(ExperienceOrb.class)
public abstract class BoreXpTouchObserverMixin {
    @Inject(method = "playerTouch(Lnet/minecraft/world/entity/player/Player;)V", at = @At("HEAD"), require = 1, allow = 1)
    private void thaumcraft$boreXpTouch(Player player, CallbackInfo ci) {
        BoreClientSequence.observeXpTouch((ExperienceOrb)(Object)this, player);
    }
}
