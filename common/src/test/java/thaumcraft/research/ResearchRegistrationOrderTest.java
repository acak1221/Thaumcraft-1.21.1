package thaumcraft.research;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.gson.JsonParser;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import thaumcraft.common.config.ResearchSettings;
import thaumcraft.common.config.ThaumcraftSettings;
import thaumcraft.common.lib.research.ResearchCatalog;
import thaumcraft.common.lib.research.ResearchConditions;
import thaumcraft.common.lib.research.ResearchDefinition;
import thaumcraft.common.lib.research.ResearchRegistrationOrder;

/**
 * The order in which the enabled research is handed to the hidden-note and clue randomness: the six categories in registration
 * order, and inside each category the iteration order of a default {@code HashMap} filled in original registration order.
 */
class ResearchRegistrationOrderTest {
    private static final List<String> CATEGORIES = List.of("BASICS", "THAUMATURGY", "ALCHEMY", "ARTIFICE", "GOLEMANCY", "ELDRITCH");
    private ResearchSettings before;

    @BeforeAll static void load() { ResearchCatalog.loadBundled(); }

    @BeforeEach void remember() { before = ThaumcraftSettings.server(); }

    @AfterEach void restore() { ThaumcraftSettings.configureServer(before); }

    private static List<String> keys(List<ResearchDefinition> definitions) { return definitions.stream().map(ResearchDefinition::key).toList(); }

    @Test void everyEnabledResearchAppearsExactlyOnce() {
        List<String> keys = keys(ResearchRegistrationOrder.enabled());
        assertEquals(keys.size(), new HashSet<>(keys).size(), "a key is listed twice");
        Set<String> expected = ResearchCatalog.all().stream().filter(ResearchDefinition::isEnabled).map(ResearchDefinition::key).collect(Collectors.toSet());
        assertEquals(expected, new HashSet<>(keys));
        assertFalse(keys.isEmpty());
    }

    @Test void theCategoriesFollowEachOtherInRegistrationOrderWithoutInterleaving() {
        List<String> seen = new ArrayList<>();
        for (ResearchDefinition definition : ResearchRegistrationOrder.enabled()) {
            if (seen.isEmpty() || !seen.get(seen.size() - 1).equals(definition.category())) seen.add(definition.category());
        }
        assertEquals(seen.size(), new HashSet<>(seen).size(), "a category came back after another one: " + seen);
        assertEquals(CATEGORIES, seen, "with the default settings every category has enabled research");
    }

    @Test void insideACategoryTheOrderIsTheOriginalHashMapIterationOrder() throws Exception {
        Map<String, Integer> lineOf = new HashMap<>();
        try (var reader = new InputStreamReader(getClass().getClassLoader().getResourceAsStream("data/thaumcraft/tc4/research.json"), StandardCharsets.UTF_8)) {
            for (var element : JsonParser.parseReader(reader).getAsJsonObject().getAsJsonArray("research")) {
                var row = element.getAsJsonObject();
                lineOf.put(row.get("key").getAsString(), row.getAsJsonObject("source").get("line").getAsInt());
            }
        }
        List<ResearchDefinition> inRegistrationOrder = new ArrayList<>(ResearchCatalog.all().stream().filter(ResearchDefinition::isEnabled).toList());
        inRegistrationOrder.sort(Comparator.comparingInt(definition -> lineOf.get(definition.key())));
        Map<String, HashMap<String, ResearchDefinition>> byCategory = new LinkedHashMap<>();
        for (String category : CATEGORIES) byCategory.put(category, new HashMap<>());
        for (ResearchDefinition definition : inRegistrationOrder) byCategory.get(definition.category()).put(definition.key(), definition);
        List<String> expected = new ArrayList<>();
        for (var category : byCategory.values()) expected.addAll(keys(new ArrayList<>(category.values())));
        assertEquals(expected, keys(ResearchRegistrationOrder.enabled()));
    }

    @Test void theListIsTheSameEveryTimeAndCannotBeChangedByACaller() {
        List<ResearchDefinition> first = ResearchRegistrationOrder.enabled();
        assertEquals(keys(first), keys(ResearchRegistrationOrder.enabled()));
        assertThrows(UnsupportedOperationException.class, () -> first.add(first.get(0)));
        assertThrows(UnsupportedOperationException.class, () -> first.remove(0));
        assertThrows(UnsupportedOperationException.class, first::clear);
    }

    @Test void switchingTheWardAndMirrorFeaturesOffDropsExactlyTheResearchThatNeedsThem() {
        List<String> all = keys(ResearchRegistrationOrder.enabled());
        ResearchConditions.setFeatures(false, false);
        List<String> reduced = keys(ResearchRegistrationOrder.enabled());
        assertTrue(reduced.size() < all.size(), "the ward and mirror research disappears with its feature");
        Set<String> removed = new HashSet<>(all);
        removed.removeAll(reduced);
        for (String key : removed) {
            String conditions = String.join(" ", ResearchCatalog.get(key).conditions());
            assertTrue(conditions.contains("wardedStone") || conditions.contains("allowMirrors"), key + " vanished without a feature condition: " + conditions);
        }
        assertTrue(all.containsAll(reduced), "switching a feature off may not add research");
        ResearchConditions.setFeatures(true, true);
        assertEquals(all, keys(ResearchRegistrationOrder.enabled()), "switching back restores the same list in the same order");
    }

    @Test void theOrderDoesNotDependOnTheSettingsThatDoNotGateResearch() {
        List<String> baseline = keys(ResearchRegistrationOrder.enabled());
        ThaumcraftSettings.configureServer(new ResearchSettings(true, true, true, false, true, -1, 7));
        assertEquals(baseline, keys(ResearchRegistrationOrder.enabled()));
    }
}
