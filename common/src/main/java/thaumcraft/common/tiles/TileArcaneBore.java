package thaumcraft.common.tiles;

import dev.architectury.registry.menu.MenuRegistry;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
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
import net.minecraft.util.Mth;
import net.minecraft.world.Container;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.PickaxeItem;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import thaumcraft.api.wands.FocusUpgradeType;
import thaumcraft.common.Thaumcraft;
import thaumcraft.common.blocks.BlockArcaneBore;
import thaumcraft.common.config.ThaumcraftArcaneBore;
import thaumcraft.common.container.ContainerArcaneBore;
import thaumcraft.common.items.wands.ItemWandCasting;
import thaumcraft.common.items.wands.foci.FocusUpgradeData;
import thaumcraft.common.items.wands.foci.ItemFocusExcavation;
import thaumcraft.common.lib.bore.ArcaneBoreMath;
import thaumcraft.common.lib.bore.BoreWork;
import thaumcraft.common.lib.bore.BoreRepairState;
import thaumcraft.common.lib.essentia.EssentiaTransfers;

/** Stationary original bore scheduler and two-slot owner. REAL root BoreWork
 * owns every debit/wear/removal/output/evacuation; no fallback implementation.
 * The private native phases/consumable sequence do not accept supplied targets,
 * outputs, success booleans, mutable slot aliases or arbitrary commit callbacks. */
public final class TileArcaneBore extends BlockEntity implements Container, MenuProvider {
    private static final int SCHEMA = 1;
    private static final SoundEvent TOOL = SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath("thaumcraft", "tool"));
    private static final SoundEvent RUMBLE = SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath("thaumcraft", "rumble"));
    public enum WorkPhase { PREFILL, COMPLETE, RECONCILE, RETIRE, REPAIR }
    public enum CompletionState { READY, ARMED, REFUSED, WITNESSED, UNKNOWN }
    private ItemStack focus = ItemStack.EMPTY, pick = ItemStack.EMPTY;
    private ArcaneBoreMath.Stats stats = new ArcaneBoreMath.Stats(0, 0, 0);
    private ArcaneBoreMath.Rotation rotation = new ArcaneBoreMath.Rotation(0, 0, 0, 0, 0, 0);
    private ArcaneBoreMath.ScanCursor scan = ArcaneBoreMath.ScanCursor.initial();
    private Direction rotationDirection;
    private TileArcaneBoreBase boundBase;
    private UUID boundBaseLifetime;
    private long revision, sequence, visualSequence;
    private UUID lifetime = UUID.randomUUID(), identity = UUID.randomUUID();
    private int count;
    private float credit;
    private BoreRepairState repairState = BoreRepairState.EMPTY;
    private Target pending;
    private Completion completion;
    private WorkPhase workPhase;
    private CompoundTag rejected, workReference;
    private boolean ticking, mutating, serializing, selecting, retired, evacuated, fenced, needsReconciliation;
    // Client-only read state, not disk state or a source of server permission.
    private boolean clientFirst = true, clientOnlyPayload;
    private Direction clientDirection;
    private BlockPos visualTarget;
    private int topRotation, visualPause = 100, visualMaxPause = 100, beamLength;
    private boolean visualPending;
    private float aimX, aimZ, targetAimX, targetAimZ, aimVelocityX, aimVelocityZ;
    private long soundDelay;

    private record Target(BlockPos pos, BlockState state) {
        private Target { pos = pos.immutable(); }
    }
    private static final class Completion {
        private final long sequence;
        private final Target target;
        private CompletionState state = CompletionState.READY;
        private boolean selectionUsed;
        private Completion(long sequence, Target target) { this.sequence = sequence; this.target = target; }
    }

    public TileArcaneBore(BlockPos pos, BlockState state) {
        super(ThaumcraftArcaneBore.ARCANE_BORE_BLOCK_ENTITY.get(), pos, state);
        rotationDirection = state.getValue(BlockArcaneBore.MINING); // Initial placement does not invent a server turn.
    }
    public Direction miningDirection() { return getBlockState().getValue(BlockArcaneBore.MINING); }
    public Direction attachment() { return getBlockState().getValue(BlockArcaneBore.ATTACHMENT); }
    public UUID lifetimeId() { return lifetime; }
    public UUID machineIdentity() { return identity; }
    public long revision() { return revision; }
    public ArcaneBoreMath.Stats stats() { return stats; }
    public ArcaneBoreMath.Rotation rotation() { return rotation; }
    public ArcaneBoreMath.ScanCursor scanCursor() { return scan; }
    public int getTopRotation() { return topRotation; }
    public int getBeamLength() { return beamLength; }
    public float getAimX() { return aimX; }
    public float getAimZ() { return aimZ; }
    public int getCountdown() { return count; }
    public float getAccelerationCredit() { return credit; }
    public BoreRepairState repairState() { return repairState; }
    public boolean hasFocus() { return validSlot(0, focus); }
    public boolean hasPickaxe() { return validSlot(1, pick); }
    public boolean isQuarantined() { return rejected != null || fenced; }
    public boolean wasEvacuated() { return evacuated; }
    public boolean needsLoadedReconciliation() { return needsReconciliation; }
    public boolean isSerializing() { return serializing; }
    public BlockPos selectedTarget() { return pending == null ? null : pending.pos(); }
    public BlockState selectedState() { return pending == null ? null : pending.state(); }
    public BlockPos clientTarget() { return visualTarget; }
    public CompoundTag workReference() { return workReference == null ? null : workReference.copy(); }
    public CompoundTag rejectedPayload() { return rejected == null ? null : rejected.copy(); }
    public boolean isNativePhase(WorkPhase phase) {
        return ticking && workPhase == phase && !retired && !evacuated
                && (phase != WorkPhase.COMPLETE || completion != null);
    }
    public boolean isSelecting() { return ticking && selecting && !retired && !evacuated; }
    public boolean isCurrent() {
        return level != null && !retired && !isRemoved() && !evacuated
                && (level.isClientSide || !clientOnlyPayload && BoreWork.isCurrentServerOwner(this)) && level.isInWorldBounds(worldPosition)
                && level.hasChunkAt(worldPosition) && level.getBlockEntity(worldPosition) == this
                && level.getBlockState(worldPosition) == getBlockState()
                && getBlockState().is(ThaumcraftArcaneBore.ARCANE_BORE.get());
    }
    private TileArcaneBoreBase installedBase() {
        if (level == null) return null;
        BlockPos pos = ArcaneBoreMath.basePosition(worldPosition, attachment());
        if (!level.isInWorldBounds(pos) || !level.hasChunkAt(pos)) return null;
        return level.getBlockEntity(pos) instanceof TileArcaneBoreBase base && base.isCurrent() ? base : null;
    }
    public TileArcaneBoreBase accelerationBase() {
        TileArcaneBoreBase actual = installedBase();
        return actual == boundBase && actual != null && actual.lifetimeId().equals(boundBaseLifetime) ? actual : null;
    }
    public boolean isPowered() {
        if (!isCurrent()) return false;
        BlockPos base = ArcaneBoreMath.basePosition(worldPosition, attachment());
        // Native signal lookup can follow a conducting neighbour to its own
        // neighbours. Pause at an unloaded edge instead of loading for power.
        if (!loadedCube(worldPosition, 2) || !loadedCube(base, 2)) return false;
        return level.hasNeighborSignal(worldPosition) || level.hasNeighborSignal(base);
    }
    public static boolean validSlot(int slot, ItemStack stack) {
        if (stack == null || stack.isEmpty() || stack.getCount() != 1) return false;
        if (slot == 0) return stack.getItem() instanceof ItemFocusExcavation && FocusUpgradeData.supportedForFocus(stack);
        return slot == 1 && stack.getItem() instanceof PickaxeItem;
    }
    private void updateStats() {
        stats = derivedStats(focus, pick);
    }
    private ArcaneBoreMath.Stats derivedStats(ItemStack installedFocus, ItemStack installedPick) {
        boolean validFocus = validSlot(0, installedFocus), validPick = validSlot(1, installedPick);
        var data = validFocus ? FocusUpgradeData.read(installedFocus) : FocusUpgradeData.EMPTY;
        int fortune = 0, efficiency = 0;
        if (validPick && level != null) {
            var enchantments = level.registryAccess().registryOrThrow(Registries.ENCHANTMENT);
            fortune = EnchantmentHelper.getItemEnchantmentLevel(enchantments.getHolderOrThrow(Enchantments.FORTUNE), installedPick);
            efficiency = EnchantmentHelper.getItemEnchantmentLevel(enchantments.getHolderOrThrow(Enchantments.EFFICIENCY), installedPick);
        }
        return ArcaneBoreMath.stats(new ArcaneBoreMath.FocusLevels(data.level(FocusUpgradeType.potency),
                data.level(FocusUpgradeType.frugal), data.level(FocusUpgradeType.treasure), data.level(FocusUpgradeType.enlarge)), fortune, efficiency);
    }
    private boolean itemsValid() {
        return ArcaneBoreMath.itemsValid(validSlot(0, focus), validSlot(1, pick), !pick.isEmpty() && pick.isDamageableItem(),
                pick.getDamageValue(), pick.getMaxDamage());
    }
    public boolean inventoryAvailable() {
        if (!(level instanceof ServerLevel server)) return false;
        if (!server.getServer().isSameThread()) throw new IllegalStateException("Bore inventory requires the server thread");
        return isCurrent() && !isQuarantined() && !ticking && !mutating && !serializing && revision < Long.MAX_VALUE - 16
                && !BoreWork.isMutationActive() && !BoreWork.isLocked(this) && !EssentiaTransfers.isMutationActive();
    }
    @Override public int getContainerSize() { return 2; }
    @Override public int getMaxStackSize() { return 1; }
    @Override public boolean isEmpty() { return focus.isEmpty() && pick.isEmpty(); }
    @Override public ItemStack getItem(int slot) { return slot == 0 ? focus.copy() : slot == 1 ? pick.copy() : ItemStack.EMPTY; }
    @Override public boolean canPlaceItem(int slot, ItemStack stack) {
        return validSlot(slot, stack) && !isQuarantined()
                && (level != null && level.isClientSide || inventoryAvailable());
    }
    @Override public void setItem(int slot, ItemStack stack) {
        if (slot < 0 || slot > 1 || stack == null || !inventoryAvailable() || !stack.isEmpty() && !validSlot(slot, stack)) return;
        mutating = true;
        try {
            ItemStack prepared = stack.isEmpty() ? ItemStack.EMPTY : stack.copy();
            var preparedStats = derivedStats(slot == 0 ? prepared : focus, slot == 1 ? prepared : pick);
            if (!isCurrent() || isQuarantined()) return;
            if (slot == 0) focus = prepared; else pick = prepared;
            stats = preparedStats; ++revision; changed();
        } finally { mutating = false; }
    }
    @Override public ItemStack removeItem(int slot, int amount) {
        if (slot < 0 || slot > 1 || amount <= 0 || !inventoryAvailable()) return ItemStack.EMPTY;
        ItemStack old = slot == 0 ? focus : pick;
        if (old.isEmpty()) return ItemStack.EMPTY;
        mutating = true;
        try {
            var preparedStats = derivedStats(slot == 0 ? ItemStack.EMPTY : focus, slot == 1 ? ItemStack.EMPTY : pick);
            if (!isCurrent() || isQuarantined()) return ItemStack.EMPTY;
            if (slot == 0) focus = ItemStack.EMPTY; else pick = ItemStack.EMPTY;
            stats = preparedStats; ++revision; changed(); return old;
        } finally { mutating = false; }
    }
    @Override public ItemStack removeItemNoUpdate(int slot) { return removeItem(slot, 1); }
    @Override public void clearContent() {
        // Do not silently discard physical tools through an unrelated clear API.
        // Native clicks/quick-move use the two guarded slots; retirement is BoreWork.
        if (!isEmpty()) throw new IllegalStateException("Bore contents require native slot transfer or durable evacuation");
    }
    @Override public boolean stillValid(Player player) {
        return player != null && player.level() == level && player.isAlive() && !player.isSpectator() && isCurrent()
                && !isQuarantined() && player.distanceToSqr(worldPosition.getX() + .5, worldPosition.getY() + .5, worldPosition.getZ() + .5) <= 64;
    }
    public InteractionResult openMenu(Player player) {
        if (!stillValid(player) || !level.mayInteract(player, worldPosition)) return InteractionResult.FAIL;
        if (level.isClientSide) return InteractionResult.SUCCESS;
        if (!(player instanceof ServerPlayer sender) || !inventoryAvailable()) return InteractionResult.FAIL;
        MenuRegistry.openExtendedMenu(sender, new SimpleMenuProvider(
                (id, inventory, viewer) -> new ContainerArcaneBore(id, inventory, this), getDisplayName()),
                buffer -> { buffer.writeBlockPos(worldPosition); buffer.writeUUID(lifetime); });
        return InteractionResult.CONSUME;
    }
    @Override public Component getDisplayName() { return Component.translatable("container.thaumcraft.arcane_bore"); }
    @Override public AbstractContainerMenu createMenu(int id, Inventory inventory, Player player) { return new ContainerArcaneBore(id, inventory, this); }
    @Override public void setChanged() {
        if (level != null && !level.isClientSide && !ticking && !mutating && !serializing && !BoreWork.isMutationActive() && !EssentiaTransfers.isMutationActive()) changed();
    }
    private void changed() {
        if (!(level instanceof ServerLevel server) || !isCurrent()) return;
        // Stock has already committed. A post-commit notification failure must
        // not throw away a physically removed return stack or imply a refund.
        try { server.blockEntityChanged(worldPosition); }
        catch (RuntimeException failure) { Thaumcraft.LOG.warn("Bore dirty notification failed at {}", worldPosition, failure); }
        try { if (isCurrent()) level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), Block.UPDATE_CLIENTS); }
        catch (RuntimeException failure) { Thaumcraft.LOG.warn("Bore post-commit sync failed at {}", worldPosition, failure); }
    }
    public InteractionResult onWandUse(UseOnContext context) {
        Player player = context.getPlayer();
        if (player == null || player.level() != level || !worldPosition.equals(context.getClickedPos())
                || context.getItemInHand() != player.getItemInHand(context.getHand())
                || !(context.getItemInHand().getItem() instanceof ItemWandCasting) || context.getItemInHand().getCount() != 1) return InteractionResult.PASS;
        if (!stillValid(player) || !player.canInteractWithBlock(worldPosition, 0) || !level.mayInteract(player, worldPosition)
                || !player.mayUseItemAt(worldPosition, context.getClickedFace(), context.getItemInHand())) return InteractionResult.FAIL;
        if (level.isClientSide) return InteractionResult.SUCCESS;
        if (!inventoryAvailable()) return InteractionResult.FAIL;
        mutating = true;
        try {
            ++revision;
            rotation = ArcaneBoreMath.reorient(rotation, context.getClickedFace(), false);
            rotationDirection = context.getClickedFace();
            scan = ArcaneBoreMath.resetScanForOrientation(scan);
            pending = null; visualTarget = null; visualPause = 100;
            targetAimX = targetAimZ = aimVelocityX = aimVelocityZ = 0;
            BlockState desired = getBlockState().setValue(BlockArcaneBore.MINING, context.getClickedFace());
            level.setBlock(worldPosition, desired, Block.UPDATE_ALL);
            if (isCurrent() && getBlockState() == desired) {
                changed();
                level.playSound(null, worldPosition.getX() + .5, worldPosition.getY() + .5, worldPosition.getZ() + .5,
                        TOOL, SoundSource.BLOCKS, .5F, .9F + level.random.nextFloat() * .2F);
                player.swing(context.getHand());
            } else if (isCurrent()) {
                // A refused/replaced native state update is not permission to
                // mine using a direction different from the installed state.
                rotationDirection = miningDirection();
                rotation = ArcaneBoreMath.reorient(rotation, rotationDirection, false);
            }
            return InteractionResult.CONSUME; // Original costs0 vis; never activates the installed focus.
        } finally { mutating = false; }
    }

    /** Native block ticker binding only. Real root native-origin guard is a
     * required integration, not an externally supplied tick capability. */
    public static void tick(Level level, BlockPos pos, BlockState state, TileArcaneBore top) {
        if (!level.isClientSide && (!(level instanceof ServerLevel server) || !server.getServer().isSameThread()))
            throw new IllegalStateException("Bore tick requires the server thread");
        if (top.level != level || !pos.equals(top.worldPosition) || state != top.getBlockState() || !top.isCurrent()) return;
        if (level.isClientSide) { top.clientTick(); return; }
        if (top.ticking || top.mutating || top.serializing || BoreWork.isMutationActive() || EssentiaTransfers.isMutationActive()
                || !BoreWork.nativeTickAllowed(top)) return;
        top.ticking = true;
        try {
            top.serverTick();
            // Original repair runs after digging, including an ordinary early return for redstone OFF/no focus.
            if (top.isCurrent() && !top.isQuarantined() && !top.needsReconciliation && top.revision < Long.MAX_VALUE - 16) {
                top.workPhase = WorkPhase.REPAIR;
                try { BoreWork.repair(top); } finally { top.workPhase = null; }
            }
        } finally { top.ticking = false; top.selecting = false; top.workPhase = null; top.completion = null; }
    }
    private void serverTick() {
        if (needsReconciliation) {
            workPhase = WorkPhase.RECONCILE;
            try { BoreWork.reconcileLoaded(this); } finally { workPhase = null; }
            if (needsReconciliation || !isCurrent()) return;
        }
        if (rejected != null || fenced || revision >= Long.MAX_VALUE - 16) return;
        // Prefill is BEFORE rotation/power/items, even redstone OFF or no pick.
        workPhase = WorkPhase.PREFILL;
        try { BoreWork.prefill(this); } finally { workPhase = null; }
        if (!isCurrent() || isQuarantined()) return;
        // Detached disk loads have no registry Level; refresh the real pick's
        // enchantment holders once native ticking has attached that Level.
        updateStats();
        if (rotationDirection != miningDirection()) {
            rotationDirection = miningDirection();
            rotation = ArcaneBoreMath.reorient(rotation, rotationDirection, false);
            scan = ArcaneBoreMath.resetScanForOrientation(scan); pending = null;
            ++revision;
        }
        rotation = ArcaneBoreMath.stepRotation(rotation);
        if (!isPowered() || !itemsValid() || !rotation.aligned()) return;
        TileArcaneBoreBase actualBase = installedBase();
        if (actualBase == null) return;
        if (actualBase != boundBase || !actualBase.lifetimeId().equals(boundBaseLifetime)) {
            boundBase = actualBase; boundBaseLifetime = actualBase.lifetimeId(); pending = null;
        }
        if (--count > 0) return;
        if (pending == null) { selectNext(credit); return; }
        if (sequence == Long.MAX_VALUE) { fenced = true; changed(); return; }
        completion = new Completion(++sequence, pending);
        workPhase = WorkPhase.COMPLETE;
        try { BoreWork.finish(this); }
        finally {
            if (completion != null && (completion.state == CompletionState.READY || completion.state == CompletionState.ARMED) && isCurrent()) {
                // Root protocol failure is NOT an ordinary free retry. Do not
                // invent a successful outcome, refund or local output here.
                fenced = true; pending = null;
                changed();
                Thaumcraft.LOG.error("BoreWork left native completion{} in {} at {}; machine fenced", completion.sequence, completion.state, worldPosition);
            }
            workPhase = null; completion = null;
        }
        // NO implicit true/false success handling, debit, output or retry here.
        // Root consumes/fences this phase and requests guarded next-selection.
    }
    private void selectNext(float creditBeforeDigDebit) {
        if (!isCurrent() || isQuarantined() || selecting || installedBase() != boundBase || accelerationBase() == null) return;
        final long observedRevision = revision;
        final BlockState topState = getBlockState(), baseState = boundBase.getBlockState();
        final long baseRevision = boundBase.revision();
        selecting = true;
        try {
            ArcaneBoreMath.PlaneStep step;
            do { step = ArcaneBoreMath.stepPlane(scan, worldPosition, miningDirection(), stats.area()); scan = step.cursor(); }
            while (!step.changedPlane()); // Same native selection, NOT one sample per server tick.
            LoadedView view = new LoadedView();
            for (int index = 0; index < ArcaneBoreMath.CANDIDATE_READS; index++) {
                BlockPos pos = ArcaneBoreMath.candidate(scan.lastPlane(), miningDirection(), index);
                if (!loadedVacuum(pos)) return;
                BlockState state = level.getBlockState(pos);
                float hardness = state.getDestroySpeed(view, pos);
                if (!scanBinding(observedRevision, topState, baseState, baseRevision)) return;
                if (hardness < 0.0F) break; // Original column barrier, NOT skip-to-ore behind it.
                boolean collidable = !state.isAir() && !state.getCollisionShape(view, pos).isEmpty();
                if (!scanBinding(observedRevision, topState, baseState, baseRevision)) return;
                if (!collidable) continue;
                BlockPos chosen = pos; BlockState chosenState = state; float chosenHardness = hardness;
                Vec3 start = ArcaneBoreMath.occlusionRayStart(worldPosition, miningDirection()), end = Vec3.atCenterOf(pos);
                if (!rayLoaded(start, end)) return;
                BlockHitResult hit = view.clip(new ClipContext(start, end, ClipContext.Block.COLLIDER,
                        ClipContext.Fluid.NONE, CollisionContext.empty()));
                if (!scanBinding(observedRevision, topState, baseState, baseRevision)) return;
                if (hit.getType() == HitResult.Type.BLOCK) {
                    BlockPos near = hit.getBlockPos();
                    if (!loadedVacuum(near)) return;
                    BlockState nearState = level.getBlockState(near);
                    float nearHardness = nearState.getDestroySpeed(view, near);
                    if (!scanBinding(observedRevision, topState, baseState, baseRevision)) return;
                    if (nearHardness > -1.0F && !nearState.getCollisionShape(view, near).isEmpty()) {
                        chosen = near; chosenState = nearState; chosenHardness = nearHardness;
                    }
                }
                if (!scanBinding(observedRevision, topState, baseState, baseRevision)) return;
                // No known-block/BE/vacuum shortcut here: root's REAL boundary
                // must refuse unsupported targets before ANY destructive work.
                if (!loadedVacuum(chosen)) return;
                pending = new Target(chosen, chosenState);
                count = ArcaneBoreMath.delayForTarget(chosenHardness, stats.speed(), creditBeforeDigDebit);
                ++visualSequence;
                BoreWork.selected(this); // Actual private selection only; root S2C, no supplied target.
                return;
            }
            pending = null;
        } catch (UnavailableRead unavailable) { pending = null; }
        finally { selecting = false; }
    }
    private boolean scanBinding(long expectedRevision, BlockState topState, BlockState baseState, long baseRevision) {
        return isCurrent() && !isQuarantined() && revision == expectedRevision && getBlockState() == topState
                && accelerationBase() != null && boundBase.getBlockState() == baseState && boundBase.revision() == baseRevision;
    }
    private boolean loaded(BlockPos pos) { return level.isInWorldBounds(pos) && level.hasChunkAt(pos); }
    private boolean loadedVacuum(BlockPos pos) {
        return loadedCube(pos, 1);
    }
    private boolean loadedCube(BlockPos pos, int radius) {
        BlockPos low = pos.offset(-radius, -radius, -radius), high = pos.offset(radius, radius, radius);
        return level.isInWorldBounds(low) && level.isInWorldBounds(high) && level.hasChunksAt(low, high);
    }
    private boolean rayLoaded(Vec3 start, Vec3 end) {
        return BlockGetter.traverseBlocks(start, end, this, (top, pos) -> top.loaded(pos) ? null : Boolean.FALSE, top -> Boolean.TRUE);
    }
    private static final class UnavailableRead extends RuntimeException {
        private static final UnavailableRead INSTANCE = new UnavailableRead();
        private UnavailableRead() { super("Unavailable bore scan cell", null, false, false); }
    }
    private final class LoadedView implements BlockGetter {
        private final long rev = revision, baseRev = boundBase.revision();
        private final BlockState state = TileArcaneBore.this.getBlockState(), baseState = boundBase.getBlockState();
        private void requireReadable(BlockPos pos) {
            if (!scanBinding(rev, state, baseState, baseRev) || !loaded(pos)) throw UnavailableRead.INSTANCE;
        }
        @Override public BlockState getBlockState(BlockPos pos) {
            requireReadable(pos);
            return level.getBlockState(pos);
        }
        @Override public FluidState getFluidState(BlockPos pos) { return getBlockState(pos).getFluidState(); }
        @Override public BlockEntity getBlockEntity(BlockPos pos) {
            requireReadable(pos);
            return level.getBlockEntity(pos);
        }
        @Override public int getHeight() { return level.getHeight(); }
        @Override public int getMinBuildHeight() { return level.getMinBuildHeight(); }
    }

    // Root-owned private-constructor token API. There is intentionally no local
    // implementation of BoreWork and no unguarded credit/tool setter.
    public void applyWorkChange(BoreWork.Change change) {
        BoreWork.requireChange(this, change);
        if (change.expectedRevision() != revision || revision == Long.MAX_VALUE)
            throw new IllegalStateException("Stale bore raw change");
        switch (change.kind()) {
            case PREFILL -> {
                if (!isNativePhase(WorkPhase.PREFILL)) throw new IllegalStateException("No native bore prefill");
                credit = change.effectiveCredit();
            }
            case REPAIR -> {
                if (!isNativePhase(WorkPhase.REPAIR)) throw new IllegalStateException("No native bore repair");
                ItemStack preparedPick = change.preparedPick();
                BoreRepairState preparedRepair = change.preparedRepairState();
                if (preparedPick == null || preparedRepair == null) throw new IllegalStateException("Incomplete native bore repair change");
                pick = preparedPick; repairState = preparedRepair;
            }
            case ARM -> {
                requireUnconsumedCompletion();
                // Extract ALL prepared fields before the first actual slot write.
                ItemStack preparedPick = change.preparedPick();
                float preparedCredit = change.effectiveCredit();
                CompoundTag preparedReference = change.preparedReference();
                pick = preparedPick; credit = preparedCredit; workReference = preparedReference;
                completion.state = CompletionState.ARMED; pending = null;
            }
            case REFUSE -> { requireUnconsumedCompletion(); completion.state = CompletionState.REFUSED; pending = null; }
            case WITNESS -> { requireArmedCompletion(); workReference = change.preparedReference(); completion.state = CompletionState.WITNESSED; }
            case UNKNOWN -> { requireArmedCompletion(); workReference = change.preparedReference(); completion.state = CompletionState.UNKNOWN; fenced = true; pending = null; }
            case EVACUATE -> {
                // Root already owns these ACTUAL currently reserved/worn stacks
                // and rejected payload in its durable queue before this clear.
                focus = pick = ItemStack.EMPTY; rejected = null; evacuated = true; retired = true;
                boundBase = null; boundBaseLifetime = null; pending = null;
            }
            case RECONCILE -> {
                if (!isNativePhase(WorkPhase.RECONCILE)) throw new IllegalStateException("No native bore reconciliation");
                workReference = change.preparedReference(); fenced = change.fenced(); needsReconciliation = false;
            }
        }
        ++revision;
        // No codec/registry/world/notification calls during raw reservation.
        // Cached stats get recomputed only in the guarded notification phase.
    }
    /** Closed opaque sink for a dying owner, including one already detached by
     * a codec callback. The token never authorizes gameplay or physical output.
     * Transfer the ACTUAL objects, then clear, with no call-out between them. */
    public void retainRetirementRaw(BoreWork.RetirementRetention retention) {
        BoreWork.requireRetention(this, retention);
        retention.custody().claimRaw(retention, focus, pick, rejected, workReference);
        focus = pick = ItemStack.EMPTY; rejected = workReference = null;
        evacuated = retired = fenced = true;
        boundBase = null; boundBaseLifetime = null; pending = null;
        if (revision < Long.MAX_VALUE) ++revision;
    }
    private void requireUnconsumedCompletion() {
        if (!isNativePhase(WorkPhase.COMPLETE) || completion.state != CompletionState.READY) throw new IllegalStateException("No unconsumed native completion");
    }
    private void requireArmedCompletion() {
        if (!isNativePhase(WorkPhase.COMPLETE) || completion.state != CompletionState.ARMED) throw new IllegalStateException("No armed native completion");
    }
    public void selectAfterAttempt(BoreWork.Selection selection) {
        BoreWork.requireSelection(this, selection);
        if (!isNativePhase(WorkPhase.COMPLETE) || completion.selectionUsed
                || completion.state != CompletionState.WITNESSED && completion.state != CompletionState.REFUSED)
            throw new IllegalStateException("No selectable native completion");
        completion.selectionUsed = true;
        if (isCurrent() && !isQuarantined()) { updateStats(); selectNext(selection.creditBeforeDigDebit()); }
    }
    public void notifyWorkChanged(BoreWork.Change change) {
        BoreWork.requireNotification(this, change);
        if (!retired && !evacuated && isCurrent()) { updateStats(); changed(); }
    }

    public WorkSnapshot workSnapshot() {
        BoreWork.requireSnapshot(this);
        if (workPhase == null || !isNativePhase(workPhase)) throw new IllegalStateException("No native bore work phase");
        return new WorkSnapshot(workPhase);
    }
    public WorkSnapshot retirementSnapshot() {
        BoreWork.requireRetirement(this); // Exact root native onRemove observation, NOT caller intent.
        return new WorkSnapshot(WorkPhase.RETIRE);
    }
    /** Ephemeral read binding. Private slot objects are retained ONLY for exact
     * matching, never returned. Root must canonicalize copies after opening its
     * frame and recheck after every callback; copy() alone is not a codec freeze. */
    public final class WorkSnapshot {
        private final WorkPhase phase;
        private final UUID life = lifetime, machine = identity;
        private final long rev = revision, seq = completion == null ? sequence : completion.sequence;
        private final CompletionState completionState = completion == null ? null : completion.state;
        private final boolean selectionUsed = completion != null && completion.selectionUsed;
        private final BlockState state = getBlockState();
        private final TileArcaneBoreBase base = installedBase();
        private final boolean essentiaBound = base != null && base == accelerationBase();
        private final BlockState baseState = base == null ? null : base.getBlockState();
        private final UUID baseLife = base == null ? null : base.lifetimeId();
        private final long baseRev = base == null ? -1 : base.revision();
        private final ItemStack focusObject = focus, pickObject = pick, focusBefore = focus.copy(), pickBefore = pick.copy();
        private final ArcaneBoreMath.Rotation turns = rotation;
        private final Target target = completion == null ? pending : completion.target;
        private final Target queued = pending;
        private final ArcaneBoreMath.ScanCursor cursor = scan;
        private final long visualSerial = visualSequence;
        private final int remainingCount = count, creditBits = Float.floatToIntBits(credit);
        private final BoreRepairState repairBefore = repairState;
        private WorkSnapshot(WorkPhase phase) { this.phase = phase; }
        public WorkPhase phase() { return phase; }
        public UUID lifetimeId() { return life; }
        public UUID machineIdentity() { return machine; }
        public long revision() { return rev; }
        public long sequence() { return seq; }
        public boolean sequenceConsumed() { return completionState != null && completionState != CompletionState.READY; }
        public CompletionState completionState() { return completionState; }
        public BlockPos topPos() { return worldPosition; }
        public BlockState topState() { return state; }
        public TileArcaneBoreBase base() { return base; }
        public boolean allowsBaseEssentia() { return essentiaBound; }
        public BlockState baseState() { return baseState; }
        public UUID baseLifetime() { return baseLife; }
        public long baseRevision() { return baseRev; }
        public Direction mining() { return state.getValue(BlockArcaneBore.MINING); }
        public Direction attachment() { return state.getValue(BlockArcaneBore.ATTACHMENT); }
        public Direction nozzle() { return baseState == null ? null : baseState.getValue(thaumcraft.common.blocks.BlockArcaneBoreBase.NOZZLE); }
        public ItemStack focusCopy() { return focusBefore.copy(); }
        public ItemStack pickCopy() { return pickBefore.copy(); }
        public BlockPos targetPos() { return target == null ? null : target.pos(); }
        public BlockState targetState() { return target == null ? null : target.state(); }
        public int countdown() { return remainingCount; }
        public float credit() { return Float.intBitsToFloat(creditBits); }
        public BoreRepairState repairState() { return repairBefore; }
        public ArcaneBoreMath.Rotation rotation() { return turns; }
        public boolean matchesCurrent() {
            if (phase == WorkPhase.RETIRE) {
                if (!retirementMetadataMatches()) return false;
                boolean matches = ItemStack.matches(focus, focusBefore) && ItemStack.matches(pick, pickBefore);
                return matches && retirementMetadataMatches();
            }
            if (!metadataMatches()) return false;
            boolean matches = ItemStack.matches(focus, focusBefore) && ItemStack.matches(pick, pickBefore);
            return matches && metadataMatches();
        }
        private boolean retirementMetadataMatches() {
            // Native onRemove may already expose the REPLACEMENT block state.
            // Only root's exact retirement observation can authorize this case;
            // it is not a public bypass of ordinary installed-owner validation.
            return BoreWork.matchesRetirement(TileArcaneBore.this, this) && life.equals(lifetime) && machine.equals(identity)
                    && rev == revision && focusObject == focus && pickObject == pick
                    && creditBits == Float.floatToIntBits(TileArcaneBore.this.credit);
        }
        private boolean metadataMatches() {
            if (!isCurrent() || !isNativePhase(phase) || !life.equals(lifetime) || !machine.equals(identity) || rev != revision
                    || state != getBlockState() || focusObject != focus || pickObject != pick || count != remainingCount
                    || !turns.equals(rotation) || creditBits != Float.floatToIntBits(TileArcaneBore.this.credit) || repairBefore != repairState
                    || queued != pending || !cursor.equals(scan) || visualSerial != visualSequence
                    || base != installedBase() || essentiaBound != (base != null && base == accelerationBase())
                    || base != null && (baseState != base.getBlockState()
                        || !baseLife.equals(base.lifetimeId()) || baseRev != base.revision())
                    || seq != (completion == null ? TileArcaneBore.this.sequence : completion.sequence)
                    || completionState != (completion == null ? null : completion.state)
                    || selectionUsed != (completion != null && completion.selectionUsed)) return false;
            return true;
        }
    }

    private void clientTick() {
        if (clientFirst) { updateStats(); rotation = ArcaneBoreMath.reorient(rotation, miningDirection(), true); clientDirection = miningDirection(); clientFirst = false; }
        else if (clientDirection != miningDirection()) {
            rotation = ArcaneBoreMath.reorient(rotation, miningDirection(), false); clientDirection = miningDirection();
            visualPause = 100; visualTarget = null; visualPending = false;
            targetAimX = targetAimZ = aimVelocityX = aimVelocityZ = 0;
        }
        rotation = ArcaneBoreMath.stepRotation(rotation);
        if (!rotation.aligned() || !isPowered() || !itemsValid()) {
            if (topRotation % 90 != 0) topRotation += Math.min(10, 90 - topRotation % 90);
            aimX *= .9F; aimZ *= .9F;
            return;
        }
        ++visualPause;
        if (visualPause < visualMaxPause && soundDelay < System.currentTimeMillis()) {
            soundDelay = System.currentTimeMillis() + 1200L + level.random.nextInt(100);
            level.playLocalSound(worldPosition.getX() + .5, worldPosition.getY() + .5, worldPosition.getZ() + .5,
                    RUMBLE, SoundSource.BLOCKS, .25F, .9F + level.random.nextFloat() * .2F, false);
        }
        if (beamLength > 0 && visualPause > visualMaxPause) --beamLength;
        if (visualPending && visualTarget != null && loaded(visualTarget)) {
            float hardness = level.getBlockState(visualTarget).getDestroySpeed(level, visualTarget);
            // Original CLIENT maxPause has an additional10 BEFORE slow*4.
            // SpeedyTime is not in original custom S2C data: keep client0,
            // do not fabricate server acceleration or imply matching timelines.
            visualMaxPause = 10 + Math.max(10 - stats.speed(), (int) (hardness * 2.0F) - stats.speed() * 2);
            if (credit <= 0) visualMaxPause *= 4;
            visualPause = 0; beamLength = 64; visualPending = false;
            double dx = worldPosition.getX() - visualTarget.getX(), dy = worldPosition.getY() - visualTarget.getY(), dz = worldPosition.getZ() - visualTarget.getZ();
            float rx = (float) (Math.atan2(dz, dx) * 180.0 / Math.PI);
            float rz = (float) (-(Math.atan2(dy, (float) Math.sqrt(dx * dx + dz * dz)) * 180.0 / Math.PI)) + 90.0F;
            targetAimX = Mth.wrapDegrees((float) rotation.x()) + rx;
            if (miningDirection() == Direction.EAST) {
                if (targetAimX > 180.0F) targetAimX -= 360.0F;
                if (targetAimX < -180.0F) targetAimX += 360.0F;
            }
            targetAimZ = rz - rotation.z();
            if (miningDirection().getAxis() == Direction.Axis.Y) {
                targetAimZ += 180.0F;
                if (aimX - targetAimX >= 180.0F) aimX -= 360.0F;
                if (aimX - targetAimX <= -180.0F) aimX += 360.0F;
            }
            aimVelocityX = Math.abs((aimX - targetAimX) / 6.0F);
            aimVelocityZ = Math.abs((aimZ - targetAimZ) / 6.0F);
        }
        if (visualPause < visualMaxPause) {
            if (aimX < targetAimX) aimX += aimVelocityX; else if (aimX > targetAimX) aimX -= aimVelocityX;
            if (aimZ < targetAimZ) aimZ += aimVelocityZ; else if (aimZ > targetAimZ) aimZ -= aimVelocityZ;
        } else { aimX *= .9F; aimZ *= .9F; }
        aimVelocityX *= .9F; aimVelocityZ *= .9F;
        topRotation += beamLength / 6;
    }
    @Override public void setRemoved() {
        retired = true;
        try { BoreWork.retired(this); } finally { super.setRemoved(); }
    }
    @Override protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        if (clientOnlyPayload && (level == null || !level.isClientSide)) throw new IllegalStateException("Client bore view is not a disk owner");
        beginSerialization();
        try {
            super.saveAdditional(tag, registries); writeCustom(tag, registries);
            tag.putShort("SpeedyTime", ArcaneBoreMath.persistedCredit(credit));
            tag.putInt("BoreSchema", SCHEMA); tag.putUUID("BoreIdentity", identity); tag.putLong("BoreRevision", revision);
            if (workReference != null) tag.put("BoreWorkReference", workReference.copy());
            if (fenced) tag.putBoolean("BoreFenced", true);
            if (rejected != null) tag.put("RejectedBore", rejected.copy());
        } finally { serializing = false; }
    }
    private void beginSerialization() {
        if (serializing || level instanceof ServerLevel server && !server.getServer().isSameThread())
            throw new IllegalStateException("Recursive/off-thread bore serialization");
        serializing = true;
    }
    private void writeCustom(CompoundTag tag, HolderLookup.Provider registries) {
        UUID life = lifetime; long rev = revision;
        ItemStack beforeFocus = focus, beforePick = pick;
        BlockState state = getBlockState(); boolean wasRetired = retired;
        tag.putInt("orientation", miningDirection().get3DDataValue());
        tag.putInt("baseOrientation", attachment().get3DDataValue());
        ListTag inventory = new ListTag();
        for (int slot = 0; slot < 2; slot++) {
            ItemStack stack = slot == 0 ? beforeFocus : beforePick;
            if (stack.isEmpty()) continue;
            CompoundTag row = new CompoundTag(); row.putByte("Slot", (byte) slot); row.put("Stack", stack.save(registries)); inventory.add(row);
        }
        if (!life.equals(lifetime) || rev != revision || state != getBlockState() || beforeFocus != focus || beforePick != pick || wasRetired != retired)
            throw new IllegalStateException("Bore changed during item serialization; no mixed owner snapshot may be saved");
        tag.put("Inventory", inventory);
    }
    @Override protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        if (BoreWork.isMutationActive() || EssentiaTransfers.isMutationActive() || ticking || mutating || serializing || retired)
            throw new IllegalStateException("Bore load cannot restore an attached OR detached owner during mutation/retirement");
        if (level instanceof ServerLevel server && (!server.getServer().isSameThread() || BoreWork.isLocked(this)))
            throw new IllegalStateException("Bore load requires an idle server owner");
        mutating = true;
        try {
            super.loadAdditional(tag, registries);
            // Initial chunk custom data may be decoded before a client Level is
            // attached. Such a view is NEVER allowed to become a server/disk owner.
            if (level != null && level.isClientSide || level == null && tag.contains("BoreVisual")) {
                clientOnlyPayload = true; readClient(tag, registries); return;
            }
            readDisk(tag, registries);
        } finally { mutating = false; }
    }
    private void readDisk(CompoundTag tag, HolderLookup.Provider registries) {
        clientOnlyPayload = false;
        lifetime = UUID.randomUUID(); identity = UUID.randomUUID(); revision = sequence = visualSequence = 0;
        focus = pick = ItemStack.EMPTY; credit = 0; count = 0; pending = null; completion = null; workPhase = null;
        repairState = BoreRepairState.EMPTY; // Original counter, cost and accumulated repair vis are transient, not NBT.
        boundBase = null; boundBaseLifetime = null; scan = ArcaneBoreMath.ScanCursor.initial();
        rotation = ArcaneBoreMath.reorient(new ArcaneBoreMath.Rotation(0, 0, 0, 0, 0, 0), miningDirection(), true);
        rotationDirection = miningDirection();
        rejected = workReference = null; fenced = needsReconciliation = evacuated = false;
        if (tag.contains("RejectedBore")) {
            rejected = tag.contains("RejectedBore", Tag.TAG_COMPOUND) ? tag.getCompound("RejectedBore").copy() : tag.copy();
            stats = new ArcaneBoreMath.Stats(0, 0, 0); fenced = true; return;
        }
        try {
            require(tag, "BoreSchema", Tag.TAG_INT); require(tag, "BoreRevision", Tag.TAG_LONG);
            if (tag.getInt("BoreSchema") != SCHEMA || !tag.hasUUID("BoreIdentity") || tag.getLong("BoreRevision") < 0
                    || tag.getLong("BoreRevision") == Long.MAX_VALUE) throw new IllegalArgumentException("Invalid bore schema/identity/revision");
            identity = tag.getUUID("BoreIdentity"); revision = tag.getLong("BoreRevision");
            Direction mining = direction(tag, "orientation"), attached = direction(tag, "baseOrientation");
            if (attached != Direction.UP && attached != Direction.DOWN || mining != miningDirection() || attached != attachment())
                throw new IllegalArgumentException("Bore direction/state mismatch");
            require(tag, "SpeedyTime", Tag.TAG_SHORT);
            credit = ArcaneBoreMath.loadedCredit(tag.getShort("SpeedyTime"));
            // Float rounding can produce40F from a value just below20F +20F;
            // do not replace original floating arithmetic with a <40 clamp.
            if (credit < 0 || credit > 40) throw new IllegalArgumentException("Invalid saved bore acceleration credit");
            ItemStack[] loaded = readInventory(tag, registries); focus = loaded[0]; pick = loaded[1];
            if (tag.contains("BoreWorkReference")) {
                require(tag, "BoreWorkReference", Tag.TAG_COMPOUND); workReference = tag.getCompound("BoreWorkReference").copy();
                needsReconciliation = fenced = true; // No inference of payment/witness from air, tool wear or a saved phase.
            }
            if (tag.contains("BoreFenced")) { require(tag, "BoreFenced", Tag.TAG_BYTE); fenced |= tag.getBoolean("BoreFenced"); }
            updateStats();
        } catch (RuntimeException malformed) {
            // Preserve WHOLE payload, including any unread item/reference. Root
            // retirement must custody that payload before this BE can disappear.
            rejected = tag.copy(); fenced = true; pending = null;
            // ONE quarantine owner, not decoded slots PLUS duplicate raw rows.
            focus = pick = ItemStack.EMPTY; stats = new ArcaneBoreMath.Stats(0, 0, 0);
            workReference = null; needsReconciliation = false;
            Thaumcraft.LOG.warn("Quarantined malformed bore at {}", worldPosition, malformed);
        }
    }
    private static ItemStack[] readInventory(CompoundTag tag, HolderLookup.Provider registries) {
        require(tag, "Inventory", Tag.TAG_LIST);
        ListTag raw = (ListTag) tag.get("Inventory"), rows = tag.getList("Inventory", Tag.TAG_COMPOUND);
        if (raw.size() != rows.size() || rows.size() > 2) throw new IllegalArgumentException("Invalid bore inventory rows");
        ItemStack[] items = {ItemStack.EMPTY, ItemStack.EMPTY}; boolean[] seen = new boolean[2];
        for (Tag entry : rows) {
            CompoundTag row = (CompoundTag) entry; require(row, "Slot", Tag.TAG_BYTE); require(row, "Stack", Tag.TAG_COMPOUND);
            int slot = row.getByte("Slot");
            if (slot < 0 || slot > 1 || seen[slot]) throw new IllegalArgumentException("Invalid/duplicate bore slot");
            seen[slot] = true;
            CompoundTag encoded = row.getCompound("Stack");
            ItemStack item = ItemStack.parse(registries, encoded).orElseThrow(() -> new IllegalArgumentException("Invalid bore stack"));
            if (!validSlot(slot, item) || !encoded.equals(item.save(registries))) throw new IllegalArgumentException("Discarded/unsupported bore item components");
            items[slot] = item;
        }
        return items;
    }
    private static Direction direction(CompoundTag tag, String key) {
        require(tag, key, Tag.TAG_INT); int value = tag.getInt(key);
        if (value < 0 || value > 5) throw new IllegalArgumentException("Invalid bore direction");
        return Direction.from3DDataValue(value);
    }
    private static void require(CompoundTag tag, String key, int type) {
        if (!tag.contains(key, type)) throw new IllegalArgumentException("Missing/wrong bore field " + key);
    }
    private void readClient(CompoundTag tag, HolderLookup.Provider registries) {
        try {
            ItemStack[] items = readInventory(tag, registries); focus = items[0]; pick = items[1]; updateStats();
            // BlockState packet owns direction. Never load a client packet's
            // SpeedyTime/journal into authoritative server state.
            if (tag.contains("BoreVisual", Tag.TAG_COMPOUND)) {
                CompoundTag visual = tag.getCompound("BoreVisual");
                long serial = visual.getLong("Sequence");
                if (serial != visualSequence) {
                    visualSequence = serial; visualTarget = null;
                    if (visual.contains("X", Tag.TAG_INT) && visual.contains("Y", Tag.TAG_INT) && visual.contains("Z", Tag.TAG_INT)) {
                        BlockPos target = new BlockPos(visual.getInt("X"), visual.getInt("Y"), visual.getInt("Z"));
                        if (Math.abs((long) target.getX() - worldPosition.getX()) <= 70
                                && Math.abs((long) target.getY() - worldPosition.getY()) <= 70
                                && Math.abs((long) target.getZ() - worldPosition.getZ()) <= 70) {
                            visualTarget = target; visualPending = true;
                        }
                    }
                }
            }
        } catch (RuntimeException malformed) {
            focus = pick = ItemStack.EMPTY; stats = new ArcaneBoreMath.Stats(0, 0, 0);
            visualTarget = null; visualPending = false;
        }
    }
    @Override public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        beginSerialization();
        try {
            CompoundTag tag = new CompoundTag(); writeCustom(tag, registries); // Original custom packet excludes disk SpeedyTime.
            CompoundTag visual = new CompoundTag(); visual.putLong("Sequence", visualSequence);
            if (pending != null) {
                visual.putInt("X", pending.pos().getX()); visual.putInt("Y", pending.pos().getY()); visual.putInt("Z", pending.pos().getZ());
            }
            tag.put("BoreVisual", visual); // S2C-only visual adaptation, NEVER persisted by saveAdditional.
            return tag;
        } finally { serializing = false; }
    }
    @Override public ClientboundBlockEntityDataPacket getUpdatePacket() { return ClientboundBlockEntityDataPacket.create(this); }
}
