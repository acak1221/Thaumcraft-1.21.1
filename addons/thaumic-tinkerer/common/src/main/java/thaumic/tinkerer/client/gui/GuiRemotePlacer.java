/**
 * This class was created by <Vazkii>. It's distributed as part of the ThaumicTinkerer Mod.
 *
 * ThaumicTinkerer is Open Source and distributed under a Creative Commons Attribution-NonCommercial-ShareAlike 3.0
 * License (http://creativecommons.org/licenses/by-nc-sa/3.0/deed.en_GB)
 *
 * ThaumicTinkerer is a Derivative Work on Thaumcraft 4. Thaumcraft 4 (c) Azanor 2012
 * (http://www.minecraftforum.net/topic/1585216-)
 *
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
import thaumic.tinkerer.client.gui.button.GuiButtonRP;
import thaumic.tinkerer.client.gui.button.GuiButtonRPRadio;
import thaumic.tinkerer.client.gui.button.IRadioButton;
import thaumic.tinkerer.client.lib.LibResources;
import thaumic.tinkerer.common.block.tile.TileRPlacer;
import thaumic.tinkerer.common.block.tile.container.ContainerRemotePlacer;
import thaumic.tinkerer.common.network.packet.PacketPlacerButton;

/**
 * The Remote Placer's screen: the block slot and four radio buttons for the distance of one to four blocks. (1.7.10 drew
 * the background of the Aspect Analyzer, which is the same artwork as the placer's own texture, used here.)
 */
public class GuiRemotePlacer extends AbstractContainerScreen<ContainerRemotePlacer> {

    private static final ResourceLocation gui = ResourceLocation.parse(LibResources.GUI_REMOTE_PLACER);

    private final List<IRadioButton> radioButtons = new ArrayList<>();
    private final TileRPlacer placer;

    public GuiRemotePlacer(ContainerRemotePlacer menu, Inventory inv, Component title) {
        super(menu, inv, title);
        this.placer = menu.placer;
    }

    @Override
    protected void init() {
        super.init();
        radioButtons.clear();
        for (int i = 0; i < 4; i++) {
            final int blocks = i + 1;
            GuiButtonRPRadio button = addRenderableWidget(new GuiButtonRPRadio(leftPos + 100, topPos + 13 * i, placer.blocks == blocks,
                    radioButtons, clicked -> actionPerformed(clicked, blocks)));
            radioButtons.add(button);
        }
    }

    /** {@code actionPerformed}: the clicked radio button turns on, the placer and the server learn the distance. */
    private void actionPerformed(GuiButtonRP button, int blocks) {
        if (button instanceof IRadioButton radio) radio.enableFromClick();

        placer.blocks = blocks;

        NetworkManager.sendToServer(new PacketPlacerButton(placer));
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);
        renderTooltip(graphics, mouseX, mouseY);
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        graphics.blit(gui, leftPos, topPos, 0, 0, imageWidth, imageHeight);
        graphics.drawString(font, "1", leftPos + 120, topPos + 2, 0x999999, false);
        graphics.drawString(font, "2", leftPos + 120, topPos + 15, 0x999999, false);
        graphics.drawString(font, "3", leftPos + 120, topPos + 28, 0x999999, false);
        graphics.drawString(font, "4", leftPos + 120, topPos + 41, 0x999999, false);
    }

    /** The 1.7.10 screen drew no title and no inventory label. */
    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {}
}
