package thaumcraft.client.renderers;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;

/**
 * A {@code VertexConsumer} fetched from a {@code MultiBufferSource} must not be held across a
 * call that fetches another one.
 *
 * <p>1.21.1's {@code MultiBufferSource.BufferSource.getBuffer} closes the previous buffer for a
 * {@code RenderType} that cannot consolidate consecutive geometry: the bytecode ends the batch and
 * builds a fresh {@code BufferBuilder} when
 * {@code RenderType.canConsolidateConsecutiveGeometry()} is false, which is the case for
 * {@code RenderType.eyes}. Writing into the variable afterwards throws
 * {@code IllegalStateException: Not building!} and crashes the client.</p>
 *
 * <p>This happened for real: the eldritch obelisk's tunnel held the {@code RenderType.eyes} buffer
 * across a fifteen-layer loop, and the client crashed the moment an obelisk entered the view. A
 * render test cannot catch it (no client runs in CI), so this test reads the source and refuses the
 * shape.</p>
 *
 * <p>It is a source-shape test, not a behavioural one — stated plainly so nobody mistakes it for
 * proof that the rendering is correct.</p>
 */
class BufferSourceDisciplineTest {
    /** A vertex buffer bound to a name. */
    private static final Pattern BUFFER_BINDING = Pattern.compile(
            "\\b(?:var|VertexConsumer)\\s+(\\w+)\\s*=\\s*[\\w.]+\\.getBuffer\\(");

    @Test void noVertexBufferIsHeldAcrossAnotherGetBufferCall() throws IOException {
        List<String> offences = new ArrayList<>();
        try (Stream<Path> files = Files.walk(Path.of("src/main/java/thaumcraft/client"))) {
            for (Path file : files.filter(p -> p.toString().endsWith(".java")).toList()) {
                String source = Files.readString(file);
                Matcher binding = BUFFER_BINDING.matcher(source);
                while (binding.find()) {
                    String name = binding.group(1);
                    int from = binding.end();
                    // The scope of the binding ends at the next statement at the same depth; a
                    // conservative approximation is the rest of the method body, which is what the
                    // real defect looked like (a loop below the binding).
                    String rest = source.substring(from);
                    Pattern use = Pattern.compile("\\b" + Pattern.quote(name) + "\\b");
                    int nextFetch = indexOfNextGetBuffer(rest);
                    int firstUse = indexOfFirstMatch(use, rest);
                    if (firstUse >= 0 && nextFetch >= 0 && nextFetch < firstUse) {
                        offences.add(file + ": '" + name + "' is fetched, then another buffer is "
                                + "fetched before it is used - the first buffer is already closed");
                    }
                }
            }
        }
        assertTrue(offences.isEmpty(),
                "a VertexConsumer must not be held across another getBuffer call:\n  "
                        + String.join("\n  ", offences));
    }

    private static int indexOfNextGetBuffer(String text) {
        int index = text.indexOf(".getBuffer(");
        return index;
    }

    private static int indexOfFirstMatch(Pattern pattern, String text) {
        Matcher matcher = pattern.matcher(text);
        return matcher.find() ? matcher.start() : -1;
    }
}
