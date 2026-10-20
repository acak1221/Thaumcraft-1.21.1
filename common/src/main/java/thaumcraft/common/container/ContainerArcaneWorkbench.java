package thaumcraft.common.container;

import java.util.ArrayList;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.ResultContainer;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import thaumcraft.common.config.ThaumcraftArcane;
import thaumcraft.common.crafting.ArcaneCrafting;
import thaumcraft.common.crafting.ArcaneRecipeInput;
import thaumcraft.common.crafting.ArcaneVis;
import thaumcraft.common.items.wands.WandData;
import thaumcraft.common.tiles.TileArcaneWorkbench;

/** Persistent shared inputs, per-viewer preview, and a server-atomic result transaction. */
public final class ContainerArcaneWorkbench extends AbstractContainerMenu {
    private final Container contents;
    private final TileArcaneWorkbench tile;
    private final Player viewer;
    private final BlockPos tablePos;
    private final ResultContainer result = new ResultContainer();
    // Affordable flag, then low/high unsigned halves of each server-priced
    // primal cost. Vanilla menu data packets carry signed shorts, not ints.
    private final SimpleContainerData prices = new SimpleContainerData(13);

    public ContainerArcaneWorkbench(int id, Inventory inventory, TileArcaneWorkbench tile) {
        this(id, inventory, tile, tile.getBlockPos(), tile);
    }

    public ContainerArcaneWorkbench(int id, Inventory inventory, FriendlyByteBuf data) {
        this(id, inventory, new SimpleContainer(11), data.readBlockPos(), null);
    }

    private ContainerArcaneWorkbench(int id, Inventory inventory, Container contents, BlockPos pos, TileArcaneWorkbench tile) {
        super(ThaumcraftArcane.ARCANE_WORKBENCH_MENU.get(), id);
        this.contents = contents;
        this.tile = tile;
        viewer = inventory.player;
        tablePos = pos.immutable();
        checkContainerSize(contents, 11);
        addSlot(new Slot(result, 0, 160, 64) {
            @Override public boolean mayPlace(ItemStack stack) { return false; }
            @Override public boolean mayPickup(Player player) { return tile == null ? craftable() : canTakeResult(player); }
            @Override public ItemStack remove(int amount) { return amount <= 0 ? ItemStack.EMPTY : takeResult(); }
            @Override public void onTake(Player player, ItemStack stack) {} // Payment precedes extraction, never follows it.
            @Override public boolean isFake() { return true; }
        });
        addSlot(new SlotLimitedByWand(contents, TileArcaneWorkbench.WAND_SLOT, 160, 24, 1));
        for (int row = 0; row < 3; row++) {
            for (int column = 0; column < 3; column++) addSlot(new Slot(contents, column + row * 3, 40 + column * 24, 40 + row * 24));
        }
        for (int row = 0; row < 3; row++) {
            for (int column = 0; column < 9; column++) addSlot(new Slot(inventory, column + row * 9 + 9, 16 + column * 18, 151 + row * 18));
        }
        for (int column = 0; column < 9; column++) addSlot(new Slot(inventory, column, 16 + column * 18, 209));
        addDataSlots(prices);
        refreshResult();
    }

    public BlockPos tablePos() { return tablePos; }
    public TileArcaneWorkbench tile() { return tile; }
    public boolean craftable() { return prices.get(0) == 1; }
    public int requiredVis(String primal) {
        int index = WandData.PRIMALS.indexOf(primal);
        return index < 0 ? 0 : (prices.get(1 + index * 2) & 0xffff) | ((prices.get(2 + index * 2) & 0xffff) << 16);
    }

    @Override public boolean stillValid(Player player) {
        return player == viewer && player.isAlive() && !player.isSpectator() && (tile == null || tile.stillValid(player));
    }

    private boolean active(Player player) {
        return tile != null && player == viewer && viewer instanceof ServerPlayer && viewer.containerMenu == this && stillValid(player);
    }

    private boolean allowed(ArcaneCrafting.Match match) {
        return match != null && (match.arcane() || result.setRecipeUsed(viewer.level(), (ServerPlayer) viewer, match.recipe()));
    }

    private boolean funded(ArcaneCrafting.Match match) {
        return !match.arcane() || ArcaneVis.canPay(contents.getItem(TileArcaneWorkbench.WAND_SLOT), match.prices());
    }

    private void refreshResult() {
        if (tile == null || !(viewer instanceof ServerPlayer)) return;
        ArcaneCrafting.Match match = tile.stillValid(viewer) ? ArcaneCrafting.find(viewer, contents) : null;
        if (!allowed(match)) match = null;
        // A non-takeable preview replaces the old GUI-only ghost item. It is
        // never physical inventory and every extraction path validates afresh.
        result.setItem(0, match == null ? ItemStack.EMPTY : match.output().copy());
        prices.set(0, match != null && funded(match) ? 1 : 0);
        for (int i = 0; i < WandData.PRIMALS.size(); i++) {
            int cost = match == null ? 0 : match.prices().getOrDefault(WandData.PRIMALS.get(i), 0);
            prices.set(1 + i * 2, cost & 0xffff);
            prices.set(2 + i * 2, cost >>> 16);
        }
        if (match == null) result.setRecipeUsed(null);
    }

    private boolean canTakeResult(Player player) {
        if (!active(player)) return false;
        ArcaneCrafting.Match match = ArcaneCrafting.find(viewer, contents);
        return allowed(match) && funded(match) && ItemStack.matches(match.output(), result.getItem(0));
    }

    /** All callers receive output only after current research, matrix, wand and BE identity pass. */
    private ItemStack takeResult() {
        if (!active(viewer)) return ItemStack.EMPTY;
        ArcaneCrafting.Match match = ArcaneCrafting.find(viewer, contents);
        if (!allowed(match) || !funded(match) || !ItemStack.matches(match.output(), result.getItem(0))
                || !ItemStack.listMatches(match.input().items(), ArcaneRecipeInput.snapshot(contents).items())) {
            refreshResult();
            return ItemStack.EMPTY;
        }
        ItemStack wand = contents.getItem(TileArcaneWorkbench.WAND_SLOT).copy();
        if (match.arcane() && !ArcaneVis.debit(wand, match.prices())) return ItemStack.EMPTY;
        var matrix = new ArrayList<ItemStack>(9);
        for (int slot=0;slot<match.input().items().size();slot++) {
            boolean retain=thaumcraft.common.lib.AdvancedFurnaceCrafting.retainCenterPearl(match,slot);
            ItemStack next = retain?contents.getItem(slot):match.input().items().get(slot).copy();
            if (!next.isEmpty() && !retain) next.shrink(1);
            matrix.add(next);
        }
        tile.applyCraft(matrix, wand);
        for (int slot = 0; slot < 9; slot++) {
            ItemStack remainder = match.remainders().get(slot).copy();
            if (remainder.isEmpty()) continue;
            // TC4 SlotCraftingArcaneWorkbench:49-63 prefers player inventory
            // for container items; if full, use the now-empty matrix cell/drop.
            // Native recipe remainders retain modern component-sensitive data.
            if (viewer.getInventory().add(remainder)) continue;
            if (contents.getItem(slot).isEmpty()) contents.setItem(slot, remainder);
            else viewer.drop(remainder, false);
        }
        ItemStack output = match.output().copy();
        output.onCraftedBy(viewer.level(), viewer, output.getCount());
        dev.architectury.event.events.common.PlayerEvent.CRAFT_ITEM.invoker().craft(viewer, output, contents);
        result.setRecipeUsed(match.recipe());
        result.awardUsedRecipes(viewer, match.input().items());
        viewer.getInventory().setChanged();
        refreshResult();
        return output;
    }

    @Override public void slotsChanged(Container container) { refreshResult(); super.slotsChanged(container); }
    @Override public void broadcastChanges() { refreshResult(); super.broadcastChanges(); }
    @Override public void sendAllDataToRemote() { refreshResult(); super.sendAllDataToRemote(); }

    @Override public void clicked(int index, int button, ClickType type, Player player) {
        if (!stillValid(player) || tile != null && !active(player)) return;
        // Original ContainerArcaneWorkbench:128-136: throw whole stacks, and
        // right-clicking result/wand behaves like left click (no half output).
        if (type == ClickType.THROW) button = 1;
        else if ((index == 0 || index == 1) && button > 0) button = 0;
        if (tile != null) refreshResult();
        // The original insufficient-vis ghost was not an actual slot and could
        // not be creative-cloned. A funded result keeps vanilla clone behavior.
        if (index == 0 && type == ClickType.CLONE && (tile == null ? !craftable() : !canTakeResult(player))) return;
        if (index == 0 && (type == ClickType.PICKUP || type == ClickType.SWAP || type == ClickType.THROW)) {
            // No optimistic client crafting. Native slot/data sync supplies all
            // outcomes, and vanilla containerId/state routing supplies the session.
            if (tile == null) return;
            ItemStack preview = result.getItem(0);
            if (preview.isEmpty() || !canTakeResult(player)) return;
            if (type == ClickType.PICKUP) {
                ItemStack carried = getCarried();
                if (!carried.isEmpty() && (!ItemStack.isSameItemSameComponents(carried, preview)
                        || carried.getCount() + preview.getCount() > carried.getMaxStackSize())) return;
                ItemStack crafted = takeResult();
                if (!crafted.isEmpty()) {
                    if (carried.isEmpty()) setCarried(crafted);
                    else if (ItemStack.isSameItemSameComponents(carried, crafted)
                            && carried.getCount() + crafted.getCount() <= carried.getMaxStackSize()) carried.grow(crafted.getCount());
                    else returnToInventory(crafted); // A native onCraftedBy hook may change map/components.
                }
            } else if (type == ClickType.THROW) {
                if (getCarried().isEmpty()) {
                    ItemStack crafted = takeResult();
                    if (!crafted.isEmpty()) player.drop(crafted, true);
                }
            } else if (button >= 0 && button < 9 && player.getInventory().getItem(button).isEmpty()) {
                ItemStack crafted = takeResult();
                if (!crafted.isEmpty()) {
                    // Container remainders may have occupied this empty hotbar
                    // cell during the transaction. Never overwrite that item.
                    if (player.getInventory().getItem(button).isEmpty()) player.getInventory().setItem(button, crafted);
                    else returnToInventory(crafted);
                }
            }
        } else super.clicked(index, button, type, player);
        if (tile != null) broadcastChanges();
    }

    private void returnToInventory(ItemStack stack) {
        if (!viewer.getInventory().add(stack) && !stack.isEmpty()) viewer.drop(stack, false);
    }

    @Override public ItemStack quickMoveStack(Player player, int index) {
        if (!active(player) || index < 0 || index >= slots.size()) return ItemStack.EMPTY;
        refreshResult();
        Slot slot = getSlot(index);
        if (!slot.hasItem() || !slot.mayPickup(player)) return ItemStack.EMPTY;
        ItemStack current = slot.getItem();
        ItemStack original = current.copy();
        if (index == 0) {
            if (!hasOutputRoom(original)) return ItemStack.EMPTY;
            ItemStack output = takeResult();
            if (output.isEmpty()) return ItemStack.EMPTY;
            moveItemStackTo(output, 11, 47, true);
            // Original/vanilla shift-result semantics: a partial transfer still
            // consumes one recipe and drops the uninserted part, never duplicates it.
            if (!output.isEmpty()) player.drop(output, false);
            refreshResult();
            return original;
        }
        boolean moved;
        if (index >= 11 && index < 38) moved = ArcaneVis.isWorkbenchWand(current)
                ? moveItemStackTo(current, 1, 2, false) : moveItemStackTo(current, 38, 47, false);
        else if (index >= 38 && index < 47) moved = ArcaneVis.isWorkbenchWand(current)
                ? moveItemStackTo(current, 1, 2, false) : moveItemStackTo(current, 11, 38, false);
        else moved = moveItemStackTo(current, 11, 47, false);
        if (!moved || current.getCount() == original.getCount()) return ItemStack.EMPTY;
        if (current.isEmpty()) slot.set(ItemStack.EMPTY);
        else slot.setChanged();
        slot.onTake(player, current);
        refreshResult();
        return original;
    }

    private boolean hasOutputRoom(ItemStack output) {
        for (int slot = 11; slot < 47; slot++) {
            ItemStack target = getSlot(slot).getItem();
            if (target.isEmpty() || ItemStack.isSameItemSameComponents(output, target)
                    && target.getCount() < getSlot(slot).getMaxStackSize(output)) return true;
        }
        return false;
    }

    @Override public boolean canTakeItemForPickAll(ItemStack stack, Slot slot) {
        return slot.container != result && slot.container != contents && super.canTakeItemForPickAll(stack, slot);
    }
    @Override public boolean canDragTo(Slot slot) { return slot.container != result && super.canDragTo(slot); }
    @Override public void removed(Player player) {
        // Closing a menu returns only the native cursor, never the persistent
        // shared inputs/wand and never a cached output preview.
        super.removed(player);
        result.clearContent();
    }
}
