package thaumcraft.common.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/**
 * The public configuration options of the original.
 *
 * <p>Oracle {@code thaumcraft/common/config/Config.java:102-200} declares the options and the
 * static block at {@code :830-895} gives their original defaults (CFR 0.152 decompilation of
 * {@code Thaumcraft-1.7.10-4.2.3.5.jar}).</p>
 */
class ConfigDefaultsTest {
    @Test void theWorldGenerationOptionsKeepTheirOriginalDefaults() {
        assertTrue(Config.genAura);
        assertTrue(Config.genStructure);
        assertTrue(Config.genCinnibar);
        assertTrue(Config.genAmber);
        assertTrue(Config.genInfusedStone);
        assertTrue(Config.genTrees);
        assertTrue(Config.genTaint);
        assertFalse(Config.regenAura);
        assertFalse(Config.regenStructure);
        assertFalse(Config.regenCinnibar);
        assertFalse(Config.regenAmber);
        assertFalse(Config.regenInfusedStone);
        assertFalse(Config.regenTrees);
        assertFalse(Config.regenTaint);
        assertEquals("DEFAULT", Config.regenKey);
    }

    @Test void theResearchOptionsKeepTheirOriginalDefaults() {
        // The original declared the seven research facts in its static block (oracle
        // Config.java:830-895). The public options of the native port carry the runtime
        // values that a configuration replaces, so the declared defaults are published by
        // Config.Defaults.
        assertEquals(0, Config.Defaults.RESEARCH_DIFFICULTY);
        assertEquals(100, Config.Defaults.ASPECT_TOTAL_CAP);
        assertEquals(0, Config.Defaults.CRESEARCH_DIFFICULTY);
        assertEquals(100, Config.Defaults.CASPECT_TOTAL_CAP);
        assertTrue(Config.Defaults.HARD_NODE);
        assertTrue(Config.Defaults.CHARD_NODE);
        assertFalse(Config.Defaults.WUSS);
        assertFalse(Config.Defaults.CWUSS);
        assertTrue(Config.Defaults.ALLOW_CHEAT_SHEET);
        assertTrue(Config.Defaults.CALLOW_CHEAT_SHEET);
        assertTrue(Config.Defaults.ALLOW_MIRRORS);
        assertTrue(Config.Defaults.CALLOW_MIRRORS);
        assertTrue(Config.Defaults.WARDED_STONE);
        assertTrue(Config.Defaults.CWARDED_STONE);
    }

    /**
     * The original replaces the public options with the values of its configuration file
     * (oracle Config.java:388-392 and :457-459), and the cheat sheet is the one option whose
     * configuration default differs from its declared default: the configuration file writes
     * {@code false} for it (oracle Config.java:456).
     */
    @Test void thePublicOptionsFollowTheAppliedConfiguration() {
        ResearchSettings saved = new ResearchSettings(Config.allowCheatSheet, Config.wardedStone,
                Config.allowMirrors, Config.hardNode, Config.wuss, Config.researchDifficulty,
                Config.aspectTotalCap);
        try {
            Config.apply(new ResearchSettings(false, true, false, true, false, 0, 100));
            assertFalse(Config.allowCheatSheet, "the original's configuration disables the cheat sheet");
            assertFalse(Config.CallowCheatSheet, "the cheat sheet copy follows the configuration");
            assertFalse(Config.allowMirrors, "the applied configuration carries the mirrors");
            assertFalse(Config.CallowMirrors, "the mirrors copy follows the configuration");
            assertTrue(Config.hardNode, "the applied configuration carries the hard nodes");
            assertEquals(0, Config.researchDifficulty, "the applied configuration carries the difficulty");
            assertEquals(100, Config.aspectTotalCap, "the applied configuration carries the aspect cap");
        } finally {
            Config.apply(saved);
        }
    }

    @Test void theSpawnOptionsKeepTheirOriginalDefaults() {
        assertTrue(Config.spawnAngryZombie);
        assertTrue(Config.spawnFireBat);
        assertTrue(Config.spawnTaintacle);
        assertTrue(Config.spawnWisp);
        // The taint settings are runtime values as well, so their declared default is
        // published by Config.Defaults (oracle Config.java:894).
        assertTrue(Config.Defaults.SPAWN_TAINT_SPORE);
        assertTrue(Config.spawnTaintSpore);
        assertTrue(Config.spawnPech);
        assertTrue(Config.spawnElder);
        assertTrue(Config.championMobs);
    }

    @Test void theRemainingOptionsKeepTheirOriginalDefaults() {
        assertEquals(192, Config.biomeTaintID);
        assertEquals(193, Config.biomeMagicalForestID);
        assertEquals(194, Config.biomeEerieID);
        assertEquals(195, Config.biomeEldritchID);
        assertEquals(2, Config.biomeTaintWeight);
        assertEquals(5, Config.biomeMagicalForestWeight);
        assertEquals(200, Config.Defaults.TAINT_SPREAD_RATE);
        assertEquals(200, Config.taintSpreadRate);
        assertTrue(Config.taintFromFlux);
        assertEquals(-42, Config.dimensionOuterId);
        assertEquals(2000, Config.shieldRecharge);
        assertEquals(80, Config.shieldWait);
        assertEquals(50, Config.shieldCost);
        assertFalse(Config.colorBlind);
        assertTrue(Config.shaders);
        assertTrue(Config.crooked);
        assertFalse(Config.showTags);
        assertFalse(Config.blueBiome);
        assertFalse(Config.dialBottom);
        assertEquals(10, Config.nodeRefresh);
        assertEquals(36, Config.nodeRarity);
        assertEquals(18, Config.specialNodeRarity);
        assertEquals(5000, Config.notificationDelay);
        assertEquals(15, Config.notificationMax);
        assertTrue(Config.Defaults.GLOWY_TAINT);
        assertTrue(Config.glowyTaint);
        assertEquals(5, Config.golemDelay);
        assertEquals(10000, Config.golemIgnoreDelay);
        assertEquals(16, Config.golemLinkQuality);
        assertTrue(Config.golemChestInteract);
        assertEquals(4.0f, Config.auraSize);
    }

    @Test void thePotionIdsKeepTheirOriginalValues() {
        assertEquals(18, Config.potionVisExhaustID);
        assertEquals(18, Config.potionInfVisExhaustID);
        assertEquals(18, Config.potionBlurredID);
        assertEquals(18, Config.potionThaumarhiaID);
        assertEquals(19, Config.potionTaintPoisonID);
        assertEquals(17, Config.potionUnHungerID);
        assertEquals(17, Config.potionSunScornedID);
        assertEquals(23, Config.potionWarpWardID);
        assertEquals(17, Config.potionDeathGazeID);
    }

    @Test void theCategoryNamesAndTheFoundFlagsKeepTheirOriginalValues() {
        assertEquals("Enchantments", Config.CATEGORY_ENCH);
        assertEquals("Entities", Config.CATEGORY_ENTITIES);
        assertEquals("Biomes", Config.CATEGORY_BIOMES);
        assertEquals("Research", Config.CATEGORY_RESEARCH);
        assertEquals("World_Generation", Config.CATEGORY_GEN);
        assertEquals("World_Regeneration", Config.CATEGORY_REGEN);
        assertEquals("Monster_Spawning", Config.CATEGORY_SPAWN);
        assertEquals("Runic_Shielding", Config.CATEGORY_RUNIC);
        assertFalse(Config.foundCopperIngot);
        assertFalse(Config.foundTinIngot);
        assertFalse(Config.foundSilverIngot);
        assertFalse(Config.foundLeadIngot);
        assertFalse(Config.foundCopperOre);
        assertFalse(Config.foundTinOre);
        assertFalse(Config.foundSilverOre);
        assertFalse(Config.foundLeadOre);
        assertNull(Config.enchHaste);
        assertNull(Config.enchRepair);
        assertTrue(Config.aspectOrder.isEmpty());
    }
}
