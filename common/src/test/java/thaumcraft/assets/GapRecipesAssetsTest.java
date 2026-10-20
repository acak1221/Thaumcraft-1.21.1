package thaumcraft.assets;

import static org.junit.jupiter.api.Assertions.assertEquals;
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
 * The original TC4 4.2.3.5 recipes whose result item or block the 1.21.1 port registers, pinned
 * against the oracle values of {@code ConfigRecipes.java}. Every number here is the original one:
 * the pattern, the ingredients, the vis and aspect costs, the instability, the research gate and
 * the crucible registration order.
 *
 * <p>The oracle lines are the ones of the decompiled 4.2.3.5 {@code ConfigRecipes.java}.
 */
class GapRecipesAssetsTest {
    private static final String RECIPE = "data/thaumcraft/recipe/";

    /** An original arcane recipe: a pattern, a symbol map, a result and a vis cost. */
    private record Arcane(String key, String file, String research, List<String> pattern,
                          Map<String, String> ingredients, String result, int count,
                          Map<String, Integer> vis) {}

    /** An original crucible recipe: a catalyst, a result, an aspect cost and a registration order. */
    private record Crucible(String key, String file, String research, int order, String catalyst,
                            String result, int count, Map<String, Integer> aspects) {}

    /** An original infusion recipe: a centre, its components, an aspect cost and an instability. */
    private record Infusion(String key, String file, String research, String input, List<String> components,
                            Map<String, Integer> aspects, int instability, String result, int count) {}

    // ConfigRecipes:191 (ArcaneDoor), 193 (IronKey), 196 (GoldKey), 216 (BoneBow).
    private static final List<Arcane> ARCANE = List.of(
            new Arcane("ArcaneDoor", "arcane/arcane_door", "WARDEDARCANA", List.of("TDT", "DBD", "TDT"),
                    Map.of("T", "thaumcraft:thaumium_ingot", "B", "thaumcraft:zombie_brain", "D", "thaumcraft:greatwood_planks"),
                    "thaumcraft:arcane_door", 1, Map.of("aqua", 20, "ordo", 10, "terra", 10, "ignis", 5)),
            new Arcane("IronKey", "arcane/iron_key", "WARDEDARCANA", List.of("NNI", "N  "),
                    Map.of("I", "minecraft:iron_ingot", "N", "#c:nuggets/iron"),
                    "thaumcraft:key_iron", 2, Map.of("aqua", 5, "ordo", 5)),
            new Arcane("GoldKey", "arcane/gold_key", "WARDEDARCANA", List.of("NNI", "N  "),
                    Map.of("I", "minecraft:gold_ingot", "N", "minecraft:gold_nugget"),
                    "thaumcraft:key_gold", 2, Map.of("aqua", 5, "ordo", 5)),
            new Arcane("BoneBow", "arcane/bone_bow", "BONEBOW", List.of("SB ", "SEB", "SB "),
                    Map.of("E", "thaumcraft:entropy_shard", "B", "minecraft:bone", "S", "minecraft:string"),
                    "thaumcraft:bone_bow", 1, Map.of("aer", 16, "perditio", 32)));

    // ConfigRecipes:159 (EtherealBloom), 160 (LiquidDeath), 161-163 (BottleTaint).
    private static final List<Crucible> CRUCIBLE = List.of(
            new Crucible("EtherealBloom", "crucible/ethereal_bloom", "ETHEREALBLOOM", 159,
                    "thaumcraft:shimmerleaf", "thaumcraft:ethereal_bloom", 1,
                    Map.of("praecantatio", 16, "herba", 16, "sano", 16, "vitium", 8)),
            new Crucible("LiquidDeath", "crucible/bucket_of_death", "LIQUIDDEATH", 160,
                    "minecraft:bucket", "thaumcraft:bucket_death", 1,
                    Map.of("mortuus", 32, "venenum", 32, "perditio", 32)),
            new Crucible("BottleTaint", "crucible/bottle_taint", "BOTTLETAINT", 163,
                    "thaumcraft:phial_filled", "thaumcraft:bottle_taint", 1,
                    Map.of("vitium", 8, "praecantatio", 8)));

    // ConfigRecipes:344 (JarBrain), 360-362 (Mirror, MirrorHand, MirrorEssentia), 390 (SanityCheck),
    // 392 (SinStone), 393 (PrimalCrusher), 341 (WandPed).
    private static final List<Infusion> INFUSION = List.of(
            new Infusion("JarBrain", "infusion/jar_brain", "JARBRAIN", "thaumcraft:jar",
                    List.of("thaumcraft:zombie_brain", "minecraft:spider_eye", "minecraft:water_bucket", "minecraft:spider_eye"),
                    Map.of("cognitio", 10, "sensus", 10, "exanimis", 20), 4, "thaumcraft:jar_brain", 1),
            new Infusion("Mirror", "infusion/mirror", "MIRROR", "thaumcraft:mirrored_glass",
                    List.of("minecraft:gold_ingot", "minecraft:gold_ingot", "minecraft:gold_ingot", "minecraft:ender_pearl"),
                    Map.of("iter", 8, "tenebrae", 8, "permutatio", 8), 1, "thaumcraft:mirror", 1),
            new Infusion("MirrorHand", "infusion/hand_mirror", "MIRRORHAND", "thaumcraft:mirror",
                    List.of("minecraft:stick", "minecraft:compass", "minecraft:map"),
                    Map.of("instrumentum", 16, "iter", 16), 5, "thaumcraft:hand_mirror", 1),
            new Infusion("SanityCheck", "infusion/sanity_checker", "SANITYCHECK", "thaumcraft:thaumometer",
                    List.of("thaumcraft:mirrored_glass", "thaumcraft:zombie_brain", "minecraft:diamond"),
                    Map.of("cognitio", 24, "sensus", 24, "alienis", 8), 4, "thaumcraft:sanity_checker", 1),
            new Infusion("SinStone", "infusion/compass_stone", "SINSTONE", "minecraft:flint",
                    List.of("thaumcraft:nitor", "thaumcraft:order_shard", "thaumcraft:knowledge_fragment", "thaumcraft:entropy_shard"),
                    Map.of("sensus", 8, "tenebrae", 8, "alienis", 8, "auram", 8), 5, "thaumcraft:compass_stone", 1),
            new Infusion("PrimalCrusher", "infusion/primal_crusher", "PRIMALCRUSHER", "thaumcraft:primordial_pearl",
                    List.of("thaumcraft:primal_charm", "thaumcraft:void_pickaxe", "thaumcraft:void_shovel",
                            "thaumcraft:primal_charm", "thaumcraft:elemental_pickaxe", "thaumcraft:elemental_shovel"),
                    Map.of("perfodio", 24, "instrumentum", 24, "perditio", 16, "vacuos", 16, "telum", 16, "alienis", 16, "lucrum", 16),
                    6, "thaumcraft:primal_crusher", 1),
            new Infusion("WandPed", "infusion/wand_pedestal", "WANDPED", "thaumcraft:arcane_pedestal",
                    List.of("minecraft:gold_ingot", "minecraft:diamond", "thaumcraft:primal_charm", "minecraft:diamond"),
                    Map.of("auram", 10, "praecantatio", 15, "permutatio", 15), 3, "thaumcraft:wand_pedestal", 1));

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

    @Test void everyArcaneRecipeKeepsItsPatternResultAndVisCost() throws Exception {
        for (Arcane arcane : ARCANE) {
            JsonObject recipe = ResourceTree.object(RECIPE + arcane.file() + ".json");
            assertEquals("thaumcraft:arcane_shaped", recipe.get("type").getAsString(), arcane.key());
            assertEquals(arcane.research(), recipe.get("research").getAsString(), arcane.key());
            List<String> pattern = new ArrayList<>();
            recipe.getAsJsonArray("pattern").forEach(row -> pattern.add(row.getAsString()));
            assertEquals(arcane.pattern(), pattern, arcane.key());
            Map<String, String> ingredients = new LinkedHashMap<>();
            recipe.getAsJsonObject("key").entrySet()
                    .forEach(entry -> ingredients.put(entry.getKey(), ingredient(entry.getValue())));
            assertEquals(arcane.ingredients(), ingredients, arcane.key());
            assertEquals(arcane.result(), recipe.getAsJsonObject("result").get("id").getAsString(), arcane.key());
            assertEquals(arcane.count(), recipe.getAsJsonObject("result").get("count").getAsInt(), arcane.key());
            assertEquals(new TreeMap<>(arcane.vis()), amounts(recipe.getAsJsonObject("vis")), arcane.key());
            assertEquals("thaumcraft:" + arcane.file(), ResearchRecipeCoverage.nativeRecipeId(arcane.key()), arcane.key());
        }
    }

    @Test void everyCrucibleRecipeKeepsItsCatalystResultAspectsAndOrder() throws Exception {
        for (Crucible crucible : CRUCIBLE) {
            JsonObject recipe = ResourceTree.object(RECIPE + crucible.file() + ".json");
            assertEquals("thaumcraft:crucible", recipe.get("type").getAsString(), crucible.key());
            assertEquals(crucible.research(), recipe.get("research").getAsString(), crucible.key());
            assertEquals(crucible.order(), recipe.get("source_order").getAsInt(), crucible.key());
            assertEquals(crucible.catalyst(), ingredient(recipe.get("catalyst")), crucible.key());
            assertEquals(crucible.result(), recipe.getAsJsonObject("result").get("id").getAsString(), crucible.key());
            assertEquals(crucible.count(), recipe.getAsJsonObject("result").get("count").getAsInt(), crucible.key());
            assertEquals(new TreeMap<>(crucible.aspects()), amounts(recipe.getAsJsonObject("aspects")), crucible.key());
            assertEquals("thaumcraft:" + crucible.file(), ResearchRecipeCoverage.nativeRecipeId(crucible.key()), crucible.key());
        }
    }

    @Test void everyInfusionRecipeKeepsItsCentreComponentsAspectsAndInstability() throws Exception {
        for (Infusion infusion : INFUSION) {
            JsonObject recipe = ResourceTree.object(RECIPE + infusion.file() + ".json");
            assertEquals("thaumcraft:infusion_stack", recipe.get("type").getAsString(), infusion.key());
            assertEquals(infusion.research(), recipe.get("research").getAsString(), infusion.key());
            assertEquals(infusion.input(), ingredient(recipe.get("input")), infusion.key());
            List<String> components = new ArrayList<>();
            recipe.getAsJsonArray("components").forEach(component -> components.add(ingredient(component)));
            assertEquals(infusion.components(), components, infusion.key());
            assertEquals(new TreeMap<>(infusion.aspects()), amounts(recipe.getAsJsonObject("aspects")), infusion.key());
            assertEquals(infusion.instability(), recipe.get("instability").getAsInt(), infusion.key());
            assertEquals(infusion.result(), recipe.getAsJsonObject("result").get("id").getAsString(), infusion.key());
            assertEquals(infusion.count(), recipe.getAsJsonObject("result").get("count").getAsInt(), infusion.key());
            assertEquals("thaumcraft:" + infusion.file(), ResearchRecipeCoverage.nativeRecipeId(infusion.key()), infusion.key());
        }
    }

    /** ConfigRecipes:452 - the item grate is an ordinary crafting recipe of iron bars over a trapdoor. */
    @Test void theItemGrateIsTheOriginalOrdinaryCraftingRecipe() throws Exception {
        JsonObject recipe = ResourceTree.object(RECIPE + "item_grate.json");
        assertEquals("minecraft:crafting_shaped", recipe.get("type").getAsString());
        List<String> pattern = new ArrayList<>();
        recipe.getAsJsonArray("pattern").forEach(row -> pattern.add(row.getAsString()));
        assertEquals(List.of("#", "T"), pattern);
        assertEquals("minecraft:iron_bars", ingredient(recipe.getAsJsonObject("key").get("#")));
        assertEquals("minecraft:oak_trapdoor", ingredient(recipe.getAsJsonObject("key").get("T")));
        assertEquals("thaumcraft:item_grate", recipe.getAsJsonObject("result").get("id").getAsString());
        assertEquals("thaumcraft:item_grate", ResearchRecipeCoverage.nativeRecipeId("Grate"));
    }

    /** The crucible registration order is unique; the new recipes keep the original place. */
    @Test void theNewCrucibleRecipesKeepTheirOriginalRegistrationOrder() throws Exception {
        assertTrue(ResearchRecipeCoverage.nativeRecipeId("EtherealBloom").endsWith("crucible/ethereal_bloom"));
        assertTrue(ResearchRecipeCoverage.nativeRecipeId("LiquidDeath").endsWith("crucible/bucket_of_death"));
        assertTrue(ResearchRecipeCoverage.nativeRecipeId("BottleTaint").endsWith("crucible/bottle_taint"));
        int ethereal = ResourceTree.object(RECIPE + "crucible/ethereal_bloom.json").get("source_order").getAsInt();
        int death = ResourceTree.object(RECIPE + "crucible/bucket_of_death.json").get("source_order").getAsInt();
        int taint = ResourceTree.object(RECIPE + "crucible/bottle_taint.json").get("source_order").getAsInt();
        assertTrue(ethereal < death && death < taint, "ConfigRecipes:159, 160 and 163");
        int straw = ResourceTree.object(RECIPE + "crucible/golem_straw.json").get("source_order").getAsInt();
        assertTrue(taint < straw, "BottleTaint is registered before GolemStraw");
    }
}
