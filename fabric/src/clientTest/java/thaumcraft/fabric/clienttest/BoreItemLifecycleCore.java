package thaumcraft.fabric.clienttest;

import java.util.List;
import java.util.UUID;

/** Detached transition logic, NOT native evidence. Only BoreItemLifecycle reads
 * actual Minecraft state/frames. No reset, retry, admission, or stock API. */
final class BoreItemLifecycleCore {
    static final String NATIVE_CALLBACK_CLASS = "net.minecraft.world.level.entity.PersistentEntitySectionManager$Callback";
    static final String ARCHITECTURY13_WRAPPER_CLASS = "dev.architectury.hooks.level.entity.fabric.EntityHooksImpl$1";
    enum Representation { DIRECT_NATIVE_CALLBACK, EXACT_ARCHITECTURY13_WRAPPER, NATIVE_NULL }
    record CallbackField(String name, String descriptor, int modifiers) { }
    private static final List<CallbackField> WRAPPER_FIELDS = List.of(
            new CallbackField("val$entity", "Lnet/minecraft/world/entity/Entity;", 0x1010),
            new CallbackField("val$callback", "Lnet/minecraft/world/level/entity/EntityInLevelCallback;", 0x1010),
            new CallbackField("lastSectionKey", "J", 0x0002));
    /** Actual metadata only in the adapter; test metadata is explicitly symbolic.
     * No class loading, construction, recursive unwrap or supplied canonical flag. */
    static Representation representation(String rawClass, Object rawLoader, Object nativeLoader, List<CallbackField> wrapperFields) {
        require(nativeLoader != null && rawLoader == nativeLoader && wrapperFields != null, "Exact native-manager callback loader/metadata");
        if (NATIVE_CALLBACK_CLASS.equals(rawClass)) {
            require(wrapperFields.isEmpty(), "Direct native callback is not a wrapper schema");
            return Representation.DIRECT_NATIVE_CALLBACK;
        }
        require(ARCHITECTURY13_WRAPPER_CLASS.equals(rawClass) && wrapperFields.size() == 3 && wrapperFields.containsAll(WRAPPER_FIELDS), "Only exact Architectury13 captured-field schema");
        return Representation.EXACT_ARCHITECTURY13_WRAPPER;
    }
    static void nativeDelegateClass(String name, Object loader, Object nativeLoader) {
        require(NATIVE_CALLBACK_CLASS.equals(name) && nativeLoader != null && loader == nativeLoader, "Exact native canonical delegate class/loader, never a wrapper chain");
    }
    static boolean wrapperDelegation(String method, String descriptor, String delegateMethod, String delegateDescriptor, String callerMethod, String callerDescriptor) {
        return method.equals(delegateMethod) && descriptor.equals(delegateDescriptor)
                && (method.equals("onMove") && descriptor.equals("()V") && callerMethod.equals("setPosRaw") && callerDescriptor.equals("(DDD)V")
                || method.equals("onRemove") && descriptor.equals("(Lnet/minecraft/world/entity/Entity$RemovalReason;)V")
                && callerMethod.equals("setRemoved") && callerDescriptor.equals(descriptor));
    }
    static boolean fastutilSaveDelegation(String method, String descriptor, String childMethod, String childDescriptor, String parentMethod, String parentDescriptor) {
        return "removeIf".equals(method) && "(Ljava/util/function/LongPredicate;)Z".equals(descriptor)
                && "method_31842".equals(childMethod) && "(J)Z".equals(childDescriptor) && "saveAll".equals(parentMethod) && "()V".equals(parentDescriptor);
    }
    enum Status { LIVE, TRACKING_PENDING, HIDDEN_AUDIT_REQUIRED, UNLOAD_PENDING, UNLOADED_AUDIT_REQUIRED, FAILED }
    enum Origin { VISIBILITY, UNLOAD, INNER_REASON, NESTED_ON_REMOVE }
    enum ActorUse { TRACKING_UNLOAD_CONTEXT, BIRTH_SOURCE_BODY_CURRENT_POST }
    record ActorContext(Object actor, Object rootActor, Object server, Object rootServer, Object world, Object rootWorld,
                        boolean removed, boolean publicSameOwner, boolean stopHeadReturned, boolean actualNativeStopFrame) { }
    static boolean actorAllowed(ActorContext a, ActorUse use) {
        return a.actor != null && a.actor == a.rootActor && a.server != null && a.server == a.rootServer && a.world != null && a.world == a.rootWorld
                && (!a.removed && a.publicSameOwner || use == ActorUse.TRACKING_UNLOAD_CONTEXT && a.stopHeadReturned && a.actualNativeStopFrame);
    }
    record Physical(String fullStack, int age, int counter, int health, int delay, boolean invulnerable,
                    List<Double> position, List<Double> velocity, List<Double> bounds) {
        Physical {
            require(fullStack != null && !fullStack.isBlank() && fullStack.length() <= 65536, "Full typed physical stack");
            require(age >= 0 && age < 6000 && counter >= 0 && health == 5 && delay >= 0 && !invulnerable, "Supported physical scalars");
            position = List.copyOf(position); velocity = List.copyOf(velocity); bounds = List.copyOf(bounds);
            require(position.size() == 3 && velocity.size() == 3 && bounds.size() == 6, "Full physical geometry");
            for (var v : List.of(position, velocity, bounds)) for (Double n : v) require(n != null && Double.isFinite(n), "Finite physical geometry");
            for (int i = 0; i < 3; i++) require(bounds.get(i) < bounds.get(i + 3), "Nonempty physical body");
        }
        boolean sameAfterReload(Physical other) {
            return other.counter == 0 && new Physical(fullStack, age, 0, health, delay, invulnerable, position, velocity, bounds).equals(other);
        }
    }
    /** Raw references, not caller-supplied canonical booleans. Adapter proves
     * exact native classes; this core proves all per-event identity relations. */
    record Sample(Object manager, Object canonicalManager, Object entity, Object world, Object thread,
                  UUID uuid, int generation, Object callback, Object callbackEntity, Object callbackManager,
                  Object sectionStorage, Object sectionMap, long sectionKey, Object section, Object indexedSection,
                  Object allInstances, Object knownSet, boolean member, boolean known, Object publicOwner,
                  Object nullCallback, boolean removed, String reason, Physical physical, long tick, long time,
                  Object canonicalCallback, Object wrapperEntity, Representation representation) { }
    record Pair(long serial, Sample head, Sample returned, Origin origin) { }
    private final Sample live;
    private Status status = Status.LIVE;
    private Sample trackingHead, unloadHead;
    private Pair retirement, nestedRetirement, completedUnload;
    private long serial, trackingSerial, unloadSerial;
    private boolean reasonSeen;

    BoreItemLifecycleCore(Sample actualLive) {
        live = actualLive;
        registered(actualLive, true);
        require(!actualLive.removed && actualLive.publicOwner == actualLive.entity && "NONE".equals(actualLive.reason), "Prior actual public live certificate");
    }
    Status status() { return status; }
    Sample live() { return live; }
    Pair retirement() { return retirement; }
    Pair completedUnload() { return completedUnload; }
    long serial() { return serial; }
    long trackingSerial() { return trackingSerial; }
    long unloadSerial() { return unloadSerial; }
    boolean hidden() { return status == Status.HIDDEN_AUDIT_REQUIRED; }
    boolean completed() { return status == Status.UNLOADED_AUDIT_REQUIRED; }
    void fail() { status = Status.FAILED; }
    private interface Work { void run(); }
    private void guarded(Work work) {
        try { require(status != Status.FAILED, "Lifecycle permanently failed"); work.run(); }
        catch (RuntimeException | Error error) { fail(); throw error; }
    }
    void liveBoundary(Sample s) {
        guarded(() -> {
            require(status == Status.LIVE, "Hidden/pending/unloaded owner cannot supply live body/POST authority");
            registered(s, true); require(!s.removed && s.publicOwner == live.entity && "NONE".equals(s.reason), "Strict current public live owner");
        });
    }
    void trackingHead(Sample s, Origin origin) {
        guarded(() -> {
            require(trackingHead == null, "No double tracking HEAD");
            if (origin == Origin.NESTED_ON_REMOVE) {
                require(status == Status.UNLOAD_PENDING && reasonSeen && unloadHead.publicOwner == live.entity, "Only matched visible pending unload inner onRemove tracking");
                synchronous(unloadHead, s); registered(s, false);
                require(s.removed && "UNLOADED_TO_CHUNK".equals(s.reason) && nestedRetirement == null, "Actual inner removed state exactly once");
            } else {
                require(origin == Origin.VISIBILITY && status == Status.LIVE, "Only native visibility tracking retirement of a live owner");
                registered(s, true); require(!s.removed && "NONE".equals(s.reason), "No unrelated already-removed retirement");
                status = Status.TRACKING_PENDING;
            }
            require(s.publicOwner == live.entity, "Actual same public lookup at tracking HEAD");
            trackingHead = s; trackingSerial = ++serial;
        });
    }
    void trackingReturn(Sample s, Origin origin) {
        guarded(() -> {
            Sample head = trackingHead; require(head != null, "Missing tracking HEAD");
            boolean nested = status == Status.UNLOAD_PENDING;
            require(origin == (nested ? Origin.NESTED_ON_REMOVE : Origin.VISIBILITY), "Paired actual tracking origin");
            require(nested || status == Status.TRACKING_PENDING, "Paired tracking state");
            synchronous(head, s); sameSection(head, s); registered(s, !nested);
            require(s.publicOwner == null && s.removed == nested && s.reason.equals(nested ? "UNLOADED_TO_CHUNK" : "NONE"), "Actual lookup retirement, not physical unload");
            Pair pair = new Pair(trackingSerial, head, s, origin);
            if (nested) nestedRetirement = pair;
            else { retirement = pair; status = Status.HIDDEN_AUDIT_REQUIRED; }
            trackingHead = null;
        });
    }
    void unloadHead(Sample s, Origin origin) {
        guarded(() -> {
            require(origin == Origin.UNLOAD && (status == Status.LIVE || status == Status.HIDDEN_AUDIT_REQUIRED)
                    && unloadHead == null && trackingHead == null, "Only one native pending unload");
            registered(s, true); require(!s.removed && "NONE".equals(s.reason), "No repeated/removed unload subject");
            if (status == Status.HIDDEN_AUDIT_REQUIRED) hiddenSample(s);
            else require(s.publicOwner == live.entity, "Visible unload starts with actual same public owner; hidden needs earlier pair");
            unloadHead = s; reasonSeen = false; status = Status.UNLOAD_PENDING; unloadSerial = ++serial;
        });
    }
    void innerReason(Sample s, Origin origin, String reason) {
        guarded(() -> {
            require(origin == Origin.INNER_REASON && status == Status.UNLOAD_PENDING && unloadHead != null && !reasonSeen
                    && trackingHead == null && "UNLOADED_TO_CHUNK".equals(reason), "Exactly one matched native unload inner reason");
            synchronous(unloadHead, s); sameSection(unloadHead, s); registered(s, true);
            require(!s.removed && "NONE".equals(s.reason) && s.publicOwner == unloadHead.publicOwner, "Unchanged actual setRemoved HEAD prestate");
            reasonSeen = true;
        });
    }
    void unloadReturn(Sample s, Origin origin) {
        guarded(() -> {
            require(origin == Origin.UNLOAD && status == Status.UNLOAD_PENDING && unloadHead != null && reasonSeen && trackingHead == null, "Missing/failed unload pair or inner reason");
            synchronous(unloadHead, s); sameSection(unloadHead, s);
            identity(s);
            require(s.removed && "UNLOADED_TO_CHUNK".equals(s.reason) && s.publicOwner == null && !s.known
                    && s.callback == s.nullCallback && !s.member && (s.indexedSection == null || s.indexedSection == s.section), "Completed native manager custody release");
            require(unloadHead.publicOwner == null || nestedRetirement != null, "Visible unload must witness its actual nested lookup retirement");
            completedUnload = new Pair(unloadSerial, unloadHead, s, origin); status = Status.UNLOADED_AUDIT_REQUIRED;
        });
    }
    void checkpoint(Sample s) {
        guarded(() -> {
            require(trackingHead == null && status != Status.TRACKING_PENDING && status != Status.UNLOAD_PENDING, "Open synchronous lifecycle frame at checkpoint/POST/stop");
            if (status == Status.HIDDEN_AUDIT_REQUIRED) hiddenSample(s);
            else if (status == Status.LIVE) liveBoundary(s);
            else {
                require(status == Status.UNLOADED_AUDIT_REQUIRED, "Unsupported lifecycle checkpoint state");
                identity(s); require(completedUnload.returned.physical.equals(s.physical) && s.removed && "UNLOADED_TO_CHUNK".equals(s.reason)
                        && s.publicOwner == null && !s.known && s.callback == s.nullCallback && !s.member, "Dead old owner retained until rebind; no replacement");
            }
        });
    }
    void rebind(Physical physical) {
        guarded(() -> require(completed() && completedUnload.returned.physical.sameAfterReload(physical), "Rebind only after completed unload, exact full state and native counter0"));
    }
    private void hiddenSample(Sample s) {
        require(retirement != null && retirement.origin == Origin.VISIBILITY, "No fabricated prior tracking pair/late hidden baseline");
        Sample last = retirement.returned; identity(s); registered(s, true); sameSection(last, s);
        require(s.publicOwner == null && !s.removed && "NONE".equals(s.reason) && s.physical.equals(last.physical)
                && s.tick >= last.tick && s.time >= last.time, "Hidden custody unchanged; no unobserved ticks, drift, or retracking");
    }
    private void registered(Sample s, boolean member) {
        identity(s);
        require(s.callback == liveCallback(s) && s.callback != s.nullCallback && s.callbackEntity == s.entity && s.callbackManager == s.manager
                && s.canonicalCallback == live.canonicalCallback && s.wrapperEntity == live.wrapperEntity && s.representation == live.representation
                && s.section != null && s.indexedSection == s.section && s.member == member && s.known, "Exact canonical callback/section/knownUUID registration");
    }
    private Object liveCallback(Sample s) { return live == null ? s.callback : live.callback; }
    private void identity(Sample s) {
        require(s != null && s.manager != null && s.manager == s.canonicalManager && s.entity != null && s.world != null && s.thread != null
                && s.uuid != null && s.generation > 0 && s.sectionStorage != null && s.sectionMap != null && s.allInstances != null
                && s.knownSet != null && s.nullCallback != null && s.physical != null && s.tick >= 0 && s.time >= 0, "Actual canonical lifecycle objects");
        callbackShape(s);
        if (live != null) require(s.manager == live.manager && s.entity == live.entity && s.world == live.world && s.thread == live.thread
                && s.uuid.equals(live.uuid) && s.generation == live.generation && s.sectionStorage == live.sectionStorage
                && s.sectionMap == live.sectionMap && s.knownSet == live.knownSet && s.nullCallback == live.nullCallback
                && s.tick >= live.tick && s.time >= live.time, "Original same-owner generation/world/thread/manager domain");
    }
    private void callbackShape(Sample s) {
        require(s.callback != null && s.representation != null, "No Java-null callback/representation");
        if (s.callback == s.nullCallback) {
            require(s.representation == Representation.NATIVE_NULL && s.canonicalCallback == null && s.wrapperEntity == null
                    && s.callbackEntity == null && s.callbackManager == null, "Actual raw native NULL only; no invented delegate metadata");
            return;
        }
        require(s.canonicalCallback != null && s.canonicalCallback != s.nullCallback, "Canonical delegate is neither Java-null nor native NULL");
        require(s.representation == Representation.DIRECT_NATIVE_CALLBACK && s.callback == s.canonicalCallback && s.wrapperEntity == null
                || s.representation == Representation.EXACT_ARCHITECTURY13_WRAPPER && s.callback != s.canonicalCallback && s.wrapperEntity == s.entity,
                "Exact direct or single captured-entity wrapper relation");
        if (live != null) require(s.callback == live.callback && s.canonicalCallback == live.canonicalCallback
                && s.wrapperEntity == live.wrapperEntity && s.representation == live.representation, "Pinned raw callback AND canonical delegate for original generation");
    }
    private void synchronous(Sample head, Sample s) {
        identity(s); require(s.tick == head.tick && s.time == head.time && s.physical.equals(head.physical), "Same actual native operation clocks and all physical fields");
    }
    private void sameSection(Sample head, Sample s) {
        require(s.sectionKey == head.sectionKey && s.section == head.section && s.allInstances == head.allInstances, "Same actual operation callback section and raw identity list");
    }
    private static void require(boolean condition, String message) { if (!condition) throw new IllegalStateException(message); }
}
