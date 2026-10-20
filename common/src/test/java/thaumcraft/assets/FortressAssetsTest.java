package thaumcraft.assets;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.List;
import org.junit.jupiter.api.Test;

/** Resource wiring of the Thaumium Fortress armor (original ItemFortressArmor, ConfigRecipes:360-370, ConfigResearch ARMORFORTRESS). */
class FortressAssetsTest {
    private record Piece(String id, String tag, String icon, String name, String russian) {}

    private static final List<Piece> PIECES = List.of(
            new Piece("fortress_helmet", "head_armor", "thaumiumfortresshelm", "Thaumium Fortress Helm", "Боевой таум-шлем"),
            new Piece("fortress_chestplate", "chest_armor", "thaumiumfortresschest", "Thaumium Fortress Cuirass", "Боевая таум-кираса"),
            new Piece("fortress_leggings", "leg_armor", "thaumiumfortresslegs", "Thaumium Fortress Thigh Guards", "Боевые таум-поножи"));

    private JsonObject json(String path) throws Exception {
        var stream = getClass().getClassLoader().getResourceAsStream(path);
        assertNotNull(stream, "missing resource " + path);
        try (var reader = new InputStreamReader(stream, StandardCharsets.UTF_8)) {
            return JsonParser.parseReader(reader).getAsJsonObject();
        }
    }

    private static List<String> items(com.google.gson.JsonArray array) {
        return array.asList().stream().map(c -> c.getAsJsonObject().get("item").getAsString()).toList();
    }

    @Test void everyPieceHasItsIconModelTagAndNames() throws Exception {
        for (Piece piece : PIECES) {
            var model = json("assets/thaumcraft/models/item/" + piece.id() + ".json");
            assertEquals("thaumcraft:items/" + piece.icon(), model.getAsJsonObject("textures").get("layer0").getAsString(), piece.id());
            assertTrue(getClass().getClassLoader().getResource("assets/thaumcraft/textures/items/" + piece.icon() + ".png") != null, piece.icon());
            assertTrue(json("data/minecraft/tags/item/" + piece.tag() + ".json").getAsJsonArray("values").asList().stream()
                    .anyMatch(value -> value.getAsString().equals("thaumcraft:" + piece.id())), piece.id() + " is in #" + piece.tag());
            assertEquals(piece.name(), json("assets/thaumcraft/lang/en_us.json").get("item.thaumcraft." + piece.id()).getAsString());
            assertEquals(piece.russian(), json("assets/thaumcraft/lang/ru_ru.json").get("item.thaumcraft." + piece.id()).getAsString());
        }
        for (String lang : List.of("en_us", "ru_ru")) for (int mask = 0; mask < 3; mask++) {
            assertTrue(json("assets/thaumcraft/lang/" + lang + ".json").has("item.HelmetFortress.mask." + mask), lang + " names mask " + mask);
        }
    }

    private void armor(String file, String research, String centre, List<String> components, String aspects, int instability, String result) throws Exception {
        var recipe = json("data/thaumcraft/recipe/infusion/" + file + ".json");
        assertEquals("thaumcraft:infusion_stack", recipe.get("type").getAsString());
        assertEquals(research, recipe.get("research").getAsString());
        assertEquals(centre, recipe.getAsJsonObject("input").get("item").getAsString());
        assertEquals(0, recipe.get("input_damage").getAsInt(), "the original centre is the undamaged stack: " + file);
        assertEquals(components, items(recipe.getAsJsonArray("components")));
        assertEquals(JsonParser.parseString(aspects), recipe.get("aspects"));
        assertEquals(instability, recipe.get("instability").getAsInt());
        assertEquals(result, recipe.getAsJsonObject("result").get("id").getAsString());
        assertFalse(recipe.has("result_patch"), file);
    }

    private void patch(String file, String research, List<String> components, String aspects, int instability, String patch) throws Exception {
        var recipe = json("data/thaumcraft/recipe/infusion/" + file + ".json");
        assertEquals(research, recipe.get("research").getAsString());
        assertEquals("thaumcraft:fortress_helmet", recipe.getAsJsonObject("input").get("item").getAsString());
        assertFalse(recipe.has("input_damage"), "any wear of the helm will do (the original's wildcard): " + file);
        assertEquals(components, items(recipe.getAsJsonArray("components")));
        assertEquals(JsonParser.parseString(aspects), recipe.get("aspects"));
        assertEquals(instability, recipe.get("instability").getAsInt());
        assertEquals(patch, recipe.get("result_patch").getAsString());
        assertEquals("thaumcraft:fortress_helmet", recipe.getAsJsonObject("result").get("id").getAsString());
    }

    @Test void theThreeArmorInfusionsKeepTheOriginalRecipes() throws Exception {
        String ti = "thaumcraft:thaumium_ingot", au = "minecraft:gold_ingot", le = "minecraft:leather";
        armor("fortress_helm", "ARMORFORTRESS", "thaumcraft:thaumium_helmet", List.of(ti, ti, au, au, "minecraft:emerald"),
                "{\"metallum\":24,\"tutamen\":16,\"praecantatio\":16}", 3, "thaumcraft:fortress_helmet");
        armor("fortress_chest", "ARMORFORTRESS", "thaumcraft:thaumium_chestplate", List.of(ti, ti, ti, ti, au, le),
                "{\"metallum\":24,\"tutamen\":24,\"praecantatio\":16}", 3, "thaumcraft:fortress_chestplate");
        armor("fortress_legs", "ARMORFORTRESS", "thaumcraft:thaumium_leggings", List.of(ti, ti, ti, au, le),
                "{\"metallum\":24,\"tutamen\":20,\"praecantatio\":16}", 3, "thaumcraft:fortress_leggings");
    }

    @Test void theGogglesAndMaskUpgradesAreInfusionsOfTheHelmItself() throws Exception {
        String fe = "minecraft:iron_ingot", le = "minecraft:leather";
        patch("fortress_goggles", "HELMGOGGLES", List.of("minecraft:slime_ball", "thaumcraft:goggles_revealing"),
                "{\"sensus\":32,\"auram\":16,\"tutamen\":16}", 5, "fortress_goggles");
        patch("fortress_mask_devil", "MASKGRINNINGDEVIL", List.of("minecraft:ink_sac", fe, le, "thaumcraft:shimmerleaf", "thaumcraft:zombie_brain", fe),
                "{\"cognitio\":64,\"sano\":64,\"tutamen\":16}", 8, "fortress_mask_0");
        patch("fortress_mask_ghost", "MASKANGRYGHOST", List.of("minecraft:bone_meal", fe, le, "minecraft:poisonous_potato", "minecraft:wither_skeleton_skull", fe),
                "{\"perditio\":64,\"mortuus\":64,\"tutamen\":16}", 8, "fortress_mask_1");
        patch("fortress_mask_fiend", "MASKSIPPINGFIEND", List.of("minecraft:red_dye", fe, le, "minecraft:ghast_tear", "minecraft:milk_bucket", fe),
                "{\"exanimis\":64,\"victus\":64,\"tutamen\":16}", 8, "fortress_mask_2");
    }

    @Test void theWardHookCarriesTheMasksAndTheArmorValue() throws Exception {
        assertTrue(json("thaumcraft-common.mixins.json").getAsJsonArray("mixins").asList().stream().anyMatch(m -> m.getAsString().equals("LivingEntityMagicWardMixin")));
    }
}
