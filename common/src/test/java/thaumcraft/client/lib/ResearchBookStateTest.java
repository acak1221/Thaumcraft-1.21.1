package thaumcraft.client.lib;

import static org.junit.jupiter.api.Assertions.*;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/** The port's own Thaumonomicon conveniences: the reopened page and the bookmark file. */
class ResearchBookStateTest {
    @TempDir Path folder;

    @AfterEach void forget() { ResearchBookState.forgetPage(); }

    @Test void aClosedPageIsReopenedUntilThePlayerGoesBackToTheMap() {
        assertNull(ResearchBookState.rememberedResearch());
        ResearchBookState.rememberPage("NITOR", 4);
        assertEquals("NITOR", ResearchBookState.rememberedResearch());
        assertEquals(4, ResearchBookState.rememberedPage());
        ResearchBookState.forgetPage();
        assertNull(ResearchBookState.rememberedResearch());
        assertEquals(0, ResearchBookState.rememberedPage());
    }

    @Test void bookmarksToggleAndSurviveAReload() throws Exception {
        Path file = folder.resolve("config/thaumcraft-bookmarks.txt");
        var bookmarks = ResearchBookState.Bookmarks.load(file);
        assertTrue(bookmarks.keys().isEmpty(), "a missing file is no bookmarks");
        assertTrue(bookmarks.toggle("NITOR"));
        assertTrue(bookmarks.toggle("ALUMENTUM"));
        assertFalse(bookmarks.toggle("NITOR"), "a second toggle removes the bookmark");
        assertEquals(List.of("ALUMENTUM"), bookmarks.keys());
        assertEquals(List.of("ALUMENTUM"), Files.readAllLines(file, StandardCharsets.UTF_8));
        assertEquals(List.of("ALUMENTUM"), ResearchBookState.Bookmarks.load(file).keys());
        bookmarks.remove("ALUMENTUM");
        assertTrue(ResearchBookState.Bookmarks.load(file).keys().isEmpty());
    }

    @Test void aDamagedFileKeepsOnlyValidKeysAndTheLimit() throws Exception {
        Path file = folder.resolve("thaumcraft-bookmarks.txt");
        StringBuilder text = new StringBuilder("  NITOR \n\nnot a key!\n@CLUE\n");
        for (int index = 0; index < 20; index++) text.append("KEY").append(index).append('\n');
        Files.writeString(file, text, StandardCharsets.UTF_8);
        var bookmarks = ResearchBookState.Bookmarks.load(file);
        assertEquals(ResearchBookState.MAX_BOOKMARKS, bookmarks.keys().size());
        assertEquals("NITOR", bookmarks.keys().getFirst());
        assertTrue(bookmarks.contains("@CLUE"));
        assertTrue(bookmarks.full());
        assertFalse(bookmarks.toggle("ANOTHER"), "a full list takes no further bookmark");
        assertFalse(bookmarks.toggle("bad key"));
    }
}
