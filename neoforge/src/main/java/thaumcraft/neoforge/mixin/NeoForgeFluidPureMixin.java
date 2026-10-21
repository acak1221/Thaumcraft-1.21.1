package thaumcraft.neoforge.mixin;

import net.neoforged.neoforge.common.extensions.IFluidExtension;
import net.neoforged.neoforge.common.NeoForgeMod;
import net.neoforged.neoforge.fluids.FluidType;
import org.spongepowered.asm.mixin.Mixin;
import thaumcraft.common.lib.fluid.FluidPure;

/**
 * Gives the TC4 purifying fluid the NeoForge fluid type of the vanilla water.
 *
 * <p>NeoForge refuses a modded fluid without an own {@link FluidType}: the vanilla
 * {@code Fluid.getFluidType} throws "Mod fluids must override getFluidType." as soon as the fluid
 * is placed, so the purifying water of the arcane spa crashed the world on NeoForge.</p>
 *
 * <p>The type is the water type of NeoForge and NOT an own one, because the original purifying
 * fluid IS the water of the original: the shared module puts it into the
 * {@code minecraft:water} tag, so on Fabric an entity in it swims, drowns and sees its eyes in
 * the fluid, and a ray of the client hits it as a source. NeoForge does not ask the tag but the
 * identity of the fluid type: {@code Entity.isEyeInFluid(minecraft:water)} is answered by
 * comparing the type of the fluid at the eye with the water type. An own type would therefore
 * make the purifying fluid invisible to every water rule of the engine, which is exactly the
 * difference the NeoForge GameTest suite found ("actual native entity tick sees eyes just inside
 * original Forge filledPercentage: 0").</p>
 *
 * <p>The original never converts the purifying fluid to a source. That rule does not belong to
 * the fluid type here: the shared {@code FluidPure.canConvertToSource} answers it, and the
 * NeoForge extension of {@code FlowingFluid} delegates to that very method.</p>
 */
@Mixin(FluidPure.class)
public abstract class NeoForgeFluidPureMixin implements IFluidExtension {
    @Override
    public FluidType getFluidType() {
        return NeoForgeMod.WATER_TYPE.value();
    }
}
