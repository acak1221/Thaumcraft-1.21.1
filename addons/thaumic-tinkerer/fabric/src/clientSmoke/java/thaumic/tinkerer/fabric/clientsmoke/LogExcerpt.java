package thaumic.tinkerer.fabric.clientsmoke;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * Pulls the suspicious lines out of the client log: every WARN, ERROR and FATAL line that mentions the add-on, a missing
 * model, texture or translation, or an exception, with the stack lines that follow it. Test-only code of the port (no
 * 1.7.10 counterpart); CC BY-NC-SA 3.0 like the add-on, not distributed.
 *
 * <p>Lines are classified: {@code addon} when they name {@code thaumictinkerer}/{@code thaumic.tinkerer}, {@code resource}
 * when they are the game's complaint about a model, texture, sprite or translation, {@code exception} for a throwable, and
 * {@code environment} for the ones this machine always produces (no sound device, no controller, no GPU). Nothing is
 * dropped; the class only says how likely a line is to be a defect of the port.</p>
 */
final class LogExcerpt {
    private static final Pattern HEADER = Pattern.compile("^\\[\\d\\d:\\d\\d:\\d\\d] \\[([^/]+)/(WARN|ERROR|FATAL)]");
    private static final Pattern ANY_HEADER = Pattern.compile("^\\[\\d\\d:\\d\\d:\\d\\d] \\[");
    private static final Pattern ADDON = Pattern.compile("thaumictinkerer|thaumic\\.tinkerer|Thaumic Tinkerer|ThaumicTinkerer", Pattern.CASE_INSENSITIVE);
    private static final Pattern RESOURCE = Pattern.compile("missing|unable to (?:load|resolve|bake|find)|failed to (?:load|bake|parse)|"
            + "no model|no texture|sprite|translation|blockstate|Unknown (?:key|translation)", Pattern.CASE_INSENSITIVE);
    private static final Pattern EXCEPTION = Pattern.compile("Exception|Error\\b|Caused by|\\bat [a-z]+\\.[a-zA-Z.$]+\\(", 0);
    private static final Pattern ENVIRONMENT = Pattern.compile("OpenAL|openal|sound device|SoundEngine|narrator|Narrator|controller|gamepad|"
            + "Realms|telemetry|Telemetry|Failed to open audio|GLFW error|libdecor|Error parsing|Unable to authenticate|profile|session service|"
            + "Failed to fetch user properties|Could not authenticate|Failed to verify authentication|MESA|mesa|libEGL", 0);

    private LogExcerpt() {}

    /** Reads {@code log} and returns the suspicious entries (line, kind, the stack lines that follow). */
    static List<Map<String, Object>> collect(Path log) throws IOException {
        if (!Files.isRegularFile(log)) return new ArrayList<>();
        return collect(Files.readAllLines(log, StandardCharsets.UTF_8), 0);
    }

    /** The entries of {@code lines}; {@code firstLineNumber} is the number of the line before the first one (for a log read in pieces). */
    static List<Map<String, Object>> collect(List<String> lines, int firstLineNumber) {
        List<Map<String, Object>> entries = new ArrayList<>();
        for (int i = 0; i < lines.size(); i++) {
            String line = lines.get(i);
            var header = HEADER.matcher(line);
            if (!header.find()) continue;
            List<String> follow = new ArrayList<>();
            int j = i + 1;
            while (j < lines.size() && !ANY_HEADER.matcher(lines.get(j)).find() && follow.size() < 40) follow.add(lines.get(j++));
            String all = line + "\n" + String.join("\n", follow);
            boolean addon = ADDON.matcher(all).find();
            boolean resource = RESOURCE.matcher(line).find();
            boolean exception = EXCEPTION.matcher(all).find();
            if (!(addon || resource || exception)) continue;
            Map<String, Object> entry = new LinkedHashMap<>();
            entry.put("line_number", firstLineNumber + i + 1);
            entry.put("level", header.group(2));
            entry.put("kind", ENVIRONMENT.matcher(all).find() && !addon ? "environment" : addon ? "addon" : resource ? "resource" : "exception");
            entry.put("text", line);
            if (!follow.isEmpty()) entry.put("following", follow);
            entries.add(entry);
        }
        return entries;
    }

    /** Writes the entries as plain text, one block per entry, to {@code client-log-excerpt.txt}. */
    static void write(Path file, List<Map<String, Object>> entries) throws IOException {
        StringBuilder text = new StringBuilder();
        text.append("# WARN/ERROR/FATAL lines of the client log that mention the add-on, a missing model/texture/translation or an exception\n");
        text.append("# ").append(entries.size()).append(" entries; kind = addon | resource | exception | environment\n\n");
        for (Map<String, Object> entry : entries) {
            text.append('[').append(entry.get("kind").toString().toUpperCase(Locale.ROOT)).append("] line ").append(entry.get("line_number"))
                    .append(": ").append(entry.get("text")).append('\n');
            Object following = entry.get("following");
            if (following instanceof List<?> list) for (Object each : list) text.append("    ").append(each).append('\n');
        }
        Files.writeString(file, text.toString(), StandardCharsets.UTF_8);
    }
}
