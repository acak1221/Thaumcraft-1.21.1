package thaumcraft.client.gui;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;
import thaumcraft.common.container.ContainerArcaneBore;

/** Original176x141 art/half-size stats. Native synced slots and menu data only;
 * no client recomputation of Fortune/speed/area, start action or charge display. */
public final class GuiArcaneBore extends AbstractContainerScreen<ContainerArcaneBore> {
    private static final ResourceLocation BACKGROUND = ResourceLocation.fromNamespaceAndPath("thaumcraft", "textures/gui/gui_arcanebore.png");

    public GuiArcaneBore(ContainerArcaneBore menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        imageWidth = ContainerArcaneBore.IMAGE_WIDTH;
        imageHeight = ContainerArcaneBore.IMAGE_HEIGHT;
    }

    @Override protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        graphics.setColor(1, 1, 1, 1);
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        graphics.blit(BACKGROUND, leftPos, topPos, 0, 0, imageWidth, imageHeight, 256, 256);
        ItemStack pick = menu.getSlot(1).getItem().copy();
        if (!pick.isEmpty() && (long) pick.getDamageValue() + 1 >= pick.getMaxDamage()) {
            graphics.blit(BACKGROUND, leftPos + 74, topPos + 18, 184, 0, 16, 16, 256, 256);
        }
        RenderSystem.disableBlend();
        var stats = menu.stats(); // Data0 Fortune,1 speed,2 area; width is NOT data0.
        graphics.pose().pushPose();
        try {
            graphics.pose().translate(leftPos + 112, topPos + 8, 505);
            graphics.pose().scale(.5F, .5F, 1);
            graphics.drawString(font, Component.translatableWithFallback("tc.port.bore.width", "Width: %s", stats.width()), 0, 0, 0xFFFFFFFF, true);
            graphics.drawString(font, Component.translatableWithFallback("tc.port.bore.speed", "Speed: +%s", stats.speed()), 0, 10, 0xFFFFFFFF, true);
            graphics.drawString(font, Component.translatableWithFallback("tc.port.bore.properties", "Other properties:"), 0, 24, 0xFFFFFFFF, true);
            int row = 34;
            // Native Clusters / focus Silk / Dowsing are not implemented in the
            // bounded machine. Do not imply they work by drawing original rows.
            if (stats.fortune() > 0) {
                graphics.drawString(font, Component.translatable("enchantment.minecraft.fortune").append(" " + stats.fortune()), 4, row, 0xFFEEC64A, true);
                row += 9;
            }
            if (hasPickSilk(pick)) graphics.drawString(font, Component.translatable("enchantment.minecraft.silk_touch"), 4, row, 0xFF8080FF, true);
        } finally { graphics.pose().popPose(); }
    }

    private boolean hasPickSilk(ItemStack pick) {
        return !pick.isEmpty() && minecraft != null && minecraft.level != null
                && EnchantmentHelper.getItemEnchantmentLevel(minecraft.level.registryAccess()
                .lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(Enchantments.SILK_TOUCH), pick) > 0;
    }

    @Override protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {}
    @Override public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);
        renderTooltip(graphics, mouseX, mouseY);
    }
}
