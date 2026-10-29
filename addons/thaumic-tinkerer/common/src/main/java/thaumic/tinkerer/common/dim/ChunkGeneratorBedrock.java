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

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.WorldGenRegion;
import net.minecraft.world.level.LevelHeightAccessor;
import net.minecraft.world.level.NoiseColumn;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.biome.BiomeManager;
import net.minecraft.world.level.biome.BiomeSource;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.chunk.LevelChunkSection;
import net.minecraft.world.level.levelgen.GenerationStep;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.RandomState;
import net.minecraft.world.level.levelgen.blending.Blender;

/**
 * The terrain of the Bedrock dimension (1.7.10 {@code ChunkProviderBedrock}, which was a copy of the superflat provider
 * with one layer of 256 blocks of bedrock): every block of every chunk is bedrock, and the ore clusters of
 * {@link OreClusterGenerator} are the only variation. There are no caves, no surface, no structures and no features of
 * the biome. The dimension file names this generator by its registered type {@code thaumictinkerer:bedrock}.
 */
public final class ChunkGeneratorBedrock extends ChunkGenerator {
    public static final MapCodec<ChunkGeneratorBedrock> CODEC = RecordCodecBuilder.mapCodec(instance -> instance
            .group(BiomeSource.CODEC.fieldOf("biome_source").forGetter(generator -> generator.biomeSource))
            .apply(instance, instance.stable(ChunkGeneratorBedrock::new)));

    public ChunkGeneratorBedrock(BiomeSource biomeSource) {
        super(biomeSource);
    }

    @Override
    protected MapCodec<? extends ChunkGenerator> codec() {
        return CODEC;
    }

    /** 1.7.10 {@code provideChunk}: the 256 layers of bedrock (the light and heightmaps are the world's to calculate). */
    @Override
    public CompletableFuture<ChunkAccess> fillFromNoise(Blender blender, RandomState randomState,
            StructureManager structureManager, ChunkAccess chunk) {
        fill(chunk);
        return CompletableFuture.completedFuture(chunk);
    }

    /** Fills every section of the chunk with bedrock and primes the generation heightmaps (the top of the column is bedrock). */
    public static void fill(ChunkAccess chunk) {
        BlockState bedrock = Blocks.BEDROCK.defaultBlockState();
        for (int index = 0; index < chunk.getSectionsCount(); index++) {
            LevelChunkSection section = chunk.getSection(index);
            section.acquire();
            try {
                for (int y = 0; y < 16; y++) {
                    for (int x = 0; x < 16; x++) {
                        for (int z = 0; z < 16; z++) section.setBlockState(x, y, z, bedrock, false);
                    }
                }
            } finally {
                section.release();
            }
        }
        Heightmap oceanFloor = chunk.getOrCreateHeightmapUnprimed(Heightmap.Types.OCEAN_FLOOR_WG);
        Heightmap worldSurface = chunk.getOrCreateHeightmapUnprimed(Heightmap.Types.WORLD_SURFACE_WG);
        int top = chunk.getMaxBuildHeight() - 1;
        for (int x = 0; x < 16; x++) {
            for (int z = 0; z < 16; z++) {
                oceanFloor.update(x, top, z, bedrock);
                worldSurface.update(x, top, z, bedrock);
            }
        }
    }

    /** 1.7.10 {@code populate}: the ore clusters. */
    @Override
    public void applyBiomeDecoration(WorldGenLevel level, ChunkAccess chunk, StructureManager structureManager) {
        OreClusterGenerator.populate(level, chunk.getPos().x, chunk.getPos().z);
    }

    @Override
    public void applyCarvers(WorldGenRegion region, long seed, RandomState randomState, BiomeManager biomeManager,
            StructureManager structureManager, ChunkAccess chunk, GenerationStep.Carving carving) {}

    @Override
    public void buildSurface(WorldGenRegion region, StructureManager structureManager, RandomState randomState,
            ChunkAccess chunk) {}

    /** The beach biome of 1.7.10 spawned no animals at generation either. */
    @Override
    public void spawnOriginalMobs(WorldGenRegion region) {}

    @Override
    public int getGenDepth() {
        return WorldProviderBedrock.HEIGHT;
    }

    /** 1.7.10 {@code getAverageGroundLevel}: 1. */
    @Override
    public int getSeaLevel() {
        return 1;
    }

    @Override
    public int getMinY() {
        return 0;
    }

    /** Solid to the top: the first free height is the world height. */
    @Override
    public int getBaseHeight(int x, int z, Heightmap.Types type, LevelHeightAccessor level, RandomState randomState) {
        return level.getMaxBuildHeight();
    }

    @Override
    public NoiseColumn getBaseColumn(int x, int z, LevelHeightAccessor level, RandomState randomState) {
        BlockState[] column = new BlockState[level.getHeight()];
        java.util.Arrays.fill(column, Blocks.BEDROCK.defaultBlockState());
        return new NoiseColumn(level.getMinBuildHeight(), column);
    }

    @Override
    public void addDebugScreenInfo(List<String> info, RandomState randomState, BlockPos pos) {}
}
