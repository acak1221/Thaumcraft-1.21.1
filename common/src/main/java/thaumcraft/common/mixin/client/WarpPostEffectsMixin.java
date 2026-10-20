package thaumcraft.common.mixin.client;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.renderer.GameRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import thaumcraft.client.ThaumcraftWarpClient;
@Mixin(GameRenderer.class)
abstract class WarpPostEffectsMixin {
    @Inject(method = "renderLevel", at = @At("RETURN"))
    private void thaumcraft$warpVision(DeltaTracker delta, CallbackInfo callback) { ThaumcraftWarpClient.renderPostEffects(delta.getGameTimeDeltaPartialTick(false)); }
}
