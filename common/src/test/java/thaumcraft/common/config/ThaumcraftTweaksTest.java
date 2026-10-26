package thaumcraft.common.config;

import static org.junit.jupiter.api.Assertions.*;

import java.util.Properties;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import thaumcraft.common.config.ThaumcraftTweaks.EarthShockTargets;
import thaumcraft.common.config.ThaumcraftTweaks.Values;

/** The settings adopted from TC4Tweaks: their defaults, the file keys and the earth shock table. */
class ThaumcraftTweaksTest {
    @AfterEach void restore() { ThaumcraftTweaks.configure(ThaumcraftTweaks.DEFAULTS); }

    @Test void theDefaultsAreTheTc4TweaksOnesAndTheOriginalIsEverythingOff() {
        assertEquals(new Values(true, EarthShockTargets.ONLY_LIVING, true, true, true, true, true), ThaumcraftTweaks.DEFAULTS);
        assertEquals(new Values(false, EarthShockTargets.ALL, false, false, false, false, false), ThaumcraftTweaks.ORIGINAL);
        ThaumcraftTweaks.load(new Properties());
        assertEquals(ThaumcraftTweaks.DEFAULTS, ThaumcraftTweaks.values());
    }

    @Test void theFileKeysSwitchEverySettingBackToTheOriginal() {
        Properties file = new Properties();
        file.setProperty("more_randomized_loot", "false");
        file.setProperty("earth_shock_targets", "0");
        file.setProperty("infusion_keeps_item_data", " FALSE ");
        file.setProperty("filled_container_to_hand", "false");
        file.setProperty("research_search", "false");
        file.setProperty("research_counter", "false");
        file.setProperty("notification_clear_button", "false");
        ThaumcraftTweaks.load(file);
        assertEquals(ThaumcraftTweaks.ORIGINAL, ThaumcraftTweaks.values());
    }

    @Test void anUnreadableValueKeepsItsDefault() {
        Properties file = new Properties();
        file.setProperty("more_randomized_loot", "maybe");
        file.setProperty("earth_shock_targets", "7");
        file.setProperty("infusion_keeps_item_data", "");
        file.setProperty("filled_container_to_hand", "no");
        ThaumcraftTweaks.load(file);
        assertEquals(ThaumcraftTweaks.DEFAULTS, ThaumcraftTweaks.values());
        file.setProperty("earth_shock_targets", "one");
        ThaumcraftTweaks.load(file);
        assertEquals(EarthShockTargets.ONLY_LIVING, ThaumcraftTweaks.earthShock());
    }

    @Test void earthShockHurtsWhatItsSettingNames() {
        // living, item or experience orb, anything else (an arrow, a minecart)
        assertTrue(ThaumcraftTweaks.earthShockHurts(EarthShockTargets.ALL, false, true));
        assertTrue(ThaumcraftTweaks.earthShockHurts(EarthShockTargets.ALL, false, false));
        assertTrue(ThaumcraftTweaks.earthShockHurts(EarthShockTargets.EXCEPT_ITEMS_AND_XP, true, false));
        assertTrue(ThaumcraftTweaks.earthShockHurts(EarthShockTargets.EXCEPT_ITEMS_AND_XP, false, false));
        assertFalse(ThaumcraftTweaks.earthShockHurts(EarthShockTargets.EXCEPT_ITEMS_AND_XP, false, true));
        assertTrue(ThaumcraftTweaks.earthShockHurts(EarthShockTargets.ONLY_LIVING, true, false));
        assertFalse(ThaumcraftTweaks.earthShockHurts(EarthShockTargets.ONLY_LIVING, false, false));
        assertFalse(ThaumcraftTweaks.earthShockHurts(EarthShockTargets.ONLY_LIVING, false, true));
        assertEquals(EarthShockTargets.ALL, EarthShockTargets.byId(0));
        assertEquals(EarthShockTargets.ONLY_LIVING, EarthShockTargets.byId(-1));
    }

    @Test void anOverrideLastsUntilItIsClosed() {
        try (var original = ThaumcraftTweaks.override(ThaumcraftTweaks.ORIGINAL)) {
            assertFalse(ThaumcraftTweaks.moreRandomizedLoot());
            try (var inner = ThaumcraftTweaks.override(ThaumcraftTweaks.DEFAULTS)) {
                assertTrue(ThaumcraftTweaks.filledToHand());
            }
            assertEquals(ThaumcraftTweaks.ORIGINAL, ThaumcraftTweaks.values());
        }
        assertEquals(ThaumcraftTweaks.DEFAULTS, ThaumcraftTweaks.values());
    }

    @Test void theScreenOffersEveryTweakWithItsDefault() {
        Properties missing = new Properties();
        var entries = ThaumcraftConfigStore.entries(ThaumcraftConfigStore.Category.TWEAKS);
        assertEquals(7, entries.size());
        for (var entry : entries) missing.setProperty(entry.key(), entry.value(new Properties()));
        ThaumcraftTweaks.load(missing);
        assertEquals(ThaumcraftTweaks.DEFAULTS, ThaumcraftTweaks.values());
    }
}
