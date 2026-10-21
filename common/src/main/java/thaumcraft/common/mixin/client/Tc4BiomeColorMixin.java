package thaumcraft.common.mixin.client;

import net.minecraft.client.Minecraft;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.biome.Biome;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import thaumcraft.common.config.ThaumcraftBiomeSettings;
import thaumcraft.common.config.ThaumcraftBiomes;
import thaumcraft.common.lib.world.biomes.BiomeSurfaceRules;

/**
 * Original blue_magical_forest is a local display preference: the magical forest of
 * the original is blue while the option is on (oracle
 * {@code thaumcraft/common/config/Config.java:491}).
 *
 * <p>Minecraft 1.21.1 only knows the registered colour resolvers, so the colour of
 * the biome itself is changed and not a resolver of our own: a resolver that the
 * renderer does not know crashes the terrain rendering with
 * "ClientWorld.getColor called with unregistered ColorResolver". The native
 * neighbour blending of 1.21.1 is kept, because the blending reads the colour of
 * the biome.</p>
 */
@Mixin(Biome.class)
public abstract class Tc4BiomeColorMixin {
    @Inject(method = "getGrassColor", at = @At("HEAD"), cancellable = true)
    private void thaumcraft$grass(double x, double z, CallbackInfoReturnable<Integer> result) {
        if (blueMagicalForest((Biome) (Object) this)) {
            result.setReturnValue(BiomeSurfaceRules.grassColour(BiomeSurfaceRules.Generator.MAGICAL_FOREST, true));
        }
    }

    @Inject(method = "getFoliageColor", at = @At("HEAD"), cancellable = true)
    private void thaumcraft$foliage(CallbackInfoReturnable<Integer> result) {
        if (blueMagicalForest((Biome) (Object) this)) {
            result.setReturnValue(BiomeSurfaceRules.foliageColour(BiomeSurfaceRules.Generator.MAGICAL_FOREST, true));
        }
    }

    /** True when the biome is the magical forest of the original and the option is on. */
    private static boolean blueMagicalForest(Biome biome) {
        var level = Minecraft.getInstance().level;
        if (level == null) return false;
        if (!ThaumcraftBiomeSettings.current().blueMagicalForest()) return false;
        return level.registryAccess().registryOrThrow(Registries.BIOME).getResourceKey(biome)
                .filter(ThaumcraftBiomes.MAGICAL_FOREST::equals).isPresent();
    }
}
