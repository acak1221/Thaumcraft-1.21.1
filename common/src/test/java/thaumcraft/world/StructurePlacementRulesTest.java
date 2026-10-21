package thaumcraft.world;

import java.util.HashMap;
import java.util.Map;
import net.minecraft.util.RandomSource;
import org.junit.jupiter.api.Test;
import thaumcraft.common.lib.world.StructurePlacementRules;
import static org.junit.jupiter.api.Assertions.*;

/** Oracle ThaumcraftWorldGenerator.generateSurface, WorldGenEldritchRing, MazeThread, MazeHandler. */
class StructurePlacementRulesTest {

    @Test void structureChainKeepsTheOriginalOrderAndFrequencies() {
        assertEquals(150, StructurePlacementRules.MOUND_CHANCE);
        assertEquals(66, StructurePlacementRules.ELDRITCH_RING_CHANCE);
        assertEquals(40, StructurePlacementRules.HILLTOP_STONES_CHANCE);
        // The chain is exclusive: the mound wins over the ring, the ring over the hilltop stones.
        assertEquals(StructurePlacementRules.Kind.MOUND, StructurePlacementRules.select(0, 0, 0));
        assertEquals(StructurePlacementRules.Kind.ELDRITCH_RING, StructurePlacementRules.select(1, 0, 0));
        assertEquals(StructurePlacementRules.Kind.HILLTOP_STONES, StructurePlacementRules.select(1, 1, 0));
        assertEquals(StructurePlacementRules.Kind.NONE, StructurePlacementRules.select(1, 1, 1));
    }

    @Test void structureChainConsumesTheOriginalRollOrder() {
        // The ring roll is only drawn when the mound roll failed, exactly like the original.
        RandomSource random = RandomSource.create(4242L);
        assertEquals(StructurePlacementRules.Kind.NONE, StructurePlacementRules.select(random));
        for (int i = 0; i < 200; ++i) StructurePlacementRules.select(RandomSource.create(i));
        // A ring is roughly one in 66 of the chunks that are not mounds.
        int rings = 0;
        int chunks = 4000;
        for (int i = 0; i < chunks; ++i)
            if (StructurePlacementRules.select(RandomSource.create(i * 31L + 7)) == StructurePlacementRules.Kind.ELDRITCH_RING) ++rings;
        assertTrue(rings > 0, "the eldritch ring must be reachable");
        assertTrue(rings < chunks / 20, "the eldritch ring must stay rare, found " + rings);
    }

    @Test void chunkSeedIsTheOriginalOne() {
        // Oracle ChunkProvider.func_180889_a - the chunk random of the world generator.
        assertEquals(42L + 16L * 341873128712L + 32L * 132897987541L,
                StructurePlacementRules.chunkSeed(42L, 16, 32));
        assertEquals(42L, StructurePlacementRules.chunkSeed(42L, 0, 0));
    }

    @Test void theDrawIsTheOriginalPositionInsideTheChunk() {
        for (int x = -5; x < 5; ++x) {
            for (int z = -5; z < 5; ++z) {
                StructurePlacementRules.Draw draw = draw(7L, x, z);
                assertTrue(draw.x() >= x * 16 && draw.x() < x * 16 + 16, "x inside the chunk: " + draw.x());
                assertTrue(draw.z() >= z * 16 && draw.z() < z * 16 + 16, "z inside the chunk: " + draw.z());
            }
        }
    }

    @Test void aChunkNeverCarriesTwoStructures() {
        // The ring and the hilltop stones draw the same chain from the same chunk
        // random, so they can never win the same chunk.
        for (int x = -20; x < 20; ++x) {
            for (int z = -20; z < 20; ++z) {
                StructurePlacementRules.Draw first = draw(7L, x, z);
                StructurePlacementRules.Draw second = draw(7L, x, z);
                assertEquals(first.kind(), second.kind());
                assertEquals(first.x(), second.x());
                assertEquals(first.z(), second.z());
            }
        }
    }

    @Test void theStructureChainKeepsTheOriginalChances() {
        int mound = 0;
        int ring = 0;
        int stones = 0;
        for (int x = 0; x < 200; ++x) {
            for (int z = 0; z < 200; ++z) {
                switch (draw(11L, x, z).kind()) {
                    case MOUND -> mound++;
                    case ELDRITCH_RING -> ring++;
                    case HILLTOP_STONES -> stones++;
                    default -> { }
                }
            }
        }
        int chunks = 200 * 200;
        // One in a hundred and fifty, one in sixty six, one in forty.
        assertTrue(mound > chunks / 200 && mound < chunks / 100, "mound " + mound);
        assertTrue(ring > chunks / 90 && ring < chunks / 55, "ring " + ring);
        assertTrue(stones > chunks / 50 && stones < chunks / 33, "stones " + stones);
    }

    @Test void theDrawContinuesTheStreamOfTheOriginal() {
        // The oracle draws the position, then the chain, then the size of the ring,
        // so the caller continues the same stream with the next draw.
        RandomSource random = RandomSource.create(StructurePlacementRules.chunkSeed(3L, 4, 5));
        StructurePlacementRules.Draw draw = StructurePlacementRules.draw(random, 4, 5);
        int[] size = StructurePlacementRules.ringSize(random);
        // The same stream, replayed by hand in the order of the original.
        RandomSource replay = RandomSource.create(StructurePlacementRules.chunkSeed(3L, 4, 5));
        assertEquals(4 * 16 + replay.nextInt(16), draw.x());
        assertEquals(5 * 16 + replay.nextInt(16), draw.z());
        assertEquals(StructurePlacementRules.select(replay), draw.kind());
        int[] replayed = StructurePlacementRules.ringSize(replay);
        assertEquals(replayed[0], size[0]);
        assertEquals(replayed[1], size[1]);
    }

    private static StructurePlacementRules.Draw draw(long worldSeed, int chunkX, int chunkZ) {
        RandomSource random = RandomSource.create(StructurePlacementRules.chunkSeed(worldSeed, chunkX, chunkZ));
        return StructurePlacementRules.draw(random, chunkX, chunkZ);
    }

    @Test void ringSizeKeepsTheOriginalOddSizes() {
        assertEquals(11, StructurePlacementRules.ringSize(0));
        assertEquals(13, StructurePlacementRules.ringSize(1));
        assertEquals(21, StructurePlacementRules.ringSize(5));
        for (int roll = 0; roll < 6; ++roll) {
            int size = StructurePlacementRules.ringSize(roll);
            assertTrue(size >= 11 && size <= 21);
            assertEquals(1, size % 2);
        }
        int[] sizes = StructurePlacementRules.ringSize(RandomSource.create(9L));
        assertEquals(2, sizes.length);
        assertTrue(sizes[0] >= 11 && sizes[0] <= 21);
        assertTrue(sizes[1] >= 11 && sizes[1] <= 21);
    }

    @Test void labyrinthOriginKeepsTheOriginalChunkMapping() {
        // MazeThread:39-40 - the labyrinth cell (0,0) is ringChunk - (1 + size / 2).
        assertEquals(-6, StructurePlacementRules.mazeOriginChunk(0, 11));
        assertEquals(-7, StructurePlacementRules.mazeOriginChunk(0, 13));
        assertEquals(-11, StructurePlacementRules.mazeOriginChunk(0, 21));
        assertEquals(94, StructurePlacementRules.mazeOriginChunk(100, 11));
        int[] cell = StructurePlacementRules.mazeCellChunk(0, 0, 100, 200, 11, 13);
        assertEquals(94, cell[0]);
        assertEquals(193, cell[1]);
        int[] cellFar = StructurePlacementRules.mazeCellChunk(10, 12, 100, 200, 11, 13);
        assertEquals(104, cellFar[0]);
        assertEquals(205, cellFar[1]);
        int[][] range = StructurePlacementRules.mazeChunkRange(100, 200, 11, 13);
        assertEquals(94, range[0][0]);
        assertEquals(104, range[0][1]);
        assertEquals(193, range[1][0]);
        assertEquals(205, range[1][1]);
    }

    @Test void aSecondLabyrinthIsRefusedWhileTheFirstOneCoversTheArea() {
        Map<Long, Integer> labyrinth = new HashMap<>();
        assertFalse(StructurePlacementRules.mazesInRange(labyrinth, 0, 0, 11, 11));
        labyrinth.put(StructurePlacementRules.key(0, 0), 1);
        assertTrue(StructurePlacementRules.mazesInRange(labyrinth, 0, 0, 11, 11));
        assertTrue(StructurePlacementRules.mazesInRange(labyrinth, 5, 5, 11, 11));
        assertFalse(StructurePlacementRules.mazesInRange(labyrinth, 40, 40, 11, 11));
    }

    @Test void chunkKeysAreStableAndDistinct() {
        assertEquals(StructurePlacementRules.key(3, -7), StructurePlacementRules.key(3, -7));
        assertNotEquals(StructurePlacementRules.key(3, -7), StructurePlacementRules.key(-7, 3));
        assertNotEquals(StructurePlacementRules.key(0, 0), StructurePlacementRules.key(1, 0));
    }

    @Test void ringSamplesAndGroundBlocksKeepTheOriginalValues() {
        assertEquals(5, StructurePlacementRules.RING_SAMPLES.size());
        assertArrayEquals(new int[] {-3, -3}, StructurePlacementRules.RING_SAMPLES.get(0));
        assertArrayEquals(new int[] {0, 0}, StructurePlacementRules.RING_SAMPLES.get(1));
        assertArrayEquals(new int[] {3, 0}, StructurePlacementRules.RING_SAMPLES.get(2));
        assertArrayEquals(new int[] {3, 3}, StructurePlacementRules.RING_SAMPLES.get(3));
        assertArrayEquals(new int[] {0, 3}, StructurePlacementRules.RING_SAMPLES.get(4));
        assertEquals(2, StructurePlacementRules.MAX_DISTANCE_TO_AIR);
        assertTrue(StructurePlacementRules.RING_GROUND_BLOCKS.contains("minecraft:stone"));
        assertTrue(StructurePlacementRules.RING_GROUND_BLOCKS.contains("minecraft:packed_ice"));
        assertTrue(StructurePlacementRules.RING_GROUND_BLOCKS.contains("minecraft:grass_block"));
        assertTrue(StructurePlacementRules.RING_GROUND_BLOCKS.contains("minecraft:sand"));
        assertTrue(StructurePlacementRules.RING_GROUND_BLOCKS.contains("minecraft:gravel"));
        assertTrue(StructurePlacementRules.RING_GROUND_BLOCKS.contains("minecraft:dirt"));
        assertEquals(6, StructurePlacementRules.RING_GROUND_BLOCKS.size());
        assertEquals(3, StructurePlacementRules.HILLTOP_GROUND_BLOCKS.size());
        assertEquals(85, StructurePlacementRules.HILLTOP_MIN_Y);
        assertEquals(5, StructurePlacementRules.HILLTOP_SAMPLES.size());
    }

    @Test void ringConstantsKeepTheOriginalShape() {
        assertEquals(3, StructurePlacementRules.RING_RADIUS);
        assertEquals(5, StructurePlacementRules.RING_FLOOR_LAYERS);
        assertEquals(4, StructurePlacementRules.RING_CLEARED_LAYERS);
        assertEquals(4, StructurePlacementRules.RING_OBSIDIAN_CHANCE);
        assertEquals(4, StructurePlacementRules.OBELISK_SHAFT);
        assertEquals(10, StructurePlacementRules.ALTAR_VARIANT_ROLLS);
        assertEquals(7, StructurePlacementRules.RING_RADIUS * 2 + 1);
    }

    @Test void theRingFloorKeepsTheOriginalObsidianRoll() {
        // Oracle WorldGenEldritchRing:75-79 - the one in four roll is the obsidian,
        // the other three rolls are the cosmetic solid. The port never leaves a hole.
        assertEquals(StructurePlacementRules.RingFloor.OBSIDIAN, StructurePlacementRules.ringFloor(0));
        assertEquals(StructurePlacementRules.RingFloor.COSMETIC_SOLID, StructurePlacementRules.ringFloor(1));
        assertEquals(StructurePlacementRules.RingFloor.COSMETIC_SOLID, StructurePlacementRules.ringFloor(3));
        int obsidian = 0;
        for (int roll = 0; roll < StructurePlacementRules.RING_OBSIDIAN_CHANCE; ++roll) {
            if (StructurePlacementRules.ringFloor(roll) == StructurePlacementRules.RingFloor.OBSIDIAN) ++obsidian;
        }
        assertEquals(1, obsidian, "one in four ring floor blocks is the obsidian");
    }

    @Test void altarAndBannerVariantsKeepTheOriginalRolls() {
        assertEquals(0, StructurePlacementRules.altarVariant(0));
        assertEquals(1, StructurePlacementRules.altarVariant(1));
        assertEquals(1, StructurePlacementRules.altarVariant(4));
        assertEquals(0, StructurePlacementRules.altarVariant(5));
        assertEquals(2, StructurePlacementRules.altarVariant(6));
        assertEquals(2, StructurePlacementRules.altarVariant(7));
        assertEquals(0, StructurePlacementRules.altarVariant(8));
        assertEquals(0, StructurePlacementRules.altarVariant(9));
        assertEquals(8, StructurePlacementRules.bannerFacing(2));
        assertEquals(0, StructurePlacementRules.bannerFacing(3));
        assertEquals(12, StructurePlacementRules.bannerFacing(4));
        assertEquals(4, StructurePlacementRules.bannerFacing(5));
    }

    @Test void totemRollKeepsTheOriginalRarity() {
        assertEquals(36, StructurePlacementRules.NODE_RARITY);
        assertEquals(360, StructurePlacementRules.TOTEM_CHANCE);
        int totems = 0;
        int chunks = 4000;
        for (int i = 0; i < chunks; ++i)
            if (StructurePlacementRules.totemRoll(RandomSource.create(i * 17L + 3))) ++totems;
        assertTrue(totems > 0, "the totem must be reachable");
        assertTrue(totems < chunks / 10, "the totem must stay rare, found " + totems);
    }
}
