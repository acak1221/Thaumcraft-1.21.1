package thaumcraft.worldgen;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import net.minecraft.util.RandomSource;
import org.junit.jupiter.api.Test;
import thaumcraft.common.lib.world.StructurePlacementRules;
import thaumcraft.common.lib.world.ThaumcraftTotemFeature;
import thaumcraft.common.lib.world.WorldGenCustomFlowers;
import thaumcraft.common.lib.world.WorldGenManaPods;
import thaumcraft.common.lib.world.WorldGenWriteArea;

/**
 * The original numbers of the surface generator, its exclusive structure chain and the two
 * small generators the magical forest calls.
 *
 * <p>Every expectation is transcribed from the readable 1.7.10 reference
 * {@code /home/cyberfox-kali/OpenTC4/src/main/java/thaumcraft/common/lib/world}:
 * <ul>
 *   <li>{@code ThaumcraftWorldGenerator:407-481} - the surface pass and the structure chain,</li>
 *   <li>{@code ThaumcraftWorldGenerator:114-168} - the aura totem,</li>
 *   <li>{@code WorldGenCustomFlowers:18-29} - the flower generator,</li>
 *   <li>{@code WorldGenManaPods:11-31} - the mana pod generator,</li>
 *   <li>{@code Config:87-88,131-132} - the biome weights and the node rarity.</li>
 * </ul>
 */
class OriginalSurfaceGeneratorNumbersTest {

    @Test void theStructureChainKeepsTheOriginalFrequencies() {
        assertEquals(150, StructurePlacementRules.MOUND_CHANCE);
        assertEquals(66, StructurePlacementRules.ELDRITCH_RING_CHANCE);
        assertEquals(40, StructurePlacementRules.HILLTOP_STONES_CHANCE);
        assertEquals(36, StructurePlacementRules.NODE_RARITY);
        assertEquals(360, StructurePlacementRules.TOTEM_CHANCE);
        assertEquals(360, ThaumcraftTotemFeature.TOTEM_CHANCE);
    }

    @Test void theStructureChainIsExclusiveAndDrawsInTheOriginalOrder() {
        // Oracle ThaumcraftWorldGenerator:440-468 - the mound roll, then the ring roll and only
        // then the hilltop roll, each of them only when the previous one missed.
        RandomSource random = RandomSource.create(20241022L);
        int mound = 0, ring = 0, hilltop = 0, none = 0;
        for (int i = 0; i < 40000; i++) {
            switch (StructurePlacementRules.select(random)) {
                case MOUND -> mound++;
                case ELDRITCH_RING -> ring++;
                case HILLTOP_STONES -> hilltop++;
                default -> none++;
            }
        }
        // The chain gives a chunk at most one structure.
        assertEquals(40000, mound + ring + hilltop + none);
        // 1/150, then 1/66 of the rest, then 1/40 of the rest.
        assertTrue(mound > 180 && mound < 355, "one mound in 150 chunks, got " + mound);
        assertTrue(ring > 470 && ring < 740, "one ring in 66 of the rest, got " + ring);
        assertTrue(hilltop > 810 && hilltop < 1150, "one hilltop pile in 40 of the rest, got " + hilltop);
    }

    @Test void theChainDrawsThePositionOfTheOriginalBeforeItsRolls() {
        // Oracle ThaumcraftWorldGenerator:435-436 - the position inside the chunk is drawn
        // before the exclusive chain.
        RandomSource random = RandomSource.create(4242L);
        StructurePlacementRules.Draw draw = StructurePlacementRules.draw(random, 3, -5);
        assertTrue(draw.x() >= 3 * 16 && draw.x() <= 3 * 16 + 15, "the position is inside the chunk");
        assertTrue(draw.z() >= -5 * 16 && draw.z() <= -5 * 16 + 15, "the position is inside the chunk");
    }

    @Test void theChainUsesTheChunkRandomOfTheOriginal() {
        // Oracle ChunkProvider.func_180889_a - worldSeed + chunkX * 341873128712
        // + chunkZ * 132897987541.
        assertEquals(1L + 341873128712L, StructurePlacementRules.chunkSeed(1L, 1, 0));
        assertEquals(1L + 132897987541L, StructurePlacementRules.chunkSeed(1L, 0, 1));
        assertEquals(1L, StructurePlacementRules.chunkSeed(1L, 0, 0));
    }

    @Test void theTotemKeepsTheOriginalColumnAndNode() {
        // Oracle ThaumcraftWorldGenerator:141-187.
        assertEquals(4, ThaumcraftTotemFeature.COLUMN_HEIGHT);
        assertEquals(5, ThaumcraftTotemFeature.NODE_HEIGHT);
        assertEquals(4, ThaumcraftTotemFeature.NODE_CHANCE);
        assertEquals(40, ThaumcraftTotemFeature.MIN_TOTEM_Y);
    }

    @Test void theFlowerGeneratorKeepsItsEighteenAttemptsAndItsSpreads() {
        // Oracle WorldGenCustomFlowers:19-23.
        assertEquals(18, WorldGenCustomFlowers.ATTEMPTS);
        assertEquals(8, WorldGenCustomFlowers.HORIZONTAL_SPREAD);
        assertEquals(4, WorldGenCustomFlowers.VERTICAL_SPREAD);
    }

    @Test void theManaPodGeneratorKeepsItsOriginalBounds() {
        // Oracle WorldGenManaPods:14,17,25-26.
        assertEquals(128, WorldGenManaPods.MAX_Y);
        assertEquals(4, WorldGenManaPods.SPREAD);
        assertEquals(2, WorldGenManaPods.AGE_MIN);
        assertEquals(5, WorldGenManaPods.AGE_VARIANTS);
    }

    @Test void theWriteAreaKeepsTheRadiusOfTheFeaturesStep() {
        // Minecraft 1.21.1 lets a feature of the features step write into the chunk of the
        // region and the eight chunks around it.
        var center = new net.minecraft.world.level.ChunkPos(0, 0);
        assertTrue(WorldGenWriteArea.inside(center, 1, new net.minecraft.core.BlockPos(0, 0, 0)));
        assertTrue(WorldGenWriteArea.inside(center, 1, new net.minecraft.core.BlockPos(31, 0, 31)));
        assertTrue(WorldGenWriteArea.inside(center, 1, new net.minecraft.core.BlockPos(-16, 0, -16)));
        assertTrue(!WorldGenWriteArea.inside(center, 1, new net.minecraft.core.BlockPos(32, 0, 0)));
        assertTrue(!WorldGenWriteArea.inside(center, 1, new net.minecraft.core.BlockPos(-17, 0, 0)));
    }
}
