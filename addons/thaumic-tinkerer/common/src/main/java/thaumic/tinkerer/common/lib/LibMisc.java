/**
 * This class was created by <Vazkii>. It's distributed as part of the ThaumicTinkerer Mod.
 *
 * ThaumicTinkerer is Open Source and distributed under a Creative Commons Attribution-NonCommercial-ShareAlike 3.0
 * License (http://creativecommons.org/licenses/by-nc-sa/3.0/deed.en_GB)
 *
 * ThaumicTinkerer is a Derivative Work on Thaumcraft 4. Thaumcraft 4 (c) Azanor 2012
 * (http://www.minecraftforum.net/topic/1585216-)
 *
 * File Created @ [4 Sep 2013, 15:51:36 (GMT)]
 *
 * Ported to Minecraft 1.21.1 for Thaumcraft 4 Reload (see addons/thaumic-tinkerer/NOTICE.md).
 */
package thaumic.tinkerer.common.lib;

import net.minecraft.resources.ResourceLocation;

public final class LibMisc {
    /** 1.7.10: {@code ThaumicTinkerer}, assets under {@code ttinkerer}. Both are {@code thaumictinkerer} now. */
    public static final String MOD_ID = "thaumictinkerer";
    public static final String MOD_NAME = "Thaumic Tinkerer";
    /** The translation key of the creative tab. */
    public static final String CREATIVE_TAB_KEY = "itemGroup." + MOD_ID;

    private LibMisc() {}

    public static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath(MOD_ID, path);
    }
}
