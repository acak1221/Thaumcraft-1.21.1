package thaumcraft.fabric.clienttest;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** Executes the actual detached helper, not MC stubs or a reimplemented driver.
 * All files must be NEW, under .tmp/bore-stop-tail-review1/sandbox/run-clienttest.
 * Simulated callbacks establish control-flow contracts only, NEVER native proof.
 */
public final class BoreStopTailChecks {
    private static int checks, scenarios, serial;
    private static Path sandbox;
    private static final List<String> ORDER = List.of("finalize", "post", "add", "private", "active", "encode");

    private static final class Driver implements BoreStopTail.Ops {
        final List<String> order = new ArrayList<>();
        final Map<String, Throwable> thrown = new LinkedHashMap<>();
        final Map<String, Object> report = new LinkedHashMap<>(Map.of("original_marker", "READ_REFERENCE_UNCHANGED"));
        Throwable root, latchDuringFinalizer, latchDuringEncoding;
        boolean encoderThrows, encoderNull, malformed, oversized, getterThrows, latchThrows;
        int encodes;
        Map<String, Object> envelope;

        private void action(String name) throws Throwable {
            order.add(name);
            if (name.equals("finalize") && latchDuringFinalizer != null) root = latchDuringFinalizer;
            if (thrown.containsKey(name)) throw thrown.get(name);
        }
        public void finalizeLedger() throws Throwable { action("finalize"); }
        public void unregisterServerPost() throws Throwable { action("post"); }
        public void unregisterEntityAdd() throws Throwable { action("add"); }
        public void unregisterPrivateStopHook() throws Throwable { action("private"); }
        public void clearActiveReference() throws Throwable { action("active"); }
        public Throwable rootFailure() throws Throwable {
            if (getterThrows) throw new IOException("injected root getter failure");
            return root;
        }
        public void latchFirstFailure(Throwable error) throws Throwable {
            if (latchThrows) throw new IOException("injected root latch failure");
            if (root == null) root = error;
        }
        public String encodeFullEnvelope(Map<String, Object> value) throws Throwable {
            order.add("encode"); encodes++; envelope = value;
            check(value.get("report") == report, "original report identity");
            check(report.equals(Map.of("original_marker", "READ_REFERENCE_UNCHANGED")), "report unchanged");
            primitive(value.get("stop_completion"));
            if (latchDuringEncoding != null) root = latchDuringEncoding;
            if (encoderThrows) throw new IOException("encoder \"boom\"\\\n\t\u0001 \ud800", new IllegalStateException("codec cause"));
            if (encoderNull) return null;
            if (malformed) return "{\"bad\":\"\ud800\"}";
            if (oversized) return "\u0800".repeat((int) (BoreStopTail.MAX_UTF8_BYTES / 3 + 1));
            return encode(value);
        }
    }

    private static BoreStopTail.Context context(boolean story, boolean baseline, boolean head) {
        String id = "client-smoke-" + UUID.nameUUIDFromBytes(("detached-stop-" + ++serial).getBytes(StandardCharsets.UTF_8));
        return new BoreStopTail.Context(id, sandbox.resolve("bore-tail-" + id + ".json"), story, baseline, head);
    }
    private static BoreStopTail.Result run(Driver driver, BoreStopTail.Context context) {
        scenarios++;
        var result = BoreStopTail.complete(context, driver.report, driver);
        check(driver.encodes == 1, "full encoder called once, no failed-Gson reuse");
        check(driver.order.equals(context.baselineReady() ? ORDER : ORDER.subList(1, ORDER.size())), "all real supplied callbacks in order");
        check(driver.report.size() == 1 && driver.report.get("original_marker").equals("READ_REFERENCE_UNCHANGED"), "no report mutations");
        primitive(result.completion());
        return result;
    }
    private static void check(boolean value, String why) {
        checks++;
        if (!value) throw new AssertionError(why);
    }
    private static void primitive(Object value) {
        if (value == null || value instanceof String || value instanceof Boolean || value instanceof Integer || value instanceof Long) {
            check(true, "diagnostic primitive"); return;
        }
        if (value instanceof Map<?, ?> map) {
            for (var entry : map.entrySet()) { check(entry.getKey() instanceof String, "primitive map key"); primitive(entry.getValue()); }
        } else if (value instanceof List<?> list) { for (var item : list) primitive(item); }
        else check(false, "raw Throwable or nonprimitive diagnostic");
    }
    private static Map<?, ?> object(Object value) { check(value instanceof Map, "JSON object"); return (Map<?, ?>) value; }
    private static Path proof(BoreStopTail.Context context) {
        return context.exactTailPath().resolveSibling(context.exactTailPath().getFileName() + ".write-proof.json");
    }
    private static Map<?, ?> read(Path path) throws IOException { return object(new Parser(Files.readString(path)).parse()); }
    private static void written(BoreStopTail.Context context, BoreStopTail.Result result, boolean full, String status) throws Exception {
        check(result.primaryCloseCompleted() && result.proofCloseCompleted(), "real writes and closes returned");
        check(result.status().equals(status) && result.tailStatus().equals(status), "honest result/tail status");
        check(result.fullEvidence() == full, "full vs diagnostic evidence");
        byte[] actual = Files.readAllBytes(context.exactTailPath());
        String sha = HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(actual));
        var tail = read(context.exactTailPath()); var writeProof = read(proof(context));
        check(tail.get("status").equals(status), "primary original status");
        check(tail.get("run_id").equals(context.runId()) && tail.get("story_complete").equals(context.storyComplete()), "legacy run/story keys");
        check(tail.containsKey("error") && tail.containsKey("root_stopped_audit"), "legacy error/audit keys");
        check(tail.containsKey("report") == full, "fallback contains NO full report");
        check(object(tail.get("stop_completion")).get("schema").equals("bore_stop_completion_v1"), "completion schema");
        check(writeProof.get("schema").equals("bore_stop_write_proof_v1"), "proof schema");
        check(writeProof.get("run_id").equals(context.runId()), "proof run");
        check(writeProof.get("tail_basename").equals(context.exactTailPath().getFileName().toString()), "proof exact basename");
        check(writeProof.get("sha256").equals(sha) && result.sha256().equals(sha), "proof hashes actual primary bytes");
        check(((Long) writeProof.get("UTF8bytes")) == actual.length && result.utf8Bytes() == actual.length, "UTF8 bytes including newline");
        check(writeProof.get("primary_close_completed").equals(true), "proof only after actual primary close");
        check(writeProof.get("full_evidence").equals(full) && writeProof.get("status").equals(status), "proof does not upgrade fallback");
        check(!object(tail.get("stop_completion")).containsKey("primary_close_completed"), "primary makes no premature close claim");
    }

    public static void main(String[] args) throws Exception {
        if (args.length != 1) throw new IllegalArgumentException("NEW review sandbox/run-clienttest path required");
        sandbox = Path.of(args[0]).toAbsolutePath().normalize();
        Path parent = sandbox.getParent();
        check(sandbox.getFileName().toString().equals("run-clienttest") && parent != null
                && parent.getFileName().toString().equals("sandbox")
                && parent.getParent().getFileName().toString().equals("bore-stop-tail-review1")
                && parent.getParent().getParent().getFileName().toString().equals(".tmp"), "isolated review sandbox only");
        Files.createDirectory(parent);
        Files.createDirectory(sandbox);

        var context = context(true, true, true); var driver = new Driver();
        var result = run(driver, context); written(context, result, true, "STOPPED_UNINSPECTED");
        check(driver.root == null, "healthy actual detached callbacks latch nothing");

        for (String name : ORDER.subList(0, 5)) {
            context = context(true, true, true); driver = new Driver();
            Throwable injected = new AssertionError("injected " + name, new IOException("stage cause"));
            driver.thrown.put(name, injected); result = run(driver, context);
            written(context, result, true, "FAILED_OR_INCOMPLETE");
            check(driver.root == injected, "first thrown stage retained by root");
            var stage = name.equals("finalize") ? object(result.completion().get("ledger_finalization"))
                    : object(object(result.completion().get("server_cleanup")).get(switch (name) {
                        case "post" -> "SERVER_POST_unregister"; case "add" -> "ADD_unregister";
                        case "private" -> "private_stop_hook_unregister"; default -> "active_reference_clear";
                    }));
            check(stage.get("entered").equals(true) && stage.get("returned").equals(false), "thrown actual callback not complete");
        }

        context = context(true, true, true); driver = new Driver();
        Throwable original = new IllegalStateException("original root first"); driver.root = original;
        for (String name : ORDER.subList(0, 5)) driver.thrown.put(name, new IOException("separate " + name));
        result = run(driver, context); written(context, result, true, "FAILED_OR_INCOMPLETE");
        check(driver.root == original && object(result.completion().get("preexisting_root_failure")).containsKey("cause_chain"), "preexisting root preserved");
        check(((List<?>) result.completion().get("stop_errors")).size() == 6, "original plus all five separate stop failures retained");

        context = context(true, true, true); driver = new Driver();
        Throwable firstStop = new IOException("first actual stop error");
        driver.thrown.put("finalize", firstStop);
        for (String name : ORDER.subList(1, 5)) driver.thrown.put(name, new IOException("later " + name));
        result = run(driver, context); written(context, result, true, "FAILED_OR_INCOMPLETE");
        check(driver.root == firstStop && ((List<?>) result.completion().get("stop_errors")).size() == 5, "first stop failure preserved across later errors");

        context = context(true, true, true); driver = new Driver();
        driver.latchDuringFinalizer = new IOException("helper merely latched");
        result = run(driver, context); written(context, result, true, "FAILED_OR_INCOMPLETE");
        check(object(result.completion().get("ledger_finalization")).get("returned").equals(true), "return does not erase helper latch");

        context = context(true, true, true); driver = new Driver(); driver.root = original;
        driver.latchDuringFinalizer = new IOException("broken callback replaced root");
        result = run(driver, context); written(context, result, true, "FAILED_OR_INCOMPLETE");
        check(result.completion().get("first_root_failure_preserved").equals(false), "root replacement detected, never papered over");

        for (int missing = 0; missing < 3; missing++) {
            context = context(missing != 0, missing != 1, missing != 2); driver = new Driver();
            result = run(driver, context); written(context, result, true, "FAILED_OR_INCOMPLETE");
            if (missing == 1) check(object(result.completion().get("ledger_finalization")).get("state").equals("NOT_RUN"), "missing baseline cannot call fake finalizer");
        }

        context = context(true, true, true); driver = new Driver(); driver.root = new IOException("pending root failure");
        result = run(driver, context); written(context, result, true, "FAILED_OR_INCOMPLETE");

        for (int encodingFault = 0; encodingFault < 5; encodingFault++) {
            context = context(true, true, true); driver = new Driver();
            driver.encoderThrows = encodingFault == 0; driver.encoderNull = encodingFault == 1;
            driver.malformed = encodingFault == 2; driver.oversized = encodingFault == 3;
            if (encodingFault == 4) driver.latchDuringEncoding = new IOException("encoder latched, returned text");
            result = run(driver, context); written(context, result, false, "FAILED_DIAGNOSTIC_ONLY");
            check(!Files.readString(context.exactTailPath()).contains("READ_REFERENCE_UNCHANGED"), "fallback cannot leak full evidence");
            check(driver.root != null, "encoding failure latched");
        }

        context = context(true, true, true); driver = new Driver();
        Files.writeString(context.exactTailPath(), "PREEXISTING_PRIMARY", StandardOpenOption.CREATE_NEW);
        result = run(driver, context);
        check(result.status().equals("FAILED_OR_INCOMPLETE") && !result.primaryCloseCompleted() && !result.proofCloseCompleted(), "existing primary is IO failure");
        check(Files.readString(context.exactTailPath()).equals("PREEXISTING_PRIMARY") && !Files.exists(proof(context)), "no overwrite/rewrite/proof for existing primary");

        context = context(true, true, true); driver = new Driver();
        Files.writeString(proof(context), "PREEXISTING_PROOF", StandardOpenOption.CREATE_NEW);
        result = run(driver, context);
        check(result.status().equals("FAILED_OR_INCOMPLETE") && result.primaryCloseCompleted() && !result.proofCloseCompleted(), "proof collision explicitly fails result");
        check(Files.readString(proof(context)).equals("PREEXISTING_PROOF"), "existing proof untouched");
        check(read(context.exactTailPath()).get("status").equals("STOPPED_UNINSPECTED"), "cannot rewrite closed primary after proof failure");
        check(driver.root != null, "proof IO failure latched");

        context = context(true, true, true); driver = new Driver(); Files.createDirectory(context.exactTailPath());
        result = run(driver, context);
        check(result.status().equals("FAILED_OR_INCOMPLETE") && !result.primaryCloseCompleted() && !Files.exists(proof(context)), "invalid primary directory no proof");

        var missingParent = sandbox.resolve("absent-parent").resolve("run-clienttest");
        context = context(true, true, true);
        context = new BoreStopTail.Context(context.runId(), missingParent.resolve(context.exactTailPath().getFileName()), true, true, true);
        driver = new Driver(); result = run(driver, context);
        check(result.status().equals("FAILED_OR_INCOMPLETE") && !result.primaryCloseCompleted() && !Files.exists(missingParent), "writer does not create directories");

        context = context(true, true, true); driver = new Driver(); driver.getterThrows = true; driver.latchThrows = true;
        result = run(driver, context); written(context, result, false, "FAILED_DIAGNOSTIC_ONLY");
        check(result.completion().get("root_failure_read_complete").equals(false), "unreadable root cannot pass");

        context = context(true, true, true); driver = new Driver(); driver.latchThrows = true;
        driver.thrown.put("finalize", new IOException("first error cannot be retained"));
        result = run(driver, context); written(context, result, true, "FAILED_OR_INCOMPLETE");
        check(result.completion().get("first_root_failure_preserved").equals(false), "broken latch cannot pass");

        context = context(true, true, true); driver = new Driver();
        Throwable chain = new IOException("deep cause");
        for (int i = 0; i < 12; i++) chain = new IOException("long-" + "x".repeat(2000), chain);
        driver.thrown.put("finalize", chain); result = run(driver, context);
        written(context, result, true, "FAILED_OR_INCOMPLETE");
        var error = object(((Map<?, ?>) ((List<?>) result.completion().get("stop_errors")).get(0)).get("error"));
        check(((List<?>) error.get("cause_chain")).size() == 8 && error.get("cause_chain_truncated").equals(true), "bounded diagnostic cause chain");
        check(Files.size(context.exactTailPath()) < 100_000, "bounded diagnostic text, not full-report truncation");

        invalidContext("client-smoke-1-1-1-1-1", sandbox.resolve("wrong.json"));
        invalidContext("client-smoke-ABCDEFAB-1234-1234-1234-ABCDEFABCDEF", sandbox.resolve("wrong.json"));
        invalidContext(context.runId(), sandbox.resolve("wrong.json"));
        invalidContext(context.runId(), sandbox.getParent().resolve(context.exactTailPath().getFileName()));
        check(BoreStopTail.MAX_UTF8_BYTES == 536870912L, "reader bound not expanded");
        System.out.println("ACTUAL detached BoreStopTail Java assertions: " + checks + "; scenarios: " + scenarios
                + "; native/world/Gradle evidence: NONE");
    }

    private static void invalidContext(String id, Path path) {
        boolean denied = false;
        try { new BoreStopTail.Context(id, path, true, true, true); }
        catch (IllegalArgumentException expected) { denied = true; }
        check(denied, "invalid run/path rejected");
    }

    // Independent minimal test encoder/parser; production full encoder remains root-owned Gson.
    private static String encode(Object value) {
        if (value == null) return "null";
        if (value instanceof Boolean || value instanceof Number) return value.toString();
        if (value instanceof Map<?, ?> map) {
            var parts = new ArrayList<String>();
            for (var entry : map.entrySet()) parts.add(encode(entry.getKey()) + ":" + encode(entry.getValue()));
            return "{" + String.join(",", parts) + "}";
        }
        if (value instanceof List<?> list) return "[" + String.join(",", list.stream().map(BoreStopTailChecks::encode).toList()) + "]";
        if (!(value instanceof String text)) throw new AssertionError("test encoder nonprimitive");
        var out = new StringBuilder("\"");
        for (char c : text.toCharArray()) {
            if (c == '"' || c == '\\') out.append('\\').append(c);
            else if (c < 32 || Character.isSurrogate(c)) out.append(String.format("\\u%04x", (int) c));
            else out.append(c);
        }
        return out.append('"').toString();
    }

    private static final class Parser {
        private final String text;
        private int at;
        Parser(String text) { this.text = text; }
        Object parse() { Object result = value(); whitespace(); check(at == text.length(), "valid JSON end"); return result; }
        private void whitespace() { while (at < text.length() && Character.isWhitespace(text.charAt(at))) at++; }
        private char next() { return text.charAt(at++); }
        private void expect(char c) { whitespace(); check(next() == c, "JSON punctuation " + c); }
        private Object value() {
            whitespace(); char c = text.charAt(at);
            if (c == '"') return string();
            if (c == '{') {
                at++; var map = new LinkedHashMap<String, Object>(); whitespace();
                if (text.charAt(at) == '}') { at++; return map; }
                do { String key = string(); expect(':'); check(!map.containsKey(key), "no duplicate JSON keys"); map.put(key, value()); whitespace(); }
                while (next() == ',');
                check(text.charAt(at - 1) == '}', "JSON object end"); return map;
            }
            if (c == '[') {
                at++; var list = new ArrayList<Object>(); whitespace();
                if (text.charAt(at) == ']') { at++; return list; }
                do { list.add(value()); whitespace(); } while (next() == ',');
                check(text.charAt(at - 1) == ']', "JSON array end"); return list;
            }
            for (String token : List.of("true", "false", "null")) if (text.startsWith(token, at)) {
                at += token.length(); return token.equals("null") ? null : token.equals("true");
            }
            int start = at; while (at < text.length() && Character.isDigit(text.charAt(at))) at++;
            check(at > start, "JSON integer"); return Long.parseLong(text.substring(start, at));
        }
        private String string() {
            expect('"'); var out = new StringBuilder();
            for (char c; (c = next()) != '"';) {
                check(c >= 32, "no raw JSON controls");
                if (c != '\\') { out.append(c); continue; }
                char escaped = next();
                switch (escaped) {
                    case '"', '\\', '/' -> out.append(escaped);
                    case 'b' -> out.append('\b'); case 'f' -> out.append('\f');
                    case 'n' -> out.append('\n'); case 'r' -> out.append('\r'); case 't' -> out.append('\t');
                    case 'u' -> { out.append((char) Integer.parseInt(text.substring(at, at + 4), 16)); at += 4; }
                    default -> throw new AssertionError("Invalid JSON escape");
                }
            }
            return out.toString();
        }
    }
}
