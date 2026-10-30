package thaumic.tinkerer.fabric.clientsmoke;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;

/**
 * The language files of every namespace for one language code, read straight from the resource manager. Test-only code of
 * the port (no 1.7.10 counterpart); CC BY-NC-SA 3.0 like the add-on, not distributed.
 *
 * <p>Switching the game to Russian means a full resource reload (seconds with the software renderer, and a re-stitched atlas
 * under every later picture), so the Russian keys are compared as files. The game's own lookup is
 * {@code Language.getInstance().has(key)} for the language the run starts in (English).</p>
 */
final class LangFiles {
    private LangFiles() {}

    /** Union of {@code <namespace>:lang/<code>.json} over all namespaces and packs; a later pack wins. */
    static Map<String, String> load(ResourceManager manager, String code) throws IOException {
        Map<String, String> keys = new HashMap<>();
        for (String namespace : manager.getNamespaces()) {
            for (Resource resource : manager.getResourceStack(ResourceLocation.fromNamespaceAndPath(namespace, "lang/" + code + ".json"))) {
                try (var reader = resource.openAsReader()) {
                    JsonObject object = JsonParser.parseReader(reader).getAsJsonObject();
                    for (Map.Entry<String, JsonElement> entry : object.entrySet()) {
                        if (entry.getValue().isJsonPrimitive()) keys.put(entry.getKey(), entry.getValue().getAsString());
                    }
                }
            }
        }
        return keys;
    }
}
