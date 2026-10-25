package thaumcraft.fabric.compat;

import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;
import thaumcraft.client.gui.ThaumcraftConfigScreen;

/**
 * Optional Mod Menu entry: the configure button opens the port's configuration screen.
 *
 * <p>The original opened its settings from the Forge mod list
 * ({@code thaumcraft/client/ThaumcraftGuiFactory.java:24-25}); Mod Menu is the Fabric
 * counterpart of that list. Loaded only through the {@code modmenu} entrypoint.</p>
 */
public final class ThaumcraftModMenu implements ModMenuApi {
    @Override public ConfigScreenFactory<?> getModConfigScreenFactory() { return ThaumcraftConfigScreen::new; }
}
