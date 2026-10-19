package thaumcraft.fabric.clienttest.mixin;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import thaumcraft.fabric.clienttest.BoreClientSequence;

/** Private read-only native body witnesses. Never tick, move, cancel, restore,
 * reclassify despawn ownership, or feed an alleged outcome to production code. */
@Mixin(ServerLevel.class)
public abstract class BoreMobileTickObserverMixin {
    @Inject(method = "tickNonPassenger(Lnet/minecraft/world/entity/Entity;)V", at = @At("HEAD"), require = 1, allow = 1)
    private void thaumcraft$boreMobileHead(Entity entity, CallbackInfo ci) {
        BoreClientSequence.observeMobileMobTickHead((ServerLevel)(Object)this, entity);
    }
    @Inject(method = "tickNonPassenger(Lnet/minecraft/world/entity/Entity;)V", at = @At("RETURN"), require = 1)
    private void thaumcraft$boreMobileReturn(Entity entity, CallbackInfo ci) {
        BoreClientSequence.observeMobileMobTickReturn((ServerLevel)(Object)this, entity);
    }
}
