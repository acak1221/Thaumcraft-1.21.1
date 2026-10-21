package thaumcraft.assets;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import org.junit.jupiter.api.Test;
import thaumcraft.common.lib.research.ResearchRecipeCoverage;

/**
 * The original TC4 4.2.3.5 recipes of the catalogue entries the port audit found missing, pinned against the
 * oracle values of {@code ConfigRecipes.java}.
 *
 * <p>The oracle lines are the ones of the decompiled 4.2.3.5 {@code ConfigRecipes.java}:
 * <ul>
 *   <li>{@code ConfigRecipes}:134 and :148 - the native copper cluster and the copper nugget multiplication.</li>
 *   <li>{@code ConfigRecipes}:184-189 - the sixteen crimson cult banners.</li>
 *   <li>{@code ConfigRecipes}:194 - the flux scrubber.</li>
 *   <li>{@code ConfigRecipes}:197 - the arcane pressure plate.</li>
 *   <li>{@code ConfigRecipes}:231 - the copper wand cap.</li>
 *   <li>{@code ConfigRecipes}:342 and :343 - the compound recharge focus and the advanced node stabilizer.</li>
 *   <li>{@code ConfigRecipes}:410-413 - the four triple meat treats.</li>
 * </ul>
 */
class GapCatalogueRecipesTest {
    private static final String RECIPE = "data/thaumcraft/recipe/";

    /** The single ingredient of a key or a tag, as the recipe JSON writes it. */
    private static String ingredient(JsonElement element) {
        JsonObject object = element.getAsJsonObject();
        if (object.has("tag")) return "#" + object.get("tag").getAsString();
        return object.get("item").getAsString();
    }

    private static Map<String, Integer> amounts(JsonObject object) {
        Map<String, Integer> result = new TreeMap<>();
        object.entrySet().forEach(entry -> result.put(entry.getKey(), entry.getValue().getAsInt()));
        return result;
    }

    private static List<String> pattern(JsonObject recipe) {
        List<String> rows = new ArrayList<>();
        recipe.getAsJsonArray("pattern").forEach(row -> rows.add(row.getAsString()));
        return rows;
    }

    private static Map<String, String> key(JsonObject recipe) {
        Map<String, String> ingredients = new LinkedHashMap<>();
        recipe.getAsJsonObject("key").entrySet()
                .forEach(entry -> ingredients.put(entry.getKey(), ingredient(entry.getValue())));
        return ingredients;
    }

    /** ConfigRecipes:231 - the copper wand cap: five copper nuggets in a ring and the craft cost of two. */
    @Test void theCopperWandCapKeepsItsPatternResultAndVisCost() throws Exception {
        JsonObject recipe = ResourceTree.object(RECIPE + "arcane/wand_cap_copper.json");
        assertEquals("thaumcraft:arcane_shaped", recipe.get("type").getAsString());
        assertEquals("CAP_copper", recipe.get("research").getAsString());
        assertEquals(List.of("NNN", "N N"), pattern(recipe));
        assertEquals(Map.of("N", "#c:nuggets/copper"), key(recipe));
        assertEquals("thaumcraft:wand_cap_copper", recipe.getAsJsonObject("result").get("id").getAsString());
        assertEquals(1, recipe.getAsJsonObject("result").get("count").getAsInt());
        assertEquals(Map.of("aer", 2, "ignis", 2, "ordo", 2), amounts(recipe.getAsJsonObject("vis")));
        assertEquals("thaumcraft:arcane/wand_cap_copper", ResearchRecipeCoverage.nativeRecipeId("WandCapCopper"));
    }

    /** ConfigRecipes:197 - the arcane pressure plate of the wooden device family. */
    @Test void theArcanePressurePlateKeepsItsPatternResultAndVisCost() throws Exception {
        JsonObject recipe = ResourceTree.object(RECIPE + "arcane/arcane_pressure_plate.json");
        assertEquals("thaumcraft:arcane_shaped", recipe.get("type").getAsString());
        assertEquals("WARDEDARCANA", recipe.get("research").getAsString());
        assertEquals(List.of(" B ", "TDT"), pattern(recipe));
        assertEquals(Map.of("B", "thaumcraft:zombie_brain", "D", "thaumcraft:greatwood_planks",
                "T", "thaumcraft:thaumium_ingot"), key(recipe));
        assertEquals("thaumcraft:arcane_pressure_plate", recipe.getAsJsonObject("result").get("id").getAsString());
        assertEquals(Map.of("aqua", 20, "ordo", 10, "ignis", 5, "terra", 10), amounts(recipe.getAsJsonObject("vis")));
        assertEquals("thaumcraft:arcane/arcane_pressure_plate", ResearchRecipeCoverage.nativeRecipeId("ArcanePressurePlate"));
    }

    /** ConfigRecipes:194 - the flux scrubber of the stone device family. */
    @Test void theFluxScrubberKeepsItsPatternResultAndVisCost() throws Exception {
        JsonObject recipe = ResourceTree.object(RECIPE + "arcane/flux_scrubber.json");
        assertEquals("thaumcraft:arcane_shaped", recipe.get("type").getAsString());
        assertEquals("FLUXSCRUB", recipe.get("research").getAsString());
        assertEquals(List.of(" B ", "GOG", "STS"), pattern(recipe));
        assertEquals(Map.of("B", "thaumcraft:bellows", "G", "minecraft:iron_bars", "O", "thaumcraft:vis_filter",
                "S", "thaumcraft:arcane_stone_bricks", "T", "thaumcraft:essentia_tube"), key(recipe));
        assertEquals("thaumcraft:flux_scrubber", recipe.getAsJsonObject("result").get("id").getAsString());
        assertEquals(Map.of("aqua", 16, "ordo", 16, "aer", 8), amounts(recipe.getAsJsonObject("vis")));
        assertEquals("thaumcraft:arcane/flux_scrubber", ResearchRecipeCoverage.nativeRecipeId("FluxScrubber"));
    }

    /** ConfigRecipes:342 - the compound recharge focus of the stone device family. */
    @Test void theCompoundRechargeFocusKeepsItsComponentsAspectsAndInstability() throws Exception {
        JsonObject recipe = ResourceTree.object(RECIPE + "infusion/wand_pedestal_focus.json");
        assertEquals("thaumcraft:infusion_stack", recipe.get("type").getAsString());
        assertEquals("WANDPEDFOC", recipe.get("research").getAsString());
        assertEquals("minecraft:comparator", ingredient(recipe.get("input")));
        List<String> components = new ArrayList<>();
        recipe.getAsJsonArray("components").forEach(component -> components.add(ingredient(component)));
        assertEquals(List.of("thaumcraft:earth_shard", "thaumcraft:vis_filter", "thaumcraft:earth_shard",
                "thaumcraft:vis_filter", "thaumcraft:earth_shard", "thaumcraft:vis_filter",
                "thaumcraft:earth_shard", "thaumcraft:vis_filter"), components);
        assertEquals(Map.of("ordo", 10, "praecantatio", 15, "permutatio", 10), amounts(recipe.getAsJsonObject("aspects")));
        assertEquals(4, recipe.get("instability").getAsInt());
        assertEquals("thaumcraft:wand_pedestal_focus", recipe.getAsJsonObject("result").get("id").getAsString());
        assertEquals("thaumcraft:infusion/wand_pedestal_focus", ResearchRecipeCoverage.nativeRecipeId("WandPedFocus"));
    }

    /** ConfigRecipes:343 - the advanced node stabilizer of the stone device family. */
    @Test void theAdvancedNodeStabilizerKeepsItsComponentsAspectsAndInstability() throws Exception {
        JsonObject recipe = ResourceTree.object(RECIPE + "infusion/advanced_node_stabilizer.json");
        assertEquals("thaumcraft:infusion_stack", recipe.get("type").getAsString());
        assertEquals("NODESTABILIZERADV", recipe.get("research").getAsString());
        assertEquals("thaumcraft:node_stabilizer", ingredient(recipe.get("input")));
        List<String> components = new ArrayList<>();
        recipe.getAsJsonArray("components").forEach(component -> components.add(ingredient(component)));
        assertEquals(List.of("thaumcraft:nitor", "minecraft:redstone_block", "thaumcraft:alumentum",
                "minecraft:redstone_block", "thaumcraft:nitor", "minecraft:redstone_block",
                "thaumcraft:alumentum", "minecraft:redstone_block"), components);
        assertEquals(Map.of("auram", 32, "praecantatio", 16, "ordo", 16, "potentia", 16),
                amounts(recipe.getAsJsonObject("aspects")));
        assertEquals(10, recipe.get("instability").getAsInt());
        assertEquals("thaumcraft:advanced_node_stabilizer", recipe.getAsJsonObject("result").get("id").getAsString());
        assertEquals("thaumcraft:infusion/advanced_node_stabilizer", ResearchRecipeCoverage.nativeRecipeId("NodeStabilizerAdv"));
    }

    /** ConfigRecipes:134 and :148 - the native copper cluster and the copper nugget multiplication. */
    @Test void theCopperCrucibleRecipesKeepTheirCatalystResultAspectsAndOrder() throws Exception {
        JsonObject pure = ResourceTree.object(RECIPE + "crucible/pure_copper.json");
        assertEquals("thaumcraft:crucible", pure.get("type").getAsString());
        assertEquals("PURECOPPER", pure.get("research").getAsString());
        assertEquals(24, pure.get("source_order").getAsInt());
        assertTrue(pure.get("catalyst").isJsonArray());
        assertEquals("#thaumcraft:original_ore/ore_copper", ingredient(pure.getAsJsonArray("catalyst").get(0)));
        assertEquals("thaumcraft:cluster_copper", pure.getAsJsonObject("result").get("id").getAsString());
        assertEquals(1, pure.getAsJsonObject("result").get("count").getAsInt());
        assertEquals(Map.of("metallum", 1, "ordo", 1), amounts(pure.getAsJsonObject("aspects")));
        assertEquals("thaumcraft:crucible/pure_copper", ResearchRecipeCoverage.nativeRecipeId("PureCopper"));

        JsonObject trans = ResourceTree.object(RECIPE + "crucible/trans_copper.json");
        assertEquals("TRANSCOPPER", trans.get("research").getAsString());
        assertEquals(27, trans.get("source_order").getAsInt());
        assertEquals("#thaumcraft:original_ore/nugget_copper", ingredient(trans.get("catalyst")));
        assertEquals("thaumcraft:copper_nugget", trans.getAsJsonObject("result").get("id").getAsString());
        assertEquals(3, trans.getAsJsonObject("result").get("count").getAsInt());
        assertEquals(Map.of("metallum", 2, "permutatio", 1), amounts(trans.getAsJsonObject("aspects")));
        assertEquals("thaumcraft:crucible/trans_copper", ResearchRecipeCoverage.nativeRecipeId("TransCopper"));
    }

    /**
     * The crucible recipes keep the registration order of the original: the pure copper (ConfigRecipes:134)
     * comes before the trans iron (:145) and the trans copper (:148) after the trans gold (:146).
     */
    @Test void theCopperCrucibleRecipesKeepTheOriginalRegistrationOrder() throws Exception {
        int pure = ResourceTree.object(RECIPE + "crucible/pure_copper.json").get("source_order").getAsInt();
        int gold = ResourceTree.object(RECIPE + "crucible/pure_gold.json").get("source_order").getAsInt();
        int iron = ResourceTree.object(RECIPE + "crucible/trans_iron.json").get("source_order").getAsInt();
        int transGold = ResourceTree.object(RECIPE + "crucible/trans_gold.json").get("source_order").getAsInt();
        int trans = ResourceTree.object(RECIPE + "crucible/trans_copper.json").get("source_order").getAsInt();
        assertTrue(gold < pure && pure < iron && iron < transGold && transGold < trans,
                "ConfigRecipes:132, 134, 145, 146 and 148");
    }

    /** ConfigRecipes:184-189 - the sixteen crimson cult banners, one per dye colour. */
    @Test void theSixteenBannersKeepTheirPatternResultAndVisCost() throws Exception {
        List<String> colours = List.of("white", "orange", "magenta", "light_blue", "yellow", "lime", "pink", "gray",
                "light_gray", "cyan", "purple", "blue", "brown", "green", "red", "black");
        assertEquals(16, colours.size());
        for (int index = 0; index < colours.size(); index++) {
            String colour = colours.get(index);
            JsonObject recipe = ResourceTree.object(RECIPE + "arcane/banner_" + colour + ".json");
            assertEquals("thaumcraft:arcane_shaped", recipe.get("type").getAsString(), colour);
            assertEquals("BANNERS", recipe.get("research").getAsString(), colour);
            assertEquals(List.of("WS", "WS", "WB"), pattern(recipe), colour);
            // The original W is the coloured wool block (Blocks.field_150325_L with the metadata of the
            // dye), S is the stick and B is "slabWood", so ANY wooden slab.
            assertEquals(Map.of("B", "#minecraft:wooden_slabs", "S", "minecraft:stick",
                    "W", "minecraft:" + colour + "_wool"), key(recipe), colour);
            // The original kept the colour in the item NBT; the native item carries it in a component.
            assertEquals("thaumcraft:banner", recipe.getAsJsonObject("result").get("id").getAsString(), colour);
            assertEquals(1, recipe.getAsJsonObject("result").get("count").getAsInt(), colour);
            assertEquals(index, recipe.getAsJsonObject("result").getAsJsonObject("components")
                    .getAsJsonObject("thaumcraft:banner_data").get("color").getAsInt(), colour);
            assertEquals(Map.of("aqua", 5, "terra", 5), amounts(recipe.getAsJsonObject("vis")), colour);
        }
    }

    /** ConfigRecipes:410-413 - the four triple meat treats. */
    @Test void theFourTripleMeatTreatsKeepTheirIngredients() throws Exception {
        Map<String, List<String>> treats = new LinkedHashMap<>();
        treats.put("chicken_beef_pork", List.of("nugget_chicken", "nugget_beef", "nugget_pork"));
        treats.put("chicken_beef_fish", List.of("nugget_chicken", "nugget_beef", "nugget_fish"));
        treats.put("chicken_pork_fish", List.of("nugget_chicken", "nugget_pork", "nugget_fish"));
        treats.put("beef_pork_fish", List.of("nugget_beef", "nugget_pork", "nugget_fish"));
        for (var entry : treats.entrySet()) {
            JsonObject recipe = ResourceTree.object(RECIPE + "triple_meat_treat_" + entry.getKey() + ".json");
            assertEquals("minecraft:crafting_shapeless", recipe.get("type").getAsString(), entry.getKey());
            List<String> ingredients = new ArrayList<>();
            recipe.getAsJsonArray("ingredients").forEach(item -> ingredients.add(ingredient(item)));
            List<String> expected = new ArrayList<>();
            expected.add("minecraft:sugar");
            for (String nugget : entry.getValue()) expected.add("thaumcraft:" + nugget);
            assertEquals(expected, ingredients, entry.getKey());
            assertEquals("thaumcraft:triple_meat_treat", recipe.getAsJsonObject("result").get("id").getAsString(), entry.getKey());
            assertEquals(1, recipe.getAsJsonObject("result").get("count").getAsInt(), entry.getKey());
        }
    }

    /**
     * ConfigRecipes:399 and :402 - the nine-fold compression of the iron nugget. The original compressed its own
     * iron nugget; 1.21.1 has the vanilla iron nugget, so the port carries the iron nugget as that item and the
     * copper nugget with the same shape, because vanilla copper has no nugget.
     */
    @Test void theNuggetCompressionKeepsTheOriginalNineFoldShape() throws Exception {
        JsonObject nuggets = ResourceTree.object(RECIPE + "copper_nuggets.json");
        assertEquals("minecraft:crafting_shapeless", nuggets.get("type").getAsString());
        assertEquals(List.of("minecraft:copper_ingot"), List.of(ingredient(nuggets.getAsJsonArray("ingredients").get(0))));
        assertEquals("thaumcraft:copper_nugget", nuggets.getAsJsonObject("result").get("id").getAsString());
        assertEquals(9, nuggets.getAsJsonObject("result").get("count").getAsInt());

        JsonObject ingot = ResourceTree.object(RECIPE + "copper_ingot_from_nuggets.json");
        assertEquals("minecraft:crafting_shaped", ingot.get("type").getAsString());
        assertEquals(List.of("###", "###", "###"), pattern(ingot));
        assertEquals(Map.of("#", "thaumcraft:copper_nugget"), key(ingot));
        assertEquals("minecraft:copper_ingot", ingot.getAsJsonObject("result").get("id").getAsString());
        assertEquals(1, ingot.getAsJsonObject("result").get("count").getAsInt());
    }

    /**
     * The modern convention tags the port uses must exist: vanilla 1.21.1 has no {@code c:} tags at all, so every
     * recipe that names one needs the port to define it.
     */
    @Test void everyConventionTagARecipeNamesExists() throws Exception {
        List<String> tags = new ArrayList<>();
        for (String file : ResourceTree.files(RECIPE)) {
            if (!file.endsWith(".json")) continue;
            collectTags(ResourceTree.json(RECIPE + file), tags);
        }
        List<String> missing = new ArrayList<>();
        for (String tag : tags) {
            if (!tag.startsWith("c:")) continue;
            if (!ResourceTree.exists("data/c/tags/item/" + tag.substring("c:".length()) + ".json")) missing.add(tag);
        }
        assertEquals(List.of(), missing);
    }

    /**
     * The nine-fold compression of the iron nugget of the original (ConfigRecipes:399 and :402). The original
     * compressed its own iron nugget; vanilla 1.21.1 has the iron nugget and both halves of the compression, so
     * the port carries the iron nugget as that item and names it in the modern convention tags.
     */
    @Test void theIronNuggetKeepsTheOriginalNineFoldCompression() throws Exception {
        JsonObject nuggets = ResourceTree.object("data/minecraft/recipe/iron_nugget.json");
        assertEquals("minecraft:crafting_shapeless", nuggets.get("type").getAsString());
        assertEquals("minecraft:iron_ingot", ingredient(nuggets.getAsJsonArray("ingredients").get(0)));
        assertEquals("minecraft:iron_nugget", nuggets.getAsJsonObject("result").get("id").getAsString());
        assertEquals(9, nuggets.getAsJsonObject("result").get("count").getAsInt());

        JsonObject ingot = ResourceTree.object("data/minecraft/recipe/iron_ingot_from_nuggets.json");
        assertEquals("minecraft:crafting_shaped", ingot.get("type").getAsString());
        assertEquals(List.of("###", "###", "###"), pattern(ingot));
        assertEquals(Map.of("#", "minecraft:iron_nugget"), key(ingot));
        assertEquals("minecraft:iron_ingot", ingot.getAsJsonObject("result").get("id").getAsString());
        assertEquals(1, ingot.getAsJsonObject("result").get("count").getAsInt());

        // The ore dictionary name of the original iron nugget and the modern convention tag carry it.
        assertEquals(List.of("minecraft:iron_nugget"), values("data/thaumcraft/tags/item/original_ore/nugget_iron.json"));
        assertEquals(List.of("minecraft:iron_nugget"), values("data/c/tags/item/nuggets/iron.json"));
        assertEquals(List.of("minecraft:iron_nugget"), values("data/thaumcraft/tags/item/iron_nuggets.json"));
    }

    /** The copper nugget and the native copper cluster of the original are carried by the port. */
    @Test void theCopperNuggetAndClusterAreCarriedByThePort() throws Exception {
        assertEquals(List.of("thaumcraft:copper_nugget"), values("data/c/tags/item/nuggets/copper.json"));
        assertEquals(List.of("thaumcraft:cluster_copper"), values("data/thaumcraft/tags/item/original_ore/cluster_copper.json"));
        assertTrue(values("data/thaumcraft/tags/item/original_ore/nugget_copper.json").contains("thaumcraft:copper_nugget"));
    }

    /**
     * The native copper cluster has its icon and its name like the other native clusters, but the original melts
     * only the iron, the cinnabar and the gold cluster (ConfigRecipes:488-490), so it has no furnace recipe.
     */
    @Test void theNativeCopperClusterHasItsIconAndNameButNoFurnaceRecipe() throws Exception {
        JsonObject model = ResourceTree.object("assets/thaumcraft/models/item/cluster_copper.json");
        assertEquals("minecraft:item/generated", model.get("parent").getAsString());
        assertEquals("thaumcraft:items/clustercopper", model.getAsJsonObject("textures").get("layer0").getAsString());
        assertTrue(ResourceTree.exists("assets/thaumcraft/textures/items/clustercopper.png"));
        assertEquals("Native Copper Cluster",
                ResourceTree.object("assets/thaumcraft/lang/en_us.json").get("item.thaumcraft.cluster_copper").getAsString());
        assertEquals("Меднорудный кластер",
                ResourceTree.object("assets/thaumcraft/lang/ru_ru.json").get("item.thaumcraft.cluster_copper").getAsString());
        assertFalse(ResourceTree.exists("data/thaumcraft/recipe/cluster_copper_smelting.json"));
    }

    /** The copper nugget has its own icon: the original texture of ItemNugget metadata one. */
    @Test void theCopperNuggetHasItsOriginalIcon() throws Exception {
        JsonObject model = ResourceTree.object("assets/thaumcraft/models/item/copper_nugget.json");
        assertEquals("minecraft:item/generated", model.get("parent").getAsString());
        assertEquals("thaumcraft:items/nuggetcopper", model.getAsJsonObject("textures").get("layer0").getAsString());
        assertTrue(ResourceTree.exists("assets/thaumcraft/textures/items/nuggetcopper.png"));
        assertEquals("Copper Nugget",
                ResourceTree.object("assets/thaumcraft/lang/en_us.json").get("item.thaumcraft.copper_nugget").getAsString());
    }

    /** The plain values of an item tag, the optional references ignored. */
    private static List<String> values(String path) throws Exception {
        List<String> values = new ArrayList<>();
        for (JsonElement element : ResourceTree.object(path).getAsJsonArray("values")) {
            if (element.isJsonPrimitive()) values.add(element.getAsString());
        }
        return values;
    }

    private static void collectTags(JsonElement element, List<String> tags) {
        if (element.isJsonObject()) {
            for (var entry : element.getAsJsonObject().entrySet()) {
                JsonElement value = entry.getValue();
                if (entry.getKey().equals("tag") && value.isJsonPrimitive()) tags.add(value.getAsString());
                else collectTags(value, tags);
            }
        } else if (element.isJsonArray()) {
            for (JsonElement child : element.getAsJsonArray()) collectTags(child, tags);
        }
    }
}
