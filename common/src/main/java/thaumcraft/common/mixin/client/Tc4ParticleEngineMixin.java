package thaumcraft.common.mixin.client;

import net.minecraft.client.Camera;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleEngine;
import net.minecraft.client.renderer.LightTexture;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Keep the original TC4 queue independent of vanilla's 16384 render-type queues. */
@Mixin(ParticleEngine.class)
public abstract class Tc4ParticleEngineMixin {
    @Shadow protected ClientLevel level;

    @Inject(method = "add", at = @At("HEAD"), cancellable = true)
    private void thaumcraft$add(Particle particle, CallbackInfo ci) {
        if (thaumcraft.client.fx.ParticleEngine.add(level, particle)) ci.cancel();
    }

    @Inject(method = "tick", at = @At("HEAD"))
    private void thaumcraft$tick(CallbackInfo ci) {
        thaumcraft.client.fx.ParticleEngine.tick(level);
    }

    @Inject(method = "render", at = @At("TAIL"))
    private void thaumcraft$render(LightTexture light, Camera camera, float partialTick, CallbackInfo ci) {
        light.turnOnLightLayer();
        try {
            thaumcraft.client.fx.ParticleEngine.render(level, camera, partialTick);
        } finally {
            light.turnOffLightLayer();
        }
    }

    @Inject(method = "setLevel", at = @At("HEAD"))
    private void thaumcraft$disconnect(ClientLevel next, CallbackInfo ci) {
        if (next == null) thaumcraft.client.fx.ParticleEngine.clear();
    }

    @Inject(method = "close", at = @At("HEAD"))
    private void thaumcraft$reload(CallbackInfo ci) {
        thaumcraft.client.fx.ParticleEngine.clear();
    }
}
