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
     *   <li>{@link #THIRD_PARTY_ORE} - the eleven original recipes the oracle registers only when another mod
     *       supplies the matching ore (Config.foundCopperIngot, foundTinIngot, foundSilverIngot, foundLeadIngot).
     *       Without another mod's ingots there is no copper, lead, silver or tin item in 1.21.1, so the original
     *       never registers them either. They stay deferred on purpose.</li>
     *   <li>The remaining entries are blocked on a native BLOCK or ITEM that still does not exist; the reason for
     *       each one is kept in {@link ResearchRecipeCoverage#deferredRecipes()}.</li>
     * </ul>
     */
    private static final List<String> EXPECTED_MISSING = List.of(
            "ArcanePressurePlate",
            "FluxScrubber",
            "MirrorEssentia",
            "NodeStabilizerAdv",
            "PureCopper",
            "PureLead",
            "PureSilver",
            "PureTin",
            "TransCopper",
            "TransLead",
            "TransSilver",
            "TransTin",
            "WandCapCopper",
            "WandCapSilver",
            "WandCapSilverInert",
            "WandPedFocus");

    /**
     * The eleven original recipes the oracle registers only when another mod supplies copper, lead, silver or tin.
     * They are the part of the remainder that no amount of native content can close.
     */
    private static final List<String> THIRD_PARTY_ORE = List.of(
            "PureCopper", "PureLead", "PureSilver", "PureTin",
            "TransCopper", "TransLead", "TransSilver", "TransTin",
            "WandCapCopper", "WandCapSilver", "WandCapSilverInert");

    /** The research entries whose pages still show an unresolved original recipe. */
    private static final List<String> EXPECTED_RESEARCH = List.of(
            "CAP_copper", "CAP_silver", "FLUXSCRUB", "MIRRORESSENTIA", "NODESTABILIZERADV",
            "PURECOPPER", "PURELEAD", "PURESILVER", "PURETIN",
            "TRANSCOPPER", "TRANSLEAD", "TRANSSILVER", "TRANSTIN",
            "WANDPEDFOC", "WARDEDARCANA");

    /** The original keys the corpus itself stores as a plain List, not as a recipe. */
    private static final List<String> EXPECTED_CONSTRUCTS = List.of(
            "AdvAlchemyFurnace", "ArcTable", "Crucible", "InfernalFurnace", "InfusionAltar", "NodeJar",
            "ResTable", "Thaumatorium", "Thaumonomicon");

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
        assertEquals(250, ids.size());
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
        assertEquals(Set.copyOf(ResearchRecipeCoverage.recipeKeyCounts().keySet()), classified);
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
    void theReportNamesTheUnresolvedKeysAndTheirReasons() {
        String report = ResearchRecipeCoverage.report();
        assertTrue(report.contains("TC4 recipe coverage:"), report);
        assertTrue(report.contains("Unresolved original recipe keys:"), report);
        for (String key : EXPECTED_MISSING) {
            assertTrue(report.contains(key), "Missing from the report: " + key);
        }
    }
}
