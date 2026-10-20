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

/** Resource wiring of the Sword of the Zephyr (original ItemElementalSword, ConfigRecipes:347, ConfigResearch ELEMENTALSWORD). */
class ElementalSwordAssetsTest {
    private JsonObject json(String path) throws Exception {
        var stream = getClass().getClassLoader().getResourceAsStream(path);
        assertNotNull(stream, "missing resource " + path);
        try (var reader = new InputStreamReader(stream, StandardCharsets.UTF_8)) {
            return JsonParser.parseReader(reader).getAsJsonObject();
        }
    }

    @Test void theItemHasItsIconModelTagAndNames() throws Exception {
        var model = json("assets/thaumcraft/models/item/elemental_sword.json");
        assertEquals("minecraft:item/handheld", model.get("parent").getAsString());
        assertEquals("thaumcraft:items/elementalsword", model.getAsJsonObject("textures").get("layer0").getAsString());
        assertTrue(getClass().getClassLoader().getResource("assets/thaumcraft/textures/items/elementalsword.png") != null, "the original icon");
        assertTrue(json("data/minecraft/tags/item/swords.json").getAsJsonArray("values").asList().stream()
                .anyMatch(value -> value.getAsString().equals("thaumcraft:elemental_sword")), "it is a sword");
        assertEquals("Sword of the Zephyr", json("assets/thaumcraft/lang/en_us.json").get("item.thaumcraft.elemental_sword").getAsString());
        assertEquals("Меч Бури", json("assets/thaumcraft/lang/ru_ru.json").get("item.thaumcraft.elemental_sword").getAsString());
    }

    @Test void theInfusionKeepsTheOriginalRecipe() throws Exception {
        var recipe = json("data/thaumcraft/recipe/infusion/elemental_sword.json");
        assertEquals("thaumcraft:infusion_stack", recipe.get("type").getAsString());
        assertEquals("ELEMENTALSWORD", recipe.get("research").getAsString());
        assertEquals("thaumcraft:thaumium_sword", recipe.getAsJsonObject("input").get("item").getAsString());
        assertEquals(List.of("thaumcraft:air_shard", "thaumcraft:air_shard", "minecraft:diamond", "thaumcraft:greatwood_log"),
                recipe.getAsJsonArray("components").asList().stream().map(c -> c.getAsJsonObject().get("item").getAsString()).toList());
        assertEquals(JsonParser.parseString("{\"aer\":8,\"motus\":8,\"potentia\":8}"), recipe.get("aspects"));
        assertEquals(1, recipe.get("instability").getAsInt());
        assertEquals("thaumcraft:elemental_sword", recipe.getAsJsonObject("result").get("id").getAsString());
    }

    @Test void theTwoHooksAndTheirAccessorsAreRegistered() throws Exception {
        var mixins = json("thaumcraft-common.mixins.json").getAsJsonArray("mixins").asList().stream().map(m -> m.getAsString()).toList();
        for (String name : List.of("PlayerAttackSweepMixin", "LivingEntityAttackStrengthAccess", "ServerGamePacketListenerFloatAccess")) {
            assertTrue(mixins.contains(name), name + " is registered");
        }
    }
}
