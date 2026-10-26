package thaumcraft.fabric;

import net.fabricmc.api.DedicatedServerModInitializer;

/** The dedicated server side of the add-on hook; the client side runs it from {@link ThaumcraftFabricClient}. */
public final class ThaumcraftFabricServer implements DedicatedServerModInitializer {
    @Override public void onInitializeServer() {
        FabricAddons.fire();
    }
}
