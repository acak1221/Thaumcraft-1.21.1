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
package thaumic.tinkerer.client.core.handler;

import dev.architectury.event.events.client.ClientGuiEvent;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.state.BlockState;
import thaumcraft.api.wands.ItemFocusBasic;
import thaumcraft.api.wands.WandHelper;
import thaumic.tinkerer.common.item.foci.FociComponents.DislocationData;
import thaumic.tinkerer.common.item.foci.ItemFocusDislocation;

/**
 * The readout of the Focus of Dislocation (1.7.10 {@code HUDHandler.drawDislocationFocusHUD}): while the player holds a wand
 * whose focus carries a block, a small box under the wand's dial shows the block, "Currently Dislocating" and, when the
 * block has a block entity, "Extra Contents".
 *
 * <p>1.7.10 moved the box to the bottom of the screen with Thaumcraft's {@code dialBottom} setting; the API does not expose
 * that client setting ({@code docs/api-requests/Foci.md}), so the box is at the position of the default dial. 1.7.10 looked
 * at the wand in the main hand only; the off hand's wand counts here too. This is the only overlay of the Dislocation: the
 * copy that BasicItems once ported was deleted.</p>
 */
public final class DislocationHUDHandler {
    private static final int X = 4;
    /** Under the dial of the wand, which is 32 pixels high and starts at the top of the screen. */
    private static final int Y = 50;
    private static final int BACKGROUND = 0x66000000;
    private static final int TEXT_COLOR = 0xFFAA00;
    /** 1.7.10 drew the two lines with {@code font.setUnicodeFlag(true)}: the uniform (unicode) font of 1.21.1. */
    private static final Style UNICODE = Style.EMPTY.withFont(Minecraft.UNIFORM_FONT);

    private DislocationHUDHandler() {}

    public static void register() {
        ClientGuiEvent.RENDER_HUD.register(DislocationHUDHandler::draw);
    }

    /** The wand in the player's hands: the main hand's, else the off hand's. */
    private static ItemStack heldWand(Player player) {
        ItemStack main = player.getMainHandItem();
        if (WandHelper.isWand(main)) return main;
        ItemStack off = player.getOffhandItem();
        return WandHelper.isWand(off) ? off : ItemStack.EMPTY;
    }

    private static void draw(GuiGraphics graphics, DeltaTracker deltaTracker) {
        Minecraft mc = Minecraft.getInstance();
        Player player = mc.player;
        if (player == null || mc.screen != null || mc.options.hideGui) return;

        ItemStack wand = heldWand(player);
        if (wand.isEmpty()) return;
        ItemFocusBasic focus = WandHelper.getFocus(wand);
        if (!(focus instanceof ItemFocusDislocation)) return;
        DislocationData data = ItemFocusDislocation.getData(wand);
        if (data == null) return;

        Font font = mc.font;
        Component name = Component.translatable("ttmisc.focusDislocation.tooltip").withStyle(UNICODE);
        int width = font.width(name);

        graphics.fill(X - 1, Y - 1, X + 18, Y + 18, BACKGROUND);
        graphics.fill(X + 18, Y, X + 18 + width + 4, Y + 9, BACKGROUND);
        graphics.drawString(font, name, X + 20, Y, TEXT_COLOR, true);

        if (data.hasTile()) {
            Component extra = Component.translatable("ttmisc.focusDislocation.tooltipExtra").withStyle(UNICODE);
            int extraWidth = Math.max(width, font.width(extra));
            graphics.fill(X + 18, Y + 9, X + 18 + extraWidth + 4, Y + 18, BACKGROUND);
            graphics.drawString(font, extra, X + 20, Y + 9, TEXT_COLOR, true);
        }

        ItemStack icon = iconOf(data.state());
        if (!icon.isEmpty()) graphics.renderItem(icon, X, Y);
    }

    /** The block as an item; a fluid, which has no block item, as its bucket. */
    private static ItemStack iconOf(BlockState state) {
        Item item = state.getBlock().asItem();
        if (item == Items.AIR && !state.getFluidState().isEmpty()) item = state.getFluidState().getType().getBucket();
        return item == Items.AIR ? ItemStack.EMPTY : new ItemStack(item);
    }
}
