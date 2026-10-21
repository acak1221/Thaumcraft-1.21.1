package thaumcraft.neoforge.mixin;

import net.neoforged.neoforge.common.extensions.IFluidExtension;
import net.neoforged.neoforge.fluids.FluidType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import thaumcraft.common.lib.fluid.FluidPure;

/**
 * Gives the TC4 purifying fluid its NeoForge fluid type.
 *
 * <p>NeoForge refuses a modded fluid without an own {@link FluidType}: the vanilla
 * {@code Fluid.getFluidType} throws "Mod fluids must override getFluidType." as soon as the fluid
 * is placed, so the purifying water of the arcane spa crashed the world on NeoForge. The type
 * mirrors the vanilla water, except that the original TC4 fluid never converts to a source, which
 * the shared {@code FluidPure.canConvertToSource} already refuses.</p>
 */
@Mixin(FluidPure.class)
public abstract class NeoForgeFluidPureMixin implements IFluidExtension {
    @Unique
    private static final FluidType THAUMCRAFT$PURE_FLUID_TYPE = new FluidType(FluidType.Properties.create()
            .descriptionId("thaumcraft.purifying_fluid")
            .canConvertToSource(false)
            .canSwim(true)
            .canDrown(true)
            .canExtinguish(true)
            .canPushEntity(true)
            .motionScale(1.0D)
            .density(1000)
            .temperature(300)
            .viscosity(1000));

    @Override
    public FluidType getFluidType() {
        return THAUMCRAFT$PURE_FLUID_TYPE;
    }
}
