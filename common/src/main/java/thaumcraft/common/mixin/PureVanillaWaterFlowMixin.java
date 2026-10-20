package thaumcraft.common.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.world.level.material.FlowingFluid;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.WaterFluid;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import thaumcraft.common.lib.fluid.FluidPure;

/** Native water's reverse neighbor comparisons used to compare Material.WATER.
 * This consumer-local adapter never changes Classic's own identity checks. */
@Mixin(FlowingFluid.class)
public abstract class PureVanillaWaterFlowMixin {
    @WrapOperation(method={"getNewLiquid","affectsFlow","isSolidFace","isWaterHole","isSourceBlockOfThisType"},
            at=@At(value="INVOKE",target="Lnet/minecraft/world/level/material/Fluid;isSame(Lnet/minecraft/world/level/material/Fluid;)Z"),
            require=6,allow=6)
    private boolean thaumcraft$originalWaterNeighbor(Fluid neighbor,Fluid consumer,Operation<Boolean> original){
        if((Object)this instanceof WaterFluid&&consumer==(Object)this&&neighbor instanceof FluidPure)return true;
        return original.call(neighbor,consumer);
    }
}
