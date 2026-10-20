package thaumcraft.common.lib.utils;

import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
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
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.phys.Vec3;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.common.Thaumcraft;
import thaumcraft.common.entities.EntityAspectOrb;
import thaumcraft.common.tiles.TileNode;

/** Closed one-vis UNSTABLE emission custody. Spawn callbacks never run during the node debit. */
public final class AspectOrbQueue extends SavedData {
    public static final String ID = "thaumcraft_pending_aspect_orbs";
    private static final Factory<AspectOrbQueue> FACTORY = new Factory<>(AspectOrbQueue::new, AspectOrbQueue::load, null);
    private static final ThreadLocal<Prepared> RAW = new ThreadLocal<>();
    private static final ThreadLocal<Map<UUID, Attempt>> ACTIVE = ThreadLocal.withInitial(HashMap::new);
    private static final Map<ServerLevel, Set<UUID>> RECEIPTS = new WeakHashMap<>();
    private final Map<UUID, Pending> pending = new LinkedHashMap<>();
    private final Set<UUID> settled = new HashSet<>();
    private final ListTag rejected = new ListTag();
    private boolean flushing;
    private record Pending(UUID id, Vec3 position, Vec3 motion, float yaw, Aspect aspect, long retryAt) {}
    private static final class Attempt {
        final AspectOrbQueue queue; final ServerLevel level; final Pending row;
        EntityAspectOrb entity; boolean admitted;
        Attempt(AspectOrbQueue queue, ServerLevel level, Pending row) { this.queue = queue; this.level = level; this.row = row; }
        void accept() {
            if (admitted) return;
            admitted = true; queue.pending.remove(row.id); queue.settled.add(row.id);
            RECEIPTS.computeIfAbsent(level, ignored -> new HashSet<>()).add(row.id); queue.setDirty();
        }
    }
    private AspectOrbQueue() {}
    private static boolean live(ServerLevel level) { return level.getServer().isSameThread() && level.getServer().getLevel(level.dimension()) == level; }
    private static boolean finite(Vec3 vector) { return Double.isFinite(vector.x) && Double.isFinite(vector.y) && Double.isFinite(vector.z); }
    private static boolean valid(Pending row) {
        return row.id != null && row.aspect != null && row.aspect.isPrimal() && Aspect.getAspect(row.aspect.getTag()) == row.aspect && finite(row.position) && finite(row.motion)
                && Math.abs(row.position.x) < 30000000 && Math.abs(row.position.z) < 30000000 && Math.abs(row.position.y) < 30000000
                && Math.abs(row.motion.x) <= .200001 && row.motion.y >= 0 && row.motion.y <= .400001 && Math.abs(row.motion.z) <= .200001 && Float.isFinite(row.yaw);
    }
    public static final class Prepared {
        private final AspectOrbQueue queue; private final ServerLevel level; private final Pending row;
        private boolean committed;
        private Prepared(AspectOrbQueue queue, ServerLevel level, Pending row) { this.queue = queue; this.level = level; this.row = row; }
        public boolean isFor(ServerLevel world, BlockPos source, Aspect aspect) { return level == world && row.position.equals(Vec3.atCenterOf(source)) && row.aspect == aspect; }
        public boolean commit(TileNode source) {
            if (committed || RAW.get() != null || !live(level) || queue.flushing || level.getDataStorage().get(FACTORY, ID) != queue
                    || source.getLevel() != level || !source.canCommitOrbEmission(this)) return false;
            // Allocate/install durable custody before the callback-free source debit.
            queue.pending.put(row.id, row); queue.setDirty();
            RAW.set(this);
            try {
                source.commitOrbEmissionRaw(this); committed = true;
                return true;
            } finally {
                if (!committed) queue.pending.remove(row.id);
                RAW.remove();
            }
        }
    }
    /** Prepare storage and original scatter before the source opens its private completion scope. */
    public static Prepared prepare(ServerLevel level, BlockPos source, Aspect aspect) {
        if (!live(level) || level.isOutsideBuildHeight(source) || !level.hasChunkAt(source) || RAW.get() != null) return null;
        var queue = level.getDataStorage().computeIfAbsent(FACTORY, ID);
        if (queue.flushing) return null;
        float yaw = (float)(Math.random() * 360D);
        var row = new Pending(UUID.randomUUID(), Vec3.atCenterOf(source), new Vec3(
                (float)(Math.random() * (double).2F - (double).1F) * 2F,
                (float)(Math.random() * .2D) * 2F,
                (float)(Math.random() * (double).2F - (double).1F) * 2F), yaw, aspect, Long.MIN_VALUE);
        return valid(row) ? new Prepared(queue, level, row) : null;
    }
    public static void requireRaw(Prepared expected) { if (expected == null || RAW.get() != expected) throw new IllegalStateException("No paid native node emission scope"); }

    /** The native section manager calls this AFTER UUID/section admission and
     * BEFORE its onCreated callbacks. Own entity only; no global entity mixin. */
    public static void admitted(EntityAspectOrb entity) {
        Attempt attempt = ACTIVE.get().get(entity.getUUID());
        if (attempt != null && attempt.entity == entity && attempt.level == entity.level()) attempt.accept();
    }
    public static void tick(ServerLevel level) {
        if (!live(level)) throw new IllegalStateException("Aspect orb delivery requires the native server thread");
        AspectOrbQueue queue = level.getDataStorage().get(FACTORY, ID);
        if (queue == null || queue.flushing || RAW.get() != null) return;
        queue.flushing = true;
        try {
            int attempted = 0;
            for (Pending row : java.util.List.copyOf(queue.pending.values())) {
                if (queue.settled.contains(row.id) || RECEIPTS.getOrDefault(level, Set.of()).contains(row.id)) { queue.pending.remove(row.id); queue.settled.add(row.id); queue.setDirty(); continue; }
                BlockPos pos = BlockPos.containing(row.position);
                if (level.isOutsideBuildHeight(pos)) { queue.reject(row, "Outside native build height"); continue; }
                if (!level.hasChunkAt(pos) || level.getGameTime() < row.retryAt || ++attempted > 64) continue;
                var existing = level.getEntity(row.id);
                if (existing != null) {
                    if (existing instanceof EntityAspectOrb orb && orb.wasIssuedAs(row.aspect, 1)) { var accepted = new Attempt(queue, level, row); accepted.accept(); }
                    else queue.reject(row, "UUID is occupied by another owner");
                    continue;
                }
                var attempt = new Attempt(queue, level, row);
                RejectedEntitySpawn.Snapshot beforeSpawn = null;
                boolean added = false;
                try {
                    attempt.entity = EntityAspectOrb.unspawned(level, row.position, row.motion, row.yaw, row.aspect, 1);
                    attempt.entity.setUUID(row.id); ACTIVE.get().put(row.id, attempt);
                    beforeSpawn = RejectedEntitySpawn.snapshotBefore(level, attempt.entity);
                    added = level.addFreshEntity(attempt.entity);
                    if (!attempt.admitted && (added || level.getEntity(row.id) == attempt.entity)) attempt.accept();
                } catch (RuntimeException | Error failure) {
                    if (!attempt.admitted && attempt.entity != null && level.getEntity(row.id) == attempt.entity) attempt.accept();
                    Thaumcraft.LOG.warn("Paid aspect orb delivery failed at {}; ownership retained", pos, failure);
                } finally {
                    ACTIVE.get().remove(row.id);
                    if (!attempt.admitted) {
                        try {
                            if (attempt.entity != null) {
                                attempt.entity.discard();
                                // Fabric Architectury cancels after knownUuids reservation but before
                                // the entity receives its removal callback. Release only this exact
                                // unused reservation, never a pre-existing or hidden native owner.
                                if (beforeSpawn != null) RejectedEntitySpawn.releaseRejected(level, attempt.entity, beforeSpawn, added);
                            }
                        } catch (RuntimeException | Error failure) {
                            Thaumcraft.LOG.warn("Rejected paid aspect orb cleanup failed at {}; ownership retained", pos, failure);
                        } finally {
                            queue.pending.put(row.id, new Pending(row.id, row.position, row.motion, row.yaw, row.aspect, level.getGameTime() + 20)); queue.setDirty();
                        }
                    }
                }
            }
        } finally { queue.flushing = false; }
    }
    private void reject(Pending row, String reason) { CompoundTag tag = write(row); tag.putString("Rejection", reason); rejected.add(tag); pending.remove(row.id); setDirty(); }
    public static int pendingCount(ServerLevel level) { var queue = level.getDataStorage().get(FACTORY, ID); return queue == null ? 0 : queue.pending.size(); }
    public static int rejectedCount(ServerLevel level) { var queue = level.getDataStorage().get(FACTORY, ID); return queue == null ? 0 : queue.rejected.size(); }
    public static CompoundTag snapshot(ServerLevel level) { var queue = level.getDataStorage().get(FACTORY, ID); return queue == null ? new CompoundTag() : queue.save(new CompoundTag(), level.registryAccess()); }
    private static CompoundTag write(Pending row) {
        var tag = new CompoundTag(); tag.putUUID("Id", row.id); tag.putString("Aspect", row.aspect.getTag()); tag.putInt("Value", 1);
        tag.putDouble("X", row.position.x); tag.putDouble("Y", row.position.y); tag.putDouble("Z", row.position.z);
        tag.putDouble("MotionX", row.motion.x); tag.putDouble("MotionY", row.motion.y); tag.putDouble("MotionZ", row.motion.z); tag.putFloat("Yaw", row.yaw); tag.putLong("RetryAt", row.retryAt); return tag;
    }
    @Override public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        tag.putInt("Version", 1); var rows = new ListTag(); for (Pending row : pending.values()) rows.add(write(row)); tag.put("Pending", rows);
        var receipts = new ListTag(); for (UUID id : settled) { var receipt = new CompoundTag(); receipt.putUUID("Id", id); receipts.add(receipt); } tag.put("Settled", receipts); tag.put("Rejected", rejected.copy()); return tag;
    }
    /** Detached decoding never authorizes delivery: tick resolves the actual live dimension storage. */
    public static AspectOrbQueue load(CompoundTag tag, HolderLookup.Provider registries) {
        var queue = new AspectOrbQueue();
        if (tag.getInt("Version") != 1) { queue.rejected.add(tag.copy()); return queue; }
        for (String name : new String[]{"Pending", "Settled", "Rejected"}) {
            if (tag.contains(name) && (!(tag.get(name) instanceof ListTag list) || !list.isEmpty() && list.getElementType() != Tag.TAG_COMPOUND)) { queue.rejected.add(tag.copy()); return queue; }
        }
        queue.rejected.addAll(tag.getList("Rejected", Tag.TAG_COMPOUND).copy());
        for (Tag value : tag.getList("Settled", Tag.TAG_COMPOUND)) {
            var row = (CompoundTag)value;
            if (row.hasUUID("Id")) queue.settled.add(row.getUUID("Id")); else queue.rejected.add(row.copy());
        }
        Set<UUID> seen = new HashSet<>();
        Map<UUID, CompoundTag> originals = new HashMap<>();
        for (Tag value : tag.getList("Pending", Tag.TAG_COMPOUND)) {
            var row = (CompoundTag)value;
            try {
                if (row.hasUUID("Id") && !seen.add(row.getUUID("Id"))) {
                    UUID id = row.getUUID("Id");
                    CompoundTag first = originals.remove(id);
                    if (first != null) queue.rejected.add(first);
                    queue.pending.remove(id);
                    throw new IllegalArgumentException("Ambiguous duplicate paid orb row");
                }
                for (String field : new String[]{"X", "Y", "Z", "MotionX", "MotionY", "MotionZ"}) if (!row.contains(field, Tag.TAG_DOUBLE)) throw new IllegalArgumentException("Missing native vector component");
                if (!row.contains("Aspect", Tag.TAG_STRING) || !row.contains("Value", Tag.TAG_INT) || !row.contains("Yaw", Tag.TAG_FLOAT) || !row.contains("RetryAt", Tag.TAG_LONG)) throw new IllegalArgumentException("Missing native orb fields");
                if (!row.hasUUID("Id") || row.getInt("Value") != 1) throw new IllegalArgumentException("Invalid orb identity/value");
                Pending entry = new Pending(row.getUUID("Id"), new Vec3(row.getDouble("X"), row.getDouble("Y"), row.getDouble("Z")),
                        new Vec3(row.getDouble("MotionX"), row.getDouble("MotionY"), row.getDouble("MotionZ")), row.getFloat("Yaw"), Aspect.getAspect(row.getString("Aspect")), row.getLong("RetryAt"));
                if (!valid(entry)) throw new IllegalArgumentException("Invalid paid orb row");
                originals.put(entry.id, row.copy());
                if (!queue.settled.contains(entry.id)) queue.pending.put(entry.id, entry);
            } catch (RuntimeException failure) { queue.rejected.add(row.copy()); }
        }
        return queue;
    }
}
