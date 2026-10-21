package thaumcraft.common.lib.world;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.biome.Biome;

/**
 * The biome set of the TC4 Outer Lands.
 *
 * Oracle {@code thaumcraft/common/lib/world/biomes/BiomeGenEldritch.java:23-50} is the only
 * biome of the dimension: the 1.7 {@code WorldChunkManagerHell} is created with the single
 * {@code biomeEldritchLands} entry (oracle {@code WorldProviderOuter.func_76572_b}:64-68), so
 * every chunk of the Outer Lands is Eldritch.
 *
 * The biome itself is supplied by the native worldgen resource
 * {@code data/thaumcraft/worldgen/biome/eldritch.json}; this class owns the keys, the tags and
 * the original property values the JSON encodes.
 */
public final class OuterLandsBiomes {
    /** The single Eldritch biome of the Outer Lands. */
    public static final ResourceKey<Biome> ELDRITCH = ResourceKey.create(Registries.BIOME,
            ResourceLocation.fromNamespaceAndPath("thaumcraft", "eldritch_lands"));
    /** Every biome that belongs to the Outer Lands dimension. */
    public static final TagKey<Biome> OUTER_LANDS_BIOMES = TagKey.create(Registries.BIOME,
            ResourceLocation.fromNamespaceAndPath("thaumcraft", "outer_lands_biomes"));

    private OuterLandsBiomes() {}

    /** The original biome name (oracle BiomeGenEldritch.func_76735_a). */
    public static String name() {
        return "Eldritch";
    }

    /** The 1.21 sky colour of the biome: the original returns 0 (oracle BiomeGenEldritch:39-42). */
    public static int skyColor() {
        return OuterLandsRules.SKY_COLOR;
    }

    /**
     * The fog colour of the dimension of the Outer Lands.
     *
     * <p>The original dimension fog is 0xA080A0 scaled by 0.15 (oracle
     * {@code WorldProviderOuter.func_76562_b:84-97}). The fog colour of the biome itself is
     * the black of the original sky and lives in the native biome document
     * {@code data/thaumcraft/worldgen/biome/eldritch_lands.json} and in
     * {@link OuterLandsRules#BIOME_FOG_COLOR}.
     */
    public static int fogColor() {
        return OuterLandsRules.packedFogColor();
    }

    /** The 1.21 water colour of the biome (the 1.7 default). */
    public static int waterColor() {
        return OuterLandsRules.WATER_COLOR;
    }

    /** The 1.21 water fog colour of the biome (the 1.7 default). */
    public static int waterFogColor() {
        return OuterLandsRules.WATER_FOG_COLOR;
    }

    /** The biome never precipitates. */
    public static boolean hasPrecipitation() {
        return OuterLandsRules.BIOME_PRECIPITATION;
    }

    /** The biome temperature and downfall (the 1.7 defaults). */
    public static float temperature() {
        return OuterLandsRules.BIOME_TEMPERATURE;
    }

    public static float downfall() {
        return OuterLandsRules.BIOME_DOWNFALL;
    }

    /** True when the given biome key is the Eldritch biome of the Outer Lands. */
    public static boolean isEldritch(ResourceKey<Biome> key) {
        return ELDRITCH.equals(key);
    }
}
