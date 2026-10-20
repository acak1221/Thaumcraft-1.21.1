package thaumcraft.client;

import dev.architectury.registry.menu.MenuRegistry;
import thaumcraft.client.gui.GuiGhostSlots;
import thaumcraft.client.gui.GuiHandMirror;
import thaumcraft.client.gui.GuiMagicBox;
import thaumcraft.common.config.ThaumcraftDeviceMenus;

/** Client screens of the machine and device menus the common module owns. */
public final class ThaumcraftDeviceGuiClient {
    private static boolean initialized;
    private ThaumcraftDeviceGuiClient() {}

    public static void init() {
        if (initialized) return;
        initialized = true;
        MenuRegistry.registerScreenFactory(ThaumcraftDeviceMenus.MAGIC_BOX.get(), GuiMagicBox::new);
        MenuRegistry.registerScreenFactory(ThaumcraftDeviceMenus.HAND_MIRROR.get(), GuiHandMirror::new);
        MenuRegistry.registerScreenFactory(ThaumcraftDeviceMenus.GHOST_SLOTS.get(), GuiGhostSlots::new);
    }
}
