package thaumic.tinkerer.parity;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.Test;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import thaumic.tinkerer.testing.parity.KnownDifference;

/**
 * The allow-list of the parity check (tt_oracle/known_differences.json) may only hold differences that a deviation
 * text documents: each entry names the file below docs/deviations, quotes the sentence, and the passage around the sentence
 * has to name the entry (see {@link DeviationQuotes}). Test-only code of the port (no 1.7.10 counterpart), CC BY-NC-SA 3.0
 * like the add-on.
 */
class KnownDifferencesTest {
    private static Path deviations() {
        for (Path p : List.of(Path.of("docs/deviations"), Path.of("addons/thaumic-tinkerer/docs/deviations"))) {
            if (Files.isDirectory(p)) return p;
        }
        throw new AssertionError("docs/deviations not found from " + Path.of("").toAbsolutePath());
    }

    private static KnownDifference entry(String kind, String key, String deviation, String quote, List<String> mentions) {
        JsonObject o = new JsonObject();
        o.addProperty("kind", kind);
        o.addProperty("key", key);
        o.addProperty("field", "presence");
        o.addProperty("original", "a");
        o.addProperty("port", "b");
        o.addProperty("reason", "a test");
        o.addProperty("deviation", deviation);
        o.addProperty("quote", quote);
        JsonArray names = new JsonArray();
        mentions.forEach(names::add);
        o.add("mentions", names);
        JsonArray list = new JsonArray();
        list.add(o);
        JsonObject doc = new JsonObject();
        doc.add("differences", list);
        return KnownDifference.parse(doc).get(0);
    }

    private static final String MACHINES_SENTENCE = "Without a computer mod the block would be unobtainable, so it is registered always";

    @Test
    void everyKnownDifferenceQuotesASentenceOfADeviationTextThatNamesIt() throws IOException {
        List<KnownDifference> known = KnownDifference.fromClasspath();
        assertFalse(known.isEmpty());
        Set<String> seen = new HashSet<>();
        for (KnownDifference k : known) {
            assertFalse(k.reason().isBlank(), k + " has no reason");
            assertTrue(seen.add(k.kind() + "|" + k.key() + "|" + k.field() + "|" + k.original() + "|" + k.port()), "duplicate entry " + k);
            assertEquals(List.of(), DeviationQuotes.problems(k, deviations()), k.toString());
        }
    }

    @Test
    void anEntryAllowsOnlyAPresenceOrAValueDifferenceItNamesExactly() {
        for (KnownDifference k : KnownDifference.fromClasspath()) {
            assertFalse(k.original().equals(k.port()), k + " allows nothing");
            assertTrue(k.kind().equals("research") || k.kind().equals("recipe"), k.toString());
            assertTrue(k.occurrences() >= 1, k.toString());
        }
    }

    @Test
    void theRealCitationOfTheAspectalyzerPasses() throws IOException {
        KnownDifference k = entry("research", "ASPECT_ANALYZER", "Machines.md", MACHINES_SENTENCE, List.of("BlockAspectAnalyzer", "Aspectalyzer"));
        assertEquals(List.of(), DeviationQuotes.problems(k, deviations()));
    }

    @Test
    void aSentenceThatIsNotInTheFileFails() throws IOException {
        KnownDifference k = entry("research", "ASPECT_ANALYZER", "Machines.md", "The Aspectalyzer is registered always, said nobody",
                List.of("Aspectalyzer"));
        assertFalse(DeviationQuotes.problems(k, deviations()).isEmpty());
    }

    @Test
    void aRealSentenceOfAnotherPassageDoesNotAllowListAnotherEntry() throws IOException {
        // the Aspectalyzer sentence, used for the Remote Placer: its passage does not name REMOTE_PLACER
        KnownDifference other = entry("research", "REMOTE_PLACER", "Machines.md", MACHINES_SENTENCE, List.of("REMOTE_PLACER"));
        List<String> problems = DeviationQuotes.problems(other, deviations());
        assertTrue(problems.stream().anyMatch(p -> p.contains("REMOTE_PLACER") && p.contains("not in the passage")), problems.toString());

        // naming what the passage does contain does not help, because that is not the entry's own name
        KnownDifference borrowed = entry("research", "REMOTE_PLACER", "Machines.md", MACHINES_SENTENCE, List.of("Aspectalyzer"));
        problems = DeviationQuotes.problems(borrowed, deviations());
        assertTrue(problems.stream().anyMatch(p -> p.contains("no mention names the entry")), problems.toString());
    }

    @Test
    void aRealSentenceOfTheSameFileFarFromTheEntryFails() throws IOException {
        // a sentence of TileBlocks.md section 2.4 (Golem Connector) cannot allow-list the Remote Placer's column
        KnownDifference k = entry("research", "REMOTE_PLACER", "TileBlocks.md",
                "The port registers it always, with its research, recipe and the Binder's golem binding", List.of("REMOTE_PLACER"));
        assertFalse(DeviationQuotes.problems(k, deviations()).isEmpty());
    }

    @Test
    void anEntryWithoutMentionsOrWithoutAFileFails() throws IOException {
        assertFalse(DeviationQuotes.problems(entry("research", "ASPECT_ANALYZER", "Machines.md", MACHINES_SENTENCE, List.of()), deviations()).isEmpty());
        assertFalse(DeviationQuotes.problems(entry("research", "ASPECT_ANALYZER", "NoSuchGroup.md", MACHINES_SENTENCE, List.of("Aspectalyzer")),
                deviations()).isEmpty());
    }

    @Test
    void aRecipeEntryIsTiedByItsNameOrByTheRegistryPathOfItsResult() {
        KnownDifference k = entry("recipe", "<unnamed> -> thaumictinkerer:dark_quartz_stairs", "SimpleBlocks.md", "x", List.of("stairs"));
        assertEquals(List.of("darkquartzstairs"), DeviationQuotes.ownNames(k));
        k = entry("recipe", "ASPECT_ANALYZER -> thaumictinkerer:aspect_analyzer", "Machines.md", "x", List.of("x"));
        assertEquals(List.of("aspectanalyzer", "aspectanalyzer"), DeviationQuotes.ownNames(k));
    }
}
