package thaumcraft.common.mixin;

import net.minecraft.core.Holder;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.BiomeGenerationSettings;
import net.minecraft.world.level.levelgen.flat.FlatLevelGeneratorSettings;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import thaumcraft.common.lib.world.OuterLandsBiomes;

/**
 * The Outer Lands keeps the features of its biome.
 *
 * <p>The flat generator of Minecraft 1.21.1 drops every feature of the biome when its single
 * layer is an air block: {@code FlatLevelGeneratorSettings.voidGen} is then true and
 * {@code adjustGenerationSettings} only keeps the features of the biome when the biome is
 * {@code minecraft:the_void}. The Outer Lands is a void dimension with the single Eldritch
 * biome (oracle {@code ChunkProviderOuter}) and the labyrinth is the feature of that biome, so
 * the features of the Eldritch biome have to survive the flat generator. The decoration of the
 * biome refuses the Outer Lands itself ({@code ThaumcraftTreeFeature.surfaceDimension}), so
 * only the labyrinth is placed.
 */
@Mixin(FlatLevelGeneratorSettings.class)
public abstract class Tc4OuterLandsFlatGeneratorMixin {
    @Inject(method = "adjustGenerationSettings", at = @At("HEAD"), cancellable = true)
    private void thaumcraft$outerLandsFeatures(Holder<Biome> biome,
                                                CallbackInfoReturnable<BiomeGenerationSettings> result) {
        if (biome.is(OuterLandsBiomes.ELDRITCH)) {
            result.setReturnValue(biome.value().getGenerationSettings());
        }
    }
}
