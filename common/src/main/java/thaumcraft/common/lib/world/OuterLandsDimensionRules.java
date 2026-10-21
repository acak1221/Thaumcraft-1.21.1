package thaumcraft.common.lib.world;

/**
 * The pure rules of the TC4 Outer Lands dimension and of its registration in Minecraft 1.21.1.
 *
 * <p>Minecraft 1.21.1 keeps the level stems in the {@code minecraft:level_stem} registry and
 * that registry is a data pack registry: {@code RegistryDataLoader.DIMENSION_REGISTRIES} loads
 * it from the {@code dimension} folder of every data pack, because the folder name is
 * {@code Registries.elementsDirPath(Registries.LEVEL_STEM)}. The level stem of the Outer Lands
 * therefore stays the data pack resource {@code data/thaumcraft/dimension/outer_lands.json}, the
 * server creates a level for every entry of the registry
 * ({@code MinecraftServer.createLevels}) and {@code WorldDimensions.bake} merges the data pack
 * registry into the dimension list of the world, so even a world that was created before the mod
 * receives the dimension.
 *
 * <p>The dimension type itself is the data pack resource
 * {@code data/thaumcraft/dimension_type/outer_lands.json}. It is loaded into the worldgen layer
 * of the registry access, which is read before the level stems, so the level stem can name it.
 *
 * <p>Oracle sources:
 * <ul>
 *   <li>{@code thaumcraft/common/lib/world/dim/WorldProviderOuter.java:30-133} - the dimension
 *       identity, the missing sky light, the fixed celestial angle and the fog colour.</li>
 *   <li>{@code thaumcraft/common/lib/world/dim/ChunkProviderOuter.java:38-127} - the void chunk
 *       provider and its single Eldritch biome.</li>
 *   <li>{@code thaumcraft/common/lib/world/dim/TeleporterThaumcraft.java:57-112} - the gate
 *       search and the landing one block beside the gate.</li>
 * </ul>
 */
public final class OuterLandsDimensionRules {
    private OuterLandsDimensionRules() {}

    /** The registry id of the Outer Lands level stem. */
    public static final String LEVEL_STEM_ID = "thaumcraft:outer_lands";
    /** The registry id of the Outer Lands dimension type. */
    public static final String DIMENSION_TYPE_ID = "thaumcraft:outer_lands";
    /** The registry id of the single biome of the Outer Lands. */
    public static final String BIOME_ID = "thaumcraft:eldritch_lands";
    /** The registry id of the dimension effects of the Outer Lands. */
    public static final String EFFECTS_ID = "thaumcraft:outer_lands";

    /**
     * The data pack folder of the level stems.
     *
     * <p>{@code Registries.elementsDirPath(Registries.LEVEL_STEM)} returns the path of the
     * registry key, and the key of the level stem registry is {@code minecraft:dimension}.
     */
    public static final String LEVEL_STEM_FOLDER = "dimension";
    /** The data pack resource that carries the level stem of the Outer Lands. */
    public static final String LEVEL_STEM_RESOURCE = "data/thaumcraft/dimension/outer_lands.json";
    /** The data pack resource that carries the dimension type of the Outer Lands. */
    public static final String DIMENSION_TYPE_RESOURCE = "data/thaumcraft/dimension_type/outer_lands.json";
    /** The data pack resource that carries the single biome of the Outer Lands. */
    public static final String BIOME_RESOURCE = "data/thaumcraft/worldgen/biome/eldritch_lands.json";

    /** The generator of the Outer Lands: the flat generator that reproduces the void chunks. */
    public static final String GENERATOR_ID = "minecraft:flat";
    /** The block of the single layer of the flat generator. */
    public static final String LAYER_BLOCK_ID = "minecraft:air";
    /** The height of the single layer of the flat generator. */
    public static final int LAYER_HEIGHT = 1;

    /**
     * The original values of the dimension type of the Outer Lands.
     *
     * <p>They are the values of the oracle dimension provider: no sky light at all, no ambient
     * light, the 256 block tall 1.7 world, a fixed time of zero because the sun never moves and
     * a coordinate scale of one because the labyrinth keeps its place in the overworld.
     */
    public record DimensionTypeValues(boolean hasSkyLight, float ambientLight, int height, int minY,
                                      int logicalHeight, long fixedTime, double coordinateScale,
                                      boolean bedWorks, boolean respawnAnchorWorks,
                                      int monsterSpawnBlockLightLimit) {}

    /** The original values of the dimension type of the Outer Lands. */
    public static DimensionTypeValues dimensionTypeValues() {
        return new DimensionTypeValues(false, 0.0F, 256, 0, 256, 0L, 1.0D, false, false, 0);
    }

    /**
     * The offset of the landing next to an eldritch gate.
     *
     * <p>Oracle {@code TeleporterThaumcraft:102-108} drops the player one block beside the gate,
     * the side is drawn from the world random.
     */
    public static int gateLandingOffset(boolean positive) {
        return positive ? 1 : -1;
    }

    /**
     * The landing height of an eldritch gate.
     *
     * <p>Oracle {@code TeleporterThaumcraft:104} keeps the height of the gate block itself.
     */
    public static int gateLandingHeight(int gateY) {
        return gateY;
    }
}
