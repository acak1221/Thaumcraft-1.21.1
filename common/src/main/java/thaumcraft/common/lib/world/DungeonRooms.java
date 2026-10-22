package thaumcraft.common.lib.world;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.util.RandomSource;

/**
 * The original TC4 Outer Lands rooms.
 *
 * Oracle sources:
 * <ul>
 *   <li>{@code thaumcraft/common/lib/world/dim/GenCommon.java:30-394} - the shared block table,
 *       the room connection pattern and the decoration pass.</li>
 *   <li>{@code thaumcraft/common/lib/world/dim/GenPassage.java:24-260} - the passage.</li>
 *   <li>{@code thaumcraft/common/lib/world/dim/GenPortal.java:19-118} - the portal room.</li>
 *   <li>{@code thaumcraft/common/lib/world/dim/GenKeyRoom.java:29-117} - the key room.</li>
 *   <li>{@code thaumcraft/common/lib/world/dim/GenNestRoom.java:18-122} - the nest room.</li>
 *   <li>{@code thaumcraft/common/lib/world/dim/GenLibraryRoom.java:17-101} - the library room.</li>
 *   <li>{@code thaumcraft/common/lib/world/dim/GenBossRoom.java:20-88} and
 *       {@code thaumcraft/common/lib/world/dim/Gen2x2.java:16-225} - the 2x2 boss room.</li>
 * </ul>
 *
 * The class is pure: it turns a labyrinth cell into a list of block placements and entity
 * spawns. The world dependent parts (the "is this the eldritch nothing block" test, the exposed
 * side count and the neighbour lookups) are supplied through {@link View} so the layouts stay
 * unit testable.
 *
 * Two random streams are kept apart, exactly like the original: {@code random} is the chunk
 * generation stream the room code draws from and {@code worldRandom} is the world stream the
 * shared block table draws from.
 */
public final class DungeonRooms {
    private DungeonRooms() {}

    /* ------------------------------------------------------------------ */
    /* The original block ids (oracle GenCommon:34-49)                    */
    /* ------------------------------------------------------------------ */

    public static final int BEDROCK = 1;
    public static final int STONE = 2;
    public static final int VOID = 8;
    public static final int AIR_REPL = 9;
    public static final int STAIR_DIRECTIONAL = 10;
    public static final int STAIR_DIRECTIONAL_INV = 11;
    public static final int SLAB = 12;
    public static final int DOOR_BLOCK = 15;
    public static final int DOOR_LOCK = 16;
    public static final int VOID_DOOR = 17;
    public static final int ROCK = 18;
    public static final int STONE_NOSPAWN = 19;
    public static final int STONE_TRAPPED = 20;
    public static final int CRUST = 21;
    public static final int BEDROCK_REPL = 99;

    /** Oracle GenCommon.PAT_CONNECT:49 - the 11x11 tunnel cross section. */
    public static final int[][] PAT_CONNECT = {
            {0, 1, 1, 1, 1, 1, 1, 1, 1, 1, 0},
            {1, 8, 8, 8, 8, 8, 8, 8, 8, 8, 1},
            {1, 8, 8, 2, 2, 2, 2, 2, 8, 8, 1},
            {1, 8, 2, 5, 9, 9, 9, 6, 2, 8, 1},
            {1, 8, 2, 9, 9, 9, 9, 9, 2, 8, 1},
            {1, 8, 2, 9, 9, 9, 9, 9, 2, 8, 1},
            {1, 8, 2, 9, 9, 9, 9, 9, 2, 8, 1},
            {1, 8, 2, 3, 9, 9, 9, 4, 2, 8, 1},
            {1, 8, 8, 2, 2, 2, 2, 2, 8, 8, 1},
            {1, 8, 8, 8, 8, 8, 8, 8, 8, 8, 1},
            {0, 1, 1, 1, 1, 1, 1, 1, 1, 1, 0}};

    /** Oracle GenBossRoom.PAT_DOORWAY:22 - the 7x7 boss room doorway. */
    public static final int[][] PAT_DOORWAY = {
            {0, 2, 2, 2, 2, 2, 0},
            {2, 2, 9, 9, 9, 2, 2},
            {2, 9, 9, 9, 9, 9, 2},
            {2, 9, 9, 1, 9, 9, 2},
            {2, 9, 9, 9, 9, 9, 2},
            {2, 2, 9, 9, 9, 2, 2},
            {0, 2, 2, 2, 2, 2, 0}};

    /* ------------------------------------------------------------------ */
    /* Placement model                                                      */
    /* ------------------------------------------------------------------ */

    /** The native block kinds the original block ids resolve to. */
    public enum Kind {
        /** minecraft:bedrock - the room shell. */
        BEDROCK,
        /** thaumcraft:cosmetic_solid - the wall and floor surfaces. */
        COSMETIC_SOLID,
        /** thaumcraft:eldritch_nothing - the invisible wall face. */
        ELDRITCH_NOTHING,
        /** thaumcraft:airy - the invisible door. */
        AIRY,
        /** thaumcraft:stairs_eldritch - the room stairs. */
        STAIRS,
        /** thaumcraft:eldritch - the doors, the locks, the traps and the altar. */
        ELDRITCH,
        /** thaumcraft:eldritch_portal - the portal into the Outer Lands. */
        ELDRITCH_PORTAL,
        /** minecraft:air - the open doorway. */
        AIR,
        /** thaumcraft:taint_fibres - the taint overgrowth. */
        TAINT_FIBRES,
        /** minecraft:cobweb - the mind spider webs. */
        WEB,
        /** minecraft:spawner - the mind spider spawner. */
        MOB_SPAWNER,
        /** thaumcraft:crystal - the eldritch crystal. */
        CRYSTAL,
        /** thaumcraft:loot_urn - the loot urn. */
        LOOT_URN,
        /** thaumcraft:loot_crate - the loot crate. */
        LOOT_CRATE,
        /** thaumcraft:cosmetic_slab_stone - the library desks. */
        COSMETIC_SLAB_STONE
    }

    /** One block placement. The variant carries the original 1.7 metadata. */
    public record Placement(int x, int y, int z, Kind kind, int variant, int facing) {
    }

    /** The entity spawns a room creates. */
    public enum SpawnKind {
        ELDRITCH_GUARDIAN,
        PERMANENT_ITEM,
        MIND_SPIDER
    }

    /**
     * The home area of a spawned entity.
     *
     * <p>Oracle {@code thaumcraft/common/entities/monster/EntityEldritchGuardian.java} and
     * {@code thaumcraft/common/lib/world/dim/GenKeyRoom.java:110} - the guardian of the key room
     * keeps the centre of its room as its home with a radius of sixteen.</p>
     */
    public record Home(int x, int y, int z, int radius) {
    }

    /** One entity spawn. */
    public record Spawn(int x, int y, int z, SpawnKind kind, int variant, int count, boolean champion, Home home) {
    }

    /** The world queries the original room code performs. */
    public interface View {
        /** True when the position holds air (1.7 {@code World.isAirBlock}). */
        boolean isAir(int x, int y, int z);

        /** True when the position holds the eldritch nothing block (never overwritten). */
        boolean isVoidBlock(int x, int y, int z);

        /** True when the position touches a solid block (1.7 {@code BlockUtils.isAdjacentToSolidBlock}). */
        boolean isSolidAdjacent(int x, int y, int z);

        /** True when the position holds a cosmetic solid or an eldritch stair. */
        boolean isCosmeticSolidOrStair(int x, int y, int z);
    }

    /** Collects the placements, the decoration candidates and the spawns of one room. */
    public static final class Builder {
        public final List<Placement> placements = new ArrayList<>();
        public final List<int[]> urns = new ArrayList<>();
        public final List<int[]> commons = new ArrayList<>();
        public final List<int[]> crabSpawners = new ArrayList<>();
        public final List<Spawn> spawns = new ArrayList<>();

        /** Oracle GenCommon.placeBlock:66-98 removes the decoration of an overwritten position. */
        void clearDecoration(int x, int y, int z) {
            urns.removeIf(p -> p[0] == x && p[1] == y && p[2] == z);
            commons.removeIf(p -> p[0] == x && p[1] == y && p[2] == z);
            crabSpawners.removeIf(p -> p[0] == x && p[1] == y && p[2] == z);
        }
    }

    /* ------------------------------------------------------------------ */
    /* The shared block table                                              */
    /* ------------------------------------------------------------------ */

    /**
     * Oracle GenCommon.placeBlock:51-269. The 1.7 block id, the original metadata and the
     * direction are translated into a native placement.
     *
     * @param facing the 1.7 direction ordinal (2 north, 3 south, 4 west, 5 east) or -1
     */
    public static void place(Builder out, View view, RandomSource worldRandom, int x, int y, int z,
                             int block, int facing, MazeGenerator.Cell cell) {
        Kind kind;
        int variant = 0;
        switch (block) {
            case BEDROCK -> {
                if (!view.isAir(x, y, z)) return;
                kind = Kind.BEDROCK;
            }
            case BEDROCK_REPL -> kind = Kind.BEDROCK;
            case VOID -> kind = Kind.ELDRITCH_NOTHING;
            case VOID_DOOR -> {
                kind = Kind.AIRY;
                variant = 12;
            }
            case DOOR_BLOCK -> {
                kind = Kind.ELDRITCH;
                variant = 7;
                out.clearDecoration(x, y, z);
            }
            case DOOR_LOCK -> {
                kind = Kind.ELDRITCH;
                variant = 8;
                out.clearDecoration(x, y, z);
            }
            case AIR_REPL -> {
                kind = Kind.AIR;
                out.clearDecoration(x, y, z);
            }
            case STONE -> {
                // Oracle:100-116. The nest room (feature 7) falls through to the crust branch.
                if (cell.feature != 7 || worldRandom.nextInt(3) != 0) {
                    if (view.isVoidBlock(x, y, z)) return;
                    if (worldRandom.nextInt(25) == 0) {
                        boolean crab = cell.feature == 7 ? true : (worldRandom.nextInt(50) == 0);
                        if ((crab && cell.feature == 0) || (crab && cell.feature == 7)) {
                            out.crabSpawners.add(new int[] {x, y, z});
                        } else {
                            out.commons.add(new int[] {x, y, z});
                        }
                    }
                    kind = Kind.COSMETIC_SOLID;
                    variant = 11;
                    break;
                }
                kind = Kind.COSMETIC_SOLID;
                variant = 14;
                if (view.isVoidBlock(x, y, z)) return;
                if (worldRandom.nextInt(25) == 0) {
                    kind = Kind.ELDRITCH;
                    variant = 4;
                    break;
                }
                if (worldRandom.nextInt(25) != 0) break;
                boolean crab = cell.feature == 7 ? true
                        : (cell.feature == 12 && worldRandom.nextBoolean() ? true : (worldRandom.nextInt(25) == 0));
                if (!((crab && cell.feature == 0) || (crab && cell.feature == 7)) && (!crab || cell.feature != 12)) break;
                out.crabSpawners.add(new int[] {x, y, z});
            }
            case CRUST -> {
                if (view.isVoidBlock(x, y, z)) return;
                kind = Kind.COSMETIC_SOLID;
                variant = 14;
                if (worldRandom.nextInt(25) == 0) {
                    kind = Kind.ELDRITCH;
                    variant = 4;
                    break;
                }
                if (worldRandom.nextInt(25) != 0) break;
                boolean crab = cell.feature == 7 ? true
                        : (cell.feature == 12 && worldRandom.nextBoolean() ? true : (worldRandom.nextInt(25) == 0));
                if (!((crab && cell.feature == 0) || (crab && cell.feature == 7)) && (!crab || cell.feature != 12)) break;
                out.crabSpawners.add(new int[] {x, y, z});
            }
            case ROCK -> {
                if (view.isVoidBlock(x, y, z)) return;
                kind = Kind.COSMETIC_SOLID;
                variant = 12;
            }
            case STONE_NOSPAWN -> {
                if (view.isVoidBlock(x, y, z)) return;
                kind = Kind.COSMETIC_SOLID;
                variant = 13;
            }
            case STONE_TRAPPED -> {
                if (view.isVoidBlock(x, y, z)) return;
                kind = Kind.ELDRITCH;
                variant = 10;
            }
            case STAIR_DIRECTIONAL -> {
                kind = Kind.STAIRS;
                variant = switch (facing) {
                    case 2 -> 3; // north
                    case 3 -> 2; // south
                    case 5 -> 0; // east
                    case 4 -> 1; // west
                    default -> 0;
                };
            }
            case STAIR_DIRECTIONAL_INV -> {
                kind = Kind.STAIRS;
                variant = switch (facing) {
                    case 2 -> 7;
                    case 3 -> 6;
                    case 5 -> 4;
                    case 4 -> 5;
                    default -> 0;
                };
            }
            case 3 -> {
                if (worldRandom.nextFloat() < 0.005F) out.urns.add(new int[] {x, y, z});
                kind = Kind.STAIRS;
                variant = stairsMeta(facing, 1, 3);
            }
            case 4 -> {
                if (worldRandom.nextFloat() < 0.005F) out.urns.add(new int[] {x, y, z});
                kind = Kind.STAIRS;
                variant = stairsMeta(facing, 0, 2);
            }
            case 5 -> {
                kind = Kind.STAIRS;
                variant = stairsMeta(facing, 5, 7);
            }
            case 6 -> {
                kind = Kind.STAIRS;
                variant = stairsMeta(facing, 4, 6);
            }
            case 7 -> {
                kind = Kind.ELDRITCH;
                variant = 4;
            }
            default -> {
                return;
            }
        }
        out.placements.add(new Placement(x, y, z, kind, variant, facing));
    }

    /** Oracle GenCommon.placeBlock:195-260 maps the direction ordinal onto the stair metadata. */
    private static int stairsMeta(int facing, int northSouthMeta, int westEastMeta) {
        return switch (facing) {
            case 2, 3 -> northSouthMeta;
            case 4, 5 -> westEastMeta;
            default -> 0;
        };
    }

    /* ------------------------------------------------------------------ */
    /* The room connections                                                */
    /* ------------------------------------------------------------------ */

    /**
     * Oracle GenCommon.generateConnections:327-393.
     *
     * @param depth the passage uses 4, every room uses 3
     * @param justthetip the room flavour, the passage uses false
     * @param skipOuterLayer true for the south and east stubs, whose first layer belongs to the
     *                       neighbouring chunk in the 1.21 chunk local generation
     */
    public static void connections(Builder out, View view, RandomSource worldRandom, int cx, int cz, int y,
                                   MazeGenerator.Cell cell, int depth, boolean justthetip,
                                   boolean skipOuterLayer) {
        int x = cx * 16;
        int z = cz * 16;
        if (cell.north) {
            for (int d = 0; d <= depth; ++d) {
                int from = layerStart(d, depth, justthetip);
                int to = layerEnd(d, depth, justthetip);
                for (int w = from; w < to; ++w) {
                    for (int h = from; h < to; ++h) {
                        if (d != depth || !justthetip || PAT_CONNECT[h][w] != 8) {
                            place(out, view, worldRandom, x + 3 + w, y + 10 - h, z + d, PAT_CONNECT[h][w], 2, cell);
                        }
                    }
                }
            }
        }
        if (cell.south) {
            for (int d = skipOuterLayer ? 1 : 0; d <= depth; ++d) {
                int from = layerStart(d, depth, justthetip);
                int to = layerEnd(d, depth, justthetip);
                for (int w = from; w < to; ++w) {
                    for (int h = from; h < to; ++h) {
                        if (d != depth || !justthetip || PAT_CONNECT[h][w] != 8) {
                            place(out, view, worldRandom, x + 3 + w, y + 10 - h, z + 16 - d, PAT_CONNECT[h][w], 3, cell);
                        }
                    }
                }
            }
        }
        if (cell.east) {
            for (int d = skipOuterLayer ? 1 : 0; d <= depth; ++d) {
                int from = layerStart(d, depth, justthetip);
                int to = layerEnd(d, depth, justthetip);
                for (int w = from; w < to; ++w) {
                    for (int h = from; h < to; ++h) {
                        if (d != depth || !justthetip || PAT_CONNECT[h][w] != 8) {
                            place(out, view, worldRandom, x + 16 - d, y + 10 - h, z + 3 + w, PAT_CONNECT[h][w], 5, cell);
                        }
                    }
                }
            }
        }
        if (cell.west) {
            for (int d = 0; d <= depth; ++d) {
                int from = layerStart(d, depth, justthetip);
                int to = layerEnd(d, depth, justthetip);
                for (int w = from; w < to; ++w) {
                    for (int h = from; h < to; ++h) {
                        if (d != depth || !justthetip || PAT_CONNECT[h][w] != 8) {
                            place(out, view, worldRandom, x + d, y + 10 - h, z + 3 + w, PAT_CONNECT[h][w], 4, cell);
                        }
                    }
                }
            }
        }
    }

    /** Oracle GenCommon.generateConnections:335-338 - the layer window of a connection depth. */
    private static int layerStart(int depth, int maxDepth, boolean justthetip) {
        if (justthetip && depth == maxDepth) return 2;
        if (justthetip && depth == maxDepth - 1) return 1;
        return 0;
    }

    private static int layerEnd(int depth, int maxDepth, boolean justthetip) {
        if (justthetip && depth == maxDepth) return 9;
        if (justthetip && depth == maxDepth - 1) return 10;
        return 11;
    }

    /* ------------------------------------------------------------------ */
    /* The passage                                                          */
    /* ------------------------------------------------------------------ */

    /**
     * Oracle GenPassage.generateDefaultPassage:26-259. The connections are generated first,
     * exactly like the original.
     */
    public static void generatePassage(Builder out, View view, RandomSource random, RandomSource worldRandom,
                                       int cx, int cz, int y, MazeGenerator.Cell cell) {
        int x = cx * 16;
        int z = cz * 16;
        connections(out, view, worldRandom, cx, cz, y, cell, 4, false, true);
        int mod = 0;
        if (cell.north && cell.south && cell.west && cell.east && random.nextBoolean()) mod = 1;
        for (int w = 1; w < 8; ++w) {
            for (int h = 1; h < 8; ++h) {
                if (w == 4 && h == 4 && mod == 1) {
                    place(out, view, worldRandom, x + 4 + w, y + 2, z + 4 + h, 7, -1, cell);
                    place(out, view, worldRandom, x + 4 + w, y + 8, z + 4 + h, 7, -1, cell);
                } else {
                    // Oracle GenPassage:54-55 - the floor and the ceiling draw the buried stone
                    // of a trapped passage separately, so the floor and the ceiling of the same
                    // column may differ and the random stream of the passage advances by two
                    // rolls of three for every column.
                    place(out, view, worldRandom, x + 4 + w, y + 2, z + 4 + h,
                            cell.feature == OuterLandsRules.FEATURE_TRAPPED_PASSAGE && random.nextInt(3) == 0 ? 20 : 2, -1, cell);
                    place(out, view, worldRandom, x + 4 + w, y + 8, z + 4 + h,
                            cell.feature == OuterLandsRules.FEATURE_TRAPPED_PASSAGE && random.nextInt(3) == 0 ? 20 : 2, -1, cell);
                }
                place(out, view, worldRandom, x + 4 + w, y, z + 4 + h, 1, -1, cell);
                place(out, view, worldRandom, x + 4 + w, y + 10, z + 4 + h, 1, -1, cell);
                place(out, view, worldRandom, x + 4 + w, y + 1, z + 4 + h, 8, -1, cell);
                place(out, view, worldRandom, x + 4 + w, y + 9, z + 4 + h, 8, -1, cell);
            }
        }
        if (cell.north) {
            for (int w = 2 + mod; w < 9 - mod; ++w) {
                for (int h = 2 + mod; h < 9 - mod; ++h) {
                    place(out, view, worldRandom, x + 3 + w, y + 10 - h, z + 5, PAT_CONNECT[h][w], 2, cell);
                }
            }
            if (mod == 0) {
                if (cell.west) {
                    place(out, view, worldRandom, x + 6, y + 3, z + 6, 3, 5, cell);
                    place(out, view, worldRandom, x + 6, y + 7, z + 6, 5, 5, cell);
                }
                if (cell.east) {
                    place(out, view, worldRandom, x + 10, y + 3, z + 6, 3, 5, cell);
                    place(out, view, worldRandom, x + 10, y + 7, z + 6, 5, 5, cell);
                }
            }
        } else {
            closedWall(out, view, random, worldRandom, cell, x, y, z, 3, false);
        }
        if (cell.south) {
            for (int w = 2 + mod; w < 9 - mod; ++w) {
                for (int h = 2 + mod; h < 9 - mod; ++h) {
                    place(out, view, worldRandom, x + 3 + w, y + 10 - h, z + 11, PAT_CONNECT[h][w], 3, cell);
                }
            }
            if (mod == 0) {
                if (cell.west) {
                    place(out, view, worldRandom, x + 6, y + 3, z + 10, 4, 5, cell);
                    place(out, view, worldRandom, x + 6, y + 7, z + 10, 6, 5, cell);
                }
                if (cell.east) {
                    place(out, view, worldRandom, x + 10, y + 3, z + 10, 4, 5, cell);
                    place(out, view, worldRandom, x + 10, y + 7, z + 10, 6, 5, cell);
                }
            }
        } else {
            closedWall(out, view, random, worldRandom, cell, x, y, z, 11, false);
        }
        if (cell.east) {
            for (int w = 2 + mod; w < 9 - mod; ++w) {
                for (int h = 2 + mod; h < 9 - mod; ++h) {
                    place(out, view, worldRandom, x + 11, y + 10 - h, z + 3 + w, PAT_CONNECT[h][w], 5, cell);
                }
            }
            if (mod == 0) {
                if (cell.north) {
                    place(out, view, worldRandom, x + 10, y + 3, z + 6, 4, 2, cell);
                    place(out, view, worldRandom, x + 10, y + 7, z + 6, 6, 2, cell);
                }
                if (cell.south) {
                    place(out, view, worldRandom, x + 10, y + 3, z + 10, 4, 2, cell);
                    place(out, view, worldRandom, x + 10, y + 7, z + 10, 6, 2, cell);
                }
            }
        } else {
            closedWall(out, view, random, worldRandom, cell, x, y, z, 15, true);
        }
        if (cell.west) {
            for (int w = 2 + mod; w < 9 - mod; ++w) {
                for (int h = 2 + mod; h < 9 - mod; ++h) {
                    place(out, view, worldRandom, x + 5, y + 10 - h, z + 3 + w, PAT_CONNECT[h][w], 4, cell);
                }
            }
            if (mod == 0) {
                if (cell.north) {
                    place(out, view, worldRandom, x + 6, y + 3, z + 6, 3, 2, cell);
                    place(out, view, worldRandom, x + 6, y + 7, z + 6, 5, 2, cell);
                }
                if (cell.south) {
                    place(out, view, worldRandom, x + 6, y + 3, z + 10, 3, 2, cell);
                    place(out, view, worldRandom, x + 6, y + 7, z + 10, 5, 2, cell);
                }
            }
        } else {
            closedWall(out, view, random, worldRandom, cell, x, y, z, 7, true);
        }
        if (mod == 1) {
            place(out, view, worldRandom, x + 5, y + 3, z + 5, 3, 5, cell);
            place(out, view, worldRandom, x + 5, y + 7, z + 5, 5, 5, cell);
            place(out, view, worldRandom, x + 5, y + 3, z + 6, 3, 2, cell);
            place(out, view, worldRandom, x + 5, y + 7, z + 6, 5, 2, cell);
            place(out, view, worldRandom, x + 11, y + 3, z + 5, 3, 5, cell);
            place(out, view, worldRandom, x + 11, y + 7, z + 5, 5, 5, cell);
            place(out, view, worldRandom, x + 11, y + 3, z + 6, 4, 2, cell);
            place(out, view, worldRandom, x + 11, y + 7, z + 6, 6, 2, cell);
            place(out, view, worldRandom, x + 5, y + 3, z + 11, 3, 2, cell);
            place(out, view, worldRandom, x + 5, y + 7, z + 11, 5, 2, cell);
            place(out, view, worldRandom, x + 6, y + 3, z + 11, 4, 5, cell);
            place(out, view, worldRandom, x + 6, y + 7, z + 11, 6, 5, cell);
            place(out, view, worldRandom, x + 11, y + 3, z + 11, 4, 2, cell);
            place(out, view, worldRandom, x + 11, y + 7, z + 11, 6, 2, cell);
            place(out, view, worldRandom, x + 10, y + 3, z + 11, 4, 5, cell);
            place(out, view, worldRandom, x + 10, y + 7, z + 11, 6, 5, cell);
        }
        if (cell.feature == OuterLandsRules.FEATURE_CRUST_PASSAGE) {
            for (int w = -4; w <= 4; ++w) {
                for (int h = -4; h < 5; ++h) {
                    for (int j = -4; j <= 4; ++j) {
                        if (!view.isAir(x + 8 + w, y + 4 + h, z + 8 + j)
                                && !view.isCosmeticSolidOrStair(x + 8 + w, y + 4 + h, z + 8 + j)) continue;
                        if (!random.nextBoolean()) continue;
                        place(out, view, worldRandom, x + 8 + w, y + 4 + h, z + 8 + j, 21, -1, cell);
                    }
                }
            }
        }
        if (cell.feature == OuterLandsRules.FEATURE_TAINT_PASSAGE) {
            for (int w = -4; w <= 4; ++w) {
                for (int h = -3; h <= 3; ++h) {
                    for (int j = -4; j <= 4; ++j) {
                        if (!view.isAir(x + 8 + w, y + 4 + h, z + 8 + j)) continue;
                        if (!view.isSolidAdjacent(x + 8 + w, y + 4 + h, z + 8 + j)) continue;
                        if (random.nextInt(3) != 0) {
                            out.placements.add(new Placement(x + 8 + w, y + 4 + h, z + 8 + j, Kind.TAINT_FIBRES,
                                    OuterLandsRules.taintFibreMeta(random.nextInt(4)), -1));
                        }
                    }
                }
            }
        }
        if (cell.feature == OuterLandsRules.FEATURE_SPIDER_PASSAGE) {
            for (int w = -3; w <= 3; ++w) {
                for (int h = -3; h <= 3; ++h) {
                    for (int j = -3; j <= 3; ++j) {
                        if (view.isAir(x + 8 + w, y + 4 + h, z + 8 + j) && random.nextFloat() < 0.35F) {
                            out.placements.add(new Placement(x + 8 + w, y + 4 + h, z + 8 + j, Kind.WEB, 0, -1));
                        }
                        out.placements.add(new Placement(x + 8, y + 4, z + 8, Kind.MOB_SPAWNER, 0, -1));
                        out.spawns.add(new Spawn(x + 8, y + 4, z + 8, SpawnKind.MIND_SPIDER, 0, 1, false, null));
                    }
                }
            }
        }
    }

    /**
     * The closed side of a passage (oracle GenPassage:79-96 for the north, 114-131 for the south,
     * 149-166 for the east and 184-201 for the west).
     *
     * @param side 3 north, 11 south, 15 east, 7 west - the block id of the wall pattern
     * @param eastWest true for the east and west walls
     */
    private static void closedWall(Builder out, View view, RandomSource random, RandomSource worldRandom,
                                   MazeGenerator.Cell cell, int x, int y, int z, int side, boolean eastWest) {
        for (int w = 1; w < 8; ++w) {
            for (int h = 1; h < 8; ++h) {
                int block = cell.feature == OuterLandsRules.FEATURE_TRAPPED_PASSAGE && random.nextInt(3) == 0 ? 20 : 2;
                if (!eastWest) {
                    int wallZ = side == 3 ? z + 5 : z + 11;
                    int voidZ = side == 3 ? z + 4 : z + 12;
                    int rockZ = side == 3 ? z + 3 : z + 13;
                    place(out, view, worldRandom, x + 4 + w, y + 9 - h, wallZ, block, -1, cell);
                    place(out, view, worldRandom, x + 4 + w, y + 9 - h, voidZ, 8, -1, cell);
                    place(out, view, worldRandom, x + 4 + w, y + 9 - h, rockZ, 1, -1, cell);
                    if (h == 7) {
                        place(out, view, worldRandom, x + 4 + w, y + 1, voidZ, 1, -1, cell);
                        place(out, view, worldRandom, x + 4 + w, y + 9, voidZ, 1, -1, cell);
                    }
                    if (w != 7) continue;
                    place(out, view, worldRandom, x + 4, y + 9 - h, voidZ, 1, -1, cell);
                    place(out, view, worldRandom, x + 12, y + 9 - h, voidZ, 1, -1, cell);
                } else {
                    int wallX = side == 15 ? x + 11 : x + 5;
                    int voidX = side == 15 ? x + 12 : x + 4;
                    int rockX = side == 15 ? x + 13 : x + 3;
                    place(out, view, worldRandom, wallX, y + 9 - h, z + 4 + w, block, -1, cell);
                    place(out, view, worldRandom, voidX, y + 9 - h, z + 4 + w, 8, -1, cell);
                    place(out, view, worldRandom, rockX, y + 9 - h, z + 4 + w, 1, -1, cell);
                    if (h == 7) {
                        place(out, view, worldRandom, voidX, y + 1, z + 4 + w, 1, -1, cell);
                        place(out, view, worldRandom, voidX, y + 9, z + 4 + w, 1, -1, cell);
                    }
                    if (w != 7) continue;
                    place(out, view, worldRandom, voidX, y + 9 - h, z + 4, 1, -1, cell);
                    place(out, view, worldRandom, voidX, y + 9 - h, z + 12, 1, -1, cell);
                }
            }
        }
        for (int w = 2; w < 7; ++w) {
            if (!eastWest) {
                int stairZ = side == 3 ? z + 6 : z + 10;
                int first = side == 3 ? 3 : 4;
                int second = side == 3 ? 5 : 6;
                place(out, view, worldRandom, x + 4 + w, y + 3, stairZ, first, 5, cell);
                place(out, view, worldRandom, x + 4 + w, y + 7, stairZ, second, 5, cell);
            } else {
                int stairX = side == 15 ? x + 10 : x + 6;
                int first = side == 15 ? 4 : 3;
                int second = side == 15 ? 6 : 5;
                place(out, view, worldRandom, stairX, y + 3, z + 4 + w, first, 2, cell);
                place(out, view, worldRandom, stairX, y + 7, z + 4 + w, second, 2, cell);
            }
        }
    }

    /* ------------------------------------------------------------------ */
    /* The portal room                                                      */
    /* ------------------------------------------------------------------ */

    /** Oracle GenPortal.generatePortal:21-117. */
    public static void generatePortal(Builder out, View view, RandomSource random, RandomSource worldRandom,
                                      int cx, int cz, int y, MazeGenerator.Cell cell) {
        int x = cx * 16;
        int z = cz * 16;
        for (int a = 1; a <= 15; ++a) {
            for (int b = 1; b <= 15; ++b) {
                for (int c = 0; c < 13; ++c) {
                    if (a != 1 && a != 15 && b != 1 && b != 15) continue;
                    place(out, view, worldRandom, x + a, y + c, z + b, 1, -1, cell);
                }
            }
        }
        for (int a = 2; a <= 14; ++a) {
            for (int b = 2; b <= 14; ++b) {
                for (int c = 1; c < 12; ++c) {
                    if (a != 2 && a != 14 && b != 2 && b != 14) {
                        if (a == 2 && b > 3 && b < 12 && cell.west && c < 10) continue;
                        if (a == 14 && b > 3 && b < 12 && cell.east && c < 10) continue;
                        if (b == 2 && a > 3 && a < 12 && cell.north && c < 10) continue;
                        if (b == 14 && a > 3 && a < 12 && cell.south && c < 10) continue;
                    }
                    place(out, view, worldRandom, x + a, y + c, z + b, 8, -1, cell);
                }
            }
        }
        for (int a = 3; a <= 13; ++a) {
            for (int b = 3; b <= 13; ++b) {
                for (int c = 2; c < 11; ++c) {
                    if (a != 3 && a != 13 && b != 3 && b != 13) continue;
                    if (a <= 4 && b <= 4 || a <= 4 && b >= 12 || a >= 12 && b <= 4 || a >= 12 && b >= 12) continue;
                    place(out, view, worldRandom, x + a, y + c, z + b, 2, -1, cell);
                }
            }
        }
        for (int a = 2; a <= 14; ++a) {
            for (int b = 2; b <= 14; ++b) {
                place(out, view, worldRandom, x + a, y - 1, z + b, 1, -1, cell);
                place(out, view, worldRandom, x + a, y, z + b, 8, -1, cell);
                place(out, view, worldRandom, x + a, y + 1, z + b, 19, -1, cell);
                place(out, view, worldRandom, x + a, y + 13, z + b, 1, -1, cell);
                place(out, view, worldRandom, x + a, y + 12, z + b, 8, -1, cell);
                place(out, view, worldRandom, x + a, y + 11, z + b, 2, -1, cell);
                if (a > 1 && a < 15 && b > 1 && b < 15) {
                    int q = Math.min(Math.abs(8 - a), Math.abs(8 - b));
                    for (int g = 0; g < q - 1; ++g) {
                        place(out, view, worldRandom, x + a, y + 1 + g, z + b, 19, -1, cell);
                    }
                }
                if (a <= 3 || a >= 13 || b <= 3 || b >= 13) continue;
                int q = Math.min(Math.abs(8 - a), Math.abs(8 - b));
                for (int g = 0; g < q; ++g) {
                    place(out, view, worldRandom, x + a, y + 11 - g, z + b, 19, -1, cell);
                }
            }
        }
        for (int g = 0; g < 5; ++g) {
            place(out, view, worldRandom, x + 6 + g, y + 2, z + 4, 10, 2, cell);
            place(out, view, worldRandom, x + 6 + g, y + 2, z + 12, 10, 3, cell);
            place(out, view, worldRandom, x + 12, y + 2, z + 6 + g, 10, 5, cell);
            place(out, view, worldRandom, x + 4, y + 2, z + 6 + g, 10, 4, cell);
        }
        connections(out, view, worldRandom, cx, cz, y, cell, 3, true, true);
        for (int a = 3; a <= 13; ++a) {
            for (int b = 3; b <= 13; ++b) {
                for (int c = 1; c < 12; ++c) {
                    // Oracle GenPortal:84 - the portal room is cleared in all four corners.
                    boolean corner = a <= 4 && b <= 4 || a <= 4 && b >= 12
                            || a >= 12 && b <= 4 || a >= 12 && b >= 12;
                    if (!corner) continue;
                    place(out, view, worldRandom, x + a, y + c, z + b, 9, -1, cell);
                }
            }
        }
        place(out, view, worldRandom, x + 5, y + 3, z + 5, 10, 2, cell);
        place(out, view, worldRandom, x + 4, y + 3, z + 5, 10, 2, cell);
        place(out, view, worldRandom, x + 5, y + 3, z + 4, 10, 4, cell);
        place(out, view, worldRandom, x + 5, y + 8, z + 5, 11, 2, cell);
        place(out, view, worldRandom, x + 4, y + 8, z + 5, 11, 2, cell);
        place(out, view, worldRandom, x + 5, y + 8, z + 4, 11, 4, cell);
        place(out, view, worldRandom, x + 12, y + 3, z + 5, 10, 2, cell);
        place(out, view, worldRandom, x + 11, y + 3, z + 5, 10, 2, cell);
        place(out, view, worldRandom, x + 11, y + 3, z + 4, 10, 5, cell);
        place(out, view, worldRandom, x + 12, y + 8, z + 5, 11, 2, cell);
        place(out, view, worldRandom, x + 11, y + 8, z + 5, 11, 2, cell);
        place(out, view, worldRandom, x + 11, y + 8, z + 4, 11, 5, cell);
        place(out, view, worldRandom, x + 5, y + 3, z + 11, 10, 3, cell);
        place(out, view, worldRandom, x + 4, y + 3, z + 11, 10, 3, cell);
        place(out, view, worldRandom, x + 5, y + 3, z + 12, 10, 4, cell);
        place(out, view, worldRandom, x + 5, y + 8, z + 11, 11, 3, cell);
        place(out, view, worldRandom, x + 4, y + 8, z + 11, 11, 3, cell);
        place(out, view, worldRandom, x + 5, y + 8, z + 12, 11, 4, cell);
        place(out, view, worldRandom, x + 12, y + 3, z + 11, 10, 3, cell);
        place(out, view, worldRandom, x + 11, y + 3, z + 11, 10, 3, cell);
        place(out, view, worldRandom, x + 11, y + 3, z + 12, 10, 5, cell);
        place(out, view, worldRandom, x + 12, y + 8, z + 11, 11, 3, cell);
        place(out, view, worldRandom, x + 11, y + 8, z + 11, 11, 3, cell);
        place(out, view, worldRandom, x + 11, y + 8, z + 12, 11, 5, cell);
        out.placements.add(new Placement(x + 8, y + 2, z + 8, Kind.ELDRITCH, 3, -1));
        out.placements.add(new Placement(x + 8, y + 3, z + 8, Kind.ELDRITCH_PORTAL, 0, -1));
        genObelisk(out, x + 8, y + 4, z + 8);
    }

    /** Oracle GenCommon.genObelisk:271-277. */
    public static void genObelisk(Builder out, int x, int y, int z) {
        out.placements.add(new Placement(x, y, z, Kind.ELDRITCH, 1, -1));
        for (int i = 1; i <= 4; ++i) out.placements.add(new Placement(x, y + i, z, Kind.ELDRITCH, 2, -1));
    }

    /* ------------------------------------------------------------------ */
    /* The key room                                                         */
    /* ------------------------------------------------------------------ */

    /**
     * Oracle GenKeyRoom.generateRoom:31-116. The guardians and the permanent item are returned
     * as spawns so the world layer can create them.
     */
    public static void generateKeyRoom(Builder out, View view, RandomSource random, RandomSource worldRandom,
                                       int cx, int cz, int y, MazeGenerator.Cell cell, int difficulty) {
        int x = cx * 16;
        int z = cz * 16;
        for (int a = 1; a <= 15; ++a) {
            for (int b = 1; b <= 15; ++b) {
                for (int c = 0; c < 13; ++c) {
                    if (a != 1 && a != 15 && b != 1 && b != 15) continue;
                    place(out, view, worldRandom, x + a, y + c, z + b, 1, -1, cell);
                }
            }
        }
        for (int a = 2; a <= 14; ++a) {
            for (int b = 2; b <= 14; ++b) {
                for (int c = 1; c < 12; ++c) {
                    if (a != 2 && a != 14 && b != 2 && b != 14) {
                        if (a == 2 && b > 3 && b < 12 && cell.west && c < 10) continue;
                        if (a == 14 && b > 3 && b < 12 && cell.east && c < 10) continue;
                        if (b == 2 && a > 3 && a < 12 && cell.north && c < 10) continue;
                        if (b == 14 && a > 3 && a < 12 && cell.south && c < 10) continue;
                    }
                    place(out, view, worldRandom, x + a, y + c, z + b, 8, -1, cell);
                }
            }
        }
        for (int a = 3; a <= 13; ++a) {
            for (int b = 3; b <= 13; ++b) {
                for (int c = 2; c < 11; ++c) {
                    if (a != 3 && a != 13 && b != 3 && b != 13) continue;
                    boolean pillar = c > 3 && c < 9 && (a == 8 || b == 8)
                            || c > 4 && c < 8 && (a == 7 || b == 7 || a == 9 || b == 9);
                    if (pillar) {
                        if ((a == 8 || b == 8) && c == 6) continue;
                        place(out, view, worldRandom, x + a, y + c, z + b, 19, -1, cell);
                        continue;
                    }
                    place(out, view, worldRandom, x + a, y + c, z + b, 18, -1, cell);
                }
            }
        }
        for (int a = 2; a <= 14; ++a) {
            for (int b = 2; b <= 14; ++b) {
                place(out, view, worldRandom, x + a, y - 1, z + b, 1, -1, cell);
                place(out, view, worldRandom, x + a, y, z + b, 8, -1, cell);
                place(out, view, worldRandom, x + a, y + 1, z + b, 2, -1, cell);
                place(out, view, worldRandom, x + a, y + 13, z + b, 1, -1, cell);
                place(out, view, worldRandom, x + a, y + 12, z + b, 8, -1, cell);
                place(out, view, worldRandom, x + a, y + 11, z + b, 2, -1, cell);
                if (a > 1 && a < 15 && b > 1 && b < 15) {
                    int q = Math.min(Math.abs(8 - a), Math.abs(8 - b));
                    for (int g = 0; g < q - 1; ++g) {
                        place(out, view, worldRandom, x + a, y + 1 + g, z + b, 2, -1, cell);
                    }
                }
                if (a <= 3 || a >= 13 || b <= 3 || b >= 13) continue;
                int q = Math.min(Math.abs(8 - a), Math.abs(8 - b));
                for (int g = 0; g < q; ++g) {
                    place(out, view, worldRandom, x + a, y + 11 - g, z + b, 2, -1, cell);
                }
            }
        }
        for (int g = 0; g < 5; ++g) {
            place(out, view, worldRandom, x + 6 + g, y + 2, z + 4, 10, 2, cell);
            place(out, view, worldRandom, x + 6 + g, y + 2, z + 12, 10, 3, cell);
            place(out, view, worldRandom, x + 12, y + 2, z + 6 + g, 10, 5, cell);
            place(out, view, worldRandom, x + 4, y + 2, z + 6 + g, 10, 4, cell);
        }
        connections(out, view, worldRandom, cx, cz, y, cell, 3, true, true);
        out.placements.add(new Placement(x + 8, y + 2, z + 8, Kind.ELDRITCH, 3, -1));
        out.spawns.add(new Spawn(x + 8, y + 3, z + 8, SpawnKind.PERMANENT_ITEM, 2, 1, false, null));
        int guardians = OuterLandsRules.keyRoomGuardians(difficulty);
        for (int i = 0; i < guardians; ++i) {
            int offsetX = (1 + worldRandom.nextInt(3)) * (worldRandom.nextInt(3) - 1);
            int offsetZ = (1 + worldRandom.nextInt(3)) * (worldRandom.nextInt(3) - 1);
            // Oracle GenKeyRoom:110 - the guardian keeps the centre of the room as its home.
            out.spawns.add(new Spawn(x + 8 + offsetX, y + 2, z + 8 + offsetZ, SpawnKind.ELDRITCH_GUARDIAN, 0, 1,
                    OuterLandsRules.keyRoomChampion(difficulty, i), new Home(x + 8, y + 2, z + 8, 16)));
        }
    }

    /* ------------------------------------------------------------------ */
    /* The nest room                                                        */
    /* ------------------------------------------------------------------ */

    /** Oracle GenNestRoom.generateRoom:20-121. */
    public static void generateNestRoom(Builder out, View view, RandomSource random, RandomSource worldRandom,
                                        int cx, int cz, int y, MazeGenerator.Cell cell) {
        int x = cx * 16;
        int z = cz * 16;
        for (int a = 1; a <= 15; ++a) {
            for (int b = 1; b <= 15; ++b) {
                for (int c = 0; c < 11; ++c) {
                    if (a != 1 && a != 15 && b != 1 && b != 15) continue;
                    place(out, view, worldRandom, x + a, y + c, z + b, 1, -1, cell);
                }
            }
        }
        for (int a = 2; a <= 14; ++a) {
            for (int b = 2; b <= 14; ++b) {
                for (int c = 1; c < 10; ++c) {
                    if (a != 2 && a != 14 && b != 2 && b != 14) {
                        if (a == 2 && b > 3 && b < 12 && cell.west && c < 10) continue;
                        if (a == 14 && b > 3 && b < 12 && cell.east && c < 10) continue;
                        if (b == 2 && a > 3 && a < 12 && cell.north && c < 10) continue;
                        if (b == 14 && a > 3 && a < 12 && cell.south && c < 10) continue;
                    }
                    place(out, view, worldRandom, x + a, y + c, z + b, 8, -1, cell);
                }
            }
        }
        for (int a = 3; a <= 13; ++a) {
            for (int b = 3; b <= 13; ++b) {
                for (int c = 2; c < 9; ++c) {
                    if (a == 3 || a == 13 || b == 3 || b == 13) {
                        place(out, view, worldRandom, x + a, y + c, z + b, 21, -1, cell);
                    }
                    boolean open = !(a == 4 && !cell.west || a == 12 && !cell.east || b == 4 && !cell.north)
                            && (b != 12 || cell.south);
                    if (!open || !random.nextBoolean()) continue;
                    place(out, view, worldRandom, x + a, y + c, z + b, 21, -1, cell);
                }
            }
        }
        for (int a = 2; a <= 14; ++a) {
            for (int b = 2; b <= 14; ++b) {
                place(out, view, worldRandom, x + a, y - 1, z + b, 1, -1, cell);
                place(out, view, worldRandom, x + a, y, z + b, 8, -1, cell);
                place(out, view, worldRandom, x + a, y + 1, z + b, 21, -1, cell);
                place(out, view, worldRandom, x + a, y + 11, z + b, 1, -1, cell);
                place(out, view, worldRandom, x + a, y + 10, z + b, 8, -1, cell);
                place(out, view, worldRandom, x + a, y + 9, z + b, 21, -1, cell);
                if (random.nextBoolean()) {
                    place(out, view, worldRandom, x + a, y + 8, z + b, 21, -1, cell);
                    continue;
                }
                if (!random.nextBoolean()) continue;
                if (!view.isAir(x + a, y + 8, z + b)) continue;
                // Oracle GenNestRoom:66-68 - the crystal points down.
                out.placements.add(new Placement(x + a, y + 8, z + b, Kind.CRYSTAL, 7, 0));
            }
        }
        place(out, view, worldRandom, x + 8, y + 2, z + 8, 21, -1, cell);
        place(out, view, worldRandom, x + 8, y + 3, z + 8, 21, -1, cell);
        place(out, view, worldRandom, x + 8, y + 4, z + 8, 21, -1, cell);
        place(out, view, worldRandom, x + 7, y + 2, z + 8, 21, -1, cell);
        place(out, view, worldRandom, x + 8, y + 2, z + 7, 21, -1, cell);
        place(out, view, worldRandom, x + 9, y + 2, z + 8, 21, -1, cell);
        place(out, view, worldRandom, x + 8, y + 2, z + 9, 21, -1, cell);
        if (random.nextBoolean()) place(out, view, worldRandom, x + 7, y + 3, z + 8, 21, -1, cell);
        if (random.nextBoolean()) place(out, view, worldRandom, x + 8, y + 3, z + 7, 21, -1, cell);
        if (random.nextBoolean()) place(out, view, worldRandom, x + 9, y + 3, z + 8, 21, -1, cell);
        if (random.nextBoolean()) place(out, view, worldRandom, x + 8, y + 3, z + 9, 21, -1, cell);
        if (random.nextBoolean()) place(out, view, worldRandom, x + 8, y + 5, z + 8, 7, -1, cell);
        place(out, view, worldRandom, x + 8, y + 8, z + 8, 21, -1, cell);
        place(out, view, worldRandom, x + 8, y + 7, z + 8, 21, -1, cell);
        place(out, view, worldRandom, x + 8, y + 6, z + 8, 21, -1, cell);
        place(out, view, worldRandom, x + 7, y + 8, z + 8, 21, -1, cell);
        place(out, view, worldRandom, x + 8, y + 8, z + 7, 21, -1, cell);
        place(out, view, worldRandom, x + 9, y + 8, z + 8, 21, -1, cell);
        place(out, view, worldRandom, x + 8, y + 8, z + 9, 21, -1, cell);
        if (random.nextBoolean()) place(out, view, worldRandom, x + 7, y + 7, z + 8, 21, -1, cell);
        if (random.nextBoolean()) place(out, view, worldRandom, x + 8, y + 7, z + 7, 21, -1, cell);
        if (random.nextBoolean()) place(out, view, worldRandom, x + 9, y + 7, z + 8, 21, -1, cell);
        if (random.nextBoolean()) place(out, view, worldRandom, x + 8, y + 7, z + 9, 21, -1, cell);
        connections(out, view, worldRandom, cx, cz, y, cell, 3, true, true);
        for (int a = -5; a <= 5; ++a) {
            for (int b = -5; b <= 5; ++b) {
                if (!(random.nextFloat() < OuterLandsRules.NEST_LOOT_DENSITY)) continue;
                if (!view.isAir(x + 8 + a, y + 2, z + 8 + b)) continue;
                // Oracle GenNestRoom:117 - the nest room rolls its own rarity table.
                int meta = OuterLandsRules.nestLootUrnMeta(random.nextFloat());
                Kind kind = OuterLandsRules.nestLootIsCrate(random.nextFloat()) ? Kind.LOOT_CRATE : Kind.LOOT_URN;
                out.placements.add(new Placement(x + 8 + a, y + 2, z + 8 + b, kind, meta, -1));
            }
        }
    }

    /* ------------------------------------------------------------------ */
    /* The library room                                                     */
    /* ------------------------------------------------------------------ */

    /** Oracle GenLibraryRoom.generateRoom:19-100. */
    public static void generateLibraryRoom(Builder out, View view, RandomSource random, RandomSource worldRandom,
                                           int cx, int cz, int y, MazeGenerator.Cell cell) {
        int x = cx * 16;
        int z = cz * 16;
        for (int a = 1; a <= 15; ++a) {
            for (int b = 1; b <= 15; ++b) {
                for (int c = 0; c < 13; ++c) {
                    if (a != 1 && a != 15 && b != 1 && b != 15) continue;
                    place(out, view, worldRandom, x + a, y + c, z + b, 1, -1, cell);
                }
            }
        }
        for (int a = 2; a <= 14; ++a) {
            for (int b = 2; b <= 14; ++b) {
                for (int c = 1; c < 12; ++c) {
                    if (a != 2 && a != 14 && b != 2 && b != 14) {
                        if (a == 2 && b > 3 && b < 12 && cell.west && c < 10) continue;
                        if (a == 14 && b > 3 && b < 12 && cell.east && c < 10) continue;
                        if (b == 2 && a > 3 && a < 12 && cell.north && c < 10) continue;
                        if (b == 14 && a > 3 && a < 12 && cell.south && c < 10) continue;
                    }
                    place(out, view, worldRandom, x + a, y + c, z + b, 8, -1, cell);
                }
            }
        }
        for (int a = 3; a <= 13; ++a) {
            for (int b = 3; b <= 13; ++b) {
                for (int c = 2; c < 11; ++c) {
                    if (a != 3 && a != 13 && b != 3 && b != 13) continue;
                    place(out, view, worldRandom, x + a, y + c, z + b, 2, -1, cell);
                }
            }
        }
        for (int a = 2; a <= 14; ++a) {
            for (int b = 2; b <= 14; ++b) {
                place(out, view, worldRandom, x + a, y - 1, z + b, 1, -1, cell);
                place(out, view, worldRandom, x + a, y, z + b, 8, -1, cell);
                place(out, view, worldRandom, x + a, y + 1, z + b, 2, -1, cell);
                place(out, view, worldRandom, x + a, y + 12, z + b, 1, -1, cell);
                place(out, view, worldRandom, x + a, y + 11, z + b, 8, -1, cell);
                place(out, view, worldRandom, x + a, y + 10, z + b, 2, -1, cell);
                if (a <= 3 || a >= 13 || b <= 3 || b >= 13) continue;
                if (a <= 5 && b <= 5 || a <= 5 && b >= 11 || a >= 11 && b <= 5 || a >= 11 && b >= 11) {
                    place(out, view, worldRandom, x + a, y + 2, z + b, 2, -1, cell);
                    place(out, view, worldRandom, x + a, y + 9, z + b, 2, -1, cell);
                }
                boolean corner = a == 5 && b == 5 || a == 5 && b == 11 || a == 11 && b == 5 || a == 11 && b == 11;
                if (!corner) continue;
                out.placements.add(new Placement(x + a, y + 3, z + b, Kind.COSMETIC_SOLID, 15, -1));
                out.placements.add(new Placement(x + a, y + 8, z + b, Kind.COSMETIC_SOLID, 15, -1));
            }
        }
        for (int g = 0; g < 5; ++g) {
            place(out, view, worldRandom, x + 6 + g, y + 2, z + 4, 10, 2, cell);
            place(out, view, worldRandom, x + 6 + g, y + 2, z + 12, 10, 3, cell);
            place(out, view, worldRandom, x + 12, y + 2, z + 6 + g, 10, 5, cell);
            place(out, view, worldRandom, x + 4, y + 2, z + 6 + g, 10, 4, cell);
            place(out, view, worldRandom, x + 6 + g, y + 9, z + 4, 11, 2, cell);
            place(out, view, worldRandom, x + 6 + g, y + 9, z + 12, 11, 3, cell);
            place(out, view, worldRandom, x + 12, y + 9, z + 6 + g, 11, 5, cell);
            place(out, view, worldRandom, x + 4, y + 9, z + 6 + g, 11, 4, cell);
        }
        libraryDesk(out, x + 5, y + 4, z + 5, false);
        libraryDesk(out, x + 5, y + 4, z + 11, false);
        libraryDesk(out, x + 11, y + 4, z + 5, false);
        libraryDesk(out, x + 11, y + 4, z + 11, false);
        libraryDesk(out, x + 5, y + 7, z + 5, true);
        libraryDesk(out, x + 5, y + 7, z + 11, true);
        libraryDesk(out, x + 11, y + 7, z + 5, true);
        libraryDesk(out, x + 11, y + 7, z + 11, true);
        out.placements.add(new Placement(x + 8, y + 2, z + 8, Kind.COSMETIC_SOLID, 15, -1));
        libraryDesk(out, x + 8, y + 3, z + 8, false);
        out.placements.add(new Placement(x + 8, y + 9, z + 8, Kind.COSMETIC_SOLID, 15, -1));
        libraryDesk(out, x + 8, y + 8, z + 8, true);
        connections(out, view, worldRandom, cx, cz, y, cell, 3, true, true);
    }

    /**
     * Oracle GenLibraryRoom:77-96 - a desk is an eldritch block over a stone slab.
     *
     * <p>The lower desks carry the slab above them, the upper desks carry it below them as the
     * upper half of the slab, exactly like the original metadata one and nine.
     *
     * @param top true for the upper desks, whose slab hangs below the eldritch block
     */
    private static void libraryDesk(Builder out, int x, int y, int z, boolean top) {
        out.placements.add(new Placement(x, y, z, Kind.ELDRITCH, 5, -1));
        out.placements.add(new Placement(x, top ? y - 1 : y + 1, z, Kind.COSMETIC_SLAB_STONE, top ? 9 : 1, -1));
    }

    /* ------------------------------------------------------------------ */
    /* The 2x2 boss room                                                    */
    /* ------------------------------------------------------------------ */

    /**
     * Oracle GenBossRoom.generateRoom:24-87. The four quadrant features build the four chunks of
     * the boss room; every chunk places its own quadrant and the doorway of its own side.
     */
    public static void generateBossRoom(Builder out, View view, RandomSource random, RandomSource worldRandom,
                                        int cx, int cz, int y, MazeGenerator.Cell cell) {
        int x = cx * 16;
        int z = cz * 16;
        switch (cell.feature) {
            case OuterLandsRules.FEATURE_BOSS_UPPER_LEFT -> generate2x2UpperLeft(out, view, worldRandom, cx, cz, y, cell);
            case OuterLandsRules.FEATURE_BOSS_UPPER_RIGHT -> generate2x2UpperRight(out, view, worldRandom, cx, cz, y, cell);
            case OuterLandsRules.FEATURE_BOSS_LOWER_LEFT -> generate2x2LowerLeft(out, view, worldRandom, cx, cz, y, cell);
            case OuterLandsRules.FEATURE_BOSS_LOWER_RIGHT -> generate2x2LowerRight(out, view, worldRandom, cx, cz, y, cell);
            default -> {
            }
        }
        for (int a = 0; a < 7; ++a) {
            for (int b = 0; b < 7; ++b) {
                int xx = 0;
                int zz = 0;
                // Oracle GenBossRoom:48 - the doorway starts with the unknown direction and the
                // last side of the cell that has an exit wins, exactly like the original.
                int facing = OuterLandsRules.DIRECTION_UNKNOWN;
                if (cell.north) {
                    xx = x + 5 + a;
                    zz = z + 3;
                    facing = 2;
                }
                if (cell.south) {
                    xx = x + 5 + a;
                    zz = z + 13;
                    facing = 3;
                }
                if (cell.east) {
                    xx = x + 13;
                    zz = z + 5 + a;
                    facing = 5;
                }
                if (cell.west) {
                    xx = x + 3;
                    zz = z + 5 + a;
                    facing = 4;
                }
                if (facing == OuterLandsRules.DIRECTION_UNKNOWN) {
                    // Deviation: the original keeps the coordinates zero and writes the doorway
                    // of a boss room quadrant without an exit into the world origin, which is
                    // outside of the chunk that is generated in 1.21.1 and would abort the
                    // generation of the chunk. The doorway keeps the corner of its own chunk.
                    xx = x;
                    zz = z;
                }
                switch (PAT_DOORWAY[a][b]) {
                    // The lock takes the direction of the side of the room it stands on
                    // (oracle GenBossRoom:72-75), the door and the invisible door do not.
                    case 1 -> place(out, view, worldRandom, xx, y + 2 + b, zz, 16, facing, cell);
                    case 2 -> place(out, view, worldRandom, xx, y + 2 + b, zz, 15, OuterLandsRules.DIRECTION_UNKNOWN, cell);
                    case 9 -> place(out, view, worldRandom, xx, y + 2 + b, zz, 17, OuterLandsRules.DIRECTION_UNKNOWN, cell);
                    default -> {
                    }
                }
            }
        }
    }

    /** Oracle Gen2x2.generateUpperLeft:18-68. */
    public static void generate2x2UpperLeft(Builder out, View view, RandomSource worldRandom,
                                             int cx, int cz, int y, MazeGenerator.Cell cell) {
        int x = cx * 16;
        int z = cz * 16;
        for (int a = 1; a <= 15; ++a) {
            for (int b = 1; b <= 15; ++b) {
                for (int c = 0; c < 13; ++c) {
                    if (a != 1 && b != 1) continue;
                    place(out, view, worldRandom, x + a, y + c, z + b, 1, -1, cell);
                }
            }
        }
        for (int a = 2; a <= 15; ++a) {
            for (int b = 2; b <= 15; ++b) {
                for (int c = 1; c < 12; ++c) {
                    if (a != 2 && b != 2) continue;
                    if (a == 2 && b > 4 && b < 12 && cell.west && c < 10) continue;
                    if (b == 2 && a > 4 && a < 12 && cell.north && c < 10) continue;
                    place(out, view, worldRandom, x + a, y + c, z + b, 8, -1, cell);
                }
            }
        }
        for (int a = 3; a <= 15; ++a) {
            for (int b = 3; b <= 15; ++b) {
                for (int c = 2; c < 11; ++c) {
                    if (a != 3 && b != 3) continue;
                    place(out, view, worldRandom, x + a, y + c, z + b, 18, -1, cell);
                }
            }
        }
        for (int a = 2; a <= 15; ++a) {
            for (int b = 2; b <= 15; ++b) {
                place(out, view, worldRandom, x + a, y - 1, z + b, 1, -1, cell);
                place(out, view, worldRandom, x + a, y, z + b, 8, -1, cell);
                place(out, view, worldRandom, x + a, y + 1, z + b, 19, -1, cell);
                place(out, view, worldRandom, x + a, y + 13, z + b, 1, -1, cell);
                place(out, view, worldRandom, x + a, y + 12, z + b, 8, -1, cell);
                place(out, view, worldRandom, x + a, y + 11, z + b, 2, -1, cell);
            }
        }
        for (int g = 4; g <= 15; ++g) {
            place(out, view, worldRandom, x + g, y + 2, z + 4, 10, 2, cell);
            place(out, view, worldRandom, x + g, y + 10, z + 4, 11, 2, cell);
        }
        for (int g = 4; g <= 15; ++g) {
            place(out, view, worldRandom, x + 4, y + 2, z + g, 10, 4, cell);
            place(out, view, worldRandom, x + 4, y + 10, z + g, 11, 4, cell);
        }
        connections(out, view, worldRandom, cx, cz, y, cell, 3, true, true);
    }

    /** Oracle Gen2x2.generateUpperRight:70-119. */
    public static void generate2x2UpperRight(Builder out, View view, RandomSource worldRandom,
                                              int cx, int cz, int y, MazeGenerator.Cell cell) {
        int x = cx * 16;
        int z = cz * 16;
        for (int a = 0; a <= 15; ++a) {
            for (int b = 1; b <= 15; ++b) {
                for (int c = 0; c < 13; ++c) {
                    if (a != 15 && b != 1) continue;
                    place(out, view, worldRandom, x + a, y + c, z + b, 1, -1, cell);
                }
            }
        }
        for (int a = 0; a <= 14; ++a) {
            for (int b = 2; b <= 15; ++b) {
                for (int c = 1; c < 12; ++c) {
                    if (a != 14 && b != 2) continue;
                    if (a == 14 && b > 4 && b < 12 && cell.east && c < 10) continue;
                    if (b == 2 && a > 4 && a < 12 && cell.north && c < 10) continue;
                    place(out, view, worldRandom, x + a, y + c, z + b, 8, -1, cell);
                }
            }
        }
        for (int a = 0; a <= 13; ++a) {
            for (int b = 3; b <= 15; ++b) {
                for (int c = 2; c < 11; ++c) {
                    if (a != 13 && b != 3) continue;
                    place(out, view, worldRandom, x + a, y + c, z + b, 18, -1, cell);
                }
            }
        }
        for (int a = 0; a <= 14; ++a) {
            for (int b = 2; b <= 15; ++b) {
                place(out, view, worldRandom, x + a, y - 1, z + b, 1, -1, cell);
                place(out, view, worldRandom, x + a, y, z + b, 8, -1, cell);
                place(out, view, worldRandom, x + a, y + 1, z + b, 19, -1, cell);
                place(out, view, worldRandom, x + a, y + 13, z + b, 1, -1, cell);
                place(out, view, worldRandom, x + a, y + 12, z + b, 8, -1, cell);
                place(out, view, worldRandom, x + a, y + 11, z + b, 2, -1, cell);
            }
        }
        for (int g = 0; g <= 11; ++g) {
            place(out, view, worldRandom, x + g, y + 2, z + 4, 10, 2, cell);
            place(out, view, worldRandom, x + g, y + 10, z + 4, 11, 2, cell);
        }
        for (int g = 4; g <= 15; ++g) {
            place(out, view, worldRandom, x + 12, y + 2, z + g, 10, 5, cell);
            place(out, view, worldRandom, x + 12, y + 10, z + g, 11, 5, cell);
        }
        connections(out, view, worldRandom, cx, cz, y, cell, 3, true, true);
    }

    /** Oracle Gen2x2.generateLowerLeft:122-171. */
    public static void generate2x2LowerLeft(Builder out, View view, RandomSource worldRandom,
                                             int cx, int cz, int y, MazeGenerator.Cell cell) {
        int x = cx * 16;
        int z = cz * 16;
        for (int a = 1; a <= 15; ++a) {
            for (int b = 0; b <= 15; ++b) {
                for (int c = 0; c < 13; ++c) {
                    if (a != 1 && b != 15) continue;
                    place(out, view, worldRandom, x + a, y + c, z + b, 1, -1, cell);
                }
            }
        }
        for (int a = 2; a <= 15; ++a) {
            for (int b = 0; b <= 14; ++b) {
                for (int c = 1; c < 12; ++c) {
                    if (a != 2 && b != 14) continue;
                    if (a == 2 && b > 4 && b < 12 && cell.west && c < 10) continue;
                    if (b == 14 && a > 4 && a < 12 && cell.south && c < 10) continue;
                    place(out, view, worldRandom, x + a, y + c, z + b, 8, -1, cell);
                }
            }
        }
        for (int a = 3; a <= 15; ++a) {
            for (int b = 0; b <= 13; ++b) {
                for (int c = 2; c < 11; ++c) {
                    if (a != 3 && b != 13) continue;
                    place(out, view, worldRandom, x + a, y + c, z + b, 18, -1, cell);
                }
            }
        }
        for (int a = 2; a <= 15; ++a) {
            for (int b = 0; b <= 14; ++b) {
                place(out, view, worldRandom, x + a, y - 1, z + b, 1, -1, cell);
                place(out, view, worldRandom, x + a, y, z + b, 8, -1, cell);
                place(out, view, worldRandom, x + a, y + 1, z + b, 19, -1, cell);
                place(out, view, worldRandom, x + a, y + 13, z + b, 1, -1, cell);
                place(out, view, worldRandom, x + a, y + 12, z + b, 8, -1, cell);
                place(out, view, worldRandom, x + a, y + 11, z + b, 2, -1, cell);
            }
        }
        for (int g = 4; g <= 15; ++g) {
            place(out, view, worldRandom, x + g, y + 2, z + 12, 10, 3, cell);
            place(out, view, worldRandom, x + g, y + 10, z + 12, 11, 3, cell);
        }
        for (int g = 0; g <= 11; ++g) {
            place(out, view, worldRandom, x + 4, y + 2, z + g, 10, 4, cell);
            place(out, view, worldRandom, x + 4, y + 10, z + g, 11, 4, cell);
        }
        connections(out, view, worldRandom, cx, cz, y, cell, 3, true, true);
    }

    /** Oracle Gen2x2.generateLowerRight:174-224. */
    public static void generate2x2LowerRight(Builder out, View view, RandomSource worldRandom,
                                              int cx, int cz, int y, MazeGenerator.Cell cell) {
        int x = cx * 16;
        int z = cz * 16;
        for (int a = 0; a <= 15; ++a) {
            for (int b = 0; b <= 15; ++b) {
                for (int c = 0; c < 13; ++c) {
                    if (a != 15 && b != 15) continue;
                    place(out, view, worldRandom, x + a, y + c, z + b, 1, -1, cell);
                }
            }
        }
        for (int a = 0; a <= 14; ++a) {
            for (int b = 0; b <= 14; ++b) {
                for (int c = 1; c < 12; ++c) {
                    if (a != 14 && b != 14) continue;
                    if (a == 14 && b > 4 && b < 12 && cell.east && c < 10) continue;
                    if (b == 14 && a > 4 && a < 12 && cell.south && c < 10) continue;
                    place(out, view, worldRandom, x + a, y + c, z + b, 8, -1, cell);
                }
            }
        }
        for (int a = 0; a <= 13; ++a) {
            for (int b = 0; b <= 13; ++b) {
                for (int c = 2; c < 11; ++c) {
                    if (a != 13 && b != 13) continue;
                    place(out, view, worldRandom, x + a, y + c, z + b, 18, -1, cell);
                }
            }
        }
        for (int a = 0; a <= 14; ++a) {
            for (int b = 0; b <= 14; ++b) {
                place(out, view, worldRandom, x + a, y - 1, z + b, 1, -1, cell);
                place(out, view, worldRandom, x + a, y, z + b, 8, -1, cell);
                place(out, view, worldRandom, x + a, y + 1, z + b, 19, -1, cell);
                place(out, view, worldRandom, x + a, y + 13, z + b, 1, -1, cell);
                place(out, view, worldRandom, x + a, y + 12, z + b, 8, -1, cell);
                place(out, view, worldRandom, x + a, y + 11, z + b, 2, -1, cell);
            }
        }
        for (int g = 0; g <= 11; ++g) {
            place(out, view, worldRandom, x + g, y + 2, z + 12, 10, 3, cell);
            place(out, view, worldRandom, x + g, y + 10, z + 12, 11, 3, cell);
        }
        for (int g = 0; g <= 12; ++g) {
            place(out, view, worldRandom, x + 12, y + 2, z + g, 10, 5, cell);
            place(out, view, worldRandom, x + 12, y + 10, z + g, 11, 5, cell);
        }
        connections(out, view, worldRandom, cx, cz, y, cell, 3, true, true);
    }
}
