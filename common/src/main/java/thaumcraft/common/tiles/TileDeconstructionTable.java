package thaumcraft.common.tiles;

import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.Nameable;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.api.aspects.AspectList;
import thaumcraft.common.Thaumcraft;
import thaumcraft.common.config.ThaumcraftDeconstruction;
import thaumcraft.common.container.ContainerDeconstructionTable;
import thaumcraft.common.lib.capabilities.KnowledgeAccess;
import thaumcraft.common.lib.network.KnowledgeEvents;
import thaumcraft.common.lib.research.DeconstructionRules;
import thaumcraft.common.lib.research.ObjectAspectResolver;
import thaumcraft.common.lib.utils.DeferredItemDrops;

/** One physical input and one unclaimed primal point; elapsed work is deliberately not saved. */
public final class TileDeconstructionTable extends BlockEntity implements WorldlyContainer, MenuProvider, Nameable {
    private ItemStack input = ItemStack.EMPTY;
    private Aspect reward;
    private int breakTime;
    private Component customName;
    private CompoundTag rejected;
    private boolean working, claiming, evacuating;

    public TileDeconstructionTable(BlockPos pos, BlockState state) { super(ThaumcraftDeconstruction.TILE.get(), pos, state); }
    public Aspect reward() { return reward; }
    public int breakTime() { return breakTime; }
    public boolean isQuarantined() { return rejected != null; }
    public boolean current() {
        return level != null && !isRemoved() && level.hasChunkAt(worldPosition) && level.getBlockEntity(worldPosition) == this
                && level.getBlockState(worldPosition).is(ThaumcraftDeconstruction.TABLE.get());
    }
    private boolean serverOwner() { return level instanceof ServerLevel server && server.getServer().isSameThread() && current() && !isQuarantined() && !evacuating; }

    public static void tick(Level level, BlockPos pos, BlockState state, TileDeconstructionTable tile) {
        if (!tile.serverOwner() || tile.working || tile.claiming) return;
        tile.working = true;
        try { tile.advance(); }
        catch (RuntimeException failure) { tile.breakTime = 0; Thaumcraft.LOG.warn("Deconstruction work failed at {}; current input and reward retained", pos, failure); }
        finally { tile.working = false; }
    }
    private void advance() {
        if (reward != null || input.isEmpty()) { breakTime = 0; return; }
        ItemStack before = ItemStack.parse(level.registryAccess(), input.copyWithCount(1).save(level.registryAccess())).orElseThrow();
        before.setCount(input.getCount());
        AspectList tags = ObjectAspectResolver.get(before, level);
        if (!serverOwner() || !ItemStack.matches(input, before)) return;
        if (tags.size() == 0) { breakTime = 0; return; }
        AspectList primals = DeconstructionRules.primals(tags);
        if (primals.size() == 0) { breakTime = 0; return; }
        boolean started = breakTime == 0;
        if (started) breakTime = DeconstructionRules.BREAK_TICKS;
        --breakTime; // Original starts and decrements during the SAME tick.
        if (breakTime == 0) {
            Aspect nextReward = DeconstructionRules.roll(primals, level.random::nextInt);
            if (!serverOwner() || !ItemStack.matches(input, before) || reward != null) return;
            input = before.getCount() == 1 ? ItemStack.EMPTY : before.copyWithCount(before.getCount() - 1);
            reward = nextReward; // Failure still consumes exactly one actual input.
            changed();
        } else if (started) changed();
    }

    public boolean takeReward(ServerPlayer player, ContainerDeconstructionTable menu) {
        if (!serverOwner() || working || claiming || reward == null || menu == null || !menu.owns(this, player)) return false;
        var knowledge = KnowledgeAccess.get(player);
        if (knowledge.getAspectPoolFor(reward.getTag()) == Integer.MAX_VALUE) return false;
        Aspect paidReward = reward;
        var after = knowledge.copy();
        if (!after.addAspectPool(reward.getTag(), 1)) return false;
        claiming = true;
        try {
            // Both owners change before block/menu/network notifications can reenter.
            reward = null; breakTime = 0; knowledge.copyFrom(after);
            try {
                changed(); KnowledgeEvents.sync(player);
                thaumcraft.common.lib.network.KnowledgeNotificationNetworking.pool(player, paidReward.getTag(), 1);
                menu.broadcastChanges();
            }
            catch (RuntimeException feedbackFailure) { Thaumcraft.LOG.warn("Deconstruction reward was granted; feedback failed at {}", worldPosition, feedbackFailure); }
            return true;
        } finally { claiming = false; }
    }

    @Override public int getContainerSize() { return 1; }
    @Override public boolean isEmpty() { return input.isEmpty(); }
    @Override public ItemStack getItem(int slot) { return slot == 0 && !isQuarantined() ? input : ItemStack.EMPTY; }
    @Override public int getMaxStackSize() { return 64; }
    @Override public ItemStack removeItem(int slot, int count) {
        if (slot != 0 || count <= 0 || isQuarantined() || claiming || evacuating) return ItemStack.EMPTY;
        ItemStack removed = input.split(count); if (input.isEmpty()) input = ItemStack.EMPTY;
        if (!removed.isEmpty()) changed(); return removed;
    }
    @Override public ItemStack removeItemNoUpdate(int slot) {
        if (slot != 0 || isQuarantined() || claiming || evacuating) return ItemStack.EMPTY;
        ItemStack removed = input; input = ItemStack.EMPTY; return removed;
    }
    @Override public void setItem(int slot, ItemStack stack) {
        if (slot != 0 || stack == null || isQuarantined() || claiming || evacuating) return;
        // Native Slots enforce 64. Retain an externally supplied whole stack
        // instead of reproducing the original setter's destructive truncation.
        input = stack; changed();
    }
    @Override public void clearContent() { if (!isQuarantined() && !claiming && !evacuating) { input = ItemStack.EMPTY; changed(); } }
    @Override public boolean canPlaceItem(int slot, ItemStack stack) {
        return slot == 0 && !isQuarantined() && stack != null && !stack.isEmpty() && level != null && ObjectAspectResolver.get(stack, level).size() > 0;
    }
    @Override public int[] getSlotsForFace(Direction side) { return side == Direction.UP || isQuarantined() ? new int[0] : new int[]{0}; }
    @Override public boolean canPlaceItemThroughFace(int slot, ItemStack stack, Direction side) { return side != Direction.UP && canPlaceItem(slot, stack); }
    @Override public boolean canTakeItemThroughFace(int slot, ItemStack stack, Direction side) { return slot == 0 && !isQuarantined(); }
    @Override public boolean stillValid(Player player) { return current() && player.level() == level && player.distanceToSqr(worldPosition.getX() + .5, worldPosition.getY() + .5, worldPosition.getZ() + .5) <= 64; }

    public void evacuate() {
        if (!(level instanceof ServerLevel server) || !server.getServer().isSameThread() || evacuating || isQuarantined()) return;
        evacuating = true;
        try {
            if (!input.isEmpty()) DeferredItemDrops.enqueue(server, worldPosition, List.of(input.copy()));
            input = ItemStack.EMPTY; reward = null; breakTime = 0; super.setChanged();
        } finally { evacuating = false; }
    }
    private void changed() {
        super.setChanged();
        if (level != null && !level.isClientSide && !isRemoved()) level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), Block.UPDATE_CLIENTS);
    }
    @Override public void setChanged() { changed(); }
    @Override public Component getName() { return customName == null ? Component.translatable("container.decontable") : customName; }
    @Override public Component getCustomName() { return customName; }
    public void setCustomName(Component name) { customName = name; changed(); }
    @Override public Component getDisplayName() { return getName(); }
    @Override public AbstractContainerMenu createMenu(int id, Inventory inventory, Player player) { return new ContainerDeconstructionTable(id, inventory, this); }

    @Override protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        if (rejected != null) { tag.put("RejectedDeconstruction", rejected.copy()); return; }
        ListTag items = new ListTag();
        if (!input.isEmpty()) { CompoundTag item = (CompoundTag) input.copyWithCount(1).save(registries); item.putByte("Slot", (byte)0); items.add(item); }
        tag.put("Items", items);
        tag.putInt("InputCount", input.getCount());
        if (reward != null) tag.putString("Aspect", reward.getTag());
        if (customName != null) tag.putString("CustomName", Component.Serializer.toJson(customName, registries));
        // Original breaktime is transient and restarts at 40 after reload.
    }
    @Override protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        input = ItemStack.EMPTY; reward = null; breakTime = 0; customName = null; rejected = null;
        if (tag.contains("RejectedDeconstruction", Tag.TAG_COMPOUND)) { rejected = tag.getCompound("RejectedDeconstruction").copy(); return; }
        try {
            if (tag.contains("Items") && !tag.contains("Items", Tag.TAG_LIST)) throw new IllegalArgumentException("Invalid input list");
            if (tag.get("Items") instanceof ListTag rawItems && !rawItems.isEmpty() && rawItems.getElementType() != Tag.TAG_COMPOUND) throw new IllegalArgumentException("Invalid input entries");
            ListTag items = tag.getList("Items", Tag.TAG_COMPOUND);
            if (items.size() > 1) throw new IllegalArgumentException("Duplicate input inventory");
            if (!items.isEmpty()) {
                CompoundTag item = items.getCompound(0);
                if (!item.contains("Slot", Tag.TAG_ANY_NUMERIC) || item.getInt("Slot") != 0) throw new IllegalArgumentException("Invalid input slot");
                input = ItemStack.parse(registries, item).orElseThrow();
                if (input.isEmpty() || input.getCount() <= 0) throw new IllegalArgumentException("Invalid input stack");
            }
            if (tag.contains("InputCount")) {
                if (!tag.contains("InputCount", Tag.TAG_INT) || tag.getInt("InputCount") < 0 || input.isEmpty() != (tag.getInt("InputCount") == 0)) throw new IllegalArgumentException("Invalid native input count");
                if (!input.isEmpty()) input.setCount(tag.getInt("InputCount"));
            }
            if (tag.contains("Aspect")) {
                if (!tag.contains("Aspect", Tag.TAG_STRING)) throw new IllegalArgumentException("Invalid reward tag");
                reward = Aspect.getAspect(tag.getString("Aspect"));
                if (reward == null || !reward.isPrimal()) throw new IllegalArgumentException("Unknown reward primal");
            }
            if (tag.contains("CustomName")) {
                customName = Component.Serializer.fromJson(tag.getString("CustomName"), registries);
                if (customName == null) throw new IllegalArgumentException("Invalid custom name");
            }
        } catch (RuntimeException failure) { rejected = tag.copy(); input = ItemStack.EMPTY; reward = null; customName = null; }
    }
    @Override protected void applyImplicitComponents(DataComponentInput components) { super.applyImplicitComponents(components); customName = components.get(DataComponents.CUSTOM_NAME); }
    @Override protected void collectImplicitComponents(DataComponentMap.Builder components) { super.collectImplicitComponents(components); if (customName != null) components.set(DataComponents.CUSTOM_NAME, customName); }
    @Override public void removeComponentsFromTag(CompoundTag tag) { super.removeComponentsFromTag(tag); tag.remove("CustomName"); }
    @Override public CompoundTag getUpdateTag(HolderLookup.Provider registries) { return saveWithoutMetadata(registries); }
    @Override public ClientboundBlockEntityDataPacket getUpdatePacket() { return ClientboundBlockEntityDataPacket.create(this); }
}
