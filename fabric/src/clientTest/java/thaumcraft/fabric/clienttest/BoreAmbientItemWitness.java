package thaumcraft.fabric.clienttest;

import java.lang.StackWalker.StackFrame;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.function.Consumer;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/** Private client-test observer, NOT gameplay or native proof by itself.
 * Root owns one instance in Ledger, supplies only receipt/failure sinks, and
 * independently checks ORIGINAL unprotected-cohort membership and geometry.
 * No permit, source picker, loot evaluation, entity mutation, save, load or tick.
 * Literal Mojmap reflection/stack names deliberately restrict this to the dev
 * client-test harness; do not put this helper in production/common/API code. */
final class BoreAmbientItemWitness {
    private static final String SCHEMA = "bore_ambient_item_witness_v1";
    private static final String SPAWN = "(Lnet/minecraft/world/item/ItemStack;F)Lnet/minecraft/world/entity/item/ItemEntity;";
    private static final String LOOT = "(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/world/damagesource/DamageSource;)V";
    private static final String BODY = "(Lnet/minecraft/world/entity/Entity;)V";
    private static final String TICK = "(Ljava/util/function/BooleanSupplier;)V";
    private static final String DIE = "(Lnet/minecraft/world/damagesource/DamageSource;)V";
    private static final String TABLE = "(Lnet/minecraft/world/damagesource/DamageSource;Z)V";
    private static final Set<String> ENVIRONMENT = Set.of("minecraft:on_fire", "minecraft:in_fire", "minecraft:fall",
            "minecraft:out_of_world", "minecraft:drown", "minecraft:in_wall", "minecraft:lava", "minecraft:freeze");
    // Deliberately finite slice; flesh additionally requires an empty raw patch.
    // Other death-loot items, all equipment,
    // custom drops, chicken eggs and XP are unsupported, not ambient permission.
    private static final Set<String> ITEMS = Set.of("minecraft:bone", "minecraft:arrow", "minecraft:rotten_flesh");
    private static final int MAX_STACK = 128, MAX_CALLBACKS = 4000000, MAX_ATTEMPTS = 256;
    private static final int MAX_RECEIPTS = MAX_ATTEMPTS * 3, MAX_STACK_TEXT = 65536;
    private static final StackWalker WALKER = StackWalker.getInstance(StackWalker.Option.RETAIN_CLASS_REFERENCE);

    private final ServerLevel level;
    private final ServerPlayer owner;
    private final Thread thread;
    private final Consumer<Map<String, Object>> receipts;
    private final Consumer<Throwable> rootFailure;
    private volatile Throwable failure;
    private int callbacks, attempts, receiptCount;
    private Body body;
    private Death death;
    private Spawn spawn;

    BoreAmbientItemWitness(ServerLevel level, ServerPlayer owner, Consumer<Map<String, Object>> receipts,
                           Consumer<Throwable> rootFailure) {
        this.level = level;
        this.owner = owner;
        this.thread = Thread.currentThread();
        this.receipts = receipts;
        this.rootFailure = rootFailure;
    }

    // Root forwards the EXISTING tickNonPassenger hooks for all Mob subjects,
    // not only the protected-mobile cohort. No callback may simulate a body.
    void bodyHead(ServerLevel world, Entity actual) { safe(() -> {
        if (world != level || !(actual instanceof Mob mob)) return;
        require(body == null && death == null && spawn == null, "Nested/unclosed native entity body");
        if (!ordinary(mob)) return;
        // Native checkDespawn can discard this subject before tickNonPassenger.
        // An already-removed subject opens NO new body or loot authority.
        if (mob.isRemoved()) return;
        current(mob);
        List<StackFrame> frames = frames();
        int at = injected(frames, ServerLevel.class, "tickNonPassenger", BODY, "thaumcraft$boreMobileHead");
        spine(frames, at);
        body = new Body(mob, mob.getUUID(), tick(), mob.tickCount, source(mob), detached(frames));
    }); }

    void bodyReturned(ServerLevel world, Entity actual) { safe(() -> {
        if (world != level || body == null) return;
        require(actual == body.source && body.uuid.equals(actual.getUUID()) && tick() == body.tick,
                "Wrong subject/world/tick at native body RETURN");
        List<StackFrame> frames = frames();
        int at = injected(frames, ServerLevel.class, "tickNonPassenger", BODY, "thaumcraft$boreMobileReturn");
        spine(frames, at);
        require(death == null && spawn == null, "Missing native loot/spawn RETURN, including throw");
        // Do not read a removed owner as a live animal. Retirement is a separate
        // root lifecycle witness, not a fabricated loaded-owner receipt here.
        body = null;
    }); }

    // Actual dropAllDeathLoot argument is necessary: hurt caches its lethal
    // DamageSource only AFTER die/dropAllDeathLoot has finished in 1.21.1.
    void deathHead(LivingEntity actual, ServerLevel world, DamageSource actualDamage) { safe(() -> {
        if (world != level) return;
        require(death == null && spawn == null, "Nested/unclosed native death-loot frame");
        if (!(actual instanceof Mob mob) || body == null || body.source != actual || !ordinary(mob)) return;
        current(mob);
        requireBody(mob);
        List<StackFrame> frames = frames();
        int loot = injected(frames, LivingEntity.class, "dropAllDeathLoot", LOOT, "thaumcraft$boreAmbientDeathHead");
        int die = find(frames, loot + 1, LivingEntity.class, "die", DIE);
        int nativeBody = find(frames, die + 1, ServerLevel.class, "tickNonPassenger", BODY);
        nativeOnly(frames, loot, nativeBody);
        spine(frames, nativeBody);
        // Conservative cache restriction: first-hit lethal/stale/different-cache
        // deaths stay unsupported. NEVER substitute getLastDamageSource(),
        // because that getter can WRITE/clear lastDamageSource.
        DamageSource cached = (DamageSource) read(mob, "lastDamageSource");
        long stamp = number(mob, "lastDamageStamp").longValue();
        long elapsed = level.getGameTime() - stamp;
        if (rawHealth(mob) > 0 || !((Boolean) read(mob, "dead")) || cached == null || cached != actualDamage
                || elapsed < 0 || elapsed > 40 || !unownedEnvironment(cached)
                || read(mob, "lastHurtByMob") != null || read(mob, "lastHurtByPlayer") != null
                || number(mob, "lastHurtByPlayerTime").intValue() > 0) return;
        death = new Death(mob, cached, tick(), stamp, damage(cached), detached(frames));
    }); }

    void deathReturned(LivingEntity actual, ServerLevel world, DamageSource actualDamage) { safe(() -> {
        if (world != level || death == null) return;
        require(death.source == actual && death.damage == actualDamage && death.tick == tick() && spawn == null,
                "Wrong/missing paired death-loot RETURN");
        requireBody(death.source);
        current(death.source);
        requireDeathState(death.source);
        List<StackFrame> frames = frames();
        int loot = injected(frames, LivingEntity.class, "dropAllDeathLoot", LOOT, "thaumcraft$boreAmbientDeathReturn");
        int die = find(frames, loot + 1, LivingEntity.class, "die", DIE);
        int nativeBody = find(frames, die + 1, ServerLevel.class, "tickNonPassenger", BODY);
        nativeOnly(frames, loot, nativeBody);
        spine(frames, nativeBody);
        death = null;
    }); }

    void head(Entity actualSource, ItemStack offered, float yOffset) { safe(() -> {
        if (actualSource.level() != level) return;
        require(spawn == null, "Nested/unclosed terminal native spawn frame");
        if (!(actualSource instanceof Mob mob) || death == null || death.source != mob) return;
        // No generic Mob drop authority: require actual vanilla loot-table path
        // inside this exact dying Mob's real native body and dropAllDeathLoot.
        current(mob);
        requireBody(mob);
        requireDeathState(mob);
        if (offered.isEmpty() || !ITEMS.contains(itemType(offered))) return;
        require(Float.isFinite(yOffset) && Float.floatToIntBits(yOffset) == Float.floatToIntBits(0.0F), "Unsupported loot offset");
        List<StackFrame> frames = frames();
        int at = injected(frames, Entity.class, "spawnAtLocation", SPAWN, "thaumcraft$boreAmbientSpawnHead");
        spawnPath(frames, at);
        require(++attempts <= MAX_ATTEMPTS, "Ambient attempt limit");
        Map<String, Object> actualState = source(mob);
        String fullStack = stack(offered);
        spawn = new Spawn(attempts, mob, offered, yOffset, tick(), fullStack);
        emit(row("event", "ambient_item_source_HEAD", "attempt", attempts, "actual_source", actualState,
                "offered_fullstack_snbt", fullStack, "y_offset", yOffset,
                "actual_owner", owner(), "native_frames", detached(frames),
                "body_HEAD_source", body.state, "body_HEAD_frames", body.frames,
                "death_argument", death.state, "death_HEAD_frames", death.frames,
                "death_cached_stamp", death.stamp));
    }); }

    // Root MUST call before its owned-area early return for EVERY native
    // ItemEntity ADD, even outside the broad rectangles. ADD is only an
    // observation of an attempt: Architectury can still deny it afterward.
    void added(ItemEntity actualItem) { safe(() -> {
        if (actualItem.level() != level || spawn == null) return;
        requireSpawn();
        require(spawn.added == null, "Multiple ADDs in one terminal spawn");
        List<StackFrame> frames = frames();
        int fresh = find(frames, 0, ServerLevel.class, "addFreshEntity", "(Lnet/minecraft/world/entity/Entity;)Z");
        int at = find(frames, fresh + 1, Entity.class, "spawnAtLocation", SPAWN);
        nativeOnly(frames, fresh, at);
        spawnPath(frames, at);
        require(actualItem.getClass() == ItemEntity.class && !actualItem.isRemoved(), "Not the actual vanilla live ItemEntity ADD");
        require(stack(rawItem(actualItem)).equals(spawn.stack), "ADD full stack differs from SOURCE");
        spawn.added = actualItem;
        spawn.itemUUID = actualItem.getUUID();
        emit(row("event", "ambient_item_ADD_observed", "attempt", spawn.id, "source_UUID", spawn.source.getUUID().toString(),
                "actual_source", source(spawn.source), "actual_item", item(actualItem), "actual_owner", owner(),
                "native_frames", detached(frames), "admission", "ADD_CALLBACK_ONLY_NOT_ACCEPTANCE"));
    }); }

    void returned(Entity actualSource, ItemStack offered, float yOffset, ItemEntity actualNullableReturn) { safe(() -> {
        if (actualSource.level() != level || spawn == null) return;
        requireSpawn();
        require(actualSource == spawn.source && offered == spawn.offered
                && Float.floatToIntBits(yOffset) == Float.floatToIntBits(spawn.yOffset), "Mismatched terminal RETURN arguments");
        List<StackFrame> frames = frames();
        int at = injected(frames, Entity.class, "spawnAtLocation", SPAWN, "thaumcraft$boreAmbientSpawnReturn");
        spawnPath(frames, at);
        ItemEntity actual = actualNullableReturn;
        boolean loaded = actual != null && actual == spawn.added && actual.level() == level
                && spawn.itemUUID.equals(actual.getUUID()) && !actual.isRemoved()
                && level.getEntity(actual.getUUID()) == actual;
        // The first three tests short-circuit before a possibly absent UUID.
        String outcome = actual == null ? "NO_BIRTH_NULL_RETURN" : spawn.added == null ? "NO_BIRTH_NO_ADD"
                : actual != spawn.added ? "NO_BIRTH_DIFFERENT_RETURN" : !loaded ? "NO_BIRTH_NOT_CURRENT_UUID_OWNER"
                : !stack(rawItem(actual)).equals(spawn.stack) ? "NO_BIRTH_STACK_CHANGED" : "PAIRED_SOURCE_ADD_RETURN";
        emit(row("event", "ambient_item_source_RETURN", "attempt", spawn.id, "actual_source", source(spawn.source),
                "offered_fullstack_snbt", stack(offered), "y_offset", yOffset, "actual_owner", owner(),
                "actual_nullable_return", actual == null ? "NULL" : item(actual),
                "loaded_same_UUID_owner_at_RETURN", loaded, "outcome", outcome, "native_frames", detached(frames),
                "root_requirement", "ORIGINAL_UNPROTECTED_COHORT_AND_GEOMETRY_PLUS_LIFECYCLE_CORRELATION"));
        require(outcome.equals("PAIRED_SOURCE_ADD_RETURN"), "Native attempt did not establish birth: " + outcome);
        require(stack(offered).equals(spawn.stack), "Offered full stack mutated inside native spawn");
        spawn = null;
    }); }

    // Call at POST, final capture, and BEFORE normal-stop teardown. A native
    // throw can skip RETURN; it never silently resets the pending attempt.
    void checkpoint() { safe(() -> require(body == null && death == null && spawn == null, "Unclosed native ambient frame at boundary")); }
    Throwable failure() { return failure; }
    // For root's synchronous receipt sink to compare ORIGINAL cohort objects
    // using ==, not descriptive identity hashes. No history, lookup or permit;
    // root must never select/mutate a source through this read-only accessor.
    synchronized Mob pendingSource() { return failure == null && spawn != null ? spawn.source : null; }

    private synchronized void safe(Runnable action) {
        if (failure != null) return;
        try {
            require(Thread.currentThread() == thread && level.getServer().isSameThread(), "Ambient observer wrong thread");
            require(++callbacks <= MAX_CALLBACKS, "Ambient callback limit");
            action.run();
        } catch (Throwable error) {
            // A reentrant sink may already have failed a nested observation.
            // Keep the FIRST error and call even a broken root sink only once.
            if (failure != null) return;
            failure = error;
            body = null; death = null; spawn = null;
            try { rootFailure.accept(error); } catch (Throwable ignored) { /* failure remains latched for root polling */ }
        }
    }

    private void requireSpawn() {
        require(spawn.tick == tick() && death != null && death.source == spawn.source && death.tick == tick(), "Spawn world/tick/death binding lost");
        requireBody(spawn.source);
        current(spawn.source);
        requireDeathState(spawn.source);
    }
    private void requireDeathState(Mob actual) {
        require(death != null && death.source == actual && ordinary(actual) && rawHealth(actual) <= 0
                && ((Boolean) read(actual, "dead")), "Source flags/health changed");
        require(read(actual, "lastDamageSource") == death.damage && unownedEnvironment(death.damage)
                && number(actual, "lastDamageStamp").longValue() == death.stamp
                && read(actual, "lastHurtByMob") == null && read(actual, "lastHurtByPlayer") == null
                && number(actual, "lastHurtByPlayerTime").intValue() <= 0, "Actor/stale damage provenance");
    }
    private void requireBody(Mob actual) {
        require(body != null && body.source == actual && body.uuid.equals(actual.getUUID()) && body.tick == tick()
                && actual.tickCount == body.preCounter + 1, "Not the exact actual native body subject/counter");
    }
    private int tick() { return level.getServer().getTickCount(); }
    private void current(Mob mob) {
        MinecraftServer server = level.getServer();
        require(server.isSameThread(), "Ambient current: wrong server thread");
        require(!server.isStopped(), "Ambient current: server stopped");
        require(server.getLevel(level.dimension()) == level, "Ambient current: server level owner changed");
        require(mob.level() == level, "Ambient current: subject world changed");
        require(!mob.isRemoved(), "Ambient current: active subject removed");
        require(level.getEntity(mob.getUUID()) == mob, "Ambient current: live subject UUID owner absent/replaced (no observer loads)");
        require(owner.level() == level, "Ambient current: actor world changed");
        require(!owner.isRemoved(), "Ambient current: actor removed");
        require(level.getEntity(owner.getUUID()) == owner, "Ambient current: actor UUID owner absent/replaced (no observer loads)");
    }
    private static boolean ordinary(Mob mob) {
        return mob.getClass().getName().startsWith("net.minecraft.world.entity.")
                && BuiltInRegistries.ENTITY_TYPE.getKey(mob.getType()).getNamespace().equals("minecraft")
                && !named(mob) && (mobFlags(mob) & 1) == 0 && !((Boolean) read(mob, "persistenceRequired"))
                && !((Boolean) read(mob, "invulnerable")) && read(mob, "vehicle") == null;
    }
    private static boolean unownedEnvironment(DamageSource damage) {
        return ENVIRONMENT.contains(damageType(damage)) && read(damage, "causingEntity") == null && read(damage, "directEntity") == null;
    }
    private static String damageType(DamageSource damage) {
        return damage.typeHolder().unwrapKey().orElseThrow().location().toString();
    }
    private static Map<String, Object> damage(DamageSource actual) {
        return row("type", damageType(actual), "actual_damage_object", System.identityHashCode(actual),
                "causing_entity", actor(read(actual, "causingEntity")), "direct_entity", actor(read(actual, "directEntity")));
    }
    private static Object actor(Object actual) {
        return actual instanceof Entity entity ? row("UUID", entity.getUUID().toString(), "class", entity.getClass().getName(),
                "type", BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType()).toString()) : "NONE";
    }
    private Map<String, Object> source(Mob mob) {
        Object cached = read(mob, "lastDamageSource");
        return row("UUID", mob.getUUID().toString(), "actual_object", System.identityHashCode(mob),
                "type", BuiltInRegistries.ENTITY_TYPE.getKey(mob.getType()).toString(), "class", mob.getClass().getName(),
                "position", point((Vec3) read(mob, "position")), "velocity", point((Vec3) read(mob, "deltaMovement")),
                "body_bounds", box((AABB) read(mob, "bb")), "health_raw", rawHealth(mob), "dead_raw", read(mob, "dead"),
                "named", named(mob), "NoAI", (mobFlags(mob) & 1) != 0, "mob_flags_raw", mobFlags(mob),
                "persistent", read(mob, "persistenceRequired"), "invulnerable", read(mob, "invulnerable"),
                "vehicle", actor(read(mob, "vehicle")), "native_entity_tickCount", mob.tickCount,
                "deathTime", number(mob, "deathTime"), "hurtTime", number(mob, "hurtTime"),
                "remainingFireTicks", number(mob, "remainingFireTicks"),
                "lastDamageStamp", number(mob, "lastDamageStamp"), "lastDamageSource", cached instanceof DamageSource ds ? damage(ds) : "NONE",
                "lastHurtByMob", actor(read(mob, "lastHurtByMob")), "lastHurtByMobTimestamp", number(mob, "lastHurtByMobTimestamp"),
                "lastHurtByPlayer", actor(read(mob, "lastHurtByPlayer")), "lastHurtByPlayerTime", number(mob, "lastHurtByPlayerTime"));
    }
    private Map<String, Object> owner() {
        return row("UUID", owner.getUUID().toString(), "actual_object", System.identityHashCode(owner),
                "body_bounds", box((AABB) read(owner, "bb")), "position", point((Vec3) read(owner, "position")));
    }
    private Map<String, Object> item(ItemEntity actual) {
        return row("UUID", actual.getUUID().toString(), "actual_object", System.identityHashCode(actual), "class", actual.getClass().getName(),
                "position", point((Vec3) read(actual, "position")), "velocity", point((Vec3) read(actual, "deltaMovement")),
                "body_bounds", box((AABB) read(actual, "bb")), "fullstack_snbt", stack(rawItem(actual)),
                "pickupDelay", number(actual, "pickupDelay"), "health", number(actual, "health"), "age", number(actual, "age"),
                "native_entity_tickCount", actual.tickCount, "removed", actual.isRemoved());
    }
    private String stack(ItemStack actual) {
        require(!actual.isEmpty(), "Empty offered/actual native stack");
        String text = BorePassiveStackSnapshot.ambientTag(actual, level).toString();
        require(text.length() <= MAX_STACK_TEXT, "Fullstack receipt size limit");
        return text;
    }
    private static String itemType(ItemStack actual) { return BuiltInRegistries.ITEM.getKey(actual.getItem()).toString(); }
    private static ItemStack rawItem(ItemEntity actual) { return (ItemStack) synced(actual, "DATA_ITEM"); }
    private static float rawHealth(LivingEntity actual) { return (Float) synced(actual, "DATA_HEALTH_ID"); }
    private static byte mobFlags(Mob actual) { return (Byte) synced(actual, "DATA_MOB_FLAGS_ID"); }
    private static boolean named(Entity actual) { return ((Optional<?>) synced(actual, "DATA_CUSTOM_NAME")).isPresent(); }
    private static Object synced(Entity actual, String accessorName) {
        Object accessor = read(actual, accessorName), data = read(actual, "entityData");
        int index = ((Number) read(accessor, "id")).intValue();
        Object[] items = (Object[]) read(data, "itemsById");
        require(index >= 0 && index < items.length && items[index] != null, "Native raw synced-data slot changed");
        return read(items[index], "value");
    }
    private static Number number(Object actual, String field) { return (Number) read(actual, field); }
    private static Object read(Object actual, String name) {
        for (Class<?> type = actual.getClass(); type != null; type = type.getSuperclass()) {
            try { Field field = type.getDeclaredField(name); field.setAccessible(true); return field.get(actual); }
            catch (NoSuchFieldException ignored) { }
            catch (ReflectiveOperationException error) { throw new IllegalStateException("Native raw field unreadable: " + name, error); }
        }
        throw new IllegalStateException("Native raw field missing: " + name);
    }

    private static List<StackFrame> frames() {
        List<StackFrame> frames = WALKER.walk(stream -> stream.limit(MAX_STACK + 1L).toList());
        require(frames.size() <= MAX_STACK, "Native stack depth limit");
        return frames;
    }
    private static int injected(List<StackFrame> frames, Class<?> owner, String method, String descriptor, String handler) {
        int at = find(frames, 0, owner, method, descriptor);
        require(at > 0 && frames.get(at - 1).getDeclaringClass() == owner
                && frames.get(at - 1).getMethodName().contains(handler), "Not actual required transformed native injection: " + handler);
        return at;
    }
    private static int find(List<StackFrame> frames, int start, Class<?> owner, String method, String descriptor) {
        for (int i = start; i < frames.size(); i++) {
            StackFrame frame = frames.get(i);
            if (frame.getDeclaringClass() == owner && frame.getMethodName().equals(method) && frame.getDescriptor().equals(descriptor)) return i;
        }
        throw new IllegalStateException("Missing actual native frame: " + owner.getName() + "." + method + descriptor);
    }
    private static void nativeOnly(List<StackFrame> frames, int start, int end) {
        require(end >= start, "Reversed native stack spine");
        for (int i = start; i <= end; i++) {
            String name = frames.get(i).getClassName();
            require(name.startsWith("net.minecraft.") || name.startsWith("java.") || name.startsWith("jdk."),
                    "Foreign/direct caller in native loot/body spine: " + name);
        }
    }
    private static void spine(List<StackFrame> frames, int body) {
        int world = find(frames, body + 1, ServerLevel.class, "tick", TICK);
        int server = find(frames, world + 1, MinecraftServer.class, "tickServer", TICK);
        nativeOnly(frames, body, server);
    }
    private static void spawnPath(List<StackFrame> frames, int at) {
        int table = find(frames, at + 1, LivingEntity.class, "dropFromLootTable", TABLE);
        int loot = find(frames, table + 1, LivingEntity.class, "dropAllDeathLoot", LOOT);
        int die = find(frames, loot + 1, LivingEntity.class, "die", DIE);
        int body = find(frames, die + 1, ServerLevel.class, "tickNonPassenger", BODY);
        nativeOnly(frames, at, body);
        spine(frames, body);
    }
    private static List<Map<String, Object>> detached(List<StackFrame> frames) {
        var out = new ArrayList<Map<String, Object>>(frames.size());
        for (StackFrame frame : frames) out.add(row("class", frame.getClassName(), "method", frame.getMethodName(),
                "descriptor", frame.getDescriptor(), "bci", frame.getByteCodeIndex(), "line", frame.getLineNumber()));
        return List.copyOf(out);
    }
    private void emit(Map<String, Object> event) {
        require(++receiptCount <= MAX_RECEIPTS, "Ambient receipt limit");
        var out = new LinkedHashMap<String, Object>();
        out.put("schema", SCHEMA); out.put("native_tick", tick()); out.put("world_game_time", level.getGameTime());
        out.put("dimension", level.dimension().location().toString()); out.put("world_object", System.identityHashCode(level));
        out.put("server_object", System.identityHashCode(level.getServer())); out.put("observer_mutations", 0);
        out.putAll(event); receipts.accept(Map.copyOf(out));
    }
    private static List<Double> point(Vec3 value) { return List.of(value.x, value.y, value.z); }
    private static List<Double> box(AABB value) { return List.of(value.minX, value.minY, value.minZ, value.maxX, value.maxY, value.maxZ); }
    private static Map<String, Object> row(Object... values) {
        var out = new LinkedHashMap<String, Object>();
        for (int i = 0; i < values.length; i += 2) out.put((String) values[i], values[i + 1]);
        return Map.copyOf(out);
    }
    private static void require(boolean condition, String message) { if (!condition) throw new IllegalStateException(message); }

    private record Body(Mob source, UUID uuid, int tick, int preCounter, Map<String, Object> state,
                        List<Map<String, Object>> frames) { }
    private record Death(Mob source, DamageSource damage, int tick, long stamp, Map<String, Object> state,
                         List<Map<String, Object>> frames) { }
    private static final class Spawn {
        final int id, tick;
        final Mob source;
        final ItemStack offered;
        final float yOffset;
        final String stack;
        ItemEntity added;
        UUID itemUUID;
        Spawn(int id, Mob source, ItemStack offered, float yOffset, int tick, String stack) {
            this.id = id; this.source = source; this.offered = offered; this.yOffset = yOffset; this.tick = tick; this.stack = stack;
        }
    }
}
