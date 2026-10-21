package thaumcraft.common.mixin;

import net.minecraft.server.level.WorldGenRegion;
import net.minecraft.world.level.chunk.status.ChunkStep;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/**
 * The generation step of a world generation region. {@code WorldGenRegion#ensureCanWrite} answers
 * from {@code ChunkStep#blockStateWriteRadius}, but it also logs a "Detected setBlock in a far
 * chunk" error whenever it answers no, so a feature that only wants to ask cannot use it.
 */
@Mixin(WorldGenRegion.class)
public interface WorldGenRegionStepAccess {
    @Accessor("generatingStep")
    ChunkStep thaumcraft$getGeneratingStep();
}
