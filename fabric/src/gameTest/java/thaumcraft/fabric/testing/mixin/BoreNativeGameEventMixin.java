package thaumcraft.fabric.testing.mixin;

import net.minecraft.core.Holder;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import thaumcraft.common.testing.BoreNativeDispatchRuntimeChecks;

/** Isolated GameTest module only; never cancels or changes an event. */
@Mixin(ServerLevel.class)
public abstract class BoreNativeGameEventMixin {
    @Inject(method = "gameEvent(Lnet/minecraft/core/Holder;Lnet/minecraft/world/phys/Vec3;Lnet/minecraft/world/level/gameevent/GameEvent$Context;)V",
            at = @At("HEAD"), require = 1, allow = 1)
    private void thaumcraft$boreNativeDestroyObservation(Holder<GameEvent> event, Vec3 pos, GameEvent.Context context, CallbackInfo ci) {
        BoreNativeDispatchRuntimeChecks.observeNativeEvent((ServerLevel)(Object)this, event, pos);
    }
}
