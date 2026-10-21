package thaumcraft.client;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import thaumcraft.client.fx.particles.FXSmokeSpiral;
import thaumcraft.common.config.ThaumcraftConfigStore;

/**
 * The numbers of the native smoke spiral and the catalogue of the native configuration screen.
 *
 * <p>Original smoke spiral (oracle {@code thaumcraft/client/fx/particles/FXSmokeSpiral.java:46-53}):
 * the turn is {@code start + 720 * t} degrees, the polar angle is {@code 90 - 180 * t} degrees, the
 * circle follows that angle and the radius scales the whole circle. The first frame therefore
 * starts below its owner and the last one ends above it.
 *
 * <p>Original configuration screen (oracle {@code thaumcraft/client/gui/ThaumcraftGuiConfig.java:10-34}
 * and {@code thaumcraft/common/config/Config.java:104-111}): the five categories are general,
 * Monster_Spawning, World_Generation, World_Regeneration and Research.
 */
class ClientEffectsParityTest {
    @TempDir Path folder;

    @Test void theSmokeSpiralTurnsSevenHundredAndTwentyDegreesAroundItsOwner() {
        var start = FXSmokeSpiral.spiralOffset(1.5F, 0, 0F);
        // Original FXSmokeSpiral:48-53: the polar angle starts at 90 degrees, so the spiral starts
        // one radius below its owner and on the axis.
        assertEquals(0.0, start.x, 1.0E-6);
        assertEquals(-1.5, start.y, 1.0E-6);
        assertEquals(0.0, start.z, 1.0E-6);
        var end = FXSmokeSpiral.spiralOffset(1.5F, 0, 1F);
        // The polar angle ends at -90 degrees, so the spiral ends one radius above its owner.
        assertEquals(1.5, end.y, 1.0E-6);
        // The turn is a full 720 degrees, so the end lies on the same axis as the start.
        assertEquals(start.x, end.x, 1.0E-5);
        assertEquals(start.z, end.z, 1.0E-5);
        // The middle of the life is the equator of the circle.
        var middle = FXSmokeSpiral.spiralOffset(1.5F, 0, 0.5F);
        assertEquals(0.0, middle.y, 1.0E-6);
        assertEquals(1.5, Math.sqrt(middle.x * middle.x + middle.z * middle.z), 1.0E-6);
    }

    @Test void theSmokeSpiralKeepsTheRadiusOfItsOwner() {
        for (float t = 0F; t <= 1F; t += 0.1F) {
            var point = FXSmokeSpiral.spiralOffset(2.5F, 90, t);
            // The 1.7.10 MathHelper sin/cos are the fast approximations of 1.21.1 too, so the
            // circle keeps its radius to the accuracy of those tables.
            assertEquals(2.5, point.length(), 1.0E-3, "the circle of the spiral keeps the radius");
        }
    }

    @Test void theConfigurationScreenShowsTheFiveOriginalCategories() {
        assertEquals(5, ThaumcraftConfigStore.Category.values().length);
        assertEquals("general", ThaumcraftConfigStore.Category.GENERAL.id());
        assertEquals("Monster_Spawning", ThaumcraftConfigStore.Category.MONSTER_SPAWNING.id());
        assertEquals("World_Generation", ThaumcraftConfigStore.Category.WORLD_GENERATION.id());
        assertEquals("World_Regeneration", ThaumcraftConfigStore.Category.WORLD_REGENERATION.id());
        assertEquals("Research", ThaumcraftConfigStore.Category.RESEARCH.id());
    }

    @Test void everyConfigurationKeyOfTheScreenIsAKeyOfTheOriginal() {
        // The original keys of oracle thaumcraft/common/config/Config.java:369-500.
        java.util.Set<String> original = java.util.Set.of("allow_mirrors", "hard_mode_nodes", "wuss_mode",
                "allow_cheat_sheet", "allow_warded_stone", "blue_magical_forest", "glowing_taint",
                "biome_taint_spread", "spawn_angry_zombies", "spawn_fire_bats", "spawn_wisps",
                "spawn_taintacles", "spawn_taint_spores", "spawn_pechs", "spawn_eldercreatures",
                "generate_aura_nodes", "generate_structures", "generate_cinnibar_ore", "generate_amber_ore",
                "generate_infused_stone", "generate_trees", "generate_taint", "research_difficulty",
                "aspect_total_cap");
        for (ThaumcraftConfigStore.Entry entry : ThaumcraftConfigStore.ENTRIES) {
            if ("shaders".equals(entry.key())) continue; // the original general category, oracle Config.java:410
            assertTrue(original.contains(entry.key()), entry.key() + " is not an original configuration key");
        }
    }

    @Test void theConfigurationFileKeepsItsHeaderAndItsUnknownKeys() throws IOException {
        Path file = folder.resolve("thaumcraft.properties");
        Files.writeString(file, "# a hand written header\nallow_mirrors=false\nresearch_difficulty=-1\nunknown_key=7\n",
                StandardCharsets.UTF_8);
        Properties values = ThaumcraftConfigStore.read(file);
        assertEquals("false", values.getProperty("allow_mirrors"));
        assertEquals("-1", values.getProperty("research_difficulty"));
        values.setProperty("allow_mirrors", "true");
        values.setProperty("wuss_mode", "true");
        ThaumcraftConfigStore.write(file, values);
        String text = Files.readString(file, StandardCharsets.UTF_8);
        assertTrue(text.startsWith("# Thaumcraft server research settings"), "the file keeps its header");
        assertTrue(text.contains("allow_mirrors=true\n"), "the changed value is written");
        assertTrue(text.contains("wuss_mode=true\n"), "a changed value that the file did not hold is written");
        assertTrue(text.contains("unknown_key=7\n"), "a key the screen does not know survives");
        Properties again = ThaumcraftConfigStore.read(file);
        assertEquals("true", again.getProperty("allow_mirrors"));
        assertEquals("7", again.getProperty("unknown_key"));
    }
}
