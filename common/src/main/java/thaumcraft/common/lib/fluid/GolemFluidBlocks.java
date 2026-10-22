package thaumcraft.common.lib.fluid;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.FluidState;

/**
 * The fluid of a block, without any loader.
 *
 * <p>Oracle {@code thaumcraft/common/entities/golems/GolemHelper.java:431-446}: a marked block is a
 * source of the wanted fluid when the block of the fluid is the block of the marker, or when the
 * block is a full source of the water or of the lava of the original.</p>
 */
public final class GolemFluidBlocks {
    private GolemFluidBlocks() {}

    /** The fluid of the block state, or {@code null} when the block holds none. */
    public static Fluid fluid(BlockState state) {
        if (state == null) return null;
        FluidState fluid = state.getFluidState();
        return fluid.isEmpty() ? null : fluid.getType();
    }

    /** The name of the fluid of the block state, or {@code null}. */
    public static ResourceLocation fluidId(BlockState state) {
        Fluid fluid = fluid(state);
        return fluid == null ? null : BuiltInRegistries.FLUID.getKey(fluid);
    }

    /** The named fluid of the registry, or {@code null}. */
    public static Fluid fluid(ResourceLocation id) {
        if (id == null) return null;
        return BuiltInRegistries.FLUID.get(id);
    }

    /** Whether the block is a full source of its fluid. */
    public static boolean isSource(BlockState state) {
        return state != null && state.getFluidState().isSource();
    }

    /** Whether the block holds the named fluid. */
    public static boolean isFluid(BlockState state, ResourceLocation id) {
        return id != null && id.equals(fluidId(state));
    }
}
