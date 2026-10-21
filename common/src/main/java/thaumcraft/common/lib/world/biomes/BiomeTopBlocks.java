package thaumcraft.common.lib.world.biomes;

import java.util.Map;
import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

/**
 * The top block of a biome, the 1.7.10 {@code BiomeGenBase.topBlock}.
 *
 * <p>Oracle {@code net/minecraft/world/biome/BiomeGenBase.java:135-136} gives every biome a
 * top block; most of them keep the default {@code Blocks.grass} and a few of them replace
 * it: the desert with sand, the mesa with the red sand, the mushroom island with the
 * mycelium, the stone beach with the stone, the end with the dirt and the hell with the
 * netherrack. The TC4 eldritch biome replaces it with the dirt
 * ({@code thaumcraft/common/lib/world/biomes/BiomeGenEldritch.java:33}).
 *
 * <p>1.21.1 has no top block in the biome document, so the value is a table of the native
 * biomes here. The original used the top block of the biome of the place of the structure
 * to fill the ground under it: {@code WorldGenHilltopStones:67,81-82} and, as dead code,
 * {@code WorldGenEldritchRing:68}.
 */
public final class BiomeTopBlocks {
    private BiomeTopBlocks() {}

    /** The default top block of the 1.7.10 biomes, oracle BiomeGenBase:135. */
    public static final BlockState DEFAULT = Blocks.GRASS_BLOCK.defaultBlockState();

    /** The biomes of 1.7.10 that replaced the top block. */
    private static final Map<String, BlockState> OVERRIDES = Map.of(
            // Oracle BiomeGenDesert:18 - the desert.
            "minecraft:desert", Blocks.SAND.defaultBlockState(),
            // Oracle BiomeGenMesa:33-34 - the mesa and its red sand.
            "minecraft:badlands", Blocks.RED_SAND.defaultBlockState(),
            // Oracle BiomeGenMushroomIsland:20 - the mushroom island.
            "minecraft:mushroom_island", Blocks.MYCELIUM.defaultBlockState(),
            // Oracle BiomeGenStoneBeach:14 - the stone beach.
            "minecraft:stone_shore", Blocks.STONE.defaultBlockState(),
            // Oracle BiomeGenEnd:21 - the end.
            "minecraft:the_end", Blocks.DIRT.defaultBlockState(),
            // The hell of the original.
            "minecraft:the_nether", Blocks.NETHERRACK.defaultBlockState(),
            // Oracle BiomeGenHills:72 - the stone hill.
            "minecraft:windswept_hills", Blocks.STONE.defaultBlockState(),
            // Oracle BiomeGenHills:69 - the gravel hill.
            "minecraft:windswept_gravelly_hills", Blocks.GRAVEL.defaultBlockState(),
            // Oracle BiomeGenEldritch:33 - the eldritch biome of the Outer Lands.
            "thaumcraft:eldritch_lands", Blocks.DIRT.defaultBlockState());

    /**
     * The top block of a biome.
     *
     * @param biome the biome of the column
     * @return the block the original of the biome would put on the ground
     */
    public static BlockState topBlock(Holder<Biome> biome) {
        ResourceLocation key = biome.unwrapKey().map(holder -> holder.location()).orElse(null);
        if (key == null) return DEFAULT;
        return OVERRIDES.getOrDefault(key.toString(), DEFAULT);
    }
}
