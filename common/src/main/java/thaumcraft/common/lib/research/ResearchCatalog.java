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

/**
 * Loads the extracted original corpus without resolving old item or recipe code, and lays the research of
 * add-ons over it.
 *
 * <p>{@link #all()} and {@link #get(String)} are the live view every gameplay system reads: the original
 * corpus, with the entries an add-on changed through the public API in place, followed by the research
 * add-ons registered through {@code ResearchCategories.addResearch}, in registration order.
 * {@link #bundled()} and {@link #original(String)} are the unchanged original corpus.</p>
 */
public final class ResearchCatalog {
    private static final String RESOURCE = "data/thaumcraft/tc4/research.json";
    private static volatile Map<String, ResearchDefinition> definitions = Map.of();
    /** Original entries an add-on changed through the public API, by key. */
    private static volatile Map<String, ResearchDefinition> overrides = Map.of();
    /** Add-on entries, in registration order. */
    private static volatile Map<String, ResearchDefinition> addons = Map.of();
    /** The corpus with its overrides, then the add-on entries. */
    private static volatile Map<String, ResearchDefinition> live = Map.of();

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
            rebuild();
        } catch (java.io.IOException exception) {
            throw new IllegalStateException("Cannot load original TC4 research corpus", exception);
        }
    }

    /** The live entry of a key: an original one (as an add-on may have changed it) or an add-on one. */
    public static ResearchDefinition get(String key) { return key == null ? null : live.get(key); }
    /** Every live entry: the original corpus in its order, then the add-on entries in registration order. */
    public static Collection<ResearchDefinition> all() { return live.values(); }
    /** The unchanged original corpus. */
    public static Collection<ResearchDefinition> bundled() { return definitions.values(); }
    /** The unchanged original entry of a key, or null for an add-on key. */
    public static ResearchDefinition original(String key) { return key == null ? null : definitions.get(key); }
    /** True for a key of the original corpus. */
    public static boolean isBundled(String key) { return key != null && definitions.containsKey(key); }
    /** The add-on entries, in registration order. */
    public static Collection<ResearchDefinition> addons() { return addons.values(); }

    /**
     * Replaces the add-on layer: the add-on entries and the changed original entries.
     *
     * <p>Called by {@link AddonResearch} only. An add-on entry may not reuse an original key and an override
     * must keep the key it replaces.</p>
     */
    static synchronized void replaceAddonLayer(Map<String, ResearchDefinition> addonEntries,
                                               Map<String, ResearchDefinition> changedOriginals) {
        loadBundled();
        LinkedHashMap<String, ResearchDefinition> added = new LinkedHashMap<>();
        for (var entry : addonEntries.entrySet()) {
            if (definitions.containsKey(entry.getKey()) || !entry.getKey().equals(entry.getValue().key()))
                throw new IllegalArgumentException("Invalid add-on research " + entry.getKey());
            added.put(entry.getKey(), entry.getValue());
        }
        LinkedHashMap<String, ResearchDefinition> changed = new LinkedHashMap<>();
        for (var entry : changedOriginals.entrySet()) {
            if (!definitions.containsKey(entry.getKey()) || !entry.getKey().equals(entry.getValue().key()))
                throw new IllegalArgumentException("Invalid research override " + entry.getKey());
            changed.put(entry.getKey(), entry.getValue());
        }
        addons = Collections.unmodifiableMap(added);
        overrides = Collections.unmodifiableMap(changed);
        rebuild();
    }

    private static void rebuild() {
        LinkedHashMap<String, ResearchDefinition> view = new LinkedHashMap<>();
        for (ResearchDefinition definition : definitions.values())
            view.put(definition.key(), overrides.getOrDefault(definition.key(), definition));
        view.putAll(addons);
        live = Collections.unmodifiableMap(view);
    }

    public static Set<String> unconditionalAutoUnlocks() {
        Set<String> result = new LinkedHashSet<>();
        for (ResearchDefinition definition : all()) {
            if (definition.isAutoUnlock() && definition.isEnabled()) result.add(definition.key());
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
