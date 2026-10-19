package thaumcraft.fabric.clienttest.mixin;

import net.minecraft.client.Minecraft;
import net.minecraft.client.MouseHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import thaumcraft.fabric.clienttest.FocalManipulatorClientSequence;

/** Isolated smoke harness ONLY. Never installed in ordinary mod distributions. */
@Mixin(MouseHandler.class)
public abstract class FocalInputIsolationMixin {
    @Inject(method = "onScroll(JDD)V", at = @At("HEAD"), cancellable = true, require = 1, allow = 1)
    private void thaumcraft$focalOwnedScroll(long window, double horizontal, double vertical, CallbackInfo callback) {
        Minecraft client = Minecraft.getInstance();
        if (client.getWindow().getWindow() == window
                && FocalManipulatorClientSequence.suppressHotbarScroll(client, horizontal, vertical)) callback.cancel();
    }
}
