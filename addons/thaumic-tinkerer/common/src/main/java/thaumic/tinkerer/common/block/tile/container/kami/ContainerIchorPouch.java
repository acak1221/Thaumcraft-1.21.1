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

/**
 * The menu of the Bottomless Pouch: 13 x 9 slots of the pouch above the player's inventory (the layout of the 256 x 256
 * texture). The slots are saved into the pouch in the player's hand whenever one changes; the slot of the pouch itself
 * cannot be touched, and the menu closes when the pouch leaves the hand, so the pouch cannot be put into itself.
 */
public class ContainerIchorPouch extends AbstractContainerMenu {
    public static final int SLOTS = ItemIchorPouch.SIZE;
    public static final int PLAYER_X = 48;
    public static final int PLAYER_Y = 177;

    private final PouchContainer pouchInventory;
    private final Player player;
    private final InteractionHand hand;
    /** The menu slot of the held pouch in the hotbar, or -1 (the pouch is in the off hand). */
    private final int blockedSlot;

    /** The client side of {@code MenuRegistry.ofExtended}: the hand comes in the opening packet. */
    public ContainerIchorPouch(int id, Inventory inventory, FriendlyByteBuf buf) {
        this(id, inventory, buf.readBoolean() ? InteractionHand.OFF_HAND : InteractionHand.MAIN_HAND);
    }

    public ContainerIchorPouch(int id, Inventory inventory, InteractionHand hand) {
        super(ItemIchorPouch.MENU.get(), id);
        this.player = inventory.player;
        this.hand = hand;
        this.blockedSlot = hand == InteractionHand.MAIN_HAND ? SLOTS + 27 + inventory.selected : -1;
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
            if (hand == InteractionHand.MAIN_HAND && x == inventory.selected) {
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
        ItemStack pouch = player.getItemInHand(hand);
        if (pouch.getItem() instanceof ItemIchorPouch) {
            ItemStack[] contents = ItemIchorPouch.readContents(pouch);
            for (int i = 0; i < SLOTS; i++) pouchInventory.items().set(i, contents[i]);
        }
        pouchInventory.ready = true;
    }

    public InteractionHand hand() {
        return hand;
    }

    private void save() {
        if (player.level().isClientSide) return;
        ItemStack pouch = player.getItemInHand(hand);
        if (!(pouch.getItem() instanceof ItemIchorPouch)) return;
        ItemStack[] contents = new ItemStack[SLOTS];
        for (int i = 0; i < SLOTS; i++) contents[i] = pouchInventory.getItem(i);
        ItemIchorPouch.writeContents(pouch, contents);
    }

    @Override
    public void clicked(int slotId, int button, ClickType clickType, Player clicker) {
        // 1.7.10 ignored every click on the slot of the pouch, and the GUI refused the hotbar key of that slot (a dupe).
        if (slotId == blockedSlot) return;
        if (clickType == ClickType.SWAP && hand == InteractionHand.MAIN_HAND && button == clicker.getInventory().selected) {
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
        return viewer.getItemInHand(hand).getItem() instanceof ItemIchorPouch;
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
