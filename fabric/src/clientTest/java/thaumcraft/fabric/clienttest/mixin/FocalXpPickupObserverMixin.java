package thaumcraft.fabric.clienttest.mixin;

import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import thaumcraft.fabric.clienttest.FocalXpPickupObserver;

/** TEST-ONLY observer. No cancellation, argument/return/resource mutation, or
 * manually triggered pickup. Root registers only after staged promotion. */
@Mixin(ExperienceOrb.class)
public abstract class FocalXpPickupObserverMixin {
    @Inject(method="playerTouch(Lnet/minecraft/world/entity/player/Player;)V",at=@At("HEAD"),require=1,allow=1)
    private void thaumcraft$focalXpHead(Player player,CallbackInfo callback) {
        FocalXpPickupObserver.touchHead((ExperienceOrb)(Object)this,player);
    }
    @Inject(method="playerTouch(Lnet/minecraft/world/entity/player/Player;)V",at=@At("RETURN"),require=1)
    private void thaumcraft$focalXpReturn(Player player,CallbackInfo callback) {
        FocalXpPickupObserver.touchReturn((ExperienceOrb)(Object)this,player);
    }
}
