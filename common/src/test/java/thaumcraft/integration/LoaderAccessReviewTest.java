package thaumcraft.integration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.TreeMap;
import org.junit.jupiter.api.Test;

/** Shared code needs the same member access when either loader defines Minecraft classes. */
class LoaderAccessReviewTest {
    @Test void everySharedAccessRuleHasTheSameNeoForgeTransformation() throws IOException {
        Path root = Path.of("").toAbsolutePath();
        if (!Files.isRegularFile(root.resolve("settings.gradle"))) root = root.getParent();
        Map<String, String> expected = new TreeMap<>();
        for (String line : Files.readAllLines(root.resolve("common/src/main/resources/thaumcraft.accesswidener"))) {
            String text = line.strip();
            if (text.isEmpty() || text.startsWith("#") || text.startsWith("accessWidener ")) continue;
            String[] words = text.split("\\s+");
            String member = words[2].replace('/', '.') + " " + words[3]
                    + (words[1].equals("method") ? words[4] : "");
            String access = switch (words[0]) {
                case "accessible" -> "public";
                case "extendable" -> "public-f";
                default -> throw new AssertionError("Unreviewed shared access mode: " + text);
            };
            expected.put(member, access);
        }
        Map<String, String> actual = new TreeMap<>();
        for (String line : Files.readAllLines(root.resolve("neoforge/src/main/resources/META-INF/accesstransformer.cfg"))) {
            String text = line.strip();
            if (text.isEmpty() || text.startsWith("#")) continue;
            String[] words = text.split("\\s+");
            actual.put(words[1] + " " + words[2], words[0]);
        }
        assertFalse(expected.isEmpty(), "the authoritative shared access rules must be present");
        assertEquals(expected, actual,
                "every shared member requires matching runtime visibility and final removal on NeoForge");
    }
}
