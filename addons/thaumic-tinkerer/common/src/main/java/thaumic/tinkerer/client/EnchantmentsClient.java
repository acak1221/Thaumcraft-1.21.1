/**
 * This file is new in the Minecraft 1.21.1 port of Thaumic Tinkerer for Thaumcraft 4 Reload (no 1.7.10 counterpart, see
 * addons/thaumic-tinkerer/NOTICE.md). Thaumic Tinkerer was created by Vazkii; the port is distributed under a Creative
 * Commons Attribution-NonCommercial-ShareAlike 3.0 License (http://creativecommons.org/licenses/by-nc-sa/3.0/deed.en_GB)
 *
 * ThaumicTinkerer is a Derivative Work on Thaumcraft 4. Thaumcraft 4 (c) Azanor 2012
 * (http://www.minecraftforum.net/topic/1585216-)
 */
package thaumic.tinkerer.client;

import dev.architectury.registry.ReloadListenerRegistry;
import dev.architectury.registry.client.rendering.BlockEntityRendererRegistry;
import dev.architectury.registry.menu.MenuRegistry;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.resources.ResourceManagerReloadListener;
import thaumic.tinkerer.client.core.handler.EnchantmentClientHandler;
import thaumic.tinkerer.client.gui.EnchantIcons;
import thaumic.tinkerer.client.gui.GuiEnchanting;
import thaumic.tinkerer.client.render.tile.RenderTileEnchanter;
import thaumic.tinkerer.common.content.Enchantments;
import thaumic.tinkerer.common.lib.LibMisc;

/**
 * Client registration of the Enchantments group: the screen and the block entity renderer of the Osmotic Enchanter, the
 * jump hook of Ascent Boost, and the reload listener that makes the icons of the screen read their size again after a
 * resource reload (F3+T, a change of resource pack).
 */
public final class EnchantmentsClient {
    private EnchantmentsClient() {}

    public static void register() {
        MenuRegistry.registerScreenFactory(Enchantments.enchanterMenu(), GuiEnchanting::new);
        BlockEntityRendererRegistry.register(Enchantments.enchanterType(), RenderTileEnchanter::new);
        EnchantmentClientHandler.register();
        ReloadListenerRegistry.register(PackType.CLIENT_RESOURCES, (ResourceManagerReloadListener) manager -> EnchantIcons.clear(),
                LibMisc.id("enchant_icons"));
    }
}
