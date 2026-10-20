package thaumcraft.common.container;

import dev.architectury.networking.NetworkManager;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import thaumcraft.api.IScribeTools;
import thaumcraft.common.config.ThaumcraftContent;
import thaumcraft.common.items.ItemResearchNotes;
import thaumcraft.common.lib.capabilities.KnowledgeAccess;
import thaumcraft.common.lib.network.ResearchTableCombinePayload;
import thaumcraft.common.lib.network.ResearchTableCopyPayload;
import thaumcraft.common.lib.network.ResearchTableCopyNetworking;
import thaumcraft.common.lib.network.ResearchTableEditPayload;
import thaumcraft.common.lib.network.ResearchTableStatePayload;
import thaumcraft.common.lib.research.ResearchAspectCombination;
import thaumcraft.common.lib.research.ResearchNoteEditing;
import thaumcraft.common.lib.research.ResearchNoteCopying;
import thaumcraft.common.lib.research.ScribingInk;
import thaumcraft.common.lib.utils.DeferredItemDrops;
import thaumcraft.common.tiles.TileResearchTable;

/** Original shared slots/transfer order with authoritative editing, combination and paid discovery copying. */
public final class ContainerResearchTable extends AbstractContainerMenu {
    // Session identity is not a gameplay roll or a secret; it must never consume
    // Level.random, and must not repeat when vanilla reuses a containerId.
    private static final AtomicLong NEXT_SESSION = new AtomicLong();
    private final BlockPos mainPos;
    private final TileResearchTable tile;
    private final Player viewer;
    private long sessionToken;
    private long syncedRevision;
    private long sentRevision = -1L;
    private Map<String, Integer> syncedBonus = Map.of();
    private boolean stateReady;
    private boolean editPending;
    private boolean remoteSlotsReady;

    public ContainerResearchTable(int id, Inventory inventory, TileResearchTable tile) {
        this(id, inventory, tile, tile.getBlockPos(), tile);
    }

    /** Client slots mirror authoritative sync; they are never a second physical inventory. */
    public ContainerResearchTable(int id, Inventory inventory, FriendlyByteBuf data) {
        this(id, inventory, clientSlots(), data.readBlockPos(), null);
    }

    private ContainerResearchTable(int id, Inventory inventory, Container contents, BlockPos pos, TileResearchTable tile) {
        super(ThaumcraftContent.RESEARCH_TABLE_MENU.get(), id);
        this.mainPos = pos.immutable();
        this.tile = tile;
        this.viewer = inventory.player;
        if (tile != null) {
            sessionToken = NEXT_SESSION.updateAndGet(previous -> {
                if (previous == Long.MAX_VALUE) throw new IllegalStateException("Research table sessions exhausted");
                return previous + 1;
            });
        }
        checkContainerSize(contents, 2);
        addSlot(new SlotLimitedByClass(IScribeTools.class, contents, 0, 14, 10));
        // Do not delegate to the automation predicate: original GUI permits
        // solved/discovery notes and future unknown ItemResearchNotes too.
        addSlot(new SlotLimitedByClass(ItemResearchNotes.class, contents, 1, 70, 10));
        for (int row = 0; row < 3; row++) {
            for (int column = 0; column < 9; column++) {
                addSlot(new Slot(inventory, column + row * 9 + 9, 48 + column * 18, 175 + row * 18));
            }
        }
        for (int column = 0; column < 9; column++) addSlot(new Slot(inventory, column, 48 + column * 18, 233));
    }

    public BlockPos mainPos() { return mainPos; }
    public TileResearchTable tile() { return tile; }
    public long sessionToken() { return sessionToken; }
    public long tableRevision() { return tile == null ? syncedRevision : tile.tableRevision(); }
    public int bonusFor(String aspect) { return aspect == null ? 0 : tile == null ? syncedBonus.getOrDefault(aspect, 0) : tile.bonusFor(aspect); }
    public Map<String, Integer> bonusPools() { return tile == null ? syncedBonus : tile.bonusPools(); }

    /** No guessed session or optimistic grid/pool/ink mutation on the client. */
    public boolean editReady() {
        return tile == null && stateReady && !editPending && syncedRevision < Long.MAX_VALUE
                && viewer.containerMenu == this;
    }

    public boolean requestEdit(ResearchNoteEditing.Action action, int q, int r, String aspect) {
        if (!editReady()) return false;
        ResearchTableEditPayload payload;
        try {
            payload = new ResearchTableEditPayload(containerId, sessionToken, syncedRevision, action, q, r, aspect);
        } catch (IllegalArgumentException | NullPointerException invalid) {
            return false;
        }
        ItemStack noteStack = getSlot(1).getItem();
        var note = ItemResearchNotes.data(noteStack);
        if (!noteStack.is(ThaumcraftContent.RESEARCH_NOTE.get()) || note == null || note.complete()
                || !ScribingInk.consumeFromTable(getSlot(0).getItem(), false)) return false;
        var cell = note.cellAt(q, r);
        if (cell == null || (action == ResearchNoteEditing.Action.ERASE ? cell.type() != 2 : cell.type() != 0)) return false;
        if (action == ResearchNoteEditing.Action.PLACE) {
            var knowledge = KnowledgeAccess.get(viewer);
            if (!knowledge.hasDiscoveredAspect(aspect)
                    || (ResearchNoteEditing.queryPool(knowledge.getAspectPoolFor(aspect)) <= 0 && bonusFor(aspect) <= 0)) return false;
        }
        editPending = true;
        try {
            NetworkManager.sendToServer(payload);
        } catch (RuntimeException failure) {
            editPending = false;
            throw failure;
        }
        return true;
    }

    /** Palette selection is presentation only; the server recomputes both debits and discovery. */
    public boolean requestCombine(String left, String right) {
        if (!editReady()) return false;
        ResearchTableCombinePayload payload;
        try {
            payload = new ResearchTableCombinePayload(containerId, sessionToken, syncedRevision, left, right);
        } catch (IllegalArgumentException | NullPointerException invalid) {
            return false;
        }
        if (!ResearchAspectCombination.canCombine(KnowledgeAccess.get(viewer), syncedBonus, left, right)) return false;
        editPending = true;
        try {
            NetworkManager.sendToServer(payload);
        } catch (RuntimeException failure) {
            editPending = false;
            throw failure;
        }
        return true;
    }

    /** State follows native slot/knowledge sync and also acknowledges refused intents. */
    public boolean canCopy() {
        if (!editReady()) return false;
        ItemStack stack = getSlot(1).getItem();
        return stack.is(ThaumcraftContent.DISCOVERY.get()) && ResearchNoteCopying.canPay(KnowledgeAccess.get(viewer), ItemResearchNotes.data(stack), stack.getCount())
                && ResearchTableCopyNetworking.findSupply(viewer.getInventory(), net.minecraft.world.item.Items.PAPER) >= 0
                && ResearchTableCopyNetworking.findSupply(viewer.getInventory(), net.minecraft.world.item.Items.INK_SAC) >= 0;
    }
    public boolean requestCopy() {
        if (!canCopy()) return false;
        var payload = new ResearchTableCopyPayload(containerId, sessionToken, syncedRevision);
        editPending = true;
        try { NetworkManager.sendToServer(payload); }
        catch (RuntimeException failure) { editPending = false; throw failure; }
        return true;
    }

    /** State follows native slot/knowledge sync and also acknowledges refused intents. */
    public void applyState(ResearchTableStatePayload state) {
        if (tile != null || state.containerId() != containerId
                || (stateReady && state.sessionToken() != sessionToken)
                || (stateReady && state.tableRevision() < syncedRevision)) return;
        sessionToken = state.sessionToken();
        syncedRevision = state.tableRevision();
        syncedBonus = state.bonusPools();
        stateReady = true;
        editPending = false;
    }

    @Override public void broadcastChanges() {
        if (tile != null) tile.tableRevision(); // Observe in-place changes BEFORE any listeners/network.
        super.broadcastChanges();
        if (remoteSlotsReady && tile != null && sentRevision != tile.tableRevision()) sendEditState();
    }

    @Override public void sendAllDataToRemote() {
        if (tile != null) tile.tableRevision();
        super.sendAllDataToRemote();
        remoteSlotsReady = true;
        sendEditState();
    }

    private void sendEditState() {
        if (tile == null || !(viewer instanceof ServerPlayer player) || player.containerMenu != this || !stillValid(player)) return;
        long revision = tile.tableRevision();
        NetworkManager.sendToPlayer(player, new ResearchTableStatePayload(containerId, sessionToken, revision, tile.bonusPools()));
        sentRevision = revision;
    }

    @Override public boolean stillValid(Player player) { return tile == null || tile.stillValid(player); }

    @Override public boolean clickMenuButton(Player player, int button) {
        // Original button1 is a no-op; button5 belongs to R10 validation/copy.
        return button == 1 && stillValid(player);
    }

    /** The old container drops its entire cursor on close; do not split copied max-stack-one discoveries into player slots. */
    @Override public void removed(Player player) {
        ItemStack cursor = getCarried();
        if (player instanceof ServerPlayer server && cursor.getItem() instanceof ItemResearchNotes && cursor.getCount() > 1) {
            DeferredItemDrops.enqueue(server.serverLevel(), server.blockPosition(), java.util.List.of(cursor));
            setCarried(ItemStack.EMPTY); // Paid queue owns a copy before the native close path sees the empty cursor.
        }
        super.removed(player);
    }

    @Override public ItemStack quickMoveStack(Player player, int index) {
        if (index < 0 || index >= slots.size() || !stillValid(player)) return ItemStack.EMPTY;
        Slot slot = slots.get(index);
        if (!slot.hasItem()) return ItemStack.EMPTY;
        ItemStack current = slot.getItem();
        ItemStack original = current.copy();
        boolean moved = index < 2 ? moveItemStackTo(current, 2, slots.size(), true) : moveItemStackTo(current, 0, 2, false);
        if (!moved) return ItemStack.EMPTY;
        if (current.isEmpty()) slot.set(ItemStack.EMPTY);
        else slot.setChanged();
        return original;
    }

    @Override protected boolean moveItemStackTo(ItemStack stack, int start, int end, boolean reverse) {
        boolean moved = false;
        int index = reverse ? end - 1 : start;
        if (stack.isStackable()) {
            while (!stack.isEmpty() && (reverse ? index >= start : index < end)) {
                Slot slot = slots.get(index);
                ItemStack target = slot.getItem();
                if (!target.isEmpty() && slot.mayPlace(stack) && ItemStack.isSameItemSameComponents(stack, target)) {
                    int room = stack.getMaxStackSize() - target.getCount();
                    if (room > 0) {
                        int amount = Math.min(room, stack.getCount());
                        target.grow(amount);
                        stack.shrink(amount);
                        slot.setChanged();
                        moved = true;
                    }
                }
                index += reverse ? -1 : 1;
            }
        }
        if (!stack.isEmpty()) {
            index = reverse ? end - 1 : start;
            while (reverse ? index >= start : index < end) {
                Slot slot = slots.get(index);
                if (!slot.hasItem() && slot.mayPlace(stack)) {
                    // Quirk kept: original ContainerResearchTable.mergeItemStack
                    // copies the whole remainder into the first valid empty
                    // slot, instead of modern empty-slot splitting to itemmax1.
                    slot.set(stack.copy());
                    slot.setChanged();
                    stack.setCount(0);
                    moved = true;
                    break;
                }
                index += reverse ? -1 : 1;
            }
        }
        return moved;
    }

    private static SimpleContainer clientSlots() {
        return new SimpleContainer(2) {
            @Override public void setItem(int slot, ItemStack stack) {
                // SimpleContainer's default setter applies itemmax1. An actual
                // server slot sync must retain its full count and components.
                getItems().set(slot, stack);
                setChanged();
            }
        };
    }
}
