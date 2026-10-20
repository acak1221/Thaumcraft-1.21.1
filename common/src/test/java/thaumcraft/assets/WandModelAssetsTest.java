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
                // A staff is drawn with the long staff shape and the skin of the plain rod it is made from.
                String shape = (rod.staff() ? "thaumcraft:item/staff" : "thaumcraft:item/wand") + (focus.suffix().isEmpty() ? "_shape" : "_focus_shape");
                if (!textures.get("rod").getAsString().equals("thaumcraft:models/wand_rod_" + rod.baseTag())
                        || !textures.get("cap").getAsString().equals("thaumcraft:models/wand_cap_" + cap.tag())
                        || textures.has("focus") != !focus.suffix().isEmpty()
                        || !model.get("parent").getAsString().equals(shape))
                    wrong.add(chosen + " shows the wrong parts: " + textures);
            }
        }
        assertTrue(wrong.isEmpty(), "wrong selections: " + wrong);
    }

    @Test void thePropertyValuesAreExactBinaryFractionsInOrder() {
        assertTrue(WandMaterials.RODS.size() <= WandModels.ROD_SCALE + 1 && WandMaterials.CAPS.size() <= WandModels.CAP_SCALE + 1,
                "the scales must hold every rod and cap: more parts need a bigger power of two (and the generator's scales)");
        float last = -1;
        for (var rod : WandMaterials.RODS) {
            float value = WandModels.rodValue(rod.tag());
            assertTrue(value > last && value >= 0 && value <= 1, rod.tag());
            assertEquals(value, (float) (Math.round(value * 32) / 32.0), 0F, rod.tag() + " is a multiple of 1/32");
            last = value;
        }
        assertEquals(0F, WandModels.rodValue("wood"), 0F);
        assertEquals(8 / 32F, WandModels.rodValue("reed"), 0F);
        assertEquals(17 / 32F, WandModels.rodValue("primal_staff"), 0F);
        assertEquals(0F, WandModels.capValue("iron"), 0F);
        assertEquals(.25F, WandModels.capValue("gold"), 0F);
        assertEquals(.5F, WandModels.capValue("thaumium"), 0F);
        assertEquals(.75F, WandModels.capValue("void"), 0F);
        assertEquals(List.of(0F, .25F, .5F, .75F, .8125F, .875F, .9375F, 1F), WandModels.FOCI.stream().map(WandModels.Focus::value).toList());
    }

    @Test void everyNamedModelExistsWithItsParentAndTextures() throws Exception {
        JsonArray overrides = json("assets/thaumcraft/models/item/wand.json").getAsJsonArray("overrides");
        for (var entry : overrides) {
            String name = entry.getAsJsonObject().get("model").getAsString();
            assertTrue(name.startsWith("thaumcraft:item/"), name);
            JsonObject model = json("assets/thaumcraft/models/item/" + name.substring("thaumcraft:item/".length()) + ".json");
            String parent = model.get("parent").getAsString();
            boolean withFocus = parent.equals("thaumcraft:item/wand_focus_shape") || parent.equals("thaumcraft:item/staff_focus_shape");
            assertTrue(withFocus || parent.equals("thaumcraft:item/wand_shape") || parent.equals("thaumcraft:item/staff_shape"), name + " has the parent " + parent);
            assertEquals(name.endsWith("_fire") || name.endsWith("_excavation") || name.endsWith("_frost") || name.endsWith("_shock")
                    || name.endsWith("_warding") || name.endsWith("_trade") || name.endsWith("_portable_hole"), withFocus, name);
            JsonObject textures = model.getAsJsonObject("textures");
            assertEquals(withFocus ? 3 : 2, textures.size(), name);
            for (String key : textures.keySet()) {
                String texture = textures.get(key).getAsString();
                assertTrue(exists("assets/thaumcraft/textures/" + texture.substring("thaumcraft:".length()) + ".png"), name + ": texture " + texture);
            }
        }
        for (String shape : List.of("wand_shape", "wand_focus_shape", "staff_shape", "staff_focus_shape")) assertTrue(exists("assets/thaumcraft/models/item/" + shape + ".json"), shape);
    }

    @Test void everyRodHasItsItemModelAndTextures() throws Exception {
        for (var rod : WandMaterials.RODS) {
            if (rod.tag().equals("wood")) continue; // the vanilla stick
            // A staff core is the item staff_rod_<rod> with its own icon; the model skin is the plain rod's.
            String item = (rod.staff() ? "staff_rod_" : "wand_rod_") + rod.baseTag();
            JsonObject model = json("assets/thaumcraft/models/item/" + item + ".json");
            assertEquals("thaumcraft:items/" + item, model.getAsJsonObject("textures").get("layer0").getAsString(), rod.tag());
            assertTrue(exists("assets/thaumcraft/textures/items/" + item + ".png"), rod.tag() + " icon");
            assertTrue(exists("assets/thaumcraft/textures/models/wand_rod_" + rod.baseTag() + ".png"), rod.tag() + " model skin");
        }
        for (var cap : WandMaterials.CAPS) assertTrue(exists("assets/thaumcraft/textures/models/wand_cap_" + cap.tag() + ".png"), cap.tag());
    }
}
