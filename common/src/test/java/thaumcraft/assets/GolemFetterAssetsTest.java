package thaumcraft.assets;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;

/** Resource wiring of the Golem Fetter (original cosmetic-solid metadata 9 and 10). */
class GolemFetterAssetsTest {
    private JsonObject json(String path) throws Exception {
        var stream = getClass().getClassLoader().getResourceAsStream(path);
        assertNotNull(stream, "missing resource " + path);
        try (var reader = new InputStreamReader(stream, StandardCharsets.UTF_8)) {
            return JsonParser.parseReader(reader).getAsJsonObject();
        }
    }

    private boolean exists(String path) { return getClass().getClassLoader().getResource(path) != null; }

    @Test void bothStatesHaveAModelWithTheOriginalTextures() throws Exception {
        var variants = json("assets/thaumcraft/blockstates/golem_fetter.json").getAsJsonObject("variants");
        assertEquals("thaumcraft:block/golem_fetter", variants.getAsJsonObject("active=false").get("model").getAsString());
        assertEquals("thaumcraft:block/golem_fetter_active", variants.getAsJsonObject("active=true").get("model").getAsString());
        assertEquals(2, variants.size());
        // BlockCosmeticSolid:105-106: bottom icon[13] arcane_stone, top icon[14] / icon[16] (active), sides icon[15].
        var idle = json("assets/thaumcraft/models/block/golem_fetter.json").getAsJsonObject("textures");
        var live = json("assets/thaumcraft/models/block/golem_fetter_active.json").getAsJsonObject("textures");
        assertEquals("thaumcraft:blocks/golem_stone_top", idle.get("top").getAsString());
        assertEquals("thaumcraft:blocks/golem_stone_top_active", live.get("top").getAsString());
        for (var textures : new JsonObject[] {idle, live}) {
            assertEquals("thaumcraft:blocks/golem_stone_side", textures.get("side").getAsString());
            assertEquals("thaumcraft:blocks/arcane_stone", textures.get("bottom").getAsString());
            for (String key : new String[] {"top", "side", "bottom"}) {
                assertTrue(exists("assets/thaumcraft/textures/" + textures.get(key).getAsString().substring("thaumcraft:".length()) + ".png"), "texture of " + key);
            }
        }
        assertEquals("thaumcraft:block/golem_fetter", json("assets/thaumcraft/models/item/golem_fetter.json").get("parent").getAsString());
    }

    @Test void breakingEitherStateDropsTheInactiveItem() throws Exception {
        var table = json("data/thaumcraft/loot_table/blocks/golem_fetter.json");
        var entry = table.getAsJsonArray("pools").get(0).getAsJsonObject().getAsJsonArray("entries").get(0).getAsJsonObject();
        assertEquals("thaumcraft:golem_fetter", entry.get("name").getAsString());
        assertTrue(!entry.has("conditions") && !entry.has("functions"), "original damageDropped(10) = 9 with quantity 1, whatever the state");
    }

    @Test void theRecipeAndNamesExist() throws Exception {
        var recipe = json("data/thaumcraft/recipe/arcane/golem_fetter.json");
        assertEquals("GOLEMFETTER", recipe.get("research").getAsString());
        assertEquals("SSS", recipe.getAsJsonArray("pattern").get(0).getAsString());
        assertEquals("IRI", recipe.getAsJsonArray("pattern").get(1).getAsString());
        assertEquals("BBB", recipe.getAsJsonArray("pattern").get(2).getAsString());
        assertEquals(5, recipe.getAsJsonObject("vis").get("terra").getAsInt());
        assertEquals(5, recipe.getAsJsonObject("vis").get("ordo").getAsInt());
        for (String lang : new String[] {"en_us", "ru_ru"}) {
            assertTrue(json("assets/thaumcraft/lang/" + lang + ".json").has("block.thaumcraft.golem_fetter"), lang + " names the block");
        }
        assertTrue(json("data/minecraft/tags/block/mineable/pickaxe.json").getAsJsonArray("values").toString().contains("thaumcraft:golem_fetter"));
    }
}
