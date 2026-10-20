package thaumcraft.common.lib.utils;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;
import java.util.List;
import java.util.Random;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.entity.EntityInLevelCallback;
import net.minecraft.world.level.entity.EntitySection;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.phys.Vec3;
import thaumcraft.common.Thaumcraft;
import thaumcraft.common.config.ThaumcraftContent;
import thaumcraft.common.entities.EntityInventoryItem;
import thaumcraft.common.entities.golems.EntityGolemBase;
import thaumcraft.api.aspects.IEssentiaContainerItem;
import thaumcraft.common.lib.essentia.EssentiaAtomicEndpoint;
import thaumcraft.common.lib.essentia.EssentiaTransfers;
import thaumcraft.common.lib.essentia.BoreInventoryPlan;
import thaumcraft.common.lib.bore.BoreWork;
import thaumcraft.common.lib.bore.BoreNativeContext;
import thaumcraft.common.mixin.EntityLevelCallbackAccess;
import thaumcraft.common.mixin.PersistentEntitySectionManagerAccess;
import thaumcraft.common.mixin.ServerLevelEntityManagerAccess;

/**
 * InventoryUtils.dropItems adaptation: keep an undelivered physical inventory
 * after its block disappears. Successful drops retain the original local RNG,
 * count/components and scatter; a canceled native spawn is retried, not lost.
 */
public final class DeferredItemDrops extends SavedData {
    private static final String ID = "thaumcraft_pending_inventory_drops";
    private static final Factory<DeferredItemDrops> FACTORY = new Factory<>(
            DeferredItemDrops::new, DeferredItemDrops::load, null);
    private static final int VERSION = 3; // V1/V2 rows retain their original physics/ownership.
    private static final int BATCH = 64;
    /** Bounded first-slice physical plan, NOT a source-count clamp. */
    public static final int MAX_BORE_EJECTION_SEGMENTS = 4096;
    private static final long RETRY_DELAY = 20L;
    private final ArrayDeque<Pending> pending = new ArrayDeque<>();
    private final ListTag quarantined = new ListTag();
    private final Set<UUID> settled = new HashSet<>();
    private final Set<UUID> routingFrames = new HashSet<>();
    private final Map<UUID, CompoundTag> boreJournals = new HashMap<>();
    // Sole live opaque owners, NOT Pending rows or another completed bundle.
    // They retain no Level/BE/frame and are never routed, flushed or restored.
    private final List<BoreRetirementCustody> boreRetirements = new ArrayList<>();
    private long retirementRevision;
    private int saveDepth;
    private Attempt inFlight;
    private static final Map<ServerLevel, Set<UUID>> LIVE_RECEIPTS = java.util.Collections.synchronizedMap(new WeakHashMap<>());
    private static final ThreadLocal<Map<UUID, Attempt>> ACTIVE = ThreadLocal.withInitial(HashMap::new);
    private static final ThreadLocal<Set<UUID>> ROUTING_ACTIVE = ThreadLocal.withInitial(HashSet::new);
    private boolean flushing;
    private boolean deliveryRequested;
    private long retryAfter;

    private record BoreDrop(UUID transaction, BlockPos target, int nozzle, int attachment, boolean evacuation, int member, int segment) {}
    /** The longest a catch may be saved as burning (a Fishing golem sets two seconds); anything above is a malformed row. */
    private static final int MAX_FIRE_TICKS = 600;
    private record Pending(UUID id, BlockPos source, Vec3 position, Vec3 motion, ItemStack stack, int pickupDelay, long retryAfter, int crystalFace, boolean routing, BoreDrop bore, int fireTicks) {
        private Pending(UUID id, BlockPos source, Vec3 position, Vec3 motion, ItemStack stack, int pickupDelay, long retryAfter, int crystalFace, boolean routing, BoreDrop bore) {
            this(id, source, position, motion, stack, pickupDelay, retryAfter, crystalFace, routing, bore, 0);
        }
        private Pending(UUID id, BlockPos source, Vec3 position, Vec3 motion, ItemStack stack) {
            this(id, source, position, motion, stack, 0, Long.MIN_VALUE, -1, false, null);
        }
        private Pending(UUID id, BlockPos source, Vec3 position, Vec3 motion, ItemStack stack, int pickupDelay, long retryAfter) {
            this(id, source, position, motion, stack, pickupDelay, retryAfter, -1, false, null);
        }
        private Pending retryAt(long time) { return new Pending(id, source, position, motion, stack, pickupDelay, time, crystalFace, routing, bore, fireTicks); }
        private Pending ready() { return new Pending(id, source, position, motion, stack, pickupDelay, retryAfter, crystalFace, false, bore, fireTicks); }
    }

    private static final class Attempt {
        final DeferredItemDrops queue;
        final ServerLevel level;
        final Pending entry;
        ItemEntity entity;
        boolean accepted, cleanup, destroyedBeforeAdmission, destructionRequested;
        Attempt(DeferredItemDrops queue, ServerLevel level, Pending entry) { this.queue = queue; this.level = level; this.entry = entry; }
        void accept() {
            accepted = true; queue.settled.add(entry.id); receipts(level).add(entry.id); queue.setDirty();
        }
        void reconcile() {
            if (!cleanup && entity != null && (destructionRequested && entity.isRemoved() || level.getEntity(entry.id) == entity)) accept();
        }
    }
    private static Set<UUID> receipts(ServerLevel level) { return LIVE_RECEIPTS.computeIfAbsent(level, ignored -> new HashSet<>()); }

    /** Prepared but UNSPENDABLE until the paid executor authorizes its exact
     * identity. No public enqueue-by-position capability is added for crystals. */
    public static final class CrystalEscrow {
        private final DeferredItemDrops queue;
        private final ServerLevel level;
        private Pending entry;
        private boolean published, finished;
        private CrystalEscrow(DeferredItemDrops queue, ServerLevel level, Pending entry) { this.queue = queue; this.level = level; this.entry = entry; }
        public void publishRaw() {
            EssentiaTransfers.requireCrystalEscrowMutation(this, EssentiaTransfers.CrystalOwnership.PUBLISH);
            if (published) throw new IllegalStateException("Crystal escrow already published");
            queue.pending.addLast(entry); queue.routingFrames.add(entry.id); ROUTING_ACTIVE.get().add(entry.id); published = true; queue.setDirty();
        }
        public void inventoryRaw() {
            EssentiaTransfers.requireCrystalEscrowMutation(this, EssentiaTransfers.CrystalOwnership.INVENTORY);
            if (!published || finished || !queue.pending.remove(entry)) throw new IllegalStateException("Crystal escrow has no inventory credit right");
            queue.settled.add(entry.id); receipts(level).add(entry.id); finished = true; queue.setDirty();
        }
        public void dropRaw() {
            EssentiaTransfers.requireCrystalEscrowMutation(this, EssentiaTransfers.CrystalOwnership.DROP);
            if (!published || finished || !queue.pending.remove(entry)) throw new IllegalStateException("Crystal escrow has no residual drop right");
            entry = entry.ready(); queue.pending.addLast(entry); finished = true; queue.deliveryRequested = true; queue.setDirty();
        }
        public boolean needsRoute() { return published && !finished; }
        public void finishFrame() {
            EssentiaTransfers.requireCrystalEscrowScope(this);
            queue.routingFrames.remove(entry.id);
            ROUTING_ACTIVE.get().remove(entry.id);
        }
    }
    public static CrystalEscrow prepareCrystal(ServerLevel level, BlockPos source, Direction facing, ItemStack typed) {
        if (!level.getServer().isSameThread() || facing == null || !producedCrystal(typed)) throw new IllegalArgumentException("Canonical native typed crystal required");
        DeferredItemDrops queue = level.getDataStorage().computeIfAbsent(FACTORY, ID);
        Direction out = facing.getOpposite();
        Vec3 position = new Vec3(source.getX() + .5 + out.getStepX() * .65, source.getY() + .5 + out.getStepY() * .65, source.getZ() + .5 + out.getStepZ() * .65);
        Vec3 motion = new Vec3(out.getStepX() * .04F, out.getStepY() * .04F, out.getStepZ() * .04F);
        return new CrystalEscrow(queue, level, new Pending(UUID.randomUUID(), source.immutable(), position, motion, typed.copy(), 0, Long.MIN_VALUE, facing.get3DDataValue(), true, null));
    }
    private static boolean producedCrystal(ItemStack stack) {
        if (stack.isEmpty() || stack.getCount() != 1 || !stack.is(ThaumcraftContent.CRYSTAL_ESSENCE.get()) || !(stack.getItem() instanceof IEssentiaContainerItem item)) return false;
        var contents = item.getAspects(stack);
        return contents != null && contents.size() == 1 && EssentiaAtomicEndpoint.validAspect(contents.getAspects()[0]) && contents.getAmount(contents.getAspects()[0]) == 1;
    }

    /** Bore-only candidate/journal. PREPARED and ARMED are NOT spendable loot.
     * No constructor, supplied success flag, target or bundle enqueue capability
     * is public. Only the exact existing executor's raw operation can publish. */
    public static final class BoreEscrow {
        private final DeferredItemDrops queue;
        private final UUID id;
        private final CompoundTag armed, witnessed, unknown, evacuated;
        private final List<Pending> routing, ready;
        private int phase; // 0 prepared, 1 armed, 2 witnessed, 3 unknown, 4 evacuated, 5 routed
        private BoreEscrow(DeferredItemDrops queue, CompoundTag evidence, List<Pending> rows, ListTag encoded) {
            this.queue = queue; id = evidence.getUUID("Id"); routing = List.copyOf(rows);
            ready = rows.stream().map(Pending::ready).toList();
            armed = envelope(evidence, encoded, "ARMED"); witnessed = envelope(evidence, encoded, "WITNESSED");
            unknown = envelope(evidence, encoded, "UNKNOWN"); evacuated = envelope(evidence, encoded, "EVACUATED");
        }
        private static CompoundTag envelope(CompoundTag evidence, ListTag rows, String phase) {
            CompoundTag tag = evidence.copy(); tag.putString("Phase", phase); tag.putBoolean("Fenced", phase.equals("ARMED") || phase.equals("UNKNOWN"));
            tag.put("Rows", rows.copy()); return tag;
        }
        public CompoundTag reference(String phase) {
            CompoundTag tag = switch (phase) {
                case "ARMED" -> armed.copy(); case "WITNESSED" -> witnessed.copy(); case "UNKNOWN" -> unknown.copy();
                default -> throw new IllegalArgumentException("Not a Bore reference phase");
            };
            tag.remove("Rows"); tag.remove("OriginalBundle"); return tag;
        }
        public boolean isUnknown() { return phase == 3; }
        public void armRaw() {
            EssentiaTransfers.requireBoreEscrow(this, EssentiaTransfers.BoreOwnership.ARM);
            if (phase != 0 || queue.boreJournals.containsKey(id)) throw new IllegalStateException("Bore candidate already published");
            queue.boreJournals.put(id, armed); phase = 1; queue.setDirty();
        }
        public void witnessRaw() {
            EssentiaTransfers.requireBoreEscrow(this, EssentiaTransfers.BoreOwnership.WITNESS);
            if (phase != 1 || queue.boreJournals.get(id) != armed) throw new IllegalStateException("No exact armed Bore candidate");
            queue.boreJournals.put(id, witnessed);
            for (Pending row : routing) {
                queue.pending.addLast(row); queue.routingFrames.add(row.id); ROUTING_ACTIVE.get().add(row.id);
            }
            phase = 2; queue.setDirty();
        }
        public void unknownRaw() {
            EssentiaTransfers.requireBoreEscrow(this, EssentiaTransfers.BoreOwnership.UNKNOWN);
            if (phase != 1 || queue.boreJournals.get(id) != armed) throw new IllegalStateException("No armed Bore reservation to quarantine");
            queue.boreJournals.put(id, unknown); phase = 3; queue.setDirty();
        }
        public void evacuateRaw() {
            EssentiaTransfers.requireBoreEscrow(this, EssentiaTransfers.BoreOwnership.EVACUATE);
            if (phase != 0 || queue.boreJournals.containsKey(id)) throw new IllegalStateException("Bore retirement already claimed");
            // Claim BEFORE the dying BE clears its currently worn slots/payload.
            queue.boreJournals.put(id, evacuated); queue.pending.addAll(ready);
            phase = 4; queue.deliveryRequested = true; queue.setDirty();
        }
        public void dropRaw() {
            EssentiaTransfers.requireBoreEscrow(this, EssentiaTransfers.BoreOwnership.DROP);
            if (phase == 5) return;
            if (phase != 2) throw new IllegalStateException("Only witnessed Bore output can route");
            for (int index = 0; index < routing.size(); index++) {
                if (!queue.pending.remove(routing.get(index))) throw new IllegalStateException("Missing witnessed Bore residual");
                queue.pending.addLast(ready.get(index));
            }
            phase = 5; queue.deliveryRequested = true; queue.setDirty();
        }
        public void finishFrame() {
            EssentiaTransfers.requireBoreEscrowScope(this);
            for (Pending row : routing) { queue.routingFrames.remove(row.id); ROUTING_ACTIVE.get().remove(row.id); }
        }
    }

    /** Only a private-constructor native-retirement token can fill this sink.
     * Actual slot objects move here; no mutable getter or physical delivery API.
     * A successful save is a checkpoint of this ONE owner, not a second stock. */
    public static final class BoreRetirementCustody {
        private final DeferredItemDrops queue;
        private final UUID id = UUID.randomUUID(), life, machine;
        private final BlockPos source;
        private final long revision;
        private final String reason;
        private ItemStack focus, pick;
        private CompoundTag rejected, reference;
        private boolean claimed;
        private String lastSaveFailure = "";
        private BoreRetirementCustody(DeferredItemDrops queue, BoreWork.Frame frame, String reason) {
            this.queue = queue; this.reason = reason;
            var top = frame.top(); source = top.getBlockPos().immutable();
            life = top.lifetimeId(); machine = top.machineIdentity(); revision = top.revision();
        }
        public void claimRaw(BoreWork.RetirementRetention retention, ItemStack actualFocus, ItemStack actualPick,
                             CompoundTag actualRejected, CompoundTag actualReference) {
            if (claimed) throw new IllegalStateException("Bore retirement custody already owns stock");
            BoreWork.requireRetentionCapture(retention, this);
            // RAW: no copies, codecs, component equality, world reads or notices.
            focus = actualFocus; pick = actualPick; rejected = actualRejected; reference = actualReference;
            queue.boreRetirements.add(this); claimed = true; ++queue.retirementRevision; queue.setDirty();
            // The calling tile now clears its fields before this raw step ends.
        }
        private CompoundTag payload(HolderLookup.Provider registries) {
            var row = new CompoundTag(); row.putString("Kind", "BORE_RETIREMENT_CUSTODY");
            row.putInt("CustodyVersion", 1); row.putUUID("Id", id); row.putLong("Source", source.asLong());
            row.putUUID("Machine", machine); row.putUUID("Lifetime", life); row.putLong("Revision", revision);
            row.putBoolean("Unsendable", true); row.putString("Reason", reason);
            row.put("Focus", retainedStack(focus, registries)); row.put("Pick", retainedStack(pick, registries));
            if (rejected != null) row.put("RejectedOwner", rejected.copy());
            if (reference != null) row.put("WorkReference", reference.copy());
            return row;
        }
        private static CompoundTag retainedStack(ItemStack actual, HolderLookup.Provider registries) {
            var row = new CompoundTag();
            if (actual.isEmpty()) { row.putBoolean("Empty", true); return row; }
            int count = actual.getCount();
            Tag encoded = actual.copyWithCount(1).save(registries).copy();
            if (!(encoded instanceof CompoundTag unit)) throw new IllegalStateException("Missing whole retained stack payload");
            var decoded = ItemStack.parse(registries, unit.copy()).orElseThrow();
            if (decoded.isEmpty() || decoded.getCount() != 1 || count != actual.getCount()
                    || !decoded.save(registries).equals(unit)) throw new IllegalStateException("Unrepresentable retained stack components");
            row.putInt("Count", count); row.put("Stack", unit); return row;
        }
        private CompoundTag checkpoint(HolderLookup.Provider registries) {
            if (!claimed) throw new IllegalStateException("Unclaimed Bore retirement checkpoint");
            try {
                CompoundTag first = payload(registries), second = payload(registries);
                if (!first.equals(second)) throw new IllegalStateException("Bore custody changed during whole-payload encoding");
                lastSaveFailure = "";
                return second;
            } catch (RuntimeException | Error failure) {
                lastSaveFailure = failure.getClass().getName(); queue.setDirty();
                // Do NOT substitute an empty diagnostic row. Aborting this save
                // leaves actual objects in this queue owner for a coherent retry.
                throw failure;
            }
        }
    }
    public static BoreRetirementCustody prepareBoreRetirement(BoreWork.Frame frame, String reason) {
        BoreWork.requireRetentionPreparation(frame);
        var level = (ServerLevel) frame.top().getLevel();
        DeferredItemDrops queue = level.getDataStorage().computeIfAbsent(FACTORY, ID);
        BoreWork.requireRetentionPreparation(frame);
        if (level.getDataStorage().get(FACTORY, ID) != queue) throw new IllegalStateException("Detached Bore retention queue");
        return new BoreRetirementCustody(queue, frame, reason);
    }
    /** Read-only diagnostics; neither actual stacks nor a restore permit escape. */
    public int boreRetirementCustodyCount() { return boreRetirements.size(); }
    public List<String> boreRetirementSaveFailures() { return boreRetirements.stream().map(owner -> owner.lastSaveFailure).toList(); }
    @Override public boolean isDirty() {
        // Opaque live owners keep requesting checkpoints, including after native
        // SavedData's IO-error path clears its ordinary dirty flag. Other queue
        // kinds retain their existing dirty behavior. No ownership is duplicated.
        return !boreRetirements.isEmpty() || super.isDirty();
    }

    public static BoreEscrow prepareBore(BoreWork.Output output) {
        output.claim(); // One-shot claim BEFORE registry/codec/SavedData callbacks.
        ServerLevel level = output.level();
        if (!BoreNativeContext.isLiveLevel(level)) throw new IllegalStateException("Bore queue needs a live canonical level");
        DeferredItemDrops queue = level.getDataStorage().computeIfAbsent(FACTORY, ID);
        CompoundTag evidence = output.evidence();
        UUID transaction = evidence.getUUID("Id"); BlockPos source = BlockPos.of(evidence.getLong("Source"));
        boolean evacuation = output.evacuation();
        int nozzle = evacuation ? -1 : evidence.getInt("Nozzle"), attachment = evacuation ? -1 : evidence.getInt("Attachment");
        BlockPos target = evacuation ? source : BlockPos.of(evidence.getLong("Target"));
        var rows = new ArrayList<Pending>(); ListTag encoded = new ListTag();
        ListTag original = new ListTag();
        Random scatter = evacuation ? new Random() : null; // Original inventory evacuation's local RNG.
        long segments = 0;
        for (var value : output.bundle()) segments = Math.addExact(segments, ((long) value.count() + 63) / 64);
        if (segments > MAX_BORE_EJECTION_SEGMENTS) throw new IllegalArgumentException("Bore physical segment budget exceeded");
        for (int member = 0; member < output.bundle().size(); member++) {
            var value = output.bundle().get(member);
            ItemStack whole = value.copyStack(level.registryAccess());
            CompoundTag sourceRow = new CompoundTag(); sourceRow.putInt("Member", member); sourceRow.putInt("Count", value.count());
            sourceRow.put("Stack", whole.copyWithCount(1).save(level.registryAccess()).copy()); original.add(sourceRow);
            Vec3 position, motion;
            if (evacuation) {
                float x = scatter.nextFloat() * .8F + .1F, y = scatter.nextFloat() * .8F + .1F, z = scatter.nextFloat() * .8F + .1F;
                position = new Vec3((float) source.getX() + x, (float) source.getY() + y, (float) source.getZ() + z);
                motion = new Vec3(scatter.nextGaussian() * .05F, scatter.nextGaussian() * .05F + .2F, scatter.nextGaussian() * .05F);
            } else {
                Direction out = Direction.from3DDataValue(nozzle), mount = Direction.from3DDataValue(attachment);
                position = borePosition(source, out, mount); motion = boreMotion(out);
            }
            int remaining = value.count(), segment = 0;
            while (remaining > 0) {
                int count = Math.min(64, remaining);
                // Decode each segment independently; native ADD callbacks must
                // not alias a sibling's custom data/text or the candidate owner.
                ItemStack physical = value.copyStack(level.registryAccess()); physical.setCount(count);
                Pending row = new Pending(UUID.randomUUID(), source, position, motion, physical, 0, Long.MIN_VALUE, -1, true,
                        new BoreDrop(transaction, target, nozzle, attachment, evacuation, member, segment++));
                rows.add(row); encoded.add(writePending(row, level.registryAccess())); remaining -= count;
            }
        }
        evidence.put("OriginalBundle", original);
        return new BoreEscrow(queue, evidence, rows, encoded);
    }
    private static Vec3 borePosition(BlockPos source, Direction out, Direction mount) {
        return new Vec3(source.getX() + .5 + out.getStepX() * .66, source.getY() + .4 + mount.getOpposite().getStepY(), source.getZ() + .5 + out.getStepZ() * .66);
    }
    private static Vec3 boreMotion(Direction out) { return new Vec3(out.getStepX() * .075F, .025F, out.getStepZ() * .075F); }

    public static boolean ownsBoreWitness(ServerLevel level, UUID machine, BlockPos pos, CompoundTag reference) {
        if (!BoreNativeContext.isLiveLevel(level)) return false;
        DeferredItemDrops queue = level.getDataStorage().get(FACTORY, ID);
        if (queue == null) return false;
        try {
            CompoundTag journal = queue.boreJournals.get(reference.getUUID("Id"));
            if (journal == null || !journal.getString("Phase").equals("WITNESSED") || !journal.getUUID("Machine").equals(machine)
                    || journal.getLong("Source") != pos.asLong()) return false;
            CompoundTag expected = journal.copy(); expected.remove("Rows"); expected.remove("OriginalBundle");
            if (!expected.equals(reference)) return false; // No ARMED/stale-reference inference from current air.
            var owned = new HashSet<UUID>(queue.pendingIds()); owned.addAll(queue.settled); owned.addAll(receipts(level));
            for (Tag raw : journal.getList("Rows", Tag.TAG_COMPOUND)) if (!owned.contains(((CompoundTag) raw).getUUID("Id"))) return false;
            return true;
        } catch (RuntimeException malformed) { return false; }
    }
    public int boreJournalCount() { return boreJournals.size(); }
    /** Detached diagnostics only; never a restore, debit, witness or output permit. */
    public static List<CompoundTag> boreJournalView(ServerLevel level) {
        if (!BoreNativeContext.isLiveLevel(level)) return List.of();
        DeferredItemDrops data = level.getDataStorage().get(FACTORY, ID);
        return data == null ? List.of() : data.boreJournals.values().stream().map(CompoundTag::copy).toList();
    }
    public int boreUnknownCount() {
        int count = 0; for (CompoundTag tag : boreJournals.values()) if (tag.getString("Phase").equals("UNKNOWN")) count++; return count;
    }

    /** Called only by native lifecycle mixins for the EXACT in-flight entity.
     * Deliberate ADD-hook kill/pickup transfers/destructs ownership; our own
     * rejected-spawn cleanup is explicitly excluded and must still retry. */
    public static void observeRemoval(Entity entity, Entity.RemovalReason reason) {
        Attempt attempt = ACTIVE.get().get(entity.getUUID());
        if (attempt != null && attempt.entity == entity && !attempt.cleanup) {
            boolean unattached = ((EntityLevelCallbackAccess) entity).thaumcraft$getLevelCallback() == EntityInLevelCallback.NULL;
            // HEAD is only intent: a preceding removal hook can still throw
            // before the native removal field is written. Reconcile actual state.
            if (reason.shouldDestroy()) { attempt.destroyedBeforeAdmission |= unattached; attempt.destructionRequested = true; }
            else if (!unattached) attempt.accept();
        }
    }
    public static void confirmRemoval(Entity entity) {
        Attempt attempt = ACTIVE.get().get(entity.getUUID());
        if (attempt != null && attempt.entity == entity) attempt.reconcile();
    }
    public static void observeAdmission(Entity entity, EntityInLevelCallback callback) {
        Attempt attempt = ACTIVE.get().get(entity.getUUID());
        if (attempt != null && attempt.entity == entity && callback != EntityInLevelCallback.NULL && !attempt.cleanup) attempt.accept();
    }
    public static boolean allowPlayerTouch(ItemEntity entity) {
        Attempt attempt = ACTIVE.get().get(entity.getUUID());
        if (attempt == null || attempt.entity != entity) return true;
        if (attempt.level.getEntity(entity.getUUID()) != entity) return false; // No pickup of an unadmitted native ADD candidate.
        attempt.accept(); return true; // Native world owns it BEFORE inventory/pickup callbacks.
    }
    private static boolean managed(ServerLevel level, ItemEntity entity) {
        var manager = ((ServerLevelEntityManagerAccess) level).thaumcraft$getEntityManager();
        var sections = ((PersistentEntitySectionManagerAccess) manager).thaumcraft$getSectionStorage();
        for (long chunk : sections.getAllChunksWithExistingSections()) {
            try (var stream = sections.getExistingSectionsInChunk(chunk)) {
                if (stream.flatMap(EntitySection::getEntities).anyMatch(existing -> existing == entity)) return true;
            }
        }
        return false;
    }

    public DeferredItemDrops() {}

    /** AIHomeDrop/AIEmptyDrop's directed trajectory, not inventory scatter.
     * The worker authorizes this only inside its actual cargo publication guard.
     * Delivery remains level-post, so spawn callbacks cannot reenter the handoff. */
    public static boolean enqueueGolemThrow(ServerLevel level, EntityGolemBase golem, BlockPos target) {
        if (!level.getServer().isSameThread()) throw new IllegalStateException("Golem throws require the server thread");
        if (golem == null || target == null || golem.level() != level || level.getEntity(golem.getUUID()) != golem
                || !golem.canPublishDirectedThrow(target)) return false;
        var data = level.getDataStorage().computeIfAbsent(FACTORY, ID);
        ItemStack cargo = golem.getCarriedItem();
        if (cargo.isEmpty()) return false;
        BlockPos source = golem.blockPosition().immutable();
        Vec3 position = new Vec3(golem.getX(), golem.getY() + (double) (golem.getBbHeight() / 2.0F), golem.getZ());
        Vec3 aim = Vec3.atCenterOf(target);
        double factor = Math.sqrt(golem.distanceToSqr(aim.x, aim.y, aim.z)) / 3.0D;
        Vec3 motion = new Vec3((aim.x - golem.getX()) * factor,
                .1D + (aim.y - position.y) * factor, (aim.z - golem.getZ()) * factor);
        Pending entry = new Pending(UUID.randomUUID(), source, position, motion, cargo, 10, Long.MIN_VALUE);
        if (!golem.canPublishDirectedThrow(target) || level.getEntity(golem.getUUID()) != golem) return false;
        data.pending.addLast(entry);
        // No delivery, notification, or native callback occurs between append
        // and the matching raw cargo clear. A refused handoff leaves both unchanged.
        boolean detached;
        try {
            detached = golem.detachDirectedThrowCargo(target, cargo);
        } catch (RuntimeException failure) {
            // The bridge validates before its raw clear and performs no fallible
            // work after it; preparation failure must not leave a second owner.
            data.pending.removeLastOccurrence(entry);
            throw failure;
        }
        if (!detached) {
            data.pending.removeLastOccurrence(entry);
            return false;
        }
        data.deliveryRequested = true;
        data.setDirty();
        return true;
    }

    /** The live Fishing goal already owns this one rolled result and sampled
     * trajectory. No RNG, callbacks or delivery occur during its publication;
     * caller clears its unpublished result immediately after normal return.
     * {@code fireTicks} is the flame the delivered item carries (a Fire golem's catch burns for two seconds); it is kept with the
     * paid row and set on the entity at admission, and a row that is retried or saved keeps it. */
    public static void enqueueGolemFishingCatch(ServerLevel level, BlockPos water, Vec3 motion, ItemStack actualResult, int fireTicks) {
        if (!level.getServer().isSameThread() || level.getServer().getLevel(level.dimension()) != level)
            throw new IllegalStateException("Fishing output requires the canonical server level and thread");
        if (fireTicks < 0 || fireTicks > MAX_FIRE_TICKS) throw new IllegalArgumentException("Fishing output cannot burn for " + fireTicks + " ticks");
        if (water == null || motion == null || actualResult == null || actualResult.isEmpty()
                || !Double.isFinite(motion.x) || !Double.isFinite(motion.y) || !Double.isFinite(motion.z))
            throw new IllegalArgumentException("Fishing output requires its actual catch and finite sampled trajectory");
        var data = level.getDataStorage().computeIfAbsent(FACTORY, ID);
        BlockPos source = water.immutable();
        Pending entry = new Pending(UUID.randomUUID(), source,
                new Vec3(source.getX() + .5, source.getY() + 1, source.getZ() + .5),
                motion, actualResult.copy(), 20, Long.MIN_VALUE, -1, false, null, fireTicks);
        data.pending.addLast(entry);
        data.deliveryRequested = true;
        data.setDirty();
    }

    public static void enqueue(ServerLevel level, BlockPos source, List<ItemStack> stacks) {
        if (!level.getServer().isSameThread()) throw new IllegalStateException("Inventory drops require the server thread");
        var data = level.getDataStorage().computeIfAbsent(FACTORY, ID);
        // Original InventoryUtils makes a fresh java.util.Random per evacuation;
        // neither world RNG nor a player's research/ink RNG is consumed.
        Random random = new Random();
        for (ItemStack stack : stacks) {
            if (stack.isEmpty()) continue;
            float rx = random.nextFloat() * 0.8F + 0.1F;
            float ry = random.nextFloat() * 0.8F + 0.1F;
            float rz = random.nextFloat() * 0.8F + 0.1F;
            var position = new Vec3((float) source.getX() + rx, (float) source.getY() + ry, (float) source.getZ() + rz);
            float factor = 0.05F;
            var motion = new Vec3(random.nextGaussian() * factor,
                    random.nextGaussian() * factor + 0.2F, random.nextGaussian() * factor);
            data.pending.addLast(new Pending(UUID.randomUUID(), source.immutable(), position, motion, stack.copy()));
            data.deliveryRequested = true;
            data.setDirty();
        }
        // Caller clears the physical inventory before returning. Delivery is
        // deferred until SERVER_LEVEL_POST: a synchronous entity-spawn callback
        // must not reenter a Block.onRemove/setBlock operation still in flight.
    }

    /** BlockAiry harvest uses Block.dropBlockAsItem, NOT InventoryUtils scatter.
     * Caller has already sampled doTileDrops before the successful harvest.
     * Preserve its three world-RNG offsets and10-tick pickup delay while sharing
     * only safe, durable level-post delivery with inventory evacuations. */
    public static void enqueueBlockHarvest(ServerLevel level, BlockPos source, List<ItemStack> stacks) {
        if (!level.getServer().isSameThread()) throw new IllegalStateException("Block harvest drops require the server thread");
        var data = level.getDataStorage().computeIfAbsent(FACTORY, ID);
        for (ItemStack stack : stacks) {
            if (stack.isEmpty()) continue;
            float spread = .7F;
            double dx = level.random.nextFloat() * spread + (1 - spread) * .5D;
            double dy = level.random.nextFloat() * spread + (1 - spread) * .5D;
            double dz = level.random.nextFloat() * spread + (1 - spread) * .5D;
            Random random = new Random(); // Original EntityItem's own RNG, not world RNG.
            Vec3 motion = new Vec3(random.nextFloat() * .2F - .1F, .2F, random.nextFloat() * .2F - .1F);
            data.pending.addLast(new Pending(UUID.randomUUID(), source.immutable(),
                    new Vec3(source.getX() + dx, source.getY() + dy, source.getZ() + dz), motion, stack.copy(), 10, Long.MIN_VALUE));
            data.deliveryRequested = true;
            data.setDirty();
        }
    }

    public static void tick(ServerLevel level) {
        // Do not create a SavedData file in every dimension just for ticking.
        var data = level.getDataStorage().get(FACTORY, ID);
        if (data == null || data.pending.isEmpty()) return;
        if (!data.deliveryRequested && level.getGameTime() < data.retryAfter) return;
        data.deliveryRequested = false;
        data.retryAfter = level.getGameTime() + RETRY_DELAY;
        data.flush(level);
    }

    /** Safe level-post retry path, also used by real-loader persistence/failure checks. */
    public void flush(ServerLevel level) {
        if (!level.getServer().isSameThread()) throw new IllegalStateException("Inventory drops require the server thread");
        if (flushing) return;
        flushing = true;
        try {
        int count = Math.min(BATCH, pending.size());
        for (int index = 0; index < count; index++) {
            Pending entry = pending.removeFirst();
            if (entry.bore != null && (EssentiaTransfers.isMutationActive() || !BoreNativeContext.canDispatchOutputs(level)
                    || level.getDataStorage().get(FACTORY, ID) != this)) {
                pending.addLast(entry); continue; // Detached/callback/stale queues cannot publish Bore output.
            }
            if (settled.contains(entry.id) || receipts(level).contains(entry.id)) { settled.add(entry.id); setDirty(); continue; }
            // A fresh evacuation may wake this queue immediately. It must not
            // bypass another output's cooldown after a rejected native ADD.
            // Retry timing is transient; a loaded SavedData may retry at once.
            if (entry.routing || routingFrames.contains(entry.id) || ROUTING_ACTIVE.get().contains(entry.id) || ACTIVE.get().containsKey(entry.id)
                    || level.getGameTime() < entry.retryAfter || !level.hasChunkAt(entry.source)
                    || entry.crystalFace >= 0 && !level.hasChunkAt(BlockPos.containing(entry.position))
                    || entry.bore != null && (EssentiaTransfers.isMutationActive() || !BoreNativeContext.canDispatchOutputs(level)
                        || !level.hasChunkAt(BlockPos.containing(entry.position)) || !level.hasChunkAt(entry.bore.target))) {
                pending.addLast(entry);
                continue;
            }
            var existing = level.getEntity(entry.id);
            if (existing != null) {
                // Stable IDs make repeated delivery harmless while the original
                // output is already present; conflicting IDs retain the entry.
                if (existing.isRemoved() || !(existing instanceof ItemEntity item) || !ItemStack.matches(item.getItem(), entry.stack)) {
                    pending.addLast(entry.retryAt(level.getGameTime() + RETRY_DELAY));
                } else { settled.add(entry.id); receipts(level).add(entry.id); setDirty(); }
                continue;
            }
            Attempt attempt = new Attempt(this, level, entry);
            inFlight = attempt; ACTIVE.get().put(entry.id, attempt);
            try {
            ItemEntity output;
            RejectedEntitySpawn.Snapshot beforeSpawn;
            try {
                if (entry.bore != null && entry.stack.getCount() > 64) throw new IllegalStateException("Unrepresentable native Bore entity segment");
                ItemStack physical = entry.bore == null ? entry.stack.copy()
                        : BoreInventoryPlan.Stack.of(level.registryAccess(), entry.stack, entry.stack.getMaxStackSize()).copyStack(level.registryAccess());
                if (entry.stack.getCount() > 99) {
                    // The native ItemStack save codec rejects count100..127, which
                    // original discovery overstack could hold in one entity.
                    output = new EntityInventoryItem(ThaumcraftContent.INVENTORY_ITEM.get(), level);
                    output.setPos(entry.position);
                    output.setItem(physical);
                } else if (entry.pickupDelay > 0 || entry.crystalFace >= 0 || entry.bore != null) {
                    // Explicit velocity avoids extra world-RNG draws from the
                    // modern convenience constructor; harvest sampled it once.
                    output = new ItemEntity(level, entry.position.x, entry.position.y, entry.position.z, physical,
                            entry.motion.x, entry.motion.y, entry.motion.z);
                } else {
                    output = new ItemEntity(level, entry.position.x, entry.position.y, entry.position.z, physical);
                }
                output.setUUID(entry.id);
                attempt.entity = output;
                output.setDeltaMovement(entry.motion);
                output.setPickUpDelay(entry.pickupDelay);
                if (entry.fireTicks > 0) output.setRemainingFireTicks(entry.fireTicks);
                beforeSpawn = RejectedEntitySpawn.snapshotBefore(level, output);
            } catch (RuntimeException exception) {
                pending.addLast(entry.retryAt(level.getGameTime() + RETRY_DELAY));
                setDirty();
                Thaumcraft.LOG.warn("Inventory output preparation failed at {}; retaining pending items", entry.source, exception);
                continue;
            }
            boolean nativeAdded = false;
            try {
                if (entry.bore == null || BoreNativeContext.canDispatchOutputs(level) && level.getDataStorage().get(FACTORY, ID) == this
                        && level.hasChunkAt(entry.source) && level.hasChunkAt(entry.bore.target)
                        && level.hasChunkAt(BlockPos.containing(entry.position))) nativeAdded = level.addFreshEntity(output);
            } catch (RuntimeException exception) {
                Thaumcraft.LOG.warn("Inventory output spawn failed at {}; retaining pending items", entry.source, exception);
            }
            attempt.reconcile();
            if (attempt.accepted || nativeAdded || level.getEntity(entry.id) == output) {
                // Admission or a real native kill/pickup is final even if a later
                // hook throws/moves/changes the item. Never recreate the old stack.
                attempt.accept();
                if (attempt.destroyedBeforeAdmission && output.isRemoved()) {
                    // A hook killed it before vanilla installed its callback.
                    // Run native cleanup now that admission installed that callback;
                    // the deliberately destroyed paid item is never recreated.
                    attempt.cleanup = true;
                    try {
                        if (managed(level, output)) output.discard();
                        RejectedEntitySpawn.releaseRejected(level, output, beforeSpawn, nativeAdded);
                    } catch (RuntimeException exception) { Thaumcraft.LOG.warn("Destroyed paid item cleanup failed at {}", entry.source, exception); }
                }
            } else {
                try {
                    attempt.cleanup = true;
                    output.discard();
                    // Fabric Architectury ADD cancellation occurs after UUID
                    // reservation but before the entity receives its removal
                    // callback. Release only that unused, newly reserved UUID;
                    // a visible or hidden owner always keeps its reservation.
                    RejectedEntitySpawn.releaseRejected(level, output, beforeSpawn, nativeAdded);
                } catch (RuntimeException exception) {
                    Thaumcraft.LOG.warn("Inventory output removal failed at {}; retaining pending items", entry.source, exception);
                } finally {
                    pending.addLast(entry.retryAt(level.getGameTime() + RETRY_DELAY));
                    setDirty();
                }
            }
            } finally { ACTIVE.get().remove(entry.id); inFlight = null; }
        }
        } finally { flushing = false; }
    }

    private List<Pending> ownedRows() {
        if (inFlight != null) inFlight.reconcile();
        var owned = new ArrayList<Pending>();
        var ids = new HashSet<UUID>();
        for (Pending entry : pending) if (!settled.contains(entry.id) && ids.add(entry.id)) owned.add(entry);
        if (inFlight != null && !inFlight.accepted && !settled.contains(inFlight.entry.id) && ids.add(inFlight.entry.id)) owned.add(inFlight.entry);
        return owned;
    }
    public int pendingCount() { return ownedRows().size(); }
    public int quarantinedCount() { return quarantined.size() + boreRetirements.size(); }
    public List<UUID> pendingIds() {
        List<UUID> result = new ArrayList<>();
        for (Pending entry : ownedRows()) result.add(entry.id);
        return List.copyOf(result);
    }

    public static DeferredItemDrops load(CompoundTag tag, HolderLookup.Provider registries) {
        var data = new DeferredItemDrops();
        if (!tag.contains("Version", Tag.TAG_INT) || tag.getInt("Version") < 1 || tag.getInt("Version") > VERSION
                || tag.getInt("Version") < 3 && tag.contains("BoreJournals")) {
            // Keep unknown data recoverable instead of overwriting it with an
            // empty queue when vanilla catches a SavedData reader exception.
            data.quarantined.add(tag.copy());
            Thaumcraft.LOG.error("Unsupported pending inventory-drop data version {}; data retained in quarantine", tag.getInt("Version"));
            return data;
        }
        for (String name : List.of("Pending", "Settled", "Quarantined", "BoreJournals")) {
            if (tag.contains(name) && (!(tag.get(name) instanceof ListTag rows) || !rows.isEmpty() && rows.getElementType() != Tag.TAG_COMPOUND)) {
                data.quarantined.add(tag.copy());
                Thaumcraft.LOG.error("Malformed pending-drop collection retained whole: {}", name);
                return data;
            }
        }
        for (Tag entry : tag.getList("Quarantined", Tag.TAG_COMPOUND)) data.quarantined.add(entry.copy());
        for (Tag raw : tag.getList("Settled", Tag.TAG_COMPOUND)) {
            var row = (CompoundTag) raw;
            try { data.settled.add(row.getUUID("Id")); }
            catch (RuntimeException malformed) { data.quarantined.add(row.copy()); }
        }
        // Reserve known Bore identities BEFORE classifying any Pending row.
        // Even a malformed/quarantined journal cannot let its UUID downgrade
        // to legacy output merely by stripping the typed metadata.
        Set<UUID> reservedBoreIds = new HashSet<>();
        for (Tag raw : tag.getList("BoreJournals", Tag.TAG_COMPOUND)) {
            for (Tag candidate : ((CompoundTag) raw).getList("Rows", Tag.TAG_COMPOUND)) {
                CompoundTag row = (CompoundTag) candidate;
                if (row.hasUUID("Id")) reservedBoreIds.add(row.getUUID("Id"));
            }
        }
        Set<UUID> blockedTransactions = new HashSet<>(); Map<UUID, UUID> candidateOwners = new HashMap<>();
        for (Tag raw : tag.getList("BoreJournals", Tag.TAG_COMPOUND)) {
            CompoundTag row = ((CompoundTag) raw).copy();
            try {
                validateBoreJournal(row, registries);
                UUID id = row.getUUID("Id");
                if (blockedTransactions.contains(id)) throw new IllegalArgumentException("Blocked duplicate Bore transaction");
                if (data.boreJournals.containsKey(id)) {
                    blockedTransactions.add(id); data.quarantined.add(data.boreJournals.remove(id).copy());
                    throw new IllegalArgumentException("Duplicate Bore transaction");
                }
                for (Tag candidate : row.getList("Rows", Tag.TAG_COMPOUND)) {
                    UUID output = ((CompoundTag) candidate).getUUID("Id"); UUID old = candidateOwners.putIfAbsent(output, id);
                    if (old != null && !old.equals(id)) {
                        blockedTransactions.add(id); blockedTransactions.add(old);
                        CompoundTag conflicting = data.boreJournals.remove(old);
                        if (conflicting != null) data.quarantined.add(conflicting.copy());
                        throw new IllegalArgumentException("Cross-journal duplicate output UUID");
                    }
                }
                // A saved callback has no future native-return witness.
                if (row.getString("Phase").equals("ARMED")) row.putString("Phase", "UNKNOWN");
                data.boreJournals.put(id, row);
            } catch (RuntimeException malformed) { data.quarantined.add(row.copy()); }
        }
        for (Tag raw : tag.getList("Pending", Tag.TAG_COMPOUND)) {
            var row = (CompoundTag) raw;
            try {
                UUID id = row.getUUID("Id");
                if (reservedBoreIds.contains(id) && !row.contains("Bore", Tag.TAG_COMPOUND))
                    throw new IllegalArgumentException("Reserved Bore UUID cannot downgrade to legacy output");
                if (!row.contains("Source", Tag.TAG_ANY_NUMERIC)) throw new IllegalArgumentException("Missing drop source");
                int count = row.getInt("Count");
                int pickupDelay = row.getInt("PickupDelay"); // Old inventory records default to their original0.
                ItemStack stack = ItemStack.parse(registries, row.getCompound("Stack")).orElseThrow();
                Vec3 position = vector(row, "X", "Y", "Z");
                Vec3 motion = vector(row, "DX", "DY", "DZ");
                int crystalFace = row.contains("CrystalFace") ? row.getInt("CrystalFace") : -1;
                int fireTicks = row.contains("Fire") ? row.getInt("Fire") : 0; // Only a Fire golem's fishing catch is ever saved alight.
                if (row.contains("Fire") && (!row.contains("Fire", Tag.TAG_INT) || fireTicks <= 0 || fireTicks > MAX_FIRE_TICKS
                        || pickupDelay != 20 || row.contains("CrystalFace") || row.contains("Routing") || row.contains("Bore")))
                    throw new IllegalArgumentException("Invalid burning flag on a pending drop");
                if (count <= 0 || pickupDelay < 0 || pickupDelay > 32767 || stack.isEmpty() || data.pending.stream().anyMatch(entry -> entry.id.equals(id))) {
                    throw new IllegalArgumentException("Invalid pending stack/count/duplicate ID");
                }
                stack.setCount(count);
                BlockPos source = BlockPos.of(row.getLong("Source"));
                BoreDrop bore = null;
                if (row.contains("Bore")) {
                    if (tag.getInt("Version") < 3 || row.contains("CrystalFace") || row.contains("Routing") || pickupDelay != 0
                            || !row.contains("Bore", Tag.TAG_COMPOUND) || !row.contains("Count", Tag.TAG_INT)
                            || count > 64 || !row.contains("Source", Tag.TAG_LONG) || !row.contains("Stack", Tag.TAG_COMPOUND)
                            || !stack.copyWithCount(1).save(registries).equals(row.getCompound("Stack"))) throw new IllegalArgumentException("Invalid Bore output");
                    CompoundTag metadata = row.getCompound("Bore");
                    CompoundTag journal = data.boreJournals.get(metadata.getUUID("Transaction"));
                    if (journal == null || !Set.of("WITNESSED", "EVACUATED").contains(journal.getString("Phase")))
                        throw new IllegalArgumentException("Unwitnessed Bore candidate is NOT output");
                    boolean matched = false;
                    for (Tag candidate : journal.getList("Rows", Tag.TAG_COMPOUND)) if (candidate.equals(row)) { matched = true; break; }
                    if (!matched) throw new IllegalArgumentException("Bore row differs from its exact owned candidate");
                    bore = readBoreDrop(metadata);
                }
                if (row.contains("CrystalFace")) {
                    if (!row.contains("CrystalFace", Tag.TAG_INT) || !row.contains("Count", Tag.TAG_INT) || !row.contains("Source", Tag.TAG_LONG)
                            || !row.contains("Stack", Tag.TAG_COMPOUND) || row.contains("PickupDelay") && !row.contains("PickupDelay", Tag.TAG_INT)
                            || crystalFace < 0 || crystalFace > 5 || pickupDelay != 0 || !producedCrystal(stack)) throw new IllegalArgumentException("Invalid paid crystal record");
                    if (!stack.save(registries).equals(row.getCompound("Stack"))) throw new IllegalArgumentException("Noncanonical or discarded paid crystal components");
                    Direction out = Direction.from3DDataValue(crystalFace).getOpposite();
                    Vec3 expectedPos = new Vec3(source.getX() + .5 + out.getStepX() * .65, source.getY() + .5 + out.getStepY() * .65, source.getZ() + .5 + out.getStepZ() * .65);
                    Vec3 expectedMotion = new Vec3(out.getStepX() * .04F, out.getStepY() * .04F, out.getStepZ() * .04F);
                    if (!position.equals(expectedPos) || !motion.equals(expectedMotion) || row.contains("Routing") && (!row.contains("Routing", Tag.TAG_BYTE) || row.getByte("Routing") < 0 || row.getByte("Routing") > 1)) throw new IllegalArgumentException("Invalid crystal geometry/route state");
                } else if (row.contains("Routing")) throw new IllegalArgumentException("Routing without a paid crystal");
                // Saved ROUTING/IN_FLIGHT never replays inventory. The same paid
                // UUID becomes a residual physical delivery, unless acknowledged.
                if (!data.settled.contains(id)) data.pending.addLast(new Pending(id, source, position, motion, stack, pickupDelay, Long.MIN_VALUE, crystalFace, false, bore, fireTicks));
            } catch (RuntimeException exception) {
                data.quarantined.add(row.copy());
                Thaumcraft.LOG.error("Invalid pending inventory drop retained in quarantine", exception);
            }
        }
        return data;
    }

    private static Vec3 vector(CompoundTag row, String x, String y, String z) {
        if (!row.contains(x, Tag.TAG_ANY_NUMERIC) || !row.contains(y, Tag.TAG_ANY_NUMERIC) || !row.contains(z, Tag.TAG_ANY_NUMERIC)) {
            throw new IllegalArgumentException("Missing pending drop vector");
        }
        var value = new Vec3(row.getDouble(x), row.getDouble(y), row.getDouble(z));
        if (!Double.isFinite(value.x) || !Double.isFinite(value.y) || !Double.isFinite(value.z)) {
            throw new IllegalArgumentException("Non-finite pending drop vector");
        }
        return value;
    }

    private static BoreDrop readBoreDrop(CompoundTag tag) {
        if (!tag.hasUUID("Transaction") || !tag.contains("Target", Tag.TAG_LONG) || !tag.contains("Evacuation", Tag.TAG_BYTE)
                || tag.getByte("Evacuation") < 0 || tag.getByte("Evacuation") > 1
                || !tag.contains("Nozzle", Tag.TAG_INT) || !tag.contains("Attachment", Tag.TAG_INT)
                || !tag.contains("Member", Tag.TAG_INT) || tag.getInt("Member") < 0
                || !tag.contains("Segment", Tag.TAG_INT) || tag.getInt("Segment") < 0) throw new IllegalArgumentException("Malformed Bore geometry metadata");
        boolean evacuation = tag.getBoolean("Evacuation"); int nozzle = tag.getInt("Nozzle"), attachment = tag.getInt("Attachment");
        if (evacuation ? nozzle != -1 || attachment != -1 : nozzle < 0 || nozzle > 5 || attachment < 0 || attachment > 1)
            throw new IllegalArgumentException("Unsupported Bore directions");
        return new BoreDrop(tag.getUUID("Transaction"), BlockPos.of(tag.getLong("Target")), nozzle, attachment, evacuation, tag.getInt("Member"), tag.getInt("Segment"));
    }
    private static void validateBoreJournal(CompoundTag tag, HolderLookup.Provider registries) {
        if (!tag.contains("BoreJournalVersion", Tag.TAG_INT) || tag.getInt("BoreJournalVersion") != 1
                || !tag.hasUUID("Id") || !tag.hasUUID("Machine") || !tag.hasUUID("Lifetime")
                || !tag.contains("Source", Tag.TAG_LONG) || !tag.contains("Revision", Tag.TAG_LONG) || tag.getLong("Revision") < 0
                || !tag.contains("Sequence", Tag.TAG_LONG) || tag.getLong("Sequence") < 0 || !tag.contains("CreditBits", Tag.TAG_INT)
                || !Float.isFinite(Float.intBitsToFloat(tag.getInt("CreditBits"))) || Float.intBitsToFloat(tag.getInt("CreditBits")) < -1
                || Float.intBitsToFloat(tag.getInt("CreditBits")) > 40 || !tag.contains("Phase", Tag.TAG_STRING)
                || !Set.of("ARMED", "WITNESSED", "UNKNOWN", "EVACUATED").contains(tag.getString("Phase"))
                || !tag.contains("Fenced", Tag.TAG_BYTE) || tag.getByte("Fenced") < 0 || tag.getByte("Fenced") > 1
                || tag.getBoolean("Fenced") != Set.of("ARMED", "UNKNOWN").contains(tag.getString("Phase"))
                || !tag.contains("Rows", Tag.TAG_LIST) || !(tag.get("Rows") instanceof ListTag rows)
                || !rows.isEmpty() && rows.getElementType() != Tag.TAG_COMPOUND) throw new IllegalArgumentException("Malformed Bore journal");
        boolean evacuation = tag.getString("Mode").equals("EVACUATE");
        if (!evacuation && !tag.getString("Mode").equals("HARVEST") || evacuation != tag.getString("Phase").equals("EVACUATED"))
            throw new IllegalArgumentException("Bore journal mode/phase mismatch");
        if (!evacuation && (!tag.contains("Target", Tag.TAG_LONG) || !tag.contains("OldState", Tag.TAG_COMPOUND)
                || !tag.contains("Nozzle", Tag.TAG_INT) || !tag.contains("Attachment", Tag.TAG_INT)
                || tag.getInt("Nozzle") < 0 || tag.getInt("Nozzle") > 5 || tag.getInt("Attachment") < 0 || tag.getInt("Attachment") > 1
                || !tag.contains("PickBefore", Tag.TAG_COMPOUND) || !tag.contains("PickAfter", Tag.TAG_COMPOUND))) throw new IllegalArgumentException("Missing Bore reservation evidence");
        if (!evacuation) {
            var state = NbtUtils.readBlockState(registries.lookupOrThrow(Registries.BLOCK), tag.getCompound("OldState"));
            if (state.isAir() || state.hasBlockEntity() || !NbtUtils.writeBlockState(state).equals(tag.getCompound("OldState")))
                throw new IllegalArgumentException("Discarded Bore old-state evidence");
            ItemStack before = ItemStack.parse(registries, tag.getCompound("PickBefore")).orElseThrow();
            ItemStack after = ItemStack.parse(registries, tag.getCompound("PickAfter")).orElseThrow();
            if (before.isEmpty() || after.isEmpty() || before.getCount() != 1 || after.getCount() != 1 || before.getItem() != after.getItem()
                    || !before.save(registries).equals(tag.getCompound("PickBefore")) || !after.save(registries).equals(tag.getCompound("PickAfter")))
                throw new IllegalArgumentException("Discarded Bore wear reservation evidence");
        }
        if (tag.contains("RejectedOwner") && !tag.contains("RejectedOwner", Tag.TAG_COMPOUND)) throw new IllegalArgumentException("Malformed Bore rejected-owner custody");
        if (!tag.contains("OriginalBundle", Tag.TAG_LIST) || !(tag.get("OriginalBundle") instanceof ListTag originals)
                || !originals.isEmpty() && originals.getElementType() != Tag.TAG_COMPOUND
                || originals.size() > MAX_BORE_EJECTION_SEGMENTS || tag.getList("Rows", Tag.TAG_COMPOUND).size() > MAX_BORE_EJECTION_SEGMENTS)
            throw new IllegalArgumentException("Malformed Bore original bundle/segment count");
        long originalTotal = 0; long[] memberCounts = new long[originals.size()]; int[] nextSegment = new int[originals.size()];
        for (int index = 0; index < originals.size(); index++) {
            CompoundTag member = originals.getCompound(index);
            if (!member.contains("Member", Tag.TAG_INT) || member.getInt("Member") != index || !member.contains("Count", Tag.TAG_INT)
                    || member.getInt("Count") <= 0 || !member.contains("Stack", Tag.TAG_COMPOUND)) throw new IllegalArgumentException("Malformed original Bore member");
            ItemStack unit = ItemStack.parse(registries, member.getCompound("Stack")).orElseThrow();
            if (unit.isEmpty() || unit.getCount() != 1 || !unit.save(registries).equals(member.getCompound("Stack"))) throw new IllegalArgumentException("Discarded original Bore components");
            originalTotal = Math.addExact(originalTotal, member.getInt("Count"));
            if (originalTotal > Integer.MAX_VALUE) throw new IllegalArgumentException("Unrepresentable original Bore bundle total");
        }
        Set<UUID> ids = new HashSet<>(); long total = 0; int lastMember = -1;
        for (Tag raw : tag.getList("Rows", Tag.TAG_COMPOUND)) {
            CompoundTag row = (CompoundTag) raw;
            if (!row.hasUUID("Id") || !ids.add(row.getUUID("Id")) || !row.contains("Source", Tag.TAG_LONG)
                    || row.getLong("Source") != tag.getLong("Source") || !row.contains("Count", Tag.TAG_INT) || row.getInt("Count") <= 0 || row.getInt("Count") > 64
                    || !row.contains("Stack", Tag.TAG_COMPOUND) || !row.contains("Bore", Tag.TAG_COMPOUND)
                    || row.contains("CrystalFace") || row.contains("Routing") || row.contains("PickupDelay")) throw new IllegalArgumentException("Malformed Bore candidate row");
            ItemStack unit = ItemStack.parse(registries, row.getCompound("Stack")).orElseThrow();
            if (unit.isEmpty() || unit.getCount() != 1 || !unit.save(registries).equals(row.getCompound("Stack"))) throw new IllegalArgumentException("Discarded Bore candidate components");
            total = Math.addExact(total, row.getInt("Count"));
            if (total > Integer.MAX_VALUE) throw new IllegalArgumentException("Unrepresentable Bore candidate total");
            BoreDrop bore = readBoreDrop(row.getCompound("Bore"));
            if (!bore.transaction.equals(tag.getUUID("Id")) || bore.evacuation != evacuation) throw new IllegalArgumentException("Cross-journal Bore row");
            if (bore.member >= originals.size() || bore.member < lastMember || bore.member > lastMember + 1
                    || bore.segment != nextSegment[bore.member]++
                    || !row.getCompound("Stack").equals(originals.getCompound(bore.member).getCompound("Stack"))) throw new IllegalArgumentException("Bore segment/member identity mismatch");
            long segmentCount = Math.min(64L, (long) originals.getCompound(bore.member).getInt("Count") - 64L * bore.segment);
            if (segmentCount <= 0 || row.getInt("Count") != segmentCount) throw new IllegalArgumentException("Noncanonical Bore segment size");
            lastMember = bore.member;
            memberCounts[bore.member] = Math.addExact(memberCounts[bore.member], row.getInt("Count"));
            Vec3 position = vector(row, "X", "Y", "Z"), motion = vector(row, "DX", "DY", "DZ");
            if (!evacuation && (bore.target.asLong() != tag.getLong("Target") || bore.nozzle != tag.getInt("Nozzle")
                    || bore.attachment != tag.getInt("Attachment") || !position.equals(borePosition(BlockPos.of(tag.getLong("Source")),
                        Direction.from3DDataValue(bore.nozzle), Direction.from3DDataValue(bore.attachment)))
                    || !motion.equals(boreMotion(Direction.from3DDataValue(bore.nozzle))))) throw new IllegalArgumentException("Bore output geometry mismatch");
        }
        if (total != originalTotal) throw new IllegalArgumentException("Bore segments changed whole-bundle mass");
        for (int member = 0; member < originals.size(); member++) if (memberCounts[member] != originals.getCompound(member).getInt("Count"))
            throw new IllegalArgumentException("Bore segments changed an original member's count");
    }

    private static CompoundTag writePending(Pending entry, HolderLookup.Provider registries) {
        var row = new CompoundTag(); row.putUUID("Id", entry.id); row.putLong("Source", entry.source.asLong());
        Tag encoded = entry.stack.copyWithCount(1).save(registries);
        row.put("Stack", entry.bore == null ? encoded : encoded.copy()); row.putInt("Count", entry.stack.getCount());
        if (entry.pickupDelay != 0) row.putInt("PickupDelay", entry.pickupDelay);
        if (entry.fireTicks != 0) row.putInt("Fire", entry.fireTicks);
        row.putDouble("X", entry.position.x); row.putDouble("Y", entry.position.y); row.putDouble("Z", entry.position.z);
        row.putDouble("DX", entry.motion.x); row.putDouble("DY", entry.motion.y); row.putDouble("DZ", entry.motion.z);
        if (entry.crystalFace >= 0) { row.putInt("CrystalFace", entry.crystalFace); if (entry.routing) row.putBoolean("Routing", true); }
        if (entry.bore != null) {
            var bore = new CompoundTag(); bore.putUUID("Transaction", entry.bore.transaction); bore.putLong("Target", entry.bore.target.asLong());
            bore.putInt("Nozzle", entry.bore.nozzle); bore.putInt("Attachment", entry.bore.attachment); bore.putBoolean("Evacuation", entry.bore.evacuation);
            bore.putInt("Member", entry.bore.member); bore.putInt("Segment", entry.bore.segment);
            row.put("Bore", bore);
        }
        return row;
    }

    @Override public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        if (saveDepth > 0 && !boreRetirements.isEmpty())
            throw new IllegalStateException("Recursive save while encoding opaque Bore custody");
        long beforeRetirements = retirementRevision;
        ++saveDepth;
        try {
        if (inFlight != null) inFlight.reconcile();
        tag.putInt("Version", VERSION);
        ListTag rows = new ListTag();
        for (Pending entry : ownedRows()) {
            rows.add(writePending(entry, registries));
        }
        tag.put("Pending", rows);
        ListTag receipts = new ListTag();
        for (UUID id : settled) { var row = new CompoundTag(); row.putUUID("Id", id); receipts.add(row); }
        tag.put("Settled", receipts);
        ListTag journals = new ListTag(); for (CompoundTag journal : boreJournals.values()) journals.add(journal.copy());
        if (!journals.isEmpty()) tag.put("BoreJournals", journals);
        ListTag quarantine = quarantined.copy();
        for (BoreRetirementCustody owner : List.copyOf(boreRetirements)) quarantine.add(owner.checkpoint(registries));
        if (retirementRevision != beforeRetirements) {
            setDirty();
            throw new IllegalStateException("New Bore custody arrived during queue save; retry whole checkpoint");
        }
        tag.put("Quarantined", quarantine);
        return tag;
        } finally { --saveDepth; }
    }
}
