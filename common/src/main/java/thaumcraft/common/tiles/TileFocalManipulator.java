package thaumcraft.common.tiles;

import dev.architectury.registry.menu.MenuRegistry;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.SimpleMenuProvider;
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
import thaumcraft.api.visnet.VisNetHandler;
import thaumcraft.api.wands.FocusUpgradeType;
import thaumcraft.api.wands.ItemFocusBasic;
import thaumcraft.common.Thaumcraft;
import thaumcraft.common.config.ThaumcraftFocalManipulation;
import thaumcraft.common.container.ContainerFocalManipulator;
import thaumcraft.common.lib.capabilities.KnowledgeAccess;
import thaumcraft.common.lib.foci.FocalUpgradeCosts;
import thaumcraft.common.lib.utils.DeferredItemDrops;

/** Native single-slot paid job. Source authority: original TileFocalManipulator.
 * Safety deviation: external slot mutations CANCEL before another debit instead
 * of clearing only the debt and upgrading a replacement item for free (original
 * setter74-81 followed by tick92-108). No slot lock and no resource refund. */
public final class TileFocalManipulator extends BlockEntity implements WorldlyContainer, MenuProvider {
    private static final int SCHEMA = 1;
    private static final ThreadLocal<Boolean> ACTIVE = ThreadLocal.withInitial(() -> false);
    private static final SoundEvent START = sound("craftstart"), FAIL = sound("craftfail"), FINISH = sound("wand");
    private ItemStack item = ItemStack.EMPTY, observed = ItemStack.EMPTY;
    private AspectList remaining = new AspectList();
    private int size, rank = -1, upgrade = -1, ticks;
    private long revision;
    private UUID lifetime = UUID.randomUUID();
    private Job job;
    private CompoundTag rejected;
    private boolean working, evacuating, notifying;
    private static ClientEffects clientEffects;

    @FunctionalInterface public interface ClientEffects { void tick(Level level, BlockPos pos); }
    public static void setClientEffects(ClientEffects effects) { clientEffects = effects; }

    private record Job(UUID id, long revision, ItemStack baseline, AspectList initial) {}

    public TileFocalManipulator(BlockPos pos, BlockState state) {
        super(ThaumcraftFocalManipulation.FOCAL_MANIPULATOR_BLOCK_ENTITY.get(), pos, state);
    }
    public int getSize() { return size; }
    public int getRank() { return rank; }
    public int getUpgrade() { return upgrade; }
    public int getTickCount() { return ticks; }
    public AspectList getAspects() { return remaining.copy(); }
    public boolean isQuarantined() { return rejected != null; }
    public UUID lifetimeId() { return lifetime; }
    public long getSelectionRevision() { observeExternalChange(); return revision; }

    private boolean current() {
        return level != null && (!(level instanceof ServerLevel) || VisNetHandler.isNativeServerLevel(level))
                && !isRemoved() && level.hasChunkAt(worldPosition)
                && level.getBlockEntity(worldPosition) == this && level.getBlockState(worldPosition) == getBlockState()
                && getBlockState().is(ThaumcraftFocalManipulation.FOCAL_MANIPULATOR.get());
    }
    private boolean mutable() {
        if (!(level instanceof ServerLevel server)) return false;
        if (!server.getServer().isSameThread()) throw new IllegalStateException("Focal mutations require the server thread");
        return current() && !isQuarantined() && !evacuating && revision < Long.MAX_VALUE - 4;
    }
    @Override public boolean stillValid(Player player) {
        return player != null && current() && !isQuarantined() && player.level() == level && player.isAlive()
                && !player.isSpectator() && player.distanceToSqr(worldPosition.getX() + .5,
                worldPosition.getY() + .5, worldPosition.getZ() + .5) <= 64
                && KnowledgeAccess.get(player).isResearchComplete("FOCALMANIPULATION");
    }

    public void openMenu(ServerPlayer player) {
        if (!mutable() || !stillValid(player) || player.isShiftKeyDown()) return;
        UUID session = UUID.randomUUID();
        MenuRegistry.openExtendedMenu(player, new SimpleMenuProvider(
                (id, inventory, viewer) -> new ContainerFocalManipulator(id, inventory, this, session), getDisplayName()),
                data -> { data.writeBlockPos(worldPosition); data.writeUUID(session); });
    }
    @Override public Component getDisplayName() { return Component.translatable("container.thaumcraft.focal_manipulator"); }
    @Override public AbstractContainerMenu createMenu(int id, Inventory inventory, Player player) {
        return new ContainerFocalManipulator(id, inventory, this);
    }

    /** Only an admitting session-aware menu can call this public compatibility
     * entrypoint. An ID-only caller cannot bypass the payload/session fence. */
    public boolean startCraft(int id, ServerPlayer player, long expectedRevision) {
        if (!mutable() || working || ACTIVE.get() || !(player.containerMenu instanceof ContainerFocalManipulator menu)
                || !menu.authorizesStart(this, player, expectedRevision)) return false;
        observeExternalChange();
        if (revision != expectedRevision || job != null || size != 0 || !stillValid(player)
                || item.getCount() != 1 || !(item.getItem() instanceof ItemFocusBasic focus)) return false;
        working = true; ACTIVE.set(true);
        try {
            ItemStack baseline = item.copy();
            short[] before = applied(focus, baseline);
            int nextRank = firstHole(before);
            FocusUpgradeType type = FocusUpgradeType.get(id);
            if (nextRank > 5 || type == null || player.experienceLevel < nextRank * 8
                    || !offered(focus, baseline, type, nextRank)
                    || !focus.canApplyUpgrade(baseline.copy(), player, type, nextRank)) return false;
            FocalUpgradeCosts.Plan plan = FocalUpgradeCosts.plan(baseline, type, nextRank);
            // All extension queries used private copies. They may still call out
            // into the world, so neither the admission nor the item is cached trust.
            if (!mutable() || !menu.authorizesStart(this, player, expectedRevision) || revision != expectedRevision
                    || job != null || !ItemStack.matches(item, baseline) || !ItemStack.matches(observed, baseline)
                    || !stillValid(player) || player.experienceLevel < plan.xpLevels()) return false;
            long nextRevision = revision + 1;
            Job admitted = new Job(UUID.randomUUID(), nextRevision, baseline.copy(), plan.aspects());
            if (!player.isCreative()) player.giveExperienceLevels(-plan.xpLevels());
            revision = nextRevision; job = admitted; remaining = plan.aspects(); size = plan.initialTotal();
            rank = nextRank; upgrade = type.id; observed = item.copy();
            changed(); play(START, .25F);
            return true;
        } catch (RuntimeException failure) {
            Thaumcraft.LOG.warn("Focal admission refused at {}", worldPosition, failure);
            return false;
        } finally { working = false; ACTIVE.remove(); }
    }

    public static void tick(Level level, BlockPos pos, BlockState state, TileFocalManipulator tile) {
        if (level.isClientSide) {
            if (tile.level != level || !tile.worldPosition.equals(pos) || state != tile.getBlockState()
                    || !tile.current() || tile.isQuarantined()) return;
            ++tile.ticks;
            if (tile.size > 0 && clientEffects != null) clientEffects.tick(level, pos);
            return;
        }
        if (!(level instanceof ServerLevel server)) return;
        if (!server.getServer().isSameThread()) throw new IllegalStateException("Focal ticks require the server thread");
        if (tile.level != level || !tile.worldPosition.equals(pos) || state != tile.getBlockState()
                || !tile.current() || tile.isQuarantined() || tile.working || ACTIVE.get()) return;
        tile.working = true; ACTIVE.set(true);
        try { tile.serverTick(); }
        finally { tile.working = false; ACTIVE.remove(); }
    }
    private void serverTick() {
        observeExternalChange();
        if (rank < 0) rank = 0; // Original transient completion/reset sentinel.
        ++ticks; // Neither start nor disk persistence resets this live phase.
        if (ticks % 5 != 0 || job == null || !bound(job)) return;
        Job paid = job;
        if (size <= 0 || remaining.visSize() <= 0) { cancelJob(); changed(); return; }
        for (Aspect aspect : FocalUpgradeCosts.drainOrder(remaining)) {
            if (!bound(paid)) return;
            int request = Math.min(100, remaining.getAmount(aspect));
            if (request <= 0) continue;
            int actual = VisNetHandler.drainVis(level, worldPosition, aspect, request);
            // Native provider/pulse/FX call-outs are POST debit. An extracted,
            // reloaded or replaced owner cannot credit or reuse that paid grant.
            if (!bound(paid)) return;
            if (actual < 0 || actual > request) {
                cancelJob(); changed();
                Thaumcraft.LOG.warn("Focal consumer refused invalid VisNet grant at {}", worldPosition);
                return;
            }
            if (actual == 0) continue;
            remaining.reduce(aspect, actual);
            if (remaining.visSize() == 0) { complete(paid); return; }
            changed();
        }
    }
    private boolean bound(Job paid) {
        observeExternalChange();
        return current() && !isQuarantined() && job == paid && revision == paid.revision()
                && ItemStack.matches(item, paid.baseline());
    }
    private void complete(Job paid) {
        if (!bound(paid)) return;
        final int completedRank = rank, completedUpgrade = upgrade;
        ItemStack candidate = paid.baseline().copy();
        // Retire BEFORE the overridable apply hook. A hook/save cannot resurrect
        // an active zero-debt job, and false/throw is a spent, failed job, not success.
        cancelJob();
        long terminalRevision = revision;
        boolean success = false;
        try {
            FocusUpgradeType type = FocusUpgradeType.get(completedUpgrade);
            if (type != null && candidate.getItem() instanceof ItemFocusBasic focus) {
                short[] expected = applied(focus, paid.baseline()); expected[completedRank - 1] = type.id;
                success = focus.applyUpgrade(candidate, type, completedRank)
                        && candidate.getCount() == 1 && candidate.getItem() == paid.baseline().getItem()
                        && Arrays.equals(expected, applied(focus, candidate));
            }
        } catch (RuntimeException failure) { Thaumcraft.LOG.warn("Focal completion failed at {}", worldPosition, failure); }
        observeExternalChange();
        if (!current() || isQuarantined() || revision != terminalRevision
                || !ItemStack.matches(item, paid.baseline())) return;
        if (success && revision < Long.MAX_VALUE) { item = candidate.copy(); observed = item.copy(); ++revision; }
        else success = false;
        changed(); play(success ? FINISH : FAIL, success ? 1 : .33F);
    }

    private static short[] applied(ItemFocusBasic focus, ItemStack stack) {
        short[] ids = focus.getAppliedUpgrades(stack.copy());
        if (ids == null || ids.length != 5) throw new IllegalArgumentException("Focus needs five ordered upgrade slots");
        for (short id : ids) if (id != -1 && FocusUpgradeType.get(id) == null) throw new IllegalArgumentException("Unknown applied upgrade");
        // The public level query additionally validates item-specific component
        // support in the native focus API, including an explicitly empty vector.
        int expectedFrugal = 0;
        for (short id : ids) if (id == FocusUpgradeType.frugal.id) ++expectedFrugal;
        if (focus.getUpgradeLevel(stack.copy(), FocusUpgradeType.frugal) != expectedFrugal)
            throw new IllegalArgumentException("Inconsistent focus upgrade data");
        return ids.clone();
    }
    private static int firstHole(short[] ids) { for (int i = 0; i < 5; i++) if (ids[i] == -1) return i + 1; return 6; }
    private static boolean offered(ItemFocusBasic focus, ItemStack stack, FocusUpgradeType type, int rank) {
        FocusUpgradeType[] choices = focus.getPossibleUpgradesByRank(stack.copy(), rank);
        if (choices == null) return false;
        for (FocusUpgradeType choice : choices) if (choice == type && FocusUpgradeType.get(choice.id) == choice) return true;
        return false;
    }
    private void cancelJob() {
        job = null; size = 0; remaining = new AspectList(); rank = -1;
        if (revision < Long.MAX_VALUE) ++revision;
        markDirtyOnly();
    }
    private void observeExternalChange() {
        if (level == null || level.isClientSide || isQuarantined() || ItemStack.matches(item, observed)) return;
        if (!(level instanceof ServerLevel server) || !server.getServer().isSameThread()) throw new IllegalStateException("Focal observation requires the server thread");
        cancelJob(); observed = item.copy(); changed();
    }
    private void externalMutation() { cancelJob(); observed = item.copy(); changed(); }

    @Override public int getContainerSize() { return 1; }
    @Override public boolean isEmpty() { return item.isEmpty(); }
    @Override public ItemStack getItem(int slot) { return slot == 0 ? item : ItemStack.EMPTY; }
    @Override public ItemStack removeItem(int slot, int amount) {
        if (slot != 0 || amount <= 0 || !mutable() || item.isEmpty()) return ItemStack.EMPTY;
        ItemStack removed = item.split(amount);
        if (!removed.isEmpty()) externalMutation();
        return removed;
    }
    @Override public ItemStack removeItemNoUpdate(int slot) {
        if (slot != 0 || !mutable() || item.isEmpty()) return ItemStack.EMPTY;
        ItemStack removed = item; item = ItemStack.EMPTY; externalMutation(); return removed;
    }
    @Override public void setItem(int slot, ItemStack stack) {
        if (slot != 0 || !mutable()) return;
        item = stack.isEmpty() ? ItemStack.EMPTY : stack.copy();
        externalMutation();
    }
    @Override public void clearContent() {
        if (!mutable()) return;
        item = ItemStack.EMPTY; externalMutation();
    }
    @Override public boolean canPlaceItem(int slot, ItemStack stack) {
        return slot == 0 && !isQuarantined() && !stack.isEmpty() && stack.getItem() instanceof ItemFocusBasic;
    }
    @Override public int[] getSlotsForFace(Direction face) { return isQuarantined() ? new int[0] : new int[]{0}; }
    @Override public boolean canPlaceItemThroughFace(int slot, ItemStack stack, Direction face) { return canPlaceItem(slot, stack); }
    @Override public boolean canTakeItemThroughFace(int slot, ItemStack stack, Direction face) { return slot == 0 && !isQuarantined(); }
    @Override public void setChanged() { observeExternalChange(); changed(); }
    private void markDirtyOnly() { if (level instanceof ServerLevel server && current()) server.blockEntityChanged(worldPosition); }
    private void changed() {
        markDirtyOnly();
        if (!(level instanceof ServerLevel) || !current() || notifying || evacuating) return;
        notifying = true;
        try { level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), Block.UPDATE_CLIENTS); }
        catch (RuntimeException failure) { Thaumcraft.LOG.warn("Focal post-commit sync failed at {}", worldPosition, failure); }
        finally { notifying = false; }
    }
    public void failStart() { if (level instanceof ServerLevel server && server.getServer().isSameThread() && current()) play(FAIL, .33F); }
    private void play(SoundEvent event, float volume) {
        if (!current() || !(level instanceof ServerLevel)) return;
        try { level.playSound(null, worldPosition.getX(), worldPosition.getY(), worldPosition.getZ(), event, SoundSource.BLOCKS, volume, 1); }
        catch (RuntimeException failure) { Thaumcraft.LOG.warn("Focal post-commit sound failed at {}", worldPosition, failure); }
    }
    private static SoundEvent sound(String name) { return SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath("thaumcraft", name)); }

    public void evacuate() {
        if (!(level instanceof ServerLevel server) || !VisNetHandler.isNativeServerLevel(level) || evacuating || isQuarantined() || isRemoved()
                || !level.hasChunkAt(worldPosition) || level.getBlockEntity(worldPosition) != this) return;
        if (!server.getServer().isSameThread()) throw new IllegalStateException("Focal evacuation requires the server thread");
        evacuating = true;
        try {
            if (!item.isEmpty()) DeferredItemDrops.enqueue(server, worldPosition, List.of(item.copy()));
            item = ItemStack.EMPTY; observed = ItemStack.EMPTY; cancelJob();
            // No sync/comparator/spawn callback inside LevelChunk replacement.
        } finally { evacuating = false; }
    }

    @Override protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) { super.saveAdditional(tag, registries); writeState(tag, registries); }
    private void writeState(CompoundTag tag, HolderLookup.Provider registries) {
        if (rejected != null) { tag.put("RejectedFocal", rejected.copy()); return; }
        CompoundTag state = new CompoundTag();
        state.putInt("Schema", SCHEMA); state.putLong("Revision", revision); state.putInt("Size", size);
        state.putInt("Rank", rank); state.putInt("Upgrade", upgrade); remaining.writeToNBT(state, "Remaining");
        if (!item.isEmpty()) state.put("Stack", item.save(registries));
        if (job != null) {
            CompoundTag paid = new CompoundTag(); paid.putUUID("Id", job.id()); paid.putLong("Revision", job.revision());
            paid.put("Baseline", job.baseline().save(registries)); job.initial().writeToNBT(paid, "Initial"); state.put("Job", paid);
        }
        tag.put("FocalState", state);
    }
    @Override protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        if (ACTIVE.get()) throw new IllegalStateException("Focal reload cannot resurrect state during an active payment/debit/apply");
        if (level instanceof ServerLevel server && (!server.getServer().isSameThread() || working || evacuating)) throw new IllegalStateException("Focal reload requires an idle server owner");
        super.loadAdditional(tag, registries);
        // The counter is not serialized. A new BE naturally starts at zero;
        // reading an update into an existing owner must not restart its phase.
        item = observed = ItemStack.EMPTY; remaining = new AspectList(); size = 0; rank = upgrade = -1;
        revision = 0; job = null; rejected = null; lifetime = UUID.randomUUID();
        if (tag.contains("RejectedFocal")) { quarantine(tag.get("RejectedFocal") instanceof CompoundTag saved ? saved : tag); return; }
        try {
            requireType(tag, "FocalState", Tag.TAG_COMPOUND);
            CompoundTag state = tag.getCompound("FocalState");
            for (String key : List.of("Schema", "Size", "Rank", "Upgrade")) requireType(state, key, Tag.TAG_INT);
            requireType(state, "Revision", Tag.TAG_LONG);
            if (state.getInt("Schema") != SCHEMA) throw new IllegalArgumentException("Unknown focal schema");
            long parsedRevision = state.getLong("Revision"); int parsedSize = state.getInt("Size"), parsedRank = state.getInt("Rank"), parsedUpgrade = state.getInt("Upgrade");
            if (parsedRevision < 0 || parsedRevision >= Long.MAX_VALUE - 2 || parsedSize < 0 || parsedRank < -1 || parsedRank > 5
                    || parsedUpgrade != -1 && FocusUpgradeType.get(parsedUpgrade) == null) throw new IllegalArgumentException("Invalid focal scalars");
            ItemStack parsedItem = state.contains("Stack") ? strictStack(state, "Stack", registries) : ItemStack.EMPTY;
            if (!parsedItem.isEmpty() && parsedItem.getItem() instanceof ItemFocusBasic focus) applied(focus, parsedItem);
            AspectList parsedRemaining = strictAspects(state, "Remaining", true);
            Job parsedJob = null;
            if (state.contains("Job")) {
                requireType(state, "Job", Tag.TAG_COMPOUND); CompoundTag paid = state.getCompound("Job");
                requireType(paid, "Revision", Tag.TAG_LONG);
                if (!paid.hasUUID("Id") || paid.getLong("Revision") != parsedRevision || parsedSize <= 0
                        || parsedRank < 1 || parsedRank > 5 || parsedRemaining.visSize() <= 0
                        || parsedItem.getCount() != 1 || !(parsedItem.getItem() instanceof ItemFocusBasic focus)) throw new IllegalArgumentException("Invalid paid focal job");
                ItemStack baseline = strictStack(paid, "Baseline", registries);
                AspectList initial = strictAspects(paid, "Initial", false);
                FocusUpgradeType type = FocusUpgradeType.get(parsedUpgrade);
                if (!ItemStack.matches(parsedItem, baseline) || firstHole(applied(focus, baseline)) != parsedRank
                        || type == null || !offered(focus, baseline, type, parsedRank)) throw new IllegalArgumentException("Focal target binding mismatch");
                FocalUpgradeCosts.Plan plan = FocalUpgradeCosts.plan(baseline, type, parsedRank);
                if (plan.initialTotal() != parsedSize || !orderedEquals(initial, plan.aspects())
                        || !Arrays.equals(initial.getAspects(), parsedRemaining.getAspects())) throw new IllegalArgumentException("Focal cost/binding mismatch");
                for (Aspect aspect : initial.getAspects()) if (parsedRemaining.getAmount(aspect) > initial.getAmount(aspect)) throw new IllegalArgumentException("Focal debt exceeds initial cost");
                parsedJob = new Job(paid.getUUID("Id"), parsedRevision, baseline.copy(), initial.copy());
            } else if (parsedSize != 0 || parsedRemaining.size() != 0 || parsedRank > 0) throw new IllegalArgumentException("Unbound focal debt");
            item = parsedItem; observed = item.copy(); remaining = parsedRemaining; size = parsedSize;
            rank = parsedRank; upgrade = parsedUpgrade; revision = parsedRevision; job = parsedJob;
        } catch (RuntimeException malformed) {
            quarantine(tag); Thaumcraft.LOG.warn("Quarantined malformed focal state at {}", worldPosition, malformed);
        }
    }
    private static ItemStack strictStack(CompoundTag tag, String key, HolderLookup.Provider registries) {
        requireType(tag, key, Tag.TAG_COMPOUND);
        ItemStack stack = ItemStack.parse(registries, tag.getCompound(key)).orElseThrow(() -> new IllegalArgumentException("Invalid focal item"));
        if (stack.isEmpty() || !stack.save(registries).equals(tag.getCompound(key))) throw new IllegalArgumentException("Discarded/noncanonical focal components");
        return stack;
    }
    private static AspectList strictAspects(CompoundTag tag, String key, boolean zeroAllowed) {
        requireType(tag, key, Tag.TAG_LIST); ListTag list = tag.getList(key, Tag.TAG_COMPOUND);
        if (((ListTag) tag.get(key)).size() != list.size()) throw new IllegalArgumentException("Wrong focal debt entries");
        AspectList result = new AspectList(); long total = 0;
        for (Tag raw : list) {
            CompoundTag row = (CompoundTag) raw; requireType(row, "key", Tag.TAG_STRING); requireType(row, "amount", Tag.TAG_INT);
            Aspect aspect = Aspect.getAspect(row.getString("key")); int amount = row.getInt("amount");
            if (aspect == null || !aspect.isPrimal() || result.aspects.containsKey(aspect) || amount < 0 || !zeroAllowed && amount == 0
                    || (total += amount) > Integer.MAX_VALUE) throw new IllegalArgumentException("Invalid focal primal debt");
            result.add(aspect, amount);
        }
        return result;
    }
    private static void requireType(CompoundTag tag, String key, int type) { if (!tag.contains(key, type)) throw new IllegalArgumentException("Missing/wrong focal field " + key); }
    private static boolean orderedEquals(AspectList left, AspectList right) { return left.aspects.equals(right.aspects) && Arrays.equals(left.getAspects(), right.getAspects()); }
    private void quarantine(CompoundTag payload) {
        rejected = payload.copy(); item = observed = ItemStack.EMPTY; remaining = new AspectList(); job = null;
        size = ticks = 0; rank = upgrade = -1; revision = 0;
    }
    @Override public CompoundTag getUpdateTag(HolderLookup.Provider registries) { CompoundTag tag = new CompoundTag(); writeState(tag, registries); return tag; }
    @Override public ClientboundBlockEntityDataPacket getUpdatePacket() { return ClientboundBlockEntityDataPacket.create(this); }
}
