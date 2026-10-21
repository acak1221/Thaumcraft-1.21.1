package thaumcraft.common.lib.world;

import java.util.List;

/**
 * Pure rules of the TC4 Outer Lands (the "Eldritch" dimension).
 *
 * Oracle sources:
 * <ul>
 *   <li>{@code thaumcraft/common/lib/world/dim/WorldProviderOuter.java:30-133} - dimension identity,
 *       sky light, celestial angle, fog colour and cloud height.</li>
 *   <li>{@code thaumcraft/common/lib/world/dim/ChunkProviderOuter.java:38-127} - the void chunk
 *       provider and its single eldritch biome.</li>
 *   <li>{@code thaumcraft/common/lib/world/biomes/BiomeGenEldritch.java:23-50} - the Eldritch biome
 *       (top/filler block, spawn lists, sky colour, empty decoration).</li>
 *   <li>{@code thaumcraft/common/lib/world/dim/MazeHandler.java:152-186} - the cell feature dispatch.</li>
 *   <li>{@code thaumcraft/common/lib/world/dim/GenKeyRoom.java:31-116} - key room contents.</li>
 *   <li>{@code thaumcraft/common/lib/world/dim/GenCommon.java:51-325} - decoration rules.</li>
 *   <li>{@code thaumcraft/common/config/Config.java:271-272,835-873} - dimension id and generation
 *       defaults (the Outer Lands uses {@code outer_lands_dim = -42}).</li>
 * </ul>
 *
 * Every value here is a decision, not a side effect, so it can be unit tested
 * without a running world.
 */
public final class OuterLandsRules {
    private OuterLandsRules() {}

    /** Original 1.7 dimension id (Config.outer_lands_dim default). */
    public static final int DIMENSION_ID = -42;
    /** Original 1.7 save folder (legacy port WorldProviderOuter.getSaveFolder). */
    public static final String SAVE_FOLDER = "DIM_OUTERLANDS";
    public static final String NAME = "The Outer Lands";
    public static final String WELCOME_MESSAGE = "Entering The Outer Lands";
    public static final String DEPART_MESSAGE = "Leaving The Outer Lands";

    /** WorldProviderOuter.func_76557_i - the average ground level of the dimension. */
    public static final int AVERAGE_GROUND_LEVEL = 50;
    /** The labyrinth is generated at Y=50 (MazeHandler.generateEldritch passes 50). */
    public static final int MAZE_FLOOR_Y = 50;
    /** WorldProviderOuter.func_76571_f - cloud height. */
    public static final float CLOUD_HEIGHT = 1.0F;
    /** WorldProviderOuter.func_76563_a - the sun never moves in the Outer Lands. */
    public static final float CELESTIAL_ANGLE = 0.0F;

    /** WorldProviderOuter.func_76562_b - the dimension fog colour before the brightness scale. */
    public static final int FOG_COLOR = 0xA080A0;
    /** WorldProviderOuter.func_76562_b multiplies the fog colour by 0.15. */
    public static final float FOG_BRIGHTNESS = 0.15F;
    /** BiomeGenEldritch.func_76731_a - the Outer Lands sky is black. */
    public static final int SKY_COLOR = 0x000000;
    /**
     * The 1.21 grass colour of the Eldritch biome.
     *
     * <p>Oracle {@code BiomeGenEldritch:38-42} only overrides the sky colour and makes it
     * black, so the grass colour stays the default biome colour of {@code BiomeGenBase}.
     */
    public static final int GRASS_COLOR = 0x000000;
    /** The 1.21 foliage colour of the Eldritch biome, the default biome colour as well. */
    public static final int FOLIAGE_COLOR = 0x000000;
    /** BiomeGenBase default water colour and water fog colour. */
    public static final int WATER_COLOR = 0x3F76E4;
    public static final int WATER_FOG_COLOR = 329011;
    /**
     * The 1.21 biome fog colour of the Eldritch biome.
     *
     * <p>Oracle {@code BiomeGenEldritch:38-42} only overrides the sky colour and makes it
     * black; the 1.7 client used that single colour for the fog as well. The value is the
     * one of the native biome document
     * {@code data/thaumcraft/worldgen/biome/eldritch_lands.json} and of
     * {@link thaumcraft.common.lib.world.biomes.BiomeSurfaceRules#fogColour}.
     */
    public static final int BIOME_FOG_COLOR = 0x000000;

    public static final float BIOME_TEMPERATURE = 0.5F;
    public static final float BIOME_DOWNFALL = 0.5F;
    /** The Outer Lands never precipitates (WorldProviderOuter.canDoRainSnowIce). */
    public static final boolean BIOME_PRECIPITATION = false;
    /** WorldProviderOuter.func_76572_b sets hasNoSky; the dimension has no sky light at all. */
    public static final boolean HAS_SKY_LIGHT = false;
    /** WorldProviderOuter.func_76569_d - the Outer Lands is not a surface world. */
    public static final boolean SURFACE_WORLD = false;
    /** WorldProviderOuter.func_76567_e - respawning in the Outer Lands is refused. */
    public static final boolean RESPAWN_ALLOWED = false;
    /** WorldProviderOuter.func_76568_b - the Outer Lands always shows fog. */
    public static final boolean FOG_ENABLED = true;
    /** WorldProviderOuter.func_76561_g - the sky is never sky coloured here. */
    public static final boolean SKY_COLOURED = false;

    /** The 1.21 dimension stack: 0..255 with the logical height of the whole column. */
    public static final int MIN_Y = 0;
    public static final int HEIGHT = 256;
    public static final int LOGICAL_HEIGHT = 256;

    /** The 1.7 world height of the Outer Lands (1.7 worlds are 256 tall). */
    public static final int LEGACY_WORLD_HEIGHT = 256;

    /* ------------------------------------------------------------------ */
    /* Cell feature markers (oracle MazeGenerator / Cell)                 */
    /* ------------------------------------------------------------------ */

    /** Feature 1: the portal cell in the middle of the labyrinth. */
    public static final int FEATURE_PORTAL = 1;
    /** Features 2..5: the four quadrants of the 2x2 boss room. */
    public static final int FEATURE_BOSS_UPPER_LEFT = 2;
    public static final int FEATURE_BOSS_UPPER_RIGHT = 3;
    public static final int FEATURE_BOSS_LOWER_LEFT = 4;
    public static final int FEATURE_BOSS_LOWER_RIGHT = 5;
    /** Feature 6: the key room. */
    public static final int FEATURE_KEY_ROOM = 6;
    /** Feature 7: the nest room. */
    public static final int FEATURE_NEST_ROOM = 7;
    /** Feature 8: the library room. */
    public static final int FEATURE_LIBRARY_ROOM = 8;
    /** Feature 9: a dead end without a special room. */
    public static final int FEATURE_DEAD_END = 9;
    /** Feature 10: a plain decorated passage. */
    public static final int FEATURE_PASSAGE = 10;
    /** Feature 11: a passage with trapped stone. */
    public static final int FEATURE_TRAPPED_PASSAGE = 11;
    /** Feature 12: a passage with a taint crust. */
    public static final int FEATURE_CRUST_PASSAGE = 12;
    /** Feature 13: a passage overgrown with taint fibres. */
    public static final int FEATURE_TAINT_PASSAGE = 13;
    /** Feature 14: a passage with mind spiders. */
    public static final int FEATURE_SPIDER_PASSAGE = 14;

    /** The rooms the oracle dispatches to (MazeHandler.generateEldritch:152-186). */
    public enum RoomKind {
        PORTAL,
        BOSS,
        KEY_ROOM,
        NEST_ROOM,
        LIBRARY_ROOM,
        PASSAGE
    }

    /**
     * The 1.7 {@code ForgeDirection.UNKNOWN} ordinal.
     *
     * <p>Oracle {@code GenBossRoom:48} starts the doorway of a boss room with the unknown
     * direction and only replaces it when the cell has an exit on that side, so the lock of
     * a boss room quadrant without any exit keeps the unknown direction.
     */
    public static final int DIRECTION_UNKNOWN = 6;

    /**
     * Oracle MazeHandler.generateEldritch:152-186. Feature 1 is the portal, 2..5 the boss
     * room quadrants, 6 the key room, 7 the nest room, 8 the library room and everything
     * else (including 0, 9, 10..14) is a passage.
     */
    public static RoomKind roomForFeature(int feature) {
        return switch (feature) {
            case FEATURE_PORTAL -> RoomKind.PORTAL;
            case FEATURE_BOSS_UPPER_LEFT, FEATURE_BOSS_UPPER_RIGHT, FEATURE_BOSS_LOWER_LEFT, FEATURE_BOSS_LOWER_RIGHT -> RoomKind.BOSS;
            case FEATURE_KEY_ROOM -> RoomKind.KEY_ROOM;
            case FEATURE_NEST_ROOM -> RoomKind.NEST_ROOM;
            case FEATURE_LIBRARY_ROOM -> RoomKind.LIBRARY_ROOM;
            default -> RoomKind.PASSAGE;
        };
    }

    /** True when the feature marks one of the four 2x2 boss room quadrants. */
    public static boolean isBossQuadrant(int feature) {
        return feature >= FEATURE_BOSS_UPPER_LEFT && feature <= FEATURE_BOSS_LOWER_RIGHT;
    }

    /**
     * The boss room quadrant of a boss feature: 0 upper left, 1 upper right,
     * 2 lower left, 3 lower right (oracle GenBossRoom.generateRoom:27-43).
     */
    public static int bossQuadrant(int feature) {
        return switch (feature) {
            case FEATURE_BOSS_UPPER_LEFT -> 0;
            case FEATURE_BOSS_UPPER_RIGHT -> 1;
            case FEATURE_BOSS_LOWER_LEFT -> 2;
            case FEATURE_BOSS_LOWER_RIGHT -> 3;
            default -> -1;
        };
    }

    /* ------------------------------------------------------------------ */
    /* Room shells                                                          */
    /* ------------------------------------------------------------------ */

    /** Every room is inset one block from the chunk edge (oracle a/b loops 1..15). */
    public static final int ROOM_INSET = 1;
    /** The room shell is 15x15 blocks wide (oracle a/b loops 1..15 inclusive). */
    public static final int ROOM_WIDTH = 15;
    /** The portal, key and library rooms are 13 blocks tall (oracle c loop 0..12). */
    public static final int ROOM_HEIGHT_FULL = 13;
    /** The nest room is 11 blocks tall (oracle c loop 0..10). */
    public static final int ROOM_HEIGHT_NEST = 11;
    /** The 2x2 boss room spans four chunks; each quadrant is 16 wide. */
    public static final int BOSS_QUADRANT_SIZE = 16;

    /** The wall layer of a full room: the shell floor sits one below the room base. */
    public static final int ROOM_FLOOR_OFFSET = -1;
    /** The ceiling layer of a full room. */
    public static final int ROOM_CEILING_OFFSET = 12;

    /** The key room guardians scale with the difficulty (oracle GenKeyRoom:102-115). */
    public static int keyRoomGuardians(int difficulty) {
        return switch (difficulty) {
            case 0 -> 2;  // peaceful
            case 1 -> 2;  // easy
            case 2 -> 3;  // normal
            default -> 4; // hard
        };
    }

    /** The first guardian of a hard key room is a champion (oracle GenKeyRoom:111-113). */
    public static boolean keyRoomChampion(int difficulty, int guardianIndex) {
        return guardianIndex == 0 && keyRoomGuardians(difficulty) >= 4;
    }

    /* ------------------------------------------------------------------ */
    /* Decoration rules (oracle GenCommon.processDecorations)              */
    /* ------------------------------------------------------------------ */

    /** The oracle rolls a loot urn rarity: 2.5% rich, 7.5% common, 90% plain. */
    public static int lootUrnMeta(float roll) {
        if (roll < 0.025F) return 2;
        if (roll < 0.1F) return 1;
        return 0;
    }

    /**
     * The nest room rolls its own rarity: 15% rich, 25% common, 60% plain.
     *
     * <p>Oracle {@code GenNestRoom:116-118}: the nest room draws its own rarity table and
     * not the rarity of the common decoration of {@code GenCommon:284-285}, so the nest is
     * much richer than the rest of the labyrinth.
     */
    public static int nestLootUrnMeta(float roll) {
        if (roll < 0.15F) return 2;
        if (roll < 0.4F) return 1;
        return 0;
    }

    /** The nest room loot: 20% crates, otherwise urns (oracle GenNestRoom:113-120). */
    public static boolean nestLootIsCrate(float roll) {
        return roll < 0.2F;
    }

    /** The nest room loot density: 15% of the 11x11 floor (oracle GenNestRoom:115). */
    public static final float NEST_LOOT_DENSITY = 0.15F;

    /** The common decoration: 2/3 eldritch meta 4, 7/24 meta 5, 1/24 meta 10. */
    public static int commonDecorationMeta(int firstRoll, int secondRoll) {
        if (firstRoll != 0) return 4;
        return secondRoll != 0 ? 5 : 10;
    }

    /** The chance that a common decoration also grows a crystal (oracle GenCommon:293). */
    public static final int CRYSTAL_DECORATION_CHANCE = 12;

    /** The taint fibres in a taint passage: one in four is the "grown" variant. */
    public static int taintFibreMeta(int roll) {
        return roll == 0 ? 1 : 0;
    }

    /* ------------------------------------------------------------------ */
    /* Aura / vis nodes                                                     */
    /* ------------------------------------------------------------------ */

    /**
     * The original never runs the surface generator inside the Outer Lands
     * (ThaumcraftWorldGenerator.worldGeneration:125-146), so the dimension holds
     * no wild aura nodes, no totems and no eldritch ring structures.
     */
    public static boolean generatesAuraNodes() {
        return false;
    }

    /** The Outer Lands has no natural light source at all. */
    public static final float AMBIENT_LIGHT = 0.0F;

    /** The eldritch biome spawns: only two monsters, one weight each, never a group. */
    public static final List<String> MONSTER_SPAWNS = List.of("thaumcraft:inhabited_zombie", "thaumcraft:eldritch_guardian");
    /** The Eldritch biome clears the creature, water creature and cave creature lists. */
    public static final List<String> PASSIVE_SPAWNS = List.of();

    /** The monster spawn chance of the eldritch biome (BiomeGenBase default). */
    public static final float CREATURE_SPAWN_PROBABILITY = 0.1F;

    /** The packed 1.21 fog colour of the dimension: 0xA080A0 scaled by 0.15. */
    public static int packedFogColor() {
        int r = (int) (((FOG_COLOR >> 16) & 0xFF) / 255.0F * FOG_BRIGHTNESS * 255.0F);
        int g = (int) (((FOG_COLOR >> 8) & 0xFF) / 255.0F * FOG_BRIGHTNESS * 255.0F);
        int b = (int) ((FOG_COLOR & 0xFF) / 255.0F * FOG_BRIGHTNESS * 255.0F);
        return (r << 16) | (g << 8) | b;
    }

    /** True when the given native dimension key is the Outer Lands. */
    public static boolean isOuterLands(String namespace, String path) {
        return "thaumcraft".equals(namespace) && "outer_lands".equals(path);
    }
}
