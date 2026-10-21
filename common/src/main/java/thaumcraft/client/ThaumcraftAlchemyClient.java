package thaumcraft.client;

import dev.architectury.registry.client.rendering.BlockEntityRendererRegistry;
import dev.architectury.registry.client.rendering.RenderTypeRegistry;
import net.minecraft.client.renderer.RenderType;
import thaumcraft.client.fx.ThaumatoriumClientEffects;
import thaumcraft.client.renderers.tile.TileCrucibleRenderer;
import thaumcraft.common.config.ThaumcraftAlchemy;
import thaumcraft.common.tiles.TileCrucible;
import thaumcraft.common.tiles.TileThaumatorium;

/** Client-only entry point for the original cutout shell, fluid and crucible FX. */
public final class ThaumcraftAlchemyClient {
    private static boolean initialized;
    private ThaumcraftAlchemyClient() {}

    public static void init() {
        if (initialized) return;
        initialized = true;
        RenderTypeRegistry.register(RenderType.cutout(), ThaumcraftAlchemy.CRUCIBLE.get());
        BlockEntityRendererRegistry.register(ThaumcraftAlchemy.CRUCIBLE_BLOCK_ENTITY.get(), TileCrucibleRenderer::new);
        TileCrucible.setClientEffects(new TileCrucibleRenderer.Effects());
        // Original TileThaumatorium:221-230 - the vent of the machine.
        TileThaumatorium.setClientEffects(new ThaumatoriumClientEffects());
    }
}
