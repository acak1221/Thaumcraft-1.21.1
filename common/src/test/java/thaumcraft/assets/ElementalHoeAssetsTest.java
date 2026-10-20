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

/** Resource wiring of the Hoe of Growth (original ItemElementalHoe, ConfigRecipes:349, ConfigResearch ELEMENTALHOE). */
class ElementalHoeAssetsTest {
    private JsonObject json(String path) throws Exception {
        var stream = getClass().getClassLoader().getResourceAsStream(path);
        assertNotNull(stream, "missing resource " + path);
        try (var reader = new InputStreamReader(stream, StandardCharsets.UTF_8)) {
            return JsonParser.parseReader(reader).getAsJsonObject();
        }
    }

    @Test void theItemHasItsIconModelTagAndNames() throws Exception {
        var model = json("assets/thaumcraft/models/item/elemental_hoe.json");
        assertEquals("minecraft:item/handheld", model.get("parent").getAsString());
        assertEquals("thaumcraft:items/elementalhoe", model.getAsJsonObject("textures").get("layer0").getAsString());
        assertTrue(getClass().getClassLoader().getResource("assets/thaumcraft/textures/items/elementalhoe.png") != null, "the original icon");
        assertTrue(json("data/minecraft/tags/item/hoes.json").getAsJsonArray("values").asList().stream()
                .anyMatch(value -> value.getAsString().equals("thaumcraft:elemental_hoe")), "it is a hoe");
        assertEquals("Hoe of Growth", json("assets/thaumcraft/lang/en_us.json").get("item.thaumcraft.elemental_hoe").getAsString());
        assertEquals("Мотыга Роста", json("assets/thaumcraft/lang/ru_ru.json").get("item.thaumcraft.elemental_hoe").getAsString());
    }

    @Test void theInfusionKeepsTheOriginalRecipe() throws Exception {
        var recipe = json("data/thaumcraft/recipe/infusion/elemental_hoe.json");
        assertEquals("thaumcraft:infusion_stack", recipe.get("type").getAsString());
        assertEquals("ELEMENTALHOE", recipe.get("research").getAsString());
        assertEquals("thaumcraft:thaumium_hoe", recipe.getAsJsonObject("input").get("item").getAsString());
        assertEquals(List.of("thaumcraft:order_shard", "thaumcraft:entropy_shard", "minecraft:diamond", "thaumcraft:greatwood_log"),
                recipe.getAsJsonArray("components").asList().stream().map(c -> c.getAsJsonObject().get("item").getAsString()).toList());
        assertEquals(JsonParser.parseString("{\"meto\":8,\"herba\":8,\"terra\":8}"), recipe.get("aspects"));
        assertEquals(1, recipe.get("instability").getAsInt());
        assertEquals("thaumcraft:elemental_hoe", recipe.getAsJsonObject("result").get("id").getAsString());
    }
}
