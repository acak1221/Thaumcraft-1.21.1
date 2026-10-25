package thaumcraft.neoforge.client;

import net.minecraft.client.Minecraft;
import net.minecraft.world.inventory.InventoryMenu;
import net.neoforged.neoforge.client.extensions.common.IClientFluidTypeExtensions;
import thaumcraft.client.gui.GuiSpa;
import thaumcraft.client.renderers.fluid.PureFluidRender;
import thaumcraft.common.lib.fluid.FluidPure;

/** Native fluid appearance lookup; the original spa draws its sprite white, without biome tint. */
public final class NeoForgeSpaClient {
    private NeoForgeSpaClient() {}

    public static void init() {
        // Resolve on each draw, after fluid extensions/atlases exist and after resource reloads.
        GuiSpa.setFluidAppearance((level, pos, stack) -> {
            var state = stack.getFluid().defaultFluidState();
            // Pure borrows NeoForge's WATER_TYPE for immersion physics. Its actual
            // world renderer still uses the TC4 fluidpure sprite, as Fabric does.
            var texture = stack.getFluid() instanceof FluidPure ? PureFluidRender.TEXTURE
                    : IClientFluidTypeExtensions.of(state).getStillTexture(state, level, pos);
            if (texture == null) return null;
            var sprite = Minecraft.getInstance().getTextureAtlas(InventoryMenu.BLOCK_ATLAS).apply(texture);
            return new GuiSpa.FluidVisual(sprite, 0xFFFFFFFF);
        });
    }
}
