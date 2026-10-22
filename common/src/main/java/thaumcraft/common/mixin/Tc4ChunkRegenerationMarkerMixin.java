package thaumcraft.common.mixin;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.ai.village.poi.PoiManager;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.ProtoChunk;
import net.minecraft.world.level.chunk.storage.ChunkSerializer;
import net.minecraft.world.level.chunk.storage.RegionStorageInfo;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import thaumcraft.common.lib.events.ChunkRegeneration;

/**
 * The chunk save and chunk load marker of the original rides on the chunk NBT of the port.
 *
 * <p>The original of 1.7.10 wrote the compound {@code Thaumcraft} with the key of
 * {@code Config.regenKey} into the NBT of every chunk it saved and read it back when the chunk
 * was loaded (oracle {@code thaumcraft/common/lib/events/EventHandlerWorld.java:114-137}). The
 * Forge 1.7.10 platform offered the two events {@code ChunkDataEvent.Save} and
 * {@code ChunkDataEvent.Load} for that; Minecraft 1.21.1 has no such event and no extension
 * point for a per chunk compound of a mod, because the vanilla writes the chunk NBT as a fixed
 * set of keys.</p>
 *
 * <p>The vanilla writes and reads the chunk NBT in exactly one place each
 * ({@code net.minecraft.world.level.chunk.storage.ChunkSerializer.write} and {@code .read}), so
 * the marker of the original is written and read there. The compound is the one of the original:
 * the name {@code Thaumcraft} and the key of the regeneration category of the original
 * configuration.</p>
 */
@Mixin(ChunkSerializer.class)
public abstract class Tc4ChunkRegenerationMarkerMixin {
    /**
     * The write side of the marker (oracle EventHandlerWorld:114-119).
     */
    @Inject(method = "write", at = @At("RETURN"))
    private static void thaumcraft$writeMarker(ServerLevel level, ChunkAccess chunk,
            CallbackInfoReturnable<CompoundTag> cir) {
        ChunkRegeneration.writeMarker(cir.getReturnValue());
    }

    /**
     * The read side of the marker (oracle EventHandlerWorld:121-137).
     */
    @Inject(method = "read", at = @At("RETURN"))
    private static void thaumcraft$readMarker(ServerLevel level, PoiManager poi, RegionStorageInfo info,
            ChunkPos pos, CompoundTag tag, CallbackInfoReturnable<ProtoChunk> cir) {
        ChunkRegeneration.readMarker(level, pos, tag);
    }
}
