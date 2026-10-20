package thaumcraft.assets;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;

/** Resource wiring of the Arcane Levitator (original ConfigBlocks, ConfigRecipes:194, ConfigResearch LEVITATOR). */
class LevitatorAssetsTest {
    private JsonObject json(String path) throws Exception {
        var stream = getClass().getClassLoader().getResourceAsStream(path);
        assertNotNull(stream, "missing resource " + path);
        try (var reader = new InputStreamReader(stream, StandardCharsets.UTF_8)) {
            return JsonParser.parseReader(reader).getAsJsonObject();
        }
    }

    @Test void theBlockHasItsStateModelsTexturesAndNames() throws Exception {
        assertEquals("thaumcraft:block/levitator", json("assets/thaumcraft/blockstates/levitator.json").getAsJsonObject("variants").getAsJsonObject("").get("model").getAsString());
        var model = json("assets/thaumcraft/models/block/levitator.json");
        assertEquals("minecraft:block/cube_bottom_top", model.get("parent").getAsString());
        var textures = model.getAsJsonObject("textures");
        assertEquals("thaumcraft:blocks/liftertop", textures.get("top").getAsString());
        assertEquals("thaumcraft:blocks/lifterside", textures.get("side").getAsString());
        assertEquals("thaumcraft:blocks/arcaneearbottom", textures.get("bottom").getAsString());
        for (String texture : new String[] {"liftertop", "lifterside", "arcaneearbottom"}) {
            assertTrue(getClass().getClassLoader().getResource("assets/thaumcraft/textures/blocks/" + texture + ".png") != null, texture);
        }
        assertEquals("thaumcraft:block/levitator", json("assets/thaumcraft/models/item/levitator.json").get("parent").getAsString());
        assertEquals("Arcane Levitator", json("assets/thaumcraft/lang/en_us.json").get("block.thaumcraft.levitator").getAsString());
        assertEquals("Магический подъёмник", json("assets/thaumcraft/lang/ru_ru.json").get("block.thaumcraft.levitator").getAsString());
    }

    @Test void itDropsItselfAndIsChoppedWithAnAxe() throws Exception {
        var loot = json("data/thaumcraft/loot_table/blocks/levitator.json");
        assertEquals("thaumcraft:levitator", loot.getAsJsonArray("pools").get(0).getAsJsonObject().getAsJsonArray("entries").get(0).getAsJsonObject().get("name").getAsString());
        assertTrue(json("data/minecraft/tags/block/mineable/axe.json").getAsJsonArray("values").asList().stream().anyMatch(v -> v.getAsString().equals("thaumcraft:levitator")));
    }

    @Test void theArcaneRecipeKeepsTheOriginalShapeIngredientsAndVis() throws Exception {
        var recipe = json("data/thaumcraft/recipe/arcane/levitator.json");
        assertEquals("thaumcraft:arcane_shaped", recipe.get("type").getAsString());
        assertEquals("LEVITATOR", recipe.get("research").getAsString());
        assertEquals(JsonParser.parseString("[\"WEW\",\"BNB\",\"WAW\"]"), recipe.get("pattern"));
        var key = recipe.getAsJsonObject("key");
        assertEquals("thaumcraft:greatwood_planks", key.getAsJsonObject("W").get("item").getAsString());
        assertEquals("thaumcraft:earth_shard", key.getAsJsonObject("E").get("item").getAsString());
        assertEquals("thaumcraft:air_shard", key.getAsJsonObject("A").get("item").getAsString());
        assertEquals("thaumcraft:nitor", key.getAsJsonObject("N").get("item").getAsString());
        assertEquals("minecraft:iron_ingot", key.getAsJsonObject("B").get("item").getAsString());
        assertEquals(JsonParser.parseString("{\"aer\":10,\"terra\":5}"), recipe.get("vis"));
        assertEquals("thaumcraft:levitator", recipe.getAsJsonObject("result").get("id").getAsString());
    }
}
