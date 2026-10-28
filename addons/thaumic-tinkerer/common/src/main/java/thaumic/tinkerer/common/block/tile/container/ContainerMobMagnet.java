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
package thaumic.tinkerer.common.block.tile.container;

import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import thaumic.tinkerer.common.block.tile.TileMobMagnet;
import thaumic.tinkerer.common.block.tile.container.slot.SlotMobMagnet;
import thaumic.tinkerer.common.content.Machines;
import thaumic.tinkerer.common.item.ItemSoulMould;

/**
 * The menu of the Corporeal Attractor: the mould slot, the player inventory and the adult / child choice. The choice
 * is a data slot (server to client) and two menu buttons (client to server); this is what the 1.7.10 packet
 * {@code PacketMobMagnetButton} carried.
 */
public class ContainerMobMagnet extends AbstractContainerMenu {

    /** Menu button: search for adults. */
    public static final int BUTTON_ADULT = 0;
    /** Menu button: search for children. */
    public static final int BUTTON_CHILD = 1;

    private final Container mobMagnet;
    private final ContainerData data;

    /** The client side: slot contents and the adult flag arrive from the server. */
    public ContainerMobMagnet(int id, Inventory playerInv) {
        this(id, playerInv, new SimpleContainer(1), new SimpleContainerData(1));
    }

    public ContainerMobMagnet(int id, Inventory playerInv, TileMobMagnet mobMagnet) {
        this(id, playerInv, mobMagnet, mobMagnet.getMenuData());
    }

    private ContainerMobMagnet(int id, Inventory playerInv, Container mobMagnet, ContainerData data) {
        super(Machines.MENU_MOB_MAGNET.get(), id);
        this.mobMagnet = mobMagnet;
        this.data = data;
        checkContainerSize(mobMagnet, 1);
        mobMagnet.startOpen(playerInv.player);

        addSlot(new SlotMobMagnet(mobMagnet, 0, 53, 37));
        PlayerInventoryLayout.addPlayerSlots(playerInv, this::addSlot);
        addDataSlots(data);
    }

    /** True when the magnet searches for adults, false for children. */
    public boolean isAdult() {
        return data.get(TileMobMagnet.DATA_ADULT) != 0;
    }

    @Override
    public boolean clickMenuButton(Player player, int id) {
        if (id != BUTTON_ADULT && id != BUTTON_CHILD) return false;
        data.set(TileMobMagnet.DATA_ADULT, id == BUTTON_ADULT ? 1 : 0);
        return true;
    }

    @Override
    public boolean stillValid(Player player) {
        return mobMagnet.stillValid(player);
    }

    @Override
    public void removed(Player player) {
        super.removed(player);
        mobMagnet.stopOpen(player);
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        ItemStack moved = ItemStack.EMPTY;
        Slot clicked = slots.get(index);

        if (clicked != null && clicked.hasItem()) {
            ItemStack stack = clicked.getItem();

            if (stack.getItem() instanceof ItemSoulMould) {
                moved = stack.copy();

                if (index < 1) {
                    if (!moveItemStackTo(stack, 1, 37, false)) return ItemStack.EMPTY;
                } else if (!moveItemStackTo(stack, 0, 1, false)) return ItemStack.EMPTY;

                if (stack.isEmpty()) clicked.setByPlayer(ItemStack.EMPTY);
                else clicked.setChanged();

                if (stack.getCount() == moved.getCount()) return ItemStack.EMPTY;

                clicked.onTake(player, stack);
            }
        }

        return moved;
    }
}
