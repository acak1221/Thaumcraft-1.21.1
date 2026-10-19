package thaumcraft.fabric.clienttest.mixin;

import net.minecraft.world.entity.Mob;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import thaumcraft.fabric.clienttest.FocalManipulatorClientSequence;

/** Observe natural late activation before native despawn; never cancel or mutate. */
@Mixin(Mob.class)
public abstract class FocalLaterMobMixin {
    @Inject(method = "checkDespawn()V", at = @At("HEAD"), require = 1, allow = 1)
    private void thaumcraft$focalLaterMob(CallbackInfo callback) {
        FocalManipulatorClientSequence.observeLaterMob((Mob)(Object)this);
    }
}
