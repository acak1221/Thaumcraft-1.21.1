package thaumcraft.fabric.clienttest;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HexFormat;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

/** Private stop-report control flow, NOT native validation or stock authority.
 * Root supplies the actual callbacks and an unchanged report read reference.
 * No client task is submitted, awaited, or described as completed here.
 * CREATE_NEW plus successful close is not fsync, crash safety, or acceptance.
 * A missed callback, killed JVM, OOM, unavailable console/storage, existing file,
 * or IO failure can prevent a usable tail; never retry/reconstruct that evidence.
 */
final class BoreStopTail {
    static final long MAX_UTF8_BYTES = 512L * 1024 * 1024;
    private static final String STOPPED = "STOPPED_UNINSPECTED";
    private static final String FAILED = "FAILED_OR_INCOMPLETE";
    private static final String DIAGNOSTIC = "FAILED_DIAGNOSTIC_ONLY";
    private static final String AUDIT = "REQUIRED; observer never saves or restores world";
    private static final int CAUSES = 8, TEXT = 1024;

    record Context(String runId, Path exactTailPath, boolean storyComplete,
                   boolean baselineReady, boolean actualHeadReturned) {
        Context {
            Objects.requireNonNull(runId, "runId");
            Objects.requireNonNull(exactTailPath, "exactTailPath");
            String prefix = "client-smoke-";
            if (!runId.startsWith(prefix)
                    || !UUID.fromString(runId.substring(prefix.length())).toString()
                    .equals(runId.substring(prefix.length())))
                throw new IllegalArgumentException("Canonical client-smoke UUID required");
            exactTailPath = exactTailPath.toAbsolutePath().normalize();
            Path parent = exactTailPath.getParent();
            if (parent == null || parent.getFileName() == null
                    || !parent.getFileName().toString().equals("run-clienttest")
                    || !exactTailPath.getFileName().toString().equals("bore-tail-" + runId + ".json"))
                throw new IllegalArgumentException("Exact root-owned run-clienttest tail required");
            // Root owns/validates the directory. Do not create it or inspect a save.
        }
    }

    interface Ops {
        void finalizeLedger() throws Throwable;
        void unregisterServerPost() throws Throwable;
        void unregisterEntityAdd() throws Throwable;
        void unregisterPrivateStopHook() throws Throwable;
        void clearActiveReference() throws Throwable;
        Throwable rootFailure() throws Throwable;
        /** Must retain the first root failure; helper only offers its first observed error. */
        void latchFirstFailure(Throwable error) throws Throwable;
        /** Root builds Gson and encodes this FULL envelope; never a report-only encoder. */
        String encodeFullEnvelope(Map<String, Object> envelope) throws Throwable;
    }

    record Result(String status, String tailStatus, boolean fullEvidence,
                  boolean primaryCloseCompleted, boolean proofCloseCompleted,
                  long utf8Bytes, String sha256, Map<String, Object> completion) {}

    private interface Action { void run() throws Throwable; }
    private BoreStopTail() {}

    /** Call once, only from root's bound actual STOPPED handler. No retry API. */
    static Result complete(Context context, Map<String, ?> report, Ops ops) {
        Objects.requireNonNull(context, "context");
        Objects.requireNonNull(report, "original report read reference");
        Objects.requireNonNull(ops, "actual stop callbacks");
        return new Stop(context, ops).complete(report);
    }

    private static final class Stop {
        private final Context context;
        private final Ops ops;
        private final List<Map<String, Object>> errors = new ArrayList<>();
        private final IdentityHashMap<Throwable, Boolean> observed = new IdentityHashMap<>();
        private final Map<String, Object> cleanup = new LinkedHashMap<>();
        private Map<String, Object> finalizer = stage(false, false, null);
        private Throwable first, rootBefore, rootAfter;
        private boolean rootReadable = true, firstPreserved = true, latchViolation;

        Stop(Context context, Ops ops) { this.context = context; this.ops = ops; }

        Result complete(Map<String, ?> report) {
            pollRoot("entry");
            rootBefore = rootAfter;
            if (context.baselineReady()) finalizer = attempt("ledger_finalization", ops::finalizeLedger);
            cleanup.put("SERVER_POST_unregister", attempt("SERVER_POST_unregister", ops::unregisterServerPost));
            cleanup.put("ADD_unregister", attempt("ADD_unregister", ops::unregisterEntityAdd));
            cleanup.put("private_stop_hook_unregister", attempt("private_stop_hook_unregister", ops::unregisterPrivateStopHook));
            cleanup.put("active_reference_clear", attempt("active_reference_clear", ops::clearActiveReference));
            pollRoot("before_encoding");

            boolean full = true;
            String tailStatus = status();
            byte[] bytes;
            try {
                int count = errors.size();
                String text = ops.encodeFullEnvelope(envelope(tailStatus, report));
                pollRoot("after_full_encoding");
                if (errors.size() != count) throw new IllegalStateException("Full encoder latched a failure");
                bytes = payload(text);
            } catch (Throwable error) {
                error("full_envelope_encoding", error);
                pollRoot("after_encoding_failure");
                full = false;
                tailStatus = DIAGNOSTIC;
                // Entirely detached primitives; no original report/world/codec access.
                bytes = payload(json(envelope(DIAGNOSTIC, null)));
            }

            boolean primaryClosed = false, proofClosed = false;
            String hash = "NONE";
            try {
                // Serialize and enforce the bound BEFORE opening the final target.
                try (var out = Files.newOutputStream(context.exactTailPath(),
                        StandardOpenOption.CREATE_NEW, StandardOpenOption.WRITE)) {
                    out.write(bytes);
                    out.flush();
                }
                primaryClosed = true; // Only AFTER try-with-resources close returned.
            } catch (Throwable error) {
                error("primary_write_or_close", error);
            }
            pollRoot("after_primary_write_or_close");
            if (primaryClosed) {
                try {
                    hash = HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes));
                    String proofStatus = status().equals(STOPPED) ? tailStatus : FAILED;
                    if (!full) proofStatus = DIAGNOSTIC;
                    byte[] proof = payload(json(map("schema", "bore_stop_write_proof_v1",
                            "run_id", context.runId(), "tail_basename", context.exactTailPath().getFileName().toString(),
                            "sha256", hash, "UTF8bytes", bytes.length, "primary_close_completed", true,
                            "full_evidence", full, "status", proofStatus)));
                    Path proofPath = context.exactTailPath().resolveSibling(
                            context.exactTailPath().getFileName() + ".write-proof.json");
                    try (var out = Files.newOutputStream(proofPath,
                            StandardOpenOption.CREATE_NEW, StandardOpenOption.WRITE)) {
                        out.write(proof);
                        out.flush();
                    }
                    proofClosed = true;
                } catch (Throwable error) {
                    error("write_proof_write_or_close", error);
                }
            }
            pollRoot("return");
            String resultStatus = !primaryClosed || !proofClosed ? FAILED : full ? status() : DIAGNOSTIC;
            return new Result(resultStatus, tailStatus, full, primaryClosed, proofClosed,
                    bytes.length, hash, diagnostic());
        }

        private Map<String, Object> attempt(String name, Action action) {
            Throwable failure = null;
            boolean returned = false;
            try { action.run(); returned = true; }
            catch (Throwable error) { failure = error; error(name, error); }
            pollRoot("after_" + name);
            return stage(true, returned, failure);
        }

        private void pollRoot(String where) {
            try {
                rootAfter = ops.rootFailure();
                if (rootAfter != null && !observed.containsKey(rootAfter)) error(where + "_latched_root_failure", rootAfter);
                if (first != null && rootAfter != first && !latchViolation) {
                    latchViolation = true;
                    firstPreserved = false;
                    error(where + "_first_failure_not_retained", new IllegalStateException("Root first-failure latch changed or cleared"));
                }
            } catch (Throwable failure) {
                rootReadable = false;
                error(where + "_root_failure_getter", failure);
            }
        }

        private void error(String where, Throwable failure) {
            observed.put(failure, true);
            if (first == null) first = failure;
            errors.add(map("stage", where, "error", cause(failure)));
            log(where, failure);
            try { ops.latchFirstFailure(first); }
            catch (Throwable latchError) {
                observed.put(latchError, true);
                errors.add(map("stage", where + "_root_failure_latch", "error", cause(latchError)));
                log(where + "_root_failure_latch", latchError);
            }
        }

        private void log(String where, Throwable error) {
            try {
                System.err.println("[BoreStopTail] run=" + context.runId() + " stage=" + where
                        + " tail=" + context.exactTailPath());
                error.printStackTrace(System.err);
                System.err.flush();
                if (System.err.checkError()) throw new IOException("Stop diagnostic console write failed");
            } catch (Throwable logError) {
                // No recursive logger/latch loop when the console itself is unavailable.
                errors.add(map("stage", where + "_console_logging", "error", cause(logError)));
            }
        }

        private String status() {
            return context.storyComplete() && context.baselineReady() && context.actualHeadReturned()
                    && Boolean.TRUE.equals(finalizer.get("returned")) && cleanup.size() == 4
                    && cleanup.values().stream().allMatch(value -> Boolean.TRUE.equals(((Map<?, ?>) value).get("returned")))
                    && rootReadable && firstPreserved && rootAfter == null && errors.isEmpty() ? STOPPED : FAILED;
        }

        private Map<String, Object> envelope(String status, Map<String, ?> report) {
            var result = new LinkedHashMap<String, Object>();
            result.put("status", status);
            result.put("run_id", context.runId());
            result.put("story_complete", context.storyComplete());
            result.put("error", first == null ? (status.equals(STOPPED) ? "NONE" : "INCOMPLETE; see stop_completion") : summary(first));
            result.put("root_stopped_audit", AUDIT);
            result.put("full_evidence", report != null);
            result.put("stop_completion", diagnostic());
            if (report != null) result.put("report", report); // Same reference, never copied, traversed, or mutated here.
            return Collections.unmodifiableMap(result);
        }

        private Map<String, Object> diagnostic() {
            return map("schema", "bore_stop_completion_v1", "sequence_handler_entered", true,
                    "run_id", context.runId(), "exact_tail_path", context.exactTailPath().toString(),
                    "story_complete", context.storyComplete(), "baseline_ready", context.baselineReady(),
                    "actual_head_returned", context.actualHeadReturned(), "ledger_finalization", finalizer,
                    "server_cleanup", Collections.unmodifiableMap(new LinkedHashMap<>(cleanup)),
                    "preexisting_root_failure", cause(rootBefore), "stop_errors", List.copyOf(errors),
                    "root_failure_after", cause(rootAfter), "root_failure_read_complete", rootReadable,
                    "first_root_failure_preserved", firstPreserved,
                    "writer_outcome", "NOT_KNOWN_DURING_PRIMARY_ENCODING; require post-close companion and returned Result",
                    "client_cleanup", "OUTSIDE_HELPER; queued is not completed");
        }
    }

    private static Map<String, Object> stage(boolean entered, boolean returned, Throwable error) {
        return map("entered", entered, "returned", returned, "complete", returned,
                "state", returned ? "RETURNED" : entered ? "THREW" : "NOT_RUN", "error", cause(error));
    }

    private static Object cause(Throwable failure) {
        if (failure == null) return "NONE";
        var rows = new ArrayList<Map<String, Object>>();
        var seen = new IdentityHashMap<Throwable, Boolean>();
        Throwable current = failure;
        while (current != null && rows.size() < CAUSES && seen.put(current, true) == null) {
            String message;
            try { message = current.getMessage(); }
            catch (Throwable unavailable) { message = "MESSAGE_UNAVAILABLE"; }
            rows.add(map("type", bounded(current.getClass().getName()), "message", bounded(message)));
            try { current = current.getCause(); }
            catch (Throwable unavailable) { return map("cause_chain", List.copyOf(rows), "cause_chain_truncated", true); }
        }
        return map("cause_chain", List.copyOf(rows), "cause_chain_truncated", current != null);
    }

    private static String summary(Throwable failure) {
        String message;
        try { message = failure.getMessage(); }
        catch (Throwable unavailable) { message = "MESSAGE_UNAVAILABLE"; }
        return bounded(failure.getClass().getName() + ": " + bounded(message));
    }

    private static String bounded(String text) {
        if (text == null) return "";
        return text.length() <= TEXT ? text : text.substring(0, TEXT) + "[TRUNCATED_DIAGNOSTIC_TEXT]";
    }

    private static Map<String, Object> map(Object... pairs) {
        var result = new LinkedHashMap<String, Object>();
        for (int i = 0; i < pairs.length; i += 2) result.put((String) pairs[i], pairs[i + 1]);
        return Collections.unmodifiableMap(result);
    }

    private static byte[] payload(String text) {
        if (text == null || text.isBlank()) throw new IllegalArgumentException("Empty full-envelope encoding");
        long count = 1; // Include the final newline in the existing reader's bound.
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if (Character.isHighSurrogate(c)) {
                if (++i >= text.length() || !Character.isLowSurrogate(text.charAt(i)))
                    throw new IllegalArgumentException("Malformed UTF-16 in encoded envelope");
                count += 4;
            } else if (Character.isLowSurrogate(c)) throw new IllegalArgumentException("Malformed UTF-16 in encoded envelope");
            else count += c < 0x80 ? 1 : c < 0x800 ? 2 : 3;
            if (count > MAX_UTF8_BYTES) throw new IllegalArgumentException("Encoded envelope exceeds 512 MiB UTF-8 reader bound");
        }
        return (text + "\n").getBytes(StandardCharsets.UTF_8);
    }

    /** Deliberately accepts only private primitive diagnostic trees, never a report. */
    private static String json(Object value) {
        if (value == null) return "null";
        if (value instanceof String text) return quote(text);
        if (value instanceof Boolean || value instanceof Integer || value instanceof Long) return value.toString();
        var out = new StringBuilder();
        if (value instanceof Map<?, ?> values) {
            out.append('{');
            for (var entry : values.entrySet()) {
                if (out.length() > 1) out.append(',');
                out.append(quote((String) entry.getKey())).append(':').append(json(entry.getValue()));
            }
            return out.append('}').toString();
        }
        if (value instanceof List<?> values) {
            out.append('[');
            for (var item : values) {
                if (out.length() > 1) out.append(',');
                out.append(json(item));
            }
            return out.append(']').toString();
        }
        throw new IllegalArgumentException("Nonprimitive private diagnostic value");
    }

    private static String quote(String text) {
        var out = new StringBuilder("\"");
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            switch (c) {
                case '"' -> out.append("\\\"");
                case '\\' -> out.append("\\\\");
                case '\b' -> out.append("\\b");
                case '\f' -> out.append("\\f");
                case '\n' -> out.append("\\n");
                case '\r' -> out.append("\\r");
                case '\t' -> out.append("\\t");
                default -> {
                    if (c < 0x20 || Character.isSurrogate(c)) {
                        out.append("\\u");
                        for (int shift = 12; shift >= 0; shift -= 4) out.append("0123456789abcdef".charAt((c >>> shift) & 15));
                    } else out.append(c);
                }
            }
        }
        return out.append('"').toString();
    }
}
