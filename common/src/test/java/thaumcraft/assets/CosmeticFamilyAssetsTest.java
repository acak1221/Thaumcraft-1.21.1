package thaumcraft.assets;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.gson.JsonObject;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

/**
 * The unified TC4 cosmetic block family: one block per hardness group, the original 1.7.10 metadata as
 * the {@code variant} block state.
 *
 * <p>The two content waves that ported the same original families registered one block per metadata and
 * one block per hardness group at the same time. The group design won, so the per metadata ids are gone
 * and their content moved to the group ids and the original metadata. This test locks that mapping: the
 * texture of every metadata, the loot table of every group block and the absence of the old ids.</p>
 */
class CosmeticFamilyAssetsTest {
    /** The block ids of the per metadata wave that the group design replaced. */
    private static final List<String> OLD_IDS = List.of("obsidian_tile", "paving_stone_travel", "paving_stone_warding",
            "obsidian_totem_base", "eldritch_stone", "infused_stone_pillar", "warded_glass", "cosmetic_stone_slab",
            "cosmetic_wood_slab");

    /** The block ids of the group design. */
    private static final List<String> NEW_IDS = List.of("cosmetic_solid", "cosmetic_solid_tough",
            "cosmetic_solid_thaumium", "cosmetic_opaque", "cosmetic_stone_slab_arcane",
            "cosmetic_stone_slab_eldritch", "cosmetic_wood_slab_greatwood", "cosmetic_wood_slab_silverwood");

    private JsonObject json(String path) throws Exception { return ResourceTree.object(path); }

    private JsonObject variants(String block) throws Exception {
        return json("assets/thaumcraft/blockstates/" + block + ".json").getAsJsonObject("variants");
    }

    private String model(String block, String state) throws Exception {
        return variants(block).getAsJsonObject(state).get("model").getAsString();
    }

    private String texture(String model) throws Exception {
        String path = model.substring("thaumcraft:".length());
        JsonObject textures = json("assets/thaumcraft/models/" + path + ".json").getAsJsonObject("textures");
        return textures.has("all") ? textures.get("all").getAsString() : textures.get("side").getAsString();
    }

    @Test void theObsidianTileKeepsItsOriginalMetadata() throws Exception {
        // BlockCosmeticSolid:106,148-155 - the metadata one is the obsidian tile of the obsidiantile texture.
        assertEquals("thaumcraft:blocks/obsidiantile", texture(model("cosmetic_solid", "variant=1")));
        // The metadata zero is the obsidian totem: the base texture on the top and the bottom.
        JsonObject totem = json("assets/thaumcraft/models/block/obsidian_totem_base.json").getAsJsonObject("textures");
        assertEquals("thaumcraft:blocks/obsidiantotembase", totem.get("top").getAsString());
        assertEquals("thaumcraft:blocks/obsidiantotembaseshaded", totem.get("side").getAsString());
        assertEquals("thaumcraft:block/obsidian_totem_base", model("cosmetic_solid_tough", "variant=0"));
    }

    @Test void theTravelAndWardingPavingStonesKeepTheirOriginalMetadata() throws Exception {
        // BlockCosmeticSolid:107-108 - the metadata two is the travel stone and three the warding stone.
        assertEquals("thaumcraft:blocks/paving_stone_travel", texture(model("cosmetic_solid", "variant=2")));
        assertEquals("thaumcraft:blocks/paving_stone_warding", texture(model("cosmetic_solid", "variant=3")));
    }

    @Test void theChargedObsidianTotemKeepsItsOriginalMetadata() throws Exception {
        // BlockCosmeticSolid:106,148-155 - the metadata eight is the totem that holds the node.
        assertEquals("thaumcraft:blocks/obsidiantotembaseshaded", texture(model("cosmetic_solid_tough", "variant=8")));
    }

    @Test void theEldritchRockAndTheAncientStoneKeepTheirOriginalMetadata() throws Exception {
        // BlockCosmeticSolid:117-125,133 - the metadata twelve is the eldritch rock and eleven the ancient stone.
        assertEquals("thaumcraft:blocks/er_1", texture(model("cosmetic_solid", "variant=12")));
        assertEquals("thaumcraft:blocks/es_1", texture(model("cosmetic_solid", "variant=11")));
        assertEquals("thaumcraft:blocks/es_1", texture(model("cosmetic_solid", "variant=13")));
    }

    @Test void theAncientStonePedestalKeepsItsOriginalMetadata() throws Exception {
        // BlockCosmeticSolid:140 - the metadata fifteen is the pedestal with the es_p side texture.
        assertEquals("thaumcraft:blocks/es_p", texture(model("cosmetic_solid", "variant=15")));
    }

    @Test void theWardedGlassIsTheOwnedMetadataOfTheCosmeticOpaque() throws Exception {
        // BlockCosmeticOpaque:88-89,214-218 - the metadata two is the warded glass with the owned tile.
        assertEquals("thaumcraft:block/warded_glass", model("cosmetic_opaque", "variant=2"));
        assertEquals("thaumcraft:blocks/wardedglass", texture(model("cosmetic_opaque", "variant=2")));
        assertEquals("thaumcraft:block/amber_block", model("cosmetic_opaque", "variant=0"));
        assertEquals("thaumcraft:block/amber_bricks", model("cosmetic_opaque", "variant=1"));
    }

    @Test void theSlabsKeepTheirOriginalMetadata() throws Exception {
        // BlockCosmeticStoneSlab:44 - the metadata zero is the arcane stone and one the eldritch stone.
        assertEquals("thaumcraft:blocks/arcane_stone", texture(model("cosmetic_stone_slab_arcane", "type=bottom")));
        assertEquals("thaumcraft:blocks/es_1", texture(model("cosmetic_stone_slab_eldritch", "type=bottom")));
        // BlockCosmeticWoodSlab:44 - the metadata zero is the greatwood and one the silverwood planks.
        assertEquals("thaumcraft:blocks/planks_greatwood", texture(model("cosmetic_wood_slab_greatwood", "type=bottom")));
        assertEquals("thaumcraft:blocks/planks_silverwood", texture(model("cosmetic_wood_slab_silverwood", "type=bottom")));
    }

    @Test void theEldritchStairsTakeTheTextureOfTheOriginalMetadataEleven() throws Exception {
        // ConfigBlocks:297 - the eldritch stairs are the cosmetic stairs of the metadata eleven.
        assertEquals("thaumcraft:blocks/es_1", texture("thaumcraft:block/eldritch_stairs"));
    }

    @Test void everyMetadataOfTheCosmeticSolidHasAModel() throws Exception {
        for (String group : List.of("cosmetic_solid", "cosmetic_solid_tough", "cosmetic_solid_thaumium")) {
            for (int variant = 0; variant <= 15; variant++) {
                String reference = model(group, "variant=" + variant);
                assertTrue(ResourceTree.exists("assets/thaumcraft/models/" + reference.substring("thaumcraft:".length()) + ".json"),
                        group + " variant " + variant + " -> " + reference);
            }
        }
    }

    @Test void theOldPerMetadataIdsAreGone() {
        for (String id : OLD_IDS) {
            assertFalse(ResourceTree.exists("assets/thaumcraft/blockstates/" + id + ".json"), id + " blockstate");
            assertFalse(ResourceTree.exists("data/thaumcraft/loot_table/blocks/" + id + ".json"), id + " loot table");
            assertFalse(ResourceTree.exists("assets/thaumcraft/models/item/" + id + ".json"), id + " item model");
        }
    }

    @Test void everyGroupBlockDropsItsOwnItemAndIsNamedInBothLanguages() throws Exception {
        for (String id : NEW_IDS) {
            JsonObject table = json("data/thaumcraft/loot_table/blocks/" + id + ".json");
            String drop = table.getAsJsonArray("pools").get(0).getAsJsonObject().getAsJsonArray("entries")
                    .get(0).getAsJsonObject().get("name").getAsString();
            assertEquals("thaumcraft:" + id, drop, id + " drops itself");
            assertTrue(ResourceTree.exists("assets/thaumcraft/models/item/" + id + ".json"), id + " item model");
            for (String lang : List.of("en_us", "ru_ru")) {
                assertTrue(json("assets/thaumcraft/lang/" + lang + ".json").has("block.thaumcraft." + id), lang + " " + id);
            }
        }
    }

    @Test void theRemovedNamesAreGoneFromBothLanguages() throws Exception {
        for (String lang : List.of("en_us", "ru_ru")) {
            JsonObject names = json("assets/thaumcraft/lang/" + lang + ".json");
            for (String id : OLD_IDS) assertFalse(names.has("block.thaumcraft." + id), lang + " " + id);
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
            }
        }
    }
}
