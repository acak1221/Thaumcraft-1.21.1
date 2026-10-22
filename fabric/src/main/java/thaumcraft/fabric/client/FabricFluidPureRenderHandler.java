package thaumcraft.fabric.client;

import com.mojang.blaze3d.vertex.VertexConsumer;
import net.fabricmc.fabric.api.client.render.fluid.v1.FluidRenderHandler;
import net.fabricmc.fabric.api.client.render.fluid.v1.FluidRenderHandlerRegistry;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;
import thaumcraft.client.renderers.fluid.PureFluidRender;
import thaumcraft.common.config.ThaumcraftSpa;

/**
 * The Fabric adapter of the shared original Forge Classic corner heights/UV/face visibility of
 * the TC4 purifying fluid.
 *
 * <p>The geometry itself lives in the shared module
 * ({@code thaumcraft.client.renderers.fluid.PureFluidRender}), so the NeoForge adapter draws
 * exactly the same fluid.</p>
 */
public final class FabricFluidPureRenderHandler implements FluidRenderHandler {
    private TextureAtlasSprite[] sprites;

    public static void init() {
        var handler = new FabricFluidPureRenderHandler();
        FluidRenderHandlerRegistry.INSTANCE.register(ThaumcraftSpa.PURE_FLUID_SOURCE.get(), ThaumcraftSpa.PURE_FLUID_FLOWING.get(), handler);
    }

    @Override public void reloadTextures(TextureAtlas atlas) {
        PureFluidRender.reload(atlas);
        var sprite = atlas.getSprite(PureFluidRender.TEXTURE);
        sprites = new TextureAtlasSprite[]{sprite, sprite};
    }

    @Override public TextureAtlasSprite[] getFluidSprites(BlockAndTintGetter level, BlockPos pos, FluidState state) {
        return sprites;
    }

    @Override public int getFluidColor(BlockAndTintGetter level, BlockPos pos, FluidState state) {
        return 0xFFFFFFFF;
    }

    @Override public void renderFluid(BlockPos pos, BlockAndTintGetter level, VertexConsumer out, BlockState state, FluidState fluid) {
        PureFluidRender.render(pos, level, out, state, fluid);
    }
}
