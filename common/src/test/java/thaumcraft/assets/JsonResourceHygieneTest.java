package thaumcraft.assets;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.gson.JsonParser;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

/** Every JSON file the mod packages must parse, must not define a key twice (Gson would silently keep the last) and must be plain UTF-8. */
class JsonResourceHygieneTest {
    private static List<String> jsonFiles() throws Exception {
        List<String> files = new ArrayList<>();
        for (String root : new String[] {"assets/thaumcraft", "data/thaumcraft", "data/minecraft"}) {
            for (String file : ResourceTree.files(root)) if (file.endsWith(".json")) files.add(root + "/" + file);
        }
        files.add("thaumcraft-common.mixins.json");
        return files;
    }

    @Test void theResourceTreeIsFound() throws Exception {
        assertTrue(jsonFiles().size() > 1500, "JSON resources found: " + jsonFiles().size());
    }

    @Test void everyJsonFileParses() throws Exception {
        List<String> broken = new ArrayList<>();
        for (String path : jsonFiles()) {
            try {
                JsonParser.parseString(ResourceTree.text(path));
            } catch (RuntimeException e) {
                broken.add(path + ": " + e.getMessage());
            }
        }
        assertEquals(List.of(), broken);
    }

    @Test void noJsonFileDefinesAKeyTwiceInOneObject() throws Exception {
        List<String> duplicated = new ArrayList<>();
        for (String path : jsonFiles()) for (String key : ResourceTree.duplicateKeys(ResourceTree.text(path))) duplicated.add(path + " " + key);
        assertEquals(List.of(), duplicated);
    }

    @Test void noJsonFileStartsWithAByteOrderMarkOrIsEmpty() throws Exception {
        List<String> bad = new ArrayList<>();
        for (String path : jsonFiles()) {
            byte[] bytes = ResourceTree.bytes(path);
            if (bytes.length == 0) bad.add(path + " is empty");
            else if (bytes.length >= 3 && bytes[0] == (byte) 0xEF && bytes[1] == (byte) 0xBB && bytes[2] == (byte) 0xBF) bad.add(path + " starts with a byte order mark");
        }
        assertEquals(List.of(), bad);
        assertFalse(bad.size() > 0);
    }
}
