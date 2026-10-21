package thaumcraft.common.testing;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.core.Logger;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.appender.AbstractAppender;
import org.apache.logging.log4j.core.config.Property;

/**
 * The messages that Minecraft and the mod really log while a runtime check runs.
 *
 * <p>The two log defects of the port are defects of the log itself ("Detected setBlock in a far chunk" and
 * "Tried to load a block entity ... but failed" are lines vanilla writes), so a check that wants to prove
 * them gone has to read the log and not a counter of the mod.
 */
public final class VanillaLogCapture implements AutoCloseable {
    private final Logger root = (Logger) LogManager.getRootLogger();
    private final AbstractAppender appender;
    private final List<String> lines = Collections.synchronizedList(new ArrayList<>());

    public VanillaLogCapture() {
        appender = new AbstractAppender("thaumcraft-runtime-check-capture-" + System.nanoTime(), null, null, true, Property.EMPTY_ARRAY) {
            @Override public void append(LogEvent event) {
                lines.add(event.getLevel() + " " + event.getMessage().getFormattedMessage());
            }
        };
        appender.start();
        root.addAppender(appender);
    }

    /** Every captured line that contains the text. */
    public List<String> containing(String text) {
        synchronized (lines) {
            return lines.stream().filter(line -> line.contains(text)).toList();
        }
    }

    /** The first few captured lines that contain the text, for a failure message. */
    public String sample(String text) {
        List<String> found = containing(text);
        return found.size() + " lines" + (found.isEmpty() ? "" : ", first: " + found.get(0));
    }

    @Override public void close() {
        root.removeAppender(appender);
        appender.stop();
    }
}
