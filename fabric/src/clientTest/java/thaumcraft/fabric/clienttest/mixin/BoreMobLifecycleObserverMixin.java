package thaumcraft.fabric.clienttest.mixin;

import net.minecraft.world.entity.Mob;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import thaumcraft.fabric.clienttest.BoreClientSequence;

@Mixin(Mob.class)
public abstract class BoreMobLifecycleObserverMixin {
    @Inject(method = "checkDespawn()V", at = @At("HEAD"), require = 1, allow = 1)
    private void thaumcraft$boreLateMob(CallbackInfo ci) {
        BoreClientSequence.observeLaterMob((Mob)(Object)this);
    }
}
