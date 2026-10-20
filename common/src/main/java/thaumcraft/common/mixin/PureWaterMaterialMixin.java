package thaumcraft.common.mixin;

import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.WaterFluid;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import thaumcraft.common.lib.fluid.FluidPure;

/** Old vanilla water compared Material.WATER; Classic compared its exact block.
 * Native fluid identity splits those original asymmetric decisions. */
@Mixin(WaterFluid.class)
public abstract class PureWaterMaterialMixin {
    @Inject(method="isSame",at=@At("HEAD"),cancellable=true)
    private void thaumcraft$originalPureWaterMaterial(Fluid other,CallbackInfoReturnable<Boolean> result){
        if(other instanceof FluidPure)result.setReturnValue(true);
    }
}
