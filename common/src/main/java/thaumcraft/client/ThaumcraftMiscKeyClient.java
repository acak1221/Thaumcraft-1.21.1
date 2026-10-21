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
 * The original "Misc Wand Toggle" key (KeyHandler.keyG, LWJGL2 key 34, the G key).
 * <p>
 * The original action is {@code PacketItemKeyToServer(player, 1)} (oracle
 * {@code thaumcraft/common/lib/network/misc/PacketItemKeyToServer.java:69-77}), which the server
 * resolves against the main hand: an {@code ItemWandCasting} runs
 * {@code WandManager.toggleMisc} and an {@code ItemElementalShovel} advances its placing
 * orientation. Both branches are ported: the wand branch is the Architect area and axis toggle
 * ({@code WandArchitect.Area.next}, oracle {@code WandManager.toggleMisc:568-609}) and the shovel
 * branch is {@code ShovelOrientationNetworking}, the native replacement of the original's single
 * item key packet.
 */
public final class ThaumcraftMiscKeyClient {
    // Original KeyHandler.keyG is LWJGL2 key 34, and the LWJGL2 key table makes 34 the G key.
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
            else if (thaumcraft.common.items.wands.WandArchitect.enabled(client.player.getMainHandItem()))
                thaumcraft.common.lib.network.WandArchitectNetworking.request();
        }
    }
}
