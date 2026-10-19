package thaumcraft.fabric.clienttest;

import it.unimi.dsi.fastutil.longs.LongCollection;
import java.lang.StackWalker.StackFrame;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.entity.EntityInLevelCallback;
import net.minecraft.world.level.entity.PersistentEntitySectionManager;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/** Private passive MC adapter. Metadata reads are existing raw fields/maps/list;
 * public UUID lookup means visibility ONLY. Never requests sections or chunks. */
final class BoreItemLifecycle {
    static final String SCHEMA = "bore_item_manager_lifecycle_v1";
    private static final String MANAGER = "net.minecraft.world.level.entity.PersistentEntitySectionManager";
    private static final String CALLBACK = MANAGER + "$Callback";
    private static final String WRAPPER = BoreItemLifecycleCore.ARCHITECTURY13_WRAPPER_CLASS;
    private static final int MAX_DIAGNOSTIC_FIELDS = 8, MAX_METADATA_NAME = 160, MAX_CALLBACK_DIAGNOSTIC = 4096;
    private static final String ENTITY_ARG = "(Lnet/minecraft/world/level/entity/EntityAccess;)V";
    private static final String REASON_ARG = "(Lnet/minecraft/world/entity/Entity$RemovalReason;)V";
    private static final StackWalker WALKER = StackWalker.getInstance(StackWalker.Option.RETAIN_CLASS_REFERENCE);
    private final ServerLevel level;
    private final ItemEntity item;
    private final UUID uuid;
    private final int generation;
    private final String domain, source, prefix;
    private final BoreItemLifecycleCore core;
    private final Map<String, Object> priorLive;
    private BoreItemLifecycleCore.Sample anchor;
    private Map<String, Object> trackingHead = Map.of(), trackingReturn = Map.of(), unloadHead = Map.of(), reason = Map.of();

    BoreItemLifecycle(ServerLevel level, ItemEntity item, int generation, String domain, String source) {
        require(Set.of("INHERITED_FIXED7", "PROVEN_AMBIENT").contains(domain) && source != null, "Finite existing lifecycle domains");
        this.level = level; this.item = item; uuid = item.getUUID(); this.generation = generation; this.domain = domain; this.source = source;
        prefix = "item-manager/" + domain + "/" + uuid + "/g" + generation;
        anchor = sample(read(level, "entityManager")); core = new BoreItemLifecycleCore(anchor);
        priorLive = observation("prior_actual_public_live_certificate", anchor, frames());
    }
    BoreItemLifecycleCore.Status status() { return core.status(); }
    boolean hidden() { return core.hidden(); }
    boolean completed() { return core.completed(); }
    void fail() { core.fail(); }
    void liveBoundary() { core.liveBoundary(sample(read(level, "entityManager"))); }
    void rebindPhysical(ItemEntity next) {
        try {
        require(core.completed() && next != item && next.getClass() == ItemEntity.class && next.level() == level && uuid.equals(next.getUUID())
                && level.getEntity(uuid) == next && !next.isRemoved() && item.isRemoved() && item.getRemovalReason() == Entity.RemovalReason.UNLOADED_TO_CHUNK
                && read(item, "levelCallback") == EntityInLevelCallback.NULL && physical(item).equals(core.completedUnload().returned().physical()), "Actual dead old owner retained, not revived/replaced before completed unload");
        for (Object member : (List<?>) read(read(anchor.section(), "storage"), "allInstances")) require(member != item, "Dead old owner cannot regain section membership");
        core.rebind(physical(next));
        } catch (RuntimeException | Error error) { core.fail(); throw error; }
    }
    void verifyCompleted() { core.checkpoint(sample(read(level, "entityManager"))); }
    Map<String, Object> certificate() { return priorLive; }

    Map<String, Object> tracking(Object manager, boolean returned) {
        var frames = frames(); int at = injected(frames, MANAGER, "stopTracking", ENTITY_ARG,
                returned ? "thaumcraft$boreItemTrackingReturn" : "thaumcraft$boreItemTrackingHead");
        noPhysicalFrames(frames);
        var actual = sample(manager);
        BoreItemLifecycleCore.Origin origin = trackingOrigin(frames, at, actual);
        if (!returned) { core.trackingHead(actual, origin); anchor = actual; trackingHead = observation("stopTracking_HEAD", actual, frames); return trackingHead; }
        core.trackingReturn(actual, origin); trackingReturn = observation("stopTracking_RETURN", actual, frames);
        return row("event", "completed_tracking_retirement_pair", "HEAD", trackingHead, "RETURN", trackingReturn,
                "origin", origin.name(), "physical_unload_completed", false, "coverage", "MANAGER_CUSTODY_UNAVAILABLE_STOPPED_AUDIT_REQUIRED");
    }
    Map<String, Object> unload(Object manager, boolean returned) {
        var frames = frames(); int at = injected(frames, MANAGER, "unloadEntity", ENTITY_ARG,
                returned ? "thaumcraft$boreItemUnloadReturn" : "thaumcraft$boreItemUnloadHead");
        noPhysicalFrames(frames);
        var actual = sample(manager);
        unloadSpine(frames, at, actual);
        if (!returned) {
            String prestate = core.hidden() ? "HIDDEN_AFTER_WITNESSED_TRACKING_RETURN" : "VISIBLE_SAME_OWNER";
            core.unloadHead(actual, BoreItemLifecycleCore.Origin.UNLOAD); anchor = actual;
            unloadHead = row("prestate", prestate, "observation", observation("unloadEntity_HEAD", actual, frames),
                    "earlier_completed_tracking_RETURN", trackingReturn.isEmpty() ? "NONE" : trackingReturn);
            return unloadHead;
        }
        core.unloadReturn(actual, BoreItemLifecycleCore.Origin.UNLOAD);
        return row("event", "completed_native_manager_unload_pair", "HEAD", unloadHead, "inner_setRemoved_HEAD", reason,
                "nested_tracking_RETURN", core.completedUnload().head().publicOwner() == null ? "NOT_NEEDED_HIDDEN" : trackingReturn,
                "RETURN", observation("unloadEntity_RETURN", actual, frames), "physical_unload_completed", true, "coverage", "STOPPED_AUDIT_REQUIRED", "durable_world_save_proved", false);
    }
    Map<String, Object> innerReason(Entity.RemovalReason actualReason) {
        var frames = frames(); int at = injected(frames, Entity.class.getName(), "setRemoved", REASON_ARG, "thaumcraft$boreEntityRemoval");
        noPhysicalFrames(frames);
        var actual = sample(read(level, "entityManager"));
        int unload = find(frames, at + 1, MANAGER, "unloadEntity", ENTITY_ARG); nativeOnly(frames, at, unload, actual); unloadSpine(frames, unload, actual);
        core.innerReason(actual, BoreItemLifecycleCore.Origin.INNER_REASON, actualReason.name());
        reason = observation("setRemoved_UNLOADED_TO_CHUNK_HEAD", actual, frames); return reason;
    }
    Map<String, Object> checkpoint(String boundary) {
        var actual = sample(read(level, "entityManager")); core.checkpoint(actual);
        return row("event", "passive_manager_custody_checkpoint", "boundary", boundary,
                "coverage", "MANAGER_CUSTODY_UNAVAILABLE_STOPPED_AUDIT_REQUIRED", "current_public_live_POST_authority", false,
                "last_completed_tracking_RETURN", trackingReturn, "observation", observation(core.hidden() ? "hidden_checkpoint" : "completed_unload_checkpoint", actual, frames()));
    }
    void closedBoundary() {
        if (core.status() == BoreItemLifecycleCore.Status.TRACKING_PENDING || core.status() == BoreItemLifecycleCore.Status.UNLOAD_PENDING
                || core.status() == BoreItemLifecycleCore.Status.FAILED) {
            core.fail(); throw new IllegalStateException("No pending/failed native lifecycle frame at boundary");
        }
    }
    private BoreItemLifecycleCore.Sample sample(Object manager) {
        try { return readSample(manager); }
        catch (RuntimeException | Error error) { if (core != null) core.fail(); throw error; }
    }
    private BoreItemLifecycleCore.Sample readSample(Object manager) {
        require(item.getClass() == ItemEntity.class && item.level() == level && level.dimension() == Level.OVERWORLD
                && level.getServer().overworld() == level && level.getServer().isSameThread() && uuid.equals(item.getUUID()), "Exact native item/world/server/thread/UUID domain");
        require(manager != null && manager.getClass() == PersistentEntitySectionManager.class, "Exact pinned native manager class");
        Object canonical = read(level, "entityManager"), callback = read(item, "levelCallback");
        boolean nil = callback == EntityInLevelCallback.NULL;
        CallbackView view = callbackView(callback);
        require(!nil || anchor != null, "Cannot first bind a NULL callback");
        Object delegate = view.delegate();
        Object callbackEntity = nil ? null : read(delegate, "entity"), callbackManager = nil ? null : read(delegate, "field_27271");
        Object storage = read(manager, "sectionStorage"), sections = read(storage, "sections");
        long key = nil ? anchor.sectionKey() : (Long) read(delegate, "currentSectionKey");
        Object section = nil ? anchor.section() : read(delegate, "currentSection");
        require(sections instanceof Map<?, ?> && section != null, "Existing raw section map/section only");
        Object indexed = ((Map<?, ?>) sections).get(Long.valueOf(key));
        Object instances = read(read(section, "storage"), "allInstances"), knownSet = read(manager, "knownUuids");
        require(instances instanceof List<?> && knownSet instanceof Set<?>, "Existing native raw section identity list/known UUID set");
        boolean member = false; for (Object owner : (List<?>) instances) if (owner == item) { member = true; break; }
        return new BoreItemLifecycleCore.Sample(manager, canonical, item, level, Thread.currentThread(), item.getUUID(), generation,
                callback, callbackEntity, callbackManager, storage, sections, key, section, indexed, instances, knownSet, member,
                ((Set<?>) knownSet).contains(uuid), level.getEntity(uuid), EntityInLevelCallback.NULL, item.isRemoved(),
                item.getRemovalReason() == null ? "NONE" : item.getRemovalReason().name(), physical(item), level.getServer().getTickCount(), level.getGameTime(),
                delegate, view.wrapperEntity(), view.representation());
    }
    private record CallbackView(Object delegate, Object wrapperEntity, BoreItemLifecycleCore.Representation representation) { }
    private CallbackView callbackView(Object raw) {
        Object delegate = null;
        boolean delegateObserved = false;
        try {
            if (raw == EntityInLevelCallback.NULL) return new CallbackView(null, null, BoreItemLifecycleCore.Representation.NATIVE_NULL);
            require(raw != null, "Java-null native callback is unsupported");
            Class<?> type = raw.getClass();
            List<BoreItemLifecycleCore.CallbackField> fields = type.getName().equals(WRAPPER) ? wrapperFields(type) : List.of();
            var representation = BoreItemLifecycleCore.representation(type.getName(), type.getClassLoader(), PersistentEntitySectionManager.class.getClassLoader(), fields);
            Object entity = null;
            if (representation == BoreItemLifecycleCore.Representation.EXACT_ARCHITECTURY13_WRAPPER) {
                // Exact already-existing captured fields ONLY. NEVER lastSectionKey,
                // factory/constructor, unwrap method, callback, or native getters.
                entity = read(raw, "val$entity"); delegate = read(raw, "val$callback"); delegateObserved = true;
                require(entity == item, "Architectury captured entity must be this actual item");
            } else { delegate = raw; delegateObserved = true; }
            require(delegate != null && delegate != EntityInLevelCallback.NULL, "Wrapper delegate cannot be null/NULL");
            BoreItemLifecycleCore.nativeDelegateClass(delegate.getClass().getName(), delegate.getClass().getClassLoader(), PersistentEntitySectionManager.class.getClassLoader());
            return new CallbackView(delegate, entity, representation);
        } catch (RuntimeException | Error error) {
            throw new IllegalStateException(bounded("Unsupported item callback representation; raw=" + callbackDiagnostic(raw, true)
                    + "; delegate=" + (delegateObserved ? callbackDiagnostic(delegate, false) : "NOT_OBSERVED"), MAX_CALLBACK_DIAGNOSTIC), error);
        }
    }
    private static List<BoreItemLifecycleCore.CallbackField> wrapperFields(Class<?> type) {
        require(type.getClassLoader() == PersistentEntitySectionManager.class.getClassLoader() && type.getSuperclass() == Object.class
                && type.getInterfaces().length == 1 && type.getInterfaces()[0] == EntityInLevelCallback.class, "Exact Architectury13 wrapper superclass/interface/loader");
        Field[] fields = type.getDeclaredFields(); require(fields.length == 3, "Exactly three Architectury13 wrapper declared fields");
        var schema = new ArrayList<BoreItemLifecycleCore.CallbackField>(3);
        for (Field field : fields) {
            Class<?> expected = switch (field.getName()) {
                case "val$entity" -> Entity.class;
                case "val$callback" -> EntityInLevelCallback.class;
                case "lastSectionKey" -> long.class;
                default -> throw new IllegalStateException("Unknown Architectury13 wrapper field");
            };
            require(field.getDeclaringClass() == type && field.getType() == expected, "Exact declared wrapper field Class identity");
            schema.add(new BoreItemLifecycleCore.CallbackField(field.getName(), field.getType().descriptorString(), field.getModifiers()));
        }
        return List.copyOf(schema);
    }
    private BoreItemLifecycleCore.Physical physical(ItemEntity actual) {
        Object accessor = read(actual, "DATA_ITEM"), data = read(actual, "entityData"); int index = (Integer) read(accessor, "id");
        Object[] slots = (Object[]) read(data, "itemsById"); require(index >= 0 && index < slots.length && slots[index] != null, "Actual raw physical stack slot");
        ItemStack stack = (ItemStack) read(slots[index], "value");
        String full = (domain.equals("PROVEN_AMBIENT") ? BorePassiveStackSnapshot.ambientTag(stack, level) : BorePassiveStackSnapshot.nativeTag(stack, level)).toString();
        Vec3 pos = (Vec3) read(actual, "position"), velocity = (Vec3) read(actual, "deltaMovement"); AABB body = (AABB) read(actual, "bb");
        return new BoreItemLifecycleCore.Physical(full, (Integer) read(actual, "age"), actual.tickCount, (Integer) read(actual, "health"),
                (Integer) read(actual, "pickupDelay"), (Boolean) read(actual, "invulnerable"), List.of(pos.x, pos.y, pos.z), List.of(velocity.x, velocity.y, velocity.z),
                List.of(body.minX, body.minY, body.minZ, body.maxX, body.maxY, body.maxZ));
    }
    private Map<String, Object> observation(String event, BoreItemLifecycleCore.Sample s, List<StackFrame> frames) {
        var p = s.physical();
        long serial = event.startsWith("stopTracking") ? core.trackingSerial() : event.startsWith("unloadEntity") || event.startsWith("setRemoved") ? core.unloadSerial() : core.serial();
        return row("schema", SCHEMA, "event", event, "prefix", prefix, "operation_serial", serial, "marker", prefix + "/op" + serial + "/" + event,
                "domain", domain, "source", source, "UUID", uuid.toString(), "owner_generation", generation,
                "status", core.status().name(), "native_tick", s.tick(), "world_game_time", s.time(),
                "actual_objects", row("manager", id(s.manager()), "ServerLevel_entityManager", id(s.canonicalManager()), "item", id(s.entity()), "world", id(s.world()),
                         "server", id(level.getServer()), "thread", id(s.thread()), "callback", id(s.callback()), "callback_class", s.callback().getClass().getName(),
                        "canonical_callback", id(s.canonicalCallback()), "wrapper_captured_entity", id(s.wrapperEntity()),
                        "callback_entity", id(s.callbackEntity()), "callback_outer_field_27271", id(s.callbackManager()), "section_storage", id(s.sectionStorage()),
                        "section_map", id(s.sectionMap()), "section_key", s.sectionKey(), "section", id(s.section()), "indexed_existing_section", id(s.indexedSection()),
                        "section_allInstances", id(s.allInstances()), "known_UUID_set", id(s.knownSet()), "current_UUID_lookup", id(s.publicOwner()), "NULL_callback_singleton", id(s.nullCallback())),
                "actual_metadata", row("known_UUID_present", s.known(), "section_contains_item_by_identity", s.member(), "callback_is_NULL", s.callback() == s.nullCallback(),
                        "section_status", read(s.section(), "chunkStatus").toString(), "section_reference_origin", s.callback() == s.nullCallback() ? "SAVED_OPERATION_SECTION_NOT_CURRENT_CALLBACK" : "ACTUAL_CANONICAL_DELEGATE_CURRENT_SECTION",
                        "public_UUID_lookup_status", s.publicOwner() == null ? "ABSENT" : s.publicOwner() == item ? "SAME_OBJECT" : "DIFFERENT_OBJECT", "removed", s.removed(), "removal_reason", s.reason()),
                "physical", row("fullstack_snbt", p.fullStack(), "Age", p.age(), "entity_tickCount", p.counter(), "Health", p.health(), "PickupDelay", p.delay(), "Invulnerable", p.invulnerable(),
                        "position", p.position(), "velocity", p.velocity(), "body_bounds", p.bounds()),
                "native_frames", detach(frames), "prior_live_certificate", priorLive == null ? "THIS_ACTUAL_PUBLIC_LIVE_BINDING" : priorLive,
                "callback_representation", row("schema", "bore_exact_callback_representation_v1", "shape", s.representation().name(),
                        "native_manager_class", PersistentEntitySectionManager.class.getName(), "native_manager_loader", loaderEvidence(PersistentEntitySectionManager.class.getClassLoader()),
                        "raw", classEvidence(s.callback()), "canonical_delegate", classEvidence(s.canonicalCallback()), "captured_wrapper_entity", id(s.wrapperEntity()),
                        "raw_is_canonical_delegate", s.callback() == s.canonicalCallback(), "raw_is_native_NULL", s.callback() == s.nullCallback(),
                        "raw_declared_fields", declaredFieldEvidence(s.callback().getClass()), "mutable_wrapper_section_key_used_as_authority", false),
                "root_context", BoreClientSequence.itemLifecycleContext(level), "observer_mutations", 0);
    }
    private BoreItemLifecycleCore.Origin trackingOrigin(List<StackFrame> f, int at, BoreItemLifecycleCore.Sample actual) {
        if (index(f, at + 1, CALLBACK, "onRemove", REASON_ARG) >= 0) {
            int removed = find(f, at + 1, Entity.class.getName(), "setRemoved", REASON_ARG);
            int unload = find(f, removed + 1, MANAGER, "unloadEntity", ENTITY_ARG);
            nativeOnly(f, at, unload, actual); unloadSpine(f, unload, actual); return BoreItemLifecycleCore.Origin.NESTED_ON_REMOVE;
        }
        int update = index(f, at + 1, CALLBACK, "updateStatus", "(Lnet/minecraft/world/level/entity/Visibility;Lnet/minecraft/world/level/entity/Visibility;)V");
        int end;
        if (update >= 0) end = find(f, update + 1, CALLBACK, "onMove", "()V");
        else {
            int consumer = find(f, at + 1, MANAGER, "method_31825", "(Lnet/minecraft/world/level/entity/Visibility;Lnet/minecraft/world/level/entity/EntitySection;)V");
            end = find(f, consumer + 1, MANAGER, "updateChunkStatus", "(Lnet/minecraft/world/level/ChunkPos;Lnet/minecraft/world/level/entity/Visibility;)V");
        }
        nativeOnly(f, at, end, actual); nativeTerminal(f, end, actual); return BoreItemLifecycleCore.Origin.VISIBILITY;
    }
    private void unloadSpine(List<StackFrame> f, int at, BoreItemLifecycleCore.Sample actual) {
        int consumer = find(f, at + 1, MANAGER, "method_31858", ENTITY_ARG);
        int store = find(f, consumer + 1, MANAGER, "storeChunkSections", "(JLjava/util/function/Consumer;)Z");
        int process = find(f, store + 1, MANAGER, "processChunkUnload", "(J)Z"); nativeOnly(f, at, process, actual); nativeTerminal(f, process, actual);
    }
    private void nativeTerminal(List<StackFrame> f, int start, BoreItemLifecycleCore.Sample actual) {
        int stop = index(f, start + 1, MinecraftServer.class.getName(), "stopServer", "()V");
        int tick = index(f, start + 1, MinecraftServer.class.getName(), "tickServer", "(Ljava/util/function/BooleanSupplier;)V");
        int run = index(f, start + 1, MinecraftServer.class.getName(), "runServer", "()V");
        // Native distance-manager tasks can run in waitUntilNextTick, outside
        // tickServer. They still need the exact visibility/unload origin AND
        // an entirely native spine to this bound server's actual server thread.
        require(stop >= 0 || tick >= 0 || run >= 0, "Actual native server run/tick/stop spine, never synthetic/direct calls");
        nativeOnly(f, start, stop >= 0 ? stop : tick >= 0 ? tick : run, actual);
    }
    private static int injected(List<StackFrame> f, String owner, String method, String desc, String handler) {
        int at = find(f, 0, owner, method, desc);
        require(at > 0 && f.get(at - 1).getClassName().equals(owner) && f.get(at - 1).getMethodName().contains(handler), "Actual required transformed native callback " + handler);
        return at;
    }
    private static int index(List<StackFrame> f, int start, String owner, String method, String desc) {
        for (int i = start; i < f.size(); i++) if (f.get(i).getClassName().equals(owner) && f.get(i).getDeclaringClass().getClassLoader() == PersistentEntitySectionManager.class.getClassLoader()
                && f.get(i).getMethodName().equals(method) && f.get(i).getDescriptor().equals(desc)) return i;
        return -1;
    }
    private static int find(List<StackFrame> f, int start, String owner, String method, String desc) {
        int at = index(f, start, owner, method, desc); require(at >= 0, "Missing actual native lifecycle frame " + owner + "." + method + desc); return at;
    }
    private void nativeOnly(List<StackFrame> f, int start, int end, BoreItemLifecycleCore.Sample actual) {
        int wrappers = 0, fastutilSaves = 0;
        for (int i = start; i <= end; i++) {
            String n = f.get(i).getClassName();
            if (n.startsWith("net.minecraft.") || n.startsWith("java.") || n.startsWith("jdk.")) continue;
            if (n.equals(LongCollection.class.getName())) {
                require(++fastutilSaves == 1 && fastutilSaveFrame(f, i, actual), "Foreign native lifecycle caller " + n);
                continue;
            }
            require(n.equals(WRAPPER) && ++wrappers == 1 && wrapperFrame(f, i, actual), "Foreign native lifecycle caller " + n);
        }
    }
    private boolean fastutilSaveFrame(List<StackFrame> frames, int at, BoreItemLifecycleCore.Sample s) {
        if (at == 0 || at + 1 >= frames.size()) return false;
        StackFrame frame = frames.get(at), child = frames.get(at - 1), parent = frames.get(at + 1);
        return s.manager() == s.canonicalManager() && s.manager() == core.live().manager()
                && frame.getDeclaringClass() == LongCollection.class
                && frame.getDeclaringClass().getClassLoader() == PersistentEntitySectionManager.class.getClassLoader()
                && child.getDeclaringClass() == PersistentEntitySectionManager.class && parent.getDeclaringClass() == PersistentEntitySectionManager.class
                && BoreItemLifecycleCore.fastutilSaveDelegation(frame.getMethodName(), frame.getDescriptor(), child.getMethodName(), child.getDescriptor(), parent.getMethodName(), parent.getDescriptor());
    }
    private boolean wrapperFrame(List<StackFrame> frames, int at, BoreItemLifecycleCore.Sample s) {
        if (s.representation() != BoreItemLifecycleCore.Representation.EXACT_ARCHITECTURY13_WRAPPER || at == 0 || at + 1 >= frames.size()) return false;
        var live = core.live(); StackFrame frame = frames.get(at), delegate = frames.get(at - 1), caller = frames.get(at + 1);
        return s.callback() == live.callback() && s.canonicalCallback() == live.canonicalCallback() && s.wrapperEntity() == item && live.wrapperEntity() == item
                && frame.getDeclaringClass() == s.callback().getClass() && frame.getDeclaringClass().getName().equals(WRAPPER)
                && frame.getDeclaringClass().getClassLoader() == PersistentEntitySectionManager.class.getClassLoader()
                && delegate.getDeclaringClass() == s.canonicalCallback().getClass() && delegate.getClassName().equals(CALLBACK)
                && caller.getDeclaringClass() == Entity.class
                && BoreItemLifecycleCore.wrapperDelegation(frame.getMethodName(), frame.getDescriptor(), delegate.getMethodName(), delegate.getDescriptor(), caller.getMethodName(), caller.getDescriptor());
    }
    private static void noPhysicalFrames(List<StackFrame> frames) {
        for (StackFrame f : frames) require(!(f.getClassName().equals(ItemEntity.class.getName()) && Set.of("tick", "playerTouch").contains(f.getMethodName()))
                && !Set.of("dropAllDeathLoot", "spawnAtLocation").contains(f.getMethodName()), "No item body/touch/loot/source frame at native lifecycle boundary");
    }
    private static List<StackFrame> frames() {
        var f = WALKER.walk(stream -> stream.limit(161).toList()); require(f.size() <= 160, "Bounded complete native lifecycle spine"); return f;
    }
    private static List<Map<String, Object>> detach(List<StackFrame> frames) {
        return frames.stream().map(f -> row("class", f.getClassName(), "method", f.getMethodName(), "descriptor", f.getDescriptor(), "bci", f.getByteCodeIndex(), "line", f.getLineNumber())).toList();
    }
    private static Object classEvidence(Object actual) {
        if (actual == null) return "ABSENT";
        Class<?> type = actual.getClass(); ClassLoader loader = type.getClassLoader();
        return row("actual_object", id(actual), "class", bounded(type.getName(), MAX_METADATA_NAME), "class_object", id(type),
                "loader", loaderEvidence(loader), "same_native_manager_loader", loader == PersistentEntitySectionManager.class.getClassLoader());
    }
    private static Map<String, Object> loaderEvidence(ClassLoader loader) {
        return row("class", loader == null ? "BOOTSTRAP" : bounded(loader.getClass().getName(), MAX_METADATA_NAME), "actual_object", id(loader));
    }
    private static List<Map<String, Object>> declaredFieldEvidence(Class<?> type) {
        Field[] fields = type.getDeclaredFields(); var out = new ArrayList<Map<String, Object>>();
        for (int i = 0; i < Math.min(fields.length, MAX_DIAGNOSTIC_FIELDS); i++) {
            Field field = fields[i]; out.add(row("name", bounded(field.getName(), MAX_METADATA_NAME), "type", bounded(field.getType().getName(), MAX_METADATA_NAME),
                    "descriptor", bounded(field.getType().descriptorString(), MAX_METADATA_NAME), "modifiers", field.getModifiers(), "synthetic", field.isSynthetic()));
        }
        return List.copyOf(out);
    }
    /** Bounded Class/Field metadata ONLY: never values, loader toString, class
     * initialization, probes, methods, factories, or callback execution. */
    private static String callbackDiagnostic(Object raw, boolean includeFields) {
        ClassLoader nativeLoader = PersistentEntitySectionManager.class.getClassLoader();
        Class<?> type = raw == null ? null : raw.getClass(); ClassLoader loader = type == null ? null : type.getClassLoader();
        var text = new StringBuilder("java_null=").append(raw == null).append(",native_NULL=").append(raw == EntityInLevelCallback.NULL)
                .append(",class=").append(type == null ? "ABSENT" : bounded(type.getName(), MAX_METADATA_NAME))
                .append(",loader_class=").append(type == null ? "ABSENT" : loader == null ? "BOOTSTRAP" : bounded(loader.getClass().getName(), MAX_METADATA_NAME))
                .append(",loader_id=").append(loader == null ? 0 : System.identityHashCode(loader))
                .append(",native_manager_loader_class=").append(nativeLoader == null ? "BOOTSTRAP" : bounded(nativeLoader.getClass().getName(), MAX_METADATA_NAME))
                .append(",native_manager_loader_id=").append(nativeLoader == null ? 0 : System.identityHashCode(nativeLoader))
                .append(",same_native_manager_loader=").append(type != null && loader == nativeLoader).append(",fields_included=").append(includeFields).append(",declared_fields=[");
        if (type != null && includeFields) {
            try {
                Field[] fields = type.getDeclaredFields();
                for (int i = 0; i < Math.min(fields.length, MAX_DIAGNOSTIC_FIELDS); i++) {
                    Field field = fields[i]; if (i != 0) text.append(';');
                    text.append(bounded(field.getName(), MAX_METADATA_NAME)).append(':').append(bounded(field.getType().getName(), MAX_METADATA_NAME)).append(':').append(field.getModifiers());
                }
                text.append("],declared_field_count=").append(fields.length).append(",fields_truncated=").append(fields.length > MAX_DIAGNOSTIC_FIELDS);
            } catch (RuntimeException | Error metadataFailure) {
                // This formatter is called ONLY while rejecting the callback;
                // metadata failure never becomes a canonical/default success.
                text.append("],field_metadata_failure_class=").append(bounded(metadataFailure.getClass().getName(), MAX_METADATA_NAME));
            }
        } else text.append(']');
        return bounded(text.toString(), MAX_CALLBACK_DIAGNOSTIC);
    }
    private static String bounded(String text, int limit) { return text.length() <= limit ? text : text.substring(0, limit - 12) + "[TRUNCATED]"; }
    private static Object read(Object actual, String field) { return BoreInheritedItemWitness.read(actual, field); }
    private static Object id(Object actual) { return actual == null ? "ABSENT" : System.identityHashCode(actual); }
    private static Map<String, Object> row(Object... fields) { var out = new LinkedHashMap<String, Object>(); for (int i = 0; i < fields.length; i += 2) out.put((String) fields[i], fields[i + 1]); return Map.copyOf(out); }
    private static void require(boolean ok, String message) { if (!ok) throw new IllegalStateException(message); }
}
