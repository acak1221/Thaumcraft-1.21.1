package thaumcraft.client.gui;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import thaumcraft.common.container.ContainerAlchemyFurnace;

/** Original 176x166 art and bars, fed only by the server menu's full live integers. */
public final class GuiAlchemyFurnace extends AbstractContainerScreen<ContainerAlchemyFurnace> {
    private static final ResourceLocation BACKGROUND = ResourceLocation.fromNamespaceAndPath("thaumcraft", "textures/gui/gui_alchemyfurnace.png");

    public GuiAlchemyFurnace(ContainerAlchemyFurnace menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        imageWidth = 176;
        imageHeight = 166;
    }

    @Override protected void renderBg(GuiGraphics graphics, float delta, int mouseX, int mouseY) {
        graphics.setColor(1, 1, 1, 1);
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        graphics.blit(BACKGROUND, leftPos, topPos, 0, 0, imageWidth, imageHeight, 256, 256);
        if (menu.isBurning()) {
            int height = menu.getBurnTimeRemainingScaled(20);
            graphics.blit(BACKGROUND, leftPos + 80, topPos + 46 - height, 176, 20 - height, 16, height, 256, 256);
        }
        int height = menu.getCookProgressScaled(46);
        graphics.blit(BACKGROUND, leftPos + 106, topPos + 59 - height, 216, 46 - height, 9, height, 256, 256);
        height = menu.getContentsScaled(48);
        graphics.blit(BACKGROUND, leftPos + 61, topPos + 60 - height, 200, 48 - height, 8, height, 256, 256);
        graphics.blit(BACKGROUND, leftPos + 60, topPos + 8, 232, 0, 10, 55, 256, 256);
        RenderSystem.disableBlend();
    }

    @Override protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {}
    @Override public void render(GuiGraphics graphics, int mouseX, int mouseY, float delta) {
        super.render(graphics, mouseX, mouseY, delta);
        renderTooltip(graphics, mouseX, mouseY);
    }
}
