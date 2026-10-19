package thaumcraft.client.gui;

import com.mojang.blaze3d.systems.RenderSystem;
import java.math.BigDecimal;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.common.container.ContainerArcaneWorkbench;
import thaumcraft.common.crafting.ArcaneVis;
import thaumcraft.common.items.wands.ItemWandCasting;
import thaumcraft.common.items.wands.WandData;

/** Original 190x234 workbench art/slot/aspect positions; prices and preview come from the server. */
public final class GuiArcaneWorkbench extends AbstractContainerScreen<ContainerArcaneWorkbench> {
    private static final ResourceLocation BACKGROUND = ResourceLocation.fromNamespaceAndPath("thaumcraft", "textures/gui/gui_arcaneworkbench.png");
    private static final int[][] ASPECT_LOCS = {{72, 21}, {24, 43}, {24, 102}, {72, 124}, {120, 102}, {120, 43}};

    public GuiArcaneWorkbench(ContainerArcaneWorkbench menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        imageWidth = 190;
        imageHeight = 234;
    }

    @Override protected void renderBg(GuiGraphics graphics, float delta, int mouseX, int mouseY) {
        RenderSystem.enableBlend();
        graphics.blit(BACKGROUND, leftPos, topPos, 0, 0, imageWidth, imageHeight, 256, 256);
        var wand = menu.getSlot(1).getItem();
        for (int i = 0; i < WandData.PRIMALS.size(); i++) {
            String tag = WandData.PRIMALS.get(i);
            int cost = menu.requiredVis(tag);
            if (cost <= 0) continue;
            Aspect aspect = Aspect.getAspect(tag);
            int x = leftPos + ASPECT_LOCS[i][0] - 8, y = topPos + ASPECT_LOCS[i][1] - 8;
            boolean funded = ArcaneVis.isCraftingWand(wand) && ItemWandCasting.getVis(wand, tag) >= cost;
            float alpha = funded ? 1 : .3F + .2F * (float) Math.sin(((minecraft.player == null ? 0 : minecraft.player.tickCount) + i * 10 + delta) / 2);
            int color = aspect.getColor();
            graphics.setColor((color >> 16 & 255) / 255F, (color >> 8 & 255) / 255F, (color & 255) / 255F, alpha);
            graphics.blit(aspect.getImage(), x, y, 0, 0, 16, 16, 16, 16);
            graphics.setColor(1, 1, 1, 1);
            graphics.pose().pushPose();
            graphics.pose().translate(x + 8, y + 12, 200);
            graphics.pose().scale(.65F, .65F, 1);
            String amount = amount(cost);
            graphics.drawString(font, amount, -font.width(amount) / 2, 0, funded ? 0xFFFFFF : 0xEE6E6E, true);
            graphics.pose().popPose();
        }
        RenderSystem.disableBlend();
    }

    @Override protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {}

    @Override public void render(GuiGraphics graphics, int mouseX, int mouseY, float delta) {
        super.render(graphics, mouseX, mouseY, delta);
        if (menu.getSlot(0).hasItem() && !menu.craftable()) {
            graphics.fill(leftPos + 160, topPos + 64, leftPos + 176, topPos + 80, 0x99000000);
            graphics.pose().pushPose();
            graphics.pose().translate(leftPos + 168, topPos + 46, 300);
            graphics.pose().scale(.5F, .5F, 1);
            Component message = Component.translatable(ArcaneVis.isCraftingWand(menu.getSlot(1).getItem())
                    ? "tc.port.arcane.insufficient_vis" : "tc.port.arcane.no_wand");
            graphics.drawString(font, message, -font.width(message) / 2, 0, 0xEE6E6E, false);
            graphics.pose().popPose();
        }
        renderTooltip(graphics, mouseX, mouseY);
        for (int i = 0; i < WandData.PRIMALS.size(); i++) {
            String tag = WandData.PRIMALS.get(i);
            int required = menu.requiredVis(tag);
            int x = leftPos + ASPECT_LOCS[i][0] - 8, y = topPos + ASPECT_LOCS[i][1] - 8;
            if (required <= 0 || mouseX < x || mouseX >= x + 16 || mouseY < y || mouseY >= y + 16) continue;
            int stored = ArcaneVis.isCraftingWand(menu.getSlot(1).getItem()) ? ItemWandCasting.getVis(menu.getSlot(1).getItem(), tag) : 0;
            graphics.renderComponentTooltip(font, List.of(Component.translatable("tc.aspect." + tag),
                    Component.translatable("tc.port.arcane.required_vis", amount(required)),
                    Component.translatable("tc.port.arcane.stored_vis", amount(stored)).withStyle(stored >= required ? ChatFormatting.GREEN : ChatFormatting.RED)), mouseX, mouseY);
            break;
        }
    }

    private static String amount(int centivis) { return BigDecimal.valueOf(centivis, 2).stripTrailingZeros().toPlainString(); }
}
