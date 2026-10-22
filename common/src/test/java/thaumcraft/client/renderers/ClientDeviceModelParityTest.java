package thaumcraft.client.renderers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;

/**
 * The block models of the client rendering units of this wave, checked against the
 * numbers of the original TC4 4.2.3.5 renderers.
 *
 * <p>Every expectation below is the literal of the oracle:</p>
 * <ul>
 *   <li>{@code thaumcraft/client/renderers/block/BlockLifterRenderer.java:28-50} - the two
 *       emissive inner slabs of the inventory render;</li>
 *   <li>{@code thaumcraft/client/renderers/block/BlockEssentiaReservoirRenderer.java:28-52} -
 *       the vessel of two to fourteen sixteenths;</li>
 *   <li>{@code thaumcraft/client/renderers/block/BlockWoodenDeviceRenderer.java:44-83} - the
 *       ten boxes of the arcane ear;</li>
 *   <li>{@code thaumcraft/client/renderers/block/BlockStoneDeviceRenderer.java:36-70} - the
 *       boxes of the wand pedestals;</li>
 *   <li>{@code thaumcraft/client/renderers/block/BlockMetalDeviceRenderer.java:44-70} - the
 *       boxes of the alchemy lamps.</li>
 * </ul>
 *
 * <p>The test reads the model assets of the port; it never constructs a {@code Block},
 * because the 1.21.1 block constructor registers an intrusive holder in the block
 * registry and a block that is never registered makes the later registry freeze
 * throw, which breaks every test of the JVM.</p>
 */
class ClientDeviceModelParityTest {
    private static final String ASSETS = "assets/thaumcraft/";
    private static final float EPS = 1.0E-4F;

    private static JsonObject model(String path) {
        try (InputStream stream = ClientDeviceModelParityTest.class
                .getClassLoader().getResourceAsStream(ASSETS + "models/" + path + ".json")) {
            if (stream == null) throw new IllegalStateException("the model " + path + " is missing");
            return JsonParser.parseReader(new InputStreamReader(stream, StandardCharsets.UTF_8)).getAsJsonObject();
        } catch (IOException error) {
            throw new IllegalStateException("the model " + path + " cannot be read", error);
        }
    }

    /** The box of an element as the original writes it, in sixteenths of a block. */
    private static float[] box(JsonObject element) {
        JsonArray from = element.getAsJsonArray("from");
        JsonArray to = element.getAsJsonArray("to");
        return new float[]{from.get(0).getAsFloat(), from.get(1).getAsFloat(), from.get(2).getAsFloat(),
                to.get(0).getAsFloat(), to.get(1).getAsFloat(), to.get(2).getAsFloat()};
    }

    private static void assertBox(JsonObject element, float x0, float y0, float z0,
                                   float x1, float y1, float z1, String message) {
        float[] box = box(element);
        assertEquals(x0, box[0], EPS, message + " x0");
        assertEquals(y0, box[1], EPS, message + " y0");
        assertEquals(z0, box[2], EPS, message + " z0");
        assertEquals(x1, box[3], EPS, message + " x1");
        assertEquals(y1, box[4], EPS, message + " y1");
        assertEquals(z1, box[5], EPS, message + " z1");
    }

    private static String texture(JsonObject element, String face) {
        return element.getAsJsonObject("faces").getAsJsonObject(face).get("texture").getAsString();
    }

    private static int tint(JsonObject element, String face) {
        var tint = element.getAsJsonObject("faces").getAsJsonObject(face).get("tintindex");
        return tint == null ? -1 : tint.getAsInt();
    }

    // ------------------------------------------------------------ the levitator

    @Test void theLevitatorItemKeepsTheTwoEmissiveInnerSlabs() {
        // Original BlockLifterRenderer:38-50 - the slab of 0.01..0.99 by 0.9..0.99 with the
        // colour BlockCustomOreItem.colors[4] and the slab of 0.01..0.99 by 0.1..0.9 with the
        // colour BlockCustomOreItem.colors[5], both with the animated glow texture.
        JsonObject item = model("item/levitator");
        JsonArray elements = item.getAsJsonArray("elements");
        assertEquals(3, elements.size(), "the cube and the two inner slabs of the original");
        assertBox(elements.get(0).getAsJsonObject(), 0, 0, 0, 16, 16, 16, "the cube of the levitator");
        assertBox(elements.get(1).getAsJsonObject(), .16F, 14.4F, .16F, 15.84F, 15.84F, 15.84F,
                "the upper inner slab of the original");
        assertBox(elements.get(2).getAsJsonObject(), .16F, 1.6F, .16F, 15.84F, 14.4F, 15.84F,
                "the lower inner slab of the original");
        assertEquals(0, tint(elements.get(1).getAsJsonObject(), "north"),
                "the upper slab carries BlockCustomOreItem.colors[4] as its tint zero");
        assertEquals(1, tint(elements.get(2).getAsJsonObject(), "north"),
                "the lower slab carries BlockCustomOreItem.colors[5] as its tint one");
        assertEquals("#glow", texture(elements.get(1).getAsJsonObject(), "north"),
                "the original draws the slabs with the animated glow texture");
        assertEquals("thaumcraft:blocks/animatedglow", item.getAsJsonObject("textures").get("glow").getAsString(),
                "the animated glow sheet of the original");
    }

    @Test void theLevitatorBlockModelStaysThePlainCube() {
        // Original BlockLifterRenderer:55-88 - the world render draws the cube of the block
        // itself and the glow faces of the neighbours; the two inner slabs belong to the
        // inventory render only (oracle BlockLifterRenderer:32-53). The world cube of the
        // original is the ordinary cube of the block, so the world model of the port is the
        // cube of the parent and carries no element of its own: it cannot carry the slabs.
        JsonObject block = model("block/levitator");
        JsonArray elements = block.getAsJsonArray("elements");
        assertTrue(elements == null || elements.isEmpty(),
                "the world render of the original draws the cube of the levitator, not the slabs");
        assertEquals("minecraft:block/cube_bottom_top", block.get("parent").getAsString(),
                "the cube of the world model of the levitator");
        assertFalse(block.getAsJsonObject("textures").has("glow"),
                "the glow of the original is drawn by the world renderer, not by a model element");
    }

    // --------------------------------------------------------- the essentia tank

    @Test void theReservoirVesselIsTheOriginalInsetCube() {
        // Original BlockEssentiaReservoirRenderer:28-31 and :36-39 - the world and the
        // inventory render draw the vessel of two to fourteen sixteenths.
        for (String path : new String[]{"block/essentia_reservoir", "item/essentia_reservoir"}) {
            JsonObject model = model(path);
            if (model.getAsJsonArray("elements") == null) continue; // the item model has the block as its parent
            assertEquals(1, model.getAsJsonArray("elements").size(), "the vessel of the original is one box");
            assertBox(model.getAsJsonArray("elements").get(0).getAsJsonObject(), 2, 2, 2, 14, 14, 14,
                    "the vessel of the original is two to fourteen sixteenths");
        }
        assertEquals("thaumcraft:block/essentia_reservoir", model("item/essentia_reservoir").get("parent").getAsString(),
                "the item of the reservoir shows the same vessel of the original");
    }

    // -------------------------------------------------------------- the arcane ear

    @Test void theArcaneEarKeepsTheTenBoxesOfTheOriginal() {
        // Original BlockWoodenDeviceRenderer:44-83 - the base plate of 0..1 by 0..W3, the
        // pillar of W4..W12 by W3..1 and the eight arms of the bell.
        for (String path : new String[]{"block/arcane_ear", "block/arcane_ear_on"}) {
            JsonArray elements = model(path).getAsJsonArray("elements");
            assertEquals(10, elements.size(), "the arcane ear of the original has ten boxes");
            assertBox(elements.get(0).getAsJsonObject(), 0, 0, 0, 16, 3, 16, "the base plate");
            assertBox(elements.get(1).getAsJsonObject(), 4, 3, 4, 12, 16, 12, "the pillar");
            assertBox(elements.get(2).getAsJsonObject(), 4, 8, 1, 12, 16, 3, "the first arm");
            assertBox(elements.get(3).getAsJsonObject(), 5, 8, 3, 11, 15, 4, "the second arm");
            assertBox(elements.get(4).getAsJsonObject(), 1, 8, 4, 3, 16, 12, "the third arm");
            assertBox(elements.get(5).getAsJsonObject(), 3, 8, 5, 4, 15, 11, "the fourth arm");
            assertBox(elements.get(6).getAsJsonObject(), 4, 8, 13, 12, 16, 15, "the fifth arm");
            assertBox(elements.get(7).getAsJsonObject(), 5, 8, 12, 11, 15, 13, "the sixth arm");
            assertBox(elements.get(8).getAsJsonObject(), 13, 8, 4, 15, 16, 12, "the seventh arm");
            assertBox(elements.get(9).getAsJsonObject(), 12, 8, 5, 13, 15, 11, "the eighth arm");
        }
    }

    @Test void theArcaneEarKeepsTheOriginalSheets() {
        // Original BlockWoodenDevice:97-103 - the base plate and the pillar use the ear side
        // and the ear top of the render state, the eight arms use the bell sheets.
        JsonObject off = model("block/arcane_ear");
        assertEquals("thaumcraft:blocks/arcaneearsideoff", off.getAsJsonObject("textures").get("side").getAsString(),
                "the unpowered ear of the original uses the side sheet of the off state");
        assertEquals("thaumcraft:blocks/arcaneeartopoff", off.getAsJsonObject("textures").get("top").getAsString(),
                "the unpowered ear of the original uses the top sheet of the off state");
        JsonObject on = model("block/arcane_ear_on");
        assertEquals("thaumcraft:blocks/arcaneearsideon", on.getAsJsonObject("textures").get("side").getAsString(),
                "the powered ear of the original uses the side sheet of the on state");
        assertEquals("thaumcraft:blocks/arcaneeartopon", on.getAsJsonObject("textures").get("top").getAsString(),
                "the powered ear of the original uses the top sheet of the on state");
        assertEquals("thaumcraft:blocks/arcaneearbellside", on.getAsJsonObject("textures").get("bellside").getAsString(),
                "the arms of the original use the bell side sheet");
        assertEquals("thaumcraft:blocks/arcaneearbelltop", on.getAsJsonObject("textures").get("belltop").getAsString(),
                "the arms of the original use the bell top sheet");
        // The inventory of the original always draws the on state, oracle
        // BlockWoodenDeviceRenderer:46-51.
        assertEquals("thaumcraft:block/arcane_ear_on", model("item/arcane_ear").get("parent").getAsString(),
                "the item of the arcane ear shows the on state of the original");
    }

    // -------------------------------------------------------- the wand pedestals

    @Test void theWandPedestalKeepsTheThreeBoxesOfTheOriginal() {
        // Original BlockStoneDeviceRenderer:36-43 - the pedestal of 0..1 by 0..0.25, the
        // middle of 0.125..0.875 by 0.25..0.5 and the top of 0.25..0.75 by 0.5..1.
        JsonArray elements = model("block/wand_pedestal").getAsJsonArray("elements");
        assertEquals(3, elements.size(), "the wand pedestal of the original has three boxes");
        assertBox(elements.get(0).getAsJsonObject(), 0, 0, 0, 16, 4, 16, "the base of the pedestal");
        assertBox(elements.get(1).getAsJsonObject(), 2, 4, 2, 14, 8, 14, "the middle of the pedestal");
        assertBox(elements.get(2).getAsJsonObject(), 4, 8, 4, 12, 16, 12, "the top of the pedestal");
    }

    @Test void theWandPedestalKeepsTheOriginalSheets() {
        // Original BlockStoneDeviceRenderer:36-43 - the bottom of the pedestal is the
        // pedestal top, the top and the sides are the wand pedestal sheets.
        JsonObject model = model("block/wand_pedestal");
        assertEquals("thaumcraft:blocks/wandpedestal_side", model.getAsJsonObject("textures").get("side").getAsString(),
                "the sides of the pedestal of the original");
        assertEquals("thaumcraft:blocks/wandpedestal_top", model.getAsJsonObject("textures").get("top").getAsString(),
                "the top of the pedestal of the original");
        assertEquals("thaumcraft:blocks/pedestal_top", model.getAsJsonObject("textures").get("bottom").getAsString(),
                "the bottom of the pedestal of the original");
    }

    @Test void theFocusPedestalKeepsTheNineBoxesOfTheOriginal() {
        // Original BlockStoneDeviceRenderer:44-70 - the focus pedestal of the arcane wand
        // pedestal is the cross of nine boxes of one sixteenth of height.
        JsonArray elements = model("block/wand_pedestal_focus").getAsJsonArray("elements");
        assertEquals(9, elements.size(), "the focus pedestal of the original has nine boxes");
        assertBox(elements.get(0).getAsJsonObject(), 5, 0, 5, 11, 1, 11, "the centre of the focus pedestal");
        assertBox(elements.get(1).getAsJsonObject(), 1, 0, 7, 5, 1, 9, "the west arm of the focus pedestal");
        assertBox(elements.get(2).getAsJsonObject(), 11, 0, 7, 15, 1, 9, "the east arm of the focus pedestal");
        assertBox(elements.get(3).getAsJsonObject(), 7, 0, 1, 9, 1, 5, "the north arm of the focus pedestal");
        assertBox(elements.get(4).getAsJsonObject(), 7, 0, 11, 9, 1, 15, "the south arm of the focus pedestal");
        assertBox(elements.get(5).getAsJsonObject(), 1, 1, 7, 3, 7, 9, "the west post of the focus pedestal");
        assertBox(elements.get(6).getAsJsonObject(), 7, 1, 1, 9, 7, 3, "the north post of the focus pedestal");
        assertBox(elements.get(7).getAsJsonObject(), 13, 1, 7, 15, 7, 9, "the east post of the focus pedestal");
        assertBox(elements.get(8).getAsJsonObject(), 7, 1, 13, 9, 7, 15, "the south post of the focus pedestal");
    }

    // ---------------------------------------------------------- the alchemy lamps

    @Test void theAlchemyLampsKeepTheOriginalBox() {
        // Original BlockMetalDeviceRenderer:44-58 - the lamps of the metal device are the
        // box of W4..W12 by W2..W14.
        for (String path : new String[]{"block/arcane_lamp", "block/growth_lamp", "block/fertility_lamp"}) {
            JsonArray elements = model(path).getAsJsonArray("elements");
            assertEquals(1, elements.size(), "the lamp of the original is one box");
            assertBox(elements.get(0).getAsJsonObject(), 4, 2, 4, 12, 14, 12, "the box of the lamp of the original");
        }
    }

    @Test void theLampSheetsAreTheOriginalOnes() {
        // Original BlockMetalDevice:117-126 - lamp_side, lamp_top, lamp_grow_side,
        // lamp_grow_top, lamp_fert_side and lamp_fert_top.
        assertEquals("thaumcraft:blocks/lamp_side", model("block/arcane_lamp").getAsJsonObject("textures").get("side").getAsString(),
                "the arcane lamp of the original");
        assertEquals("thaumcraft:blocks/lamp_grow_side", model("block/growth_lamp").getAsJsonObject("textures").get("side").getAsString(),
                "the growth lamp of the original");
        assertEquals("thaumcraft:blocks/lamp_fert_side", model("block/fertility_lamp").getAsJsonObject("textures").get("side").getAsString(),
                "the fertility lamp of the original");
        assertTrue(model("block/growth_lamp").getAsJsonObject("textures").get("top").getAsString().endsWith("lamp_grow_top"),
                "the top of the growth lamp of the original");
    }
}
