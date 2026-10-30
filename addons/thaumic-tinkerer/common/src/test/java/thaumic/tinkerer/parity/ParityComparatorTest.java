package thaumic.tinkerer.parity;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

import org.junit.jupiter.api.Test;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import thaumic.tinkerer.testing.parity.KnownDifference;
import thaumic.tinkerer.testing.parity.OracleData;
import thaumic.tinkerer.testing.parity.ParityComparator;
import thaumic.tinkerer.testing.parity.ParityComparator.Difference;
import thaumic.tinkerer.testing.parity.ParityComparator.Result;
import thaumic.tinkerer.testing.parity.PortSnapshot;
import thaumic.tinkerer.testing.parity.OracleData.Recipe;
import thaumic.tinkerer.testing.parity.PortSnapshot.PortRecipe;
import thaumic.tinkerer.testing.parity.PortSnapshot.PortResearch;

/**
 * The comparison of the port with the original must fail when a position, an aspect amount, a parent or any other
 * compared value differs. The tests take the real oracle (tt_oracle/*.json), build a port that equals it, change one
 * value of a copy of the oracle and expect exactly that difference. Test-only code of the port (no 1.7.10
 * counterpart), CC BY-NC-SA 3.0 like the add-on.
 */
class ParityComparatorTest {
    private static final ParityComparator.Environment PORT = PortFixtures.PORT;

    private static JsonObject copy(String resource) {
        return OracleData.readResource(resource).deepCopy();
    }

    private static JsonObject entry(JsonObject document, String key, int which) {
        int seen = 0;
        for (JsonElement e : document.getAsJsonArray("entries")) {
            JsonObject o = e.getAsJsonObject();
            String id = o.has("key") ? o.get("key").getAsString() : String.valueOf(o.get("name"));
            if (id.equals(key) && seen++ == which) return o;
        }
        throw new AssertionError("no entry " + key);
    }

    private static JsonObject recipeByName(JsonObject document, String name, String kind) {
        for (JsonElement e : document.getAsJsonArray("entries")) {
            JsonObject o = e.getAsJsonObject();
            if (o.has("name") && !o.get("name").isJsonNull() && o.get("name").getAsString().equals(name)
                    && o.get("kind").getAsString().equals(kind) && o.get("registered").getAsBoolean()) {
                return o;
            }
        }
        throw new AssertionError("no recipe " + name);
    }

    private static Result compareMutated(Consumer<JsonObject> research, Consumer<JsonObject> recipes) {
        OracleData real = OracleData.fromClasspath();
        PortSnapshot port = PortFixtures.fromOracle(real);
        JsonObject r = copy(OracleData.RESEARCH_RESOURCE);
        JsonObject c = copy(OracleData.RECIPES_RESOURCE);
        research.accept(r);
        recipes.accept(c);
        return ParityComparator.compare(OracleData.parse(r, c), port, PORT, PortFixtures.KAMI);
    }

    private static Result compareResearchMutated(Consumer<JsonObject> mutation) {
        return compareMutated(mutation, x -> {});
    }

    private static Result compareRecipesMutated(Consumer<JsonObject> mutation) {
        return compareMutated(x -> {}, mutation);
    }

    private static void assertSingleDifference(Result result, String kind, String subject, String field) {
        List<Difference> found = result.failures.stream().filter(d -> d.kind().equals(kind) && d.subject().startsWith(subject)
                && d.field().equals(field)).toList();
        assertFalse(found.isEmpty(), "expected a difference of " + kind + " " + subject + " " + field + " but got:\n" + result.describe());
        assertEquals(1, result.failures.size(), "only that one difference expected:\n" + result.describe());
    }

    /** The difference is among the failures (a changed recipe also changes what the research page of it shows). */
    private static void assertHas(Result result, String kind, String subject, String field) {
        assertTrue(result.failures.stream().anyMatch(d -> d.kind().equals(kind) && d.subject().startsWith(subject)
                && d.field().equals(field)), "expected a difference of " + kind + " " + subject + " " + field + " but got:\n" + result.describe());
    }

    // ---------------------------------------------------------------------------------------- the data

    @Test
    void theOracleIsLoadedAndHasTheSizeOfTheOriginal() {
        OracleData oracle = OracleData.fromClasspath();
        assertEquals("28f4def938e80d7a0b6f433c2b84300dfb7c35ce", oracle.commit());
        assertEquals(78, oracle.research().stream().map(OracleData.Research::key).distinct().count());
        assertEquals(130, oracle.recipes().size());
        assertEquals(17, oracle.objectTags().size());
        assertTrue(oracle.research().stream().anyMatch(r -> r.key().equals("ENCHANTER") && r.column() == 5 && r.row() == 4));
    }

    @Test
    void theFlagsTheEntriesDependOnAreAtTheDefaultsOfTheOriginalOrTheRunIsRefused() {
        OracleData oracle = OracleData.fromClasspath();
        assertEquals(Map.of("Config.allowMirrors", true, "ConfigHandler.enableFire", true, "ConfigHandler.enableKami", true,
                "ConfigHandler.enableSurvivalShareTome", true), oracle.configDefaults());
        assertEquals(List.of(), ParityComparator.configProblems(oracle, PORT));
        ParityComparator.Environment kamiOff = c -> !c.expr().equals("ConfigHandler.enableKami");
        List<String> problems = ParityComparator.configProblems(oracle, kamiOff);
        assertEquals(1, problems.size(), problems.toString());
        assertTrue(problems.get(0).contains("ConfigHandler.enableKami") && problems.get(0).contains("false in the port"), problems.get(0));
        ParityComparator.Environment mirrorsOff = c -> !c.expr().equals("Config.allowMirrors");
        assertEquals(1, ParityComparator.configProblems(oracle, mirrorsOff).size());
    }

    @Test
    void aPortThatEqualsTheOraclePasses() {
        OracleData oracle = OracleData.fromClasspath();
        Result result = ParityComparator.compare(oracle, PortFixtures.fromOracle(oracle), PORT, PortFixtures.KAMI);
        assertTrue(result.passed(), result.describe());
        assertEquals(1, result.allowed.size(), "only the Kami requirements, which the port deliberately widens");
        assertEquals(74, result.counts.get("research compared"), result.counts.toString());
        assertEquals(127, result.counts.get("recipes compared"), result.counts.toString());
        assertEquals(808, result.counts.get("ingredients compared"), result.counts.toString());
        assertEquals(0, result.counts.get("ingredients not comparable (unresolved)"), result.counts.toString());
        assertEquals(17, result.counts.get("wildcard ingredients checked against every variant"), result.counts.toString());
        assertEquals(2, result.counts.get("ingredients that are ore names (the tag of the port may hold more)"), result.counts.toString());
    }

    @Test
    void withoutTheAllowListTheKamiRequirementsAreTheOnlyDifferenceOfAPortThatEqualsTheOracle() {
        OracleData oracle = OracleData.fromClasspath();
        Result result = ParityComparator.compare(oracle, PortFixtures.fromOracle(oracle), PORT, List.of());
        assertEquals(1, result.failures.size(), result.describe());
        assertEquals("<Kami entries>", result.failures.get(0).subject());
        assertEquals("parentsHidden (membership)", result.failures.get(0).field());
        assertTrue(result.notes.stream().anyMatch(n -> n.contains("block-owned research of the add-on") && n.contains("ENCHANTER")), result.notes.toString());
    }

    // ---------------------------------------------------------------------------------------- research mutations

    @Test
    void aChangedPositionFails() {
        assertSingleDifference(compareResearchMutated(r -> entry(r, "ENCHANTER", 0).addProperty("column", 6)),
                "research", "ENCHANTER", "column");
        assertSingleDifference(compareResearchMutated(r -> entry(r, "ENCHANTER", 0).addProperty("row", 9)),
                "research", "ENCHANTER", "row");
    }

    @Test
    void aChangedAspectAmountFails() {
        Result result = compareResearchMutated(r -> {
            JsonObject aspects = entry(r, "ENCHANTER", 0).getAsJsonObject("aspects");
            String first = aspects.keySet().iterator().next();
            aspects.addProperty(first, aspects.get(first).getAsInt() + 1);
        });
        assertSingleDifference(result, "research", "ENCHANTER", "aspects");
        assertTrue(result.failures.get(0).original().contains("praecantatio:3"), result.failures.get(0).toString());
        assertTrue(result.failures.get(0).port().contains("praecantatio:2"), result.failures.get(0).toString());
    }

    @Test
    void aChangedAspectOrderFails() {
        Result result = compareResearchMutated(r -> {
            JsonObject e = entry(r, "ENCHANTER", 0);
            JsonObject swapped = new JsonObject();
            List<String> keys = new ArrayList<>(e.getAsJsonObject("aspects").keySet());
            JsonObject old = e.getAsJsonObject("aspects");
            swapped.add(keys.get(1), old.get(keys.get(1)));
            swapped.add(keys.get(0), old.get(keys.get(0)));
            for (int i = 2; i < keys.size(); i++) swapped.add(keys.get(i), old.get(keys.get(i)));
            e.add("aspects", swapped);
        });
        assertSingleDifference(result, "research", "ENCHANTER", "aspects");
    }

    @Test
    void aChangedParentFails() {
        Result result = compareResearchMutated(r -> {
            JsonArray parents = new JsonArray();
            parents.add("NITOR");
            entry(r, "ENCHANTER", 0).add("parents", parents);
        });
        assertSingleDifference(result, "research", "ENCHANTER", "parents");
        assertEquals("[SPELL_CLOTH]", result.failures.get(0).port());
        assertEquals("[NITOR]", result.failures.get(0).original());
    }

    @Test
    void aChangedHiddenParentFlagComplexityAndWarpFail() {
        assertSingleDifference(compareResearchMutated(r -> {
            JsonArray hidden = new JsonArray();
            hidden.add("THAUMIUM");
            entry(r, "ENCHANTER", 0).add("parentsHidden", hidden);
        }), "research", "ENCHANTER", "parentsHidden");
        assertSingleDifference(compareResearchMutated(r -> entry(r, "ENCHANTER", 0).getAsJsonObject("flags").addProperty("secondary", true)),
                "research", "ENCHANTER", "flag secondary");
        assertSingleDifference(compareResearchMutated(r -> entry(r, "ENCHANTER", 0).addProperty("complexity", 1)),
                "research", "ENCHANTER", "complexity");
        assertSingleDifference(compareResearchMutated(r -> entry(r, "ICHOR", 0).addProperty("warp", 6)),
                "research", "ICHOR", "warp");
        assertSingleDifference(compareResearchMutated(r -> entry(r, "ENCHANTER", 0).addProperty("category", "ALCHEMY")),
                "research", "ENCHANTER", "category");
    }

    @Test
    void aChangedPageFails() {
        assertHas(compareResearchMutated(r -> entry(r, "ENCHANTER", 0).getAsJsonArray("pages").remove(0)),
                "research", "ENCHANTER", "pages.count");
        Result kind = compareResearchMutated(r -> entry(r, "ENCHANTER", 0).getAsJsonArray("pages").get(0).getAsJsonObject().addProperty("kind", "image"));
        assertTrue(kind.failures.stream().anyMatch(d -> d.subject().equals("ENCHANTER") && d.field().equals("pages[0].kind")), kind.describe());
        Result text = compareResearchMutated(r -> entry(r, "ENCHANTER", 0).getAsJsonArray("pages").get(1).getAsJsonObject().addProperty("key", "9"));
        assertSingleDifference(text, "research", "ENCHANTER", "pages[1].text");
        Result recipe = compareResearchMutated(r -> {
            JsonArray refs = new JsonArray();
            refs.add("MAGNET");
            entry(r, "ENCHANTER", 0).getAsJsonArray("pages").get(3).getAsJsonObject().add("recipes", refs);
        });
        assertSingleDifference(recipe, "research", "ENCHANTER", "pages[3].recipes");
    }

    @Test
    void aMissingAndAnUnexpectedResearchFail() {
        OracleData oracle = OracleData.fromClasspath();
        PortSnapshot port = PortFixtures.fromOracle(oracle);
        Map<String, PortResearch> research = new LinkedHashMap<>(port.research());
        PortResearch removed = research.remove("ENCHANTER");
        Result missing = ParityComparator.compare(oracle, new PortSnapshot(research, port.researchPool(), port.recipes()), PORT, PortFixtures.KAMI);
        assertSingleDifference(missing, "research", "ENCHANTER", "presence");
        assertEquals("missing", missing.failures.get(0).port());

        research = new LinkedHashMap<>(port.research());
        research.put("EXTRA_THING", new PortResearch("EXTRA_THING", removed.category(), removed.aspects(), 40, 40, 1, List.of(),
                List.of(), List.of(), removed.flags(), 0, List.of(), null));
        Result extra = ParityComparator.compare(oracle, new PortSnapshot(research, port.researchPool(), port.recipes()), PORT, PortFixtures.KAMI);
        assertSingleDifference(extra, "research", "EXTRA_THING", "presence");
        assertEquals("not in the original", extra.failures.get(0).original());
    }

    @Test
    void anEntryUnderAModThatDoesNotExistIsNotExpectedButItsPresenceAndFieldsAreChecked() {
        OracleData oracle = OracleData.fromClasspath();
        PortSnapshot port = PortFixtures.fromOracle(oracle);
        assertFalse(port.research().containsKey("ASPECT_ANALYZER"), "ComputerCraft is not in the port");
        Result clean = ParityComparator.compare(oracle, port, PORT, PortFixtures.KAMI);
        assertTrue(clean.passed(), clean.describe());

        OracleData.Research analyzer = oracle.research().stream().filter(r -> r.key().equals("ASPECT_ANALYZER")).findFirst().orElseThrow();
        PortResearch registered = PortFixtures.portResearch(analyzer, port.researchPool(), PortFixtures.lastByName(oracle));
        Map<String, PortResearch> research = new LinkedHashMap<>(port.research());
        research.put("ASPECT_ANALYZER", registered);
        Result present = ParityComparator.compare(oracle, new PortSnapshot(research, port.researchPool(), port.recipes()), PORT, PortFixtures.KAMI);
        assertSingleDifference(present, "research", "ASPECT_ANALYZER", "presence");
        assertEquals("registered only when LoadedMods.computerCraftLoaded", present.failures.get(0).original());

        // and what the port registers instead is still held against the original's definition
        research.put("ASPECT_ANALYZER", new PortResearch(registered.key(), registered.category(), registered.aspects(), registered.column() + 1,
                registered.row(), registered.complexity(), registered.parents(), registered.parentsHidden(), registered.siblings(),
                registered.flags(), registered.warp(), registered.pages(), registered.icon()));
        Result moved = ParityComparator.compare(oracle, new PortSnapshot(research, port.researchPool(), port.recipes()), PORT, PortFixtures.KAMI);
        assertHas(moved, "research", "ASPECT_ANALYZER", "presence");
        assertHas(moved, "research", "ASPECT_ANALYZER", "column");
        assertEquals(2, moved.failures.size(), moved.describe());
    }

    @Test
    void aRecipeTheOriginalRegistersOnlyUnderAMissingModIsHeldAgainstItsDefinition() {
        OracleData oracle = OracleData.fromClasspath();
        PortSnapshot port = PortFixtures.fromOracle(oracle);
        Recipe analyzer = oracle.recipes().stream().filter(r -> "ASPECT_ANALYZER".equals(r.name())).findFirst().orElseThrow();
        List<PortRecipe> recipes = new ArrayList<>(port.recipes());
        recipes.add(PortFixtures.portRecipe(analyzer));
        Result present = ParityComparator.compare(oracle, new PortSnapshot(port.research(), port.researchPool(), recipes), PORT, PortFixtures.KAMI);
        assertSingleDifference(present, "recipe", "ASPECT_ANALYZER", "presence");

        PortRecipe base = PortFixtures.portRecipe(analyzer);
        Map<String, Integer> changed = new LinkedHashMap<>(base.aspects());
        String first = changed.keySet().iterator().next();
        changed.put(first, changed.get(first) + 1);
        recipes.set(recipes.size() - 1, new PortRecipe(base.id(), base.name(), base.kind(), base.research(), base.output(), base.count(),
                changed, base.instability(), base.width(), base.height(), base.cells(), base.components()));
        Result different = ParityComparator.compare(oracle, new PortSnapshot(port.research(), port.researchPool(), recipes), PORT, PortFixtures.KAMI);
        assertHas(different, "recipe", "ASPECT_ANALYZER", "aspects");
        assertEquals(2, different.failures.size(), different.describe());
    }

    @Test
    void aSwitchedOffFlagChangesWhatIsExpected() {
        OracleData oracle = OracleData.fromClasspath();
        ParityComparator.Environment noKami = c -> c.kind().equals("config") && !c.expr().equals("ConfigHandler.enableKami");
        Result result = ParityComparator.compare(oracle, PortFixtures.fromOracle(oracle), noKami, List.of());
        assertTrue(result.failures.stream().anyMatch(d -> d.subject().equals("ICHOR") && d.field().equals("presence")
                && d.original().equals("registered only when ConfigHandler.enableKami")), result.describe());
    }

    @Test
    void theHiddenParentsOfAKamiEntryAreEveryOtherResearchOfTheSixTabs() {
        OracleData oracle = OracleData.fromClasspath();
        PortSnapshot port = PortFixtures.fromOracle(oracle);
        PortResearch ichor = port.research().get("ICHOR");
        assertTrue(ichor.parentsHidden().contains("INFUSION") && ichor.parentsHidden().contains("ENCHANTER"));
        assertFalse(ichor.parentsHidden().contains("MINILITHX"), "blacklisted");
        assertFalse(ichor.parentsHidden().contains("SOMEKAMI"), "ends with KAMI");
        assertFalse(ichor.parentsHidden().contains("LOSTONE") || ichor.parentsHidden().contains("VIRTUALONE"));
        assertFalse(ichor.parentsHidden().contains("ELDRITCH_MINOR"), "not one of the six tabs");
        assertFalse(ichor.parentsHidden().contains("ICHOR_CLOTH"), "a Kami entry never requires another Kami entry");

        for (String drop : List.of("ENCHANTER", "INFUSION")) {
            Map<String, PortResearch> research = new LinkedHashMap<>(port.research());
            List<String> hidden = new ArrayList<>(ichor.parentsHidden());
            hidden.remove(drop);
            research.put("ICHOR", new PortResearch("ICHOR", ichor.category(), ichor.aspects(), ichor.column(), ichor.row(),
                    ichor.complexity(), ichor.parents(), hidden, ichor.siblings(), ichor.flags(), ichor.warp(), ichor.pages(), ichor.icon()));
            Result result = ParityComparator.compare(oracle, new PortSnapshot(research, port.researchPool(), port.recipes()), PORT, PortFixtures.KAMI);
            assertSingleDifference(result, "research", "ICHOR", "parentsHidden");
            assertTrue(result.failures.get(0).port().contains("missing [" + drop + "]"), result.failures.get(0).toString());
        }

        Map<String, PortResearch> research = new LinkedHashMap<>(port.research());
        List<String> hidden = new ArrayList<>(ichor.parentsHidden());
        hidden.add("SOMEKAMI");
        research.put("ICHOR", new PortResearch("ICHOR", ichor.category(), ichor.aspects(), ichor.column(), ichor.row(),
                ichor.complexity(), ichor.parents(), hidden, ichor.siblings(), ichor.flags(), ichor.warp(), ichor.pages(), ichor.icon()));
        Result result = ParityComparator.compare(oracle, new PortSnapshot(research, port.researchPool(), port.recipes()), PORT, PortFixtures.KAMI);
        assertSingleDifference(result, "research", "ICHOR", "parentsHidden");
        assertTrue(result.failures.get(0).port().contains("unexpected [SOMEKAMI]"));
    }

    // ---------------------------------------------------------------------------------------- recipe mutations

    @Test
    void aChangedRecipeFails() {
        Result aspects = compareRecipesMutated(c -> {
            JsonObject a = recipeByName(c, "MAGNET", "arcane_shaped").getAsJsonObject("aspects");
            a.addProperty("aer", 21);
        });
        assertSingleDifference(aspects, "recipe", "MAGNET", "aspects");

        assertSingleDifference(compareRecipesMutated(c -> recipeByName(c, "ENCHANTER", "infusion").addProperty("instability", 14)),
                "recipe", "ENCHANTER", "instability");
        Result count = compareRecipesMutated(c -> recipeByName(c, "MAGNET", "arcane_shaped").getAsJsonObject("output").addProperty("count", 2));
        assertHas(count, "recipe", "MAGNET", "count");
        assertHas(count, "research", "MAGNETS", "pages[2].recipes");      // the page of the research shows that recipe
        assertSingleDifference(compareRecipesMutated(c -> recipeByName(c, "MAGNET", "arcane_shaped").addProperty("research", "OTHER")),
                "recipe", "MAGNET", "research");
    }

    @Test
    void aChangedIngredientCountOrShapeFails() {
        Result count = compareRecipesMutated(c -> recipeByName(c, "ENCHANTER", "infusion").getAsJsonArray("components").remove(0));
        assertTrue(count.failures.stream().anyMatch(d -> d.subject().equals("ENCHANTER") && d.field().equals("ingredients.count")), count.describe());

        Result shape = compareRecipesMutated(c -> {
            JsonObject r = recipeByName(c, "MAGNET", "arcane_shaped");
            JsonArray wider = new JsonArray();
            for (JsonElement row : r.getAsJsonArray("pattern")) wider.add(row.getAsString() + " ");
            r.add("pattern", wider);
            r.addProperty("width", r.get("width").getAsInt() + 1);
        });
        assertTrue(shape.failures.stream().anyMatch(d -> d.subject().equals("MAGNET") && d.field().equals("shape")), shape.describe());
        assertTrue(shape.failures.stream().noneMatch(d -> d.subject().equals("MAGNET") && d.field().equals("ingredients.count")),
                "the number of ingredients is unchanged by a wider box");

        Result ingredient = compareRecipesMutated(c -> {
            JsonObject key = recipeByName(c, "MAGNET", "arcane_shaped").getAsJsonObject("key");
            key.getAsJsonObject("I").addProperty("resolved", "minecraft:gold_ingot");
        });
        assertTrue(ingredient.failures.stream().anyMatch(d -> d.subject().equals("MAGNET") && d.field().startsWith("ingredient[")),
                ingredient.describe());
        Result catalyst = compareRecipesMutated(c -> recipeByName(c, "GASEOUS_LIGHT", "crucible").getAsJsonObject("catalyst").addProperty("resolved", "minecraft:stick"));
        assertSingleDifference(catalyst, "recipe", "GASEOUS_LIGHT", "catalyst[0]");
    }

    @Test
    void aMissingRecipeAndAnUnexpectedRecipeFail() {
        OracleData oracle = OracleData.fromClasspath();
        PortSnapshot port = PortFixtures.fromOracle(oracle);
        List<PortRecipe> recipes = new ArrayList<>(port.recipes());
        PortRecipe magnet = recipes.stream().filter(r -> "MAGNET".equals(r.name())).findFirst().orElseThrow();
        recipes.remove(magnet);
        Result missing = ParityComparator.compare(oracle, new PortSnapshot(port.research(), port.researchPool(), recipes), PORT, PortFixtures.KAMI);
        assertSingleDifference(missing, "recipe", "MAGNET", "presence");

        recipes.add(magnet);
        recipes.add(new PortRecipe("tt:extra", "EXTRA", "crucible", "EXTRA", "thaumictinkerer:extra", 1, Map.of(), null, 0, 0, List.of(List.of("minecraft:stick")), List.of()));
        Result extra = ParityComparator.compare(oracle, new PortSnapshot(port.research(), port.researchPool(), recipes), PORT, PortFixtures.KAMI);
        assertSingleDifference(extra, "recipe", "EXTRA", "presence");
    }

    @Test
    void theDuplicateNamedRecipesAreEachMatched() {
        // SUMMON and SUMMON1 are registered 17 times each under one name; every one needs its own port recipe
        OracleData oracle = OracleData.fromClasspath();
        PortSnapshot port = PortFixtures.fromOracle(oracle);
        List<PortRecipe> recipes = new ArrayList<>(port.recipes());
        PortRecipe summon = recipes.stream().filter(r -> "SUMMON".equals(r.name())).findFirst().orElseThrow();
        recipes.remove(summon);
        Result result = ParityComparator.compare(oracle, new PortSnapshot(port.research(), port.researchPool(), recipes), PORT, PortFixtures.KAMI);
        assertEquals(1, result.failures.size(), result.describe());
        assertTrue(result.failures.get(0).subject().startsWith("SUMMON -> thaumictinkerer:mob_aspect_infused_"), result.failures.get(0).toString());
    }

    // ---------------------------------------------------------------------------------------- every entry is checked

    @Test
    void everyResearchEntryFailsWhenItsPositionItsFirstAspectOrItsParentsChange() {
        OracleData real = OracleData.fromClasspath();
        PortSnapshot port = PortFixtures.fromOracle(real);
        int checked = 0;
        for (OracleData.Research r : real.research()) {
            if (!r.registered() || !ParityComparator.holdsForTest(r.when(), PORT)) continue;
            int index = (int) real.research().stream().filter(o -> o.key().equals(r.key())).takeWhile(o -> o != r).count();
            Result column = compareResearchMutated(doc -> {
                JsonObject e = entry(doc, r.key(), index);
                e.addProperty("column", e.get("column").getAsInt() + 1);
            });
            assertTrue(column.failures.stream().anyMatch(d -> d.subject().equals(r.key()) && d.field().equals("column")), "column of " + r.key());
            if (!r.aspects().isEmpty()) {
                Result amount = compareResearchMutated(doc -> {
                    JsonObject a = entry(doc, r.key(), index).getAsJsonObject("aspects");
                    String first = a.keySet().iterator().next();
                    a.addProperty(first, a.get(first).getAsInt() + 1);
                });
                assertTrue(amount.failures.stream().anyMatch(d -> d.subject().equals(r.key()) && d.field().equals("aspects")), "aspects of " + r.key());
            }
            Result parents = compareResearchMutated(doc -> {
                JsonArray p = entry(doc, r.key(), index).getAsJsonArray("parents");
                p.add("A_NEW_PARENT");
            });
            assertTrue(parents.failures.stream().anyMatch(d -> d.subject().equals(r.key()) && d.field().equals("parents")), "parents of " + r.key());
            checked++;
        }
        assertEquals(74, checked, "the entries that apply to the port");
        assertNotNull(port);
    }

    @Test
    void everyRecipeFailsWhenItsOutputCountOrItsAspectChanges() {
        OracleData real = OracleData.fromClasspath();
        JsonArray entries = OracleData.readResource(OracleData.RECIPES_RESOURCE).getAsJsonArray("entries");
        int checked = 0;
        for (int i = 0; i < entries.size(); i++) {
            JsonObject original = entries.get(i).getAsJsonObject();
            if (original.get("output").isJsonNull()) continue;                  // a custom recipe class has no fixed output
            if (!original.get("registered").getAsBoolean() || !original.getAsJsonArray("when").isEmpty() && !original.getAsJsonArray("when").toString().contains("\"kind\":\"config\"")) continue;
            if (original.getAsJsonArray("when").asList().stream().anyMatch(w -> w.getAsJsonObject().get("kind").getAsString().equals("mod")
                    && w.getAsJsonObject().get("value").getAsBoolean())) continue;
            int at = i;
            Result count = compareRecipesMutated(doc -> {
                JsonObject o = doc.getAsJsonArray("entries").get(at).getAsJsonObject().getAsJsonObject("output");
                o.addProperty("count", o.get("count").getAsInt() + 1);
            });
            assertTrue(count.failures.stream().anyMatch(d -> d.kind().equals("recipe") && d.field().equals("count")), "count of recipe #" + i + " " + original.get("name"));
            if (original.has("aspects") && !original.getAsJsonObject("aspects").keySet().isEmpty()) {
                Result aspects = compareRecipesMutated(doc -> {
                    JsonObject a = doc.getAsJsonArray("entries").get(at).getAsJsonObject().getAsJsonObject("aspects");
                    String first = a.keySet().iterator().next();
                    a.addProperty(first, a.get(first).getAsInt() + 1);
                });
                assertTrue(aspects.failures.stream().anyMatch(d -> d.kind().equals("recipe") && d.field().equals("aspects")), "aspects of recipe #" + i);
            }
            checked++;
        }
        assertEquals(126, checked, "the recipes with an output that apply to the port (127 expected, one of them the custom recipe class)");
        assertEquals(130, real.recipes().size());
    }


    // ---------------------------------------------------------------------------------------- the shape: where the gaps are

    /** The same recipe with the gaps moved: the pattern's characters keep their order and the box its size. */
    private static JsonArray movedGaps(JsonArray pattern) {
        List<String> rows = new ArrayList<>();
        pattern.forEach(e -> rows.add(e.getAsString()));
        String flat = String.join("", rows);
        int width = rows.get(0).length();
        List<Character> letters = new ArrayList<>();
        for (char c : flat.toCharArray()) if (c != ' ') letters.add(c);
        for (int shift = 1; shift < flat.length(); shift++) {
            StringBuilder moved = new StringBuilder();
            int next = 0;
            for (int i = 0; i < flat.length(); i++) {
                boolean gap = flat.charAt((i + flat.length() - shift) % flat.length()) == ' ';
                moved.append(gap ? ' ' : letters.get(next++));
            }
            JsonArray out = new JsonArray();
            List<String> movedRows = new ArrayList<>();
            for (int r = 0; r < rows.size(); r++) movedRows.add(moved.substring(r * width, (r + 1) * width));
            List<String> mirrored = new ArrayList<>();
            for (String row : rows) mirrored.add(new StringBuilder(row).reverse().toString());
            if (movedRows.equals(rows) || movedRows.equals(mirrored)) continue;
            movedRows.forEach(out::add);
            return out;
        }
        throw new AssertionError("no other layout for " + rows);
    }

    @Test
    void aGapThatMovesFailsEvenWhenTheOrderOfTheIngredientsAndTheBoxStay() {
        // ICHORCLOTH_HELM "CCC" / "C C" -> "CCC" / "CC ": the same three letters in a row and the same 3x2 box
        Result helm = compareRecipesMutated(c -> recipeByName(c, "ICHORCLOTH_HELM", "arcane_shaped").add("pattern", rows("CCC", "CC ")));
        assertTrue(helm.failures.stream().anyMatch(d -> d.subject().equals("ICHORCLOTH_HELM") && d.field().startsWith("ingredient[")), helm.describe());
        assertTrue(helm.failures.stream().noneMatch(d -> d.field().equals("shape") || d.field().equals("ingredients.count")),
                "the box and the number of ingredients are the same: only the positions differ\n" + helm.describe());

        // the X of the dark quartz recipe becomes a plus
        JsonObject quartz = new JsonObject();
        Result plus = compareRecipesMutated(c -> {
            JsonObject r = recipeByName(c, "DARK_QUARTZ0", "crafting_shaped");
            r.add("pattern", rows(" Q ", "QCQ", " Q "));
        });
        assertTrue(plus.failures.stream().anyMatch(d -> d.subject().startsWith("DARK_QUARTZ0") && d.field().startsWith("ingredient[")), plus.describe());
        assertNotNull(quartz);
    }

    private static JsonArray rows(String... rows) {
        JsonArray array = new JsonArray();
        for (String row : rows) array.add(row);
        return array;
    }

    @Test
    void movingTheGapsOfEveryShapedRecipeThatHasThemFails() {
        JsonArray entries = OracleData.readResource(OracleData.RECIPES_RESOURCE).getAsJsonArray("entries");
        int checked = 0;
        for (int i = 0; i < entries.size(); i++) {
            JsonObject original = entries.get(i).getAsJsonObject();
            if (!original.has("pattern") || !original.get("registered").getAsBoolean()) continue;
            String flat = String.join("", original.getAsJsonArray("pattern").asList().stream().map(JsonElement::getAsString).toList());
            if (!flat.contains(" ") || original.getAsJsonArray("when").asList().stream()
                    .anyMatch(w -> w.getAsJsonObject().get("kind").getAsString().equals("mod"))) continue;
            int at = i;
            String name = original.get("name").isJsonNull() ? "<unnamed>" : original.get("name").getAsString();
            Result moved = compareRecipesMutated(c -> {
                JsonObject o = c.getAsJsonArray("entries").get(at).getAsJsonObject();
                o.add("pattern", movedGaps(o.getAsJsonArray("pattern")));
            });
            assertTrue(moved.failures.stream().anyMatch(d -> d.kind().equals("recipe") && d.field().startsWith("ingredient[")),
                    "moving the gaps of " + name + " (" + original.getAsJsonArray("pattern") + ") was not noticed:\n" + moved.describe());
            checked++;
        }
        assertEquals(22, checked, "the shaped recipes of the oracle that have gaps (all apply to the port)");
    }

    @Test
    void aMirrorImageOfThePatternIsTheSameRecipe() {
        // 1.7.10 and 1.21.1 both match a shaped recipe mirrored, so the left staircase is the right one
        Result mirrored = compareRecipesMutated(c -> recipeByName(c, "DARK_QUARTZ5", "crafting_shaped").add("pattern", rows("Q  ", "QQ ", "QQQ")));
        assertTrue(mirrored.passed(), mirrored.describe());
        assertEquals(1, mirrored.counts.get("shaped patterns that are a mirror image of the original"), mirrored.counts.toString());
    }

    // ---------------------------------------------------------------------------------------- what makes two outputs of one item differ

    @Test
    void theSixInfusedSeedRecipesMakeSixDifferentSeedsAndAPortThatMakesOneKindFails() {
        OracleData oracle = OracleData.fromClasspath();
        List<Recipe> seeds = oracle.recipes().stream().filter(r -> r.name() != null && r.name().matches("INFUSED_POTIONS\\d") && r.kind().equals("infusion")).toList();
        assertEquals(6, seeds.size());
        assertEquals(6, seeds.stream().map(r -> r.output().aspect()).distinct().count(), "the oracle tells the six apart");
        PortSnapshot port = PortFixtures.fromOracle(oracle);
        for (Recipe seed : seeds) {
            PortRecipe made = port.recipes().stream().filter(p -> seed.name().equals(p.name())).findFirst().orElseThrow();
            // a port whose recipe makes the seed of another aspect: the aspect of the result is compared
            PortRecipe swapped = new PortRecipe(made.id(), made.name(), made.kind(), made.research(), made.output(), made.count(), made.aspects(),
                    made.instability(), made.width(), made.height(), made.cells(), made.components(), "aer".equals(made.outputAspect()) ? "ignis" : "aer");
            Result result = ParityComparator.compare(oracle, PortFixtures.withRecipe(port, made, swapped), PORT, PortFixtures.KAMI);
            assertTrue(result.failures.stream().anyMatch(d -> d.subject().equals(seed.name()) && d.field().equals("output.aspect")), seed.name() + "\n" + result.describe());
        }
        // all six recipes making the plain seed (no aspect at all)
        List<PortRecipe> recipes = new ArrayList<>(port.recipes());
        for (Recipe seed : seeds) {
            PortRecipe made = recipes.stream().filter(p -> seed.name().equals(p.name())).findFirst().orElseThrow();
            recipes.set(recipes.indexOf(made), new PortRecipe(made.id(), made.name(), made.kind(), made.research(), made.output(), made.count(),
                    made.aspects(), made.instability(), made.width(), made.height(), made.cells(), made.components(), null));
        }
        Result none = ParityComparator.compare(oracle, new PortSnapshot(port.research(), port.researchPool(), recipes), PORT, PortFixtures.KAMI);
        assertEquals(6, none.failures.stream().filter(d -> d.field().equals("output.aspect")).count(), none.describe());
    }

    @Test
    void theAspectOfAResearchIconAndOfAShownRecipeIsCompared() {
        OracleData oracle = OracleData.fromClasspath();
        PortSnapshot port = PortFixtures.fromOracle(oracle);
        PortResearch potions = port.research().get("INFUSED_POTIONS");
        assertEquals("ignis", potions.iconAspect());
        Map<String, PortResearch> research = new LinkedHashMap<>(port.research());
        research.put("INFUSED_POTIONS", new PortResearch(potions.key(), potions.category(), potions.aspects(), potions.column(), potions.row(),
                potions.complexity(), potions.parents(), potions.parentsHidden(), potions.siblings(), potions.flags(), potions.warp(), potions.pages(),
                potions.icon(), "aer"));
        Result result = ParityComparator.compare(oracle, new PortSnapshot(research, port.researchPool(), port.recipes()), PORT, PortFixtures.KAMI);
        assertSingleDifference(result, "research", "INFUSED_POTIONS", "icon.aspect");
        assertTrue(potions.pages().stream().anyMatch(page -> page.recipes().stream().anyMatch(r -> r.contains("{aspect=aer}"))),
                "the infusion page of the potions shows the six seeds, told apart: " + potions.pages());
    }

    // ---------------------------------------------------------------------------------------- what an ingredient stands for

    /** The port recipe that equals the first registered recipe of the original under this name (the fixture's crafting recipes have no name). */
    private static PortRecipe recipeNamed(PortSnapshot port, String name) {
        Recipe r = OracleData.fromClasspath().recipes().stream().filter(x -> name.equals(x.name()) && x.registered()).findFirst().orElseThrow();
        return port.recipes().stream().filter(p -> p.id().equals("tt:" + r.order())).findFirst().orElseThrow();
    }

    @Test
    void aBareBlockIngredientIsEveryMetadataSoANarrowedSlotFails() {
        OracleData oracle = OracleData.fromClasspath();
        PortSnapshot port = PortFixtures.fromOracle(oracle);
        PortRecipe slab = recipeNamed(port, "DARK_QUARTZ2");
        assertEquals(List.of("thaumictinkerer:dark_quartz", "thaumictinkerer:dark_quartz_chiseled", "thaumictinkerer:dark_quartz_pillar"), slab.cells().get(0));

        // the family of the quartz block narrowed to the plain block: the chiseled and the pillar block are no longer accepted
        Result narrowed = ParityComparator.compare(oracle, PortFixtures.withRecipe(port, slab, PortFixtures.withSlot(slab, false, 0,
                List.of("thaumictinkerer:dark_quartz"))), PORT, PortFixtures.KAMI);
        assertTrue(narrowed.failures.stream().anyMatch(d -> d.subject().equals("DARK_QUARTZ2") && d.field().equals("ingredient[0]")
                && d.port().contains("lacks [thaumictinkerer:dark_quartz_chiseled, thaumictinkerer:dark_quartz_pillar]")), narrowed.describe());

        // the Remote Placer: any stone device, and any dispenser
        PortRecipe placer = port.recipes().stream().filter(p -> "REMOTE_PLACER".equals(p.name())).findFirst().orElse(null);
        assertNull(placer, "the original never registered it");
        Recipe dead = oracle.recipes().stream().filter(r -> "REMOTE_PLACER".equals(r.name())).findFirst().orElseThrow();
        assertTrue(dead.cells().get(0).wildcard() && dead.cells().get(0).variants().size() == 11, "s is any of the 11 stone devices with an item");
        PortRecipe narrow = PortFixtures.withSlot(PortFixtures.portRecipe(dead), false, 0, List.of("thaumcraft:alchemy_furnace"));
        List<PortRecipe> recipes = new ArrayList<>(port.recipes());
        recipes.add(narrow);
        Result placerResult = ParityComparator.compare(oracle, new PortSnapshot(port.research(), port.researchPool(), recipes), PORT, PortFixtures.KAMI);
        assertTrue(placerResult.failures.stream().anyMatch(d -> d.subject().startsWith("REMOTE_PLACER") && d.field().equals("ingredient[0]")
                && d.port().contains("lacks")), placerResult.describe());
    }

    @Test
    void aSlotThatAcceptsMoreThanTheOriginalFailsUnlessItIsAMergeOrAnOpenTag() {
        OracleData oracle = OracleData.fromClasspath();
        PortSnapshot port = PortFixtures.fromOracle(oracle);
        // a bare Item is damage 0 exactly: the middle of the quartz recipe is coal and not charcoal; accepting both is a broader recipe
        PortRecipe quartz = port.recipes().stream().filter(p -> "thaumictinkerer:dark_quartz_item".equals(p.output())).findFirst().orElseThrow();
        Result broader = ParityComparator.compare(oracle, PortFixtures.withRecipe(port, quartz, PortFixtures.withSlot(quartz, false, 4,
                List.of("minecraft:coal", "minecraft:charcoal"))), PORT, PortFixtures.KAMI);
        assertTrue(broader.failures.stream().anyMatch(d -> d.field().equals("ingredient[4] (broader)") && d.port().contains("minecraft:charcoal")),
                broader.describe());

        // a plain ingredient widened to a tag
        PortRecipe magnet = recipeNamed(port, "MAGNET");
        int filled = magnet.cells().indexOf(magnet.cells().stream().filter(c -> !c.isEmpty()).findFirst().orElseThrow());
        Result widened = ParityComparator.compare(oracle, PortFixtures.withRecipe(port, magnet, PortFixtures.withSlot(magnet, false, filled,
                List.of(magnet.cells().get(filled).get(0), "minecraft:stone"))), PORT, PortFixtures.KAMI);
        assertTrue(widened.failures.stream().anyMatch(d -> d.subject().equals("MAGNET") && d.field().endsWith("(broader)")), widened.describe());

        // a wildcard that accepts more than the variants of the original
        PortRecipe slab = recipeNamed(port, "DARK_QUARTZ2");
        Result more = ParityComparator.compare(oracle, PortFixtures.withRecipe(port, slab, PortFixtures.withSlot(slab, false, 0,
                List.of("thaumictinkerer:dark_quartz", "thaumictinkerer:dark_quartz_chiseled", "thaumictinkerer:dark_quartz_pillar", "minecraft:stone"))),
                PORT, PortFixtures.KAMI);
        assertTrue(more.failures.stream().anyMatch(d -> d.subject().equals("DARK_QUARTZ2") && d.field().endsWith("(broader)")), more.describe());
    }

    @Test
    void anOreNameStandsForTheItemsOfTheNameAndTheFallbackStackIsNotEnough() {
        OracleData oracle = OracleData.fromClasspath();
        PortSnapshot port = PortFixtures.fromOracle(oracle);
        PortRecipe mob = recipeNamed(port, "MOB_MAGNET");
        assertEquals(List.of("minecraft:copper_ingot"), mob.cells().get(4), "G (the middle of ' G ' / 'SGs' / 'WFW') is the copper ingot");
        // the port keeps the fallback of 1.7.10, the gold ingot, instead of the copper the ore name stands for
        Result gold = ParityComparator.compare(oracle, PortFixtures.withRecipe(port, mob, PortFixtures.withSlot(PortFixtures.withSlot(mob, false, 1,
                List.of("minecraft:gold_ingot")), false, 4, List.of("minecraft:gold_ingot"))), PORT, PortFixtures.KAMI);
        assertEquals(2, gold.failures.stream().filter(d -> d.subject().equals("MOB_MAGNET") && d.port().contains("lacks [minecraft:copper_ingot]")).count(),
                gold.describe());
        // a tag with more members (what other mods add) is fine
        Result open = ParityComparator.compare(oracle, PortFixtures.withRecipe(port, mob, PortFixtures.withSlot(PortFixtures.withSlot(mob, false, 1,
                List.of("minecraft:copper_ingot", "othermod:copper_ingot")), false, 4, List.of("othermod:copper_ingot", "minecraft:copper_ingot"))),
                PORT, PortFixtures.KAMI);
        assertTrue(open.passed(), open.describe());
        assertEquals(2, open.counts.get("ingredients that are ore names (the tag of the port may hold more)"));
    }

    @Test
    void aSlotThatShouldBeEmptyOrFilledFails() {
        OracleData oracle = OracleData.fromClasspath();
        PortSnapshot port = PortFixtures.fromOracle(oracle);
        PortRecipe helm = recipeNamed(port, "ICHORCLOTH_HELM");
        Result filled = ParityComparator.compare(oracle, PortFixtures.withRecipe(port, helm, PortFixtures.withSlot(helm, false, 4, List.of("minecraft:stone"))),
                PORT, PortFixtures.KAMI);
        assertTrue(filled.failures.stream().anyMatch(d -> d.subject().equals("ICHORCLOTH_HELM") && d.original().equals("empty")), filled.describe());
        Result emptied = ParityComparator.compare(oracle, PortFixtures.withRecipe(port, helm, PortFixtures.withSlot(helm, false, 0, List.of())),
                PORT, PortFixtures.KAMI);
        assertTrue(emptied.failures.stream().anyMatch(d -> d.subject().equals("ICHORCLOTH_HELM") && d.port().equals("empty")), emptied.describe());
    }

    // ---------------------------------------------------------------------------------------- the documented state

    @Test
    void thePortInTheStateTheDeviationTextsDescribePassesWithTheRealAllowListAndUsesEveryEntry() {
        OracleData oracle = OracleData.fromClasspath();
        List<KnownDifference> known = KnownDifference.fromClasspath();
        Result result = ParityComparator.compare(oracle, PortFixtures.documentedPort(oracle), PORT, known);
        assertTrue(result.passed(), result.describe());
        assertEquals(known.stream().mapToInt(KnownDifference::occurrences).sum(), result.allowed.size(), "every entry is consumed exactly");
        assertEquals(11, result.allowed.size());
        assertEquals(1, result.counts.get("shaped patterns that are a mirror image of the original"), "the left staircase");
        assertEquals(18, result.counts.get("slots of merged recipes checked against the union"), "two 3x3 recipes");
        assertTrue(result.counts.get("ingredient slots broader than the original (merged recipes)") >= 1, result.counts.toString());
    }

    private static Result compareDocumented(java.util.function.UnaryOperator<PortSnapshot> change) {
        OracleData oracle = OracleData.fromClasspath();
        return ParityComparator.compare(oracle, change.apply(PortFixtures.documentedPort(oracle)), PORT, KnownDifference.fromClasspath());
    }

    private static PortRecipe byId(PortSnapshot port, String id) {
        return port.recipes().stream().filter(p -> id.equals(p.id())).findFirst().orElseThrow();
    }

    @Test
    void theMergedQuartzRecipeHasToAcceptCoalAndCharcoalAndNothingElse() {
        // deleting the charcoal from the merged recipe: the allow-list entry only says the two recipes are one, it does not hide this
        Result noCharcoal = compareDocumented(port -> PortFixtures.withRecipe(port, byId(port, "thaumictinkerer:dark_quartz_0"),
                PortFixtures.withSlot(byId(port, "thaumictinkerer:dark_quartz_0"), false, 4, List.of("minecraft:coal"))));
        assertTrue(noCharcoal.failures.stream().anyMatch(d -> d.subject().startsWith("DARK_QUARTZ0") && d.field().equals("ingredient[4]")
                && d.original().equals("minecraft:charcoal")), noCharcoal.describe());
        assertTrue(noCharcoal.failures.stream().anyMatch(d -> d.field().equals("ingredient[4] (merged)")), noCharcoal.describe());

        Result noCoal = compareDocumented(port -> PortFixtures.withRecipe(port, byId(port, "thaumictinkerer:dark_quartz_0"),
                PortFixtures.withSlot(byId(port, "thaumictinkerer:dark_quartz_0"), false, 4, List.of("minecraft:charcoal"))));
        assertTrue(noCoal.failures.stream().anyMatch(d -> d.field().equals("ingredient[4]") && d.original().equals("minecraft:coal")), noCoal.describe());

        Result more = compareDocumented(port -> PortFixtures.withRecipe(port, byId(port, "thaumictinkerer:dark_quartz_0"),
                PortFixtures.withSlot(byId(port, "thaumictinkerer:dark_quartz_0"), false, 4, List.of("minecraft:coal", "minecraft:charcoal", "minecraft:stick"))));
        assertTrue(more.failures.stream().anyMatch(d -> d.field().equals("ingredient[4] (merged)") && d.port().contains("minecraft:stick")), more.describe());

        // and a slot of the other eight cells that grew
        Result outer = compareDocumented(port -> PortFixtures.withRecipe(port, byId(port, "thaumictinkerer:dark_quartz_0"),
                PortFixtures.withSlot(byId(port, "thaumictinkerer:dark_quartz_0"), false, 0, List.of("minecraft:quartz", "minecraft:stick"))));
        assertTrue(outer.failures.stream().anyMatch(d -> d.field().equals("ingredient[0] (merged)")), outer.describe());
    }

    @Test
    void theMergedStairsRecipeStillHasToBeAStaircaseOfTheDarkQuartzFamily() {
        Result narrowed = compareDocumented(port -> PortFixtures.withRecipe(port, byId(port, "thaumictinkerer:dark_quartz_5"),
                PortFixtures.withSlot(byId(port, "thaumictinkerer:dark_quartz_5"), false, 8, List.of("thaumictinkerer:dark_quartz"))));
        assertFalse(narrowed.passed(), narrowed.describe());
        assertTrue(narrowed.failures.stream().anyMatch(d -> d.subject().contains("dark_quartz_stairs") || d.subject().equals("DARK_QUARTZ5")), narrowed.describe());

        // a different shape: the gaps of the staircase moved
        Result gaps = compareDocumented(port -> {
            PortRecipe stairs = byId(port, "thaumictinkerer:dark_quartz_5");
            List<List<String>> cells = new ArrayList<>(stairs.cells());
            List<String> corner = cells.get(2);
            cells.set(2, List.of());
            cells.set(3, corner);
            PortRecipe changed = new PortRecipe(stairs.id(), stairs.name(), stairs.kind(), stairs.research(), stairs.output(), stairs.count(),
                    stairs.aspects(), stairs.instability(), stairs.width(), stairs.height(), cells, stairs.components(), stairs.outputAspect());
            return PortFixtures.withRecipe(port, stairs, changed);
        });
        assertFalse(gaps.passed(), gaps.describe());
    }

    @Test
    void aMergedRecipeThatIsMissingEntirelyIsNotHiddenByTheAllowList() {
        Result result = compareDocumented(port -> {
            List<PortRecipe> recipes = new ArrayList<>(port.recipes());
            recipes.remove(byId(port, "thaumictinkerer:dark_quartz_0"));
            return new PortSnapshot(port.research(), port.researchPool(), recipes);
        });
        assertTrue(result.failures.stream().anyMatch(d -> d.subject().startsWith("DARK_QUARTZ0") && d.port().equals("missing")), result.describe());
        assertFalse(result.passed());
    }

    // ---------------------------------------------------------------------------------------- the allow-list is consumed

    @Test
    void anEntryAllowsAsManyIdenticalDifferencesAsItsOccurrencesAndNoMore() {
        OracleData oracle = OracleData.fromClasspath();
        // a third dark quartz recipe in the oracle: the port has merged it too, so there are two identical "merged" differences
        JsonObject recipes = copy(OracleData.RECIPES_RESOURCE);
        JsonObject second = recipeByName(recipes, "DARK_QUARTZ0", "crafting_shaped");
        JsonObject third = second.deepCopy();
        third.addProperty("order", 500);
        recipes.getAsJsonArray("entries").add(third);
        OracleData three = OracleData.parse(copy(OracleData.RESEARCH_RESOURCE), recipes);
        PortSnapshot documented = PortFixtures.documentedPort(oracle);

        List<KnownDifference> real = new ArrayList<>(KnownDifference.fromClasspath());
        Result once = ParityComparator.compare(three, documented, PORT, real);
        long merged = once.failures.stream().filter(d -> d.field().equals("presence") && d.port().startsWith("merged into")).count();
        assertEquals(1, merged, "the real entry (one occurrence) allows one of the two identical differences:\n" + once.describe());

        KnownDifference quartz = real.stream().filter(k -> k.key().equals("DARK_QUARTZ0 -> thaumictinkerer:dark_quartz_item")).findFirst().orElseThrow();
        List<KnownDifference> twice = new ArrayList<>(real);
        twice.set(twice.indexOf(quartz), withOccurrences(quartz, 2));
        Result both = ParityComparator.compare(three, documented, PORT, twice);
        assertEquals(0, both.failures.stream().filter(d -> d.port().startsWith("merged into")).count(), both.describe());

        // an entry that allows two but matches one is stale
        Result stale = ParityComparator.compare(oracle, documented, PORT, twice);
        assertEquals(1, stale.unusedKnown.size(), stale.describe());
        assertEquals(1, stale.unusedMatched.get(0));
        assertTrue(stale.describe().contains("matched 1 of 2"), stale.describe());
        assertFalse(stale.passed());
    }

    private static KnownDifference withOccurrences(KnownDifference k, int n) {
        return new KnownDifference(k.kind(), k.key(), k.field(), k.original(), k.port(), k.reason(), k.deviation(), k.quote(), k.mentions(), n);
    }

    @Test
    void aResearchOfTheOriginalThatSitsInAnotherTabIsSaidSo() {
        OracleData oracle = OracleData.fromClasspath();
        PortSnapshot port = PortFixtures.fromOracle(oracle);
        Map<String, PortResearch> research = new LinkedHashMap<>(port.research());
        research.remove("MAGNETS");
        List<PortSnapshot.PoolEntry> pool = new ArrayList<>(port.researchPool());
        pool.removeIf(e -> e.key().equals("MAGNETS"));
        pool.add(new PortSnapshot.PoolEntry("MAGNETS", "ARTIFICE", false, false));
        Result result = ParityComparator.compare(oracle, new PortSnapshot(research, pool, port.recipes()), PORT, PortFixtures.KAMI);
        assertTrue(result.failures.stream().anyMatch(d -> d.subject().equals("MAGNETS") && d.field().equals("presence")
                && d.port().equals("missing (registered in the tab ARTIFICE)")), result.describe());
    }

    @Test
    void theObjectTagsOfTheMobEssencesAreComparedAndNoOtherTagIsRegistered() {
        OracleData oracle = OracleData.fromClasspath();
        assertEquals(17, oracle.objectTags().size());
        PortSnapshot port = PortFixtures.fromOracle(oracle);
        Result clean = ParityComparator.compare(oracle, port, PORT, PortFixtures.KAMI);
        assertTrue(clean.passed(), clean.describe());
        assertEquals(17, clean.counts.get("object tags compared"));

        Map<String, Map<String, Integer>> changed = new LinkedHashMap<>(port.objectTags());
        changed.put("thaumictinkerer:mob_aspect_aer", Map.of("aer", 7));
        Result amount = ParityComparator.compare(oracle, new PortSnapshot(port.research(), port.researchPool(), port.recipes(), changed), PORT, PortFixtures.KAMI);
        assertSingleDifference(amount, "objectTag", "thaumictinkerer:mob_aspect_aer", "aspects");
        assertEquals("aer:8", amount.failures.get(0).original());

        Map<String, Map<String, Integer>> wrongAspect = new LinkedHashMap<>(port.objectTags());
        wrongAspect.put("thaumictinkerer:mob_aspect_aer", Map.of("ignis", 8));
        assertSingleDifference(ParityComparator.compare(oracle, new PortSnapshot(port.research(), port.researchPool(), port.recipes(), wrongAspect),
                PORT, PortFixtures.KAMI), "objectTag", "thaumictinkerer:mob_aspect_aer", "aspects");

        Map<String, Map<String, Integer>> missing = new LinkedHashMap<>(port.objectTags());
        missing.remove("thaumictinkerer:mob_aspect_terra");
        Result gone = ParityComparator.compare(oracle, new PortSnapshot(port.research(), port.researchPool(), port.recipes(), missing), PORT, PortFixtures.KAMI);
        assertSingleDifference(gone, "objectTag", "thaumictinkerer:mob_aspect_terra", "presence");

        Map<String, Map<String, Integer>> extra = new LinkedHashMap<>(port.objectTags());
        extra.put("thaumictinkerer:ichor", Map.of("praecantatio", 4));
        Result added = ParityComparator.compare(oracle, new PortSnapshot(port.research(), port.researchPool(), port.recipes(), extra), PORT, PortFixtures.KAMI);
        assertSingleDifference(added, "objectTag", "thaumictinkerer:ichor", "presence");
        assertEquals("not in the original", added.failures.get(0).original());

        // a snapshot without tags does not compare them (the GameTest always reads them)
        Result unread = ParityComparator.compare(oracle, new PortSnapshot(port.research(), port.researchPool(), port.recipes()), PORT, PortFixtures.KAMI);
        assertTrue(unread.passed());
        assertFalse(unread.counts.containsKey("object tags compared"));
    }

    @Test
    void theKamiRequirementsOfThePortAreTheUnionAndAnythingElseIsADifference() {
        OracleData oracle = OracleData.fromClasspath();
        PortSnapshot port = PortFixtures.fromOracle(oracle);
        PortResearch ichor = port.research().get("ICHOR");
        assertTrue(ichor.parentsHidden().contains("ENCHANTER"), "a block-owned research is required by the item-owned Kami entry (the union)");
        Map<String, PortResearch> research = new LinkedHashMap<>(port.research());
        List<String> hidden = new ArrayList<>(ichor.parentsHidden());
        hidden.remove("ENCHANTER");
        research.put("ICHOR", new PortResearch("ICHOR", ichor.category(), ichor.aspects(), ichor.column(), ichor.row(), ichor.complexity(),
                ichor.parents(), hidden, ichor.siblings(), ichor.flags(), ichor.warp(), ichor.pages(), ichor.icon()));
        Result result = ParityComparator.compare(oracle, new PortSnapshot(research, port.researchPool(), port.recipes()), PORT, PortFixtures.KAMI);
        assertSingleDifference(result, "research", "ICHOR", "parentsHidden");
    }

    // ---------------------------------------------------------------------------------------- the allow-list

    private static KnownDifference known(String kind, String key, String field, String original, String port) {
        JsonObject o = new JsonObject();
        o.addProperty("kind", kind);
        o.addProperty("key", key);
        o.addProperty("field", field);
        o.addProperty("original", original);
        o.addProperty("port", port);
        o.addProperty("reason", "documented in a test");
        JsonArray list = new JsonArray();
        list.add(o);
        JsonObject doc = new JsonObject();
        doc.add("differences", list);
        return KnownDifference.parse(doc).get(0);
    }

    @Test
    void aDocumentedDifferenceIsAllowedOnlyWhenOriginalAndPortValuesBothMatch() {
        OracleData oracle = OracleData.fromClasspath();
        PortSnapshot port = PortFixtures.fromOracle(oracle);
        Map<String, PortResearch> research = new LinkedHashMap<>(port.research());
        PortResearch e = research.get("ENCHANTER");
        research.put("ENCHANTER", new PortResearch("ENCHANTER", e.category(), e.aspects(), 6, e.row(), e.complexity(), e.parents(),
                e.parentsHidden(), e.siblings(), e.flags(), e.warp(), e.pages(), e.icon()));
        PortSnapshot changed = new PortSnapshot(research, port.researchPool(), port.recipes());

        Result unlisted = ParityComparator.compare(oracle, changed, PORT, PortFixtures.KAMI);
        assertSingleDifference(unlisted, "research", "ENCHANTER", "column");

        Result allowed = ParityComparator.compare(oracle, changed, PORT, PortFixtures.withKami(known("research", "ENCHANTER", "column", "5", "6")));
        assertTrue(allowed.passed(), allowed.describe());
        assertEquals(2, allowed.allowed.size(), "the column and the Kami requirements");

        Result otherValue = ParityComparator.compare(oracle, changed, PORT, PortFixtures.withKami(known("research", "ENCHANTER", "column", "5", "7")));
        assertFalse(otherValue.passed());
        assertEquals(1, otherValue.failures.size(), "the real difference is still a failure");
        assertEquals(1, otherValue.unusedKnown.size(), "and the entry that did not match is stale");

        Result otherField = ParityComparator.compare(oracle, changed, PORT, PortFixtures.withKami(known("research", "ENCHANTER", "row", "5", "6")));
        assertEquals(1, otherField.failures.size());
        Result otherKey = ParityComparator.compare(oracle, changed, PORT, PortFixtures.withKami(known("research", "MAGNETS", "column", "5", "6")));
        assertEquals(1, otherKey.failures.size());
        Result otherKind = ParityComparator.compare(oracle, changed, PORT, PortFixtures.withKami(known("recipe", "ENCHANTER", "column", "5", "6")));
        assertEquals(1, otherKind.failures.size());
    }

    @Test
    void anAllowListEntryThatMatchesNothingIsAFailure() {
        OracleData oracle = OracleData.fromClasspath();
        Result result = ParityComparator.compare(oracle, PortFixtures.fromOracle(oracle), PORT,
                PortFixtures.withKami(known("research", "ENCHANTER", "column", "5", "6")));
        assertFalse(result.passed());
        assertTrue(result.failures.isEmpty());
        assertEquals(1, result.unusedKnown.size());
        assertTrue(result.describe().contains("STALE"));
    }

    @Test
    void anAllowListEntryNeedsAReason() {
        JsonObject o = new JsonObject();
        for (String f : List.of("kind", "key", "field", "original", "port")) o.addProperty(f, "x");
        o.addProperty("reason", " ");
        JsonArray list = new JsonArray();
        list.add(o);
        JsonObject doc = new JsonObject();
        doc.add("differences", list);
        assertThrows(IllegalStateException.class, () -> KnownDifference.parse(doc));
        assertEquals(1, JsonParser.parseString("{\"differences\": []}").getAsJsonObject().getAsJsonArray("differences").size() + 1);
    }
}
