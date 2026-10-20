package thaumcraft.fabric.mixin.compat;

import net.minecraft.core.component.DataComponentPatch;
import net.minecraft.core.component.PatchedDataComponentMap;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Architectury13.0.11 Fabric inverts Pair.getPatch's empty predicate, so
 * nonempty stacks lose every component in equality/copyWithAmount/codec and
 * native FluidVariant conversion. Keep its actual map, change only that
 * proven predicate; no library fork or Spa-only equality workaround. */
@Mixin(targets="dev.architectury.fluid.fabric.FluidStackImpl$Pair",remap=false)
public abstract class ArchitecturyFluidPatchMixin {
    @Shadow public Fluid fluid;
    @Shadow public PatchedDataComponentMap components;
    @Shadow public long amount;
    @Inject(method="getPatch",at=@At("HEAD"),cancellable=true,remap=false)
    private void thaumcraft$actualFluidPatch(CallbackInfoReturnable<DataComponentPatch> result) {
        result.setReturnValue(amount>0L&&fluid!=Fluids.EMPTY?components.asPatch():DataComponentPatch.EMPTY);
    }
}
