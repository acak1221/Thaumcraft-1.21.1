package thaumcraft.fabric;

import java.util.ArrayList;
import java.util.List;
import net.fabricmc.loader.api.FabricLoader;
import thaumcraft.api.IThaumcraftAddon;
import thaumcraft.common.lib.AddonLifecycle;

/**
 * The {@code "thaumcraft"} entrypoint of add-ons. Fabric runs every {@code main} entrypoint before any
 * {@code client} or {@code server} one, so the client and the dedicated server initializers of Thaumcraft call
 * {@link #fire()} first: every mod has registered its items by then.
 */
final class FabricAddons {
    private FabricAddons() {}

    static void fire() {
        List<AddonLifecycle.Hook> hooks = new ArrayList<>();
        for (var container : FabricLoader.getInstance().getEntrypointContainers("thaumcraft", IThaumcraftAddon.class)) {
            hooks.add(new AddonLifecycle.Hook(container.getProvider().getMetadata().getId(), container.getEntrypoint()));
        }
        AddonLifecycle.fireReady(hooks);
    }
}
