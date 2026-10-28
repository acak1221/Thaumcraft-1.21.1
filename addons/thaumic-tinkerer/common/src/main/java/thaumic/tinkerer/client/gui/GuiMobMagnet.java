/**
 * This class was created by <Vazkii>. It's distributed as part of the ThaumicTinkerer Mod.
 *
 * ThaumicTinkerer is Open Source and distributed under a Creative Commons Attribution-NonCommercial-ShareAlike 3.0
 * License (http://creativecommons.org/licenses/by-nc-sa/3.0/deed.en_GB)
 *
 * ThaumicTinkerer is a Derivative Work on Thaumcraft 4. Thaumcraft 4 (c) Azanor 2012
 * (http://www.minecraftforum.net/topic/1585216-)
 *
 * File Created @ [12 Sep 2013, 18:37:02 (GMT)]
 *
 * Ported to Minecraft 1.21.1 for Thaumcraft 4 Reload (see addons/thaumic-tinkerer/NOTICE.md).
 */
package thaumic.tinkerer.client.gui;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import thaumic.tinkerer.client.gui.button.GuiButtonMMRadio;
import thaumic.tinkerer.client.lib.LibResources;
import thaumic.tinkerer.common.block.tile.TileMobMagnet;
import thaumic.tinkerer.common.block.tile.container.ContainerMobMagnet;
import thaumic.tinkerer.common.item.ItemSoulMould;

public class GuiMobMagnet extends AbstractContainerScreen<ContainerMobMagnet> {

    private static final ResourceLocation gui = ResourceLocation.parse(LibResources.GUI_MOB_MAGNET);

    private final List<GuiButtonMMRadio> radioButtons = new ArrayList<>();
    private GuiButtonMMRadio adultButton;
    private GuiButtonMMRadio childButton;

    public GuiMobMagnet(ContainerMobMagnet menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
    }

    @Override
    protected void init() {
        super.init();
        radioButtons.clear();
        adultButton = addRadio(leftPos + 100, topPos + 28, menu.isAdult(), ContainerMobMagnet.BUTTON_ADULT);
        childButton = addRadio(leftPos + 100, topPos + 48, !menu.isAdult(), ContainerMobMagnet.BUTTON_CHILD);
    }

    private GuiButtonMMRadio addRadio(int x, int y, boolean enabled, int buttonId) {
        GuiButtonMMRadio button = new GuiButtonMMRadio(x, y, enabled,
                Component.translatable(buttonId == ContainerMobMagnet.BUTTON_ADULT ? "ttmisc.adult" : "ttmisc.child"),
                radioButtons, () -> choose(buttonId));
        radioButtons.add(button);
        return addRenderableWidget(button);
    }

    /** The choice of a radio button: shown at once, and sent to the server (what {@code PacketMobMagnetButton} did). */
    private void choose(int buttonId) {
        boolean adult = buttonId == ContainerMobMagnet.BUTTON_ADULT;
        menu.setData(TileMobMagnet.DATA_ADULT, adult ? 1 : 0);
        if (minecraft != null && minecraft.gameMode != null) {
            minecraft.gameMode.handleInventoryButtonClick(menu.containerId, buttonId);
        }
    }

    @Override
    protected void containerTick() {
        super.containerTick();
        // the server's value wins
        if (adultButton != null && childButton != null) {
            adultButton.setEnabled(menu.isAdult());
            childButton.setEnabled(!menu.isAdult());
        }
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        graphics.setColor(1.0F, 1.0F, 1.0F, 1.0F);
        graphics.blit(gui, leftPos, topPos, 0, 0, imageWidth, imageHeight);
        Component adult = Component.translatable("ttmisc.adult");
        Component child = Component.translatable("ttmisc.child");
        ItemStack stack = menu.getSlot(0).getItem();

        Component filter;

        if (!stack.isEmpty()) {
            String name = ItemSoulMould.getPatternName(stack);
            if (name.isEmpty()) filter = Component.translatable("ttmisc.none");
            else filter = ItemSoulMould.patternDisplayName(name);
        } else filter = Component.translatable("ttmisc.all");

        graphics.drawString(font, filter, leftPos + imageWidth / 2 - font.width(filter) / 2 - 26, topPos + 16, 0x999999,
                false);
        graphics.drawString(font, adult, leftPos + 120, topPos + 30, 0x999999, false);
        graphics.drawString(font, child, leftPos + 120, topPos + 50, 0x999999, false);
        graphics.setColor(1F, 1F, 1F, 1F);
    }

    /** 1.7.10 drew no title and no "Inventory" label in this screen. */
    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {}

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);
        renderTooltip(graphics, mouseX, mouseY);
    }
}
