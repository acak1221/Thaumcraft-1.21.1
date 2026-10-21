package thaumcraft.client;

import com.mojang.blaze3d.platform.InputConstants;
import dev.architectury.event.events.client.ClientTickEvent;
import dev.architectury.registry.client.keymappings.KeyMappingRegistry;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import org.lwjgl.glfw.GLFW;
import thaumcraft.client.gui.ThaumcraftConfigScreen;

/**
 * The native entry point of the configuration screen.
 *
 * <p>The original opened its Forge configuration screen from the mod list
 * (oracle {@code thaumcraft/client/ThaumcraftGuiFactory.java:24-25}). 1.21.1 has no mod list and
 * no Forge configuration screen, so the port opens its own screen with a key binding that has no
 * default key: the user binds it in the vanilla Controls screen.
 */
public final class ThaumcraftConfigClient {
    public static final KeyMapping CONFIG = new KeyMapping("key.thaumcraft.config", InputConstants.Type.KEYSYM,
            InputConstants.UNKNOWN.getValue(), KeyMapping.CATEGORY_MISC);
    private static boolean initialized;

    private ThaumcraftConfigClient() {}

    public static void init() {
        if (initialized) return;
        initialized = true;
        KeyMappingRegistry.register(CONFIG);
        ClientTickEvent.CLIENT_POST.register(ThaumcraftConfigClient::tick);
    }

    private static void tick(Minecraft client) {
        while (CONFIG.consumeClick()) {
            if (client.player == null || client.level == null) continue;
            client.setScreen(new ThaumcraftConfigScreen(null));
        }
    }
}
