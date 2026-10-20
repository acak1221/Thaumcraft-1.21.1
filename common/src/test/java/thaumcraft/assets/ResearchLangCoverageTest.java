package thaumcraft.assets;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;

/** The bundled research corpus (tc4/research.json) against the language files and the aspect list. */
class ResearchLangCoverageTest {
    private static final String RESEARCH = "data/thaumcraft/tc4/research.json";
    private static final String EN = "assets/thaumcraft/lang/en_us.json";
    private static final String RU = "assets/thaumcraft/lang/ru_ru.json";

    /** Wand-part research that only gates a recipe: the original gave it no name or text of its own. */
    private static final Set<String> WITHOUT_TEXT = Set.of("CAP_iron", "ROD_wood");

    private static JsonArray research() throws Exception { return ResourceTree.object(RESEARCH).getAsJsonArray("research"); }

    private static Set<String> keys() throws Exception {
        Set<String> keys = new HashSet<>();
        for (JsonElement element : research()) keys.add(element.getAsJsonObject().get("key").getAsString());
        return keys;
    }

    @Test void everyResearchKeyIsUnique() throws Exception {
        assertEquals(research().size(), keys().size());
        assertFalse(keys().isEmpty());
    }

    @Test void everyResearchHasANameAndTextInBothLanguagesExceptTheListedOnes() throws Exception {
        JsonObject en = ResourceTree.object(EN);
        JsonObject ru = ResourceTree.object(RU);
        List<String> missing = new ArrayList<>();
        for (String key : keys()) {
            if (WITHOUT_TEXT.contains(key)) continue;
            for (String prefix : new String[] {"tc.research_name.", "tc.research_text."}) {
                if (!en.has(prefix + key)) missing.add("en_us " + prefix + key);
                if (!ru.has(prefix + key)) missing.add("ru_ru " + prefix + key);
            }
        }
        assertEquals(List.of(), missing);
    }

    @Test void theResearchWithoutTextReallyHasNone() throws Exception {
        JsonObject en = ResourceTree.object(EN);
        List<String> stale = new ArrayList<>();
        for (String key : WITHOUT_TEXT) if (!keys().contains(key) || en.has("tc.research_name." + key) || en.has("tc.research_text." + key)) stale.add(key);
        assertEquals(List.of(), stale, "delete from WITHOUT_TEXT: the research is gone or has been given a text");
    }

    @Test void everyTextPageNamesATextThatExistsInBothLanguages() throws Exception {
        JsonObject en = ResourceTree.object(EN);
        JsonObject ru = ResourceTree.object(RU);
        List<String> missing = new ArrayList<>();
        for (JsonElement element : research()) {
            JsonObject definition = element.getAsJsonObject();
            for (JsonElement page : definition.getAsJsonArray("pages")) {
                JsonElement textKey = page.getAsJsonObject().get("text_key");
                if (textKey == null || textKey.isJsonNull()) continue;
                String key = textKey.getAsString();
                if (!en.has(key)) missing.add(definition.get("key").getAsString() + " en_us " + key);
                if (!ru.has(key)) missing.add(definition.get("key").getAsString() + " ru_ru " + key);
            }
        }
        assertEquals(List.of(), missing);
    }

    @Test void parentsHiddenParentsAndSiblingsNameExistingResearch() throws Exception {
        Set<String> keys = keys();
        List<String> dangling = new ArrayList<>();
        for (JsonElement element : research()) {
            JsonObject definition = element.getAsJsonObject();
            for (String field : new String[] {"parents", "parents_hidden", "siblings"}) {
                for (JsonElement reference : definition.getAsJsonArray(field)) {
                    if (!keys.contains(reference.getAsString())) dangling.add(definition.get("key").getAsString() + "." + field + " -> " + reference.getAsString());
                }
            }
        }
        assertEquals(List.of(), dangling);
    }

    @Test void pagesGatedByResearchNameExistingResearch() throws Exception {
        Set<String> keys = keys();
        List<String> dangling = new ArrayList<>();
        for (JsonElement element : research()) {
            JsonObject definition = element.getAsJsonObject();
            for (JsonElement page : definition.getAsJsonArray("pages")) {
                JsonElement required = page.getAsJsonObject().get("required_research");
                if (required != null && !required.isJsonNull() && !keys.contains(required.getAsString())) dangling.add(definition.get("key").getAsString() + " -> " + required.getAsString());
            }
        }
        assertEquals(List.of(), dangling);
    }

    @Test void everyAspectCostUsesAKnownAspectWithAPositiveAmount() throws Exception {
        Set<String> aspects = new HashSet<>();
        for (JsonElement aspect : ResourceTree.object("data/thaumcraft/tc4/aspects.json").getAsJsonArray("aspects")) aspects.add(aspect.getAsJsonObject().get("tag").getAsString());
        assertEquals(48, aspects.size(), "the original aspect list");
        List<String> bad = new ArrayList<>();
        for (JsonElement element : research()) {
            JsonObject definition = element.getAsJsonObject();
            for (var cost : definition.getAsJsonObject("aspect_cost").entrySet()) {
                if (!aspects.contains(cost.getKey())) bad.add(definition.get("key").getAsString() + " unknown aspect " + cost.getKey());
                else if (cost.getValue().getAsInt() <= 0) bad.add(definition.get("key").getAsString() + " non-positive cost of " + cost.getKey());
            }
        }
        assertEquals(List.of(), bad);
    }
}
