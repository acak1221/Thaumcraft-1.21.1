package thaumic.tinkerer.fabric.clientsmoke;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.BufferedReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;

/**
 * Reads model and blockstate JSON from the client's resource manager and follows parents and texture variables. Test-only
 * code of the port (no 1.7.10 counterpart); CC BY-NC-SA 3.0 like the add-on, not distributed.
 *
 * <p>This is deliberately independent of the game's own model loader: the loader reports a missing parent or texture as a
 * log line and a purple cube, the smoke wants the reason and the file that caused it.</p>
 */
final class ModelFiles {
    /** What a model resolves to: its chain, the textures it declares, the variables its faces use that nothing defines. */
    record Resolved(ResourceLocation model, List<ResourceLocation> chain, Map<String, String> textures, Set<String> unresolvedFaces,
            List<String> problems, List<ResourceLocation> overrideModels) {}

    private final ResourceManager manager;

    ModelFiles(ResourceManager manager) {
        this.manager = manager;
    }

    /** {@code ns:block/foo} becomes {@code ns:models/block/foo.json}. */
    static ResourceLocation modelFile(ResourceLocation model) {
        return ResourceLocation.fromNamespaceAndPath(model.getNamespace(), "models/" + model.getPath() + ".json");
    }

    static ResourceLocation textureFile(ResourceLocation texture) {
        return ResourceLocation.fromNamespaceAndPath(texture.getNamespace(), "textures/" + texture.getPath() + ".png");
    }

    Optional<JsonObject> readJson(ResourceLocation file) {
        return manager.getResource(file).map(resource -> {
            try (BufferedReader reader = resource.openAsReader()) {
                JsonElement element = JsonParser.parseReader(reader);
                return element.isJsonObject() ? element.getAsJsonObject() : null;
            } catch (IOException | RuntimeException e) {
                return null;
            }
        });
    }

    boolean exists(ResourceLocation file) {
        return manager.getResource(file).isPresent();
    }

    /** Follows {@code parent} up to the vanilla roots. {@code builtin/*} parents are the game's, not files. */
    Resolved resolve(ResourceLocation model) {
        List<ResourceLocation> chain = new ArrayList<>();
        Map<String, String> textures = new LinkedHashMap<>();
        Set<String> faceVariables = new LinkedHashSet<>();
        List<String> problems = new ArrayList<>();
        List<ResourceLocation> overrides = new ArrayList<>();
        ResourceLocation current = model;
        boolean first = true;
        while (current != null && chain.size() < 16) {
            if (current.getPath().startsWith("builtin/")) {
                chain.add(current);
                break;
            }
            chain.add(current);
            Optional<JsonObject> json = readJson(modelFile(current));
            if (json.isEmpty()) {
                problems.add((first ? "model file" : "parent model file") + " " + modelFile(current) + " is missing or not JSON");
                break;
            }
            JsonObject object = json.get();
            if (object.has("textures") && object.get("textures").isJsonObject()) {
                // a child's value wins over its parent's: the walk starts at the child, so only fill what is not set yet
                for (Map.Entry<String, JsonElement> entry : object.getAsJsonObject("textures").entrySet()) {
                    textures.putIfAbsent(entry.getKey(), entry.getValue().getAsString());
                }
            }
            if (object.has("elements") && object.get("elements").isJsonArray()) {
                for (JsonElement element : object.getAsJsonArray("elements")) {
                    JsonObject faces = element.getAsJsonObject().has("faces") ? element.getAsJsonObject().getAsJsonObject("faces") : null;
                    if (faces == null) continue;
                    for (Map.Entry<String, JsonElement> face : faces.entrySet()) {
                        String texture = face.getValue().getAsJsonObject().has("texture")
                                ? face.getValue().getAsJsonObject().get("texture").getAsString() : null;
                        if (texture != null && texture.startsWith("#")) faceVariables.add(texture.substring(1));
                    }
                }
            }
            if (first && object.has("overrides") && object.get("overrides").isJsonArray()) {
                for (JsonElement override : object.getAsJsonArray("overrides")) {
                    if (override.getAsJsonObject().has("model")) {
                        overrides.add(ResourceLocation.parse(override.getAsJsonObject().get("model").getAsString()));
                    }
                }
            }
            first = false;
            current = object.has("parent") ? ResourceLocation.parse(object.get("parent").getAsString()) : null;
        }
        Set<String> unresolved = new LinkedHashSet<>();
        for (String variable : faceVariables) {
            if (!resolvesToTexture(variable, textures)) unresolved.add(variable);
        }
        return new Resolved(model, chain, textures, unresolved, problems, overrides);
    }

    /** True when {@code #variable} ends in a texture path (not another undefined variable) within eight indirections. */
    static boolean resolvesToTexture(String variable, Map<String, String> textures) {
        String value = textures.get(variable);
        for (int i = 0; i < 8 && value != null; i++) {
            if (!value.startsWith("#")) return true;
            value = textures.get(value.substring(1));
        }
        return false;
    }

    /** The texture locations a model ends up using: every declared value that is not a variable reference. */
    static List<ResourceLocation> textureLocations(Resolved resolved) {
        List<ResourceLocation> locations = new ArrayList<>();
        for (String value : resolved.textures().values()) {
            if (!value.startsWith("#")) locations.add(ResourceLocation.parse(value));
        }
        return locations;
    }

    /** The model locations a blockstate JSON points to (variants, arrays of variants, multipart). */
    List<ResourceLocation> blockstateModels(ResourceLocation blockstate) {
        List<ResourceLocation> models = new ArrayList<>();
        Optional<JsonObject> json = readJson(ResourceLocation.fromNamespaceAndPath(blockstate.getNamespace(),
                "blockstates/" + blockstate.getPath() + ".json"));
        if (json.isEmpty()) return models;
        JsonObject root = json.get();
        if (root.has("variants")) {
            for (Map.Entry<String, JsonElement> variant : root.getAsJsonObject("variants").entrySet()) collectModel(variant.getValue(), models);
        }
        if (root.has("multipart")) {
            for (JsonElement part : root.getAsJsonArray("multipart")) {
                if (part.getAsJsonObject().has("apply")) collectModel(part.getAsJsonObject().get("apply"), models);
            }
        }
        return models;
    }

    private static void collectModel(JsonElement element, List<ResourceLocation> models) {
        if (element.isJsonArray()) {
            JsonArray array = element.getAsJsonArray();
            for (JsonElement each : array) collectModel(each, models);
        } else if (element.isJsonObject() && element.getAsJsonObject().has("model")) {
            ResourceLocation model = ResourceLocation.parse(element.getAsJsonObject().get("model").getAsString());
            if (!models.contains(model)) models.add(model);
        }
    }
}
