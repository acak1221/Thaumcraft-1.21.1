package thaumcraft.common.lib.research;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.resources.ResourceLocation;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.api.aspects.AspectList;

import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/** Bounded original-TC4 overrides, not a replacement for the complete aspect catalogue. */
public final class EarlyObjectAspectBindings {
    private static final String RESOURCE = "data/thaumcraft/tc4/early_object_aspects.json";

    private EarlyObjectAspectBindings() {}

    /** Apply after the legacy fallback with put/putAll, including explicitly empty bindings. */
    public static Map<ResourceLocation, AspectList> direct() {
        return copy(Holder.BINDINGS.direct());
    }

    /**
     * Raw additions only; the recipe resolver supplies the original 0.75-scaled recipe base.
     * A divergent legacy fallback for these ids is not an original direct registration.
     */
    public static Map<ResourceLocation, AspectList> complex() {
        return copy(Holder.BINDINGS.complex());
    }

    /** The raw addition of one id, as an independent copy, or null; {@link #complex()} without copying all of them. */
    public static AspectList complexOf(ResourceLocation id) {
        AspectList addition = Holder.BINDINGS.complex().get(id);
        return addition == null ? null : addition.copy();
    }

    /** Old subtype identities are flattened; a declared wildcard remains a wildcard rather than becoming native damage0. */
    public static Integer metadata(ResourceLocation id) { return Holder.BINDINGS.metadata().get(id); }

    // Neither the resource nor Aspect instances are touched when this outer class is initialized.
    private static final class Holder {
        private static final Bindings BINDINGS = load();
    }

    private static Bindings load() {
        var stream = EarlyObjectAspectBindings.class.getClassLoader().getResourceAsStream(RESOURCE);
        if (stream == null) throw new IllegalStateException("Missing early object aspect bindings: " + RESOURCE);
        try (var reader = new InputStreamReader(stream, StandardCharsets.UTF_8)) {
            JsonObject root = JsonParser.parseReader(reader).getAsJsonObject();
            if (root.get("schema_version").getAsBigDecimal().intValueExact() != 1) {
                throw new IllegalStateException("Unsupported early object aspect schema");
            }
            Map<ResourceLocation, AspectList> direct = new LinkedHashMap<>();
            Map<ResourceLocation, AspectList> complex = new LinkedHashMap<>();
            Map<ResourceLocation, Integer> metadata = new LinkedHashMap<>();
            int previousLine = 0;
            for (JsonElement element : root.getAsJsonArray("bindings")) {
                JsonObject row = element.getAsJsonObject();
                int line = row.get("original_source_line").getAsBigDecimal().intValueExact();
                if (line <= 0 || line < previousLine) {
                    throw new IllegalStateException("Early aspect rows are not in original registration order: " + line);
                }
                previousLine = line;
                String registration = row.get("registration").getAsString();
                if (!"direct".equals(registration) && !"complex".equals(registration)) {
                    throw new IllegalStateException("Unknown early aspect registration at original source line " + line);
                }
                AspectList aspects = new AspectList();
                for (Map.Entry<String, JsonElement> amount : row.getAsJsonObject("aspects").entrySet()) {
                    // Calling the static method initializes all of Aspect's built-ins before lookup.
                    Aspect aspect = Aspect.getAspect(amount.getKey());
                    int value = amount.getValue().getAsBigDecimal().intValueExact();
                    if (aspect == null || value <= 0) {
                        throw new IllegalStateException("Invalid early aspect binding at original source line " + line);
                    }
                    aspects.add(aspect, value);
                }
                if (row.getAsJsonArray("modern_ids").isEmpty()) {
                    throw new IllegalStateException("Missing early aspect identifiers at original source line " + line);
                }
                for (JsonElement identifier : row.getAsJsonArray("modern_ids")) {
                    ResourceLocation id = ResourceLocation.parse(identifier.getAsString());
                    var declared = row.get("original_metadata");
                    int nativeMetadata = declared != null && declared.isJsonPrimitive()
                            && declared.getAsJsonPrimitive().isString() && declared.getAsString().equals("wildcard:32767")
                            ? ObjectAspectRegistry.WILDCARD : 0;
                    if ("direct".equals(registration)) {
                        direct.put(id, aspects.copy());
                        complex.remove(id);
                        metadata.put(id, nativeMetadata);
                    } else if (!direct.containsKey(id)) {
                        // Original API259-273 does not add the supplied amounts once a tag exists.
                        // This bounded resource includes only the no-existing-tag complex branch.
                        complex.putIfAbsent(id, aspects.copy());
                        metadata.putIfAbsent(id, nativeMetadata);
                    }
                }
            }
            return new Bindings(Collections.unmodifiableMap(direct), Collections.unmodifiableMap(complex), Collections.unmodifiableMap(metadata));
        } catch (IOException | IllegalArgumentException | IllegalStateException | ArithmeticException exception) {
            throw new IllegalStateException("Cannot load early object aspect bindings: " + RESOURCE, exception);
        }
    }

    /** Map structure is immutable; every value and every call is isolated from the cached data. */
    private static Map<ResourceLocation, AspectList> copy(Map<ResourceLocation, AspectList> source) {
        Map<ResourceLocation, AspectList> result = new LinkedHashMap<>();
        source.forEach((id, aspects) -> result.put(id, aspects.copy()));
        return Collections.unmodifiableMap(result);
    }

    private record Bindings(Map<ResourceLocation, AspectList> direct, Map<ResourceLocation, AspectList> complex, Map<ResourceLocation, Integer> metadata) {}
}
