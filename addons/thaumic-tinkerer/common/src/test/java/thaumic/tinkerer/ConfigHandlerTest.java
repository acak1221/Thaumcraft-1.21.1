package thaumic.tinkerer;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import thaumic.tinkerer.common.core.handler.ConfigHandler;

/** Test-only code of the port (no 1.7.10 counterpart), CC BY-NC-SA 3.0 like the add-on. */
class ConfigHandlerTest {
    @TempDir Path dir;

    @AfterEach
    void restoreDefaults() {
        ConfigHandler.resetToDefaults();
    }

    @Test
    void aMissingFileGetsTheDefaultsAndIsWritten() throws IOException {
        Path file = dir.resolve("thaumictinkerer.json");
        ConfigHandler.loadConfig(file);
        assertTrue(Files.isRegularFile(file), "the defaults are written");
        assertTrue(ConfigHandler.enableKami);
        assertEquals(49, ConfigHandler.soulHeartHeight);
        assertEquals(15, ConfigHandler.visShadowbeamAir);
        String text = Files.readString(file);
        assertTrue(text.contains("\"kami.forceenabled\": true"), text);
    }

    @Test
    void valuesFromTheFileWinAndAreClamped() throws IOException {
        Path file = dir.resolve("thaumictinkerer.json");
        Files.writeString(file, """
                {"general": {"soulHeart.height": 9999, "modFlight.enabled": false, "forbiddenDimensions": ["minecraft:the_end"]},
                 "kami": {"kami.forceenabled": false},
                 "balance.shadowbeam": {"baseDamage": 3}}
                """, StandardCharsets.UTF_8);
        ConfigHandler.loadConfig(file);
        assertEquals(256, ConfigHandler.soulHeartHeight, "clamped to the 1.7.10 range");
        assertFalse(ConfigHandler.enableFlight);
        assertFalse(ConfigHandler.enableKami);
        assertEquals(3, ConfigHandler.baseDamageShadowbeam);
        assertArrayEquals(new String[] {"minecraft:the_end"}, ConfigHandler.forbiddenDimensions);
        String rewritten = Files.readString(file);
        assertTrue(rewritten.contains("\"imbuedFire.cake.enabled\": true"), "unknown keys appear with their defaults");
    }

    @Test
    void aBrokenFileFallsBackToTheDefaults() throws IOException {
        Path file = dir.resolve("thaumictinkerer.json");
        Files.writeString(file, "{ not json", StandardCharsets.UTF_8);
        ConfigHandler.loadConfig(file);
        assertTrue(ConfigHandler.enableKami);
        assertEquals(49, ConfigHandler.soulHeartHeight);
    }
}
