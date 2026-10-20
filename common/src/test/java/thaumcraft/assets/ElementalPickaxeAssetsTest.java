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

/** Resource wiring of the Pickaxe of the Core (original ItemElementalPickaxe, ConfigRecipes:346, ConfigResearch ELEMENTALPICK). */
class ElementalPickaxeAssetsTest {
    private JsonObject json(String path) throws Exception {
        var stream = getClass().getClassLoader().getResourceAsStream(path);
        assertNotNull(stream, "missing resource " + path);
        try (var reader = new InputStreamReader(stream, StandardCharsets.UTF_8)) {
            return JsonParser.parseReader(reader).getAsJsonObject();
        }
    }

    @Test void theItemHasItsIconModelTagAndNames() throws Exception {
        var model = json("assets/thaumcraft/models/item/elemental_pickaxe.json");
        assertEquals("minecraft:item/handheld", model.get("parent").getAsString());
        assertEquals("thaumcraft:items/elementalpick", model.getAsJsonObject("textures").get("layer0").getAsString());
        assertTrue(getClass().getClassLoader().getResource("assets/thaumcraft/textures/items/elementalpick.png") != null, "the original icon");
        assertTrue(json("data/minecraft/tags/item/pickaxes.json").getAsJsonArray("values").asList().stream()
                .anyMatch(value -> value.getAsString().equals("thaumcraft:elemental_pickaxe")), "it is a pickaxe");
        assertEquals("Pickaxe of the Core", json("assets/thaumcraft/lang/en_us.json").get("item.thaumcraft.elemental_pickaxe").getAsString());
        assertEquals("Кирка Огня", json("assets/thaumcraft/lang/ru_ru.json").get("item.thaumcraft.elemental_pickaxe").getAsString());
    }

    @Test void theInfusionKeepsTheOriginalRecipe() throws Exception {
        var recipe = json("data/thaumcraft/recipe/infusion/elemental_pickaxe.json");
        assertEquals("thaumcraft:infusion_stack", recipe.get("type").getAsString());
        assertEquals("ELEMENTALPICK", recipe.get("research").getAsString());
        assertEquals("thaumcraft:thaumium_pickaxe", recipe.getAsJsonObject("input").get("item").getAsString());
        assertEquals(List.of("thaumcraft:fire_shard", "thaumcraft:fire_shard", "minecraft:diamond", "thaumcraft:greatwood_log"),
                recipe.getAsJsonArray("components").asList().stream().map(c -> c.getAsJsonObject().get("item").getAsString()).toList());
        assertEquals(JsonParser.parseString("{\"ignis\":8,\"perfodio\":8,\"sensus\":8}"), recipe.get("aspects"));
        assertEquals(1, recipe.get("instability").getAsInt());
        assertEquals("thaumcraft:elemental_pickaxe", recipe.getAsJsonObject("result").get("id").getAsString());
    }

    @Test void theBlockDropHookIsRegistered() throws Exception {
        var mixins = json("thaumcraft-common.mixins.json").getAsJsonArray("mixins").asList().stream().map(m -> m.getAsString()).toList();
        assertTrue(mixins.contains("SpecialMiningBlockMixin"), "without it no cluster ever drops");
    }
}
