package thaumcraft.client;

import dev.architectury.registry.menu.MenuRegistry;
import net.minecraft.client.multiplayer.ClientLevel;
import thaumcraft.client.gui.GuiSpa;
import thaumcraft.common.blocks.BlockFluidDeath;
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
        // Original BlockFluidDeath:65-80 - the death fluid draws a white slimy bubble with the
        // alpha 0.8 over every cell. The native bubble is the white bubble of the port; the
        // sprite sheet of the original (the particle sheet index 144) has no counterpart.
        BlockFluidDeath.setClientEffects((level,x,y,z,size,alpha)->{
            if(level instanceof ClientLevel client)ThaumcraftEldritchItemsClient.bubbleAlpha(client,(float)x,(float)y,(float)z,1,1,1,0,alpha);
        });
    }
}
