package thaumcraft.client.gui;

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
    @Override protected void renderBg(GuiGraphics graphics,float partial,int x,int y){graphics.blit(TEXTURE,leftPos,topPos,0,0,imageWidth,imageHeight,256,256);}
    @Override protected void renderLabels(GuiGraphics graphics,int x,int y){if(menu.lockedSlot()>=45)graphics.blit(TEXTURE,8+(menu.lockedSlot()-45)*18,209,240,0,16,16,256,256);}
    @Override public void render(GuiGraphics graphics,int x,int y,float partial){super.render(graphics,x,y,partial);renderTooltip(graphics,x,y);}
}
