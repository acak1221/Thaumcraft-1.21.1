package thaumcraft.common.lib.bore;

import dev.architectury.event.events.common.BlockEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.PickaxeItem;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.api.aspects.IEssentiaTransport;
import thaumcraft.api.visnet.VisNetHandler;
import thaumcraft.common.Thaumcraft;
import thaumcraft.common.items.equipment.ItemThaumiumPickaxe;
import thaumcraft.common.lib.essentia.BoreInventoryPlan;
import thaumcraft.common.lib.essentia.EssentiaAtomicEndpoint;
import thaumcraft.common.lib.essentia.EssentiaTransfers;
import thaumcraft.common.lib.essentia.EssentiaTransfers.BoreOperation;
import thaumcraft.common.lib.essentia.EssentiaTransfers.BoreOwnership;
import thaumcraft.common.lib.utils.DeferredItemDrops;
import thaumcraft.common.tiles.TileArcaneBore;
import thaumcraft.common.tiles.TileJar;
import thaumcraft.common.tiles.TileTube;
import thaumcraft.common.tiles.TileTubeBuffer;

/** Closed native machine work. All write authority belongs to the existing
 * EssentiaTransfers executor; there is no second thread-local transaction loop.
 * First slice deliberately ejects: native inventory routing/BE/vacuum parity is
 * pending. Native vanilla loot includes modern raw ores, not TC4 ore-block loot. */
public final class BoreWork {
    private static final Set<Block> ORDINARY = Set.of(Blocks.STONE, Blocks.COBBLESTONE, Blocks.DIRT,
            Blocks.COAL_ORE, Blocks.DEEPSLATE_COAL_ORE, Blocks.IRON_ORE, Blocks.DEEPSLATE_IRON_ORE,
            Blocks.DIAMOND_ORE, Blocks.DEEPSLATE_DIAMOND_ORE, Blocks.GLASS,
            Blocks.OAK_LOG, Blocks.SPRUCE_LOG, Blocks.BIRCH_LOG);
    private static final List<Direction> DRAW_ORDER = List.of(Direction.DOWN, Direction.UP, Direction.NORTH,
            Direction.SOUTH, Direction.WEST, Direction.EAST);
    private BoreWork() {}

    public static void prefill(TileArcaneBore top) { EssentiaTransfers.workBore(top, BoreOperation.PREFILL); }
    public static void finish(TileArcaneBore top) { EssentiaTransfers.workBore(top, BoreOperation.FINISH); }
    public static void evacuate(TileArcaneBore top) { if (!top.wasEvacuated()) EssentiaTransfers.workBore(top, BoreOperation.EVACUATE); }
    public static void reconcileLoaded(TileArcaneBore top) { EssentiaTransfers.workBore(top, BoreOperation.RECONCILE); }
    public static boolean isMutationActive() { return EssentiaTransfers.isMutationActive(); }
    public static boolean isLocked(BlockEntity owner) { return EssentiaTransfers.isLocked(owner); }
    public static boolean isCurrentServerOwner(BlockEntity owner) { return BoreNativeContext.isCurrentOwner(owner); }
    public static boolean nativeTickAllowed(TileArcaneBore top) { return BoreNativeContext.isNativeTick(top); }
    public static void retired(BlockEntity owner) { BoreNativeContext.retired(owner); }

    public static void selected(TileArcaneBore top) {
        if (!BoreNativeContext.isNativeTick(top) || !BoreNativeContext.isCurrentOwner(top) || !top.isSelecting())
            throw new IllegalStateException("Bore selection FX requires its native selector");
        ServerLevel level = (ServerLevel) top.getLevel();
        level.sendBlockUpdated(top.getBlockPos(), top.getBlockState(), top.getBlockState(), Block.UPDATE_CLIENTS);
    }

    public static final class Change {
        public enum Kind { PREFILL, ARM, REFUSE, WITNESS, UNKNOWN, EVACUATE, RECONCILE }
        private final Frame frame;
        private final Kind kind;
        private final long revision;
        private final ItemStack pick;
        private final float credit;
        private final CompoundTag reference;
        private final boolean fenced, applyTop;
        private final DeferredItemDrops.BoreEscrow escrow;
        private final EssentiaAtomicEndpoint.Change debit;
        private boolean used, applied, notified;
        private Change(Frame frame, Kind kind, ItemStack pick, float credit, CompoundTag reference,
                       boolean fenced, boolean applyTop, DeferredItemDrops.BoreEscrow escrow, EssentiaAtomicEndpoint.Change debit) {
            this.frame = frame; this.kind = kind; revision = frame.top.revision(); this.pick = pick;
            this.credit = credit; this.reference = reference; this.fenced = fenced; this.applyTop = applyTop;
            this.escrow = escrow; this.debit = debit;
        }
        public Kind kind() { return kind; }
        public long expectedRevision() { return revision; }
        public ItemStack preparedPick() { return pick; }
        public float effectiveCredit() { return credit; }
        public CompoundTag preparedReference() { return reference; }
        public boolean fenced() { return fenced; }
        public DeferredItemDrops.BoreEscrow escrow() { return escrow; }
        public EssentiaAtomicEndpoint.Change sourceDebit() { return debit; }
        public BoreOwnership ownership() {
            return switch (kind) {
                case ARM -> BoreOwnership.ARM; case WITNESS -> BoreOwnership.WITNESS;
                case UNKNOWN -> BoreOwnership.UNKNOWN; case EVACUATE -> BoreOwnership.EVACUATE; default -> null;
            };
        }
    }
    public static final class Selection {
        private final Frame frame;
        private final long revision;
        private final float credit;
        private boolean used;
        private Selection(Frame frame) { this.frame = frame; revision = frame.top.revision(); credit = frame.initialCredit; }
        public float creditBeforeDigDebit() { return credit; }
    }

    /** One-shot opaque-retention token, not a tool/output getter or a normal
     * Change. Only the existing executor can run its raw dying-owner sink. */
    public static final class RetirementRetention {
        private final Frame frame;
        private final DeferredItemDrops.BoreRetirementCustody custody;
        private final UUID life, machine;
        private final long revision;
        private boolean used, captured, applied;
        private RetirementRetention(Frame frame, DeferredItemDrops.BoreRetirementCustody custody) {
            this.frame = frame; this.custody = custody;
            life = frame.top.lifetimeId(); machine = frame.top.machineIdentity(); revision = frame.top.revision();
        }
        public DeferredItemDrops.BoreRetirementCustody custody() { return custody; }
    }

    /** Only reachable during the executor's exact ephemeral scope. No static
     * strong level/owner/actor references survive finally. */
    public static final class Frame {
        private final TileArcaneBore top;
        private final EssentiaTransfers.BoreScope scope;
        private final ServerLevel level;
        private TileArcaneBore.WorkSnapshot before;
        private BoreNativeContext.Watch watch;
        private BoreNativeContext.Watch outputWatch;
        private ItemStack focus, pick;
        private UUID life, machine;
        private BlockEntity baseIdentity;
        private BlockState baseState;
        private UUID baseLife;
        private long baseRevision;
        private long expectedRevision;
        private float initialCredit;
        private Change prepared, last;
        private Selection selection;
        private Output output;
        private DeferredItemDrops.BoreEscrow escrow;
        private RetirementRetention retention;
        private CompoundTag witnessedReference, unknownReference;
        private BlockEntity drawingSource;
        private boolean closed, armed, entered, witnessed;
        private Frame(TileArcaneBore top, EssentiaTransfers.BoreScope scope) {
            this.top = top; this.scope = scope; level = (ServerLevel) top.getLevel();
        }
        public TileArcaneBore top() { return top; }
        public EssentiaTransfers.BoreScope scope() { return scope; }
        private boolean owned() {
            return !closed && BoreNativeContext.isCurrentOwner(top) && top.lifetimeId().equals(life)
                    && top.machineIdentity().equals(machine) && top.revision() == expectedRevision && !top.isSerializing();
        }
        private boolean current() {
            return owned() && baseUnchanged() && (watch == null || watch.valid()) && (outputWatch == null || outputWatch.valid()) && before.matchesCurrent()
                    && ItemStack.matches(focus, top.getItem(0)) && ItemStack.matches(pick, top.getItem(1));
        }
        private boolean baseUnchanged() {
            return before.base() == baseIdentity && (before.base() == null || before.base().getBlockState() == baseState
                    && before.base().lifetimeId().equals(baseLife) && before.base().revision() == baseRevision);
        }
        private void closeWatch() {
            if (outputWatch != null) { outputWatch.close(); outputWatch = null; }
            if (watch != null) { watch.close(); watch = null; }
        }
        private void refresh(Change change) {
            expectedRevision = change.revision + 1;
            if (change.kind == Change.Kind.ARM) pick = change.pick;
            // ARM -> native delegate must have NO callback-bearing snapshot or
            // notification window. Rebind metadata only after an outcome exists.
            if (change.kind != Change.Kind.ARM && owned() && scope.operation() != BoreOperation.EVACUATE) before = top.workSnapshot();
        }
        private Change change(Change.Kind kind, ItemStack paidPick, float credit, CompoundTag reference,
                              boolean fenced, boolean applyTop, EssentiaAtomicEndpoint.Change debit) {
            return new Change(this, kind, paidPick, credit, reference, fenced, applyTop, escrow, debit);
        }
        private void commit(Change change, boolean notify) {
            prepared = change;
            RuntimeException failure = EssentiaTransfers.commitBore(this, change);
            last = change;
            if (change.applied && change.applyTop) refresh(change);
            if (notify && change.applied && change.applyTop && owned()) {
                try { top.notifyWorkChanged(change); }
                catch (RuntimeException notice) { Thaumcraft.LOG.warn("Bore committed notification failed at {}", top.getBlockPos(), notice); }
            }
            if (failure != null) Thaumcraft.LOG.warn("Bore committed source notification failed at {}", top.getBlockPos(), failure);
        }
        private void select() {
            if (!owned() || !baseUnchanged() || top.isQuarantined() || before.base() == null || !before.matchesCurrent()) return;
            selection = new Selection(this);
            top.selectAfterAttempt(selection);
        }
        private void refuse() {
            if (armed || last != null && (last.kind == Change.Kind.REFUSE || last.kind == Change.Kind.WITNESS)) return;
            closeWatch();
            if (!owned() || !top.isNativePhase(TileArcaneBore.WorkPhase.COMPLETE)) return;
            commit(change(Change.Kind.REFUSE, null, initialCredit, null, false, true, null), false);
            select();
        }
    }

    /** Prepared candidate envelope, NOT a completed-output capability. Only the
     * queue may claim this exact one-shot object before codec callbacks. */
    public static final class Output {
        private final Frame frame;
        private final UUID transaction = UUID.randomUUID();
        private final List<BoreInventoryPlan.Stack> bundle;
        private final CompoundTag evidence;
        private final boolean evacuation;
        private boolean claimed;
        private Output(Frame frame, List<BoreInventoryPlan.Stack> bundle, ItemStack worn, float credit, boolean evacuation) {
            this.frame = frame; this.bundle = List.copyOf(bundle); this.evacuation = evacuation;
            var s = frame.before; evidence = new CompoundTag();
            evidence.putInt("BoreJournalVersion", 1); evidence.putUUID("Id", transaction);
            evidence.putUUID("Machine", s.machineIdentity()); evidence.putUUID("Lifetime", s.lifetimeId());
            evidence.putLong("Source", s.topPos().asLong()); evidence.putLong("Revision", s.revision());
            evidence.putLong("Sequence", s.sequence()); evidence.putInt("CreditBits", Float.floatToIntBits(credit));
            evidence.putString("Mode", evacuation ? "EVACUATE" : "HARVEST");
            if (!evacuation) {
                evidence.putLong("Target", s.targetPos().asLong()); evidence.put("OldState", NbtUtils.writeBlockState(s.targetState()));
                evidence.putInt("Nozzle", s.nozzle().get3DDataValue()); evidence.putInt("Attachment", s.attachment().get3DDataValue());
                evidence.put("PickBefore", frame.pick.save(frame.level.registryAccess()));
                evidence.put("PickAfter", worn.save(frame.level.registryAccess()));
            } else if (frame.top.rejectedPayload() != null) evidence.put("RejectedOwner", frame.top.rejectedPayload());
        }
        public void claim() {
            EssentiaTransfers.requireBoreFrame(frame);
            if (claimed || frame.output != this || frame.closed) throw new IllegalStateException("Bore candidate already claimed");
            claimed = true;
        }
        public ServerLevel level() { return frame.level; }
        public List<BoreInventoryPlan.Stack> bundle() { return bundle; }
        public CompoundTag evidence() { return evidence.copy(); }
        public boolean evacuation() { return evacuation; }
    }

    public static void run(TileArcaneBore top, EssentiaTransfers.BoreScope scope) {
        EssentiaTransfers.requireBoreScope(scope, top);
        Frame frame = new Frame(top, scope);
        EssentiaTransfers.bindBoreFrame(scope, top, frame);
        try {
            if (scope.operation() == BoreOperation.EVACUATE) {
                BoreNativeContext.requireRetirement(top);
                if (top.wasEvacuated()) return;
                frame.before = top.retirementSnapshot();
            } else {
                var cells = new ArrayList<BlockPos>(); cells.add(top.getBlockPos());
                BlockPos base = ArcaneBoreMath.basePosition(top.getBlockPos(), top.attachment());
                if (frame.level.isInWorldBounds(base) && frame.level.hasChunkAt(base)) cells.add(base);
                // An old/unloaded pending target must not gate unconditional CV
                // prefill or loaded-reference reconciliation.
                if (scope.operation() == BoreOperation.FINISH && top.selectedTarget() != null
                        && frame.level.hasChunkAt(top.selectedTarget())) cells.add(top.selectedTarget());
                frame.watch = BoreNativeContext.watch(frame.level, List.copyOf(cells));
                frame.before = top.workSnapshot();
            }
            frame.life = frame.before.lifetimeId(); frame.machine = frame.before.machineIdentity();
            frame.expectedRevision = frame.before.revision(); frame.initialCredit = frame.before.credit();
            frame.baseIdentity = frame.before.base(); frame.baseState = frame.before.baseState();
            frame.baseLife = frame.before.baseLifetime(); frame.baseRevision = frame.before.baseRevision();
            if (scope.operation() == BoreOperation.FINISH && frame.before.base() != null) {
                BlockPos out = outputCell(frame.before);
                if (frame.level.hasChunkAt(out)) frame.outputWatch = BoreNativeContext.watch(frame.level, List.of(out));
            }
            frame.focus = canonical(frame.level, frame.before.focusCopy()); frame.pick = canonical(frame.level, frame.before.pickCopy());
            if (frame.before.base() != null) EssentiaTransfers.lockBoreActor(frame, frame.before.base());
            switch (scope.operation()) {
                case PREFILL -> prefillBound(frame); case FINISH -> finishBound(frame);
                case EVACUATE -> evacuateBound(frame); case RECONCILE -> reconcileBound(frame);
            }
        } catch (RuntimeException | Error failure) {
            if (scope.operation() == BoreOperation.EVACUATE) {
                // Also covers failure before the initial canonical capture. Do
                // not let native onRemove delete an unclaimed physical owner.
                try { retainRetirement(frame, failure.getClass().getName()); }
                catch (RuntimeException | Error retentionFailure) {
                    if (retentionFailure != failure) failure.addSuppressed(retentionFailure);
                    throw failure;
                }
                if (failure instanceof Error) throw failure;
                Thaumcraft.LOG.warn("Bore retirement retained as unsendable custody at {}", top.getBlockPos(), failure);
                return;
            } else if (frame.armed && !frame.witnessed && frame.escrow != null) unknown(frame);
            else if (!frame.entered && scope.operation() == BoreOperation.FINISH) {
                try { frame.refuse(); } catch (RuntimeException refused) { failure.addSuppressed(refused); }
            }
            throw failure;
        } finally {
            try {
                if (frame.escrow != null) {
                    if (frame.witnessed) EssentiaTransfers.routeBoreOutput(frame, frame.escrow);
                    frame.escrow.finishFrame();
                }
            } finally { try { frame.closeWatch(); } finally { frame.closed = true; } }
        }
    }

    private static void prefillBound(Frame f) {
        if (!f.current() || f.before.phase() != TileArcaneBore.WorkPhase.PREFILL || f.before.credit() >= 20) return;
        int actual = VisNetHandler.drainVis(f.level, f.before.topPos(), Aspect.ENTROPY, 100);
        if (actual < 0 || actual > 100) throw new IllegalStateException("Invalid Bore raw VisNet grant");
        if (!f.current()) return; // Already committed raw source vis is NEVER refunded/drained again.
        if (actual > 0) f.commit(f.change(Change.Kind.PREFILL, null, f.before.credit() + actual / 5.0F, null, false, true, null), true);
        if (!f.current() || f.before.credit() >= 20 || !f.before.allowsBaseEssentia()) return;
        for (Direction face : DRAW_ORDER) {
            BlockPos pos = f.before.base().getBlockPos().relative(face);
            if (!f.level.isInWorldBounds(pos) || !f.level.hasChunkAt(pos)) continue;
            BlockEntity source = f.level.getBlockEntity(pos);
            if (!(source instanceof IEssentiaTransport transport)) continue;
            f.drawingSource = source;
            EssentiaTransfers.lockBoreActor(f, source);
            try (var sourceWatch = BoreNativeContext.watch(f.level, List.of(pos))) {
                BlockState state = source.getBlockState();
                EssentiaAtomicEndpoint endpoint = nativeSource(source);
                var stock = endpoint == null ? null : endpoint.snapshotAtomicState();
                boolean connected = transport.isConnectable(face.getOpposite());
                if (!f.current() || !sourceWatch.valid()) return;
                if (!connected) continue;
                boolean output = transport.canOutputTo(face.getOpposite());
                if (!f.current() || !sourceWatch.valid()) return;
                if (!output) return; // Original first connectable non-output aborts ALL six.
                int suction = transport.getSuctionAmount(face.getOpposite());
                if (!f.current() || !sourceWatch.valid()) return;
                if (suction >= (face == f.before.nozzle() ? 0 : 128) || endpoint == null) continue;
                if (!endpoint.preflightTake(Aspect.ENTROPY, face.getOpposite())) continue;
                var debit = endpoint.prepareExtract(stock, Aspect.ENTROPY, 1);
                if (debit == null) continue;
                if (!f.current() || !sourceWatch.valid() || source.isRemoved() || source.getLevel() != f.level
                        || !f.level.hasChunkAt(pos) || f.level.getBlockEntity(pos) != source || f.level.getBlockState(pos) != state
                        || !endpoint.matchesAtomicState(stock)) return;
                f.commit(f.change(Change.Kind.PREFILL, null, f.before.credit() + 20.0F, null, false, true, debit), true);
                return;
            }
        }
    }
    private static EssentiaAtomicEndpoint nativeSource(BlockEntity source) {
        return source.getClass() == TileJar.class || source.getClass() == TileTube.class || source.getClass() == TileTubeBuffer.class
                ? (EssentiaAtomicEndpoint) source : null;
    }

    private static void finishBound(Frame f) {
        if (!harvestable(f)) { f.refuse(); return; }
        var platform = BorePlatform.required();
        ServerPlayer actor = platform.actor(f.level);
        ActorLease lease = new ActorLease(f.level, actor, platform);
        if (!harvestable(f) || !lease.valid() || !f.level.mayInteract(actor, f.before.targetPos())) { f.refuse(); return; }
        if (BlockEvent.BREAK.invoker().breakBlock(f.level, f.before.targetPos(), f.before.targetState(), actor, null).isFalse()) { f.refuse(); return; }
        if (!harvestable(f) || !lease.valid()) { f.refuse(); return; }
        boolean allowed = platform.before(f.level, actor, f.before.targetPos(), f.before.targetState());
        if (!allowed) {
            try { platform.canceled(f.level, actor, f.before.targetPos(), f.before.targetState()); }
            finally { f.refuse(); }
            return;
        }
        if (!harvestable(f) || !lease.valid()) { f.refuse(); return; }
        ItemStack lootTool = canonical(f.level, f.pick);
        var registry = f.level.registryAccess().lookupOrThrow(Registries.ENCHANTMENT);
        int silk = EnchantmentHelper.getItemEnchantmentLevel(registry.getOrThrow(Enchantments.SILK_TOUCH), f.pick);
        int fortune = Math.max(f.top.stats().fortune(), EnchantmentHelper.getItemEnchantmentLevel(registry.getOrThrow(Enchantments.FORTUNE), f.pick));
        lootTool.set(DataComponents.ENCHANTMENTS, ItemEnchantments.EMPTY); lootTool.remove(DataComponents.STORED_ENCHANTMENTS);
        if (silk > 0) lootTool.enchant(registry.getOrThrow(Enchantments.SILK_TOUCH), silk);
        else if (fortune > 0) lootTool.enchant(registry.getOrThrow(Enchantments.FORTUNE), fortune);
        if (!harvestable(f) || !lease.valid()) { f.refuse(); return; }
        List<ItemStack> loot = Block.getDrops(f.before.targetState(), f.level, f.before.targetPos(), null, actor, lootTool);
        if (loot != null && loot.size() > DeferredItemDrops.MAX_BORE_EJECTION_SEGMENTS) { f.refuse(); return; }
        var bundle = freezeBundle(f.level, loot);
        if (!harvestable(f) || !lease.valid()) { f.refuse(); return; }
        // Native entity persistence is bounded99, regardless of the queue's
        // separate full Count. Refuse an excessive segment budget BEFORE even
        // wear preparation; ordinary representable mass is never truncated.
        long segments = 0;
        for (var value : bundle) segments = Math.addExact(segments, ((long) value.count() + 63) / 64);
        if (segments > DeferredItemDrops.MAX_BORE_EJECTION_SEGMENTS) { f.refuse(); return; }
        ItemStack worn = canonical(f.level, f.pick);
        boolean[] broken = {false};
        worn.hurtAndBreak(1, f.level, null, item -> broken[0] = true);
        if (broken[0] || worn.isEmpty() || worn.getCount() != 1 || worn.getDamageValue() >= worn.getMaxDamage()) { f.refuse(); return; }
        worn = canonical(f.level, worn);
        float paidCredit = f.initialCredit > 0 ? f.initialCredit - 1.0F : f.initialCredit;
        f.output = new Output(f, bundle, worn, paidCredit, false);
        f.escrow = DeferredItemDrops.prepareBore(f.output);
        CompoundTag reference = f.escrow.reference("ARMED");
        f.witnessedReference = f.escrow.reference("WITNESSED");
        f.unknownReference = f.escrow.reference("UNKNOWN");
        Change arm = f.change(Change.Kind.ARM, worn, paidCredit, reference, false, true, null);
        if (!harvestable(f) || !lease.valid()) { f.refuse(); return; }
        f.closeWatch(); // Last preflight action; root close must not call out.
        f.armed = true;
        f.commit(arm, false);
        f.entered = true;
        final boolean removed;
        try { removed = f.level.removeBlock(f.before.targetPos(), false); } // ONLY destructive delegate, exactly once.
        catch (RuntimeException failure) {
            // No trustworthy outcome, even if a callback already changed the
            // target. Fence the paid reservation; never retry/refund/emit it.
            unknown(f);
            Thaumcraft.LOG.error("Bore removal threw at {}; UNKNOWN reservation retained without output", f.before.targetPos(), failure);
            return;
        }
        if (!removed) { unknown(f); return; }
        f.witnessed = true;
        Change witness = f.change(Change.Kind.WITNESS, null, paidCredit, f.witnessedReference, false, f.owned(), null);
        f.commit(witness, false); // Durable true witness BEFORE AFTER/selection/notifications.
        try {
            // No renewed harvest privilege is granted by this observation. The
            // bridge still verifies native actor/profile/level; changed hands
            // AFTER the true delegate must not hide a completed removal.
            if (BoreNativeContext.isLiveLevel(f.level)) {
                thaumcraft.common.lib.ArcaneLampLighting.afterBoreDig(f.top, f.before.base());
                try { platform.after(f.level, actor, f.before.targetPos(), f.before.targetState()); }
                catch (RuntimeException notice) {
                    // The true native witness is already durable. An observer
                    // failure cannot revoke ownership or crash away settlement.
                    Thaumcraft.LOG.warn("Bore AFTER notification failed after witnessed removal at {}", f.before.targetPos(), notice);
                }
            }
        } finally {
            try { f.select(); }
            finally { if (witness.applyTop && f.owned()) f.top.notifyWorkChanged(witness); }
        }
    }
    private static boolean harvestable(Frame f) {
        if (!f.current() || f.before.phase() != TileArcaneBore.WorkPhase.COMPLETE
                || f.before.completionState() != TileArcaneBore.CompletionState.READY || f.before.sequenceConsumed()
                || f.before.base() == null || f.before.targetPos() == null || f.before.targetState() == null
                || f.before.countdown() > 0 || !f.before.rotation().aligned() || f.top.isQuarantined() || !f.top.isPowered()) return false;
        BlockPos pos = f.before.targetPos(); BlockState state = f.before.targetState();
        if (!ORDINARY.contains(state.getBlock()) || state.hasBlockEntity() || !f.level.isInWorldBounds(pos)
                || !f.level.getWorldBorder().isWithinBounds(pos) || !f.level.hasChunkAt(pos)
                || f.level.getBlockState(pos) != state || f.level.getBlockEntity(pos) != null) return false;
        if (!TileArcaneBore.validSlot(0, f.focus) || !TileArcaneBore.validSlot(1, f.pick)
                || f.pick.getItem().getClass() != PickaxeItem.class && f.pick.getItem().getClass() != ItemThaumiumPickaxe.class
                || !f.pick.isDamageableItem() || (long) f.pick.getDamageValue() + 1 >= f.pick.getMaxDamage()) return false;
        BlockPos low = pos.offset(-1, -1, -1), high = pos.offset(2, 2, 2);
        if (!f.level.isInWorldBounds(low) || !f.level.isInWorldBounds(high) || !f.level.hasChunksAt(low, high)
                || !f.level.hasChunkAt(outputCell(f.before))) return false;
        return f.level.getEntitiesOfClass(ItemEntity.class, new AABB(pos).inflate(1)).isEmpty() && f.current();
    }
    private static BlockPos outputCell(TileArcaneBore.WorkSnapshot before) {
        Direction out = before.nozzle();
        return BlockPos.containing(before.topPos().getX() + .5 + out.getStepX() * .66,
                before.topPos().getY() + .4 + before.attachment().getOpposite().getStepY(),
                before.topPos().getZ() + .5 + out.getStepZ() * .66);
    }
    private static void unknown(Frame f) {
        if (f.escrow == null || f.escrow.isUnknown()) return;
        f.commit(f.change(Change.Kind.UNKNOWN, null, 0, f.unknownReference, true, f.owned(), null), false);
        Thaumcraft.LOG.error("Bore native removal indeterminate at {}; reservation retained, candidate quarantined, no output/refund/retry", f.top.getBlockPos());
    }
    private static void evacuateBound(Frame f) {
        if (f.top.wasEvacuated()) return;
        // The physical EVACUATE raw path advances revision. At MAX it would
        // publish Pending custody before the tile refuses that increment.
        // Retain the sole actual stock opaquely BEFORE any physical publication.
        if (f.top.revision() == Long.MAX_VALUE) { retainRetirement(f, "retirement_revision_exhausted"); return; }
        if (!f.before.matchesCurrent()) { retainRetirement(f, "retirement_binding_changed"); return; }
        var bundle = new ArrayList<ItemStack>();
        if (!f.focus.isEmpty()) bundle.add(f.focus); if (!f.pick.isEmpty()) bundle.add(f.pick);
        if (f.top.rejectedPayload() != null && !bundle.isEmpty()) throw new IllegalStateException("Rejected Bore has duplicate decoded stock");
        f.output = new Output(f, freezeBundle(f.level, bundle), null, f.before.credit(), true);
        f.escrow = DeferredItemDrops.prepareBore(f.output);
        if (!f.before.matchesCurrent() || !ItemStack.matches(f.focus, f.top.getItem(0)) || !ItemStack.matches(f.pick, f.top.getItem(1))) {
            retainRetirement(f, "retirement_components_changed"); return;
        }
        BoreNativeContext.requireRetirement(f.top);
        f.commit(f.change(Change.Kind.EVACUATE, null, 0, null, true, true, null), false);
    }
    private static void retainRetirement(Frame f, String reason) {
        if (f.top.wasEvacuated()) return;
        requireRetentionPreparation(f);
        var custody = DeferredItemDrops.prepareBoreRetirement(f, reason);
        // SavedData preparation may call codecs; authenticate the original slot
        // identities again immediately before the callback-free claim + clear.
        f.retention = new RetirementRetention(f, custody);
        EssentiaTransfers.commitBoreRetention(f, f.retention);
    }
    private static void reconcileBound(Frame f) {
        if (!f.current() || f.before.phase() != TileArcaneBore.WorkPhase.RECONCILE) return;
        CompoundTag reference = f.top.workReference();
        boolean clear = reference != null && DeferredItemDrops.ownsBoreWitness(f.level, f.before.machineIdentity(), f.before.topPos(), reference);
        if (!f.current()) return;
        f.commit(f.change(Change.Kind.RECONCILE, null, f.before.credit(), clear ? null : reference, !clear, true, null), true);
    }

    private static ItemStack canonical(ServerLevel level, ItemStack stack) {
        return stack.isEmpty() ? ItemStack.EMPTY : BoreInventoryPlan.Stack.of(level.registryAccess(), stack, stack.getMaxStackSize()).copyStack(level.registryAccess());
    }
    private static List<BoreInventoryPlan.Stack> freezeBundle(ServerLevel level, List<ItemStack> stacks) {
        if (stacks == null) throw new IllegalArgumentException("Missing whole Bore loot bundle");
        var result = new ArrayList<BoreInventoryPlan.Stack>(); long total = 0;
        for (ItemStack stack : stacks) {
            if (stack == null || stack.isEmpty() || stack.getCount() <= 0) throw new IllegalArgumentException("Malformed Bore loot member");
            total = Math.addExact(total, stack.getCount());
            if (total > BoreInventoryPlan.MAX_BUNDLE_ITEMS) throw new IllegalArgumentException("Unrepresentable whole Bore loot count");
            result.add(BoreInventoryPlan.Stack.of(level.registryAccess(), stack, stack.getMaxStackSize()));
        }
        return List.copyOf(result);
    }

    public static void requireSnapshot(TileArcaneBore top) { requireFrame(top); }
    public static void requireRetirement(TileArcaneBore top) {
        Frame f = requireFrame(top);
        if (f.scope.operation() != BoreOperation.EVACUATE) throw new IllegalStateException("No Bore retirement work");
        BoreNativeContext.requireRetirement(top);
    }
    public static boolean matchesRetirement(TileArcaneBore top, TileArcaneBore.WorkSnapshot before) {
        Frame f = EssentiaTransfers.currentBoreFrame(top);
        return f != null && !f.closed && f.scope.operation() == BoreOperation.EVACUATE && BoreNativeContext.matchesRetirement(top, before);
    }
    private static Frame requireFrame(TileArcaneBore top) {
        Frame f = EssentiaTransfers.currentBoreFrame(top);
        EssentiaTransfers.requireBoreFrame(f);
        if (f.closed || f.top != top) throw new IllegalStateException("Expired Bore work frame");
        return f;
    }
    public static void requirePrepared(Frame f, Change change) {
        EssentiaTransfers.requireBoreFrame(f);
        if (f.closed || change == null || change.frame != f || f.prepared != change || change.used)
            throw new IllegalStateException("Wrong or reused Bore prepared change");
    }
    public static void requireRetentionPreparation(Frame f) {
        EssentiaTransfers.requireBoreFrame(f);
        if (f.closed || f.scope.operation() != BoreOperation.EVACUATE || f.top.wasEvacuated())
            throw new IllegalStateException("No dying Bore retention preparation");
        BoreNativeContext.requireRetirementRetention(f.top);
    }
    public static void requireRetentionPrepared(Frame f, RetirementRetention retention) {
        requireRetentionPreparation(f);
        if (retention == null || f.retention != retention || retention.frame != f || retention.used || retention.applied
                || !retention.life.equals(f.top.lifetimeId()) || !retention.machine.equals(f.top.machineIdentity())
                || retention.revision != f.top.revision()) throw new IllegalStateException("Wrong Bore retention token");
    }
    public static void requireRetention(TileArcaneBore top, RetirementRetention retention) {
        Frame f = requireFrame(top);
        requireRetentionPrepared(f, retention);
        EssentiaTransfers.requireBoreRetentionRaw(f, retention);
        retention.used = true;
    }
    public static void requireRetentionCapture(RetirementRetention retention, DeferredItemDrops.BoreRetirementCustody custody) {
        if (retention == null) throw new IllegalStateException("Missing Bore retention token");
        Frame f = retention.frame;
        EssentiaTransfers.requireBoreRetentionRaw(f, retention);
        if (f.closed || f.retention != retention || !retention.used || retention.captured || retention.applied
                || retention.custody != custody) throw new IllegalStateException("Replayed Bore retention capture");
        retention.captured = true;
    }
    public static void applyRetentionRaw(Frame f, RetirementRetention retention) {
        EssentiaTransfers.requireBoreRetentionRaw(f, retention);
        if (f.retention != retention || retention.applied) throw new IllegalStateException("Replayed Bore retention raw step");
        f.top.retainRetirementRaw(retention);
        retention.applied = true;
    }
    public static void requireChange(TileArcaneBore top, Change change) {
        Frame f = requireFrame(top); EssentiaTransfers.requireBoreRaw(f, change);
        if (change != f.prepared || change.frame != f || change.used || !change.applyTop || top.revision() != change.revision
                || !top.lifetimeId().equals(f.life) || !top.machineIdentity().equals(f.machine)) throw new IllegalStateException("Stale Bore field-write token");
        change.used = true;
    }
    public static void applyPreparedRaw(Frame f, Change change) {
        EssentiaTransfers.requireBoreRaw(f, change);
        if (change != f.prepared || change.applied) throw new IllegalStateException("Replayed Bore raw change");
        if (change.escrow != null && change.ownership() != null) switch (change.ownership()) {
            case ARM -> change.escrow.armRaw(); case WITNESS -> change.escrow.witnessRaw();
            case UNKNOWN -> change.escrow.unknownRaw(); case EVACUATE -> change.escrow.evacuateRaw();
            default -> throw new IllegalStateException("Not a Bore reservation operation");
        }
        if (change.applyTop) f.top.applyWorkChange(change); else change.used = true;
        change.applied = true;
    }
    public static void requireSelection(TileArcaneBore top, Selection selection) {
        Frame f = requireFrame(top);
        if (selection == null || f.selection != selection || selection.frame != f || selection.used || !f.owned()
                || selection.revision != top.revision() || f.last == null
                || f.last.kind != Change.Kind.WITNESS && f.last.kind != Change.Kind.REFUSE) throw new IllegalStateException("Wrong Bore selection token");
        selection.used = true;
    }
    public static void requireNotification(TileArcaneBore top, Change change) {
        Frame f = requireFrame(top);
        if (change == null || change.frame != f || change != f.last || !change.applied || change.notified || !f.owned())
            throw new IllegalStateException("Wrong Bore notification token");
        change.notified = true;
    }
    public static boolean ownsEscrow(Frame f, DeferredItemDrops.BoreEscrow escrow) {
        return f != null && !f.closed && f.escrow == escrow && escrow != null;
    }
    public static boolean mayLockActor(Frame f, BlockEntity actor) {
        return f != null && !f.closed && actor != null && (actor == f.baseIdentity
                || f.scope.operation() == BoreOperation.PREFILL && actor == f.drawingSource);
    }

    private static final class ActorLease {
        private final ServerLevel level;
        private final ServerPlayer actor;
        private final BorePlatform.Bridge platform;
        private final Inventory inventory;
        private final Object items, armor, offhand;
        private final List<ItemStack> slotObjects;
        private final int selected;
        private final Vec3 position, motion;
        private final float yaw, pitch;
        private final Object gameMode, abilities;
        private final Object gameType, pose;
        private final CompoundTag abilityState = new CompoundTag();
        private final boolean crouching;
        private ActorLease(ServerLevel level, ServerPlayer actor, BorePlatform.Bridge platform) {
            this.level = level; this.actor = actor; this.platform = platform; inventory = actor.getInventory();
            items = inventory.items; armor = inventory.armor; offhand = inventory.offhand; selected = inventory.selected;
            var slots = new ArrayList<ItemStack>(); slots.addAll(inventory.items); slots.addAll(inventory.armor); slots.addAll(inventory.offhand);
            slotObjects = List.copyOf(slots);
            position = actor.position(); motion = actor.getDeltaMovement(); yaw = actor.getYRot(); pitch = actor.getXRot();
            gameMode = actor.gameMode; gameType = actor.gameMode.getGameModeForPlayer(); abilities = actor.getAbilities();
            actor.getAbilities().addSaveData(abilityState); pose = actor.getPose(); crouching = actor.isShiftKeyDown();
        }
        private boolean valid() {
            if (!BoreNativeContext.isLiveLevel(level) || !platform.isNativeMachineActor(actor) || actor.serverLevel() != level
                    || actor.isRemoved() || !actor.isAlive() || actor.isCreative() || actor.isSpectator() || actor.getAbilities().instabuild
                    || actor.getInventory() != inventory || inventory.getClass() != Inventory.class
                    || inventory.items.size() != 36 || inventory.armor.size() != 4 || inventory.offhand.size() != 1 || selected < 0 || selected > 8
                    || inventory.items != items || inventory.armor != armor || inventory.offhand != offhand || inventory.selected != selected
                    || actor.gameMode != gameMode || actor.gameMode.getGameModeForPlayer() != gameType || actor.getAbilities() != abilities
                    || actor.getPose() != pose || actor.isShiftKeyDown() != crouching
                    || !actor.position().equals(position) || !actor.getDeltaMovement().equals(motion) || actor.getYRot() != yaw || actor.getXRot() != pitch) return false;
            CompoundTag currentAbilities = new CompoundTag(); actor.getAbilities().addSaveData(currentAbilities);
            if (!abilityState.equals(currentAbilities)) return false;
            int index = 0;
            for (ItemStack stack : inventory.items) if (!stack.isEmpty() || stack != slotObjects.get(index++)) return false;
            for (ItemStack stack : inventory.armor) if (!stack.isEmpty() || stack != slotObjects.get(index++)) return false;
            for (ItemStack stack : inventory.offhand) if (!stack.isEmpty() || stack != slotObjects.get(index++)) return false;
            return true;
        }
    }
}
