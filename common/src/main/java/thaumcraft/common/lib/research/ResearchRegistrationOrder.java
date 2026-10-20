package thaumcraft.common.lib.research;

import com.google.gson.JsonParser;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;

/** Original category registration plus each FULL default HashMap's values order, shared by hidden-note and clue RNG. */
public final class ResearchRegistrationOrder {
    private static final List<String> CATEGORIES = List.of("BASICS", "THAUMATURGY", "ALCHEMY", "ARTIFICE", "GOLEMANCY", "ELDRITCH");
    private static volatile List<String> registrationKeys;
    private ResearchRegistrationOrder() {}

    public static List<ResearchDefinition> enabled() {
        var categories = new LinkedHashMap<String, HashMap<String, ResearchDefinition>>();
        for (String category : CATEGORIES) categories.put(category, new HashMap<>());
        for (String key : originalRegistration()) {
            var definition = ResearchCatalog.get(key);
            if (definition != null && definition.isEnabled()) categories.get(definition.category()).put(key, definition);
        }
        var ordered = new ArrayList<ResearchDefinition>();
        for (var category : categories.values()) ordered.addAll(category.values());
        return List.copyOf(ordered);
    }

    private static List<String> originalRegistration() {
        List<String> ready = registrationKeys;
        if (ready != null) return ready;
        synchronized (ResearchRegistrationOrder.class) {
            if (registrationKeys != null) return registrationKeys;
            ResearchCatalog.loadBundled();
            var stream = ResearchRegistrationOrder.class.getClassLoader().getResourceAsStream("data/thaumcraft/tc4/research.json");
            if (stream == null) throw new IllegalStateException("Missing original research-order provenance");
            record Row(String key, int line) {}
            try (var reader = new InputStreamReader(stream, StandardCharsets.UTF_8)) {
                var rows = new ArrayList<Row>();
                for (var value : JsonParser.parseReader(reader).getAsJsonObject().getAsJsonArray("research")) {
                    var row = value.getAsJsonObject();
                    rows.add(new Row(row.get("key").getAsString(), row.getAsJsonObject("source").get("line").getAsInt()));
                }
                rows.sort(Comparator.comparingInt(Row::line));
                if (rows.size() != ResearchCatalog.all().size()) throw new IllegalStateException("Incomplete original research-order provenance");
                registrationKeys = rows.stream().map(Row::key).toList();
                return registrationKeys;
            } catch (java.io.IOException invalid) { throw new IllegalStateException("Cannot load original research-order provenance", invalid); }
        }
    }
}
