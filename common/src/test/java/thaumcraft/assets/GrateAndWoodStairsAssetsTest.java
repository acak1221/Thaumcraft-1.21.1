package thaumcraft.assets;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import thaumcraft.client.gui.ThaumcraftRecipeDisplay;

/**
 * Resource wiring of the Item Grate (ConfigBlocks blockMetalDevice 5/6, recipe "Grate" ConfigRecipes:433) and of the Greatwood and
 * Silverwood stairs and slabs (ConfigRecipes:415-418): recipes, tags, loot tables, models and names.
 */
class GrateAndWoodStairsAssetsTest {
    private static final List<String> WOOD = List.of("greatwood", "silverwood");

    private static List<String> tagValues(String path) throws Exception {
        List<String> values = new ArrayList<>();
        for (JsonElement value : ResourceTree.object(path).getAsJsonArray("values")) values.add(value.getAsString());
        return values;
    }

    @Test void theGrateIsIronBarsOverAWoodenTrapdoor() throws Exception {
        JsonObject recipe = ResourceTree.object("data/thaumcraft/recipe/item_grate.json");
        assertEquals("minecraft:crafting_shaped", recipe.get("type").getAsString());
        assertEquals(List.of("#", "T"), List.of(recipe.getAsJsonArray("pattern").get(0).getAsString(), recipe.getAsJsonArray("pattern").get(1).getAsString()));
        assertEquals("minecraft:iron_bars", recipe.getAsJsonObject("key").getAsJsonObject("#").get("item").getAsString());
        assertEquals("minecraft:oak_trapdoor", recipe.getAsJsonObject("key").getAsJsonObject("T").get("item").getAsString());
        assertEquals("thaumcraft:item_grate", recipe.getAsJsonObject("result").get("id").getAsString());
        assertEquals(1, recipe.getAsJsonObject("result").get("count").getAsInt());
    }

    @Test void theBookRecipeKeyOfTheGrateResolvesToItsRecipe() {
        assertEquals("thaumcraft:item_grate", String.valueOf(ThaumcraftRecipeDisplay.aliases().get("Grate")));
    }

    @Test void stairsComeFourFromTheStaircasePatternAndSlabsSixFromARow() throws Exception {
        for (String wood : WOOD) {
            JsonObject stairs = ResourceTree.object("data/thaumcraft/recipe/" + wood + "_stairs.json");
            JsonArray pattern = stairs.getAsJsonArray("pattern");
            assertEquals(List.of("K  ", "KK ", "KKK"), List.of(pattern.get(0).getAsString(), pattern.get(1).getAsString(), pattern.get(2).getAsString()), wood);
            assertEquals("thaumcraft:" + wood + "_planks", stairs.getAsJsonObject("key").getAsJsonObject("K").get("item").getAsString(), wood);
            assertEquals("thaumcraft:" + wood + "_stairs", stairs.getAsJsonObject("result").get("id").getAsString());
            assertEquals(4, stairs.getAsJsonObject("result").get("count").getAsInt(), wood + " stairs");
            JsonObject slab = ResourceTree.object("data/thaumcraft/recipe/" + wood + "_slab.json");
            assertEquals("KKK", slab.getAsJsonArray("pattern").get(0).getAsString(), wood);
            assertEquals(1, slab.getAsJsonArray("pattern").size());
            assertEquals("thaumcraft:" + wood + "_planks", slab.getAsJsonObject("key").getAsJsonObject("K").get("item").getAsString(), wood);
            assertEquals("thaumcraft:" + wood + "_slab", slab.getAsJsonObject("result").get("id").getAsString());
            assertEquals(6, slab.getAsJsonObject("result").get("count").getAsInt(), wood + " slab");
        }
    }

    @Test void theNewBlocksJoinTheVanillaStairAndSlabTagsAndTheWoodenOnes() throws Exception {
        for (String kind : List.of("block", "item")) {
            for (String wood : WOOD) {
                assertTrue(tagValues("data/minecraft/tags/" + kind + "/stairs.json").contains("thaumcraft:" + wood + "_stairs"), kind + " stairs tag " + wood);
                assertTrue(tagValues("data/minecraft/tags/" + kind + "/slabs.json").contains("thaumcraft:" + wood + "_slab"), kind + " slabs tag " + wood);
                assertTrue(tagValues("data/minecraft/tags/" + kind + "/wooden_stairs.json").contains("thaumcraft:" + wood + "_stairs"), kind + " wooden_stairs tag " + wood);
                assertTrue(tagValues("data/minecraft/tags/" + kind + "/wooden_slabs.json").contains("thaumcraft:" + wood + "_slab"), kind + " wooden_slabs tag " + wood);
            }
            assertTrue(tagValues("data/minecraft/tags/" + kind + "/stairs.json").contains("thaumcraft:arcane_stone_brick_stairs"), "the earlier stairs stay in the tag");
        }
    }

    @Test void woodIsBrokenWithAnAxeAndTheGrateWithAPickaxe() throws Exception {
        List<String> axe = tagValues("data/minecraft/tags/block/mineable/axe.json");
        List<String> pickaxe = tagValues("data/minecraft/tags/block/mineable/pickaxe.json");
        for (String wood : WOOD) {
            assertTrue(axe.contains("thaumcraft:" + wood + "_stairs"), wood + " stairs");
            assertTrue(axe.contains("thaumcraft:" + wood + "_slab"), wood + " slab");
        }
        assertTrue(pickaxe.contains("thaumcraft:item_grate"));
    }

    @Test void aDoubleSlabDropsTwoAndEverythingElseDropsItself() throws Exception {
        for (String wood : WOOD) {
            JsonObject pool = ResourceTree.object("data/thaumcraft/loot_table/blocks/" + wood + "_slab.json").getAsJsonArray("pools").get(0).getAsJsonObject();
            JsonObject entry = pool.getAsJsonArray("entries").get(0).getAsJsonObject();
            assertEquals("thaumcraft:" + wood + "_slab", entry.get("name").getAsString());
            JsonObject count = entry.getAsJsonArray("functions").get(0).getAsJsonObject();
            assertEquals("minecraft:set_count", count.get("function").getAsString());
            assertEquals(2, count.get("count").getAsInt());
            assertEquals("double", count.getAsJsonArray("conditions").get(0).getAsJsonObject().getAsJsonObject("properties").get("type").getAsString());
            String stairs = ResourceTree.text("data/thaumcraft/loot_table/blocks/" + wood + "_stairs.json");
            assertTrue(stairs.contains("thaumcraft:" + wood + "_stairs"), wood + " stairs drop themselves");
        }
        assertTrue(ResourceTree.text("data/thaumcraft/loot_table/blocks/item_grate.json").contains("thaumcraft:item_grate"));
    }

    @Test void theGrateDropsTheSameItemWhetherOpenOrClosedBecauseTheLootTableIgnoresTheState() throws Exception {
        assertTrue(!ResourceTree.text("data/thaumcraft/loot_table/blocks/item_grate.json").contains("closed"));
    }

    @Test void openAndClosedGrateHaveTheirOwnModelsAndTheClosedOneAddsTheHatch() throws Exception {
        JsonObject variants = ResourceTree.object("assets/thaumcraft/blockstates/item_grate.json").getAsJsonObject("variants");
        assertEquals("thaumcraft:block/item_grate", variants.getAsJsonObject("closed=false").get("model").getAsString());
        assertEquals("thaumcraft:block/item_grate_closed", variants.getAsJsonObject("closed=true").get("model").getAsString());
        JsonObject open = ResourceTree.object("assets/thaumcraft/models/block/item_grate.json");
        JsonObject closed = ResourceTree.object("assets/thaumcraft/models/block/item_grate_closed.json");
        assertEquals(1, open.getAsJsonArray("elements").size());
        assertEquals(2, closed.getAsJsonArray("elements").size());
        JsonArray from = open.getAsJsonArray("elements").get(0).getAsJsonObject().getAsJsonArray("from");
        JsonArray to = open.getAsJsonArray("elements").get(0).getAsJsonObject().getAsJsonArray("to");
        assertEquals(List.of(0, 13, 0, 16, 16, 16), List.of(from.get(0).getAsInt(), from.get(1).getAsInt(), from.get(2).getAsInt(), to.get(0).getAsInt(), to.get(1).getAsInt(), to.get(2).getAsInt()),
                "the plate fills the top three sixteenths");
    }

    @Test void everyStairAndSlabStateIsMappedToAModelThatExists() throws Exception {
        for (String wood : WOOD) {
            JsonObject stairs = ResourceTree.object("assets/thaumcraft/blockstates/" + wood + "_stairs.json").getAsJsonObject("variants");
            assertEquals(5 * 4 * 2, stairs.size(), wood + " stairs: facing x half x shape = 4 x 2 x 5");
            for (var variant : stairs.entrySet()) {
                String model = variant.getValue().getAsJsonObject().get("model").getAsString();
                assertTrue(ResourceTree.exists("assets/thaumcraft/models/" + model.substring("thaumcraft:".length()) + ".json"), variant.getKey() + " -> " + model);
            }
            JsonObject slab = ResourceTree.object("assets/thaumcraft/blockstates/" + wood + "_slab.json").getAsJsonObject("variants");
            assertEquals(3, slab.size(), wood + " slab: bottom, top, double");
            for (var variant : slab.entrySet()) {
                String model = variant.getValue().getAsJsonObject().get("model").getAsString();
                assertTrue(ResourceTree.exists("assets/thaumcraft/models/" + model.substring("thaumcraft:".length()) + ".json"), variant.getKey() + " -> " + model);
            }
        }
    }
}
