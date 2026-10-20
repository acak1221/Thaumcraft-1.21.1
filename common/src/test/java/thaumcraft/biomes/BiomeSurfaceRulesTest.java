package thaumcraft.biomes;

import static org.junit.jupiter.api.Assertions.*;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Random;
import net.minecraft.util.RandomSource;
import org.junit.jupiter.api.Test;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.common.lib.world.biomes.BiomeGenEerie;
import thaumcraft.common.lib.world.biomes.BiomeGenEldritch;
import thaumcraft.common.lib.world.biomes.BiomeGenMagicalForest;
import thaumcraft.common.lib.world.biomes.BiomeGenTaint;
import thaumcraft.common.lib.world.biomes.BiomeHandler;
import thaumcraft.common.lib.world.biomes.BiomeSurfaceRules;
import thaumcraft.common.lib.world.biomes.BiomeSurfaceRules.Generator;
import thaumcraft.common.lib.world.biomes.BiomeSurfaceRules.TreeKind;

/**
 * The original TC4 4.2.3.5 biome generators and the BiomeHandler dispatch.
 *
 * <p>Every expectation is transcribed from the oracle
 * {@code thaumcraft.common.lib.world.biomes} package and from
 * {@code thaumcraft.common.config.Config.registerBiomes}. The last test reads the four
 * native biome documents and proves that the committed native data still carries these
 * original numbers, so the rules class cannot drift away from the running game.
 */
class BiomeSurfaceRulesTest {
    private static JsonObject document(Generator generator) throws Exception {
        String path="data/thaumcraft/worldgen/biome/"+generator.id().substring("thaumcraft:".length())+".json";
        try(InputStream stream=BiomeSurfaceRulesTest.class.getClassLoader().getResourceAsStream(path)) {
            assertNotNull(stream,"missing native biome document "+path);
            return JsonParser.parseReader(new InputStreamReader(stream,StandardCharsets.UTF_8)).getAsJsonObject();
        }
    }

    @Test void everyOriginalGeneratorOwnsItsBiomeNameAndId() {
        assertEquals("Eerie",Generator.EERIE.biomeName());
        assertEquals("Eldritch",Generator.ELDRITCH.biomeName());
        assertEquals("Magical Forest",Generator.MAGICAL_FOREST.biomeName());
        assertEquals("Tainted Land",Generator.TAINT.biomeName());
        assertEquals(194,Generator.EERIE.originalId());
        assertEquals(195,Generator.ELDRITCH.originalId());
        assertEquals(193,Generator.MAGICAL_FOREST.originalId());
        assertEquals(192,Generator.TAINT.originalId());
        assertEquals("Eerie",BiomeGenEerie.NAME);
        assertEquals("Eldritch",BiomeGenEldritch.NAME);
        assertEquals("Magical Forest",BiomeGenMagicalForest.NAME);
        assertEquals("Tainted Land",BiomeGenTaint.NAME);
    }

    @Test void theDispatchResolvesEveryOriginalBiomeId() {
        assertEquals(Generator.EERIE,BiomeSurfaceRules.generator("thaumcraft:eerie"));
        assertEquals(Generator.ELDRITCH,BiomeSurfaceRules.generator("thaumcraft:eldritch_lands"));
        assertEquals(Generator.MAGICAL_FOREST,BiomeSurfaceRules.generator("thaumcraft:magical_forest"));
        assertEquals(Generator.TAINT,BiomeSurfaceRules.generator("thaumcraft:taint"));
        assertNull(BiomeSurfaceRules.generator("minecraft:plains"));
        assertEquals(Generator.EERIE,BiomeSurfaceRules.generator(194));
        assertEquals(Generator.ELDRITCH,BiomeSurfaceRules.generator(195));
        assertEquals(Generator.MAGICAL_FOREST,BiomeSurfaceRules.generator(193));
        assertEquals(Generator.TAINT,BiomeSurfaceRules.generator(192));
        assertNull(BiomeSurfaceRules.generator(0));
        assertEquals(Generator.EERIE,BiomeHandler.generator(BiomeGenEerie.KEY));
        assertEquals(Generator.ELDRITCH,BiomeHandler.generator(BiomeGenEldritch.KEY));
        assertEquals(Generator.MAGICAL_FOREST,BiomeHandler.generator(BiomeGenMagicalForest.KEY));
        assertEquals(Generator.TAINT,BiomeHandler.generator(BiomeGenTaint.KEY));
    }

    @Test void everyOriginalClimateValueIsPreserved() {
        assertEquals(0.5F,BiomeSurfaceRules.temperature(Generator.EERIE));
        assertEquals(0.5F,BiomeSurfaceRules.rainfall(Generator.EERIE));
        assertEquals(0.5F,BiomeSurfaceRules.temperature(Generator.ELDRITCH));
        assertEquals(0.5F,BiomeSurfaceRules.rainfall(Generator.ELDRITCH));
        assertEquals(0.7F,BiomeSurfaceRules.temperature(Generator.MAGICAL_FOREST));
        assertEquals(0.6F,BiomeSurfaceRules.rainfall(Generator.MAGICAL_FOREST));
        assertEquals(0.5F,BiomeSurfaceRules.temperature(Generator.TAINT));
        assertEquals(0.5F,BiomeSurfaceRules.rainfall(Generator.TAINT));
        for(Generator generator:Generator.values())assertFalse(BiomeSurfaceRules.highHumidity(generator),
            generator+" is not a high humidity biome");
        assertTrue(BiomeSurfaceRules.precipitation(Generator.EERIE));
        assertTrue(BiomeSurfaceRules.precipitation(Generator.MAGICAL_FOREST));
        assertTrue(BiomeSurfaceRules.precipitation(Generator.TAINT));
        assertFalse(BiomeSurfaceRules.precipitation(Generator.ELDRITCH),"the Eldritch biome disabled rain");
    }

    @Test void everyOriginalColourIsPreserved() {
        assertEquals(0x404840,BiomeSurfaceRules.baseColour(Generator.EERIE,false));
        assertEquals(0x404840,BiomeSurfaceRules.grassColour(Generator.EERIE,false));
        assertEquals(4215616,BiomeSurfaceRules.foliageColour(Generator.EERIE,false));
        assertEquals(0x222299,BiomeSurfaceRules.skyColour(Generator.EERIE));
        assertEquals(3035999,BiomeSurfaceRules.waterColour(Generator.EERIE));
        assertEquals(6747307,BiomeSurfaceRules.baseColour(Generator.MAGICAL_FOREST,false));
        assertEquals(5635969,BiomeSurfaceRules.grassColour(Generator.MAGICAL_FOREST,false));
        assertEquals(6750149,BiomeSurfaceRules.foliageColour(Generator.MAGICAL_FOREST,false));
        assertEquals(0x66AACC,BiomeSurfaceRules.baseColour(Generator.MAGICAL_FOREST,true));
        assertEquals(0x66AACC,BiomeSurfaceRules.grassColour(Generator.MAGICAL_FOREST,true));
        assertEquals(0x77CCEE,BiomeSurfaceRules.foliageColour(Generator.MAGICAL_FOREST,true));
        assertEquals(30702,BiomeSurfaceRules.waterColour(Generator.MAGICAL_FOREST));
        assertEquals(7160201,BiomeSurfaceRules.baseColour(Generator.TAINT,false));
        assertEquals(7160201,BiomeSurfaceRules.grassColour(Generator.TAINT,false));
        assertEquals(8154503,BiomeSurfaceRules.foliageColour(Generator.TAINT,false));
        assertEquals(8144127,BiomeSurfaceRules.skyColour(Generator.TAINT));
        assertEquals(0xCC1188,BiomeSurfaceRules.waterColour(Generator.TAINT));
        assertEquals(0x000000,BiomeSurfaceRules.grassColour(Generator.ELDRITCH,false));
        assertEquals(0x000000,BiomeSurfaceRules.foliageColour(Generator.ELDRITCH,false));
        assertEquals(0x000000,BiomeSurfaceRules.skyColour(Generator.ELDRITCH));
        assertEquals(0x3F76E4,BiomeSurfaceRules.waterColour(Generator.ELDRITCH));
    }

    @Test void theOriginalDefaultSkyColourFormulaIsKept() {
        // 1.7.10 BiomeGenBase.getSkyColorByTemp truncated, the native document rounds.
        assertEquals(0x79A6FF,BiomeSurfaceRules.defaultSkyColour(0.7F));
        assertEquals(0x7BA4FF,BiomeSurfaceRules.defaultSkyColour(0.5F));
        assertEquals(8038143,BiomeSurfaceRules.skyColour(Generator.MAGICAL_FOREST));
    }

    @Test void theEerieKeepsItsFogAndItsVanillaDeadBushes() {
        assertEquals(0x222299,BiomeGenEerie.skyColour(),"1.7.10 used the sky colour as the distance fog colour");
        assertEquals(4,BiomeGenEerie.deadBushesPerChunk());
        assertFalse(BiomeGenEerie.hasDeadTrees(),"the original eerie biome had no dead tree generator");
        assertEquals(2,BiomeGenEerie.treesPerChunk());
        assertEquals(1,BiomeGenEerie.flowersPerChunk());
        assertEquals(2,BiomeGenEerie.grassPerChunk());
        var random=RandomSource.create(4242);
        for(int i=0;i<64;i++) {
            TreeKind kind=BiomeGenEerie.treeKind(random);
            assertTrue(kind==TreeKind.OAK||kind==TreeKind.BIG_OAK,"the eerie biome inherited the vanilla tree draw");
        }
    }

    @Test void theMagicalForestKeepsItsOriginalVegetation() {
        assertEquals(10,BiomeSurfaceRules.flowersPerChunk(Generator.MAGICAL_FOREST));
        assertEquals(12,BiomeSurfaceRules.grassPerChunk(Generator.MAGICAL_FOREST));
        assertEquals(6,BiomeSurfaceRules.waterlilyPerChunk(Generator.MAGICAL_FOREST));
        assertEquals(6,BiomeSurfaceRules.mushroomsPerChunk(Generator.MAGICAL_FOREST));
        assertEquals(2,BiomeSurfaceRules.treesPerChunk(Generator.MAGICAL_FOREST));
        assertEquals(List.of("minecraft:poppy","minecraft:blue_orchid","minecraft:allium","minecraft:azure_bluet",
            "minecraft:red_tulip","minecraft:orange_tulip","minecraft:white_tulip","minecraft:pink_tulip",
            "minecraft:oxeye_daisy"),BiomeGenMagicalForest.flowers());
        assertEquals(10,BiomeSurfaceRules.MAGICAL_FOREST_FLOWER_WEIGHT);
        assertEquals(10,BiomeGenMagicalForest.manaPods());
        assertEquals(64,BiomeGenMagicalForest.manaPodY());
        assertEquals(8,BiomeGenMagicalForest.vishrooms());
        assertEquals(50,BiomeGenMagicalForest.vishroomMinY());
        assertTrue(BiomeGenMagicalForest.vishroomNeedsWood());
        var random=RandomSource.create(1337);
        int silverwood=0,greatwood=0,magic=0;
        for(int i=0;i<20000;i++) {
            TreeKind kind=BiomeGenMagicalForest.treeKind(random);
            if(kind==TreeKind.SILVERWOOD)silverwood++;
            else if(kind==TreeKind.GREATWOOD)greatwood++;
            else magic++;
        }
        assertTrue(silverwood>1200&&silverwood<1700,"one silverwood tree in fourteen, got "+silverwood);
        assertTrue(greatwood>1700&&greatwood<2050,"one greatwood tree in ten of the remaining draws, got "+greatwood);
        assertTrue(magic>16000,"otherwise the big magic tree, got "+magic);
        var grass=RandomSource.create(99);
        int doubled=0;
        for(int i=0;i<4000;i++)if(BiomeGenMagicalForest.doubleGrass(grass))doubled++;
        assertTrue(doubled>800&&doubled<1200,"double grass one time in four, got "+doubled);
    }

    @Test void theTaintedBiomeKeepsItsNegativeCountersAndQuirks() {
        assertEquals(-999,BiomeSurfaceRules.flowersPerChunk(Generator.TAINT));
        assertEquals(-999,BiomeSurfaceRules.reedsPerChunk(Generator.TAINT));
        assertEquals(0,BiomeGenTaint.effectiveFlowers());
        assertEquals(0,BiomeGenTaint.effectiveReeds());
        assertEquals(2,BiomeSurfaceRules.treesPerChunk(Generator.TAINT));
        assertEquals(2,BiomeSurfaceRules.grassPerChunk(Generator.TAINT));
        assertEquals(10,BiomeGenTaint.surfaceFibres());
        assertEquals(8,BiomeGenTaint.columnFibres());
        var random=RandomSource.create(2024);
        int bigMagic=0;
        for(int i=0;i<8000;i++)if(BiomeGenTaint.treeKind(random)==TreeKind.BIG_MAGIC)bigMagic++;
        assertTrue(bigMagic>800&&bigMagic<1200,"one big magic tree in eight, got "+bigMagic);
    }

    @Test void theEldritchBiomeIsDirtOnBlackWithoutDecoration() {
        assertEquals("minecraft:dirt",BiomeGenEldritch.topBlock());
        assertEquals("minecraft:dirt",BiomeGenEldritch.fillerBlock());
        assertEquals(0x000000,BiomeGenEldritch.skyColour());
        assertFalse(BiomeGenEldritch.precipitation());
        assertFalse(BiomeGenEldritch.decorates(),"the original Eldritch biome decorated nothing");
        assertEquals(0,BiomeSurfaceRules.treesPerChunk(Generator.ELDRITCH));
        assertEquals(195,BiomeGenEldritch.ORIGINAL_BIOME_ID);
        assertEquals(List.of("thaumcraft:inhabitedzombie","thaumcraft:eldritch_guardian"),
            BiomeGenEldritch.spawnRows().stream().map(row->row.entity()).toList());
        assertEquals(1,BiomeGenEldritch.spawnRows().get(0).weight());
        assertEquals(1,BiomeGenEldritch.spawnRows().get(0).min());
        assertEquals(1,BiomeGenEldritch.spawnRows().get(0).max());
        assertEquals(List.of("thaumcraft:inhabited_zombie","thaumcraft:eldritch_guardian"),
            BiomeGenEldritch.encodableSpawnRows().stream().map(row->row.entity()).toList());
    }

    @Test void theBiomeDictionaryDispatchMatchesTheOriginalRegistration() {
        assertEquals(List.of("magical","spooky"),BiomeHandler.dictionaryTypes(Generator.EERIE));
        assertEquals(List.of("magical","spooky","end"),BiomeHandler.dictionaryTypes(Generator.ELDRITCH));
        assertEquals(List.of("magical","forest"),BiomeHandler.dictionaryTypes(Generator.MAGICAL_FOREST));
        assertEquals(List.of("magical","wasteland"),BiomeHandler.dictionaryTypes(Generator.TAINT));
        assertEquals(31,BiomeHandler.types().size());
        assertEquals(90,BiomeHandler.biomeAura(Generator.EERIE));
        assertEquals(86,BiomeHandler.biomeAura(Generator.ELDRITCH));
        assertEquals(110,BiomeHandler.biomeAura(Generator.MAGICAL_FOREST));
        assertEquals(90,BiomeHandler.biomeAura(Generator.TAINT));
        assertEquals(100,BiomeHandler.biomeAura(List.of()),"the original fallback of an untyped biome");
        assertEquals(1.0F,BiomeHandler.greatwoodChance(Generator.EERIE));
        assertEquals(1.0F,BiomeHandler.greatwoodChance(Generator.ELDRITCH));
        assertEquals(1.0F,BiomeHandler.greatwoodChance(Generator.MAGICAL_FOREST));
        assertEquals(1.0F,BiomeHandler.greatwoodChance(Generator.TAINT));
        assertEquals(0.0F,BiomeHandler.greatwoodChance(List.of("spooky")));
        assertNull(BiomeHandler.randomBiomeTag(List.of("magical"),0),"the magical type carried no aspect");
        assertEquals(Aspect.SOUL,BiomeHandler.randomBiomeTag(List.of("magical","spooky"),1));
        assertEquals(Aspect.EARTH,BiomeHandler.randomBiomeTag(List.of("magical","forest"),1));
        assertEquals(Aspect.ENTROPY,BiomeHandler.randomBiomeTag(List.of("magical","wasteland"),1));
        assertEquals(Aspect.VOID,BiomeHandler.randomBiomeTag(List.of("magical","spooky","end"),2));
        assertNull(BiomeHandler.randomBiomeTag(List.of(),0));
        var random=new Random(7);
        assertNull(BiomeHandler.randomBiomeTag(List.of("magical"),random));
        assertEquals(Aspect.SOUL,BiomeHandler.randomBiomeTag(List.of("magical","spooky"),random));
    }

    @Test void everyNativeBiomeDocumentCarriesTheOriginalValues() throws Exception {
        for(Generator generator:Generator.values()) {
            JsonObject document=document(generator);
            assertEquals(BiomeSurfaceRules.temperature(generator),document.get("temperature").getAsFloat(),generator+" temperature");
            assertEquals(BiomeSurfaceRules.rainfall(generator),document.get("downfall").getAsFloat(),generator+" downfall");
            assertEquals(BiomeSurfaceRules.precipitation(generator),document.get("has_precipitation").getAsBoolean(),generator+" precipitation");
            JsonObject effects=document.getAsJsonObject("effects");
            assertEquals(BiomeSurfaceRules.skyColour(generator),effects.get("sky_color").getAsInt(),generator+" sky colour");
            assertEquals(BiomeSurfaceRules.fogColour(generator),effects.get("fog_color").getAsInt(),generator+" fog colour");
            assertEquals(BiomeSurfaceRules.waterColour(generator),effects.get("water_color").getAsInt(),generator+" water colour");
            assertEquals(BiomeSurfaceRules.waterFogColour(generator),effects.get("water_fog_color").getAsInt(),generator+" water fog colour");
            assertEquals(BiomeSurfaceRules.grassColour(generator,false),effects.get("grass_color").getAsInt(),generator+" grass colour");
            assertEquals(BiomeSurfaceRules.foliageColour(generator,false),effects.get("foliage_color").getAsInt(),generator+" foliage colour");
        }
    }

    @Test void theEldritchBiomeDocumentIsTheOnlyOneWithoutTheThaumcraftDecorator() throws Exception {
        for(Generator generator:Generator.values()) {
            JsonObject document=document(generator);
            boolean decorated=document.getAsJsonArray("features").toString().contains("thaumcraft:tc4_biome_decoration");
            assertEquals(generator!=Generator.ELDRITCH,decorated,generator+" TC4 decoration");
            JsonObject spawners=document.getAsJsonObject("spawners");
            int monsters=spawners.getAsJsonArray("monster").size();
            if(generator==Generator.ELDRITCH) {
                assertEquals(2,monsters,"both original cleared monster rows are encoded");
                assertEquals("thaumcraft:inhabited_zombie",spawners.getAsJsonArray("monster").get(0).getAsJsonObject().get("type").getAsString());
                assertEquals("thaumcraft:eldritch_guardian",spawners.getAsJsonArray("monster").get(1).getAsJsonObject().get("type").getAsString());
                assertEquals(0,spawners.getAsJsonArray("creature").size());
                assertEquals(0,spawners.getAsJsonArray("water_creature").size());
                assertEquals(0,spawners.getAsJsonArray("ambient").size());
            } else assertTrue(monsters>0,generator+" keeps its original monster rows");
        }
    }
}
