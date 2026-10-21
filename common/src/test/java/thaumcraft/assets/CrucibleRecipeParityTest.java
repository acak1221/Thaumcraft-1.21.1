package thaumcraft.assets;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import org.junit.jupiter.api.Test;

/**
 * The whole crucible catalogue of the port, pinned against the TC4 4.2.3.5 oracle
 * ({@code thaumcraft/common/config/ConfigRecipes.java}, method {@code initializeAlchemyRecipes}, lines 105-181).
 *
 * <p>The oracle registers 54 crucible recipes at run time (49 registration sites, of which the balanced shard loop of line 113 runs
 * six times); 8 of them exist only when a third-party mod supplies copper, tin, silver or lead ({@code Config.foundCopperIngot} and
 * friends, lines 133-157). Copper is vanilla in 1.21.1, so its two recipes (lines 134 and 148) are live and the port ships them:
 * 54 minus the six recipes that need a third-party tin, silver or lead mod are the 48 native recipes of the port. The registration
 * order is part of the original behaviour: the crucible keeps the first registered recipe among the matches with the most distinct aspects
 * ({@code ThaumcraftCraftingManager.java:127-145}), so the rows below are listed in the original registration order and the native
 * {@code source_order} has to reproduce it.
 *
 * <p>Three rows carry the decisions the a6 content audit got wrong; the oracle decides, not the audit:
 * <ul>
 *   <li>{@code CoreGuard} / {@code CoreButcher} (lines 176-177): the oracle's {@code addCrucibleRecipe(key, result, catalyst, aspects)}
 *       takes the RESULT second and the CATALYST third. {@code CoreGuard} makes a guard core (metadata 4) from a blank core
 *       (metadata 100) and {@code CoreButcher} makes a butcher core (metadata 9) from a guard core (metadata 4). The audit had read the
 *       two arguments the other way round and reported the port as swapped.</li>
 *   <li>{@code Alumentum} (line 115): the catalyst is {@code Items.field_151044_h}, the 1.7.10 coal item, with the WILDCARD metadata
 *       {@code Short.MAX_VALUE}. {@code ThaumcraftApiHelper.itemMatches} skips the metadata comparison for such a stack
 *       ({@code ThaumcraftApiHelper.java:148-153}), and in 1.7.10 coal and charcoal are one item with two metadata values
 *       (0 = coal, 1 = charcoal), so the original accepts BOTH. 1.8 split them into two items, and the port therefore lists both
 *       modern ids.</li>
 *   <li>{@code LiquidDeath} (line 160): the catalyst is {@code Items.field_151133_ar}, which is the vanilla BUCKET, not a glass
 *       bottle. The oracle's aspect table proves it: {@code ConfigAspects.java:314} gives that field METAL 8 + VOID 1 and lines
 *       315-317 derive the water, lava and milk buckets from it, which are exactly the aspects the original gives
 *       {@code Items.BUCKET}. The glass bottle is a different field, {@code Items.field_151069_bo} ({@code ConfigAspects.java:302}).</li>
 * </ul>
 */
class CrucibleRecipeParityTest {
    private static final String CRUCIBLE = "data/thaumcraft/recipe/crucible/";

    /** One native crucible recipe with the values the oracle registers for it. */
    private record Recipe(String file, int order, String research, List<String> catalyst,
                           String result, int count, Map<String, Integer> aspects, int oracleLine) {}

    private static Map<String, Integer> aspects(String... pairs) {
        Map<String, Integer> amounts = new TreeMap<>();
        for (int i = 0; i < pairs.length; i += 2) amounts.put(pairs[i], Integer.parseInt(pairs[i + 1]));
        return amounts;
    }

    private static final List<Recipe> ORACLE = List.of(
            // BalancedShard_0..5 (line 113): one balance shard from the primal shard of the same index, paid with 2 of each other primal.
            new Recipe("balanced_shard_air", 0, "CRUCIBLE", List.of("thaumcraft:air_shard"), "thaumcraft:balance_shard", 1,
                    aspects("ignis", "2", "aqua", "2", "terra", "2", "ordo", "2", "perditio", "2"), 113),
            new Recipe("balanced_shard_fire", 1, "CRUCIBLE", List.of("thaumcraft:fire_shard"), "thaumcraft:balance_shard", 1,
                    aspects("aer", "2", "aqua", "2", "terra", "2", "ordo", "2", "perditio", "2"), 113),
            new Recipe("balanced_shard_water", 2, "CRUCIBLE", List.of("thaumcraft:water_shard"), "thaumcraft:balance_shard", 1,
                    aspects("aer", "2", "ignis", "2", "terra", "2", "ordo", "2", "perditio", "2"), 113),
            new Recipe("balanced_shard_earth", 3, "CRUCIBLE", List.of("thaumcraft:earth_shard"), "thaumcraft:balance_shard", 1,
                    aspects("aer", "2", "ignis", "2", "aqua", "2", "ordo", "2", "perditio", "2"), 113),
            new Recipe("balanced_shard_order", 4, "CRUCIBLE", List.of("thaumcraft:order_shard"), "thaumcraft:balance_shard", 1,
                    aspects("aer", "2", "ignis", "2", "aqua", "2", "terra", "2", "perditio", "2"), 113),
            new Recipe("balanced_shard_entropy", 5, "CRUCIBLE", List.of("thaumcraft:entropy_shard"), "thaumcraft:balance_shard", 1,
                    aspects("aer", "2", "ignis", "2", "aqua", "2", "terra", "2", "ordo", "2"), 113),
            new Recipe("alumentum", 6, "ALUMENTUM", List.of("minecraft:coal", "minecraft:charcoal"), "thaumcraft:alumentum", 1,
                    aspects("potentia", "3", "ignis", "3", "perditio", "3"), 115),
            new Recipe("nitor", 7, "NITOR", List.of("minecraft:glowstone_dust", "#c:dusts/glowstone"), "thaumcraft:nitor", 1,
                    aspects("potentia", "3", "ignis", "3", "lux", "3"), 116),
            new Recipe("thaumium", 8, "THAUMIUM", List.of("minecraft:iron_ingot"), "thaumcraft:thaumium_ingot", 1,
                    aspects("praecantatio", "4"), 117),
            new Recipe("void_ingot", 9, "VOIDMETAL", List.of("thaumcraft:void_seed"), "thaumcraft:void_ingot", 1,
                    aspects("metallum", "8"), 118),
            new Recipe("void_seed", 10, "VOIDMETAL", List.of("minecraft:wheat_seeds"), "thaumcraft:void_seed", 1,
                    aspects("tenebrae", "8", "vacuos", "8", "alienis", "2"), 119),
            new Recipe("tallow", 11, "TALLOW", List.of("minecraft:rotten_flesh"), "thaumcraft:magic_tallow", 1,
                    aspects("praecantatio", "2"), 120),
            new Recipe("alt_gunpowder", 12, "ALCHEMICALDUPLICATION", List.of("minecraft:gunpowder"), "minecraft:gunpowder", 2,
                    aspects("ignis", "4", "perditio", "4"), 121),
            new Recipe("alt_slime", 13, "ALCHEMICALDUPLICATION", List.of("minecraft:slime_ball"), "minecraft:slime_ball", 2,
                    aspects("aqua", "2", "victus", "2"), 122),
            new Recipe("alt_clay", 14, "ALCHEMICALDUPLICATION", List.of("minecraft:clay_ball"), "minecraft:clay_ball", 2,
                    aspects("aqua", "1", "terra", "2"), 123),
            new Recipe("alt_glowstone", 15, "ALCHEMICALDUPLICATION", List.of("minecraft:glowstone_dust", "#c:dusts/glowstone"),
                    "minecraft:glowstone_dust", 2, aspects("lux", "3", "sensus", "1"), 124),
            new Recipe("alt_ink", 16, "ALCHEMICALDUPLICATION", List.of("minecraft:ink_sac"), "minecraft:ink_sac", 2,
                    aspects("aqua", "2", "sensus", "2"), 125),
            new Recipe("alt_web", 17, "ALCHEMICALMANUFACTURE", List.of("minecraft:string"), "minecraft:cobweb", 1,
                    aspects("vinculum", "2", "pannus", "2"), 126),
            new Recipe("alt_mossy_cobble", 18, "ALCHEMICALMANUFACTURE", List.of("minecraft:cobblestone"), "minecraft:mossy_cobblestone", 1,
                    aspects("herba", "2", "praecantatio", "1"), 127),
            new Recipe("alt_ice", 19, "ALCHEMICALMANUFACTURE", List.of("minecraft:snow_block"), "minecraft:ice", 1,
                    aspects("ordo", "1", "gelum", "1"), 128),
            new Recipe("alt_cracked_brick", 20, "ENTROPICPROCESSING", List.of("minecraft:stone_bricks"), "minecraft:cracked_stone_bricks", 1,
                    aspects("perditio", "2"), 129),
            new Recipe("alt_bonemeal", 21, "ENTROPICPROCESSING", List.of("minecraft:bone"), "minecraft:bone_meal", 4,
                    aspects("perditio", "1"), 130),
            new Recipe("pure_iron", 22, "PUREIRON", List.of("#c:ores/iron", "#c:raw_materials/iron"), "thaumcraft:cluster_iron", 1,
                    aspects("metallum", "1", "ordo", "1"), 131),
            new Recipe("pure_gold", 23, "PUREGOLD", List.of("#c:ores/gold", "#c:raw_materials/gold"), "thaumcraft:cluster_gold", 1,
                    aspects("metallum", "1", "ordo", "1"), 132),
            // Copper is vanilla in 1.21.1, so the original branch Config.foundCopperIngot (line 133) is
            // live and the PureCopper recipe of line 134 is registered. Tin, silver and lead still need
            // a third-party mod, so their Pure recipes (lines 136, 138 and 140) stay absent and the
            // registration order continues with the Trans recipes of lines 145, 146 and 148.
            new Recipe("pure_copper", 24, "PURECOPPER", List.of("#thaumcraft:original_ore/ore_copper", "#c:raw_materials/copper"),
                    "thaumcraft:cluster_copper", 1, aspects("metallum", "1", "ordo", "1"), 134),
            new Recipe("trans_iron", 25, "TRANSIRON", List.of("#c:nuggets/iron"), "minecraft:iron_nugget", 3,
                    aspects("metallum", "2"), 145),
            new Recipe("trans_gold", 26, "TRANSGOLD", List.of("minecraft:gold_nugget"), "minecraft:gold_nugget", 3,
                    aspects("metallum", "2", "lucrum", "1"), 146),
            new Recipe("trans_copper", 27, "TRANSCOPPER", List.of("#thaumcraft:original_ore/nugget_copper"), "thaumcraft:copper_nugget", 3,
                    aspects("metallum", "2", "permutatio", "1"), 148),
            new Recipe("ethereal_bloom", 159, "ETHEREALBLOOM", List.of("thaumcraft:shimmerleaf"), "thaumcraft:ethereal_bloom", 1,
                    aspects("praecantatio", "16", "herba", "16", "sano", "16", "vitium", "8"), 159),
            new Recipe("bucket_of_death", 160, "LIQUIDDEATH", List.of("minecraft:bucket"), "thaumcraft:bucket_death", 1,
                    aspects("mortuus", "32", "venenum", "32", "perditio", "32"), 160),
            new Recipe("bottle_taint", 163, "BOTTLETAINT", List.of("thaumcraft:phial_filled"), "thaumcraft:bottle_taint", 1,
                    aspects("vitium", "8", "praecantatio", "8"), 163),
            new Recipe("golem_straw", 164, "GOLEMSTRAW", List.of("minecraft:hay_block"), "thaumcraft:golem_straw", 1,
                    aspects("humanus", "4", "motus", "4", "spiritus", "4"), 164),
            new Recipe("golem_wood", 165, "GOLEMWOOD", List.of("thaumcraft:greatwood_log"), "thaumcraft:golem_wood", 1,
                    aspects("humanus", "4", "motus", "4", "spiritus", "4"), 165),
            new Recipe("golem_tallow", 166, "GOLEMTALLOW", List.of("thaumcraft:tallow_block"), "thaumcraft:golem_tallow", 1,
                    aspects("humanus", "8", "motus", "8", "spiritus", "8"), 166),
            new Recipe("golem_clay", 167, "GOLEMCLAY", List.of("minecraft:clay"), "thaumcraft:golem_clay", 1,
                    aspects("humanus", "4", "motus", "4", "spiritus", "4"), 167),
            new Recipe("golem_flesh", 168, "GOLEMFLESH", List.of("thaumcraft:flesh_block"), "thaumcraft:golem_flesh", 1,
                    aspects("humanus", "8", "motus", "8", "spiritus", "8"), 168),
            new Recipe("golem_stone", 169, "GOLEMSTONE", List.of("minecraft:stone_bricks"), "thaumcraft:golem_stone", 1,
                    aspects("humanus", "4", "motus", "4", "spiritus", "4"), 169),
            new Recipe("golem_iron", 170, "GOLEMIRON", List.of("minecraft:iron_block"), "thaumcraft:golem_iron", 1,
                    aspects("humanus", "4", "motus", "4", "spiritus", "4"), 170),
            new Recipe("golem_thaumium", 171, "GOLEMTHAUMIUM", List.of("thaumcraft:thaumium_block"), "thaumcraft:golem_thaumium", 1,
                    aspects("humanus", "8", "motus", "8", "spiritus", "8"), 171),
            // The golem cores: metadata 100 is the blank core, 4 the guard core and 9 the butcher core
            // (oracle entities/golems/ItemGolemCore.java:44-57).
            new Recipe("golem_core_gather", 172, "COREGATHER", List.of("thaumcraft:golem_core_blank"), "thaumcraft:golem_core_gather", 1,
                    aspects("lucrum", "5", "terra", "5"), 172),
            new Recipe("golem_core_fill", 173, "COREFILL", List.of("thaumcraft:golem_core_blank"), "thaumcraft:golem_core_fill", 1,
                    aspects("fames", "5", "vacuos", "5"), 173),
            new Recipe("golem_core_empty", 174, "COREEMPTY", List.of("thaumcraft:golem_core_blank"), "thaumcraft:golem_core_empty", 1,
                    aspects("lucrum", "5", "vacuos", "5"), 174),
            new Recipe("golem_core_harvest", 175, "COREHARVEST", List.of("thaumcraft:golem_core_blank"), "thaumcraft:golem_core_harvest", 1,
                    aspects("meto", "5", "messis", "5"), 175),
            new Recipe("golem_core_guard", 176, "COREGUARD", List.of("thaumcraft:golem_core_blank"), "thaumcraft:golem_core_guard", 1,
                    aspects("telum", "5", "vinculum", "5"), 176),
            new Recipe("golem_core_butcher", 177, "COREBUTCHER", List.of("thaumcraft:golem_core_guard"), "thaumcraft:golem_core_butcher", 1,
                    aspects("corpus", "5", "bestia", "5"), 177),
            new Recipe("golem_core_liquid", 178, "CORELIQUID", List.of("thaumcraft:golem_core_blank"), "thaumcraft:golem_core_liquid", 1,
                    aspects("aqua", "5", "vacuos", "5"), 178),
            new Recipe("bath_salts", 179, "BATHSALTS", List.of("thaumcraft:salis_mundus"), "thaumcraft:bath_salts", 1,
                    aspects("cognitio", "6", "auram", "6", "ordo", "6", "sano", "6"), 179),
            new Recipe("sanity_soap", 180, "SANESOAP", List.of("thaumcraft:tallow_block"), "thaumcraft:sanity_soap", 1,
                    aspects("cognitio", "16", "alienis", "16", "ordo", "16", "sano", "16"), 180));

    /**
     * The six oracle crucible recipes that only exist with a third-party tin, silver or lead ore mod
     * (lines 135-144 and 149-157). Copper is vanilla in 1.21.1, so its two recipes (lines 134 and 148)
     * are registered and shipped.
     */
    private static final List<String> THIRD_PARTY_ORE = List.of(
            "pure_tin", "pure_silver", "pure_lead", "trans_tin", "trans_silver", "trans_lead");

    private static List<String> catalysts(JsonElement catalyst) {
        List<String> found = new ArrayList<>();
        JsonArray options = catalyst.isJsonArray() ? catalyst.getAsJsonArray() : new JsonArray();
        if (!catalyst.isJsonArray()) options.add(catalyst);
        for (JsonElement option : options) {
            JsonObject object = option.getAsJsonObject();
            found.add(object.has("item") ? object.get("item").getAsString() : "#" + object.get("tag").getAsString());
        }
        return found;
    }

    private static Map<String, Integer> aspects(JsonObject recipe) {
        Map<String, Integer> amounts = new TreeMap<>();
        recipe.getAsJsonObject("aspects").entrySet().forEach(entry -> amounts.put(entry.getKey(), entry.getValue().getAsInt()));
        return amounts;
    }

    @Test void everyCrucibleRecipeKeepsTheOriginalGateCatalystResultAndAspects() throws Exception {
        for (Recipe expected : ORACLE) {
            JsonObject recipe = ResourceTree.object(CRUCIBLE + expected.file() + ".json");
            assertEquals("thaumcraft:crucible", recipe.get("type").getAsString(), expected.file());
            assertEquals(expected.research(), recipe.get("research").getAsString(), expected.file());
            assertEquals(expected.order(), recipe.get("source_order").getAsInt(), expected.file());
            assertEquals(expected.catalyst(), catalysts(recipe.get("catalyst")), expected.file() + " catalyst, oracle line " + expected.oracleLine());
            assertEquals(expected.result(), recipe.getAsJsonObject("result").get("id").getAsString(), expected.file());
            assertEquals(expected.count(), recipe.getAsJsonObject("result").get("count").getAsInt(), expected.file());
            assertEquals(expected.aspects(), aspects(recipe), expected.file() + " aspects, oracle line " + expected.oracleLine());
        }
    }

    @Test void theShippedCrucibleSetIsTheOracleSetWithoutTheThirdPartyOreRecipes() throws Exception {
        List<String> shipped = ResourceTree.stems(CRUCIBLE, ".json");
        List<String> expected = new ArrayList<>();
        for (Recipe recipe : ORACLE) expected.add(recipe.file());
        assertEquals(expected.size(), shipped.size(), "the original registers 54 crucible recipes, 6 of them behind a third-party ore mod");
        assertTrue(shipped.containsAll(expected), "missing native crucible recipes: " + expected.stream().filter(name -> !shipped.contains(name)).toList());
        assertTrue(expected.containsAll(shipped), "native crucible recipes the original does not register: " + shipped.stream().filter(name -> !expected.contains(name)).toList());
        for (String name : THIRD_PARTY_ORE) assertFalse(shipped.contains(name), name + " needs a third-party ore mod");
    }

    @Test void theRegistrationOrderIsTheOriginalOne() throws Exception {
        List<String> expected = new ArrayList<>();
        for (Recipe recipe : ORACLE) expected.add(recipe.file());
        Map<String, Integer> orders = new java.util.HashMap<>();
        for (String stem : ResourceTree.stems(CRUCIBLE, ".json")) {
            orders.put(stem, ResourceTree.object(CRUCIBLE + stem + ".json").get("source_order").getAsInt());
        }
        List<String> shipped = new ArrayList<>(orders.keySet());
        shipped.sort(java.util.Comparator.comparingInt(orders::get));
        assertEquals(expected, shipped, "the crucible keeps the first registered of the equally matching recipes");
    }

    /**
     * The number of the crucible recipes of the original and of the port.
     *
     * <p>The oracle registers 54 crucible recipes at run time: 49 registration sites, of which the balanced
     * shard loop of line 113 runs six times. Eight of them are conditional on a third-party ore mod
     * ({@code Config.foundCopperIngot}, {@code foundTinIngot}, {@code foundSilverIngot} and
     * {@code foundLeadIngot}, lines 133-157). Copper is vanilla in 1.21.1, so the port ships the two copper
     * recipes and documents the six tin, silver and lead recipes as skips: 54 - 6 = 48 native recipes.</p>
     */
    @Test void theCountsAreTheOriginalOnes() throws Exception {
        assertEquals(48, ORACLE.size(), "the port ships the 48 crucible recipes the 1.21.1 world can carry");
        assertEquals(48, ResourceTree.stems(CRUCIBLE, ".json").size(), "the packaged crucible recipes");
        assertEquals(6, THIRD_PARTY_ORE.size(), "the six recipes that need a third-party tin, silver or lead mod");
        // The oracle registers six balanced shards, one of them per primal, and the rest one by one.
        long shards = ORACLE.stream().filter(recipe -> recipe.file().startsWith("balanced_shard_")).count();
        assertEquals(6, shards, "the balanced shard loop of oracle line 113 runs six times");
        // The eight conditional registrations of the oracle are the four Pure and the four Trans recipes of
        // copper, tin, silver and lead (lines 134, 137, 140, 143 and 148, 151, 154, 157). The two copper ones
        // are live in 1.21.1 and are shipped, the six others are documented as skips.
        assertTrue(ORACLE.stream().anyMatch(recipe -> recipe.file().equals("pure_copper") && recipe.oracleLine() == 134),
                "the copper purification of oracle line 134 is carried, copper is vanilla in 1.21.1");
        assertTrue(ORACLE.stream().anyMatch(recipe -> recipe.file().equals("trans_copper") && recipe.oracleLine() == 148),
                "the copper multiplication of oracle line 148 is carried, copper is vanilla in 1.21.1");
        for (String third : THIRD_PARTY_ORE) {
            assertFalse(ORACLE.stream().anyMatch(recipe -> recipe.file().equals(third)),
                    third + " needs a third-party ore mod and is not part of the shipped set");
        }
    }

    /**
     * The native {@code source_order} grows with the original registration order.
     *
     * <p>The crucible sorts its recipes by this number and keeps the first registered of the equally
     * matching ones, so the number has to grow with the oracle lines. The port numbers the first recipes
     * with their registration index and the rest with the line of the oracle; both schemes are accepted,
     * a value that breaks the order is not.</p>
     */
    @Test void theSourceOrderFollowsTheLinesOfTheOracle() throws Exception {
        int previousOrder = -1;
        int previousLine = -1;
        for (int index = 0; index < ORACLE.size(); ++index) {
            Recipe recipe = ORACLE.get(index);
            int order = ResourceTree.object(CRUCIBLE + recipe.file() + ".json").get("source_order").getAsInt();
            assertTrue(order > previousOrder, recipe.file() + " keeps the original registration order");
            // The six balanced shards come from one loop of the oracle, so they share its line.
            assertTrue(recipe.oracleLine() >= previousLine, recipe.file() + " keeps the original line order");
            assertTrue(order == index || order == recipe.oracleLine(),
                    recipe.file() + " is numbered with its registration index or with its oracle line: " + order);
            previousOrder = order;
            previousLine = recipe.oracleLine();
        }
    }

    @Test void theGolemCoresRunFromTheBlankCoreToTheGuardCoreAndOnToTheButcherCore() throws Exception {
        // Oracle ConfigRecipes:176-177: CoreGuard makes the guard core (metadata 4) from the blank core (metadata 100),
        // CoreButcher makes the butcher core (metadata 9) from the guard core (metadata 4). The a6 audit had read the
        // result and the catalyst argument the other way round and reported the port as swapped.
        assertEquals(List.of("thaumcraft:golem_core_blank"), catalysts(ResourceTree.object(CRUCIBLE + "golem_core_guard.json").get("catalyst")));
        assertEquals("thaumcraft:golem_core_guard", ResourceTree.object(CRUCIBLE + "golem_core_guard.json").getAsJsonObject("result").get("id").getAsString());
        assertEquals(List.of("thaumcraft:golem_core_guard"), catalysts(ResourceTree.object(CRUCIBLE + "golem_core_butcher.json").get("catalyst")));
        assertEquals("thaumcraft:golem_core_butcher", ResourceTree.object(CRUCIBLE + "golem_core_butcher.json").getAsJsonObject("result").get("id").getAsString());
        // Every other core comes out of the blank core.
        for (String stem : List.of("golem_core_gather", "golem_core_fill", "golem_core_empty", "golem_core_harvest", "golem_core_liquid")) {
            assertEquals(List.of("thaumcraft:golem_core_blank"), catalysts(ResourceTree.object(CRUCIBLE + stem + ".json").get("catalyst")), stem);
        }
    }

    @Test void theAlumentumAcceptsCoalAndCharcoalBecauseTheyWereOneItemIn17() throws Exception {
        // Oracle ConfigRecipes:115 registers the catalyst as Items.field_151044_h with the WILDCARD metadata Short.MAX_VALUE.
        // ThaumcraftApiHelper.itemMatches (ThaumcraftApiHelper.java:148-153) skips the metadata comparison for such a stack, and in
        // 1.7.10 the coal item carries metadata 0 = coal and metadata 1 = charcoal, so the original accepts both.
        assertEquals(List.of("minecraft:coal", "minecraft:charcoal"),
                catalysts(ResourceTree.object(CRUCIBLE + "alumentum.json").get("catalyst")));
        // The same item smelts the magical logs into its metadata 1, charcoal (oracle ConfigRecipes:487).
        for (String log : List.of("greatwood", "silverwood")) {
            JsonObject smelting = ResourceTree.object("data/thaumcraft/recipe/charcoal_from_smelting_" + log + "_log.json");
            assertEquals("minecraft:charcoal", smelting.getAsJsonObject("result").get("id").getAsString(), log);
        }
    }

    @Test void theBucketOfDeathIsBrewedFromTheBucketTheOriginalUsed() throws Exception {
        // Oracle ConfigRecipes:160 registers Items.field_151133_ar as the catalyst. The oracle's own aspect table proves that
        // field is the vanilla bucket: ConfigAspects:314 gives it METAL 8 + VOID 1, and lines 315-317 derive the water, lava
        // and milk buckets from it. Those are exactly the aspects the original gives Items.BUCKET (legacy ConfigAspects:208),
        // while the glass bottle is a different field, Items.field_151069_bo (ConfigAspects:302).
        assertEquals(List.of("minecraft:bucket"),
                catalysts(ResourceTree.object(CRUCIBLE + "bucket_of_death.json").get("catalyst")));
        // The same field is the empty container of the Bucket of Death (oracle ConfigItems.java:489, items/ItemBucketDeath.java:87)
        // and the 'B' centre of the alembic (oracle ConfigRecipes.java:281), so both stay a bucket.
        JsonObject alembic = ResourceTree.object("data/thaumcraft/recipe/arcane/alembic.json");
        assertEquals("minecraft:bucket", alembic.getAsJsonObject("key").getAsJsonObject("B").get("item").getAsString());
    }
}
