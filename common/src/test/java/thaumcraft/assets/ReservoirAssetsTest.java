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

/** Resource wiring of the Essentia Reservoir (original ConfigBlocks, ConfigRecipes:372, ConfigResearch ESSENTIARESERVOIR). */
class ReservoirAssetsTest {
    private JsonObject json(String path) throws Exception {
        var stream = getClass().getClassLoader().getResourceAsStream(path);
        assertNotNull(stream, "missing resource " + path);
        try (var reader = new InputStreamReader(stream, StandardCharsets.UTF_8)) {
            return JsonParser.parseReader(reader).getAsJsonObject();
        }
    }

    @Test void theBlockHasItsStateModelsItemTextureAndNames() throws Exception {
        assertEquals("thaumcraft:block/essentia_reservoir", json("assets/thaumcraft/blockstates/essentia_reservoir.json").getAsJsonObject("variants").getAsJsonObject("").get("model").getAsString());
        assertEquals("thaumcraft:blocks/essentiareservoir", json("assets/thaumcraft/models/block/essentia_reservoir.json").getAsJsonObject("textures").get("all").getAsString());
        assertTrue(getClass().getClassLoader().getResource("assets/thaumcraft/textures/blocks/essentiareservoir.png") != null);
        assertEquals("thaumcraft:block/essentia_reservoir", json("assets/thaumcraft/models/item/essentia_reservoir.json").get("parent").getAsString());
        assertEquals("Essentia Reservoir", json("assets/thaumcraft/lang/en_us.json").get("block.thaumcraft.essentia_reservoir").getAsString());
        assertEquals("Резервуар эссенции", json("assets/thaumcraft/lang/ru_ru.json").get("block.thaumcraft.essentia_reservoir").getAsString());
    }

    @Test void itDropsItselfAndIsMinedWithAPickaxe() throws Exception {
        var loot = json("data/thaumcraft/loot_table/blocks/essentia_reservoir.json");
        assertEquals("thaumcraft:essentia_reservoir", loot.getAsJsonArray("pools").get(0).getAsJsonObject().getAsJsonArray("entries").get(0).getAsJsonObject().get("name").getAsString());
        assertTrue(json("data/minecraft/tags/block/mineable/pickaxe.json").getAsJsonArray("values").asList().stream().anyMatch(v -> v.getAsString().equals("thaumcraft:essentia_reservoir")));
    }

    @Test void theInfusionKeepsTheOriginalCentreComponentsAspectsAndInstability() throws Exception {
        var recipe = json("data/thaumcraft/recipe/infusion/essentia_reservoir.json");
        assertEquals("thaumcraft:infusion_stack", recipe.get("type").getAsString());
        assertEquals("ESSENTIARESERVOIR", recipe.get("research").getAsString());
        assertEquals("thaumcraft:essentia_buffer", recipe.getAsJsonObject("input").get("item").getAsString());
        String ingot = "thaumcraft:void_ingot", jar = "thaumcraft:jar";
        assertEquals(List.of(ingot, jar, jar, ingot, jar, jar),
                recipe.getAsJsonArray("components").asList().stream().map(c -> c.getAsJsonObject().get("item").getAsString()).toList());
        assertEquals(JsonParser.parseString("{\"aqua\":8,\"vacuos\":8,\"praecantatio\":8,\"permutatio\":8}"), recipe.get("aspects"));
        assertEquals(6, recipe.get("instability").getAsInt());
        assertEquals("thaumcraft:essentia_reservoir", recipe.getAsJsonObject("result").get("id").getAsString());
    }
}
