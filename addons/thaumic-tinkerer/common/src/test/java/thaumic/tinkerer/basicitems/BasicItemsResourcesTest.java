/**
 * New in the 1.21.1 port of Thaumic Tinkerer (no 1.7.10 counterpart: tests). Distributed under the same licence as the
 * add-on (CC BY-NC-SA 3.0, see addons/thaumic-tinkerer/NOTICE.md).
 */
package thaumic.tinkerer.basicitems;

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
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import thaumic.tinkerer.client.BasicItemsClient;
import thaumic.tinkerer.common.lib.LibItemNames;
import thaumic.tinkerer.common.lib.LibResearch;

/**
 * Test-only code of the port (no 1.7.10 counterpart), CC BY-NC-SA 3.0 like the add-on.
 *
 * <p>The data of the BasicItems group: every item has a model whose textures exist and whose overrides use the predicate
 * the client registers, the armour layer of the helmet is a 64 x 32 PNG where the armour material looks for it, the three
 * recipe files are the types the group registers and point at registered items, the tag files add only registered items,
 * and every language key the items, the tooltips and the research entries use exists in the merged language files.</p>
 */
class BasicItemsResourcesTest {
    private static final String NS = "thaumictinkerer";

    /** The six items of the group, in the registration order of {@code BasicItems.register()}. */
    private static final List<String> ITEMS = List.of(LibItemNames.SPELL_CLOTH, LibItemNames.CLEANSING_TALISMAN,
            LibItemNames.XP_TALISMAN, LibItemNames.SHARE_BOOK, LibItemNames.REVEALING_HELM,
            LibItemNames.INFUSED_INKWELL);

    private static JsonObject json(String path) {
        try (InputStream in = BasicItemsResourcesTest.class.getResourceAsStream(path)) {
            assertNotNull(in, "missing resource " + path);
            return JsonParser.parseReader(new InputStreamReader(in, StandardCharsets.UTF_8)).getAsJsonObject();
        } catch (IOException e) {
            throw new AssertionError(path, e);
        }
    }

    private static boolean exists(String path) {
        return BasicItemsResourcesTest.class.getResource(path) != null;
    }

    private static String own(String id) {
        return id.startsWith(NS + ":") ? id.substring(NS.length() + 1) : null;
    }

    private static int[] pngSize(String path) {
        try (InputStream in = BasicItemsResourcesTest.class.getResourceAsStream(path)) {
            assertNotNull(in, "missing resource " + path);
            ByteBuffer header = ByteBuffer.wrap(in.readNBytes(24));
            assertEquals(0x89504E47, header.getInt(0), path + " is a PNG");
            return new int[] { header.getInt(16), header.getInt(20) };
        } catch (IOException e) {
            throw new AssertionError(path, e);
        }
    }

    // ---- models --------------------------------------------------------------------------------------------------

    @Test
    void everyItemHasAModelWithAnExistingTexture() {
        for (String item : ITEMS) {
            JsonObject model = json("/assets/" + NS + "/models/item/" + item + ".json");
            assertTrue(model.has("parent"), item + " has a parent");
            for (Map.Entry<String, JsonElement> texture : model.getAsJsonObject("textures").entrySet()) {
                String path = own(texture.getValue().getAsString());
                assertNotNull(path, item + " uses a texture of the add-on: " + texture.getValue());
                assertTrue(exists("/assets/" + NS + "/textures/" + path + ".png"), item + ": texture " + path);
            }
        }
    }

    @Test
    void theTalismansSwitchTheirModelWithThePredicateTheClientRegisters() {
        for (String talisman : List.of(LibItemNames.CLEANSING_TALISMAN, LibItemNames.XP_TALISMAN)) {
            JsonObject model = json("/assets/" + NS + "/models/item/" + talisman + ".json");
            JsonArray overrides = model.getAsJsonArray("overrides");
            assertEquals(1, overrides.size(), talisman + " has one override");
            JsonObject override = overrides.get(0).getAsJsonObject();
            assertEquals(List.of(NS + ":" + BasicItemsClient.PREDICATE_ENABLED),
                    List.copyOf(override.getAsJsonObject("predicate").keySet()), talisman);
            assertEquals(1.0, override.getAsJsonObject("predicate").get(NS + ":enabled").getAsDouble(), 0.0);

            String target = own(override.get("model").getAsString());
            assertEquals("item/" + talisman + "_1", target, talisman + " points at its switched-on model");
            JsonObject on = json("/assets/" + NS + "/models/" + target + ".json");
            String texture = own(on.getAsJsonObject("textures").get("layer0").getAsString());
            assertEquals("item/" + talisman + "_1", texture, "the switched-on texture is the 1.7.10 icon 1");
            assertTrue(exists("/assets/" + NS + "/textures/" + texture + ".png"));

            // the default model shows icon 0
            String off = own(model.getAsJsonObject("textures").get("layer0").getAsString());
            assertEquals("item/" + talisman + "_0", off);
        }
    }

    @Test
    void theSpellClothIsTintedSoItHasAPlainGeneratedModel() {
        JsonObject model = json("/assets/" + NS + "/models/item/spell_cloth.json");
        assertEquals("minecraft:item/generated", model.get("parent").getAsString());
        assertEquals(NS + ":item/spell_cloth", model.getAsJsonObject("textures").get("layer0").getAsString());
        assertTrue(!model.has("overrides"));
    }

    // ---- the helmet ------------------------------------------------------------------------------------------------

    @Test
    void theHelmetsWornLayerIsTheOriginalTextureWhereTheArmourMaterialLooksForIt() {
        // ArmorMaterial.Layer(thaumictinkerer:revealing) -> textures/models/armor/revealing_layer_1.png
        int[] layer = pngSize("/assets/" + NS + "/textures/models/armor/revealing_layer_1.png");
        assertEquals(64, layer[0], "width of a humanoid armour layer");
        assertEquals(32, layer[1], "height of a humanoid armour layer");
        int[] original = pngSize("/assets/" + NS + "/textures/models/revealing.png");
        assertEquals(original[0], layer[0]);
        assertEquals(original[1], layer[1]);
    }

    // ---- recipes ---------------------------------------------------------------------------------------------------

    @Test
    void theSpellClothRecipeIsTheSpecialTypeOfTheGroup() {
        JsonObject recipe = json("/data/" + NS + "/recipe/spell_cloth.json");
        assertEquals(NS + ":spell_cloth", recipe.get("type").getAsString(), "BasicItems.SPELL_CLOTH_SERIALIZER");
        assertEquals("misc", recipe.get("category").getAsString());
    }

    @Test
    void theTomeRecipeIsTheShapedRecipeOfTheOriginalWithTheConfigGate() {
        JsonObject recipe = json("/data/" + NS + "/recipe/share_tome.json");
        assertEquals(NS + ":share_tome", recipe.get("type").getAsString(), "BasicItems.SHARE_TOME_SERIALIZER");
        JsonArray pattern = recipe.getAsJsonArray("pattern");
        assertEquals(" S ", pattern.get(0).getAsString());
        assertEquals("PTP", pattern.get(1).getAsString());
        assertEquals(" P ", pattern.get(2).getAsString());
        JsonObject key = recipe.getAsJsonObject("key");
        assertEquals("thaumcraft:inkwell", key.getAsJsonObject("S").get("item").getAsString(), "Scribing Tools");
        assertEquals("thaumcraft:thaumonomicon", key.getAsJsonObject("T").get("item").getAsString());
        assertEquals("minecraft:paper", key.getAsJsonObject("P").get("item").getAsString());
        assertEquals(NS + ":" + LibItemNames.SHARE_BOOK, recipe.getAsJsonObject("result").get("id").getAsString());
        assertEquals(1, recipe.getAsJsonObject("result").get("count").getAsInt());
    }

    @Test
    void theInkwellRecipeRefillsToolsOfAnyWearWithEightInkSacs() {
        JsonObject recipe = json("/data/" + NS + "/recipe/infused_inkwell0.json");
        assertEquals("minecraft:crafting_shaped", recipe.get("type").getAsString());
        JsonArray pattern = recipe.getAsJsonArray("pattern");
        assertEquals("QQQ", pattern.get(0).getAsString());
        assertEquals("QCQ", pattern.get(1).getAsString());
        assertEquals("QQQ", pattern.get(2).getAsString());
        JsonObject key = recipe.getAsJsonObject("key");
        assertEquals("minecraft:ink_sac", key.getAsJsonObject("Q").get("item").getAsString());
        assertEquals(NS + ":" + LibItemNames.INFUSED_INKWELL, key.getAsJsonObject("C").get("item").getAsString());
        assertEquals(NS + ":" + LibItemNames.INFUSED_INKWELL, recipe.getAsJsonObject("result").get("id").getAsString());
    }

    // ---- tags ------------------------------------------------------------------------------------------------------

    private static List<String> tagValues(String path) {
        JsonObject tag = json(path);
        assertEquals(false, tag.get("replace").getAsBoolean(), path + " adds to the tag, it does not replace it");
        return tag.getAsJsonArray("values").asList().stream().map(value -> value.isJsonObject()
                ? value.getAsJsonObject().get("id").getAsString() : value.getAsString()).toList();
    }

    @Test
    void theTagsAddOnlyItemsOfTheGroupAndTheBloodSword() {
        List<String> registered = List.of(NS + ":" + LibItemNames.REVEALING_HELM, NS + ":" + LibItemNames.CLEANSING_TALISMAN,
                NS + ":" + LibItemNames.BLOOD_SWORD);
        for (String path : List.of("/data/thaumcraft/tags/item/enchantable/repair.json",
                "/data/minecraft/tags/item/enchantable/head_armor.json",
                "/data/minecraft/tags/item/enchantable/durability.json")) {
            // The tag files are shared with the other groups, which add their own items (and the Thaumcraft blades that carry
            // the Blood Sword's enchantability): only an add-on item of another group's list would be an error here.
            for (String id : tagValues(path)) {
                assertTrue(registered.contains(id) || !id.startsWith(NS + ":"), path + " names " + id);
            }
        }
        assertTrue(tagValues("/data/thaumcraft/tags/item/enchantable/repair.json")
                .contains(NS + ":" + LibItemNames.REVEALING_HELM), "the helmet takes the Repair enchantment (IRepairable)");
        assertEquals(List.of(NS + ":" + LibItemNames.REVEALING_HELM),
                tagValues("/data/minecraft/tags/item/enchantable/head_armor.json"), "armour enchantments for the helmet");
        // vanilla's durability tag lists #minecraft:head_armor, not the enchantable one, so the helmet is added here too
        assertTrue(tagValues("/data/minecraft/tags/item/enchantable/durability.json")
                .containsAll(List.of(NS + ":" + LibItemNames.REVEALING_HELM, NS + ":" + LibItemNames.CLEANSING_TALISMAN)),
                "Unbreaking for the helmet and for the talisman (enchantability 10)");
    }

    // ---- language ----------------------------------------------------------------------------------------------------

    @Test
    void everyLanguageKeyOfTheGroupExistsInEnglishAndRussian() {
        List<String> keys = new java.util.ArrayList<>(List.of("ttmisc.active", "ttmisc.inactive", "ttmisc.full",
                "ttmisc.absorbing", "ttmisc.notAbsorbing", "ttmisc.xpTalisman.xp", "ttmisc.shareTome.write",
                "ttmisc.shareTome.sync", "ttmisc.shareTome.noAssign", "ttmisc.shareTome.playerName",
                "ttmisc.focusDislocation.tooltip", "ttmisc.focusDislocation.tooltipExtra"));
        for (String item : ITEMS) keys.add("item." + NS + "." + item);
        for (String research : List.of(LibResearch.KEY_SPELL_CLOTH, LibResearch.KEY_CLEANSING_TALISMAN,
                LibResearch.KEY_XP_TALISMAN, LibResearch.KEY_REVEALING_HELM, LibResearch.KEY_SHARE_TOME)) {
            keys.add("tc.research_name." + research);
            keys.add("tc.research_text." + research);
            keys.add("ttresearch.page." + research + ".0");
        }
        for (String lang : List.of("en_us", "ru_ru")) {
            JsonObject table = json("/assets/" + NS + "/lang/" + lang + ".json");
            for (String key : keys) {
                assertTrue(table.has(key) && !table.get(key).getAsString().isEmpty(), lang + " has " + key);
            }
        }
        // the two formats that take an argument
        JsonObject en = json("/assets/" + NS + "/lang/en_us.json");
        assertTrue(en.get("ttmisc.xpTalisman.xp").getAsString().contains("%s"));
        assertTrue(en.get("ttmisc.shareTome.playerName").getAsString().contains("%s"));
    }

    @Test
    void thePeripheralsPageNoLongerPromisesTheEnterShortcutOrTheWebPage() {
        // GuiResearchPeripheral (Enter opened a web address) is not ported, so no language may still say it does.
        for (String lang : List.of("en_us", "ru_ru", "de_de", "fr_fr", "ko_kr", "zh_cn")) {
            JsonObject table = json("/assets/" + NS + "/lang/" + lang + ".json");
            assertTrue(table.has("ttresearch.page.PERIPHERALS.0"), lang + " has the page");
            String text = table.get("ttresearch.page.PERIPHERALS.0").getAsString();
            for (String promise : List.of("ENTER", "ENTRÉE", "vazkii.us", "回车")) {
                assertTrue(!text.contains(promise), lang + " still says " + promise + ": " + text);
            }
            assertTrue(text.contains("ComputerCraft"), lang + " still names ComputerCraft");
        }
    }
}
