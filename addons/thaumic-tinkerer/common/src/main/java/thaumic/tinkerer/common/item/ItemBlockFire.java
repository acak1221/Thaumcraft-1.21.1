/**
 * Created for the Thaumic Tinkerer imbued fires (the original header of this file names no author).
 *
 * ThaumicTinkerer is Open Source and distributed under a Creative Commons Attribution-NonCommercial-ShareAlike 3.0
 * License (http://creativecommons.org/licenses/by-nc-sa/3.0/deed.en_GB)
 *
 * ThaumicTinkerer is a Derivative Work on Thaumcraft 4. Thaumcraft 4 (c) Azanor 2012
 * (http://www.minecraftforum.net/topic/1585216-)
 *
 * Ported to Minecraft 1.21.1 for Thaumcraft 4 Reload (see addons/thaumic-tinkerer/NOTICE.md).
 */
package thaumic.tinkerer.common.item;

import net.minecraft.world.item.BlockItem;
import net.minecraft.world.level.block.Block;

/**
 * The item of an imbued fire. 1.7.10 let the fire be placed anywhere ({@code canPlaceBlockAt} answered true) and
 * removed it on the next neighbour change when it had nothing to stand on or burn: {@link #mustSurvive()} is false for
 * the same effect.
 */
public class ItemBlockFire extends BlockItem {

    public ItemBlockFire(Block block, Properties properties) {
        super(block, properties);
    }

    @Override
    protected boolean mustSurvive() {
        return false;
    }
}
