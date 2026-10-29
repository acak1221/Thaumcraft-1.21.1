/**
 * This file is new in the Minecraft 1.21.1 port of Thaumic Tinkerer for Thaumcraft 4 Reload (no 1.7.10 counterpart, see
 * addons/thaumic-tinkerer/NOTICE.md). Thaumic Tinkerer was created by Vazkii; the port is distributed under a Creative
 * Commons Attribution-NonCommercial-ShareAlike 3.0 License (http://creativecommons.org/licenses/by-nc-sa/3.0/deed.en_GB)
 *
 * ThaumicTinkerer is a Derivative Work on Thaumcraft 4. Thaumcraft 4 (c) Azanor 2012
 * (http://www.minecraftforum.net/topic/1585216-)
 */
package thaumic.tinkerer.client;

import dev.architectury.event.events.client.ClientPlayerEvent;
import dev.architectury.platform.Platform;
import dev.architectury.registry.client.rendering.BlockEntityRendererRegistry;
import dev.architectury.registry.client.rendering.RenderTypeRegistry;
import dev.architectury.registry.menu.MenuRegistry;
import net.minecraft.client.renderer.RenderType;
import thaumic.tinkerer.client.gui.GuiAspectAnalyzer;
import thaumic.tinkerer.client.gui.GuiMobMagnet;
import thaumic.tinkerer.client.render.item.RenderMobDisplay;
import thaumic.tinkerer.client.render.tile.RenderTileFunnel;
import thaumic.tinkerer.client.render.tile.RenderTileMagnet;
import thaumic.tinkerer.client.render.tile.RenderTileRepairer;
import thaumic.tinkerer.common.block.tile.TileMagnet;
import thaumic.tinkerer.common.block.tile.TileMobMagnet;
import thaumic.tinkerer.common.ThaumicTinkerer;
import thaumic.tinkerer.common.content.Machines;

/**
 * Client registration of the Machines group: screens, block entity renderers and render layers.
 *
 * <p>The built-in item renderer of {@code mob_display} needs a hook of the loader that Architectury does not have. On
 * Fabric {@code MachinesFabricClient} (reached by name by {@link #register()}, so nothing has to be listed in
 * {@code fabric.mod.json}) registers {@link RenderMobDisplay#render}; NeoForge has none yet, see
 * {@code docs/api-requests/Machines.md} section 2.</p>
 */
public final class MachinesClient {
    private MachinesClient() {}

    public static void register() {
        MenuRegistry.registerScreenFactory(Machines.MENU_ASPECT_ANALYZER.get(), GuiAspectAnalyzer::new);
        MenuRegistry.registerScreenFactory(Machines.MENU_MOB_MAGNET.get(), GuiMobMagnet::new);

        BlockEntityRendererRegistry.register(Machines.TILE_REPAIRER.get(), RenderTileRepairer::new);
        BlockEntityRendererRegistry.register(Machines.TILE_FUNNEL.get(), RenderTileFunnel::new);
        BlockEntityRendererRegistry.<TileMagnet>register(Machines.TILE_MAGNET.get(), RenderTileMagnet<TileMagnet>::new);
        BlockEntityRendererRegistry.<TileMobMagnet>register(Machines.TILE_MOB_MAGNET.get(), RenderTileMagnet<TileMobMagnet>::new);

        // The glass of the Thaumic Restorer is translucent; the planes of the magnets have transparent pixels.
        RenderTypeRegistry.register(RenderType.translucent(), Machines.REPAIRER.get());
        RenderTypeRegistry.register(RenderType.cutout(), Machines.MAGNET.get(), Machines.MOB_MAGNET.get(),
                Machines.FUNNEL.get(), Machines.SUMMON.get());

        // The mobs of the mob display items belong to a level.
        ClientPlayerEvent.CLIENT_PLAYER_QUIT.register(player -> RenderMobDisplay.clearCache());

        registerLoaderGlue();
    }

    /** The renderer of the mob display item: the Fabric module is not visible to common code, so it is reached by name. */
    private static void registerLoaderGlue() {
        if (!Platform.isFabric()) return;
        try {
            Class.forName("thaumic.tinkerer.fabric.client.MachinesFabricClient").getMethod("register").invoke(null);
        } catch (ReflectiveOperationException | LinkageError e) {
            ThaumicTinkerer.LOG.error("Machines: the Fabric client glue of the mob display item renderer failed: {}", e.toString());
        }
    }
}
