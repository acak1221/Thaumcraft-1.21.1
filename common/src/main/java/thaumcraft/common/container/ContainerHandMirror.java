package thaumcraft.common.container;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import thaumcraft.common.config.ThaumcraftDeviceMenus;
import thaumcraft.common.lib.crafting.DeviceContainerRules;
import thaumcraft.common.lib.utils.DeferredItemDrops;

/**
 * Original ContainerHandMirror: one item slot and the player inventory. An
 * item dropped into the slot is transported to the mirror block the hand
 * mirror is linked to, and everything the slot still holds when the GUI
 * closes returns to the player.
 *
 * <p>The item implements the destination bridge. Input ownership leaves the menu before that bridge can call out;
 * the native bridge publishes a paid delivery to the destination's durable queue before returning success.</p>
 */
public final class ContainerHandMirror extends AbstractContainerMenu {
    /** Implemented by the hand mirror item; the container owns no transport. */
    public interface Bridge {
        boolean isMirror(ItemStack stack);
        /** The original transport needs the mirror block link of the item. */
        boolean isLinked(ItemStack mirror);
        /** True transfers ownership; false must publish no output. No exception may escape after publication. */
        boolean transport(ItemStack mirror, ItemStack items, Player player, Level level);
    }

    private final Player viewer;
    private final InteractionHand hand;
    private final ItemStack mirror;
    private final MirrorInventory input;
    private final boolean server;
    private boolean writing, closed;

    public ContainerHandMirror(int id, Inventory inventory, InteractionHand hand) { this(id, inventory, hand, true); }

    public ContainerHandMirror(int id, Inventory inventory, InteractionHand hand, boolean server) {
        super(ThaumcraftDeviceMenus.HAND_MIRROR.get(), id);
        this.viewer = inventory.player;
        this.hand = hand;
        this.server = server;
        this.mirror = viewer.getItemInHand(hand);
        this.input = new MirrorInventory(() -> !writing && !closed, this::onInputChanged);
        addSlot(new Slot(input, 0, DeviceContainerRules.HAND_MIRROR_INPUT_X, DeviceContainerRules.HAND_MIRROR_INPUT_Y) {
            /** Original InventoryHandMirror:74 - the input slot accepts anything. */
            @Override public boolean mayPlace(ItemStack stack) { return DeviceContainerRules.handMirrorSlotAcceptsAnyItem(); }
        });
        bindPlayerInventory(inventory);
        onInputChanged();
    }

    public ContainerHandMirror(int id, Inventory inventory, FriendlyByteBuf buffer) {
        this(id, inventory, buffer.readBoolean() ? InteractionHand.OFF_HAND : InteractionHand.MAIN_HAND, false);
    }

    /** Original ContainerHandMirror:43-52. */
    void bindPlayerInventory(Inventory inventory) {
        for (int row = 0; row < 3; row++) {
            for (int column = 0; column < 9; column++) {
                addSlot(new Slot(inventory, column + row * 9 + 9, 8 + column * 18, DeviceContainerRules.HAND_MIRROR_PLAYER_Y + row * 18));
            }
        }
        for (int column = 0; column < 9; column++) addSlot(new Slot(inventory, column, 8 + column * 18, DeviceContainerRules.HAND_MIRROR_HOTBAR_Y));
    }

    private Bridge bridge() {
        return mirror.getItem() instanceof Bridge bridge && bridge.isMirror(mirror) ? bridge : null;
    }

    /**
     * Original ContainerHandMirror:55-66. A stack that equals the mirror opens
     * the player inventory, any other stack is transported to the linked
     * mirror block and leaves the slot empty.
     */
    private void onInputChanged() {
        if (writing || closed || !server) return;
        ItemStack stack = input.getItem(0);
        if (stack.isEmpty()) return;
        if (DeviceContainerRules.handMirrorShowsLinkedInventory(stack.isEmpty(), ItemStack.matches(stack, mirror))) {
            openLinkedInventory();
            return;
        }
        Bridge bridge = bridge();
        if (bridge == null) return;
        if (!DeviceContainerRules.handMirrorCanTransport(server, bridge.isLinked(mirror))) return;
        // Relinquish the live slot BEFORE a link lookup, sound, queue publication or ADD-related callback can close
        // or reenter this menu. The detached actual input has exactly one refund right until the bridge accepts it.
        writing = true;
        ItemStack owned = input.takeRaw();
        boolean accepted = false;
        try {
            accepted = bridge.transport(mirror, owned.copy(), viewer, viewer.level());
        } finally {
            if (!accepted) {
                if (closed) returnStock(owned);
                else input.restoreRaw(owned);
            }
            writing = false;
        }
        if (!closed) broadcastChanges();
    }

    /**
     * Original ContainerHandMirror:65 - the mirror GUI is replaced by the
     * player inventory. The modern client owns its own inventory screen, so
     * the server only closes the mirror menu.
     */
    private void openLinkedInventory() {
        if (viewer instanceof ServerPlayer serverPlayer) serverPlayer.doCloseContainer();
    }

    public ItemStack mirror() { return mirror; }
    public InteractionHand hand() { return hand; }
    public ItemStack inputStack() { return input.getItem(0); }

    @Override public boolean stillValid(Player player) { return !closed && player == viewer && !player.isSpectator(); }

    private boolean active(Player player) {
        return !writing && player == viewer && (!server || viewer.containerMenu == this && stillValid(player));
    }

    @Override public void clicked(int slot, int button, ClickType type, Player player) {
        if (active(player)) super.clicked(slot, button, type, player);
    }

    /** Original ContainerHandMirror:73-77 - a mirror never travels into the slot. */
    @Override public ItemStack quickMoveStack(Player player, int index) {
        if (!active(player) || index < 0 || index >= slots.size()) return ItemStack.EMPTY;
        Slot slot = slots.get(index);
        if (!slot.hasItem() || !slot.mayPickup(player)) return ItemStack.EMPTY;
        ItemStack stack = slot.getItem();
        if (index != 0 && !DeviceContainerRules.handMirrorQuickMoveAllowed(isMirror(stack))) return ItemStack.EMPTY;
        ItemStack original = stack.copy();
        boolean moved = index == 0
                ? moveItemStackTo(stack, 1, slots.size(), true)
                : moveItemStackTo(stack, 0, 1, false);
        if (!moved || stack.getCount() == original.getCount()) return ItemStack.EMPTY;
        if (stack.isEmpty()) slot.set(ItemStack.EMPTY); else slot.setChanged();
        slot.onTake(player, stack);
        return original;
    }

    private boolean isMirror(ItemStack stack) {
        Bridge bridge = bridge();
        return bridge != null && bridge.isMirror(stack);
    }

    /** Original ContainerHandMirror:98-105 - the slot content returns to the player. */
    @Override public void removed(Player player) {
        if (closed || player != viewer) return;
        closed = true;
        super.removed(player);
        if (!server) return;
        ItemStack stack = input.takeRaw();
        returnStock(stack);
    }

    private void returnStock(ItemStack stack) {
        if (stack.isEmpty()) return;
        if (viewer instanceof ServerPlayer player && player.isAlive() && !player.hasDisconnected()) {
            player.getInventory().add(stack);
        }
        if (!stack.isEmpty() && viewer.level() instanceof ServerLevel level) {
            DeferredItemDrops.enqueue(level, viewer.blockPosition(), java.util.List.of(stack));
        }
    }

    /** Original InventoryHandMirror: the one slot mirror of the hand mirror. */
    private static final class MirrorInventory extends SimpleContainer {
        private final Runnable onChanged;
        private final java.util.function.BooleanSupplier writable;
        MirrorInventory(java.util.function.BooleanSupplier writable, Runnable onChanged) {
            super(DeviceContainerRules.HAND_MIRROR_INPUT_SLOTS); this.writable = writable; this.onChanged = onChanged;
        }
        ItemStack takeRaw() { return super.removeItemNoUpdate(0); }
        void restoreRaw(ItemStack stack) { super.setItem(0, stack); }
        @Override public int getMaxStackSize() { return DeviceContainerRules.HAND_MIRROR_STACK_LIMIT; }
        /** Original InventoryHandMirror:57 - every change notifies the container. */
        @Override public void setItem(int slot, ItemStack stack) { if (writable.getAsBoolean()) { super.setItem(slot, stack); onChanged.run(); } }
        @Override public ItemStack removeItem(int slot, int amount) { if (!writable.getAsBoolean()) return ItemStack.EMPTY; ItemStack taken = super.removeItem(slot, amount); onChanged.run(); return taken; }
        @Override public ItemStack removeItemNoUpdate(int slot) { if (!writable.getAsBoolean()) return ItemStack.EMPTY; ItemStack taken = super.removeItemNoUpdate(slot); onChanged.run(); return taken; }
    }
}
