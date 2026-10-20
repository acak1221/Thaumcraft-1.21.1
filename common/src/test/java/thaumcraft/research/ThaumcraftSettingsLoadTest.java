package thaumcraft.research;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import net.minecraft.SharedConstants;
import net.minecraft.server.Bootstrap;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import thaumcraft.common.config.ResearchSettings;
import thaumcraft.common.config.ThaumcraftSettings;
import thaumcraft.common.lib.WarpEvents;

/** Loading the server research settings file: the first-run file, every option, and the fallback for an unreadable file. */
class ThaumcraftSettingsLoadTest {
    @BeforeAll static void bootstrap() {
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
    }

    @TempDir Path folder;
    private ResearchSettings before;
    private boolean wussBefore;

    @BeforeEach void remember() {
        before = ThaumcraftSettings.server();
        wussBefore = WarpEvents.isWussMode();
    }

    @AfterEach void restore() {
        ThaumcraftSettings.configureServer(before);
        WarpEvents.setWussMode(wussBefore);
    }

    @Test void aMissingFileIsCreatedWithTheDocumentedDefaultsAndReadsBackAsThem() throws IOException {
        Path file = folder.resolve("config").resolve("thaumcraft.properties");
        ThaumcraftSettings.load(file);
        assertTrue(Files.isRegularFile(file), "the first run writes the file, including its missing folder");
        assertEquals(ResearchSettings.DEFAULTS, ThaumcraftSettings.server());
        String text = Files.readString(file, StandardCharsets.UTF_8);
        for (String line : new String[] {"allow_cheat_sheet=false", "allow_warded_stone=true", "allow_mirrors=true", "hard_mode_nodes=true",
                "wuss_mode=false", "research_difficulty=0", "aspect_total_cap=100"}) {
            assertTrue(text.contains(line + "\n"), line);
        }
        assertTrue(text.startsWith("#"), "the file starts with its explanation");
    }

    @Test void theFileThatWasJustWrittenIsNotOverwrittenByTheNextLoad() throws IOException {
        Path file = folder.resolve("thaumcraft.properties");
        ThaumcraftSettings.load(file);
        Files.writeString(file, "allow_cheat_sheet=true\nresearch_difficulty=-1\n", StandardCharsets.UTF_8);
        ThaumcraftSettings.load(file);
        assertEquals(new ResearchSettings(true, true, true, true, false, -1, 100), ThaumcraftSettings.server());
        assertEquals("allow_cheat_sheet=true\nresearch_difficulty=-1\n", Files.readString(file, StandardCharsets.UTF_8));
    }

    @Test void everyOptionOfAnExistingFileIsApplied() throws IOException {
        Path file = folder.resolve("thaumcraft.properties");
        Files.writeString(file, "allow_cheat_sheet=true\nallow_warded_stone=false\nallow_mirrors=false\nhard_mode_nodes=false\n"
                + "wuss_mode=true\nresearch_difficulty=1\naspect_total_cap=250\n", StandardCharsets.UTF_8);
        ThaumcraftSettings.load(file);
        assertEquals(new ResearchSettings(true, false, false, false, true, 1, 250), ThaumcraftSettings.server());
    }

    @Test void wussModeIsPushedToTheWarpSystemOnEveryLoad() throws IOException {
        Path file = folder.resolve("thaumcraft.properties");
        Files.writeString(file, "wuss_mode=true\n", StandardCharsets.UTF_8);
        ThaumcraftSettings.load(file);
        assertTrue(WarpEvents.isWussMode());
        Files.writeString(file, "wuss_mode=false\n", StandardCharsets.UTF_8);
        ThaumcraftSettings.load(file);
        assertFalse(WarpEvents.isWussMode());
    }

    @Test void aFileThatCannotBeReadFallsBackToTheOriginalDefaults() {
        ThaumcraftSettings.configureServer(new ResearchSettings(true, false, false, false, true, 1, 5));
        ThaumcraftSettings.load(folder);
        assertEquals(ResearchSettings.DEFAULTS, ThaumcraftSettings.server(), "a directory is not a settings file");
        assertFalse(WarpEvents.isWussMode(), "the fallback also switches wuss mode back off");
    }

    @Test void theCurrentSettingsOfAnyOtherThreadAreTheServerSettings() throws IOException {
        Path file = folder.resolve("thaumcraft.properties");
        Files.writeString(file, "aspect_total_cap=7\n", StandardCharsets.UTF_8);
        ThaumcraftSettings.load(file);
        assertEquals(7, ThaumcraftSettings.current().aspectCap());
        assertEquals(ThaumcraftSettings.server(), ThaumcraftSettings.current());
    }
}
