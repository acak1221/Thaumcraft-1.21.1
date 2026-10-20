package thaumcraft.client;

import com.mojang.blaze3d.platform.InputConstants;
import dev.architectury.event.events.client.ClientTickEvent;
import dev.architectury.registry.client.keymappings.KeyMappingRegistry;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import org.lwjgl.glfw.GLFW;
import thaumcraft.common.items.equipment.ItemElementalShovel;
import thaumcraft.common.lib.network.ShovelOrientationNetworking;

/**
 * The original "Misc Wand Toggle" key (KeyHandler.keyG, default G). Its wand toggle is not ported; for now it turns the placing orientation
 * of a Shovel of the Earthmover held in the main hand, one request per press.
 */
public final class ThaumcraftMiscKeyClient {
    public static final KeyMapping MISC_TOGGLE = new KeyMapping("key.thaumcraft.misc_toggle", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_G, KeyMapping.CATEGORY_MISC);
    private static boolean initialized;

    private ThaumcraftMiscKeyClient() {}

    public static void init() {
        if (initialized) return;
        initialized = true;
        KeyMappingRegistry.register(MISC_TOGGLE);
        ClientTickEvent.CLIENT_POST.register(ThaumcraftMiscKeyClient::tick);
    }

    private static void tick(Minecraft client) {
        while (MISC_TOGGLE.consumeClick()) {
            if (client.player == null || client.level == null || client.screen != null) continue;
            if (client.player.getMainHandItem().getItem() instanceof ItemElementalShovel) ShovelOrientationNetworking.request();
        }
    }
}
