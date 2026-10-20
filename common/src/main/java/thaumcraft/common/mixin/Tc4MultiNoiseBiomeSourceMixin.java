package thaumcraft.common.mixin;

import com.mojang.datafixers.util.Either;
import java.util.stream.Stream;
import net.minecraft.core.Holder;
import net.minecraft.tags.BiomeTags;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.biome.Climate;
import net.minecraft.world.level.biome.MultiNoiseBiomeSource;
import net.minecraft.world.level.biome.MultiNoiseBiomeSourceParameterList;
import net.minecraft.world.level.biome.MultiNoiseBiomeSourceParameterLists;
import net.minecraft.world.level.biome.OverworldBiomeBuilder;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import thaumcraft.common.config.ThaumcraftBiomes;
import thaumcraft.common.config.ThaumcraftBiomeSettings;
import thaumcraft.common.lib.world.biomes.BiomePatchPolicy;
import thaumcraft.common.lib.world.biomes.SeededClimateSampler;
import thaumcraft.common.lib.world.biomes.ThaumcraftBiomeSourceContext;

/** No mutation of the native climate parameter table and no extra fake points.
 * Only the real minecraft:overworld preset sees the actual TC4 biome holders. */
@Mixin(MultiNoiseBiomeSource.class)
public abstract class Tc4MultiNoiseBiomeSourceMixin {
    @Shadow @Final private Either<Climate.ParameterList<Holder<Biome>>,Holder<MultiNoiseBiomeSourceParameterList>> parameters;
    @Unique private final ThreadLocal<BiomePatchPolicy> thaumcraft$patches=new ThreadLocal<>();
    @Unique private ThaumcraftBiomeSourceContext thaumcraft$context() {
        var preset=parameters.right().orElse(null);
        if(preset==null||!preset.is(MultiNoiseBiomeSourceParameterLists.OVERWORLD))return null;
        var context=(ThaumcraftBiomeSourceContext)(Object)preset.value();
        return context.thaumcraft$biome(ThaumcraftBiomes.MAGICAL_FOREST)!=null?context:null;
    }
    @Inject(method="collectPossibleBiomes",at=@At("RETURN"),cancellable=true)
    private void thaumcraft$possible(CallbackInfoReturnable<Stream<Holder<Biome>>> result) {
        var context=thaumcraft$context();if(context!=null)result.setReturnValue(Stream.concat(result.getReturnValue(),context.thaumcraft$possibleBiomes()).distinct());
    }
    @Inject(method="getNoiseBiome(IIILnet/minecraft/world/level/biome/Climate$Sampler;)Lnet/minecraft/core/Holder;",at=@At("RETURN"),cancellable=true)
    private void thaumcraft$surface(int x,int y,int z,Climate.Sampler sampler,CallbackInfoReturnable<Holder<Biome>> result) {
        var context=thaumcraft$context();if(context==null)return;
        Holder<Biome> base=result.getReturnValue();
        if(!base.is(BiomeTags.IS_OVERWORLD)||base.is(BiomeTags.IS_OCEAN)||base.is(BiomeTags.IS_RIVER)||base.is(BiomeTags.IS_BEACH)
            ||base.is(Biomes.LUSH_CAVES)||base.is(Biomes.DRIPSTONE_CAVES)||base.is(Biomes.DEEP_DARK)||base.is(Biomes.MUSHROOM_FIELDS))return;
        // Preserve addon/custom biome entries. Native climate/depth determine
        // surface/cave routing; only original warm/cool inland temperatures
        // admit tickets, rather than painting every biome above a flat plane.
        if(base.unwrapKey().map(key->!key.location().getNamespace().equals("minecraft")).orElse(true))return;
        var target=sampler.sample(x,y,z);int band=BiomePatchPolicy.temperatureBand(target.temperature());
        if(band==0||band==4||target.continentalness()<Climate.quantizeCoord(OverworldBiomeBuilder.NEAR_INLAND_START))return;
        long seed=((SeededClimateSampler)(Object)sampler).thaumcraft$worldSeed();
        var settings=ThaumcraftBiomeSettings.current();
        var policy=thaumcraft$patches.get();if(policy==null||!policy.matches(seed,sampler,settings)){policy=new BiomePatchPolicy(seed,sampler,settings);thaumcraft$patches.set(policy);}
        int choice=policy.sample(x,z);
        if(choice==BiomePatchPolicy.MAGICAL_FOREST)result.setReturnValue(context.thaumcraft$biome(ThaumcraftBiomes.MAGICAL_FOREST));
        else if(choice==BiomePatchPolicy.TAINT)result.setReturnValue(context.thaumcraft$biome(ThaumcraftBiomes.TAINT));
    }
}
