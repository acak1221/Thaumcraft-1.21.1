package thaumic.tinkerer.parity;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
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
        return ParityComparator.compare(OracleData.parse(r, c), port, PORT, List.of());
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
        assertEquals(129, oracle.recipes().size());
        assertTrue(oracle.research().stream().anyMatch(r -> r.key().equals("ENCHANTER") && r.column() == 5 && r.row() == 4));
    }

    @Test
    void aPortThatEqualsTheOraclePasses() {
        OracleData oracle = OracleData.fromClasspath();
        Result result = ParityComparator.compare(oracle, PortFixtures.fromOracle(oracle), PORT, List.of());
        assertTrue(result.passed(), result.describe());
        assertTrue(result.counts.get("research compared") >= 70, result.counts.toString());
        assertTrue(result.counts.get("recipes compared") >= 120, result.counts.toString());
        assertTrue(result.counts.get("ingredients compared") > 300, result.counts.toString());
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
        Result missing = ParityComparator.compare(oracle, new PortSnapshot(research, port.researchPool(), port.recipes()), PORT, List.of());
        assertSingleDifference(missing, "research", "ENCHANTER", "presence");
        assertEquals("missing", missing.failures.get(0).port());

        research = new LinkedHashMap<>(port.research());
        research.put("EXTRA_THING", new PortResearch("EXTRA_THING", removed.category(), removed.aspects(), 40, 40, 1, List.of(),
                List.of(), List.of(), removed.flags(), 0, List.of(), null));
        Result extra = ParityComparator.compare(oracle, new PortSnapshot(research, port.researchPool(), port.recipes()), PORT, List.of());
        assertSingleDifference(extra, "research", "EXTRA_THING", "presence");
        assertEquals("not in the original", extra.failures.get(0).original());
    }

    @Test
    void anEntryUnderAModThatDoesNotExistIsNotExpectedButItsPresenceIsReported() {
        OracleData oracle = OracleData.fromClasspath();
        PortSnapshot port = PortFixtures.fromOracle(oracle);
        assertFalse(port.research().containsKey("ASPECT_ANALYZER"), "ComputerCraft is not in the port");
        Result clean = ParityComparator.compare(oracle, port, PORT, List.of());
        assertTrue(clean.passed(), clean.describe());

        Map<String, PortResearch> research = new LinkedHashMap<>(port.research());
        PortResearch any = research.get("ENCHANTER");
        research.put("ASPECT_ANALYZER", new PortResearch("ASPECT_ANALYZER", any.category(), any.aspects(), any.column(), any.row(),
                any.complexity(), any.parents(), any.parentsHidden(), any.siblings(), any.flags(), 0, any.pages(), null));
        Result present = ParityComparator.compare(oracle, new PortSnapshot(research, port.researchPool(), port.recipes()), PORT, List.of());
        assertSingleDifference(present, "research", "ASPECT_ANALYZER", "presence");
        assertEquals("registered only when LoadedMods.computerCraftLoaded", present.failures.get(0).original());
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
            Result result = ParityComparator.compare(oracle, new PortSnapshot(research, port.researchPool(), port.recipes()), PORT, List.of());
            assertSingleDifference(result, "research", "ICHOR", "parentsHidden");
            assertTrue(result.failures.get(0).port().contains("missing [" + drop + "]"), result.failures.get(0).toString());
        }

        Map<String, PortResearch> research = new LinkedHashMap<>(port.research());
        List<String> hidden = new ArrayList<>(ichor.parentsHidden());
        hidden.add("SOMEKAMI");
        research.put("ICHOR", new PortResearch("ICHOR", ichor.category(), ichor.aspects(), ichor.column(), ichor.row(),
                ichor.complexity(), ichor.parents(), hidden, ichor.siblings(), ichor.flags(), ichor.warp(), ichor.pages(), ichor.icon()));
        Result result = ParityComparator.compare(oracle, new PortSnapshot(research, port.researchPool(), port.recipes()), PORT, List.of());
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
            r.addProperty("width", 4);
        });
        assertTrue(shape.failures.stream().anyMatch(d -> d.subject().equals("MAGNET") && d.field().equals("shape")), shape.describe());
        assertTrue(shape.failures.stream().anyMatch(d -> d.subject().equals("MAGNET") && d.field().equals("ingredients.count")) == false,
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
        Result missing = ParityComparator.compare(oracle, new PortSnapshot(port.research(), port.researchPool(), recipes), PORT, List.of());
        assertSingleDifference(missing, "recipe", "MAGNET", "presence");

        recipes.add(magnet);
        recipes.add(new PortRecipe("tt:extra", "EXTRA", "crucible", "EXTRA", "thaumictinkerer:extra", 1, Map.of(), null, 0, 0, List.of(List.of("minecraft:stick")), List.of()));
        Result extra = ParityComparator.compare(oracle, new PortSnapshot(port.research(), port.researchPool(), recipes), PORT, List.of());
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
        Result result = ParityComparator.compare(oracle, new PortSnapshot(port.research(), port.researchPool(), recipes), PORT, List.of());
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
        assertTrue(checked >= 120, "checked " + checked);
        assertEquals(129, real.recipes().size());
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

        Result unlisted = ParityComparator.compare(oracle, changed, PORT, List.of());
        assertSingleDifference(unlisted, "research", "ENCHANTER", "column");

        Result allowed = ParityComparator.compare(oracle, changed, PORT, List.of(known("research", "ENCHANTER", "column", "5", "6")));
        assertTrue(allowed.passed(), allowed.describe());
        assertEquals(1, allowed.allowed.size());

        Result otherValue = ParityComparator.compare(oracle, changed, PORT, List.of(known("research", "ENCHANTER", "column", "5", "7")));
        assertFalse(otherValue.passed());
        assertEquals(1, otherValue.failures.size(), "the real difference is still a failure");
        assertEquals(1, otherValue.unusedKnown.size(), "and the entry that did not match is stale");

        Result otherField = ParityComparator.compare(oracle, changed, PORT, List.of(known("research", "ENCHANTER", "row", "5", "6")));
        assertEquals(1, otherField.failures.size());
        Result otherKey = ParityComparator.compare(oracle, changed, PORT, List.of(known("research", "MAGNETS", "column", "5", "6")));
        assertEquals(1, otherKey.failures.size());
        Result otherKind = ParityComparator.compare(oracle, changed, PORT, List.of(known("recipe", "ENCHANTER", "column", "5", "6")));
        assertEquals(1, otherKind.failures.size());
    }

    @Test
    void anAllowListEntryThatMatchesNothingIsAFailure() {
        OracleData oracle = OracleData.fromClasspath();
        Result result = ParityComparator.compare(oracle, PortFixtures.fromOracle(oracle), PORT,
                List.of(known("research", "ENCHANTER", "column", "5", "6")));
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
