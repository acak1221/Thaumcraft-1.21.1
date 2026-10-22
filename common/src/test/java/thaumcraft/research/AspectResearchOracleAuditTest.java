package thaumcraft.research;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.Test;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.api.aspects.AspectList;
import thaumcraft.common.lib.research.ObjectAspectCatalog;
import thaumcraft.common.lib.research.PlayerKnowledge;
import thaumcraft.common.lib.research.ResearchAspectCombination;
import thaumcraft.common.lib.research.ResearchCatalog;

import static org.junit.jupiter.api.Assertions.*;

/** The fixture's amounts/components are extracted from the private original, not the native tags. */
class AspectResearchOracleAuditTest {
    private static JsonObject oracle() throws Exception {
        try (var reader = new InputStreamReader(AspectResearchOracleAuditTest.class.getResourceAsStream(
                "/thaumcraft/aspect_research_oracle.json"), StandardCharsets.UTF_8)) {
            return JsonParser.parseReader(reader).getAsJsonObject();
        }
    }

    @Test
    void allResearchIsInTheParentClosureAndVisibleCoordinatesNeverOverlap() {
        ResearchCatalog.loadBundled();
        assertEquals(201, ResearchCatalog.all().size());
        Set<String> reached = new HashSet<>();
        int previous;
        do {
            previous = reached.size();
            for (var research : ResearchCatalog.all()) {
                if (research.hasPrerequisites(reached)) reached.add(research.key());
            }
        } while (reached.size() != previous);
        Set<String> unreachable = new HashSet<>();
        Set<String> positions = new HashSet<>();
        for (var research : ResearchCatalog.all()) {
            if (!reached.contains(research.key())) unreachable.add(research.key());
            if (!research.isVirtual()) {
                assertTrue(positions.add(research.category() + ":" + research.column() + ":" + research.row()), research.key());
            }
        }
        assertEquals(Set.of(), unreachable, "Unreachable declared research (normal and hidden parents)");
    }

    @Test
    void nativeDirectItemAndBlockTagsMatchOriginalLiteralRegistrations() throws Exception {
        ObjectAspectCatalog.loadBundled();
        var tags = oracle().getAsJsonObject("tags");
        assertTrue(tags.size() >= 80);
        for (var entry : tags.entrySet()) {
            Map<String, Integer> expected = new LinkedHashMap<>();
            entry.getValue().getAsJsonObject().getAsJsonObject("aspects").entrySet().forEach(
                    amount -> expected.put(amount.getKey(), amount.getValue().getAsInt()));
            assertEquals(expected, amounts(ObjectAspectCatalog.get(ResourceLocation.parse(entry.getKey()))),
                    entry.getKey() + " oracle ConfigAspects:" + entry.getValue().getAsJsonObject().get("source_line"));
        }
    }

    @Test
    void everyOriginalCompoundPaysOneOfEachInputAndAwardsOnePlusFirstDiscoveryTwo() throws Exception {
        var combinations = oracle().getAsJsonObject("combinations");
        assertEquals(42, combinations.size());
        for (var entry : combinations.entrySet()) {
            String output = entry.getKey();
            var parents = entry.getValue().getAsJsonArray();
            assertEquals(2, parents.size(), output);
            String left = parents.get(0).getAsString(), right = parents.get(1).getAsString();
            assertSame(Aspect.getAspect(output), Aspect.getCombinationResult(Aspect.getAspect(left), Aspect.getAspect(right)), output);
            assertSame(Aspect.getAspect(output), Aspect.getCombinationResult(Aspect.getAspect(right), Aspect.getAspect(left)), output);
            var actor = new PlayerKnowledge();
            actor.setAspectPool(left, 2);
            actor.setAspectPool(right, 2);
            var first = ResearchAspectCombination.plan(actor, Map.of(), left, right);
            assertNotNull(first, output);
            assertEquals(output, first.output().getTag());
            assertTrue(first.discovery(), output);
            assertEquals(3, first.award(), output);
            assertEquals(1, first.knowledge().getAspectPoolFor(left), output);
            assertEquals(1, first.knowledge().getAspectPoolFor(right), output);
            assertEquals(3, first.knowledge().getAspectPoolFor(output), output);
            assertEquals(2, actor.getAspectPoolFor(left), "Plan must not mutate the actor");
            var repeat = ResearchAspectCombination.plan(first.knowledge(), Map.of(), right, left);
            assertNotNull(repeat, output);
            assertEquals(1, repeat.award(), output);
            assertFalse(repeat.discovery(), output);
            assertEquals(0, repeat.knowledge().getAspectPoolFor(left), output);
            assertEquals(0, repeat.knowledge().getAspectPoolFor(right), output);
            assertEquals(4, repeat.knowledge().getAspectPoolFor(output), output);
        }
    }

    private static Map<String, Integer> amounts(AspectList aspects) {
        Map<String, Integer> result = new LinkedHashMap<>();
        for (Aspect aspect : aspects.getAspects()) result.put(aspect.getTag(), aspects.getAmount(aspect));
        return result;
    }
}
