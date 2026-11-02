package thaumcraft.common.lib.research;

import com.google.gson.JsonParser;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import thaumcraft.api.ThaumcraftApi;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.api.aspects.AspectList;

/** Owner-requested modern mob extension; the retained original entity corpus is unchanged. */
public final class ModernEntityAspectBindings {
    private static final String RESOURCE = "data/thaumcraft/port/modern_entity_aspects.json";
    private static final Map<String, Map<String, Integer>> ROWS = load();
    private ModernEntityAspectBindings() {}

    /** Fresh mutable API rows, so later addon replacements/removals remain authoritative. */
    public static List<ThaumcraftApi.EntityTags> tags() {
        var result = new ArrayList<ThaumcraftApi.EntityTags>();
        ROWS.forEach((entity, amounts) -> {
            var aspects = new AspectList();
            amounts.forEach((tag, amount) -> aspects.add(Aspect.getAspect(tag), amount));
            result.add(new ThaumcraftApi.EntityTags(entity, aspects, new ThaumcraftApi.EntityTagsNBT[0]));
        });
        return result;
    }

    private static Map<String, Map<String, Integer>> load() {
        var stream = ModernEntityAspectBindings.class.getClassLoader().getResourceAsStream(RESOURCE);
        if (stream == null) throw new IllegalStateException("Missing modern entity aspect extension");
        try (var reader = new InputStreamReader(stream, StandardCharsets.UTF_8)) {
            var root = JsonParser.parseReader(reader).getAsJsonObject();
            if (root.get("schema_version").getAsInt() != 1) throw new IllegalStateException("Unsupported modern entity aspect schema");
            var rows = new LinkedHashMap<String, Map<String, Integer>>();
            for (var entry : root.getAsJsonArray("entries")) {
                var row = entry.getAsJsonObject();
                String entity = row.get("entity").getAsString();
                if (!entity.matches("minecraft:[a-z0-9_]+")) throw new IllegalStateException("Invalid modern entity identifier");
                var amounts = new LinkedHashMap<String, Integer>();
                for (var amount : row.getAsJsonObject("aspects").entrySet()) {
                    if (Aspect.getAspect(amount.getKey()) == null || amount.getValue().getAsInt() <= 0)
                        throw new IllegalStateException("Invalid modern entity aspect: " + entity);
                    amounts.put(amount.getKey(), amount.getValue().getAsInt());
                }
                if (amounts.isEmpty() || rows.putIfAbsent(entity, java.util.Collections.unmodifiableMap(amounts)) != null)
                    throw new IllegalStateException("Empty or duplicate modern entity aspect row: " + entity);
            }
            return java.util.Collections.unmodifiableMap(rows);
        } catch (java.io.IOException failure) { throw new IllegalStateException("Cannot read modern entity aspect extension", failure); }
    }
}
