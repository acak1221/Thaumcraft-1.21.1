package thaumic.tinkerer.fabric.clientsmoke;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Tells what the client log gained since a mark, so that a step can assert "no exception was logged while this screen
 * opened". Test-only code of the port (no 1.7.10 counterpart); CC BY-NC-SA 3.0 like the add-on, not distributed.
 *
 * <p>The log is read from the file (log4j writes each line at once), with the same rules as {@link LogExcerpt}: a WARN, ERROR
 * or FATAL line that names the add-on, a missing resource or an exception. Lines classified as environment noise (no sound
 * device and the like) are not counted.</p>
 */
final class LogWatcher {
    private final Path log = SmokeRun.gameDir().resolve("logs").resolve("latest.log");

    /** The number of lines in the log now. */
    int mark() {
        try {
            return Files.isRegularFile(log) ? Files.readAllLines(log, StandardCharsets.UTF_8).size() : 0;
        } catch (IOException e) {
            return 0;
        }
    }

    /** The suspicious entries logged after {@code mark}. */
    List<Map<String, Object>> since(int mark) {
        try {
            List<String> lines = Files.readAllLines(log, StandardCharsets.UTF_8);
            if (mark >= lines.size()) return List.of();
            List<Map<String, Object>> found = new ArrayList<>();
            for (Map<String, Object> entry : LogExcerpt.collect(lines.subList(mark, lines.size()), mark)) {
                if (!"environment".equals(entry.get("kind"))) found.add(entry);
            }
            return found;
        } catch (IOException e) {
            return List.of();
        }
    }

    /** One line per entry, for a report detail. */
    static String summarize(List<Map<String, Object>> entries) {
        StringBuilder text = new StringBuilder();
        for (Map<String, Object> entry : entries) {
            if (text.length() > 0) text.append(" | ");
            text.append(entry.get("text"));
            if (text.length() > 1500) break;
        }
        return text.toString();
    }
}
