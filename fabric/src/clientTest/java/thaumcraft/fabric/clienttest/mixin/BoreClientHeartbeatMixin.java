package thaumcraft.fabric.clienttest.mixin;

import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import thaumcraft.fabric.clienttest.BoreClientSequence;

@Mixin(Minecraft.class)
public abstract class BoreClientHeartbeatMixin {
    @Inject(method = "tick()V", at = @At("HEAD"), require = 1, allow = 1)
    private void thaumcraft$boreClientHeartbeat(CallbackInfo ci) {
        BoreClientSequence.nativeClientTick((Minecraft)(Object)this);
    }
}
