package thaumcraft.common.mixin;

import net.minecraft.core.HolderGetter;
import net.minecraft.world.level.levelgen.NoiseGeneratorSettings;
import net.minecraft.world.level.levelgen.RandomState;
import net.minecraft.world.level.levelgen.synth.NormalNoise;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import thaumcraft.common.lib.world.biomes.SeededClimateSampler;

@Mixin(RandomState.class)
public abstract class Tc4RandomStateMixin {
    @Inject(method="<init>",at=@At("TAIL"))
    private void thaumcraft$seed(NoiseGeneratorSettings settings,HolderGetter<NormalNoise.NoiseParameters> noises,long seed,CallbackInfo callback) {
        ((SeededClimateSampler)(Object)((RandomState)(Object)this).sampler()).thaumcraft$setWorldSeed(seed);
    }
}
