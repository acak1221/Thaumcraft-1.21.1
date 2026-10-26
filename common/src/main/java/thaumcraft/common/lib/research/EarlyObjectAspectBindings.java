package thaumcraft.common.lib.research;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.api.aspects.AspectList;

import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Original-TC4 overrides: the bounded early rows, then the rest of the original ConfigAspects that
 * {@code scripts/porting/extract_config_aspects.py} accounts for from the bytecode, and the aspect
 * families of the port's own aspect inference.
 */
public final class EarlyObjectAspectBindings {
    private static final String RESOURCE = "data/thaumcraft/tc4/early_object_aspects.json";
    private static final String COMPLETION = "data/thaumcraft/tc4/original_object_aspects.json";

    /**
     * An original {@code new AspectList(new ItemStack(base))} copy: the resolved tags of {@code base}
     * plus {@code additions}. A complex one also keeps the item's own recipe-generated base.
     */
    public record Expression(boolean complex, ResourceLocation base, AspectList additions) {
        public Expression { additions = additions.copy(); }
        @Override public AspectList additions() { return additions.copy(); }
    }

    /**
     * One original registration as the port's aspect inference applies it to the members of a tag:
     * a direct one replaces nothing but gives an item without a registration its aspects, a complex
     * one adds to the item's recipe-generated base.
     */
    public record Family(TagKey<Item> tag, boolean complex, AspectList aspects) {
        public Family { aspects = aspects.copy(); }
        @Override public AspectList aspects() { return aspects.copy(); }
    }

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

    /** The original copy expression of one id, or null. */
    public static Expression expressionOf(ResourceLocation id) { return Holder.BINDINGS.expressions().get(id); }

    /** Every id whose original row is a copy expression. */
    public static java.util.Set<ResourceLocation> expressions() { return Holder.BINDINGS.expressions().keySet(); }

    /** The aspect families in the original registration order; a later family wins over an earlier one. */
    public static List<Family> families() { return Holder.BINDINGS.families(); }

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
            Map<ResourceLocation, Expression> expressions = new LinkedHashMap<>();
            List<Family> families = new ArrayList<>();
            loadCompletion(direct, complex, metadata, expressions, families);
            return new Bindings(Collections.unmodifiableMap(direct), Collections.unmodifiableMap(complex), Collections.unmodifiableMap(metadata),
                    Collections.unmodifiableMap(expressions), List.copyOf(families));
        } catch (IOException | IllegalArgumentException | IllegalStateException | ArithmeticException exception) {
            throw new IllegalStateException("Cannot load early object aspect bindings: " + RESOURCE, exception);
        }
    }

    /**
     * The rest of the original ConfigAspects, in the original order. A row never repeats an id of an
     * early row (the extraction script checks it), so the early rows keep their place.
     */
    private static void loadCompletion(Map<ResourceLocation, AspectList> direct, Map<ResourceLocation, AspectList> complex,
                                       Map<ResourceLocation, Integer> metadata, Map<ResourceLocation, Expression> expressions,
                                       List<Family> families) throws IOException {
        var stream = EarlyObjectAspectBindings.class.getClassLoader().getResourceAsStream(COMPLETION);
        if (stream == null) throw new IllegalStateException("Missing original object aspect rows: " + COMPLETION);
        try (var reader = new InputStreamReader(stream, StandardCharsets.UTF_8)) {
            JsonObject root = JsonParser.parseReader(reader).getAsJsonObject();
            if (root.get("schema_version").getAsBigDecimal().intValueExact() != 1) {
                throw new IllegalStateException("Unsupported original object aspect schema");
            }
            var early = new java.util.HashSet<>(direct.keySet());
            early.addAll(complex.keySet());
            int previousLine = 0;
            for (JsonElement element : root.getAsJsonArray("rows")) {
                JsonObject row = element.getAsJsonObject();
                previousLine = ordered(row, previousLine);
                boolean isComplex = registration(row);
                AspectList aspects = aspects(row);
                var declared = row.get("original_target").getAsString().endsWith("@32767") ? ObjectAspectRegistry.WILDCARD : 0;
                ResourceLocation base = row.has("base_item") ? ResourceLocation.parse(row.get("base_item").getAsString()) : null;
                if (row.getAsJsonArray("modern_ids").isEmpty()) throw new IllegalStateException("Missing identifiers at jar line " + previousLine);
                for (JsonElement identifier : row.getAsJsonArray("modern_ids")) {
                    ResourceLocation id = ResourceLocation.parse(identifier.getAsString());
                    if (early.contains(id)) {
                        throw new IllegalStateException("Original row repeats an early id " + id + " at jar line " + previousLine);
                    }
                    // Like the original registry, a later row replaces an earlier one (dirt, then podzol).
                    if (base != null) {
                        expressions.put(id, new Expression(isComplex, base, aspects));
                        direct.remove(id);
                        complex.remove(id);
                        metadata.put(id, declared);
                    } else if (!isComplex) {
                        direct.put(id, aspects.copy());
                        expressions.remove(id);
                        complex.remove(id);
                        metadata.put(id, declared);
                    } else if (!direct.containsKey(id) && !expressions.containsKey(id)) {
                        // Original API259-273: a complex row adds nothing once the item has a tag.
                        complex.putIfAbsent(id, aspects.copy());
                        metadata.putIfAbsent(id, declared);
                    }
                }
            }
            previousLine = 0;
            for (JsonElement element : root.getAsJsonArray("families")) {
                JsonObject row = element.getAsJsonObject();
                previousLine = ordered(row, previousLine);
                if (row.has("base_item")) throw new IllegalStateException("An aspect family cannot copy a base at jar line " + previousLine);
                families.add(new Family(TagKey.create(Registries.ITEM, ResourceLocation.parse(row.get("tag").getAsString())),
                        registration(row), aspects(row)));
            }
        }
    }

    private static int ordered(JsonObject row, int previousLine) {
        int line = row.get("original_jar_line").getAsBigDecimal().intValueExact();
        if (line <= 0 || line < previousLine) throw new IllegalStateException("Original aspect rows are not in jar line order: " + line);
        return line;
    }

    private static boolean registration(JsonObject row) {
        String registration = row.get("registration").getAsString();
        if (!"direct".equals(registration) && !"complex".equals(registration)) {
            throw new IllegalStateException("Unknown original aspect registration " + registration);
        }
        return "complex".equals(registration);
    }

    private static AspectList aspects(JsonObject row) {
        AspectList aspects = new AspectList();
        for (Map.Entry<String, JsonElement> amount : row.getAsJsonObject("aspects").entrySet()) {
            Aspect aspect = Aspect.getAspect(amount.getKey());
            int value = amount.getValue().getAsBigDecimal().intValueExact();
            if (aspect == null || value <= 0) throw new IllegalStateException("Invalid original aspect " + amount.getKey() + "=" + value);
            aspects.add(aspect, value);
        }
        return aspects;
    }

    /** Map structure is immutable; every value and every call is isolated from the cached data. */
    private static Map<ResourceLocation, AspectList> copy(Map<ResourceLocation, AspectList> source) {
        Map<ResourceLocation, AspectList> result = new LinkedHashMap<>();
        source.forEach((id, aspects) -> result.put(id, aspects.copy()));
        return Collections.unmodifiableMap(result);
    }

    private record Bindings(Map<ResourceLocation, AspectList> direct, Map<ResourceLocation, AspectList> complex, Map<ResourceLocation, Integer> metadata,
                            Map<ResourceLocation, Expression> expressions, List<Family> families) {}
}
