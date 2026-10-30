package thaumic.tinkerer.testing.parity;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

/**
 * The research and the recipes of Thaumic Tinkerer 1.7.10, as {@code scripts/porting/tt_oracle.py} extracted them
 * from the original source ({@code tt_oracle/research.json}, {@code recipes.json}). Test-only code of the port (no
 * 1.7.10 counterpart), plain Java so the comparison logic can be tested without a world. Distributed under the same
 * licence as the add-on (CC BY-NC-SA 3.0, see addons/thaumic-tinkerer/NOTICE.md).
 */
public final class OracleData {
    public static final String RESEARCH_RESOURCE = "/tt_oracle/research.json";
    public static final String RECIPES_RESOURCE = "/tt_oracle/recipes.json";

    /** A condition under which the original registers an entry: a configuration flag or a loaded mod. */
    public record Condition(String expr, String kind, boolean value) {
        @Override
        public String toString() {
            return (value ? "" : "!") + expr;
        }
    }

    /** An item expression of the original: its source text and, where it is mechanical, the 1.21.1 id. */
    public record Item(String text, String kind, String ref, String resolved, int count, int meta, String ore,
            String unresolved) {
        public boolean isOreName() {
            return "oredict".equals(kind) || "oredict_or_stack".equals(kind);
        }
    }

    public record Page(String kind, String key, List<String> recipes, String source) {}

    public record Research(String key, String category, boolean kami, Map<String, Integer> aspects, int column, int row,
            int complexity, int complexitySource, String iconResolved, String iconResource, List<String> parents,
            List<String> parentsHidden, boolean parentsHiddenAuto, List<String> siblings, Map<String, Boolean> flags,
            int warp, List<Page> pages, List<Condition> when, boolean registered, String owner, String source) {}

    public record Recipe(int order, String name, String kind, String research, Item output, Map<String, Integer> aspects,
            Integer instability, List<String> pattern, int width, int height, int ingredientCount, List<Item> cells,
            List<Item> components, Item input, Item catalyst, List<Condition> when, boolean registered, String owner,
            String source) {

        /** The ingredients in the order the port lists them: the cells of the pattern, or input + components, or the catalyst. */
        public List<Item> ingredients() {
            List<Item> all = new ArrayList<>();
            if (input != null) all.add(input);
            if (catalyst != null) all.add(catalyst);
            all.addAll(components);
            for (Item cell : cells) if (cell != null) all.add(cell);
            return all;
        }
    }

    private final List<Research> research;
    private final List<Recipe> recipes;
    private final String commit;

    private OracleData(List<Research> research, List<Recipe> recipes, String commit) {
        this.research = Collections.unmodifiableList(research);
        this.recipes = Collections.unmodifiableList(recipes);
        this.commit = commit;
    }

    public List<Research> research() {
        return research;
    }

    public List<Recipe> recipes() {
        return recipes;
    }

    /** The commit of the 1.7.10 source the oracle was extracted from. */
    public String commit() {
        return commit;
    }

    public static OracleData fromClasspath() {
        return parse(readResource(RESEARCH_RESOURCE), readResource(RECIPES_RESOURCE));
    }

    /** The raw document of an oracle resource (the tests copy it, edit a value and parse the copy). */
    public static JsonObject readResource(String name) {
        try (InputStream in = OracleData.class.getResourceAsStream(name)) {
            if (in == null) throw new IllegalStateException("the oracle file " + name + " is not on the classpath");
            try (Reader reader = new InputStreamReader(in, StandardCharsets.UTF_8)) {
                return JsonParser.parseReader(reader).getAsJsonObject();
            }
        } catch (IOException error) {
            throw new IllegalStateException("cannot read the oracle file " + name, error);
        }
    }

    /** Parses the two documents; a copy of either may have been edited first (the tests mutate values this way). */
    public static OracleData parse(JsonObject researchDocument, JsonObject recipesDocument) {
        List<Research> research = new ArrayList<>();
        for (JsonElement e : researchDocument.getAsJsonArray("entries")) research.add(parseResearch(e.getAsJsonObject()));
        List<Recipe> recipes = new ArrayList<>();
        for (JsonElement e : recipesDocument.getAsJsonArray("entries")) recipes.add(parseRecipe(e.getAsJsonObject()));
        String commit = researchDocument.getAsJsonObject("source").get("commit").getAsString();
        return new OracleData(research, recipes, commit);
    }

    private static Research parseResearch(JsonObject o) {
        JsonObject icon = o.getAsJsonObject("icon");
        List<Page> pages = new ArrayList<>();
        for (JsonElement p : o.getAsJsonArray("pages")) {
            JsonObject page = p.getAsJsonObject();
            pages.add(new Page(page.get("kind").getAsString(), string(page, "key"), strings(page, "recipes"), string(page, "source")));
        }
        Map<String, Boolean> flags = new LinkedHashMap<>();
        for (Map.Entry<String, JsonElement> f : o.getAsJsonObject("flags").entrySet()) flags.put(f.getKey(), f.getValue().getAsBoolean());
        return new Research(o.get("key").getAsString(), o.get("category").getAsString(), o.get("kami").getAsBoolean(),
                aspects(o.getAsJsonObject("aspects")), o.get("column").getAsInt(), o.get("row").getAsInt(),
                o.get("complexity").getAsInt(), o.get("complexitySource").getAsInt(), string(icon, "resolved"),
                string(icon, "resource"), strings(o, "parents"), strings(o, "parentsHidden"),
                o.get("parentsHiddenAuto").getAsBoolean(), strings(o, "siblings"), flags, o.get("warp").getAsInt(), pages,
                conditions(o), o.get("registered").getAsBoolean(), string(o, "owner"), string(o, "source"));
    }

    private static Recipe parseRecipe(JsonObject o) {
        List<Item> cells = new ArrayList<>();
        List<String> pattern = o.has("pattern") ? strings(o, "pattern") : List.of();
        if (o.has("key")) {
            JsonObject key = o.getAsJsonObject("key");
            for (String row : pattern) {
                for (char c : row.toCharArray()) {
                    if (c != ' ') cells.add(item(key.getAsJsonObject(String.valueOf(c))));
                }
            }
        }
        List<Item> components = new ArrayList<>();
        if (o.has("components")) for (JsonElement e : o.getAsJsonArray("components")) components.add(item(e.getAsJsonObject()));
        if (o.has("ingredients")) for (JsonElement e : o.getAsJsonArray("ingredients")) components.add(item(e.getAsJsonObject()));
        return new Recipe(o.get("order").getAsInt(), string(o, "name"), o.get("kind").getAsString(), string(o, "research"),
                item(o.getAsJsonObject("output")), o.has("aspects") ? aspects(o.getAsJsonObject("aspects")) : null,
                o.has("instability") ? o.get("instability").getAsInt() : null, pattern,
                o.has("width") ? o.get("width").getAsInt() : 0, o.has("height") ? o.get("height").getAsInt() : 0,
                o.has("ingredientCount") ? o.get("ingredientCount").getAsInt() : 0, cells, components,
                o.has("input") ? item(o.getAsJsonObject("input")) : null,
                o.has("catalyst") ? item(o.getAsJsonObject("catalyst")) : null, conditions(o),
                o.get("registered").getAsBoolean(), string(o, "owner"), string(o, "source"));
    }

    private static Item item(JsonObject o) {
        return new Item(o.get("text").getAsString(), o.get("kind").getAsString(), string(o, "ref"), string(o, "resolved"),
                o.has("count") ? o.get("count").getAsInt() : 1, o.has("meta") ? o.get("meta").getAsInt() : 0, string(o, "ore"),
                string(o, "unresolved"));
    }

    private static List<Condition> conditions(JsonObject o) {
        List<Condition> out = new ArrayList<>();
        for (JsonElement e : o.getAsJsonArray("when")) {
            JsonObject c = e.getAsJsonObject();
            out.add(new Condition(c.get("expr").getAsString(), c.get("kind").getAsString(), c.get("value").getAsBoolean()));
        }
        return out;
    }

    private static Map<String, Integer> aspects(JsonObject o) {
        Map<String, Integer> out = new LinkedHashMap<>();
        for (Map.Entry<String, JsonElement> e : o.entrySet()) out.put(e.getKey(), e.getValue().getAsInt());
        return out;
    }

    private static List<String> strings(JsonObject o, String name) {
        List<String> out = new ArrayList<>();
        if (!o.has(name) || o.get(name).isJsonNull()) return out;
        JsonArray array = o.getAsJsonArray(name);
        for (JsonElement e : array) out.add(e.getAsString());
        return out;
    }

    private static String string(JsonObject o, String name) {
        return o.has(name) && !o.get(name).isJsonNull() ? o.get(name).getAsString() : null;
    }
}
