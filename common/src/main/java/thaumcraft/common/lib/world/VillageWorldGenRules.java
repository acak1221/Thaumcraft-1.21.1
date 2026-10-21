package thaumcraft.common.lib.world;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Pure decision rules for the TC4 4.2.3.5 village components and the two
 * remaining surface worldgen extras.
 *
 * <p>Every number in this class is copied from the 1.7.10 oracle:
 * {@code thaumcraft/common/lib/world/ComponentBankerHome.java},
 * {@code ComponentWizardTower.java}, {@code VillageBankerManager.java},
 * {@code VillageWizardManager.java}, {@code WorldGenHilltopStones.java},
 * {@code WorldGenEldritchRing.java} and the surface structure pass of
 * {@code ThaumcraftWorldGenerator.java}.
 *
 * <p>The class touches no world, level, registry or entity: the village
 * weights, the block composition of both components, the structure chances,
 * the placement maths and the trade tables are all plain data and plain
 * arithmetic, so they are unit testable without a game runtime. The world
 * facing glue (block states, villagers, loot) lives in the feature classes.
 */
public final class VillageWorldGenRules {
    private VillageWorldGenRules() {}

    // ------------------------------------------------------------------ village pieces

    /**
     * Oracle VillageBankerManager:59 -
     * {@code new StructureVillagePieces.PieceWeight(ComponentBankerHome.class, 25, ...)}.
     */
    public static final int BANKER_HOME_WEIGHT = 25;
    /**
     * Oracle VillageWizardManager:56 -
     * {@code new StructureVillagePieces.PieceWeight(ComponentWizardTower.class, 15, ...)}.
     */
    public static final int WIZARD_TOWER_WEIGHT = 15;
    /**
     * The rest of the 1.7.10 village piece weight pool, i.e. the vanilla
     * pieces that 1.21.1 still generates itself. TC4 only added its two
     * entries to the vanilla list, so this is the weight the TC4 pieces share
     * the village house slots with.
     */
    public static final int VANILLA_HOUSE_WEIGHT = 60;
    /** The whole weight pool a village draws its house types from. */
    public static final int TOTAL_HOUSE_WEIGHT = BANKER_HOME_WEIGHT + WIZARD_TOWER_WEIGHT + VANILLA_HOUSE_WEIGHT;
    /** 1.7.10 village size: {@code nextInt(12) + 1} house slots. */
    public static final int VILLAGE_HOUSE_SLOTS = 12;
    /** The 1.7.10 village region size in chunks. */
    public static final int VILLAGE_CELL_CHUNKS = 32;
    /** Oracle ConfigEntities:83 - {@code entBankerId}. */
    public static final int BANKER_PROFESSION_ID = 191;
    /** Oracle ConfigEntities:82 - {@code entWizardId}. */
    public static final int WIZARD_PROFESSION_ID = 190;

    /** The house types a 1.7.10 village house slot can take. */
    public enum HouseType { BANKER_HOME, WIZARD_TOWER, VANILLA_HOUSE }

    /**
     * The chunk offsets of the village house slots, in the order the village
     * hands them out. The centre slot comes first, then the eight neighbours
     * and finally the four slots at distance two; thirteen slots cover the
     * twelve house slots a village can have.
     */
    private static final int[][] HOUSE_CHUNK_OFFSETS = {
            {0, 0}, {1, 0}, {-1, 0}, {0, 1}, {0, -1},
            {1, 1}, {-1, 1}, {1, -1}, {-1, -1},
            {2, 0}, {-2, 0}, {0, 2}, {0, -2}
    };

    /**
     * The house slots of a village reach two chunks around its anchor.
     *
     * <p>The anchor of a village therefore keeps that distance from the border of
     * the village cell. Without it a slot that falls outside the cell would never
     * be built: the chunk outside the cell asks the plan of its own cell, that
     * plan has another anchor and the slot of the first village is unreachable.
     * The original 1.7.10 village never loses a house, because the village piece
     * system hands the pieces of one village to the chunks of that village.
     */
    public static final int HOUSE_SLOT_RADIUS = 2;

    /**
     * Oracle VillageBankerManager:59 and VillageWizardManager:56 both build
     * their piece weight with {@code MathHelper.getIntWithMinMax(random, 0 + i, 1 + i)}.
     * That helper is {@code min + random.nextInt(max - min)}, so the range is
     * one wide and the draw is always {@code i}: the piece is available in a
     * village of exactly the size the village was created with.
     */
    public static int pieceWeightMinSize(int villageSize) {
        return villageSize;
    }

    /** The village house count, 1..12, from the original {@code nextInt(12) + 1}. */
    public static int villageSize(int roll) {
        return Math.floorMod(roll, VILLAGE_HOUSE_SLOTS) + 1;
    }

    /** The house type a weight roll selects. */
    public static HouseType houseType(int roll) {
        int r = Math.floorMod(roll, TOTAL_HOUSE_WEIGHT);
        if (r < BANKER_HOME_WEIGHT) return HouseType.BANKER_HOME;
        if (r < BANKER_HOME_WEIGHT + WIZARD_TOWER_WEIGHT) return HouseType.WIZARD_TOWER;
        return HouseType.VANILLA_HOUSE;
    }

    /** The house types of one village, drawn in order from the weight pool. */
    public static List<HouseType> villageHouses(int villageSize, Random random) {
        List<HouseType> houses = new ArrayList<>(villageSize);
        for (int i = 0; i < villageSize; i++) houses.add(houseType(random.nextInt(TOTAL_HOUSE_WEIGHT)));
        return houses;
    }

    /** How many houses of one type the village holds. */
    public static int componentCount(List<HouseType> houses, HouseType type) {
        int count = 0;
        for (HouseType house : houses) if (house == type) count++;
        return count;
    }

    /** The village that owns a chunk cell. */
    public record VillagePlan(int anchorChunkX, int anchorChunkZ, int size, List<HouseType> houses) {
        public VillagePlan { houses = List.copyOf(houses); }
    }

    /** The village cell a chunk belongs to. */
    public static int villageCell(int chunkCoord) {
        return Math.floorDiv(chunkCoord, VILLAGE_CELL_CHUNKS);
    }

    /**
     * The deterministic village of the cell that owns the chunk. The anchor
     * chunk, the house count and the house types only depend on the world seed
     * and the cell, never on the order the chunks are generated in.
     */
    public static VillagePlan villagePlan(long worldSeed, int chunkX, int chunkZ) {
        int cellX = villageCell(chunkX);
        int cellZ = villageCell(chunkZ);
        long seed = villageSeed(worldSeed, cellX, cellZ);
        int anchorX = cellX * VILLAGE_CELL_CHUNKS + anchorOffset(mix64(seed));
        int anchorZ = cellZ * VILLAGE_CELL_CHUNKS + anchorOffset(mix64(seed ^ 0x9E3779B97F4A7C15L));
        Random random = new Random(mix64(seed ^ 0xD1B54A32D192ED03L));
        int size = villageSize(random.nextInt());
        return new VillagePlan(anchorX, anchorZ, size, villageHouses(size, random));
    }

    /**
     * The chunk of the village cell the anchor of the village sits in.
     *
     * <p>The offset is clamped so that every house slot of the village stays inside
     * its own village cell. A slot outside the cell would belong to the plan of the
     * neighbouring cell, that plan has another anchor, and the house of the first
     * village would never be built.
     */
    public static int anchorOffset(long mixed) {
        int offset = (int) Long.remainderUnsigned(mixed, VILLAGE_CELL_CHUNKS);
        return Math.min(Math.max(offset, HOUSE_SLOT_RADIUS), VILLAGE_CELL_CHUNKS - 1 - HOUSE_SLOT_RADIUS);
    }

    /** The house slot that owns the chunk, or -1 when the chunk holds no house. */
    public static int houseIndex(VillagePlan plan, int chunkX, int chunkZ) {
        int dx = chunkX - plan.anchorChunkX();
        int dz = chunkZ - plan.anchorChunkZ();
        int limit = Math.min(plan.houses().size(), HOUSE_CHUNK_OFFSETS.length);
        for (int i = 0; i < limit; i++) {
            if (HOUSE_CHUNK_OFFSETS[i][0] == dx && HOUSE_CHUNK_OFFSETS[i][1] == dz) return i;
        }
        return -1;
    }

    /** The chunk offset of a house slot on the x axis. */
    public static int houseChunkOffsetX(int house) {
        return HOUSE_CHUNK_OFFSETS[house][0];
    }

    /** The chunk offset of a house slot on the z axis. */
    public static int houseChunkOffsetZ(int house) {
        return HOUSE_CHUNK_OFFSETS[house][1];
    }

    /**
     * The rotation of the house slot: the component faces the village centre,
     * so the door side always looks at the other houses.
     */
    public static int houseRotation(VillagePlan plan, int houseIndex) {
        if (houseIndex < 0 || houseIndex >= HOUSE_CHUNK_OFFSETS.length) return 0;
        int dx = HOUSE_CHUNK_OFFSETS[houseIndex][0];
        int dz = HOUSE_CHUNK_OFFSETS[houseIndex][1];
        if (dx > 0) return 3;
        if (dx < 0) return 1;
        if (dz > 0) return 0;
        if (dz < 0) return 2;
        return 0;
    }

    // ------------------------------------------------------------------ rotation

    /** The x of a local component coordinate after the rotation. */
    public static int rotateX(int x, int z, int width, int depth, int rotation) {
        return switch (rotation & 3) {
            case 1 -> depth - 1 - z;
            case 2 -> width - 1 - x;
            case 3 -> z;
            default -> x;
        };
    }

    /** The z of a local component coordinate after the rotation. */
    public static int rotateZ(int x, int z, int width, int depth, int rotation) {
        return switch (rotation & 3) {
            case 1 -> x;
            case 2 -> depth - 1 - z;
            case 3 -> width - 1 - x;
            default -> z;
        };
    }

    /** The width of the rotated footprint. */
    public static int rotatedWidth(int width, int depth, int rotation) {
        return (rotation & 1) == 1 ? depth : width;
    }

    /** The depth of the rotated footprint. */
    public static int rotatedDepth(int width, int depth, int rotation) {
        return (rotation & 1) == 1 ? width : depth;
    }

    /** The compass direction the front of a rotated component looks at. */
    public static String frontDirection(int rotation) {
        return rotateDirection("north", rotation);
    }

    /**
     * A compass direction turned clockwise by the rotation, the same turn the
     * component coordinates take.
     */
    public static String rotateDirection(String direction, int rotation) {
        return switch (direction) {
            case "north" -> switch (rotation & 3) {
                case 1 -> "east";
                case 2 -> "south";
                case 3 -> "west";
                default -> "north";
            };
            case "east" -> switch (rotation & 3) {
                case 1 -> "south";
                case 2 -> "west";
                case 3 -> "north";
                default -> "east";
            };
            case "south" -> switch (rotation & 3) {
                case 1 -> "west";
                case 2 -> "north";
                case 3 -> "east";
                default -> "south";
            };
            case "west" -> switch (rotation & 3) {
                case 1 -> "north";
                case 2 -> "east";
                case 3 -> "south";
                default -> "west";
            };
            default -> direction;
        };
    }

    // ------------------------------------------------------------------ materials

    /**
     * The block materials of the two village components and of the two surface
     * structures. The id is the 1.21.1 block; the variant is the original
     * 1.7.10 metadata for the block families that became a block state.
     */
    public enum Material {
        AIR("minecraft:air", -1),
        COBBLESTONE("minecraft:cobblestone", -1),
        DIRT("minecraft:dirt", -1),
        LOG("minecraft:oak_log", -1),
        PLANKS("minecraft:oak_planks", -1),
        IRON_BARS("minecraft:iron_bars", -1),
        COBBLESTONE_WALL("minecraft:cobblestone_wall", -1),
        STONE_PRESSURE_PLATE("minecraft:stone_pressure_plate", -1),
        DOOR_BOTTOM("minecraft:oak_door", -1),
        DOOR_TOP("minecraft:oak_door", -1),
        STONE_STAIRS("minecraft:stone_stairs", -1),
        GLASS_PANE("minecraft:glass_pane", -1),
        LADDER("minecraft:ladder", -1),
        TRAPDOOR("minecraft:oak_trapdoor", -1),
        GLOWSTONE("minecraft:glowstone", -1),
        CHEST("minecraft:chest", -1),
        /** Oracle ConfigBlocks.blockCosmeticSolid metadata 0, the obsidian totem. */
        COSMETIC_SOLID_0("thaumcraft:cosmetic_solid_tough", 0),
        /** Oracle ConfigBlocks.blockCosmeticSolid metadata 1, the obsidian tile. */
        COSMETIC_SOLID_1("thaumcraft:cosmetic_solid_tough", 1),
        /**
         * Oracle ConfigBlocks.blockCosmeticSolid metadata 8, the host of the node of the
         * totem of the original (ThaumcraftWorldGenerator:175,180).
         */
        COSMETIC_SOLID_NODE("thaumcraft:cosmetic_solid_tough", 8),
        /** Oracle ConfigBlocks.blockEldritch metadata 0, the eldritch altar. */
        ELDRITCH_ALTAR("thaumcraft:eldritch", 0),
        /** Oracle ConfigBlocks.blockEldritch metadata 1, the obelisk base. */
        ELDRITCH_OBELISK("thaumcraft:eldritch", 1),
        /** Oracle ConfigBlocks.blockEldritch metadata 2, the obelisk middle. */
        ELDRITCH_OBELISK_MIDDLE("thaumcraft:eldritch", 2),
        /** Oracle ConfigBlocks.blockEldritch metadata 3, the capstone. */
        ELDRITCH_CAPSTONE("thaumcraft:eldritch", 3),
        OBSIDIAN("minecraft:obsidian", -1),
        MOB_SPAWNER("minecraft:spawner", -1),
        VINE("minecraft:vine", -1);

        private final String id;
        private final int variant;

        Material(String id, int variant) { this.id = id; this.variant = variant; }

        /** The 1.21.1 block id of the material. */
        public String id() { return id; }

        /** The original 1.7.10 metadata, or -1 when the block has no variant. */
        public int variant() { return variant; }
    }

    /** One block of a component, in the component's own local coordinates. */
    public record Placement(int x, int y, int z, Material material) {}

    // ------------------------------------------------------------------ banker home

    /** Oracle ComponentBankerHome:56 - the piece bounding box is 4 wide, 6 high and 5 deep. */
    public static final int BANKER_HOME_WIDTH = 4;
    public static final int BANKER_HOME_HEIGHT = 6;
    public static final int BANKER_HOME_DEPTH = 5;
    /** Oracle ComponentBankerHome:40 - the table sits in one of three wall slots. */
    public static final int BANKER_HOME_TABLE_SLOTS = 3;
    /** Oracle ComponentBankerHome:39 - the house is either the low or the high roof. */
    public static final int BANKER_HOME_TABLE_MIN = 0;
    /** The villager of the banker home, oracle ComponentBankerHome:112. */
    public static final int BANKER_HOME_VILLAGER_X = 1;
    public static final int BANKER_HOME_VILLAGER_Y = 1;
    public static final int BANKER_HOME_VILLAGER_Z = 2;

    /**
     * The whole banker home, in the order the original places the blocks.
     * Oracle ComponentBankerHome:68-101.
     */
    public static List<Placement> bankerHome(boolean tallHouse, int tablePosition) {
        List<Placement> out = new ArrayList<>(160);
        // func_151549_a(1,1,1, 3,5,4, AIR, AIR, false) - the hollow interior.
        for (int x = 1; x <= 3; x++) for (int y = 1; y <= 5; y++) for (int z = 1; z <= 4; z++) {
            out.add(new Placement(x, y, z, Material.AIR));
        }
        // func_151549_a(0,0,0, 3,0,4, COBBLESTONE, COBBLESTONE, false) - the cobble floor.
        for (int x = 0; x <= 3; x++) for (int z = 0; z <= 4; z++) {
            out.add(new Placement(x, 0, z, Material.COBBLESTONE));
        }
        // func_151549_a(1,0,1, 2,0,3, DIRT, DIRT, false) - the dirt floor inside.
        for (int x = 1; x <= 2; x++) for (int z = 1; z <= 3; z++) {
            out.add(new Placement(x, 0, z, Material.DIRT));
        }
        // func_151549_a(1,4,1, 2,4,3, LOG) or func_151549_a(1,5,1, 2,5,3, LOG) - the roof panel.
        int roof = tallHouse ? 4 : 5;
        for (int x = 1; x <= 2; x++) for (int z = 1; z <= 3; z++) {
            out.add(new Placement(x, roof, z, Material.LOG));
        }
        // The roof edge, always on the fourth layer.
        out.add(new Placement(1, 4, 0, Material.LOG));
        out.add(new Placement(2, 4, 0, Material.LOG));
        out.add(new Placement(1, 4, 4, Material.LOG));
        out.add(new Placement(2, 4, 4, Material.LOG));
        for (int y = 1; y <= 3; y++) {
            out.add(new Placement(0, 4, y, Material.LOG));
            out.add(new Placement(3, 4, y, Material.LOG));
        }
        // The four corner posts.
        for (int y = 1; y <= 3; y++) {
            out.add(new Placement(0, y, 0, Material.LOG));
            out.add(new Placement(3, y, 0, Material.LOG));
            out.add(new Placement(0, y, 4, Material.LOG));
            out.add(new Placement(3, y, 4, Material.LOG));
        }
        // The plank walls between the corner posts.
        for (int y = 1; y <= 3; y++) for (int z = 1; z <= 3; z++) {
            out.add(new Placement(0, y, z, Material.PLANKS));
            out.add(new Placement(3, y, z, Material.PLANKS));
        }
        for (int y = 1; y <= 3; y++) for (int x = 1; x <= 2; x++) {
            out.add(new Placement(x, y, 0, Material.PLANKS));
            out.add(new Placement(x, y, 4, Material.PLANKS));
        }
        // The two windows.
        out.add(new Placement(0, 2, 2, Material.IRON_BARS));
        out.add(new Placement(3, 2, 2, Material.IRON_BARS));
        // The table: a cobble wall with a stone pressure plate on top of it.
        if (tablePosition > 0) {
            out.add(new Placement(tablePosition, 1, 3, Material.COBBLESTONE_WALL));
            out.add(new Placement(tablePosition, 2, 3, Material.STONE_PRESSURE_PLATE));
        }
        // The doorway: the planks are cleared and the door goes in.
        out.add(new Placement(1, 1, 0, Material.AIR));
        out.add(new Placement(1, 2, 0, Material.AIR));
        out.add(new Placement(1, 1, 0, Material.DOOR_BOTTOM));
        out.add(new Placement(1, 2, 0, Material.DOOR_TOP));
        return out;
    }

    // ------------------------------------------------------------------ wizard tower

    /** Oracle ComponentWizardTower:47 - the piece bounding box is 6 wide, 12 high and 6 deep. */
    public static final int WIZARD_TOWER_WIDTH = 6;
    public static final int WIZARD_TOWER_HEIGHT = 12;
    public static final int WIZARD_TOWER_DEPTH = 6;
    /**
     * Oracle ComponentWizardTower:106 - the wizard spawns at x 7, which is
     * outside the six wide piece, so the original never spawns it.
     */
    public static final int WIZARD_TOWER_VILLAGER_X = 7;
    public static final int WIZARD_TOWER_VILLAGER_Y = 1;
    public static final int WIZARD_TOWER_VILLAGER_Z = 1;
    /** Oracle ComponentWizardTower:92 - the chest holds four to nine items. */
    public static final int WIZARD_TOWER_CHEST_MIN = 4;
    public static final int WIZARD_TOWER_CHEST_MAX = 9;

    /**
     * The whole wizard tower, in the order the original places the blocks.
     * Oracle ComponentWizardTower:59-97.
     */
    public static List<Placement> wizardTower() {
        List<Placement> out = new ArrayList<>(320);
        // func_151549_a(2,1,2, 4,11,4, AIR, AIR, false) - the hollow interior.
        for (int x = 2; x <= 4; x++) for (int y = 1; y <= 11; y++) for (int z = 2; z <= 4; z++) {
            out.add(new Placement(x, y, z, Material.AIR));
        }
        // The three plank floors.
        for (int x = 2; x <= 4; x++) for (int z = 2; z <= 4; z++) {
            out.add(new Placement(x, 0, z, Material.PLANKS));
            out.add(new Placement(x, 5, z, Material.PLANKS));
            out.add(new Placement(x, 10, z, Material.PLANKS));
        }
        // The four cobble pillars.
        for (int y = 0; y <= 11; y++) {
            for (int z = 2; z <= 4; z++) {
                out.add(new Placement(1, y, z, Material.COBBLESTONE));
                out.add(new Placement(5, y, z, Material.COBBLESTONE));
            }
            for (int x = 2; x <= 4; x++) {
                out.add(new Placement(x, y, 1, Material.COBBLESTONE));
                out.add(new Placement(x, y, 5, Material.COBBLESTONE));
            }
        }
        // The cobble corners on the three floors.
        for (int y : new int[]{0, 5, 10}) {
            out.add(new Placement(1, y, 1, Material.COBBLESTONE));
            out.add(new Placement(1, y, 5, Material.COBBLESTONE));
            out.add(new Placement(5, y, 1, Material.COBBLESTONE));
            out.add(new Placement(5, y, 5, Material.COBBLESTONE));
        }
        // The six windows.
        out.add(new Placement(3, 7, 1, Material.GLASS_PANE));
        out.add(new Placement(3, 8, 1, Material.GLASS_PANE));
        out.add(new Placement(3, 7, 5, Material.GLASS_PANE));
        out.add(new Placement(3, 8, 5, Material.GLASS_PANE));
        out.add(new Placement(3, 2, 5, Material.GLASS_PANE));
        out.add(new Placement(3, 3, 5, Material.GLASS_PANE));
        // The ladder to the roof and its trapdoor.
        for (int y = 1; y <= 9; y++) out.add(new Placement(4, y, 3, Material.LADDER));
        out.add(new Placement(4, 10, 3, Material.TRAPDOOR));
        // The glowstone on the middle floor and the chest above it.
        out.add(new Placement(3, 5, 3, Material.GLOWSTONE));
        out.add(new Placement(2, 6, 2, Material.CHEST));
        // The doorway: the cobble is cleared and the door goes in.
        out.add(new Placement(3, 1, 1, Material.AIR));
        out.add(new Placement(3, 2, 1, Material.AIR));
        out.add(new Placement(3, 1, 1, Material.DOOR_BOTTOM));
        out.add(new Placement(3, 2, 1, Material.DOOR_TOP));
        return out;
    }

    // ------------------------------------------------------------------ hilltop stones

    /** Oracle ThaumcraftWorldGenerator:450 - one chunk in forty grows hilltop stones. */
    public static final int HILLTOP_STONE_CHANCE = 40;
    /** The seven by seven footprint of the stone cluster. */
    public static final int HILLTOP_STONE_RADIUS = 3;
    /** Oracle WorldGenHilltopStones:78 - four layers below the surface. */
    public static final int HILLTOP_STONE_LAYERS = 4;
    /** Oracle WorldGenHilltopStones:37 - below this height the ground is refused. */
    public static final int HILLTOP_STONE_MIN_Y = 85;
    /** Oracle WorldGenHilltopStones:105-115 - one in three vine per side. */
    public static final int HILLTOP_STONE_VINE_CHANCE = 3;
    /** Oracle WorldGenHilltopStones:124 - a vine grows four blocks further down. */
    public static final int HILLTOP_STONE_VINE_LENGTH = 4;
    /**
     * The five ground samples of oracle WorldGenHilltopStones:64, in the order
     * the original takes them.
     */
    public static final int[][] HILLTOP_SAMPLE_OFFSETS = {{-2, -2}, {0, 0}, {2, 0}, {2, 2}, {0, 2}};
    /** The ground blocks the stones may grow on, oracle WorldGenHilltopStones:32. */
    public static final String[] HILLTOP_VALID_GROUND = {"minecraft:stone", "minecraft:grass_block", "minecraft:dirt"};
    /** The blocks the original replaces with the ground block, oracle WorldGenHilltopStones:81. */
    public static final String[] HILLTOP_REPLACED_BLOCKS = {
            "minecraft:snow", "minecraft:short_grass", "minecraft:grass", "minecraft:fern",
            "minecraft:dead_bush", "minecraft:dandelion", "minecraft:poppy"
    };

    /** The four corners of the footprint the original skips. */
    public static boolean isHilltopCorner(int dx, int dz) {
        return Math.abs(dx) == HILLTOP_STONE_RADIUS && Math.abs(dz) == HILLTOP_STONE_RADIUS;
    }

    /**
     * The pillars of oracle WorldGenHilltopStones:100 - the border of the
     * footprint at an odd distance from the centre, corners excluded.
     */
    public static boolean isHilltopPillar(int dx, int dz) {
        if (isHilltopCorner(dx, dz)) return false;
        return (Math.abs(dx) == HILLTOP_STONE_RADIUS && isOdd(dz))
                || (Math.abs(dz) == HILLTOP_STONE_RADIUS && isOdd(dx));
    }

    /** The original uses the absolute value of the remainder, so -1 counts as odd. */
    public static boolean isOdd(int value) {
        return Math.abs(value % 2) == 1;
    }

    // ------------------------------------------------------------------ eldritch ring

    /** Oracle ThaumcraftWorldGenerator:435 - one chunk in sixty six grows an eldritch ring. */
    public static final int ELDRITCH_RING_CHANCE = 66;
    /** The seven by seven footprint of the ring. */
    public static final int ELDRITCH_RING_RADIUS = 3;
    /** Oracle WorldGenEldritchRing:72 - nine layers, four below and four above. */
    public static final int ELDRITCH_RING_LAYERS = 9;
    /** Oracle ThaumcraftWorldGenerator:438 - the maze size is eleven to twenty one. */
    public static final int ELDRITCH_RING_MIN_SIZE = 11;
    /** Oracle ThaumcraftWorldGenerator:438 - {@code 11 + random.nextInt(6) * 2}. */
    public static final int ELDRITCH_RING_SIZE_ROLLS = 6;
    /** Oracle ThaumcraftWorldGenerator:438 - the size step of two chunks. */
    public static final int ELDRITCH_RING_SIZE_STEP = 2;
    /**
     * The five ground samples of oracle WorldGenEldritchRing:65, in the order
     * the original takes them.
     */
    public static final int[][] ELDRITCH_SAMPLE_OFFSETS = {{-3, -3}, {0, 0}, {3, 0}, {3, 3}, {0, 3}};
    /** The ground blocks the ring may grow on, oracle WorldGenEldritchRing:36. */
    public static final String[] ELDRITCH_VALID_GROUND = {
            "minecraft:stone", "minecraft:sand", "minecraft:packed_ice",
            "minecraft:grass_block", "minecraft:gravel", "minecraft:dirt"
    };
    /** The four banner devices of the altar, oracle WorldGenEldritchRing:97-100. */
    public static final int[][] ELDRITCH_BANNER_OFFSETS = {{0, 3}, {0, -3}, {3, 0}, {-3, 0}};
    /** The original banner metadata of the four devices, oracle WorldGenEldritchRing:103-120. */
    public static final int[] ELDRITCH_BANNER_FACINGS = {8, 0, 12, 4};

    /** The maze size of the ring, eleven to twenty one chunks. */
    public static int eldritchRingSize(int roll) {
        return ELDRITCH_RING_MIN_SIZE
                + Math.floorMod(roll, ELDRITCH_RING_SIZE_ROLLS) * ELDRITCH_RING_SIZE_STEP;
    }

    /** The corners of the ring footprint the original skips. */
    public static boolean isEldritchCorner(int dx, int dz) {
        return Math.abs(dx) == ELDRITCH_RING_RADIUS && Math.abs(dz) == ELDRITCH_RING_RADIUS;
    }

    /**
     * The ring stones of oracle WorldGenEldritchRing:139 - the border of the
     * footprint at an odd distance from the centre, the diagonal excluded.
     */
    public static boolean isEldritchRingPosition(int dx, int dz) {
        if (isEldritchCorner(dx, dz)) return false;
        boolean border = (Math.abs(dx) == ELDRITCH_RING_RADIUS && isOdd(dz))
                || (Math.abs(dz) == ELDRITCH_RING_RADIUS && isOdd(dx));
        return border && Math.abs(dx) != Math.abs(dz);
    }

    /**
     * Oracle WorldGenEldritchRing:91-96 - the altar roll one to four arms the
     * altar with its four banner devices.
     */
    public static boolean eldritchArmsAltar(int roll) {
        return roll >= 1 && roll <= 4;
    }

    /** Oracle WorldGenEldritchRing:125-128 - the roll six or seven arms the altar. */
    public static boolean eldritchAltarSpawnerTypeOne(int roll) {
        return roll == 6 || roll == 7;
    }

    // ------------------------------------------------------------------ surface structures

    /*
     * The exclusive structure chain of the original, oracle
     * ThaumcraftWorldGenerator:429-453, lives in StructurePlacementRules: the mound
     * is drawn first, then the eldritch ring, then the hilltop stones, and the chain
     * only ever gives one structure to a chunk. The chunk random of the original that
     * feeds the chain is StructurePlacementRules.chunkSeed.
     */

    // ------------------------------------------------------------------ trades

    /** A trade of the banker or the wizard, oracle VillageBankerManager:42-55. */
    public record TradeSpec(String payItem, int payMin, int payMax, List<String> receiveItems, int receiveMin, int receiveMax) {
        public TradeSpec { receiveItems = List.copyOf(receiveItems); }
    }

    /** The tower chest content, oracle ComponentWizardTower:35. */
    public record ChestContent(String item, int min, int max, int weight) {}

    /**
     * The aspect shards of the native tree, in the original metadata order.
     *
     * <p>Oracle VillageWizardManager:46 - the shards of the original are the items of the
     * metadata zero to five of {@code ConfigItems.itemShard}. The native ids of the shards
     * carry the name of the element, not the name of the aspect.
     */
    public static final List<String> ASPECT_SHARDS = List.of(
            "thaumcraft:air_shard", "thaumcraft:fire_shard", "thaumcraft:water_shard",
            "thaumcraft:earth_shard", "thaumcraft:order_shard", "thaumcraft:entropy_shard");

    /** The bauble blanks of the native tree, in the original metadata order. */
    public static final List<String> BAUBLE_BLANKS = List.of(
            "thaumcraft:discount_ring_aer", "thaumcraft:discount_ring_ignis", "thaumcraft:discount_ring_aqua",
            "thaumcraft:discount_ring_terra", "thaumcraft:discount_ring_ordo", "thaumcraft:discount_ring_perditio");

    /**
     * The banker trades, oracle VillageBankerManager:42-55. The original pays
     * with resource metadata 18, the gold coin, and the 1.7.10 item metadata
     * became a native item id.
     */
    public static List<TradeSpec> bankerTrades() {
        List<TradeSpec> trades = new ArrayList<>(12);
        trades.add(new TradeSpec("thaumcraft:gold_coin", 20, 22, List.of("minecraft:emerald"), 1, 1));
        trades.add(new TradeSpec("thaumcraft:gold_coin", 2, 3, List.of("minecraft:arrow"), 1, 1));
        trades.add(new TradeSpec("thaumcraft:gold_coin", 6, 8, List.of("minecraft:white_wool"), 1, 1));
        trades.add(new TradeSpec("thaumcraft:gold_coin", 3, 4, List.of("minecraft:paper"), 1, 1));
        // Oracle VillageBankerManager:47 - the book costs seven plus nextInt three coins,
        // so seven to nine coins.
        trades.add(new TradeSpec("thaumcraft:gold_coin", 7, 9, List.of("minecraft:book"), 1, 1));
        trades.add(new TradeSpec("thaumcraft:gold_coin", 16, 20, List.of("minecraft:experience_bottle"), 1, 1));
        trades.add(new TradeSpec("thaumcraft:gold_coin", 9, 12, List.of("minecraft:glowstone"), 1, 1));
        trades.add(new TradeSpec("thaumcraft:gold_coin", 2, 3, List.of("minecraft:coal"), 1, 1));
        trades.add(new TradeSpec("thaumcraft:gold_coin", 22, 24, List.of("minecraft:diamond"), 1, 1));
        trades.add(new TradeSpec("thaumcraft:gold_coin", 6, 8, List.of("minecraft:iron_ingot"), 1, 1));
        trades.add(new TradeSpec("thaumcraft:gold_coin", 10, 12, List.of("thaumcraft:thaumium_ingot"), 1, 1));
        trades.add(new TradeSpec("thaumcraft:gold_coin", 25, 32, List.of("minecraft:saddle"), 1, 1));
        return trades;
    }

    /**
     * The wizard trades, oracle VillageWizardManager:36-52.
     *
     * <p>The third and the fifth trade of the original pay with the resource
     * metadata three, the quicksilver, and with the resource metadata six, the
     * amber; the port keeps the two resources apart instead of paying with the
     * gold coin of the first trade.
     */
    public static List<TradeSpec> wizardTrades() {
        List<TradeSpec> trades = new ArrayList<>(15);
        trades.add(new TradeSpec("thaumcraft:gold_coin", 20, 22, List.of("minecraft:emerald"), 1, 1));
        trades.add(new TradeSpec("minecraft:emerald", 1, 1, List.of("thaumcraft:knowledge_fragment"), 1, 1));
        trades.add(new TradeSpec("thaumcraft:quick_silver", 4, 6, List.of("minecraft:emerald"), 1, 1));
        trades.add(new TradeSpec("minecraft:emerald", 1, 1, List.of("thaumcraft:alumentum"), 1, 1));
        trades.add(new TradeSpec("thaumcraft:amber_gem", 4, 6, List.of("minecraft:emerald"), 1, 1));
        trades.add(new TradeSpec("minecraft:emerald", 1, 1, List.of("thaumcraft:nitor"), 1, 1));
        // Oracle VillageWizardManager:43 - the chicken nugget of the original, the native id
        // of the edible nugget zero (ThaumcraftLegacyItems:168).
        trades.add(new TradeSpec("thaumcraft:nugget_chicken", 24, 31, List.of("minecraft:emerald"), 1, 1));
        trades.add(new TradeSpec("minecraft:book", 4, 6, List.of("thaumcraft:knowledge_fragment"), 1, 1));
        // Oracle VillageWizardManager:45 - the beef nugget of the original, the native id of
        // the edible nugget one.
        trades.add(new TradeSpec("thaumcraft:nugget_beef", 24, 31, List.of("minecraft:emerald"), 1, 1));
        trades.add(new TradeSpec("minecraft:emerald", 1, 1, ASPECT_SHARDS, 2, 3));
        trades.add(new TradeSpec("minecraft:emerald", 1, 1, List.of("thaumcraft:mana_bean"), 1, 2));
        trades.add(new TradeSpec("minecraft:emerald", 5, 7, List.of("thaumcraft:bath_salts"), 1, 1));
        trades.add(new TradeSpec("minecraft:emerald", 5, 7, List.of("thaumcraft:runic_ring_healing"), 1, 1));
        trades.add(new TradeSpec("minecraft:emerald", 5, 7, List.of("thaumcraft:vis_amulet"), 1, 1));
        trades.add(new TradeSpec("minecraft:emerald", 5, 7, BAUBLE_BLANKS, 1, 1));
        return trades;
    }

    /**
     * The tower chest content, oracle ComponentWizardTower:35. The total
     * weight is 81, the same pool the original rolls from.
     */
    public static List<ChestContent> towerChestContents() {
        List<ChestContent> contents = new ArrayList<>(10);
        contents.add(new ChestContent("minecraft:glowstone_dust", 1, 3, 3));
        contents.add(new ChestContent("minecraft:glass_bottle", 1, 5, 10));
        contents.add(new ChestContent("minecraft:gold_nugget", 1, 3, 5));
        contents.add(new ChestContent("minecraft:fire_charge", 1, 1, 5));
        contents.add(new ChestContent("minecraft:skull", 1, 1, 3));
        contents.add(new ChestContent("thaumcraft:knowledge_fragment", 1, 3, 20));
        contents.add(new ChestContent("thaumcraft:alumentum", 1, 1, 5));
        contents.add(new ChestContent("thaumcraft:nitor", 1, 1, 5));
        contents.add(new ChestContent("thaumcraft:thaumium_ingot", 1, 2, 5));
        contents.add(new ChestContent("thaumcraft:thaumonomicon", 1, 1, 20));
        return contents;
    }

    /** The total weight of the tower chest pool. */
    public static int towerChestTotalWeight() {
        int total = 0;
        for (ChestContent content : towerChestContents()) total += content.weight();
        return total;
    }

    /** The number of items the tower chest holds, four to nine. */
    public static int towerChestItemCount(int roll) {
        int span = WIZARD_TOWER_CHEST_MAX - WIZARD_TOWER_CHEST_MIN + 1;
        return WIZARD_TOWER_CHEST_MIN + Math.floorMod(roll, span);
    }

    // ------------------------------------------------------------------ helpers

    /** The 64 bit finalizer used to derive the village seeds. */
    public static long mix64(long value) {
        long z = value;
        z ^= z >>> 30;
        z *= 0xBF58476D1CE4E5B9L;
        z ^= z >>> 27;
        z *= 0x94D049BB133111EBL;
        z ^= z >>> 31;
        return z;
    }

    /** The seed of a village cell. */
    public static long villageSeed(long worldSeed, int cellX, int cellZ) {
        return mix64(worldSeed ^ (cellX * 0x9E3779B97F4A7C15L) ^ (cellZ * 0xC2B2AE3D27D4EB4FL));
    }

    /** The seed of one house slot of a village. */
    public static long houseSeed(long worldSeed, int chunkX, int chunkZ, int houseIndex) {
        return mix64(villageSeed(worldSeed, villageCell(chunkX), villageCell(chunkZ))
                ^ (houseIndex * 0x9E3779B97F4A7C15L));
    }

    // ------------------------------------------------------------------ outer lands maze

    /**
     * Oracle MazeHandler.mazesInRange:142-150 - the ring may not grow where an
     * Outer Lands maze already is.
     *
     * <p>The eldritch ring is the single port of the original WorldGenEldritchRing and it
     * belongs to the Outer Lands, because the ring seeds the labyrinth. The ring therefore
     * asks the native maze store of the overworld directly, and this hook is gone.
     */
    public interface MazeRangeCheck {
        boolean mazesInRange(int chunkX, int chunkZ, int width, int height);
    }

    private static volatile MazeRangeCheck MAZE_RANGE_CHECK = (chunkX, chunkZ, width, height) -> false;

    /** Installs the check of the Outer Lands maze unit. */
    public static void setMazeRangeCheck(MazeRangeCheck check) {
        MAZE_RANGE_CHECK = check == null ? (chunkX, chunkZ, width, height) -> false : check;
    }

    /** Whether a maze is in range of the chunk. */
    public static boolean mazesInRange(int chunkX, int chunkZ, int width, int height) {
        return MAZE_RANGE_CHECK.mazesInRange(chunkX, chunkZ, width, height);
    }
}
