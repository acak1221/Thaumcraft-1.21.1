/**
 * This class was created by <Vazkii>. It's distributed as part of the ThaumicTinkerer Mod.
 *
 * ThaumicTinkerer is Open Source and distributed under a Creative Commons Attribution-NonCommercial-ShareAlike 3.0
 * License (http://creativecommons.org/licenses/by-nc-sa/3.0/deed.en_GB)
 *
 * ThaumicTinkerer is a Derivative Work on Thaumcraft 4. Thaumcraft 4 (c) Azanor 2012
 * (http://www.minecraftforum.net/topic/1585216-)
 *
 * File Created @ [10 Sep 2013, 12:16:36 (GMT)]
 *
 * Ported to Minecraft 1.21.1 for Thaumcraft 4 Reload (see addons/thaumic-tinkerer/NOTICE.md).
 */
package thaumic.tinkerer.client.core.handler;

import dev.architectury.event.events.client.ClientGuiEvent;
import java.util.concurrent.atomic.AtomicReference;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;
import thaumcraft.api.wands.WandHelper;
import thaumic.tinkerer.common.item.IDislocationHudSource;

/**
 * The overlay of the Focus of Dislocation: while the player holds a wand whose focus carries a block, a small panel at the
 * left edge of the screen shows the block, the words "Currently Dislocating" and, when the block brings block-entity
 * contents, "Extra Contents".
 *
 * <p>1.7.10 drew it in {@code RenderGameOverlayEvent.Post} for the hotbar, with no screen open, at {@code x = 4} and
 * {@code y = 50} (or {@code height - 70} when Thaumcraft's {@code dialBottom} option was on; that option is the position of
 * Thaumcraft's own wand dial and has no counterpart in the API, so the panel keeps the default place). The text was drawn
 * with the unicode flag of the font on; the uniform font of 1.21.1 is the same glyph set. The data comes from the focus
 * through {@link IDislocationHudSource}.</p>
 */
public final class HUDHandler {
    /** 1.7.10 {@code xpos}. */
    static final int X = 4;
    /** 1.7.10 {@code ypos} with Thaumcraft's {@code dialBottom} off. */
    static final int Y = 50;
    /** The translucent black of the three boxes. */
    static final int BACKGROUND = 0x66000000;
    /** The orange of the text. */
    static final int TEXT_COLOR = 0xFFAA00;

    private static final AtomicReference<IDislocationHudSource> SOURCE = new AtomicReference<>();
    private static boolean registered;

    private HUDHandler() {}

    /** Hooks the overlay into the HUD; safe to call twice. */
    public static void register() {
        if (registered) return;
        registered = true;
        ClientGuiEvent.RENDER_HUD.register(HUDHandler::drawDislocationFocusHUD);
    }

    /**
     * Sets the source the overlay asks when the focus item of the wand does not implement {@link IDislocationHudSource}
     * itself (null removes it).
     */
    public static void setDislocationSource(@Nullable IDislocationHudSource source) {
        SOURCE.set(source);
    }

    /** The source for this focus item: the focus itself when it implements the contract, else the one that was set. */
    @Nullable
    static IDislocationHudSource sourceFor(ItemStack focus) {
        if (focus.getItem() instanceof IDislocationHudSource own) return own;
        return SOURCE.get();
    }

    public static void drawDislocationFocusHUD(GuiGraphics graphics, DeltaTracker deltaTracker) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.screen != null || mc.options.hideGui) return;
        LocalPlayer player = mc.player;
        if (player == null) return;

        ItemStack wand = player.getMainHandItem();
        if (!WandHelper.isWand(wand)) return;
        ItemStack focus = WandHelper.getFocusItem(wand);
        if (focus.isEmpty()) return;
        IDislocationHudSource source = sourceFor(focus);
        if (source == null) return;
        ItemStack pickedBlock = source.pickedBlock(wand, focus);
        if (pickedBlock.isEmpty()) return;

        Font font = mc.font;
        Style unicode = Style.EMPTY.withFont(Minecraft.UNIFORM_FONT);
        MutableComponent name = Component.translatable("ttmisc.focusDislocation.tooltip").withStyle(unicode);
        int strLength = font.width(name);

        graphics.fill(X - 1, Y - 1, X + 18, Y + 18, BACKGROUND);

        graphics.fill(X + 18, Y, X + 18 + strLength + 4, Y + 9, BACKGROUND);
        graphics.drawString(font, name, X + 20, Y, TEXT_COLOR, true);

        if (source.hasExtraContents(wand, focus)) {
            MutableComponent content = Component.translatable("ttmisc.focusDislocation.tooltipExtra").withStyle(unicode);
            // 1.7.10 sized this box by the width of the first line only
            graphics.fill(X + 18, Y + 9, X + 18 + strLength + 4, Y + 18, BACKGROUND);
            graphics.drawString(font, content, X + 20, Y + 9, TEXT_COLOR, true);
        }

        graphics.renderItem(pickedBlock, X, Y);
    }
}
