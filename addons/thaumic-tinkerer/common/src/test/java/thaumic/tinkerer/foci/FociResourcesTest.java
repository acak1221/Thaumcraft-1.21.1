package thaumic.tinkerer.foci;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import org.junit.jupiter.api.Test;
import thaumic.tinkerer.common.lib.LibItemNames;
import thaumic.tinkerer.common.lib.LibResearch;

/**
 * Test-only code of the port (no 1.7.10 counterpart), CC BY-NC-SA 3.0 like the add-on.
 *
 * <p>The data of the Foci group: every focus has a model with an existing texture, the ornaments and the depth layers the
 * 1.7.10 foci used are there (and the depth layer of the Mending focus is animated), the language files carry the names, the
 * research texts and the pages of every focus in English and Russian, and the blacklist tag of the Distortion focus.</p>
 */
class FociResourcesTest {
    private static final String NS = "thaumictinkerer";

    /** The seven foci, in the registration order of {@code Foci.register()}. */
    private static final List<String> FOCI = List.of(LibItemNames.FOCUS_DEFLECT, LibItemNames.FOCUS_DISLOCATION,
            LibItemNames.FOCUS_ENDER_CHEST, LibItemNames.FOCUS_FLIGHT, LibItemNames.FOCUS_HEAL, LibItemNames.FOCUS_SMELT,
            LibItemNames.FOCUS_TELEKINESIS);

    /** The research key of every focus and the number of its text pages (the recipe page is added by the code). */
    private static final Map<String, String> RESEARCH = Map.of(LibItemNames.FOCUS_DEFLECT, LibResearch.KEY_FOCUS_DEFLECT,
            LibItemNames.FOCUS_DISLOCATION, LibResearch.KEY_FOCUS_DISLOCATION, LibItemNames.FOCUS_ENDER_CHEST,
            LibResearch.KEY_FOCUS_ENDER_CHEST, LibItemNames.FOCUS_FLIGHT, LibResearch.KEY_FOCUS_FLIGHT,
            LibItemNames.FOCUS_HEAL, LibResearch.KEY_FOCUS_HEAL, LibItemNames.FOCUS_SMELT, LibResearch.KEY_FOCUS_SMELT,
            LibItemNames.FOCUS_TELEKINESIS, LibResearch.KEY_FOCUS_TELEKINESIS);

    private static JsonObject json(String path) {
        try (InputStream in = FociResourcesTest.class.getResourceAsStream(path)) {
            assertNotNull(in, "missing resource " + path);
            return JsonParser.parseReader(new InputStreamReader(in, StandardCharsets.UTF_8)).getAsJsonObject();
        } catch (IOException e) {
            throw new AssertionError(path, e);
        }
    }

    private static boolean exists(String path) {
        return FociResourcesTest.class.getResource(path) != null;
    }

    private static String texture(String name) {
        return "/assets/" + NS + "/textures/item/" + name + ".png";
    }

    @Test
    void everyFocusHasAFlatItemModelWithItsOwnTexture() {
        assertEquals(7, FOCI.size());
        assertEquals(7, FOCI.stream().distinct().count());
        for (String focus : FOCI) {
            JsonObject model = json("/assets/" + NS + "/models/item/" + focus + ".json");
            assertEquals("minecraft:item/generated", model.get("parent").getAsString(), focus);
            JsonObject textures = model.getAsJsonObject("textures");
            assertEquals(NS + ":item/" + focus, textures.get("layer0").getAsString(), focus);
            assertEquals(1, textures.size(), focus + " has one layer");
            assertTrue(exists(texture(focus)), focus + ": the item texture exists");
        }
    }

    @Test
    void theOrnamentsAndDepthLayersOfTheOriginalFociExist() {
        // 1.7.10: Flight, Dislocation, Smelt and Telekinesis have an ornament, Heal and Ender Chest a depth layer.
        for (String focus : List.of(LibItemNames.FOCUS_FLIGHT, LibItemNames.FOCUS_DISLOCATION, LibItemNames.FOCUS_SMELT,
                LibItemNames.FOCUS_TELEKINESIS)) {
            assertTrue(exists(texture(focus + "_orn")), focus + " ornament");
        }
        for (String focus : List.of(LibItemNames.FOCUS_HEAL, LibItemNames.FOCUS_ENDER_CHEST)) {
            assertTrue(exists(texture(focus + "_depth")), focus + " depth layer");
        }
        assertFalse(exists(texture(LibItemNames.FOCUS_DEFLECT + "_orn")), "Distortion has no ornament");
        assertFalse(exists(texture(LibItemNames.FOCUS_DEFLECT + "_depth")), "Distortion has no depth layer");
        // The depth layer of the Mending focus is an animation: a strip of nine frames and its metadata.
        assertTrue(exists(texture(LibItemNames.FOCUS_HEAL + "_depth") + ".mcmeta"), "the animation of the Mending depth layer");
        json(texture(LibItemNames.FOCUS_HEAL + "_depth") + ".mcmeta");
    }

    /**
     * The depth layers are 8x8 (the strip of Mending 8x72, nine frames) and are drawn by Thaumcraft's wand renderer from the
     * texture file, as standalone sheets. If the block atlas stitched them, the 8x8 size would lower the maximum mip level of
     * the whole atlas (the client logged "limits mip level from 4 to 3"), so the atlas file filters them out; no model may name
     * one, or the filter would turn that model's texture into the missing texture.
     */
    @Test
    void theDepthLayersAreKeptOutOfTheBlockAtlasAndNoModelNamesThem() {
        List<Pattern> filtered = new ArrayList<>();
        for (JsonElement source : json("/assets/minecraft/atlases/blocks.json").getAsJsonArray("sources")) {
            JsonObject object = source.getAsJsonObject();
            if (!"minecraft:filter".equals(object.get("type").getAsString())) continue;
            JsonObject pattern = object.getAsJsonObject("pattern");
            if (!NS.equals(pattern.get("namespace").getAsString())) continue;
            filtered.add(Pattern.compile(pattern.get("path").getAsString()));
        }
        assertFalse(filtered.isEmpty(), "the atlas file has a filter for the namespace of the add-on");
        // The Foci with a depth layer, and the Kami focus that has one too (the same pattern covers all of them).
        for (String focus : List.of(LibItemNames.FOCUS_HEAL, LibItemNames.FOCUS_ENDER_CHEST, LibItemNames.FOCUS_RECALL)) {
            String sprite = "item/" + focus + "_depth";
            assertTrue(exists(texture(focus + "_depth")), sprite + ": the texture file stays (the wand renderer reads it)");
            assertTrue(filtered.stream().anyMatch(p -> p.matcher(sprite).matches()), sprite + " is filtered out of the atlas");
        }
        // The filter must not swallow what the models use: the 16x16 icons and ornaments stay stitched.
        for (String focus : FOCI) {
            assertTrue(filtered.stream().noneMatch(p -> p.matcher("item/" + focus).matches()), focus + " icon stays in the atlas");
            assertTrue(filtered.stream().noneMatch(p -> p.matcher("item/" + focus + "_orn").matches()), focus + " ornament stays");
        }
        for (String focus : FOCI) {
            String model = json("/assets/" + NS + "/models/item/" + focus + ".json").toString();
            assertFalse(model.contains("_depth"), focus + ": the model does not name a depth layer");
        }
    }

    @Test
    void theLanguageFilesNameEveryFocusAndItsResearch() {
        for (String lang : List.of("en_us", "ru_ru")) {
            JsonObject text = json("/assets/" + NS + "/lang/" + lang + ".json");
            for (String focus : FOCI) {
                String key = RESEARCH.get(focus);
                for (String entry : List.of("item." + NS + "." + focus, "tc.research_name." + key, "tc.research_text." + key,
                        "ttresearch.page." + key + ".0")) {
                    assertTrue(text.has(entry) && !text.get(entry).getAsString().isBlank(), lang + ": " + entry);
                }
            }
            assertTrue(text.has("ttresearch.page." + LibResearch.KEY_FOCUS_DISLOCATION + ".1"), lang + ": second Dislocation page");
            assertTrue(text.has("ttmisc.focusDislocation.tooltip"), lang);
            assertTrue(text.has("ttmisc.focusDislocation.tooltipExtra"), lang);
            assertTrue(text.has("ttmisc.focusDislocation.tooBig"), lang + ": the message of a load that is too large");
            // The research texts of the Thaumic Tinkerer entries carry the [TT] mark.
            assertTrue(text.get("tc.research_text." + LibResearch.KEY_FOCUS_FLIGHT).getAsString().startsWith("[TT]"), lang);
        }
    }

    @Test
    void theEnglishNamesAreTheOriginalOnes() {
        JsonObject text = json("/assets/" + NS + "/lang/en_us.json");
        assertEquals("Wand Focus: Uprising", text.get("item." + NS + ".focus_flight").getAsString());
        assertEquals("Wand Focus: Dislocation", text.get("item." + NS + ".focus_dislocation").getAsString());
        assertEquals("Wand Focus: Telekinesis", text.get("item." + NS + ".focus_telekinesis").getAsString());
        assertEquals("Wand Focus: Efreet's Flame", text.get("item." + NS + ".focus_smelt").getAsString());
        assertEquals("Wand Focus: Distortion", text.get("item." + NS + ".focus_deflect").getAsString());
        assertEquals("Wand Focus: Mending", text.get("item." + NS + ".focus_heal").getAsString());
        assertEquals("Wand Focus: Ender Rift", text.get("item." + NS + ".focus_ender_chest").getAsString());
    }

    @Test
    void theBlacklistTagOfTheDistortionFocusHoldsTheExperienceBottle() {
        JsonObject tag = json("/data/" + NS + "/tags/entity_type/deflect_blacklist.json");
        assertFalse(tag.get("replace").getAsBoolean());
        JsonArray values = tag.getAsJsonArray("values");
        boolean bottle = false;
        for (JsonElement value : values) bottle |= value.getAsString().equals("minecraft:experience_bottle");
        assertTrue(bottle, "the experience bottle is never deflected");
    }
}
