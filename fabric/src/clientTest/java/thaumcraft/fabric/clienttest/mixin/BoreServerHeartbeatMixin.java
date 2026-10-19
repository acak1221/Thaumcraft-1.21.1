package thaumcraft.fabric.clienttest.mixin;

import java.util.function.BooleanSupplier;
import net.minecraft.server.MinecraftServer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import thaumcraft.fabric.clienttest.BoreClientSequence;

@Mixin(MinecraftServer.class)
public abstract class BoreServerHeartbeatMixin {
    @Inject(method = "tickServer(Ljava/util/function/BooleanSupplier;)V", at = @At("HEAD"), require = 1, allow = 1)
    private void thaumcraft$boreServerHeartbeat(BooleanSupplier keepTicking, CallbackInfo ci) {
        BoreClientSequence.nativeServerTick((MinecraftServer)(Object)this);
    }
}
