package thaumcraft.client;

import dev.architectury.registry.menu.MenuRegistry;
import thaumcraft.client.gui.GuiAlchemyFurnace;
import thaumcraft.common.config.ThaumcraftDistillation;

/** Client-only normal furnace integration; cube models need no BE renderer. */
public final class ThaumcraftDistillationClient {
    private static boolean initialized;
    private ThaumcraftDistillationClient() {}

    public static void init() {
        if (initialized) return;
        initialized = true;
        MenuRegistry.registerScreenFactory(ThaumcraftDistillation.ALCHEMY_FURNACE_MENU.get(), GuiAlchemyFurnace::new);
    }
}
