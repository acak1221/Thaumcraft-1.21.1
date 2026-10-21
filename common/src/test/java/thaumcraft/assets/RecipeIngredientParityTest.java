package thaumcraft.assets;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

/**
 * The ingredient identity of the original TC4 4.2.3.5 recipes against the native 1.21.1 recipe JSON.
 *
 * <p>The comparison of every original recipe key of the oracle {@code ConfigRecipes.java} against
 * the native recipe tree found the recipes below: their native ingredient was a DIFFERENT item than
 * the ingredient of the original. The original matches the components of an infusion or the
 * ingredients of an arcane recipe as items
 * ({@code thaumcraft/api/crafting/InfusionEnchantmentRecipe.matches} compares the ItemStacks), so
 * a substitute item changes what the player has to bring. This test pins the original ingredient
 * of every one of them, so the substitution cannot come back.</p>
 *
 * <p>The oracle line numbers are the line numbers of
 * {@code /home/cyberfox-kali/OpenTC4/src/main/java/thaumcraft/common/config/ConfigRecipes.java}.</p>
 */
class RecipeIngredientParityTest {
    private static final String RECIPE = "data/thaumcraft/recipe/";

    /** One original ingredient: the oracle line, the native recipe and the original item. */
    private record Row(int oracleLine, String nativeRecipe, String symbol, String originalItem) {}

    /**
     * The original ingredients the port had replaced.
     *
     * <ul>
     *   <li>ConfigRecipes:297 - the projectile protection wanted an arrow, the port wanted a feather.</li>
     *   <li>ConfigRecipes:298 - the feather falling wanted a feather, the port wanted a string.</li>
     *   <li>ConfigRecipes:299 and :300 - the respiration and the aqua affinity wanted the reeds, the
     *       port wanted a cod.</li>
     *   <li>ConfigRecipes:301 - the thorns wanted a dead bush, the port wanted a cactus.</li>
     *   <li>ConfigRecipes:303 - the smite wanted the glowstone dust, the port wanted a rotten flesh.</li>
     *   <li>ConfigRecipes:309 - the silk touch wanted the web, the port wanted a glass.</li>
     *   <li>ConfigRecipes:315 - the infinity wanted an arrow, the port wanted a feather.</li>
     *   <li>ConfigRecipes:198 - the arcane spa wanted the arcane stone block, the original
     *       blockCosmeticSolid metadata 6, the port wanted the arcane stone bricks, metadata 7.</li>
     *   <li>ConfigRecipes:341 - the hover harness wanted two greatwood planks, the original
     *       blockWoodenDevice metadata 6, the port wanted two arcane bellows, metadata 0.</li>
     * </ul>
     */
    private static final List<Row> ORIGINAL_INGREDIENTS = List.of(
            new Row(297, "infusion_enchantment/projectile_protection", "1", "minecraft:arrow"),
            new Row(298, "infusion_enchantment/feather_falling", "0", "minecraft:feather"),
            new Row(299, "infusion_enchantment/respiration", "0", "minecraft:sugar_cane"),
            new Row(300, "infusion_enchantment/aqua_affinity", "0", "minecraft:sugar_cane"),
            new Row(301, "infusion_enchantment/thorns", "0", "minecraft:dead_bush"),
            new Row(303, "infusion_enchantment/smite", "1", "minecraft:glowstone_dust"),
            new Row(309, "infusion_enchantment/silk_touch", "1", "minecraft:cobweb"),
            new Row(315, "infusion_enchantment/infinity", "1", "minecraft:arrow"),
            new Row(198, "arcane/arcane_spa", "S", "thaumcraft:arcane_stone"),
            new Row(341, "infusion/hover_harness", "2", "thaumcraft:greatwood_planks"),
            new Row(341, "infusion/hover_harness", "3", "thaumcraft:greatwood_planks"));

    /** The native ids an ingredient element accepts, an item or a tag. */
    private static String ingredient(JsonElement element) {
        JsonObject object = element.getAsJsonObject();
        if (object.has("tag")) return "#" + object.get("tag").getAsString();
        return object.get("item").getAsString();
    }

    @Test void everyRestoredIngredientIsTheOriginalOne() throws Exception {
        List<String> problems = new ArrayList<>();
        for (Row row : ORIGINAL_INGREDIENTS) {
            JsonObject recipe = ResourceTree.object(RECIPE + row.nativeRecipe() + ".json");
            String found;
            if (row.nativeRecipe().startsWith("infusion_enchantment")) {
                found = ingredient(recipe.getAsJsonArray("components").get(Integer.parseInt(row.symbol())));
            } else if (row.nativeRecipe().startsWith("infusion/")) {
                found = ingredient(recipe.getAsJsonArray("components").get(Integer.parseInt(row.symbol())));
            } else {
                found = ingredient(recipe.getAsJsonObject("key").get(row.symbol()));
            }
            if (!found.equals(row.originalItem())) {
                problems.add("oracle line " + row.oracleLine() + " " + row.nativeRecipe() + " " + row.symbol()
                        + " wants " + row.originalItem() + " but uses " + found);
            }
        }
        assertEquals(List.of(), problems);
    }

    /**
     * The original metadata of the two families the port flattens into one native item each: the
     * arcane stone block (blockCosmeticSolid metadata 6) and the arcane stone bricks (metadata 7).
     * The arcane spa of ConfigRecipes:198 wants the block, the flux scrubber of ConfigRecipes:180
     * wants the bricks, and the two native recipes must not use the same item.
     */
    @Test void theArcaneSpaAndTheFluxScrubberUseDifferentArcaneStones() throws Exception {
        JsonObject spa = ResourceTree.object(RECIPE + "arcane/arcane_spa.json");
        JsonObject scrubber = ResourceTree.object(RECIPE + "arcane/flux_scrubber.json");
        String spaStone = ingredient(spa.getAsJsonObject("key").get("S"));
        String scrubberStone = ingredient(scrubber.getAsJsonObject("key").get("S"));
        assertEquals("thaumcraft:arcane_stone", spaStone, "ConfigRecipes:198 wants blockCosmeticSolid metadata 6");
        assertEquals("thaumcraft:arcane_stone_bricks", scrubberStone, "ConfigRecipes:180 wants blockCosmeticSolid metadata 7");
    }

    /**
     * The original metadata of the wooden device family: the greatwood planks (blockWoodenDevice
     * metadata 6) and the arcane bellows (metadata 0). The traveling trunk of ConfigRecipes:372 and
     * the hover harness of ConfigRecipes:341 both want the planks, and the arcane bellows of
     * ConfigRecipes:276 is built from the plankWood ore name.
     */
    @Test void theGreatwoodPlanksAndTheBellowsStayDifferentWoodenDevices() throws Exception {
        JsonObject trunk = ResourceTree.object(RECIPE + "infusion/traveling_trunk.json");
        JsonObject harness = ResourceTree.object(RECIPE + "infusion/hover_harness.json");
        JsonObject bellows = ResourceTree.object(RECIPE + "arcane/bellows.json");
        List<String> trunkComponents = new ArrayList<>();
        trunk.getAsJsonArray("components").forEach(item -> trunkComponents.add(ingredient(item)));
        assertTrue(trunkComponents.contains("thaumcraft:greatwood_planks"),
                "ConfigRecipes:372 wants the greatwood planks: " + trunkComponents);
        assertEquals("thaumcraft:greatwood_planks", ingredient(harness.getAsJsonArray("components").get(2)));
        assertEquals("thaumcraft:greatwood_planks", ingredient(harness.getAsJsonArray("components").get(3)));
        // The bellows of the original is the blockWoodenDevice metadata 0 and is built from the planks.
        assertEquals("thaumcraft:bellows", bellows.getAsJsonObject("result").get("id").getAsString());
        assertEquals("#minecraft:planks", ingredient(bellows.getAsJsonObject("key").get("W")));
    }

    /**
     * The six balanced shards of ConfigRecipes:89. The original walks the aspect array
     * {AIR, FIRE, WATER, EARTH, ORDER, ENTROPY} and excludes the aspect at the index of the SHARD
     * metadata, whose own order is the same {air, fire, water, earth, order, entropy}. Every recipe
     * therefore costs the five primal aspects except the one of its own shard, two of each.
     */
    @Test void theBalancedShardsCostEveryPrimalAspectExceptTheirOwn() throws Exception {
        Map<String, String> excluded = Map.of(
                "air", "aer", "fire", "ignis", "water", "aqua",
                "earth", "terra", "order", "ordo", "entropy", "perditio");
        List<String> all = List.of("aer", "ignis", "aqua", "terra", "ordo", "perditio");
        for (Map.Entry<String, String> entry : excluded.entrySet()) {
            JsonObject recipe = ResourceTree.object(RECIPE + "crucible/balanced_shard_" + entry.getKey() + ".json");
            Map<String, Integer> aspects = new java.util.TreeMap<>();
            recipe.getAsJsonObject("aspects").entrySet()
                    .forEach(aspect -> aspects.put(aspect.getKey(), aspect.getValue().getAsInt()));
            assertEquals(5, aspects.size(), entry.getKey());
            List<String> expected = new ArrayList<>(all);
            expected.remove(entry.getValue());
            assertEquals(new java.util.TreeSet<>(expected), new java.util.TreeSet<>(aspects.keySet()), entry.getKey());
            for (int amount : aspects.values()) assertEquals(2, amount, entry.getKey());
            assertEquals("thaumcraft:" + entry.getKey() + "_shard",
                    ingredient(recipe.get("catalyst")), entry.getKey());
            assertEquals("thaumcraft:balance_shard", recipe.getAsJsonObject("result").get("id").getAsString());
        }
    }
}
