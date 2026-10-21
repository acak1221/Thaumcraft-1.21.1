package thaumcraft.world;

import org.junit.jupiter.api.Test;
import thaumcraft.common.lib.world.OuterLandsDimensionRules;
import thaumcraft.common.lib.world.OuterLandsRules;
import static org.junit.jupiter.api.Assertions.*;

/** The 1.21.1 registration of the TC4 Outer Lands dimension and the original values it keeps. */
class OuterLandsDimensionRulesTest {

    @Test void theLevelStemStaysInTheVanillaLevelStemFolder() {
        // RegistryDataLoader.DIMENSION_REGISTRIES reads the level stems from the folder of
        // Registries.elementsDirPath(Registries.LEVEL_STEM), and that key is "dimension".
        assertEquals("dimension", OuterLandsDimensionRules.LEVEL_STEM_FOLDER);
        assertEquals("data/thaumcraft/dimension/outer_lands.json", OuterLandsDimensionRules.LEVEL_STEM_RESOURCE);
        assertEquals("data/thaumcraft/dimension_type/outer_lands.json", OuterLandsDimensionRules.DIMENSION_TYPE_RESOURCE);
        assertEquals("data/thaumcraft/worldgen/biome/eldritch_lands.json", OuterLandsDimensionRules.BIOME_RESOURCE);
        assertEquals("thaumcraft:outer_lands", OuterLandsDimensionRules.LEVEL_STEM_ID);
        assertEquals("thaumcraft:outer_lands", OuterLandsDimensionRules.DIMENSION_TYPE_ID);
        assertEquals("thaumcraft:outer_lands", OuterLandsDimensionRules.EFFECTS_ID);
    }

    @Test void theVoidChunksBecomeOneAirLayerOfTheFlatGenerator() {
        assertEquals("minecraft:flat", OuterLandsDimensionRules.GENERATOR_ID);
        assertEquals("minecraft:air", OuterLandsDimensionRules.LAYER_BLOCK_ID);
        assertEquals(1, OuterLandsDimensionRules.LAYER_HEIGHT);
        assertEquals("thaumcraft:eldritch_lands", OuterLandsDimensionRules.BIOME_ID);
    }

    @Test void theDimensionTypeKeepsTheOriginalValues() {
        var values = OuterLandsDimensionRules.dimensionTypeValues();
        assertFalse(values.hasSkyLight());
        assertEquals(0.0F, values.ambientLight());
        assertEquals(256, values.height());
        assertEquals(0, values.minY());
        assertEquals(256, values.logicalHeight());
        assertEquals(0L, values.fixedTime());
        assertEquals(1.0D, values.coordinateScale());
        assertFalse(values.bedWorks());
        assertFalse(values.respawnAnchorWorks());
        assertEquals(0, values.monsterSpawnBlockLightLimit());
        // The same values the oracle dimension provider and the shared rules carry.
        assertEquals(OuterLandsRules.HAS_SKY_LIGHT, values.hasSkyLight());
        assertEquals(OuterLandsRules.AMBIENT_LIGHT, values.ambientLight());
        assertEquals(OuterLandsRules.HEIGHT, values.height());
        assertEquals(OuterLandsRules.MIN_Y, values.minY());
        assertEquals(OuterLandsRules.LOGICAL_HEIGHT, values.logicalHeight());
    }

    @Test void theGateLandingStaysOneBlockBesideTheGate() {
        assertEquals(1, OuterLandsDimensionRules.gateLandingOffset(true));
        assertEquals(-1, OuterLandsDimensionRules.gateLandingOffset(false));
        assertEquals(50, OuterLandsDimensionRules.gateLandingHeight(50));
        assertEquals(OuterLandsRules.MAZE_FLOOR_Y + 3, OuterLandsDimensionRules.gateLandingHeight(OuterLandsRules.MAZE_FLOOR_Y + 3));
    }
}
