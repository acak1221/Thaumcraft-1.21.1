/**
 * New in the 1.21.1 port of Thaumic Tinkerer (no 1.7.10 counterpart): the Fabric glue of the Machines group's client
 * side. Distributed under the same licence as the add-on (CC BY-NC-SA 3.0, see addons/thaumic-tinkerer/NOTICE.md).
 */
package thaumic.tinkerer.fabric.client;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.rendering.v1.BuiltinItemRendererRegistry;
import thaumic.tinkerer.client.render.item.RenderMobDisplay;
import thaumic.tinkerer.common.lib.LibItemNames;
import thaumic.tinkerer.common.registry.TTRegistry;

/**
 * The one client hook of the Machines group that Architectury has no registry for: the built-in item renderer of the
 * {@code mob_display} item, which draws the mob in place of the item (1.7.10: {@code MinecraftForgeClient.registerItemRenderer}).
 * The item model is {@code minecraft:builtin/entity}, so without this hook the item would be invisible.
 *
 * <p>{@code MachinesClient.register()} calls {@link #register()} by name on Fabric, so nothing has to be listed in
 * {@code fabric.mod.json}; {@link #register()} registers only once, so listing this class as a {@code client} entrypoint
 * as well is harmless.</p>
 */
public final class MachinesFabricClient implements ClientModInitializer {
    private static boolean registered;

    @Override
    public void onInitializeClient() {
        register();
    }

    /** Registers the renderer of the mob display item (once). */
    public static synchronized void register() {
        if (registered) return;
        registered = true;
        BuiltinItemRendererRegistry.INSTANCE.register(TTRegistry.item(LibItemNames.MOB_DISPLAY),
                (BuiltinItemRendererRegistry.DynamicItemRenderer) RenderMobDisplay::render);
    }
}
