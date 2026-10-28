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

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import thaumcraft.api.aspects.Aspect;

/**
 * The aspect icon of the screens: the counterpart of {@code UtilsFX.drawTag} of Thaumcraft, which is not part of the
 * API. The glyph (a white texture of 32 by 32 pixels) is tinted with the colour of the aspect and drawn 16 by 16, the
 * amount in half-size letters in the lower right corner.
 */
public final class AspectIcons {
    private AspectIcons() {}

    public static void drawTag(GuiGraphics graphics, Font font, int x, int y, Aspect aspect, int amount) {
        int color = aspect.getColor();
        graphics.setColor((color >> 16 & 255) / 255F, (color >> 8 & 255) / 255F, (color & 255) / 255F, 1F);
        graphics.blit(aspect.getImage(), x, y, 16, 16, 0, 0, 32, 32, 32, 32);
        graphics.setColor(1F, 1F, 1F, 1F);

        if (amount > 0) {
            String text = String.valueOf(amount);
            graphics.pose().pushPose();
            graphics.pose().translate(0, 0, 200);
            graphics.pose().scale(0.5F, 0.5F, 0.5F);
            graphics.drawString(font, text, (x + 16) * 2 - font.width(text), (y + 16) * 2 - font.lineHeight, 0xFFFFFF,
                    true);
            graphics.pose().popPose();
        }
    }
}
