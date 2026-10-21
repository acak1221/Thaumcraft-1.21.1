package thaumcraft.research;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import thaumcraft.common.lib.research.ResearchCatalog;
import thaumcraft.common.lib.research.ResearchDefinition;
import thaumcraft.common.lib.research.ResearchRecipeCoverage;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import static org.junit.jupiter.api.Assertions.*;

/**
 * The original TC4 research corpus references 272 original recipe keys. This test pins the exact
 * catalogue coverage: every referenced key must resolve to a native recipe file or to one of the nine
 * original wand constructs, and the remainder must be exactly the documented list below.
 */
class ResearchRecipeCoverageTest {
    /**
     * The original recipes that still have no native recipe.
     *
     * <p>Two groups remain:
     * <ul>
     *   <li>{@link #THIRD_PARTY_ORE} - the eight original recipes the oracle registers only when another mod
     *       supplies the matching ore (Config.foundTinIngot, foundSilverIngot, foundLeadIngot).
     *       Vanilla 1.21.1 has no tin, no silver and no lead at all, so the original never registers them
     *       either. They stay deferred on purpose. Copper is vanilla, so its three original recipes
     *       (PureCopper, TransCopper and WandCapCopper) are carried by this port.</li>
     *   <li>The remaining entry is blocked on a native BLOCK that still does not exist; the reason for
     *       it is kept in {@link ResearchRecipeCoverage#deferredRecipes()}.</li>
     * </ul>
     */
    private static final List<String> EXPECTED_MISSING = List.of(
            "MirrorEssentia",
            "PureLead",
            "PureSilver",
            "PureTin",
            "TransLead",
            "TransSilver",
            "TransTin",
            "WandCapSilver",
            "WandCapSilverInert");

    /**
     * The original recipes that the oracle registers only when another mod supplies tin, silver or lead.
     * They are the part of the remainder that no amount of native content can close.
     */
    private static final List<String> THIRD_PARTY_ORE = List.of(
            "PureLead", "PureSilver", "PureTin",
            "TransLead", "TransSilver", "TransTin",
            "WandCapSilver", "WandCapSilverInert");

    /** The research entries whose pages still show an unresolved original recipe. */
    private static final List<String> EXPECTED_RESEARCH = List.of(
            "CAP_silver", "MIRRORESSENTIA", "PURELEAD", "PURESILVER", "PURETIN",
            "TRANSLEAD", "TRANSSILVER", "TRANSTIN");

    /** The original keys the corpus itself stores as a plain List, not as a recipe. */
    private static final List<String> EXPECTED_CONSTRUCTS = List.of(
            "AdvAlchemyFurnace", "ArcTable", "Crucible", "InfernalFurnace", "InfusionAltar", "NodeJar",
            "ResTable", "Thaumatorium", "Thaumonomicon");

    /**
     * The original recipes that produce ONE metadata value of a family block. The 1.21.1 port has one
     * item per metadata value, so they resolve instead of staying deferred.
     */
    private static final List<String> VARIANT_RECIPES = List.of("PaveTravel", "PaveWard", "WardedGlass",
            "ArcanePressurePlate", "FluxScrubber", "WandPedFocus");

    @BeforeAll
    static void load() { ResearchCatalog.loadBundled(); }

    @Test
    void everyOriginalRecipeKeyIsClassifiedAndTheRemainderIsPinned() {
        assertEquals(EXPECTED_MISSING, ResearchRecipeCoverage.missingRecipeKeys(),
                "The original recipe catalogue is no longer closed over the native registry; update this list.");
        assertEquals(EXPECTED_RESEARCH, ResearchRecipeCoverage.missingResearchKeys());
        assertEquals(Set.copyOf(EXPECTED_MISSING), ResearchRecipeCoverage.deferredRecipes().keySet(),
                "Every unresolved key needs exactly one documented reason.");
        for (String key : EXPECTED_MISSING) {
            assertFalse(ResearchRecipeCoverage.deferredRecipes().get(key).isBlank(), key + " needs a reason");
        }
    }

    @Test
    void theThirdPartyOreRecipesStayDeferredAndAreReportedAsSuch() {
        Map<String, String> deferred = ResearchRecipeCoverage.deferredRecipes();
        for (String key : THIRD_PARTY_ORE) {
            assertTrue(deferred.containsKey(key), key + " must stay deferred");
            assertTrue(deferred.get(key).contains("third-party ore"), key + " must report the third-party ore condition");
        }
        // The third-party ore group is the part of the remainder that no native content can close.
        assertTrue(THIRD_PARTY_ORE.containsAll(EXPECTED_MISSING.stream()
                .filter(key -> deferred.get(key).contains("third-party ore")).toList()));
    }

    @Test
    void everyMappedOriginalKeyResolvesToAPackagedNativeRecipe() {
        Map<String, String> ids = ResearchRecipeCoverage.nativeRecipeIds();
        assertEquals(293, ids.size());
        Set<String> present = ResearchRecipeCoverage.nativeRecipeIdsPresent();
        for (Map.Entry<String, String> entry : ids.entrySet()) {
            String id = entry.getValue();
            assertTrue(id.startsWith("thaumcraft:"), entry.getKey() + " -> " + id);
            assertTrue(present.contains(id), "Missing native recipe file for " + entry.getKey() + ": " + id);
            assertTrue(ResearchRecipeCoverage.isResolved(entry.getKey()), entry.getKey());
            assertEquals(id, ResearchRecipeCoverage.nativeRecipeId(entry.getKey()));
        }
        assertTrue(present.size() >= ids.size());
    }

    /**
     * The original recipes that the research book shows as an ARRAY of recipes: the sixteen banners
     * and the six primal arrows (ConfigRecipes:184-189 and :217-220). They carry no recipe key of
     * their own, but they have a native recipe each.
     */
    @Test
    void theBannersAndThePrimalArrowsAreMappedToTheirNativeRecipes() {
        Map<String, String> ids = ResearchRecipeCoverage.nativeRecipeIds();
        Set<String> present = ResearchRecipeCoverage.nativeRecipeIdsPresent();
        for (int index = 0; index < 16; index++) {
            String key = "Banner_" + index;
            assertTrue(ids.containsKey(key), key + " must be mapped to a native recipe");
            assertTrue(present.contains(ids.get(key)), key + " -> " + ids.get(key));
        }
        for (int index = 0; index < 6; index++) {
            String key = "PrimalArrow_" + index;
            assertTrue(ids.containsKey(key), key + " must be mapped to a native recipe");
            assertTrue(present.contains(ids.get(key)), key + " -> " + ids.get(key));
        }
        assertEquals(36, ResearchRecipeCoverage.outsideBookRecipeKeys().size());
        assertEquals(Set.of("BANNERS", "PRIMALARROW"), ResearchRecipeCoverage.pageRecipeArrays().keySet());
    }

    /**
     * The original registers fourteen more recipes that no research page names by key: the six
     * balanced shards (ConfigRecipes:89), the six aspect clusters and the mixed cluster
     * (ConfigRecipes:442 and :445) and the second kinetic runic girdle (ConfigRecipes:351).
     *
     * <p>The port shipped a native recipe for each of them, but the coverage map did not name the
     * original key, so the catalogue was not closed over the original. This test pins the mapping
     * and the native recipe of every one of them.</p>
     */
    @Test
    void theOriginalRecipesOutsideTheResearchBookAreMappedToTheirNativeRecipes() {
        Map<String, String> ids = ResearchRecipeCoverage.nativeRecipeIds();
        Set<String> present = ResearchRecipeCoverage.nativeRecipeIdsPresent();
        List<String> shards = List.of("air", "fire", "water", "earth", "order", "entropy");
        for (int index = 0; index < shards.size(); index++) {
            String key = "BalancedShard_" + index;
            assertTrue(ids.containsKey(key), key + " must be mapped to a native recipe");
            assertEquals("thaumcraft:crucible/balanced_shard_" + shards.get(index), ids.get(key));
            assertTrue(present.contains(ids.get(key)), key + " -> " + ids.get(key));
        }
        for (int index = 0; index < shards.size(); index++) {
            String key = "Clusters" + index;
            assertTrue(ids.containsKey(key), key + " must be mapped to a native recipe");
            assertEquals("thaumcraft:clusters/" + shards.get(index), ids.get(key));
            assertTrue(present.contains(ids.get(key)), key + " -> " + ids.get(key));
        }
        assertEquals("thaumcraft:clusters/mixed", ids.get("Clusters6"));
        assertTrue(present.contains(ids.get("Clusters6")));
        assertEquals("thaumcraft:infusion/runic_girdle_kinetic_2", ids.get("RunicGirdleKinetic_2"));
        assertTrue(present.contains(ids.get("RunicGirdleKinetic_2")));
        // All fourteen are outside the research book.
        Set<String> outside = ResearchRecipeCoverage.outsideBookRecipeKeys();
        for (int index = 0; index < shards.size(); index++) {
            assertTrue(outside.contains("BalancedShard_" + index));
            assertTrue(outside.contains("Clusters" + index));
        }
        assertTrue(outside.contains("Clusters6"));
        assertTrue(outside.contains("RunicGirdleKinetic_2"));
    }

    @Test
    void theNineOriginalWandConstructsAreExactlyTheCorpusListPages() {
        Set<String> corpusLists = new TreeSet<>();
        for (ResearchDefinition definition : ResearchCatalog.all()) {
            for (ResearchDefinition.Page page : definition.pages()) {
                if (page.recipeKey() == null) continue;
                String expression = page.javaExpression();
                if (expression != null && expression.startsWith("(List)recipes.get(")) corpusLists.add(page.recipeKey());
            }
        }
        assertEquals(Set.copyOf(EXPECTED_CONSTRUCTS), corpusLists,
                "The original stores those keys as plain lists, never as recipes.");
        assertEquals(Set.copyOf(EXPECTED_CONSTRUCTS), ResearchRecipeCoverage.constructBlueprints().keySet());
        for (String key : EXPECTED_CONSTRUCTS) {
            assertTrue(ResearchRecipeCoverage.isResolved(key), key);
            assertFalse(ResearchRecipeCoverage.deferredRecipes().containsKey(key), key);
        }
    }

    @Test
    void theCorpusReferenceCountsStayTheOriginalOnes() {
        Map<String, Integer> counts = ResearchRecipeCoverage.recipeKeyCounts();
        assertEquals(272, counts.size());
        assertEquals(273, counts.values().stream().mapToInt(Integer::intValue).sum());
        assertEquals(2, counts.get("Thaumometer"));
        for (Map.Entry<String, Integer> entry : counts.entrySet()) {
            if (!"Thaumometer".equals(entry.getKey())) assertEquals(1, entry.getValue(), entry.getKey());
        }
    }

    @Test
    void everyCorpusRecipeKeyIsMappedConstructedOrDeferred() {
        Set<String> classified = new TreeSet<>(ResearchRecipeCoverage.nativeRecipeIds().keySet());
        classified.addAll(ResearchRecipeCoverage.constructBlueprints().keySet());
        classified.addAll(ResearchRecipeCoverage.deferredRecipes().keySet());
        Set<String> corpus = new TreeSet<>(ResearchRecipeCoverage.recipeKeyCounts().keySet());
        // The only original recipes outside the corpus are the ones the book shows as an array.
        Set<String> outside = new TreeSet<>(classified);
        outside.removeAll(corpus);
        assertEquals(ResearchRecipeCoverage.outsideBookRecipeKeys(), outside);
        assertTrue(corpus.stream().allMatch(classified::contains));
    }

    @Test
    void everyMappedNativeRecipeKeepsARealResearchGate() {
        Set<String> corpusResearch = new TreeSet<>();
        for (ResearchDefinition definition : ResearchCatalog.all()) corpusResearch.add(definition.key());
        Map<String, String> gates = ResearchRecipeCoverage.nativeResearchGates();
        assertEquals(ResearchRecipeCoverage.nativeRecipeIds().keySet(), gates.keySet());
        for (Map.Entry<String, String> entry : gates.entrySet()) {
            String gate = entry.getValue();
            if (gate == null) continue;
            assertTrue(corpusResearch.contains(gate),
                    entry.getKey() + " -> " + gate + " is not a research key of the original corpus");
        }
    }

    @Test
    void onlyOrdinaryCraftingRecipesStayWithoutAResearchGate() {
        Set<String> gatedTypes = Set.of("thaumcraft:arcane_shaped", "thaumcraft:arcane_shapeless",
                "thaumcraft:crucible", "thaumcraft:infusion_stack", "thaumcraft:infusion_enchantment",
                "thaumcraft:infusion_runic_augment");
        for (Map.Entry<String, String> entry : ResearchRecipeCoverage.nativeRecipeIds().entrySet()) {
            String type = recipeType(entry.getValue());
            String gate = ResearchRecipeCoverage.nativeResearchGate(entry.getKey());
            if (gatedTypes.contains(type)) {
                assertNotNull(gate, entry.getKey() + " -> " + type + " must keep the original research gate");
            }
        }
    }

    private static String recipeType(String recipeId) {
        String path = recipeId.substring(recipeId.indexOf(':') + 1);
        try (var stream = ResearchRecipeCoverageTest.class.getClassLoader()
                .getResourceAsStream("data/thaumcraft/recipe/" + path + ".json")) {
            assertNotNull(stream, "Missing native recipe file " + recipeId);
            return com.google.gson.JsonParser.parseReader(
                    new java.io.InputStreamReader(stream, java.nio.charset.StandardCharsets.UTF_8))
                    .getAsJsonObject().get("type").getAsString();
        } catch (java.io.IOException exception) {
            throw new IllegalStateException("Cannot read " + recipeId, exception);
        }
    }

    @Test
    void theRecipesThatProduceOneMetadataValueResolveToTheirVariantItem() {
        Map<String, String> ids = ResearchRecipeCoverage.nativeRecipeIds();
        Set<String> present = ResearchRecipeCoverage.nativeRecipeIdsPresent();
        for (String key : VARIANT_RECIPES) {
            assertTrue(ids.containsKey(key), key + " must be mapped to a native recipe");
            assertFalse(ResearchRecipeCoverage.deferredRecipes().containsKey(key), key + " must not stay deferred");
            assertTrue(present.contains(ids.get(key)), key + " -> " + ids.get(key));
            assertTrue(ResearchRecipeCoverage.isResolved(key), key);
        }
        assertEquals("thaumcraft:arcane/paving_stone_travel", ids.get("PaveTravel"));
        assertEquals("thaumcraft:arcane/paving_stone_warding", ids.get("PaveWard"));
        assertEquals("thaumcraft:arcane/warded_glass", ids.get("WardedGlass"));
        assertEquals("thaumcraft:arcane/arcane_pressure_plate", ids.get("ArcanePressurePlate"));
        assertEquals("thaumcraft:arcane/flux_scrubber", ids.get("FluxScrubber"));
        assertEquals("thaumcraft:infusion/wand_pedestal_focus", ids.get("WandPedFocus"));
    }

    /**
     * The copper content of the original is carried because copper is vanilla in 1.21.1: the copper
     * nugget, the native copper cluster, the copper wand cap and their three original recipes.
     */
    @Test
    void theVanillaCopperContentOfTheOriginalIsCarried() {
        Map<String, String> ids = ResearchRecipeCoverage.nativeRecipeIds();
        assertEquals("thaumcraft:arcane/wand_cap_copper", ids.get("WandCapCopper"));
        assertEquals("thaumcraft:crucible/pure_copper", ids.get("PureCopper"));
        assertEquals("thaumcraft:crucible/trans_copper", ids.get("TransCopper"));
        for (String key : List.of("WandCapCopper", "PureCopper", "TransCopper")) {
            assertFalse(ResearchRecipeCoverage.deferredRecipes().containsKey(key), key + " must not stay deferred");
            assertTrue(ResearchRecipeCoverage.isResolved(key), key);
        }
    }

    @Test
    void theReportNamesTheUnresolvedKeysAndTheirReasons() {
        String report = ResearchRecipeCoverage.report();
        assertTrue(report.contains("TC4 recipe coverage:"), report);
        assertTrue(report.contains("Unresolved original recipe keys:"), report);
        for (String key : EXPECTED_MISSING) {
            assertTrue(report.contains(key), "Missing from the report: " + key);
        }
    }
}
