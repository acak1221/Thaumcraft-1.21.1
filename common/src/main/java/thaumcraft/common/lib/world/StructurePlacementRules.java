package thaumcraft.common.lib.world;

import java.util.List;
import java.util.Set;
import net.minecraft.util.RandomSource;

/**
 * Pure placement rules of the original TC4 surface structures and of the
 * labyrinth seeding they trigger.
 *
 * Oracle sources:
 * <ul>
 *   <li>{@code thaumcraft/common/lib/world/ThaumcraftWorldGenerator.java:403-457} - the structure
 *       selection chain (mound / eldritch ring / hilltop stones) and the totem roll.</li>
 *   <li>{@code thaumcraft/common/lib/world/WorldGenEldritchRing.java:28-146} - the ring samples and
 *       its layout.</li>
 *   <li>{@code thaumcraft/common/lib/world/WorldGenHilltopStones.java:29-131} - the hilltop stones
 *       samples and their minimum height.</li>
 *   <li>{@code thaumcraft/common/lib/world/dim/MazeThread.java:12-70} - the labyrinth seeding and
 *       the chunk mapping of a labyrinth cell.</li>
 *   <li>{@code thaumcraft/common/lib/world/dim/MazeHandler.java:142-150} - the "a maze already
 *       covers this area" test.</li>
 *   <li>{@code thaumcraft/common/config/Config.java:449-456,835-873} - node rarity defaults.</li>
 * </ul>
 */
public final class StructurePlacementRules {
    private StructurePlacementRules() {}

    /** The oracle draws the mound roll first: 1 in 150 chunks. */
    public static final int MOUND_CHANCE = 150;
    /** Then the eldritch ring: 1 in 66 of the remaining chunks. */
    public static final int ELDRITCH_RING_CHANCE = 66;
    /** Then the hilltop stones: 1 in 40 of the remaining chunks. */
    public static final int HILLTOP_STONES_CHANCE = 40;

    /** Config.nodeRarity default (36) and the totem uses ten times that (oracle:149). */
    public static final int NODE_RARITY = 36;
    public static final int TOTEM_CHANCE = NODE_RARITY * 10;

    /** The eldritch ring radius in blocks: the ring spans -3..+3 around its centre. */
    public static final int RING_RADIUS = 3;
    /** The ring floor is five blocks thick below the ring base (oracle WorldGenEldritchRing:72-83). */
    public static final int RING_FLOOR_LAYERS = 5;
    /** The ring clears this many layers above its base. */
    public static final int RING_CLEARED_LAYERS = 4;
    /** One in four ring floor blocks is left empty (oracle WorldGenEldritchRing:75-79). */
    public static final int RING_FLOOR_AIR_CHANCE = 4;
    /** The obelisk above the altar is one base block plus four shaft blocks. */
    public static final int OBELISK_SHAFT = 4;
    /** The altar spawner variant: 4 of 10 rolls are the type 0 altar, 2 of 10 the type 1 altar. */
    public static final int ALTAR_VARIANT_ROLLS = 10;

    /** The smallest labyrinth size, the largest is 11 + 5*2. */
    public static final int MAZE_MIN_SIZE = 11;
    /** The number of distinct labyrinth sizes (11, 13, 15, 17, 19, 21). */
    public static final int MAZE_SIZE_ROLLS = 6;

    /** The hilltop stones never generate below this height (oracle WorldGenHilltopStones:36-38). */
    public static final int HILLTOP_MIN_Y = 85;
    /** The hilltop stones radius in blocks. */
    public static final int HILLTOP_RADIUS = 2;

    /** The 1.7 ground blocks an eldritch ring may stand on (oracle WorldGenEldritchRing:36). */
    public static final Set<String> RING_GROUND_BLOCKS = Set.of(
            "minecraft:stone", "minecraft:gravel", "minecraft:terracotta",
            "minecraft:grass_block", "minecraft:sand", "minecraft:dirt");
    /** The 1.7 ground blocks a hilltop stone pile may stand on (oracle WorldGenHilltopStones:32). */
    public static final Set<String> HILLTOP_GROUND_BLOCKS = Set.of(
            "minecraft:stone", "minecraft:grass_block", "minecraft:dirt");
    /** The snow blocks that may cover a valid ground block (1.7 field_150431_aC/field_150329_H). */
    public static final Set<String> SNOW_COVER_BLOCKS = Set.of("minecraft:snow", "minecraft:snow_block");

    /** The samples the eldritch ring validates before it is built (oracle WorldGenEldritchRing:65). */
    public static final List<int[]> RING_SAMPLES = List.of(
            new int[] {-3, -3}, new int[] {0, 0}, new int[] {3, 0}, new int[] {3, 3}, new int[] {0, 3});
    /** The samples the hilltop stones validate (oracle WorldGenHilltopStones:64). */
    public static final List<int[]> HILLTOP_SAMPLES = List.of(
            new int[] {-2, -2}, new int[] {0, 0}, new int[] {2, 0}, new int[] {2, 2}, new int[] {0, 2});

    /** The maximum distance from the surface to the first air block for a valid spawn. */
    public static final int MAX_DISTANCE_TO_AIR = 2;

    /** The structures the original surface generator may roll for a chunk. */
    public enum Kind {
        MOUND,
        ELDRITCH_RING,
        HILLTOP_STONES,
        NONE
    }

    /**
     * Oracle ThaumcraftWorldGenerator.generateSurface:429-453. The draws happen in the original
     * order and only when the previous branch was not taken.
     */
    public static Kind select(RandomSource random) {
        if (random.nextInt(MOUND_CHANCE) == 0) return Kind.MOUND;
        if (random.nextInt(ELDRITCH_RING_CHANCE) == 0) return Kind.ELDRITCH_RING;
        if (random.nextInt(HILLTOP_STONES_CHANCE) == 0) return Kind.HILLTOP_STONES;
        return Kind.NONE;
    }

    /** The same decision for already drawn rolls; 0 means "hit". */
    public static Kind select(int moundRoll, int ringRoll, int hilltopRoll) {
        if (moundRoll == 0) return Kind.MOUND;
        if (ringRoll == 0) return Kind.ELDRITCH_RING;
        if (hilltopRoll == 0) return Kind.HILLTOP_STONES;
        return Kind.NONE;
    }

    /** The oracle ring size: 11 + nextInt(6) * 2, so always an odd size between 11 and 21. */
    public static int ringSize(int roll) {
        return MAZE_MIN_SIZE + roll * 2;
    }

    /** The oracle draws the ring width first and the ring height second. */
    public static int[] ringSize(RandomSource random) {
        int width = ringSize(random.nextInt(MAZE_SIZE_ROLLS));
        int height = ringSize(random.nextInt(MAZE_SIZE_ROLLS));
        return new int[] {width, height};
    }

    /**
     * Oracle MazeThread:39-40. The labyrinth cell (0,0) of a maze seeded at the ring chunk
     * lands in the chunk {@code ringChunk - (1 + size / 2)}.
     */
    public static int mazeOriginChunk(int ringChunk, int size) {
        return ringChunk - (1 + size / 2);
    }

    /**
     * Oracle MazeThread:47-53. A labyrinth grid cell maps to the Outer Lands chunk that
     * carries it; the grid x axis is the chunk x axis and the grid y axis the chunk z axis.
     */
    public static int[] mazeCellChunk(int gridX, int gridY, int ringChunkX, int ringChunkZ, int width, int height) {
        int column = mazeOriginChunk(ringChunkX, width);
        int row = mazeOriginChunk(ringChunkZ, height);
        return new int[] {gridX + column, gridY + row};
    }

    /** The chunk range a seeded labyrinth covers. */
    public static int[][] mazeChunkRange(int ringChunkX, int ringChunkZ, int width, int height) {
        int minX = mazeOriginChunk(ringChunkX, width);
        int minZ = mazeOriginChunk(ringChunkZ, height);
        return new int[][] {{minX, minX + width - 1}, {minZ, minZ + height - 1}};
    }

    /**
     * Oracle MazeHandler.mazesInRange:142-150. A new ring may not be built while another
     * labyrinth already covers the requested area.
     */
    public static boolean mazesInRange(java.util.Map<Long, ?> labyrinth, int chunkX, int chunkZ, int width, int height) {
        for (int x = -width; x <= width; ++x) {
            for (int z = -height; z <= height; ++z) {
                if (labyrinth.containsKey(key(chunkX + x, chunkZ + z))) return true;
            }
        }
        return false;
    }

    /** The stable key of a labyrinth cell; the chunk pair is packed into one long. */
    public static long key(int chunkX, int chunkZ) {
        return ((long) chunkX << 32) | (chunkZ & 0xFFFFFFFFL);
    }

    /**
     * The oracle totem roll (ThaumcraftWorldGenerator:149): 1 in 360 chunks, only when the
     * chunk did not already produce an aura node.
     */
    public static boolean totemRoll(RandomSource random) {
        return random.nextInt(TOTEM_CHANCE) == 0;
    }

    /** The altar spawner variant of a ring: 0 none, 1 the type 0 altar, 2 the type 1 altar. */
    public static int altarVariant(int roll) {
        if (roll >= 1 && roll <= 4) return 1;
        if (roll == 6 || roll == 7) return 2;
        return 0;
    }

    /** The banner facing of an altar banner (oracle WorldGenEldritchRing:103-120). */
    public static int bannerFacing(int forgeDirectionOrdinal) {
        return switch (forgeDirectionOrdinal) {
            case 2 -> 8;  // north
            case 3 -> 0;  // south
            case 4 -> 12; // west
            case 5 -> 4;  // east
            default -> 0;
        };
    }
}
