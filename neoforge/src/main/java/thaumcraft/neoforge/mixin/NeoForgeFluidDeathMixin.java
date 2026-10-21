package thaumcraft.neoforge.mixin;

import net.neoforged.neoforge.common.extensions.IFluidExtension;
import net.neoforged.neoforge.fluids.FluidType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import thaumcraft.common.blocks.BlockFluidDeath;

/**
 * Gives the TC4 death fluid its NeoForge fluid type.
 *
 * <p>Like the purifying fluid, the death fluid is a modded fluid, and NeoForge refuses a modded
 * fluid without an own {@link FluidType}. The type mirrors the vanilla water, except that the
 * original TC4 death fluid never converts to a source and glows with the light of its block
 * (eight, oracle BlockFluidDeath:66).</p>
 */
@Mixin(BlockFluidDeath.DeathFluid.class)
public abstract class NeoForgeFluidDeathMixin implements IFluidExtension {
    @Unique
    private static final FluidType THAUMCRAFT$DEATH_FLUID_TYPE = new FluidType(FluidType.Properties.create()
            .descriptionId("thaumcraft.death_fluid")
            .canConvertToSource(false)
            .canSwim(false)
            .canDrown(true)
            .canExtinguish(false)
            .canPushEntity(false)
            .motionScale(0.25D)
            .density(3000)
            .temperature(200)
            .viscosity(6000)
            .lightLevel(8));

    @Override
    public FluidType getFluidType() {
        return THAUMCRAFT$DEATH_FLUID_TYPE;
    }
}
