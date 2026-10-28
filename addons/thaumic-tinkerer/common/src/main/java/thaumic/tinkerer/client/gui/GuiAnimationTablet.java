/**
 * This class was created by <Vazkii>. It's distributed as part of the ThaumicTinkerer Mod.
 *
 * ThaumicTinkerer is Open Source and distributed under a Creative Commons Attribution-NonCommercial-ShareAlike 3.0
 * License (http://creativecommons.org/licenses/by-nc-sa/3.0/deed.en_GB)
 *
 * ThaumicTinkerer is a Derivative Work on Thaumcraft 4. Thaumcraft 4 (c) Azanor 2012
 * (http://www.minecraftforum.net/topic/1585216-)
 *
 * File Created @ [9 Sep 2013, 16:31:14 (GMT)]
 * Ported to Minecraft 1.21.1 for Thaumcraft 4 Reload (see addons/thaumic-tinkerer/NOTICE.md).
 */
package thaumic.tinkerer.client.gui;

import dev.architectury.networking.NetworkManager;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import thaumic.tinkerer.client.gui.button.GuiButtonAT;
import thaumic.tinkerer.client.gui.button.GuiButtonATRadio;
import thaumic.tinkerer.client.gui.button.IRadioButton;
import thaumic.tinkerer.client.lib.LibResources;
import thaumic.tinkerer.common.block.tile.container.ContainerAnimationTablet;
import thaumic.tinkerer.common.block.tile.tablet.TileAnimationTablet;
import thaumic.tinkerer.common.network.packet.PacketTabletButton;

/**
 * The Animation Tablet's screen: the item slot, the left/right click radio buttons and the redstone toggle. The texts are
 * drawn in the background layer in grey, as in 1.7.10, and the screen has no title.
 */
public class GuiAnimationTablet extends AbstractContainerScreen<ContainerAnimationTablet> {

    private static final ResourceLocation gui = ResourceLocation.parse(LibResources.GUI_ANIMATION_TABLET);

    private final TileAnimationTablet tablet;
    private final List<IRadioButton> radioButtons = new ArrayList<>();
    private GuiButtonAT redstoneButton;
    private GuiButtonATRadio leftClickButton;

    public GuiAnimationTablet(ContainerAnimationTablet menu, Inventory playerInv, Component title) {
        super(menu, playerInv, title);
        this.tablet = menu.tablet;
    }

    @Override
    protected void init() {
        super.init();
        radioButtons.clear();
        redstoneButton = addRenderableWidget(new GuiButtonAT(leftPos + imageWidth / 2 - 7, topPos + 60, tablet.redstone, this::actionPerformed));
        leftClickButton = addRenderableWidget(new GuiButtonATRadio(leftPos + 52, topPos + 15, tablet.leftClick, radioButtons, this::actionPerformed));
        GuiButtonATRadio rightClickButton = addRenderableWidget(
                new GuiButtonATRadio(leftPos + 111, topPos + 15, !tablet.leftClick, radioButtons, this::actionPerformed));
        radioButtons.add(leftClickButton);
        radioButtons.add(rightClickButton);
    }

    /** {@code actionPerformed}: a radio button turns itself on, the toggle flips; then the tablet and the server learn. */
    private void actionPerformed(GuiButtonAT button) {
        if (button instanceof IRadioButton radio) radio.enableFromClick();
        else {
            redstoneButton.buttonEnabled = !redstoneButton.buttonEnabled;
        }

        tablet.leftClick = leftClickButton.buttonEnabled;
        tablet.redstone = redstoneButton.buttonEnabled;

        NetworkManager.sendToServer(new PacketTabletButton(tablet));
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);
        renderTooltip(graphics, mouseX, mouseY);
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        graphics.blit(gui, leftPos, topPos, 0, 0, imageWidth, imageHeight);
        String left = Component.translatable("ttmisc.leftClick").getString();
        String right = Component.translatable("ttmisc.rightClick").getString();
        String redstone = Component.translatable("ttmisc.redstoneControl").getString();

        graphics.drawString(font, left, leftPos + 48 - font.width(left), topPos + 18, 0x999999, false);
        graphics.drawString(font, right, leftPos + 128, topPos + 18, 0x999999, false);
        graphics.drawString(font, redstone, leftPos + imageWidth / 2 - font.width(redstone) / 2, topPos + 50, 0x999999, false);
    }

    /** The 1.7.10 screen drew no title and no inventory label. */
    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {}
}
