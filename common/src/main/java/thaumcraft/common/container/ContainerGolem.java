package thaumcraft.common.container;

import dev.architectury.registry.menu.MenuRegistry;
import java.util.UUID;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import thaumcraft.common.config.ThaumcraftGolems;
import thaumcraft.common.entities.golems.EntityGolemBase;
import thaumcraft.common.items.golems.GolemData;
import thaumcraft.common.items.golems.GolemGuardTargets;
import thaumcraft.common.items.golems.GolemItemMatch;
import thaumcraft.common.items.golems.GolemMarker;

/** Item cores show six ghosts per page (every Fire upgrade adds a page), Decanting one, Sorting/Guard none. Ghosts own no stock.
 * Paging is a server menu button (66 previous, 67 next); the page and slot count reach the client as menu data. */
public final class ContainerGolem extends AbstractContainerMenu {
    public static final int IMAGE_WIDTH = 176, IMAGE_HEIGHT = 166, GHOSTS = 6, DATA = 21;
    /** Original ContainerGolem:84-90: the page arrows are the menu buttons 66 (previous) and 67 (next). */
    public static final int PREVIOUS_PAGE = 66, NEXT_PAGE = 67;
    /** Order Guard target switches keep the original ids 51 (Monsters), 52 (Animals), 53 (Players), 54 (Creepers); see GolemGuardTargets. */
    public static final int GUARD_BUTTON = GolemGuardTargets.FIRST_BUTTON;
    /** Entropy's item-comparison switches keep the original ids 55 (tags), 56 (damage), 57 (components); see GolemItemMatch. */
    public static final int ITEM_MATCH_BUTTON = GolemItemMatch.FIRST_BUTTON;
    private final EntityGolemBase serverGolem;
    private final int entityId;
    private final UUID entityUuid;
    private final Player viewer;
    private final ContainerData configuration;
    private final int openingCore, ghostSlots;
    private final SimpleContainer filters = new SimpleContainer(GHOSTS);
    private final int[] serverPage; // The one authoritative page; null on the client, which reads the synced copy.
    private boolean clicking;

    public ContainerGolem(int id, Inventory inventory, EntityGolemBase golem) {
        this(id, inventory, golem.getId(), golem.getUUID(), golem, new int[1], golem.getGolemCore());
        refreshFilters();
        golem.startConfiguration(inventory.player);
    }
    public ContainerGolem(int id, Inventory inventory, FriendlyByteBuf buffer) {
        this(id, inventory, buffer.readVarInt(), buffer.readUUID(), null, null, buffer.readVarInt());
        configuration.set(7, openingCore);
    }
    private ContainerGolem(int id, Inventory inventory, int entityId, UUID entityUuid, EntityGolemBase golem, int[] page, int openingCore) {
        super(ThaumcraftGolems.GOLEM_MENU.get(), id);
        this.serverGolem = golem; this.entityId = entityId; this.entityUuid = entityUuid; viewer = inventory.player;
        this.serverPage = page;
        this.configuration = golem == null ? new SimpleContainerData(DATA) : serverData(golem, page);
        this.openingCore = openingCore;
        ghostSlots = openingCore == GolemData.SORTING || openingCore == GolemData.GUARD ? 0
                : openingCore == GolemData.LIQUID ? 1 : GHOSTS;
        checkContainerDataCount(configuration, DATA);
        for (int index = 0; index < ghostSlots; index++) {
            addSlot(new Slot(filters, index, 100 + index / 2 * 28, 16 + index % 2 * 31) {
                @Override public boolean mayPickup(Player player) { return false; }
                @Override public boolean mayPlace(ItemStack stack) { return false; }
                @Override public int getMaxStackSize() { return 1; }
                @Override public int getMaxStackSize(ItemStack stack) { return 1; }
            });
        }
        for (int row = 0; row < 3; row++) for (int column = 0; column < 9; column++)
            addSlot(new Slot(inventory, column + row * 9 + 9, 8 + column * 18, 84 + row * 18));
        for (int column = 0; column < 9; column++) addSlot(new Slot(inventory, column, 8 + column * 18, 142));
        addDataSlots(configuration);
    }

    private static ContainerData serverData(EntityGolemBase golem, int[] page) {
        return new ContainerData() {
            @Override public int get(int index) {
                return index >= 0 && index < GHOSTS ? golem.getWantedCount(page[0] * GHOSTS + index)
                        : index == 6 ? (golem.isFillAnyAmount() ? 1 : 0) : index == 7 ? golem.getGolemCore()
                        : index == 8 ? page[0] : index == 9 ? golem.getSlotCount()
                        : index >= 10 && index < 10 + GHOSTS ? golem.getSlotColor(page[0] * GHOSTS + index - 10)
                        : index == 16 ? (golem.canColorSlots() ? 1 : 0)
                        : index == 17 ? golem.getGuardTargets() : index == 18 ? (golem.canSwitchGuardTargets() ? 1 : 0)
                        : index == 19 ? golem.getItemMatch() : index == 20 ? (golem.canSwitchItemMatch() ? 1 : 0) : 0;
            }
            @Override public void set(int index, int value) {} // Display sync, never a C2S configuration setter.
            @Override public int getCount() { return DATA; }
        };
    }
    /** Wanted count of the {@code slot}-th ghost on the CURRENT page. */
    public int getWantedCount(int slot) { return slot >= 0 && slot < GHOSTS ? configuration.get(slot) : 0; }
    /** Zero-based page shown, and the number of pages the golem's slot count provides. */
    public int page() { return configuration.get(8); }
    /** The golem's own slot count (the original's {@code mobInv.slotCount}); the Order colour button ids use it. */
    public int slotCount() { return configuration.get(9); }
    /** Order colour (-1 any, 0..15) of the {@code slot}-th ghost on the current page, and whether colours can be edited at all. */
    public int slotColor(int slot) { return slot >= 0 && slot < GHOSTS ? configuration.get(10 + slot) : -1; }
    public boolean canColorSlots() { return ghostSlots != 0 && openingCore != GolemData.LIQUID && configuration.get(16) != 0; }
    /** The Guard's target mask (GolemGuardTargets) as the server last synced it, and whether the switches exist at all. */
    public int guardTargets() { return configuration.get(17); }
    public boolean canSwitchGuardTargets() { return openingCore == GolemData.GUARD && configuration.get(18) != 0; }
    /** Entropy's switches (GolemItemMatch bits) as the server last synced them, and whether the golem has them at all. */
    public int itemMatch() { return configuration.get(19); }
    public boolean canSwitchItemMatch() { return GolemItemMatch.appliesTo(openingCore) && configuration.get(20) != 0; }
    public int pageCount() { return openingCore == GolemData.LIQUID || ghostSlots == 0 ? 1 : Math.max(1, (configuration.get(9) + GHOSTS - 1) / GHOSTS); }
    private int base() { return serverPage == null ? 0 : serverPage[0] * GHOSTS; }
    public boolean isFillAnyAmount() { return configuration.get(6) != 0; }
    public int core() { return configuration.get(7); }
    public boolean hasGhostFilters() { return ghostSlots != 0; }
    public int ghostSlotCount() { return ghostSlots; }

    public static InteractionResult open(ServerPlayer player, EntityGolemBase golem) {
        if (!player.server.isSameThread() || !golem.canConfigure(player) || !golem.hasConfigurationMenu()) return InteractionResult.FAIL;
        MenuRegistry.openExtendedMenu(player, new SimpleMenuProvider(
                (id, inventory, viewer) -> new ContainerGolem(id, inventory, golem), golem.getDisplayName()),
                buffer -> { buffer.writeVarInt(golem.getId()); buffer.writeUUID(golem.getUUID()); buffer.writeVarInt(golem.getGolemCore()); });
        return InteractionResult.CONSUME;
    }

    public EntityGolemBase golem() {
        if (serverGolem != null) return serverGolem;
        var entity = viewer.level().getEntity(entityId);
        return entity instanceof EntityGolemBase golem && entityUuid.equals(golem.getUUID()) ? golem : null;
    }
    @Override public boolean stillValid(Player player) {
        EntityGolemBase golem = golem();
        return player == viewer && !player.isSpectator() && golem != null && entityUuid.equals(golem.getUUID())
                && golem.canConfigure(player) && golem.hasConfigurationMenu() && golem.getGolemCore() == openingCore;
    }
    private boolean active(Player player) {
        return serverGolem != null && player instanceof ServerPlayer sender && sender.server.isSameThread()
                && sender.containerMenu == this && stillValid(player)
                && sender.serverLevel().getEntity(entityUuid) == serverGolem;
    }
    private void refreshFilters() {
        if (serverGolem == null || ghostSlots == 0) return;
        // A dismantled Fire page must not leave the menu on a page that no longer exists.
        int pages = Math.max(1, (serverGolem.getSlotCount() + GHOSTS - 1) / GHOSTS);
        if (serverPage[0] >= pages) serverPage[0] = pages - 1;
        for (int index = 0; index < ghostSlots; index++) {
            ItemStack exemplar = serverGolem.getFilter(base() + index);
            if (!ItemStack.matches(exemplar, filters.getItem(index))) filters.setItem(index, exemplar.copy());
        }
    }
    @Override public void broadcastChanges() { refreshFilters(); super.broadcastChanges(); }

    @Override public void clicked(int index, int button, ClickType type, Player player) {
        if (clicking || !active(player)) return;
        clicking = true;
        try {
            if (index >= slots.size()) return;
            if (index < 0 || index >= ghostSlots) { super.clicked(index, button, type, player); return; }
            if (button < 0 || button > 1) return;
            int entry = base() + index; // The golem's own slot behind this page's ghost.
            ItemStack existing = serverGolem.getFilter(entry), cursor = getCarried();
            if (openingCore == GolemData.LIQUID) {
                ItemStack next;
                if (type == ClickType.QUICK_MOVE && button == 0 || type == ClickType.PICKUP && cursor.isEmpty()) {
                    next = ItemStack.EMPTY;
                } else if (type == ClickType.PICKUP && GolemData.isSupportedWaterFilter(cursor)) {
                    next = cursor.copy();
                } else return;
                serverGolem.setFilter(0, next);
                // No wanted quantity, cursor payment or real container transfer.
                refreshFilters();
                broadcastChanges();
                return;
            }
            ItemStack next = existing;
            int limit = serverGolem.getGolemCore() == GolemData.FILL ? 256 : 1;
            int wanted = existing.isEmpty() ? 0 : serverGolem.getWantedCount(entry);
            if (type == ClickType.QUICK_MOVE) {
                if (button == 0) { next = ItemStack.EMPTY; wanted = 0; }
                else if (!existing.isEmpty()) wanted = Math.min(limit, wanted + 16);
            } else if (type == ClickType.PICKUP) {
                if (existing.isEmpty() && !cursor.isEmpty()) {
                    next = cursor.copyWithCount(1);
                    wanted = Math.min(limit, button == 0 ? cursor.getCount() : 1);
                } else if (!existing.isEmpty() && cursor.isEmpty()) {
                    wanted = Math.max(0, Math.min(limit, wanted + (button == 0 ? -1 : 1)));
                    if (wanted == 0) next = ItemStack.EMPTY;
                } else if (!cursor.isEmpty() && ItemStack.isSameItemSameComponents(existing, cursor)) {
                    wanted = Math.min(limit, wanted + (button == 0 ? cursor.getCount() : 1));
                } else if (!cursor.isEmpty() && (limit > 1 || cursor.getCount() == 1)) {
                    next = cursor.copyWithCount(1);
                    wanted = Math.min(limit, button == 0 ? cursor.getCount() : 1);
                }
            } else return;
            // A ghost click never shrinks the cursor, returns a filter, or writes player inventory.
            serverGolem.setFilter(entry, next);
            serverGolem.setWantedCount(entry, next.isEmpty() ? 0 : wanted);
            refreshFilters();
            broadcastChanges();
        } finally { clicking = false; }
    }
    @Override public boolean canDragTo(Slot slot) { return slot.index >= ghostSlots; }
    @Override public boolean canTakeItemForPickAll(ItemStack stack, Slot slot) { return slot.index >= ghostSlots; }
    @Override public boolean clickMenuButton(Player player, int button) {
        if (!active(player)) return false;
        if (button == PREVIOUS_PAGE || button == NEXT_PAGE) {
            int pages = Math.max(1, (serverGolem.getSlotCount() + GHOSTS - 1) / GHOSTS);
            int next = serverPage[0] + (button == NEXT_PAGE ? 1 : -1);
            if (openingCore == GolemData.LIQUID || ghostSlots == 0 || next < 0 || next >= pages) return false;
            serverPage[0] = next;
            refreshFilters();
            broadcastChanges();
            return true;
        }
        if (adjustColor(button)) return true;
        int category = GolemGuardTargets.categoryOfButton(button);
        if (category != 0) {
            // Server-side gate: only a Guard that carries Order has switches, whatever a client sends.
            if (openingCore != GolemData.GUARD || serverGolem.getGolemCore() != GolemData.GUARD || !serverGolem.toggleGuardTarget(category)) return false;
            broadcastChanges();
            return true;
        }
        int flag = GolemItemMatch.flagOfButton(button);
        if (flag != 0) {
            // Server-side gate: only a Fill/Empty/Gather/Use/Sorting golem carrying Entropy has switches, whatever a client sends.
            if (!GolemItemMatch.appliesTo(openingCore) || serverGolem.getGolemCore() != openingCore || !serverGolem.toggleItemMatch(flag)) return false;
            broadcastChanges();
            return true;
        }
        if (button != 50 || serverGolem.getGolemCore() != GolemData.FILL) return false;
        serverGolem.setFillAnyAmount(!serverGolem.isFillAnyAmount());
        broadcastChanges();
        return true;
    }

    /**
     * The Order colour buttons of the original menu.
     *
     * <p>Original {@code ContainerGolem.onButtonClick}:96-109 — a button id <em>below</em> the golem's slot count lowers the
     * colour of that very slot, and an id from the slot count up to twice the slot count raises it. The colour wraps the
     * original way: lowering turns {@code -1} (any) into {@code 15} and raises {@code 15} into {@code -1}.</p>
     *
     * @return whether the button was an Order colour button of this golem
     */
    private boolean adjustColor(int button) {
        int slots = serverGolem.getSlotCount();
        if (slots <= 0 || !serverGolem.canColorSlots()) return false;
        int slot, step;
        if (button >= 0 && button < slots) { slot = button; step = -1; }
        else if (button >= slots && button < slots * 2) { slot = button - slots; step = 1; }
        else return false;
        int current = serverGolem.getSlotColor(slot);
        int next = current + step;
        if (step < 0 && next < GolemMarker.ANY) next = GolemMarker.LAST_COLOR;
        if (step > 0 && next > GolemMarker.LAST_COLOR) next = GolemMarker.ANY;
        if (!serverGolem.setSlotColor(slot, next)) return false;
        broadcastChanges();
        return true;
    }

    @Override public ItemStack quickMoveStack(Player player, int index) {
        if (!active(player) || index < ghostSlots || index >= slots.size()) return ItemStack.EMPTY;
        Slot slot = slots.get(index);
        if (!slot.hasItem()) return ItemStack.EMPTY;
        ItemStack stack = slot.getItem(), before = stack.copy();
        // Original transferStackInSlot could turn ghost exemplars into real stock.
        // Keep shift movement strictly within the player's real inventory instead.
        boolean moved = index < ghostSlots + 27 ? moveItemStackTo(stack, ghostSlots + 27, slots.size(), false)
                : moveItemStackTo(stack, ghostSlots, ghostSlots + 27, false);
        if (!moved) return ItemStack.EMPTY;
        slot.set(stack.isEmpty() ? ItemStack.EMPTY : stack);
        slot.onTake(player, stack);
        return before;
    }
    @Override public void removed(Player player) {
        if (serverGolem != null) serverGolem.stopConfiguration(player);
        super.removed(player);
        // Never clearContainer(filters): exemplars must not become dropped items.
    }
}
