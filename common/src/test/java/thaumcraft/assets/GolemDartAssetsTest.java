package thaumcraft.assets;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;

/** Resource wiring of the golem dart launcher (original ItemGolemDecoration damage 4, ConfigRecipes:256). */
class GolemDartAssetsTest {
    private JsonObject json(String path) throws Exception {
        var stream = getClass().getClassLoader().getResourceAsStream(path);
        assertNotNull(stream, "missing resource " + path);
        try (var reader = new InputStreamReader(stream, StandardCharsets.UTF_8)) {
            return JsonParser.parseReader(reader).getAsJsonObject();
        }
    }

    @Test void theArcaneRecipeIsArrowsAndIronAroundADispenser() throws Exception {
        var recipe = json("data/thaumcraft/recipe/arcane/golem_decoration_dart.json");
        assertEquals("thaumcraft:arcane_shaped", recipe.get("type").getAsString());
        assertEquals("TINYDART", recipe.get("research").getAsString());
        var pattern = recipe.getAsJsonArray("pattern");
        assertEquals(3, pattern.size());
        assertEquals("AIA", pattern.get(0).getAsString());
        assertEquals("ADA", pattern.get(1).getAsString());
        assertEquals("AIA", pattern.get(2).getAsString());
        var key = recipe.getAsJsonObject("key");
        assertEquals(3, key.size());
        assertEquals("minecraft:arrow", key.getAsJsonObject("A").get("item").getAsString());
        assertEquals("minecraft:iron_ingot", key.getAsJsonObject("I").get("item").getAsString());
        assertEquals("minecraft:dispenser", key.getAsJsonObject("D").get("item").getAsString());
        assertEquals("thaumcraft:golem_decoration_dart", recipe.getAsJsonObject("result").get("id").getAsString());
        assertEquals(1, recipe.getAsJsonObject("result").get("count").getAsInt());
        var vis = recipe.getAsJsonObject("vis");
        assertEquals(2, vis.size());
        assertEquals(4, vis.get("aer").getAsInt());
        assertEquals(4, vis.get("ignis").getAsInt());
    }

    @Test void theItemUsesTheOriginalIcon() throws Exception {
        var model = json("assets/thaumcraft/models/item/golem_decoration_dart.json");
        assertEquals("minecraft:item/generated", model.get("parent").getAsString());
        assertEquals("thaumcraft:items/golemdecodart", model.getAsJsonObject("textures").get("layer0").getAsString());
        assertNotNull(getClass().getClassLoader().getResource("assets/thaumcraft/textures/items/golemdecodart.png"), "the original icon");
    }

    @Test void theItemTheEntityAndTheUpgradeNotesAreNamed() throws Exception {
        for (String lang : new String[] {"en_us", "ru_ru"}) {
            var names = json("assets/thaumcraft/lang/" + lang + ".json");
            assertTrue(names.has("item.thaumcraft.golem_decoration_dart"), lang + " names the launcher");
            assertTrue(names.has("entity.thaumcraft.dart"), lang + " names the dart");
            // The Air and Water notes no longer say the launcher is missing.
            assertTrue(names.get("tooltip.thaumcraft.golem.upgrade_air_port").getAsString().length() > 0
                    && !names.get("tooltip.thaumcraft.golem.upgrade_air_port").getAsString().contains("Пока нет")
                    && !names.get("tooltip.thaumcraft.golem.upgrade_air_port").getAsString().contains("Not yet"), lang + " Air note");
            assertTrue(!names.get("tooltip.thaumcraft.golem.upgrade_water_port").getAsString().contains("Пока нет")
                    && !names.get("tooltip.thaumcraft.golem.upgrade_water_port").getAsString().contains("Not yet"), lang + " Water note");
        }
    }
}
