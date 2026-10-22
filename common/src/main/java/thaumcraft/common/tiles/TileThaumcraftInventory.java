package thaumcraft.common.tiles;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.Nameable;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Original TileThaumcraftInventory: the shared base of the TC4 tiles that own an inventory.
 * <p>
 * The original keeps two lists apart. The slots named in {@code syncedSlots} are written to the client with the
 * block update and are stored under "ItemsSynced"; every other slot is only stored on disk under "Items". The
 * custom name is stored as well and shown instead of the generic container name.
 * <p>
 * Oracle: {@code thaumcraft/common/tiles/TileThaumcraftInventory.java}, lines 22-179.
 */
public class TileThaumcraftInventory extends BlockEntity implements WorldlyContainer, Nameable {
    /** TileThaumcraftInventory:27-29 - one slot, no synced slot and no custom name by default. */
    public static final int DEFAULT_SIZE = 1;
    /** TileThaumcraftInventory:150-152 - a slot holds sixty four items. */
    public static final int STACK_LIMIT = 64;
    /** TileThaumcraftInventory:154-156 - the tile may only be used from within sixty four blocks. */
    public static final double USE_RANGE = 64.0D;

    protected final NonNullList<ItemStack> itemStacks;
    protected final int[] syncedSlots;
    private Component customName;

    public TileThaumcraftInventory(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        this(type, pos, state, DEFAULT_SIZE, new int[0]);
    }

    public TileThaumcraftInventory(BlockEntityType<?> type, BlockPos pos, BlockState state, int size, int[] syncedSlots) {
        super(type, pos, state);
        this.itemStacks = NonNullList.withSize(Math.max(1, size), ItemStack.EMPTY);
        this.syncedSlots = syncedSlots == null ? new int[0] : syncedSlots.clone();
    }

    /** TileThaumcraftInventory:85-91 - only the named slots travel to the client. */
    protected boolean isSyncedSlot(int slot) {
        for (int synced : syncedSlots) if (synced == slot) return true;
        return false;
    }

    /** The synced slot numbers, for the client and for the menus. */
    public int[] syncedSlots() { return syncedSlots.clone(); }

    @Override public int getContainerSize() { return itemStacks.size(); }
    @Override public boolean isEmpty() { return itemStacks.stream().allMatch(ItemStack::isEmpty); }
    @Override public ItemStack getItem(int slot) { return slot >= 0 && slot < itemStacks.size() ? itemStacks.get(slot) : ItemStack.EMPTY; }
    @Override public ItemStack removeItem(int slot, int amount) {
        if (slot < 0 || slot >= itemStacks.size() || amount <= 0) return ItemStack.EMPTY;
        ItemStack removed = itemStacks.get(slot).split(amount);
        if (itemStacks.get(slot).isEmpty()) itemStacks.set(slot, ItemStack.EMPTY);
        if (!removed.isEmpty()) setChanged();
        return removed;
    }
    @Override public ItemStack removeItemNoUpdate(int slot) {
        if (slot < 0 || slot >= itemStacks.size()) return ItemStack.EMPTY;
        ItemStack removed = itemStacks.get(slot);
        itemStacks.set(slot, ItemStack.EMPTY);
        return removed;
    }
    @Override public void setItem(int slot, ItemStack stack) {
        if (slot < 0 || slot >= itemStacks.size()) return;
        // TileThaumcraftInventory:65-71 clamps the ordinary setter.
        if (!stack.isEmpty() && stack.getCount() > getMaxStackSize()) stack.setCount(getMaxStackSize());
        itemStacks.set(slot, stack);
        setChanged();
    }
    @Override public void clearContent() { for (int slot = 0; slot < itemStacks.size(); slot++) itemStacks.set(slot, ItemStack.EMPTY); setChanged(); }
    @Override public int getMaxStackSize() { return STACK_LIMIT; }

    @Override public boolean stillValid(Player player) {
        return level != null && !isRemoved() && player.level() == level && level.hasChunkAt(worldPosition)
                && level.getBlockEntity(worldPosition) == this
                && player.distanceToSqr(worldPosition.getX() + .5, worldPosition.getY() + .5, worldPosition.getZ() + .5) <= USE_RANGE;
    }

    /** TileThaumcraftInventory:168-170 - only the first slot is reachable from a side. */
    @Override public int[] getSlotsForFace(Direction side) { return new int[]{0}; }
    @Override public boolean canPlaceItemThroughFace(int slot, ItemStack stack, Direction side) { return canPlaceItem(slot, stack); }
    @Override public boolean canTakeItemThroughFace(int slot, ItemStack stack, Direction side) { return true; }

    @Override public Component getDisplayName() { return customName == null ? Component.translatable("container.thaumcraft") : customName; }
    @Override public Component getName() { return getDisplayName(); }
    @Override public Component getCustomName() { return customName; }
    /** TileThaumcraftInventory:81-83 - the gui name of the tile. */
    public void setCustomName(Component name) { customName = name; setChanged(); }
    /** TileThaumcraftInventory:77-79 - a tile only has a custom name when one was given. */
    public boolean hasCustomName() { return customName != null && !customName.getString().isEmpty(); }

    @Override protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        if (customName != null) tag.putString("CustomName", Component.Serializer.toJson(customName, registries));
        tag.put("Items", writeSlots(registries, false));
        tag.put("ItemsSynced", writeSlots(registries, true));
    }

    @Override protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        customName = tag.contains("CustomName") ? Component.Serializer.fromJson(tag.getString("CustomName"), registries) : null;
        // Lists are complete snapshots, not patches. In particular, the empty
        // ItemsSynced update after removing a wand must clear the previous wand.
        // Original readCustomNBT allocates a fresh inventory before reading.
        itemStacks.clear();
        readSlots(tag.getList("Items", Tag.TAG_COMPOUND), registries, false);
        readSlots(tag.getList("ItemsSynced", Tag.TAG_COMPOUND), registries, true);
    }

    /** TileThaumcraftInventory:106-116 - the synced slots travel to the client with the block update. */
    @Override public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        CompoundTag tag = new CompoundTag();
        if (customName != null) tag.putString("CustomName", Component.Serializer.toJson(customName, registries));
        tag.put("ItemsSynced", writeSlots(registries, true));
        return tag;
    }
    @Override public ClientboundBlockEntityDataPacket getUpdatePacket() { return ClientboundBlockEntityDataPacket.create(this); }

    private ListTag writeSlots(HolderLookup.Provider registries, boolean synced) {
        ListTag list = new ListTag();
        for (int slot = 0; slot < itemStacks.size(); slot++) {
            if (itemStacks.get(slot).isEmpty()) continue;
            if (isSyncedSlot(slot) != synced) continue;
            CompoundTag entry = new CompoundTag();
            entry.putByte("Slot", (byte) slot);
            entry.put("Stack", itemStacks.get(slot).save(registries));
            list.add(entry);
        }
        return list;
    }

    private void readSlots(ListTag list, HolderLookup.Provider registries, boolean synced) {
        for (int index = 0; index < list.size(); index++) {
            CompoundTag entry = list.getCompound(index);
            int slot = entry.getByte("Slot") & 0xFF;
            if (slot < 0 || slot >= itemStacks.size()) continue;
            if (isSyncedSlot(slot) != synced) continue;
            itemStacks.set(slot, ItemStack.parse(registries, entry.getCompound("Stack")).orElse(ItemStack.EMPTY));
        }
    }
}
