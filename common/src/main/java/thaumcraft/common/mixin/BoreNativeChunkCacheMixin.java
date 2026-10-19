package thaumcraft.common.mixin;

import net.minecraft.server.level.ChunkHolder;
import net.minecraft.server.level.ServerChunkCache;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.status.ChunkStatus;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;
import thaumcraft.common.lib.bore.BoreNativeContext;

/** Native visible-holder/FULL-result READ, without getChunkNow's profiler call,
 * a chunk request, task scheduling, wait, generation, or native storage write. */
@Mixin(ServerChunkCache.class)
public abstract class BoreNativeChunkCacheMixin implements BoreNativeContext.LoadedChunks {
    @Invoker("getVisibleChunkIfPresent")
    public abstract ChunkHolder thaumcraft$boreVisibleChunk(long pos);

    @Override public LevelChunk thaumcraft$boreLoadedChunk(int x, int z) {
        ChunkHolder holder = thaumcraft$boreVisibleChunk(ChunkPos.asLong(x, z));
        if (holder == null || holder.getClass() != ChunkHolder.class) return null;
        var chunk = holder.getChunkIfPresent(ChunkStatus.FULL);
        return chunk instanceof LevelChunk loaded ? loaded : null;
    }
}
