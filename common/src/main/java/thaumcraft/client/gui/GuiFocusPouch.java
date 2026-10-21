package thaumcraft.client.gui;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import thaumcraft.common.container.ContainerFocusPouch;

/** Original175x232 art,18-slot coordinates and locked-hotbar marker. */
public final class GuiFocusPouch extends AbstractContainerScreen<ContainerFocusPouch> {
    private static final ResourceLocation TEXTURE=ResourceLocation.fromNamespaceAndPath("thaumcraft","textures/gui/gui_focuspouch.png");
    public GuiFocusPouch(ContainerFocusPouch menu,Inventory inventory,Component title){super(menu,inventory,title);imageWidth=175;imageHeight=232;}
    // Original GuiFocusPouch draws the sheet and the locked marker with the
    // ordinary alpha blend of the 1.7.10 GUI. The plain 1.21.1 texture blit
    // neither enables nor disables blending, and the sheet of the pouch has
    // a translucent frame (9950 of 40600 pixels of the drawn region) plus a
    // half transparent locked marker (alpha 0..128), so without the blend
    // both are drawn as solid black.
    @Override protected void renderBg(GuiGraphics graphics,float partial,int x,int y){
        RenderSystem.enableBlend();RenderSystem.defaultBlendFunc();
        try{graphics.blit(TEXTURE,leftPos,topPos,0,0,imageWidth,imageHeight,256,256);}
        finally{RenderSystem.disableBlend();}
    }
    @Override protected void renderLabels(GuiGraphics graphics,int x,int y){
        if(menu.lockedSlot()<45)return;
        RenderSystem.enableBlend();RenderSystem.defaultBlendFunc();
        try{graphics.blit(TEXTURE,8+(menu.lockedSlot()-45)*18,209,240,0,16,16,256,256);}
        finally{RenderSystem.disableBlend();}
    }
    @Override public void render(GuiGraphics graphics,int x,int y,float partial){super.render(graphics,x,y,partial);renderTooltip(graphics,x,y);}
}
