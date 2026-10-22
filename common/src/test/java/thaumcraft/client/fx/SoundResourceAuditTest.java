package thaumcraft.client.fx;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Pattern;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/** Bundled resources and registration declarations; not an audible-client acceptance test. */
class SoundResourceAuditTest {
    private static final Path ASSETS = Path.of("src/main/resources/assets/thaumcraft");
    private static JsonObject json(Path file) throws Exception {
        return JsonParser.parseString(Files.readString(file)).getAsJsonObject();
    }

    @Test void everySoundHasRegistrationAudioAndBothSubtitles() throws Exception {
        JsonObject sounds = json(ASSETS.resolve("sounds.json"));
        Set<String> registered = new HashSet<>();
        Path registration = Path.of("src/main/java/thaumcraft/common/config/ThaumcraftSounds.java");
        assertTrue(Files.isRegularFile(registration), "sound registry bootstrap is missing");
        var matcher = Pattern.compile("registerSound\\(\"([^\"]+)\"\\)").matcher(Files.readString(registration));
        while (matcher.find()) assertTrue(registered.add(matcher.group(1)), "duplicate registration");
        assertEquals(sounds.keySet(), registered, "registry declarations must cover exactly the bundled events");
        String bootstrap = Files.readString(Path.of("src/main/java/thaumcraft/common/Thaumcraft.java"));
        assertTrue(bootstrap.contains("ThaumcraftSounds.register();"));
        JsonObject en = json(ASSETS.resolve("lang/en_us.json"));
        JsonObject ru = json(ASSETS.resolve("lang/ru_ru.json"));
        for (String id : registered) {
            JsonObject event = sounds.getAsJsonObject(id);
            assertTrue(event.has("subtitle"), id + " has no subtitle");
            String key = event.get("subtitle").getAsString();
            for (JsonObject lang : new JsonObject[]{en, ru}) {
                assertTrue(lang.has(key), key);
                assertFalse(lang.get(key).getAsString().isBlank(), key);
            }
            assertFalse(event.getAsJsonArray("sounds").isEmpty(), id);
            for (var entry : event.getAsJsonArray("sounds")) {
                String name = entry.isJsonPrimitive() ? entry.getAsString() : entry.getAsJsonObject().get("name").getAsString();
                if (entry.isJsonObject() && entry.getAsJsonObject().has("type")
                        && entry.getAsJsonObject().get("type").getAsString().equals("event")) {
                    assertTrue(sounds.has(name.replace("thaumcraft:", "")), name);
                } else {
                    assertTrue(name.startsWith("thaumcraft:"), name);
                    Path ogg = ASSETS.resolve("sounds/" + name.substring(11) + ".ogg");
                    assertTrue(Files.isRegularFile(ogg), ogg.toString());
                    assertEquals("OggS", new String(Files.readAllBytes(ogg), 0, 4, java.nio.charset.StandardCharsets.US_ASCII));
                }
            }
        }
    }

    @Test void literalSoundHelperNamesResolve() throws Exception {
        Set<String> ids = json(ASSETS.resolve("sounds.json")).keySet();
        try (var paths = Files.walk(Path.of("src/main/java"))) {
            for (Path path : paths.filter(p -> p.toString().endsWith(".java")).toList()) {
                String source = Files.readString(path);
                var names = Pattern.compile("\\bsound\\(\"([^\"]+)\"").matcher(source);
                while (names.find()) assertTrue(ids.contains(names.group(1)), path + ": " + names.group(1));
                var direct = Pattern.compile("createVariableRangeEvent\\((?:[\\w.]+\\.)?fromNamespaceAndPath\\((?:\"thaumcraft\"|Thaumcraft.MODID),\"([^\"]+)\"").matcher(source.replaceAll("\\s+", ""));
                while (direct.find()) assertTrue(ids.contains(direct.group(1)), path + ": " + direct.group(1));
                var types = Pattern.compile("new CustomSoundType\\(\"([^\"]+)\"").matcher(source);
                while (types.find()) assertTrue(ids.contains(types.group(1)), path + ": " + types.group(1));
            }
        }
        // The only non-literal helper arguments found in the complete source inventory.
        for (String id : new String[]{"hhon", "hhoff", "erase", "write", "gore"}) assertTrue(ids.contains(id), id);
    }
}
