package thaumcraft.common.mixin;

import net.minecraft.world.level.biome.Climate;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import thaumcraft.common.lib.world.biomes.SeededClimateSampler;

@Mixin(Climate.Sampler.class)
public abstract class Tc4ClimateSamplerMixin implements SeededClimateSampler {
    @Unique private long thaumcraft$worldSeed;
    @Override public long thaumcraft$worldSeed() {return thaumcraft$worldSeed;}
    @Override public void thaumcraft$setWorldSeed(long seed) {thaumcraft$worldSeed=seed;}
}
