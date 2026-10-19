package thaumcraft.research;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import thaumcraft.common.lib.research.ResearchCatalog;
import thaumcraft.common.lib.research.ResearchDefinition;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

class ResearchCatalogTest {
    @BeforeAll
    static void load() { ResearchCatalog.loadBundled(); }

    @Test
    void researchAndTextCorporaHaveOriginalFingerprints() throws Exception {
        assertEquals(201, ResearchCatalog.all().size());
        Set<String> categories = ResearchCatalog.all().stream().map(ResearchDefinition::category).collect(Collectors.toSet());
        assertEquals(Set.of("BASICS", "THAUMATURGY", "ALCHEMY", "ARTIFICE", "GOLEMANCY", "ELDRITCH"), categories);
        TreeSet<String> pairs = new TreeSet<>();
        TreeSet<String> pages = new TreeSet<>();
        int gated = 0;
        for (ResearchDefinition research : ResearchCatalog.all()) {
            pairs.add(research.key() + "|" + research.category());
            for (ResearchDefinition.Page page : research.pages()) {
                if (page.requiredResearch() != null) gated++;
                if (page.textKey() != null && page.requiredResearch() == null && page.textKey().startsWith("tc.research_page.")) {
                    pages.add(page.textKey());
                }
            }
        }
        assertEquals(294, pages.size());
        assertEquals(15, gated);
        assertEquals("397d620877d429c49d4db6ae65552a681ac5b26be5d60cd1e95e5e28cae9f40e", fingerprint(pairs));
        assertEquals("66a10cd39a47e78472134a35d3b7b4e1b78618996ac6c87907aeb63303d4bf38", fingerprint(pages));
    }

    @Test
    void hiddenParentsAndGatedPagesRemainEffectiveData() {
        ResearchDefinition hellbat = ResearchCatalog.get("FOCUSHELLBAT");
        assertTrue(hellbat.isHidden());
        assertFalse(hellbat.hasPrerequisites(Set.of("FOCUSFIRE")));
        assertTrue(hellbat.hasPrerequisites(Set.of("FOCUSFIRE", "INFUSION")));
        assertEquals(2, hellbat.warp());
        assertEquals(6, hellbat.aspectCost().get("bestia"));
        ResearchDefinition.Page gated = ResearchCatalog.get("ARCANELAMP").pages().stream()
                .filter(page -> page.requiredResearch() != null).findFirst().orElseThrow();
        assertFalse(gated.isVisible(Set.of()));
        assertTrue(gated.isVisible(Set.of("ARCANEBORE")));
    }

    @Test
    void conditionsAndRecipeReferencesDoNotPretendToBeExecutableRecipes() {
        ResearchDefinition mirror = ResearchCatalog.get("MIRROR");
        assertEquals(java.util.List.of("if (Config.allowMirrors)"), mirror.conditions());
        ResearchDefinition.Page recipe = ResearchCatalog.get("FOCUSFIRE").pages().stream()
                .filter(page -> "recipe_reference".equals(page.kind())).findFirst().orElseThrow();
        assertEquals("FocusFire", recipe.recipeKey());
        assertTrue(recipe.javaExpression().contains("recipes.get"));
        assertThrows(UnsupportedOperationException.class, () -> mirror.parents().add("FAKE"));
        assertTrue(ResearchCatalog.unconditionalAutoUnlocks().contains("CAP_iron"));
        assertTrue(ResearchCatalog.unconditionalAutoUnlocks().contains("ROD_wood"));
        assertNull(ResearchCatalog.get("FAKE"));
    }

    @Test
    void valvesAndResonatorShareTheOriginalTubeResearchNotTheLaterFilterGate() {
        var tubes = ResearchCatalog.get("TUBES");
        assertEquals("ALCHEMY", tubes.category());
        assertEquals(7, tubes.column());
        assertEquals(0, tubes.row());
        assertEquals(1, tubes.complexity());
        assertEquals(java.util.List.of("DISTILESSENTIA"), tubes.parents());
        assertTrue(tubes.isSecondary());
        assertTrue(tubes.flags().contains("Concealed"));
        assertEquals(java.util.Map.of("aqua", 3, "praecantatio", 3, "permutatio", 3), tubes.aspectCost());
        assertEquals(java.util.List.of("Tube", "TubeValve", "Resonator"), tubes.pages().stream()
                .filter(page -> "recipe_reference".equals(page.kind())).map(ResearchDefinition.Page::recipeKey).toList());
        assertEquals(java.util.List.of("TUBES"), ResearchCatalog.get("TUBEFILTER").parents());
        assertFalse(tubes.hasPrerequisites(Set.of()));
        assertTrue(tubes.hasPrerequisites(Set.of("DISTILESSENTIA")));
    }

    @Test
    void laterTubeVariantsKeepTheirDistinctOriginalFilterResearchGate() {
        var filter = ResearchCatalog.get("TUBEFILTER");
        assertEquals("ALCHEMY", filter.category());
        assertEquals(9, filter.column());
        assertEquals(1, filter.row());
        assertEquals(2, filter.complexity());
        assertTrue(filter.isSecondary());
        assertTrue(filter.flags().contains("Concealed"));
        assertEquals(java.util.Map.of("aqua", 3, "praecantatio", 3, "permutatio", 3, "ordo", 3), filter.aspectCost());
        assertEquals(java.util.List.of("TUBES"), filter.parents());
        assertFalse(filter.hasPrerequisites(Set.of("DISTILESSENTIA")));
        assertTrue(filter.hasPrerequisites(Set.of("TUBES")));
        assertEquals(java.util.List.of("TubeFilter", "TubeRestrict", "TubeOneway"), filter.pages().stream()
                .filter(page -> "recipe_reference".equals(page.kind())).map(ResearchDefinition.Page::recipeKey).toList());
    }

    @Test
    void bufferKeepsTheOriginalPrimaryCentrifugeGateRatherThanAnEasySecondary() {
        var centrifuge = ResearchCatalog.get("CENTRIFUGE");
        assertEquals("ALCHEMY", centrifuge.category());
        assertEquals(10, centrifuge.column());
        assertEquals(0, centrifuge.row());
        assertEquals(2, centrifuge.complexity());
        assertFalse(centrifuge.isSecondary());
        assertTrue(centrifuge.flags().contains("Concealed"));
        assertEquals(java.util.List.of("TUBEFILTER"), centrifuge.parents());
        assertEquals(java.util.Map.of("perditio", 3, "praecantatio", 3, "permutatio", 3, "fabrico", 3), centrifuge.aspectCost());
        assertFalse(centrifuge.hasPrerequisites(Set.of("TUBES")));
        assertTrue(centrifuge.hasPrerequisites(Set.of("TUBEFILTER")));
        assertEquals(java.util.List.of("Centrifuge", "TubeBuffer"), centrifuge.pages().stream()
                .filter(page -> "recipe_reference".equals(page.kind())).map(ResearchDefinition.Page::recipeKey).toList());
    }

    @Test
    void crystallizerKeepsItsOriginalPrimaryTubeGateAndOriginalRecipeSpelling() {
        var crystallizer = ResearchCatalog.get("ESSENTIACRYSTAL");
        assertEquals("ALCHEMY", crystallizer.category());
        assertEquals(8, crystallizer.column());
        assertEquals(-2, crystallizer.row());
        assertEquals(1, crystallizer.complexity());
        assertFalse(crystallizer.isSecondary());
        assertTrue(crystallizer.flags().contains("Concealed"));
        assertFalse(crystallizer.flags().contains("Stub"));
        assertEquals(java.util.List.of("TUBES"), crystallizer.parents());
        assertEquals(java.util.Map.of("aqua", 5, "vitreus", 5, "permutatio", 3, "praecantatio", 5), crystallizer.aspectCost());
        assertFalse(crystallizer.hasPrerequisites(Set.of("DISTILESSENTIA")));
        assertTrue(crystallizer.hasPrerequisites(Set.of("TUBES")));
        assertEquals(java.util.List.of("EssentiaCrystalizer"), crystallizer.pages().stream()
                .filter(page -> "recipe_reference".equals(page.kind())).map(ResearchDefinition.Page::recipeKey).toList());
    }

    @Test
    void basicNodeStabilizerKeepsItsOriginalPrimaryPreservationGate() {
        var stabilizer = ResearchCatalog.get("NODESTABILIZER");
        assertEquals("THAUMATURGY", stabilizer.category());
        assertEquals(-7, stabilizer.column());
        assertEquals(-4, stabilizer.row());
        assertEquals(1, stabilizer.complexity());
        assertFalse(stabilizer.isSecondary());
        assertFalse(stabilizer.flags().contains("Concealed"));
        assertFalse(stabilizer.flags().contains("Stub"));
        assertEquals(java.util.List.of("NODEPRESERVE"), stabilizer.parents());
        assertEquals(java.util.Map.of("auram", 4, "ordo", 4, "potentia", 4), stabilizer.aspectCost());
        assertFalse(stabilizer.hasPrerequisites(Set.of("NODES")));
        assertTrue(stabilizer.hasPrerequisites(Set.of("NODEPRESERVE")));
        assertEquals(java.util.List.of("NodeStabilizer"), stabilizer.pages().stream()
                .filter(page -> "recipe_reference".equals(page.kind())).map(ResearchDefinition.Page::recipeKey).toList());
    }

    @Test
    void harnessingVisKeepsItsPrimarySpecialGateAndOriginalInterleavedPages() {
        var power = ResearchCatalog.get("VISPOWER");
        assertEquals("THAUMATURGY", power.category());
        assertEquals(-5, power.column());
        assertEquals(-6, power.row());
        assertEquals(2, power.complexity());
        assertFalse(power.isSecondary());
        assertTrue(power.flags().contains("Special"));
        assertFalse(power.flags().contains("Concealed"));
        assertFalse(power.flags().contains("Stub"));
        assertEquals(java.util.List.of("NODESTABILIZER"), power.parents());
        assertEquals(java.util.Map.of("auram", 3, "machina", 3, "potentia", 6), power.aspectCost());
        assertFalse(power.hasPrerequisites(Set.of("NODEPRESERVE")));
        assertTrue(power.hasPrerequisites(Set.of("NODESTABILIZER")));
        assertEquals(java.util.List.of("text", "recipe_reference", "text", "text", "recipe_reference", "text", "text"),
                power.pages().stream().map(ResearchDefinition.Page::kind).toList());
        assertEquals(java.util.List.of("NodeTransducer", "NodeRelay"), power.pages().stream()
                .filter(page -> "recipe_reference".equals(page.kind())).map(ResearchDefinition.Page::recipeKey).toList());
    }

    private static String fingerprint(Set<String> values) throws Exception {
        byte[] content = (String.join("\n", values) + "\n").getBytes(StandardCharsets.UTF_8);
        return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(content));
    }
}
