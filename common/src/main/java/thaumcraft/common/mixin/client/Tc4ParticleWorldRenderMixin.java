package thaumcraft.common.mixin.client;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Camera;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.LightTexture;
import org.joml.Matrix4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Original RenderWorldLast: after weather/transparency composition, before the hand. */
@Mixin(LevelRenderer.class)
public abstract class Tc4ParticleWorldRenderMixin {
    @Inject(method = "renderLevel", at = @At("TAIL"))
    private void thaumcraft$render(DeltaTracker delta, boolean outline, Camera camera, GameRenderer renderer,
                                  LightTexture light, Matrix4f view, Matrix4f projection, CallbackInfo ci) {
        // Vanilla LevelRenderer pops the world view at its tail; restore it only
        // for this pass (CFR named 1.21.1 LevelRenderer:905-908,1080-1086).
        var modelView = RenderSystem.getModelViewStack();
        modelView.pushMatrix();
        modelView.mul(view);
        RenderSystem.applyModelViewMatrix();
        light.turnOnLightLayer();
        RenderSystem.enableDepthTest();
        try {
            thaumcraft.client.fx.ParticleEngine.render(Minecraft.getInstance().level, camera,
                    delta.getGameTimeDeltaPartialTick(false));
        } finally {
            light.turnOffLightLayer();
            modelView.popMatrix();
            RenderSystem.applyModelViewMatrix();
            RenderSystem.depthMask(true);
            RenderSystem.disableBlend();
        }
    }
}
