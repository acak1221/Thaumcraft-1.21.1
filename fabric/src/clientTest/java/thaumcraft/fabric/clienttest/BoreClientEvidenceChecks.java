package thaumcraft.fabric.clienttest;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.BufferedWriter;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.security.DigestOutputStream;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Standalone clientTest-only fixture checks; NO native callbacks, game/world,
 * rendering, inherited NPC observation or gameplay proof. Root executes AFTER
 * source freeze with the real Java21 clientTest classpath, never as a stub test. */
public final class BoreClientEvidenceChecks {
    private BoreClientEvidenceChecks() {}
    private static void check(boolean value, String message) { if (!value) throw new AssertionError(message); }
    private static void rejects(Runnable action) {
        try { action.run(); } catch (IllegalArgumentException | IllegalStateException expected) { return; }
        throw new AssertionError("Expected rejection");
    }
    private static Map<String, Object> snapshot(int tick) {
        var inventory = new ArrayList<Object>();
        for (int i = 0; i < 36; i++) inventory.add(Map.of("slot", i, "stack", "synthetic:item" + i + ":" + (i == 10 ? tick / 400 : 0)));
        var tags = new ArrayList<Object>();
        for (int i = 0; i < 113; i++) tags.add(Map.of("pos", "fixture:" + i, "tag", "{fixture:" + i + ",stock:" + (i == 0 ? tick / 500 : 0) + "}"));
        return Map.of("inventory", inventory, "BE", tags, "cursor", "EMPTY", "knowledge", Map.of("fixture", "not native research"));
    }
    private static BoreClientEvidence.Ref normalize(BoreClientEvidence evidence, Map<String, Object> raw) {
        var inventory = new ArrayList<Object>();
        for (Object value : (List<?>) raw.get("inventory")) {
            var row = (Map<?, ?>) value;
            inventory.add(evidence.payloadVersion("inventory_slot", Map.of("slot", row.get("slot"), "stack", evidence.payloadVersion("stack", row.get("stack")))));
        }
        var tags = new ArrayList<Object>();
        for (Object value : (List<?>) raw.get("BE")) {
            var row = (Map<?, ?>) value;
            tags.add(evidence.payloadVersion("BE_row", Map.of("pos", row.get("pos"), "tag", evidence.payloadVersion("BE_tag", row.get("tag")))));
        }
        return evidence.payloadVersion("fixture_snapshot", Map.of("inventory", evidence.payloadVersion("inventory", inventory),
                "BE", evidence.payloadVersion("BE_rows", tags), "cursor", evidence.payloadVersion("cursor", raw.get("cursor")),
                "knowledge", evidence.payloadVersion("knowledge", raw.get("knowledge"))));
    }
    private static JsonElement replayJson(JsonElement value, JsonObject versions) {
        if (value.isJsonObject()) {
            JsonObject object = value.getAsJsonObject();
            if (object.size() == 2 && object.has("type") && object.has("version")) {
                JsonObject entry = versions.getAsJsonObject(object.get("version").getAsString());
                check(entry != null && entry.get("type").equals(object.get("type")), "Serialized reference type/version");
                return replayJson(entry.get("payload"), versions);
            }
            var out = new JsonObject(); object.entrySet().forEach(e -> out.add(e.getKey(), replayJson(e.getValue(), versions))); return out;
        }
        if (value.isJsonArray()) { var out = new JsonArray(); value.getAsJsonArray().forEach(v -> out.add(replayJson(v, versions))); return out; }
        return value.deepCopy();
    }
    private static void aliasChecks() {
        var evidence = new BoreClientEvidence();
        var list = new ArrayList<Object>(List.of("old"));
        var map = new LinkedHashMap<String, Object>(); map.put("nested", list);
        var ref = evidence.payloadVersion("mutable", map);
        list.set(0, "CHANGED"); map.put("injected", true);
        check(evidence.replay(ref).equals(Map.of("nested", List.of("old"))), "Mutable aliases changed old evidence");
        check(ref.equals(evidence.payloadVersion("mutable", Map.of("nested", List.of("old")))), "Equal immutable payload did not intern");
        check(!ref.equals(evidence.payloadVersion("different_type", Map.of("nested", List.of("old")))), "Types were conflated");
        rejects(() -> evidence.payloadVersion("bad", new Object()));
        rejects(() -> evidence.payloadVersion("bad", new java.util.concurrent.atomic.AtomicInteger(1)));
        rejects(() -> evidence.replay(new BoreClientEvidence.Ref("mutable", "missing")));
        check(evidence.replay(evidence.payloadVersion("null_payload", null)) == null, "JSON null payload");
        try { evidence.versions().put("bad", 1); throw new AssertionError("Mutable dictionary view"); }
        catch (UnsupportedOperationException expected) { /* read-only publication */ }
    }
    private static void boreChecks() {
        var birth = new BoreClientEvidence.BorePosts();
        check("NOT_PLACED".equals(birth.post(10, false, 0, null)), "Unplaced status");
        check("BIRTH_POST_NO_TICK_WITNESS".equals(birth.post(11, true, 1, "lifeA")), "Birth is not a fake RETURN");
        birth.returned(12, 1, "lifeA");
        check("ACTUAL_NATIVE_RETURN".equals(birth.post(12, true, 1, "lifeA")), "Actual paired owner RETURN");
        rejects(() -> birth.post(13, true, 1, "lifeA"));
        var missing = new BoreClientEvidence.BorePosts(); missing.post(20, true, 1, "lifeA");
        rejects(() -> missing.post(21, true, 1, "lifeA"));
        var replacement = new BoreClientEvidence.BorePosts(); replacement.returned(30, 1, "lifeA");
        rejects(() -> replacement.post(30, true, 2, "lifeB"));
        var reset = new BoreClientEvidence.BorePosts(); reset.returned(40, 1, "lifeA");
        rejects(() -> reset.post(40, true, 1, "newLifetime"));
        var duplicate = new BoreClientEvidence.BorePosts(); duplicate.returned(50, 1, "lifeA");
        rejects(() -> duplicate.returned(50, 1, "lifeA"));
        var gap = new BoreClientEvidence.BorePosts(); gap.post(60, false, 0, null);
        rejects(() -> gap.post(62, false, 0, null));
        var removed = new BoreClientEvidence.BorePosts(); removed.returned(70, 1, "lifeA"); removed.post(70, true, 1, "lifeA");
        rejects(() -> removed.post(71, false, 0, null));
        var stop = new BoreClientEvidence.BorePosts(); stop.returned(80, 1, "lifeA"); stop.post(80, true, 1, "lifeA");
        check("NORMAL_STOP_NO_TICK_WITNESS".equals(stop.post(81, true, 1, "lifeA", true)), "Stop must not manufacture RETURN");
        rejects(() -> stop.post(82, true, 2, "lifeB", true));
        var stopAbsent = new BoreClientEvidence.BorePosts(); stopAbsent.returned(90, 1, "lifeA"); stopAbsent.post(90, true, 1, "lifeA");
        check("NORMAL_STOP_UNAVAILABLE_NO_TICK_WITNESS".equals(stopAbsent.post(91, false, 0, null, true)), "Explicit terminal absence is not a tick witness");
    }
    private static void chunkEnvelopeChecks() {
        // Real Minecraft NBT classes, detached metadata only; no world load.
        var model=new BoreClientParentModel();
        check(model.bes.size()==113 && model.cows.size()==11 && model.candles.size()==72,"Exact bound parent metadata parses on the actual classpath");
        var saved=model.bes.values().stream().filter(v->v.pos().getX()==1458 && v.pos().getY()==216 && v.pos().getZ()==377).findFirst().orElseThrow().tag();
        var original=saved.copy();var live=saved.copy();live.remove("keepPacked");
        check(BoreClientParentModel.loadedOwnerExpected(saved,live).equals(live) && saved.equals(original),"Only detached false outer chunk envelope is normalized");
        var packed=saved.copy();packed.putBoolean("keepPacked",true);
        check(!BoreClientParentModel.loadedOwnerExpected(packed,live).equals(live),"True packed ownership must not be silently stripped");
        var typed=saved.copy();typed.putInt("keepPacked",0);
        check(!BoreClientParentModel.loadedOwnerExpected(typed,live).equals(live),"Unexpected typed header must not be silently stripped");
        var changed=live.copy();changed.putString("unrelated_actual_stock","unexpected");
        check(!BoreClientParentModel.loadedOwnerExpected(saved,changed).equals(changed),"Header normalization must not hide any other native metadata change");
        check(BoreClientParentModel.loadedOwnerExpected(saved,saved).equals(saved),"An explicitly present live header remains part of exact comparison");
    }
    public static void main(String[] args) throws Exception {
        if (args.length != 1) throw new IllegalArgumentException("One NEW result JSON path required");
        Path result = Path.of(args[0]); Path stream = result.resolveSibling(result.getFileName() + ".synthetic.json");
        aliasChecks(); boreChecks();chunkEnvelopeChecks();
        var evidence = new BoreClientEvidence(); List<Map<String, Object>> posts = BoreClientEvidence.log();
        Gson gson = new GsonBuilder().disableHtmlEscaping().create(); long rawBytes = 0;
        for (int tick = 0; tick < 16_000; tick++) {
            var raw = snapshot(tick); var ref = normalize(evidence, raw);
            check(raw.equals(evidence.replay(ref)), "Immediate lossless replay at " + tick);
            rawBytes += gson.toJson(raw).getBytes(StandardCharsets.UTF_8).length;
            posts.add(Map.of("POST", tick + 1, "server_tick", tick + 1000, "failed_tail_fixture", tick >= 15_900, "snapshot", ref));
        }
        for (int tick = 0; tick < posts.size(); tick++) {
            var row = posts.get(tick);
            check(((Number) row.get("POST")).intValue() == tick + 1 && ((Number) row.get("server_tick")).intValue() == tick + 1000, "Noncontiguous fixture POST");
            check(snapshot(tick).equals(evidence.replay(row.get("snapshot"))), "Retained lossless replay at " + tick);
        }
        int versions = evidence.versionCount();
        normalize(evidence, snapshot(15_999)); check(versions == evidence.versionCount(), "Unchanged payload added dictionary version");
        // Initial303 +39*(stack,row,inventory) +31*(tag,row,BE-list)
        // +63 distinct subsequent (400/500) combined snapshot boundaries.
        check(versions == 576, "Exact finite fixture version count changed: " + versions);
        var same = new BoreClientEvidence();
        check(normalize(same, snapshot(0)).equals(posts.getFirst().get("snapshot")), "IDs depend on non-deterministic identity/RNG");
        var digest = MessageDigest.getInstance("SHA-256");
        try (var out = new DigestOutputStream(Files.newOutputStream(stream, StandardOpenOption.CREATE_NEW, StandardOpenOption.WRITE), digest);
             var writer = new BufferedWriter(new OutputStreamWriter(out, StandardCharsets.UTF_8))) {
            gson.toJson(Map.of("fixture_only", true, "payload_versions", evidence.versions(), "POSTs", posts), writer); writer.write('\n');
        }
        long streamBytes = Files.size(stream);
        check(streamBytes < rawBytes, "Fixture normalization did not reduce repeated payload output");
        try (var reader = Files.newBufferedReader(stream, StandardCharsets.UTF_8)) {
            var persisted = JsonParser.parseReader(reader).getAsJsonObject(); var dictionary = persisted.getAsJsonObject("payload_versions");
            var actualPosts = persisted.getAsJsonArray("POSTs");check(actualPosts.size() == 16_000, "Stream lost POSTs");
            for (int tick = 0; tick < actualPosts.size(); tick++) {
                var actual = actualPosts.get(tick).getAsJsonObject();
                check(actual.get("server_tick").getAsInt() == tick + 1000 && actual.get("POST").getAsInt() == tick + 1, "Serialized monotonic POSTs");
                check(replayJson(actual.get("snapshot"), dictionary).equals(gson.toJsonTree(snapshot(tick))), "Serialized lossless replay at " + tick);
            }
        }
        try (var writer = Files.newBufferedWriter(result, StandardCharsets.UTF_8, StandardOpenOption.CREATE_NEW, StandardOpenOption.WRITE)) {
            gson.toJson(Map.of("status", "PURE_SYNTHETIC_CHECKS_PASSED_NOT_GAMEPLAY", "POSTs", posts.size(), "versions", versions,
                    "raw_snapshot_JSON_bytes_sum", rawBytes, "normalized_stream_bytes", streamBytes,
                     "normalized_stream_sha256", HexFormat.of().formatHex(digest.digest()), "stream", stream.toString(),
                     "detached_parent_metadata_and_header_checks",6,
                    "limitations", "No game/native callbacks/render/NPC/BE audit or actual game heap measurement; fixture failure-tail rows are not native evidence"), writer);
            writer.write('\n');
        }
    }
}
