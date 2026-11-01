package thaumcraft.common.lib.utils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.WeakHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.level.entity.EntityInLevelCallback;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.phys.Vec3;
import thaumcraft.common.Thaumcraft;

/** Paid native XP custody. Preparation never emits; a source commits beside its raw debit.
 * UUID admission receipts prevent retrying already consumed/merged XP, including a stale live queue reload. */
public final class ExperienceOrbQueue extends SavedData {
    public static final String ID = "thaumcraft_pending_experience_orbs";
    private static final Factory<ExperienceOrbQueue> FACTORY = new Factory<>(ExperienceOrbQueue::new, ExperienceOrbQueue::load, null);
    private static final ThreadLocal<Map<UUID, Attempt>> ACTIVE = ThreadLocal.withInitial(HashMap::new);
    private static final Map<ServerLevel, Set<UUID>> RECEIPTS = java.util.Collections.synchronizedMap(new WeakHashMap<>());
    private final Map<UUID, Pending> pending = new LinkedHashMap<>();
    private final Set<UUID> settled = new HashSet<>();
    private final ListTag rejected = new ListTag();
    private boolean flushing;
    private record Pending(UUID id, Vec3 position, Vec3 motion, int value, long retryAt) {}
    private static final class Attempt {
        final ExperienceOrbQueue queue; final ServerLevel level; final Pending row;
        ExperienceOrb entity; boolean admitted, cleanup, destructionRequested;
        Attempt(ExperienceOrbQueue queue, ServerLevel level, Pending row) { this.queue = queue; this.level = level; this.row = row; }
        void accept() {
            if (admitted) return;
            admitted = true; queue.pending.remove(row.id); queue.settled.add(row.id);
            RECEIPTS.computeIfAbsent(level, ignored -> new HashSet<>()).add(row.id); queue.setDirty();
        }
    }
    public ExperienceOrbQueue() {}
    private static boolean live(ServerLevel level) {
        return level.getServer().isSameThread() && level.getServer().getLevel(level.dimension()) == level;
    }
    private static boolean finite(Vec3 vector) { return vector != null && Double.isFinite(vector.lengthSqr()); }
    private static boolean valid(Pending row) { return row.id != null && finite(row.position) && finite(row.motion) && row.value > 0 && row.value <= Short.MAX_VALUE; }
    public static final class Prepared {
        private final ExperienceOrbQueue queue; private final ServerLevel level; private final List<Pending> rows;
        private boolean committed;
        private Prepared(ExperienceOrbQueue queue, ServerLevel level, List<Pending> rows) { this.queue = queue; this.level = level; this.rows = List.copyOf(rows); }
        public boolean ready() { return !committed && live(level) && level.getDataStorage().get(FACTORY, ID) == queue; }
        /** Copies, codecs, storage loading and authority observations have already finished. */
        public void commitRaw() {
            if (committed) return;
            committed = true;
            for (Pending row : rows) queue.pending.put(row.id, row);
            queue.setDirty();
        }
    }
    public static Prepared prepare(ServerLevel level, Vec3 position, Vec3 motion, int amount) {
        if (!live(level) || !finite(position) || !finite(motion) || amount < 0)
            throw new IllegalArgumentException("Canonical native level and finite paid XP required");
        var rows = new ArrayList<Pending>();
        for (int remaining = amount; remaining > 0;) {
            int value = ExperienceOrb.getExperienceValue(remaining); remaining -= value;
            rows.add(new Pending(UUID.randomUUID(), position, motion, value, Long.MIN_VALUE));
        }
        var queue = level.getDataStorage().computeIfAbsent(FACTORY, ID);
        return new Prepared(queue, level, rows);
    }
    public static void observeAdmission(Entity entity, EntityInLevelCallback callback) {
        Attempt attempt = ACTIVE.get().get(entity.getUUID());
        if (attempt != null && attempt.entity == entity && !attempt.cleanup && callback != EntityInLevelCallback.NULL) attempt.accept();
    }
    public static void observeRemoval(Entity entity, Entity.RemovalReason reason) {
        Attempt attempt = ACTIVE.get().get(entity.getUUID());
        if (attempt != null && attempt.entity == entity && !attempt.cleanup && reason.shouldDestroy()) attempt.destructionRequested = true;
    }
    public static void confirmRemoval(Entity entity) {
        Attempt attempt = ACTIVE.get().get(entity.getUUID());
        if (attempt != null && attempt.entity == entity && !attempt.cleanup && attempt.destructionRequested && entity.isRemoved()) attempt.accept();
    }
    public static boolean allowPickup(ExperienceOrb entity) {
        Attempt attempt = ACTIVE.get().get(entity.getUUID());
        if (attempt == null || attempt.entity != entity) return true;
        if (attempt.level.getEntity(entity.getUUID()) != entity) return false;
        attempt.accept(); return true;
    }
    public static void tick(ServerLevel level) {
        if (!live(level)) throw new IllegalStateException("Paid XP delivery requires the native server thread");
        var queue = level.getDataStorage().get(FACTORY, ID);
        if (queue == null || queue.flushing) return;
        queue.flushing = true;
        try {
            int attempted = 0;
            for (Pending row : List.copyOf(queue.pending.values())) {
                if (level.getDataStorage().get(FACTORY, ID) != queue) return;
                if (ACTIVE.get().containsKey(row.id)) continue;
                if (queue.settled.contains(row.id) || RECEIPTS.getOrDefault(level, Set.of()).contains(row.id)) {
                    queue.pending.remove(row.id); queue.settled.add(row.id); queue.setDirty(); continue;
                }
                BlockPos pos = BlockPos.containing(row.position);
                if (level.isOutsideBuildHeight(pos)) { queue.reject(row); continue; }
                if (!level.hasChunkAt(pos) || level.getGameTime() < row.retryAt) continue;
                if (++attempted > 64) break;
                var existing = level.getEntity(row.id);
                if (existing != null) {
                    if (existing instanceof ExperienceOrb orb && orb.getValue() == row.value) new Attempt(queue, level, row).accept();
                    else queue.reject(row);
                    continue;
                }
                var attempt = new Attempt(queue, level, row);
                RejectedEntitySpawn.Snapshot before = null;
                boolean added = false;
                try {
                    var orb = new ExperienceOrb(level, row.position.x, row.position.y, row.position.z, row.value);
                    orb.setUUID(row.id); orb.setDeltaMovement(row.motion);
                    attempt.entity = orb; ACTIVE.get().put(row.id, attempt);
                    before = RejectedEntitySpawn.snapshotBefore(level, orb);
                    added = level.addFreshEntity(orb);
                    if (!attempt.admitted && (added || level.getEntity(row.id) == orb)) attempt.accept();
                } catch (RuntimeException | Error failure) {
                    if (!attempt.admitted && attempt.entity != null && level.getEntity(row.id) == attempt.entity) attempt.accept();
                    Thaumcraft.LOG.warn("Paid XP emission failed at {}; custody retained", pos, failure);
                } finally {
                    if (attempt.admitted && attempt.entity != null && attempt.entity.isRemoved()) {
                        attempt.cleanup = true;
                        try {
                            // A hook may destroy the candidate before native
                            // admission installs its section removal callback.
                            attempt.entity.discard();
                            if (before != null) RejectedEntitySpawn.releaseRejected(level, attempt.entity, before, added);
                        } catch (RuntimeException | Error failure) { Thaumcraft.LOG.warn("Consumed paid XP cleanup failed", failure); }
                    }
                    if (!attempt.admitted) {
                        attempt.cleanup = true;
                        try {
                            if (attempt.entity != null) {
                                attempt.entity.discard();
                                if (before != null) RejectedEntitySpawn.releaseRejected(level, attempt.entity, before, added);
                            }
                        } catch (RuntimeException | Error failure) { Thaumcraft.LOG.warn("Rejected XP cleanup failed; custody retained", failure); }
                        queue.pending.put(row.id, new Pending(row.id, row.position, row.motion, row.value, level.getGameTime() + 20)); queue.setDirty();
                    }
                    ACTIVE.get().remove(row.id);
                }
            }
        } finally { queue.flushing = false; }
    }
    private void reject(Pending row) { rejected.add(write(row)); pending.remove(row.id); setDirty(); }
    public static int pendingCount(ServerLevel level) { var queue = level.getDataStorage().get(FACTORY, ID); return queue == null ? 0 : queue.pending.size(); }
    public static CompoundTag snapshot(ServerLevel level) { var queue = level.getDataStorage().get(FACTORY, ID); return queue == null ? new CompoundTag() : queue.save(new CompoundTag(), level.registryAccess()); }
    private static CompoundTag write(Pending row) {
        var tag = new CompoundTag(); tag.putUUID("Id", row.id); tag.putInt("Value", row.value); tag.putLong("RetryAt", row.retryAt);
        tag.putDouble("X", row.position.x); tag.putDouble("Y", row.position.y); tag.putDouble("Z", row.position.z);
        tag.putDouble("DX", row.motion.x); tag.putDouble("DY", row.motion.y); tag.putDouble("DZ", row.motion.z); return tag;
    }
    @Override public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        // A callback may install an older decoded queue and save it before its
        // first tick. Native admission receipts belong to the level lifetime,
        // so that checkpoint must include them too, without retaining a level.
        List<Map.Entry<ServerLevel, Set<UUID>>> liveReceipts;
        synchronized (RECEIPTS) { liveReceipts = List.copyOf(RECEIPTS.entrySet()); }
        for (var entry : liveReceipts) {
            var level = entry.getKey();
            if (level != null && live(level) && level.getDataStorage().get(FACTORY, ID) == this) settled.addAll(entry.getValue());
        }
        tag.putInt("Version", 1);
        var rows = new ListTag(); for (Pending row : pending.values()) if (!settled.contains(row.id)) rows.add(write(row)); tag.put("Pending", rows);
        var receipts = new ListTag(); for (UUID id : settled) { var receipt = new CompoundTag(); receipt.putUUID("Id", id); receipts.add(receipt); }
        tag.put("Settled", receipts); tag.put("Rejected", rejected.copy()); return tag;
    }
    /** Unknown/malformed custody remains opaque. A detached decoded queue cannot emit until installed in native SavedData. */
    public static ExperienceOrbQueue load(CompoundTag tag, HolderLookup.Provider registries) {
        var queue = new ExperienceOrbQueue();
        if (!tag.contains("Version", Tag.TAG_INT) || tag.getInt("Version") != 1) { queue.rejected.add(tag.copy()); return queue; }
        for (String key : List.of("Pending", "Settled", "Rejected")) {
            if (tag.contains(key) && (!(tag.get(key) instanceof ListTag list) || !list.isEmpty() && list.getElementType() != Tag.TAG_COMPOUND)) {
                queue.rejected.add(tag.copy()); return queue;
            }
        }
        queue.rejected.addAll(tag.getList("Rejected", Tag.TAG_COMPOUND).copy());
        for (Tag raw : tag.getList("Settled", Tag.TAG_COMPOUND)) {
            var row = (CompoundTag) raw;
            if (row.hasUUID("Id")) queue.settled.add(row.getUUID("Id")); else queue.rejected.add(row.copy());
        }
        var seen = new HashSet<UUID>(); var originals = new HashMap<UUID, CompoundTag>();
        for (Tag raw : tag.getList("Pending", Tag.TAG_COMPOUND)) {
            var row = (CompoundTag) raw;
            try {
                if (!row.hasUUID("Id")) throw new IllegalArgumentException("Missing paid XP identity");
                UUID id = row.getUUID("Id");
                if (!seen.add(id)) {
                    var first = originals.remove(id); if (first != null) queue.rejected.add(first);
                    queue.pending.remove(id); throw new IllegalArgumentException("Ambiguous paid XP identity");
                }
                for (String key : List.of("X", "Y", "Z", "DX", "DY", "DZ")) if (!row.contains(key, Tag.TAG_DOUBLE)) throw new IllegalArgumentException("Invalid XP vector");
                if (!row.contains("Value", Tag.TAG_INT) || !row.contains("RetryAt", Tag.TAG_LONG)) throw new IllegalArgumentException("Invalid paid XP value");
                var value = new Pending(id, new Vec3(row.getDouble("X"), row.getDouble("Y"), row.getDouble("Z")),
                        new Vec3(row.getDouble("DX"), row.getDouble("DY"), row.getDouble("DZ")), row.getInt("Value"), Long.MIN_VALUE);
                if (!valid(value)) throw new IllegalArgumentException("Malformed paid XP");
                originals.put(id, row.copy()); if (!queue.settled.contains(id)) queue.pending.put(id, value);
            } catch (RuntimeException invalid) { queue.rejected.add(row.copy()); }
        }
        return queue;
    }
}
