package thaumcraft.client.gui;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Inventory;
import thaumcraft.common.container.ContainerTravelingTrunk;
import thaumcraft.common.items.golems.GolemUpgrade;

/** TC4 GuiTravelingTrunk: owner caption at half size, a health bar, the Earth row band and the "stay" mark with its button. The mark
 * is read from the synced trunk, never flipped locally; the click only asks the server (menu button 1) and prints the reply line. */
public final class GuiTravelingTrunk extends ThaumcraftContainerScreen<ContainerTravelingTrunk> {
    private static final ResourceLocation BACKGROUND = ResourceLocation.fromNamespaceAndPath("thaumcraft", "textures/gui/guitrunkbase.png");
    private static final int CAPTION_COLOR = 0xFFC0A0F0;

    public GuiTravelingTrunk(ContainerTravelingTrunk menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        imageWidth = ContainerTravelingTrunk.IMAGE_WIDTH;
        imageHeight = ContainerTravelingTrunk.IMAGE_HEIGHT;
    }

    @Override protected void containerTick() {
        super.containerTick();
        var trunk = menu.trunk();
        if (minecraft != null && minecraft.player != null && (trunk == null || !trunk.isAlive())) minecraft.player.closeContainer();
    }

    @Override protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        var trunk = menu.trunk();
        if (trunk == null) return;
        graphics.pose().pushPose();
        try {
            graphics.pose().scale(0.5F, 0.5F, 1);
            graphics.drawString(font, trunk.ownerLabel().copy().append(Component.translatable("entity.trunk.guiname")), 8, 4, CAPTION_COLOR, false);
        } finally { graphics.pose().popPose(); }
    }

    @Override protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        graphics.setColor(1, 1, 1, 1);
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        graphics.blit(BACKGROUND, leftPos, topPos, 0, 0, imageWidth, imageHeight, 256, 256);
        var trunk = menu.trunk();
        if (trunk != null) {
            int bar = Math.round(trunk.getHealth() / trunk.getMaxHealth() * 39.0F);
            graphics.blit(BACKGROUND, leftPos + 134, topPos + 2, 176, 16, bar, 6, 256, 256);
            if (trunk.getUpgrade() == GolemUpgrade.EARTH.id) graphics.blit(BACKGROUND, leftPos, topPos + 80, 0, 206, imageWidth, 27, 256, 256);
            if (trunk.getStay()) graphics.blit(BACKGROUND, leftPos + 112, topPos, 176, 0, 10, 10, 256, 256);
        }
        RenderSystem.disableBlend();
    }

    @Override public boolean mouseClicked(double mouseX, double mouseY, int button) {
        var trunk = menu.trunk();
        if (button == 0 && trunk != null && minecraft != null && minecraft.gameMode != null && minecraft.player != null
                && isHovering(112, 0, 10, 11, mouseX, mouseY)) {
            trunk.level().playLocalSound(trunk.getX(), trunk.getY(), trunk.getZ(), SoundEvents.UI_BUTTON_CLICK.value(), SoundSource.NEUTRAL,
                    0.3F, 0.6F + (trunk.getStay() ? 0.0F : 0.2F), false);
            minecraft.player.sendSystemMessage(Component.translatable(trunk.getStay() ? "entity.trunk.move" : "entity.trunk.stay"));
            minecraft.gameMode.handleInventoryButtonClick(menu.containerId, ContainerTravelingTrunk.STAY_BUTTON);
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);
        renderTooltip(graphics, mouseX, mouseY);
    }
}
