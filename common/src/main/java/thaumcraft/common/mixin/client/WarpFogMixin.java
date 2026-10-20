package thaumcraft.common.mixin.client;
import net.minecraft.client.Camera;
import net.minecraft.client.renderer.FogRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import thaumcraft.client.ThaumcraftWarpClient;
@Mixin(FogRenderer.class)
abstract class WarpFogMixin {
    @Inject(method = "setupFog", at = @At("RETURN"))
    private static void thaumcraft$warpMist(Camera camera, FogRenderer.FogMode mode, float distance, boolean thick, float partialTick, CallbackInfo callback) {
        if (mode == FogRenderer.FogMode.FOG_TERRAIN) ThaumcraftWarpClient.applyFog();
    }
}
