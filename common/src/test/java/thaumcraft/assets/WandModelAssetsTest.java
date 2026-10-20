package thaumcraft.assets;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import thaumcraft.common.items.wands.WandMaterials;
import thaumcraft.common.items.wands.WandModels;

/**
 * The wand's item model selector: wand.json must pick, for every rod, cap and focus the wand can have, the model of exactly that
 * combination, searching its overrides the way the game does (from the end, the first entry whose predicates are all met wins), and every
 * model it names must exist with parents and textures that exist.
 */
class WandModelAssetsTest {
    private JsonObject json(String path) throws Exception {
        var stream = getClass().getClassLoader().getResourceAsStream(path);
        assertNotNull(stream, "missing resource " + path);
        try (var reader = new InputStreamReader(stream, StandardCharsets.UTF_8)) {
            return JsonParser.parseReader(reader).getAsJsonObject();
        }
    }

    private boolean exists(String path) { return getClass().getClassLoader().getResource(path) != null; }

    private static String resolve(JsonArray overrides, float rod, float cap, float focus) {
        for (int index = overrides.size() - 1; index >= 0; index--) {
            JsonObject override = overrides.get(index).getAsJsonObject();
            JsonObject predicate = override.getAsJsonObject("predicate");
            boolean met = predicate.get("thaumcraft:wand_rod").getAsFloat() <= rod && predicate.get("thaumcraft:wand_cap").getAsFloat() <= cap
                    && predicate.get("thaumcraft:wand_focus").getAsFloat() <= focus;
            if (met) return override.get("model").getAsString();
        }
        return null;
    }

    @Test void everyCombinationOfRodCapAndFocusSelectsItsOwnModel() throws Exception {
        JsonArray overrides = json("assets/thaumcraft/models/item/wand.json").getAsJsonArray("overrides");
        assertEquals(WandMaterials.RODS.size() * WandMaterials.CAPS.size() * WandModels.FOCI.size(), overrides.size(), "one entry per combination");
        List<String> wrong = new ArrayList<>();
        for (var rod : WandMaterials.RODS) for (var cap : WandMaterials.CAPS) for (var focus : WandModels.FOCI) {
            String chosen = resolve(overrides, WandModels.rodValue(rod.tag()), WandModels.capValue(cap.tag()), focus.value());
            String expected = WandModels.model(rod.tag(), cap.tag(), focus);
            if (!expected.equals(chosen)) wrong.add(rod.tag() + "/" + cap.tag() + "/" + focus.suffix() + " -> " + chosen + " (wanted " + expected + ")");
            else {
                // The chosen model really is that rod and that cap, with the focus part only when a focus is fitted.
                JsonObject model = json("assets/thaumcraft/models/item/" + chosen.substring("thaumcraft:item/".length()) + ".json");
                JsonObject textures = model.getAsJsonObject("textures");
                if (!textures.get("rod").getAsString().equals("thaumcraft:models/wand_rod_" + rod.tag())
                        || !textures.get("cap").getAsString().equals("thaumcraft:models/wand_cap_" + cap.tag())
                        || textures.has("focus") != !focus.suffix().isEmpty()
                        || !model.get("parent").getAsString().equals(focus.suffix().isEmpty() ? "thaumcraft:item/wand_shape" : "thaumcraft:item/wand_focus_shape"))
                    wrong.add(chosen + " shows the wrong parts: " + textures);
            }
        }
        assertTrue(wrong.isEmpty(), "wrong selections: " + wrong);
    }

    @Test void thePropertyValuesAreExactBinaryFractionsInOrder() {
        float last = -1;
        for (var rod : WandMaterials.RODS) {
            float value = WandModels.rodValue(rod.tag());
            assertTrue(value > last && value >= 0 && value <= 1, rod.tag());
            assertEquals(value, (float) (Math.round(value * 8) / 8.0), 0F, rod.tag() + " is a multiple of 1/8");
            last = value;
        }
        assertEquals(0F, WandModels.rodValue("wood"), 0F);
        assertEquals(1F, WandModels.rodValue("reed"), 0F);
        assertEquals(0F, WandModels.capValue("iron"), 0F);
        assertEquals(.5F, WandModels.capValue("gold"), 0F);
        assertEquals(1F, WandModels.capValue("thaumium"), 0F);
        assertEquals(List.of(0F, .25F, .5F, .75F, 1F), WandModels.FOCI.stream().map(WandModels.Focus::value).toList());
    }

    @Test void everyNamedModelExistsWithItsParentAndTextures() throws Exception {
        JsonArray overrides = json("assets/thaumcraft/models/item/wand.json").getAsJsonArray("overrides");
        for (var entry : overrides) {
            String name = entry.getAsJsonObject().get("model").getAsString();
            assertTrue(name.startsWith("thaumcraft:item/"), name);
            JsonObject model = json("assets/thaumcraft/models/item/" + name.substring("thaumcraft:item/".length()) + ".json");
            String parent = model.get("parent").getAsString();
            boolean withFocus = parent.equals("thaumcraft:item/wand_focus_shape");
            assertTrue(withFocus || parent.equals("thaumcraft:item/wand_shape"), name + " has the parent " + parent);
            assertEquals(name.endsWith("_fire") || name.endsWith("_excavation") || name.endsWith("_frost") || name.endsWith("_shock"), withFocus, name);
            JsonObject textures = model.getAsJsonObject("textures");
            assertEquals(withFocus ? 3 : 2, textures.size(), name);
            for (String key : textures.keySet()) {
                String texture = textures.get(key).getAsString();
                assertTrue(exists("assets/thaumcraft/textures/" + texture.substring("thaumcraft:".length()) + ".png"), name + ": texture " + texture);
            }
        }
        for (String shape : List.of("wand_shape", "wand_focus_shape")) assertTrue(exists("assets/thaumcraft/models/item/" + shape + ".json"), shape);
    }

    @Test void everyRodHasItsItemModelAndTextures() throws Exception {
        for (var rod : WandMaterials.RODS) {
            if (rod.tag().equals("wood")) continue; // the vanilla stick
            JsonObject item = json("assets/thaumcraft/models/item/wand_rod_" + rod.tag() + ".json");
            assertEquals("thaumcraft:items/wand_rod_" + rod.tag(), item.getAsJsonObject("textures").get("layer0").getAsString(), rod.tag());
            assertTrue(exists("assets/thaumcraft/textures/items/wand_rod_" + rod.tag() + ".png"), rod.tag() + " icon");
            assertTrue(exists("assets/thaumcraft/textures/models/wand_rod_" + rod.tag() + ".png"), rod.tag() + " model skin");
        }
        for (var cap : WandMaterials.CAPS) assertTrue(exists("assets/thaumcraft/textures/models/wand_cap_" + cap.tag() + ".png"), cap.tag());
    }
}
