/**
 * This class was created by <Vazkii>. It's distributed as part of the ThaumicTinkerer Mod.
 *
 * ThaumicTinkerer is Open Source and distributed under a Creative Commons Attribution-NonCommercial-ShareAlike 3.0
 * License (http://creativecommons.org/licenses/by-nc-sa/3.0/deed.en_GB)
 *
 * ThaumicTinkerer is a Derivative Work on Thaumcraft 4. Thaumcraft 4 (c) Azanor 2012
 * (http://www.minecraftforum.net/topic/1585216-)
 *
 * File Created @ [14 Sep 2013, 18:35:49 (GMT)]
 * Ported to Minecraft 1.21.1 for Thaumcraft 4 Reload (see addons/thaumic-tinkerer/NOTICE.md).
 */
package thaumic.tinkerer.common.block.tile.container.slot;

import net.minecraft.world.Container;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import thaumcraft.api.wands.WandCap;
import thaumcraft.api.wands.WandHelper;

/**
 * A slot that holds one wand (not a staff) whose cap does not raise the vis cost: caps with a base cost modifier above
 * one (the 1.7.10 {@code SlotWand}; used by the Enchanter's menu).
 */
public class SlotWand extends Slot {

    public SlotWand(Container container, int slot, int x, int y) {
        super(container, slot, x, y);
    }

    @Override
    public boolean mayPlace(ItemStack stack) {
        if (!WandHelper.isWand(stack) || WandHelper.isStaff(stack)) return false;

        WandCap cap = WandHelper.getCap(stack);
        return cap != null && cap.getBaseCostModifier() <= 1F;
    }

    @Override
    public int getMaxStackSize() {
        return 1;
    }
}
