package thaumcraft.client.gui;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import thaumcraft.common.container.ContainerHoverHarness;

/** Original default176x166 art and actual source hotbar marker. */
public final class GuiHoverHarness extends AbstractContainerScreen<ContainerHoverHarness> {
    private static final ResourceLocation TEXTURE=ResourceLocation.fromNamespaceAndPath("thaumcraft","textures/gui/guihoverharness.png");
    public GuiHoverHarness(ContainerHoverHarness menu,Inventory inventory,Component title) {super(menu,inventory,title);}
    @Override protected void renderBg(GuiGraphics graphics,float partial,int x,int y) {graphics.blit(TEXTURE,leftPos,topPos,0,0,imageWidth,imageHeight,256,256);}
    @Override protected void renderLabels(GuiGraphics graphics,int x,int y) {if(menu.lockedSlot()>=28)graphics.blit(TEXTURE,8+(menu.lockedSlot()-28)*18,142,240,0,16,16,256,256);}
    @Override public void render(GuiGraphics graphics,int x,int y,float partial) {super.render(graphics,x,y,partial);renderTooltip(graphics,x,y);}
}
