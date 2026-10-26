package thaumcraft.common.mixin;

import java.util.List;
import net.minecraft.world.level.biome.Climate;
import net.minecraft.world.level.levelgen.Aquifer;
import net.minecraft.world.level.levelgen.DensityFunctions;
import net.minecraft.world.level.levelgen.NoiseChunk;
import net.minecraft.world.level.levelgen.NoiseGeneratorSettings;
import net.minecraft.world.level.levelgen.NoiseRouter;
import net.minecraft.world.level.levelgen.NoiseSettings;
import net.minecraft.world.level.levelgen.RandomState;
import net.minecraft.world.level.levelgen.blending.Blender;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import thaumcraft.common.lib.world.biomes.SeededClimateSampler;

/**
 * Chunk generation fills the biomes of a chunk from the climate sampler of its NoiseChunk, a new
 * sampler over the chunk's cached noise rather than {@code RandomState.sampler()}. Without the
 * world seed on it, the TC4 biome patches of generated chunks followed seed 0 in every world while
 * {@code /locate} and every other query of the biome source used the real seed.
 */
@Mixin(NoiseChunk.class)
public abstract class Tc4NoiseChunkSeedMixin {
    @Unique private long thaumcraft$worldSeed;

    @Inject(method="<init>",at=@At("TAIL"))
    private void thaumcraft$seed(int cellCountXZ,RandomState random,int firstNoiseX,int firstNoiseZ,NoiseSettings noiseSettings,
            DensityFunctions.BeardifierOrMarker beardifier,NoiseGeneratorSettings settings,Aquifer.FluidPicker fluidPicker,Blender blender,CallbackInfo callback) {
        thaumcraft$worldSeed=((SeededClimateSampler)(Object)random.sampler()).thaumcraft$worldSeed();
    }

    @Inject(method="cachedClimateSampler",at=@At("RETURN"))
    private void thaumcraft$seedSampler(NoiseRouter router,List<Climate.ParameterPoint> spawnTarget,CallbackInfoReturnable<Climate.Sampler> result) {
        ((SeededClimateSampler)(Object)result.getReturnValue()).thaumcraft$setWorldSeed(thaumcraft$worldSeed);
    }
}
