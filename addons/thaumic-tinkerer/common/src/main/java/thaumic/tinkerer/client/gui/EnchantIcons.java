/**
 * This file is new in the Minecraft 1.21.1 port of Thaumic Tinkerer for Thaumcraft 4 Reload (no 1.7.10 counterpart, see
 * addons/thaumic-tinkerer/NOTICE.md). Thaumic Tinkerer was created by Vazkii; the port is distributed under a Creative
 * Commons Attribution-NonCommercial-ShareAlike 3.0 License (http://creativecommons.org/licenses/by-nc-sa/3.0/deed.en_GB)
 *
 * ThaumicTinkerer is a Derivative Work on Thaumcraft 4. Thaumcraft 4 (c) Azanor 2012
 * (http://www.minecraftforum.net/topic/1585216-)
 */
package thaumic.tinkerer.client.gui;

import com.mojang.blaze3d.systems.RenderSystem;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;

/**
 * The icons of the enchantments on the buttons of the Enchanter's screen. The textures of 1.7.10 come in two sizes (32
 * pixels, and 512 pixels for the Thaumic Tinkerer ones), and the screen drew whichever it was onto 16 by 16 pixels with the
 * texture coordinates 0 to 1 ({@code drawTexturedModalRect16}); 1.21.1 needs the size of the texture to do the same, which is
 * read from the header of the PNG once per resource load ({@link #clear} runs when the resources reload).
 */
public final class EnchantIcons {
    /** The size of an icon on the screen. */
    public static final int SIZE = 16;

    private static final Map<ResourceLocation, Integer> SIZES = new HashMap<>();

    private EnchantIcons() {}

    /** Draws the whole texture of the icon onto 16 by 16 pixels at the position. */
    public static void draw(GuiGraphics graphics, ResourceLocation texture, int x, int y) {
        int size = sizeOf(texture);
        RenderSystem.enableBlend();
        graphics.blit(texture, x, y, SIZE, SIZE, 0F, 0F, size, size, size, size);
        RenderSystem.disableBlend();
    }

    /** The width (the textures are square) of a PNG of the resource packs; 32 when it cannot be read. */
    public static int sizeOf(ResourceLocation texture) {
        return SIZES.computeIfAbsent(texture, EnchantIcons::readSize);
    }

    /**
     * Forgets every size read so far (and the 32 that stands for a texture that could not be read). Called when the resources
     * reload ({@code EnchantmentsClient}): a resource pack may bring icons of another size, and the buttons would draw a
     * wrong part of them or a stretched icon with the size of the old texture.
     */
    public static void clear() {
        SIZES.clear();
    }

    private static int readSize(ResourceLocation texture) {
        try (InputStream in = Minecraft.getInstance().getResourceManager().getResource(texture).orElseThrow().open()) {
            byte[] header = in.readNBytes(24);
            return pngWidth(header);
        } catch (IOException | RuntimeException e) {
            return 32;
        }
    }

    /** The width in the IHDR chunk of a PNG (bytes 16 to 19); 32 for anything that is no PNG header. */
    public static int pngWidth(byte[] header) {
        if (header.length < 24 || header[1] != 'P' || header[2] != 'N' || header[3] != 'G') return 32;
        int width = ((header[16] & 0xFF) << 24) | ((header[17] & 0xFF) << 16) | ((header[18] & 0xFF) << 8) | (header[19] & 0xFF);
        return width > 0 ? width : 32;
    }
}
