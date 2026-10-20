package thaumcraft.common.mixin.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BiomeColors;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.ColorResolver;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import thaumcraft.common.config.ThaumcraftBiomes;
import thaumcraft.common.config.ThaumcraftBiomeSettings;

/** Original blue_magical_forest is a local display preference. Resolve only
 * the real canonical biome identity and preserve native neighbor tint blending. */
@Mixin(BiomeColors.class)
public abstract class Tc4BiomeColorsMixin {
    @Unique private static final ColorResolver thaumcraft$grass=(biome,x,z)->{
        var level=Minecraft.getInstance().level;
        if(level!=null&&level.registryAccess().registryOrThrow(Registries.BIOME).getResourceKey(biome).filter(ThaumcraftBiomes.MAGICAL_FOREST::equals).isPresent())
            return ThaumcraftBiomeSettings.current().blueMagicalForest()?0x66AACC:5635969;
        return biome.getGrassColor(x,z);
    };
    @Unique private static final ColorResolver thaumcraft$foliage=(biome,x,z)->{
        var level=Minecraft.getInstance().level;
        if(level!=null&&level.registryAccess().registryOrThrow(Registries.BIOME).getResourceKey(biome).filter(ThaumcraftBiomes.MAGICAL_FOREST::equals).isPresent())
            return ThaumcraftBiomeSettings.current().blueMagicalForest()?0x77CCEE:6750149;
        return biome.getFoliageColor();
    };
    @Inject(method="getAverageGrassColor",at=@At("HEAD"),cancellable=true)
    private static void thaumcraft$grass(BlockAndTintGetter level,BlockPos pos,CallbackInfoReturnable<Integer> result) {result.setReturnValue(level.getBlockTint(pos,thaumcraft$grass));}
    @Inject(method="getAverageFoliageColor",at=@At("HEAD"),cancellable=true)
    private static void thaumcraft$foliage(BlockAndTintGetter level,BlockPos pos,CallbackInfoReturnable<Integer> result) {result.setReturnValue(level.getBlockTint(pos,thaumcraft$foliage));}
}
