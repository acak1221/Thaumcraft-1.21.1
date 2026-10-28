/**
 * This class was created by <Vazkii>. It's distributed as part of the ThaumicTinkerer Mod.
 *
 * ThaumicTinkerer is Open Source and distributed under a Creative Commons Attribution-NonCommercial-ShareAlike 3.0
 * License (http://creativecommons.org/licenses/by-nc-sa/3.0/deed.en_GB)
 *
 * ThaumicTinkerer is a Derivative Work on Thaumcraft 4. Thaumcraft 4 (c) Azanor 2012
 * (http://www.minecraftforum.net/topic/1585216-)
 *
 * File Created @ [9 Sep 2013, 16:27:10 (GMT)]
 * Ported to Minecraft 1.21.1 for Thaumcraft 4 Reload (see addons/thaumic-tinkerer/NOTICE.md).
 */
package thaumic.tinkerer.common.block.tile.container;

import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.Slot;

/** A menu with the player's inventory under its own slots (3 rows of 9, then the hotbar), at the standard place. */
public abstract class ContainerPlayerInv extends AbstractContainerMenu {

    protected final Inventory playerInv;

    protected ContainerPlayerInv(MenuType<?> type, int containerId, Inventory playerInv) {
        super(type, containerId);
        this.playerInv = playerInv;
    }

    public void initPlayerInv() {
        int ys = getInvYStart();
        int xs = getInvXStart();

        for (int x = 0; x < 3; ++x) for (int y = 0; y < 9; ++y)
            addSlot(new Slot(playerInv, y + x * 9 + 9, xs + y * 18, ys + x * 18));

        for (int x = 0; x < 9; ++x) addSlot(new Slot(playerInv, x, xs + x * 18, ys + 58));
    }

    public int getInvYStart() {
        return 84;
    }

    public int getInvXStart() {
        return 8;
    }
}
