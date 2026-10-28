package thaumic.tinkerer.simpleblocks;

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
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;

/**
 * Test-only code of the port (no 1.7.10 counterpart), CC BY-NC-SA 3.0 like the add-on.
 *
 * <p>The data of the SimpleBlocks group is complete and consistent: every block has a blockstate whose models and
 * textures exist, the blocks with an item have an item model, the blocks that drop have a loot table, the tags and
 * the recipes name only what exists, and every text the group shows exists in English and Russian. Block and item
 * paths are written out here on purpose: a path changed in the registrar must be changed in the data too.</p>
 */
class SimpleBlocksResourcesTest {
    private static final String NS = "thaumictinkerer";

    private static final List<String> BLOCKS_WITH_ITEM = List.of("dark_quartz", "dark_quartz_chiseled", "dark_quartz_pillar",
            "dark_quartz_slab", "dark_quartz_stairs", "travel_slab", "travel_stairs", "ward_slab", "platform", "fire_air",
            "fire_water", "fire_earth", "fire_fire", "fire_order", "fire_chaos");
    private static final List<String> BLOCKS_WITHOUT_ITEM = List.of("gaseous_light", "gaseous_shadow", "nitor_gas", "forcefield",
            "infused_farmland", "infused_grain_block");
    private static final List<String> BLOCKS_WITH_LOOT = List.of("dark_quartz", "dark_quartz_chiseled", "dark_quartz_pillar",
            "dark_quartz_slab", "dark_quartz_stairs", "travel_slab", "travel_stairs", "ward_slab", "platform", "infused_farmland");
    private static final List<String> FIRES = List.of("fire_air", "fire_water", "fire_earth", "fire_fire", "fire_order", "fire_chaos");
    private static final List<String> FLAT_ITEMS = List.of("dark_quartz_item", "gaseous_light_item", "gaseous_shadow_item",
            "gas_remover", "bright_nitor", "infused_grain_aer", "infused_grain_ignis", "infused_grain_terra", "infused_grain_aqua",
            "infused_potion_aer", "infused_potion_ignis", "infused_potion_terra", "infused_potion_aqua", "infused_seeds");
    /** The names of the crop and seed textures by texture group: aer, ignis, aqua, terra, ordo, perditio, generic. */
    private static final List<String> GROUPS = List.of("aer", "ignis", "aqua", "terra", "ordo", "perditio", "generic");

    /** The research of the group and the number of text pages of each. */
    private static final Map<String, Integer> RESEARCH_PAGES = Map.ofEntries(Map.entry("PLATFORM", 1), Map.entry("DARK_QUARTZ", 1),
            Map.entry("FIRE_AER", 1), Map.entry("FIRE_AQUA", 1), Map.entry("FIRE_IGNIS", 1), Map.entry("FIRE_ORDO", 1),
            Map.entry("FIRE_PERDITIO", 1), Map.entry("FIRE_TERRA", 1), Map.entry("GASEOUS_LIGHT", 1), Map.entry("GASEOUS_SHADOW", 1),
            Map.entry("GAS_REMOVER", 1), Map.entry("BRIGHT_NITOR", 1), Map.entry("INFUSED_POTIONS", 6));

    private static final List<String> RECIPES = List.of("dark_quartz_0", "dark_quartz_1", "dark_quartz_2", "dark_quartz_3",
            "dark_quartz_4", "dark_quartz_5", "pave_travel_slab", "pave_travel_stairs", "pave_ward_slab");

    // ---- helpers ---------------------------------------------------------------------------------------------------

    private static JsonObject json(String resource) throws IOException {
        try (InputStream in = SimpleBlocksResourcesTest.class.getClassLoader().getResourceAsStream(resource)) {
            assertNotNull(in, "missing resource " + resource);
            JsonElement parsed = JsonParser.parseReader(new InputStreamReader(in, StandardCharsets.UTF_8));
            return parsed.getAsJsonObject();
        }
    }

    private static boolean exists(String resource) {
        return SimpleBlocksResourcesTest.class.getClassLoader().getResource(resource) != null;
    }

    private static String path(String id) {
        return id.contains(":") ? id.substring(id.indexOf(':') + 1) : id;
    }

    private static boolean ours(String id) {
        return id.startsWith(NS + ":");
    }

    /** Every model a blockstate file names. */
    private static Set<String> modelsOf(JsonObject state) {
        Set<String> models = new HashSet<>();
        if (state.has("variants")) {
            for (Map.Entry<String, JsonElement> variant : state.getAsJsonObject("variants").entrySet()) {
                JsonElement value = variant.getValue();
                if (value.isJsonArray()) value.getAsJsonArray().forEach(e -> models.add(e.getAsJsonObject().get("model").getAsString()));
                else models.add(value.getAsJsonObject().get("model").getAsString());
            }
        }
        if (state.has("multipart")) {
            for (JsonElement part : state.getAsJsonArray("multipart")) {
                JsonElement apply = part.getAsJsonObject().get("apply");
                if (apply.isJsonArray()) apply.getAsJsonArray().forEach(e -> models.add(e.getAsJsonObject().get("model").getAsString()));
                else models.add(apply.getAsJsonObject().get("model").getAsString());
            }
        }
        return models;
    }

    /** Checks the model, its parent chain inside the add-on and its textures inside the add-on. */
    private static void checkModel(String id, String owner, List<String> problems) throws IOException {
        if (!ours(id)) return;
        String resource = "assets/" + NS + "/models/" + path(id) + ".json";
        if (!exists(resource)) {
            problems.add(owner + ": model " + id);
            return;
        }
        JsonObject model = json(resource);
        if (model.has("parent")) checkModel(model.get("parent").getAsString(), owner, problems);
        if (model.has("textures")) {
            for (Map.Entry<String, JsonElement> texture : model.getAsJsonObject("textures").entrySet()) {
                String value = texture.getValue().getAsString();
                if (ours(value) && !exists("assets/" + NS + "/textures/" + path(value) + ".png")) {
                    problems.add(owner + ": texture " + value + " of " + id);
                }
            }
        }
        if (model.has("overrides")) {
            for (JsonElement override : model.getAsJsonArray("overrides")) {
                checkModel(override.getAsJsonObject().get("model").getAsString(), owner, problems);
            }
        }
    }

    // ---- blocks ----------------------------------------------------------------------------------------------------

    @Test
    void everyBlockHasABlockstateWhoseModelsAndTexturesExist() throws IOException {
        List<String> problems = new ArrayList<>();
        List<String> all = new ArrayList<>(BLOCKS_WITH_ITEM);
        all.addAll(BLOCKS_WITHOUT_ITEM);
        for (String block : all) {
            String resource = "assets/" + NS + "/blockstates/" + block + ".json";
            if (!exists(resource)) {
                problems.add("blockstate " + block);
                continue;
            }
            for (String model : modelsOf(json(resource))) checkModel(model, block, problems);
        }
        assertEquals(List.of(), problems);
    }

    @Test
    void everyBlockWithAnItemHasAnItemModel() throws IOException {
        List<String> problems = new ArrayList<>();
        for (String block : BLOCKS_WITH_ITEM) checkModel(NS + ":item/" + block, block, problems);
        for (String item : FLAT_ITEMS) checkModel(NS + ":item/" + item, item, problems);
        assertEquals(List.of(), problems);
    }

    @Test
    void theFlatItemsUseTheirOwnTexture() throws IOException {
        for (String item : FLAT_ITEMS) {
            JsonObject model = json("assets/" + NS + "/models/item/" + item + ".json");
            assertEquals("minecraft:item/generated", model.get("parent").getAsString(), item);
            String texture = model.getAsJsonObject("textures").get("layer0").getAsString();
            assertTrue(exists("assets/" + NS + "/textures/" + path(texture) + ".png"), item + " texture " + texture);
        }
    }

    @Test
    void everyBlockThatDropsHasALootTableThatDropsIt() throws IOException {
        for (String block : BLOCKS_WITH_LOOT) {
            JsonObject loot = json("data/" + NS + "/loot_table/blocks/" + block + ".json");
            assertEquals("minecraft:block", loot.get("type").getAsString(), block);
            String drop = loot.getAsJsonArray("pools").get(0).getAsJsonObject().getAsJsonArray("entries").get(0).getAsJsonObject()
                    .get("name").getAsString();
            assertEquals(block.equals("infused_farmland") ? "minecraft:dirt" : NS + ":" + block, drop, block);
        }
    }

    @Test
    void aDoubleSlabDropsTwoSlabs() throws IOException {
        for (String slab : List.of("dark_quartz_slab", "travel_slab", "ward_slab")) {
            JsonObject entry = json("data/" + NS + "/loot_table/blocks/" + slab + ".json").getAsJsonArray("pools").get(0).getAsJsonObject()
                    .getAsJsonArray("entries").get(0).getAsJsonObject();
            JsonObject count = entry.getAsJsonArray("functions").get(0).getAsJsonObject();
            assertEquals("minecraft:set_count", count.get("function").getAsString(), slab);
            assertEquals(2.0, count.get("count").getAsDouble(), slab);
            assertEquals("double", count.getAsJsonArray("conditions").get(0).getAsJsonObject().getAsJsonObject("properties").get("type")
                    .getAsString(), slab);
        }
    }

    // ---- the crops and fires -----------------------------------------------------------------------------------

    @Test
    void theCropShowsFourStagesForEveryTextureGroupAndAge() throws IOException {
        JsonObject variants = json("assets/" + NS + "/blockstates/infused_grain_block.json").getAsJsonObject("variants");
        assertEquals(8 * 7, variants.size());
        // the 1.7.10 mapping: age 0-1 -> picture 0, 2-3 -> 1, 4-5 and 6 -> 2, 7 -> 3
        int[] stage = { 0, 0, 1, 1, 2, 2, 2, 3 };
        for (int age = 0; age < 8; age++) {
            for (int group = 0; group < 7; group++) {
                String key = "age=" + age + ",texture=" + group;
                assertTrue(variants.has(key), key);
                assertEquals(NS + ":block/crop_" + GROUPS.get(group) + "_" + stage[age], variants.getAsJsonObject(key).get("model").getAsString(),
                        key);
            }
        }
    }

    @Test
    void theGenericCropIsTintedAndThePrimalOnesAreNot() throws IOException {
        JsonObject generic = json("assets/" + NS + "/models/block/crop_generic_3.json");
        JsonArray elements = generic.getAsJsonArray("elements");
        assertTrue(elements.size() > 0);
        for (JsonElement element : elements) {
            for (Map.Entry<String, JsonElement> face : element.getAsJsonObject().getAsJsonObject("faces").entrySet()) {
                assertEquals(0, face.getValue().getAsJsonObject().get("tintindex").getAsInt());
            }
        }
        assertEquals("minecraft:block/crop", json("assets/" + NS + "/models/block/crop_aer_3.json").get("parent").getAsString());
    }

    @Test
    void theSeedsPickTheirPictureByTheAspectTexture() throws IOException {
        JsonArray overrides = json("assets/" + NS + "/models/item/infused_seeds.json").getAsJsonArray("overrides");
        assertEquals(7, overrides.size());
        for (int i = 0; i < 7; i++) {
            JsonObject override = overrides.get(i).getAsJsonObject();
            assertEquals((double) i, override.getAsJsonObject("predicate").get(NS + ":aspect_texture").getAsDouble());
            assertEquals(NS + ":item/infused_seeds_" + GROUPS.get(i), override.get("model").getAsString());
        }
    }

    @Test
    void everyFireDrawsTwoLayersInFiveShapes() throws IOException {
        for (String fire : FIRES) {
            for (String shape : List.of("floor", "side", "side_alt", "up", "up_alt")) {
                for (int layer = 0; layer < 2; layer++) {
                    String id = "assets/" + NS + "/models/block/" + fire + "_" + shape + layer + ".json";
                    assertTrue(exists(id), id);
                    String texture = json(id).getAsJsonObject("textures").get("fire").getAsString();
                    assertEquals(NS + ":block/" + fire + "_layer_" + layer, texture);
                    assertTrue(exists("assets/" + NS + "/textures/block/" + fire + "_layer_" + layer + ".png"), texture);
                }
            }
            assertTrue(json("assets/" + NS + "/blockstates/" + fire + ".json").has("multipart"), fire);
        }
    }

    @Test
    void theGasesAndTheForcefieldAreDrawnAsNothing() throws IOException {
        JsonObject model = json("assets/" + NS + "/models/block/gas.json");
        assertTrue(!model.has("elements"), "no elements");
        assertTrue(exists("assets/" + NS + "/textures/block/empty_texture.png"));
    }

    @Test
    void theEffectsHaveTheirIcons() {
        for (String effect : List.of("potion_air", "potion_fire", "potion_earth", "potion_water")) {
            assertTrue(exists("assets/" + NS + "/textures/mob_effect/" + effect + ".png"), effect);
        }
    }

    // ---- tags and recipes --------------------------------------------------------------------------------------

    /**
     * The values of a tag file from every copy on the class path: the vanilla tags ({@code minecraft:fire}) are in the
     * resources of Thaumcraft and of other add-ons too, and the game merges them.
     */
    private static List<String> tag(String resource) throws IOException {
        List<String> values = new ArrayList<>();
        var urls = SimpleBlocksResourcesTest.class.getClassLoader().getResources(resource);
        boolean any = false;
        while (urls.hasMoreElements()) {
            any = true;
            try (InputStream in = urls.nextElement().openStream()) {
                JsonObject parsed = JsonParser.parseReader(new InputStreamReader(in, StandardCharsets.UTF_8)).getAsJsonObject();
                parsed.getAsJsonArray("values").forEach(e -> values.add(e.getAsString()));
            }
        }
        assertTrue(any, "missing resource " + resource);
        return values;
    }

    @Test
    void everyBlockIsMineableWithItsTool() throws IOException {
        List<String> pickaxe = tag("data/" + NS + "/tags/block/mineable/simple_blocks/pickaxe.json");
        List<String> axe = tag("data/" + NS + "/tags/block/mineable/simple_blocks/axe.json");
        List<String> shovel = tag("data/" + NS + "/tags/block/mineable/simple_blocks/shovel.json");
        for (String block : List.of("dark_quartz", "dark_quartz_chiseled", "dark_quartz_pillar", "dark_quartz_slab", "dark_quartz_stairs",
                "travel_slab", "travel_stairs", "ward_slab")) {
            assertTrue(pickaxe.contains(NS + ":" + block), block);
        }
        assertTrue(axe.contains(NS + ":platform"));
        assertTrue(shovel.contains(NS + ":infused_farmland"));
    }

    @Test
    void theFiresSlabsAndStairsAreInTheVanillaTags() throws IOException {
        List<String> fire = tag("data/minecraft/tags/block/fire.json");
        for (String f : FIRES) assertTrue(fire.contains(NS + ":" + f), f);
        for (String kind : List.of("block", "item")) {
            List<String> slabs = tag("data/minecraft/tags/" + kind + "/slabs.json");
            List<String> stairs = tag("data/minecraft/tags/" + kind + "/stairs.json");
            for (String slab : List.of("dark_quartz_slab", "travel_slab", "ward_slab")) assertTrue(slabs.contains(NS + ":" + slab), kind + " " + slab);
            for (String stair : List.of("dark_quartz_stairs", "travel_stairs")) assertTrue(stairs.contains(NS + ":" + stair), kind + " " + stair);
        }
        assertEquals(List.of(NS + ":dark_quartz", NS + ":dark_quartz_chiseled", NS + ":dark_quartz_pillar"),
                tag("data/" + NS + "/tags/item/dark_quartz_blocks.json"));
    }

    @Test
    void everyRecipeNamesItsResultAndOnlyKnownIngredients() throws IOException {
        Set<String> ours = new HashSet<>(List.of("dark_quartz_item", "dark_quartz", "dark_quartz_slab", "dark_quartz_pillar",
                "dark_quartz_chiseled", "dark_quartz_stairs", "travel_slab", "travel_stairs", "ward_slab"));
        for (String recipe : RECIPES) {
            JsonObject json = json("data/" + NS + "/recipe/" + recipe + ".json");
            assertEquals("minecraft:crafting_shaped", json.get("type").getAsString(), recipe);
            String result = json.getAsJsonObject("result").get("id").getAsString();
            assertTrue(ours(result) && ours.contains(path(result)), recipe + " result " + result);
            JsonObject key = json.getAsJsonObject("key");
            Set<Character> used = new HashSet<>();
            for (JsonElement row : json.getAsJsonArray("pattern")) for (char c : row.getAsString().toCharArray()) if (c != ' ') used.add(c);
            for (char c : used) assertTrue(key.has(String.valueOf(c)), recipe + " key " + c);
            for (Map.Entry<String, JsonElement> ingredient : key.entrySet()) {
                List<JsonObject> options = new ArrayList<>();
                if (ingredient.getValue().isJsonArray()) ingredient.getValue().getAsJsonArray().forEach(e -> options.add(e.getAsJsonObject()));
                else options.add(ingredient.getValue().getAsJsonObject());
                for (JsonObject option : options) {
                    if (option.has("item") && ours(option.get("item").getAsString())) {
                        assertTrue(ours.contains(path(option.get("item").getAsString())), recipe + " " + option);
                    }
                    if (option.has("tag") && ours(option.get("tag").getAsString())) {
                        assertTrue(exists("data/" + NS + "/tags/item/" + path(option.get("tag").getAsString()) + ".json"), recipe + " " + option);
                    }
                }
            }
        }
    }

    @Test
    void theQuartzRecipesAreTheOnesOfTheOriginal() throws IOException {
        JsonObject first = json("data/" + NS + "/recipe/dark_quartz_0.json");
        assertEquals(List.of("Q Q", " C ", "Q Q"), strings(first.getAsJsonArray("pattern")));
        assertEquals(4, first.getAsJsonObject("result").get("count").getAsInt());
        assertEquals(6, json("data/" + NS + "/recipe/dark_quartz_2.json").getAsJsonObject("result").get("count").getAsInt());
        assertEquals(2, json("data/" + NS + "/recipe/dark_quartz_3.json").getAsJsonObject("result").get("count").getAsInt());
        assertEquals(4, json("data/" + NS + "/recipe/dark_quartz_5.json").getAsJsonObject("result").get("count").getAsInt());
        assertEquals(6, json("data/" + NS + "/recipe/pave_travel_slab.json").getAsJsonObject("result").get("count").getAsInt());
        assertEquals(4, json("data/" + NS + "/recipe/pave_travel_stairs.json").getAsJsonObject("result").get("count").getAsInt());
        assertEquals(6, json("data/" + NS + "/recipe/pave_ward_slab.json").getAsJsonObject("result").get("count").getAsInt());
    }

    private static List<String> strings(JsonArray array) {
        List<String> list = new ArrayList<>();
        array.forEach(e -> list.add(e.getAsString()));
        return list;
    }

    // ---- texts -------------------------------------------------------------------------------------------------

    private static void require(JsonObject texts, String key, List<String> missing) {
        if (!texts.has(key) || texts.get(key).getAsString().isEmpty()) missing.add(key);
    }

    @Test
    void everyTextOfTheGroupExistsInEnglishAndRussian() throws IOException {
        for (String lang : new String[] { "en_us", "ru_ru" }) {
            JsonObject texts = json("assets/" + NS + "/lang/" + lang + ".json");
            List<String> missing = new ArrayList<>();
            for (String block : BLOCKS_WITH_ITEM) require(texts, "block." + NS + "." + block, missing);
            for (String block : BLOCKS_WITHOUT_ITEM) require(texts, "block." + NS + "." + block, missing);
            // the grain, the potion and the seeds share their keys between variants
            for (String item : List.of("dark_quartz_item", "gaseous_light_item", "gaseous_shadow_item", "gas_remover", "bright_nitor",
                    "infused_grain", "infused_seeds")) {
                require(texts, "item." + NS + "." + item, missing);
            }
            for (String aspect : List.of("Aer", "Ignis", "Terra", "Aqua")) require(texts, "item.infusedPotion." + aspect + ".name", missing);
            for (String effect : List.of("air", "fire", "earth", "water")) require(texts, "effect." + NS + ".potion_" + effect, missing);
            for (Map.Entry<String, Integer> research : RESEARCH_PAGES.entrySet()) {
                require(texts, "tc.research_name." + research.getKey(), missing);
                require(texts, "tc.research_text." + research.getKey(), missing);
                for (int page = 0; page < research.getValue(); page++) {
                    require(texts, "ttresearch.page." + research.getKey() + "." + page, missing);
                }
            }
            assertEquals(List.of(), missing, lang + " lacks texts");
        }
    }

    @Test
    void theRussianFragmentHasEveryKeyOfTheEnglishOne() throws IOException {
        Path en = Path.of("lang-extra", "simple_blocks.en_us.json");
        Path ru = Path.of("lang-extra", "simple_blocks.ru_ru.json");
        assertTrue(Files.isRegularFile(en) && Files.isRegularFile(ru), "the fragments exist (tests run in addons/thaumic-tinkerer)");
        JsonObject english = JsonParser.parseString(Files.readString(en)).getAsJsonObject();
        JsonObject russian = JsonParser.parseString(Files.readString(ru)).getAsJsonObject();
        assertTrue(russian.keySet().containsAll(english.keySet()));
        for (String key : english.keySet()) {
            assertTrue(!english.get(key).getAsString().isEmpty() && !russian.get(key).getAsString().isEmpty(), key);
        }
    }
}
