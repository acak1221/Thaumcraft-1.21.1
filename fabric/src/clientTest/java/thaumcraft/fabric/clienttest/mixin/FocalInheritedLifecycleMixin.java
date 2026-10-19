package thaumcraft.fabric.clienttest.mixin;

import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import thaumcraft.fabric.clienttest.FocalManipulatorClientSequence;

/** Isolated client-test observation only; never cancel, mutate, or recreate entities. */
@Mixin(Entity.class)
public abstract class FocalInheritedLifecycleMixin {
    @Inject(method = "setRemoved(Lnet/minecraft/world/entity/Entity$RemovalReason;)V", at = @At("HEAD"), require = 1, allow = 1)
    private void thaumcraft$focalInheritedRemoval(Entity.RemovalReason reason, CallbackInfo callback) {
        FocalManipulatorClientSequence.observeInheritedRemoval((Entity)(Object)this, reason);
    }
}
