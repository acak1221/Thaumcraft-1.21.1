package thaumcraft.fabric.client;

import net.fabricmc.fabric.api.client.render.fluid.v1.FluidRenderHandlerRegistry;
import net.fabricmc.fabric.api.client.render.fluid.v1.SimpleFluidRenderHandler;
import thaumcraft.client.renderers.fluid.DeathFluidAppearance;
import thaumcraft.common.config.ThaumcraftLegacyTiles;

/** Both finite death-fluid states draw the original sheet, without the vanilla biome water tint. */
public final class FabricFluidDeathRenderHandler extends SimpleFluidRenderHandler {
    public FabricFluidDeathRenderHandler() {
        super(DeathFluidAppearance.TEXTURE, DeathFluidAppearance.TEXTURE, DeathFluidAppearance.COLOR);
    }

    public static void init() {
        FluidRenderHandlerRegistry.INSTANCE.register(ThaumcraftLegacyTiles.DEATH_FLUID_SOURCE.get(),
                ThaumcraftLegacyTiles.DEATH_FLUID_FLOWING.get(), new FabricFluidDeathRenderHandler());
    }
}
