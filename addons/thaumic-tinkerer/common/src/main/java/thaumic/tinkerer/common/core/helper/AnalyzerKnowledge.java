/**
 * This file is new in the Minecraft 1.21.1 port of Thaumic Tinkerer for Thaumcraft 4 Reload (no 1.7.10 counterpart, see
 * addons/thaumic-tinkerer/NOTICE.md). Thaumic Tinkerer was created by Vazkii; the port is distributed under a Creative
 * Commons Attribution-NonCommercial-ShareAlike 3.0 License (http://creativecommons.org/licenses/by-nc-sa/3.0/deed.en_GB)
 *
 * ThaumicTinkerer is a Derivative Work on Thaumcraft 4. Thaumcraft 4 (c) Azanor 2012
 * (http://www.minecraftforum.net/topic/1585216-)
 */
package thaumic.tinkerer.common.core.helper;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import thaumcraft.api.ThaumcraftApiHelper;

/**
 * What the player knows about an item, for the screen of the Aspectalyzer.
 *
 * <p>1.7.10 showed the aspects of the item in the slot only if the player had scanned the item: the screen looked the
 * item hash up in {@code Thaumcraft.proxy.getScannedObjects()} (the keys {@code @hash} and {@code #hash}). The API
 * answers this with {@link ThaumcraftApiHelper#hasScannedItem(Player, ItemStack)}, which covers both markers and
 * works for the client's own player.</p>
 */
public final class AnalyzerKnowledge {
    private AnalyzerKnowledge() {}

    /** True when the player has scanned the item of the stack (a scan or an advanced scan). */
    public static boolean hasScanned(Player player, ItemStack stack) {
        return player != null && !stack.isEmpty() && ThaumcraftApiHelper.hasScannedItem(player, stack);
    }
}
