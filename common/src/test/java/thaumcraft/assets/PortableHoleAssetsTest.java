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

/** Resource wiring of the Portable Hole (original ItemFocusPortableHole, BlockHole, ConfigRecipes:320, ConfigResearch FOCUSPORTABLEHOLE). */
class PortableHoleAssetsTest {
    private JsonObject json(String path) throws Exception {
        var stream = getClass().getClassLoader().getResourceAsStream(path);
        assertNotNull(stream, "missing resource " + path);
        try (var reader = new InputStreamReader(stream, StandardCharsets.UTF_8)) {
            return JsonParser.parseReader(reader).getAsJsonObject();
        }
    }

    @Test void theFocusHasItsIconModelAndNames() throws Exception {
        var model = json("assets/thaumcraft/models/item/focus_portable_hole.json");
        assertEquals("thaumcraft:items/focus_portablehole", model.getAsJsonObject("textures").get("layer0").getAsString());
        assertTrue(getClass().getClassLoader().getResource("assets/thaumcraft/textures/items/focus_portablehole.png") != null, "the original icon");
        assertEquals("Wand Focus: Portable Hole", json("assets/thaumcraft/lang/en_us.json").get("item.thaumcraft.focus_portable_hole").getAsString());
        assertEquals("Набалдашник: Червоточина", json("assets/thaumcraft/lang/ru_ru.json").get("item.thaumcraft.focus_portable_hole").getAsString());
    }

    @Test void theInfusionKeepsTheOriginalRecipe() throws Exception {
        var recipe = json("data/thaumcraft/recipe/infusion/focus_portable_hole.json");
        assertEquals("thaumcraft:infusion_stack", recipe.get("type").getAsString());
        assertEquals("FOCUSPORTABLEHOLE", recipe.get("research").getAsString());
        assertEquals("minecraft:ender_pearl", recipe.getAsJsonObject("input").get("item").getAsString());
        assertEquals(List.of("minecraft:quartz", "thaumcraft:earth_shard", "minecraft:quartz", "thaumcraft:air_shard", "minecraft:quartz", "thaumcraft:entropy_shard"),
                recipe.getAsJsonArray("components").asList().stream().map(c -> c.getAsJsonObject().get("item").getAsString()).toList());
        assertEquals(JsonParser.parseString("{\"iter\":25,\"alienis\":10,\"permutatio\":10,\"perditio\":25}"), recipe.get("aspects"));
        assertEquals(3, recipe.get("instability").getAsInt());
        assertEquals("thaumcraft:focus_portable_hole", recipe.getAsJsonObject("result").get("id").getAsString());
    }

    @Test void theBlacklistTagKeepsTheOriginalBlocks() throws Exception {
        var values = json("data/thaumcraft/tags/block/portable_hole_blacklist.json").getAsJsonArray("values").asList().stream().map(v -> v.getAsString()).toList();
        assertEquals(List.of("#minecraft:beds", "#minecraft:doors", "minecraft:piston", "minecraft:sticky_piston", "minecraft:piston_head", "minecraft:moving_piston",
                "thaumcraft:flux_goo", "thaumcraft:flux_gas"), values);
    }

    @Test void theHoleBlockIsInvisibleAndHasNoItem() throws Exception {
        var state = json("assets/thaumcraft/blockstates/hole.json");
        assertEquals("thaumcraft:block/hole", state.getAsJsonObject("variants").getAsJsonObject("").get("model").getAsString());
        var model = json("assets/thaumcraft/models/block/hole.json");
        assertEquals(0, model.getAsJsonArray("elements").size());
        assertTrue(getClass().getClassLoader().getResource("assets/thaumcraft/models/item/hole.json") == null, "no item model: the hole has no item");
    }
}
