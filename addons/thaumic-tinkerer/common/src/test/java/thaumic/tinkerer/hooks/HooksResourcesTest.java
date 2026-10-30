package thaumic.tinkerer.hooks;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;

/**
 * Test-only code of the port (no 1.7.10 counterpart), CC BY-NC-SA 3.0 like the add-on.
 *
 * <p>The wiring of the Fabric mixins that no compiler checks: every mixin class is in {@code thaumictinkerer.mixins.json} and
 * the other way round, the config is listed in the mod's {@code fabric.mod.json} (the game test mod's list of mixins is
 * separate), no other class names a mixin class (Mixin refuses to load one directly), the handler methods carry the mod's
 * prefix, and common code never reaches into the Fabric tree. The tests run in {@code addons/thaumic-tinkerer}.</p>
 */
class HooksResourcesTest {
    private static final Path MIXIN_DIR = Path.of("fabric", "src", "main", "java", "thaumic", "tinkerer", "fabric", "mixin");
    private static final Path MIXIN_JSON = Path.of("fabric", "src", "main", "resources", "thaumictinkerer.mixins.json");
    private static final Path MOD_JSON = Path.of("fabric", "src", "main", "resources", "fabric.mod.json");

    private static List<String> mixinClasses() throws IOException {
        try (Stream<Path> files = Files.list(MIXIN_DIR)) {
            return files.map(p -> p.getFileName().toString()).filter(n -> n.endsWith(".java")).map(n -> n.substring(0, n.length() - 5))
                    .sorted().toList();
        }
    }

    private static JsonObject json(Path path) throws IOException {
        return JsonParser.parseString(Files.readString(path)).getAsJsonObject();
    }

    @Test
    void theMixinConfigListsExactlyTheMixinClasses() throws IOException {
        JsonObject config = json(MIXIN_JSON);
        assertEquals("thaumic.tinkerer.fabric.mixin", config.get("package").getAsString());
        assertEquals("JAVA_21", config.get("compatibilityLevel").getAsString());
        assertTrue(config.get("required").getAsBoolean(), "a missing hook must stop the game, not go unnoticed");
        assertEquals(1, config.getAsJsonObject("injectors").get("defaultRequire").getAsInt(), "an injector that finds nothing is an error");
        assertFalse(config.has("client"), "all the hooks are on common classes: no client section");
        List<String> listed = new ArrayList<>();
        for (JsonElement entry : config.getAsJsonArray("mixins")) listed.add(entry.getAsString());
        assertEquals(new TreeSet<>(listed).size(), listed.size(), "no mixin is listed twice");
        assertEquals(mixinClasses(), new TreeSet<>(listed).stream().toList(), "the config and the mixin package list the same classes");
    }

    @Test
    void theModListsTheMixinConfig() throws IOException {
        JsonArray mixins = json(MOD_JSON).getAsJsonArray("mixins");
        assertTrue(mixins != null && mixins.contains(JsonParser.parseString("\"thaumictinkerer.mixins.json\"")),
                "fabric.mod.json of the add-on names thaumictinkerer.mixins.json");
    }

    @Test
    void noOneNamesAMixinClassAndTheHandlersCarryThePrefix() throws IOException {
        for (String name : mixinClasses()) {
            String text = Files.readString(MIXIN_DIR.resolve(name + ".java"));
            assertTrue(text.contains("@Mixin("), name + " is a mixin");
            Matcher handler = Pattern.compile("@(Inject|WrapOperation|WrapMethod|ModifyVariable|ModifyReturnValue)\\b.*?\\)\\s*(?:private|protected|public)\\s+\\S+\\s+([\\w$]+)\\(", Pattern.DOTALL).matcher(text);
            int handlers = 0;
            while (handler.find()) {
                handlers++;
                assertTrue(handler.group(2).startsWith("thaumictinkerer$"), name + "." + handler.group(2) + " is not prefixed with thaumictinkerer$");
            }
            if (!name.equals("ServerGamePacketListenerFloatingMixin")) assertTrue(handlers > 0, name + " has a handler");
        }
        List<String> offenders = new ArrayList<>();
        for (Path root : List.of(Path.of("common", "src"), Path.of("fabric", "src"))) {
            try (Stream<Path> files = Files.walk(root)) {
                for (Path file : files.filter(p -> p.toString().endsWith(".java")).toList()) {
                    if (file.startsWith(MIXIN_DIR)) continue;
                    for (String line : Files.readAllLines(file)) {
                        String trimmed = line.trim();
                        // imports and fully qualified uses; the Javadoc and the test files talk about the classes by simple name
                        if (trimmed.startsWith("import thaumic.tinkerer.fabric.mixin.") || (!trimmed.startsWith("*") && !trimmed.startsWith("//")
                                && trimmed.contains("thaumic.tinkerer.fabric.mixin.") && !trimmed.contains("\""))) {
                            offenders.add(file + ": " + trimmed);
                        }
                    }
                }
            }
        }
        assertEquals(List.of(), offenders, "a mixin class cannot be referenced from other code");
    }

    @Test
    void commonCodeNeverReachesIntoTheLoaderModules() throws IOException {
        List<String> offenders = new ArrayList<>();
        try (Stream<Path> files = Files.walk(Path.of("common", "src", "main", "java"))) {
            for (Path file : files.filter(p -> p.toString().endsWith(".java")).toList()) {
                for (String line : Files.readAllLines(file)) {
                    String trimmed = line.trim();
                    if (trimmed.startsWith("import thaumic.tinkerer.fabric.") || trimmed.startsWith("import thaumic.tinkerer.neoforge.")
                            || trimmed.startsWith("import org.spongepowered.") || trimmed.startsWith("import com.llamalad7.")) {
                        offenders.add(file + ": " + trimmed);
                    }
                }
            }
        }
        assertEquals(List.of(), offenders, "common code is loader neutral");
    }
}
