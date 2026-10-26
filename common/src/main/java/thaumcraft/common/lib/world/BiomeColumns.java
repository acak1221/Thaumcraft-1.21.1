package thaumcraft.common.lib.world;

import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.QuartPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.WorldGenRegion;
import net.minecraft.util.Mth;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.Climate;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.status.ChunkStatus;
import net.minecraft.world.level.levelgen.Heightmap;

/** Native replacement for Utils.setBiomeAt's removed two-dimensional byte array and numeric-biome packet. */
public final class BiomeColumns {
    private BiomeColumns() {}

    private static void requireWorld(ServerLevel level) {
        if (!level.getServer().isSameThread() || level.getServer().getLevel(level.dimension()) != level)
            throw new IllegalStateException("Biome columns require the current native server world and its server thread");
    }

    /** The stored surface quart holder, not a biome-manager Voronoi sample of an adjacent storage cell. Never loads a chunk. */
    public static Holder<Biome> surfaceHolder(ServerLevel level, int x, int z) {
        requireWorld(level);
        LevelChunk chunk = level.getChunkSource().getChunkNow(x >> 4, z >> 4);
        if (chunk == null) return null;
        int y = Mth.clamp(chunk.getHeight(Heightmap.Types.WORLD_SURFACE, x & 15, z & 15), level.getMinBuildHeight(), level.getMaxBuildHeight() - 1);
        return chunk.getNoiseBiome(QuartPos.fromBlock(x), QuartPos.fromBlock(y), QuartPos.fromBlock(z));
    }

    /** Generation-region overload: read only the existing biome-ready region, without requesting another chunk. */
    public static Holder<Biome> surfaceHolder(WorldGenLevel level, int x, int z) {
        if (level instanceof ServerLevel server) return surfaceHolder(server, x, z);
        requireRegion(level);
        if (!level.hasChunk(x >> 4, z >> 4)) return null;
        ChunkAccess chunk = level.getChunk(x >> 4, z >> 4, ChunkStatus.BIOMES, false);
        if (chunk == null) return null;
        int y = Mth.clamp(chunk.getHeight(Heightmap.Types.WORLD_SURFACE, x & 15, z & 15), level.getMinBuildHeight(), level.getMaxBuildHeight() - 1);
        return chunk.getNoiseBiome(QuartPos.fromBlock(x), QuartPos.fromBlock(y), QuartPos.fromBlock(z));
    }

    private static void requireRegion(WorldGenLevel level) {
        if (!(level instanceof WorldGenRegion)) throw new IllegalArgumentException("Biome generation columns require the native generation region");
    }

    /**
     * One old 1x1 horizontal column becomes one native4x4 quart column across every quart Y.
     * All other stored columns remain byte-for-byte equivalent holders; no remote chunk is requested.
     */
    public static boolean set(ServerLevel level, int x, int z, ResourceKey<Biome> targetKey) {
        requireWorld(level);
        LevelChunk chunk = level.getChunkSource().getChunkNow(x >> 4, z >> 4);
        if (chunk == null) return false;
        Holder<Biome> target = target(level, new BlockPos(x, chunk.getHeight(Heightmap.Types.WORLD_SURFACE, x & 15, z & 15), z), targetKey);
        if (target == null) return false;
        if (!rewriteColumn(chunk, target, targetKey, x, z, level.getChunkSource().randomState().sampler())) return false;
        level.getChunkSource().chunkMap.resendBiomesForChunks(List.of(chunk));
        return true;
    }

    /**
     * Utils.setBiomeAt with the biome WorldChunkManager.getBiomeGenAt(..., true) generated for the column:
     * every quart Y of the column gets back the biome the generator gives it, and a generated
     * {@code refused} biome becomes {@code replacement} instead. Never loads a chunk.
     */
    public static boolean restoreGenerated(ServerLevel level, int x, int z, ResourceKey<Biome> refused, ResourceKey<Biome> replacement) {
        requireWorld(level);
        LevelChunk chunk = level.getChunkSource().getChunkNow(x >> 4, z >> 4);
        if (chunk == null) return false;
        Holder<Biome> substitute = target(level, new BlockPos(x, chunk.getHeight(Heightmap.Types.WORLD_SURFACE, x & 15, z & 15), z), replacement);
        if (substitute == null) return false;
        var source = level.getChunkSource().getGenerator().getBiomeSource();
        Climate.Sampler sampler = level.getChunkSource().randomState().sampler();
        int selectedX = QuartPos.fromBlock(x), selectedZ = QuartPos.fromBlock(z);
        int minY = QuartPos.fromBlock(chunk.getMinBuildHeight());
        @SuppressWarnings("unchecked")
        Holder<Biome>[] generated = new Holder[QuartPos.fromBlock(chunk.getMaxBuildHeight() - 1) - minY + 1];
        boolean changed = false;
        for (int index = 0; index < generated.length; index++) {
            Holder<Biome> biome = source.getNoiseBiome(selectedX, minY + index, selectedZ, sampler);
            generated[index] = biome.is(refused) ? substitute : biome;
            if (chunk.getNoiseBiome(selectedX, minY + index, selectedZ) != generated[index]) changed = true;
        }
        if (!changed) return false;
        chunk.fillBiomesFromNoise((quartX, quartY, quartZ, ignoredSampler) -> quartX == selectedX && quartZ == selectedZ
                ? generated[quartY - minY] : chunk.getNoiseBiome(quartX, quartY, quartZ), sampler);
        chunk.setUnsaved(true);
        level.getChunkSource().chunkMap.resendBiomesForChunks(List.of(chunk));
        return true;
    }

    /** Generation-stage variant: mutate only the existing writable region chunk; its eventual full chunk packet carries the biome data. */
    public static boolean set(WorldGenLevel level, int x, int z, ResourceKey<Biome> targetKey) {
        if (level instanceof ServerLevel server) return set(server, x, z, targetKey);
        requireRegion(level);
        if (!level.hasChunk(x >> 4, z >> 4)) return false;
        ChunkAccess chunk = level.getChunk(x >> 4, z >> 4, ChunkStatus.BIOMES, false);
        if (chunk == null) return false;
        int y = Mth.clamp(chunk.getHeight(Heightmap.Types.WORLD_SURFACE, x & 15, z & 15), level.getMinBuildHeight(), level.getMaxBuildHeight() - 1);
        if (!WorldGenWriteArea.canWrite(level, new BlockPos(x, y, z))) return false;
        Holder<Biome> target = target(level, new BlockPos(x, y, z), targetKey);
        if (target == null) return false;
        // RandomState/Sampler are immutable generation inputs. No mutable ServerLevel chunk query, update or packet occurs here.
        return rewriteColumn(chunk, target, targetKey, x, z, level.getLevel().getChunkSource().randomState().sampler());
    }

    /** Admit an optional feature before it writes anything, not halfway through its conversion loop. */
    public static boolean targetAvailable(WorldGenLevel level, BlockPos position, ResourceKey<Biome> targetKey) {
        return target(level, position, targetKey) != null;
    }

    private static Holder<Biome> target(WorldGenLevel level, BlockPos position, ResourceKey<Biome> targetKey) {
        Holder<Biome> target = level.registryAccess().registry(Registries.BIOME)
                .flatMap(registry -> registry.getHolder(targetKey)).orElse(null);
        if (target == null) com.mojang.logging.LogUtils.getLogger().error(
                "Skipping biome conversion/decoration at {}: Missing native biome {}", position, targetKey.location());
        return target;
    }

    private static boolean rewriteColumn(ChunkAccess chunk, Holder<Biome> target, ResourceKey<Biome> targetKey,
                                         int x, int z, Climate.Sampler sampler) {
        int selectedX = QuartPos.fromBlock(x), selectedZ = QuartPos.fromBlock(z);
        boolean changed = false;
        for (int y = QuartPos.fromBlock(chunk.getMinBuildHeight()); y <= QuartPos.fromBlock(chunk.getMaxBuildHeight() - 1); y++) {
            if (!chunk.getNoiseBiome(selectedX, y, selectedZ).is(targetKey)) { changed = true; break; }
        }
        if (!changed) return false;
        chunk.fillBiomesFromNoise((quartX, quartY, quartZ, ignoredSampler) -> quartX == selectedX && quartZ == selectedZ
                ? target : chunk.getNoiseBiome(quartX, quartY, quartZ), sampler);
        chunk.setUnsaved(true);
        return true;
    }
}
