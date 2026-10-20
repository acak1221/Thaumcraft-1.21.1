package thaumcraft.assets;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.gson.JsonObject;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

/**
 * The TC4 cosmetic block families: ONE block per original family, the original 1.7.10 metadata as
 * the {@code variant} block state.
 *
 * <p>The original BlockCosmeticSolid is one block with sixteen metadata values and the original
 * BlockCosmeticOpaque is one block with three. Three original recipes produce a single metadata
 * value, so each of them gets an item that places the block with that variant, because a 1.21.1
 * recipe result cannot carry a block state. This test locks that mapping: the texture of every
 * metadata, the loot table of the family blocks and the names of the variant items.</p>
 */
class CosmeticFamilyAssetsTest {
    /** The block ids of the per metadata wave that the family design replaced. */
    private static final List<String> OLD_BLOCK_IDS = List.of("obsidian_tile", "obsidian_totem_base",
            "eldritch_stone", "infused_stone_pillar", "cosmetic_stone_slab", "cosmetic_wood_slab");

    /** The block ids of the family design. */
    private static final List<String> NEW_IDS = List.of("cosmetic_solid", "cosmetic_opaque",
            "cosmetic_stone_slab_arcane", "cosmetic_stone_slab_eldritch",
            "cosmetic_wood_slab_greatwood", "cosmetic_wood_slab_silverwood");

    /** The side, the top and the bottom texture of one original metadata. */
    private record Faces(String side, String top, String bottom) {}

    /**
     * The texture of every metadata of the cosmetic solid.
     *
     * <p>BlockCosmeticSolid:75-101 registers the icons, :106-142 picks them per side and metadata
     * and :146-179 picks the world dependent ones. The totem, the metadata zero and the metadata
     * eight, shows the totem base on its sides and the obsidian tile on its top and its bottom;
     * the golem fetter, the metadata nine and ten, shows the golem stone on its sides, the golem
     * stone top on its top and the arcane stone on its bottom.
     */
    private static final Map<Integer, Faces> SOLID_TEXTURES = Map.ofEntries(
            Map.entry(0, new Faces("thaumcraft:blocks/obsidiantotembase", "thaumcraft:blocks/obsidiantile", "thaumcraft:blocks/obsidiantile")),
            Map.entry(1, new Faces("thaumcraft:blocks/obsidiantile", "thaumcraft:blocks/obsidiantile", "thaumcraft:blocks/obsidiantile")),
            Map.entry(2, new Faces("thaumcraft:blocks/paving_stone_travel", "thaumcraft:blocks/paving_stone_travel", "thaumcraft:blocks/paving_stone_travel")),
            Map.entry(3, new Faces("thaumcraft:blocks/paving_stone_warding", "thaumcraft:blocks/paving_stone_warding", "thaumcraft:blocks/paving_stone_warding")),
            Map.entry(4, new Faces("thaumcraft:blocks/thaumiumblock", "thaumcraft:blocks/thaumiumblock", "thaumcraft:blocks/thaumiumblock")),
            Map.entry(5, new Faces("thaumcraft:blocks/tallowblock", "thaumcraft:blocks/tallowblock_top", "thaumcraft:blocks/tallowblock_top")),
            Map.entry(6, new Faces("thaumcraft:blocks/pedestal_top", "thaumcraft:blocks/pedestal_top", "thaumcraft:blocks/pedestal_top")),
            Map.entry(7, new Faces("thaumcraft:blocks/arcane_stone", "thaumcraft:blocks/arcane_stone", "thaumcraft:blocks/arcane_stone")),
            Map.entry(8, new Faces("thaumcraft:blocks/obsidiantotembase", "thaumcraft:blocks/obsidiantile", "thaumcraft:blocks/obsidiantile")),
            Map.entry(9, new Faces("thaumcraft:blocks/golem_stone_side", "thaumcraft:blocks/golem_stone_top", "thaumcraft:blocks/arcane_stone")),
            Map.entry(10, new Faces("thaumcraft:blocks/golem_stone_side", "thaumcraft:blocks/golem_stone_top_active", "thaumcraft:blocks/arcane_stone")),
            Map.entry(11, new Faces("thaumcraft:blocks/es_1", "thaumcraft:blocks/es_1", "thaumcraft:blocks/es_1")),
            Map.entry(12, new Faces("thaumcraft:blocks/er_1", "thaumcraft:blocks/er_1", "thaumcraft:blocks/er_1")),
            Map.entry(13, new Faces("thaumcraft:blocks/es_1", "thaumcraft:blocks/es_1", "thaumcraft:blocks/es_1")),
            Map.entry(14, new Faces("thaumcraft:blocks/crust", "thaumcraft:blocks/crust", "thaumcraft:blocks/crust")),
            Map.entry(15, new Faces("thaumcraft:blocks/es_p", "thaumcraft:blocks/es_1", "thaumcraft:blocks/es_1")));

    /** The original recipes that produce one metadata value and the item that closes them. */
    private static final Map<String, String> VARIANT_ITEMS = Map.of(
            "paving_stone_travel", "thaumcraft:arcane/paving_stone_travel",
            "paving_stone_warding", "thaumcraft:arcane/paving_stone_warding",
            "warded_glass", "thaumcraft:arcane/warded_glass");

    /** The Russian names of the variant items from the original 1.7.10 jar. */
    private static final Map<String, String> VARIANT_ITEM_NAMES = Map.of(
            "paving_stone_travel", "Камень путешественника",
            "paving_stone_warding", "Камень сдерживания",
            "warded_glass", "Усиленное стекло");

    private JsonObject json(String path) throws Exception { return ResourceTree.object(path); }

    private JsonObject variants(String block) throws Exception {
        return json("assets/thaumcraft/blockstates/" + block + ".json").getAsJsonObject("variants");
    }

    private String model(String block, String state) throws Exception {
        return variants(block).getAsJsonObject(state).get("model").getAsString();
    }

    /** The texture of a face of a block model; a cube_all model has one texture for every face. */
    private String face(JsonObject textures, String key) {
        if (textures.has(key)) return textures.get(key).getAsString();
        return textures.get("all").getAsString();
    }

    private JsonObject modelTextures(String reference) throws Exception {
        return json("assets/thaumcraft/models/" + reference.substring("thaumcraft:".length()) + ".json").getAsJsonObject("textures");
    }

    @Test void everyMetadataOfTheCosmeticSolidKeepsItsOriginalTexture() throws Exception {
        JsonObject states = variants("cosmetic_solid");
        assertEquals(16, states.size(), "the cosmetic solid has the sixteen original metadata values");
        for (var entry : SOLID_TEXTURES.entrySet()) {
            String state = "variant=" + entry.getKey();
            assertTrue(states.has(state), "cosmetic_solid misses " + state);
            JsonObject textures = modelTextures(model("cosmetic_solid", state));
            Faces faces = entry.getValue();
            assertEquals(faces.side(), face(textures, "side"), state + " side");
            assertEquals(faces.top(), face(textures, "top"), state + " top");
            assertEquals(faces.bottom(), face(textures, "bottom"), state + " bottom");
        }
    }

    @Test void theWardedGlassIsTheOwnedMetadataOfTheCosmeticOpaque() throws Exception {
        // BlockCosmeticOpaque:88-89,214-218 - the metadata two is the warded glass with the owned tile.
        assertEquals("thaumcraft:block/warded_glass", model("cosmetic_opaque", "variant=2"));
        assertEquals("thaumcraft:blocks/wardedglass", face(modelTextures(model("cosmetic_opaque", "variant=2")), "all"));
        assertEquals("thaumcraft:block/amber_block", model("cosmetic_opaque", "variant=0"));
        assertEquals("thaumcraft:block/amber_bricks", model("cosmetic_opaque", "variant=1"));
        assertEquals(3, variants("cosmetic_opaque").size(), "the cosmetic opaque has the three original metadata values");
    }

    @Test void theSlabsKeepTheirOriginalMetadata() throws Exception {
        // BlockCosmeticStoneSlab:44 - the metadata zero is the arcane stone and one the eldritch stone.
        assertEquals("thaumcraft:blocks/arcane_stone", face(modelTextures(model("cosmetic_stone_slab_arcane", "type=bottom")), "side"));
        assertEquals("thaumcraft:blocks/es_1", face(modelTextures(model("cosmetic_stone_slab_eldritch", "type=bottom")), "side"));
        // BlockCosmeticWoodSlab:44 - the metadata zero is the greatwood and one the silverwood planks.
        assertEquals("thaumcraft:blocks/planks_greatwood", face(modelTextures(model("cosmetic_wood_slab_greatwood", "type=bottom")), "side"));
        assertEquals("thaumcraft:blocks/planks_silverwood", face(modelTextures(model("cosmetic_wood_slab_silverwood", "type=bottom")), "side"));
    }

    @Test void theEldritchStairsTakeTheTextureOfTheOriginalMetadataEleven() throws Exception {
        // ConfigBlocks:297 - the eldritch stairs are the cosmetic stairs of the metadata eleven.
        assertEquals("thaumcraft:blocks/es_1", face(modelTextures("thaumcraft:block/eldritch_stairs"), "side"));
    }

    @Test void everyMetadataOfTheCosmeticSolidHasAModelThatExists() throws Exception {
        for (int variant = 0; variant <= 15; variant++) {
            String reference = model("cosmetic_solid", "variant=" + variant);
            assertTrue(ResourceTree.exists("assets/thaumcraft/models/" + reference.substring("thaumcraft:".length()) + ".json"),
                    "cosmetic_solid variant " + variant + " -> " + reference);
        }
    }

    @Test void everyFamilyBlockHasALootTableAndIsNamedInBothLanguages() throws Exception {
        for (String id : NEW_IDS) {
            assertTrue(ResourceTree.exists("data/thaumcraft/loot_table/blocks/" + id + ".json"), id + " loot table");
            assertTrue(ResourceTree.exists("assets/thaumcraft/models/item/" + id + ".json"), id + " item model");
            for (String lang : List.of("en_us", "ru_ru")) {
                assertTrue(json("assets/thaumcraft/lang/" + lang + ".json").has("block.thaumcraft." + id), lang + " " + id);
            }
        }
    }

    @Test void theSlabsDropThemselves() throws Exception {
        for (String id : List.of("cosmetic_stone_slab_arcane", "cosmetic_stone_slab_eldritch",
                "cosmetic_wood_slab_greatwood", "cosmetic_wood_slab_silverwood")) {
            JsonObject table = json("data/thaumcraft/loot_table/blocks/" + id + ".json");
            String drop = table.getAsJsonArray("pools").get(0).getAsJsonObject().getAsJsonArray("entries")
                    .get(0).getAsJsonObject().get("name").getAsString();
            assertEquals("thaumcraft:" + id, drop, id + " drops itself");
        }
    }

    @Test void theCosmeticSolidDropsTheVariantItemsOfItsPavingStones() throws Exception {
        JsonObject table = json("data/thaumcraft/loot_table/blocks/cosmetic_solid.json");
        List<String> drops = new java.util.ArrayList<>();
        for (var pool : table.getAsJsonArray("pools")) {
            for (var entry : pool.getAsJsonObject().getAsJsonArray("entries")) {
                drops.add(entry.getAsJsonObject().get("name").getAsString());
            }
        }
        assertTrue(drops.contains("thaumcraft:cosmetic_solid"), "the family item drops itself");
        assertTrue(drops.contains("thaumcraft:paving_stone_travel"), "the travel stone drops its own item");
        assertTrue(drops.contains("thaumcraft:paving_stone_warding"), "the warding stone drops its own item");
    }

    @Test void theVariantItemsCloseTheOriginalRecipesAndAreNamedInBothLanguages() throws Exception {
        for (var entry : VARIANT_ITEMS.entrySet()) {
            String id = entry.getKey();
            assertTrue(ResourceTree.exists("assets/thaumcraft/models/item/" + id + ".json"), id + " item model");
            JsonObject recipe = json("data/thaumcraft/recipe/" + entry.getValue().substring("thaumcraft:".length()) + ".json");
            assertEquals("thaumcraft:" + id, recipe.getAsJsonObject("result").get("id").getAsString(), id + " is the recipe result");
            for (String lang : List.of("en_us", "ru_ru")) {
                assertTrue(json("assets/thaumcraft/lang/" + lang + ".json").has("item.thaumcraft." + id), lang + " " + id);
            }
            // The original names of the 1.7.10 jar: tile.blockCosmeticSolid.N.name and tile.blockCosmeticOpaque.2.name.
            assertEquals(VARIANT_ITEM_NAMES.get(id), json("assets/thaumcraft/lang/ru_ru.json").get("item.thaumcraft." + id).getAsString(), id);
        }
    }

    @Test void theVariantItemsAreItemsAndNotBlocks() throws Exception {
        for (String id : VARIANT_ITEMS.keySet()) {
            assertFalse(ResourceTree.exists("assets/thaumcraft/blockstates/" + id + ".json"), id + " is not a block");
            assertFalse(ResourceTree.exists("data/thaumcraft/loot_table/blocks/" + id + ".json"), id + " is not a block");
        }
    }

    @Test void theOldPerMetadataIdsAreGone() throws Exception {
        for (String id : OLD_BLOCK_IDS) {
            assertFalse(ResourceTree.exists("assets/thaumcraft/blockstates/" + id + ".json"), id + " blockstate");
            assertFalse(ResourceTree.exists("data/thaumcraft/loot_table/blocks/" + id + ".json"), id + " loot table");
            assertFalse(ResourceTree.exists("assets/thaumcraft/models/item/" + id + ".json"), id + " item model");
        }
    }

    @Test void theRemovedNamesAreGoneFromBothLanguages() throws Exception {
        for (String lang : List.of("en_us", "ru_ru")) {
            JsonObject names = json("assets/thaumcraft/lang/" + lang + ".json");
            for (String id : OLD_BLOCK_IDS) assertFalse(names.has("block.thaumcraft." + id), lang + " " + id);
            for (String id : List.of("cosmetic_solid_tough", "cosmetic_solid_thaumium")) {
                assertFalse(names.has("block.thaumcraft." + id), lang + " " + id);
                assertFalse(names.has("item.thaumcraft." + id), lang + " " + id);
            }
        }
    }

    @Test void theCosmeticSolidKeepsTheOriginalNamesOfItsMetadata() throws Exception {
        // The original lang of the 1.7.10 jar: tile.blockCosmeticSolid.N.name.
        Map<Integer, String> names = Map.ofEntries(
                Map.entry(0, "Obsidian Totem"), Map.entry(1, "Obsidian Tile"), Map.entry(2, "Paving Stone of Travel"),
                Map.entry(3, "Paving Stone of Warding"), Map.entry(4, "Thaumium Block"), Map.entry(5, "Tallow Block"),
                Map.entry(6, "Arcane Stone Block"), Map.entry(7, "Arcane Stone Bricks"),
                Map.entry(8, "Charged Obsidian Totem"), Map.entry(9, "Golem Fetter"),
                Map.entry(10, "Active Golem Fetter"), Map.entry(11, "Ancient Stone"), Map.entry(12, "Ancient Rock"),
                Map.entry(13, "Ancient Stone"), Map.entry(14, "Crusted Stone"), Map.entry(15, "Ancient Stone Pedestal"));
        for (var entry : names.entrySet()) {
            String key = "tile.thaumcraft.cosmetic_solid." + entry.getKey() + ".name";
            for (String lang : List.of("en_us", "ru_ru")) {
                JsonObject namesOfLang = json("assets/thaumcraft/lang/" + lang + ".json");
                assertTrue(namesOfLang.has(key), lang + " " + key);
                if ("en_us".equals(lang)) assertEquals(entry.getValue(), namesOfLang.get(key).getAsString(), key);
            }
        }
    }
}
