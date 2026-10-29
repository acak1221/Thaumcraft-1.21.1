/**
 * This class was created by <Vazkii>. It's distributed as part of the ThaumicTinkerer Mod.
 *
 * ThaumicTinkerer is Open Source and distributed under a Creative Commons Attribution-NonCommercial-ShareAlike 3.0
 * License (http://creativecommons.org/licenses/by-nc-sa/3.0/deed.en_GB)
 *
 * ThaumicTinkerer is a Derivative Work on Thaumcraft 4. Thaumcraft 4 (c) Azanor 2012
 * (http://www.minecraftforum.net/topic/1585216-)
 *
 * File Created @ [Jan 10, 2014, 4:39:07 PM (GMT)]
 * Ported to Minecraft 1.21.1 for Thaumcraft 4 Reload (see addons/thaumic-tinkerer/NOTICE.md).
 */
package thaumic.tinkerer.common.item.kami;

import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.level.block.Block;

/**
 * The item of the Celestial Gateway: a plain block item in the Kami rarity. 1.7.10's {@code TTCommonProxy.kamiRarity} is a
 * rarity of its own (light purple, "Kami"); {@link Rarity} is a fixed enum in 1.21.1 and {@link Rarity#EPIC} is the same
 * light purple, as for the other Kami items (see docs/deviations/KamiGear.md).
 */
public class ItemBlockWarpGate extends BlockItem {

    public ItemBlockWarpGate(Block block, Item.Properties properties) {
        super(block, properties.rarity(Rarity.EPIC));
    }
}
