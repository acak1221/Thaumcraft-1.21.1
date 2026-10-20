package thaumcraft.client.gui;

import com.mojang.blaze3d.systems.RenderSystem;
import java.util.List;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.Level;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.client.gui.ThaumcraftRecipeDisplay.Hover;
import thaumcraft.common.crafting.InfusionRunicAugmentRecipe;
import thaumcraft.common.lib.runic.RunicArmor;
import thaumcraft.common.lib.runic.RunicRules;

/** Original five real robe-augmentation examples; the same dynamic recipe drives costs and output. */
public final class RunicAugmentationDisplay {
    private static final ResourceLocation OVERLAY=ResourceLocation.fromNamespaceAndPath("thaumcraft","textures/gui/gui_researchbook_overlay.png");
    private RunicAugmentationDisplay() {}
    public static Hover render(InfusionRunicAugmentRecipe infusion, GuiGraphics graphics, Font font, Level level,
                                 int x, int y, int width, int height, int mouseX, int mouseY, long cycle) {
        ItemStack center = infusion.sample((int)Math.floorMod(cycle,5L));
        var components = infusion.components(center);
        var aspects = RunicRules.aspects(RunicArmor.finalCharge(center));
        float scale = Math.min(width / 120F, height / 206F);
        if (scale <= 0) return null;
        float originX = x + (width - 120 * scale) / 2, originY = y + (height - 206 * scale) / 2;
        float mx = (mouseX - originX) / scale, my = (mouseY - originY) / scale;
        Hover hover = null;
        graphics.pose().pushPose();
        graphics.pose().translate(originX, originY, 0);
        graphics.pose().scale(scale, scale, 1);
        drawCenteredFit(graphics, font, Component.translatable("recipe.type.infusion"), 56, 0, 112, 0xFF505050);
        drawCenteredFit(graphics, font, Component.translatable("tc.inst").append(" ")
                .append(Component.translatable("tc.inst." + Math.min(5, infusion.instability(center) / 2))), 56, 194, 112, 0xFF505050);
        graphics.flush();
        RenderSystem.enableBlend();
        graphics.blit(OVERLAY, 0, 20, 112, 34, 0, 6, 112, 34, 512, 512);
        graphics.blit(OVERLAY, 0, 58, 120, 88, 400, 154, 120, 88, 512, 512);
        RenderSystem.disableBlend();
        ItemStack result = RunicArmor.augment(center);
        drawItem(graphics, font, result, 48, 28);
        if (inside(mx, my, 48, 28, 16, 16)) hover = new Hover(result, List.of());
        drawItem(graphics, font, center, 48, 94);
        if (inside(mx, my, 48, 94, 16, 16)) hover = new Hover(center, List.of());
        int count = components.size();
        // Source's 360 / length is integer division before assignment to float.
        // Seven shards therefore use51-degree increments, not a normalized ring.
        int slice = count == 0 ? 0 : 360 / count;
        var selected = infusion.selectedComponents(center);
        for (int i = 0; i < count; i++) {
            double angle = (-90 + i * slice) / 180F * (float) Math.PI;
            int sx = 56 + (int)(Math.cos(angle) * 40) - 8;
            int sy = 102 + (int)(Math.sin(angle) * 40) - 8;
            ItemStack component = selected.get(i).displayStack(declaredChoice(components.get(i), level, cycle));
            drawItem(graphics, font, component, sx, sy);
            if (inside(mx, my, sx, sy, 16, 16)) hover = new Hover(component, List.of());
        }
        var tags = aspects.keySet().stream().sorted().toList();
        int rows = (tags.size() - 1) / 5, shift = (5 - tags.size() % 5) * 10;
        for (int i = 0; i < tags.size(); i++) {
            Aspect aspect = Aspect.getAspect(tags.get(i));
            int sx = 8 + i % 5 * 20 + (i / 5 >= rows && (rows > 1 || tags.size() < 5) ? shift : 0);
            int sy = 164 - 10 * rows + i / 5 * 20;
            int color = aspect.getColor();
            graphics.flush();
            RenderSystem.enableBlend();
            graphics.setColor((color >> 16 & 255) / 255F, (color >> 8 & 255) / 255F, (color & 255) / 255F, 1);
            graphics.blit(aspect.getImage(), sx, sy, 0, 0, 16, 16, 16, 16);
            graphics.setColor(1, 1, 1, 1);
            RenderSystem.disableBlend();
            graphics.pose().pushPose();
            graphics.pose().translate(sx + 16, sy + 11, 200);
            graphics.pose().scale(.5F, .5F, 1);
            String amount = Integer.toString(aspects.get(aspect.getTag()));
            graphics.drawString(font, amount, -font.width(amount), 0, 0xFFFFFFFF, true);
            graphics.pose().popPose();
            if (inside(mx, my, sx, sy, 16, 16)) hover = new Hover(ItemStack.EMPTY,
                    List.of(Component.literal(aspect.getName()), Component.translatable("tc.aspect." + aspect.getTag())));
        }
        graphics.pose().popPose();
        return hover;
    }

    private static ItemStack declaredChoice(Ingredient ingredient, Level level, long cycle) {
        var enabled = java.util.Arrays.stream(ingredient.getItems())
                .filter(stack -> !stack.isEmpty() && stack.isItemEnabled(level.enabledFeatures())).toList();
        return enabled.isEmpty() ? ItemStack.EMPTY : enabled.get((int)Math.floorMod(cycle, (long)enabled.size())).copyWithCount(1);
    }

    private static void drawItem(GuiGraphics graphics, Font font, ItemStack stack, int x, int y) {
        graphics.renderItem(stack, x, y);
        graphics.renderItemDecorations(font, stack, x, y);
    }

    private static void drawCenteredFit(GuiGraphics graphics, Font font, Component text, int center, int y, int width, int color) {
        float scale = Math.min(1, width / (float) Math.max(1, font.width(text)));
        graphics.pose().pushPose();
        graphics.pose().translate(center, y, 0);
        graphics.pose().scale(scale, scale, 1);
        graphics.drawString(font, text, -font.width(text) / 2, 0, color, false);
        graphics.pose().popPose();
    }

    private static boolean inside(float mx, float my, int x, int y, int width, int height) {
        return mx >= x && my >= y && mx < x + width && my < y + height;
    }

}
