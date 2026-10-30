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
import dev.architectury.registry.client.rendering.BlockEntityRendererRegistry;
import dev.architectury.registry.client.rendering.RenderTypeRegistry;
import dev.architectury.registry.menu.MenuRegistry;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.world.item.Item;
import thaumic.tinkerer.client.gui.GuiAspectAnalyzer;
import thaumic.tinkerer.client.gui.GuiMobMagnet;
import thaumic.tinkerer.client.hooks.ItemRenderers;
import thaumic.tinkerer.client.render.item.RenderMobDisplay;
import thaumic.tinkerer.client.render.tile.RenderTileFunnel;
import thaumic.tinkerer.client.render.tile.RenderTileMagnet;
import thaumic.tinkerer.client.render.tile.RenderTileRepairer;
import thaumic.tinkerer.common.block.tile.TileMagnet;
import thaumic.tinkerer.common.block.tile.TileMobMagnet;
import thaumic.tinkerer.common.content.Machines;
import thaumic.tinkerer.common.lib.LibItemNames;
import thaumic.tinkerer.common.registry.TTRegistry;

/**
 * Client registration of the Machines group: screens, block entity renderers, render layers and the built-in item
 * renderer of the Mob Display.
 *
 * <p>Architectury has no registry for a built-in item renderer (the Mob Display's model is {@code minecraft:builtin/entity},
 * so without one the item is invisible): the group registers {@link RenderMobDisplay#render} with the loader layer's
 * {@link ItemRenderers} ({@code docs/HOOKS.md} section 9), whose backend the loader module installs before the groups
 * register their client side. On Fabric the backend is {@code FabricItemRenderers}; NeoForge's backend is not written yet
 * ({@code docs/api-requests/Machines.md} section 2).</p>
 */
public final class MachinesClient {
    private MachinesClient() {}

    public static void register() {
        registerItemRenderers(TTRegistry.item(LibItemNames.MOB_DISPLAY));

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
    }

    /**
     * Queues the built-in renderer of the Mob Display item on the loader layer, which draws the mob in place of the item (1.7.10
     * {@code MinecraftForgeClient.registerItemRenderer}). Called once, first thing of {@link #register()}, with the item
     * {@code mob_display}; a second registration of the same item is an {@code IllegalStateException} of {@link ItemRenderers}.
     * (The item is a parameter so that a test can run the call with an item of its own: a JUnit test has no registry of the mod.)
     */
    public static void registerItemRenderers(Item mobDisplay) {
        ItemRenderers.register(mobDisplay, RenderMobDisplay::render);
    }
}
