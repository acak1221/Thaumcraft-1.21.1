package thaumcraft.research;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.CancellationException;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.common.lib.research.ResearchCatalog;
import thaumcraft.common.lib.research.ResearchDefinition;
import thaumcraft.common.lib.research.ResearchNoteData;
import thaumcraft.common.lib.research.ResearchNoteData.Cell;
import thaumcraft.common.lib.research.ResearchNoteGenerator;
import static org.junit.jupiter.api.Assertions.*;

class ResearchNoteGeneratorTest {
    @BeforeAll
    static void load() { ResearchCatalog.loadBundled(); }

    @Test
    void privateJava8VectorsPreserveFullGridOrderHolesAndFollowingRandomState() throws Exception {
        // Goldens were executed independently using the unchanged private
        // original HexUtils and adapted createNote on Java 8, then crosschecked
        // byte-for-byte on Java 21. No decompiled source is shipped as test data.
        var stream = getClass().getResourceAsStream("/thaumcraft/research/note-generation-java8.json");
        assertNotNull(stream);
        JsonObject root;
        try (var reader = new InputStreamReader(stream, StandardCharsets.UTF_8)) {
            root = JsonParser.parseReader(reader).getAsJsonObject();
        }
        assertEquals(1, root.get("schema_version").getAsInt());
        var vectors = root.getAsJsonArray("vectors");
        assertEquals(516, vectors.size());
        Set<String> keys = new HashSet<>();
        for (var element : vectors) {
            JsonObject vector = element.getAsJsonObject();
            String key = vector.get("key").getAsString();
            long seed = vector.get("seed").getAsLong();
            String label = key + " / seed " + seed;
            keys.add(key);
            var random = new TrackingRandom(seed);
            ResearchNoteData actual = ResearchNoteGenerator.generate(key, random);
            List<Cell> cells = new ArrayList<>();
            for (var cellElement : vector.getAsJsonArray("cells")) {
                var cell = cellElement.getAsJsonObject();
                cells.add(new Cell(cell.get("q").getAsInt(), cell.get("r").getAsInt(), cell.get("type").getAsInt(),
                        cell.get("aspect").isJsonNull() ? null : cell.get("aspect").getAsString()));
            }
            assertEquals(key, actual.key(), label);
            assertEquals(vector.get("color").getAsInt(), actual.color(), label);
            assertFalse(actual.complete(), label);
            assertEquals(0, actual.copies(), label);
            assertEquals(cells, actual.cells(), "complete original HashMap cell order: " + label);
            List<Integer> bounds = new ArrayList<>();
            vector.getAsJsonArray("draw_bounds").forEach(bound -> bounds.add(bound.getAsInt()));
            assertEquals(bounds, random.bounds, "bounded draws, including retries: " + label);
            assertEquals(vector.get("next_long").getAsLong(), random.nextLong(), "following RNG state: " + label);

            int radius = 1 + vector.get("complexity").getAsInt();
            Set<String> holes = new TreeSet<>();
            for (int q = -radius; q <= radius; q++) {
                for (int r = -radius; r <= radius; r++) {
                    if (distance(q, r) <= radius && actual.cellAt(q, r) == null) holes.add(q + ":" + r);
                }
            }
            Set<String> expectedHoles = new TreeSet<>();
            vector.getAsJsonArray("holes").forEach(hole -> expectedHoles.add(hole.getAsString()));
            assertEquals(expectedHoles, holes, label);
            assertCanonicalShape(ResearchCatalog.get(key), actual, label);
        }
        assertEquals(172, keys.size());
    }

    @Test
    void explicitComplexityOneTwoAndThreeVectorsExposeDifferentGridsAndHoles() {
        Random one = new Random(42);
        var fire = ResearchNoteGenerator.generate("FOCUSFIRE", one);
        assertEquals(19, fire.cells().size());
        assertEquals(new Cell(-2, 2, 1, "ignis"), fire.cellAt(-2, 2));
        assertEquals(new Cell(2, -2, 1, "praecantatio"), fire.cellAt(2, -2));
        assertEquals(Aspect.FIRE.getColor(), fire.color());
        assertEquals(1008396158678580193L, one.nextLong());

        Random two = new Random(42);
        var bats = ResearchNoteGenerator.generate("FOCUSHELLBAT", two);
        assertEquals(33, bats.cells().size());
        assertEquals(new Cell(-3, 3, 1, "iter"), bats.cellAt(-3, 3));
        assertEquals(new Cell(2, 1, 1, "bestia"), bats.cellAt(2, 1));
        for (int[] hole : new int[][]{{-2, 2}, {0, 3}, {3, -2}, {3, 0}}) assertNull(bats.cellAt(hole[0], hole[1]));
        assertEquals(Aspect.BEAST.getColor(), bats.color());
        assertEquals(-1068554554583376509L, two.nextLong());

        Random three = new Random(42);
        var staff = ResearchNoteGenerator.generate("ROD_primal_staff", three);
        assertEquals(55, staff.cells().size());
        for (int[] hole : new int[][]{{-1, -2}, {-2, 2}, {-3, 0}, {0, -2}, {2, 0}, {3, -3}}) assertNull(staff.cellAt(hole[0], hole[1]));
        assertEquals(Aspect.MAGIC.getColor(), staff.color());
        assertEquals(-5391787952107853122L, three.nextLong());
    }

    @Test
    void primaryTiesAndFixedAssignmentsUseInsertionOrderAndDoNotSortCosts() {
        ResearchDefinition original = ResearchCatalog.get("FOCUSFIRE");
        LinkedHashMap<String, Integer> reversedCosts = new LinkedHashMap<>();
        reversedCosts.put("praecantatio", 3);
        reversedCosts.put("ignis", 3);
        ResearchDefinition reversed = with(original, 1, reversedCosts);
        var first = ResearchNoteGenerator.generate(original, new Random(0));
        var second = ResearchNoteGenerator.generate(reversed, new Random(0));
        assertEquals(Aspect.FIRE.getColor(), first.color());
        assertEquals(Aspect.MAGIC.getColor(), second.color());
        assertEquals(new Cell(-2, 2, 1, "ignis"), first.cellAt(-2, 2));
        assertEquals(new Cell(-2, 2, 1, "praecantatio"), second.cellAt(-2, 2));
        assertEquals(new Cell(2, -2, 1, "ignis"), second.cellAt(2, -2));
        assertEquals(List.of("praecantatio", "ignis"), List.copyOf(reversed.aspectCost().keySet()));
        assertEquals(List.of("praecantatio", "machina", "fabrico"),
                List.copyOf(ResearchCatalog.get("INFUSION").aspectCost().keySet()));
        assertEquals(Aspect.MAGIC.getColor(), ResearchNoteGenerator.generate("INFUSION", new Random(0)).color());
    }

    @Test
    void bothOverloadsMatchForEveryAspectfulDefinitionAndNeverMutateItsCosts() {
        int count = 0;
        for (var research : ResearchCatalog.all()) {
            if (research.aspectCost().isEmpty()) continue;
            List<Map.Entry<String, Integer>> before = List.copyOf(research.aspectCost().entrySet());
            var byDefinition = ResearchNoteGenerator.generate(research, new Random(42));
            var byKey = ResearchNoteGenerator.generate(research.key(), new Random(42));
            assertEquals(byDefinition, byKey, research.key());
            assertEquals(before, List.copyOf(research.aspectCost().entrySet()), research.key());
            count++;
        }
        assertEquals(172, count);
    }

    @Test
    void invalidComplexityUnknownResearchAndBadCostsRejectBeforeRandomConsumption() {
        var random = new TrackingRandom(42);
        var base = ResearchCatalog.get("FOCUSFIRE");
        for (int complexity : new int[]{Integer.MIN_VALUE, -1, 0, 4, 100, Integer.MAX_VALUE}) {
            assertThrows(IllegalArgumentException.class, () -> ResearchNoteGenerator.generate(with(base, complexity, base.aspectCost()), random));
        }
        assertThrows(IllegalArgumentException.class, () -> ResearchNoteGenerator.generate("NO_SUCH_RESEARCH", random));
        assertThrows(IllegalArgumentException.class, () -> ResearchNoteGenerator.generate((String) null, random));
        assertThrows(IllegalArgumentException.class, () -> ResearchNoteGenerator.generate(withKey(base, "NO_SUCH_RESEARCH"), random));
        assertThrows(IllegalArgumentException.class, () -> ResearchNoteGenerator.generate(withKey(base, null), random));
        String aspectlessKey = ResearchCatalog.all().stream().filter(research -> research.aspectCost().isEmpty())
                .findFirst().orElseThrow().key();
        assertThrows(IllegalArgumentException.class, () -> ResearchNoteGenerator.generate(withKey(base, aspectlessKey), random));
        assertThrows(IllegalArgumentException.class, () -> ResearchNoteGenerator.generate(with(base, 1, Map.of()), random));
        assertThrows(IllegalArgumentException.class, () -> ResearchNoteGenerator.generate(with(base, 1, Map.of("aer", 0)), random));
        assertThrows(IllegalArgumentException.class, () -> ResearchNoteGenerator.generate(with(base, 1, Map.of("not_an_aspect", 2)), random));
        assertTrue(random.bounds.isEmpty());
        assertEquals(new Random(42).nextLong(), random.nextLong());
    }

    @Test
    void explicitCancellationAndOutOfRangeRandomFailWithoutAnArbitraryRetryCap() {
        try {
            Thread.currentThread().interrupt();
            assertThrows(CancellationException.class, () -> ResearchNoteGenerator.generate("FOCUSHELLBAT", new Random(42)));
        } finally {
            Thread.interrupted();
        }
        Random invalid = new Random(42) {
            @Override public int nextInt(int bound) { return bound; }
        };
        assertThrows(IllegalArgumentException.class, () -> ResearchNoteGenerator.generate("FOCUSHELLBAT", invalid));
    }

    private static void assertCanonicalShape(ResearchDefinition research, ResearchNoteData data, String label) {
        int complexity = research.complexity();
        int radius = 1 + complexity;
        int fullSize = 1 + 3 * radius * (radius + 1);
        int holes = complexity > 1 ? complexity * 2 : 0;
        assertEquals(fullSize - holes, data.cells().size(), label);
        assertEquals(research.aspectCost().size(), data.cells().stream().filter(cell -> cell.type() == 1).count(), label);
        Set<String> fixed = new HashSet<>();
        for (Cell cell : data.cells()) {
            assertTrue(distance(cell.q(), cell.r()) <= radius, label);
            if (cell.type() == 1) {
                assertEquals(radius, distance(cell.q(), cell.r()), label);
                assertTrue(fixed.add(cell.aspect()), label);
            } else {
                assertEquals(0, cell.type(), label);
                assertNull(cell.aspect(), label);
            }
        }
        assertEquals(research.aspectCost().keySet(), fixed, label);
    }

    private static int distance(int q, int r) { return Math.max(Math.abs(q), Math.max(Math.abs(r), Math.abs(q + r))); }

    private static ResearchDefinition with(ResearchDefinition source, int complexity, Map<String, Integer> costs) {
        return new ResearchDefinition(source.key(), source.category(), source.column(), source.row(), complexity, source.warp(),
                costs, source.flags(), source.parents(), source.parentsHidden(), source.siblings(), source.aspectTriggers(),
                source.entityTriggers(), source.itemTriggerExpressions(), source.iconExpression(), source.pages(), source.conditions());
    }

    private static ResearchDefinition withKey(ResearchDefinition source, String key) {
        return new ResearchDefinition(key, source.category(), source.column(), source.row(), source.complexity(), source.warp(),
                source.aspectCost(), source.flags(), source.parents(), source.parentsHidden(), source.siblings(), source.aspectTriggers(),
                source.entityTriggers(), source.itemTriggerExpressions(), source.iconExpression(), source.pages(), source.conditions());
    }

    private static final class TrackingRandom extends Random {
        private final List<Integer> bounds = new ArrayList<>();
        TrackingRandom(long seed) { super(seed); }
        @Override public int nextInt(int bound) { bounds.add(bound); return super.nextInt(bound); }
    }
}
