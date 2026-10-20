package thaumcraft.client.gui;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import thaumcraft.common.container.ContainerAccessories;

/** Native four-slot accessory menu; inventory art and slot positions remain ordinary GUI coordinates. */
public final class GuiAccessories extends AbstractContainerScreen<ContainerAccessories> {
    private static final ResourceLocation BACKGROUND=ResourceLocation.withDefaultNamespace("textures/gui/container/generic_54.png");
    public GuiAccessories(ContainerAccessories menu,Inventory inventory,Component title){super(menu,inventory,title);imageWidth=176;imageHeight=166;}
    @Override protected void renderBg(GuiGraphics graphics,float partial,int mouseX,int mouseY){
        graphics.blit(BACKGROUND,leftPos,topPos,0,0,176,14,256,256);
        graphics.fill(leftPos,topPos+14,leftPos+176,topPos+71,0xffc6c6c6);
        graphics.fill(leftPos,topPos+14,leftPos+1,topPos+71,0xff000000);
        graphics.fill(leftPos+1,topPos+14,leftPos+2,topPos+71,0xffffffff);
        graphics.fill(leftPos+174,topPos+14,leftPos+175,topPos+71,0xff555555);
        graphics.fill(leftPos+175,topPos+14,leftPos+176,topPos+71,0xff000000);
        graphics.blit(BACKGROUND,leftPos,topPos+71,0,126,176,95,256,256);
        for(int slot=0;slot<4;slot++)graphics.blit(BACKGROUND,leftPos+52+slot*18,topPos+34,7,17,18,18,256,256);
    }
    @Override public void render(GuiGraphics graphics,int mouseX,int mouseY,float partial){super.render(graphics,mouseX,mouseY,partial);renderTooltip(graphics,mouseX,mouseY);}
}
