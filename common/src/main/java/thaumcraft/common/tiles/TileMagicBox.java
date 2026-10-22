package thaumcraft.common.tiles;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import thaumcraft.common.config.ThaumcraftEldritchBlocks;
import thaumcraft.common.lib.world.EldritchBlockRules;

/**
 * Original TileMagicBox: the linked storage box. Boxes that touch each other
 * share their content and the first box of the chain owns it.
 */
public final class TileMagicBox extends RetainedDataBlockEntity implements Container {
    private final ArrayList<ItemStack> boxContents = new ArrayList<>();
    /** The position of the box that owns the shared content. */
    private BlockPos master;
    private byte sorting = (byte) EldritchBlockRules.MAGIC_BOX_SORT_NONE;
    private short linkedBoxes = -1;

    public TileMagicBox(BlockPos pos, BlockState state) {
        super(ThaumcraftEldritchBlocks.MAGIC_BOX_TILE.get(), pos, state);
    }
    TileMagicBox(net.minecraft.world.level.block.entity.BlockEntityType<?> type, BlockPos pos, BlockState state) { super(type,pos,state); }

    /** The sorting mode of the box. */
    public int getSorting() { return sorting; }
    public void setSorting(byte sorting) { if (!isDataRejected()) this.sorting = sorting; }
    /** The number of boxes that are linked to this one. */
    public int getLinkedBoxes() { return Math.max(0, linkedBoxes); }

    /** Original TileMagicBox:60 - the slot count of the box chain. */
    @Override public int getContainerSize() {
        return EldritchBlockRules.magicBoxSize(Math.min(getLinkedBoxes(), EldritchBlockRules.MAGIC_BOX_MAX_LINKED));
    }

    /** Original TileMagicBox:62-66 - the master owns the shared content. */
    private ArrayList<ItemStack> contents() {
        if (isDataRejected()) return new ArrayList<>();
        if (master == null) return boxContents;
        if (level == null) return boxContents;
        if (!(level.getBlockEntity(master) instanceof TileMagicBox box)) return boxContents;
        return box.isDataRejected() ? new ArrayList<>() : box.boxContents;
    }

    /** Original TileMagicBox:68-73 - the inventory of the chain. */
    private TileMagicBox inventory() {
        if (master == null) return this;
        if (level == null) return this;
        return level.getBlockEntity(master) instanceof TileMagicBox box ? box : this;
    }

    @Override public boolean isEmpty() { return contents().isEmpty(); }
    @Override public int getMaxStackSize() { return EldritchBlockRules.MAGIC_BOX_MAX_STACK; }

    @Override public ItemStack getItem(int slot) {
        ArrayList<ItemStack> list = contents();
        return slot >= 0 && slot < list.size() ? list.get(slot) : ItemStack.EMPTY;
    }

    @Override public ItemStack removeItem(int slot, int amount) {
        if (inventory().isDataRejected()) return ItemStack.EMPTY;
        ArrayList<ItemStack> list = contents();
        if (slot < 0 || slot >= list.size()) return ItemStack.EMPTY;
        ItemStack stack = list.get(slot);
        if (stack.isEmpty()) return ItemStack.EMPTY;
        ItemStack removed = stack.split(amount);
        if (stack.isEmpty()) list.remove(slot);
        inventory().setChanged();
        return removed;
    }

    @Override public ItemStack removeItemNoUpdate(int slot) {
        if (inventory().isDataRejected()) return ItemStack.EMPTY;
        ArrayList<ItemStack> list = contents();
        if (slot < 0 || slot >= list.size()) return ItemStack.EMPTY;
        ItemStack removed = list.remove(slot);
        inventory().setChanged();
        return removed;
    }

    @Override public void setItem(int slot, ItemStack stack) {
        if (isDataRejected() || inventory().isDataRejected()) return;
        ArrayList<ItemStack> list = contents();
        if (stack != null && stack.getCount() > getMaxStackSize()) stack.setCount(getMaxStackSize());
        if (slot >= 0 && slot < list.size()) {
            list.set(slot, stack);
            if (stack.isEmpty()) list.remove(slot);
        } else if (stack != null && !stack.isEmpty()) {
            list.add(stack);
        }
        inventory().setChanged();
    }

    @Override public void clearContent() {
        if (isDataRejected() || inventory().isDataRejected()) return;
        contents().clear();
        inventory().setChanged();
    }

    /**
     * Original TileMagicBox:105-108 - the original sorted the content of the box every time it
     * changed, so the merge of the equal stacks runs on every insert and every removal.
     */
    @Override public void setChanged() {
        super.setChanged();
        sort();
    }

    /** Original TileMagicBox:225-233 - everybody may put items into a magic box. */
    public boolean isItemValid(int slot, ItemStack stack) { return !isDataRejected() && !inventory().isDataRejected(); }
    @Override public boolean canPlaceItem(int slot, ItemStack stack) { return isItemValid(slot,stack); }

    /** Original TileMagicBox:218-221 - the player must be close to the box. */
    @Override public boolean stillValid(Player player) {
        if (isDataRejected() || inventory().isDataRejected()) return false;
        if (level == null) return false;
        if (player.level() != level) return false;
        if (!level.hasChunkAt(worldPosition)) return false;
        if (level.getBlockEntity(worldPosition) != this) return false;
        return player.distanceToSqr(worldPosition.getX() + .5D, worldPosition.getY() + .5D, worldPosition.getZ() + .5D) <= 64.0D;
    }

    /** Original TileMagicBox:238-260 - the link of the neighbouring boxes is refreshed. */
    public static void tick(Level level, BlockPos pos, BlockState state, TileMagicBox box) {
        if (box.isDataRejected() || box.inventory().isDataRejected()) return;
        // The original ticked the tile on both sides, so the client knows the size of the chain too.
        if (box.inventory() == box && box.linkedBoxes < 0) box.refreshLinks();
    }

    /** Original TileMagicBox:398-411 - the chain of boxes is found around the master. */
    public void refreshLinks() {
        if (isDataRejected()) return;
        if (inventory() != this) return;
        linkedBoxes = 0;
        List<BlockPos> found = new ArrayList<>();
        findBoxes(worldPosition, found);
        linkedBoxes = (short) Math.min(found.size(), EldritchBlockRules.MAGIC_BOX_MAX_LINKED);
    }

    /** Original TileMagicBox:413-424 - the search stops after 1024 boxes. */
    private void findBoxes(BlockPos pos, List<BlockPos> found) {
        if (found.size() >= EldritchBlockRules.MAGIC_BOX_MAX_LINKED) return;
        for (Direction dir : Direction.values()) {
            BlockPos next = pos.relative(dir);
            if (!(level.getBlockEntity(next) instanceof TileMagicBox box)) continue;
            if (box.isDataRejected()) continue;
            if (found.contains(next)) continue;
            found.add(next);
            findBoxes(next, found);
        }
    }

    /** Original TileMagicBox:184-223 - the content of the box is sorted and merged. */
    public void sort() {
        if (level == null || sorting < 0) return;
        ArrayList<ItemStack> list = contents();
        boolean done = false;
        while (!done) {
            done = true;
            for (int i = 0; i < list.size() - 1; i++) {
                done = swopSlots(list, i, i + 1);
                ItemStack first = list.get(i);
                ItemStack second = list.get(i + 1);
                if (!EldritchBlockRules.magicBoxSlotHasRoom(first)) continue;
                if (!EldritchBlockRules.magicBoxMerges(first, second)) continue;
                ItemStack one = first.copy();
                ItemStack two = second.copy();
                int moved = Math.min(one.getMaxStackSize() - one.getCount(), two.getCount());
                one.setCount(one.getCount() + moved);
                two.setCount(two.getCount() - moved);
                list.set(i, one);
                done = false;
                if (two.getCount() > 0) {
                    list.set(i + 1, two);
                    continue;
                }
                list.remove(i + 1);
                break;
            }
        }
    }

    /** Original TileMagicBox:262-306 - the comparison of two slots. */
    private boolean swopSlots(ArrayList<ItemStack> list, int i, int j) {
        ItemStack first = list.get(i);
        ItemStack second = list.get(j);
        if (first.isEmpty() || second.isEmpty()) return true;
        String firstName = first.getHoverName().getString();
        String secondName = second.getHoverName().getString();
        if (EldritchBlockRules.magicBoxSortsByMod(sorting)) {
            firstName = modOf(first) + firstName;
            secondName = modOf(second) + secondName;
        }
        int compare = firstName.compareToIgnoreCase(secondName);
        if (EldritchBlockRules.magicBoxSortsByName(sorting)) {
            if (sorting == EldritchBlockRules.MAGIC_BOX_SORT_NAME_ASCENDING && compare > 0) {
                list.set(i, second);
                list.set(j, first);
                return false;
            }
            if (sorting == EldritchBlockRules.MAGIC_BOX_SORT_NAME_DESCENDING && compare < 0) {
                list.set(i, second);
                list.set(j, first);
                return false;
            }
        } else if (EldritchBlockRules.magicBoxSortsByMod(sorting) && compare > 0) {
            list.set(i, second);
            list.set(j, first);
            return false;
        }
        return true;
    }

    /** The mod identifier of the item, used by the mod sorting. */
    private static String modOf(ItemStack stack) {
        return net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(stack.getItem()).getNamespace();
    }

    /** Original TileMagicBox:64-70 - the whole content is dropped when the box breaks. */
    public void dropContents(Level level, BlockPos pos) {
        if (isDataRejected() || inventory().isDataRejected()) return;
        ArrayList<ItemStack> list = contents();
        if (list.isEmpty()) return;
        for (ItemStack stack : list) {
            net.minecraft.world.entity.item.ItemEntity item = new net.minecraft.world.entity.item.ItemEntity(level,
                    pos.getX() + .5D, pos.getY() + .5D, pos.getZ() + .5D, stack.copy());
            item.setDefaultPickUpDelay();
            level.addFreshEntity(item);
        }
        list.clear();
        setChanged();
    }

    /** The content of the box, used by the menu. */
    public List<ItemStack> getContents() { return contents(); }

    @Override protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        if (retainMalformed(tag, () -> { for (Tag value:compoundList(tag,"Items")) requiredStack(registries,value); })) return;
        super.loadAdditional(tag, registries);
        sorting = tag.getByte("sort");
        master = null;
        if (tag.contains("w_x")) {
            master = new BlockPos(tag.getInt("w_x"), tag.getInt("w_y"), tag.getInt("w_z"));
        }
        boxContents.clear();
        ListTag items = tag.getList("Items", Tag.TAG_COMPOUND);
        for (int i = 0; i < items.size(); i++) {
            ItemStack stack = requiredStack(registries, items.getCompound(i));
            if (!stack.isEmpty()) boxContents.add(stack);
        }
        // Original TileMagicBox:282-289 - the number of linked boxes is a transient
        // counter, it is neither written nor read by the original. The native tree
        // persisted it, so a box chain that changed while the chunk was unloaded
        // kept its old size forever. The counter starts at minus one again and the
        // first tick re-scans the neighbours, exactly as the original does.
        linkedBoxes = (short) -1;
        // Original TileMagicBox:114-124 - the original sorted the content after it was loaded.
        sort();
    }

    @Override protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        if (saveRejected(tag)) return;
        super.saveAdditional(tag, registries);
        tag.putByte("sort", sorting);
        if (master != null) {
            tag.putInt("w_x", master.getX());
            tag.putInt("w_y", master.getY());
            tag.putInt("w_z", master.getZ());
        }
        ListTag items = new ListTag();
        for (ItemStack stack : boxContents) {
            if (stack == null || stack.isEmpty()) continue;
            items.add(stack.save(registries));
        }
        tag.put("Items", items);
    }

    @Override public CompoundTag getUpdateTag(HolderLookup.Provider registries) { return saveWithoutMetadata(registries); }
    @Override public ClientboundBlockEntityDataPacket getUpdatePacket() { return ClientboundBlockEntityDataPacket.create(this); }
}
