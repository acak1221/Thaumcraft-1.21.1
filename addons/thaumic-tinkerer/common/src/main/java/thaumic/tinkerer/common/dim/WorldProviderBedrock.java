/**
 * ThaumicTinkerer is Open Source and distributed under a Creative Commons Attribution-NonCommercial-ShareAlike 3.0
 * License (http://creativecommons.org/licenses/by-nc-sa/3.0/deed.en_GB)
 *
 * ThaumicTinkerer is a Derivative Work on Thaumcraft 4. Thaumcraft 4 (c) Azanor 2012
 * (http://www.minecraftforum.net/topic/1585216-)
 *
 * Ported to Minecraft 1.21.1 for Thaumcraft 4 Reload (see addons/thaumic-tinkerer/NOTICE.md).
 */
package thaumic.tinkerer.common.dim;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import thaumic.tinkerer.common.lib.LibMisc;

/**
 * What is left of the 1.7.10 {@code WorldProviderBedrock}: the identity of the dimension and the questions the rest of the
 * add-on asks about it. A world provider is data in 1.21.1, so its properties moved into the data pack:
 *
 * <ul>
 *   <li>{@code data/thaumictinkerer/dimension_type/bedrock.json}: sky light, no ceiling, 256 blocks from y = 0 (1.7.10's
 *       world height), {@code ambient_light} 1 (the 1.7.10 brightness table gave every light level full brightness, so the
 *       dimension has no darkness), beds and respawn anchors do not work ({@code canRespawnHere() == false}), not natural
 *       ({@code isSurfaceWorld() == false});</li>
 *   <li>{@code data/thaumictinkerer/dimension/bedrock.json}: the generator {@link ChunkGeneratorBedrock} over a single biome;</li>
 *   <li>{@code data/thaumictinkerer/worldgen/biome/bedrock.json}: the biome (1.7.10 used the beach biome): the sky of the
 *       overworld, the dark purple fog of the End's {@code getFogColor} and the beach's monsters.</li>
 * </ul>
 *
 * The sun and moon sizes, the star brightness and {@code getEntrancePortalLocation} of the original were methods that
 * nothing called (no {@code @Override}) and are not ported.
 */
public final class WorldProviderBedrock {
    /** The dimension: {@code thaumictinkerer:bedrock} (1.7.10: dimension id -19, configurable). */
    public static final ResourceKey<Level> KEY = ResourceKey.create(Registries.DIMENSION, LibMisc.id("bedrock"));

    /** The height of the dimension (the dimension type: min_y 0, height 256). */
    public static final int HEIGHT = 256;
    /** The portal layers: blocks above this height are the "top two layers", where the exit portal is made and bedrock is kept. */
    public static final int PORTAL_LAYER_ABOVE = 253;

    private WorldProviderBedrock() {}

    /** True for the Bedrock dimension (1.7.10: {@code provider instanceof WorldProviderBedrock}). */
    public static boolean isBedrock(Level level) {
        return level.dimension().equals(KEY);
    }

    /**
     * 1.7.10 {@code provider.isSurfaceWorld()}: true for the Overworld and any dimension that, like it, has a sky and no
     * ceiling; false for the Nether and the End, and for the Bedrock dimension itself (which declared itself no surface).
     */
    public static boolean isSurfaceWorld(Level level) {
        return !isBedrock(level) && level.dimensionType().hasSkyLight() && !level.dimensionType().hasCeiling();
    }
}
