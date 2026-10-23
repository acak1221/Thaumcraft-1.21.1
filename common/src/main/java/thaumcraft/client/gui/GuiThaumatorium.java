package thaumcraft.client.gui;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import thaumcraft.common.container.ContainerThaumatorium;
import thaumcraft.api.aspects.Aspect;

/** Original art and input position, with a bounded row of recipe choices sent by the authoritative menu. */
public final class GuiThaumatorium extends ThaumcraftContainerScreen<ContainerThaumatorium> {
    private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath("thaumcraft", "textures/gui/gui_thaumatorium.png");
    private int page;
    public GuiThaumatorium(ContainerThaumatorium menu, Inventory inventory, Component title) { super(menu, inventory, title); imageWidth = 176; imageHeight = 166; }
    @Override protected void renderBg(GuiGraphics graphics, float partial, int x, int y) {
        graphics.blit(TEXTURE, leftPos, topPos, 0, 0, imageWidth, imageHeight, 256, 256);
        int visible = 0;
        for (int index = 0; index < menu.recipes().size(); index++) {
            if (!menu.available(index)) continue;
            if (visible++ != page) continue;
            if (menu.selected(index)) graphics.fill(leftPos + 109, topPos + 13, leftPos + 131, topPos + 35, 0x886666ff);
            graphics.renderItem(menu.recipes().get(index).value().getResultItem(minecraft.level.registryAccess()), leftPos + 112, topPos + 16);
            var cost = menu.recipes().get(index).value().aspects();
            int aspectIndex = 0;
            for (String tag : cost.keySet().stream().sorted().limit(6).toList()) {
                Aspect aspect = Aspect.getAspect(tag); int xAspect = leftPos + 40 + aspectIndex++ * 16, color = aspect.getColor();
                RenderSystem.enableBlend();
                graphics.setColor((color >> 16 & 255) / 255F, (color >> 8 & 255) / 255F, (color & 255) / 255F, 1);
                graphics.blit(aspect.getImage(), xAspect, topPos + 40, 0, 0, 16, 16, 16, 16);
                graphics.setColor(1, 1, 1, 1); RenderSystem.disableBlend();
                graphics.pose().pushPose(); try { graphics.pose().translate(xAspect, topPos + 57, 0); graphics.pose().scale(.5F, .5F, 1);
                    graphics.drawString(font, (menu.selected(index) ? menu.stored(aspect) + "/" : "") + cost.get(tag), 0, 0, 0x443322, false);
                } finally {
                    graphics.pose().popPose();
                }
            }
            graphics.drawString(font, "<   >", leftPos + 96, topPos + 60, 0x443322, false);
            break;
        }
        if (page >= visible && page > 0) page = Math.max(0, visible - 1);
    }
    @Override public boolean mouseClicked(double x, double y, int button) {
        if (button == 0 && isHovering(96, 55, 44, 18, x, y)) { page = Math.max(0, page + (x - leftPos < 117 ? -1 : 1)); return true; }
        if (button == 0 && isHovering(110, 14, 20, 20, x, y)) {
            int visible = 0;
            for (int index = 0; index < menu.recipes().size(); index++) if (menu.available(index) && visible++ == page) {
                if (minecraft != null && minecraft.gameMode != null) minecraft.gameMode.handleInventoryButtonClick(menu.containerId, index);
                return true;
            }
        }
        return super.mouseClicked(x, y, button);
    }
    @Override protected void renderLabels(GuiGraphics graphics, int x, int y) {}
    @Override public void render(GuiGraphics graphics, int x, int y, float partial) { super.render(graphics, x, y, partial); renderTooltip(graphics, x, y); }
}
