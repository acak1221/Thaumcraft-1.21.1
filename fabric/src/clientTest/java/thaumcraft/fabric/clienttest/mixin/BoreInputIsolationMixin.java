package thaumcraft.fabric.clienttest.mixin;

import net.minecraft.client.Minecraft;
import net.minecraft.client.MouseHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import thaumcraft.fabric.clienttest.BoreClientSequence;

/** Only unsolicited hotbar scroll in this exact owned scanner/mining channel. */
@Mixin(MouseHandler.class)
public abstract class BoreInputIsolationMixin {
    @Inject(method = "onScroll(JDD)V", at = @At("HEAD"), cancellable = true, require = 1, allow = 1)
    private void thaumcraft$boreOwnedScroll(long window, double horizontal, double vertical, CallbackInfo ci) {
        Minecraft client = Minecraft.getInstance();
        if (client.getWindow().getWindow() == window && BoreClientSequence.suppressHotbarScroll(client, horizontal, vertical)) ci.cancel();
    }
}
