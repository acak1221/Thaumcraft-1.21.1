package thaumcraft.common.lib.research;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import net.minecraft.resources.ResourceLocation;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.api.aspects.AspectList;

/**
 * The port's own values for 1.21.1 items that no TC4 registration, aspect family or recipe reaches
 * (server setting {@code aspect_inference}): an item that is a form of another one takes that one's
 * aspects, and a raw material without a counterpart has a literal list. Not TC4 data.
 */
public final class AspectInferenceTable {
    private static final String RESOURCE = "data/thaumcraft/port/aspect_inference.json";

    private AspectInferenceTable() {}

    /** The item this one is a form of, or null. */
    public static ResourceLocation likeOf(ResourceLocation id) { return Holder.TABLE.like().get(id); }

    /** The literal aspects of a raw material, as an independent copy, or null. */
    public static AspectList aspectsOf(ResourceLocation id) {
        AspectList aspects = Holder.TABLE.aspects().get(id);
        return aspects == null ? null : aspects.copy();
    }

    /** Every id the table names, for the registry check of the tests. */
    public static java.util.Set<ResourceLocation> ids() {
        var ids = new java.util.LinkedHashSet<>(Holder.TABLE.like().keySet());
        ids.addAll(Holder.TABLE.like().values());
        ids.addAll(Holder.TABLE.aspects().keySet());
        return ids;
    }

    private static final class Holder {
        private static final Table TABLE = load();
    }

    private record Table(Map<ResourceLocation, ResourceLocation> like, Map<ResourceLocation, AspectList> aspects) {}

    private static Table load() {
        var stream = AspectInferenceTable.class.getClassLoader().getResourceAsStream(RESOURCE);
        if (stream == null) throw new IllegalStateException("Missing aspect inference table: " + RESOURCE);
        try (var reader = new InputStreamReader(stream, StandardCharsets.UTF_8)) {
            JsonObject root = JsonParser.parseReader(reader).getAsJsonObject();
            if (root.get("schema_version").getAsInt() != 1) throw new IllegalStateException("Unsupported aspect inference schema");
            Map<ResourceLocation, ResourceLocation> like = new LinkedHashMap<>();
            for (Map.Entry<String, JsonElement> entry : root.getAsJsonObject("like").entrySet()) {
                like.put(ResourceLocation.parse(entry.getKey()), ResourceLocation.parse(entry.getValue().getAsString()));
            }
            Map<ResourceLocation, AspectList> aspects = new LinkedHashMap<>();
            for (Map.Entry<String, JsonElement> entry : root.getAsJsonObject("aspects").entrySet()) {
                ResourceLocation id = ResourceLocation.parse(entry.getKey());
                if (like.containsKey(id)) throw new IllegalStateException("Aspect inference names " + id + " twice");
                AspectList list = new AspectList();
                for (Map.Entry<String, JsonElement> amount : entry.getValue().getAsJsonObject().entrySet()) {
                    Aspect aspect = Aspect.getAspect(amount.getKey());
                    int value = amount.getValue().getAsInt();
                    if (aspect == null || value <= 0) throw new IllegalStateException("Invalid inferred aspect " + amount.getKey() + " of " + id);
                    list.add(aspect, value);
                }
                aspects.put(id, list);
            }
            return new Table(Collections.unmodifiableMap(like), Collections.unmodifiableMap(aspects));
        } catch (IOException | IllegalArgumentException exception) {
            throw new IllegalStateException("Cannot load the aspect inference table: " + RESOURCE, exception);
        }
    }
}
