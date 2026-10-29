/**
 * This file is new in the Minecraft 1.21.1 port of Thaumic Tinkerer for Thaumcraft 4 Reload (no 1.7.10 counterpart, see
 * addons/thaumic-tinkerer/NOTICE.md). Thaumic Tinkerer was created by Vazkii; the port is distributed under a Creative
 * Commons Attribution-NonCommercial-ShareAlike 3.0 License (http://creativecommons.org/licenses/by-nc-sa/3.0/deed.en_GB)
 *
 * ThaumicTinkerer is a Derivative Work on Thaumcraft 4. Thaumcraft 4 (c) Azanor 2012
 * (http://www.minecraftforum.net/topic/1585216-)
 */
package thaumic.tinkerer.client;

import dev.architectury.registry.client.rendering.BlockEntityRendererRegistry;
import dev.architectury.registry.menu.MenuRegistry;
import thaumic.tinkerer.client.core.handler.EnchantmentClientHandler;
import thaumic.tinkerer.client.gui.GuiEnchanting;
import thaumic.tinkerer.client.render.tile.RenderTileEnchanter;
import thaumic.tinkerer.common.content.Enchantments;

/**
 * Client registration of the Enchantments group: the screen and the block entity renderer of the Osmotic Enchanter, and the
 * jump hook of Ascent Boost.
 */
public final class EnchantmentsClient {
    private EnchantmentsClient() {}

    public static void register() {
        MenuRegistry.registerScreenFactory(Enchantments.enchanterMenu(), GuiEnchanting::new);
        BlockEntityRendererRegistry.register(Enchantments.enchanterType(), RenderTileEnchanter::new);
        EnchantmentClientHandler.register();
    }
}
