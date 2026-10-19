package thaumcraft.common.tiles;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.Container;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import thaumcraft.common.config.ThaumcraftHungryChest;
import thaumcraft.common.lib.utils.DeferredItemDrops;

/** TC4's public, unsided 27-slot inventory and contact-fed animated lid. */
public final class TileChestHungry extends BlockEntity implements Container, MenuProvider {
    public static final int SIZE = 27;
    // TC4 initially allocated 36 entries but exposed only 27, and loaded only
    // 27. Fresh modern worlds keep exactly those 27 usable/persisted slots.
    private final NonNullList<ItemStack> items = NonNullList.withSize(SIZE, ItemStack.EMPTY);
    private float lidAngle;
    private float prevLidAngle;
    private int numUsingPlayers;
    private boolean eating;
    private boolean evacuating;

    public TileChestHungry(BlockPos pos, BlockState state) {
        super(ThaumcraftHungryChest.HUNGRY_CHEST_BLOCK_ENTITY.get(), pos, state);
    }

    private boolean isCurrent() {
        return level != null && !isRemoved() && level.hasChunkAt(worldPosition)
                && level.getBlockEntity(worldPosition) == this
                && level.getBlockState(worldPosition).is(ThaumcraftHungryChest.HUNGRY_CHEST.get());
    }
    private static boolean validSlot(int slot) { return slot >= 0 && slot < SIZE; }
    @Override public int getContainerSize() { return SIZE; }
    @Override public int getMaxStackSize() { return 64; }
    @Override public int getMaxStackSize(ItemStack stack) { return Math.min(64, stack.getMaxStackSize()); }
    @Override public boolean isEmpty() { return items.stream().allMatch(ItemStack::isEmpty); }
    @Override public ItemStack getItem(int slot) { return validSlot(slot) ? items.get(slot) : ItemStack.EMPTY; }
    @Override public ItemStack removeItem(int slot, int amount) {
        if (!validSlot(slot) || amount <= 0) return ItemStack.EMPTY;
        ItemStack removed = ContainerHelper.removeItem(items, slot, amount);
        if (!removed.isEmpty()) setChanged();
        return removed;
    }
    @Override public ItemStack removeItemNoUpdate(int slot) {
        return validSlot(slot) ? ContainerHelper.takeItem(items, slot) : ItemStack.EMPTY;
    }
    @Override public void setItem(int slot, ItemStack stack) {
        if (!validSlot(slot) || stack == null) return;
        // Native menus/hoppers already respect this limit. Refuse an invalid
        // overstack instead of truncating and silently deleting its remainder.
        if (!stack.isEmpty() && stack.getCount() > getMaxStackSize(stack)) return;
        items.set(slot, stack.isEmpty() ? ItemStack.EMPTY : stack);
        setChanged();
    }
    @Override public boolean canPlaceItem(int slot, ItemStack stack) { return validSlot(slot); }
    @Override public void clearContent() { items.clear(); setChanged(); }

    @Override public boolean stillValid(Player player) {
        return player != null && isCurrent() && player.level() == level
                && player.distanceToSqr(worldPosition.getX() + .5, worldPosition.getY() + .5, worldPosition.getZ() + .5) <= 64;
    }
    public boolean canOpen(Player player) {
        return stillValid(player) && player.isAlive() && player.canInteractWithBlock(worldPosition, 0)
                && level.mayInteract(player, worldPosition);
    }
    @Override public Component getDisplayName() { return Component.translatable("block.thaumcraft.hungry_chest"); }
    @Override public AbstractContainerMenu createMenu(int id, Inventory inventory, Player player) {
        return canOpen(player) && inventory == player.getInventory() ? ChestMenu.threeRows(id, inventory, this) : null;
    }

    @Override public void startOpen(Player player) {
        if (!(level instanceof ServerLevel server) || !stillValid(player) || player.isSpectator()) return;
        if (!server.getServer().isSameThread()) throw new IllegalStateException("Hungry Chest opening requires the server thread");
        ++numUsingPlayers;
        server.blockEvent(worldPosition, getBlockState().getBlock(), 1, numUsingPlayers);
    }
    @Override public void stopOpen(Player player) {
        if (!(level instanceof ServerLevel server) || player == null || player.level() != level || player.isSpectator()) return;
        if (!server.getServer().isSameThread()) throw new IllegalStateException("Hungry Chest closing requires the server thread");
        numUsingPlayers = Math.max(0, numUsingPlayers - 1);
        if (isCurrent()) server.blockEvent(worldPosition, getBlockState().getBlock(), 1, numUsingPlayers);
    }
    public int getNumUsingPlayers() { return numUsingPlayers; }

    /** Raw interpolation; the client chest renderer applies 1 - (1 - open)^3. */
    public float getOpenNess(float partialTick) { return Mth.lerp(partialTick, prevLidAngle, lidAngle); }

    public static void tick(Level level, BlockPos pos, BlockState state, TileChestHungry chest) {
        if (chest.level != level || !chest.worldPosition.equals(pos) || state != chest.getBlockState() || !chest.isCurrent()) return;
        if (level instanceof ServerLevel server && !server.getServer().isSameThread()) {
            throw new IllegalStateException("Hungry Chest ticking requires the server thread");
        }
        chest.prevLidAngle = chest.lidAngle;
        boolean openingSound = chest.numUsingPlayers > 0 && chest.lidAngle == 0;
        boolean closingSound = false;
        if (chest.numUsingPlayers == 0 && chest.lidAngle > 0 || chest.numUsingPlayers > 0 && chest.lidAngle < 1) {
            float previous = chest.lidAngle;
            chest.lidAngle = Mth.clamp(chest.lidAngle + (chest.numUsingPlayers > 0 ? .1F : -.1F), 0, 1);
            closingSound = previous >= .5F && chest.lidAngle < .5F;
        }
        // Both logical sides animate, but only the server broadcasts audio.
        if (!level.isClientSide && (openingSound || closingSound)) {
            level.playSound(null, pos.getX() + .5, pos.getY() + .5, pos.getZ() + .5,
                    openingSound ? SoundEvents.CHEST_OPEN : SoundEvents.CHEST_CLOSE,
                    SoundSource.BLOCKS, .5F, level.random.nextFloat() * .1F + .9F);
        }
    }

    @Override public boolean triggerEvent(int id, int parameter) {
        if (id == 1) {
            numUsingPlayers = Math.max(0, parameter);
            return true;
        }
        if (id == 2) {
            lidAngle = Math.max(lidAngle, Mth.clamp(parameter / 10.0F, 0, 1));
            return true;
        }
        return super.triggerEvent(id, parameter);
    }

    private boolean canEat(ServerLevel server, ItemEntity entity) {
        return entity != null && isCurrent() && !evacuating && entity.level() == server && entity.isAlive()
                && server.getEntity(entity.getUUID()) == entity
                && touchesChest(entity.getBoundingBox());
    }
    private boolean touchesChest(AABB bounds) {
        // entityInside covers the whole cell. Require the original physical
        // chest bounds, including an item resting exactly on its 15/16 top.
        double x = worldPosition.getX(), y = worldPosition.getY(), z = worldPosition.getZ();
        double epsilon = 1.0E-7;
        return bounds.maxX >= x + 1.0 / 16 - epsilon && bounds.minX <= x + 15.0 / 16 + epsilon
                && bounds.maxY >= y - epsilon && bounds.minY <= y + 15.0 / 16 + epsilon
                && bounds.maxZ >= z + 1.0 / 16 - epsilon && bounds.minZ <= z + 15.0 / 16 + epsilon;
    }

    /** Called only from the block's native entityInside contact, never its ticker. */
    public void eatItem(ItemEntity entity) {
        if (!(level instanceof ServerLevel server) || eating || !canEat(server, entity)) return;
        if (!server.getServer().isSameThread()) throw new IllegalStateException("Hungry Chest pickup requires the server thread");
        eating = true;
        try {
            // Reuse the existing admission fence. Settled drops have ordinary
            // native ownership; an unadmitted queue candidate is not inventory.
            if (!DeferredItemDrops.allowPlayerTouch(entity) || !canEat(server, entity)) return;
            ItemStack source = entity.getItem();
            if (source.isEmpty()) return;
            int originalCount = source.getCount();
            int remaining = originalCount;
            int limit = getMaxStackSize(source);
            if (limit <= 0) return;
            ItemStack[] previous = items.toArray(new ItemStack[SIZE]);
            int[] counts = new int[SIZE];
            int[] amounts = new int[SIZE];
            for (int slot = 0; slot < SIZE; slot++) {
                // An aliased entity/slot object is not two independently owned
                // stacks and cannot be treated as a new physical contribution.
                if (previous[slot] == source) return;
                counts[slot] = previous[slot].getCount();
            }
            // InventoryUtils:80-102: merge matching occupied slots first, then
            // fill empty slots. Native components replace the old strict NBT test.
            for (int slot = 0; slot < SIZE && remaining > 0; slot++) {
                ItemStack stored = previous[slot];
                if (stored.isEmpty() || !ItemStack.isSameItemSameComponents(stored, source)) continue;
                int capacity = Math.max(0, Math.min(limit, stored.getMaxStackSize()) - counts[slot]);
                amounts[slot] = Math.min(remaining, capacity);
                remaining -= amounts[slot];
            }
            for (int slot = 0; slot < SIZE && remaining > 0; slot++) {
                if (!previous[slot].isEmpty()) continue;
                amounts[slot] = Math.min(remaining, limit);
                remaining -= amounts[slot];
            }
            if (remaining == originalCount) return; // Full: leave the actual entity completely untouched.
            if (!canEat(server, entity) || entity.getItem() != source || source.getCount() != originalCount) return;
            for (int slot = 0; slot < SIZE; slot++) {
                if (items.get(slot) != previous[slot] || previous[slot].getCount() != counts[slot]) return;
            }

            // RAW OWNERSHIP WRITES: no Container.setItem/setChanged, world
            // events, audio or removal hooks until every paid unit is in a slot.
            ItemStack paid = source.split(originalCount - remaining);
            for (int slot = 0; slot < SIZE; slot++) {
                int amount = amounts[slot];
                if (amount == 0) continue;
                if (previous[slot].isEmpty()) items.set(slot, paid.split(amount));
                else {
                    paid.shrink(amount);
                    previous[slot].grow(amount);
                }
            }
            // Split, rather than clone, the live remainder. Its former entity-
            // held object is empty BEFORE setter/removal observers can reenter.
            // A different native data object also synchronizes the reduced count.
            ItemStack remainder = source.split(source.getCount());
            if (remainder.isEmpty()) entity.discard();
            else entity.setItem(remainder); // SAME entity/UUID, motion, age, delay and owner.

            if (isCurrent()) {
                setChanged();
                if (isCurrent()) server.blockEvent(worldPosition, getBlockState().getBlock(), 2, 2);
            }
            server.playSound(null, entity.getX(), entity.getY(), entity.getZ(), SoundEvents.GENERIC_EAT,
                    SoundSource.BLOCKS, .25F, (server.random.nextFloat() - server.random.nextFloat()) * .2F + 1);
        } finally { eating = false; }
    }

    /** Block replacement transfers ownership to the existing durable drop path. */
    public void evacuate() {
        if (!(level instanceof ServerLevel server) || evacuating || isEmpty()) return;
        evacuating = true;
        try {
            DeferredItemDrops.enqueue(server, worldPosition, items);
            items.clear();
            // No comparator/update callbacks inside LevelChunk's removal.
        } finally { evacuating = false; }
    }

    @Override protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        ContainerHelper.saveAllItems(tag, items, registries);
    }
    @Override protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        items.clear();
        ContainerHelper.loadAllItems(tag, items, registries);
        // Lid/users are transient in TC4. These extra fields occur only in the
        // initial client chunk tag, so a newly tracking client sees an open lid.
        numUsingPlayers = Math.max(0, tag.getInt("HungryChestUsers"));
        lidAngle = Mth.clamp(tag.getFloat("HungryChestLid"), 0, 1);
        prevLidAngle = lidAngle;
    }
    @Override public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        CompoundTag tag = saveWithoutMetadata(registries);
        tag.putInt("HungryChestUsers", numUsingPlayers);
        tag.putFloat("HungryChestLid", lidAngle);
        return tag;
    }
}
