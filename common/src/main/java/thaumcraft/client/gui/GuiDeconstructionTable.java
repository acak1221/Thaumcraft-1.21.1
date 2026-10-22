package thaumcraft.client.gui;

import com.mojang.blaze3d.systems.RenderSystem;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Inventory;
import thaumcraft.common.container.ContainerDeconstructionTable;
import thaumcraft.common.lib.research.DeconstructionRules;

/** Original GUI art, coordinates and countdown; button 1 requests the real server reward. */
public final class GuiDeconstructionTable extends ThaumcraftContainerScreen<ContainerDeconstructionTable> {
    private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath("thaumcraft", "textures/gui/gui_decontable.png");
    private static final SoundEvent CLAIM = SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath("thaumcraft", "hhoff"));
    public GuiDeconstructionTable(ContainerDeconstructionTable menu, Inventory inventory, Component title) { super(menu, inventory, title); imageWidth = 176; imageHeight = 166; }
    @Override protected void renderBg(GuiGraphics graphics, float partial, int mouseX, int mouseY) {
        RenderSystem.enableBlend();
        graphics.blit(TEXTURE, leftPos, topPos, 0, 0, imageWidth, imageHeight, 256, 256);
        int height = Math.max(0, Math.min(46, menu.breakTime() * 46 / DeconstructionRules.BREAK_TICKS));
        if (height > 0) graphics.blit(TEXTURE, leftPos + 93, topPos + 15 + 46 - height, 176, 46 - height, 9, height, 256, 256);
        var reward = menu.reward();
        if (reward != null) {
            int color = reward.getColor();
            graphics.setColor((color >> 16 & 255) / 255F, (color >> 8 & 255) / 255F, (color & 255) / 255F, 1);
            graphics.blit(reward.getImage(), leftPos + 64, topPos + 48, 0, 0, 16, 16, 16, 16);
            graphics.setColor(1, 1, 1, 1);
        }
        RenderSystem.disableBlend();
    }
    @Override protected void renderLabels(GuiGraphics graphics, int x, int y) {}
    @Override public boolean mouseClicked(double x, double y, int button) {
        if (menu.reward() != null && isHovering(64, 48, 16, 16, x, y) && minecraft != null && minecraft.gameMode != null) {
            minecraft.gameMode.handleInventoryButtonClick(menu.containerId, 1);
            if (minecraft.player != null && minecraft.level != null) minecraft.level.playLocalSound(minecraft.player.getX(), minecraft.player.getY(), minecraft.player.getZ(),
                    CLAIM, SoundSource.PLAYERS, .2F, 1F + minecraft.level.random.nextFloat() * .1F, false);
            return true;
        }
        return super.mouseClicked(x, y, button);
    }
    @Override public void render(GuiGraphics graphics, int x, int y, float partial) {
        super.render(graphics, x, y, partial); renderTooltip(graphics, x, y);
        var reward = menu.reward();
        if (reward != null && isHovering(64, 48, 16, 16, x, y)) graphics.renderTooltip(font, List.of(
                Component.literal(reward.getName()).getVisualOrderText(),
                Component.translatable("tc.aspect." + reward.getTag()).withStyle(ChatFormatting.GRAY).getVisualOrderText()), x, y - 8);
    }
}
