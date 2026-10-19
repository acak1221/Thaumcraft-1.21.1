package thaumcraft.client;

import dev.architectury.event.events.client.ClientPlayerEvent;
import dev.architectury.event.events.client.ClientTickEvent;
import dev.architectury.registry.client.rendering.BlockEntityRendererRegistry;
import thaumcraft.client.fx.InfusionClientEffects;
import thaumcraft.client.renderers.tile.TileInfusionMatrixRenderer;
import thaumcraft.client.renderers.tile.TileInfusionPillarRenderer;
import thaumcraft.client.renderers.tile.TilePedestalRenderer;
import thaumcraft.common.config.ThaumcraftInfusion;
import thaumcraft.common.lib.network.InfusionFxNetworking;

/** Client-only registration. Neither the server tiles nor common init load render classes. */
public final class ThaumcraftInfusionClient {
    private static boolean initialized;

    private ThaumcraftInfusionClient() {}

    public static void init() {
        if (initialized) return;
        initialized = true;
        BlockEntityRendererRegistry.register(ThaumcraftInfusion.MATRIX_BLOCK_ENTITY.get(), TileInfusionMatrixRenderer::new);
        BlockEntityRendererRegistry.register(ThaumcraftInfusion.PILLAR_BLOCK_ENTITY.get(), TileInfusionPillarRenderer::new);
        BlockEntityRendererRegistry.register(ThaumcraftInfusion.PEDESTAL_BLOCK_ENTITY.get(), TilePedestalRenderer::new);
        InfusionFxNetworking.initClient(InfusionClientEffects::accept);
        ClientTickEvent.CLIENT_POST.register(InfusionClientEffects::tick);
        ClientPlayerEvent.CLIENT_PLAYER_QUIT.register(player -> InfusionClientEffects.clear());
    }
}
