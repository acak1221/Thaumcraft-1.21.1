package thaumcraft.common.mixin.client;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.TextureAtlas;
import org.lwjgl.opengl.GL;
import org.lwjgl.opengl.GL11;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import thaumcraft.client.renderers.block.OreRenderRules;

/** 1.7.10's field_151443_J is anisotropicFiltering, NOT fancy graphics or mipmaps. */
@Mixin(TextureAtlas.class)
public abstract class OreAnisotropyMixin {
    @Inject(method="cycleAnimationFrames",at=@At("HEAD"))
    private void thaumcraft$sampleAtlasAnisotropy(CallbackInfo ci) {
        var atlas=(TextureAtlas)(Object)this;
        if(!atlas.location().equals(TextureAtlas.LOCATION_BLOCKS)||!RenderSystem.isOnRenderThread())return;
        var caps=GL.getCapabilities();
        int filtering=1;
        if(caps.GL_EXT_texture_filter_anisotropic||caps.GL_ARB_texture_filter_anisotropic) {
            int old=GL11.glGetInteger(GL11.GL_TEXTURE_BINDING_2D);
            atlas.bind();
            // EXT and ARB use the same GL_TEXTURE_MAX_ANISOTROPY token.
            filtering=Math.round(GL11.glGetTexParameterf(GL11.GL_TEXTURE_2D,0x84FE));
            RenderSystem.bindTexture(old);
        }
        if(OreRenderRules.updateAnisotropicFiltering(filtering)) {
            var minecraft=Minecraft.getInstance();
            if(minecraft.level!=null)minecraft.levelRenderer.allChanged();
        }
    }
}
