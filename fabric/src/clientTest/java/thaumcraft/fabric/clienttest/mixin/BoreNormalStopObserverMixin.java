package thaumcraft.fabric.clienttest.mixin;

import net.minecraft.server.MinecraftServer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import thaumcraft.fabric.clienttest.BoreClientSequence;

/** Terminal native boundary only; never cancels teardown or fabricates a tick. */
@Mixin(MinecraftServer.class)
public abstract class BoreNormalStopObserverMixin {
    @Inject(method = "stopServer()V", at = @At("HEAD"), require = 1, allow = 1)
    private void thaumcraft$boreNormalStop(CallbackInfo ci) {
        BoreClientSequence.observeNormalServerStopHead((MinecraftServer)(Object)this);
    }
}
