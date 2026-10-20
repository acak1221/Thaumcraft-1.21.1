package thaumcraft.client.gui;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Inventory;
import thaumcraft.common.container.ContainerPech;
import thaumcraft.common.entities.monster.EntityPech;

/** Original175x232 GUI and25x25 dice target. Native menu-button networking
 * requests one server trade; the client never generates or publishes rewards. */
public final class GuiPech extends AbstractContainerScreen<ContainerPech> {
    private static final ResourceLocation TEXTURE=ResourceLocation.fromNamespaceAndPath("thaumcraft","textures/gui/gui_pech.png");
    public GuiPech(ContainerPech menu,Inventory inventory,Component title){super(menu,inventory,title);imageWidth=ContainerPech.IMAGE_WIDTH;imageHeight=ContainerPech.IMAGE_HEIGHT;}
    @Override protected void renderLabels(GuiGraphics graphics,int mouseX,int mouseY){}
    @Override protected void renderBg(GuiGraphics graphics,float partial,int mouseX,int mouseY){
        graphics.setColor(1,1,1,1);RenderSystem.enableBlend();RenderSystem.defaultBlendFunc();graphics.blit(TEXTURE,leftPos,topPos,0,0,imageWidth,imageHeight,256,256);
        if(menu.canTrade())graphics.blit(TEXTURE,leftPos+67,topPos+24,176,0,25,25,256,256);RenderSystem.disableBlend();
    }
    @Override public boolean mouseClicked(double mouseX,double mouseY,int button){
        if(minecraft!=null&&minecraft.player!=null&&minecraft.gameMode!=null&&isHovering(67,24,25,25,mouseX,mouseY)&&menu.canTrade()){
            minecraft.gameMode.handleInventoryButtonClick(menu.containerId,0);var player=minecraft.player;
            player.level().playLocalSound(player.getX(),player.getY(),player.getZ(),EntityPech.sound("pech_dice"),SoundSource.PLAYERS,.5F,.95F+player.level().random.nextFloat()*.1F,false);return true;
        }
        return super.mouseClicked(mouseX,mouseY,button);
    }
    @Override public void render(GuiGraphics graphics,int mouseX,int mouseY,float partial){super.render(graphics,mouseX,mouseY,partial);renderTooltip(graphics,mouseX,mouseY);}
}
