package thaumcraft.client;

import dev.architectury.registry.menu.MenuRegistry;
import thaumcraft.client.gui.GuiFocusPouch;
import thaumcraft.common.config.ThaumcraftFocusPouches;

public final class ThaumcraftFocusPouchesClient {
    private static boolean initialized;
    private ThaumcraftFocusPouchesClient() {}
    public static void init(){if(initialized)return;initialized=true;MenuRegistry.registerScreenFactory(ThaumcraftFocusPouches.MENU.get(),GuiFocusPouch::new);}
}
