/**
 * New in the 1.21.1 port of Thaumic Tinkerer (no 1.7.10 counterpart, see addons/thaumic-tinkerer/NOTICE.md): test-only
 * code, distributed under the same licence as the add-on (CC BY-NC-SA 3.0).
 */
package thaumic.tinkerer.machines;

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
import thaumcraft.api.aspects.Aspect;
import thaumic.tinkerer.common.item.ItemMobAspect;

/**
 * <p>The data of the Machines group: every blockstate, model, loot table, tag, recipe, texture and language key that the
 * code and the research refer to exists and points at something that exists.</p>
 */
class MachinesResourcesTest {
    private static final String NS = "thaumictinkerer";
    private static final List<String> BLOCKS = List.of("aspect_analyzer", "repairer", "funnel", "magnet", "mob_magnet", "spawner");

    private static JsonObject json(String path) {
        try (InputStream in = MachinesResourcesTest.class.getResourceAsStream(path)) {
            assertNotNull(in, "missing resource " + path);
            return JsonParser.parseReader(new InputStreamReader(in, StandardCharsets.UTF_8)).getAsJsonObject();
        } catch (IOException e) {
            throw new AssertionError(path, e);
        }
    }

    private static boolean exists(String path) {
        return MachinesResourcesTest.class.getResource(path) != null;
    }

    private static String stripNamespace(String id, String namespace) {
        return id.startsWith(namespace + ":") ? id.substring(namespace.length() + 1) : null;
    }

    // ---- blocks --------------------------------------------------------------------------------------------------

    @Test
    void everyBlockHasAStateAnItemModelAndALootTable() {
        for (String block : BLOCKS) {
            assertTrue(exists("/assets/" + NS + "/blockstates/" + block + ".json"), "blockstate " + block);
            assertTrue(exists("/assets/" + NS + "/models/item/" + block + ".json"), "item model " + block);
            JsonObject loot = json("/data/" + NS + "/loot_table/blocks/" + block + ".json");
            assertEquals("minecraft:block", loot.get("type").getAsString());
            assertEquals(NS + ":" + block, loot.getAsJsonArray("pools").get(0).getAsJsonObject().getAsJsonArray("entries")
                    .get(0).getAsJsonObject().get("name").getAsString(), "a block drops itself: " + block);
        }
    }

    @Test
    void everyModelOfABlockstateExistsAndEveryTextureOfItsModelExists() {
        List<String> models = new ArrayList<>();
        for (String block : BLOCKS) {
            JsonObject variants = json("/assets/" + NS + "/blockstates/" + block + ".json").getAsJsonObject("variants");
            for (Map.Entry<String, JsonElement> variant : variants.entrySet()) {
                models.add(variant.getValue().getAsJsonObject().get("model").getAsString());
            }
        }
        assertTrue(models.size() >= 10, "the repairer has four facings, the magnets two polarities each");
        for (String model : models) {
            String path = stripNamespace(model, NS);
            assertNotNull(path, model);
            JsonObject definition = json("/assets/" + NS + "/models/" + path + ".json");
            for (Map.Entry<String, JsonElement> texture : definition.getAsJsonObject("textures").entrySet()) {
                String id = texture.getValue().getAsString();
                String own = stripNamespace(id, NS);
                if (own != null) {
                    assertTrue(exists("/assets/" + NS + "/textures/" + own + ".png"), model + " uses texture " + id);
                }
            }
        }
    }

    @Test
    void theTexturesOfTheEntityStyleModelsAreStitchedIntoTheBlockAtlas() {
        JsonArray sources = json("/assets/minecraft/atlases/blocks.json").getAsJsonArray("sources");
        List<String> stitched = new ArrayList<>();
        // Only the "single" sources name a texture; the file also carries a "filter" source (the wand depth layers, Foci group).
        for (JsonElement source : sources) {
            if (source.getAsJsonObject().has("resource")) stitched.add(source.getAsJsonObject().get("resource").getAsString());
        }
        for (String texture : List.of("repairer", "magnet_n", "magnet_s", "mob_magnet_n", "mob_magnet_s")) {
            assertTrue(stitched.contains(NS + ":models/" + texture), texture);
            assertTrue(exists("/assets/" + NS + "/textures/models/" + texture + ".png"), texture);
        }
    }

    @Test
    void theBlocksAreMinedWithTheToolsOfTheOriginalMaterials() {
        List<String> axe = values("/data/" + NS + "/tags/block/mineable/machines/axe.json");
        List<String> pickaxe = values("/data/" + NS + "/tags/block/mineable/machines/pickaxe.json");
        assertTrue(axe.contains(NS + ":aspect_analyzer"), "wood");
        for (String block : List.of("repairer", "funnel", "magnet", "mob_magnet", "spawner")) {
            assertTrue(pickaxe.contains(NS + ":" + block), block);
        }
    }

    private static List<String> values(String path) {
        List<String> values = new ArrayList<>();
        for (JsonElement value : json(path).getAsJsonArray("values")) values.add(value.getAsString());
        return values;
    }

    // ---- items ---------------------------------------------------------------------------------------------------

    @Test
    void everySoulAspectItemHasAModelAndATexture() {
        for (Aspect aspect : ItemMobAspect.ASPECTS) {
            for (ItemMobAspect.Form form : ItemMobAspect.Form.values()) {
                String path = ItemMobAspect.path(aspect, form);
                JsonObject model = json("/assets/" + NS + "/models/item/" + path + ".json");
                String texture = stripNamespace(model.getAsJsonObject("textures").get("layer0").getAsString(), NS);
                assertNotNull(texture, path);
                assertTrue(exists("/assets/" + NS + "/textures/" + texture + ".png"), path + " -> " + texture);
                String expected = form == ItemMobAspect.Form.CONDENSED ? aspect.getTag() + "_condensed" : aspect.getTag();
                assertEquals("item/" + expected, texture, path);
            }
        }
    }

    @Test
    void theBloodSwordShowsItsActiveIconThroughTheCustomModelData() {
        JsonObject sword = json("/assets/" + NS + "/models/item/blood_sword.json");
        JsonObject override = sword.getAsJsonArray("overrides").get(0).getAsJsonObject();
        assertEquals(1, override.getAsJsonObject("predicate").get("custom_model_data").getAsInt());
        String active = stripNamespace(override.get("model").getAsString(), NS);
        JsonObject activeModel = json("/assets/" + NS + "/models/" + active + ".json");
        assertEquals(NS + ":item/blood_sword_1", activeModel.getAsJsonObject("textures").get("layer0").getAsString());
        assertEquals(NS + ":item/blood_sword_0", sword.getAsJsonObject("textures").get("layer0").getAsString());
        assertTrue(exists("/assets/" + NS + "/models/item/soul_mould.json"));
        assertTrue(exists("/assets/" + NS + "/models/item/mob_display.json"));
    }

    @Test
    void nineSoulAspectsMakeOneCondensedAndTheResearchPageShowsTheLastRecipe() {
        List<Aspect> aspects = ItemMobAspect.ASPECTS;
        for (int i = 0; i < aspects.size(); i++) {
            Aspect aspect = aspects.get(i);
            String name = i == aspects.size() - 1 ? ItemMobAspect.CONDENSE_RECIPE_NAME : "summon1_" + aspect.getTag();
            JsonObject recipe = json("/data/" + NS + "/recipe/" + name + ".json");
            assertEquals("minecraft:crafting_shaped", recipe.get("type").getAsString());
            assertEquals(3, recipe.getAsJsonArray("pattern").size());
            for (JsonElement row : recipe.getAsJsonArray("pattern")) assertEquals("XXX", row.getAsString());
            assertEquals(NS + ":" + ItemMobAspect.path(aspect, ItemMobAspect.Form.BASIC),
                    recipe.getAsJsonObject("key").getAsJsonObject("X").get("item").getAsString());
            assertEquals(NS + ":" + ItemMobAspect.path(aspect, ItemMobAspect.Form.CONDENSED),
                    recipe.getAsJsonObject("result").get("id").getAsString());
            assertEquals(1, recipe.getAsJsonObject("result").get("count").getAsInt());
        }
        // the page of the necromancy research shows the last registered recipe: the crop (messis) one
        JsonObject last = json("/data/" + NS + "/recipe/" + ItemMobAspect.CONDENSE_RECIPE_NAME + ".json");
        assertEquals(NS + ":mob_aspect_messis", last.getAsJsonObject("key").getAsJsonObject("X").get("item").getAsString());
    }

    // ---- language ------------------------------------------------------------------------------------------------

    @Test
    void everyKeyTheCodeAndTheResearchUseIsTranslated() {
        List<String> keys = new ArrayList<>(List.of(
                "block.thaumictinkerer.aspect_analyzer", "block.thaumictinkerer.repairer", "block.thaumictinkerer.funnel",
                "block.thaumictinkerer.magnet", "block.thaumictinkerer.mob_magnet", "block.thaumictinkerer.spawner",
                "item.thaumictinkerer.soul_mould", "item.thaumictinkerer.blood_sword", "item.thaumictinkerer.mob_aspect",
                "item.thaumictinkerer.mob_display", "item.mobAspect.condensed.name", "item.mobAspect.infused.name",
                "ttmisc.adult", "ttmisc.child", "ttmisc.all", "ttmisc.none", "ttmisc.soulMould.nonAssigned",
                "ttmisc.soulMould.pattern", "ttmisc.bloodSword.activateEssentiaHarvest",
                "ttmisc.bloodSword.deactivateEssentiaHarvest"));
        for (String research : List.of("ASPECT_ANALYZER", "REPAIRER", "FUNNEL", "MAGNETS", "SUMMON", "BLOOD_SWORD",
                "PERIPHERALS")) {
            keys.add("tc.research_name." + research);
            keys.add("tc.research_text." + research);
        }
        // pages: the text pages of the research entries (the arcane / infusion pages are recipes)
        for (String page : List.of("ASPECT_ANALYZER.0", "REPAIRER.0", "FUNNEL.0", "MAGNETS.0", "MAGNETS.1", "SUMMON.0",
                "SUMMON.1", "BLOOD_SWORD.0", "BLOOD_SWORD.1", "PERIPHERALS.0")) {
            keys.add("ttresearch.page." + page);
        }
        for (String language : List.of("en_us", "ru_ru")) {
            JsonObject lang = json("/assets/" + NS + "/lang/" + language + ".json");
            for (String key : keys) assertTrue(lang.has(key), language + " lacks " + key);
        }
    }
}
