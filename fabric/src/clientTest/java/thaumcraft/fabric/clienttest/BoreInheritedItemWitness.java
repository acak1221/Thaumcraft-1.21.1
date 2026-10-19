package thaumcraft.fabric.clienttest;

import java.lang.StackWalker.StackFrame;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Consumer;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.nbt.TagParser;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.entity.EntityAccess;
import net.minecraft.world.level.storage.LevelResource;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/** Private, passive, finite inherited-physical observation; NOT an acquisition
 * source, unload predictor, custody exemption, or permission to act on an item.
 * Arm before opening the one private clone. Keep installed through normal stop.
 * Mojmap development names only. All native entry points catch/latch Throwable. */
public final class BoreInheritedItemWitness {
    public static final UUID OLD_JAR_UUID = UUID.fromString("14d47d60-09eb-4949-a6fd-84dd78e8eea1");
    public static final long PARENT_GAME_TIME = 133500L;
    private static final UUID PLAYER = UUID.fromString("13848433-7be7-350d-8b4a-c038819c2260");
    private static final String SCHEMA = "bore_inherited_item_witness_v1";
    private static final String AUDIT = "STOPPED_AUDIT_REQUIRED";
    private static final int MAX_CALLBACKS = 4_000_000, MAX_RECEIPTS = 65_536;
    private static final int MAX_PENDING = 16_384, MAX_STACK = 160, MAX_STACK_TEXT = 65_536;
    // These are stop margins, not permission to spend the remaining budget.
    private static final int DESPAWN_STOP_REMAINING = 128, COLLATERAL_STOP_AT = 480;
    private static final StackWalker WALKER = StackWalker.getInstance(StackWalker.Option.RETAIN_CLASS_REFERENCE);
    private static volatile State armed;
    private static volatile Throwable armingFailure;

    private BoreInheritedItemWitness() { }

    /** One-shot binding declaration, called by root nativeHooksInstalled before
     * world creation. No rearm/reset/retry/close API intentionally exists. */
    public static synchronized void arm(String fingerprint) {
        try {
            require(armed == null && armingFailure == null, "Inherited observer cannot rearm");
            require(fingerprint != null && fingerprint.matches("[a-f0-9]{64}"), "Exact private native binding hash");
            armed = new State(fingerprint);
        } catch (Throwable error) {
            if (armed != null) armed.latch(error);
            else if (armingFailure == null) armingFailure = error;
        }
    }

    /** Bind on the actual owning server thread, before bootstrap/Ledger ready.
     * Early rows and the first failure are retained until sinks are available. */
    public static void bind(ServerLevel level, ServerPlayer player,
                            Consumer<Map<String, Object>> receipts, Consumer<Throwable> failures) {
        State s = armed;
        if (s == null) { if (armingFailure == null) armingFailure = new IllegalStateException("Inherited observer was not armed before world entry"); return; }
        s.safe(() -> {
            require(!s.bound && player != null && receipts != null && failures != null, "One actual inherited observer bind");
            s.sink = receipts; s.failureSink = failures; s.bound = true;
            s.world(level);
            require(player.level() == level && player.server == level.getServer() && PLAYER.equals(player.getUUID()), "Exact inherited owner player/world");
            s.player = player;
            for (Map<String, Object> event : s.pending) receipts.accept(event);
            s.pending.clear();
            s.emit(row("event", "bound", "native_binding", s.fingerprint, "early_native_callbacks", s.callbacks,
                    "observer_armed_before_world_required", true));
            s.checkpoint("bind");
        });
        s.notifyFailure();
    }

    /** The new private ItemEntity.tick mixin calls these directly, not through
     * an active/ready controller lookup. Client copies are ignored, never used. */
    public static void tickHead(ItemEntity item) { dispatch(item, s -> s.head(item)); }
    public static void tickReturn(ItemEntity item) { dispatch(item, s -> s.returned(item)); }
    /** Root forwards actual setRemoved HEAD BEFORE its existing ready filter. */
    public static void removal(ItemEntity item, Entity.RemovalReason reason) { dispatch(item, s -> s.removed(item, reason)); }
    /** Fixed-seven dispatch BEFORE root readiness, including initially unseen
     * visible owners. Unknown UUIDs are ignored before opening any frame. */
    public static void managerLifecycle(Object manager, EntityAccess entity, boolean unload, boolean returned) {
        State s = armed;
        if (s == null) return;
        s.safe(() -> {
            if (entity == null || !s.entries.containsKey(entity.getUUID())) return;
            require(entity.getClass() == ItemEntity.class, "Known inherited UUID must be its exact ItemEntity");
            ItemEntity item = (ItemEntity) entity;
            require(item.level() instanceof ServerLevel, "Known inherited manager callback cannot change world domain");
            s.world((ServerLevel) item.level()); s.lifecycle(manager, item, unload, returned);
        });
    }
    /** Root forwards actual playerTouch HEAD/RETURN BEFORE its ready filter. */
    public static void touchHead(ItemEntity item, Player player) { dispatch(item, s -> s.touchHead(item, player)); }
    public static void touchReturn(ItemEntity item, Player player) { dispatch(item, s -> s.touchReturn(item, player)); }

    /** Raw already-loaded UUID lookups only. Call every native POST, bootstrap
     * poll, final/capture boundary and normal stop HEAD. Never signals PASS. */
    public static void checkpoint(ServerLevel level, String boundary) {
        State s = armed;
        if (s == null) return;
        s.safe(() -> { s.world(level); s.checkpoint(boundary); });
    }

    public static Throwable failure() { State s = armed; return armingFailure != null ? armingFailure : s == null ? new IllegalStateException("Inherited observer unarmed") : s.failure; }
    public static boolean picked() { State s = armed; return s != null && failure() == null && s.picked; }
    /** The returned stack is detached from the actual original HEAD owner; no
     * expected-metadata ItemStack is ever created. Comparison ONLY, never stock.
     * Exact actual raw patch is retained. EMPTY until proven transfer/failure. */
    public static ItemStack oldJarStack() {
        State s = armed;
        if (s == null || !picked()) return ItemStack.EMPTY;
        try { return BorePassiveStackSnapshot.copyForComparison(s.originalJar, s.level); }
        catch (Throwable error) { s.latch(error); return ItemStack.EMPTY; }
    }
    public static Map<String, Object> pickupReceipt() {
        State s = armed;
        return s != null && picked() ? s.pickupReceipt : Map.of();
    }
    /** Current exact native owner, not a ticket/load and not a pickup permit.
     * Root must independently satisfy its excursion/action/collision policy. */
    public static ItemEntity originalJarOwner(ServerLevel level) {
        State s = armed;
        if (s == null) return null;
        ItemEntity[] out = new ItemEntity[1];
        s.safe(() -> {
            s.world(level); s.checkpoint("originalJarOwner");
            Entity actual = level.getEntity(OLD_JAR_UUID);
            if (!s.picked && actual instanceof ItemEntity item && s.entries.get(OLD_JAR_UUID).owner == item) out[0] = item;
        });
        return failure() == null ? out[0] : null;
    }
    /** Detached observer state, never parent metadata disguised as observation. */
    public static Map<String, Object> snapshot() {
        State s = armed;
        if (s == null) return row("schema", SCHEMA, "armed", false, "failure", String.valueOf(failure()));
        var rows = new ArrayList<Map<String, Object>>();
        for (Entry e : s.entries.values()) rows.add(e.report());
        return row("schema", SCHEMA, "armed", true, "bound", s.bound, "picked", s.picked,
                "callbacks", s.callbacks, "receipts", s.receiptCount, "pending_rows", s.pending.size(),
                "failure", s.failure == null ? "NONE" : s.failure.toString(), "identities", List.copyOf(rows),
                "unseen_intervals", AUDIT, "runtime_preservation_PASS", false);
    }

    private interface Work { void run() throws Throwable; }
    private interface Event { void run(State s) throws Throwable; }
    private static void dispatch(ItemEntity item, Event event) {
        State s = armed;
        if (s == null) return;
        s.safe(() -> {
            if (item == null || item.level().isClientSide || !s.entries.containsKey(item.getUUID())) return;
            s.world((ServerLevel) item.level());
            event.run(s);
        });
    }

    private static final class State {
        final String fingerprint;
        final Map<UUID, Entry> entries = new LinkedHashMap<>();
        final List<Map<String, Object>> pending = new ArrayList<>();
        volatile Throwable failure;
        ServerLevel level;
        ServerPlayer player;
        Consumer<Map<String, Object>> sink;
        Consumer<Throwable> failureSink;
        boolean bound, notified, announced, picked;
        long callbacks, receiptCount, lastGameTime = PARENT_GAME_TIME, lastServerTick = -1;
        Touch touch;
        ItemStack originalJar = ItemStack.EMPTY;
        Map<String, Object> pickupReceipt = Map.of();

        State(String fingerprint) throws Exception {
            this.fingerprint = fingerprint;
            add("38b3dcb4-151e-40b8-8b5f-b77461ce49c8", 3241, "{id:\"minecraft:egg\",count:1}", false);
            add("4d550e3d-7493-4f95-9232-c2660ba9e186", 5702, "{id:\"minecraft:egg\",count:1}", false);
            add("097c73a1-77e1-403d-90bd-a54c759f3ef1", 1418, "{id:\"minecraft:egg\",count:1}", false);
            add("b04a8327-acd3-4279-9997-b4422a487471", 5281, "{id:\"minecraft:glowstone_dust\",count:1}", false);
            add("26563f6d-0c68-4022-adab-bf96885b4c0d", 1225, "{id:\"minecraft:arrow\",count:2}", true);
            add("252a9dd3-1427-4853-8459-21f7096db6b2", 1157, "{id:\"minecraft:rotten_flesh\",count:2}", true);
            add(OLD_JAR_UUID.toString(), 4253, "{id:\"thaumcraft:jar\",count:1,components:{\"minecraft:max_stack_size\":1,\"thaumcraft:jar_label_data\":\"potentia\",\"minecraft:block_entity_data\":{Amount:8,facing:3b,id:\"thaumcraft:jar\",Aspect:\"potentia\"}}}", false);
            require(entries.size() == 7, "Exactly seven inherited physical owners");
        }
        void add(String id, int age, String snbt, boolean collateral) throws Exception {
            UUID uuid = UUID.fromString(id);
            require(entries.put(uuid, new Entry(uuid, age, TagParser.parseTag(snbt), collateral)) == null, "Duplicate inherited UUID");
        }
        void safe(Work work) {
            try {
                require(++callbacks <= MAX_CALLBACKS, "Finite inherited observer callback cap");
                work.run();
            } catch (Throwable error) { latch(error); }
        }
        synchronized void latch(Throwable error) {
            if (failure == null) {
                failure = error;
                try {
                    System.err.println("[BoreInheritedItemWitness] FIRST_FAILURE binding=" + fingerprint + " callbacks=" + callbacks + " bound=" + bound);
                    error.printStackTrace(System.err); System.err.flush();
                } catch (Throwable ignored) { /* Reporting never replaces the original latched failure. */ }
            }
            for (Entry e : entries.values()) if (e.lifecycle != null && !e.lifecycle.completed()) e.lifecycle.fail();
            notifyFailure();
        }
        synchronized void notifyFailure() {
            if (failure == null || failureSink == null || notified) return;
            notified = true;
            try { failureSink.accept(failure); } catch (Throwable ignored) { /* root must also poll failure() */ }
        }
        void world(ServerLevel actual) {
            require(actual != null && actual.getServer().isSameThread(), "Inherited observation requires native owning server thread");
            require(actual.dimension() == Level.OVERWORLD && actual.getServer().overworld() == actual, "Actual canonical overworld");
            if (level == null) {
                var path = actual.getServer().getWorldPath(LevelResource.ROOT).toAbsolutePath().normalize();
                String name = path.getFileName().toString();
                require(name.startsWith("bore-smoke-") && name.substring(11).matches("[a-f0-9-]{36}"), "Only one root private bore-smoke clone");
                UUID.fromString(name.substring(11));
                require(path.getParent().getFileName().toString().equals("saves")
                        && path.getParent().getParent().getFileName().toString().equals("run-clienttest"), "Private clone path only");
                level = actual;
            }
            require(level == actual, "No inherited observer world/server rebind");
            long time = actual.getGameTime(), tick = actual.getServer().getTickCount();
            require(time >= lastGameTime && tick >= lastServerTick, "No native game/server time reset");
            lastGameTime = time; lastServerTick = tick;
            if (!announced) {
                announced = true;
                for (Entry e : entries.values()) emit(row("event", "initial_unseen", "identity", e.report(),
                        "interval_start_parent_game_time", PARENT_GAME_TIME, "coverage", AUDIT,
                        "expected_stack_is_NOT_runtime_observation", true));
            }
        }
        void head(ItemEntity item) {
            Entry e = entries.get(item.getUUID());
            List<StackFrame> stack = frames();
            int at = injected(stack, ItemEntity.class, "tick", "()V", "thaumcraft$boreInheritedItemHead");
            int body = find(stack, at + 1, ServerLevel.class, "tickNonPassenger", "(Lnet/minecraft/world/entity/Entity;)V");
            int world = find(stack, body + 1, ServerLevel.class, "tick", "(Ljava/util/function/BooleanSupplier;)V");
            int server = find(stack, world + 1, MinecraftServer.class, "tickServer", "(Ljava/util/function/BooleanSupplier;)V");
            nativeOnly(stack, at, server);
            require(e.frame == null && touch == null && !pickedIdentity(e), "No overlapping/inherited-after-pickup native item body");
            observe(e, item, true);
            require(item.tickCount == e.lastCounter + 1 && level.getEntity(e.uuid) == item, "Actual current native tick subject/counter");
            require(e.lastBodyServerTick != lastServerTick, "At most one native item body per actual server tick");
            e.frame = new Tick(item, age(item), item.tickCount, lastServerTick, lastGameTime);
            emit(row("event", "item_tick_HEAD", "item", item(e, item), "frames", detached(stack)));
            budget(e, age(item) + 1, e.ticks + 1);
        }
        void returned(ItemEntity item) {
            Entry e = entries.get(item.getUUID());
            List<StackFrame> stack = frames();
            int at = injected(stack, ItemEntity.class, "tick", "()V", "thaumcraft$boreInheritedItemReturn");
            int body = find(stack, at + 1, ServerLevel.class, "tickNonPassenger", "(Lnet/minecraft/world/entity/Entity;)V");
            nativeOnly(stack, at, body);
            Tick f = e.frame;
            require(f != null && f.item == item && f.serverTick == lastServerTick && f.gameTime == lastGameTime
                    && f.counter == item.tickCount, "Exact native item HEAD/RETURN world/thread/tick pair");
            require(age(item) == f.age + 1 && !item.isRemoved() && e.owner == item
                    && level.getEntity(e.uuid) == item, "Exactly one actual age increment; no native body deletion/replacement");
            checkPhysical(e, item, e.savedAge + e.ticks + 1);
            e.ticks++; e.lastCounter = item.tickCount; e.lastBodyServerTick = lastServerTick; e.frame = null;
            emit(row("event", "item_tick_RETURN", "item", item(e, item), "age_increment", 1, "frames", detached(stack)));
            budget(e, age(item), e.ticks);
        }
        void observe(Entry e, ItemEntity item, boolean atTickHead) {
            require(!pickedIdentity(e) && item.getClass() == ItemEntity.class && !item.isRemoved(), "Exact live native inherited ItemEntity class");
            require(level.getEntity(e.uuid) == item && item.level() == level, "Already-loaded same-UUID current owner");
            checkPhysical(e, item, e.savedAge + e.ticks);
            if (e.owner == item && e.lifecycle != null) e.lifecycle.liveBoundary();
            if (e.owner != item) {
                if (e.owner != null) {
                    require(e.unloadObserved && e.owner.isRemoved()
                            && e.owner.getRemovalReason() == Entity.RemovalReason.UNLOADED_TO_CHUNK, "No unexplained inherited native owner replacement");
                    finishUnavailable(e, "rebound_without_absent_lookup", false);
                }
                require(!e.everObserved || e.unloadObserved, "No unexplained inherited native owner rebound");
                require(item.tickCount == (atTickHead ? 1 : 0), "Missing preobserver native ticks on initial/rebound owner");
                if (e.lifecycle != null) {
                    e.lifecycle.rebindPhysical(item); e.retired.add(e.lifecycle);
                }
                e.owner = item; e.generation++; e.lastCounter = 0; e.unloadObserved = false; e.everObserved = true;
                e.lifecycle = new BoreItemLifecycle(level, item, e.generation, "INHERITED_FIXED7", e.uuid.toString());
                emit(row("event", "loaded_owner_observed", "item", item(e, item), "prior_availability", e.availability,
                        "manager_live_certificate", e.lifecycle.certificate(),
                        "prior_unseen_interval_start_game_time", e.unseenSince, "unseen_interval_reconciliation", AUDIT,
                        "inherited_owner_NOT_new_ADD", true));
                e.availability = "LOADED_SAME_UUID";
            }
            budget(e, age(item), e.ticks);
        }
        void checkPhysical(Entry e, ItemEntity item, int expectedAge) {
            require(e.uuid.equals(item.getUUID()) && age(item) == expectedAge, "Inherited Age mismatch: missing tick/reset/merge " + e.uuid);
            require(integer(item, "health") == 5 && integer(item, "pickupDelay") == 0
                    && !(Boolean) read(item, "invulnerable"), "Inherited health/delay/invulnerability changed " + e.uuid);
            require(stack(rawItem(item)).equals(e.expected), "Inherited full typed component/count stack changed " + e.uuid);
        }
        void budget(Entry e, int actualOrNextAge, int ticks) {
            require(actualOrNextAge < 6000 && ticks <= 6000 - e.savedAge, "Inherited native despawn hard limit " + e.uuid);
            require(ticks <= lastGameTime - PARENT_GAME_TIME, "Inherited Age contradicts conservative native elapsed " + e.uuid);
            require(!e.collateral || ticks <= 512, "x960 whole-attempt additional actual item-tick cap512 " + e.uuid);
            if (6000 - actualOrNextAge <= DESPAWN_STOP_REMAINING)
                latch(new IllegalStateException("STOP_BEFORE_EXPIRY remaining<=128 actual item ticks " + e.uuid));
            if (e.collateral && ticks >= COLLATERAL_STOP_AT)
                latch(new IllegalStateException("STOP_BEFORE_X960_CAP actual/next ticks>=480; hard cap512 " + e.uuid));
        }
        boolean pickedIdentity(Entry e) { return e.uuid.equals(OLD_JAR_UUID) && picked; }
        void checkpoint(String boundary) {
            require(boundary != null && boundary.length() <= 160, "Finite inherited checkpoint label");
            require(touch == null, "Unclosed native inherited playerTouch frame");
            for (Entry e : entries.values()) {
                require(e.frame == null, "Unclosed native inherited ItemEntity.tick frame " + e.uuid);
                if (e.lifecycle != null) e.lifecycle.closedBoundary();
                Entity loaded = level.getEntity(e.uuid); // Entity getter only: NEVER asks a chunk to load.
                if (pickedIdentity(e)) { require(loaded == null, "Original picked UUID reappeared"); continue; }
                if (loaded != null) {
                    require(loaded instanceof ItemEntity, "Inherited UUID rebound to non-item");
                    observe(e, (ItemEntity) loaded, false);
                    require(!e.unloadObserved, "Native unload did not finish before boundary");
                } else if (e.owner != null) {
                    if (e.lifecycle.hidden()) {
                        checkPhysical(e, e.owner, e.savedAge + e.ticks);
                        require(e.owner.tickCount == e.lastCounter, "Hidden inherited owner has no unobserved native ticks");
                        emit(row("event", "hidden_registered_owner_audit_required", "UUID", e.uuid.toString(),
                                "manager_lifecycle", e.lifecycle.checkpoint(boundary), "coverage", AUDIT,
                                "current_public_live_POST_authority", false));
                        continue;
                    }
                    require(e.unloadObserved, "Loaded inherited UUID vanished without actual UNLOADED_TO_CHUNK");
                    require(e.owner.isRemoved() && e.owner.getRemovalReason() == Entity.RemovalReason.UNLOADED_TO_CHUNK,
                            "Native unload HEAD must actually complete");
                    e.lifecycle.verifyCompleted();
                    finishUnavailable(e, boundary, true);
                } else if (e.lifecycle != null && e.lifecycle.completed()) e.lifecycle.verifyCompleted();
                // Initially absent is explicitly UNSEEN, never claimed UNLOADED.
            }
            if (!picked && lastGameTime - PARENT_GAME_TIME > 1160)
                latch(new IllegalStateException("Original jar conservative pickup deadline1160 elapsed missed"));
        }
        void finishUnavailable(Entry e, String boundary, boolean absentLookup) {
            emit(row("event", "unavailable_after_native_unload", "UUID", e.uuid.toString(), "owner_generation", e.generation,
                    "boundary", boundary, "actual_item_ticks_since_parent", e.ticks, "coverage", AUDIT,
                    "native_unload_game_time", e.unloadAtGameTime, "raw_UUID_lookup_absent_observed", absentLookup));
            e.owner = null; e.availability = "UNSEEN_AFTER_NATIVE_UNLOAD"; e.unseenSince = e.unloadAtGameTime;
        }
        void removed(ItemEntity item, Entity.RemovalReason reason) {
            Entry e = entries.get(item.getUUID());
            List<StackFrame> stack = frames();
            int at = injected(stack, Entity.class, "setRemoved", "(Lnet/minecraft/world/entity/Entity$RemovalReason;)V", "thaumcraft$boreEntityRemoval");
            require(reason != null && !pickedIdentity(e), "Unexpected repeated inherited removal");
            if (reason == Entity.RemovalReason.DISCARDED) {
                require(touch != null && touch.item == item && touch.player == player && e.uuid.equals(OLD_JAR_UUID)
                        && !touch.discarded && touch.serverTick == lastServerTick && touch.gameTime == lastGameTime,
                        "Inherited destruction/merge is forbidden outside paired original jar touch");
                int nativeTouch = find(stack, at + 1, ItemEntity.class, "playerTouch", "(Lnet/minecraft/world/entity/player/Player;)V");
                nativeOnly(stack, at, nativeTouch);
                require(e.owner == item && level.getEntity(e.uuid) == item && !item.isRemoved()
                        && age(item) == touch.age && integer(item, "health") == 5 && integer(item, "pickupDelay") == 0
                        && rawItem(item).isEmpty(), "Only genuine exhausted-stack native pickup discard");
                touch.discarded = true; touch.removalFrames = detached(stack);
            } else {
                require(reason == Entity.RemovalReason.UNLOADED_TO_CHUNK && touch == null && e.frame == null,
                        "Only actual native inherited UNLOADED_TO_CHUNK; no destruction/dimension escape");
                lifecyclePhysical(e, item);
                emit(row("event", "native_UNLOADED_TO_CHUNK_inner_reason_HEAD", "item", item(e, item),
                        "manager_lifecycle", e.lifecycle.innerReason(reason), "completion_NOT_yet_observed", true));
            }
        }
        void lifecyclePhysical(Entry e, ItemEntity item) {
            require(failure == null && e.owner == item && e.lifecycle != null && !e.unloadObserved && e.frame == null && touch == null
                    && !pickedIdentity(e) && item.level() == level && item.getClass() == ItemEntity.class, "Original bound inherited generation, no body/touch/unload overlap");
            checkPhysical(e, item, e.savedAge + e.ticks);
            require(item.tickCount == e.lastCounter, "Inherited lifecycle cannot create late counter baseline/unobserved ticks");
            budget(e, age(item), e.ticks);
        }
        void lifecycle(Object manager, ItemEntity item, boolean unload, boolean returned) {
            Entry e = entries.get(item.getUUID());
            // The unchanged paired old-jar DISCARDED/touch path also invokes
            // native onRemove/stopTracking. It grants no unload certificate.
            if (!unload && touch != null && touch.item == item && touch.discarded && touch.player == player
                    && e.uuid.equals(OLD_JAR_UUID) && item.isRemoved() && item.getRemovalReason() == Entity.RemovalReason.DISCARDED) {
                require(e.owner == item && manager == read(level, "entityManager") && touch.serverTick == lastServerTick && touch.gameTime == lastGameTime,
                        "Exact existing original-jar discard tracking context");
                return;
            }
            require(failure == null, "No lifecycle retry after observer failure");
            if (e.owner == null && !returned) {
                require(level.getEntity(e.uuid) == item && !item.isRemoved(), "Initially UNSEEN hidden owner cannot invent prior live/tracking certificate");
                observe(e, item, false);
            }
            lifecyclePhysical(e, item);
            Map<String, Object> proof = unload ? e.lifecycle.unload(manager, returned) : e.lifecycle.tracking(manager, returned);
            if (unload && returned) {
                require(e.lifecycle.completed(), "Only completed outer native unload RETURN commits absence");
                e.unloadObserved = true; e.unloadAtGameTime = lastGameTime;
                e.availability = "COMPLETED_NATIVE_UNLOAD_STOPPED_AUDIT_REQUIRED";
            } else if (!unload && returned && e.lifecycle.hidden()) e.availability = "HIDDEN_REGISTERED_MANAGER_CUSTODY_AUDIT_REQUIRED";
            emit(row("event", "actual_item_manager_lifecycle", "UUID", e.uuid.toString(), "owner_generation", e.generation,
                    "manager_lifecycle", proof, "coverage", AUDIT));
        }
        void touchHead(ItemEntity item, Player actualPlayer) {
            Entry e = entries.get(item.getUUID());
            List<StackFrame> stack = frames();
            int at = injected(stack, ItemEntity.class, "playerTouch", "(Lnet/minecraft/world/entity/player/Player;)V", "thaumcraft$boreTouchHead");
            touchSpine(stack, at);
            require(e.uuid.equals(OLD_JAR_UUID) && player != null && actualPlayer == player && touch == null && e.frame == null && !picked,
                    "Only bound actual player touching original loose jar");
            observe(e, item, false);
            require(lastGameTime - PARENT_GAME_TIME <= 1160 && age(item) <= 5413, "Conservative pickup deadline and actual jar Age");
            require(player.containerMenu == player.inventoryMenu && player.containerMenu.getCarried().isEmpty(), "Native pickup with empty cursor/no container");
            var inventory = inventory();
            require(inventory.subList(0, 36).stream().anyMatch(t -> t == null), "One real empty main inventory slot for original jar");
            require(inventory.stream().noneMatch(e.expected::equals), "Ambiguous preexisting exact old-jar stack");
            ItemStack actualOriginalCopy = BorePassiveStackSnapshot.copyForComparison(rawItem(item), level);
            touch = new Touch(item, player, age(item), lastServerTick, lastGameTime, inventory, actualOriginalCopy,
                    item(e, item), playerState(), detached(stack));
            emit(row("event", "oldjar_playerTouch_HEAD", "item", touch.itemState, "player", touch.playerState,
                    "inventory_fullstack_snbt", inventoryText(inventory), "frames", touch.frames));
        }
        void touchReturn(ItemEntity item, Player actualPlayer) {
            Entry e = entries.get(item.getUUID());
            List<StackFrame> stack = frames();
            int at = injected(stack, ItemEntity.class, "playerTouch", "(Lnet/minecraft/world/entity/player/Player;)V", "thaumcraft$boreTouchReturn");
            touchSpine(stack, at);
            Touch f = touch;
            require(f != null && f.item == item && f.player == actualPlayer && actualPlayer == player
                    && f.serverTick == lastServerTick && f.gameTime == lastGameTime && age(item) == f.age,
                    "Exact original native playerTouch HEAD/RETURN pair");
            List<CompoundTag> after = inventory();
            if (!f.discarded) {
                require(!item.isRemoved() && level.getEntity(e.uuid) == item && after.equals(f.inventory), "Unexplained refused-pickup mutation");
                checkPhysical(e, item, e.savedAge + e.ticks);
                emit(row("event", "oldjar_playerTouch_RETURN_REFUSED", "item", item(e, item), "frames", detached(stack),
                        "deadline_restart", false));
                touch = null; return;
            }
            // Vanilla restores count AFTER the exhausted native discard. This
            // removed entity's exact count1 shadow is NOT another stock owner.
            require(item.isRemoved() && item.getRemovalReason() == Entity.RemovalReason.DISCARDED
                    && level.getEntity(e.uuid) == null && stack(rawItem(item)).equals(e.expected), "Native original DISCARDED with exact restored dead-stack shadow");
            require(player.containerMenu == player.inventoryMenu && player.containerMenu.getCarried().isEmpty(), "Same actual empty-cursor pickup owner");
            int slot = -1;
            for (int i = 0; i < after.size(); i++) if (!java.util.Objects.equals(f.inventory.get(i), after.get(i))) {
                require(slot == -1 && i < 36 && f.inventory.get(i) == null && e.expected.equals(after.get(i)), "Exactly one empty main slot -> original fullstack/count1 delta");
                slot = i;
            }
            require(slot >= 0 && stack(f.original).equals(e.expected), "Actual original owner stack copied and transferred exactly once");
            originalJar = f.original; picked = true; e.owner = null; e.availability = "PICKED_NATIVE_PAIRED_TOUCH";
            pickupReceipt = row("event", "oldjar_playerTouch_RETURN_PICKED", "UUID", e.uuid.toString(), "owner_generation", e.generation,
                    "actual_item_ticks_since_parent", e.ticks, "native_tick", lastServerTick, "world_game_time", lastGameTime,
                    "HEAD_item", f.itemState, "HEAD_player", f.playerState, "HEAD_frames", f.frames,
                    "actual_DISCARDED_frames", f.removalFrames, "RETURN_frames", detached(stack), "RETURN_player", playerState(),
                    "player_main_slot", slot, "fullstack_snbt", stack(f.original).toString(), "inventory_before", inventoryText(f.inventory),
                    "inventory_after", inventoryText(after), "inventory_delta", 1, "later_custody_owner_chain", "ROOT_REQUIRED_NOT_PROVED_HERE");
            emit(pickupReceipt); touch = null;
        }
        List<CompoundTag> inventory() {
            require(player != null, "Actual bound inventory owner");
            var result = new ArrayList<CompoundTag>(41);
            for (ItemStack s : player.getInventory().items) result.add(s.isEmpty() ? null : stack(s));
            for (ItemStack s : player.getInventory().armor) result.add(s.isEmpty() ? null : stack(s));
            for (ItemStack s : player.getInventory().offhand) result.add(s.isEmpty() ? null : stack(s));
            require(result.size() == 41, "Exact native player inventory shape");
            return java.util.Collections.unmodifiableList(result);
        }
        CompoundTag stack(ItemStack actual) {
            require(actual != null && !actual.isEmpty(), "Actual nonempty inherited stack");
            Tag saved = BorePassiveStackSnapshot.nativeTag(actual, level); // actual raw snapshot, NEVER live copy/entity saving
            require(saved instanceof CompoundTag && saved.toString().length() <= MAX_STACK_TEXT, "Full typed stack serialization bound");
            return (CompoundTag) saved;
        }
        Map<String, Object> item(Entry e, ItemEntity actual) {
            return row("UUID", e.uuid.toString(), "actual_object", System.identityHashCode(actual), "owner_generation", e.generation,
                    "class", actual.getClass().getName(), "position", point((Vec3) read(actual, "position")),
                    "velocity", point((Vec3) read(actual, "deltaMovement")), "body_bounds", box((AABB) read(actual, "bb")),
                    "Age", age(actual), "Health", integer(actual, "health"), "PickupDelay", integer(actual, "pickupDelay"),
                    "Invulnerable", read(actual, "invulnerable"), "entity_tickCount", actual.tickCount,
                    "actual_item_ticks_since_parent", e.ticks, "remaining_to_6000", 6000 - age(actual),
                    "fullstack_snbt", stack(rawItem(actual)).toString());
        }
        Map<String, Object> playerState() {
            return row("UUID", player.getUUID().toString(), "actual_object", System.identityHashCode(player),
                    "position", point((Vec3) read(player, "position")), "body_bounds", box((AABB) read(player, "bb")));
        }
        void emit(Map<String, Object> event) {
            require(++receiptCount <= MAX_RECEIPTS, "Finite inherited event cap; no truncation");
            var out = new LinkedHashMap<String, Object>();
            out.put("schema", SCHEMA); out.put("native_tick", lastServerTick); out.put("world_game_time", lastGameTime);
            out.put("world_object", System.identityHashCode(level)); out.put("server_object", System.identityHashCode(level.getServer()));
            out.put("dimension", level.dimension().location().toString()); out.put("observer_mutations", 0); out.putAll(event);
            Map<String, Object> frozen = Map.copyOf(out);
            if (sink != null) sink.accept(frozen);
            else { require(pending.size() < MAX_PENDING, "Finite early inherited receipt buffer; bind promptly"); pending.add(frozen); }
        }
    }

    private static final class Entry {
        final UUID uuid;
        final int savedAge;
        final CompoundTag expected;
        final boolean collateral;
        ItemEntity owner;
        BoreItemLifecycle lifecycle;
        final List<BoreItemLifecycle> retired = new ArrayList<>();
        Tick frame;
        int ticks, lastCounter, generation;
        long lastBodyServerTick = -1, unseenSince = PARENT_GAME_TIME, unloadAtGameTime = -1;
        boolean everObserved, unloadObserved;
        String availability = "UNSEEN_NO_NATIVE_UNLOAD_INFERRED";
        Entry(UUID uuid, int savedAge, CompoundTag expected, boolean collateral) {
            this.uuid = uuid; this.savedAge = savedAge; this.expected = expected; this.collateral = collateral;
        }
        Map<String, Object> report() {
            return row("UUID", uuid.toString(), "saved_Age_expectation", savedAge, "expected_fullstack_snbt", expected.toString(),
                    "actual_item_ticks_since_parent", ticks, "owner_generation", generation, "availability", availability,
                    "ever_observed", everObserved, "native_unload_RETURN_completed", unloadObserved, "unseen_since_game_time", unseenSince,
                    "manager_lifecycle_status", availability.equals("PICKED_NATIVE_PAIRED_TOUCH") ? "NOT_APPLICABLE_PAIRED_ORIGINAL_JAR_PICKUP" : lifecycle == null ? "UNSEEN" : lifecycle.status().name(),
                    "retired_owner_generations_retained", retired.size(),
                    "unseen_interval_reconciliation", AUDIT, "remaining_from_accounted_ticks_NOT_unseen_observation", 6000 - savedAge - ticks,
                    "whole_attempt_extra_item_tick_cap", collateral ? 512 : 6000 - savedAge);
        }
    }
    private record Tick(ItemEntity item, int age, int counter, long serverTick, long gameTime) { }
    private static final class Touch {
        final ItemEntity item;
        final Player player;
        final int age;
        final long serverTick, gameTime;
        final List<CompoundTag> inventory;
        final ItemStack original;
        final Map<String, Object> itemState, playerState;
        final List<Map<String, Object>> frames;
        boolean discarded;
        List<Map<String, Object>> removalFrames = List.of();
        Touch(ItemEntity item, Player player, int age, long tick, long time, List<CompoundTag> inventory, ItemStack original,
              Map<String, Object> itemState, Map<String, Object> playerState, List<Map<String, Object>> frames) {
            this.item = item; this.player = player; this.age = age; serverTick = tick; gameTime = time;
            this.inventory = inventory; this.original = original; this.itemState = itemState; this.playerState = playerState; this.frames = frames;
        }
    }
    private static int age(ItemEntity item) { return integer(item, "age"); }
    private static int integer(Object actual, String name) {
        Object value = read(actual, name);
        require(value instanceof Integer, "Raw native int field type changed: " + name);
        return (Integer) value;
    }
    private static ItemStack rawItem(ItemEntity actual) {
        Object accessor = read(actual, "DATA_ITEM"), data = read(actual, "entityData");
        int index = integer(accessor, "id");
        Object[] slots = (Object[]) read(data, "itemsById");
        require(index >= 0 && index < slots.length && slots[index] != null, "Raw inherited synced-item slot changed");
        return (ItemStack) read(slots[index], "value");
    }
    static Object read(Object actual, String name) {
        for (Class<?> type = actual.getClass(); type != null; type = type.getSuperclass()) {
            try { Field field = type.getDeclaredField(name); field.setAccessible(true); return field.get(actual); }
            catch (NoSuchFieldException absent) { }
            catch (ReflectiveOperationException error) { throw new IllegalStateException("Raw inherited field unreadable: " + name, error); }
        }
        throw new IllegalStateException("Raw inherited field missing: " + name);
    }
    private static List<StackFrame> frames() {
        List<StackFrame> result = WALKER.walk(stream -> stream.limit(MAX_STACK + 1L).toList());
        require(result.size() <= MAX_STACK, "Finite native inherited stack depth"); return result;
    }
    private static int find(List<StackFrame> stack, int start, Class<?> owner, String method, String descriptor) {
        for (int i = start; i < stack.size(); i++) {
            StackFrame f = stack.get(i);
            if (f.getDeclaringClass() == owner && f.getMethodName().equals(method) && f.getDescriptor().equals(descriptor)) return i;
        }
        throw new IllegalStateException("Missing native inherited frame " + owner.getName() + "." + method + descriptor);
    }
    private static int injected(List<StackFrame> stack, Class<?> owner, String method, String descriptor, String handler) {
        int at = find(stack, 0, owner, method, descriptor);
        require(at > 0 && stack.get(at - 1).getDeclaringClass() == owner && stack.get(at - 1).getMethodName().contains(handler),
                "Actual transformed inherited native injection required: " + handler); return at;
    }
    private static void touchSpine(List<StackFrame> stack, int at) {
        int player = find(stack, at + 1, Player.class, "aiStep", "()V");
        int server = find(stack, player + 1, MinecraftServer.class, "tickServer", "(Ljava/util/function/BooleanSupplier;)V");
        nativeOnly(stack, at, server);
    }
    private static void nativeOnly(List<StackFrame> stack, int start, int end) {
        for (int i = start; i <= end; i++) {
            String name = stack.get(i).getClassName();
            require(name.startsWith("net.minecraft.") || name.startsWith("java.") || name.startsWith("jdk."), "Foreign/direct inherited native caller: " + name);
        }
    }
    private static List<Map<String, Object>> detached(List<StackFrame> stack) {
        var rows = new ArrayList<Map<String, Object>>();
        for (StackFrame f : stack) rows.add(row("class", f.getClassName(), "method", f.getMethodName(), "descriptor", f.getDescriptor(),
                "bci", f.getByteCodeIndex(), "line", f.getLineNumber()));
        return List.copyOf(rows);
    }
    private static List<String> inventoryText(List<CompoundTag> stacks) {
        return stacks.stream().map(tag -> tag == null ? "EMPTY" : tag.toString()).toList();
    }
    private static List<Double> point(Vec3 v) { return List.of(v.x, v.y, v.z); }
    private static List<Double> box(AABB v) { return List.of(v.minX, v.minY, v.minZ, v.maxX, v.maxY, v.maxZ); }
    private static Map<String, Object> row(Object... values) {
        var out = new LinkedHashMap<String, Object>();
        for (int i = 0; i < values.length; i += 2) out.put((String) values[i], values[i + 1]);
        return Map.copyOf(out);
    }
    private static void require(boolean condition, String message) { if (!condition) throw new IllegalStateException(message); }
}
