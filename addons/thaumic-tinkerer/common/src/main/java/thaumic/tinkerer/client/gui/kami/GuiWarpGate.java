/**
 * This class was created by <Vazkii>. It's distributed as part of the ThaumicTinkerer Mod.
 *
 * ThaumicTinkerer is Open Source and distributed under a Creative Commons Attribution-NonCommercial-ShareAlike 3.0
 * License (http://creativecommons.org/licenses/by-nc-sa/3.0/deed.en_GB)
 *
 * ThaumicTinkerer is a Derivative Work on Thaumcraft 4. Thaumcraft 4 (c) Azanor 2012
 * (http://www.minecraftforum.net/topic/1585216-)
 *
 * File Created @ [Jan 10, 2014, 5:43:53 PM (GMT)]
 * Ported to Minecraft 1.21.1 for Thaumcraft 4 Reload (see addons/thaumic-tinkerer/NOTICE.md).
 */
package thaumic.tinkerer.client.gui.kami;

import dev.architectury.networking.NetworkManager;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import thaumic.tinkerer.client.gui.button.kami.GuiButtonWG;
import thaumic.tinkerer.client.lib.LibResources;
import thaumic.tinkerer.common.block.tile.container.kami.ContainerWarpGate;
import thaumic.tinkerer.common.block.tile.kami.TileWarpGate;
import thaumic.tinkerer.common.network.packet.kami.PacketWarpGateButton;

/** The screen of the Celestial Gateway: ten pearl slots and the button that lets the gateway accept or refuse teleports. */
public class GuiWarpGate extends AbstractContainerScreen<ContainerWarpGate> {

    private static final ResourceLocation gui = ResourceLocation.parse(LibResources.GUI_WARP_GATE);

    private final TileWarpGate warpGate;

    public GuiWarpGate(ContainerWarpGate menu, Inventory inv, Component title) {
        super(menu, inv, title);
        this.warpGate = menu.gate;
    }

    @Override
    protected void init() {
        super.init();
        addRenderableWidget(new GuiButtonWG(leftPos + 5, topPos + 5, warpGate.locked, this::actionPerformed));
    }

    /** 1.7.10 {@code actionPerformed}: the button flips, the gate and the server learn the new value. */
    private void actionPerformed(GuiButtonWG button) {
        button.enabled = !button.enabled;
        warpGate.locked = button.enabled;

        NetworkManager.sendToServer(new PacketWarpGateButton(warpGate));
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);
        renderTooltip(graphics, mouseX, mouseY);
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        graphics.blit(gui, leftPos, topPos, 0, 0, imageWidth, imageHeight);
        graphics.drawString(font, Component.translatable("ttmisc.lockedGate"), leftPos + 20, topPos + 7, 0x999999, true);
    }

    /** The 1.7.10 screen drew no title and no inventory label. */
    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {}
}
