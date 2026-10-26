package thaumcraft.neoforge.client;

import net.neoforged.fml.ModContainer;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;
import thaumcraft.client.gui.ThaumcraftConfigScreen;

/**
 * The configure button of NeoForge's own mod list opens the port's configuration screen.
 *
 * <p>The NeoForge counterpart of the Fabric Mod Menu entry
 * ({@code thaumcraft.fabric.compat.ThaumcraftModMenu}); the original opened its settings from the
 * Forge mod list ({@code thaumcraft/client/ThaumcraftGuiFactory.java:24-25}). Client dist only.</p>
 */
public final class NeoForgeConfigScreen {
    private NeoForgeConfigScreen() {}

    public static void register(ModContainer container) {
        container.registerExtensionPoint(IConfigScreenFactory.class,
                (IConfigScreenFactory) (mod, parent) -> new ThaumcraftConfigScreen(parent));
    }
}
