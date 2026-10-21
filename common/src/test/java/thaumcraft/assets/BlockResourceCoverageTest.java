package thaumcraft.assets;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import org.junit.jupiter.api.Test;

/**
 * Every block the port ships has to be complete: a blockstate whose models exist, models whose parents and textures exist, a name in
 * both languages and (unless it is on purpose special) a loot table. The lists below name today's deliberate exceptions; a new block
 * that lands without one of these pieces fails here instead of showing up as a missing-model cube or a raw translation key in game.
 */
class BlockResourceCoverageTest {
    private static final String ASSETS = "assets/thaumcraft/";
    private static final String EN = ASSETS + "lang/en_us.json";
    private static final String RU = ASSETS + "lang/ru_ru.json";

    /** Blocks without a {@code block.thaumcraft.<id>} name: invisible or non-item blocks that carry an older {@code tile.*} key or none. */
    private static final Set<String> NO_BLOCK_NAME = Set.of("arcane_light", "hole", "warded", "shock_trap");

    /** Blocks that have no loot table because their drops come from code, or because they never drop an item. */
    private static final Set<String> NO_LOOT_TABLE = Set.of(
            "arcane_light", "arcane_workbench", "aura_node", "flux_gas", "flux_goo", "hole", "jar", "jar_void", "mana_pod",
            "shock_trap", "table", "warded", "purifying_fluid");

    /**
     * Item models that are not items of the port: the bone bow has one model per draw
     * state and the golem core models belong to the golems the port has not registered.
     */
    private static final Set<String> NOT_ITEM_MODELS = Set.of(
            "bone_bow_pulling_0", "bone_bow_pulling_1", "bone_bow_pulling_2",
            "golem_clay_core", "golem_flesh_core", "golem_iron_core", "golem_stone_core",
            "golem_straw_core", "golem_tallow_core", "golem_thaumium_core", "golem_wood_core");

    /**
     * The registered items whose name another domain still owes: the primal arrows, the
     * edible nuggets, the discount rings and the legacy research items. A new item
     * without a name fails the test below instead of showing a raw translation key.
     */
    private static final Set<String> NAMELESS_ITEMS = Set.of(
            "crimson_sword", "discount_ring_aer", "discount_ring_aqua", "discount_ring_ignis",
            "discount_ring_ordo", "discount_ring_perditio", "discount_ring_terra", "eldritch_placer",
            "eldritch_tablet",
            "thaumonomicon_cheat", "unknown_research_note",
            "unknown_research_note_24");

    private static final List<String> CANDLES = List.of("black", "blue", "brown", "cyan", "gray", "green", "light_blue", "light_gray", "lime",
            "magenta", "orange", "pink", "purple", "red", "yellow");

    private static List<String> blockstates() throws Exception { return ResourceTree.stems(ASSETS + "blockstates", ".json"); }

    private static List<String> itemModels() throws Exception { return ResourceTree.stems(ASSETS + "models/item", ".json"); }

    /**
     * The wand core models and the shape helpers of the wand renderer are drawn by the
     * custom renderer; they are not items and have no name of their own.
     */
    private static boolean isItemModel(String id) {
        if (id.endsWith("_shape")) return false;
        if (id.startsWith("wand_") && !id.startsWith("wand_rod_") && !id.startsWith("wand_cap_")) return false;
        return true;
    }

    /** An item is named by its item key, a block item by the name of its block. */
    private static boolean hasName(JsonObject lang, String id) {
        return lang.has("item.thaumcraft." + id) || lang.has("block.thaumcraft." + id);
    }

    @Test void thereAreBlockstatesToCheck() throws Exception {
        assertFalse(blockstates().isEmpty());
    }

    @Test void everyItemModelHasANameInBothLanguagesExceptTheListedOnes() throws Exception {
        JsonObject en = ResourceTree.object(EN);
        JsonObject ru = ResourceTree.object(RU);
        List<String> missing = new ArrayList<>();
        for (String id : itemModels()) {
            if (!isItemModel(id) || NOT_ITEM_MODELS.contains(id) || NAMELESS_ITEMS.contains(id)) continue;
            if (!hasName(en, id)) missing.add("en_us " + id);
            if (!hasName(ru, id)) missing.add("ru_ru " + id);
        }
        assertEquals(List.of(), missing, "items without a name: translate them, or list them in NAMELESS_ITEMS");
    }

    @Test void theListedItemModelsReallyHaveNoName() throws Exception {
        JsonObject en = ResourceTree.object(EN);
        JsonObject ru = ResourceTree.object(RU);
        List<String> stale = new ArrayList<>();
        for (String id : NAMELESS_ITEMS) {
            if (hasName(en, id) || hasName(ru, id)) stale.add(id);
        }
        for (String id : NOT_ITEM_MODELS) {
            if (hasName(en, id) || hasName(ru, id)) stale.add(id);
        }
        assertEquals(List.of(), stale, "delete the id from the lists: the item has a name now");
    }

    @Test void theTripleMeatTreatHasItsOriginalNameInBothLanguages() throws Exception {
        JsonObject en = ResourceTree.object(EN);
        JsonObject ru = ResourceTree.object(RU);
        assertTrue(en.has("item.thaumcraft.triple_meat_treat"), "the triple meat treat has no English name");
        assertTrue(ru.has("item.thaumcraft.triple_meat_treat"), "the triple meat treat has no Russian name");
    }

    @Test void everyBlockHasANameInBothLanguagesExceptTheListedOnes() throws Exception {
        JsonObject en = ResourceTree.object(EN);
        JsonObject ru = ResourceTree.object(RU);
        List<String> missing = new ArrayList<>();
        for (String id : blockstates()) {
            if (NO_BLOCK_NAME.contains(id)) continue;
            String key = "block.thaumcraft." + id;
            if (!en.has(key)) missing.add("en_us " + key);
            if (!ru.has(key)) missing.add("ru_ru " + key);
        }
        assertEquals(List.of(), missing);
    }

    @Test void theBlocksWithoutABlockNameKeepTheirOlderTileNameWhereTheyHadOne() throws Exception {
        JsonObject en = ResourceTree.object(EN);
        assertTrue(en.has("tile.thaumcraft.hole.name"));
        assertTrue(en.has("tile.thaumcraft.warded.name"));
        for (String id : NO_BLOCK_NAME) assertFalse(en.has("block.thaumcraft." + id), id + " now has a block name: remove it from NO_BLOCK_NAME");
    }

    @Test void everyLootTableBelongsToABlockstate() throws Exception {
        Set<String> blocks = new TreeSet<>(blockstates());
        List<String> orphans = new ArrayList<>();
        for (String id : ResourceTree.stems("data/thaumcraft/loot_table/blocks", ".json")) if (!blocks.contains(id)) orphans.add(id);
        assertEquals(List.of(), orphans, "loot tables of blocks that do not exist");
    }

    @Test void everyBlockHasALootTableExceptTheListedOnesAndTheCandles() throws Exception {
        Set<String> loot = new TreeSet<>(ResourceTree.stems("data/thaumcraft/loot_table/blocks", ".json"));
        List<String> missing = new ArrayList<>();
        for (String id : blockstates()) {
            if (loot.contains(id) || NO_LOOT_TABLE.contains(id) || isCandle(id)) continue;
            missing.add(id);
        }
        assertEquals(List.of(), missing, "blocks that drop nothing: add a loot table or list them in NO_LOOT_TABLE");
    }

    @Test void theListedBlocksWithoutLootReallyHaveNone() throws Exception {
        Set<String> loot = new TreeSet<>(ResourceTree.stems("data/thaumcraft/loot_table/blocks", ".json"));
        Set<String> blocks = new TreeSet<>(blockstates());
        List<String> stale = new ArrayList<>();
        for (String id : NO_LOOT_TABLE) if (loot.contains(id) || !blocks.contains(id)) stale.add(id);
        assertEquals(List.of(), stale, "delete from NO_LOOT_TABLE: the block now has a loot table or is gone");
    }

    private static boolean isCandle(String id) {
        return id.equals("tallow_candle") || (id.endsWith("_tallow_candle") && CANDLES.contains(id.substring(0, id.length() - "_tallow_candle".length())));
    }

    @Test void everyModelNamedByABlockstateExists() throws Exception {
        List<String> missing = new ArrayList<>();
        for (String id : blockstates()) {
            List<String> models = new ArrayList<>();
            JsonObject state = ResourceTree.object(ASSETS + "blockstates/" + id + ".json");
            if (state.has("variants")) for (var variant : state.getAsJsonObject("variants").entrySet()) collect(variant.getValue(), models);
            if (state.has("multipart")) for (JsonElement part : state.getAsJsonArray("multipart")) collect(part.getAsJsonObject().get("apply"), models);
            assertFalse(models.isEmpty(), id + " names no model at all");
            for (String model : models) if (!modelExists(model)) missing.add(id + " -> " + model);
        }
        assertEquals(List.of(), missing);
    }

    private static void collect(JsonElement apply, List<String> models) {
        if (apply.isJsonArray()) for (JsonElement element : apply.getAsJsonArray()) collect(element, models);
        else models.add(apply.getAsJsonObject().get("model").getAsString());
    }

    private static boolean modelExists(String reference) {
        int colon = reference.indexOf(':');
        String namespace = colon < 0 ? "minecraft" : reference.substring(0, colon);
        String path = colon < 0 ? reference : reference.substring(colon + 1);
        return !namespace.equals("thaumcraft") || ResourceTree.exists(ASSETS + "models/" + path + ".json");
    }

    @Test void everyModelHasItsParentAndTextures() throws Exception {
        List<String> missing = new ArrayList<>();
        for (String file : ResourceTree.files(ASSETS + "models")) {
            if (!file.endsWith(".json")) continue;
            JsonObject model = ResourceTree.object(ASSETS + "models/" + file);
            if (model.has("parent") && !modelExists(model.get("parent").getAsString())) missing.add(file + " parent " + model.get("parent").getAsString());
            if (model.has("textures")) {
                for (var texture : model.getAsJsonObject("textures").entrySet()) {
                    String reference = texture.getValue().getAsString();
                    if (reference.startsWith("#")) continue;
                    int colon = reference.indexOf(':');
                    String namespace = colon < 0 ? "minecraft" : reference.substring(0, colon);
                    String path = colon < 0 ? reference : reference.substring(colon + 1);
                    if (namespace.equals("thaumcraft") && !ResourceTree.exists(ASSETS + "textures/" + path + ".png")) missing.add(file + " texture " + reference);
                }
            }
        }
        assertEquals(List.of(), missing);
    }
}
