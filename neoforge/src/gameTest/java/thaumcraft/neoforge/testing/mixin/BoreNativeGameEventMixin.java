package thaumcraft.neoforge.testing.mixin;

import net.minecraft.core.Holder;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import thaumcraft.common.testing.BoreNativeDispatchRuntimeChecks;

/**
 * The NeoForge half of the native BLOCK_DESTROY observation.
 *
 * <p>The shared harness observes the actual native game event of a block destruction, so the
 * NeoForge module needs the same read-only observation the Fabric harness installs. The method is
 * the vanilla {@code ServerLevel.gameEvent(Holder, Vec3, GameEvent.Context)}; the NeoForge patch
 * only adds its own hook at the head of it, so the observation sees the same event at the same
 * place as the Fabric observation. The mixin never cancels or changes an event.</p>
 */
@Mixin(ServerLevel.class)
public abstract class BoreNativeGameEventMixin {
    @Inject(method = "gameEvent(Lnet/minecraft/core/Holder;Lnet/minecraft/world/phys/Vec3;Lnet/minecraft/world/level/gameevent/GameEvent$Context;)V",
            at = @At("HEAD"), require = 1, allow = 1)
    private void thaumcraft$boreNativeDestroyObservation(Holder<GameEvent> event, Vec3 pos, GameEvent.Context context, CallbackInfo ci) {
        BoreNativeDispatchRuntimeChecks.observeNativeEvent((ServerLevel)(Object)this, event, pos);
    }
}
