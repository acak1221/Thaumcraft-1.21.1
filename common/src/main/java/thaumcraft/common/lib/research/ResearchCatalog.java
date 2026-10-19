package thaumcraft.common.lib.research;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import thaumcraft.api.aspects.Aspect;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Loads the extracted original corpus without resolving old item or recipe code. */
public final class ResearchCatalog {
    private static final String RESOURCE = "data/thaumcraft/tc4/research.json";
    private static volatile Map<String, ResearchDefinition> definitions = Map.of();

    private ResearchCatalog() {}

    public static synchronized void loadBundled() {
        if (!definitions.isEmpty()) return;
        InputStream stream = ResearchCatalog.class.getClassLoader().getResourceAsStream(RESOURCE);
        if (stream == null) throw new IllegalStateException("Missing TC4 research corpus: " + RESOURCE);
        try (InputStreamReader reader = new InputStreamReader(stream, StandardCharsets.UTF_8)) {
            JsonObject root = JsonParser.parseReader(reader).getAsJsonObject();
            if (root.get("schema_version").getAsInt() != 1) throw new IllegalStateException("Unsupported research schema");
            LinkedHashMap<String, ResearchDefinition> loaded = new LinkedHashMap<>();
            for (JsonElement element : root.getAsJsonArray("research")) {
                JsonObject row = element.getAsJsonObject();
                Map<String, Integer> costs = new LinkedHashMap<>();
                for (Map.Entry<String, JsonElement> cost : row.getAsJsonObject("aspect_cost").entrySet()) {
                    if (Aspect.getAspect(cost.getKey()) == null || cost.getValue().getAsInt() <= 0) {
                        throw new IllegalStateException("Invalid aspect cost in " + row.get("key"));
                    }
                    costs.put(cost.getKey(), cost.getValue().getAsInt());
                }
                List<ResearchDefinition.Page> pages = new ArrayList<>();
                for (JsonElement pageElement : row.getAsJsonArray("pages")) {
                    JsonObject page = pageElement.getAsJsonObject();
                    pages.add(new ResearchDefinition.Page(text(page, "kind"), text(page, "text_key"),
                            text(page, "recipe_key"), text(page, "required_research"), text(page, "java_expression")));
                }
                ResearchDefinition definition = new ResearchDefinition(text(row, "key"), text(row, "category"),
                        row.get("column").getAsInt(), row.get("row").getAsInt(), row.get("complexity").getAsInt(),
                        row.get("warp").getAsInt(), costs, new LinkedHashSet<>(strings(row, "flags")),
                        strings(row, "parents"), strings(row, "parents_hidden"), strings(row, "siblings"),
                        strings(row, "aspect_triggers"), strings(row, "entity_triggers"),
                        strings(row, "item_trigger_expressions"), text(row, "icon_expression"), pages,
                        strings(row, "conditions"));
                if (loaded.putIfAbsent(definition.key(), definition) != null) {
                    throw new IllegalStateException("Duplicate research key " + definition.key());
                }
            }
            if (loaded.size() != 201) throw new IllegalStateException("Expected 201 original research definitions");
            for (ResearchDefinition definition : loaded.values()) {
                Set<String> dependencies = new LinkedHashSet<>(definition.parents());
                dependencies.addAll(definition.parentsHidden());
                dependencies.addAll(definition.siblings());
                if (!loaded.keySet().containsAll(dependencies)) {
                    throw new IllegalStateException("Unknown research dependency in " + definition.key());
                }
            }
            definitions = Collections.unmodifiableMap(loaded);
        } catch (java.io.IOException exception) {
            throw new IllegalStateException("Cannot load original TC4 research corpus", exception);
        }
    }

    public static ResearchDefinition get(String key) { return definitions.get(key); }
    public static Collection<ResearchDefinition> all() { return definitions.values(); }

    public static Set<String> unconditionalAutoUnlocks() {
        Set<String> result = new LinkedHashSet<>();
        for (ResearchDefinition definition : all()) {
            if (definition.isAutoUnlock() && definition.conditions().isEmpty()) result.add(definition.key());
        }
        return Set.copyOf(result);
    }

    private static List<String> strings(JsonObject row, String key) {
        List<String> result = new ArrayList<>();
        for (JsonElement element : row.getAsJsonArray(key)) result.add(element.getAsString());
        return result;
    }

    private static String text(JsonObject row, String key) {
        JsonElement value = row.get(key);
        return value == null || value.isJsonNull() ? null : value.getAsString();
    }
}
