package thaumcraft.common.lib.baubles;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import thaumcraft.api.baubles.BaubleType;
import thaumcraft.api.baubles.IBauble;

/** Exactly four owner-attached slots. Native menu synchronization carries all ItemStack components. */
public final class PlayerAccessoryInventory extends SimpleContainer {
    private static final BaubleType[] TYPES = {BaubleType.AMULET, BaubleType.RING, BaubleType.RING, BaubleType.BELT};
    private final Player owner;
    private boolean silent, mutating;
    private Tag rejected;
    private static final String REJECTED = "RejectedAccessoryAttachment";
    public PlayerAccessoryInventory(Player owner) { super(4); this.owner = owner; }
    public Player owner() { return owner; }
    public static BaubleType slotType(int slot) { return TYPES[slot]; }
    public boolean isRejected() { return rejected != null; }
    @Override public int getMaxStackSize() { return 1; }
    @Override public boolean stillValid(Player player) { return player == owner && player.isAlive() && !player.isSpectator() && rejected == null; }
    public static boolean matchesType(int slot, ItemStack stack) {
        return slot >= 0 && slot < 4 && stack.getItem() instanceof IBauble bauble && bauble.getBaubleType(stack) == TYPES[slot];
    }
    @Override public boolean canPlaceItem(int slot, ItemStack stack) {
        return rejected == null && matchesType(slot, stack) && ((IBauble)stack.getItem()).canEquip(stack, owner);
    }
    @Override public void setItem(int slot, ItemStack stack) {
        if (mutating || rejected != null) return;
        if (!stack.isEmpty() && (stack.getCount() != 1 || !matchesType(slot, stack))) throw new IllegalArgumentException("Invalid accessory slot/count");
        ItemStack before = getItem(slot);
        mutating = true;
        try {
            super.setItem(slot, stack);
            if (!silent) {
                if (!before.isEmpty() && before.getItem() instanceof IBauble bauble) bauble.onUnequipped(before, owner);
                if (!stack.isEmpty() && stack.getItem() instanceof IBauble bauble) bauble.onEquipped(stack, owner);
            }
        } finally { mutating = false; }
    }
    @Override public ItemStack removeItem(int slot, int amount) {
        if (mutating || rejected != null || amount <= 0) return ItemStack.EMPTY;
        // The removal splits the slot's own stack, which leaves it empty (and its item air): ask the removed stack.
        ItemStack removed = super.removeItem(slot, amount);
        if (!silent && !removed.isEmpty() && removed.getItem() instanceof IBauble bauble) bauble.onUnequipped(removed, owner);
        return removed;
    }
    @Override public ItemStack removeItemNoUpdate(int slot) { return removeItem(slot, 1); }
    public CompoundTag save(HolderLookup.Provider registries) {
        if (rejected instanceof CompoundTag original) return original.copy();
        if (rejected != null) { CompoundTag envelope = new CompoundTag(); envelope.put(REJECTED, rejected.copy()); return envelope; }
        CompoundTag out = new CompoundTag(); ListTag list = new ListTag();
        for (int slot = 0; slot < 4; slot++) if (!getItem(slot).isEmpty()) {
            CompoundTag entry = new CompoundTag(); entry.putByte("Slot", (byte)slot); entry.put("Item", getItem(slot).save(registries)); list.add(entry);
        }
        out.put("Items", list); return out;
    }
    public void load(CompoundTag tag, HolderLookup.Provider registries) {
        if (mutating) throw new IllegalStateException("Accessory reload during mutation");
        if (tag.contains(REJECTED)) { rejectAttachment(tag.get(REJECTED)); return; }
        ItemStack[] parsed = {ItemStack.EMPTY,ItemStack.EMPTY,ItemStack.EMPTY,ItemStack.EMPTY};
        // getList(key, COMPOUND) returns an empty list for a list of another
        // element type. Do not mistake malformed saved contents for four empty slots.
        boolean valid = tag.get("Items") instanceof ListTag raw && (raw.isEmpty() || raw.getElementType() == Tag.TAG_COMPOUND);
        if (valid) {
            ListTag list = tag.getList("Items",Tag.TAG_COMPOUND); valid = list.size() <= 4;
            boolean[] occupied = new boolean[4];
            for (int index=0; valid && index<list.size(); index++) {
                CompoundTag entry=list.getCompound(index); int slot=entry.getByte("Slot");
                if (!entry.contains("Slot",Tag.TAG_BYTE) || slot<0 || slot>=4 || occupied[slot] || !entry.contains("Item",Tag.TAG_COMPOUND)) { valid=false; break; }
                ItemStack stack=ItemStack.parse(registries,entry.getCompound("Item")).orElse(ItemStack.EMPTY);
                if (stack.isEmpty() || stack.getCount()!=1 || !matchesType(slot,stack)) { valid=false; break; }
                occupied[slot]=true; parsed[slot]=stack;
            }
        }
        silent=true;
        try { rejected=null; for(int slot=0;slot<4;slot++) super.setItem(slot, valid ? parsed[slot] : ItemStack.EMPTY); }
        finally { silent=false; }
        rejected=valid ? null : tag.copy();
        if (!valid) com.mojang.logging.LogUtils.getLogger().error("Retaining unread accessory inventory for {} at {}: invalid Items slot/stack/list", owner==null?"test owner":owner.getUUID(), owner==null?"no position":owner.blockPosition());
        thaumcraft.common.lib.runic.RunicShield.markDirty(owner);
    }
    public Tag saveAttachment(HolderLookup.Provider registries) { return rejected==null ? save(registries) : rejected.copy(); }
    public void loadAttachment(Tag value, HolderLookup.Provider registries) {
        if (value instanceof CompoundTag compound) load(compound,registries);
        else rejectAttachment(value);
    }
    private void rejectAttachment(Tag value) {
        if (mutating) throw new IllegalStateException("Accessory reload during mutation");
        rejected=value.copy();
        com.mojang.logging.LogUtils.getLogger().error("Retaining unread accessory attachment for {} at {}: expected compound, got {}", owner==null?"test owner":owner.getUUID(), owner==null?"no position":owner.blockPosition(),value.getType());
    }
    public void receive(java.util.List<ItemStack> stacks) {
        if (!owner.level().isClientSide || stacks.size()!=4) return;
        silent=true;
        try { rejected=null; for(int slot=0;slot<4;slot++) super.setItem(slot,stacks.get(slot).copy()); }
        finally { silent=false; }
    }
}
