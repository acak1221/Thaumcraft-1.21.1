package thaumcraft.common.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import java.nio.file.Path;
import java.util.Properties;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class ComprehensiveConfigRoundTripReviewTest {
    @TempDir Path directory;

    @Test void savingConfigPreservesUnknownPropertiesWithoutChangingKnownSettings() {
        Properties input = new Properties();
        input.setProperty("addon.path", "C:\\mods\\thaumcraft");
        input.setProperty("addon.caption", " leading space and Unicode: магия");
        input.setProperty("addon.instructions", "first\nhard_mode_nodes=false\nlast\r\t\f");
        input.setProperty("addon:key=part", "one=two:three # literal!");
        input.setProperty(" addon key", "  spaced value");
        Path file = directory.resolve("nested/thaumcraft.properties");
        ThaumcraftConfigStore.write(file, input);
        Properties restored = ThaumcraftConfigStore.read(file);
        for (String key : input.stringPropertyNames()) {
            assertEquals(input.getProperty(key), restored.getProperty(key), "round trip for " + key);
        }
        assertEquals("true", restored.getProperty("hard_mode_nodes"),
                "a newline in an unrelated value must not become a new configuration entry");
    }

    @Test void normalKnownSettingsKeepTheirValuesAcrossRepeatedSaves() {
        Properties input = new Properties();
        input.setProperty("hard_mode_nodes", "false");
        input.setProperty("aspect_total_cap", "250");
        Path file = directory.resolve("thaumcraft.properties");
        ThaumcraftConfigStore.write(file, input);
        ThaumcraftConfigStore.write(file, ThaumcraftConfigStore.read(file));
        Properties restored = ThaumcraftConfigStore.read(file);
        assertEquals("false", restored.getProperty("hard_mode_nodes"));
        assertEquals("250", restored.getProperty("aspect_total_cap"));
    }
}
