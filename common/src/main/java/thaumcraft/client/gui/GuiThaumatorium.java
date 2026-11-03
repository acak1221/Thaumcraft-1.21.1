package thaumcraft.client.gui;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import thaumcraft.common.container.ContainerThaumatorium;
import thaumcraft.api.aspects.Aspect;

/** Original art and recipe arrows, using choices sent by the authoritative menu. */
public final class GuiThaumatorium extends ThaumcraftContainerScreen<ContainerThaumatorium> {
    private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath("thaumcraft", "textures/gui/gui_thaumatorium.png");
    private final ThaumatoriumRecipeNavigation navigation = new ThaumatoriumRecipeNavigation();
    public GuiThaumatorium(ContainerThaumatorium menu, Inventory inventory, Component title) { super(menu, inventory, title); imageWidth = 176; imageHeight = 166; }
    @Override protected void renderBg(GuiGraphics graphics, float partial, int x, int y) {
        graphics.blit(TEXTURE, leftPos, topPos, 0, 0, imageWidth, imageHeight, 256, 256);
        navigation.refresh(menu.recipes().size(), menu::available);
        int index = navigation.recipeIndex();
        if (index >= 0) {
            if (menu.selected(index)) graphics.fill(leftPos + 109, topPos + 13, leftPos + 131, topPos + 35, 0x886666ff);
            var output = menu.recipes().get(index).value().getResultItem(minecraft.level.registryAccess());
            graphics.renderItem(output, leftPos + 112, topPos + 16);
            graphics.renderItemDecorations(font, output, leftPos + 112, topPos + 16);
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
            if (navigation.size() > 1) {
                graphics.blit(TEXTURE, leftPos + ThaumatoriumRecipeNavigation.ARROW_X, topPos + ThaumatoriumRecipeNavigation.PREVIOUS_Y,
                        navigation.hasPrevious() ? 192 : 176, 16, ThaumatoriumRecipeNavigation.ARROW_WIDTH, ThaumatoriumRecipeNavigation.ARROW_HEIGHT, 256, 256);
                graphics.blit(TEXTURE, leftPos + ThaumatoriumRecipeNavigation.ARROW_X, topPos + ThaumatoriumRecipeNavigation.NEXT_Y,
                        navigation.hasNext() ? 192 : 176, 24, ThaumatoriumRecipeNavigation.ARROW_WIDTH, ThaumatoriumRecipeNavigation.ARROW_HEIGHT, 256, 256);
                String position = (navigation.page() + 1) + "/" + navigation.size();
                graphics.drawString(font, position, leftPos + 120 - font.width(position) / 2, topPos + 66, 0x443322, false);
            }
        }
    }
    @Override public boolean mouseClicked(double x, double y, int button) {
        navigation.refresh(menu.recipes().size(), menu::available);
        if (button == 0 && navigation.click(x - leftPos, y - topPos)) return true;
        if (button == 0 && isHovering(112, 16, 16, 16, x, y)) {
            int index = navigation.recipeIndex();
            if (index >= 0) {
                if (minecraft != null && minecraft.gameMode != null) minecraft.gameMode.handleInventoryButtonClick(menu.containerId, index);
                return true;
            }
        }
        return super.mouseClicked(x, y, button);
    }
    @Override protected void renderLabels(GuiGraphics graphics, int x, int y) {}
    @Override public void render(GuiGraphics graphics, int x, int y, float partial) {
        super.render(graphics, x, y, partial);
        renderTooltip(graphics, x, y);
        int index = navigation.recipeIndex();
        if (index >= 0 && isHovering(112, 16, 16, 16, x, y)) {
            graphics.renderTooltip(font, menu.recipes().get(index).value().getResultItem(minecraft.level.registryAccess()), x, y);
        }
    }
}
