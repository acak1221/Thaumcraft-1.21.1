package thaumcraft.runic;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import thaumcraft.common.lib.runic.RunicSettings;

/** The runic shield's server settings: the original three config values, their clamps, and what a bad file falls back to. */
class RunicSettingsTest {
    @TempDir Path folder;

    @AfterEach void restoreTheDefaults() { RunicSettings.load(folder.resolve("does-not-exist.properties")); }

    private Path file(String text) throws IOException {
        Path file = folder.resolve("runic.properties");
        Files.writeString(file, text, StandardCharsets.UTF_8);
        return file;
    }

    @Test void theOriginalDefaultsAreTwoSecondsEightyTicksAndFiftyVis() {
        assertEquals(2000, RunicSettings.DEFAULTS.rechargeMillis());
        assertEquals(80, RunicSettings.DEFAULTS.delayTicks());
        assertEquals(50, RunicSettings.DEFAULTS.cost());
    }

    @Test void theConstructorClampsEveryValueToItsOriginalMinimum() {
        assertEquals(new RunicSettings(500, 0, 0), new RunicSettings(0, -1, -1));
        assertEquals(new RunicSettings(500, 0, 0), new RunicSettings(Integer.MIN_VALUE, Integer.MIN_VALUE, Integer.MIN_VALUE));
        assertEquals(new RunicSettings(500, 10, 1), new RunicSettings(500, 10, 1), "values at the minimum stay");
        assertEquals(new RunicSettings(Integer.MAX_VALUE, Integer.MAX_VALUE, Integer.MAX_VALUE).cost(), Integer.MAX_VALUE, "there is no upper clamp");
    }

    @Test void aMissingFileGivesTheDefaults() {
        RunicSettings.load(folder.resolve("nothing-here.properties"));
        assertEquals(RunicSettings.DEFAULTS, RunicSettings.current());
    }

    @Test void theThreeKeysAreReadFromTheFile() throws IOException {
        RunicSettings.load(file("runic_recharge_speed=3000\nrunic_recharge_delay=120\nrunic_cost=75\n"));
        assertEquals(new RunicSettings(3000, 120, 75), RunicSettings.current());
    }

    @Test void valuesBelowTheMinimumAreClampedNotRejected() throws IOException {
        RunicSettings.load(file("runic_recharge_speed=100\nrunic_recharge_delay=-20\nrunic_cost=-1\n"));
        assertEquals(new RunicSettings(500, 0, 0), RunicSettings.current());
    }

    @Test void aKeyThatIsMissingKeepsItsOwnDefaultOnly() throws IOException {
        RunicSettings.load(file("runic_cost=10\n"));
        assertEquals(new RunicSettings(2000, 80, 10), RunicSettings.current());
    }

    @Test void aMalformedNumberFallsBackToTheDefaultOfThatKeyBeforeAnyClamp() throws IOException {
        RunicSettings.load(file("runic_recharge_speed=fast\nrunic_recharge_delay=1.5\nrunic_cost=\n"));
        assertEquals(RunicSettings.DEFAULTS, RunicSettings.current());
        RunicSettings.load(file("runic_recharge_speed=99999999999\nrunic_cost=7\n"));
        assertEquals(new RunicSettings(2000, 80, 7), RunicSettings.current(), "an out-of-range integer is malformed too");
    }

    @Test void surroundingWhitespaceAroundANumberIsTolerated() throws IOException {
        RunicSettings.load(file("runic_recharge_speed =  2500  \nrunic_cost=\t60\n"));
        assertEquals(new RunicSettings(2500, 80, 60), RunicSettings.current());
    }

    @Test void commentsAndUnknownKeysAreIgnored() throws IOException {
        RunicSettings.load(file("# runic_cost=1\n! runic_recharge_delay=1\nsomething_else=5\nrunic_cost=40\n"));
        assertEquals(new RunicSettings(2000, 80, 40), RunicSettings.current());
    }

    @Test void aPathThatCannotBeReadAsAFileFallsBackToTheDefaults() throws IOException {
        RunicSettings.load(file("runic_cost=1\n"));
        assertEquals(1, RunicSettings.current().cost());
        RunicSettings.load(folder);
        assertSame(RunicSettings.DEFAULTS, RunicSettings.current(), "a directory is not a settings file");
    }

    @Test void aLaterLoadReplacesTheEarlierOne() throws IOException {
        RunicSettings.load(file("runic_cost=1\n"));
        RunicSettings.load(file("runic_cost=2\n"));
        assertEquals(2, RunicSettings.current().cost());
    }
}
