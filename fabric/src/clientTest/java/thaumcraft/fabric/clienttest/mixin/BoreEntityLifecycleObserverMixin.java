package thaumcraft.fabric.clienttest.mixin;

import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import thaumcraft.fabric.clienttest.BoreClientSequence;

@Mixin(Entity.class)
public abstract class BoreEntityLifecycleObserverMixin {
    @Inject(method = "setRemoved(Lnet/minecraft/world/entity/Entity$RemovalReason;)V", at = @At("HEAD"), require = 1, allow = 1)
    private void thaumcraft$boreEntityRemoval(Entity.RemovalReason reason, CallbackInfo ci) {
        BoreClientSequence.observeInheritedRemoval((Entity)(Object)this, reason);
    }
}
