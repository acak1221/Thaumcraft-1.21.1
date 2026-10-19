package thaumcraft.common.lib.utils;

import java.util.Objects;
import java.util.UUID;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.level.entity.EntityInLevelCallback;
import net.minecraft.world.level.entity.EntitySection;
import net.minecraft.world.level.entity.PersistentEntitySectionManager;
import thaumcraft.common.mixin.EntityLevelCallbackAccess;
import thaumcraft.common.mixin.PersistentEntitySectionManagerAccess;
import thaumcraft.common.mixin.ServerLevelEntityManagerAccess;

/** Recovery of an unattached UUID reserved by one rejected native entity-spawn attempt. */
public final class RejectedEntitySpawn {
    private RejectedEntitySpawn() {}

    /** Bound to one exact attempt; callers cannot manufacture a pre-add snapshot. */
    public static final class Snapshot {
        private final ServerLevel level;
        private final Entity entity;
        private final PersistentEntitySectionManager<Entity> manager;
        private final UUID uuid;
        private final boolean knownBefore;
        private final boolean unattachedBefore;

        private Snapshot(ServerLevel level, Entity entity, PersistentEntitySectionManager<Entity> manager) {
            this.level = level;
            this.entity = entity;
            this.manager = manager;
            this.uuid = entity.getUUID();
            this.knownBefore = manager.isLoaded(uuid);
            this.unattachedBefore = callback(entity) == EntityInLevelCallback.NULL;
        }

        public UUID uuid() { return uuid; }
        public boolean knownBefore() { return knownBefore; }
    }

    public static Snapshot snapshotBefore(ServerLevel level, ItemEntity entity) {
        return snapshotBefore(level, (Entity) entity);
    }

    public static Snapshot snapshotBefore(ServerLevel level, Entity entity) {
        requireServerThread(level);
        Objects.requireNonNull(entity, "entity");
        if (entity.level() != level) throw new IllegalArgumentException("Entity spawn belongs to another level");
        return new Snapshot(level, entity, manager(level));
    }

    /**
     * Call only after discard. nativeAdded is the actual addFreshEntity result,
     * or false if that attempt threw. Never call this instead of native removal.
     */
    public static boolean releaseRejected(ServerLevel level, ItemEntity entity, Snapshot snapshot, boolean nativeAdded) {
        return releaseRejected(level, (Entity) entity, snapshot, nativeAdded);
    }

    public static boolean releaseRejected(ServerLevel level, Entity entity, Snapshot snapshot, boolean nativeAdded) {
        requireServerThread(level);
        Objects.requireNonNull(entity, "entity");
        Objects.requireNonNull(snapshot, "snapshot");
        var manager = manager(level);
        if (nativeAdded || snapshot.level != level || snapshot.entity != entity || snapshot.manager != manager
                || snapshot.knownBefore || !snapshot.unattachedBefore || !entity.isRemoved()
                || entity.level() != level || !snapshot.uuid.equals(entity.getUUID())
                || callback(entity) != EntityInLevelCallback.NULL
                || level.getEntity(snapshot.uuid) != null || !manager.isLoaded(snapshot.uuid)) return false;

        // Architectury13 Fabric cancels at SectionPos.asLong after addEntityUuid,
        // before section.add/setLevelCallback. discard cannot release that UUID.
        // NeoForge cancels before reservation; a reentrant hook may instead have
        // registered another, hidden entity. Search every existing section, not
        // only ServerLevel.getEntity's visible lookup, before releasing anything.
        var access = (PersistentEntitySectionManagerAccess) manager;
        var sections = access.thaumcraft$getSectionStorage();
        for (long chunk : sections.getAllChunksWithExistingSections()) {
            try (var chunkSections = sections.getExistingSectionsInChunk(chunk)) {
                if (chunkSections.flatMap(EntitySection::getEntities)
                        .anyMatch(existing -> snapshot.uuid.equals(existing.getUUID()))) return false;
            }
        }
        return access.thaumcraft$getKnownUuids().remove(snapshot.uuid);
    }

    private static PersistentEntitySectionManager<Entity> manager(ServerLevel level) {
        return ((ServerLevelEntityManagerAccess) level).thaumcraft$getEntityManager();
    }

    private static EntityInLevelCallback callback(Entity entity) {
        return ((EntityLevelCallbackAccess) entity).thaumcraft$getLevelCallback();
    }

    private static void requireServerThread(ServerLevel level) {
        Objects.requireNonNull(level, "level");
        if (!level.getServer().isSameThread()) throw new IllegalStateException("Entity spawn recovery requires the server thread");
    }
}
