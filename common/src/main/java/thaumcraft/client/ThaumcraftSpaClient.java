package thaumcraft.client;

import dev.architectury.registry.menu.MenuRegistry;
import net.minecraft.client.multiplayer.ClientLevel;
import thaumcraft.client.gui.GuiSpa;
import thaumcraft.common.blocks.BlockFluidPure;
import thaumcraft.common.config.ThaumcraftSpa;

/** Original screen factory and pure-fluid age0 quarter-opacity white bubbles. */
public final class ThaumcraftSpaClient {
    private static boolean initialized;
    private ThaumcraftSpaClient() {}
    public static void init() {
        if(initialized)return;initialized=true;
        MenuRegistry.registerScreenFactory(ThaumcraftSpa.SPA_MENU.get(),GuiSpa::new);
        BlockFluidPure.setClientEffects((level,x,y,z)->{
            if(level instanceof ClientLevel client)ThaumcraftEldritchItemsClient.bubbleAlpha(client,(float)x,(float)y,(float)z,1,1,1,0,.25F);
        });
    }
}
