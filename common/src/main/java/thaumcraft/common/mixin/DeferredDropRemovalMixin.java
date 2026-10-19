package thaumcraft.common.mixin;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.entity.EntityInLevelCallback;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import thaumcraft.common.lib.utils.DeferredItemDrops;

/** Only the exact queue-owned in-flight entity is observed; ordinary entity
 * removal and the queue's own rejected ADD cleanup retain native behavior. */
@Mixin(Entity.class)
public abstract class DeferredDropRemovalMixin {
    @Inject(method = "setRemoved", at = @At("HEAD"))
    private void thaumcraft$observePaidDropRemoval(Entity.RemovalReason reason, CallbackInfo ci) { DeferredItemDrops.observeRemoval((Entity) (Object) this, reason); }
    @Inject(method = "setRemoved", at = @At("TAIL"))
    private void thaumcraft$confirmPaidDropRemoval(Entity.RemovalReason reason, CallbackInfo ci) { DeferredItemDrops.confirmRemoval((Entity) (Object) this); }
    @Inject(method = "setLevelCallback", at = @At("TAIL"))
    private void thaumcraft$observePaidDropAdmission(EntityInLevelCallback callback, CallbackInfo ci) { DeferredItemDrops.observeAdmission((Entity) (Object) this, callback); }
}
