package thaumcraft.eldritch;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;

/**
 * The block models and the block states of the eldritch stone family against the
 * original TC4 4.2.3.5 renderer.
 *
 * <p>The original {@code BlockEldritchRenderer:35-81} draws the metadata four, five,
 * six, seven, eight, nine and ten and nothing else; the metadata zero, one, two and
 * three - the altar, the obelisk, the obelisk middle and the capstone - are drawn by
 * their own block entity renderers only. The original
 * {@code BlockEldritch:92-94} gives the icon of every metadata.
 */
class EldritchBlockAssetParityTest {
    private static final String ASSETS = "assets/thaumcraft/";

    @Test void theStonesThatTheOriginalNeverDrewHaveNoModel() {
        // The altar, the obelisk, the obelisk middle and the capstone.
        for (String model : new String[]{"eldritch", "eldritch_1", "eldritch_2", "eldritch_3"}) {
            JsonObject json = model("block/" + model);
            assertTrue(json.getAsJsonArray("elements").isEmpty(),
                    model + " is drawn by the block entity renderer of the original and not by a cube");
        }
    }

    @Test void theGlowingCrustKeepsTheOriginalInsetCube() {
        // Original BlockEldritchRenderer:22-32 and :46-63 - the crust is a cube of
        // two to fourteen sixteenths and it uses the es_i_1 and the es_i_2 sheet.
        assertInsetCube(model("block/eldritch_crust"), "thaumcraft:blocks/es_i_1");
        assertInsetCube(model("block/eldritch_crust_5"), "thaumcraft:blocks/es_i_2");
        // Original BlockEldritchRenderer:28-32 - the decorated stone is the same cube.
        assertInsetCube(model("block/eldritch_deco"), "thaumcraft:blocks/deco_1");
    }

    @Test void theDrawnStonesKeepTheOriginalSheets() {
        // Original BlockEldritch:93 - the doorway is deco_3, the lock is deco_2, the
        // crab spawner is the crust of the cosmetic solid and the runed stone is es_5.
        assertFullCube(model("block/eldritch_doorway"), "thaumcraft:blocks/deco_3");
        assertFullCube(model("block/eldritch_doorway_8"), "thaumcraft:blocks/deco_2");
        assertFullCube(model("block/eldritch_spawner"), "thaumcraft:blocks/crust");
        assertFullCube(model("block/eldritch_spawner_10_0"), "thaumcraft:blocks/es_5");
    }

    @Test void theEldritchItemKeepsTheOriginalIcon() {
        // Original BlockEldritch:79 and :93 - the icon of the metadata zero to three
        // is the obsidian tile sheet.
        JsonObject item = model("item/eldritch");
        assertEquals("minecraft:block/cube_all", item.get("parent").getAsString(), "the item is a cube");
        assertEquals("thaumcraft:blocks/obsidiantile",
                item.getAsJsonObject("textures").get("all").getAsString(), "the icon of the item");
    }

    @Test void theBlockStateOfTheEldritchStoneCoversItsGroup() {
        // Original BlockEldritch:151-153 - the group of the altar, the obelisk, the
        // obelisk middle and the capstone is the metadata zero to three.
        JsonObject state = blockstate("eldritch");
        JsonObject variants = state.getAsJsonObject("variants");
        for (int variant = 0; variant <= 3; variant++) {
            JsonObject entry = variants.getAsJsonObject("variant=" + variant);
            assertEquals("thaumcraft:block/eldritch" + (variant == 0 ? "" : "_" + variant),
                    entry.get("model").getAsString(), "the model of the variant " + variant);
        }
    }

    // ------------------------------------------------------------------ tools

    private static void assertInsetCube(JsonObject model, String texture) {
        JsonArray elements = model.getAsJsonArray("elements");
        assertEquals(1, elements.size(), "the crust is one box");
        JsonObject box = elements.get(0).getAsJsonObject();
        JsonArray from = box.getAsJsonArray("from");
        JsonArray to = box.getAsJsonArray("to");
        for (int axis = 0; axis < 3; axis++) {
            assertEquals(2.0F, from.get(axis).getAsFloat(), 1.0E-5F, "the lower corner " + axis + " of the crust");
            assertEquals(14.0F, to.get(axis).getAsFloat(), 1.0E-5F, "the upper corner " + axis + " of the crust");
        }
        assertEquals(texture, model.getAsJsonObject("textures").get("all").getAsString(), "the sheet of the crust");
    }

    private static void assertFullCube(JsonObject model, String texture) {
        assertEquals("minecraft:block/cube_all", model.get("parent").getAsString(), "the stone is a full cube");
        assertEquals(texture, model.getAsJsonObject("textures").get("all").getAsString(), "the sheet of the stone");
    }

    /** Reads a model of the mod resources. */
    private static JsonObject model(String path) {
        return JsonParser.parseString(text(ASSETS + "models/" + path + ".json")).getAsJsonObject();
    }

    /** Reads a block state of the mod resources. */
    private static JsonObject blockstate(String id) {
        return JsonParser.parseString(text(ASSETS + "blockstates/" + id + ".json")).getAsJsonObject();
    }

    private static String text(String path) {
        try (InputStream stream = EldritchBlockAssetParityTest.class.getClassLoader().getResourceAsStream(path)) {
            if (stream == null) throw new AssertionError("missing resource " + path);
            return new String(stream.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new AssertionError("cannot read " + path, e);
        }
    }
}
