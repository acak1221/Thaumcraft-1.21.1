package thaumic.tinkerer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;

/**
 * Test-only code of the port (no 1.7.10 counterpart), CC BY-NC-SA 3.0 like the add-on.
 *
 * <p>The add-on is compiled in the same Gradle project as the whole Thaumcraft mod, so only this test keeps it from
 * reaching into the mod's internals: every import is the JDK, Minecraft, Architectury, Fabric (in the Fabric tree),
 * a few libraries Minecraft ships, the public {@code thaumcraft.api} or the add-on itself.</p>
 */
class TinkererApiOnlyTest {
    private static final List<String> COMMON_ALLOWED = List.of("java.", "javax.", "net.minecraft.", "com.mojang.",
            "dev.architectury.", "com.google.", "org.slf4j.", "org.apache.", "it.unimi.dsi.fastutil.", "io.netty.",
            "org.jetbrains.", "org.joml.", "com.electronwill.", "thaumcraft.api.", "thaumic.tinkerer.");
    // The Fabric tree also holds mixins (thaumic.tinkerer.fabric.mixin): Mixin and MixinExtras are the loader's, not the mod's.
    private static final List<String> FABRIC_ALLOWED = List.of("net.fabricmc.", "org.spongepowered.", "com.llamalad7.");

    @Test
    void thePluginSourcesImportOnlyThePublicApi() throws IOException {
        List<String> offending = new ArrayList<>();
        int files = scan(Path.of("common", "src", "main", "java"), COMMON_ALLOWED, offending)
                + scan(Path.of("fabric", "src", "main", "java"), concat(COMMON_ALLOWED, FABRIC_ALLOWED), offending)
                + scan(Path.of("neoforge", "src", "main", "java"), concat(COMMON_ALLOWED, List.of("net.neoforged.", "org.objectweb.")), offending);
        assertTrue(files > 0, "the add-on has sources: " + Path.of(".").toAbsolutePath());
        assertEquals(List.of(), offending, "the add-on reaches past thaumcraft.api");
    }

    private static List<String> concat(List<String> a, List<String> b) {
        List<String> all = new ArrayList<>(a);
        all.addAll(b);
        return all;
    }

    private static int scan(Path root, List<String> allowed, List<String> offending) throws IOException {
        if (!Files.isDirectory(root)) return 0;
        int files = 0;
        try (Stream<Path> paths = Files.walk(root)) {
            for (Path file : paths.filter(p -> p.toString().endsWith(".java")).toList()) {
                files++;
                for (String line : Files.readAllLines(file)) {
                    String trimmed = line.trim();
                    if (!trimmed.startsWith("import ")) continue;
                    String name = trimmed.substring("import ".length()).replace("static ", "").trim();
                    if (allowed.stream().noneMatch(name::startsWith)) offending.add(file + ": " + name);
                }
                String text = Files.readString(file);
                if (text.contains("thaumcraft.common.") || text.contains("thaumcraft.client.")) {
                    offending.add(file + ": qualified internal name");
                }
            }
        }
        return files;
    }
}
