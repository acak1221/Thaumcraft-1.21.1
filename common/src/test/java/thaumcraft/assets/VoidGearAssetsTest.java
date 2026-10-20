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

/** Resource wiring of the nine Void metal tools and armor (original ConfigRecipes:449-457, ConfigItems:489-506). */
class VoidGearAssetsTest {
    private record Gear(String id, List<String> pattern, boolean sticks, String icon, String parent, String tag) {}

    // Original oreDictRecipe patterns: I = ingotVoid, S = stickWood.
    private static final List<Gear> GEAR = List.of(
            new Gear("void_helmet", List.of("III", "I I"), false, "voidhelm", "generated", "head_armor"),
            new Gear("void_chestplate", List.of("I I", "III", "III"), false, "voidchest", "generated", "chest_armor"),
            new Gear("void_leggings", List.of("III", "I I", "I I"), false, "voidlegs", "generated", "leg_armor"),
            new Gear("void_boots", List.of("I I", "I I"), false, "voidboots", "generated", "foot_armor"),
            new Gear("void_shovel", List.of("I", "S", "S"), true, "voidshovel", "handheld", "shovels"),
            new Gear("void_pickaxe", List.of("III", " S ", " S "), true, "voidpick", "handheld", "pickaxes"),
            new Gear("void_axe", List.of("II", "SI", "S "), true, "voidaxe", "handheld", "axes"),
            new Gear("void_hoe", List.of("II", "S ", "S "), true, "voidhoe", "handheld", "hoes"),
            new Gear("void_sword", List.of("I", "I", "S"), true, "voidsword", "handheld", "swords"));

    private JsonObject json(String path) throws Exception {
        var stream = getClass().getClassLoader().getResourceAsStream(path);
        assertNotNull(stream, "missing resource " + path);
        try (var reader = new InputStreamReader(stream, StandardCharsets.UTF_8)) {
            return JsonParser.parseReader(reader).getAsJsonObject();
        }
    }

    private boolean exists(String path) { return getClass().getClassLoader().getResource(path) != null; }

    @Test void everyPieceHasItsOriginalRecipe() throws Exception {
        for (Gear gear : GEAR) {
            var recipe = json("data/thaumcraft/recipe/" + gear.id() + ".json");
            assertEquals("minecraft:crafting_shaped", recipe.get("type").getAsString(), gear.id());
            var pattern = recipe.getAsJsonArray("pattern");
            assertEquals(gear.pattern().size(), pattern.size(), gear.id());
            for (int row = 0; row < pattern.size(); row++) assertEquals(gear.pattern().get(row), pattern.get(row).getAsString(), gear.id() + " row " + row);
            var key = recipe.getAsJsonObject("key");
            assertEquals(gear.sticks() ? 2 : 1, key.size(), gear.id());
            assertEquals("c:ingots/void", key.getAsJsonObject("I").get("tag").getAsString(), gear.id());
            if (gear.sticks()) assertEquals("c:rods/wooden", key.getAsJsonObject("S").get("tag").getAsString(), gear.id());
            assertEquals("thaumcraft:" + gear.id(), recipe.getAsJsonObject("result").get("id").getAsString(), gear.id());
            assertEquals(1, recipe.getAsJsonObject("result").get("count").getAsInt(), gear.id());
        }
        assertEquals(List.of("thaumcraft:void_ingot"), json("data/c/tags/item/ingots/void.json").getAsJsonArray("values").asList().stream().map(v -> v.getAsString()).toList());
    }

    @Test void everyPieceHasItsIconModelTagAndNames() throws Exception {
        for (Gear gear : GEAR) {
            var model = json("assets/thaumcraft/models/item/" + gear.id() + ".json");
            assertEquals("minecraft:item/" + gear.parent(), model.get("parent").getAsString(), gear.id());
            assertEquals("thaumcraft:items/" + gear.icon(), model.getAsJsonObject("textures").get("layer0").getAsString(), gear.id());
            assertTrue(exists("assets/thaumcraft/textures/items/" + gear.icon() + ".png"), gear.id() + " icon");
            var tag = json("data/minecraft/tags/item/" + gear.tag() + ".json");
            assertTrue(tag.getAsJsonArray("values").asList().stream().anyMatch(v -> v.getAsString().equals("thaumcraft:" + gear.id())), gear.id() + " is in #" + gear.tag());
            for (String lang : List.of("en_us", "ru_ru")) {
                assertTrue(json("assets/thaumcraft/lang/" + lang + ".json").has("item.thaumcraft." + gear.id()), lang + " names " + gear.id());
            }
        }
        assertTrue(exists("assets/thaumcraft/textures/models/armor/void_layer_1.png") && exists("assets/thaumcraft/textures/models/armor/void_layer_2.png"), "the armor layers");
    }

    @Test void theSwordCarriesTheOriginalSaplessTooltipLine() throws Exception {
        for (String lang : List.of("en_us", "ru_ru")) assertTrue(json("assets/thaumcraft/lang/" + lang + ".json").has("enchantment.special.sapless"), lang);
    }
}
