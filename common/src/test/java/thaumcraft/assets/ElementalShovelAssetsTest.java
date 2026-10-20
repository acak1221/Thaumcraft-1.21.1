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

/** Resource wiring of the Shovel of the Earthmover (original ItemElementalShovel, ConfigRecipes:348, ConfigResearch ELEMENTALSHOVEL). */
class ElementalShovelAssetsTest {
    private JsonObject json(String path) throws Exception {
        var stream = getClass().getClassLoader().getResourceAsStream(path);
        assertNotNull(stream, "missing resource " + path);
        try (var reader = new InputStreamReader(stream, StandardCharsets.UTF_8)) {
            return JsonParser.parseReader(reader).getAsJsonObject();
        }
    }

    @Test void theItemHasItsIconModelTagAndNames() throws Exception {
        var model = json("assets/thaumcraft/models/item/elemental_shovel.json");
        assertEquals("minecraft:item/handheld", model.get("parent").getAsString());
        assertEquals("thaumcraft:items/elementalshovel", model.getAsJsonObject("textures").get("layer0").getAsString());
        assertTrue(getClass().getClassLoader().getResource("assets/thaumcraft/textures/items/elementalshovel.png") != null, "the original icon");
        assertTrue(json("data/minecraft/tags/item/shovels.json").getAsJsonArray("values").asList().stream()
                .anyMatch(value -> value.getAsString().equals("thaumcraft:elemental_shovel")), "it is a shovel");
        assertEquals("Shovel of the Earthmover", json("assets/thaumcraft/lang/en_us.json").get("item.thaumcraft.elemental_shovel").getAsString());
        assertEquals("Лопата Землероя", json("assets/thaumcraft/lang/ru_ru.json").get("item.thaumcraft.elemental_shovel").getAsString());
    }

    @Test void theKeyAndTheOrientationTooltipsAreNamedInBothLanguages() throws Exception {
        for (String lang : List.of("en_us", "ru_ru")) {
            var names = json("assets/thaumcraft/lang/" + lang + ".json");
            for (String key : List.of("key.thaumcraft.misc_toggle", "tc.port.shovel.orientation.0", "tc.port.shovel.orientation.1", "tc.port.shovel.orientation.2")) {
                assertTrue(names.has(key) && !names.get(key).getAsString().isBlank(), lang + " names " + key);
            }
        }
        assertEquals("Misc Wand Toggle", json("assets/thaumcraft/lang/en_us.json").get("key.thaumcraft.misc_toggle").getAsString(), "the original key's name");
    }

    @Test void theInfusionKeepsTheOriginalRecipe() throws Exception {
        var recipe = json("data/thaumcraft/recipe/infusion/elemental_shovel.json");
        assertEquals("thaumcraft:infusion_stack", recipe.get("type").getAsString());
        assertEquals("ELEMENTALSHOVEL", recipe.get("research").getAsString());
        assertEquals("thaumcraft:thaumium_shovel", recipe.getAsJsonObject("input").get("item").getAsString());
        assertEquals(List.of("thaumcraft:earth_shard", "thaumcraft:earth_shard", "minecraft:diamond", "thaumcraft:greatwood_log"),
                recipe.getAsJsonArray("components").asList().stream().map(c -> c.getAsJsonObject().get("item").getAsString()).toList());
        assertEquals(JsonParser.parseString("{\"terra\":16,\"fabrico\":8}"), recipe.get("aspects"));
        assertEquals(1, recipe.get("instability").getAsInt());
        assertEquals("thaumcraft:elemental_shovel", recipe.getAsJsonObject("result").get("id").getAsString());
    }
}
