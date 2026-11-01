package thaumcraft.neoforge.client;

import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.client.extensions.common.IClientFluidTypeExtensions;
import net.neoforged.neoforge.client.extensions.common.RegisterClientExtensionsEvent;
import net.neoforged.neoforge.common.extensions.IFluidExtension;
import net.neoforged.neoforge.fluids.FluidType;
import thaumcraft.client.renderers.fluid.DeathFluidAppearance;
import thaumcraft.common.config.ThaumcraftLegacyTiles;

/** Both finite fluid states use the original untinted death-fluid sprite on NeoForge. */
final class NeoForgeDeathFluidClient {
    private static final IClientFluidTypeExtensions APPEARANCE = new IClientFluidTypeExtensions() {
        @Override public ResourceLocation getStillTexture() { return DeathFluidAppearance.TEXTURE; }
        @Override public ResourceLocation getFlowingTexture() { return DeathFluidAppearance.TEXTURE; }
        @Override public int getTintColor() { return DeathFluidAppearance.COLOR; }
    };

    private NeoForgeDeathFluidClient() {}

    static void register(RegisterClientExtensionsEvent event) {
        FluidType source = ((IFluidExtension) ThaumcraftLegacyTiles.DEATH_FLUID_SOURCE.get()).getFluidType();
        FluidType flowing = ((IFluidExtension) ThaumcraftLegacyTiles.DEATH_FLUID_FLOWING.get()).getFluidType();
        register(event, source, flowing);
    }

    /** The common base currently shares one type; never register that same key twice. */
    static void register(RegisterClientExtensionsEvent event, FluidType source, FluidType flowing) {
        if (source == flowing) event.registerFluidType(APPEARANCE, source);
        else event.registerFluidType(APPEARANCE, source, flowing);
    }
}
