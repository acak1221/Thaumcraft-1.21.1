package thaumcraft.fabric.clienttest;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/** Client-test-only detached evidence. No Minecraft dependencies or world callbacks.
 * One server-thread writer; the full dictionary/log is serialized only after stop. */
final class BoreClientEvidence {
    record Ref(String type, String version) {}
    private record Key(String type, Object value) {}
    private final Map<Key, Ref> interned = new HashMap<>();
    private final Map<String, Object> versions = new LinkedHashMap<>();
    private final Map<String, Object> published = Collections.unmodifiableMap(versions);

    Ref payloadVersion(String type, Object payload) {
        if (type == null || type.isEmpty()) throw new IllegalArgumentException("Missing evidence type");
        Object frozen = freeze(payload);
        Key key = new Key(type, frozen);
        Ref known = interned.get(key);
        if (known != null) return known;
        Ref ref = new Ref(type, "v" + (versions.size() + 1));
        var entry = new LinkedHashMap<String, Object>(); entry.put("type", type); entry.put("payload", frozen);
        versions.put(ref.version(), Collections.unmodifiableMap(entry));
        interned.put(key, ref);
        return ref;
    }

    Map<String, Object> versions() { return published; }
    int versionCount() { return versions.size(); }

    /** Rejects arbitrary Number, NBT, ItemStack, Entity, arrays and mutable POJOs.
     * The caller must observe those into a detached JSON-shaped representation. */
    static Object freeze(Object value) {
        if (value == null || value instanceof String || value instanceof Boolean || value instanceof Ref
                || value instanceof Byte || value instanceof Short || value instanceof Integer || value instanceof Long) return value;
        if (value instanceof Float f && Float.isFinite(f) || value instanceof Double d && Double.isFinite(d)) return value;
        if (value instanceof Map<?, ?> map) {
            var result = new TreeMap<String, Object>();
            for (var entry : map.entrySet()) {
                if (!(entry.getKey() instanceof String key)) throw new IllegalArgumentException("Evidence keys must be strings");
                result.put(key, freeze(entry.getValue()));
            }
            return Collections.unmodifiableMap(result);
        }
        if (value instanceof List<?> list) {
            var result = new ArrayList<Object>(list.size());
            for (Object item : list) result.add(freeze(item));
            return Collections.unmodifiableList(result);
        }
        throw new IllegalArgumentException("Not detached evidence: " + value.getClass().getName());
    }

    Object replay(Object encoded) {
        if (encoded instanceof Ref ref) {
            Object entry = versions.get(ref.version());
            if (!(entry instanceof Map<?, ?> map) || !ref.type().equals(map.get("type"))) throw new IllegalArgumentException("Unknown/mistyped evidence reference");
            return replay(map.get("payload"));
        }
        if (encoded instanceof Map<?, ?> map) {
            var result = new TreeMap<String, Object>();
            map.forEach((key, value) -> result.put((String) key, replay(value)));
            return Collections.unmodifiableMap(result);
        }
        if (encoded instanceof List<?> list) return list.stream().map(this::replay).toList();
        return encoded;
    }

    /** Append-efficient, unlike CopyOnWriteArrayList. Root must not traverse this
     * while the server writes; small progress reports must not include full logs. */
    static <T> List<T> log() { return Collections.synchronizedList(new ArrayList<>()); }

    /** Identity selection only, never a cached clock/tick substitute. Native
     * server POST/stop may outlive Minecraft's current client-server pointer. */
    static <T> T actualServerOwner(T currentClientServer,T boundActorServer) {
        if(boundActorServer==null) {
            if(currentClientServer==null)throw new IllegalStateException("No actual native server before actor binding");
            return currentClientServer;
        }
        if(currentClientServer!=null && currentClientServer!=boundActorServer)throw new IllegalStateException("Client switched away from the actual bound native server");
        return boundActorServer;
    }

    /** Detached ownership arithmetic ONLY. Actual filter/stack observations and
     * legitimate binding/return edges are checked separately by the Ledger. */
    static int metalLabelCustodyIndex(int exactBlankOwners, List<String> actualFilters) {
        if (exactBlankOwners < 0 || actualFilters.size() != 3) throw new IllegalStateException("Exact original three alembics and nonnegative label owners required");
        int installed = -1;
        for (int i = 0; i < actualFilters.size(); i++) {
            String filter = actualFilters.get(i);
            if ("".equals(filter)) continue;
            if (!"metallum".equals(filter) || installed >= 0) throw new IllegalStateException("Only one actual metallum label may be installed");
            installed = i;
        }
        if (exactBlankOwners + (installed >= 0 ? 1 : 0) != 1) throw new IllegalStateException("Original blank label must have exactly one physical OR installed owner");
        return installed;
    }

    record SourceBuffer(String type, int amount, String filter) {}
    /** Read-only servicing decision, not a furnace/pulse simulation. Retaining
     * nonempty typed buffers lets the REAL native first pass refill them all.
     * Emptying every positive unit instead reduces it to one new type per pulse. */
    static List<Integer> sourceBuffersToDrain(Map<String,Integer> furnace, List<SourceBuffer> buffers, boolean batchCooked) {
        if (buffers.size() != 3) throw new IllegalStateException("Only the actual original three buffers");
        int total = 0;
        for (var entry : furnace.entrySet()) {
            if (entry.getKey() == null || entry.getKey().isEmpty() || entry.getValue() == null || entry.getValue() <= 0 || entry.getValue() > 50) throw new IllegalStateException("Actual positive furnace tags required");
            total += entry.getValue();
        }
        if (total > 50) throw new IllegalStateException("Native furnace capacity50");
        var typed = new java.util.HashSet<String>(); boolean usableEmpty = false;
        for (var buffer : buffers) {
            if (buffer.type() == null || buffer.filter() == null || buffer.amount() < 0 || buffer.amount() > 32 || buffer.amount() > 0 && buffer.type().isEmpty()) throw new IllegalStateException("Actual native32-unit buffer schema");
            if (buffer.amount() > 0) typed.add(buffer.type());
            else if (buffer.filter().isEmpty() || furnace.containsKey(buffer.filter())) usableEmpty = true;
        }
        boolean waitingType = furnace.keySet().stream().anyMatch(tag -> !typed.contains(tag));
        var drain = new ArrayList<Integer>();
        for (int i = 0; i < buffers.size(); i++) {
            var buffer = buffers.get(i);
            if (buffer.amount() > 0 && (buffer.amount() == 32 || furnace.isEmpty() && batchCooked
                    || waitingType && !usableEmpty && buffer.filter().isEmpty() && !furnace.containsKey(buffer.type()))) drain.add(i);
        }
        drain.sort(java.util.Comparator.comparingInt(i -> buffers.get(i).type().equals("arbor") ? 0 : 1));
        return List.copyOf(drain);
    }

    /** Pure owner/lifetime/POST rule shared with the synthetic checks. Tokens are
     * observer-local monotonic IDs, never alleged native tick/payment evidence. */
    static final class BorePosts {
        private long owner, returnedOwner, returnedTick = -1, birthTick = -1, lastPost = -1;
        private String lifetime, returnedLifetime;
        void bind(long actualOwner, String actualLifetime) {
            if (actualOwner <= 0 || actualLifetime == null) throw new IllegalStateException("Missing current Bore identity");
            if (owner == 0) { owner = actualOwner; lifetime = actualLifetime; }
            else if (owner != actualOwner || !lifetime.equals(actualLifetime)) throw new IllegalStateException("Bore owner/lifetime replaced or reset");
        }
        void returned(long tick, long actualOwner, String actualLifetime) {
            bind(actualOwner, actualLifetime);
            if (returnedTick == tick) throw new IllegalStateException("Duplicate Bore RETURN");
            returnedTick = tick; returnedOwner = actualOwner; returnedLifetime = actualLifetime;
        }
        String post(long tick, boolean present, long actualOwner, String actualLifetime) {
            return post(tick, present, actualOwner, actualLifetime, false);
        }
        String post(long tick, boolean present, long actualOwner, String actualLifetime, boolean witnessedNormalStop) {
            if (lastPost >= 0 && tick != lastPost + 1) throw new IllegalStateException("Noncontiguous Bore POST");
            lastPost = tick;
            if (!present) {
                if (witnessedNormalStop) return "NORMAL_STOP_UNAVAILABLE_NO_TICK_WITNESS";
                if (owner != 0) throw new IllegalStateException("Previously observed Bore disappeared");
                return "NOT_PLACED";
            }
            bind(actualOwner, actualLifetime);
            if (returnedTick == tick && returnedOwner == actualOwner && actualLifetime.equals(returnedLifetime)) return "ACTUAL_NATIVE_RETURN";
            if (witnessedNormalStop) return "NORMAL_STOP_NO_TICK_WITNESS";
            if (birthTick < 0 && returnedTick < 0) { birthTick = tick; return "BIRTH_POST_NO_TICK_WITNESS"; }
            throw new IllegalStateException("Missing current-owner native Bore RETURN after birth POST");
        }
    }
}
