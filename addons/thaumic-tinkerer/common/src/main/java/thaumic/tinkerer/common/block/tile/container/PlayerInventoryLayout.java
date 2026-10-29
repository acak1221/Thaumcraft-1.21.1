/**
 * This file is new in the Minecraft 1.21.1 port of Thaumic Tinkerer for Thaumcraft 4 Reload (no 1.7.10 counterpart, see
 * addons/thaumic-tinkerer/NOTICE.md). Thaumic Tinkerer was created by Vazkii; the port is distributed under a Creative
 * Commons Attribution-NonCommercial-ShareAlike 3.0 License (http://creativecommons.org/licenses/by-nc-sa/3.0/deed.en_GB)
 *
 * ThaumicTinkerer is a Derivative Work on Thaumcraft 4. Thaumcraft 4 (c) Azanor 2012
 * (http://www.minecraftforum.net/topic/1585216-)
 */
package thaumic.tinkerer.common.block.tile.container;

import java.util.function.Consumer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;

/**
 * The player inventory of the machine screens: the layout of {@code ContainerPlayerInv.initPlayerInv} of 1.7.10 (the
 * three rows of the inventory, then the hot bar 58 pixels below the first row; the container-specific slots come first,
 * so the player slots are the indices {@code first..first + 35}).
 */
public final class PlayerInventoryLayout {
    public static final int INV_X_START = 8;
    public static final int INV_Y_START = 84;

    private PlayerInventoryLayout() {}

    public static void addPlayerSlots(Inventory playerInv, Consumer<Slot> adder) {
        for (int row = 0; row < 3; ++row) {
            for (int column = 0; column < 9; ++column) {
                adder.accept(new Slot(playerInv, column + row * 9 + 9, INV_X_START + column * 18, INV_Y_START + row * 18));
            }
        }
        for (int column = 0; column < 9; ++column) {
            adder.accept(new Slot(playerInv, column, INV_X_START + column * 18, INV_Y_START + 58));
        }
    }
}
