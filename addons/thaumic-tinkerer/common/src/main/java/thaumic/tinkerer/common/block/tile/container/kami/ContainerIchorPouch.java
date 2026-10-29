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
package thaumic.tinkerer.common.block.tile.container.kami;

import java.util.Objects;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import thaumic.tinkerer.common.block.tile.container.slot.kami.SlotNoPouches;
import thaumic.tinkerer.common.item.kami.ItemIchorPouch;
import thaumic.tinkerer.common.item.kami.KamiComponents;

/**
 * The menu of the Bottomless Pouch: 13 x 9 slots of the pouch above the player's inventory (the layout of the 256 x 256
 * texture). The menu holds the pouch stack it was opened for (1.7.10: the {@code pouch} field) and saves the slots into
 * that stack whenever one changes; it never looks at the hand again, so changing the selected hotbar slot (a client may
 * send that packet while a menu is open) cannot redirect the writes into another pouch. The slot of the pouch itself
 * cannot be touched, and the menu closes when the pouch leaves its inventory slot or when something else writes the pouch's
 * contents behind the open menu, so the pouch cannot be put into itself or duplicated.
 */
public class ContainerIchorPouch extends AbstractContainerMenu {
    public static final int SLOTS = ItemIchorPouch.SIZE;
    public static final int PLAYER_X = 48;
    public static final int PLAYER_Y = 177;

    private final PouchContainer pouchInventory;
    private final Player player;
    private final InteractionHand hand;
    /** The pouch the menu was opened for: on the server the very stack object of the inventory slot {@link #pouchSlot}. */
    private final ItemStack pouch;
    /** The inventory index of the pouch: the selected hotbar slot at opening, or the off hand slot. */
    private final int pouchSlot;
    /** The menu slot of the held pouch in the hotbar, or -1 (the pouch is in the off hand). */
    private final int blockedSlot;
    /** The contents component as this menu last read or wrote it; another value means someone else wrote the pouch. */
    private Object lastSeen;

    /** The client side of {@code MenuRegistry.ofExtended}: the hand comes in the opening packet. */
    public ContainerIchorPouch(int id, Inventory inventory, FriendlyByteBuf buf) {
        this(id, inventory, buf.readBoolean() ? InteractionHand.OFF_HAND : InteractionHand.MAIN_HAND);
    }

    public ContainerIchorPouch(int id, Inventory inventory, InteractionHand hand) {
        super(ItemIchorPouch.MENU.get(), id);
        this.player = inventory.player;
        this.hand = hand;
        this.pouchSlot = hand == InteractionHand.MAIN_HAND ? inventory.selected : Inventory.SLOT_OFFHAND;
        this.pouch = inventory.getItem(pouchSlot);
        this.blockedSlot = hand == InteractionHand.MAIN_HAND ? SLOTS + 27 + pouchSlot : -1;
        this.pouchInventory = new PouchContainer();

        for (int y = 0; y < ItemIchorPouch.ROWS; y++) {
            for (int x = 0; x < ItemIchorPouch.COLUMNS; x++) {
                addSlot(new SlotNoPouches(pouchInventory, y * ItemIchorPouch.COLUMNS + x, 12 + x * 18, 8 + y * 18));
            }
        }
        for (int y = 0; y < 3; y++) {
            for (int x = 0; x < 9; x++) {
                addSlot(new Slot(inventory, x + y * 9 + 9, PLAYER_X + x * 18, PLAYER_Y + y * 18));
            }
        }
        for (int x = 0; x < 9; x++) {
            if (hand == InteractionHand.MAIN_HAND && x == pouchSlot) {
                addSlot(new Slot(inventory, x, PLAYER_X + x * 18, PLAYER_Y + 58) {
                    @Override
                    public boolean mayPickup(Player player) {
                        return false;
                    }

                    @Override
                    public boolean mayPlace(ItemStack stack) {
                        return false;
                    }
                });
            } else {
                addSlot(new Slot(inventory, x, PLAYER_X + x * 18, PLAYER_Y + 58));
            }
        }

        // The server holds the truth; the client's copy is overwritten by the slot synchronisation.
        if (pouch.getItem() instanceof ItemIchorPouch) {
            ItemStack[] contents = ItemIchorPouch.readContents(pouch);
            for (int i = 0; i < SLOTS; i++) pouchInventory.items().set(i, contents[i]);
        }
        lastSeen = pouch.get(KamiComponents.POUCH_CONTENTS.get());
        pouchInventory.ready = true;
    }

    public InteractionHand hand() {
        return hand;
    }

    /** True when something other than this menu wrote the contents of the pouch since the menu last saw them (server side). */
    private boolean changedBehindTheMenu() {
        Object now = pouch.get(KamiComponents.POUCH_CONTENTS.get());
        return now != lastSeen && !Objects.equals(now, lastSeen);
    }

    private void save() {
        if (player.level().isClientSide) return;
        // The captured stack only: never "whatever is in the hand now". A pouch that has left the game (count 0) is skipped.
        if (!(pouch.getItem() instanceof ItemIchorPouch) || changedBehindTheMenu()) return;
        ItemStack[] contents = new ItemStack[SLOTS];
        for (int i = 0; i < SLOTS; i++) contents[i] = pouchInventory.getItem(i);
        ItemIchorPouch.writeContents(pouch, contents);
        lastSeen = pouch.get(KamiComponents.POUCH_CONTENTS.get());
    }

    @Override
    public void clicked(int slotId, int button, ClickType clickType, Player clicker) {
        // 1.7.10 ignored every click on the slot of the pouch, and the GUI refused the hotbar key of that slot (a dupe).
        if (slotId == blockedSlot) return;
        // The number key of the hotbar slot of the pouch, or the off hand key for a pouch in the off hand, would move it.
        if (clickType == ClickType.SWAP && button == pouchSlot) {
            return;
        }
        super.clicked(slotId, button, clickType, clicker);
    }

    @Override
    public ItemStack quickMoveStack(Player clicker, int index) {
        if (index == blockedSlot) return ItemStack.EMPTY;
        Slot slot = slots.get(index);
        if (slot == null || !slot.hasItem()) return ItemStack.EMPTY;
        ItemStack stack = slot.getItem();
        ItemStack copy = stack.copy();
        if (index < SLOTS) {
            if (!moveItemStackTo(stack, SLOTS, slots.size(), true)) return ItemStack.EMPTY;
        } else if (SlotNoPouches.isPouch(stack) || !moveItemStackTo(stack, 0, SLOTS, false)) {
            return ItemStack.EMPTY;
        }
        if (stack.isEmpty()) slot.setByPlayer(ItemStack.EMPTY);
        else slot.setChanged();
        return copy;
    }

    @Override
    public void removed(Player closer) {
        super.removed(closer);
        save();
    }

    @Override
    public boolean stillValid(Player viewer) {
        ItemStack inSlot = viewer.getInventory().getItem(pouchSlot);
        // The client's stack objects are replaced by every synchronisation; only the server can tell by identity.
        if (viewer.level().isClientSide) return inSlot.getItem() instanceof ItemIchorPouch;
        return inSlot == pouch && pouch.getItem() instanceof ItemIchorPouch && !changedBehindTheMenu();
    }

    /** The 117 slots: a container that saves into the pouch whenever it changes (on the server). */
    private final class PouchContainer extends SimpleContainer {
        private boolean ready;

        PouchContainer() {
            super(SLOTS);
        }

        net.minecraft.core.NonNullList<ItemStack> items() {
            return getItems();
        }

        @Override
        public int getMaxStackSize() {
            return 64;
        }

        @Override
        public boolean canPlaceItem(int slot, ItemStack stack) {
            return !SlotNoPouches.isPouch(stack);
        }

        @Override
        public void setChanged() {
            super.setChanged();
            if (ready) save();
        }
    }
}
