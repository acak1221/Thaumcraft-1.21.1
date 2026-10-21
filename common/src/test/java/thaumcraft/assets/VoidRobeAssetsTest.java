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

/** Resource wiring of the Void Thaumaturge robes (original ItemVoidRobeArmor, ConfigRecipes:363-366, ConfigResearch ARMORVOIDFORTRESS). */
class VoidRobeAssetsTest {
    private record Piece(String id, String recipe, String tag, String name, String russian, List<String> icons) {}

    private static final List<Piece> PIECES = List.of(
            new Piece("void_robe_helmet", "void_robe_helm", "head_armor", "Void Thaumaturge Hood", "Пустотный капюшон тауматурга", List.of("voidrobehelm")),
            new Piece("void_robe_chestplate", "void_robe_chest", "chest_armor", "Void Thaumaturge Robe", "Пустотная мантия тауматурга", List.of("voidrobechestover", "voidrobechest")),
            new Piece("void_robe_leggings", "void_robe_legs", "leg_armor", "Void Thaumaturge Leggings", "Пустотные шаровары тауматурга", List.of("voidrobelegsover", "voidrobelegs")));

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

    @Test void everyPieceHasItsIconsModelTagAndNames() throws Exception {
        for (Piece piece : PIECES) {
            var model = json("assets/thaumcraft/models/item/" + piece.id() + ".json");
            assertEquals("minecraft:item/generated", model.get("parent").getAsString(), piece.id());
            var textures = model.getAsJsonObject("textures");
            assertEquals(piece.icons().size(), textures.size(), piece.id());
            for (int layer = 0; layer < piece.icons().size(); layer++) {
                assertEquals("thaumcraft:items/" + piece.icons().get(layer), textures.get("layer" + layer).getAsString(), piece.id() + " layer " + layer);
                assertTrue(getClass().getClassLoader().getResource("assets/thaumcraft/textures/items/" + piece.icons().get(layer) + ".png") != null, piece.icons().get(layer));
            }
            assertTrue(json("data/minecraft/tags/item/" + piece.tag() + ".json").getAsJsonArray("values").asList().stream()
                    .anyMatch(value -> value.getAsString().equals("thaumcraft:" + piece.id())), piece.id() + " is in #" + piece.tag());
            assertEquals(piece.name(), json("assets/thaumcraft/lang/en_us.json").get("item.thaumcraft." + piece.id()).getAsString());
            assertEquals(piece.russian(), json("assets/thaumcraft/lang/ru_ru.json").get("item.thaumcraft." + piece.id()).getAsString());
        }
    }

    @Test void theThreeInfusionsKeepTheOriginalRecipes() throws Exception {
        String fabric = "thaumcraft:enchanted_fabric", salis = "thaumcraft:salis_mundus";
        record Expected(String centre, List<String> components, String aspects, String goal) {}
        var expected = List.of(
                new Expected("thaumcraft:void_helmet", List.of("thaumcraft:goggles_revealing", fabric, fabric, salis, fabric, fabric),
                        "{\"metallum\":16,\"sensus\":16,\"tutamen\":16,\"pannus\":16,\"praecantatio\":16,\"alienis\":16,\"vacuos\":16}", "thaumcraft:void_robe_helmet"),
                new Expected("thaumcraft:void_chestplate", List.of("thaumcraft:robe_chestplate", "thaumcraft:void_ingot", "thaumcraft:thaumium_ingot", salis, fabric, "minecraft:leather"),
                        "{\"metallum\":24,\"tutamen\":24,\"pannus\":24,\"praecantatio\":16,\"alienis\":16,\"vacuos\":24}", "thaumcraft:void_robe_chestplate"),
                new Expected("thaumcraft:void_leggings", List.of("thaumcraft:robe_leggings", "thaumcraft:void_ingot", "thaumcraft:thaumium_ingot", salis, fabric, "minecraft:leather"),
                        "{\"metallum\":20,\"tutamen\":20,\"pannus\":20,\"praecantatio\":16,\"alienis\":16,\"vacuos\":20}", "thaumcraft:void_robe_leggings"));
        for (int index = 0; index < PIECES.size(); index++) {
            var recipe = json("data/thaumcraft/recipe/infusion/" + PIECES.get(index).recipe() + ".json");
            var want = expected.get(index);
            assertEquals("thaumcraft:infusion_stack", recipe.get("type").getAsString());
            assertEquals("ARMORVOIDFORTRESS", recipe.get("research").getAsString());
            assertEquals(want.centre(), recipe.getAsJsonObject("input").get("item").getAsString());
            assertEquals(0, recipe.get("input_damage").getAsInt(), "the original centre is the undamaged stack");
            assertEquals(want.components(), items(recipe.getAsJsonArray("components")));
            assertEquals(JsonParser.parseString(want.aspects()), recipe.get("aspects"));
            assertEquals(6, recipe.get("instability").getAsInt());
            assertEquals(want.goal(), recipe.getAsJsonObject("result").get("id").getAsString());
            assertEquals(JsonParser.parseString("[{\"component\":0,\"damage\":0}]"), recipe.get("component_predicates"), "the goggles and robes of the original recipe are undamaged ones");
        }
    }

    @Test void theMagicDamageTagAndTheWardHookAreDeclared() throws Exception {
        var values = json("data/thaumcraft/tags/damage_type/is_magic.json").getAsJsonArray("values").asList().stream().map(v -> v.getAsString()).toList();
        assertEquals(List.of("minecraft:magic", "minecraft:indirect_magic", "thaumcraft:taint", "thaumcraft:indirect_magic"), values);
        var mixins = json("thaumcraft-common.mixins.json").getAsJsonArray("mixins").asList().stream().map(m -> m.getAsString()).toList();
        assertTrue(mixins.contains("LivingEntityMagicWardMixin"), "the ward hook is registered");
    }

    @Test void theWornLayersAreTheVoidArmorsUntilTheRobeModelIsPorted() {
        assertTrue(getClass().getClassLoader().getResource("assets/thaumcraft/textures/models/armor/void_layer_1.png") != null
                && getClass().getClassLoader().getResource("assets/thaumcraft/textures/models/armor/void_layer_2.png") != null);
    }
}
