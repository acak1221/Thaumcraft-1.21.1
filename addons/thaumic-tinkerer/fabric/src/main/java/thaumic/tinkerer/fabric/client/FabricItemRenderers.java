/**
 * This file is new in the Minecraft 1.21.1 port of Thaumic Tinkerer for Thaumcraft 4 Reload (no 1.7.10 counterpart, see
 * addons/thaumic-tinkerer/NOTICE.md). Thaumic Tinkerer was created by Vazkii; the port is distributed under a Creative
 * Commons Attribution-NonCommercial-ShareAlike 3.0 License (http://creativecommons.org/licenses/by-nc-sa/3.0/deed.en_GB)
 *
 * ThaumicTinkerer is a Derivative Work on Thaumcraft 4. Thaumcraft 4 (c) Azanor 2012
 * (http://www.minecraftforum.net/topic/1585216-)
 */
package thaumic.tinkerer.fabric.client;

import net.fabricmc.fabric.api.client.rendering.v1.BuiltinItemRendererRegistry;
import thaumic.tinkerer.client.hooks.ItemRenderers;

/**
 * The Fabric backend of {@link ItemRenderers}: puts each renderer on Fabric API's {@code BuiltinItemRendererRegistry}
 * ({@code fabric-rendering-v1}). The Fabric client entrypoint installs it before the groups register their client side.
 */
public final class FabricItemRenderers {
    private FabricItemRenderers() {}

    /** Installs the backend (and registers what the groups asked for before). Safe to call more than once. */
    public static void install() {
        ItemRenderers.setBackend((item, renderer) -> BuiltinItemRendererRegistry.INSTANCE.register(item,
                (BuiltinItemRendererRegistry.DynamicItemRenderer) renderer::render));
    }
}
