package thaumcraft.common.tiles;

import java.util.List;
import java.util.Random;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Nameable;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import thaumcraft.common.Thaumcraft;
import thaumcraft.common.config.ThaumcraftContent;
import thaumcraft.common.config.ThaumcraftInfusion;
import thaumcraft.common.entities.EntityInventoryItem;
import thaumcraft.common.lib.utils.DeferredItemDrops;
import thaumcraft.common.lib.utils.RejectedEntitySpawn;

/** TilePedestal: one public, all-sided slot; an infusion result is NOT limited to one item. */
public final class TilePedestal extends RetainedDataBlockEntity implements WorldlyContainer, Nameable {
    private ItemStack stack = ItemStack.EMPTY;
    private Component customName;

    public TilePedestal(BlockPos pos, BlockState state) {
        super(ThaumcraftInfusion.PEDESTAL_BLOCK_ENTITY.get(), pos, state);
    }
    TilePedestal(net.minecraft.world.level.block.entity.BlockEntityType<?> type, BlockPos pos, BlockState state) { super(type,pos,state); }

    @Override public int getContainerSize() { return 1; }
    @Override public int getMaxStackSize() { return 1; }
    @Override public boolean isEmpty() { return stack.isEmpty(); }
    @Override public ItemStack getItem(int slot) { return !isDataRejected() && slot == 0 ? stack : ItemStack.EMPTY; }
    /** The actual physical slot, not a copied preview. */
    public ItemStack getStack() { return getItem(0); }

    @Override public ItemStack removeItem(int slot, int amount) {
        if (isDataRejected()) return ItemStack.EMPTY;
        if (slot != 0 || amount <= 0 || stack.isEmpty()) return ItemStack.EMPTY;
        ItemStack removed = stack.split(amount);
        if (stack.isEmpty()) stack = ItemStack.EMPTY;
        setChanged();
        return removed;
    }

    @Override public ItemStack removeItemNoUpdate(int slot) {
        if (isDataRejected()) return ItemStack.EMPTY;
        if (slot != 0) return ItemStack.EMPTY;
        ItemStack removed = stack;
        stack = ItemStack.EMPTY;
        return removed;
    }

    @Override public void setItem(int slot, ItemStack value) {
        if (isDataRejected()) return;
        if (slot != 0) return;
        // TilePedestal:83-91 clamps the ordinary setter, including automation.
        if (!value.isEmpty() && value.getCount() > 1) value.setCount(1);
        stack = value;
        setChanged();
    }

    /** TilePedestal:94-100: full output count and all native components survive. */
    public void setInfusionStack(ItemStack value) {
        if (isDataRejected()) return;
        stack = value;
        setChanged();
    }

    @Override public void clearContent() { if (isDataRejected()) return; stack = ItemStack.EMPTY; setChanged(); }
    @Override public boolean canPlaceItem(int slot, ItemStack value) { return !isDataRejected() && slot == 0; }
    @Override public int[] getSlotsForFace(Direction side) { return new int[] {0}; }
    @Override public boolean canPlaceItemThroughFace(int slot, ItemStack value, Direction side) { return !isDataRejected() && slot == 0 && stack.isEmpty(); }
    @Override public boolean canTakeItemThroughFace(int slot, ItemStack value, Direction side) { return !isDataRejected() && slot == 0; }

    @Override public boolean stillValid(Player player) {
        return !isDataRejected() && level != null && !isRemoved() && player.level() == level && level.hasChunkAt(worldPosition)
                && level.getBlockEntity(worldPosition) == this
                && level.getBlockState(worldPosition).is(ThaumcraftInfusion.PEDESTAL.get())
                && player.distanceToSqr(worldPosition.getX() + .5, worldPosition.getY() + .5, worldPosition.getZ() + .5) <= 64;
    }

    /** Original InventoryUtils:354-377; never spawn from inside block replacement. */
    public void evacuate() {
        if (isDataRejected()) return;
        if (!(level instanceof ServerLevel server) || stack.isEmpty()) return;
        DeferredItemDrops.enqueue(server, worldPosition, List.of(stack));
        stack = ItemStack.EMPTY;
        setChanged();
    }

    /** InventoryUtils:381-395 places the whole stack AT the player, not in a locked inventory. */
    public void ejectTo(Player player) {
        if (isDataRejected()) return;
        if (!(level instanceof ServerLevel server) || player.level() != server || stack.isEmpty()) return;
        dropAt(server, new Vec3(player.getX(), player.getY() + player.getEyeHeight() / 2.0, player.getZ()), null, player.blockPosition());
    }

    /** Hazards run outside block replacement: the real drop must exist BEFORE a type5 explosion. */
    public void dropContents() {
        if (isDataRejected()) return;
        if (!(level instanceof ServerLevel server) || stack.isEmpty()) return;
        Random random = new Random(); // InventoryUtils:355, independent of world RNG.
        float x = random.nextFloat() * .8F + .1F, y = random.nextFloat() * .8F + .1F, z = random.nextFloat() * .8F + .1F;
        Vec3 position = new Vec3((float) worldPosition.getX() + x, (float) worldPosition.getY() + y, (float) worldPosition.getZ() + z);
        float factor = .05F;
        Vec3 motion = new Vec3(random.nextGaussian() * factor, random.nextGaussian() * factor + .2F, random.nextGaussian() * factor);
        dropAt(server, position, motion, worldPosition);
    }

    private void dropAt(ServerLevel server, Vec3 position, Vec3 motion, BlockPos recoveryPos) {
        if (!server.getServer().isSameThread()) throw new IllegalStateException("Pedestal ejection requires the server thread");
        ItemStack removed = stack;
        stack = ItemStack.EMPTY; // A spawn/update callback must never see the paid item twice.
        ItemEntity output = null;
        RejectedEntitySpawn.Snapshot snapshot = null;
        boolean added = false;
        boolean delivered = false;
        try {
            if (removed.getCount() > 99) {
                output = new EntityInventoryItem(ThaumcraftContent.INVENTORY_ITEM.get(), server);
                output.setPos(position);
                output.setItem(removed.copy());
            } else {
                output = new ItemEntity(server, position.x, position.y, position.z, removed.copy());
            }
            if (motion != null) output.setDeltaMovement(motion);
            snapshot = RejectedEntitySpawn.snapshotBefore(server, output);
            added = server.addFreshEntity(output);
            delivered = server.getEntity(output.getUUID()) == output && !output.isRemoved()
                    && ItemStack.matches(output.getItem(), removed);
        } catch (RuntimeException exception) {
            Thaumcraft.LOG.warn("Pedestal ejection failed at {}; retaining the item", worldPosition, exception);
        } finally {
            try {
                if (!delivered) {
                    try {
                        if (output != null) {
                            output.discard();
                            if (snapshot != null) RejectedEntitySpawn.releaseRejected(server, output, snapshot, added);
                        }
                    } finally {
                        // Existing durable recovery, not a second pedestal inventory/hidden queue.
                        DeferredItemDrops.enqueue(server, recoveryPos, List.of(removed));
                    }
                }
            } finally {
                // Ownership already moved to a real entity or durable recovery
                // before this callback-bearing notification can run or throw.
                setChanged();
            }
        }
    }

    @Override public void setChanged() {
        super.setChanged();
        if (level != null && !level.isClientSide && !isRemoved()) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), Block.UPDATE_CLIENTS);
        }
    }

    @Override protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        if (saveRejected(tag)) return;
        super.saveAdditional(tag, registries);
        if (!stack.isEmpty()) {
            // The 1.21 stack codec rejects count >99. Keep one physical stack,
            // exactly as the existing inventory-drop adapter does, without splitting.
            tag.put("Stack", stack.copyWithCount(1).save(registries));
        }
        // ClientPacketListener deliberately ignores an EMPTY BE update tag.
        // Explicit zero clears the last displayed component/core after pickup.
        tag.putInt("Count", stack.getCount());
        if (customName != null) tag.putString("CustomName", Component.Serializer.toJson(customName, registries));
    }

    @Override protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        if (retainMalformed(tag, () -> {
            if (tag.contains("Stack")) {
                requiredStack(registries,tag.get("Stack"));
                if (!tag.contains("Count",Tag.TAG_INT) || tag.getInt("Count")<=0) throw new IllegalArgumentException("Count must be positive int");
            } else if (tag.getInt("Count")!=0) throw new IllegalArgumentException("Count without Stack");
        })) return;
        super.loadAdditional(tag, registries);
        stack = tag.contains("Stack") ? requiredStack(registries, tag.get("Stack")) : ItemStack.EMPTY;
        int count = tag.getInt("Count");
        if (!stack.isEmpty() && count > 0) stack.setCount(count);
        else stack = ItemStack.EMPTY;
        customName = tag.contains("CustomName") ? Component.Serializer.fromJson(tag.getString("CustomName"), registries) : null;
    }

    @Override public CompoundTag getUpdateTag(HolderLookup.Provider registries) { return saveWithoutMetadata(registries); }
    @Override public ClientboundBlockEntityDataPacket getUpdatePacket() { return ClientboundBlockEntityDataPacket.create(this); }
    @Override public Component getName() { return customName == null ? Component.translatable("container.pedestal") : customName; }
    @Override public Component getCustomName() { return customName; }
    public void setCustomName(Component name) { customName = name; setChanged(); }
}
