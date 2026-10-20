package thaumcraft.common.mixin;

import java.util.Map;
import java.util.stream.Stream;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderGetter;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.MultiNoiseBiomeSourceParameterList;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import thaumcraft.common.config.ThaumcraftBiomes;
import thaumcraft.common.lib.world.biomes.ThaumcraftBiomeSourceContext;
import thaumcraft.common.lib.world.biomes.BiomeSourceBootstrapScope;

@Mixin(MultiNoiseBiomeSourceParameterList.class)
public abstract class Tc4BiomeParameterListMixin implements ThaumcraftBiomeSourceContext {
    @Unique private Map<ResourceKey<Biome>,Holder<Biome>> thaumcraft$biomes=Map.of();
    @Inject(method="<init>",at=@At("TAIL"))
    private void thaumcraft$capture(MultiNoiseBiomeSourceParameterList.Preset preset,HolderGetter<Biome> biomes,CallbackInfo callback) {
        if(!BiomeSourceBootstrapScope.active()&&preset==MultiNoiseBiomeSourceParameterList.Preset.OVERWORLD)thaumcraft$biomes=Map.of(
            ThaumcraftBiomes.MAGICAL_FOREST,biomes.getOrThrow(ThaumcraftBiomes.MAGICAL_FOREST),
            ThaumcraftBiomes.EERIE,biomes.getOrThrow(ThaumcraftBiomes.EERIE),
            ThaumcraftBiomes.TAINT,biomes.getOrThrow(ThaumcraftBiomes.TAINT));
    }
    @Override public Stream<Holder<Biome>> thaumcraft$possibleBiomes() {return Stream.of(ThaumcraftBiomes.MAGICAL_FOREST,ThaumcraftBiomes.EERIE,ThaumcraftBiomes.TAINT).map(thaumcraft$biomes::get).filter(java.util.Objects::nonNull);}
    @Override public Holder<Biome> thaumcraft$biome(ResourceKey<Biome> key) {return thaumcraft$biomes.get(key);}
}
