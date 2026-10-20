package thaumcraft.assets;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import org.junit.jupiter.api.Test;

/**
 * thaumcraft-common.mixins.json against the mixin classes the build produces, without loading any of them (a mixin class cannot be
 * loaded outside the mixin transformer). A mixin that exists but is not listed is silently dead; a listed one that does not exist
 * makes the game refuse to start.
 */
class MixinConfigConsistencyTest {
    private static final String CONFIG = "thaumcraft-common.mixins.json";

    private static JsonObject config() throws Exception { return ResourceTree.object(CONFIG); }

    private static List<String> list(String field) throws Exception {
        List<String> names = new ArrayList<>();
        for (JsonElement element : config().getAsJsonArray(field)) names.add(element.getAsString());
        return names;
    }

    private static String packagePath() throws Exception { return config().get("package").getAsString().replace('.', '/'); }

    /** Top-level classes directly in the package ({@code client} = false) or in its client sub-package. */
    private static Set<String> classesOnDisk(boolean client) throws Exception {
        Set<String> names = new TreeSet<>();
        for (String file : ResourceTree.filesOnAnyRoot(packagePath())) {
            if (!file.endsWith(".class") || file.contains("$")) continue;
            boolean inClient = file.startsWith("client/");
            if (inClient != client) continue;
            if (!client && file.indexOf('/') >= 0) continue;
            names.add(file.substring(0, file.length() - ".class".length()));
        }
        return names;
    }

    @Test void theConfigDeclaresTheModernBaselineAndIsRequired() throws Exception {
        JsonObject config = config();
        assertEquals("thaumcraft.common.mixin", config.get("package").getAsString());
        assertEquals("JAVA_21", config.get("compatibilityLevel").getAsString());
        assertTrue(config.get("required").getAsBoolean(), "a missing mixin must stop the game, not be skipped");
        assertEquals(1, config.getAsJsonObject("injectors").get("defaultRequire").getAsInt(), "every injector has to find its target");
    }

    @Test void noMixinIsListedTwiceAndTheTwoListsDoNotOverlap() throws Exception {
        List<String> common = list("mixins");
        List<String> client = list("client");
        assertEquals(common.size(), new HashSet<>(common).size(), "duplicates in mixins");
        assertEquals(client.size(), new HashSet<>(client).size(), "duplicates in client");
        for (String name : client) assertTrue(name.startsWith("client."), name + " is in the client list but not in the client package");
        for (String name : common) assertFalse(name.contains("."), name + " is in the common list but not in the common package");
        assertFalse(common.isEmpty());
        assertFalse(client.isEmpty());
    }

    @Test void everyListedCommonMixinHasACompiledClass() throws Exception {
        List<String> missing = new ArrayList<>();
        for (String name : list("mixins")) if (!ResourceTree.exists(packagePath() + "/" + name + ".class")) missing.add(name);
        assertEquals(List.of(), missing);
    }

    @Test void everyListedClientMixinHasACompiledClass() throws Exception {
        List<String> missing = new ArrayList<>();
        for (String name : list("client")) if (!ResourceTree.exists(packagePath() + "/" + name.replace('.', '/') + ".class")) missing.add(name);
        assertEquals(List.of(), missing);
    }

    @Test void everyCompiledCommonMixinClassIsListed() throws Exception {
        Set<String> unlisted = new TreeSet<>(classesOnDisk(false));
        unlisted.removeAll(list("mixins"));
        assertEquals(Set.of(), unlisted, "mixin classes that are not in thaumcraft-common.mixins.json (they would never be applied)");
    }

    @Test void everyCompiledClientMixinClassIsListed() throws Exception {
        Set<String> unlisted = new TreeSet<>();
        for (String name : classesOnDisk(true)) unlisted.add("client." + name.substring("client/".length()));
        unlisted.removeAll(list("client"));
        assertEquals(Set.of(), unlisted, "client mixin classes that are not in the client list");
    }
}
