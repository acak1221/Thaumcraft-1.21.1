package thaumcraft.common.lib.world;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Random;
import net.minecraft.util.RandomSource;
import org.junit.jupiter.api.Test;

/**
 * The scattered-feature branch of the original surface generator and the {@code auraGen}
 * exclusivity it feeds into the totem and the wild node pass.
 *
 * <p>Oracle {@code thaumcraft/common/lib/world/ThaumcraftWorldGenerator.java}:</p>
 * <pre>
 * :413-418  MapGenScatteredFeature().func_151545_a(world, chunkX*16+8, surface, chunkZ*16+8)
 *           if (var7 != null &amp;&amp; !structureNode.containsKey(var7.hashCode())) {
 *               auraGen = true; ... createRandomNodeAt(world, var7.x, surface + 3, var7.z, ...) }
 * :419      auraGen = this.generateWildNodes(world, random, chunkX, chunkZ, auraGen, newGen);
 * :149      if (... &amp;&amp; !auraGen &amp;&amp; random.nextInt(Config.nodeRarity * 10) == 0)
 * </pre>
 *
 * <p>The expected chain kinds below were measured on the JVM with the oracle formula (the chunk
 * seed of {@code ChunkProvider.func_180889_a} and the {@code 150/66/40} rolls of
 * {@code generateSurface:429-453}); they are not a transcription of the port. No test of this
 * file constructs a block or a level: the rules under test are pure static functions.</p>
 */
class AuraGenRulesParityTest {

    @Test void theScatteredStructuresAreTheFiveBiomeSlotsOfTheOriginal() {
        // Oracle MapGenScatteredFeature:15 - Desert, DesertHills, Jungle, JungleHills, Swampland.
        // 1.21.1 splits those five slots over the desert pyramid (two), the jungle pyramid (two)
        // and the swamp hut (the witch hut of the original).
        assertEquals(3, AuraGenRules.SCATTERED_TEMPLES.size());
        assertEquals("minecraft:desert_pyramid", AuraGenRules.SCATTERED_TEMPLES.get(0).location().toString());
        assertEquals("minecraft:jungle_pyramid", AuraGenRules.SCATTERED_TEMPLES.get(1).location().toString());
        assertEquals("minecraft:swamp_hut", AuraGenRules.SCATTERED_TEMPLES.get(2).location().toString());
    }

    @Test void theScatteredFeatureKeepsTheSpacingSeparationAndSaltOfTheOriginal() {
        // Oracle MapGenScatteredFeature:18,19,41 - distance 32, separation 8, salt 14357617.
        assertEquals(32, AuraGenRules.SCATTERED_SPACING);
        assertEquals(8, AuraGenRules.SCATTERED_SEPARATION);
        assertEquals(14357617, AuraGenRules.SCATTERED_SALT);
    }

    @Test void theWildPassOverwritesTheScatteredFlag() {
        // Oracle :419 - the assignment replaces the flag the scattered branch set, and
        // generateWildNodes returns false while that flag is set (:190, `!auraGen`).
        assertFalse(AuraGenRules.auraGenAfterWildPass(true, true),
                "a scattered temple disables the wild pass, which then returns false");
        assertFalse(AuraGenRules.auraGenAfterWildPass(true, false));
        assertTrue(AuraGenRules.auraGenAfterWildPass(false, true));
        assertFalse(AuraGenRules.auraGenAfterWildPass(false, false));
    }

    @Test void aScatteredTempleDoesNotSuppressTheTotemButAWildNodeDoes() {
        // Oracle :149, :419, :434 - structureBuilt || (!scattered && wild).
        assertFalse(AuraGenRules.finalAuraGen(false, false, false));
        assertTrue(AuraGenRules.finalAuraGen(false, false, true));
        assertTrue(AuraGenRules.finalAuraGen(false, true, false));
        assertTrue(AuraGenRules.finalAuraGen(false, true, true));
        assertFalse(AuraGenRules.finalAuraGen(true, false, false));
        assertTrue(AuraGenRules.finalAuraGen(true, false, true));
        // The overwritten wild pass contributes nothing after a scattered node.
        assertFalse(AuraGenRules.finalAuraGen(true, true, false));
        assertTrue(AuraGenRules.finalAuraGen(true, true, true));
    }

    @Test void theChainKindIsTheOracleDrawOfTheChunkRandom() {
        // Oracle ChunkProvider.func_180889_a and ThaumcraftWorldGenerator:429-453.
        assertEquals(StructurePlacementRules.Kind.MOUND, AuraGenRules.chainKind(0L, -38, 13));
        assertEquals(StructurePlacementRules.Kind.ELDRITCH_RING, AuraGenRules.chainKind(0L, -40, -34));
        assertEquals(StructurePlacementRules.Kind.HILLTOP_STONES, AuraGenRules.chainKind(0L, -39, -5));
        assertEquals(StructurePlacementRules.Kind.NONE, AuraGenRules.chainKind(0L, -40, -40));
        assertEquals(StructurePlacementRules.Kind.MOUND, AuraGenRules.chainKind(123456789L, -39, -26));
        assertEquals(StructurePlacementRules.Kind.ELDRITCH_RING, AuraGenRules.chainKind(123456789L, -40, 24));
        assertEquals(StructurePlacementRules.Kind.HILLTOP_STONES, AuraGenRules.chainKind(123456789L, -40, -29));
    }

    @Test void theRegenerationRandomIsTheJavaRandomOfTheOriginal() {
        // Oracle ServerTickEventsFML:92-96 - the original seeds a java.util.Random and hands it
        // to worldGeneration; the native replay hands a RandomSource.create of the same seed.
        // The two generators must produce the same draws.
        for (long seed : new long[] {0L, 1L, -42L, 123456789L, Long.MIN_VALUE, Long.MAX_VALUE}) {
            Random oracle = new Random(seed);
            RandomSource nativeGenerator = RandomSource.create(seed);
            assertEquals(oracle.nextInt(12345), nativeGenerator.nextInt(12345), "seed " + seed);
            assertEquals(oracle.nextInt(16), nativeGenerator.nextInt(16), "seed " + seed);
            assertEquals(oracle.nextInt(36), nativeGenerator.nextInt(36), "seed " + seed);
            assertEquals(oracle.nextInt(4), nativeGenerator.nextInt(4), "seed " + seed);
        }
    }
}
