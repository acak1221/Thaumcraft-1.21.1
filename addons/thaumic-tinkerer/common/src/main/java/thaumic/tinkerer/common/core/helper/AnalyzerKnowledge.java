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
package thaumic.tinkerer.common.core.helper;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import thaumcraft.api.ThaumcraftApiHelper;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.api.aspects.AspectList;

/**
 * What the player knows about an item, for the screen of the Aspectalyzer.
 *
 * <p>1.7.10 showed the aspects of the item in the slot only if the player had scanned the item: the screen looked the
 * item hash up in {@code Thaumcraft.proxy.getScannedObjects()} (the keys {@code @hash} and {@code #hash}). The list of
 * scanned items is not part of {@code thaumcraft.api}, so this class is a named stub: see
 * {@code docs/api-requests/Machines.md} (request 1, {@code ThaumcraftApiHelper.hasScannedItem}).</p>
 */
public final class AnalyzerKnowledge {
    private AnalyzerKnowledge() {}

    /**
     * STUB for {@code ThaumcraftApiHelper.hasScannedItem(player, stack)}. Until the API answers whether the player
     * scanned the item, the aspects are shown when the player has discovered every aspect the item has (a scan of the
     * item discovers them, but so can other items: this shows slightly more than the original did).
     */
    public static boolean hasScanned(Player player, ItemStack stack, AspectList aspects) {
        if (player == null || stack.isEmpty() || aspects == null) return false;
        for (Aspect aspect : aspects.getAspects()) {
            if (!ThaumcraftApiHelper.hasDiscoveredAspect(player, aspect)) return false;
        }
        return true;
    }
}
