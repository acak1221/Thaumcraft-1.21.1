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
package thaumic.tinkerer.client.core.handler.kami;

import dev.architectury.event.events.client.ClientGuiEvent;
import dev.architectury.event.events.client.ClientTickEvent;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import thaumic.tinkerer.common.item.kami.KamiHooks;

/**
 * The message above the hotbar that tells the mode of an awakened tool, the size of the Placement Mirror and whether the
 * awakened armour is on: it fades in colour through the rainbow and stays for 20 seconds.
 */
public final class ToolModeHUDHandler {
    private static String currentTooltip;
    private static Component currentComponent;
    private static int tooltipDisplayTicks;
    private static long elapsedTicks;

    private ToolModeHUDHandler() {}

    public static void register() {
        KamiHooks.setTooltipSink(ToolModeHUDHandler::setTooltip);
        ClientTickEvent.CLIENT_POST.register(client -> clientTick());
        ClientGuiEvent.RENDER_HUD.register(ToolModeHUDHandler::draw);
    }

    public static void setTooltip(Component tooltip) {
        String text = tooltip.getString();
        if (!text.equals(currentTooltip)) {
            currentTooltip = text;
            currentComponent = tooltip;
            tooltipDisplayTicks = 400;
        }
    }

    public static void clientTick() {
        if (tooltipDisplayTicks > 0) --tooltipDisplayTicks;
        elapsedTicks++;
    }

    private static void draw(GuiGraphics graphics, DeltaTracker deltaTracker) {
        if (tooltipDisplayTicks <= 0 || currentTooltip == null || currentTooltip.isEmpty()) return;
        Minecraft mc = Minecraft.getInstance();
        Font font = mc.font;
        int width = graphics.guiWidth();
        int height = graphics.guiHeight();

        int tooltipStartX = (width - font.width(currentComponent)) / 2;
        int tooltipStartY = height - 72;

        int opacity = (int) (tooltipDisplayTicks * 256.0F / 10.0F);
        if (opacity > 160) opacity = 160;

        if (opacity > 0) {
            float hue = (float) Math.cos(elapsedTicks / 250D);
            hue = hue - Mth.floor(hue);
            int color = Mth.hsvToRgb(hue, 0.6F, 1F) & 0xFFFFFF;
            graphics.drawString(font, currentComponent, tooltipStartX, tooltipStartY, (opacity << 24) | color, true);
        }
    }
}
