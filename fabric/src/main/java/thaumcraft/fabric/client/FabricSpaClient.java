package thaumcraft.fabric.client;

import net.fabricmc.fabric.api.client.render.fluid.v1.FluidRenderHandlerRegistry;
import thaumcraft.client.gui.GuiSpa;

/** Native fluid icons with the original GuiSpa's white, untinted icon draw. */
public final class FabricSpaClient {
    private FabricSpaClient() {}
    public static void init() {
        FabricFluidPureRenderHandler.init();
        GuiSpa.setFluidAppearance((level,pos,stack)->{
            var handler=FluidRenderHandlerRegistry.INSTANCE.get(stack.getFluid());
            if(handler==null)return null;
            var sprites=handler.getFluidSprites(level,pos,stack.getFluid().defaultFluidState());
            return sprites==null||sprites.length==0||sprites[0]==null?null:new GuiSpa.FluidVisual(sprites[0],0xFFFFFFFF);
        });
    }
}
