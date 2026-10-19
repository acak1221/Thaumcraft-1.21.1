package thaumcraft.fabric.clienttest.mixin;

import net.minecraft.world.level.entity.EntityAccess;
import net.minecraft.world.level.entity.PersistentEntitySectionManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import thaumcraft.fabric.clienttest.BoreClientSequence;

/** Four required passive callbacks on two exact private generic descriptors. */
@Mixin(PersistentEntitySectionManager.class)
public abstract class BoreItemManagerLifecycleObserverMixin {
    @Inject(method = "stopTracking(Lnet/minecraft/world/level/entity/EntityAccess;)V", at = @At("HEAD"), require = 1, allow = 1)
    private void thaumcraft$boreItemTrackingHead(EntityAccess entity, CallbackInfo ci) {
        BoreClientSequence.observeItemManagerLifecycle(this, entity, false, false);
    }
    @Inject(method = "stopTracking(Lnet/minecraft/world/level/entity/EntityAccess;)V", at = @At("RETURN"), require = 1, allow = 1)
    private void thaumcraft$boreItemTrackingReturn(EntityAccess entity, CallbackInfo ci) {
        BoreClientSequence.observeItemManagerLifecycle(this, entity, false, true);
    }
    @Inject(method = "unloadEntity(Lnet/minecraft/world/level/entity/EntityAccess;)V", at = @At("HEAD"), require = 1, allow = 1)
    private void thaumcraft$boreItemUnloadHead(EntityAccess entity, CallbackInfo ci) {
        BoreClientSequence.observeItemManagerLifecycle(this, entity, true, false);
    }
    @Inject(method = "unloadEntity(Lnet/minecraft/world/level/entity/EntityAccess;)V", at = @At("RETURN"), require = 1, allow = 1)
    private void thaumcraft$boreItemUnloadReturn(EntityAccess entity, CallbackInfo ci) {
        BoreClientSequence.observeItemManagerLifecycle(this, entity, true, true);
    }
}
