/**
 * New in the 1.21.1 port of Thaumic Tinkerer (no 1.7.10 counterpart): JUnit tests of the data files of the KamiGear group. Distributed under the same licence as the add-on
 * (CC BY-NC-SA 3.0, see addons/thaumic-tinkerer/NOTICE.md).
 */
package thaumic.tinkerer.kamigear;

import static org.junit.jupiter.api.Assertions.assertEquals;
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
import org.junit.jupiter.api.Test;
import thaumic.tinkerer.common.block.tile.container.kami.ContainerIchorPouch;
import thaumic.tinkerer.common.item.kami.ItemIchorPouch;
import thaumic.tinkerer.common.lib.LibItemNames;

/**
 * Test-only code of the port (no 1.7.10 counterpart), CC BY-NC-SA 3.0 like the add-on.
 *
 * <p>The data of the KamiGear group: every item has a model, every model's texture and every override target exists, the
 * armour layers are PNG files in the places the armour materials name, the nugget recipe and the tag files parse and point
 * at registered paths, and the pouch's language key exists after the language merge.</p>
 */
class KamiGearResourcesTest {
    private static final String NS = "thaumictinkerer";

    /** Every item of the group, in the registration order of {@code KamiGear.register()}. */
    private static final List<String> ITEMS = items();

    private static List<String> items() {
        List<String> all = new ArrayList<>(List.of(LibItemNames.KAMI_RESOURCE_NAMES));
        all.addAll(List.of(LibItemNames.ICHOR_HELM, LibItemNames.ICHOR_CHEST, LibItemNames.ICHOR_LEGS,
                LibItemNames.ICHOR_BOOTS, LibItemNames.ICHOR_HELM_GEM, LibItemNames.ICHOR_CHEST_GEM,
                LibItemNames.ICHOR_LEGS_GEM, LibItemNames.ICHOR_BOOTS_GEM, LibItemNames.CAT_AMULET,
                LibItemNames.ICHOR_PICK, LibItemNames.ICHOR_SHOVEL, LibItemNames.ICHOR_AXE, LibItemNames.ICHOR_SWORD,
                LibItemNames.ICHOR_PICK_GEM, LibItemNames.ICHOR_SHOVEL_GEM, LibItemNames.ICHOR_AXE_GEM,
                LibItemNames.ICHOR_SWORD_GEM, LibItemNames.ICHOR_POUCH, LibItemNames.BLOCK_TALISMAN,
                LibItemNames.PLACEMENT_MIRROR, LibItemNames.PROTOCLAY, LibItemNames.SKY_PEARL,
                LibItemNames.FOCUS_SHADOWBEAM, LibItemNames.FOCUS_XP_DRAIN, LibItemNames.FOCUS_RECALL));
        return all;
    }

    private static JsonObject json(String path) {
        try (InputStream in = KamiGearResourcesTest.class.getResourceAsStream(path)) {
            assertNotNull(in, "missing resource " + path);
            return JsonParser.parseReader(new InputStreamReader(in, StandardCharsets.UTF_8)).getAsJsonObject();
        } catch (IOException e) {
            throw new AssertionError(path, e);
        }
    }

    private static boolean exists(String path) {
        return KamiGearResourcesTest.class.getResource(path) != null;
    }

    private static String own(String id) {
        return id.startsWith(NS + ":") ? id.substring(NS.length() + 1) : null;
    }

    // ---- item models ---------------------------------------------------------------------------------------------

    @Test
    void theGroupListsThirtyThreeItems() {
        assertEquals(33, ITEMS.size());
        assertEquals(33, ITEMS.stream().distinct().count(), "no path twice");
    }

    @Test
    void everyItemHasAModelWithAnExistingTexture() {
        for (String item : ITEMS) {
            JsonObject model = json("/assets/" + NS + "/models/item/" + item + ".json");
            assertTrue(model.has("parent"), item + " has a parent");
            JsonObject textures = model.getAsJsonObject("textures");
            assertNotNull(textures, item + " has textures");
            for (Map.Entry<String, JsonElement> texture : textures.entrySet()) {
                String path = own(texture.getValue().getAsString());
                assertNotNull(path, item + " uses a texture of the add-on: " + texture.getValue());
                assertTrue(exists("/assets/" + NS + "/textures/" + path + ".png"),
                        item + ": texture " + path + " exists");
            }
        }
    }

    @Test
    void everyOverrideOfAModelPointsAtAnExistingModelWithTheClientPredicates() {
        int overrides = 0;
        for (String item : ITEMS) {
            JsonObject model = json("/assets/" + NS + "/models/item/" + item + ".json");
            if (!model.has("overrides")) continue;
            JsonArray array = model.getAsJsonArray("overrides");
            for (JsonElement element : array) {
                JsonObject override = element.getAsJsonObject();
                for (String predicate : override.getAsJsonObject("predicate").keySet()) {
                    // thaumic.tinkerer.client.KamiGearClient.PREDICATE_MODE / PREDICATE_ACTIVE
                    assertTrue(predicate.equals(NS + ":kami_mode") || predicate.equals(NS + ":kami_active"),
                            item + ": predicate " + predicate + " is registered by KamiGearClient");
                }
                String target = own(override.get("model").getAsString());
                assertNotNull(target, item);
                JsonObject targetModel = json("/assets/" + NS + "/models/" + target + ".json");
                for (Map.Entry<String, JsonElement> texture : targetModel.getAsJsonObject("textures").entrySet()) {
                    assertTrue(exists("/assets/" + NS + "/textures/" + own(texture.getValue().getAsString()) + ".png"),
                            target + " texture");
                }
                overrides++;
            }
        }
        // 4 awakened tools with modes 1 and 2, and the ring when active.
        assertEquals(9, overrides);
    }

    @Test
    void theAwakenedToolsEncodeTheModeAsAHalfStep() {
        // The predicate value is the mode / 2 (predicates are clamped to 0..1): mode 1 is 0.5, mode 2 is 1.0.
        for (String tool : List.of("ichor_pick_gem", "ichor_shovel_gem", "ichor_axe_gem", "ichor_sword_gem")) {
            JsonArray overrides = json("/assets/" + NS + "/models/item/" + tool + ".json").getAsJsonArray("overrides");
            assertEquals(2, overrides.size(), tool);
            assertEquals(0.5, overrides.get(0).getAsJsonObject().getAsJsonObject("predicate")
                    .get(NS + ":kami_mode").getAsDouble(), 0, tool);
            assertEquals(1.0, overrides.get(1).getAsJsonObject().getAsJsonObject("predicate")
                    .get(NS + ":kami_mode").getAsDouble(), 0, tool);
        }
    }

    // ---- armour, wand parts ----------------------------------------------------------------------------------------

    @Test
    void theArmourLayersAndWandPartTexturesArePngFiles() throws IOException {
        List<String> paths = List.of("textures/models/armor/ichor_layer_1.png", "textures/models/armor/ichor_layer_2.png",
                "textures/models/armor/ichor_gem_layer_1.png", "textures/models/armor/ichor_gem_layer_2.png",
                "textures/models/cap_ichor.png", "textures/models/rod_ichorcloth.png", "textures/models/wings.png",
                "textures/gui/ichor_pouch.png");
        for (String path : paths) {
            try (InputStream in = KamiGearResourcesTest.class.getResourceAsStream("/assets/" + NS + "/" + path)) {
                assertNotNull(in, path);
                byte[] signature = in.readNBytes(8);
                assertEquals((byte) 0x89, signature[0], path);
                assertEquals('P', signature[1], path);
                assertEquals('N', signature[2], path);
                assertEquals('G', signature[3], path);
            }
        }
    }

    @Test
    void theArmourLayersAreCopiesOfTheConvertedOriginalTextures() throws IOException {
        // docs/deviations/KamiGear.md: the two layer files of a material are copies of ichor_1/2 and ichor_gem_1/2.
        String[][] pairs = { { "ichor_layer_1", "ichor_1" }, { "ichor_layer_2", "ichor_2" },
                { "ichor_gem_layer_1", "ichor_gem_1" }, { "ichor_gem_layer_2", "ichor_gem_2" } };
        for (String[] pair : pairs) {
            byte[] layer = read("/assets/" + NS + "/textures/models/armor/" + pair[0] + ".png");
            byte[] converted = read("/assets/" + NS + "/textures/models/" + pair[1] + ".png");
            assertTrue(java.util.Arrays.equals(layer, converted), pair[0] + " is a copy of " + pair[1]);
        }
    }

    /**
     * The pouch screen draws the 256 x 256 texture as it is (1.7.10 {@code GuiIchorPouch}): the player-inventory panel is part of
     * the texture and lies over the lower rim of the leather pouch, as in the original (the client smoke run of 2026-10-30 looked
     * like a defect, "the panel is drawn over the pouch"; the texture is byte-identical to {@code ichorPouch.png} and this is how
     * it is drawn). What must hold is that the panel's cells sit exactly under the menu's player slots and the leather's dimples
     * under the 13 x 9 pouch slots; a screen that "fixed" the overlap by moving the panel would break that.
     */
    @Test
    void thePouchTextureHasThePlayerPanelUnderThePlayerSlotsAndTheLeatherUnderThePouchSlots() throws IOException {
        java.awt.image.BufferedImage image;
        try (InputStream in = KamiGearResourcesTest.class
                .getResourceAsStream("/assets/" + NS + "/textures/gui/ichor_pouch.png")) {
            assertNotNull(in);
            image = javax.imageio.ImageIO.read(in);
        }
        assertEquals(256, image.getWidth());
        assertEquals(256, image.getHeight());
        int slot = 16;
        for (int y = 0; y < 3; y++) {
            for (int x = 0; x < 9; x++) {
                assertPanelCell(image, ContainerIchorPouch.PLAYER_X + x * 18, ContainerIchorPouch.PLAYER_Y + y * 18);
            }
        }
        for (int x = 0; x < 9; x++) {
            assertPanelCell(image, ContainerIchorPouch.PLAYER_X + x * 18, ContainerIchorPouch.PLAYER_Y + 58);
        }
        for (int y = 0; y < ItemIchorPouch.ROWS; y++) {
            for (int x = 0; x < ItemIchorPouch.COLUMNS; x++) {
                for (int dx = 0; dx < slot; dx++) {
                    for (int dy = 0; dy < slot; dy++) {
                        int argb = image.getRGB(12 + x * 18 + dx, 8 + y * 18 + dy);
                        int r = argb >> 16 & 255, g = argb >> 8 & 255, b = argb & 255;
                        assertEquals(255, argb >>> 24, "pouch cell " + x + "," + y + " is opaque");
                        assertTrue(!(r == g && g == b) || r < 20, "pouch cell " + x + "," + y + " is leather, not panel grey");
                    }
                }
            }
        }
        // The overlap itself, as in the original: the leather rim is opaque brown at the left of the panel and the panel's frame is
        // neutral (white) at the same height (a pixel row below the pouch cells, y = 200).
        int rim = image.getRGB(20, 200);
        int panel = image.getRGB(100, 200);
        assertTrue((rim >> 16 & 255) > (rim & 255) && (rim >>> 24) == 255, "leather rim at (20, 200)");
        assertEquals(panel >> 16 & 255, panel & 255, "panel frame at (100, 200) is neutral");
        assertEquals(255, panel >>> 24);
    }

    private static void assertPanelCell(java.awt.image.BufferedImage image, int x, int y) {
        // The centre of a panel cell is the flat slot grey #8b8b8b of the vanilla slot texture.
        int argb = image.getRGB(x + 8, y + 8);
        assertEquals(0xFF8B8B8B, argb, "panel cell at " + x + "," + y);
    }

    private static byte[] read(String path) throws IOException {
        try (InputStream in = KamiGearResourcesTest.class.getResourceAsStream(path)) {
            assertNotNull(in, path);
            return in.readAllBytes();
        }
    }

    // ---- recipe, tags, language ----------------------------------------------------------------------------------

    @Test
    void theNuggetRecipeTurnsOneIchoriumIntoNineNuggets() {
        JsonObject recipe = json("/data/" + NS + "/recipe/ichor_nuggets.json");
        assertEquals("minecraft:crafting_shapeless", recipe.get("type").getAsString());
        JsonArray ingredients = recipe.getAsJsonArray("ingredients");
        assertEquals(1, ingredients.size());
        assertEquals(NS + ":" + LibItemNames.KAMI_RESOURCE_NAMES[2],
                ingredients.get(0).getAsJsonObject().get("item").getAsString());
        JsonObject result = recipe.getAsJsonObject("result");
        assertEquals(NS + ":" + LibItemNames.KAMI_RESOURCE_NAMES[3], result.get("id").getAsString());
        assertEquals(9, result.get("count").getAsInt());
    }

    @Test
    void theToolAndArmourTagsNameOnlyItemsOfTheGroupAndAreOptional() {
        String[][] tags = { { "head_armor", LibItemNames.ICHOR_HELM, LibItemNames.ICHOR_HELM_GEM },
                { "chest_armor", LibItemNames.ICHOR_CHEST, LibItemNames.ICHOR_CHEST_GEM },
                { "leg_armor", LibItemNames.ICHOR_LEGS, LibItemNames.ICHOR_LEGS_GEM },
                { "foot_armor", LibItemNames.ICHOR_BOOTS, LibItemNames.ICHOR_BOOTS_GEM },
                { "pickaxes", LibItemNames.ICHOR_PICK, LibItemNames.ICHOR_PICK_GEM },
                { "shovels", LibItemNames.ICHOR_SHOVEL, LibItemNames.ICHOR_SHOVEL_GEM },
                { "axes", LibItemNames.ICHOR_AXE, LibItemNames.ICHOR_AXE_GEM },
                { "swords", LibItemNames.ICHOR_SWORD, LibItemNames.ICHOR_SWORD_GEM } };
        for (String[] tag : tags) {
            List<String> listed = new ArrayList<>();
            // The add-on's own copy of the vanilla tag; the loader merges it into the vanilla one.
            JsonObject definition = json("/data/minecraft/tags/item/" + tag[0] + ".json");
            assertTrue(!definition.has("replace") || !definition.get("replace").getAsBoolean(),
                    tag[0] + " adds to the vanilla tag, it does not replace it");
            for (JsonElement value : definition.getAsJsonArray("values")) {
                // With the Kami option off the items do not exist: the entries are optional, so the tag still loads.
                if (value.isJsonObject()) {
                    assertEquals(false, value.getAsJsonObject().get("required").getAsBoolean(), tag[0]);
                    listed.add(value.getAsJsonObject().get("id").getAsString());
                }
            }
            assertTrue(listed.contains(NS + ":" + tag[1]), tag[0] + " lists " + tag[1]);
            assertTrue(listed.contains(NS + ":" + tag[2]), tag[0] + " lists " + tag[2]);
        }
    }

    @Test
    void thePouchTitleKeyIsInTheMergedLanguageFiles() {
        for (String lang : List.of("en_us", "ru_ru")) {
            JsonObject strings = json("/assets/" + NS + "/lang/" + lang + ".json");
            assertTrue(strings.has("container.thaumictinkerer.ichor_pouch"), lang);
            assertTrue(strings.has("item.thaumictinkerer.ichor_pouch"), lang);
        }
    }

    @Test
    void everyItemHasAnEnglishAndARussianName() {
        for (String lang : List.of("en_us", "ru_ru")) {
            JsonObject strings = json("/assets/" + NS + "/lang/" + lang + ".json");
            for (String item : ITEMS) {
                assertTrue(strings.has("item." + NS + "." + item), lang + " name of " + item);
            }
        }
    }

    @Test
    void theWandPartsHaveNames() {
        // WandCap/WandRod naming: item.Wand.<TAG>.cap and item.Wand.<TAG>.rod with the 1.7.10 tags.
        JsonObject strings = json("/assets/" + NS + "/lang/en_us.json");
        assertEquals("Ichorium Adorned", strings.get("item.Wand.ICHOR.cap").getAsString());
        assertEquals("Ichor Woven Silverwood", strings.get("item.Wand.ICHORCLOTH.rod").getAsString());
    }

    // ---- the registrar -------------------------------------------------------------------------------------------

    /**
     * Fabric's payload registry throws "Packet type ... is already registered!" when a client registers an S2C payload type
     * twice, and Architectury registers the type of an S2C payload with its client receiver. The registrar therefore registers
     * the types itself on a dedicated server only; this test reads the source because a client cannot be started here.
     */
    @Test
    void theServerPayloadTypesAreRegisteredOnTheDedicatedServerOnly() throws IOException {
        java.nio.file.Path file = java.nio.file.Path.of("common", "src", "main", "java", "thaumic", "tinkerer", "common",
                "content", "KamiGear.java");
        String text = java.nio.file.Files.readString(file);
        int guard = text.indexOf("if (Platform.getEnvironment() == Env.SERVER) {");
        assertTrue(guard >= 0, "the guard is there");
        int end = text.indexOf("}", guard);
        int calls = 0;
        for (int at = text.indexOf("registerS2CPayloadType("); at >= 0; at = text.indexOf("registerS2CPayloadType(", at + 1)) {
            calls++;
            assertTrue(at > guard && at < end, "an S2C payload type is registered outside the server guard at offset " + at);
        }
        assertEquals(2, calls, "the armour status and the beam");
    }
}
