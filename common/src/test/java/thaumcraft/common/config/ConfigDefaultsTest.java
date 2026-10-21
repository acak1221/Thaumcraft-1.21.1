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
        assertEquals(0, Config.researchDifficulty);
        assertEquals(100, Config.aspectTotalCap);
        assertEquals(0, Config.CresearchDifficulty);
        assertEquals(100, Config.CaspectTotalCap);
        assertTrue(Config.hardNode);
        assertTrue(Config.ChardNode);
        assertFalse(Config.wuss);
        assertFalse(Config.Cwuss);
        assertTrue(Config.allowCheatSheet);
        assertTrue(Config.CallowCheatSheet);
        assertTrue(Config.allowMirrors);
        assertTrue(Config.CallowMirrors);
        assertTrue(Config.wardedStone);
        assertTrue(Config.CwardedStone);
    }

    @Test void theSpawnOptionsKeepTheirOriginalDefaults() {
        assertTrue(Config.spawnAngryZombie);
        assertTrue(Config.spawnFireBat);
        assertTrue(Config.spawnTaintacle);
        assertTrue(Config.spawnWisp);
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
