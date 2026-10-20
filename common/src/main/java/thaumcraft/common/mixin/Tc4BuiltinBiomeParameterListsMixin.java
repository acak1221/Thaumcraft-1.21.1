package thaumcraft.common.mixin;

import net.minecraft.data.worldgen.BootstrapContext;
import net.minecraft.world.level.biome.MultiNoiseBiomeSourceParameterList;
import net.minecraft.world.level.biome.MultiNoiseBiomeSourceParameterLists;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import thaumcraft.common.lib.world.biomes.BiomeSourceBootstrapScope;

/** Skip only native builtin RegistrySetBuilder construction. Actual datapack
 * registry decoding invokes the parameter-list constructor outside this scope. */
@Mixin(MultiNoiseBiomeSourceParameterLists.class)
public abstract class Tc4BuiltinBiomeParameterListsMixin {
    @Inject(method="bootstrap",at=@At("HEAD"))
    private static void thaumcraft$begin(BootstrapContext<MultiNoiseBiomeSourceParameterList> context,CallbackInfo callback) {BiomeSourceBootstrapScope.enter();}
    @Inject(method="bootstrap",at=@At("RETURN"))
    private static void thaumcraft$end(BootstrapContext<MultiNoiseBiomeSourceParameterList> context,CallbackInfo callback) {BiomeSourceBootstrapScope.leave();}
}
