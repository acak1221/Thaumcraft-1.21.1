package thaumcraft.client.gui;

import com.mojang.blaze3d.systems.RenderSystem;
import java.util.List;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.common.config.ThaumcraftAlchemy;
import thaumcraft.common.config.ThaumcraftDistillation;
import thaumcraft.common.lib.ThaumatoriumRules;

/** The implemented original 1x3x1 wand conversion, not a synthetic crafting recipe or a formed-machine item. */
public final class ThaumatoriumBookDiagram {
    private ThaumatoriumBookDiagram() {}
    public static ThaumcraftRecipeDisplay.Hover render(GuiGraphics graphics, Font font, int x, int y, int width, int height, int mouseX, int mouseY) {
        float scale = Math.min(width / 120F, height / 180F);
        if (scale <= 0) return null;
        float originX = x + (width - 120 * scale) / 2, originY = y + (height - 180 * scale) / 2;
        float mx = (mouseX - originX) / scale, my = (mouseY - originY) / scale;
        ThaumcraftRecipeDisplay.Hover hover = null;
        graphics.pose().pushPose(); graphics.pose().translate(originX, originY, 0); graphics.pose().scale(scale, scale, 1);
        Component title = Component.translatable("recipe.type.construct");
        graphics.drawString(font, title, Math.max(0, (120 - font.width(title)) / 2), 0, 0x505050, false);
        for (int row = 0; row < 3; row++) {
            int sy = 30 + row * 32;
            ItemStack stack = new ItemStack(row == 2 ? ThaumcraftAlchemy.CRUCIBLE_ITEM.get() : ThaumcraftDistillation.ALCHEMICAL_CONSTRUCT_ITEM.get());
            graphics.fill(46, sy - 2, 66, sy + 18, 0x22634f36); graphics.renderItem(stack, 48, sy);
            graphics.drawString(font, Integer.toString(3 - row), 30, sy + 4, 0x505050, false);
            if (mx >= 46 && mx < 66 && my >= sy - 2 && my < sy + 18) hover = new ThaumcraftRecipeDisplay.Hover(stack, List.of());
        }
        int index = 0;
        for (String tag : List.of("ignis", "ordo", "aqua")) {
            Aspect aspect = Aspect.getAspect(tag); int sx = 24 + index++ * 26, rgb = aspect.getColor();
            RenderSystem.enableBlend(); graphics.setColor((rgb >> 16 & 255) / 255F, (rgb >> 8 & 255) / 255F, (rgb & 255) / 255F, 1);
            graphics.blit(aspect.getImage(), sx, 137, 0, 0, 16, 16, 16, 16); graphics.setColor(1, 1, 1, 1); RenderSystem.disableBlend();
            String amount = Integer.toString(ThaumatoriumRules.FORM_VIS.get(tag));
            graphics.drawString(font, amount, sx + (16 - font.width(amount)) / 2, 156, 0x43301f, false);
            if (mx >= sx && mx < sx + 16 && my >= 137 && my < 167) hover = new ThaumcraftRecipeDisplay.Hover(ItemStack.EMPTY, List.of(Component.literal(aspect.getName()), Component.translatable("tc.aspect." + tag)));
        }
        graphics.pose().popPose(); return hover;
    }
}
