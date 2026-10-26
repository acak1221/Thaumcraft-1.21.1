package thaumcraft.client.gui;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import thaumcraft.api.baubles.BaubleType;
import thaumcraft.common.config.ThaumcraftBaubles;
import thaumcraft.common.container.ContainerAccessories;
import thaumcraft.common.lib.baubles.PlayerAccessoryInventory;

/**
 * Native four-slot accessory menu; inventory art and slot positions remain ordinary GUI coordinates.
 * An empty slot shows a faded mundane amulet, ring or belt and names its type on hover, since the
 * Baubles mod's own slot silhouettes are not part of Thaumcraft.
 */
public final class GuiAccessories extends ThaumcraftContainerScreen<ContainerAccessories> {
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
        for(int slot=0;slot<4;slot++) {
            int x=leftPos+52+slot*18,y=topPos+34;
            graphics.blit(BACKGROUND,x,y,7,17,18,18,256,256);
            if(!menu.getSlot(slot).hasItem()) {
                graphics.renderFakeItem(silhouette(PlayerAccessoryInventory.slotType(slot)),x+1,y+1);
                graphics.pose().pushPose();
                graphics.pose().translate(0,0,250);
                graphics.fill(x+1,y+1,x+17,y+17,0xb08b8b8b);
                graphics.pose().popPose();
            }
        }
    }
    @Override public void render(GuiGraphics graphics,int mouseX,int mouseY,float partial){
        super.render(graphics,mouseX,mouseY,partial);
        if(hoveredSlot!=null&&hoveredSlot.index<4&&!hoveredSlot.hasItem()&&menu.getCarried().isEmpty())
            graphics.renderTooltip(font,Component.translatable("gui.thaumcraft.accessories.slot."+typeKey(PlayerAccessoryInventory.slotType(hoveredSlot.index))),mouseX,mouseY);
        else renderTooltip(graphics,mouseX,mouseY);
    }
    private static ItemStack silhouette(BaubleType type){
        return new ItemStack(switch(type){case RING->ThaumcraftBaubles.MUNDANE_RING.get();case BELT->ThaumcraftBaubles.MUNDANE_BELT.get();default->ThaumcraftBaubles.MUNDANE_AMULET.get();});
    }
    private static String typeKey(BaubleType type){return switch(type){case RING->"ring";case BELT->"belt";default->"amulet";};}
}
