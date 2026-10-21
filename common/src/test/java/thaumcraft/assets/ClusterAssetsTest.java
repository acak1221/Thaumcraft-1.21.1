package thaumcraft.assets;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.List;
import org.junit.jupiter.api.Test;

/** Resource wiring of the native iron, gold and cinnabar clusters (original ItemNugget 16 / 31 / 21, ConfigRecipes:112-113, 126-127, 469-471). */
class ClusterAssetsTest {
    private record Cluster(String id, String icon, String english, String russian, String smelts, int count) {}

    private static final List<Cluster> CLUSTERS = List.of(
            new Cluster("cluster_iron", "clusteriron", "Native Iron Cluster", "Железорудный кластер", "minecraft:iron_ingot", 2),
            new Cluster("cluster_gold", "clustergold", "Native Gold Cluster", "Золоторудный кластер", "minecraft:gold_ingot", 2),
            new Cluster("cluster_cinnabar", "clustercinnabar", "Native Cinnabar Cluster", "Киноварный кластер", "thaumcraft:quick_silver", 2));

    private JsonObject json(String path) throws Exception {
        var stream = getClass().getClassLoader().getResourceAsStream(path);
        assertNotNull(stream, "missing resource " + path);
        try (var reader = new InputStreamReader(stream, StandardCharsets.UTF_8)) {
            return JsonParser.parseReader(reader).getAsJsonObject();
        }
    }

    private boolean exists(String path) { return getClass().getClassLoader().getResource(path) != null; }

    @Test void everyClusterHasItsIconModelNamesAndFurnaceRecipe() throws Exception {
        for (Cluster cluster : CLUSTERS) {
            var model = json("assets/thaumcraft/models/item/" + cluster.id() + ".json");
            assertEquals("minecraft:item/generated", model.get("parent").getAsString(), cluster.id());
            assertEquals("thaumcraft:items/" + cluster.icon(), model.getAsJsonObject("textures").get("layer0").getAsString(), cluster.id());
            assertTrue(exists("assets/thaumcraft/textures/items/" + cluster.icon() + ".png"), cluster.id() + " icon");
            assertEquals(cluster.english(), json("assets/thaumcraft/lang/en_us.json").get("item.thaumcraft." + cluster.id()).getAsString(), cluster.id());
            assertEquals(cluster.russian(), json("assets/thaumcraft/lang/ru_ru.json").get("item.thaumcraft." + cluster.id()).getAsString(), cluster.id());
            var smelting = json("data/thaumcraft/recipe/" + cluster.id() + "_smelting.json");
            assertEquals("thaumcraft:stack_smelting", smelting.get("type").getAsString(), cluster.id());
            assertEquals("thaumcraft:" + cluster.id(), smelting.getAsJsonObject("ingredient").get("item").getAsString(), cluster.id());
            assertEquals(cluster.smelts(), smelting.getAsJsonObject("result").get("id").getAsString(), cluster.id());
            assertEquals(cluster.count(), smelting.getAsJsonObject("result").get("count").getAsInt(), cluster.id());
            assertEquals(1.0F, smelting.get("experience").getAsFloat(), cluster.id());
            assertEquals(200, smelting.get("cookingtime").getAsInt(), cluster.id());
        }
    }

    @Test void theSixCrucibleRecipesKeepTheirOriginalNumbers() throws Exception {
        // path, research, original recipe order, result, count, aspects
        Object[][] recipes = {
                {"pure_iron", "PUREIRON", 22, "thaumcraft:cluster_iron", 1, "{\"metallum\":1,\"ordo\":1}"},
                {"pure_gold", "PUREGOLD", 23, "thaumcraft:cluster_gold", 1, "{\"metallum\":1,\"ordo\":1}"},
                {"pure_copper", "PURECOPPER", 24, "thaumcraft:cluster_copper", 1, "{\"metallum\":1,\"ordo\":1}"},
                {"trans_iron", "TRANSIRON", 25, "minecraft:iron_nugget", 3, "{\"metallum\":2}"},
                {"trans_gold", "TRANSGOLD", 26, "minecraft:gold_nugget", 3, "{\"metallum\":2,\"lucrum\":1}"},
                {"trans_copper", "TRANSCOPPER", 27, "thaumcraft:copper_nugget", 3, "{\"metallum\":2,\"permutatio\":1}"}};
        for (Object[] row : recipes) {
            var recipe = json("data/thaumcraft/recipe/crucible/" + row[0] + ".json");
            assertEquals("thaumcraft:crucible", recipe.get("type").getAsString(), (String) row[0]);
            assertEquals(row[1], recipe.get("research").getAsString(), (String) row[0]);
            assertEquals(row[2], recipe.get("source_order").getAsInt(), (String) row[0]);
            assertEquals(row[3], recipe.getAsJsonObject("result").get("id").getAsString(), (String) row[0]);
            assertEquals(row[4], recipe.getAsJsonObject("result").get("count").getAsInt(), (String) row[0]);
            assertEquals(JsonParser.parseString((String) row[5]), recipe.get("aspects"), (String) row[0]);
        }
        // Ores and raw ore (both stand for oreIron / oreGold of the original); nuggets of the matching metal only.
        assertEquals(List.of("c:ores/iron", "c:raw_materials/iron"), tags(json("data/thaumcraft/recipe/crucible/pure_iron.json")));
        assertEquals(List.of("c:ores/gold", "c:raw_materials/gold"), tags(json("data/thaumcraft/recipe/crucible/pure_gold.json")));
        assertEquals(List.of("c:nuggets/iron"), tags(json("data/thaumcraft/recipe/crucible/trans_iron.json")));
    }

    private List<String> tags(JsonObject recipe) {
        var catalyst = recipe.get("catalyst");
        return (catalyst.isJsonArray() ? catalyst.getAsJsonArray().asList() : List.of(catalyst)).stream()
                .map(entry -> entry.getAsJsonObject().get("tag").getAsString()).toList();
    }
}
