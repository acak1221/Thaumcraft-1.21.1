package thaumcraft.api;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;

/**
 * The example add-on of docs/api/PORTING.md compiles against the whole mod in the Fabric build, so nothing but this
 * test stops it from reaching into the mod's internals. An add-on that only needs thaumcraft.api is the point of the
 * example: every import must be the JDK, Minecraft, Fabric or the public API.
 */
class ExampleAddonApiOnlyTest {
    private static final List<String> ALLOWED = List.of("java.", "javax.", "net.minecraft.", "com.mojang.", "net.fabricmc.",
            "thaumcraft.api.", "com.example.tc4addon.");

    @Test void exampleAddonImportsOnlyThePublicApi() throws IOException {
        Path root = Path.of("..", "examples", "example-addon", "src", "main", "java");
        assertTrue(Files.isDirectory(root), "example add-on sources: " + root.toAbsolutePath());
        List<String> offending = new ArrayList<>();
        int files = 0;
        try (Stream<Path> paths = Files.walk(root)) {
            for (Path file : paths.filter(path -> path.toString().endsWith(".java")).toList()) {
                files++;
                for (String line : Files.readAllLines(file)) {
                    String trimmed = line.trim();
                    if (!trimmed.startsWith("import ")) continue;
                    String name = trimmed.substring("import ".length()).replace("static ", "").trim();
                    if (ALLOWED.stream().noneMatch(name::startsWith)) offending.add(file.getFileName() + ": " + name);
                }
                String text = Files.readString(file);
                if (text.contains("thaumcraft.common.") || text.contains("thaumcraft.client.")) offending.add(file.getFileName() + ": qualified internal name");
            }
        }
        assertTrue(files > 0, "the example add-on has sources");
        assertFalse(!offending.isEmpty(), "the example add-on reaches past thaumcraft.api: " + offending);
    }
}
