package thaumcraft.assets;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.gson.JsonObject;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

/**
 * The TC4 cosmetic block families: the original metadata of the cosmetic solid became the
 * {@code variant} block state of the hardness groups of the oracle.
 *
 * <p>Minecraft 1.21.1 binds the hardness and the blast resistance to the block and not to the block
 * state, so the original BlockCosmeticSolid became three blocks: the ordinary stones, the obsidian
 * totem group and the thaumium group. The cosmetic opaque stays ONE block with its three metadata.
 * Three original recipes produce a single metadata value, so each of them gets an item that places
 * the block with that variant, because a 1.21.1 recipe result cannot carry a block state. This test
 * locks that mapping: the texture of every metadata, the loot table of every group block and the
 * names of the variant items.</p>
 */
class CosmeticFamilyAssetsTest {
    /** The block ids of the per metadata wave that the hardness group design replaced. */
    private static final List<String> OLD_BLOCK_IDS = List.of("obsidian_totem_base",
            "eldritch_stone", "infused_stone_pillar", "cosmetic_stone_slab", "cosmetic_wood_slab");

    /** The block ids of the hardness group design. */
    private static final List<String> NEW_IDS = List.of("cosmetic_solid", "cosmetic_solid_tough",
            "cosmetic_solid_thaumium", "cosmetic_opaque", "cosmetic_stone_slab_arcane",
            "cosmetic_stone_slab_eldritch", "cosmetic_wood_slab_greatwood", "cosmetic_wood_slab_silverwood");

    /** The metadata values of every hardness group of the oracle. */
    private static final Map<String, List<Integer>> GROUP_VARIANTS = Map.of(
            "cosmetic_solid", List.of(2, 3, 5, 9, 10, 11, 12, 13, 14, 15),
            "cosmetic_solid_tough", List.of(0, 1, 8),
            "cosmetic_solid_thaumium", List.of(4, 6, 7));

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
            "warded_glass", "thaumcraft:arcane/warded_glass",
            "obsidian_tile", "thaumcraft:obsidian_tile");

    /** The Russian names of the variant items from the original 1.7.10 jar. */
    private static final Map<String, String> VARIANT_ITEM_NAMES = Map.of(
            "paving_stone_travel", "Камень путешественника",
            "paving_stone_warding", "Камень сдерживания",
            "warded_glass", "Усиленное стекло",
            "obsidian_tile", "Обсидиановая основа");

    /** The block id of the hardness group that hosts the original metadata. */
    private static String blockOf(int variant) {
        if (variant == 0 || variant == 1 || variant == 8) return "cosmetic_solid_tough";
        if (variant == 4 || variant == 6 || variant == 7) return "cosmetic_solid_thaumium";
        return "cosmetic_solid";
    }

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

    @Test void everyGroupBlockHostsExactlyItsOriginalMetadata() throws Exception {
        for (var group : GROUP_VARIANTS.entrySet()) {
            JsonObject states = variants(group.getKey());
            assertEquals(group.getValue().size(), states.size(), group.getKey() + " hosts its original metadata");
            for (int variant : group.getValue()) {
                assertTrue(states.has("variant=" + variant), group.getKey() + " misses variant=" + variant);
            }
        }
        assertEquals(16, GROUP_VARIANTS.values().stream().mapToInt(List::size).sum(),
                "the sixteen original metadata values are hosted by the three groups");
    }

    @Test void everyMetadataOfTheCosmeticSolidKeepsItsOriginalTexture() throws Exception {
        for (var entry : SOLID_TEXTURES.entrySet()) {
            String block = blockOf(entry.getKey());
            String state = "variant=" + entry.getKey();
            JsonObject textures = modelTextures(model(block, state));
            Faces faces = entry.getValue();
            assertEquals(faces.side(), face(textures, "side"), block + " " + state + " side");
            assertEquals(faces.top(), face(textures, "top"), block + " " + state + " top");
            assertEquals(faces.bottom(), face(textures, "bottom"), block + " " + state + " bottom");
        }
    }

    @Test void everyMetadataOfTheCosmeticSolidHasAModelThatExists() throws Exception {
        for (var entry : SOLID_TEXTURES.entrySet()) {
            String reference = model(blockOf(entry.getKey()), "variant=" + entry.getKey());
            assertTrue(ResourceTree.exists("assets/thaumcraft/models/" + reference.substring("thaumcraft:".length()) + ".json"),
                    "variant " + entry.getKey() + " -> " + reference);
        }
    }

    @Test void theWardedGlassIsTheOwnedMetadataOfTheCosmeticOpaque() throws Exception {
        // BlockCosmeticOpaque:88-89,214-218 - the metadata two is the warded glass with the owned tile.
        // Minecraft 1.21.1 binds the blast resistance to the block, so the warded glass
        // (resistance 999, unbreakable with the warded stone option) is a block of its own.
        assertEquals("thaumcraft:block/cosmetic_opaque_warded", model("cosmetic_opaque_warded", ""));
        assertEquals("thaumcraft:blocks/wardedglass", face(modelTextures(model("cosmetic_opaque_warded", "")), "all"));
        assertEquals("thaumcraft:block/amber_block", model("cosmetic_opaque", "variant=0"));
        assertEquals("thaumcraft:block/amber_bricks", model("cosmetic_opaque", "variant=1"));
        assertEquals(2, variants("cosmetic_opaque").size(), "the cosmetic opaque keeps the amber block and the amber brick");
        assertEquals(1, variants("cosmetic_opaque_warded").size(), "the warded glass is the metadata two of its own block");
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
        // The original draws the icon of the ancient stone from the hash of the position
        // (oracle BlockCosmeticSolid:157-162), so the port carries the index of the texture in
        // the block state and every index has its own model of every stair shape.
        assertEquals("thaumcraft:blocks/es_1", face(modelTextures("thaumcraft:block/eldritch_stairs_0"), "side"));
        assertEquals("thaumcraft:blocks/es_2", face(modelTextures("thaumcraft:block/eldritch_stairs_1"), "side"));
        assertEquals("thaumcraft:blocks/es_3", face(modelTextures("thaumcraft:block/eldritch_stairs_2"), "side"));
        assertEquals("thaumcraft:blocks/es_4", face(modelTextures("thaumcraft:block/eldritch_stairs_3"), "side"));
        assertEquals("thaumcraft:blocks/es_4", face(modelTextures("thaumcraft:block/eldritch_stairs_outer_3"), "side"));
        assertEquals(160, variants("eldritch_stairs").size(), "forty states of the stair times four textures");
    }

    @Test void everyGroupBlockHasALootTableAndIsNamedInBothLanguages() throws Exception {
        for (String id : NEW_IDS) {
            assertTrue(ResourceTree.exists("data/thaumcraft/loot_table/blocks/" + id + ".json"), id + " loot table");
            assertTrue(ResourceTree.exists("assets/thaumcraft/models/item/" + id + ".json"), id + " item model");
            for (String lang : List.of("en_us", "ru_ru")) {
                assertTrue(json("assets/thaumcraft/lang/" + lang + ".json").has("block.thaumcraft." + id), lang + " " + id);
            }
        }
    }

    @Test void everyGroupBlockDropsItsOwnItem() throws Exception {
        for (String id : List.of("cosmetic_solid_tough", "cosmetic_solid_thaumium")) {
            JsonObject table = json("data/thaumcraft/loot_table/blocks/" + id + ".json");
            String drop = table.getAsJsonArray("pools").get(0).getAsJsonObject().getAsJsonArray("entries")
                    .get(0).getAsJsonObject().get("name").getAsString();
            assertEquals("thaumcraft:" + id, drop, id + " drops the item of its own group");
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
        assertTrue(drops.contains("thaumcraft:cosmetic_solid"), "the group item drops itself");
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
