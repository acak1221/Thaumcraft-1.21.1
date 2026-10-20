package thaumcraft.assets;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.junit.jupiter.api.Test;

/** The English and Russian language files must stay in step: same keys, same format placeholders, no empty or mangled values. */
class LangParityTest {
    private static final String EN = "assets/thaumcraft/lang/en_us.json";
    private static final String RU = "assets/thaumcraft/lang/ru_ru.json";
    private static final Pattern PLACEHOLDER = Pattern.compile("%(?:\\d+\\$)?(?:\\.\\d+)?[sdf%]");

    /**
     * Values that are legitimately identical in both languages (item-group and wand names, the trademark-style Salis Mundus, axis
     * labels and the port's own short labels). Anything else that is still English in the Russian file is an untranslated string:
     * translate it, or add it here on purpose.
     */
    private static final Set<String> IDENTICAL_ON_PURPOSE = Set.of(
            "itemGroup.thaumcraft", "item.Wand.name", "item.thaumcraft.resource.14.name", "item.thaumcraft.resource.dust.name",
            "item.thaumcraft.salis_mundus", "golemblurb.3.text", "golemblurb.6.text", "golemblurb.7.text",
            "tc.port.research.cost", "tc.port.wand.name", "tc.port.focus.page",
            "tc.architect.axis.1", "tc.architect.axis.2", "tc.architect.axis.3");

    private static JsonObject lang(String path) throws Exception { return ResourceTree.object(path); }

    @Test void bothFilesDefineExactlyTheSameKeys() throws Exception {
        var en = new TreeSet<>(lang(EN).keySet());
        var ru = new TreeSet<>(lang(RU).keySet());
        var onlyEnglish = new TreeSet<>(en); onlyEnglish.removeAll(ru);
        var onlyRussian = new TreeSet<>(ru); onlyRussian.removeAll(en);
        assertEquals(List.of(), List.copyOf(onlyEnglish), "keys missing from ru_ru.json");
        assertEquals(List.of(), List.copyOf(onlyRussian), "keys missing from en_us.json");
        assertFalse(en.isEmpty());
    }

    @Test void noKeyIsDefinedTwiceInEitherFile() throws Exception {
        assertEquals(List.of(), ResourceTree.duplicateKeys(ResourceTree.text(EN)), "duplicate keys in en_us.json");
        assertEquals(List.of(), ResourceTree.duplicateKeys(ResourceTree.text(RU)), "duplicate keys in ru_ru.json");
    }

    @Test void everyValueIsATextAndNoneIsBlank() throws Exception {
        List<String> problems = new ArrayList<>();
        for (String path : new String[] {EN, RU}) {
            for (Map.Entry<String, JsonElement> entry : lang(path).entrySet()) {
                if (!entry.getValue().isJsonPrimitive() || !entry.getValue().getAsJsonPrimitive().isString()) problems.add(path + " " + entry.getKey() + " is not a string");
                else if (entry.getValue().getAsString().isBlank()) problems.add(path + " " + entry.getKey() + " is blank");
            }
        }
        assertEquals(List.of(), problems);
    }

    @Test void bothTranslationsKeepTheSameFormatPlaceholders() throws Exception {
        JsonObject en = lang(EN);
        JsonObject ru = lang(RU);
        List<String> mismatches = new ArrayList<>();
        for (String key : en.keySet()) {
            if (!ru.has(key)) continue;
            List<String> english = placeholders(en.get(key).getAsString());
            List<String> russian = placeholders(ru.get(key).getAsString());
            if (!english.equals(russian)) mismatches.add(key + ": " + english + " vs " + russian);
        }
        assertEquals(List.of(), mismatches, "a translation lost or added a %s/%d placeholder");
    }

    private static List<String> placeholders(String value) {
        List<String> found = new ArrayList<>();
        Matcher matcher = PLACEHOLDER.matcher(value);
        while (matcher.find()) found.add(matcher.group());
        found.sort(null);
        return found;
    }

    @Test void theRussianFileIsTranslatedExceptForTheListedExceptions() throws Exception {
        JsonObject en = lang(EN);
        JsonObject ru = lang(RU);
        List<String> untranslated = new ArrayList<>();
        for (String key : en.keySet()) {
            if (ru.has(key) && ru.get(key).getAsString().equals(en.get(key).getAsString()) && !IDENTICAL_ON_PURPOSE.contains(key)) untranslated.add(key);
        }
        assertEquals(List.of(), untranslated, "still English in ru_ru.json (translate, or list in IDENTICAL_ON_PURPOSE)");
    }

    @Test void theListedExceptionsAreReallyStillIdentical() throws Exception {
        JsonObject en = lang(EN);
        JsonObject ru = lang(RU);
        List<String> stale = new ArrayList<>();
        for (String key : IDENTICAL_ON_PURPOSE) {
            if (!en.has(key) || !ru.has(key) || !en.get(key).getAsString().equals(ru.get(key).getAsString())) stale.add(key);
        }
        assertEquals(List.of(), stale, "delete from IDENTICAL_ON_PURPOSE: the key is gone or has been translated");
    }

    @Test void theFilesAreUtf8WithoutByteOrderMarkOrReplacementCharacters() throws Exception {
        for (String path : new String[] {EN, RU}) {
            byte[] bytes = ResourceTree.bytes(path);
            assertTrue(bytes.length > 3, path);
            assertFalse(bytes[0] == (byte) 0xEF && bytes[1] == (byte) 0xBB && bytes[2] == (byte) 0xBF, path + " starts with a byte order mark");
            assertFalse(ResourceTree.text(path).contains("�"), path + " contains the Unicode replacement character (broken encoding)");
        }
    }

    @Test void russianValuesContainCyrillicWhereTheyAreTranslated() throws Exception {
        JsonObject ru = lang(RU);
        long cyrillic = ru.entrySet().stream().filter(e -> e.getValue().getAsString().codePoints().anyMatch(c -> c >= 0x0400 && c <= 0x04FF)).count();
        assertTrue(cyrillic * 10 >= ru.size() * 8L, "fewer than 80% of the Russian values contain Cyrillic letters: " + cyrillic + " of " + ru.size());
    }
}
