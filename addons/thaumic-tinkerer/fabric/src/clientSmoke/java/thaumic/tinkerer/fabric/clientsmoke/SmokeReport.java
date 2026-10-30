package thaumic.tinkerer.fabric.clientsmoke;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.TreeSet;

/**
 * Everything the client smoke found, written to {@code report.json} after every phase. Test-only code of the port (no
 * 1.7.10 counterpart); CC BY-NC-SA 3.0 like the add-on, not distributed.
 *
 * <p>A check has one of four results: {@code PASS}, {@code FAIL} (a defect of the port or of a resource the port ships),
 * {@code HARNESS_ERROR} (the check itself threw: the harness, not the port, is the suspect) and {@code NOT_RUN} (it was
 * skipped on purpose, the detail says why). {@code INFO} records a number or a fact and never counts as a failure.</p>
 */
final class SmokeReport {
    enum Result { PASS, FAIL, HARNESS_ERROR, NOT_RUN, INFO }

    /** One recorded check. {@code group} is the content group by registry path ("" when it concerns no single object). */
    record Check(String phase, String name, String group, String target, Result result, String detail) {}

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();

    private final Path outDir;
    private final Instant started = Instant.now();
    private final List<Check> checks = new ArrayList<>();
    private final Map<String, List<String>> inventory = new LinkedHashMap<>();
    private final List<Map<String, Object>> screenshots = new ArrayList<>();
    private final List<Map<String, Object>> suspiciousLog = new ArrayList<>();
    private final Map<String, Object> facts = new LinkedHashMap<>();
    private final Map<String, Object> harness = new LinkedHashMap<>();

    SmokeReport(Path outDir) {
        this.outDir = outDir;
        harness.put("status", "RUNNING");
        harness.put("started_utc", started.toString());
    }

    Path outDir() {
        return outDir;
    }

    synchronized void add(String phase, String name, String group, String target, Result result, String detail) {
        checks.add(new Check(phase, name, group == null ? "" : group, target == null ? "" : target, result, detail == null ? "" : detail));
    }

    void pass(String phase, String name, String group, String target, String detail) {
        add(phase, name, group, target, Result.PASS, detail);
    }

    void fail(String phase, String name, String group, String target, String detail) {
        add(phase, name, group, target, Result.FAIL, detail);
    }

    void info(String phase, String name, String detail) {
        add(phase, name, "", "", Result.INFO, detail);
    }

    void notRun(String phase, String name, String detail) {
        add(phase, name, "", "", Result.NOT_RUN, detail);
    }

    /** A check that threw: the exception is the detail. The harness goes on with the next check. */
    void harnessError(String phase, String name, Throwable error) {
        add(phase, name, "", "", Result.HARNESS_ERROR, stack(error));
    }

    /** Lists what the harness looked at ("blocks", "items", "block_entity_types", "menus", ...). */
    synchronized void inventory(String key, List<String> values) {
        inventory.put(key, new ArrayList<>(values));
    }

    synchronized void fact(String key, Object value) {
        facts.put(key, value);
    }

    synchronized void screenshot(Map<String, Object> entry) {
        screenshots.add(entry);
    }

    synchronized void suspiciousLog(List<Map<String, Object>> lines) {
        suspiciousLog.clear();
        suspiciousLog.addAll(lines);
    }

    synchronized void harnessStatus(String status, String detail) {
        harness.put("status", status);
        if (detail != null) harness.put("detail", detail);
        harness.put("finished_utc", Instant.now().toString());
    }

    synchronized List<Map<String, Object>> screenshots() {
        return new ArrayList<>(screenshots);
    }

    synchronized String harnessStatusText() {
        return String.valueOf(harness.get("status"));
    }

    synchronized List<Check> checks() {
        return new ArrayList<>(checks);
    }

    synchronized long count(Result result) {
        return checks.stream().filter(c -> c.result() == result).count();
    }

    static String stack(Throwable error) {
        StringWriter writer = new StringWriter();
        error.printStackTrace(new PrintWriter(writer));
        String text = writer.toString();
        return text.length() > 6000 ? text.substring(0, 6000) + "\n... (cut)" : text;
    }

    /** Writes {@code report.json}; called after every phase so that a crash leaves the partial result. */
    synchronized void write() {
        Map<String, Object> root = new LinkedHashMap<>();
        root.put("harness", harness);
        Map<String, Object> summary = new LinkedHashMap<>();
        for (Result result : Result.values()) summary.put(result.name(), count(result));
        Map<String, Map<String, Long>> byGroup = new TreeMap<>();
        for (Check check : checks) {
            if (check.result() != Result.FAIL) continue;
            byGroup.computeIfAbsent(check.group().isEmpty() ? "(no single group)" : check.group(), k -> new TreeMap<>())
                    .merge(check.name(), 1L, Long::sum);
        }
        summary.put("failures_by_group", byGroup);
        root.put("summary", summary);
        root.put("facts", facts);
        root.put("checks", checks);
        root.put("inventory", inventory);
        root.put("screenshots", screenshots);
        root.put("suspicious_log_lines", suspiciousLog);
        try {
            Files.createDirectories(outDir);
            Path tmp = outDir.resolve("report.json.tmp");
            Files.writeString(tmp, GSON.toJson(root) + "\n", StandardCharsets.UTF_8);
            Files.move(tmp, outDir.resolve("report.json"), java.nio.file.StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            throw new IllegalStateException("Cannot write the client smoke report", e);
        }
    }

    /** Sorted distinct values of a string collection, for the inventory lists. */
    static List<String> sorted(Iterable<String> values) {
        TreeSet<String> set = new TreeSet<>();
        values.forEach(set::add);
        return new ArrayList<>(set);
    }
}
