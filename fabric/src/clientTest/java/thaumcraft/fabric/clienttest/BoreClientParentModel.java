package thaumcraft.fabric.clienttest;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.zip.GZIPInputStream;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.TagParser;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

/** Expectations only. Never installs a tag/entity/block or opens a native world.
 * Exactly113 BE tags,72 candle cells and11 protected NPCs from the bound parent.
 * The resource is compressed metadata, NOT a region/save or runtime observation. */
final class BoreClientParentModel {
    static final String RESOURCE = "/thaumcraft-clienttest/bore-parent-model.json";
    static final String PARENT = "cbcb22db9beb7056d0a61d08852bfb5bd5df38d22c58e78baa4d7de24ba58fd3";
    static final String AUDIT = "b47eb17cfc7e78a47c36acb03f3fc14505638cf1ab8498663a6b244043b21ab0";
    static final String DECODED = "4d8a0994f908db094a3f48b934fd0a1735c0313b9c5cfbe332c7aff809dc7d7d";
    static final Set<UUID> LOCAL = Set.of(UUID.fromString("8fc5e5ae-b6c5-4974-8a22-5e67b695aa57"),
            UUID.fromString("9f1565fb-ee3e-4e92-8f72-3254c618e50e"), UUID.fromString("d188f5f2-0e95-4893-b927-9a101ac3b7e5"));
    record ExpectedBE(BlockPos pos, String type, State state, String snbt) {
        CompoundTag tag() {
            try { return TagParser.parseTag(snbt); }
            catch (Exception error) { throw new IllegalStateException("Invalid trusted BE tag at " + pos, error); }
        }
    }
    record Candle(BlockPos pos, State state) {}
    record State(String name, Map<String, String> properties) {
        boolean matches(BlockState actual) {
            if (!name.equals(BuiltInRegistries.BLOCK.getKey(actual.getBlock()).toString())) return false;
            var observed = new LinkedHashMap<String, String>();
            for (var property : actual.getProperties()) observed.put(property.getName(), propertyValue(actual, property));
            return properties.equals(observed);
        }
    }
    private static <T extends Comparable<T>> String propertyValue(BlockState state, net.minecraft.world.level.block.state.properties.Property<T> property) {
        return property.getName(state.getValue(property));
    }
    record Cow(UUID uuid, String type, Vec3 pos, float health, boolean noAi, boolean persistent, boolean invulnerable, String nameJson, String literalName) {
        void validate(LivingEntity actual, ServerLevel level, float rawHealth) {
            if (!uuid.equals(actual.getUUID()) || !type.equals(BuiltInRegistries.ENTITY_TYPE.getKey(actual.getType()).toString())
                    || !pos.equals(actual.position()) || Float.floatToIntBits(rawHealth) != Float.floatToIntBits(health)
                    || !(actual instanceof Mob mob) || mob.isNoAi() != noAi || mob.isPersistenceRequired() != persistent
                    || actual.isInvulnerable() != invulnerable || !actual.hasCustomName()
                    || !Component.literal(literalName).equals(actual.getCustomName()))
                throw new IllegalStateException("Trusted parent protected cow differs: " + uuid);
        }
        Map<String, Object> report() {
            return Map.of("UUID", uuid.toString(), "type", type, "position", List.of(pos.x, pos.y, pos.z), "health", health,
                    "NoAI", noAi, "persistent", persistent, "invulnerable", invulnerable, "name_json", nameJson,
                    "unseen_intervals", "STOPPED_AUDIT_REQUIRED");
        }
    }
    final Map<BlockPos, ExpectedBE> bes;
    final Map<UUID, Cow> cows;
    final List<Candle> candles;

    /** Vanilla chunk saving adds keepPacked=false to a live owner's tag. The
     * owner's saveWithFullMetadata does not. Normalize ONLY that proven outer
     * envelope difference, on a copy; true/typed/nested data stay strict. */
    static CompoundTag loadedOwnerExpected(CompoundTag chunkTag, CompoundTag liveTag) {
        CompoundTag compared=chunkTag.copy();
        if(compared.contains("keepPacked",net.minecraft.nbt.Tag.TAG_BYTE) && !compared.getBoolean("keepPacked")
                && !liveTag.contains("keepPacked")) compared.remove("keepPacked");
        return compared;
    }

    BoreClientParentModel() {
        try (var in = BoreClientParentModel.class.getResourceAsStream(RESOURCE)) {
            if (in == null) throw new IllegalStateException("Missing isolated parent metadata resource " + RESOURCE);
            JsonObject wrapper = JsonParser.parseString(new String(in.readAllBytes(), StandardCharsets.UTF_8)).getAsJsonObject();
            if (!"gzip-base64".equals(wrapper.get("encoding").getAsString())) throw new IllegalStateException("Unknown metadata encoding");
            byte[] decoded;
            try (var zip = new GZIPInputStream(new ByteArrayInputStream(Base64.getMimeDecoder().decode(wrapper.get("payload").getAsString())))) {
                decoded = zip.readNBytes(39_281);
            }
            String hash = HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(decoded));
            if (decoded.length != 39_280 || !DECODED.equals(hash) || !DECODED.equals(wrapper.get("decoded_sha256").getAsString())
                    || wrapper.get("decoded_bytes").getAsInt() != decoded.length) throw new IllegalStateException("Parent metadata bytes/hash mismatch");
            JsonObject root = JsonParser.parseString(new String(decoded, StandardCharsets.UTF_8)).getAsJsonObject();
            if (root.get("schema").getAsInt() != 1 || !PARENT.equals(root.get("parent_manifest_sha256").getAsString())
                    || !AUDIT.equals(root.get("stopped_audit_sha256").getAsString())) throw new IllegalStateException("Parent provenance mismatch");
            var beRows = new LinkedHashMap<BlockPos, ExpectedBE>();
            for (var element : root.getAsJsonArray("block_entities")) {
                var r = element.getAsJsonObject(); BlockPos pos = pos(r.getAsJsonArray("pos"));
                var expected = new ExpectedBE(pos, r.get("type").getAsString(), state(r.getAsJsonObject("state")), r.get("tag").getAsString());
                CompoundTag tag = expected.tag();
                if (!expected.type().equals(tag.getString("id")) || tag.getInt("x") != pos.getX() || tag.getInt("y") != pos.getY() || tag.getInt("z") != pos.getZ()
                        || beRows.put(pos, expected) != null) throw new IllegalStateException("Duplicate/invalid parent BE");
            }
            var cowRows = new LinkedHashMap<UUID, Cow>();
            for (var element : root.getAsJsonArray("protected_cows")) {
                var r = element.getAsJsonObject(); var a = r.getAsJsonArray("pos"); UUID id = UUID.fromString(r.get("uuid").getAsString());
                var cow = new Cow(id, r.get("type").getAsString(), new Vec3(a.get(0).getAsDouble(), a.get(1).getAsDouble(), a.get(2).getAsDouble()),
                        r.get("health").getAsFloat(), r.get("no_ai").getAsBoolean(), r.get("persistent").getAsBoolean(), r.get("invulnerable").getAsBoolean(), r.get("name_json").getAsString(),
                        JsonParser.parseString(r.get("name_json").getAsString()).getAsString());
                if (cowRows.put(id, cow) != null || cow.health() != 6 || !cow.noAi() || !cow.persistent()) throw new IllegalStateException("Invalid parent cow");
            }
            var candleRows = new ArrayList<Candle>(); var candleCells = new java.util.HashSet<BlockPos>();
            for (var element : root.getAsJsonArray("candles")) {
                var r = element.getAsJsonObject(); BlockPos pos = pos(r.getAsJsonArray("pos"));
                if (!candleCells.add(pos)) throw new IllegalStateException("Duplicate parent candle");
                candleRows.add(new Candle(pos, state(r.getAsJsonObject("state"))));
            }
            if (beRows.size() != 113 || cowRows.size() != 11 || !cowRows.keySet().containsAll(LOCAL) || candleRows.size() != 72) throw new IllegalStateException("Parent coverage cardinality mismatch");
            bes = java.util.Collections.unmodifiableMap(beRows); cows = java.util.Collections.unmodifiableMap(cowRows); candles = List.copyOf(candleRows);
        } catch (Exception error) { throw new IllegalStateException("Trusted parent expectation metadata unavailable", error); }
    }
    private static BlockPos pos(JsonArray a) { return new BlockPos(a.get(0).getAsInt(), a.get(1).getAsInt(), a.get(2).getAsInt()); }
    private static State state(JsonObject r) {
        var properties = new LinkedHashMap<String, String>();
        if (r.has("Properties")) r.getAsJsonObject("Properties").entrySet().forEach(e -> properties.put(e.getKey(), e.getValue().getAsString()));
        return new State(r.get("Name").getAsString(), Map.copyOf(properties));
    }
}
