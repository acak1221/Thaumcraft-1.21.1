package thaumcraft.common.mixin;

import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import thaumcraft.common.lib.utils.ExperienceOrbQueue;

/** An ADD candidate cannot pay a player until native admission transfers its custody. */
@Mixin(ExperienceOrb.class)
public abstract class PaidExperiencePickupMixin {
    @Inject(method = "playerTouch", at = @At("HEAD"), cancellable = true)
    private void thaumcraft$guardPaidExperience(Player player, CallbackInfo ci) {
        if (!ExperienceOrbQueue.allowPickup((ExperienceOrb) (Object) this)) ci.cancel();
    }
}
