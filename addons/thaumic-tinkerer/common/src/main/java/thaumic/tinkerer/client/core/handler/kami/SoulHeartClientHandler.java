/**
 * This class was created by <Vazkii>. It's distributed as part of the ThaumicTinkerer Mod.
 *
 * ThaumicTinkerer is Open Source and distributed under a Creative Commons Attribution-NonCommercial-ShareAlike 3.0
 * License (http://creativecommons.org/licenses/by-nc-sa/3.0/deed.en_GB)
 *
 * ThaumicTinkerer is a Derivative Work on Thaumcraft 4. Thaumcraft 4 (c) Azanor 2012
 * (http://www.minecraftforum.net/topic/1585216-)
 *
 * File Created @ [Dec 29, 2013, 9:35:38 PM (GMT)]
 * Ported to Minecraft 1.21.1 for Thaumcraft 4 Reload (see addons/thaumic-tinkerer/NOTICE.md).
 */
package thaumic.tinkerer.client.core.handler.kami;

import com.mojang.blaze3d.systems.RenderSystem;
import dev.architectury.event.events.client.ClientGuiEvent;
import dev.architectury.event.events.client.ClientPlayerEvent;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.resources.ResourceLocation;
import thaumic.tinkerer.client.lib.LibResources;
import thaumic.tinkerer.common.core.handler.ConfigHandler;

/**
 * The Soul Hearts on the HUD: black-edged hearts in a row that starts 10 pixels right of the middle of the screen, at the
 * height {@code ConfigHandler.soulHeartHeight} above the bottom (49: the row above the food bar), ten pixels higher while
 * the air bubbles are shown. A full heart is 9 pixels wide, a half heart 7, and they are 8 pixels apart.
 *
 * <p>1.7.10 drew them when the health bar was about to be drawn; here they are drawn after the HUD, only where the health bar
 * is (not in creative or spectator mode, not with the HUD hidden).</p>
 */
public final class SoulHeartClientHandler {

    private static final ResourceLocation heartsResource = ResourceLocation.parse(LibResources.GUI_SOUL_HEARTS);

    /** The soul hearts of this player in points (half hearts), as the server last told. */
    public static int clientPlayerHP = 0;

    private SoulHeartClientHandler() {}

    public static void register() {
        ClientGuiEvent.RENDER_HUD.register(SoulHeartClientHandler::renderHud);
        // The 1.7.10 field was static for the whole session; a new world starts without hearts.
        ClientPlayerEvent.CLIENT_PLAYER_QUIT.register(player -> clientPlayerHP = 0);
    }

    /** Draws one heart: the full heart is the left 9 by 9 of the texture, the half heart the 7 by 9 to its right. */
    private static void renderHeart(GuiGraphics graphics, int x, int y, boolean full) {
        if (full) graphics.blit(heartsResource, x, y, 9, 9, 0F, 0F, 9, 9, 16, 16);
        else graphics.blit(heartsResource, x, y, 7, 9, 9F, 0F, 7, 9, 16, 16);
    }

    private static void renderHud(GuiGraphics graphics, DeltaTracker deltaTracker) {
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        if (player == null || mc.options.hideGui || mc.gameMode == null || !mc.gameMode.canHurtPlayer()) return;
        renderHUD(graphics, player.getAirSupply() < player.getMaxAirSupply());
    }

    /** 1.7.10 {@code renderHUD}: the row of hearts; {@code airShown} moves it up past the bubbles. */
    public static void renderHUD(GuiGraphics graphics, boolean airShown) {
        if (clientPlayerHP <= 0) return;
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        int x = graphics.guiWidth() / 2 + 10;
        int y = graphics.guiHeight() - ConfigHandler.soulHeartHeight;
        if (airShown) y = graphics.guiHeight() - (ConfigHandler.soulHeartHeight + 10);

        int it = 0;
        for (int i = 0; i < clientPlayerHP; i++) {
            boolean half = i == clientPlayerHP - 1 && clientPlayerHP % 2 != 0;
            if (half || i % 2 == 0) {
                renderHeart(graphics, x + it * 8, y, !half);
                it++;
            }
        }
    }
}
