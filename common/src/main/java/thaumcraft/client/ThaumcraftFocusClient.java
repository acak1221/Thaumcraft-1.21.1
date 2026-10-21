package thaumcraft.client;

import com.mojang.blaze3d.platform.InputConstants;
import dev.architectury.event.EventResult;
import dev.architectury.event.events.client.ClientRawInputEvent;
import dev.architectury.event.events.client.ClientTickEvent;
import dev.architectury.registry.client.keymappings.KeyMappingRegistry;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import org.lwjgl.glfw.GLFW;
import thaumcraft.client.gui.GuiFocusSelect;
import thaumcraft.common.items.wands.ItemWandCasting;
import thaumcraft.common.items.wands.foci.FocusData;
import thaumcraft.common.lib.network.FocusSelectionNetworking;
import thaumcraft.common.items.golems.ItemGolemBell;
import thaumcraft.common.lib.network.GolemBellNetworking;

/** Shared item-key input: wand selection or bell-link reset, both server-owned. */
public final class ThaumcraftFocusClient {
    // Original KeyHandler.keyF is LWJGL2 key 33, and the LWJGL2 key table (Keyboard.KEY_F = 33,
    // KEY_G = 34, KEY_H = 35, KEY_SEMICOLON = 39, KEY_LBRACKET = 26) makes 33 the F key: the
    // original default is F, exactly as the original field name keyF says.
    public static final KeyMapping CHANGE_FOCUS = new KeyMapping("key.thaumcraft.change_focus",
            InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_F, KeyMapping.CATEGORY_MISC);
    private static boolean initialized;
    private static boolean pressHandled;
    private static boolean rawKeyDown;
    private static int rawHeldKey = GLFW.GLFW_KEY_UNKNOWN;
    private static int rawHeldScan = -1;
    private static String rawBinding = "";

    private ThaumcraftFocusClient() {}

    public static void init() {
        if (initialized) return;
        initialized = true;
        KeyMappingRegistry.register(CHANGE_FOCUS);
        // Scancode bindings cannot be polled with glfwGetKey. Observe, never
        // cancel, native input; keysym/mouse bindings also have physical polling.
        ClientRawInputEvent.KEY_PRESSED.register((client, key, scanCode, action, modifiers) -> {
            if (action == GLFW.GLFW_RELEASE && key == rawHeldKey && scanCode == rawHeldScan) rawKeyDown = false;
            if (CHANGE_FOCUS.matches(key, scanCode)) {
                rawKeyDown = action != GLFW.GLFW_RELEASE;
                rawHeldKey = key;
                rawHeldScan = scanCode;
                rawBinding = CHANGE_FOCUS.saveString();
            }
            return EventResult.pass();
        });
        ClientTickEvent.CLIENT_POST.register(ThaumcraftFocusClient::tick);
    }

    private static void tick(Minecraft client) {
        // A shared ordinary-hand click belongs to vanilla's exact swap site.
        // Do not eat it in CLIENT_POST before the next handleKeybinds call:
        // Fabric may have delivered it ONLY to this mapping. Wand selection
        // and bell reset use held/raw state, not the press counter.
        boolean vanillaOwnsSharedClick = sharesOffhandBinding(client, client.options.keySwapOffhand)
                && !hasItemAction(client.player);
        if (!vanillaOwnsSharedClick) while (CHANGE_FOCUS.consumeClick()) {}
        if (!client.isWindowActive()) {
            rawKeyDown = false;
            pressHandled = true;
            if (client.screen instanceof GuiFocusSelect screen) screen.onClose();
            return;
        }
        boolean down = isFocusKeyDown(client);
        if (!down) pressHandled = false;
        if (client.screen instanceof GuiFocusSelect screen) {
            screen.focusKeyState(down);
            return;
        }
        if (client.screen != null || client.getOverlay() != null || client.player == null || client.level == null) {
            // Typing F in chat/controls must not open a radial on returning.
            pressHandled = down;
            return;
        }
        if (!down || pressHandled) return;
        pressHandled = true;
        InteractionHand bell = bellHand(client.player);
        if (bell != null) {
            GolemBellNetworking.requestReset(bell);
            return;
        }
        InteractionHand hand = wandHand(client.player);
        if (hand == null) return;
        if (client.player.isShiftKeyDown()) {
            // Original sneak+F is REMOVE. Send one intent per physical press,
            // rather than re-requesting removal on every held-key tick.
            if (!FocusData.get(client.player.getItemInHand(hand)).isEmpty()) {
                FocusSelectionNetworking.request(hand, -1);
            }
        } else {
            client.setScreen(new GuiFocusSelect(hand));
        }
    }

    /** Main hand wins; offhand is the explicit native two-hand adaptation. */
    public static InteractionHand wandHand(Player player) {
        if (player == null || !player.isAlive() || player.isSpectator()) return null;
        if (player.getMainHandItem().getItem() instanceof ItemWandCasting) return InteractionHand.MAIN_HAND;
        if (player.getOffhandItem().getItem() instanceof ItemWandCasting) return InteractionHand.OFF_HAND;
        return null;
    }

    /** The main-hand item action wins, as with wand selection's two-hand adaptation. */
    private static InteractionHand bellHand(Player player) {
        if (player == null || !player.isAlive() || player.isSpectator()) return null;
        if (player.getMainHandItem().getItem() instanceof ItemGolemBell) return InteractionHand.MAIN_HAND;
        if (!(player.getMainHandItem().getItem() instanceof ItemWandCasting)
                && player.getOffhandItem().getItem() instanceof ItemGolemBell) return InteractionHand.OFF_HAND;
        return null;
    }
    private static boolean hasItemAction(Player player) { return bellHand(player) != null || wandHand(player) != null; }

    /** Screens release native mappings on opening; physical hold survives that. */
    public static boolean isFocusKeyDown(Minecraft client) {
        if (CHANGE_FOCUS.isUnbound() || !client.isWindowActive()) return false;
        String binding = CHANGE_FOCUS.saveString();
        if (CHANGE_FOCUS.isDown() || rawKeyDown && binding.equals(rawBinding)) return true;
        InputConstants.Key key = InputConstants.getKey(binding);
        long window = client.getWindow().getWindow();
        if (key.getType() == InputConstants.Type.KEYSYM) {
            return InputConstants.isKeyDown(window, key.getValue());
        }
        if (key.getType() == InputConstants.Type.MOUSE) {
            return GLFW.glfwGetMouseButton(window, key.getValue()) == GLFW.GLFW_PRESS;
        }
        return false;
    }

    /**
     * Called only at Minecraft.handleKeybinds' swap-offhand consumeClick site.
     * Rebinding either control apart restores ordinary offhand swap, including
     * while holding a wand or bell. No key assignment or other mapping is modified.
     */
    public static boolean shouldSuppressOffhandSwap(Minecraft client, KeyMapping mapping) {
        return sharesOffhandBinding(client, mapping) && hasItemAction(client.player);
    }

    private static boolean sharesOffhandBinding(Minecraft client, KeyMapping mapping) {
        return initialized && client.screen == null && client.getOverlay() == null
                && client.isWindowActive() && client.level != null && client.player != null
                && mapping == client.options.keySwapOffhand && !CHANGE_FOCUS.isUnbound()
                && CHANGE_FOCUS.same(mapping);
    }

    /**
     * Fabric's native KeyMapping MAP dispatches a shared key to ONE mapping;
     * NeoForge may dispatch to both. At the exact vanilla swap site, coalesce
     * either/both counters for ordinary hands. Otherwise registering F would
     * swallow vanilla offhand swap globally despite the wand-only suppression.
     */
    public static boolean consumeOffhandSwapClick(Minecraft client, KeyMapping mapping) {
        if (!sharesOffhandBinding(client, mapping)) return mapping.consumeClick();
        if (hasItemAction(client.player)) {
            while (mapping.consumeClick()) {}
            return false;
        }
        boolean vanilla = mapping.consumeClick();
        boolean sharedFocus = CHANGE_FOCUS.consumeClick();
        return vanilla || sharedFocus;
    }
}
